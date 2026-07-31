$ErrorActionPreference = "SilentlyContinue"

$pidsFile = Join-Path `
    $env:LOCALAPPDATA `
    "ReconX-Day6\runtime\pids.json"

if (-not (Test-Path $pidsFile)) {
    Write-Host "The Day 6 local stack is not running."
    exit 0
}

$pids = Get-Content $pidsFile -Raw | ConvertFrom-Json

@(
    $pids.grafana,
    $pids.prometheus,
    $pids.backend
) | ForEach-Object {
    if ($_ -and (Get-Process -Id $_ -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $_ /T /F | Out-Null
    }
}

Remove-Item $pidsFile -Force
Write-Host "ReconX, Prometheus, and Grafana were stopped."
