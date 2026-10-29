package org.beckenburg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * An independent Windows process waits until Beckenburg is completely closed
 * before starting the already downloaded, checksum-verified setup executable.
 * No installer code is ever run from the install directory or inside the JVM.
 */
public final class BeckenburgInstallerHandoff {
    private BeckenburgInstallerHandoff() { }

    private static final String SCRIPT = """
        param(
            [Parameter(Mandatory=$true)][long]$GamePid,
            [Parameter(Mandatory=$true)][string]$Installer,
            [Parameter(Mandatory=$true)][string]$Sha256,
            [Parameter(Mandatory=$true)][string]$LogDirectory
        )
        $ErrorActionPreference = 'Stop'
        New-Item -ItemType Directory -Force -Path $LogDirectory | Out-Null
        $stamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-fff')
        $handoffLog = Join-Path $LogDirectory ('update-handoff-' + $stamp + '.log')
        $setupLog = Join-Path $LogDirectory ('update-setup-' + $stamp + '.log')
        function Write-Event([string]$message) {
            Add-Content -LiteralPath $handoffLog -Encoding UTF8 -Value ('{0:o} {1}' -f [DateTime]::UtcNow, $message)
        }
        try {
            Write-Event ('WAITING for Beckenburg process pid=' + $GamePid)
            $game = Get-Process -Id $GamePid -ErrorAction SilentlyContinue
            if ($null -ne $game -and -not $game.WaitForExit(180000)) {
                throw 'Beckenburg did not exit within 180 seconds; setup was not launched.'
            }
            Start-Sleep -Milliseconds 1200
            $other = @(Get-Process -Name 'Beckenburg' -ErrorAction SilentlyContinue)
            if ($other.Count -ne 0) {
                throw 'A Beckenburg game process is still running. Close all copies and run the staged installer manually.'
            }
            if (-not (Test-Path -LiteralPath $Installer -PathType Leaf)) {
                throw 'Staged installer was not found.'
            }
            $actual = (Get-FileHash -LiteralPath $Installer -Algorithm SHA256).Hash
            if ($actual -ine $Sha256) {
                throw 'Installer SHA-256 changed after download; setup was not launched.'
            }
            Write-Event ('GAME CLOSED; checksum verified. Starting separate setup: ' + $Installer)
            $arguments = @('/NORESTART', ('/LOG="' + $setupLog + '"'))
            $setup = Start-Process -FilePath $Installer -ArgumentList $arguments -PassThru
            Write-Event ('INSTALLER STARTED pid=' + $setup.Id + ' log=' + $setupLog)
            $setup.WaitForExit()
            Write-Event ('INSTALLER EXIT code=' + $setup.ExitCode)
            if ($setup.ExitCode -ne 0) { exit 1 }
        } catch {
            Write-Event ('HANDOFF FAILED: ' + $_.Exception.Message)
            exit 1
        }
        """;

    static String scriptText(){ return SCRIPT; }

    static Path writeScript(Path folder) throws IOException {
        Files.createDirectories(folder);
        Path script = folder.resolve("install-after-exit.ps1");
        Files.writeString(script, SCRIPT, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return script;
    }

    static Path validate(BeckenburgUpdates.StagedInstaller staged) throws IOException {
        if (staged == null || staged.path() == null || staged.sha256() == null
                || !staged.sha256().matches("[a-fA-F0-9]{64}")) {
            throw new IOException("Installer has no verified SHA-256");
        }
        Path path = staged.path().toAbsolutePath().normalize();
        Path directory = BeckenburgUpdates.updateDirectory().toAbsolutePath().normalize();
        if (!directory.equals(path.getParent()) || !path.getFileName().toString()
                .matches("Beckenburg-[0-9]+\\\\.[0-9]+\\\\.[0-9]+-Windows-x64\\\\.exe")
                || !Files.isRegularFile(path)) {
            throw new IOException("Installer is not in the dedicated Beckenburg update staging directory");
        }
        return path;
    }

    public static void start(BeckenburgUpdates.StagedInstaller staged) throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            throw new IOException("Windows setup is only supported on Windows");
        }
        Path installer = validate(staged);
        Path script = writeScript(BeckenburgUpdates.updateDirectory().toAbsolutePath().normalize());
        Path diagnostics = BeckenburgDiagnostics.directory().toAbsolutePath().normalize();
        Files.createDirectories(diagnostics);
        // PowerShell is a separate Windows process. It does not depend on the
        // jpackage runtime, Beckenburg.exe or any locked game installation files.
        new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
            "-ExecutionPolicy", "Bypass", "-File", script.toString(),
            "-GamePid", Long.toString(ProcessHandle.current().pid()),
            "-Installer", installer.toString(),
            "-Sha256", staged.sha256(),
            "-LogDirectory", diagnostics.toString())
            .directory(script.getParent().toFile()).start();
        BeckenburgDiagnostics.record("UPDATES independent helper started; installer="+
            installer.getFileName()+" logs="+diagnostics+" (game may now exit)");
    }
}
