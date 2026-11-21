param([switch]$PrepareOnly)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
# Each invocation owns its output. Never reuse or clean output belonging to a live client.
$sessionName = (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$clientBuild = Join-Path $projectRoot ('.tooling/client-build/' + $sessionName)
$clientCache = Join-Path $clientBuild 'gradle-project-cache'
$taskName = if ($PrepareOnly) { 'prepareRunClient' } else { 'runClient' }
Write-Host "Client build output: $clientBuild"
& (Join-Path $PSScriptRoot 'Invoke-Gradle.ps1') --offline --console=plain --project-cache-dir $clientCache '-Dnet.minecraftforge.gradle.check.certs=false' "-Ptc4BuildDirectory=$clientBuild" classes $taskName
