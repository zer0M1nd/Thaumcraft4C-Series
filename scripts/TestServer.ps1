$ErrorActionPreference='Stop'
$addonRoot=(Resolve-Path "$PSScriptRoot/..").Path
$started=[DateTime]::UtcNow
& (Get-Process -Id $PID).Path -NoProfile -File "$PSScriptRoot/Build.ps1" -Pacceptance @args runServer
if($LASTEXITCODE -ne 0){throw 'Dedicated server launch failed'}
$reportPath=Join-Path $addonRoot '.tooling/server-acceptance.json'
if(!(Test-Path -LiteralPath $reportPath) -or (Get-Item -LiteralPath $reportPath).LastWriteTimeUtc -lt $started){throw 'Server did not produce a fresh acceptance report'}
$results=Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
$failures=@($results.PSObject.Properties | Where-Object Value -ne 'passed')
if(@($results.PSObject.Properties).Count -eq 0 -or $failures.Count -gt 0){throw ('Server checks failed: '+($failures.Name -join ', '))}
Write-Output 'Dedicated server acceptance passed'
