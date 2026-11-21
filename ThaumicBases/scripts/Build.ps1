$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path "$PSScriptRoot/..").Path
$env:JAVA_HOME='D:\Program Files\Java\jdk-17.0.5'
$env:GRADLE_USER_HOME="$projectRoot/.tooling/gradle-user-home"
$env:JAVA_TOOL_OPTIONS='-Djava.io.tmpdir="'+$projectRoot+'/.tooling/tmp"'
Push-Location $projectRoot
[string[]]$connectivityArguments=@('--offline')
if($env:TBMODERN_ONLINE -eq '1'){$connectivityArguments=@()}
try { & "$projectRoot/.tooling/gradle-8.8/bin/gradle.bat" @connectivityArguments --console=plain '-Dnet.minecraftforge.gradle.check.certs=false' @args; exit $LASTEXITCODE } finally { Pop-Location }
