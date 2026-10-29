package org.beckenburg;

/** Static House GPU lighting must invalidate on real occlusion and circuit
 * changes, but retain unrelated cells between ordinary frames. */
public final class FixtureLightingRegionRevisionTest {
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        RoomLighting circuits=new RoomLighting();
        FixtureLighting lighting=new FixtureLighting(map);
        var source=lighting.sources().stream()
            .filter(s->s.room()==0).findFirst().orElseThrow();
        double x=source.x(),y=source.y(),z=source.z();
        lighting.frame(circuits,0);
        long original=lighting.regionVersion(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5);
        long remote=lighting.regionVersion(1000,0,1000,1002,2,1002);
        lighting.frame(circuits,1);
        check(original==lighting.regionVersion(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5),
            "Unchanged authored lighting must keep its prepared GPU revision");
        circuits.toggle(0);
        lighting.frame(circuits,2);
        check(original!=lighting.regionVersion(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5),
            "Local room circuit must invalidate its static GPU light colours");
        check(remote==lighting.regionVersion(1000,0,1000,1002,2,1002),
            "Unrelated outdoor GPU light range should stay prepared");
        long beforeDoor=lighting.regionVersion(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5);
        var door=map.level().portals().stream().filter(p->p.door()).findFirst().orElseThrow();
        map.toggleDoor(door.id());
        lighting.frame(circuits,3);
        check(beforeDoor!=lighting.regionVersion(x-.5,y-.5,z-.5,x+.5,y+.5,z+.5),
            "Door change must invalidate source receiver occlusion");
        System.out.println("PASS static House GPU regional light revisions");
    }
}
