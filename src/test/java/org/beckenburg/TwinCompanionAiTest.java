package org.beckenburg;

import java.util.ArrayList;
import java.util.List;

/** A human Twin has a persistent AI partner, not a pose-derived sprite copy. */
public final class TwinCompanionAiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        var spawn=map.spawn(4);
        var primary=new LanGhostSpatial.Pose(4,spawn.x(),spawn.y(),spawn.z(),180);
        var companion=new TwinCompanionAi(map,primary,20261021L);
        var start=companion.pose();
        var domain=new GhostDomain(map,4,GhostIdentity.Type.TWINS);
        List<LanGhostSpatial.Pose> seen=new ArrayList<>();
        boolean minorAction=false;
        for(int tick=1;tick<=210;tick++){
            // The partner's AI continues normally even when the human controller
            // has not issued a single ghost locomotion command.
            companion.advance(tick,primary);
            var pose=companion.pose();
            if(tick%5==0)seen.add(pose);
            check(map.roomAt(pose.x(),pose.y(),pose.z())>=0,
                "AI sister cannot walk off authored world geometry");
            check(map.traversable(pose.x(),pose.y(),pose.z()),
                "AI sister must obey physical world collisions");
            domain.advance(tick,4,1);
            GhostDomain.Action activity=companion.chooseActivity(domain.observe(),tick);
            if(activity!=null){
                check(activity==GhostDomain.Action.DISTURB||activity==GhostDomain.Action.WHISPER,
                    "Companion may not create independent untelegraphed strikes");
                // The event happens where the AI sister actually stands, even
                // when the controlled Twin remains elsewhere.
                var done=domain.dispatchCompanionMinor(new GhostDomain.Command(
                    activity,pose.room(),tick,GhostDomain.Source.AI),false);
                check(done.applied(),"AI uses actual room and shared energy/cooldown");
                check(done.room()==pose.room()&&domain.room()==4&&done.harm()==0,
                    "Independent sister changes neither the player Twin's location nor damage budget");
                minorAction=true;
            }
        }
        check(seen.stream().distinct().count()>6,
            "AI sister must move independently without being dragged by human pose");
        check(Math.hypot(start.x()-companion.pose().x(),start.z()-companion.pose().z())>.05
            ||seen.stream().distinct().count()>7,
            "AI sister must not remain permanently attached to an offset");
        check(minorAction,"AI sister must sometimes perform real shared-cost ghost activity");
        var packet=companion.playerFrame(false,210);
        check(packet.startsWith("GCOMPANION|")&&packet.split("\\|").length==7,
            "Ghost player must receive a distinct moving AI companion pose");
        check(companion.playerFrame(true,210).contains("|1|210"),
            "Ancient Twins must use the approved green companion appearance");
        check(GhostIdentity.Type.TWINS.baseType()==GhostIdentity.Type.TWINS,
            "Both actors remain a single species and ritual identity");

        // A heard sound beyond a door must not make the other Twin open it
        // telekinetically across the room. The door opens only at physical reach.
        HouseMap doorwayMap=new HouseMap();
        var calm=new MissionWeather.State(EvidenceSignals.Season.AUTUMN,
            EvidenceSignals.Weather.CLEAR,MissionWeather.Mode.FIXED,1,0);
        boolean testedRemoteDoor=false;
        for(int home=0;home<HouseMap.INDOOR_ROOMS&&!testedRemoteDoor;home++){
            var homeSpawn=doorwayMap.spawn(home);
            var stationary=new LanGhostSpatial.Pose(home,homeSpawn.x(),
                homeSpawn.y(),homeSpawn.z(),180);
            var walker=new TwinCompanionAi(doorwayMap,stationary,20261021L);
            int current=walker.pose().room();
            for(var door:doorwayMap.level().portalsFrom(current)){
                int next=door.a()==current?door.b():door.a();
                if(!door.door()||next>=HouseMap.EXTRACTION_ROOM
                        ||!doorwayMap.ghostExits(current).contains(next)
                        ||doorwayMap.doorOpen(door.id())
                        ||Math.hypot(walker.pose().x()-door.x(),walker.pose().z()-door.z())<=2.0
                        ||!LanAcoustics.audible(doorwayMap,current,next,
                            LanGhostPursuit.Sound.RITUAL,calm))continue;
                walker.heard(next,doorwayMap.centerX(next),doorwayMap.centerZ(next),
                    LanGhostPursuit.Sound.RITUAL,1,calm);
                check(!doorwayMap.doorOpen(door.id()),
                    "Hearing through a closed door must not remotely open it");
                walker.advance(1,stationary);
                check(!doorwayMap.doorOpen(door.id()),
                    "Selecting a neighboring room must not open a distant door");
                testedRemoteDoor=true;
                break;
            }
        }
        check(testedRemoteDoor,"The authored house must expose a reachable distant door");
        System.out.println("PASS human Twin keeps an autonomous AI sister, real geometry, ghost activity and one shared energy authority; doors require physical approach");
    }
}
