package org.beckenburg;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/** Exercise the actual runner: malformed last entries fail before any test process launches. */
public final class BeckenburgRegressionManifestTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private record Result(int code,String text){}
    private static Result run(List<String> names)throws Exception{
        Path dir=Files.createTempDirectory("regression-preflight-");
        Files.writeString(dir.resolve("regression-tests.txt"),String.join("\n",names)+"\n");
        String cp=String.join(File.pathSeparator,Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
            .map(s->Path.of(s).toAbsolutePath().normalize().toString()).toList());
        String executable=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase().contains("win")?"java.exe":"java").toString();
        Path log=dir.resolve("result.txt");
        var process=new ProcessBuilder(executable,"-Djava.awt.headless=true","-ea","-cp",cp,
            "org.beckenburg.BeckenburgRegressionSuite","--validate").directory(dir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();throw new AssertionError("Manifest preflight launched/hung instead of validating");}
        return new Result(process.exitValue(),Files.readString(log));
    }
    public static void main(String[] args)throws Exception{
        var names=Files.readAllLines(Path.of("regression-tests.txt")).stream().map(String::trim)
            .filter(s->!s.isEmpty()&&!s.startsWith("#")).toList();
        var good=run(names);check(good.code()==0&&good.text().contains("no tests executed")&&!good.text().contains("TEST START"),"Valid actual manifest preflight failed: "+good.text());
        var qualified=new ArrayList<>(names);qualified.set(qualified.size()-1,"org.beckenburg.CastleGroundConstructionTest");
        var bad=run(qualified);check(bad.code()!=0&&bad.text().contains("Invalid regression entry")&&!bad.text().contains("TEST START"),"Qualified last entry launches tests before refusal");
        var duplicate=new ArrayList<>(names);duplicate.set(duplicate.size()-1,"CastleHubIntegrationTest");
        bad=run(duplicate);check(bad.code()!=0&&bad.text().contains("Duplicate regression entry")&&!bad.text().contains("TEST START"),"Duplicate manifest entry accepted");
        var missing=new ArrayList<>(names);missing.set(missing.size()-1,"MissingGameplayContractTest");
        bad=run(missing);check(bad.code()!=0&&bad.text().contains("ClassNotFoundException")&&!bad.text().contains("TEST START"),"Missing compiled regression entry accepted");
        System.out.println("PASS actual regression runner preflight: "+names.size()+" names/entrypoints; malformed qualified tail, duplicate and missing test refuse before any test launch; source manifest unchanged.");
    }
}
