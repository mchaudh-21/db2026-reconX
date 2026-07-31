param(
    [int]$BackendPort = 8081,
    [int]$PrometheusPort = 9090,
    [int]$GrafanaPort = 3000
)

$ErrorActionPreference = "Stop"

$prometheusVersion = "3.5.5"
$grafanaVersion = "13.1.1"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$localRoot = Join-Path $env:LOCALAPPDATA "ReconX-Day6"
$downloads = Join-Path $localRoot "downloads"
$tools = Join-Path $localRoot "tools"
$runtime = Join-Path $localRoot "runtime"
$logs = Join-Path $runtime "logs"

@(
    $downloads,
    $tools,
    $runtime,
    $logs
) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

function Assert-PortFree(
    [int]$Port,
    [string]$Name
) {
    $listener = Get-NetTCPConnection `
        -LocalPort $Port `
        -State Listen `
        -ErrorAction SilentlyContinue

    if ($listener) {
        throw "$Name cannot start because port $Port is in use."
    }
}

function Wait-Http(
    [string]$Url,
    [string]$Name,
    [int]$TimeoutSeconds
) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    do {
        try {
            $response = Invoke-WebRequest `
                -Uri $Url `
                -UseBasicParsing `
                -TimeoutSec 3

            if ($response.StatusCode -ge 200 -and
                $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)

    throw "$Name did not become ready at $Url."
}

Assert-PortFree $BackendPort "ReconX"
Assert-PortFree $PrometheusPort "Prometheus"
Assert-PortFree $GrafanaPort "Grafana"

$prometheusZip = Join-Path `
    $downloads `
    "prometheus-$prometheusVersion.windows-amd64.zip"

if (-not (Test-Path $prometheusZip)) {
    Write-Host "Downloading portable Prometheus..."
    Invoke-WebRequest `
        -Uri "https://github.com/prometheus/prometheus/releases/download/v$prometheusVersion/prometheus-$prometheusVersion.windows-amd64.zip" `
        -OutFile $prometheusZip `
        -UseBasicParsing
}

$prometheusHome = Join-Path `
    $tools `
    "prometheus-$prometheusVersion"

if (-not (Test-Path $prometheusHome)) {
    $temp = Join-Path $tools "prometheus-temp"
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
    Expand-Archive $prometheusZip $temp -Force

    $exe = Get-ChildItem `
        $temp `
        -Filter prometheus.exe `
        -Recurse |
        Select-Object -First 1

    Move-Item $exe.Directory.FullName $prometheusHome
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
}

$prometheusExe = Get-ChildItem `
    $prometheusHome `
    -Filter prometheus.exe `
    -Recurse |
    Select-Object -First 1

$grafanaZip = Join-Path `
    $downloads `
    "grafana-$grafanaVersion.windows-amd64.zip"

if (-not (Test-Path $grafanaZip)) {
    Write-Host "Downloading portable Grafana..."
    Invoke-WebRequest `
        -Uri "https://dl.grafana.com/oss/release/grafana-$grafanaVersion.windows-amd64.zip" `
        -OutFile $grafanaZip `
        -UseBasicParsing
}

$grafanaHome = Join-Path `
    $tools `
    "grafana-$grafanaVersion"

if (-not (Test-Path $grafanaHome)) {
    $temp = Join-Path $tools "grafana-temp"
    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
    Expand-Archive $grafanaZip $temp -Force

    $server = Get-ChildItem `
        $temp `
        -Filter grafana-server.exe `
        -Recurse |
        Select-Object -First 1

    if ($server) {
        Move-Item $server.Directory.Parent.FullName $grafanaHome
    } else {
        $cli = Get-ChildItem `
            $temp `
            -Filter grafana.exe `
            -Recurse |
            Select-Object -First 1

        if (-not $cli) {
            throw "Grafana executable was not found."
        }

        Move-Item $cli.Directory.Parent.FullName $grafanaHome
    }

    Remove-Item -Recurse -Force $temp -ErrorAction SilentlyContinue
}

