param(
    [string]$Jdk = 'C:/Program Files/Java/jdk-17',
    [string]$Jar = 'gpu-client/target/BeckenburgGPU.jar',
    [Parameter(Mandatory=$true)][string]$Output,
    [Parameter(Mandatory=$true)][string]$SourceLabel
)
$ErrorActionPreference='Stop'
$jarPath=(Resolve-Path -LiteralPath $Jar).Path
$root=[IO.Path]::GetFullPath($Output)
if(Test-Path -LiteralPath $root){throw 'Choose a fresh output directory; native verification never replaces an existing app'}
New-Item -ItemType Directory -Path $root | Out-Null
$inputPath=Join-Path $root 'input'
$statePath=Join-Path $root 'isolated-home'
New-Item -ItemType Directory -Path $inputPath,$statePath | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputPath 'BeckenburgGPU.jar')
$hash=(Get-FileHash -Algorithm SHA256 -LiteralPath $jarPath).Hash
$metadata=[ordered]@{
    source_label=$SourceLabel
    checkout_head=(& git rev-parse HEAD).Trim()
    worktree_status=@(& git status --short)
    jar_sha256=$hash
    note='Self-contained native diagnostic app-image. Hidden OpenGL map controls only; no installed-game or physical player acceptance.'
}
$metadata | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath (Join-Path $root 'provenance.json')
# Input is exactly one game JAR. Never package build/ or its state, test keys,
# old applications, benchmark overlays or crash dumps into a distribution.
& (Join-Path $Jdk 'bin/jpackage.exe') --type app-image --name Beckenburg --input $inputPath --main-jar BeckenburgGPU.jar --dest (Join-Path $root 'app') --java-options '-Dfile.encoding=UTF-8' --java-options '-Djava.awt.headless=true' --java-options ('-Duser.home="'+$statePath+'"')
if($LASTEXITCODE -ne 0){throw 'Native GPU verification packaging failed'}
$embedded=Join-Path $root 'app/Beckenburg/app/BeckenburgGPU.jar'
if((Get-FileHash -Algorithm SHA256 -LiteralPath $embedded).Hash -ne $hash){throw 'Embedded game JAR differs from candidate'}
$process=Start-Process -FilePath (Join-Path $root 'app/Beckenburg/Beckenburg.exe') -ArgumentList '--world-residency-review' -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $root 'native-stdout.txt') -RedirectStandardError (Join-Path $root 'native-stderr.txt') -PassThru
$deadline=[DateTime]::UtcNow.AddMinutes(3)
try{
    while(-not $process.WaitForExit(1000)){
        if([DateTime]::UtcNow -gt $deadline){throw 'Native GPU map verification timed out'}
    }
    if($process.ExitCode -ne 0){throw ('Native GPU verification failed: '+$process.ExitCode)}
    $frames=Join-Path $root 'build/world-residency-review/frames.csv'
    if(-not (Test-Path -LiteralPath $frames)){throw 'Native executable produced no GPU map evidence'}
    $metadata.native_exit_code=$process.ExitCode
    $metadata.embedded_jar_sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $embedded).Hash
    $metadata | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath (Join-Path $root 'provenance.json')
    Write-Output ('PASS native executable / exact candidate JAR / all-map OpenGL controls: '+$root)
}finally{
    if(-not $process.HasExited){Stop-Process -Id $process.Id -Force}
}
