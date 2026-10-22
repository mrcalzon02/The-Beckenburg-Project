package org.beckenburg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Audits every scored stage without audio hardware or pixel screenshot fixtures. */
public final class GhostHauntingChoreographyTest {
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static long imageHash(int[] pixels){
        long h=1469598103934665603L;
        for(int p:pixels){h^=p;h*=1099511628211L;}
        return h;
    }
    static long energy(byte[] pcm,boolean right){
        long sum=0;
        for(int i=right?2:0;i<pcm.length;i+=4){
            short sample=(short)((pcm[i]&255)|((pcm[i+1]&255)<<8));
            sum+=Math.abs((int)sample);
        }
        return sum;
    }
    public static void main(String[] args){
        check(GhostHauntingChoreography.scores().size()==14,"14 distinct authored scores");
        Set<GhostHauntingChoreography.Motif> motifs=new HashSet<>();
        Set<GhostHauntingChoreography.Timbre> sounds=new HashSet<>();
        for(var score:GhostHauntingChoreography.scores()){
            check(GhostHauntingFolio.entry(score.id())!=null,"known folio event");
            check(score.beats().size()>=2,"a bespoke score needs multiple beats");
            motifs.add(score.motif());
            int last=-1;
            for(int i=0;i<score.beats().size();i++){
                var beat=score.beats().get(i);
                sounds.add(beat.timbre());
                check(beat.offsetTicks()>last,"monotonic music timeline");
                last=beat.offsetTicks();
                var event=new GhostHauntingFolio.Occurrence(
                    GhostHauntingFolio.entry(score.id()),4,100);
                check(GhostHauntingChoreography.dueIndex(event,100+beat.offsetTicks())==i,
                    "beat scheduled at exact fixed simulation tick");
                check(GhostHauntingChoreography.due(event,100+beat.offsetTicks())==beat,
                    "beat timeline matches score");
                var frame=new GhostHauntingChoreography.SoundFrame(score.id(),i,50,75,
                    100+beat.offsetTicks());
                check(GhostHauntingChoreography.decodeSound(frame.frame()).equals(frame),
                    "score packet roundtrip");
                byte[] first=GhostHauntingChoreography.synthesize(frame,80);
                byte[] again=GhostHauntingChoreography.synthesize(frame,80);
                check(Arrays.equals(first,again),"deterministic score PCM");
                check(first.length==LanAudio.SAMPLE_RATE*3,"16 kHz stereo PCM, 0.75s");
                check(energy(first,true)>energy(first,false),"positive audio pan favors right");
                check(energy(first,true)>5000,"sound has a real waveform");
                var muted=GhostHauntingChoreography.synthesize(frame,0);
                check(energy(muted,false)==0&&energy(muted,true)==0,"mute control");
                var neutral=new GhostHauntingChoreography.SoundFrame(score.id(),i,0,75,123);
                check(energy(GhostHauntingChoreography.synthesize(neutral,80),false)>0,
                    "neutral stereo remains playable");
            }
            var event=new GhostHauntingFolio.Occurrence(GhostHauntingFolio.entry(score.id()),4,100);
            int[] one=new int[80*52],two=new int[80*52],noRoom=new int[80*52];
            Arrays.fill(one,0x353535);Arrays.fill(two,0x353535);Arrays.fill(noRoom,0x353535);
            GhostHauntingFolio.paint(one,80,52,List.of(event),4,120);
            GhostHauntingFolio.paint(two,80,52,List.of(event),4,120);
            GhostHauntingFolio.paint(noRoom,80,52,List.of(event),3,120);
            check(imageHash(one)==imageHash(two),"synchronized deterministic visuals");
            check(imageHash(one)!=imageHash(noRoom),"bespoke animation visible in correct room");
            check(Arrays.stream(noRoom).allMatch(p->p==0x353535),
                "no visual leak to other rooms");
            check(GhostHauntingChoreography.dueIndex(event,event.untilTick())==-1,
                "score expires with the actual event");
        }
        check(motifs.size()==14,"every scored event has a unique visual gesture");
        check(sounds.size()>=10,"independent sound timbres");
        check(GhostHauntingChoreography.forEvent("wall_murmur")==null,
            "unscored events must retain generic folio fallback");
        for(GhostIdentity.Type ghost:GhostIdentity.Type.values())
            check(GhostManifestationVisual.appearance(CharacterVisuals.ghost(ghost),
                    LanGhostAnimation.Phase.LURK).equals(GhostManifestationVisual.UNREVEALED),
                "bespoke haunt visuals must not reveal character sheets");
        boolean invalid=false;
        try{GhostHauntingChoreography.decodeSound("HAUNTAUDIO|unknown|0|0|50|1");}
        catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"reject forged unknown soundtrack name");
        System.out.println("PASS: 14 authored choreographies, staged audio, pan, timing, rendering and concealment");
    }
}
