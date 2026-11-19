$ErrorActionPreference = 'Stop'
$addonRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
& (Join-Path $addonRoot '../../TC4/scripts/Invoke-Gradle.ps1') -p $addonRoot @args
