package org.beckenburg;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Random;

/** A 25-minute repeatable rules workload. Hunter exposure follows a fixed schedule,
 * so these counts compare resource pressure; they do not measure completed human rounds. */
public final class GhostPacingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static int ticksToSignature(GhostIdentity.Type type,int start){
        var domain=new GhostDomain(new HouseMap(),4,type);
        for(int t=1;t<=start;t++)domain.advance(t,-1,0);
        check(domain.spendFrenzyEnergy(100),"set up drained pool");
        int occupants=GhostTraits.of(type).charge()==GhostTraits.Charge.PRESENCE?1:0;
        int t=start;
        while(domain.energy()<domain.cost(GhostDomain.Action.SIGNATURE)){
            domain.advance(++t,-1,occupants);
            check(t<start+1800,"eligible feed failed to recharge");
        }
        return t-start;
    }
    private static void workload(GhostIdentity.Type type){
        HouseMap map=new HouseMap();
        GhostDomain ghost=new GhostDomain(map,4,type);
        var ai=new LanGhostPursuit(map,618753L+type.ordinal(),type);
        var frenzy=new GhostFrenzy(type);
        var cadence=new GhostActionCadence(type,new Random(7151L+type.ordinal()));
        var counts=new EnumMap<GhostDomain.Action,Integer>(GhostDomain.Action.class);
        long sequence=0;int localCue=-100,heardRoom=-1,firstSignature=-1;
        // Each minute a team spends 24s within audible range, with a piece of
        // equipment sounding every 4s. No target is in striking reach.
        for(int tick=1;tick<=25*GhostPacing.TICKS_PER_MINUTE;tick++){
            boolean present=tick%600<240;
            if(present&&tick%40==0){
                var at=SafeAreaBarrier.ghostSpawn(map,ghost.room());
                ai.heard(ghost.room(),ghost.room(),at.x(),at.z(),LanGhostPursuit.Sound.EQUIPMENT,tick);
                localCue=tick;heardRoom=ghost.room();frenzy.provoke(3);
            }
            ghost.advance(tick,present&&tick-localCue<12?heardRoom:-1,present?1:0);
            frenzy.advance(tick);
            boolean due=ai.signatureAvailable(tick);
            if(!due)frenzy.consider(tick,ghost,present,Math.max(30,100-tick/150));
            var state=ghost.observe();
            GhostDomain.Action action=null;int target=state.room();
            int home=frenzy.active()?-1:ghost.homewardStep();
            if(home>=0){action=GhostDomain.Action.DRIFT;target=home;}
            else{
                var decision=ai.decide(state,false);
                if(decision!=null){action=decision.action();target=decision.targetRoom();}
                if(frenzy.phase()!=GhostFrenzy.Phase.CALM&&action!=GhostDomain.Action.DRIFT)action=null;
                if(action==null&&frenzy.phase()==GhostFrenzy.Phase.CALM&&cadence.due(tick))
                    action=cadence.fallback(state,false,due,due&&ghost.energy()<ghost.cost(GhostDomain.Action.SIGNATURE));
            }
            if(action==null)continue;
            var result=ghost.dispatch(new GhostDomain.Command(action,target,++sequence,GhostDomain.Source.AI));
            check(result.applied(),"AI requested illegal "+action+" on "+type+" at "+tick);
            counts.merge(action,1,Integer::sum);
            cadence.completed(tick,result);
            if(action==GhostDomain.Action.SIGNATURE){
                if(firstSignature<0)firstSignature=tick;
                ai.resetSignature(tick);
            }
        }
        check(firstSignature>0,"starved signature: "+type);
        check(100+ghost.gained()-ghost.ordinarySpent()-ghost.frenzySpent()-ghost.repelled()==ghost.energy(),
            "energy accounting drift: "+type);
        int early=ticksToSignature(type,1),late=ticksToSignature(type,GhostPacing.FULL_PRESSURE_TICK);
        check(early>late&&counts.getOrDefault(GhostDomain.Action.DRIFT,0)>0,"missing pacing or patrol");
        System.out.printf(Locale.ROOT,"%s: recharge empty to signature %.1fs/%.1fs opening/late; "
            +"25min actions %s, frenzies %d, first signature %.1fmin%n",
            type,early/10.,late/10.,counts,frenzy.attempts(),firstSignature<0?-1:firstSignature/600.);
    }
    public static void main(String[] args){
        for(var type:new GhostIdentity.Type[]{GhostIdentity.Type.DEMON,GhostIdentity.Type.SPIRIT,
                GhostIdentity.Type.HANTU,GhostIdentity.Type.POLTERGEIST})workload(type);
        check(GhostPacing.rechargePercent(999999)==GhostPacing.rechargePercent(12000),"late cap");
        check(GhostPacing.minorPeriod(29,100,200)<GhostPacing.minorPeriod(29,100,50),"frequency setting");
        var map=new HouseMap();
        int early=0,late=0;
        for(int seed=0;seed<512;seed++){
            var ai=new LanGhostPursuit(map,seed);
            if(ai.signatureAvailable(400))early++;
            if(ai.signatureAvailable(12000))late++;
        }
        check(early>0&&early<80&&late>early,"stochastic opportunity bounded, no minimum time");
    }
}
