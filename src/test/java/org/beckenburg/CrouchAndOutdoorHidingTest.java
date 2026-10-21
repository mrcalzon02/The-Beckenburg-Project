package org.beckenburg;
import java.util.ArrayList;
import org.beckenburg.LevelDefinition.*;

/** Real body sweep under low cover, host posture authority and sparse foliage concealment. */
public final class CrouchAndOutdoorHidingTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var base=new HouseMap();var l=base.level();
        var colliders=new ArrayList<>(l.colliders());colliders.add(new Collider(8.8,1.30,6.8,.85,.7,.25,0));
        var low=new LevelDefinition(l.id(),"low-cover-test",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),l.sockets(),colliders,l.dock(),l.ambience());
        var map=new HouseMap(HouseMap.Site.HOUSE,low);map.configureHiding(100);
        check(!map.traversable(8.8,0,6.8),"standing capsule hits low overhead geometry");
        check(map.traversable(8.8,0,6.8,HunterPosture.CROUCH_HEIGHT),"crouched capsule fits without moving its feet");
        var w=new LanWorld(map);AuthoredTestSupport.place(map,w,0,new Vec(7.3,0,6.8));
        w.posture(0,true);int stamina=w.stamina(0);
        double before=w.body(0).x();w.move(0,100,0,1,true);
        check(Math.abs(w.body(0).x()-before-.31*HunterPosture.CROUCH_SPEED)<1e-8,"host enforces slow crouch even when sprint requested");
        check(w.stamina(0)==stamina,"crouching cannot consume sprint stamina");
        for(int tick=2;tick<=10;tick++)w.move(0,100,0,tick,true);
        check(Math.abs(w.body(0).y())<1e-8,"posture and movement preserve support elevation");
        boolean rejected=false;try{w.posture(0,false);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected&&w.crouched(0),"stand is rejected under low cover and posture remains crouched");
        var density=LanWorld.class.getDeclaredField("hidingDensity");density.setAccessible(true);density.setInt(w,100);
        check(w.toggleHide(0)&&w.hidden(0),"crouched hunter inside authored shrub may hide");
        w.revealOutdoorNear(9.2,0,6.8);check(!w.hidden(0),"nearby searching ghost exposes shrub refuge");
        check(OutdoorHiding.containing(map.level(),8.8,0,6.8,0)==null,"zero availability removes effective refuge");
        check(OutdoorHiding.containing(map.level(),8.8,0,6.8,100)!=null,"all-cover setting restores authored refuge");
        check(OutdoorHiding.containing(map.level(),10,0,6.8,100)==null,"near shrub is not inside shrub");
        check(w.postureFrame(0,10).equals("POSTURE|0|1|10"),"separate authoritative posture snapshot preserves body protocol");
        check(w.bodyFrame(0,10).split("\\|").length==7,"legacy body frame remains compatible");
        for(int tick=11;tick<=20;tick++)w.move(0,-100,0,tick,false);
        check(!w.posture(0,false)&&!w.crouched(0),"standing works again in open space");
        var solo=new InvestigationSession(base,InvestigationSession.Role.OBSERVER,20261021L);
        base.configureHiding(100);
        check(solo.posture(true,8.8,0,6.8)&&solo.crouched(),"solo shares physical posture");
        check(solo.toggleHide(8.8,0,6.8)&&solo.hidden(),"solo uses the same shrub pocket");
        check(solo.posture(false,8.8,0,6.8)&&!solo.hidden(),"standing exposes solo shrub concealment");
        System.out.println("PASS: crouch sweep, low-headroom stand rejection, sprint exclusion, shrub boundaries, search reveal and solo parity");
    }
}
