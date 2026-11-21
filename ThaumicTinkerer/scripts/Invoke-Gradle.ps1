$ErrorActionPreference = 'Stop'
$addonRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localTemp = Join-Path $addonRoot '.tooling/tmp'
New-Item -ItemType Directory -Force -Path $localTemp | Out-Null
$previous = @{}
$overrides = @{
    JAVA_HOME = 'D:\Program Files\Java\jdk-17.0.5'
    GRADLE_USER_HOME = (Join-Path $addonRoot '.tooling/gradle-user-home')
    TEMP = $localTemp
    TMP = $localTemp
    JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir="' + $localTemp + '"'
}
foreach ($name in $overrides.Keys) { $previous[$name]=[Environment]::GetEnvironmentVariable($name,'Process'); [Environment]::SetEnvironmentVariable($name,$overrides[$name],'Process') }
Push-Location -LiteralPath $addonRoot
try { & ./gradlew.bat @args; if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE" } }
finally { Pop-Location; foreach ($name in $overrides.Keys) { [Environment]::SetEnvironmentVariable($name,$previous[$name],'Process') } }
