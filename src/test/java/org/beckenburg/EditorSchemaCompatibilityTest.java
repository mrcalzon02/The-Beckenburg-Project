package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;

/** Unsupported data is refused, not silently flattened or downgraded on native save. */
public final class EditorSchemaCompatibilityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface IO{void run()throws Exception;}
    private static void refused(IO call)throws Exception{try{call.run();}catch(java.io.IOException expected){return;}throw new AssertionError("Unsupported schema accepted");}
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("native-schema-compatibility-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            var mission=new LevelAuthoringSession(HouseMap.Site.HOUSE);mission.save();var nativeState=mission.snapshot();byte[] valid=Files.readAllBytes(mission.documentPath());
            for(String mode:List.of("property","metadata","yaw")){
                Files.write(mission.documentPath(),valid);mission.reopen();var scene=RuntimeSceneDocument.load(mission.documentPath());var node=scene.nodes().get(0);var t=node.transform();
                var data=new LinkedHashMap<>(node.components().get(0).properties());
                if(mode.equals("property"))data.put("future-feature","must-not-disappear");
                scene.replace(new RuntimeScene.Node(node.id(),mode.equals("metadata")?"Custom authored label":node.name(),node.parentId(),
                    mode.equals("metadata")?"custom-layer":node.layer(),mode.equals("yaw")?new RuntimeScene.Transform(t.x(),t.y(),t.z(),t.pitch(),t.yaw()+20,t.roll(),t.scaleX(),t.scaleY(),t.scaleZ()):t,
                    List.of(new RuntimeScene.Component(node.components().get(0).type(),data))));
                RuntimeSceneDocument.save(mission.documentPath(),scene);byte[] unsupported=Files.readAllBytes(mission.documentPath());
                refused(mission::reopen);refused(mission::save);
                check(mission.snapshot().equals(nativeState)&&Arrays.equals(unsupported,Files.readAllBytes(mission.documentPath())),"Unsupported "+mode+" preserves actual source and live native state");
            }
            Files.write(mission.documentPath(),valid);mission.reopen();var legacy=RuntimeSceneDocument.load(mission.documentPath());var node=legacy.nodes().get(0);var data=new LinkedHashMap<>(node.components().get(0).properties());data.remove("yawRad");
            legacy.replace(new RuntimeScene.Node(node.id(),node.name(),node.parentId(),node.layer(),node.transform(),List.of(new RuntimeScene.Component(node.components().get(0).type(),data))));
            RuntimeSceneDocument.save(mission.documentPath(),legacy);mission.reopen();
            check(mission.prop(0).yaw()==Math.toRadians(node.transform().yaw()),"Legacy BSCN without yawRad uses its actual portable rotation");mission.save();
            check(new LevelAuthoringSession(HouseMap.Site.HOUSE).snapshot().equals(mission.snapshot()),"Legacy optional yaw migration remains stable after native save/reopen");
            var castle=new CastleLevelEdits(home.resolve("castle.properties"));castle.save();var original=castle.snapshot();byte[] castleBytes=Files.readAllBytes(castle.path());
            for(String layout:List.of("structure","lighting","fortification","gate","windows","furniture")){
                Files.write(castle.path(),castleBytes);castle.reopen();var p=new Properties();try(var in=Files.newInputStream(castle.path())){p.load(in);}p.setProperty(layout+".layout","3");
                try(var out=Files.newOutputStream(castle.path())){p.store(out,"Unsupported future layout fixture");}byte[] future=Files.readAllBytes(castle.path());
                refused(castle::reopen);refused(castle::save);
                check(castle.snapshot().equals(original)&&Arrays.equals(future,Files.readAllBytes(castle.path())),"Future "+layout+" layout cannot be silently rewritten as version2");
            }
            System.out.println("PASS native schema compatibility: unknown component properties, custom labels/layers and contradictory yaw refused without loss; legacy yaw retained; all six future Castle layout versions protected");
        }finally{
            System.setProperty("user.home",oldHome);try(var paths=Files.walk(home)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
