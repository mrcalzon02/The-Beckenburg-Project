package org.beckenburg.editor;

import java.nio.file.*;
import java.util.List;

/** Functional portable terrain regression; no Beckenburg game or render dependencies. */
public final class TerrainLayerStackTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void near(double a,double b,String message){
        check(Math.abs(a-b)<.0005,message+": "+a+" vs "+b);
    }
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("portable-terrain-stack");
        try{
            var settings=new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1000,9927L,4);
            var world=StitchedHeightmapWorld.create(root.resolve("world"),settings);
            world.generateRegion(0,0,2,1);
            int seamBefore=world.sample(0,0,32,17);
            var a=new TerrainLayerStack(world,0,0);
            double base=a.height(16,16),edge=a.height(32,17);
            check(a.layerCount()==1,"Default authoring layer");
            a.brush(TerrainLayerStack.Brush.RAISE,16,16,4,10,0,0,1);
            a.finishStroke();
            check(a.height(16,16)>base+9,"Sculpt modifies layer");
            check(world.sample(0,0,16,16)!=-1,"Source remains present");
            near(a.height(32,17),edge,"Border is protected");
            check(a.undo(),"Undo available");
            near(a.height(16,16),base,"Sparse height undo");
            check(a.redo(),"Redo available");
            check(a.height(16,16)>base+9,"Redo reinstates stroke");
            a.addLayer("Noise");
            a.noise(TerrainLayerStack.Noise.SIMPLEX,23L,3,4,3);
            double afterNoise=a.height(13,14);
            a.save();
            check(Files.exists(a.path()),"Layer document persisted");
            var b=new TerrainLayerStack(world,0,0);
            near(b.height(13,14),afterNoise,"Reload retains layered noise");
            b.configureLayer(1,false,1);
            check(Math.abs(b.height(13,14)-afterNoise)>.000001,"Layer visibility changes compose");
            b.configureLayer(1,true,.5);
            check(Double.isFinite(b.height(13,14)),"Opacity composition");
            b.freezeRect(10,10,12,12,true);
            double frozen=b.height(11,11);
            b.brush(TerrainLayerStack.Brush.LOWER,11,11,3,6,0,0,0);
            b.finishStroke();
            near(b.height(11,11),frozen,"Frozen plot is not editable");
            b.freezeRect(10,10,12,12,false);
            b.paintMaterial(16,16,6,TerrainLayerStack.Material.MUD,90);
            check(b.material(16,16)==TerrainLayerStack.Material.MUD.ordinal(),"Material IDs");
            b.autoBiome(-50,500,45);
            b.gradePath(List.of(new TerrainLayerStack.Point(4,4,25),
                new TerrainLayerStack.Point(26,26,30)),2,3);
            near(b.height(16,16),25+5.0*12/22,"Rail grade interpolation");
            b.thermal(2,35,.12);
            b.hydraulic(20,15,91L,.1,.2);
            b.wind(2,1,0,.03);
            float[][] stamp={{0,0,0},{0,8,0},{0,0,0}};
            b.stamp(stamp,18,18,4,1);
            check(Double.isFinite(b.height(18,18)),"All erosion and stamps produce finite terrain");
            b.save();
            var c=new TerrainLayerStack(world,0,0);
            check(c.material(16,16)<TerrainLayerStack.Material.values().length,"Material readback");
            check(world.verifySeams().isEmpty(),"Editor operations cannot alter source seams");
            c.bake();
            check(!Files.exists(c.path()),"Baked layer file is cleared");
            check(world.verifySeams().isEmpty(),"Baked source retained stitched neighbors");
            check(world.sample(0,0,32,17)==seamBefore,"Baked neighbor border unchanged");
            var fresh=new TerrainLayerStack(world,0,0);
            near(fresh.height(16,16),c.height(16,16),"Baked elevations read back");
            System.out.println("PASS portable layer stack, noise, sparse undo, masks, grading, erosion, stamps, persistence and safe bake");
        }finally{
            try(var walk=Files.walk(root)){for(Path p:walk.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);}
        }
    }
}
