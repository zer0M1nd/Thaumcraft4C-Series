param([switch]$ReportOnly)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$reviewRoot = Join-Path $projectRoot 'docs\legacy-review'
$sourceRoot = Join-Path $projectRoot 'references\deobfuscated_src'
$sourcePaths = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
foreach ($file in Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java') {
    [void]$sourcePaths.Add([IO.Path]::GetRelativePath($projectRoot, $file.FullName).Replace('\', '/'))
}
$reviewed = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$invalid = [Collections.Generic.List[string]]::new()
$ledgerCounts = [ordered]@{}
foreach ($reviewer in @('root', 'core', 'content', 'client')) {
    $ledgerPath = Join-Path $reviewRoot ($reviewer + '-files.txt')
    $ledgerEntries = if (Test-Path -LiteralPath $ledgerPath) { @(Get-Content -LiteralPath $ledgerPath) } else { @() }
    $uniqueEntries = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($line in $ledgerEntries) {
        $entry = $line.Trim().Replace('\', '/')
        if (-not $entry) { continue }
        if (-not $sourcePaths.Contains($entry)) {
            $invalid.Add($reviewer + ': ' + $entry)
            continue
        }
        [void]$uniqueEntries.Add($entry)
        [void]$reviewed.Add($entry)
    }
    $ledgerCounts[$reviewer] = $uniqueEntries.Count
}
$missing = @($sourcePaths | Where-Object { -not $reviewed.Contains($_) } | Sort-Object)
$report = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    note = 'Ledger coverage only. A listed path must be backed by actual complete reading and review notes; this script does not prove comprehension or implementation.'
    totalJavaFiles = $sourcePaths.Count
    reviewedJavaFiles = $reviewed.Count
    remainingJavaFiles = $missing.Count
    ledgerCounts = $ledgerCounts
    invalidEntries = @($invalid)
    remainingPaths = $missing
}
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $reviewRoot 'coverage.json') -Encoding utf8
Write-Output ("Review ledger: {0}/{1}; remaining {2}." -f $reviewed.Count, $sourcePaths.Count, $missing.Count)
foreach ($reviewer in $ledgerCounts.Keys) { Write-Output ("  {0}: {1}" -f $reviewer, $ledgerCounts[$reviewer]) }
if ($invalid.Count -gt 0) { throw ('Invalid ledger entries: ' + ($invalid -join ', ')) }
if ($missing.Count -gt 0 -and -not $ReportOnly) {
    throw 'Full legacy review is not complete. Read the remaining sources and verify the notes before advancing to environment deployment.'
}
if ($missing.Count -eq 0) { Write-Output 'All Java paths covered; review-note audit remains required before declaring the review complete.' }
