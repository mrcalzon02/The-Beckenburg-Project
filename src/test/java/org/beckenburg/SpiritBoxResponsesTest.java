package org.beckenburg;

import java.util.Arrays;

/** Functional Spirit Box event, speech-asset and accessibility regression. */
public final class SpiritBoxResponsesTest {
    private static void ok(boolean value,String reason){
        if(!value)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        int tick=300;
        ok(SpiritBoxResponses.qualifies(4,4,4,tick,tick+10,true,true,1.25,2.1,-1),
            "fresh heard voice, same floor/room, equipment in range");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+10,true,true,1.25,2.1,tick),
            "one host event cannot generate repeated Spirit Box evidence");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+46,true,true,1.25,2.1,-1),
            "expired ghost event must remain static");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick-1,true,true,1.25,2.1,-1),
            "future event cannot be heard in advance");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+1,false,true,1.25,2.1,-1),
            "non-vocal ghost activity cannot speak through radio");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+1,true,false,1.25,2.1,-1),
            "difficulty/evidence permission must be honored");
        ok(!SpiritBoxResponses.qualifies(4,5,4,tick,tick+1,true,true,1.25,2.1,-1),
            "ghost must remain in the vocal event room");
        ok(!SpiritBoxResponses.qualifies(3,4,4,tick,tick+1,true,true,1.25,2.1,-1),
            "a hunter cannot collect speech in a distant room");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+1,true,true,4.5,2.1,-1),
            "real tier reach, not a global yes/no button");
        ok(!SpiritBoxResponses.qualifies(4,4,4,tick,tick+1,true,true,Double.NaN,2.1,-1),
            "reject invalid physical position");
        ok(SpiritBoxResponses.choose(101,2)==SpiritBoxResponses.choose(101,2),
            "host replay must preserve the selected line");
        ok(SpiritBoxResponses.observation(null).toLowerCase().contains("static"),
            "silence logs an authentic raw scan");
        var lines=SpiritBoxResponses.Phrase.values();
        ok(lines.length==4,"exactly four generic initial utterances");
        byte[][] samples=new byte[lines.length][];
        for(int i=0;i<lines.length;i++){
            var phrase=lines[i];
            ok(!phrase.words.isBlank()&&phrase.cue.caption().contains("SPIRIT BOX"),
                "voice must have a matching spoken subtitle for hearing access");
            ok(RecordedSfx.isPackaged(phrase.cue),
                "real recorded speech must be in the shipped classpath: "+phrase);
            samples[i]=RecordedSfx.render(new LanSound.CueEvent(phrase.cue,0,92,tick),80);
            ok(samples[i]!=null && samples[i].length>=20000
                    &&samples[i].length<=16000*4*4 &&samples[i].length%4==0,
                "missing/truncated decoded voice: "+phrase);
            boolean audible=false;
            for(byte value:samples[i])if(value!=0){audible=true;break;}
            ok(audible,"silent recorded phrase "+phrase);
            ok(!SpiritBoxResponses.observation(phrase).contains("detected"),
                "do not replace an actual heard phrase with a yes/no detector label");
        }
        for(int i=0;i<samples.length;i++)for(int j=i+1;j<samples.length;j++)
            ok(!Arrays.equals(samples[i],samples[j]),"distinct spoken response files");
        var hiss=LanAudio.synthesize(new LanSound.CueEvent(
            LanSound.Cue.SPIRIT_BOX_STATIC,0,35,tick));
        ok(hiss.length>10000,"radio static must be actual playable PCM");
        boolean hissAudible=false;
        for(byte sample:hiss)if(sample!=0){hissAudible=true;break;}
        ok(hissAudible,"a silent radio is not a Spirit Box");
        System.out.println("PASS Spirit Box: real speech WAVs, static PCM, host vocal/freshness rules");
    }
}
