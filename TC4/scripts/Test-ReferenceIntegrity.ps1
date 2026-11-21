param(
    [string]$Manifest = (Join-Path $PSScriptRoot '..\docs\legacy-review\reference-sha256.csv')
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$referenceRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'references'))
$manifestRows = @(Import-Csv -LiteralPath $Manifest)
$expected = @{}
$problems = [Collections.Generic.List[string]]::new()
foreach ($entry in $manifestRows) {
    $absolute = [IO.Path]::GetFullPath((Join-Path $projectRoot $entry.Path))
    if (-not $absolute.StartsWith($referenceRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Manifest path outside references: $($entry.Path)"
    }
    $expected[$absolute] = $true
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) {
        $problems.Add("Missing: $($entry.Path)")
    } elseif ((Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash -ne $entry.SHA256) {
        $problems.Add("Modified: $($entry.Path)")
    }
}
foreach ($entry in Get-ChildItem -LiteralPath $referenceRoot -Recurse -File) {
    if (-not $expected.ContainsKey($entry.FullName)) {
        $problems.Add("Unexpected file: $($entry.FullName)")
    }
}
if ($problems.Count -gt 0) {
    throw ($problems -join [Environment]::NewLine)
}
Write-Output "PASS: $($manifestRows.Count) reference files unchanged (SHA-256)."
