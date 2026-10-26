package org.beckenburg;

/** Exterior light LOD must not substitute coarse probes for indoor walls. */
public final class FixtureLightingLodTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args){
        for(var site:new HouseMap.Site[]{
            HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            FixtureLighting light=new FixtureLighting(map);
            light.frame(null,0);
            var inside=map.spawn(0);
            var outside=map.spawn(13);
            var precise=light.sampleDistanceLod(0,
                inside.x(),inside.y()+1.2,inside.z(),100);
            check(precise.equals(light.sample(0,
                inside.x(),inside.y()+1.2,inside.z())),
                "Indoor surfaces may never use approximate exterior light at "+site);
            check(light.frameStats().lodQueries()==0,
                "Indoor walls and evidence views bypass distant LOD");
            double x=outside.x(),y=outside.y()+1.2,z=outside.z();
            var reference=light.sampleDistanceLod(13,x,y,z,100);
            var second=light.sampleDistanceLod(13,x,y,z,100);
            check(reference.equals(second),"Identical probe must retain authored light");
            check(light.frameStats().lodQueries()==2
                &&light.frameStats().lodHits()==1,
                "Distant exterior probe should replace repeat source shadow rays");
            int before=(int)light.frameStats().lodQueries();
            light.sampleDistanceLod(13,x,y,z,3);
            check(light.frameStats().lodQueries()==before,
                "Close-up outdoor lighting requires full-detail source sampling");
            light.frame(null,1);
            check(light.frameStats().lodQueries()==0,
                "Per-frame counters reset without invalidating all static light cache");
            System.out.println("PASS "+site+": distant outdoor lighting LOD, indoor precision, near precision and diagnostics");
        }
    }
}
