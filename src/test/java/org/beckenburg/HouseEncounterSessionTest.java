package org.beckenburg;

import java.util.*;

/** Actual solo runtime integration, physical ownership and camera/evidence negatives. */
public final class HouseEncounterSessionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static List<InvestigationSession.WorldCue> step(InvestigationSession session,GhostDomain ghost,LooseClutter.Observer observer){
        // Isolate the location sequence from random ordinary haunts using the
        // existing suppression mechanic; no encounter test/force hook exists.
        ghost.suppress(100);session.clutterLook(observer.yaw(),observer.pitch());
        session.tick(observer.x(),observer.y(),observer.z(),true);return session.drainWorldSounds();
    }
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFurnishings.map();var session=new InvestigationSession(map,InvestigationSession.Role.CLERIC,1,1,GhostIdentity.Type.SPIRIT);
        var field=InvestigationSession.class.getDeclaredField("ghost");field.setAccessible(true);var ghost=(GhostDomain)field.get(session);
        var lights=new RoomLighting();session.connectLighting(lights);
        var van=map.spawn(14);var waiting=new LooseClutter.Observer(0,van.x(),van.y(),van.z(),0,0,true);
        for(int i=0;i<HouseEncounter.EARLIEST;i++)check(step(session,ghost,waiting).stream().noneMatch(c->c.cue()==LanSound.Cue.TOY_WINDUP),"No van-only encounter");
        var near=HouseEncounterTest.study(map,0);var old=map.clutter().authoredItem("wooden-train");
        for(int i=0;i<HouseEncounter.NOTICE_TICKS;i++)step(session,ghost,near);
        var portal=map.level().portals().stream().filter(p->p.door()&&p.joins(5,7)).findFirst().orElseThrow();if(map.doorOpen(portal.id()))map.toggleDoor(portal.id());
        var away=new LooseClutter.Observer(0,18.35,2.8,13.95,0,0,true);check(map.traversable(away.x(),away.y(),away.z()),"Supported source hall fixture");
        int cues=0;
        for(int i=0;i<HouseEncounter.EMPTY_TICKS;i++)for(var cue:step(session,ghost,away))if(cue.cue()==LanSound.Cue.TOY_WINDUP){
            check(cue.room()==7&&cue.x()==old.x()&&cue.z()==old.z(),"Actual solo cue stays at the authored physical object");cues++;
        }
        check(cues==1&&Math.abs(map.clutter().authoredItem("wooden-train").yaw()-old.yaw()-Math.PI/2)<1e-10,"Normal solo tick owns one actual object turn");
        check(lights.breaker()&&lights.sourceRunning()&&session.health()>0&&session.evidenceCount()==0&&!session.apparitionPhotographed(),"Location effect grants no electrical trip, damage or evidence");
        check(session.hauntings().stream().noneMatch(e->e.entry().cue()==LanSound.Cue.TOY_WINDUP),"Location cue is not a ghost-folio event");
        var camera=new EquipmentLoadout();camera.select(3);
        check(!session.ghostManifesting()&&!session.notePhotoFigure(),"Location encounter creates neither a ghost figure nor a photograph to review");
        session.clutterLook(near.yaw(),near.pitch());
        check(session.useEquipment(camera,near.x(),near.y(),near.z(),near.yaw()),"Photographing the actual turned toy accepts a normal shutter exposure");
        check(!session.apparitionPhotographed()&&session.evidenceCount()==0,"A toy photograph does not automatically report a figure or grant instrument evidence");
        check(camera.usesLeft(EquipmentLoadout.Item.PHOTO_CAMERA)==5,"Real shutter still consumes normal film");
        HouseEncounterTest.support(map);
        for(int i=0;i<30;i++)check(step(session,ghost,near).stream().noneMatch(c->c.cue()==LanSound.Cue.TOY_WINDUP),"Solo return never replays location cue");
        check(LanSound.Cue.TOY_WINDUP.tier()==LanSound.Tier.AMBIENT,"No paranormal-danger audio classification");
        // A notebook entry is the investigator's claim; only an optically
        // qualified figure in the actual exposure may earn a photo reward.
        check(session.notePhotoFigure()&&session.apparitionPhotographed(),"A mistaken manual note remains possible after reviewing the toy photograph");
        check(session.evidenceCount()==0,"A manual toy-photograph claim does not grant instrument evidence");
        check(session.extract(van.x(),van.y(),van.z())&&session.outcome()==InvestigationSession.Outcome.RETREATED,"Return to the actual van completes the case");
        check(session.payout()==0,"A mistaken claim about the turned toy cannot earn an apparition photo reward");
        System.out.println("PASS actual solo tick train sequence, physical source/ownership, independent power, normal shutter/film, manual claim and zero ghost/evidence/photo reward");
    }
}
