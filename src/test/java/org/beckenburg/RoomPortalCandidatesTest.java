package org.beckenburg;

/** Room graph diagnostics cannot remove an actual renderable object. */
public final class RoomPortalCandidatesTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var graph=new RoomPortalCandidates(map);
        var unknown=graph.evaluate(-1);
        check(unknown.fallbackAll()&&unknown.candidates().size()==map.level().rooms().size(),
            "Unknown camera rooms must retain every authored room");
        int indoor=map.level().rooms().stream().filter(r->!r.outside())
            .findFirst().orElseThrow().id();
        var baseline=graph.evaluate(indoor);
        check(baseline.includes(indoor)&&!baseline.fallbackAll(),
            "Indoor view must include its actual current room");
        check(baseline==graph.evaluate(indoor),"Stable camera/doors must reuse snapshot");
        for(var room:map.level().rooms())if(room.outside())
            check(baseline.includes(room.id()),"Conservative outdoors must be retained");
        for(var portal:map.level().portals()){
            if(portal.door())continue;
            check(graph.evaluate(portal.a()).includes(portal.b()),
                "Windows, stairs, archways must expose directly connected rooms");
        }
        var door=map.level().portals().stream()
            .filter(p->p.door()).findFirst().orElseThrow();
        var closed=graph.evaluate(door.a());
        map.toggleDoor(door.id());
        var open=graph.evaluate(door.a());
        check(!closed.revision().equals(open.revision()),
            "Door movement must invalidate the candidate graph");
        check(open.includes(door.b()),"Open door exposes its neighbor");
        check(open==graph.evaluate(door.a()),"Stable open door state reuses graph");
        System.out.println("PASS House portal candidate diagnostic: windows/stairs, doors, fallback, stable snapshots");
    }
}
