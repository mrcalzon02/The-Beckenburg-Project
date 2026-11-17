package org.beckenburg;

import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.beckenburg.gpu.*;

public class DoorGlReview {
    static String hex(MessageDigest digest, ByteBuffer buffer) {
        return HexFormat.of().formatHex(digest.digest(buffer.array()));
    }

    public static void main(String[] args) throws Exception {
        var start = GpuDesktop.class.getDeclaredMethod("startGpu");
        var stop = GpuDesktop.class.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try { run(Path.of(args[0])); } finally { stop.invoke(null); }
    }

    static void run(Path output) throws Exception {
        var gl = (OpenGlWorldRenderer) GpuWorldRenderer.active();
        var map = new HouseMap(HouseMap.Site.HOUSE);
        var door = map.level().portals().stream().filter(p -> p.door() && (p.a()==0||p.b()==0)).findFirst().orElseThrow();
        var doorB = map.level().portals().stream().filter(p -> p.door() && p.id()!=door.id()).findFirst().orElseThrow();
        var window = map.level().portals().stream().filter(p -> p.kind().equals("window")).findFirst().orElseThrow();
        var view = new SceneRenderer();
        view.setRenderSize(320,200);
        view.eyeHeight = door.y()+1.62;
        view.pitch = -.03;
        System.out.println(gl.rendererDescription()+"; doors="+door.id()+","+doorB.id()+" window="+window.id());
        var sha = MessageDigest.getInstance("SHA-256");
        try (var csv = Files.newBufferedWriter(output)) {
            csv.write("frame,phase,door_open,door_b_open,window_open,vertices_uploaded_bytes,uploaded_batches,gl_worker_ns,gl_issue_ns,render_thread_cpu_ns,end_to_end_ns,argb_sha256,depth_bits_sha256\n");
            var cpu = java.lang.management.ManagementFactory.getThreadMXBean();
            for (int i=0; i<58; i++) {
                String phase = i<8?"warmup":i<32?"toggle":i<40?"other_portals":i<52?"visual":"restore";
                if(i>=8 && i<32 || i==34||i==37||i==42||i==50)
                    map.toggleDoor(door.id(),System.nanoTime()-HouseMap.DOOR_SWING_NANOS);
                if(i==32||i==35) map.toggleDoor(doorB.id(),System.nanoTime()-HouseMap.DOOR_SWING_NANOS);
                if(i==33||i==36) map.toggleWindow(window.id());
                double offset = i==48||i==49?.04:1.4;
                view.x=door.x()+(door.alongX()?0:offset);
                view.z=door.z()+(door.alongX()?offset:0);
                view.yaw=Math.atan2(door.x()-view.x,door.z()-view.z)+(phase.equals("visual")?.08*Math.sin(i*.07):0);
                view.lamp=i>=44&&i<48;view.setLampHardware(EquipmentTiers.Tier.I,i>=46&&i<48);view.uvLamp=i==50||i==51;
                long t=System.nanoTime(), c=cpu.getCurrentThreadCpuTime();
                view.drawPractice(map,List.of(),i/30d);
                c=cpu.getCurrentThreadCpuTime()-c;
                long id=view.submittedGpuFrameId(),to=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
                GpuWorldRenderer.Result r;
                while((r=gl.latest())==null||r.frameId()!=id) {
                    if(!gl.ready()||System.nanoTime()>to) throw new AssertionError("GL frame "+id+" "+gl.rendererDescription());
                    Thread.sleep(1);
                }
                long e=System.nanoTime()-t;
                var stats=gl.uploadStats();
                var timing=gl.frameTiming();
                if(stats.frameId()!=id||timing.frameId()!=id) throw new AssertionError("Wrong GL frame");
                int visible=0;
                var color=ByteBuffer.allocate(r.argb().length*4);
                var depth=ByteBuffer.allocate(r.depth().length*4);
                for(int p:r.argb()){color.putInt(p);if((p>>>24)!=0)visible++;}
                for(float z:r.depth())depth.putInt(Float.floatToRawIntBits(z));
                if(Boolean.getBoolean("beckenburg.door.capturePixels")){
                    var capture=output.toAbsolutePath().getParent().resolve("pixels");
                    Files.createDirectories(capture);
                    try(var out=new java.util.zip.GZIPOutputStream(Files.newOutputStream(capture.resolve("frame-"+i+".argb.gz")))){
                        out.write(color.array());
                    }
                }
                if(visible<1000)throw new AssertionError("Blank frame");
                csv.write(String.format(Locale.ROOT,"%d,%s,%b,%b,%b,%d,%d,%d,%d,%d,%d,%s,%s%n",i,phase,
                    map.doorOpen(door.id()),map.doorOpen(doorB.id()),map.windowOpen(window.id()),stats.uploadedBytes(),stats.uploadedBatches(),
                    timing.totalNs(),timing.drawIssueNs(),c,e,hex(sha,color),hex(sha,depth)));
                csv.flush();
            }
        }
    }
}
