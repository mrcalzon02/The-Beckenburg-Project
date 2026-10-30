package org.beckenburg;

import java.util.List;
import java.util.Random;

/** Forced action must be random, bounded, legal, and shared across human/AI ghost controllers. */
public final class GhostActionCadenceTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        var cadence=new GhostActionCadence(GhostIdentity.Type.DEMON,new Random(31987));
        boolean[] periods=new boolean[GhostActionCadence.MAX_PERIOD+1];
        var success=new GhostDomain.Resolution(true,"accepted",GhostDomain.Action.DISTURB,
                4,70,3,0,false);
        int now=0,unique=0,minimum=Integer.MAX_VALUE,maximum=0;
        for(int i=0;i<2000;i++){
            int duration=cadence.dueTick()-now;
            check(duration>=GhostActionCadence.MIN_PERIOD
                    &&duration<=GhostActionCadence.MAX_PERIOD,"forced interval outside 5-45 seconds");
            if(!periods[duration]){periods[duration]=true;unique++;}
            minimum=Math.min(minimum,duration);
            maximum=Math.max(maximum,duration);
            check(!cadence.due(now+duration-1)&&cadence.due(now+duration),
                    "host-only deadline, independent of rendering or position");
            now+=duration;
            cadence.completed(now,success);
        }
        check(unique>=200&&minimum<65&&maximum>435,"random intervals not exploring the entire range");
        check(cadence.successfulEvents()==2000,"only successful haunting events count");
        int deadline=cadence.dueTick();
        cadence.completed(now,new GhostDomain.Resolution(true,"accepted",GhostDomain.Action.DRIFT,4,68,0,0,false));
        cadence.completed(now,new GhostDomain.Resolution(false,"warded",GhostDomain.Action.WHISPER,4,68,0,0,false));
        check(cadence.dueTick()==deadline&&cadence.successfulEvents()==2000,
                "drifting or rejected actions must not postpone forced activity");

        var selection=new GhostActionCadence(GhostIdentity.Type.DEMON,new Random(2026));
        var state=new GhostDomain.Observation(180,4,4,100,true,true,List.of(5),
                List.of(GhostDomain.Action.DRIFT,GhostDomain.Action.STRIKE,
                        GhostDomain.Action.DISTURB,GhostDomain.Action.WHISPER));
        int disturb=0,whisper=0,strike=0;
        for(int i=0;i<600;i++){
            var noVictim=selection.fallback(state,false);
            check(noVictim==GhostDomain.Action.DISTURB
                    ||noVictim==GhostDomain.Action.WHISPER,"fallback inferred or struck an unseen target");
            if(noVictim==GhostDomain.Action.DISTURB)disturb++;else whisper++;
            var allowed=selection.fallback(state,true);
            if(allowed==GhostDomain.Action.STRIKE)strike++;
        }
        check(disturb>100&&whisper>100&&strike>100,
                "fallback must be random among viable choices, not first-in-list");
        check(selection.fallback(new GhostDomain.Observation(181,4,4,0,false,false,
                List.of(),List.of(GhostDomain.Action.DRIFT)),false)==null,
                "illegal, warded or exhausted ghost may not fabricate evidence");
        System.out.println("PASS: 5-45s independently randomized ghost deadlines, legal fallback and no idle bypass");
    }
}
