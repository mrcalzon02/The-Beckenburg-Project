package org.beckenburg;

import java.util.HashSet;
import java.util.Set;

/**
 * Real ghost-side encounter: Hunter issues scans from its own physical
 * location, makes a measured field notebook, hears a manifested ghost, and
 * deploys a finite real-world counter. No automatic evidence confirmation.
 */
public final class GhostPracticeInvestigationTest {
    private static void ok(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
    public static void main(String[] args){
        for(HouseMap.Site site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            var practice=new GhostPracticeSession(map,20261015L);
            var hunter=practice.hunter();
            for(int i=1;i<=320;i++)practice.tick();
            var notes=hunter.observations();
            ok(notes.size()>=4,"Hunter must actually investigate with tools on "+site);
            boolean emf=false,thermal=false;
            Set<Integer> ticks=new HashSet<>();
            for(var note:notes){
                ok(note.tick()>=1&&note.tick()<=320
                    &&ticks.add(note.tick()),"Real instrument cooldown and unique timestamps");
                ok(note.room()>=0&&note.room()<HouseMap.EXTRACTION_ROOM,
                    "Measurements use the Hunter's current walkable room");
                ok(note.tool()==EquipmentLoadout.Item.EMF
                    ||note.tool()==EquipmentLoadout.Item.THERMOMETER,
                    "Only issued real instruments may make readings");
                ok(!note.reading().contains("CONFIRMED")
                    &&!note.reading().contains("ENTITY IDENTIFIED"),
                    "Never auto-confirm a ghost from a numeric scan");
                if(note.tool()==EquipmentLoadout.Item.EMF){
                    emf=true;ok(note.reading().matches("EMF [0-9]{1,2}/10"),
                        "Authoritative environment measured in EMF level units");
                }else{
                    thermal=true;ok(note.reading().startsWith("Air ")
                        &&note.reading().contains("°C")
                        &&note.reading().contains("°F"),
                        "World thermometer reports physical Celsius and Fahrenheit");
                }
            }
            ok(emf&&thermal,"Hunter uses more than one piece of equipment on "+site);
            ok(hunter.instrumentCount()==notes.size(),"Only actual observations count");
            var manifestation=practice.act(GhostDomain.Action.MANIFEST,
                practice.ghostRoom());
            ok(manifestation!=null&&manifestation.applied(),
                "Ghost may produce a legitimate audible manifestation during tutorial");
            int initial=practice.counterCharges(HunterCounters.Kind.CROSS)
                +practice.counterCharges(HunterCounters.Kind.WARD);
            practice.tick();
            int used=practice.counterCharges(HunterCounters.Kind.CROSS)
                +practice.counterCharges(HunterCounters.Kind.WARD);
            ok(used==initial-1,
                "After hearing or seeing the manifested Ghost, Hunter must consume one real defensive charge");
            ok(!practice.counterMarks().isEmpty(),
                "The defensive device must exist in authored 3D world, not just update a label");
            ok(hunter.health()==100,
                "Merely seeing a manifestation must not cause a fictitious strike");
            ok(practice.outcome()==GhostPracticeSession.Outcome.ACTIVE,
                "Evidence or a defensive ward must not silently win the mission");
            // A Hunter's inferences are formed from numeric readings, not
            // the private ghost anchor or identity.
            var analyst=new GhostPracticeHunterAi(map,4,61896L);
            ok(analyst.suspectedRoom()==-1,"No unwitnessed ghost location hints");
            for(int i=0;i<52;i++){
                int sampleTick=1+i*65;
                EquipmentLoadout.Item tool=analyst.toolDue(sampleTick);
                ok(tool!=null,"Genuinely issued instrument due");
                analyst.observed(sampleTick,tool,
                    tool==EquipmentLoadout.Item.EMF?
                        (i==0?"EMF 9/10":"EMF 3/10"):
                        "Air 12.0°C / 53.6°F");
            }
            ok(analyst.instrumentCount()==52&&analyst.observations().size()==48,
                "Bounded field notebook must not reset scanner alternation at capacity");
            ok(analyst.suspectedRoom()==4&&analyst.suspicion(4)>=3,
                "Only local abnormal readings create a tentative hotspot");
            ok(analyst.toolDue(1+52*65)==EquipmentLoadout.Item.EMF,
                "Sensor selection must remain stable after rolling notebook retention");
            var scout=new GhostPracticeHunterAi(map,4,14573L);
            var cue=LanSound.project(map,scout.listener(),4,map.centerX(4),
                map.floorY(4),map.centerZ(4),LanSound.Cue.DISTURB,320);
            ok(cue!=null,"Real world sound must pass through local acoustic projection");
            scout.hear(cue);
            ok(scout.pursuingSound(320),
                "AI must investigate a walkable local sound bearing, not auto-track the Ghost");
            var beforeScout=scout.position();
            scout.advance(321);
            ok(scout.intent().contains("audible direction"),
                "Audio causes a local on-foot search rather than arbitrary portal travel");
            ok(Math.hypot(beforeScout.x()-scout.position().x(),
                beforeScout.z()-scout.position().z())<=.191
                &&map.traversable(scout.position().x(),scout.position().y(),scout.position().z()),
                "Sound search movement obeys exactly the same sweep and collision as patrol");
            System.out.println("PASS "+site+
                ": AI instruments, finite actual counter, geometry and no automatic confirmation");
        }
    }
}
