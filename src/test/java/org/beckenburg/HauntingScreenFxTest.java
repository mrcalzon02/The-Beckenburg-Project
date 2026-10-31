package org.beckenburg;

import java.util.Arrays;
import java.util.List;

/** Objective rendering invariants. Visual taste and color choices are advisory. */
public final class HauntingScreenFxTest {
    private HauntingScreenFxTest(){}
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    private static int[] scene(int w,int h){
        int[] pixels=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int r=28+x*67/w, g=36+y*78/h, b=42+(x+y)*40/(w+h);
            pixels[y*w+x]=(r<<16)|(g<<8)|b;
        }
        return pixels;
    }
    private static long difference(int[] a,int[] b){
        long count=0;
        for(int i=0;i<a.length;i++)if(a[i]!=b[i])count++;
        return count;
    }
    public static void main(String[] args){
        int width=320,height=200;
        int[] base=scene(width,height);
        long start=System.nanoTime();
        int scored=0,generic=0;
        for(GhostHauntingFolio.Entry entry:GhostHauntingFolio.entries()){
            var event=new GhostHauntingFolio.Occurrence(entry,4,100);
            int tick=100+entry.duration()/2;
            int[] a=base.clone(),b=base.clone();
            GhostHauntingFolio.paint(a,width,height,List.of(event),4,tick);
            GhostHauntingFolio.paint(b,width,height,List.of(event),4,tick);
            check(Arrays.equals(a,b),"non-deterministic haunted world frame "+entry.id());
            check(difference(a,base)>0,"haunting did not render "+entry.id());
            int[] otherRoom=base.clone();
            GhostHauntingFolio.paint(otherRoom,width,height,List.of(event),3,tick);
            check(Arrays.equals(base,otherRoom),"haunt leaked outside authoritative room "+entry.id());
            int[] expired=base.clone();
            GhostHauntingFolio.paint(expired,width,height,List.of(event),4,event.untilTick());
            check(Arrays.equals(base,expired),"haunt remained after its host event expired");
            var score=GhostHauntingChoreography.forEvent(entry.id());
            if(score==null)generic++;else scored++;
        }
        check(scored==14&&generic==36,"all bespoke and generic effects are represented");
        int[] first=scene(320,200);
        int[] resized=scene(640,360);
        var scoredEvent=new GhostHauntingFolio.Occurrence(
            GhostHauntingFolio.entry("radio_crawl"),4,100);
        GhostHauntingFolio.paint(first,320,200,List.of(scoredEvent),4,113);
        GhostHauntingFolio.paint(resized,640,360,List.of(scoredEvent),4,113);
        check(difference(first,scene(320,200))>0 && difference(resized,scene(640,360))>0,
            "resizing the viewer must not invalidate cached spectral sampling");
        int[] absent=scene(64,40),before=absent.clone();
        GhostHauntingFolio.paint(absent,64,40,List.of(),4,120);
        check(Arrays.equals(absent,before),"empty haunting history should not touch the frame");
        // A real, old visual effect is not permission for identity art or evidence:
        // this pass only consumes the occurrence already authorized by the host.
        System.out.println("PASS haunting screen FX: 50 real folio effects, room confinement,"
            +" expiry, 14 bespoke motifs, deterministic pixels, resolution changes");
        System.out.println("DIAGNOSTIC haunting_fx_cpu_ms_50_events_at_320x200="
            +(System.nanoTime()-start)/1_000_000.0
            +" (aesthetic/performance guidance only; no timing gate)");
    }
}
