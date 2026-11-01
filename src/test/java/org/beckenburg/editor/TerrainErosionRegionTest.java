package org.beckenburg.editor;

import java.nio.file.*;
/** Source-independent functional scope checks for localized terrain solvers. */
public final class TerrainErosionRegionTest {
    private static void check(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private static void near(double a,double b,String message){
        check(Math.abs(a-b)<.0005,message+": "+a+" vs "+b);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("erosion-region-test");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,500,81923L,4));
            world.generateRegion(0,0,2,1);
            var stack=new TerrainLayerStack(world,0,0);
            var selected=TerrainLayerStack.Region.around(16,16,4,stack.cells());
            check(selected.contains(16,16)&&!selected.contains(2,2),
                "Localized selection is spatially bounded");
            double corner=stack.height(2,2),other=stack.height(27,27),
                seam=stack.height(32,16);
            stack.brush(TerrainLayerStack.Brush.RAISE,16,16,3,30,0,0,1);
            stack.finishStroke();
            stack.thermal(15,25,.20,selected);
            stack.hydraulic(800,24,88L,.1,.25,selected);
            stack.wind(12,1,0,.12,selected);
            near(stack.height(2,2),corner,"Thermal/hydraulic/wind cannot change terrain outside region");
            near(stack.height(27,27),other,"Opposite region stays unchanged");
            near(stack.height(32,16),seam,"Shared terrain border unchanged");
            check(world.verifySeams().isEmpty(),"Source seams stay untouched");
            check(stack.undo(),"Localized erosion creates undo history");
            stack.save();
            var reopened=new TerrainLayerStack(world,0,0);
            near(reopened.height(2,2),corner,"Localized save/open preserves untouched vertices");
            boolean rejected=false;
            try{stack.wind(1,1,0,.1,new TerrainLayerStack.Region(0,0,4,4));}
            catch(IllegalArgumentException correct){rejected=true;}
            check(rejected,"Attempt to edit source seam through region is rejected");
            rejected=false;
            try{TerrainLayerStack.Region.around(1000,1000,5,32);}
            catch(IllegalArgumentException correct){rejected=true;}
            check(rejected,"Out-of-bounds erosion selector rejected");
            System.out.println("PASS localized thermal, hydraulic, wind erosion and seam protection");
        }finally{
            try(var paths=Files.walk(root)){
                for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(p);
            }
        }
    }
}
