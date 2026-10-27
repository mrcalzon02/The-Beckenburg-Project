package org.beckenburg;

import java.nio.file.Files;

/** Smoke test for development-mode persistence and case-state checkpoints. */
public final class BeckenburgDeveloperTraceTest {
    public static void main(String[] args)throws Exception {
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
        System.out.println("PASS BeckenburgDeveloperTraceTest");
    }
}
