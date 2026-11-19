$ErrorActionPreference='Stop'
$horizonsRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:JAVA_HOME='D:\Program Files\Java\jdk-17.0.5'
$env:PATH=(Join-Path $env:JAVA_HOME 'bin')+[IO.Path]::PathSeparator+$env:PATH
$env:GRADLE_USER_HOME=Join-Path $horizonsRoot '.tooling/gradle-user-home'
$env:TEMP=Join-Path $horizonsRoot '.tooling/tmp'
$env:TMP=$env:TEMP
New-Item -ItemType Directory -Path $env:TEMP -Force | Out-Null
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir="'+$env:TEMP+'"'
Push-Location -LiteralPath $horizonsRoot
try { & ./gradlew.bat @args; if($LASTEXITCODE -ne 0){throw "Gradle failed: $LASTEXITCODE"} } finally { Pop-Location }
