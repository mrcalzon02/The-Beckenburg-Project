package org.beckenburg;

import org.beckenburg.editor.*;
import java.nio.file.*;
import java.util.*;

/**
 * Real authored rail and terrain OBJ -> existing HCS2 physics engine.
 * Not a preview renderer or a mocked collision implementation.
 */
public final class TerrainSceneNativePhysicsTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static TerrainInfrastructureSplines.Alignment route(double x){
        return new TerrainInfrastructureSplines.Alignment("mainline",
            TerrainInfrastructureSplines.Kind.RAIL,
            List.of(new TerrainLayerStack.Point(4,10,12.0),
                    new TerrainLayerStack.Point(x,10,12.2)),1.55,3,4,true);
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("terrain-runtime-physics");
        try{
            var settings=new StitchedHeightmapWorld.Settings(32,2,0,0,-4,4,42L,4);
            var world=StitchedHeightmapWorld.create(folder.resolve("world"),settings);
            var routeFile=TerrainInfrastructureSplines.open(world);
            routeFile.put(route(27));routeFile.save();
            var scene=new TerrainScenePackage(world).openScene();
            var published=TerrainRailScenePackage.publish(world,scene,"mainline");
            var first=TerrainSceneNativePhysics.load(world,scene);
            check(first.triangles()==EditorMeshPipeline.read(published.geometryFile()).triangles().size(),
                "Physics imports every published mesh triangle without replacement boxes");
            check(first.parts()==4&&first.supportTriangles()>0,
                "Rail ballast is walkable support, steel and sleeper parts are physical");
            double x=32,z=20;
            var raised=first.physics().support(x,z,12.1,1.0);
            check(raised!=null&&raised.y()>11.9&&raised.y()<12.3,
                "Existing HCS2 spatial support queries find correctly wound real ballast");
            check(first.physics().support(x,z,20,.5)==null,
                "No fabricated platform above actual rail geometry");
            check(!first.physics().clearSight(new LevelDefinition.Vec(x,13,z),
                new LevelDefinition.Vec(x,11,z)),
                "Native ray/triangle occlusion intersects physical ballast");
            check(first.physics().clearSight(new LevelDefinition.Vec(x,15,z),
                new LevelDefinition.Vec(x+1,15,z)),
                "Empty space is not obstructed by invented collision slabs");
            var repeat=TerrainSceneNativePhysics.load(world,scene);
            check(first.sourceSha256().equals(repeat.sourceSha256())
                &&first.triangles()==repeat.triangles(),
                "Unmodified scene imports stable deterministic native physics bytes");

            // The very same loaded runtime scene may contain terrain chunks
            // and rail nodes; no separate collision authoring document exists.
            world.generateRegion(0,0,1,1);
            var pkg=new TerrainScenePackage(world);
            pkg.publish(world,scene,"ground",
                new TerrainEntranceMesh.Bounds(1,1,7,7));
            var together=TerrainSceneNativePhysics.load(world,scene);
            check(together.triangles()>first.triangles()&&together.parts()>first.parts(),
                "Terrain and rail geometry share canonical HCS2 physical geometry");
            var controls=TerrainInfrastructureSplines.open(world);
            controls.put(route(29));controls.save();
            boolean rejected=false;
            try{TerrainSceneNativePhysics.load(world,scene);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Stale modified rail source is rejected, not silently used in gameplay");
            TerrainRailScenePackage.publish(world,scene,"mainline");
            check(TerrainSceneNativePhysics.load(world,scene).triangles()>0,
                "Rebuilt railway scene re-enters the canonical runtime physics parser");
            Files.writeString(published.geometryFile(),"tampered");
            rejected=false;
            try{TerrainSceneNativePhysics.load(world,scene);}
            catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Tampered published physical mesh cannot enter the runtime");
            check(!Files.exists(world.directory().resolve("runtime-physics.bin")),
                "Import is in-memory and does not invent mutable shadow collision files");
            System.out.println("PASS exact published terrain+rail OBJ to canonical HCS2 geometry, upward support, optical solids, replay, stale and tamper guards");
        }finally{
            try(var walk=Files.walk(folder)){
                for(var path:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
