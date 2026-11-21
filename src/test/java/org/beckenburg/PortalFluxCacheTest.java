package org.beckenburg;

import java.util.List;

/** One-hop source/door transport is computed on door-state revisions. A
 * receiver's local architectural ray is shared by different source lights. */
public final class PortalFluxCacheTest {
    private static void ok(boolean yes,String message){
        if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var portal=AuthoredTestSupport.door(map,3,4);
        var a=AuthoredTestSupport.side(map,portal,3,.78);
        var b=AuthoredTestSupport.side(map,portal,4,.78);
        var first=new FixtureLighting.Source(0,FixtureLighting.Kind.BARE_BULB,3,0,
            a.x(),portal.y()+1.35,a.z(),0,-1,0,7,5,1,.72,.38);
        var second=new FixtureLighting.Source(1,FixtureLighting.Kind.BARE_BULB,3,0,
            a.x(),portal.y()+1.45,a.z(),0,-1,0,7,5,.3,.7,1);
        var cache=new PortalFluxCache(map,List.of(first,second));
        ok(cache.rebuilds()==1&&cache.transfers()==0,
            "Closed door must initialize an empty optical transfer graph");
        ok(Double.isNaN(cache.weight(first,4,b.x(),portal.y()+1.35,b.z(),
                0,0,0,false)),"Closed door cannot invent virtual light");
        map.toggleDoor(portal.id());
        cache.refresh();
        ok(cache.rebuilds()==2&&cache.transfers()>=2,
            "Opened doorway must cache both real source-to-door incidences");
        double before=cache.weight(first,4,b.x(),portal.y()+1.35,b.z(),
            0,0,0,false);
        ok(Double.isFinite(before)&&before>0,"Open door must carry incident light");
        long rays=cache.receiverRays();
        double another=cache.weight(second,4,b.x(),portal.y()+1.35,b.z(),
            0,0,0,false);
        ok(Double.isFinite(another)&&another>0,
            "Second real source must share the doorway");
        ok(cache.receiverRays()==rays&&cache.receiverHits()>0,
            "Receiver ray reused across lights instead of recast");
        ok(cache.weight(first,4,b.x(),portal.y()+1.35,b.z(),0,0,0,false)==before,
            "Static receiver contribution changes without a source or door revision");
        cache.refresh();
        ok(cache.rebuilds()==2,"Unchanged door topology rebuilt optical flux");
        map.toggleDoor(portal.id());
        cache.refresh();
        ok(cache.rebuilds()==3&&cache.transfers()==0,
            "Shutting the door must invalidate all outgoing light");
        ok(Double.isNaN(cache.weight(first,4,b.x(),portal.y()+1.35,b.z(),
                0,0,0,false)),"Closing the door left an active portal source");
        System.out.println("PASS cached real incident flux, receiver ray sharing, and door invalidation");
    }
}
