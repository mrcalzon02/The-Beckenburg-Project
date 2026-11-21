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
        double offAxis=b.x()+(portal.alongX()?.8:0);
        double offZ=b.z()+(portal.alongX()?0:.8);
        ok(cache.mayAffectReceiver(first,portal,offAxis,
                portal.y()+1.35,offZ,0),
            "Off-axis receiving-room light must be invalidated on door changes");
        ok(!cache.mayAffectReceiver(first,portal,a.x(),
                portal.y()+1.35,a.z(),0),
            "The source-side room is not a transferred-light receiver");
        var shortRange=new FixtureLighting.Source(91,
            FixtureLighting.Kind.BARE_BULB,3,0,
            a.x(),portal.y()+1.35,a.z(),0,-1,0,
            1.1,4,1,.7,.5);
        ok(!cache.mayAffectReceiver(shortRange,portal,b.x(),
                portal.y()+1.35,b.z(),0),
            "A short-range lamp cannot dirty receiving-room probes beyond its portal reach");
        ok(cache.mayAffectReceiver(first,portal,b.x(),
                portal.y()+1.35,b.z(),0),
            "A real in-range receiver must remain invalidated by the door");
        map.toggleDoor(portal.id());
        cache.refresh();
        ok(cache.rebuilds()==3&&cache.transfers()==0,
            "Shutting the door must invalidate all outgoing light");
        ok(Double.isNaN(cache.weight(first,4,b.x(),portal.y()+1.35,b.z(),
                0,0,0,false)),"Closing the door left an active portal source");
        // A synthetic lamp can demonstrate correct math while NONE of the
        // shipped lamps reaches an actual doorway. Measure the authoritative
        // source registry on multiple real maps, rather than reporting the
        // synthetic test as production light propagation evidence.
        int physicalTransfers=0,physicalPositive=0;
        for(var site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            var real=new HouseMap(site);
            var authored=new FixtureLighting(real);
            int actualDoorways=0,sourceDoorPairs=0;
            for(var p:real.level().portals())if(p.door()){
                if(!real.doorOpen(p.id()))real.toggleDoor(p.id());
                if(!real.outside(p.a())&&!real.outside(p.b())){
                    actualDoorways++;
                    for(var source:authored.sources())
                        if(source.room()==p.a()||source.room()==p.b())
                            sourceDoorPairs++;
                }
            }
            var physical=new PortalFluxCache(real,authored.sources());
            physicalTransfers+=physical.transfers();
            int positive=0;
            for(var p:real.level().portals())if(p.door()
                    &&!real.outside(p.a())&&!real.outside(p.b())){
                for(var light:authored.sources()){
                    int target=light.room()==p.a()?p.b():
                        light.room()==p.b()?p.a():-1;
                    if(target<0)continue;
                    var at=AuthoredTestSupport.side(real,p,target,.75);
                    double value=physical.weight(light,target,
                        at.x(),p.y()+1.35,at.z(),0,0,0,false);
                    if(Double.isFinite(value)&&value>0)positive++;
                }
            }
            physicalPositive+=positive;
            System.out.println("PHYSICAL PORTAL COVERAGE site="+site
                +" authored_lamps="+authored.sources().size()
                +" interior_doors="+actualDoorways
                +" potential_source_door_pairs="+sourceDoorPairs
                +" incident_rays="+physical.incidentRays()
                +" cached_transfers="+physical.transfers()
                +" positive_receiver_samples="+positive
                +" receiver_rays="+physical.receiverRays());
        }
        ok(physicalTransfers>0&&physicalPositive>0,
            "No live authored map exercised portal illumination; synthetic lights alone are not production proof");
        System.out.println("PASS real authored portal illumination coverage: transfers="
            +physicalTransfers+" positive_samples="+physicalPositive);
        System.out.println("PASS cached real incident flux, receiver ray sharing, and door invalidation");
    }
}
