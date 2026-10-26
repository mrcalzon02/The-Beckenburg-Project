package org.beckenburg;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * BECK-LIVE-012: verify that a playable launch which is abruptly terminated
 * still leaves durable local case-start markers. Runtime.halt deliberately
 * skips shutdown hooks, like an externally killed, unresponsive process.
 */
public final class BeckenburgForcedTerminationTest {
    private BeckenburgForcedTerminationTest() {}
    private static void require(boolean valid,String why){
        if(!valid)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        if(args.length==1&&args[0].equals("--forced-kill-child")){
            BeckenburgDiagnostics.install();
            // The real report began AFTER reaching the Castle/menu. The
            // startup-complete marker is therefore already cleared.
            BeckenburgDiagnostics.ready();
            BeckenburgDeveloperTrace.caseStart(
                "campaign=false assistant=false seed=forced-kill-regression");
            BeckenburgDeveloperTrace.caseStage("loading map geometry");
            BeckenburgDiagnostics.record("CASE REGRESSION child about to be terminated");
            Runtime.getRuntime().halt(67);
            throw new AssertionError("A forcibly terminated JVM returned");
        }

        Path home=Files.createTempDirectory("beckenburg-forced-exit-");
        Path logDir=home.resolve(".beckenburg").resolve("diagnostics");
        String executable=Path.of(System.getProperty("java.home"),"bin",
            System.getProperty("os.name","").toLowerCase().contains("win")
                ?"java.exe":"java").toString();
        Process child=new ProcessBuilder(executable,
            "-Djava.awt.headless=true","-Duser.home="+home,
            "-cp",System.getProperty("java.class.path"),
            BeckenburgForcedTerminationTest.class.getName(),
            "--forced-kill-child")
            .redirectErrorStream(true)
            .redirectOutput(home.resolve("child-output.txt").toFile())
            .start();
        try{
            boolean completed=child.waitFor(15,TimeUnit.SECONDS);
            if(!completed){
                child.destroyForcibly();
                child.waitFor(5,TimeUnit.SECONDS);
            }
            require(completed,"case-start recorder child did not terminate promptly");
            require(child.exitValue()==67,
                "child did not reach the explicit post-case-start forced kill: "+child.exitValue());
            require(Files.isDirectory(logDir),"local diagnostics directory was not created");
            java.util.List<Path> sessions;
            try(var files=Files.list(logDir)){
                sessions=files.filter(p->p.getFileName().toString().startsWith("session-")
                    &&p.getFileName().toString().endsWith(".log")).toList();
            }
            require(!sessions.isEmpty(),"forced kill lost the on-disk session log");
            boolean recovered=false;
            for(Path log:sessions){
                String body=Files.readString(log,StandardCharsets.UTF_8);
                if(body.contains("CASE START campaign=false assistant=false")
                    &&body.contains("CASE STAGE loading map geometry")
                    &&body.contains("CASE REGRESSION child about to be terminated")){
                    recovered=true;break;
                }
            }
            require(recovered,
                "last case-start stage did not survive a shutdown-hook-free forced termination");
            require(!Files.exists(logDir.resolve("startup.incomplete")),
                "case begins after the launcher startup marker has been cleared");
            System.out.println("PASS: forced JVM termination preserves post-menu case-start and "+
                "last stage in durable local diagnostics, without shutdown hooks");
        }finally{
            if(child.isAlive()){
                child.destroyForcibly();
                child.waitFor(5,TimeUnit.SECONDS);
            }
            try(var files=Files.walk(home)){
                for(Path file:files.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
    }
}
