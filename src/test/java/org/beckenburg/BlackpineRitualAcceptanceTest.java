package org.beckenburg;
import java.util.*;
/** Authored-map simulation acceptance, without claiming a real network/human hunt. */
public final class BlackpineRitualAcceptanceTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        var map=BlackpineLevel.map();int cases=0;
        for(var rite:GhostIdentity.Rite.values()){
            var type=Arrays.stream(GhostIdentity.Type.values()).filter(t->!t.ancient()&&!t.elder()&&t.rite()==rite).findFirst().orElseThrow();
            var mission=new LanCase(4,map);mission.configureHunters(6);mission.configure(type,true);
            var site=RitualSites.site(map,rite);
            check(site.near(new LanWorld.Body(site.x(),site.y(),site.z(),0,site.room())),"actual graded ritual position accepted");
            check(!site.near(new LanWorld.Body(site.x(),site.y()+3,site.z(),0,site.room())),"wrong elevation rejected");
            check(mission.identify(0,type,1).accepted()&&mission.ward(0,4).accepted(),"deduction and anchor ward");
            check(!mission.performRite(0,rite,site.room(),false).accepted(),"remote ritual activation rejected");
            check(mission.performRite(0,rite,site.room(),true).accepted(),"actual Blackpine rite accepted");
            int[] defended={site.room(),14,14,14,14,14},empty={14,14,14,14,14,14};
            for(int i=0;i<15;i++)mission.advance(defended);
            mission.applyGhostEffect(site.room(),defended,new GhostDomain.Resolution(true,"validated disturbance",GhostDomain.Action.DISTURB,site.room(),40,1,0,true));
            check(mission.ritualProgress()==3,"ghost can delay defended Blackpine rite");
            check(mission.advance(empty)==LanCase.RitualAdvance.PAUSED,"unattended rite pauses");
            for(int i=0;i<19;i++)mission.advance(empty);
            check(mission.ritualProgress()==-1&&mission.outcome()==LanCase.Outcome.ACTIVE,"abandoned rite breaks without a win");
            check(mission.performRite(0,rite,site.room(),true).accepted(),"explicit restart");
            for(int i=0;i<mission.requiredRitualTicks();i++)mission.advance(defended);
            var expected=switch(rite){case CLEANSING->LanCase.Outcome.CLEANSED;case BANISHMENT->LanCase.Outcome.BANISHED;case ABSOLUTION->LanCase.Outcome.ABSOLVED;};
            check(mission.outcome()==expected,"correct authored ritual outcome "+rite);cases++;
        }
        var retreat=new LanCase(4,map);check(retreat.extract(0,14).accepted(),"canonical van extraction works");
        System.out.println("PASS "+cases+" Blackpine rite simulations: graded locations, role/remote rejection, disturbance, abandonment, restart, distinct outcomes and van extraction");
    }
}
