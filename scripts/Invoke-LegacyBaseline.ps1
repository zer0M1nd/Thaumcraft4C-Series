param([switch]$BuildOnly, [string]$ServerDirectory, [switch]$CopperFixture, [switch]$ExcavationMaterialsOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$baselineRoot = if ($ServerDirectory) { [IO.Path]::GetFullPath($ServerDirectory) } else { Join-Path $projectRoot '.tooling/legacy-server' }
if ($CopperFixture -and (-not $ServerDirectory -or $baselineRoot -eq (Join-Path $projectRoot '.tooling/legacy-server'))) {
    throw 'CopperFixture requires a separate explicit ServerDirectory; the ordinary legacy baseline must remain unchanged.'
}
$legacyJdk = 'D:\Program Files\Java\jdk-1.8_421'
$classes = Join-Path $baselineRoot 'probe-classes'
$probe = Join-Path $projectRoot 'tools/legacy-baseline/src/LegacyBaselineProbe.java'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
Push-Location $baselineRoot
try {
    & (Join-Path $legacyJdk 'bin/javac.exe') -encoding UTF-8 -cp 'forge.jar;minecraft_server.1.7.10.jar' -d $classes $probe
    if ($LASTEXITCODE -ne 0) { throw 'Legacy observer compilation failed.' }
    & (Join-Path $legacyJdk 'bin/jar.exe') cf 'mods/tc4-baseline-probe.jar' -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Legacy observer packaging failed.' }
    if (-not $BuildOnly) {
        $probeJvmOptions = @('-Djava.awt.headless=true')
        if ($ExcavationMaterialsOnly) { $probeJvmOptions += '-Dtc4baseline.excavationMaterialsOnly=true' }
        if ($CopperFixture) { $probeJvmOptions += '-Dtc4baseline.copperFixture=true' }
        & (Join-Path $legacyJdk 'bin/java.exe') @probeJvmOptions -Xms256M -Xmx1536M -jar forge.jar nogui
        if ($LASTEXITCODE -ne 0) { throw 'Legacy baseline server failed.' }
    }
} finally { Pop-Location }
