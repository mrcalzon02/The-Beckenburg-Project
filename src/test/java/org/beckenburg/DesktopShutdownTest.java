package org.beckenburg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Real child-JVM proof: Exit stops renderer and kills lingering non-daemon work. */
public final class DesktopShutdownTest {
    private DesktopShutdownTest(){}
    public static void main(String[] args)throws Exception{
        if(args.length==2&&"--child".equals(args[0])){
            // A leaked non-daemon service would otherwise keep this JVM alive.
            Thread leftover=new Thread(()->{
                try{Thread.sleep(60_000);}
                catch(InterruptedException stop){Thread.currentThread().interrupt();}
            },"simulated-lingering-game-service");
            leftover.setDaemon(false);
            leftover.start();
            Path marker=Path.of(args[1]);
            DesktopShutdown.installRendererCleanup(()->{
                try{Files.writeString(marker,"renderer-closed");}
                catch(IOException failure){throw new IllegalStateException(failure);}
            });
            DesktopShutdown.requestExit();
            DesktopShutdown.requestExit(); // duplicate close events must be harmless
            return;
        }
        if(args.length!=0)throw new IllegalArgumentException("Usage: DesktopShutdownTest");
        Path marker=Files.createTempFile("beckenburg-desktop-exit-",".txt");
        Files.delete(marker);
        String java=Path.of(System.getProperty("java.home"),"bin",
            System.getProperty("os.name","").toLowerCase().contains("win")?"java.exe":"java").toString();
        Process child=new ProcessBuilder(java,"-Djava.awt.headless=true","-cp",
            System.getProperty("java.class.path"),DesktopShutdownTest.class.getName(),
            "--child",marker.toString()).inheritIO().start();
        try{
            if(!child.waitFor(10,TimeUnit.SECONDS))
                throw new AssertionError("Exit left Java alive after the game closed");
            if(child.exitValue()!=0)
                throw new AssertionError("Desktop exited with code "+child.exitValue());
            if(!"renderer-closed".equals(Files.readString(marker)))
                throw new AssertionError("OpenGL cleanup did not run exactly once");
        }finally{
            if(child.isAlive())child.destroyForcibly();
            Files.deleteIfExists(marker);
        }
        System.out.println("PASS DesktopShutdownTest: renderer disposed; lingering JVM terminated");
    }
}
