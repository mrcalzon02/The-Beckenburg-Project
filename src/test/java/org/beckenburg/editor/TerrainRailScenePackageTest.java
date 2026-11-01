package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Authoritative saved spline -> real geometry -> shared scene manifest. */
public final class TerrainRailScenePackageTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static TerrainInfrastructureSplines.Alignment rail(double end){
        return new TerrainInfrastructureSplines.Alignment("mainline",
            TerrainInfrastructureSplines.Kind.RAIL,
            List.of(new TerrainLayerStack.Point(4,10,12),
                new TerrainLayerStack.Point(end,10,12.2)),1.55,3,4,true);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("real-rail-scene-");
        try{
            var world=StitchedHeightmapWorld.create(dir.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1000,42L,4));
            var routes=TerrainInfrastructureSplines.open(world);
            routes.put(rail(27));routes.save();
            var pkg=new TerrainScenePackage(world);
            var scene=pkg.openScene();
            var keep=new RuntimeScene.Node("keep","Existing scene prop",null,"props",
                RuntimeScene.Transform.identity(),List.of());
            scene.replace(keep);
            var result=TerrainRailScenePackage.publish(world,scene,"mainline");
            check(result.triangles()>100&&result.sleepers()>20,
                "Real track faces and sleeper bodies reach durable scene asset");
            check(Files.isRegularFile(result.geometryFile())
                &&Files.isRegularFile(result.sceneFile())
                &&scene.find(result.nodeId())!=null&&scene.find("keep")!=null,
                "Rail and previous objects share exactly one saved scene");
            world.generateRegion(0,0,1,1);
            var ground=TerrainScenePackage.publish(world,scene,"ground",
                new TerrainEntranceMesh.Bounds(1,1,6,6));
            check(scene.find(ground.nodeId())!=null&&scene.find(result.nodeId())!=null
                &&TerrainScenePackage.inspect(world,scene).isEmpty(),
                "Publishing genuine terrain into the same document preserves the rail node");
            check(TerrainRailScenePackage.inspect(world,scene).isEmpty(),
                "Actual OBJ, hash, source and rail groups verify");
            var persisted=pkg.openScene();
            check(persisted.nodes().equals(scene.nodes()),
                "Reopened terrain scene retains railway and unrelated nodes");
            byte[] first=Files.readAllBytes(result.geometryFile());
            routes.put(rail(29));routes.save();
            check(TerrainRailScenePackage.inspect(world,scene).stream()
                .anyMatch(msg->msg.contains("STALE")),
                "Editing saved spline invalidates previously exported rails");
            var rebuilt=TerrainRailScenePackage.publish(world,scene,"mainline");
            check(!java.util.Arrays.equals(first,Files.readAllBytes(result.geometryFile()))
                &&TerrainRailScenePackage.inspect(world,scene).isEmpty(),
                "Re-publish changes physical geometry without duplicating scene nodes");
            var manifest=Files.readAllBytes(result.sceneFile());
            var actual=Files.readAllBytes(result.geometryFile());
            RuntimeScene refusing=new RuntimeScene(){
                public String sceneId(){return scene.sceneId();}
                public List<Node> nodes(){return scene.nodes();}
                public Node find(String id){return scene.find(id);}
                public void replace(Node node){throw new IllegalStateException("Injected refusal");}
                public void remove(String id){throw new IllegalStateException("Injected refusal");}
                public Snapshot snapshot(){return scene.snapshot();}
                public void restore(Snapshot state){scene.restore(state);}
            };
            boolean failed=false;
            try{TerrainRailScenePackage.publish(world,refusing,"mainline");}
            catch(java.io.IOException expected){failed=true;}
            check(failed&&java.util.Arrays.equals(actual,Files.readAllBytes(result.geometryFile()))
                &&java.util.Arrays.equals(manifest,Files.readAllBytes(result.sceneFile())),
                "Host refusal rolls back asset and manifest bytes");
            failed=false;
            try{TerrainRailScenePackage.publish(world,scene,"../escape");}
            catch(IllegalArgumentException expected){failed=true;}
            check(failed,"Cannot escape world-owned asset folder");
            var other=new TerrainInfrastructureSplines.Alignment("highway",
                TerrainInfrastructureSplines.Kind.ROAD,rail(27).controls(),2,2,8);
            routes.put(other);routes.save();
            failed=false;
            try{TerrainRailScenePackage.publish(world,scene,"highway");}
            catch(java.io.IOException expected){failed=true;}
            check(failed&&!Files.exists(pkg.root().resolve("meshes/rail-highway.obj")),
                "Road cannot masquerade as a rail asset");
            Files.writeString(result.geometryFile(),"manually damaged");
            check(!TerrainRailScenePackage.inspect(world,scene).isEmpty(),
                "Out-of-band mesh corruption is reported");
            TerrainRailScenePackage.publish(world,scene,"mainline");
            check(TerrainRailScenePackage.inspect(world,scene).isEmpty(),
                "Known owned railway can be safely rebuilt");
            check(scene.nodes().size()==3&&rebuilt.nodeId().equals(result.nodeId()),
                "Stable railway scene identity across repeated exports");
            System.out.println("PASS persisted railway scene geometry, shared scene, OBJ/hash/source audits, staleness, rollback and ownership");
        }finally{
            try(var walk=Files.walk(dir)){
                for(var p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
