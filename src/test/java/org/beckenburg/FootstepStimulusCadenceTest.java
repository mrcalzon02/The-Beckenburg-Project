package org.beckenburg;

/** Fifty 20ms motion updates still mean ten 100ms host sound events. */
public final class FootstepStimulusCadenceTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        var pace=new FootstepStimulusCadence(6);
        int cues=0;
        for(int tick=0;tick<100;tick++)
            for(int packet=0;packet<5;packet++)
                if(pace.accept(0,tick))cues++;
        check(cues==100,"50Hz locomotion must not multiply 10Hz footstep stimuli");
        check(pace.accept(1,0),"independent hunter seat retains its first footstep");
        check(!pace.accept(0,99),"out-of-order duplicate is not a second event");
        check(pace.accept(0,100),"next legal tick is admitted without slowing motion");
        try{pace.accept(6,1);throw new AssertionError("accepted ghost seat");}
        catch(IllegalArgumentException correct){}
        System.out.println("PASS: realtime 50Hz input, one physical footstep stimulus per host tick, independent seats");
    }
}
