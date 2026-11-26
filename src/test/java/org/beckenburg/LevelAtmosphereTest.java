package org.beckenburg;

import java.util.Arrays;

/** Depth/sky controls, including a synthetic GPU readback composite (not GL). */
public final class LevelAtmosphereTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        int fog=0x121820;var atmosphere=new LevelAtmosphere();
        int[] pixels={0xff0000,0xff0000,0xff0000,0x123456};
        double[] depth={2,16,40,Double.POSITIVE_INFINITY},before=depth.clone();
        try(var pass=LevelAtmosphere.enter(4,1,100,1,fog)){
            for(int x=0;x<4;x++)if(Double.isFinite(depth[x]))pixels[x]=LevelAtmosphere.shadePixel(pixels[x],depth[x],x,0);
        }
        var frame=atmosphere.inspect(depth,4,1,100,1,fog);
        check(pixels[0]==0xff0000,"Near surfaces must retain their original colour");
        check(pixels[1]!=0xff0000&&pixels[1]!=fog,"Middle distance must blend continuously");
        check(pixels[2]==fog,"Distant geometry must fully converge before the level edge");
        check(pixels[3]==0x123456,"Depth-clear sky must survive foreground fog composition");
        check(Arrays.equals(before,depth),"Fog must not alter depth/occlusion authority");
        check(frame.opaqueMetres()==24&&LevelAtmosphere.opaqueMetres(0)==24
            &&LevelAtmosphere.opaqueMetres(2)==15,"All weather retains the heavy baseline");
        int[] rays=new int[5];Arrays.fill(rays,0xffffff);double[] plane=new double[5];Arrays.fill(plane,20);
        try(var pass=LevelAtmosphere.enter(5,1,2,1,fog)){for(int x=0;x<5;x++)rays[x]=LevelAtmosphere.shadePixel(rays[x],plane[x],x,0);}
        check(rays[0]==fog&&rays[2]!=fog,"Fade uses radial distance rather than a camera-facing wall");
        int[] sky=new int[20*20];Arrays.fill(sky,0x304060);
        LevelAtmosphere.skyboxHaze(sky,20,20,10,0,fog);
        check(sky[19*20]==fog&&sky[0]!=fog,"Skybox horizon and lower hemisphere must meet the fog colour");
        int[] actor=new int[320*200];double[] background=new double[actor.length];Arrays.fill(background,40);
        try(var pass=LevelAtmosphere.enter(320,200,153,1,fog)){
            Arrays.fill(actor,fog);
            DirectionalSprites.draw(actor,background,0,0,0,0,0,2,0,"banshee",1.8,1,0,128,true,320,200,153,false,LanGhostAnimation.Phase.LURK,0);
            check(Arrays.stream(actor).filter(c->c!=fog).count()>20,"Nearby translucent ghost disappeared against a fogged background");
            check(Arrays.stream(background).allMatch(d->d==40),"Transparent ghost must not acquire opaque depth");
            Arrays.fill(actor,fog);
            DirectionalSprites.draw(actor,background,0,0,0,0,0,35,0,"banshee",1.8,1,0,128,true,320,200,153,false,LanGhostAnimation.Phase.LURK,0);
            check(Arrays.stream(actor).allMatch(c->c==fog),"Distant translucent ghost bypasses fog");
        }
        check(!LevelAtmosphere.active(),"Atmosphere scope leaked into later reviews");
        if(args.length>0&&args[0].equals("--gpu-composite-control")){
            GpuWorldRenderer.install(new GpuWorldRenderer(){
                Result latest;
                public boolean ready(){return true;}
                public void submit(Frame f){check(f.atmosphere()!=null,"Runtime GPU frame lacks shared atmosphere uniforms");int[] rgb=new int[f.width()*f.height()];float[] z=new float[rgb.length];Arrays.fill(rgb,0xffff0000);Arrays.fill(z,80);latest=new Result(f.sceneId(),f.frameId(),f.width(),f.height(),rgb,z);}
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
