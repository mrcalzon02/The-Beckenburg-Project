# Central, fail-closed Blender bootstrap for Beckenburg Windows acceptance.
# Transient Chocolatey 504s must not be reported as renderer or gameplay defects.
# No fallback to generated/placeholder assets; build.cmd still runs real Blender.
$ErrorActionPreference = 'Stop'

function Find-Blender {
    $cmd = Get-Command -Name blender -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmd -and $cmd.Source -and (Test-Path -LiteralPath $cmd.Source -PathType Leaf)) {
        return $cmd.Source
    }
    $root = Join-Path -Path $env:ProgramFiles -ChildPath 'Blender Foundation'
    # Never recurse through Program Files itself: Windows Defender folders can
    # reject traversal and cause Get-ChildItem to fail the entire GPU pipeline.
    if (-not (Test-Path -LiteralPath $root -PathType Container)) { return $null }
    return Get-ChildItem -LiteralPath $root -Filter blender.exe -Recurse -File -ErrorAction Stop |
        Select-Object -First 1 -ExpandProperty FullName
}

$exe = Find-Blender
if (-not $exe) {
    for ($attempt = 1; $attempt -le 3 -and -not $exe; $attempt++) {
        Write-Host "Blender provision attempt $attempt/3 from the official Chocolatey package feed"
        & choco install blender -y --no-progress
        $exitCode = $LASTEXITCODE
        # Package install can succeed despite an unreliable exit indication;
        # executable presence and --version must still be checked below.
        $exe = Find-Blender
        if ($exe) { break }
        Write-Warning "Blender still missing after attempt $attempt (Chocolatey exit $exitCode)"
        if ($attempt -lt 3) {
            Start-Sleep -Seconds (15 * $attempt)
        }
    }
}
if (-not $exe -or -not (Test-Path -LiteralPath $exe -PathType Leaf)) {
    throw "Blender bootstrap failed after bounded retries; asset export and GPU acceptance remain blocked"
}
& $exe --version
if ($LASTEXITCODE -ne 0) { throw "Blender executable failed validation: $exe" }
$directory = Split-Path -Path $exe -Parent
Add-Content -LiteralPath $env:GITHUB_PATH -Value $directory -Encoding utf8
Write-Host "Validated original Blender exporter at $exe"
