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
        String log=Files.readString(BeckenburgDiagnostics.currentLog());
        if(!log.contains("DEVELOPER_DIAGNOSTICS enabled"))throw new AssertionError("flight recorder not enabled");
        if(!log.contains("CASE START diagnostic-test"))throw new AssertionError("case entry not recorded");
        if(!log.contains("CASE STAGE constructing GamePanel"))throw new AssertionError("stage not recorded");
        if(!log.contains("UPDATE TRACE manual-start"))throw new AssertionError("updater trace absent");
        if(!log.contains("PERF thread_snapshot="))throw new AssertionError("snapshot absent");
        System.out.println("PASS BeckenburgDeveloperTraceTest");
    }
}
