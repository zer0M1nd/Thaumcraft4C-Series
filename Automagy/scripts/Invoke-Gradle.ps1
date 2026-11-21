$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:JAVA_HOME='D:\Program Files\Java\jdk-17.0.5'
$env:PATH="$env:JAVA_HOME\bin;"+$env:PATH
$env:GRADLE_USER_HOME=Join-Path $repo '.tooling/gradle-user-home'
$env:TEMP=Join-Path $repo '.tooling/tmp';$env:TMP=$env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir="'+$env:TEMP+'"'
Push-Location -LiteralPath $repo
try { & ./gradlew.bat --console=plain '-Dnet.minecraftforge.gradle.check.certs=false' @args; $code=$LASTEXITCODE } finally { Pop-Location }
if($code -ne 0){throw "Gradle exited $code"}
