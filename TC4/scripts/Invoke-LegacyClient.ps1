param([switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
$tcProject = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$tcOriginal = 'E:\MC\Minecraft 1.7.10 Thaumcraft Only\.minecraft'
$tcClient = Join-Path $tcProject '.tooling/legacy-client'
$tcJava = 'D:\Program Files\Java\jdk-1.8_421\bin\java.exe'
$tcVersion = '1.7.10-Forge10.13.4.1614-1.7.10'
$tcBase = Get-Content -LiteralPath (Join-Path $tcOriginal 'versions/1.7.10/1.7.10.json') -Raw | ConvertFrom-Json
$tcForge = Get-Content -LiteralPath (Join-Path $tcOriginal "versions/$tcVersion/$tcVersion.json") -Raw | ConvertFrom-Json
New-Item -ItemType Directory -Path $tcClient,(Join-Path $tcClient 'config') -Force | Out-Null
if (-not (Test-Path -LiteralPath (Join-Path $tcClient 'mods'))) {
    Copy-Item -LiteralPath (Join-Path $tcOriginal 'mods') -Destination (Join-Path $tcClient 'mods') -Recurse
}
foreach ($tcConfig in @('Thaumcraft.cfg', 'baubles.cfg')) {
    $tcSource = Join-Path $tcOriginal "config/$tcConfig"
    $tcDestination = Join-Path $tcClient "config/$tcConfig"
    if ((Test-Path -LiteralPath $tcSource) -and -not (Test-Path -LiteralPath $tcDestination)) { Copy-Item -LiteralPath $tcSource -Destination $tcDestination }
}
$tcOptionsPath = Join-Path $tcClient 'options.txt'
if (-not (Test-Path -LiteralPath $tcOptionsPath)) { Copy-Item -LiteralPath (Join-Path $tcOriginal 'options.txt') -Destination $tcOptionsPath }
$tcOptions = Get-Content -LiteralPath $tcOptionsPath
foreach ($tcSetting in @('soundCategory_master:0.0', 'renderDistance:6', 'fullscreen:false', 'pauseOnLostFocus:false')) {
    $tcKey = ($tcSetting -split ':')[0]
    $tcOptions = @($tcOptions | Where-Object { -not $_.StartsWith($tcKey + ':') }) + $tcSetting
}
[IO.File]::WriteAllLines($tcOptionsPath, $tcOptions, [Text.UTF8Encoding]::new($false))

# The original installation supplies unchanged binaries/assets. Tests use separate settings and saves.
$tcLibraries = [ordered]@{}
foreach ($tcLibrary in @($tcBase.libraries) + @($tcForge.libraries)) {
    $tcParts = $tcLibrary.name -split ':'
    $tcLibraries[$tcParts[0] + ':' + $tcParts[1]] = $tcLibrary
}
$tcClassPath = [Collections.Generic.List[string]]::new()
foreach ($tcLibrary in $tcLibraries.Values) {
    $tcAllowed = -not [bool]$tcLibrary.rules
    foreach ($tcRule in $tcLibrary.rules) {
        $tcMatches = -not $tcRule.os -or (-not $tcRule.os.name -or $tcRule.os.name -eq 'windows')
        if ($tcMatches -and $tcRule.os.arch) { $tcMatches = $tcRule.os.arch -eq 'amd64' }
        if ($tcMatches) { $tcAllowed = $tcRule.action -eq 'allow' }
    }
    if (-not $tcAllowed) { continue }
    $tcParts = $tcLibrary.name -split ':'
    if ($tcLibrary.natives -and -not $tcLibrary.downloads.artifact) { continue }
    $tcRelative = $tcLibrary.downloads.artifact.path
    if (-not $tcRelative) { $tcRelative = ($tcParts[0] -replace '\.', '/') + '/' + $tcParts[1] + '/' + $tcParts[2] + '/' + $tcParts[1] + '-' + $tcParts[2] + '.jar' }
    $tcJar = Join-Path $tcOriginal "libraries/$tcRelative"
    if (-not (Test-Path -LiteralPath $tcJar)) { throw "Missing original client library: $tcRelative" }
    $tcClassPath.Add($tcJar)
}
$tcClassPath.Add((Join-Path $tcOriginal 'versions/1.7.10/1.7.10.jar'))
$tcNativeDirectory = Join-Path $tcOriginal '$natives'
if (-not (Test-Path -LiteralPath (Join-Path $tcNativeDirectory 'lwjgl64.dll'))) { throw 'Original client Windows natives are missing.' }
if ($PrepareOnly) { Write-Output "Prepared isolated legacy client with $($tcClassPath.Count) original libraries; audio muted."; return }
$tcHash = [Security.Cryptography.MD5]::HashData([Text.Encoding]::UTF8.GetBytes('OfflinePlayer:TC4Tester'))
$tcHash[6] = ($tcHash[6] -band 15) -bor 48
$tcHash[8] = ($tcHash[8] -band 63) -bor 128
$tcUuid = [Convert]::ToHexString($tcHash).ToLowerInvariant()
$tcArguments = @('-Xms256M', '-Xmx2G', "-Djava.library.path=$tcNativeDirectory", '-cp', ($tcClassPath -join ';'),
    $tcForge.mainClass, '--username', 'TC4Tester', '--version', $tcVersion, '--gameDir', $tcClient,
    '--assetsDir', (Join-Path $tcOriginal 'assets'), '--assetIndex', '1.7.10', '--uuid', $tcUuid,
    '--accessToken', '0', '--userProperties', '{}', '--userType', 'legacy', '--tweakClass', 'cpw.mods.fml.common.launcher.FMLTweaker',
    '--width', '1280', '--height', '720')
Push-Location $tcClient
try { & $tcJava @tcArguments; if ($LASTEXITCODE -ne 0) { throw "Legacy client exited with code $LASTEXITCODE" } }
finally { Pop-Location }
