package org.beckenburg;
import java.nio.file.*;
import java.util.*;
/** Run with the pre-upgrade JAR, then corrected classes: actual codec loss control. */
public final class LegacySceneLossControl {
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("castle-legacy-loss-");System.setProperty("user.home",root.toString());
        var p=new Properties();try(var in=Files.newInputStream(Path.of("development/levels/rebuild/native-upgrade-baselines/original.properties"))){p.load(in);}
        p.setProperty("structure.layout","1");
        for(int i=0;i<Integer.parseInt(p.getProperty("props.count"));i++)if("ground6".equals(p.getProperty("props."+i+".id")))p.setProperty("props."+i+".x","16.0");
        Path file=root.resolve("authored-old-castle.properties");try(var out=Files.newOutputStream(file)){p.store(out,"Actual original default with authored chair edit");}
        var scene=new CastleLevelEdits(file);double x=scene.props().stream().filter(a->a.id().equals("ground6")).findFirst().orElseThrow().x();
        System.out.println("Actual loaded authored chair x="+x+"; expected16.0");
        if(x!=16)throw new AssertionError("Structure-v1 canonical reset discarded authored chair edit");
        System.out.println("PASS authored structure-v1 data preserved by actual native loader");
    }
}