$grafanaServer = Get-ChildItem `
    $grafanaHome `
    -Filter grafana-server.exe `
    -Recurse |
    Select-Object -First 1

$grafanaCli = Get-ChildItem `
    $grafanaHome `
    -Filter grafana.exe `
    -Recurse |
    Select-Object -First 1

$prometheusRuntime = Join-Path $runtime "prometheus"
$grafanaRuntime = Join-Path $runtime "grafana"

@(
    $prometheusRuntime,
    (Join-Path $prometheusRuntime "data"),
    $grafanaRuntime,
    (Join-Path $grafanaRuntime "data"),
    (Join-Path $grafanaRuntime "logs"),
    (Join-Path $grafanaRuntime "plugins"),
    (Join-Path $grafanaRuntime "dashboards"),
    (Join-Path $grafanaRuntime "provisioning\datasources"),
    (Join-Path $grafanaRuntime "provisioning\dashboards")
) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

Copy-Item `
    "$repoRoot\monitoring\prometheus\alerts.yml" `
    "$prometheusRuntime\alerts.yml" `
    -Force

@"
global:
  scrape_interval: 5s
  evaluation_interval: 5s

rule_files:
  - "alerts.yml"

scrape_configs:
  - job_name: "recon-service"
    metrics_path: "/api/actuator/prometheus"
    static_configs:
      - targets: ["127.0.0.1:$BackendPort"]

  - job_name: "prometheus"
    static_configs:
      - targets: ["127.0.0.1:$PrometheusPort"]
"@ | Set-Content `
    "$prometheusRuntime\prometheus.yml" `
    -Encoding UTF8

Copy-Item `
    "$repoRoot\monitoring\grafana\provisioning\dashboards\reconx-overview.json" `
    "$grafanaRuntime\dashboards\reconx-overview.json" `
    -Force

@"
apiVersion: 1

datasources:
  - name: Prometheus
    uid: reconx-prometheus
    type: prometheus
    access: proxy
    url: http://127.0.0.1:$PrometheusPort
    isDefault: true
    editable: true
"@ | Set-Content `
    "$grafanaRuntime\provisioning\datasources\prometheus.yml" `
    -Encoding UTF8

$dashboardPath = (
    Join-Path $grafanaRuntime "dashboards"
).Replace("\", "/")

@"
apiVersion: 1

providers:
  - name: "ReconX local"
    orgId: 1
    folder: ""
    type: file
    disableDeletion: false
    updateIntervalSeconds: 5
    allowUiUpdates: true
    options:
      path: "$dashboardPath"
"@ | Set-Content `
    "$grafanaRuntime\provisioning\dashboards\reconx.yml" `
    -Encoding UTF8

$backendRunner = Join-Path $runtime "backend.ps1"
$backendDir = Join-Path $repoRoot "backend"
$backendPathEscaped = $backendDir.Replace("'", "''")

@"
Set-Location '$backendPathEscaped'

`$mavenArgs = @(
    'spring-boot:run'
    '-Djacoco.skip=true'
    '-Dspring-boot.run.profiles=dev,local'
    '-Dspring-boot.run.arguments=--server.port=$BackendPort'
)

& .\mvnw.cmd @mavenArgs
exit `$LASTEXITCODE
"@ | Set-Content $backendRunner -Encoding UTF8

Write-Host "Starting ReconX on port $BackendPort..."
$backend = Start-Process `
    powershell.exe `
    -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "`"$backendRunner`""
    ) `
    -RedirectStandardOutput "$logs\backend.log" `
    -RedirectStandardError "$logs\backend-error.log" `
    -PassThru `
    -WindowStyle Hidden

