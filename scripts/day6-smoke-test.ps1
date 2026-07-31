param(
    [string]$BaseUrl = "http://localhost:8081/api",
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$GrafanaUrl = "http://localhost:3000"
)

$ErrorActionPreference = "Stop"

function Login(
    [string]$Email,
    [string]$Password
) {
    return Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/auth/login" `
        -ContentType "application/json" `
        -Body (
            @{
                email = $Email
                password = $Password
            } | ConvertTo-Json
        )
}

$trader = Login "trader@db.com" "trader123"
$traderHeaders = @{
    Authorization = "Bearer $($trader.token)"
}

Invoke-RestMethod `
    "$BaseUrl/v1/instruments/SAP.DE" `
    -Headers $traderHeaders | Out-Null

Invoke-RestMethod `
    "$BaseUrl/v1/instruments/SAP.DE" `
    -Headers $traderHeaders | Out-Null

$lei = "W22LROWP2IHZNBB6K528"

Invoke-RestMethod `
    "$BaseUrl/v1/counterparties/$lei" `
    -Headers $traderHeaders | Out-Null

Invoke-RestMethod `
    "$BaseUrl/v1/counterparties/$lei" `
    -Headers $traderHeaders | Out-Null

$caches = Invoke-RestMethod `
    "$BaseUrl/actuator/caches" `
    -Headers $traderHeaders

$cacheNames = @(
    $caches.cacheManagers.PSObject.Properties |
    ForEach-Object {
        $_.Value.caches.PSObject.Properties.Name
    }
)

foreach ($name in @("instruments", "counterparties")) {
    if ($cacheNames -notcontains $name) {
        throw "Missing cache: $name"
    }
}

$recon = Login "recon@db.com" "recon123"
$reconHeaders = @{
    Authorization = "Bearer $($recon.token)"
}

$reconBody = @{
    from = "2026-07-01"
    to = "2026-07-31"
    counterpartyId = 1
} | ConvertTo-Json

1..5 | ForEach-Object {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/v1/recon/run" `
        -Headers $reconHeaders `
        -ContentType "application/json" `
        -Body $reconBody | Out-Null
}

# Generate one successful trade for the business metrics.
$tradeCreated = $false
$tradeDate = Get-Date -Format "yyyy-MM-dd"
$tradeRefDate = Get-Date -Format "yyyyMMdd"

for (
    $attempt = 1;
    $attempt -le 20 -and -not $tradeCreated;
    $attempt++
) {
    $suffix = "{0:D4}" -f (
        Get-Random -Minimum 0 -Maximum 10000
    )

    $tradeBody = @{
        tradeRef = "SMK-$tradeRefDate-$suffix"
        instrumentId = 1
        counterpartyId = 1
        assetClass = "EQUITY"
        side = "BUY"
        quantity = 100
        price = 245.50
        tradeDate = $tradeDate
    } | ConvertTo-Json

    try {
        Invoke-RestMethod `
            -Method Post `
            -Uri "$BaseUrl/v1/trades" `
            -Headers $traderHeaders `
            -ContentType "application/json" `
            -Body $tradeBody | Out-Null

        $tradeCreated = $true
    }
    catch {
        $status = $null

        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }

        if ($status -ne 409) {
            throw
        }
    }
}

if (-not $tradeCreated) {
    throw "Could not create a unique smoke-test trade."
}

Start-Sleep -Seconds 1
$metrics = (
    Invoke-WebRequest `
        "$BaseUrl/actuator/prometheus" `
        -UseBasicParsing
).Content

foreach ($metric in @(
    "cache_gets_total",
    "trade_creation_total",
    "trade_value_count",
    "reconciliation_duration_seconds_count",
    "reconciliation_duration_seconds_bucket",
    "recon_break_count",
    "trades_by_status"
)) {
    if ($metrics -notmatch [regex]::Escape($metric)) {
        throw "Missing metric: $metric"
    }
}

$rules = Invoke-RestMethod `
    "$PrometheusUrl/api/v1/rules"

$ruleNames = @(
    $rules.data.groups.rules |
    ForEach-Object { $_.name }
)

foreach ($rule in @(
    "TooManyReconBreaks",
    "HighApiLatencyP95"
)) {
    if ($ruleNames -notcontains $rule) {
        throw "Missing Prometheus rule: $rule"
    }
}

$grafana = Invoke-RestMethod "$GrafanaUrl/api/health"
if ($grafana.database -ne "ok") {
    throw "Grafana health failed."
}

[pscustomobject]@{
    Backend = "UP"
    JwtLogin = "PASS"
    InstrumentCache = "PASS"
    CounterpartyCache = "PASS"
    CustomMetrics = "PASS"
    ReconciliationHistogram = "PASS"
    PrometheusAlerts = "PASS"
    Grafana = "PASS"
}
