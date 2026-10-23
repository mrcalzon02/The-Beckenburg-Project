package org.beckenburg;

import java.lang.management.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/** Development-build flight recorder. No telemetry or uploads; all output stays local. */
public final class BeckenburgDeveloperTrace {
    private static final AtomicLong CASE_START=new AtomicLong();
    private static volatile long lastEdt=System.nanoTime();
    private static volatile long lastSample=System.nanoTime();
    private static volatile long previousGcMillis=-1;
    private static volatile long previousGcCount=-1;
    private static volatile String caseStage="idle";
    private static volatile boolean started;
    private static volatile long lastFrame=System.nanoTime();
    private static volatile long lastSnapshot;
    private static final AtomicLong lastRenderSlowReport=new AtomicLong();
    private static volatile boolean activeCase;
    private static volatile long frameCount;
    private BeckenburgDeveloperTrace(){}
    public static synchronized void start(){
        if(started)return;
        started=true;
        BeckenburgDiagnostics.record("DEVELOPER_DIAGNOSTICS enabled mode=full local_only=true sample_interval_ms=2000");
        Thread t=new Thread(BeckenburgDeveloperTrace::sampleLoop,"Beckenburg-developer-flight-recorder");
        t.setDaemon(true);t.start();
    }
    public static void caseStart(String description){
        CASE_START.set(System.nanoTime()); caseStage="begin"; activeCase=true;
        lastFrame=System.nanoTime();frameCount=0;
        BeckenburgDiagnostics.record("CASE START "+description);
    }
    public static void caseStage(String description){
        caseStage=description;
        long elapsed=CASE_START.get()==0?-1:(System.nanoTime()-CASE_START.get())/1_000_000;
        BeckenburgDiagnostics.record("CASE STAGE "+description+" elapsed_ms="+elapsed);
    }
    public static void frame(long durationNanos){
        lastFrame=System.nanoTime();frameCount++;
        if(durationNanos>100_000_000L)BeckenburgDiagnostics.record("FRAME SLOW duration_ms="+durationNanos/1_000_000+" stage="+caseStage);
    }
    /** Bounded stage timing survives a stalled or forcibly terminated renderer. */
    public static void renderSlow(int faces,int width,int height,long buildNs,long drawNs){
        long now=System.nanoTime(),prior=lastRenderSlowReport.get();
        if(now-prior<2_000_000_000L||
            !lastRenderSlowReport.compareAndSet(prior,now))return;
        BeckenburgDiagnostics.record("RENDER SLOW stage="+caseStage+
            " faces="+faces+" target="+width+"x"+height+
            " build_ms="+buildNs/1_000_000+" lighting_raster_ms="+drawNs/1_000_000);
    }
    public static void caseStop(){activeCase=false;caseStage="closed";}
    public static void update(String operation,String detail){
        BeckenburgDiagnostics.record("UPDATE TRACE "+operation+" "+detail);
    }
    private static void sampleLoop(){
        var thread=ManagementFactory.getThreadMXBean();
        var mem=ManagementFactory.getMemoryMXBean();
        var os=ManagementFactory.getOperatingSystemMXBean();
        var runtime=Runtime.getRuntime();
        while(true){
            try{Thread.sleep(2000);}catch(InterruptedException done){Thread.currentThread().interrupt();return;}
            try{
                long now=System.nanoTime();
                long used=mem.getHeapMemoryUsage().getUsed();
                long committed=mem.getHeapMemoryUsage().getCommitted();
                long gcCount=0,gcMillis=0;
                for(GarbageCollectorMXBean gc:ManagementFactory.getGarbageCollectorMXBeans()){
                    if(gc.getCollectionCount()>0)gcCount+=gc.getCollectionCount();
                    if(gc.getCollectionTime()>0)gcMillis+=gc.getCollectionTime();
                }
                long previousCount=previousGcCount,previousMillis=previousGcMillis;
                previousGcCount=gcCount;previousGcMillis=gcMillis;
                long elapsed=CASE_START.get()==0?-1:(now-CASE_START.get())/1_000_000;
                BeckenburgDiagnostics.record("PERF sample stage="+caseStage+" case_elapsed_ms="+elapsed+
                    " heap_used="+used+" heap_committed="+committed+" max_heap="+runtime.maxMemory()+
                    " threads="+thread.getThreadCount()+" daemon_threads="+thread.getDaemonThreadCount()+
                    " gc_count="+gcCount+" gc_ms="+gcMillis+
                    " gc_delta_count="+(previousCount<0?0:gcCount-previousCount)+
                    " gc_delta_ms="+(previousMillis<0?0:gcMillis-previousMillis)+
                    " system_load="+os.getSystemLoadAverage()+
                    " frame_count="+frameCount+" frame_age_ms="+(now-lastFrame)/1_000_000);
                long frameAge=(now-lastFrame)/1_000_000;
                if(activeCase && elapsed>10000 && frameAge>5000 && now-lastSnapshot>15_000_000_000L){
                    lastSnapshot=now;
                    snapshot("case-frame-stalled-"+caseStage);
                }
                if(activeCase && elapsed>10000 && !"active".equals(caseStage) && now-lastSnapshot>15_000_000_000L){
                    lastSnapshot=now;
                    snapshot("case-initialization-stalled-"+caseStage);
                }
                lastSample=now;
            }catch(Throwable error){BeckenburgDiagnostics.record("PERF collector failed "+error);}
        }
    }
    public static void snapshot(String reason){
        try{
            String safe=reason.replaceAll("[^a-zA-Z0-9_.-]","_");
            Path root=BeckenburgDiagnostics.directory();Files.createDirectories(root);
            String name="snapshot-"+Instant.now().toString().replace(':','-')+"-"+safe+".txt";
            StringBuilder report=new StringBuilder();
            report.append("Reason: ").append(reason).append("\nTimestamp: ").append(Instant.now())
                .append("\nCase stage: ").append(caseStage).append("\n");
            for(ThreadInfo info:ManagementFactory.getThreadMXBean().dumpAllThreads(true,true)){
                if(info!=null)report.append("\n").append(info);
            }
            Path file=root.resolve(name);
            Files.writeString(file,BeckenburgDiagnostics.redact(report.toString()),StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
            BeckenburgDiagnostics.record("PERF thread_snapshot="+file.getFileName());
        }catch(Throwable problem){BeckenburgDiagnostics.record("PERF snapshot_failed="+problem);}
    }
}
