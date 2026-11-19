$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Test-TaskJdk17([string]$candidate) {
    if (-not $candidate) { return $false }
    $releaseFile = Join-Path $candidate 'release'
    return (Test-Path -LiteralPath (Join-Path $candidate 'bin/java.exe')) -and
        (Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -and
        (Test-Path -LiteralPath $releaseFile) -and
        [bool](Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="17(?:\.|"|\+)')
}

$candidates = @($env:TC4_JAVA_HOME, $env:JAVA_HOME, (Join-Path $projectRoot '.tooling/jdk-17'), 'D:\Program Files\Java\jdk-17.0.5')
if ($env:TC4_JAVA_HOME -and -not (Test-TaskJdk17 $env:TC4_JAVA_HOME)) {
    throw 'TC4_JAVA_HOME must point to a Java 17 JDK with java and javac.'
}
$selectedJdk = $candidates | Where-Object { Test-TaskJdk17 $_ } | Select-Object -First 1
if (-not $selectedJdk) { throw 'Java 17 JDK not found. Set TC4_JAVA_HOME to its directory for this terminal.' }

$localTooling = Join-Path $projectRoot '.tooling'
$localTemp = Join-Path $localTooling 'tmp'
New-Item -ItemType Directory -Path $localTemp -Force | Out-Null
$overrides = @{
    JAVA_HOME = $selectedJdk
    PATH = (Join-Path $selectedJdk 'bin') + [IO.Path]::PathSeparator + $env:PATH
    GRADLE_USER_HOME = [IO.Path]::GetFullPath((Join-Path $projectRoot '../../TC4/.tooling/gradle-user-home'))
    TEMP = $localTemp
    TMP = $localTemp
    JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' -Djava.io.tmpdir="' + $localTemp + '"').Trim())
}
$previousEnvironment = @{}
foreach ($key in $overrides.Keys) {
    $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
    [Environment]::SetEnvironmentVariable($key, $overrides[$key], 'Process')
}
Push-Location -LiteralPath $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @args
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
    foreach ($key in $previousEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process')
    }
}
if ($gradleExitCode -ne 0) { throw "Gradle exited with code $gradleExitCode." }
