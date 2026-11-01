package org.beckenburg.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/**
 * End-to-end regression for managed route surface materials: originals stay
 * unchanged, an explicitly painted GRASS cell wins, and undo/redo/save and
 * route movement update exactly the current corridor. No renderer mocks.
 */
public final class TerrainRouteMaterialOverlayTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static TerrainInfrastructureSplines.Alignment road(double z){
        return new TerrainInfrastructureSplines.Alignment("road-test",
            TerrainInfrastructureSplines.Kind.ROAD,
            List.of(new TerrainLayerStack.Point(4,z,30),
                    new TerrainLayerStack.Point(28,z,30)),
            2,2,12,true);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("route-material-overlay");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,600,713L,4));
            world.generateTile(0,0);
            byte[] source=Files.readAllBytes(world.tilePath(0,0));
            var routes=TerrainInfrastructureSplines.open(world);
            routes.put(road(10));routes.save();
            var terrain=new TerrainLayerStack(world,0,0);
            int grass=TerrainLayerStack.Material.GRASS.ordinal();
            int dirt=TerrainLayerStack.Material.DIRT.ordinal();
            check(terrain.material(15,10)==grass,"Base material was not destructively painted");
            check(terrain.surfaceMaterial(15,10)==dirt,
                "Road visually selects DIRT over unpainted default GRASS");
            check(terrain.managedRouteMaterial(15,10)==dirt,
                "Procedural corridor material is separately available to host");
            check(!terrain.hasPaintedMaterial(15,10),
                "Procedural overlay never sets authored material flags");
            check(terrain.surfaceMaterial(15,19)==grass,
                "Away from corridor default material is unchanged");
            terrain.paintMaterial(15,10,.5,TerrainLayerStack.Material.GRASS,90);
            terrain.finishStroke();
            check(terrain.hasPaintedMaterial(15,10)&&terrain.material(15,10)==grass,
                "Explicit GRASS paint is tracked even when ID equals base default");
            check(terrain.surfaceMaterial(15,10)==grass,
                "Explicit hand-painted GRASS overrides procedural road DIRT");
            check(terrain.undo()&&terrain.surfaceMaterial(15,10)==dirt,
                "Undo hand-painted default reveals road overlay again");
            check(terrain.redo()&&terrain.surfaceMaterial(15,10)==grass,
                "Redo restores the explicit GRASS priority");
            terrain.paintMaterial(17,10,.5,TerrainLayerStack.Material.MUD,90);
            terrain.finishStroke();
            check(terrain.surfaceMaterial(17,10)==TerrainLayerStack.Material.MUD.ordinal(),
                "Explicit non-default paint has priority too");
            terrain.save();
            var reloaded=new TerrainLayerStack(world,0,0);
            check(reloaded.hasPaintedMaterial(15,10)&&
                reloaded.surfaceMaterial(15,10)==grass,
                "V3 material provenance survives save and reopen");
            check(reloaded.surfaceMaterial(16,10)==dirt,
                "Unpainted vertices remain procedural after reopen");

            var revised=TerrainInfrastructureSplines.open(world);
            revised.put(road(23));revised.save();
            reloaded.refreshManagedRoutes(revised.list());
            check(reloaded.surfaceMaterial(16,10)==grass,
                "Moving road restores original material along previous corridor");
            check(reloaded.surfaceMaterial(16,23)==dirt,
                "Moved road paints new material corridor without changing saved paint");
            check(reloaded.surfaceMaterial(15,10)==grass&&
                reloaded.hasPaintedMaterial(15,10)&&
                reloaded.surfaceMaterial(17,10)==TerrainLayerStack.Material.MUD.ordinal(),
                "Moving road preserves explicitly painted GRASS and MUD");
            reloaded.save();
            var afterMove=new TerrainLayerStack(world,0,0);
            check(afterMove.surfaceMaterial(16,23)==dirt&&
                afterMove.surfaceMaterial(16,10)==grass,
                "Saved route geometry and materials agree after reopen");

            revised.remove("road-test");revised.save();
            afterMove.refreshManagedRoutes(revised.list());
            check(afterMove.surfaceMaterial(16,23)==grass &&
                afterMove.managedRouteMaterial(16,23)==-1,
                "Deleting route removes its material overlay without residue");
            check(afterMove.hasPaintedMaterial(15,10)&&
                afterMove.surfaceMaterial(17,10)==TerrainLayerStack.Material.MUD.ordinal(),
                "Deleting route never erases hand painted overrides");
            check(Arrays.equals(source,Files.readAllBytes(world.tilePath(0,0))),
                "Route material changes never modify source R16 pixels");
            check(world.verifySeams().isEmpty(),"No terrain seam changes");

            // A saved v2 file lacked painted-material provenance. Its legacy
            // non-GRASS values are conservatively treated as authored.
            var legacy=StitchedHeightmapWorld.create(root.resolve("v2"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,600,971L,4));
            legacy.generateTile(0,0);
            var old=new TerrainLayerStack(legacy,0,0);
            old.paintMaterial(12,12,.5,TerrainLayerStack.Material.MUD,90);
            old.finishStroke();old.save();
            byte[] v3=Files.readAllBytes(old.path());
            // Exactly one painted sample; remove appended v3 bitset count/index
            // and downgrade only the format header to emulate a real v2 file.
            int countPainted=0;
            for(int z=0;z<=32;z++)for(int x=0;x<=32;x++)
                if(old.hasPaintedMaterial(x,z))countPainted++;
            check(countPainted==1,"Legacy fixture has one painted material");
            byte[] v2=Arrays.copyOf(v3,v3.length-4-countPainted*4);
            ByteBuffer.wrap(v2).putInt(4,2);
            Files.write(old.path(),v2);
            var imported=new TerrainLayerStack(legacy,0,0);
            check(imported.material(12,12)==TerrainLayerStack.Material.MUD.ordinal()
                &&imported.hasPaintedMaterial(12,12),
                "Version-2 non-default material survives migration as authored");
            check(!imported.hasPaintedMaterial(11,11),
                "Version-2 implicit default remains unpainted");
            imported.save();
            check(ByteBuffer.wrap(Files.readAllBytes(imported.path())).getInt(4)==3,
                "Migration writes compatible version-3 terrain document");
            System.out.println("PASS reversible route material surfaces, GRASS provenance, v3 persistence, v2 upgrade, source protection");
        }finally{
            try(var walk=Files.walk(root)){
                for(Path file:walk.sorted(Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(file);
            }
        }
    }
}
