$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$serverDirectory = Join-Path $projectRoot 'run-server'
$clientDirectory = Join-Path $projectRoot 'run-client'
New-Item -ItemType Directory -Path $serverDirectory, $clientDirectory -Force | Out-Null

function Write-NewValidationFile([string]$path, [string]$content) {
    if (-not (Test-Path -LiteralPath $path)) {
        [IO.File]::WriteAllText($path, $content, [Text.UTF8Encoding]::new($false))
    }
}

$serverProperties = @'
server-ip=127.0.0.1
server-port=25565
online-mode=false
level-name=validation
level-type=minecraft:flat
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","structures":{"structures":{}}}
gamemode=creative
difficulty=normal
spawn-protection=0
max-players=2
view-distance=6
simulation-distance=4
motd=TC4Revival local validation
enable-rcon=false
enable-query=false
'@
Write-NewValidationFile (Join-Path $serverDirectory 'server.properties') ($serverProperties + "`n")
Write-NewValidationFile (Join-Path $serverDirectory 'eula.txt') "eula=true`n"
Write-NewValidationFile (Join-Path $clientDirectory 'options.txt') "lang:zh_cn`nguiScale:2`nfullscreen:false`npauseOnLostFocus:false`n"

$propertiesPath = Join-Path $serverDirectory 'server.properties'
if (-not (Select-String -LiteralPath $propertiesPath -Pattern '^server-ip=127\.0\.0\.1$')) {
    throw 'This validation fixture requires its server to bind to 127.0.0.1.'
}
Write-Output 'Local validation directories prepared; existing files and worlds were preserved.'
