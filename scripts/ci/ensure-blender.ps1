# Central, fail-closed Blender bootstrap for Beckenburg Windows acceptance.
# Use the already-installed exporter, then the original Chocolatey package,
# then Blender Foundation's signed-release-directory portable archive.
# External package service outages must not silently remove canonical assets.
$ErrorActionPreference = 'Stop'

function Find-Blender {
    $cmd = Get-Command -Name blender -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($cmd -and $cmd.Source -and (Test-Path -LiteralPath $cmd.Source -PathType Leaf)) {
        return $cmd.Source
    }
    $root = Join-Path -Path $env:ProgramFiles -ChildPath 'Blender Foundation'
    if (-not (Test-Path -LiteralPath $root -PathType Container)) { return $null }
    return Get-ChildItem -LiteralPath $root -Filter blender.exe -Recurse -File -ErrorAction Stop |
        Select-Object -First 1 -ExpandProperty FullName
}

function Download-OfficialBlender {
    # Pinned published LTS release; get SHA-256 from the *same* Blender
    # Foundation directory, not from a third-party mirror or package metadata.
    $version = '4.5.15'
    $filename = "blender-$version-windows-x64.zip"
    $base = 'https://download.blender.org/release/Blender4.5'
    $folder = Join-Path $env:RUNNER_TEMP 'beckenburg-official-blender'
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    $zip = Join-Path $folder $filename
    $checksums = Join-Path $folder "blender-$version.sha256"
    for ($attempt=1; $attempt -le 3; $attempt++) {
        try {
            Write-Host "Downloading original Blender Foundation portable exporter ($attempt/3)"
            Invoke-WebRequest -Uri "$base/blender-$version.sha256" -OutFile $checksums -TimeoutSec 90
            $entry = Get-Content -LiteralPath $checksums |
                Where-Object { $_ -match "^([a-fA-F0-9]{64})\\s+\\*?$([regex]::Escape($filename))\\s*$" } |
                Select-Object -First 1
            if (-not $entry) { throw "Official Blender checksum manifest lacks exact $filename" }
            $expected = $Matches[1].ToLowerInvariant()
            # Force a fresh archive if a previous attempt downloaded only a fragment.
            if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
            Invoke-WebRequest -Uri "$base/$filename" -OutFile $zip -TimeoutSec 900
            $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($actual -ne $expected) { throw "Official Blender archive SHA-256 mismatch" }
            $unpack = Join-Path $folder 'unpacked'
            Expand-Archive -LiteralPath $zip -DestinationPath $unpack -Force
            $exe = Get-ChildItem -LiteralPath $unpack -Filter blender.exe -Recurse -File |
                Select-Object -First 1 -ExpandProperty FullName
            if (-not $exe) { throw "Verified Blender archive contains no blender.exe" }
            Write-Host "Verified portable Blender against official SHA-256: $actual"
            return $exe
        } catch {
            Write-Warning "Official Blender download attempt $attempt failed: $($_.Exception.Message)"
            if ($attempt -ge 3) { throw }
            Start-Sleep -Seconds (10*$attempt)
        }
    }
    throw 'Official Blender bootstrap exhausted retries'
}

$exe = Find-Blender
if (-not $exe -and (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host 'Trying official Chocolatey package as first provisioning path'
    & choco install blender -y --no-progress
    $exitCode = $LASTEXITCODE
    $exe = Find-Blender
    if (-not $exe) {
        Write-Warning "Chocolatey Blender package unavailable (exit $exitCode); trying the Blender Foundation directly."
    }
}
if (-not $exe) { $exe = Download-OfficialBlender }
if (-not $exe -or -not (Test-Path -LiteralPath $exe -PathType Leaf)) {
    throw 'Blender provisioning failed; real asset exporter is missing'
}
& $exe --version
if ($LASTEXITCODE -ne 0) { throw "Blender executable failed validation: $exe" }
$directory = Split-Path -Path $exe -Parent
Add-Content -LiteralPath $env:GITHUB_PATH -Value $directory -Encoding utf8
$env:PATH = "$directory;$env:PATH"
Write-Host "Validated original Blender exporter at $exe"
