$ErrorActionPreference='Stop'
$repo='C:\Users\Admin\Documents\ChatGPT\Phasmaphobia alike\The-Beckenburg-Unified'
$jar=Join-Path $repo 'gpu-client\target\BeckenburgGPU.jar'
$root=Join-Path $repo 'build\window-60-final-gpu'
$env:GITHUB_SHA='2e85049f54a4a0a8900169c551909f68eee55a1d'
$hash=(Get-FileHash $jar -Algorithm SHA256).Hash.ToLowerInvariant()
$cases=@(
 @('fixed-old','false','false','false','false','false'),
 @('fixed-wake','true','false','false','false','true'),
 @('events-old','false','false','true','true','false'),
 @('events-wake','true','false','true','true','false'),
 @('events-cache','true','true','true','true','false')
)
foreach($repeat in 1..2){foreach($case in $cases){
 $dest=Join-Path $root ($case[0]+'-run-'+$repeat)
 New-Item -ItemType Directory -Force -Path $dest | Out-Null
 @{'source'=$env:GITHUB_SHA;'jar_sha256'=$hash;'lowLatency'=$case[1];'retainVisibility'=$case[2];'scriptClock'=$case[3];'events'=$case[4];'depthParity'=$case[5];'started'=(Get-Date -Format o)} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $dest 'metadata.json') -Encoding utf8
 Push-Location $dest
 try {
  & 'C:\Program Files\Java\jdk-17\bin\java.exe' '-Dbeckenburg.gpu.residentHouse=true' ('-Dbeckenburg.gpu.lowLatency='+$case[1]) ('-Dbeckenburg.fixture.retainVisibility='+$case[2]) ('-Dbeckenburg.profile.scriptClock='+$case[3]) ('-Dbeckenburg.profile.events='+$case[4]) ('-Dbeckenburg.profile.depthParity='+$case[5]) '-jar' $jar '--live-house-profile' *> 'process.log'
  $code=$LASTEXITCODE
  Get-Content 'process.log' -Tail 5
  if($code -ne 0){throw ('GPU profile failed '+$dest+' exit '+$code)}
 } finally {Pop-Location}
}}
