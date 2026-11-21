param([Parameter(Mandatory)][ValidateSet('Enable', 'Disable')][string]$Mode)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtureRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'run-server/validation/datapacks/tc4-clue-tag-fixture'))
if (-not $fixtureRoot.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Fixture is outside the intended project.'
}
$tagDirectory = Join-Path $fixtureRoot 'data/forge/tags/items'
$tagFile = Join-Path $tagDirectory 'quicksilver.json'
$packFile = Join-Path $fixtureRoot 'pack.mcmeta'
if ($Mode -eq 'Enable') {
    if (Test-Path -LiteralPath $fixtureRoot) { throw 'Fixture already exists; inspect it before enabling again.' }
    New-Item -ItemType Directory -Path $tagDirectory -Force | Out-Null
    $metadata = @{ pack = @{ pack_format = 15; description = 'TC4Revival isolated research clue tag test' } }
    $tag = @{ replace = $false; values = @('minecraft:iron_nugget') }
    [IO.File]::WriteAllText($packFile, ($metadata | ConvertTo-Json -Depth 4))
    [IO.File]::WriteAllText($tagFile, ($tag | ConvertTo-Json -Depth 4))
} else {
    # Remove only the two generated fixture files and now-empty directories. Never recursively delete.
    foreach ($file in @($tagFile, $packFile)) {
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
    }
    foreach ($relative in @('data/forge/tags/items', 'data/forge/tags', 'data/forge', 'data', '')) {
        $directory = if ($relative) { Join-Path $fixtureRoot $relative } else { $fixtureRoot }
        if (Test-Path -LiteralPath $directory) {
            if (Get-ChildItem -LiteralPath $directory -Force) { throw "Unexpected fixture contents retained in $directory" }
            Remove-Item -LiteralPath $directory
        }
    }
}
Write-Output "Research clue tag fixture $Mode complete. Reload the isolated server to apply."