try {
    Wait-Http `
        "http://127.0.0.1:$BackendPort/api/actuator/prometheus" `
        "ReconX" `
        240
} catch {
    & taskkill.exe /PID $backend.Id /T /F | Out-Null
    Get-Content "$logs\backend.log" -Tail 100
    Get-Content "$logs\backend-error.log" -Tail 100
    throw
}

Write-Host "Starting Prometheus..."

$prometheusRunner = Join-Path $runtime "prometheus.ps1"
$prometheusExeEscaped = $prometheusExe.FullName.Replace("'", "''")
$prometheusConfigPath = Join-Path $prometheusRuntime "prometheus.yml"
$prometheusDataPath = Join-Path $prometheusRuntime "data"
$prometheusConfigEscaped = $prometheusConfigPath.Replace("'", "''")
$prometheusDataEscaped = $prometheusDataPath.Replace("'", "''")

@"
`$prometheusArgs = @(
    '--config.file=$prometheusConfigEscaped'
    '--storage.tsdb.path=$prometheusDataEscaped'
    '--web.listen-address=127.0.0.1:$PrometheusPort'
)

& '$prometheusExeEscaped' @prometheusArgs
exit `$LASTEXITCODE
"@ | Set-Content `
    -Path $prometheusRunner `
    -Encoding UTF8

$prometheus = Start-Process `
    powershell.exe `
    -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "`"$prometheusRunner`""
    ) `
    -WorkingDirectory $prometheusRuntime `
    -RedirectStandardOutput "$logs\prometheus.log" `
    -RedirectStandardError "$logs\prometheus-error.log" `
    -PassThru `
    -WindowStyle Hidden

Wait-Http `
    "http://127.0.0.1:$PrometheusPort/-/ready" `
    "Prometheus" `
    90

$grafanaRunner = Join-Path $runtime "grafana.ps1"

$envLines = @"
`$env:GF_PATHS_PROVISIONING = '$($grafanaRuntime.Replace("'", "''"))\provisioning'
`$env:GF_PATHS_DATA = '$($grafanaRuntime.Replace("'", "''"))\data'
`$env:GF_PATHS_LOGS = '$($grafanaRuntime.Replace("'", "''"))\logs'
`$env:GF_PATHS_PLUGINS = '$($grafanaRuntime.Replace("'", "''"))\plugins'
`$env:GF_SECURITY_ADMIN_USER = 'admin'
`$env:GF_SECURITY_ADMIN_PASSWORD = 'admin'
`$env:GF_USERS_ALLOW_SIGN_UP = 'false'
`$env:GF_SERVER_HTTP_ADDR = '127.0.0.1'
`$env:GF_SERVER_HTTP_PORT = '$GrafanaPort'
"@

if ($grafanaServer) {
    $command = "& '$($grafanaServer.FullName.Replace("'", "''"))' --homepath '$($grafanaHome.Replace("'", "''"))'"
} else {
    $command = "& '$($grafanaCli.FullName.Replace("'", "''"))' server --homepath '$($grafanaHome.Replace("'", "''"))'"
}

($envLines + "`n" + $command) |
    Set-Content $grafanaRunner -Encoding UTF8

Write-Host "Starting Grafana..."
$grafana = Start-Process `
    powershell.exe `
    -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "`"$grafanaRunner`""
    ) `
    -RedirectStandardOutput "$logs\grafana.log" `
    -RedirectStandardError "$logs\grafana-error.log" `
    -PassThru `
    -WindowStyle Hidden

Wait-Http `
    "http://127.0.0.1:$GrafanaPort/api/health" `
    "Grafana" `
    120

@{
    backend = $backend.Id
    prometheus = $prometheus.Id
    grafana = $grafana.Id
} |
    ConvertTo-Json |
    Set-Content "$runtime\pids.json" -Encoding UTF8

Write-Host ""
Write-Host "Day 6 is running locally without Docker."
Write-Host "ReconX:    http://localhost:$BackendPort"
Write-Host "Prometheus: http://localhost:$PrometheusPort"
Write-Host "Grafana:    http://localhost:$GrafanaPort"
Write-Host "Grafana: admin / admin"
