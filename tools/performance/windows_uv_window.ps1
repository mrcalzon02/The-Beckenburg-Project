# Measure the same showing Swing workspace, changing only its painter classes.
param([Parameter(Mandatory)][ValidatePattern('^[0-9a-f]{40}$')][string]$Baseline)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path '.').Path
$work = Join-Path $root 'build/uv-window-profile'
$output = Join-Path $root 'build/editor-agent-evidence'
$baseClasses = Join-Path $work 'baseline'
$drivers = Join-Path $work 'drivers'
$classes = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Force $baseClasses, $drivers, $output | Out-Null
$source = 'src/main/java/org/beckenburg/editor/EditorMaterialWorkspace.java'
$original = Join-Path $work 'EditorMaterialWorkspace.java'
git fetch --no-tags --depth=1 origin $Baseline
if ($LASTEXITCODE) { throw 'Cannot fetch baseline' }
git show "${Baseline}:$source" | Set-Content -Encoding utf8NoBOM $original
if ($LASTEXITCODE) { throw 'Cannot read baseline painter' }
javac --release 17 -encoding UTF-8 -cp $classes -d $baseClasses $original
if ($LASTEXITCODE) { throw 'Baseline compile failed' }
$fixture = 'development/quality/evidence/editor-regional-seams-2026-11-16'
$dependency = 'development/quality/evidence/editor-reviewed-mesh-2026-11-16/after/ProfilePath.java'
javac --release 17 -encoding UTF-8 -cp $classes -d $drivers "$fixture/MaterialWindowProfile.java" "$fixture/UvPreviewAllocation.java" $dependency
if ($LASTEXITCODE) { throw 'Window fixture compile failed' }
$file = Join-Path $output 'uv-window-timings.tsv'
"variant`tsample`toperation`tcanvasWidth`tcanvasHeight`tvisibleRows`tobserveValidateMs`tfullPaintMs`tobserveAndFullPaintMs`tuvClipRepaintMs`tlistClipRepaintMs" | Set-Content $file
foreach ($trial in 'baseline-1','candidate-1','candidate-2','baseline-2') {
    $classpath = "$drivers;$classes"
    if ($trial.StartsWith('baseline')) { $classpath = "$baseClasses;$classpath" }
    $rows = @(java -ea -Xms256m -Xmx256m '-Djava.awt.headless=false' -cp $classpath org.beckenburg.editor.MaterialWindowProfile $trial)
    if ($LASTEXITCODE -or $rows.Count -ne 10) { throw "Incomplete showing-window trial $trial" }
    $rows | Add-Content $file
    $rows | Write-Output
}
