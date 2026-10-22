package org.beckenburg;

import java.util.List;

/** Ghost-side communication is audible, authored by the embodied Hunter's
 * real actions, and never an omniscient transcript of their private state. */
public final class GhostPracticeCommsTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        for(HouseMap.Site site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            var simulation=new GhostPracticeSession(map,20261015L);
            ok(simulation.heardHunterChat().isEmpty(),
                "The player may not receive a Hunter transcript before anyone speaks");
            simulation.tick(); // A real EMF instrument is used by the nearby Hunter.
            List<LanChat.Entry> nearby=simulation.heardHunterChat();
            ok(!nearby.isEmpty()&&nearby.stream().anyMatch(e->e.message().contains("EMF")),
                "A physically nearby Ghost hears the Hunter announce measured EMF");
            ok(nearby.stream().allMatch(e->e.channel()==LanChat.Channel.LOCAL&&e.seat()==0),
                "No invisible radio intercepts or fabricated speaking actors");
            ok(nearby.stream().allMatch(e->e.tick()==1),
                "Only speech delivered on the actual event tick is visible");
            for(int t=2;t<=66;t++)simulation.tick();
            List<LanChat.Entry> more=simulation.heardHunterChat();
            ok(more.stream().anyMatch(e->e.message().contains("Thermometer")&&
                    e.message().contains("Air ")),
                "An actual timed thermal scan appears in the in-game chat");
            for(var e:more){
                ok(!e.message().contains("IDENTIFIED")&&!e.message().contains("Confirmed")&&
                    !e.message().contains("anchorRoom"),
                    "AI communications must not report hidden clues as confirmed facts");
                ok(LanChat.Entry.decode(e.frame()).equals(e),
                    "Overheard speech uses the same safe game chat protocol");
            }
            var actor=simulation.hunter();
            ok(actor.drainSpeech().isEmpty(),
                "Speech must have been drained and routed, not replayed later");
            int before=more.size();
            actor.observed(131,actor.toolDue(131),"EMF 9/10"); // Real timed reading, intentionally extreme.
            ok(actor.suspectedRoom()==actor.room(),"Hotspot hypothesis comes from its measured room");
            List<GhostPracticeHunterAi.Utterance> observations=actor.drainSpeech();
            ok(observations.stream().anyMatch(u->u.words().contains("Possible activity")&&
                    u.words().contains("More checks needed")),
                "AI voices suspicion without knowing the hidden Ghost identity");
            ok(observations.stream().allMatch(u->u.room()==actor.room()
                &&map.traversable(u.x(),u.y(),u.z())),
                "Every spoken line originates from a physical, traversable Hunter position");
            ok(simulation.heardHunterChat().size()==before,
                "Unrouted thoughts cannot spontaneously appear in the Ghost UI");
            var distant=map.van().spawn(0);
            var ghostVan=new LanWorld.Body(distant.x(),map.floorY(HouseMap.EXTRACTION_ROOM),distant.z(),0,
                HouseMap.EXTRACTION_ROOM);
            var hall=actor.listener();
            ok(!LanChat.localVisible(map,hall,ghostVan),
                "A Hunter inside the house cannot be heard across the van threshold");
            ok(simulation.outcome()==GhostPracticeSession.Outcome.ACTIVE,
                "Speech or a suspicion must never finish the game");
            System.out.println("PASS "+site+
                ": raw AI field chat, tentative suspicion and positional eavesdropping");
        }
    }
}
