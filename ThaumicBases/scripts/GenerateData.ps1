param([string]$Python='C:/Python314/python.exe')
$ErrorActionPreference='Stop'
$addonRoot=(Resolve-Path "$PSScriptRoot/..").Path
Push-Location $addonRoot
try {
    & $Python scripts/generate_resources.py
    if($LASTEXITCODE -ne 0){throw 'Resource generation failed'}
    if (Test-Path 'src/main/resources/data/thaumicbases/item_aspects/explicit.json') {
        Write-Output 'Preserved complete explicit aspect definitions; historical graph generation is retired.'
        return
    }
    & $Python scripts/prepare_aspect_generation.py
    if($LASTEXITCODE -ne 0){throw 'Aspect graph preparation failed'}
    & "$PSScriptRoot/Build.ps1" generateItemAspects
    if($LASTEXITCODE -ne 0){throw 'Aspect generation failed'}
} finally {Pop-Location}
