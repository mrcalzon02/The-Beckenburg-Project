package org.beckenburg;

import java.util.Arrays;

/** Depth/sky controls, including a synthetic GPU readback composite (not GL). */
public final class LevelAtmosphereTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        int fog=0x121820;var atmosphere=new LevelAtmosphere();
        int[] pixels={0xff0000,0xff0000,0xff0000,0x123456};
        double[] depth={2,16,40,Double.POSITIVE_INFINITY},before=depth.clone();
        var frame=atmosphere.apply(pixels,depth,4,1,100,1,fog);
        check(pixels[0]==0xff0000,"Near surfaces must retain their original colour");
        check(pixels[1]!=0xff0000&&pixels[1]!=fog,"Middle distance must blend continuously");
        check(pixels[2]==fog,"Distant geometry must fully converge before the level edge");
        check(pixels[3]==0x123456,"Depth-clear sky must survive foreground fog composition");
        check(Arrays.equals(before,depth),"Fog must not alter depth/occlusion authority");
        check(frame.opaqueMetres()==24&&LevelAtmosphere.opaqueMetres(0)==24
            &&LevelAtmosphere.opaqueMetres(2)==15,"All weather retains the heavy baseline");
        int[] rays=new int[5];Arrays.fill(rays,0xffffff);double[] plane=new double[5];Arrays.fill(plane,20);
        atmosphere.apply(rays,plane,5,1,2,1,fog);
        check(rays[0]==fog&&rays[2]!=fog,"Fade uses radial distance rather than a camera-facing wall");
        int[] sky=new int[20*20];Arrays.fill(sky,0x304060);
        LevelAtmosphere.skyboxHaze(sky,20,20,10,0,fog);
        check(sky[19*20]==fog&&sky[0]!=fog,"Skybox horizon and lower hemisphere must meet the fog colour");
        if(args.length>0&&args[0].equals("--gpu-composite-control")){
            GpuWorldRenderer.install(new GpuWorldRenderer(){
                Result latest;
                public boolean ready(){return true;}
                public void submit(Frame f){int[] rgb=new int[f.width()*f.height()];float[] z=new float[rgb.length];Arrays.fill(rgb,0xffff0000);Arrays.fill(z,80);latest=new Result(f.sceneId(),f.frameId(),f.width(),f.height(),rgb,z);}
                public Result latest(){return latest;}
                public String rendererDescription(){return "SYNTHETIC GPU COMPOSITE CONTROL; no OpenGL";}
            });
            var scene=new SceneRenderer();scene.setPsxPreset(PsxPipeline.Preset.OFF);
            scene.x=24;scene.z=22;scene.eyeHeight=1.62;scene.drawCastle(new CastleHubMap(),0,true);
            check(scene.atmosphereFrame().foggedPixels()>SceneRenderer.W*SceneRenderer.H/2,
                "Completed GPU depth must go through the same atmosphere pass");
        }
        System.out.println("PASS heavy baseline fog, radial depth, near preservation, sky/depth isolation"+(args.length>0?"; synthetic GPU composite control":""));
    }
}
