$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME='D:\Program Files\Java\jdk-17.0.5'
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.tooling/gradle-user-home'
$taskTemp=Join-Path $projectRoot '.tooling/tmp'
New-Item -ItemType Directory -Force $taskTemp | Out-Null
$env:TEMP=$taskTemp
$env:TMP=$taskTemp
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir="'+$taskTemp+'"'
Push-Location $projectRoot
try { & ./gradlew.bat --console=plain '-Dnet.minecraftforge.gradle.check.certs=false' @args; exit $LASTEXITCODE } finally { Pop-Location }
