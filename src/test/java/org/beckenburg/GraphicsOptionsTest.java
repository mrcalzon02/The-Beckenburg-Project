package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Headless graphics acceptance without requiring proprietary asset packages. */
public final class GraphicsOptionsTest {
    private static void ok(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        GraphicsOptions defaults=GraphicsOptions.current();
        GraphicsOptions wide=new GraphicsOptions(
            GraphicsOptions.Mode.WINDOWED,GraphicsOptions.DisplaySize.FULL_HD,
            GraphicsOptions.Detail.BALANCED,GraphicsOptions.Aspect.WIDE,
            GraphicsOptions.Upscale.CRISP,GraphicsOptions.Antialias.EDGE,95,
            0,0,false,true,false,false);
        GraphicsOptions.Viewport letterbox=wide.viewport(1024,768);
        ok(letterbox.width()==1024&&letterbox.height()==576&&letterbox.y()==96,
            "16:9 viewport letterboxes a 4:3 window");
        GraphicsOptions classic=new GraphicsOptions(
            GraphicsOptions.Mode.WINDOWED,GraphicsOptions.DisplaySize.NATIVE,
            GraphicsOptions.Detail.RETRO,GraphicsOptions.Aspect.CLASSIC,
            GraphicsOptions.Upscale.CRISP,GraphicsOptions.Antialias.OFF,93,
            0,0,false,false,false,false);
        GraphicsOptions.Viewport pillarbox=classic.viewport(1920,1080);
        ok(pillarbox.width()==1440&&pillarbox.x()==240,
            "4:3 viewport pillarboxes a 16:9 display");
        SceneRenderer renderer=new SceneRenderer();
        ok(renderer.width()==320&&renderer.height()==200,"legacy test size preserved");
        renderer.setRenderSize(640,360);
        ok(renderer.width()==640&&renderer.height()==360,
            "real 16:9 RGB framebuffer dimensions");
        ok(renderer.pixels().length==640*360&&renderer.depths().length==640*360,
            "pixel and depth arrays resize with actual rendering");
        renderer.setFieldOfView(95);
        double before=renderer.fieldOfView();
        renderer.setRenderSize(960,540);
        ok(Math.abs(renderer.fieldOfView()-before)<.01,
            "changing resolution does not distort camera field of view");
        ok(renderer.image().getWidth()==960&&renderer.image().getHeight()==540,
            "output image is physically higher-resolution");
        wide.configure(renderer,1920,1080,true);
        ok(renderer.width()==640&&renderer.height()==360,
            "quality target determines actual 3D raster dimensions");
        ok(Math.abs(renderer.fieldOfView()-95)<.01,
            "first scene honors saved FOV instead of retaining the 320x200 default");
        ok(Math.abs(renderer.width()/(double)renderer.height()-16.0/9)<.001,
            "first scene is already the requested aspect, without a stretched frame");
        ok(SceneRenderer.lightingSubdivisionLimit(1)==3
            &&SceneRenderer.lightingSubdivisionLimit(5)==2
            &&SceneRenderer.lightingSubdivisionLimit(12)==1
            &&SceneRenderer.lightingSubdivisionLimit(30)==0,
            "lighting detail tapers with depth, rather than splitting distant walls 256 times");
        for(GraphicsOptions.Detail selected:GraphicsOptions.Detail.values()){
            GraphicsOptions candidate=new GraphicsOptions(
                wide.mode(),wide.display(),selected,wide.aspect(),wide.upscale(),
                wide.antialias(),wide.fieldOfView(),wide.brightness(),wide.contrast(),
                wide.scanlines(),wide.vignette(),wide.grain(),wide.chromatic());
            candidate.configure(renderer,1920,1080,true);
            int expectedHeight=selected==GraphicsOptions.Detail.FULL_HD&&
                (GpuWorldRenderer.active()==null||!GpuWorldRenderer.active().ready())
                ?720:selected.height;
            ok(renderer.height()==expectedHeight,
                "quality presets respect render height and safe software fallback: "+selected);
            ok(renderer.pixels().length==renderer.width()*renderer.height(),
                "all quality choices allocate a matching depth/color target: "+selected);
        }
        int[] scene=new int[8*8];
        Arrays.fill(scene,0x8899aa);
        scene[3*8+3]=0xffffff;
        GraphicsOptions fullHd=new GraphicsOptions(
            wide.mode(),wide.display(),GraphicsOptions.Detail.FULL_HD,
            GraphicsOptions.Aspect.ULTRAWIDE,wide.upscale(),wide.antialias(),
            wide.fieldOfView(),0,0,false,false,false,false);
        fullHd.configure(renderer,3440,1440,true);
        ok(renderer.width()<=1920&&
            Math.abs(renderer.width()/(double)renderer.height()-21.0/9)<.01,
            "ultrawide quality cap preserves projection without stretching");
        ok(Math.abs(CastleHubPanel.hudScale(2048,574)-1.6)<.01 &&
            CastleHubPanel.hudScale(1280,720)==1.0,
            "Castle HUD text scales at 2K width without enlarging low resolution");
        int[] raw=scene.clone();
        GraphicsEffects.apply(scene,8,8,wide,2);
        ok(!Arrays.equals(scene,raw),"edge smoothing or vignette modifies world pixels");
        GraphicsOptions off=new GraphicsOptions(
            defaults.mode(),defaults.display(),defaults.detail(),defaults.aspect(),
            defaults.upscale(),GraphicsOptions.Antialias.OFF,defaults.fieldOfView(),
            0,0,false,false,false,false);
        GraphicsEffects.apply(raw,8,8,off,2);
        ok(raw[0]==0x8899aa,"disabled post-processing leaves image untouched");
        boolean rejected=false;
        try{renderer.setRenderSize(8192,8192);}catch(IllegalArgumentException ex){rejected=true;}
        ok(rejected,"oversized software frame rejected safely");
        BufferedImage output=new BufferedImage(1920,1080,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g=output.createGraphics();
        try{wide.draw(g,renderer.image(),1920,1080);}finally{g.dispose();}
        System.out.println("PASS real render resolution, widescreen, pillarbox, safe dimensions and CPU graphics effects");
    }
}
