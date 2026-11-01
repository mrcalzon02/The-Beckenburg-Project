package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;

/** Castle persistence uses the native codec, singleton and shared human history. */
public final class CastleDocumentRecoveryTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void same(CastleLevelEdits.Snapshot a,CastleLevelEdits.Snapshot b){
        check(a.props().equals(b.props())&&a.lamps().equals(b.lamps())&&a.surfaces().equals(b.surfaces())
            &&a.floorPaint().equals(b.floorPaint())&&a.wallPaint().equals(b.wallPaint())
            &&a.navBlocked().equals(b.navBlocked())&&a.doors().equals(b.doors())
            &&a.spawnX()==b.spawnX()&&a.spawnZ()==b.spawnZ()&&a.spawnYaw()==b.spawnYaw(),"Exact native scene roundtrip");
        check(a.stations().keySet().equals(b.stations().keySet()),"Station identity");
        for(String key:a.stations().keySet())check(Arrays.equals(a.stations().get(key),b.stations().get(key)),"Station coordinates "+key);
    }
    private static Properties properties(Path file)throws Exception{
        var p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}return p;
    }
    private static void write(Path file,Properties p)throws Exception{
        try(var out=Files.newOutputStream(file)){p.store(out,"Legacy fixture");}
    }
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("castle-document-recovery-");
        String oldHome=System.getProperty("user.home");System.setProperty("user.home",home.toString());
        try{
            var scene=CastleLevelEdits.current();var project=new CastleEditorProject(scene);
            var service=new EditorAgentService<>(new CastleEditorAgent(project,new CastleHubMap(),new SceneRenderer(),()->{}),new EditorHistory<>(project,60));
            var original=scene.snapshot();service.save(scene.revision());
            byte[] first=Files.readAllBytes(scene.path());
            service.transact(scene.revision(),"set-spawn",Map.of("x","13.5","z","19","yaw","1.25"));
            // Exercise removals, paint, navigation and station data through the native writer too.
            scene.removeProp(scene.props().get(0).id());scene.removeLamp(scene.lamps().get(0).id());
            scene.removeSurface(scene.surfaces().get(0).id());scene.paintFloor(2,2,1);scene.setNavBlocked(3,3,true);
            var edited=scene.snapshot();service.save(scene.revision());
            check(Arrays.equals(first,Files.readAllBytes(EditorDocumentStore.previous(scene.path()))),"Exact prior bytes retained");
            byte[] second=Files.readAllBytes(scene.path());service.save(scene.revision());
            check(Arrays.equals(second,Files.readAllBytes(scene.path())),"Deterministic no-op save");
            check(Arrays.equals(first,Files.readAllBytes(EditorDocumentStore.previous(scene.path()))),"No-op does not rotate backup");
            same(edited,new CastleLevelEdits(scene.path()).snapshot());
            Files.writeString(scene.path(),"format=invalid\n");
            try{service.reopen(scene.revision());throw new AssertionError("Corrupt scene accepted");}catch(java.io.IOException expected){}
            same(edited,scene.snapshot());service.recoverPrevious(scene.revision());same(original,scene.snapshot());
            same(original,new CastleLevelEdits(scene.path()).snapshot());
            service.undo(scene.revision());same(edited,scene.snapshot());
            check(Arrays.equals(first,Files.readAllBytes(scene.path())),"Undo changes live scene without implicit disk save");
            Files.writeString(EditorDocumentStore.previous(scene.path()),"format=invalid\n");
            try{service.recoverPrevious(scene.revision());throw new AssertionError("Invalid backup accepted");}catch(java.io.IOException expected){}
            same(edited,scene.snapshot());check(Arrays.equals(first,Files.readAllBytes(scene.path())),"Invalid recovery preserves disk");
            Path legacy=home.resolve("legacy.properties");var p=properties(scene.path());
            p.remove("surfaces.count");p.remove("doors.count");p.remove("lighting.layout");p.remove("fortification.layout");
            p.remove("gate.layout");p.remove("windows.layout");p.remove("furniture.layout");write(legacy,p);
            var migrated=new CastleLevelEdits(legacy);migrated.save();
            same(migrated.snapshot(),new CastleLevelEdits(legacy).snapshot());
            // The existing structure-v1 policy preserves original bytes before canonical rebuild.
            Path structure=home.resolve("structure-v1.properties");Files.writeString(structure,"format=1\n");
            byte[] legacyBytes=Files.readAllBytes(structure);var rebuilt=new CastleLevelEdits(structure);
            same(original,rebuilt.snapshot());
            check(Arrays.equals(legacyBytes,Files.readAllBytes(structure.resolveSibling("structure-v1.properties.before-structure-v2.properties"))),"Original migration backup retained");
            check(Arrays.equals(legacyBytes,Files.readAllBytes(EditorDocumentStore.previous(structure))),"Migration also journals prior source bytes");
            System.out.println("PASS Castle native recovery: deterministic saves, distinct backups, full codec roundtrip, same singleton recovery, shared undo, invalid backup refusal and legacy migrations");
        }finally{
            System.setProperty("user.home",oldHome);
            try(var paths=Files.walk(home)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
