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
            AllLevelAtmosphereCapture.main(args);
        }finally{if(Gdx.app!=null)Gdx.app.exit();worker.join(15000);if(worker.isAlive())throw new AssertionError("OpenGL worker did not stop");}
    }
}
