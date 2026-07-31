param(
    [string]$BaseUrl = "http://localhost:8081/api",
    [string]$PrometheusUrl = "http://localhost:9090",
    [int]$Requests = 100,
    [int]$Concurrency = 10
)

$ErrorActionPreference = "Stop"

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "$BaseUrl/auth/login" `
    -ContentType "application/json" `
    -Body (
        @{
            email = "trader@db.com"
            password = "trader123"
        } | ConvertTo-Json
    )

$tradeDate = Get-Date -Format "yyyy-MM-dd"
$referenceDate = Get-Date -Format "yyyyMMdd"

# A different three-letter prefix on each run prevents duplicate trade refs.
$prefix = -join (
    1..3 | ForEach-Object {
        [char](Get-Random -Minimum 65 -Maximum 91)
    }
)

$bodies = 1..$Requests | ForEach-Object {
    $suffix = "{0:D4}" -f $_

    @{
        tradeRef = "$prefix-$referenceDate-$suffix"
        instrumentId = 1
        counterpartyId = 1
        assetClass = "EQUITY"
        side = "BUY"
        quantity = 100
        price = 245.50
        tradeDate = $tradeDate
    } | ConvertTo-Json -Compress
}

$worker = {
    param(
        [string]$Url,
        [string]$Token,
        [string]$Body
    )

    $statusCode = 0
    $responseBody = ""
    $watch = [Diagnostics.Stopwatch]::StartNew()

    try {
        $request = [System.Net.HttpWebRequest]::Create($Url)
        $request.Method = "POST"
        $request.ContentType = "application/json"
        $request.Headers["Authorization"] = "Bearer $Token"

        $bytes = [Text.Encoding]::UTF8.GetBytes($Body)
        $request.ContentLength = $bytes.Length

        $requestStream = $request.GetRequestStream()

        try {
            $requestStream.Write(
                $bytes,
                0,
                $bytes.Length
            )
        }
        finally {
            $requestStream.Dispose()
        }

        $response = $request.GetResponse()

        try {
            $statusCode = [int]$response.StatusCode

            $reader = New-Object System.IO.StreamReader(
                $response.GetResponseStream()
            )

            try {
                $responseBody = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
        }
        finally {
            $response.Dispose()
        }
    }
    catch [System.Net.WebException] {
        $errorResponse = $_.Exception.Response

        if ($null -ne $errorResponse) {
            $statusCode = [int]$errorResponse.StatusCode

            $reader = New-Object System.IO.StreamReader(
                $errorResponse.GetResponseStream()
            )

            try {
                $responseBody = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
                $errorResponse.Dispose()
            }
        }
        else {
            $responseBody = $_.Exception.Message
        }
    }
    catch {
        $responseBody = $_.Exception.Message
    }
    finally {
        $watch.Stop()
    }

    [pscustomobject]@{
        StatusCode = $statusCode
        Milliseconds = $watch.ElapsedMilliseconds
        Body = $responseBody
    }
}

$pool = [RunspaceFactory]::CreateRunspacePool(
    1,
    $Concurrency
)

$pool.Open()

$jobs = @()
$workerText = $worker.ToString()
$url = "$BaseUrl/v1/trades"

$overallWatch = [Diagnostics.Stopwatch]::StartNew()

foreach ($body in $bodies) {
    $powerShell = [PowerShell]::Create()
    $powerShell.RunspacePool = $pool

    [void]$powerShell.AddScript($workerText)
    [void]$powerShell.AddArgument($url)
    [void]$powerShell.AddArgument($login.token)
    [void]$powerShell.AddArgument($body)

    $jobs += [pscustomobject]@{
        PowerShell = $powerShell
        Handle = $powerShell.BeginInvoke()
    }
}

$results = @()

foreach ($job in $jobs) {
    try {
        $results += $job.PowerShell.EndInvoke(
            $job.Handle
        )
    }
    finally {
        $job.PowerShell.Dispose()
    }
}

$overallWatch.Stop()
$pool.Close()
$pool.Dispose()

$created = @(
    $results |
    Where-Object {
        $_.StatusCode -eq 201
    }
).Count

$failed = $Requests - $created

$latencies = @(
    $results |
    Select-Object -ExpandProperty Milliseconds |
    Sort-Object
)

$p95Index = [Math]::Min(
    $latencies.Count - 1,
    [Math]::Ceiling(
        $latencies.Count * 0.95
    ) - 1
)

$p95 = $latencies[$p95Index]

$requestsPerSecond = $Requests / [Math]::Max(
    $overallWatch.Elapsed.TotalSeconds,
    0.001
)

# Give Prometheus time to scrape the completed requests.
Start-Sleep -Seconds 7

function Get-PrometheusValue {
    param([string]$Expression)

    $encoded = [Uri]::EscapeDataString($Expression)

    $response = Invoke-RestMethod `
        -Uri "$PrometheusUrl/api/v1/query?query=$encoded"

    if (@($response.data.result).Count -eq 0) {
        return $null
    }

    return [double]$response.data.result[0].value[1]
}

$prometheusRate = Get-PrometheusValue `
    'sum(rate(http_server_requests_seconds_count{uri="/v1/trades"}[1m]))'

$prometheusP95 = Get-PrometheusValue `
    'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{uri="/v1/trades"}[5m])))'

$summary = [pscustomobject]@{
    Requests = $Requests
    Concurrency = $Concurrency
    Created201 = $created
    Failed = $failed
    RequestsPerSecond = [Math]::Round(
        $requestsPerSecond,
        2
    )
    P95Milliseconds = $p95
    PrometheusRequestRate = if (
        $null -eq $prometheusRate
    ) {
        "no sample"
    }
    else {
        [Math]::Round($prometheusRate, 3)
    }
    PrometheusP95Milliseconds = if (
        $null -eq $prometheusP95
    ) {
        "no sample"
    }
    else {
        [Math]::Round(
            $prometheusP95 * 1000,
            3
        )
    }
}

$resultsFile = Join-Path `
    (Split-Path $PSScriptRoot -Parent) `
    "docs\day6-performance-results.md"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $resultsFile) `
    -Force | Out-Null

@"
# Day 6 performance evidence

| Measurement | Result |
|---|---:|
| Requests | $($summary.Requests) |
| Concurrency | $($summary.Concurrency) |
| HTTP 201 responses | $($summary.Created201) |
| Failed requests | $($summary.Failed) |
| Tool throughput (requests/sec) | $($summary.RequestsPerSecond) |
| Tool P95 latency (ms) | $($summary.P95Milliseconds) |
| Prometheus request rate | $($summary.PrometheusRequestRate) |
| Prometheus P95 latency (ms) | $($summary.PrometheusP95Milliseconds) |
"@ | Set-Content `
    -Path $resultsFile `
    -Encoding UTF8

$summary

if ($failed -gt 0) {
    $results |
        Where-Object {
            $_.StatusCode -ne 201
        } |
        Select-Object -First 5 `
            StatusCode,
            Body |
        Format-List

    throw "$failed request(s) failed."
}