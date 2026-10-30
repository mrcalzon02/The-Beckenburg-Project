package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Smoke test for development-mode persistence and case-state checkpoints. */
public final class BeckenburgDeveloperTraceTest {
    /** The parent must terminate this process forcibly, without shutdown hooks. */
    private static void forcedKillChild() throws Exception {
        BeckenburgDiagnostics.install();
        BeckenburgDeveloperTrace.caseStart("forced-kill-persistence");
        BeckenburgDeveloperTrace.caseStage("forced-kill-before-first-frame");
        Thread.sleep(TimeUnit.MINUTES.toMillis(5));
        throw new AssertionError("forced-kill child was not terminated by parent");
    }
    /** Test the real append-only session log after an OS-level forced process kill. */
    private static void verifyForcedKillPersistence() throws Exception {
        Path scratch=Files.createTempDirectory("beckenburg-forced-kill-");
        Path home=scratch.resolve("home");
        Files.createDirectories(home);
        Path directory=home.resolve(".beckenburg").resolve("diagnostics");
        Path java=Path.of(System.getProperty("java.home"),"bin",
            System.getProperty("os.name","").toLowerCase().contains("win")?"java.exe":"java");
        Process child=new ProcessBuilder(java.toString(),"-Duser.home="+home.toAbsolutePath(),
            "-cp",System.getProperty("java.class.path"),
            BeckenburgDeveloperTraceTest.class.getName(),"--forced-kill-child")
            .redirectErrorStream(true)
            .redirectOutput(scratch.resolve("child-output.txt").toFile())
            .start();
        Path session=null;
        try {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
            while(System.nanoTime()<deadline) {
                if(!child.isAlive())throw new AssertionError(
                    "diagnostic child exited before forced termination: "+child.exitValue());
                if(Files.isDirectory(directory)) {
                    try(var files=Files.list(directory)) {
                        session=files.filter(p->p.getFileName().toString().startsWith("session-")
                            &&p.getFileName().toString().endsWith(".log"))
                            .findFirst().orElse(null);
                    }
                    if(session!=null) {
                        String log=Files.readString(session);
                        if(log.contains("CASE START forced-kill-persistence")
                                &&log.contains("CASE STAGE forced-kill-before-first-frame"))break;
                    }
                }
                Thread.sleep(60);
            }
            if(session==null||!Files.readString(session).contains(
                    "CASE STAGE forced-kill-before-first-frame"))
                throw new AssertionError("last case stage was not flushed before forced kill");
            child.destroyForcibly();
            if(!child.waitFor(5,TimeUnit.SECONDS))
                throw new AssertionError("diagnostic child did not terminate forcibly");
            String persisted=Files.readString(session);
            if(!persisted.contains("CASE START forced-kill-persistence")
                    ||!persisted.contains("CASE STAGE forced-kill-before-first-frame"))
                throw new AssertionError("forced termination lost the last persisted stage");
            if(persisted.contains("CASE STOP")||persisted.contains("PROCESS exit"))
                throw new AssertionError("child exited normally; forced-kill test is invalid");
            Path evidence=Path.of("build","e2e","forced-kill-case-stage.log");
            Files.createDirectories(evidence.getParent());
            Files.writeString(evidence,BeckenburgDiagnostics.redact(persisted),StandardCharsets.UTF_8);
        } finally {
            if(child.isAlive()) {
                child.destroyForcibly();
                child.waitFor(5,TimeUnit.SECONDS);
            }
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length==1&&"--forced-kill-child".equals(args[0])) {
            forcedKillChild();
            return;
        }
        BeckenburgDiagnostics.install();
        BeckenburgDeveloperTrace.start();
        BeckenburgDeveloperTrace.caseStart("diagnostic-test");
        BeckenburgDeveloperTrace.caseStage("constructing GamePanel");
        BeckenburgDeveloperTrace.update("manual-start","installed=test");
        BeckenburgDeveloperTrace.snapshot("self-test");
        // A case that returns to the Castle must disarm the stall detector.
        // A subsequent case must re-arm it rather than inherit the old state.
        java.lang.reflect.Field active=BeckenburgDeveloperTrace.class.getDeclaredField("activeCase");
        active.setAccessible(true);
        if(!active.getBoolean(null))throw new AssertionError("case watchdog not armed");
        BeckenburgDeveloperTrace.caseStage("opening aerial approach");
        BeckenburgDeveloperTrace.frame(1_000_000L); // intro frames are real watchdog progress
        BeckenburgDeveloperTrace.caseStop();
        if(active.getBoolean(null))throw new AssertionError("closed case watchdog still armed");
        BeckenburgDeveloperTrace.caseStart("second-case");
        if(!active.getBoolean(null))throw new AssertionError("second case watchdog not armed");
        BeckenburgDeveloperTrace.caseStage("building initial sized scene");
        BeckenburgDeveloperTrace.caseStop();
        if(active.getBoolean(null))throw new AssertionError("second case watchdog still armed");
        BeckenburgDeveloperTrace.caseStop(); // duplicate close must not create a false stop
        String log=Files.readString(BeckenburgDiagnostics.currentLog());
        if(!log.contains("DEVELOPER_DIAGNOSTICS enabled"))throw new AssertionError("flight recorder not enabled");
        if(!log.contains("CASE START diagnostic-test"))throw new AssertionError("case entry not recorded");
        if(!log.contains("CASE STAGE constructing GamePanel"))throw new AssertionError("stage not recorded");
        if(!log.contains("UPDATE TRACE manual-start"))throw new AssertionError("updater trace absent");
        if(!log.contains("PERF thread_snapshot="))throw new AssertionError("snapshot absent");
        if(!log.contains("CASE STAGE opening aerial approach"))throw new AssertionError("intro stage absent");
        if(!log.contains("CASE START second-case"))throw new AssertionError("second case entry absent");
        if(!log.contains("CASE STAGE building initial sized scene"))throw new AssertionError("second case stage absent");
        int stops=log.split("CASE STOP elapsed_ms=", -1).length-1;
        if(stops!=2)throw new AssertionError("expected exactly two durable case stop records, got "+stops);
        verifyForcedKillPersistence();
        System.out.println("PASS BeckenburgDeveloperTraceTest (forced-kill stage persisted)");
    }
}
