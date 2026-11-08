# Exercise the production installer on a disposable Windows Actions runner.
$ErrorActionPreference = 'Stop'
if ($env:RUNNER_ENVIRONMENT -ne 'github-hosted' -or !$env:RUNNER_TEMP) {
    throw 'Installed acceptance requires a disposable GitHub-hosted runner'
}
$root = (Resolve-Path .).Path
$scratch = Join-Path $env:RUNNER_TEMP ('editor-mcp-' + [guid]::NewGuid().ToString('N'))
$inputDir = Join-Path $scratch 'input'
$probeClasses = Join-Path $scratch 'probe-classes'
$profile = Join-Path $scratch 'profile'
$target = Join-Path $scratch 'installed'
New-Item -ItemType Directory -Path $inputDir,$probeClasses,$profile | Out-Null
New-Item -ItemType Directory -Force -Path dist/installer | Out-Null
Copy-Item -LiteralPath build/beckenburg-game.jar -Destination $inputDir
& jpackage --type app-image --name Beckenburg --input $inputDir --main-jar beckenburg-game.jar --main-class org.beckenburg.InvestigationGame --dest dist --icon build/Beckenburg.ico
if ($LASTEXITCODE -ne 0) { throw 'Native editor packaging failed' }

$iscc = "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"
if (!(Test-Path -LiteralPath $iscc)) {
    $setup = Join-Path $scratch 'innosetup-6.7.4.exe'
    Invoke-WebRequest 'https://github.com/jrsoftware/issrc/releases/download/is-6_7_4/innosetup-6.7.4.exe' -OutFile $setup -TimeoutSec 180
    if ((Get-FileHash $setup -Algorithm SHA256).Hash -ne 'a549929319f9120716677c685535ca4671e4b937b5fdc87e2585f32d8eb30e8c') { throw 'Inno Setup checksum mismatch' }
    $signature = Get-AuthenticodeSignature -FilePath $setup
    if ($signature.Status -ne 'Valid' -or $signature.SignerCertificate.Subject -notlike '*Pyrsys B.V.*') { throw 'Unexpected Inno Setup signature' }
    $compiler = Start-Process $setup -ArgumentList '/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART' -WindowStyle Hidden -Wait -PassThru
    if ($compiler.ExitCode -ne 0) { throw 'Inno Setup installation failed' }
}
$env:BECKENBURG_RELEASE_VERSION = '0.0.0'
& $iscc installer/beckenburg.iss
if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed' }
$install = Start-Process 'dist/installer/Beckenburg-0.0.0-Windows-x64.exe' -ArgumentList '/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',('/DIR="' + $target + '"') -WindowStyle Hidden -Wait -PassThru
if ($install.ExitCode -ne 0) { throw "Installation failed: $($install.ExitCode)" }

# Only the external test probe goes on the instrumentation path. The editor loads from its installed JAR.
& javac --release 17 -encoding UTF-8 -cp build/beckenburg-game.jar -d $probeClasses src/test/java/org/beckenburg/EditorAgentGuiTest.java src/test/java/org/beckenburg/ModelEditorGuiTestSupport.java src/test/java/org/beckenburg/G2TexturedModelGuiTest.java src/test/java/org/beckenburg/InstalledEditorMcpTest.java
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
$manifest = Join-Path $scratch 'probe.mf'
"Premain-Class: org.beckenburg.InstalledEditorMcpTest`n" | Set-Content -Encoding ascii $manifest
$probe = Join-Path $scratch 'probe.jar'
& jar --create --file $probe --manifest $manifest -C $probeClasses .
if ($LASTEXITCODE -ne 0) { throw 'Probe packaging failed' }
$env:JAVA_TOOL_OPTIONS = '"-javaagent:' + $probe + '" "-Duser.home=' + $profile + '"'
$env:EDITOR_MCP_SCRIPT = (Resolve-Path (Join-Path $target 'tools/editor_mcp.py')).Path
$env:EDITOR_MCP_PYTHON = (Get-Command python).Source
$exe = Join-Path $target 'Beckenburg.exe'
$logs = Join-Path $root 'build/editor-agent-evidence'
$process = Start-Process $exe -ArgumentList '--editor' -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardError "$logs/installed-stderr.log" -RedirectStandardOutput "$logs/installed-stdout.log"
try {
    if (!$process.WaitForExit(180000)) { throw 'Installed editor timed out' }
    if ($process.ExitCode -ne 0 -or !(Test-Path -LiteralPath "$logs/installed-mcp.txt")) {
        Get-Content -LiteralPath "$logs/installed-stderr.log" -Tail 60
        throw "Installed MCP acceptance failed: $($process.ExitCode)"
    }
    Get-Content -LiteralPath "$logs/installed-mcp.txt"
} finally {
    if (!$process.HasExited) { $process.Kill() }
}
