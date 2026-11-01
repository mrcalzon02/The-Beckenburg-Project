package org.beckenburg.editor;

import java.nio.file.*;
/** Portable, source-independent brush preset persistence and mask integrity. */
public final class TerrainBrushPresetsTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    private static void near(double a,double b,String message){
        check(Math.abs(a-b)<.00001,message+": "+a+" != "+b);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("brush-presets");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("world"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-100,400,5L,2));
            var presets=new TerrainBrushPresets(world);
            var settings=new TerrainBrushProfile.Settings(9,7.5,21,.15,2,.75,35,
                -9,150,63,TerrainLayerStack.FlattenLimit.LOWER_ONLY);
            var alpha=new TerrainBrushProfile.Alpha(new float[][]{
                {0,0,.5f},{.2f,1,.4f},{0,.7f,0}});
            var preset=new TerrainBrushPresets.Preset(
                TerrainLayerStack.Brush.TERRACE,settings,alpha);
            presets.save("granite-cliff",preset,false);
            check(presets.list().equals(java.util.List.of("granite-cliff")),"Presets enumerated");
            var loaded=presets.open("granite-cliff");
            check(loaded.brush()==TerrainLayerStack.Brush.TERRACE,"Brush tool survived");
            check(loaded.settings().equals(settings),"All numeric and enum settings survived");
            for(double x:new double[]{-1,-.6,0,.25,.7,1})
                for(double y:new double[]{-1,-.5,.33,1})
                    near(alpha.sample(x,y,35),loaded.alpha().sample(x,y,35),
                        "Embedded alpha retains bilinear data");
            boolean duplicateRejected=false;
            try{presets.save("granite-cliff",preset,false);}
            catch(java.io.IOException expected){duplicateRejected=true;}
            check(duplicateRejected,"Overwrite is explicitly gated");
            presets.save("granite-cliff",preset,true);
            check(presets.delete("granite-cliff"),"Preset deletion");
            check(presets.list().isEmpty(),"Deleted preset is not enumerated");
            boolean unsafeRejected=false;
            try{presets.open("../outside");}
            catch(IllegalArgumentException expected){unsafeRejected=true;}
            check(unsafeRejected,"Preset paths reject traversal");
            System.out.println("PASS portable preset readback, embedded alpha, overwrite protection, safe names");
        }finally{
            try(var paths=Files.walk(root)){
                for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
