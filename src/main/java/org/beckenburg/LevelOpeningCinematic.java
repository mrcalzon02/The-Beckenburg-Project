package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nonblocking case-start presentation. The real player scene is never moved.
 * Every backend receives the SAME world-space 3D camera flyover. A bounded,
 * low-resolution SOFTWARE render runs off the Swing EDT even if OpenGL is
 * unavailable. An unready preview shows a loading message, NEVER a flat map.
 */
final class LevelOpeningCinematic implements AutoCloseable {
    private final HouseMap map;
    private final LevelOpeningFlyover flight;
    private final ExecutorService worker;
    private final SceneRenderer preview;
    private final AtomicBoolean busy=new AtomicBoolean();
    private final AtomicBoolean firstPreviewLogged=new AtomicBoolean();
    private final AtomicBoolean readinessFallbackLogged=new AtomicBoolean();
    private volatile long firstPreviewRequestNs,firstPreviewFrameNs;
    private volatile int previewRequests,previewFailures;
    private volatile BufferedImage completed;
    private final boolean gpuFlight;
    private static final long MAX_3D_PREVIEW_WAIT_NS=
        java.util.concurrent.TimeUnit.SECONDS.toNanos(7);
    private final long started=System.nanoTime();
    private final OpeningPlaybackClock playback=
        new OpeningPlaybackClock(started,MAX_3D_PREVIEW_WAIT_NS);
    private long lastRequest;
    private volatile boolean closed;
    /** Non-rendering case-start readiness evidence; elapsed time starts at
     * cinematic construction. No actor ticks, cameras or case state change. */
    record PreviewStatus(boolean gpuRequested,boolean workerBusy,
        boolean firstFrameReady,int requests,int failures,
        long firstFrameElapsedMs){}
    PreviewStatus previewStatus(){
        long elapsed=firstPreviewFrameNs>0
            ?java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(firstPreviewFrameNs-started)
            :-1;
        return new PreviewStatus(gpuFlight,busy.get(),completed!=null,
            previewRequests,previewFailures,elapsed);
    }
    LevelOpeningCinematic(HouseMap map,SceneRenderer player,int width,int height){
        this.map=map;
        flight=new LevelOpeningFlyover(map,new LevelOpeningFlyover.Pose(
            player.x,player.eyeHeight,player.z,player.yaw,player.pitch));
        var backend=GpuWorldRenderer.active();
        gpuFlight=backend!=null&&backend.ready();
        // The cinematic is a real scene on EVERY installation. Using an
        // independent synchronous software renderer avoids competing with the
        // game's OpenGL context while a separate worker renders 3D frames.
        preview=new SceneRenderer();
        preview.photographicSoftwareRaster();
        preview.roomLights=player.roomLights;
        worker=Executors.newSingleThreadExecutor(task->{
            Thread thread=new Thread(task,"Beckenburg-Opening-World3D");
            thread.setDaemon(true);
            return thread;
        });
        BeckenburgDiagnostics.record("OPENING WORLD3D "+map.site()
            +" hardware_available="+gpuFlight+" preview=async-software-world"
            +" output="+width+"x"+height+" first_frame_state=not-yet-ready");
    }
    boolean gpuFlight(){return gpuFlight;}
    boolean uses3DWorldCamera(){return true;}
    BufferedImage latest3DFrame(){return completed;}
    double progress(){
        double t=playback.progress(System.nanoTime(),LevelOpeningFlyover.DURATION_SECONDS,true);
        if(playback.readinessTimedOut()
                &&readinessFallbackLogged.compareAndSet(false,true)){
            BeckenburgDiagnostics.record("OPENING WORLD3D FRAME TIMEOUT site="+map.site()
                +" max_wait_ms="+java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                    MAX_3D_PREVIEW_WAIT_NS)
                +" fallback=plain-loading-status no-flat-map no-case-block=true");
        }
        return t;
    }
    boolean complete(){return closed||progress()>=1;}
    void update(int width,int height){
        if(closed||progress()>=1)return;
        long now=System.nanoTime();
        if(now-lastRequest<160_000_000L||!busy.compareAndSet(false,true))return;
        lastRequest=now;
        previewRequests++;
        boolean firstAttempt=firstPreviewRequestNs==0;
        if(firstAttempt){
            firstPreviewRequestNs=now;
            BeckenburgDiagnostics.record("OPENING GPU FIRST FRAME REQUEST site="+map.site()
                +" width="+width+" height="+height);
        }
        var pose=flight.pose(progress());
        worker.execute(()->{
            try{
                if(closed)return;
                pose.apply(preview);
                // Render an actual perspective 3D world, at an intentional
                // preview-only size. Never bake a 720/1080p CPU framebuffer
                // that starves the game while opening a case.
                int h=Math.min(300,Math.max(180,GraphicsOptions.current().detail().height));
                int w=Math.max(320,Math.min(640,(int)Math.round(
                    h*Math.max(1,width)/(double)Math.max(1,height))));
                preview.setRenderSize(w,h);
                preview.setFieldOfView(GraphicsOptions.current().fieldOfView());
                preview.setPsxPreset(PsxPipeline.selected());
                preview.drawPractice(map,List.of(),progress()*7.5);
                if(closed)return;
                var source=preview.image();
                BufferedImage copy=new BufferedImage(source.getWidth(),source.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=copy.createGraphics();
                try{graphics.drawImage(source,0,0,null);}
                finally{graphics.dispose();}
                completed=copy;
                if(firstPreviewLogged.compareAndSet(false,true)){
                    firstPreviewFrameNs=System.nanoTime();
                    playback.previewPrepared(firstPreviewFrameNs);
                    long elapsedMs=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        firstPreviewFrameNs-started);
                    long requestMs=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                        firstPreviewFrameNs-firstPreviewRequestNs);
                    BeckenburgDiagnostics.record("OPENING GPU FIRST FRAME READY site="+map.site()
                        +" startup_elapsed_ms="+elapsedMs+" render_request_ms="+requestMs
                        +" image="+source.getWidth()+"x"+source.getHeight());
                }
            }catch(Throwable problem){
                previewFailures++;
                // Keep the game responsive and preserve stage evidence; a
                // cinematic failure must never prevent entering the mission.
                BeckenburgDiagnostics.fail("Opening aerial renderer",problem);
            }finally{busy.set(false);}
        });
    }
    void draw(Graphics2D graphics,int width,int height){
        double t=progress();
        BufferedImage image=completed;
        if(image!=null){
            GraphicsOptions.current().draw(graphics,image,width,height);
        }else{
            // An unavailable renderer must not turn the real approach into
            // the previously regressed 2D tactical zoom. Keep the window
            // responsive and allow a bounded timeout or user skip.
            graphics.setColor(new Color(10,18,23));
            graphics.fillRect(0,0,width,height);
            graphics.setColor(new Color(163,190,187));
            graphics.setFont(new Font(Font.MONOSPACED,Font.BOLD,Math.max(13,width/55)));
            graphics.drawString("PREPARING WORLD-SPACE 3D APPROACH",
                Math.max(18,width/12),height/2);
        }
        int band=Math.max(34,height/13);
        graphics.setColor(new Color(4,8,14,232));
        graphics.fillRect(0,0,width,band);
        graphics.fillRect(0,height-band,width,band);
        graphics.setFont(new Font(Font.MONOSPACED,Font.BOLD,Math.max(12,Math.min(19,width/50))));
        graphics.setColor(new Color(235,218,172));
        graphics.drawString("THE BECKENBURG PROJECT  /  EXPEDITION APPROACH",22,band-12);
        graphics.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        graphics.setColor(new Color(194,221,215));
        String phase=image==null?"BUILDING 3D WORLD":t<.39?"AERIAL SURVEY"
            :t<.78?"DESCENDING TO SITE":t<.93
                ?"EXPEDITION VAN INBOUND":"HUNTER CAMERA HANDOFF";
        graphics.drawString(phase+"  /  "+map.level().title(),22,height-band+18);
        graphics.drawString("SPACE / ENTER / ESC TO SKIP",Math.max(22,width-236),
            height-band+18);
        int length=Math.max(30,width-42);
        graphics.setColor(new Color(37,55,62));
        graphics.fillRect(21,height-band+25,length,4);
        graphics.setColor(new Color(131,215,191));
        graphics.fillRect(21,height-band+25,(int)(length*t),4);
    }
    @Override public void close(){
        if(closed)return;
        closed=true;
        var status=previewStatus();
        BeckenburgDiagnostics.record("OPENING WORLD3D CLOSE site="+map.site()
            +" first_frame_ready="+status.firstFrameReady()
            +" worker_busy="+status.workerBusy()
            +" requests="+status.requests()+" failures="+status.failures()
            +" first_frame_ms="+status.firstFrameElapsedMs()
            +" outcome="+(status.firstFrameReady()?"world-3d-ready":
                status.workerBusy()?"cancelled-while-preparing":"no-frame-but-bounded"));
        worker.shutdownNow();
    }
}
