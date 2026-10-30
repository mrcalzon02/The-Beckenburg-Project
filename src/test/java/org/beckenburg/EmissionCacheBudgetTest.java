package org.beckenburg;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Reserved sparse material IDs must not allocate a dense multi-gigabit cache.
 * A small-heap child makes the memory contract independent of machine RAM. */
public final class EmissionCacheBudgetTest {
    public static void main(String[] args)throws Exception{
        if(args.length>0){
            var lighting=new FixtureLighting(new CastleHubMap());
            // The VAN map is a deliberately self-illuminated LCD sentinel,
            // NOT a fixture light and NOT a dense material-cache index.
            var mapFace=lighting.emission(GlbProps.VAN_MAP_MATERIAL,0,0,0);
            if(mapFace.energy()<=0)
                throw new AssertionError("Live VAN map lost its intentionally lit screen");
            // Other unallocated sentinel IDs must stay dark and must never
            // allocate a multi-gigabit BitSet in the bounded-heap child.
            for(int id:new int[]{Integer.MAX_VALUE,Integer.MAX_VALUE-1,-4}){
                if(lighting.emission(id,0,0,0).energy()!=0)
                    throw new AssertionError("Unallocated reserved material invented fixture emission: "+id);
            }
            System.out.println("PASS intentionally lit VAN map and sparse unallocated material IDs within 96MiB Java heap");return;
        }
        String binary=System.getProperty("os.name").toLowerCase().contains("win")?"java.exe":"java";
        var child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin",binary).toString(),"-Xmx96m","-ea",
            "-Djava.awt.headless=true","-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory",
            "-Duser.home="+System.getProperty("user.home"),"-Djava.io.tmpdir="+System.getProperty("java.io.tmpdir"),
            "-cp",System.getProperty("java.class.path"),EmissionCacheBudgetTest.class.getName(),"--probe").redirectErrorStream(true).start();
        if(!child.waitFor(30,TimeUnit.SECONDS)){child.destroyForcibly();throw new AssertionError("Emission material budget child timed out");}
        String output=new String(child.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        if(child.exitValue()!=0)throw new AssertionError("Reserved material cache exceeded bounded heap: "+output);
        System.out.print(output);
    }
}
