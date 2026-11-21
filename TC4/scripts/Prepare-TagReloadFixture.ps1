param([ValidateSet('iron', 'diamond')][string]$Material = 'iron')
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$fixtureName = if ($Material -eq 'iron') { 'tc4_tag_reload_fixture' } else { 'tc4_diamond_tag_reload_fixture' }
$fixtureRoot = Join-Path $projectRoot "run-server/validation/datapacks/$fixtureName"
$tagPath = if ($Material -eq 'iron') { 'data/forge/tags/items/nuggets' } else { 'data/forge/tags/items/gems' }
$tagDirectory = Join-Path $fixtureRoot $tagPath
New-Item -ItemType Directory -Path $tagDirectory -Force | Out-Null

# Generated test-only data; never placed in either mod resource source directory.
$pack = @{pack = @{pack_format = 15; description = "TC4 validation: temporarily empty $Material material tag"}}
$tag = @{replace = $true; values = @()}
[IO.File]::WriteAllText((Join-Path $fixtureRoot 'pack.mcmeta'), ($pack | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $tagDirectory "$Material.json"), ($tag | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
Write-Output 'Tag reload fixture generated. Check enabled packs before baseline tests: first server discovery may select a new pack. Disable after its recorded negative test.'
