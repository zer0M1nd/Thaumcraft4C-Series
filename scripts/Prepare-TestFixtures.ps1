$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtureSource = Join-Path $projectRoot 'verification/2026-10-03/fixtures/wg_test_metals'
$fixtureTarget = Join-Path $projectRoot 'run-server/world/datapacks/wg_test_metals'
New-Item -ItemType Directory -Path $fixtureTarget -Force | Out-Null
Copy-Item -Path (Join-Path $fixtureSource '*') -Destination $fixtureTarget -Recurse -Force
Write-Output 'Prepared WG optional-metal fixtures for the local test world. Restart or /reload before runall.'
