package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** End-to-end scene module packaging tests: no game or screenshot mocks. */
public final class TerrainScenePackageTest {
    private static void check(boolean good,String why){
        if(!good)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("terrain-scene-package-");
        try{
            var world=StitchedHeightmapWorld.create(dir,
                new StitchedHeightmapWorld.Settings(16,1,0,0,-1,1,51L,4));
            world.generateRegion(0,0,2,1);
            byte[] source=Files.readAllBytes(world.tilePath(0,0));
            var pkg=new TerrainScenePackage(world);
            check(pkg.ready()&&pkg.capabilities().contains(EnvironmentModule.Capability.AUTHORING)
                &&pkg.capabilities().contains(EnvironmentModule.Capability.SERIALIZATION),
                "Publisher exposes only actual working authoring/serialization capabilities");
            var scene=pkg.openScene();
            var unrelated=new RuntimeScene.Node("keep","Keep this item",null,"props",
                RuntimeScene.Transform.identity(),List.of());
            scene.replace(unrelated);
            EnvironmentModules modules=new EnvironmentModules();
            modules.register(pkg);
            modules.attach(scene);
            var bounds=new TerrainEntranceMesh.Bounds(1,1,6,6);
            var pub=pkg.publish("patch_0",bounds);
            check(Files.isRegularFile(pub.geometryFile())
                &&Files.isRegularFile(pub.sceneFile())&&pub.triangles()>0,
                "Scene module emits real OBJ plus persistent scene document");
            check(scene.find(pub.nodeId())!=null&&scene.find("keep")!=null,
                "Publishing creates stable node without removing unrelated scene objects");
            var meshComponent=scene.find(pub.nodeId()).components().stream()
                .filter(c->c.type().equals(TerrainScenePackage.COMPONENT)).findFirst().orElseThrow();
            check(meshComponent.properties().get("uri")
                .equals("world://scene-package/meshes/patch_0.obj"),
                "Scene node points at portable world-relative asset URI");
            check(meshComponent.properties().get("rendererStatus")
                .equals("requires-host-adapter"),
                "OBJ editor geometry does not pretend to be game-ready GLB");
            check(TerrainScenePackage.inspect(world,scene).isEmpty(),
                "Packaged OBJ asset verifies hash and geometry inventory");
            var fingerprintBefore=TerrainScenePackage.sourceFingerprint(world,bounds);
            var refinement=AdaptiveTerrainTopology.open(world);
            refinement.split(new AdaptiveTerrainTopology.Node(3,3,0,0,0));
            refinement.save();
            check(!fingerprintBefore.equals(TerrainScenePackage.sourceFingerprint(world,bounds))
                &&TerrainScenePackage.inspect(world,scene).stream()
                    .anyMatch(message->message.contains("STALE")),
                "Changing the authored quadtree must flag a checksum-valid OBJ as stale");
            pkg.publish("patch_0",bounds);
            check(TerrainScenePackage.inspect(world,scene).isEmpty(),
                "Rebaking after source changes refreshes the published provenance");
            var persisted=pkg.openScene();
            check(persisted.nodes().equals(scene.nodes()),
                "Saved scene restores mesh, parent graph and existing unrelated nodes");
            long unchanged=Files.size(pub.geometryFile());
            pkg.publish("patch_0",bounds);
            check(scene.nodes().size()==2 && Files.size(pub.geometryFile())==unchanged,
                "Republishing updates stable node without duplicates");
            var before=scene.snapshot();
            boolean rejected=false;
            try{pkg.publish("../escape",bounds);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Chunk IDs cannot traverse out of world asset folder");
            check(scene.nodes().equals(persisted.nodes()),
                "Rejected invalid chunk never mutates authoritative scene");
            var seams=EntranceSeamDocument.open(world);
            seams.addEdge(new EntranceSeamDocument.EdgeCut("unconformed",
                EntranceSeamDocument.Method.EXTRA_SEAM,"bunker",4.5,3,4.5,6));
            seams.save();
            rejected=false;
            try{pkg.publish("unconformed",bounds);}
            catch(IllegalStateException expected){rejected=true;}
            check(rejected&&!Files.exists(pkg.root().resolve("meshes/unconformed.obj"))
                &&scene.nodes().equals(persisted.nodes()),
                "Failed geometry audit leaves no package artifact or scene mutation");
            Files.delete(seams.path());
            // A manually existing OBJ without an owned scene node must not be clobbered.
            Path foreign=pkg.root().resolve("meshes/foreign.obj");
            Files.writeString(foreign,"hand-authored model; not ours");
            rejected=false;
            try{pkg.publish("foreign",bounds);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected&&Files.readString(foreign).equals("hand-authored model; not ours"),
                "Foreign authored assets are never overwritten");
            var outsider=new InMemoryRuntimeScene("different-scene");
            rejected=false;
            try{TerrainScenePackage.publish(world,outsider,"another",bounds);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected&&outsider.nodes().isEmpty(),
                "Package cannot silently replace a different scene identity");
            Files.writeString(pub.geometryFile(),"tampered geometry");
            check(!TerrainScenePackage.inspect(world,scene).isEmpty(),
                "Inspection flags out-of-band texture/geometry modifications");
            pkg.publish("patch_0",bounds);
            check(TerrainScenePackage.inspect(world,scene).isEmpty(),
                "Re-publishing a known chunk repairs its verified OBJ asset");
            check(java.util.Arrays.equals(source,Files.readAllBytes(world.tilePath(0,0)))
                &&world.verifySeams().isEmpty(),
                "Publishing must preserve original 16-bit source art and terrain borders");
            modules.detach();
            rejected=false;
            try{pkg.publish("afterDetach",bounds);}
            catch(IllegalStateException expected){rejected=true;}
            check(rejected,"Module detaches from editor scene authority");
            System.out.println("PASS scene packaging: real OBJ+scene persistence, "+
                "stable IDs, material truth, asset verification, rollback, foreign asset protection");
        }finally{
            try(var walk=Files.walk(dir)){
                for(var p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
