package org.beckenburg;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes the entire release regression manifest as isolated Java processes.
 * Per-test deadlines reveal hangs; failures are reported and NEVER waived.
 */
public final class BeckenburgRegressionSuite {
    private BeckenburgRegressionSuite(){}
    private static final long TEST_TIMEOUT_SECONDS=90;
    // Dense-versus-sparse lighting parity covers Castle and every authored map.
    // Castle alone can take over a minute on hosted CI; do not turn runner
    // scheduling variance into a false render-integrity failure. All parity
    // assertions still run, and the process remains bounded.
    private static final long LIGHT_BATCH_PARITY_TIMEOUT_SECONDS=180;
    private static final int MAX_FAILURES=12;
    public static void main(String[] args)throws Exception{
        Path manifest=Path.of("regression-tests.txt");
        if(!Files.isRegularFile(manifest))throw new IOException("Regression manifest absent");
        List<String> tests=Files.readAllLines(manifest,StandardCharsets.UTF_8).stream()
            .map(String::trim).filter(s->!s.isEmpty()&&!s.startsWith("#")).toList();
        // Validate the entire manifest BEFORE launching any process. A malformed
        // last entry must not hide the completed suite's summary or skip new tests.
        var unique=new java.util.HashSet<String>();
        // CI launches this main class with build/classes only. Its child tests
        // live in build/test, so preflight must use the same additional root.
        try(var entries=new java.net.URLClassLoader(new java.net.URL[]{Path.of("build/test").toUri().toURL()},BeckenburgRegressionSuite.class.getClassLoader())){
        for(String test:tests){
            if(!test.matches("[A-Za-z][A-Za-z0-9_]*Test"))throw new IllegalArgumentException("Invalid regression entry: "+test);
            if(!unique.add(test))throw new IllegalArgumentException("Duplicate regression entry: "+test);
            if("SmokeTest".equals(test)&&!Files.exists(Path.of("build/test/org/beckenburg/SmokeTest.class")))continue;
            var type=Class.forName("org.beckenburg."+test,false,entries);
            var entry=type.getMethod("main",String[].class);
            if(!java.lang.reflect.Modifier.isStatic(entry.getModifiers())||entry.getReturnType()!=void.class)
                throw new IllegalArgumentException("Regression entry lacks public static void main: "+test);
        }
        }
        if(tests.size()<145 || !tests.containsAll(List.of("BeckenburgDiagnosticsTest",
            "CastleHubIntegrationTest","BeckenburgUpdatesTest","GameplaySmokeTest","LanWorldIntegrationTest",
            "CastleTrainingAnnexTest","CastleWorldTerrainTest","CastleCellarConstructionTest",
            "CastleGroundConstructionTest","CastleUpperConstructionTest","CastleMuseumConstructionTest","CastleTesterBedTest","CastleFireplaceTest","CastleTapestryTest","CastleHeritageSurfacesTest","CastleHeraldicDisplayTest","CastlePlantFitTest","WorkbenchMaterialTest","CastleTrainingLessonTest","CastleFinishPlacementTest","BeckenburgRegressionManifestTest")))
            throw new IllegalStateException("Regression manifest incomplete");
        if(args.length==1&&args[0].equals("--validate")){
            System.out.println("PASS regression manifest preflight: "+tests.size()+" unique short names and available entrypoints; no tests executed");return;
        }
        if(args.length!=0)throw new IllegalArgumentException("Usage: BeckenburgRegressionSuite [--validate]");
        String java=Path.of(System.getProperty("java.home"),"bin",
            System.getProperty("os.name","").toLowerCase().contains("win")?"java.exe":"java").toString();
        String classpath=String.join(File.pathSeparator,"build/classes","build/test");
        int failed=0,passed=0,skipped=0;
        for(String test:tests){
            if("SmokeTest".equals(test)&&!Files.exists(Path.of("build/test/org/beckenburg/SmokeTest.class"))){
                System.out.println("SKIP optional SmokeTest source absent");
                skipped++;continue;
            }
            System.out.println("TEST START org.beckenburg."+test);
            Instant started=Instant.now();
            Process p=new ProcessBuilder(java,"-Djava.awt.headless=true","-ea",
                "-cp",classpath,"org.beckenburg."+test).inheritIO().start();
            long timeoutSeconds="PreparedLightBatchParityTest".equals(test)
                ?LIGHT_BATCH_PARITY_TIMEOUT_SECONDS:TEST_TIMEOUT_SECONDS;
            boolean exited=p.waitFor(timeoutSeconds,TimeUnit.SECONDS);
            if(!exited){
                p.destroyForcibly();
                p.waitFor(10,TimeUnit.SECONDS);
                failed++;
                System.err.println("FAIL TIMEOUT "+test+" after "+timeoutSeconds+" s");
            }else if(p.exitValue()!=0){
                failed++;
                System.err.println("FAIL "+test+" processExit="+p.exitValue());
            }else{
                passed++;
                System.out.println("TEST PASS "+test+" elapsed="+Duration.between(started,Instant.now()).toMillis()+"ms");
            }
            if(failed>=MAX_FAILURES){
                System.err.println("FAIL STOP: "+MAX_FAILURES+" broken tests; no game artifact permitted");
                break;
            }
        }
        System.out.println("REGRESSION SUMMARY passed="+passed+" failed="+failed+" skipped="+skipped+
            " manifest="+tests.size());
        if(failed>0 || passed+skipped!=tests.size())
            throw new IllegalStateException("Release regression gate failed: "+failed+
                " test failures; "+(tests.size()-passed-failed-skipped)+" not executed");
    }
}
