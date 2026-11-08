package org.beckenburg;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A desktop Exit must end the whole JVM, not just dispose the visible Swing
 * window while the hidden OpenGL worker or a network thread remains alive.
 * Do not use for the dedicated server; it owns its own console lifecycle.
 */
public final class DesktopShutdown {
    private static final AtomicBoolean REQUESTED=new AtomicBoolean();
    private static volatile Runnable rendererCleanup=()->{};
    private DesktopShutdown(){}

    /** GPU launcher registers its native-worker teardown after initialization. */
    public static void installRendererCleanup(Runnable cleanup){
        rendererCleanup=Objects.requireNonNull(cleanup,"cleanup");
    }
    public static void requestExit(){requestExit(0);}
    public static void requestExit(int requestedStatus){
        if(!REQUESTED.compareAndSet(false,true))return;
        // Waiting for GLFW on the Swing EDT would freeze the closing UI.
        Thread thread=new Thread(()->{
            int status=requestedStatus;
            try{
                BeckenburgDiagnostics.record("DESKTOP EXIT: stopping renderer");
                rendererCleanup.run();
                BeckenburgDiagnostics.record("DESKTOP EXIT: renderer stopped");
            }catch(Throwable failure){
                status=1;
                BeckenburgDiagnostics.fail("Desktop OpenGL termination",failure);
            }finally{
                BeckenburgDiagnostics.record("DESKTOP EXIT: process termination code="+status);
                // The GL worker has been given up to 15 seconds to stop on
                // its own thread. Even a failed native teardown must not
                // leave an immortal background Java process.
                System.exit(status);
            }
        },"beckenburg-desktop-shutdown");
        thread.setDaemon(false);
        thread.start();
    }
}
