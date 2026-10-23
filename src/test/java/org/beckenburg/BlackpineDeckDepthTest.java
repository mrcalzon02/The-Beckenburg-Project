package org.beckenburg;
import java.lang.reflect.*;
import java.util.*;
/** Inspect actual emitted geometry and cross-storey light, not only metadata. */
public final class BlackpineDeckDepthTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static double n(Object p,String key)throws Exception{return ((Number)LevelSceneSource.read(p,key)).doubleValue();}
    public static void main(String[] args)throws Exception{
        var map=BlackpineLevel.map();var l=map.level();var r=l.room(11);double bottom=r.y()-.16;
        Method m=SceneRenderer.class.getDeclaredMethod("levelDeck",List.class,LevelDefinition.class,LevelDefinition.Room.class,boolean.class);m.setAccessible(true);
        var faces=new ArrayList<Object>();m.invoke(null,faces,l,r,false);
        double topArea=0,bottomArea=0,min=999;int edges=0;
        for(Object f:faces){
            var a=LevelSceneSource.read(f,"a");var b=LevelSceneSource.read(f,"b");var c=LevelSceneSource.read(f,"c");
            double ay=n(a,"y"),by=n(b,"y"),cy=n(c,"y");min=Math.min(min,Math.min(ay,Math.min(by,cy)));
            double cross=(n(b,"x")-n(a,"x"))*(n(c,"z")-n(a,"z"))-(n(b,"z")-n(a,"z"))*(n(c,"x")-n(a,"x"));
            if(Math.abs(ay-r.y())<1e-7&&Math.abs(by-ay)<1e-7&&Math.abs(cy-ay)<1e-7)topArea+=Math.abs(cross)/2;
            else if(Math.abs(ay-bottom)<1e-7&&Math.abs(by-ay)<1e-7&&Math.abs(cy-ay)<1e-7){bottomArea+=Math.abs(cross)/2;check(cross>0,"soffit faces downward");}
            else edges++;
        }
        check(Math.abs(min-bottom)<1e-7,"loft needs a real 160mm underside, not one paper-thin floor face");
        check(topArea>40&&Math.abs(topArea-bottomArea)<1e-7,"top and soffit preserve the same stair aperture");
        check(edges>0,"loft edge and stair opening need structural fascia");
        var lamp=l.sockets().stream().filter(s->s.id().equals("blackpine-mill-underloft")).findFirst().orElseThrow();
        check(Math.abs(lamp.y()-bottom)<1e-7,"fixture mounts to actual slab underside");
        check(!map.clearLineOfSight(45.7,bottom+.08,32,46.3,bottom+.08,32),"slab edge has opaque physical thickness");
        var circuits=new RoomLighting();circuits.toggle(0);var light=new FixtureLighting(map);light.frame(circuits,1);
        double[] before=new double[3];double[] x={46.3,48.5,51.5};
        for(int i=0;i<x.length;i++)before[i]=light.sample(0,x[i],bottom-.08,35).energy();
        circuits.toggle(11);light.frame(circuits,2);
        for(int i=0;i<x.length;i++)check(Math.abs(before[i]-light.sample(0,x[i],bottom-.08,35).energy())<1e-9,"upstairs circuit must not light underside at X"+x[i]);
        System.out.println("PASS actual 160mm soffit/fascia, equal top/underside apertures, mounted lamp, opaque slab edge and isolated upstairs lighting");
    }
}
