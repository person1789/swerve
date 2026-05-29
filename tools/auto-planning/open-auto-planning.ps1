$ErrorActionPreference = "Stop"

$hub = Join-Path $PSScriptRoot "index.html"
$runner = Join-Path $PSScriptRoot "run-pedro-visualizer.ps1"
$visualizerDir = Resolve-Path (Join-Path $PSScriptRoot "..\PedroVisualizer")

Start-Process -FilePath $hub

if (Test-Path (Join-Path $visualizerDir "node_modules")) {
    Start-Process -WindowStyle Hidden -FilePath "powershell.exe" -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $runner
    )
    Start-Sleep -Seconds 2
    Start-Process "http://127.0.0.1:4174"
} else {
    Write-Host "Opened the Auto Planning Hub."
    Write-Host "Pedro Visualizer dependencies are not installed yet."
    Write-Host "Run: .\tools\auto-planning\install-pedro-visualizer.ps1"
}
