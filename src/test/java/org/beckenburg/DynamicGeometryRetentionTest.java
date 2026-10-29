package org.beckenburg;

import java.lang.reflect.Method;
import java.util.*;

/** Differential source-face checks; hardware image/lighting acceptance is separate. */
public final class DynamicGeometryRetentionTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static Method method(String name,Class<?>...types)throws Exception{
        var m=SceneRenderer.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;
    }
    private static List<Object> faces(SceneRenderer r,HouseMap map)throws Exception{
        var out=new ArrayList<Object>();
        method("beginDynamicGeometry",HouseMap.class).invoke(r,map);
        method("looseClutter",List.class,HouseMap.class).invoke(r,out,map);
        method("eventFixtures",List.class,HouseMap.class).invoke(r,out,map);
        method("finishDynamicGeometry").invoke(r);
        return out;
    }
    private static List<Object> parity(SceneRenderer cached,SceneRenderer rebuilt,HouseMap map)throws Exception{
        rebuilt.roomLightTick=cached.roomLightTick;
        var actual=faces(cached,map);
        check(actual.equals(faces(rebuilt,map)),"Every position, normal, UV, material, tint and face order must match rebuilding");
        check(cached.dynamicGeometryStats().triangles()<=cached.dynamicGeometryStats().limit(),"Bounded retained faces");
        return actual;
    }
    private static String row(LooseClutter.State s,double dx,double yaw,double roll,int holder){
        return s.id()+","+s.kind()+","+Math.round((s.x()+dx)*1000)+","+Math.round(s.y()*1000)+","+
            Math.round(s.z()*1000)+","+Math.round(yaw*1000)+","+Math.round(roll*1000)+","+holder;
    }
    public static void main(String[] args)throws Exception{
        String flag="beckenburg.dynamic.geometry.cache.disabled",old=System.getProperty(flag);
        SceneRenderer cached,rebuilt;
        try{
            System.clearProperty(flag);cached=new SceneRenderer();
            System.setProperty(flag,"true");rebuilt=new SceneRenderer();
        }finally{if(old==null)System.clearProperty(flag);else System.setProperty(flag,old);}
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);cached.roomLightTick=25;
            var first=parity(cached,rebuilt,map);var stats=cached.dynamicGeometryStats();
            cached.x+=2;cached.yaw+=.4;
            var warm=parity(cached,rebuilt,map);
            check(stats.builds()==cached.dynamicGeometryStats().builds(),"Camera motion must not reconstruct posed geometry on "+site);
            for(int i=0;i<first.size();i++)check(first.get(i)==warm.get(i),"Unchanged face identity on "+site);
            if(!map.clutter().states().isEmpty()){
                var item=map.clutter().states().get(0);
                map.clutter().acceptOne(row(item,.025,item.yaw()+.35,item.roll()+.2,0));
                var moved=parity(cached,rebuilt,map);
                check(!moved.equals(first),"Movement/rotation/holding must update actual source faces");
                map.clutter().acceptOne(row(item,0,item.yaw(),item.roll(),-2));
                var hidden=parity(cached,rebuilt,map);
                check(hidden.size()<moved.size(),"Redacted held object must not retain visible geometry");
                map.clutter().acceptOne(row(item,0,item.yaw(),item.roll(),-1));parity(cached,rebuilt,map);
                for(var fixture:map.fixtures().states())map.fixtures().activate(fixture.id(),fixture.room(),25);
                parity(cached,rebuilt,map);
                cached.roomLightTick=26;
                check(!parity(cached,rebuilt,map).equals(facesAt(rebuilt,map,25)),"Alarm animation and indicators must change with tick");
                map.fixtures().advance(26);parity(cached,rebuilt,map);
                map.fixtures().power(false);parity(cached,rebuilt,map);
            }
        }
        check(rebuilt.dynamicGeometryStats().entries()==0,"Explicit fallback must retain no geometry");
        var stress=new HouseMap();
        var kind=Arrays.stream(LooseClutter.Kind.values()).max(Comparator.comparingInt(k->GlbProps.model(k.mesh()).triangles().size())).orElseThrow();
        var rows=new StringJoiner(";");
        for(int id=1;id<=500;id++)rows.add(id+","+kind+",1000,1000,1000,0,0,-1");
        stress.clutter().accept(rows.toString());
        var crowded=parity(cached,rebuilt,stress);var repeated=parity(cached,rebuilt,stress);
        check(crowded.equals(repeated),"Budget overflow must preserve complete rendered geometry");
        stress.clutter().accept("");parity(cached,rebuilt,stress);
        check(cached.dynamicGeometryStats().entries()<=stress.fixtures().states().size(),"Deleted clutter must leave no retained entries");
        // Entries skipped while the old set filled the budget are admitted
        // on the next frame, after removed objects have been released.
        parity(cached,rebuilt,stress);
        check(cached.dynamicGeometryStats().entries()==stress.fixtures().states().stream().filter(f->f.kind().mesh()!=null&&f.kind()!=HauntedFixtures.Kind.LIGHT_SWITCH).count(),"Deleted objects must release retained faces");
        var empty=new HouseMap(HouseMap.Site.HOLLOW_CREEK);parity(cached,rebuilt,empty);
        check(cached.dynamicGeometryStats().entries()==0,"New scene must discard the old map cache");
        System.out.println("PASS all-map dynamic source parity, camera reuse, moved/held/hidden clutter, animated fixtures, water/power, bounded overflow and map disposal");
    }
    private static List<Object> facesAt(SceneRenderer r,HouseMap map,int tick)throws Exception{
        r.roomLightTick=tick;return faces(r,map);
    }
}
