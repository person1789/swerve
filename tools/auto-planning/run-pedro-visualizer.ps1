$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$visualizerDir = Join-Path $repoRoot "tools\PedroVisualizer"

if (-not (Test-Path (Join-Path $visualizerDir "node_modules"))) {
    Write-Host "Pedro Visualizer dependencies are missing."
    Write-Host "Run: .\tools\auto-planning\install-pedro-visualizer.ps1"
    exit 1
}

Push-Location $visualizerDir
try {
    npm run dev -- --host 127.0.0.1 --port 4174
} finally {
    Pop-Location
}
