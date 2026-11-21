param([string]$LegacyDirectory = 'E:\MC\Minecraft 1.7.10 Thaumcraft Only\.minecraft')
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$baselineRoot = Join-Path $projectRoot '.tooling/legacy-server'
$forgeRelative = 'net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10.jar'
$forgeSource = Join-Path $LegacyDirectory ('libraries/' + $forgeRelative)
if ((Get-FileHash -LiteralPath $forgeSource -Algorithm SHA1).Hash -ne '25fd97f72beca728112256938e03e8105b1b78cc') { throw 'Legacy Forge baseline hash mismatch.' }
New-Item -ItemType Directory -Path $baselineRoot -Force | Out-Null
Copy-Item -LiteralPath $forgeSource -Destination (Join-Path $baselineRoot 'forge.jar')

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($forgeSource)
try {
    $reader = [IO.StreamReader]::new($archive.GetEntry('META-INF/MANIFEST.MF').Open())
    try { $manifest = $reader.ReadToEnd() -replace '\r?\n ', '' } finally { $reader.Dispose() }
} finally { $archive.Dispose() }
$classPath = [regex]::Match($manifest, '(?m)^Class-Path: (.+)$').Groups[1].Value.Trim()
foreach ($relative in $classPath.Split(' ')) {
    if (-not $relative.StartsWith('libraries/')) { continue }
    $source = Join-Path $LegacyDirectory $relative
    $target = Join-Path $baselineRoot $relative
    New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($target)) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target
}
$serverJar = Join-Path $baselineRoot 'minecraft_server.1.7.10.jar'
if (-not (Test-Path -LiteralPath $serverJar)) {
    Invoke-WebRequest -Uri 'https://launcher.mojang.com/v1/objects/952438ac4e01b4d115c5fc38f891710c4941df29/server.jar' -OutFile $serverJar
}
if ((Get-FileHash -LiteralPath $serverJar -Algorithm SHA1).Hash -ne '952438ac4e01b4d115c5fc38f891710c4941df29') { throw 'Official legacy server hash mismatch.' }
$mods = Join-Path $baselineRoot 'mods'
$versionMods = Join-Path $mods '1.7.10'
New-Item -ItemType Directory -Path $versionMods -Force | Out-Null
$tcSource = Join-Path $LegacyDirectory 'mods/Thaumcraft-1.7.10-4.2.3.5.jar'
if ((Get-FileHash -LiteralPath $tcSource -Algorithm SHA256).Hash -ne '3DBA9786966974701578A658D1BB369BF35BDF5F363079F5AC9C4910A39113BE') { throw 'Legacy Thaumcraft baseline hash mismatch.' }
Copy-Item -LiteralPath $tcSource -Destination $mods
Copy-Item -LiteralPath (Join-Path $LegacyDirectory 'mods/1.7.10/Baubles-1.7.10-1.0.1.10.jar') -Destination $versionMods

function Write-BaselineFile([string]$name, [string]$text) {
    $path = Join-Path $baselineRoot $name
    if (-not (Test-Path -LiteralPath $path)) { [IO.File]::WriteAllText($path, $text, [Text.UTF8Encoding]::new($false)) }
}
Write-BaselineFile 'eula.txt' "eula=true`n"
Write-BaselineFile 'server.properties' @'
server-ip=127.0.0.1
server-port=25566
online-mode=false
level-name=baseline
level-type=FLAT
gamemode=1
difficulty=1
max-players=1
view-distance=3
spawn-protection=0
enable-rcon=false
enable-query=false
motd=TC4 isolated legacy baseline
'@
Write-Output 'Isolated legacy server prepared with verified Forge/MC/TC4 artifacts; original client and references were only read.'
