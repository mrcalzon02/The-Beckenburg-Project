package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline test of detached installer ordering, logging, safety and location. */
public final class BeckenburgInstallerHandoffTest {
    private BeckenburgInstallerHandoffTest() { }
    private static void ok(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        if(args.length==2&&"--emit-script".equals(args[0])){
            Path path=Path.of(args[1]);
            if(path.getParent()!=null)Files.createDirectories(path.getParent());
            Files.writeString(path,BeckenburgInstallerHandoff.scriptText(),StandardCharsets.UTF_8);
            System.out.println("EMITTED independent Windows setup helper: "+path);
            return;
        }
        String script=BeckenburgInstallerHandoff.scriptText();
        ok(script.indexOf("WaitForExit(180000)") >= 0,"External helper must await game process exit");
        ok(script.indexOf("WaitForExit(180000)") < script.indexOf("Start-Process -FilePath $Installer"),
            "Do not launch installer while game holds installation files");
        ok(script.contains("Get-FileHash")&&script.contains("$actual -ine $Sha256"),
            "Revalidate the verified downloaded installer just before running setup");
        ok(script.contains("update-handoff-")&&script.contains("update-setup-"),
            "Persistent independent helper and Inno Setup logs required");
        ok(script.contains("HANDOFF FAILED:")&&script.contains("INSTALLER EXIT code="),
            "Every external failure and exit code must be recorded");
        Path folder=Files.createTempDirectory("beckenburg-handoff-test-");
        try{
            Path output=BeckenburgInstallerHandoff.writeScript(folder);
            ok(Files.isRegularFile(output)&&Files.readString(output,StandardCharsets.UTF_8).equals(script),
                "The helper must be an external script, not embedded in the running game");
        }finally{
            Files.deleteIfExists(folder.resolve("install-after-exit.ps1"));
            Files.deleteIfExists(folder);
        }
        boolean rejected=false;
        try{BeckenburgInstallerHandoff.validate(new BeckenburgUpdates.StagedInstaller(
            Path.of("not-an-installer.exe"),"0".repeat(64)));}
        catch(java.io.IOException expected){rejected=true;}
        ok(rejected,"Reject installers outside Beckenburg's downloaded staging directory");
        rejected=false;
        try{BeckenburgInstallerHandoff.validate(new BeckenburgUpdates.StagedInstaller(
            BeckenburgUpdates.updateDirectory().resolve("Beckenburg-1.2.3-Windows-x64.exe"),"bogus"));}
        catch(java.io.IOException expected){rejected=true;}
        ok(rejected,"Reject setup with an invalid recorded SHA-256");
        System.out.println("PASS detached setup waits for closed game, rechecks SHA-256 and logs outcomes");
    }
}
