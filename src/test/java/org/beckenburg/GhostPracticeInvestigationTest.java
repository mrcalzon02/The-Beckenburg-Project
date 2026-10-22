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
            System.out.println("PASS "+site+
                ": AI instruments, finite actual counter, geometry and no automatic confirmation");
        }
    }
}
