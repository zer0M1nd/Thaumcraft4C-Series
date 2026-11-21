$ErrorActionPreference='Stop'
$modRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:JAVA_HOME='D:\Program Files\Java\jdk-17.0.5'
$env:PATH=(Join-Path $env:JAVA_HOME 'bin')+[IO.Path]::PathSeparator+$env:PATH
$env:GRADLE_USER_HOME=Join-Path $modRoot '.tooling/gradle-user-home'
$env:TEMP=Join-Path $modRoot '.tooling/tmp'
$env:TMP=$env:TEMP
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir="'+$env:TEMP+'"'
Push-Location -LiteralPath $modRoot
try { & ./gradlew.bat @args; if($LASTEXITCODE -ne 0){throw "Gradle exited $LASTEXITCODE"} } finally { Pop-Location }
