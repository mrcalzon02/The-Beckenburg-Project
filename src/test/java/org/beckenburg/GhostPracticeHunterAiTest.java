package org.beckenburg;

/** A human ghost faces an actual, physically moving and sound-reactive AI Hunter. */
public final class GhostPracticeHunterAiTest {
    private static void ok(boolean success,String why){if(!success)throw new AssertionError(why);}
    public static void main(String[] args){
        for(HouseMap.Site site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            int room=4;
            var hunter=new GhostPracticeHunterAi(map,room,73821L);
            var origin=hunter.position();
            ok(hunter.room()==room&&map.traversable(origin.x(),origin.y(),origin.z()),
                "Hunter must physically spawn in valid geometry");
            for(int tick=1;tick<=320;tick++)hunter.advance(tick);
            ok(hunter.position().equals(origin),
                "Training opponent must allow the player to learn legal actions");
            var noise=LanSound.project(map,hunter.listener(),room,
                map.centerX(room),map.floorY(room),map.centerZ(room),
                LanSound.Cue.DISTURB,320);
            ok(noise!=null,"Actual same-room environmental disturbance must be audible");
            hunter.hear(noise);
            ok(hunter.alert()>0&&hunter.intent().contains("disturbance"),
                "Audio observation should affect AI state without a hidden ghost identity");
            int moved=0;
            for(int tick=321;tick<=1050;tick++){
                var before=hunter.position();
                hunter.advance(tick);
                var after=hunter.position();
                double step=Math.hypot(after.x()-before.x(),after.z()-before.z());
                ok(step<=.191,"AI Hunter must not jump to the destination");
                ok(map.traversable(after.x(),after.y(),after.z()),
                    "Every AI step must obey player-grade collision");
                if(step>.03)moved++;
            }
            ok(moved>12,"AI opponent must move autonomously in "+site);
            ok(hunter.room()!=HouseMap.EXTRACTION_ROOM,
                "AI ghost-practice opponent may not illegally use the safe van");
            var session=new GhostPracticeSession(map,73821L);
            ok(session.visibleHunterBodies().isEmpty(),
                "Ghost must not see a Hunter pose before a legal presence observation");
            session.tick();
            if(session.observation().hearsHunter())
                ok(session.visibleHunterBodies().size()==1,
                    "Visible ghost-practice Hunter must be a real sprite-positioned actor");
            System.out.println("PASS "+site+": embodied AI Hunter patrol, raw acoustic reaction and collision");
        }
    }
}
