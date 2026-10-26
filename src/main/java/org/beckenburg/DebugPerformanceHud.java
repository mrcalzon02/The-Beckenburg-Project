package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Locale;

/**
 * Screenshot-ready performance flight display. The HUD is entirely local:
 * no host packets, no ghost identity, no gameplay/evidence mutation.
 *
 * Ctrl+Shift+F7 toggles it from a live game window. Use
 * -Dbeckenburg.debug.overlay=true to start with it visible.
 * CPU timings are NOT mislabeled as GPU command execution timing.
 */
public final class DebugPerformanceHud {
    private static volatile boolean enabled=Boolean.getBoolean("beckenburg.debug.overlay");
    private final double[] history=new double[120];
    private int count,next;
    private long lastFrameAt,lastLog;
    private long loopNs;
    private SceneRenderer.FrameDiagnostics metrics;
    private String view="UNKNOWN";
    private int actors,room;
    private double x,y,z,yaw;
    private int width,height;
    private static final Font FONT=new Font(Font.MONOSPACED,Font.PLAIN,11);
    public static boolean enabled(){return enabled;}
    public static boolean hotkey(KeyEvent event){
        int mask=InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK;
        if(event.getKeyCode()!=KeyEvent.VK_F7||(event.getModifiersEx()&mask)!=mask)return false;
        enabled=!enabled;
        BeckenburgDiagnostics.record("PERF HUD "+(enabled?"enabled":"disabled")+
            " toggle=Ctrl+Shift+F7");
        event.consume();
        return true;
    }
    /** Only read scene state after the real draw; never force an extra scene. */
    public void sample(long entireFrameNs,SceneRenderer scene,HouseMap map,
                       int actorCount,String viewport){
        if(!enabled)return;
        long now=System.nanoTime();
        // For screenshots, an actual draw cannot make FPS look faster than
        // its measured wall duration. First-sample display is explicit.
        double wall=lastFrameAt==0?Double.NaN:(now-lastFrameAt)/1e6;
        lastFrameAt=now;
        loopNs=Math.max(0,entireFrameNs);
        metrics=scene.frameDiagnostics();
        view=viewport;
        actors=actorCount;
        x=scene.x;y=scene.eyeHeight;z=scene.z;yaw=scene.yaw;
        width=scene.width();height=scene.height();
        room=map==null?-1:map.roomAt(x,y-1.62,z);
        double measured=Double.isNaN(wall)?loopNs/1e6:Math.max(wall,loopNs/1e6);
        if(measured>0 && Double.isFinite(measured)){
            history[next]=measured;
            next=(next+1)%history.length;
            if(count<history.length)count++;
        }
        if((loopNs>150_000_000L||measured>250) && now-lastLog>2_000_000_000L){
            lastLog=now;
            var m=metrics;
            BeckenburgDiagnostics.record(String.format(Locale.ROOT,
                "VIEWPORT PERF viewport=%s room=%d eye=%.2f,%.2f,%.2f yaw=%.2f actors=%d loop_ms=%.1f wall_ms=%.1f backend=%s build_ms=%.1f geometry_ms=%.1f effects_ms=%.1f faces=%d frustum=%d gpu_readback=%s light_cache_hits=%d shadow_rays=%d normal_rays=%d depth_rejected=%d lod_hits=%d lod_queries=%d light_candidates=%d lighting_cells_invalidated=%d",
                view,room,x,y,z,yaw,actors,loopNs/1e6,measured,
                m==null?"not-rendered":m.gpu()?"GPU":"CPU-fallback",
                m==null?0:m.sceneBuildNs()/1e6,m==null?0:m.geometryNs()/1e6,
                m==null?0:m.effectsNs()/1e6,m==null?0:m.faces(),
                m==null?0:m.frustumFaces(),
                m==null?"n/a":m.gpuReadbackFresh()?"fresh":m.gpuReadbackAvailable()?"stale":"pending",
                m==null?0:m.lighting().cacheHits(),
                m==null?0:m.lighting().shadowRays(),
                m==null?0:m.lighting().normalRays(),
                m==null?0:m.depthRejectedFaces(),
                m==null?0:m.lighting().lodHits(),
                m==null?0:m.lighting().lodQueries(),
                m==null?0:m.lighting().consideredSources(),
                m==null?0:m.lighting().invalidatedCells()));
        }
    }
    static int graphBarHeight(double ms,int maxHeight){
        if(!Double.isFinite(ms)||ms<=0)return 0;
        return Math.min(maxHeight,(int)Math.round(ms*maxHeight/100.0));
    }
    public void draw(Graphics2D g,int canvasWidth,int canvasHeight){
        if(!enabled||count==0||canvasWidth<340||canvasHeight<350)return;
        int w=Math.min(476,canvasWidth-16),h=331,left=canvasWidth-w-8,top=8;
        double[] sorted=Arrays.copyOf(history,count);Arrays.sort(sorted);
        double p50=sorted[(count-1)/2],p95=sorted[(int)Math.ceil(count*.95)-1],
            p99=sorted[(int)Math.ceil(count*.99)-1];
        double last=history[(next+history.length-1)%history.length];
        var m=metrics;
        Graphics2D out=(Graphics2D)g.create();
        try{
            out.setColor(new Color(4,9,16,234));out.fillRoundRect(left,top,w,h,10,10);
            out.setColor(new Color(92,217,171));out.drawRoundRect(left,top,w-1,h-1,10,10);
            out.setFont(FONT);
            int yLine=top+16;
            text(out,left+10,yLine,"ENGINE DIAGNOSTICS   CTRL+SHIFT+F7    "+view,0x92ecce);yLine+=17;
            text(out,left+10,yLine,String.format(Locale.ROOT,
                "FPS %.1f    FRAME %.1fms    P50 %.1f  P95 %.1f  P99 %.1f",
                last>0?1000/last:0,last,p50,p95,p99),last>100?0xff8888:0xf0e0c6);yLine+=17;
            text(out,left+10,yLine,String.format(Locale.ROOT,
                "EYE %.1f,%.1f,%.1f    YAW %.0fdeg    ROOM %d  NPC/PEERS %d",
                x,y,z,Math.toDegrees(yaw),room,actors),0xe1e8e2);yLine+=17;
            if(m==null){text(out,left+10,yLine,"RENDER WAITING FOR FIRST FRAME",0xffcc77);yLine+=17;}
            else{
                text(out,left+10,yLine,
                    "BACKEND "+(m.gpu()?"GPU ACTIVE":"CPU FALLBACK")+
                    "   BUFFER "+width+"x"+height+
                    "  READBACK "+(m.gpu()?(m.gpuReadbackFresh()?"FRESH":
                        m.gpuReadbackAvailable()?"STALE":"PENDING"):"N/A"),
                    m.gpu()?0x9eecb8:0xffbc77);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "FRAME RENDER %.1fms  SCENE BUILD %.1fms",
                    m.totalNs()/1e6,m.sceneBuildNs()/1e6),0xe1e8e2);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "GEOMETRY CPU %.1fms  POST/EFFECTS %.1fms",
                    m.geometryNs()/1e6,m.effectsNs()/1e6),0xe1e8e2);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "FACES %,d  FRUSTUM %,d  EARLY REJECT %,d",
                    m.faces(),m.frustumFaces(),m.depthRejectedFaces()),0xe1e8e2);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "SCENERY INSTANCES %,d  VISIBLE %,d  CULLED %,d",
                    m.sceneryTotal(),m.sceneryVisible(),
                    m.sceneryTotal()-m.sceneryVisible()),0xe1e8e2);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "LIGHTS %d  CACHE %,d/%,d  SHADOW %,d+%,d  LOD %,d/%,d",
                    m.lighting().lights(),m.lighting().cacheHits(),
                    m.lighting().samples(),m.lighting().shadowRays(),
                    m.lighting().normalRays(),
                    m.lighting().lodHits(),m.lighting().lodQueries()),
                    0xffddad);yLine+=17;
                text(out,left+10,yLine,String.format(Locale.ROOT,
                    "VOLUME %,d  DIRTY %,d  NORMAL CACHE %,d/%,d",
                    m.lighting().consideredSources(),m.lighting().invalidatedCells(),
                    m.lighting().normalCacheHits(),m.lighting().normalSamples()),
                    0xb8e1cd);yLine+=17;
            }
            var rt=Runtime.getRuntime();
            text(out,left+10,yLine,String.format(Locale.ROOT,
                "HEAP %dMB / %dMB  JVM MAX %dMB",
                (rt.totalMemory()-rt.freeMemory())>>20,
                rt.totalMemory()>>20,rt.maxMemory()>>20),0xbbcdd2);
            int graphTop=top+236,graphHeight=71;
            out.setColor(new Color(28,44,53));out.fillRect(left+10,graphTop,w-20,graphHeight);
            out.setColor(new Color(68,98,110));
            for(int t:new int[]{16,33,50,100}){
                int gy=graphTop+graphHeight-graphBarHeight(t,graphHeight);
                out.drawLine(left+10,gy,left+w-11,gy);
            }
            for(int i=0;i<count;i++){
                double ms=history[(next+history.length-count+i)%history.length];
                int bx=left+11+i*(w-22)/history.length;
                int bw=Math.max(2,(w-23)/history.length);
                int bh=graphBarHeight(ms,graphHeight);
                out.setColor(new Color(ms>100?233:ms>33?247:111,
                    ms>100?94:ms>33?196:225,ms>100?106:ms>33?97:172));
                out.fillRect(bx,graphTop+graphHeight-bh,bw,bh);
            }
            text(out,left+12,top+321,"120 FRAME HISTORY  /  16 33 50 100ms GRID",0xb9cbd0);
        }finally{out.dispose();}
    }
    private static void text(Graphics2D g,int x,int y,String s,int rgb){
        g.setColor(new Color(rgb));g.drawString(s,x,y);
    }
}
