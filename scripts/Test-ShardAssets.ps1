param([string]$Jar = (Join-Path $PSScriptRoot '..\build\libs\TC4Revival-0.1.0-dev.jar'))
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Jar))
function Read-Entry([string]$path) {
    $entry = $archive.GetEntry($path)
    if ($null -eq $entry) { throw "Missing JAR entry: $path" }
    $stream = $entry.Open()
    $buffer = [IO.MemoryStream]::new()
    try { $stream.CopyTo($buffer); return ,$buffer.ToArray() }
    finally { $stream.Dispose(); $buffer.Dispose() }
}
try {
    foreach ($name in @('shard', 'shard_balanced')) {
        $old = [IO.File]::ReadAllBytes((Join-Path $projectRoot "references\resources\assets\thaumcraft\textures\items\$name.png"))
        $packed = Read-Entry "assets/thaumcraft/textures/item/$name.png"
        if ([Convert]::ToBase64String($old) -cne [Convert]::ToBase64String($packed)) { throw "Legacy texture changed: $name" }
    }
    $animation = [Text.Encoding]::UTF8.GetString((Read-Entry 'assets/thaumcraft/textures/item/shard_balanced.png.mcmeta')) | ConvertFrom-Json
    if ($null -eq $animation.animation -or @($animation.animation.PSObject.Properties).Count -ne 0) { throw 'Balanced shard default animation changed' }
    foreach ($element in @('air', 'fire', 'water', 'earth', 'order', 'entropy', 'balanced')) {
        $model = [Text.Encoding]::UTF8.GetString((Read-Entry "assets/thaumcraft/models/item/${element}_shard.json")) | ConvertFrom-Json
        $texture = if ($element -eq 'balanced') { 'shard_balanced' } else { 'shard' }
        if ($model.parent -cne 'minecraft:item/generated' -or $model.textures.layer0 -cne "thaumcraft:item/$texture") { throw "Wrong shard model: $element" }
    }
    if (@($archive.Entries | Where-Object { $_.FullName -like '*LegacyBaselineProbe*' }).Count -ne 0) { throw 'Legacy observer included in mod JAR' }
    Write-Output 'PASS: exact legacy shard PNGs, default balanced animation, seven models and observer exclusion in built JAR.'
} finally { $archive.Dispose() }
