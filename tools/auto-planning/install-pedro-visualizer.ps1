$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$visualizerDir = Join-Path $repoRoot "tools\PedroVisualizer"

if (-not (Test-Path $visualizerDir)) {
    throw "Pedro Visualizer was not found at $visualizerDir"
}

Push-Location $visualizerDir
try {
    npm install
} finally {
    Pop-Location
}
