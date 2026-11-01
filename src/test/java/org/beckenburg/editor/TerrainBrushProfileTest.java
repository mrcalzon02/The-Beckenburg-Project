package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.ArrayList;

/** Verifies true alpha sampling, spacing and non-destructive brush constraints. */
public final class TerrainBrushProfileTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static void near(double a,double b,String why){
        check(Math.abs(a-b)<.0005,why+": "+a+" / "+b);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("terrain-precision-brush");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("w"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,500,197L,4));
            world.generateTile(0,0);
            var terrain=new TerrainLayerStack(world,0,0);
            double baseline=terrain.height(16,16);
            var stripe=new TerrainBrushProfile.Alpha(new float[][]{
                {0,1,0},{0,1,0},{0,1,0}});
            near(stripe.sample(0,.5,0),1,"Vertical alpha center");
            near(stripe.sample(.5,0,0),.5,"Horizontal alpha edge fade");
            near(stripe.sample(.5,0,90),1,"90 degree rotation maps horizontal to stripe");
            near(stripe.sample(1.5,0,0),0,"No sampling outside source mask");
            var profile=new TerrainBrushProfile.Settings(5,8,0,.1,1,2,0,
                -Double.MAX_VALUE,Double.MAX_VALUE,90,
                TerrainLayerStack.FlattenLimit.BOTH);
            terrain.brushPrecision(TerrainLayerStack.Brush.RAISE,16,16,profile,stripe);
            terrain.finishStroke();
            check(terrain.height(16,17)>baseline+0,"Masked brush modifies white region");
            check(terrain.undo(),"Masked dab is undoable");
            near(terrain.height(16,16),baseline,"Mask undo retains original sampled height");
            check(terrain.redo(),"Masked dab is redoable");
            terrain.undo();
            double protectedHeight=terrain.height(16,16);
            terrain.freezeRect(16,16,16,16,true);
            terrain.brushPrecision(TerrainLayerStack.Brush.RAISE,16,16,profile,stripe);
            terrain.finishStroke();
            near(terrain.height(16,16),protectedHeight,"Frozen mask overrides alpha brush");
            terrain.undo();
            terrain.freezeRect(16,16,16,16,false);
            var excluded=new TerrainBrushProfile.Settings(5,8,0,.1,1,2,0,
                1_000_000,2_000_000,90,TerrainLayerStack.FlattenLimit.BOTH);
            terrain.brushPrecision(TerrainLayerStack.Brush.RAISE,16,16,excluded,stripe);
            terrain.finishStroke();
            near(terrain.height(16,16),protectedHeight,"Elevation exclusion prevents sculpt");
            var zeroSlope=new TerrainBrushProfile.Settings(5,8,0,.1,1,2,0,
                -1_000_000,1_000_000,0,TerrainLayerStack.FlattenLimit.BOTH);
            double before=terrain.height(16,16);
            terrain.brushPrecision(TerrainLayerStack.Brush.RAISE,16,16,zeroSlope,stripe);
            terrain.finishStroke();
            near(terrain.height(16,16),before,"Slope exclusion prevents sculpt on uneven terrain");
            var locations=new ArrayList<Double>();
            var stroke=new TerrainBrushProfile.Stroke(2,(x,z)->locations.add(x));
            stroke.begin(0,0);stroke.move(5,0);stroke.move(7,0);stroke.end();
            check(locations.size()==4,"Stroke spacing is independent of mouse event count");
            near(locations.get(0),0,"First dab");
            near(locations.get(1),2,"Second dab");
            near(locations.get(2),4,"Third dab");
            near(locations.get(3),6,"Fourth dab");
            BufferedImage mask=new BufferedImage(4,4,BufferedImage.TYPE_USHORT_GRAY);
            mask.getRaster().setSample(1,1,0,65535);
            var decoded=TerrainBrushProfile.Alpha.fromImage(mask);
            check(decoded.sample(-.333333,-.333333,0)>.99,
                "16-bit grayscale alpha normalizes to full strength");
            check(decoded.sample(1,1,0)==0,"Black alpha prevents changes");
            check(world.verifySeams().isEmpty(),"Precision operations never change source");
            System.out.println("PASS brush image alpha, rotation, spacing, height/slope exclusions, freeze, sparse undo");
        }finally{
            try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
