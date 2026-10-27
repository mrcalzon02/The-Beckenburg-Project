package org.beckenburg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Nonblocking case-start presentation. The real player scene is never moved.
 * GPU-first installs receive the world-space 3D flyover from a dedicated
 * renderer. The deliberate software recovery path uses a cheap animated
 * overhead map instead of locking the Swing EDT on 200k+ cold shadow rays.
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
    private static final long MAX_GPU_PREVIEW_WAIT_NS=
        java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
    private final long started=System.nanoTime();
    private final OpeningPlaybackClock playback=
        new OpeningPlaybackClock(started,MAX_GPU_PREVIEW_WAIT_NS);
    private long lastRequest;
    private volatile boolean closed;
    private final double centerX,centerZ,span;
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
        if(gpuFlight){
            preview=new SceneRenderer();
            preview.roomLights=player.roomLights;
            worker=Executors.newSingleThreadExecutor(task->{
                Thread thread=new Thread(task,"Beckenburg-Opening-Flyover");
                thread.setDaemon(true);
                return thread;
            });
        }else{
            preview=null;worker=null;
        }
        var framing=LevelOpeningFlyover.framing(map);
        centerX=framing.cx();centerZ=framing.cz();span=framing.span();
        BeckenburgDiagnostics.record("OPENING FLIGHT "+map.site()
            +" backend="+(gpuFlight?"GPU world-space":"software tactical overview")
            +" output="+width+"x"+height+" first_frame_state="+
            (gpuFlight?"not-yet-ready":"software-fallback-ready"));
    }
    boolean gpuFlight(){return gpuFlight;}
    double progress(){
        double t=playback.progress(System.nanoTime(),LevelOpeningFlyover.DURATION_SECONDS,gpuFlight);
        if(gpuFlight&&playback.readinessTimedOut()
                &&readinessFallbackLogged.compareAndSet(false,true)){
            BeckenburgDiagnostics.record("OPENING GPU PREVIEW READINESS TIMEOUT site="+map.site()
                +" max_wait_ms="+java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                    MAX_GPU_PREVIEW_WAIT_NS)
                +" fallback=existing-tactical-overview no-case-block=true");
        }
        return t;
    }
    boolean complete(){return closed||progress()>=1;}
    void update(int width,int height){
        if(closed||!gpuFlight||progress()>=1)return;
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
                GraphicsOptions.current().configure(preview,
                    Math.max(760,width),Math.max(510,height),true);
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
        if(gpuFlight&&image!=null){
            GraphicsOptions.current().draw(graphics,image,width,height);
        }else{
            drawFallbackMap(graphics,width,height,t);
        }
        int band=Math.max(34,height/13);
        graphics.setColor(new Color(4,8,14,232));
        graphics.fillRect(0,0,width,band);
        graphics.fillRect(0,height-band,width,band);
        graphics.setFont(new Font(Font.MONOSPACED,Font.BOLD,Math.max(12,Math.min(19,width/50))));
        graphics.setColor(new Color(235,218,172));
        graphics.drawString("CASTLE BECKENBURG  /  EXPEDITION APPROACH",22,band-12);
        graphics.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        graphics.setColor(new Color(194,221,215));
        String phase=t<.39?"AERIAL SURVEY":t<.78?"DESCENDING TO SITE":t<.93
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
    private void drawFallbackMap(Graphics2D graphics,int width,int height,double t){
        graphics.setColor(new Color(13,24,29));
        graphics.fillRect(0,0,width,height);
        var dock=map.van().placement();
        double orbit=LevelOpeningFlyover.DURATION_SECONDS>0
            ?Math.max(0,Math.min(1,t)) : 0;
        double eased=orbit*orbit*(3-2*orbit);
        double focusX=centerX+(dock.x()-centerX)*eased;
        double focusZ=centerZ+(dock.z()-centerZ)*eased;
        double scale=Math.min(width,height)*(.64+.62*eased)/span;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(width/2.0,height/2.0);
            g.scale(scale,scale);
            g.translate(-focusX,-focusZ);
            // Rooms are *authored* outlines; the overview never guesses
            // secret entities or makes undiscovered evidence visible.
            for(var room:map.level().rooms()){
                if(map.nativeSite()){
                    if(room.id()>=HouseMap.INDOOR_ROOMS)continue;
                    g.setColor(new Color(164,172,166));
                    g.fill(new java.awt.geom.Ellipse2D.Double(room.cx()-.5,room.cz()-.5,1,1));
                    continue; // native outdoor zones are anchors, not rectangular buildings
                }
                if(room.outside())continue;
                int x=(int)Math.round(room.x0()),z=(int)Math.round(room.z0());
                int rw=Math.max(1,(int)Math.round(room.x1()-room.x0())),
                    rh=Math.max(1,(int)Math.round(room.z1()-room.z0()));
                g.setColor(new Color(62,71,73,205));
                g.fillRect(x,z,rw,rh);
                g.setColor(new Color(164,172,166));
                g.setStroke(new BasicStroke((float)(.10/Math.max(.1,eased+.2))));
                g.drawRect(x,z,rw,rh);
            }
            g.translate(dock.x(),dock.z());
            g.rotate(dock.yaw());
            g.setColor(new Color(181,204,185));
            g.fillRoundRect(-1,-3,2,6,1,1);
            g.setColor(new Color(223,202,134));
            g.drawLine(0,-3,0,-7);
        }finally{g.dispose();}
    }
    @Override public void close(){
        if(closed)return;
        closed=true;
        if(gpuFlight){
            var status=previewStatus();
            BeckenburgDiagnostics.record("OPENING FLIGHT CLOSE site="+map.site()
                +" first_frame_ready="+status.firstFrameReady()
                +" worker_busy="+status.workerBusy()
                +" requests="+status.requests()+" failures="+status.failures()
                +" first_frame_ms="+status.firstFrameElapsedMs()
                +" outcome="+(status.firstFrameReady()?"ready":
                    status.workerBusy()?"cancelled-while-preparing":"fallback-without-gpu-frame"));
        }
        if(worker!=null)worker.shutdownNow();
        if(preview!=null)preview.close();
    }
}
