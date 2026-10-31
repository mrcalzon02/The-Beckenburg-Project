package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Scene contract persistence, hierarchy safety and replay transaction tests. */
public final class RuntimeSceneDocumentTest {
    private static void check(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("runtime-scene-document-");
        try{
            var scene=new InMemoryRuntimeScene("level_01");
            var root=new RuntimeScene.Node("room","Room",null,"geometry",
                RuntimeScene.Transform.identity(),List.of());
            var component=new RuntimeScene.Component("mesh-asset",Map.of(
                "uri","world://scene-package/meshes/room.obj",
                "materialStatus","unassigned","purpose","game-agnostic authoring"));
            var child=new RuntimeScene.Node("lamp","Lamp","room","lighting",
                new RuntimeScene.Transform(1,2,3,0,45,0,1,1,1),List.of(component));
            scene.replace(root);scene.replace(child);
            Path path=dir.resolve("scene.bin");
            RuntimeSceneDocument.save(path,scene);
            byte[] first=Files.readAllBytes(path);
            RuntimeSceneDocument.save(path,scene);
            check(java.util.Arrays.equals(first,Files.readAllBytes(path)),
                "Repeated save must produce deterministic byte-for-byte scene");
            var loaded=RuntimeSceneDocument.load(path);
            check(loaded.sceneId().equals("level_01")&&loaded.nodes().equals(scene.nodes()),
                "All node IDs, hierarchy, transforms and component metadata roundtrip");
            var restored=new InMemoryRuntimeScene("level_01");
            restored.replace(new RuntimeScene.Node("obsolete","Old",null,"props",
                RuntimeScene.Transform.identity(),List.of()));
            RuntimeSceneDocument.restoreInto(path,restored);
            check(restored.nodes().equals(scene.nodes())&&restored.find("obsolete")==null,
                "Restoring a scene replaces prior editor state using target adapter");
            var foreign=new InMemoryRuntimeScene("different-level");
            foreign.replace(new RuntimeScene.Node("keep","Keep",null,"props",
                RuntimeScene.Transform.identity(),List.of()));
            boolean foreignRejected=false;
            try{RuntimeSceneDocument.restoreInto(path,foreign);}
            catch(java.io.IOException expected){foreignRejected=true;}
            check(foreignRejected&&foreign.find("keep")!=null,
                "A different level cannot be silently overwritten");
            // Out-of-order or orphaned children cannot pass the save gate.
            var bad=new InMemoryRuntimeScene("bad-scene");
            boolean missingRejected=false;
            try{bad.replace(new RuntimeScene.Node("orphan","Orphan","missing",
                "props",RuntimeScene.Transform.identity(),List.of()));}
            catch(IllegalArgumentException expected){missingRejected=true;}
            check(missingRejected,"In-memory host itself rejects unknown parent");
            Files.write(dir.resolve("broken.bin"),new byte[]{4,3,2,1});
            boolean malformedRejected=false;
            try{RuntimeSceneDocument.load(dir.resolve("broken.bin"));}
            catch(java.io.IOException expected){malformedRejected=true;}
            check(malformedRejected,"Untrusted scene bytes rejected without unsafe deserialization");
            var same=RuntimeSceneDocument.load(path);
            var earlier=same.snapshot();
            same.remove("room");
            check(same.nodes().isEmpty(),"Runtime removal retains subtree semantics");
            same.restore(earlier);
            check(same.nodes().equals(scene.nodes()),
                "RuntimeScene snapshots remain independent after saving");
            System.out.println("PASS scene document: deterministic atomic save/load, "+
                "parented objects, typed properties, identity isolation and undo");
        }finally{
            try(var walk=Files.walk(dir)){
                for(var p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
