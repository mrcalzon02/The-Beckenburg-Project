package org.beckenburg.gpu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import java.util.concurrent.TimeUnit;
import org.beckenburg.*;

/** Hidden real OpenGL context only; never opens the game or editor UI. */
public final class LevelAtmosphereGpuCapture {
    public static void main(String[] args)throws Exception{
        var engine=new OpenGlWorldRenderer();var config=new Lwjgl3ApplicationConfiguration();
        config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,3);
        config.setWindowedMode(640,400);config.setInitialVisible(false);config.disableAudio(true);config.useVsync(false);config.setForegroundFPS(0);config.setIdleFPS(240);
        Thread worker=new Thread(()->new Lwjgl3Application(engine,config),"atmosphere-review-opengl");worker.start();
        try{
            System.out.println("ACTUAL_OPENGL "+engine.initialization().get(30,TimeUnit.SECONDS));GpuWorldRenderer.install(engine);
            float[] vertices={-60,-40,40,0,0,1,1,1, 60,-40,40,1,0,1,1,1, 60,40,40,1,1,1,1,1,
                -60,-40,40,0,0,1,1,1, 60,40,40,1,1,1,1,1, -60,40,40,0,1,1,1,1};
            long id=GpuWorldRenderer.nextId();
            engine.submit(new GpuWorldRenderer.Frame(id,1,320,200,153,0,0,java.util.List.of(new GpuWorldRenderer.Surface(-1,vertices))));
            if(!GpuWorldRenderer.awaitPreparedFrame(engine,id,1,30_000_000_000L,()->false))throw new AssertionError("No real shader control readback");
            var unfogged=engine.latest();float originalDepth=unfogged.depth()[100*320+160];
            if((unfogged.argb()[100*320+160]&0xffffff)==0x121820||!Float.isFinite(originalDepth)||originalDepth<24)throw new AssertionError("Invalid unfogged far-surface control");
            engine.submit(new GpuWorldRenderer.Frame(id,2,320,200,153,0,0,java.util.List.of(new GpuWorldRenderer.Surface(-1,vertices)),false,0,0,0,0,0,false,0,1,1,false,new LevelAtmosphere.Policy(0x121820,6,24)));
            if(!GpuWorldRenderer.awaitPreparedFrame(engine,id,2,30_000_000_000L,()->false))throw new AssertionError("No real fog shader readback");
            var result=engine.latest();int pixel=result.argb()[100*320+160]&0xffffff;
            if(pixel!=0x121820||Float.floatToIntBits(result.depth()[100*320+160])!=Float.floatToIntBits(originalDepth))throw new AssertionError("Actual OpenGL fragment fog changed paired depth: "+Integer.toHexString(pixel));
            System.out.println("PASS actual OpenGL far-fog colour and BIT_IDENTICAL paired depth; authored40mplane readback="+originalDepth);
            AllLevelAtmosphereCapture.main(args);
        }finally{if(Gdx.app!=null)Gdx.app.exit();worker.join(15000);if(worker.isAlive())throw new AssertionError("OpenGL worker did not stop");}
    }
}
