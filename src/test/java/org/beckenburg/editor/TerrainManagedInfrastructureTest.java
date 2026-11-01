package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
/**
 * Managed route grading is derived from persisted spline metadata. Moving or
 * deleting a route must restore old land without erasing unrelated sculpt,
 * material, source pixels, or backwards-compatible legacy route records.
 */
public final class TerrainManagedInfrastructureTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double actual,double expected,String why){
        check(Math.abs(actual-expected)<.002,why+": "+actual+" vs "+expected);
    }
    private static TerrainInfrastructureSplines.Alignment route(String id,double z,
                                      double elevation,boolean managed){
        return new TerrainInfrastructureSplines.Alignment(id,
            TerrainInfrastructureSplines.Kind.ROAD,
            List.of(new TerrainLayerStack.Point(5,z,elevation),
                    new TerrainLayerStack.Point(28,z,elevation)),
            1.5,2.0,12,managed);
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("terrain-managed-spline");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-300,800,47209L,4));
            world.generateTile(0,0);
            byte[] source=Files.readAllBytes(world.tilePath(0,0));
            var terrain=new TerrainLayerStack(world,0,0);
            terrain.brush(TerrainLayerStack.Brush.RAISE,14,10,2,9,0,0,1);
            terrain.finishStroke();
            terrain.paintMaterial(14,11,1.5,TerrainLayerStack.Material.MUD,90);
            terrain.finishStroke();
            terrain.save();
            double original=terrain.height(14,10);
            int customMaterial=terrain.material(14,11);
            check(customMaterial==TerrainLayerStack.Material.MUD.ordinal(),
                "Hand-painted terrain material was saved");

            var routes=TerrainInfrastructureSplines.open(world);
            var old=route("road-a",10,125,true);
            routes.put(old);routes.save();
            var loaded=TerrainInfrastructureSplines.open(world);
            check(loaded.list().size()==1&&loaded.list().get(0).managed(),
                "Version 2 preserves authoritatively managed route mode");
            terrain.refreshManagedRoutes(loaded.list());
            near(terrain.height(14,10),125,
                "First managed route grades actual terrain at the old corridor");
            String oldFinger=TerrainScenePackage.sourceFingerprint(world,
                new TerrainEntranceMesh.Bounds(10,9,26,26));
            var relocated=route("road-a",23,140,true);
            routes.put(relocated);routes.save();
            terrain.refreshManagedRoutes(routes.list());
            near(terrain.height(14,10),original,
                "Route relocation removes ALL old grading residue");
            near(terrain.height(14,23),140,
                "Route relocation grades the new corridor");
            check(terrain.material(14,11)==customMaterial,
                "Moving a route never overwrites hand-painted material");
            check(!oldFinger.equals(TerrainScenePackage.sourceFingerprint(world,
                new TerrainEntranceMesh.Bounds(10,9,26,26))),
                "Route edit invalidates durable source mesh fingerprint");
            var reopened=new TerrainLayerStack(world,0,0);
            near(reopened.height(14,10),original,
                "Old corridor remains restored after closing and reopening terrain workbench");
            near(reopened.height(14,23),140,
                "New corridor is reconstructed from saved spline on reopen");
            boolean refused=false;
            try{reopened.bake();}
            catch(java.io.IOException expected){refused=true;}
            check(refused,"Managed routes cannot be destructively baked into R16 source");

            var sampler=new TerrainComposedHeightSampler(world);
            near(sampler.height(28,46),140,
                "Shared scene mesh/physics sampler includes route even before source bake");
            routes.remove("road-a");routes.save();
            terrain.refreshManagedRoutes(routes.list());
            near(terrain.height(14,10),original,
                "Deleted route does not resurrect original grade");
            near(terrain.height(14,23),world.height(28,46),
                "Deleting managed route restores original source terrain elsewhere");
            check(terrain.material(14,11)==customMaterial,
                "Original material remains intact after route deletion");
            near(new TerrainLayerStack(world,0,0).height(14,10),original,
                "Deleted route does not return after editor reopen");
            check(Arrays.equals(source,Files.readAllBytes(world.tilePath(0,0))),
                "No managed route operation changes authoritative R16 source");
            check(world.verifySeams().isEmpty(),"No source seams were modified");

            // Existing version 1 route files must load as UNMANAGED metadata,
            // never acquire a second automatic grading pass on top of paint.
            Path legacy=root.resolve("legacy");
            var oldWorld=StitchedHeightmapWorld.create(legacy,
                new StitchedHeightmapWorld.Settings(32,2,0,0,-300,800,733L,4));
            oldWorld.generateTile(0,0);
            try(var out=new DataOutputStream(Files.newOutputStream(
                    legacy.resolve("terrain-infrastructure-splines.bin")))){
                out.writeInt(0x5453504c);out.writeInt(1);out.writeInt(1);
                out.writeUTF("legacy-road");out.writeInt(1);
                out.writeDouble(1.5);out.writeDouble(2);out.writeDouble(12);
                out.writeInt(2);
                out.writeDouble(5);out.writeDouble(10);out.writeDouble(125);
                out.writeDouble(28);out.writeDouble(10);out.writeDouble(125);
            }
            var oldLoaded=TerrainInfrastructureSplines.open(oldWorld);
            check(oldLoaded.list().size()==1&&!oldLoaded.list().get(0).managed(),
                "Legacy v1 alignment remains explicitly unmanaged");
            var legacyTerrain=new TerrainLayerStack(oldWorld,0,0);
            near(legacyTerrain.height(14,10),oldWorld.height(28,20),
                "Legacy route does not get a new duplicate grade");
            oldLoaded.save();
            check(!TerrainInfrastructureSplines.open(oldWorld).list().get(0).managed(),
                "Migrating legacy route metadata retains its prior grading semantics");
            System.out.println("PASS managed route edit/delete restores terrain; source, sculpt, materials and v1 untouched");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path p:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
