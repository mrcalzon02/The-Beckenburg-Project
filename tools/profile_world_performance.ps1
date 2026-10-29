param(
    [string]$Java = 'C:/Program Files/Java/jdk-17/bin/java.exe',
    [string]$Jar = 'gpu-client/target/BeckenburgGPU.jar',
    [string[]]$Sites = @('CASTLE','HOUSE','FARMHOUSE','SCHOOL','MANSION','PRISON','ASYLUM','ACADEMY','COLLEGE','CHURCH','HOLLOW_CREEK'),
    [int]$Runs = 3,
    [int]$Samples = 120,
    [int]$Warmup = 90,
    [int]$Width = 640,
    [string]$Output = 'build/performance-comparison/current',
    [string]$ExtraClasspath = '',
    [string]$SourceLabel = '',
    [switch]$LightsOff,
    [switch]$Freeze
)
$ErrorActionPreference='Stop'
if($Runs -lt 1 -or $Samples -lt 1 -or $Warmup -lt 0){throw 'Invalid profile sample count'}
$javaPath=(Resolve-Path -LiteralPath $Java).Path
$jarPath=(Resolve-Path -LiteralPath $Jar).Path
$outputPath=[IO.Path]::GetFullPath((Join-Path (Get-Location) $Output))
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$control=Join-Path $outputPath 'control-classes'
New-Item -ItemType Directory -Force -Path $control | Out-Null
$javac=Join-Path (Split-Path $javaPath) 'javac.exe'
& $javac --release 17 -encoding UTF-8 -d $control src/test/java/org/beckenburg/VerificationPreferencesFactory.java
if($LASTEXITCODE -ne 0){throw 'Cannot compile isolated in-memory profile preferences'}
$classpath=$control+[IO.Path]::PathSeparator
if($ExtraClasspath){$classpath+=$ExtraClasspath+[IO.Path]::PathSeparator}
$classpath+=$jarPath
$sha=(& git rev-parse HEAD).Trim()
if(-not $SourceLabel){$SourceLabel=$sha}
$overlay=@()
if($ExtraClasspath){
    foreach($entry in $ExtraClasspath.Split([IO.Path]::PathSeparator)){
        if(Test-Path -LiteralPath $entry -PathType Container){
            $overlay+=@(Get-ChildItem -LiteralPath $entry -Recurse -Filter '*.class' | Sort-Object FullName | ForEach-Object {[ordered]@{file=$_.FullName;sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash}})
        }
    }
}
$metadata=[ordered]@{source_label=$SourceLabel;working_checkout_head=$sha;jar=$jarPath;jar_sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $jarPath).Hash;overlay_classes=$overlay;sites=$Sites;runs=$Runs;samples=$Samples;warmup=$Warmup;width=$Width;lights=(-not $LightsOff);freeze=[bool]$Freeze;timezone=[TimeZoneInfo]::Local.Id;note='Scripted same-instance rendering route; not physical navigation or installed full-round acceptance'}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 -LiteralPath (Join-Path $outputPath 'provenance.json')
for($run=1;$run -le $Runs;$run++){
    foreach($site in $Sites){
        $destination=Join-Path $outputPath ($site.ToLowerInvariant()+'-'+$run)
        $state=Join-Path $destination 'isolated-state'
        $temporary=Join-Path $state 'tmp'
        New-Item -ItemType Directory -Force -Path $temporary | Out-Null
        $javaArgs=@('-Djava.awt.headless=true','-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory',"-Duser.home=$state","-Djava.io.tmpdir=$temporary","-Dbeckenburg.profile.sha=$SourceLabel","-Dbeckenburg.profile.samples=$Samples","-Dbeckenburg.profile.warmup=$Warmup","-Dbeckenburg.profile.width=$Width","-Dbeckenburg.profile.output=$destination","-Dbeckenburg.profile.site=$site",('-Dbeckenburg.profile.lights='+(-not $LightsOff).ToString().ToLowerInvariant()),('-Dbeckenburg.profile.freeze='+$Freeze.ToString().ToLowerInvariant()),'-cp',$classpath,'org.beckenburg.CastlePerformanceReview')
        & $javaPath @javaArgs *> ($destination+'.log')
        if($LASTEXITCODE -ne 0){Get-Content -LiteralPath ($destination+'.log') -Tail 30;throw "GPU profile failed: $site / $run"}
        Get-Content -LiteralPath ($destination+'.log') | Select-String '^PROFILE '
    }
}
