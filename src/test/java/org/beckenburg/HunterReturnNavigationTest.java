package org.beckenburg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic real House return-route audit using the same legacy STEP
 * collision, authored stairs and physical door commands as a hired hunter.
 * Initial staging is the only room teleport (as in camera acceptance).
 */
public final class HunterReturnNavigationTest {
    private static String run(int origin,VanStaging.Station station){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var world=new LanWorld(map);
        var navigation=new HunterFieldNavigation(map);
        world.setRoom(1,origin);
        var target=map.van().location(station);
        var trace=new ArrayDeque<String>();
        int walked=0,doors=0,waits=0,rejected=0,changedRooms=0;
        int lastRoom=origin;
        String lastError="";
        for(int tick=1;tick<=1800;tick++){
            var here=world.body(1);
            if(here.room()==HouseMap.EXTRACTION_ROOM&&
                    map.van().stationAt(here.x(),here.z())==station){
                String passed=String.format(Locale.ROOT,
                    "PASS origin=%d station=%s tick=%d walked=%d doors=%d waits=%d collision_rejects=%d room_changes=%d final=(%.3f,%.3f,%.3f)",
                    origin,station,tick,walked,doors,waits,rejected,changedRooms,
                    here.x(),here.y(),here.z());
                System.out.println(passed);
                return null;
            }
            if(here.room()!=lastRoom){changedRooms++;lastRoom=here.room();}
            CampaignGuestBot.Input step=navigation.to(tick,here,HouseMap.EXTRACTION_ROOM,
                    target.x(),map.van().floorY(),target.z(),.11);
            try{
                switch(step.action()){
                    case WALK -> {
                        if(step.heading()>=0)world.turn(1,step.heading());
                        world.move(1,step.dx(),step.dz(),tick);
                        walked++;
                    }
                    case DOOR -> {if(world.door(1,tick))doors++;}
                    case WAIT -> waits++;
                    default -> throw new AssertionError("Invalid navigation command "+step.action());
                }
            }catch(IllegalArgumentException blocked){
                rejected++;
                lastError=blocked.getMessage();
            }
            if(tick==1||tick%45==0||step.action()==CampaignGuestBot.Action.DOOR){
                trace.addLast(String.format(Locale.ROOT,
                    "%d room=%d x=%.3f y=%.3f z=%.3f cmd=%s (%d,%d) door=%d wait=%d blocked=%d %s",
                    tick,here.room(),here.x(),here.y(),here.z(),step.action(),
                    step.dx(),step.dz(),doors,waits,rejected,lastError));
                if(trace.size()>18)trace.removeFirst();
            }
        }
        return "FAIL origin="+origin+" station="+station+" walked="+walked+
            " doors="+doors+" waits="+waits+" rejected="+rejected+
            " changedRooms="+changedRooms+" lastError="+lastError+
            " trace="+String.join(" / ",trace);
    }
    public static void main(String[] args){
        List<String> failures=new ArrayList<>();
        for(int origin:new int[]{5,6,0})
            for(var station:new VanStaging.Station[]{
                VanStaging.Station.RELAY,VanStaging.Station.EQUIPMENT}){
                String problem=run(origin,station);
                if(problem!=null){System.err.println(problem);failures.add(problem);}
            }
        if(!failures.isEmpty())
            throw new AssertionError(failures.size()+" real source-space return routes blocked");
        System.out.println("PASS all six physical House-to-van return routes without forced door opening or teleport");
    }
}
