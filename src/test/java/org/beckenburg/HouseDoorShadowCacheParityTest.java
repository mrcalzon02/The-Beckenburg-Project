package org.beckenburg;

/**
 * Nonblocking correctness diagnostic: a warmed visibility cache must yield the
 * same sampled light as a brand-new cache after every actual door change.
 */
public final class HouseDoorShadowCacheParityTest {
    private HouseDoorShadowCacheParityTest(){}
    private static void equal(FixtureLighting.RGB warm,FixtureLighting.RGB fresh,
                              int toggle,int room,double x,double z){
        if(Math.abs(warm.r()-fresh.r())>1e-6
            ||Math.abs(warm.g()-fresh.g())>1e-6
            ||Math.abs(warm.b()-fresh.b())>1e-6)
            throw new AssertionError("Stale light through door at toggle "+toggle
                +" room "+room+" point "+x+","+z
                +" warm="+warm+" fresh="+fresh);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var lights=new RoomLighting();
        var warm=new FixtureLighting(map);
        var door=map.level().portals().stream().filter(p->p.door())
            .filter(p->p.a()==0||p.b()==0)
            .findFirst().orElseThrow();
        int probes=0;
        for(int tick=1;tick<=12;tick++){
            warm.frame(lights,tick);
            var fresh=new FixtureLighting(map);
            fresh.frame(lights,tick);
            for(double normal:new double[]{-1.4,-.9,-.4,.4,.9,1.4})
                for(double tangent:new double[]{-.6,0,.6}){
                    double x=door.x()+(door.alongX()?tangent:normal);
                    double z=door.z()+(door.alongX()?normal:tangent);
                    double y=door.y()+1.55;
                    int room=map.roomAt(x,y,z);
                    if(room<0||room>=HouseMap.EXTRACTION_ROOM)continue;
                    equal(warm.sample(room,x,y,z),fresh.sample(room,x,y,z),
                        tick,room,x,z);
                    probes++;
                }
            map.toggleDoor(door.id());
        }
        if(probes<36)throw new AssertionError("Not enough real room light probes");
        System.out.println("PASS "+probes+" warm versus fresh light probes around actual House door");
    }
}
