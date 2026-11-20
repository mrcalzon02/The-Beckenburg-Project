package org.beckenburg;

/**
 * Source-space ghost steering acceptance. The test applies every planned
 * STEP through the REAL authoritative LanGhostSpatial collision gate, with
 * the fastest species and maximum non-burst speed. No room jumps or
 * collision exceptions are permitted; DRIFT remains the only room change.
 */
public final class DevMatchGhostSteeringTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        GhostIdentity.Type fastest=GhostIdentity.Type.SPIRIT;
        for(var type:GhostIdentity.Type.values())
            if(GhostTraits.movementPercent(type)>GhostTraits.movementPercent(fastest))
                fastest=type;
        check(DevMatchClientAi.legalGhostStep(map,HouseMap.EXTRACTION_ROOM,
            0,0,0,0)==null,"Ghost may never navigate inside the van");

        int emitted=0,avoided=0,deflections=0;
        for(int room:new int[]{0,1,4,5,6,13}){
            var host=new LanGhostSpatial(map,room);
            host.configureSpeed(150);
            host.configureBurst(fastest,10);
            int walked=0;
            for(int tick=1;tick<=64;tick++){
                var at=host.pose();
                // Intentionally press repeatedly toward boundaries. A correct
                // AI must steer along the wall or stand down, not flood REJECT.
                int requested=(tick-1)/16*90;
                int[] plan=DevMatchClientAi.legalGhostStep(map,at.room(),
                    Math.round(at.x()*1000)/1000.0,
                    Math.round(at.y()*1000)/1000.0,
                    Math.round(at.z()*1000)/1000.0,requested);
                if(plan==null){avoided++;continue;}
                check(Math.abs(plan[0])<=100&&Math.abs(plan[1])<=100
                    &&(plan[0]!=0||plan[1]!=0),"Bounded nonzero ghost input");
                if(Math.abs(Math.floorMod((int)Math.round(
                    Math.toDegrees(Math.atan2(plan[0],plan[1]))-requested)+180,360)-180)>10)
                    deflections++;
                try{
                    check(host.step(plan[0],plan[1],tick),"Projected move must apply");
                }catch(IllegalArgumentException rejected){
                    throw new AssertionError("AI sent host-rejected ghost STEP in room "+
                        room+" tick "+tick+" pose "+at+" input "+plan[0]+","+plan[1],rejected);
                }
                walked++;emitted++;
                check(host.pose().room()==room,
                    "Actual ghost must stay in the same room until legal DRIFT");
            }
            check(walked>0,"Ghost navigates at least one real step from room "+room);
        }
        check(emitted>50,"Planner still moves a real ghost across rooms and headings");
        System.out.println("PASS ghost STEP preflight: accepted="+emitted+
            " held="+avoided+" steered="+deflections+
            " max configured speed=150% fastest species="+fastest+
            " across six genuine House rooms");
    }
}
