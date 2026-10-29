package org.beckenburg;

import javax.swing.JButton;
import javax.swing.JTabbedPane;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** All event vocabulary must have a real, bounded deterministic PCM fallback. */
public final class SoundCoverageTest {
    public static void main(String[] args) {
        Set<String> unique = new HashSet<>();
        for (LanSound.Cue cue : LanSound.Cue.values()) {
            if (!unique.add(cue.name())) throw new AssertionError("Duplicate cue "+cue);
            if (cue.caption().isBlank()) throw new AssertionError("Missing accessibility caption "+cue);
            var center = new LanSound.CueEvent(cue, 0, 80, 1);
            if (LanSound.decode(center.frame()).cue() != cue)
                throw new AssertionError("Cannot decode "+cue);
            byte[] pcm=LanAudio.synthesize(center);
            if(pcm.length == 0 || pcm.length > 16000*4*4 || pcm.length % 4 != 0)
                throw new AssertionError("Invalid stereo PCM "+cue+": "+pcm.length);
            if(!Arrays.equals(pcm,LanAudio.synthesize(center)))
                throw new AssertionError("Nondeterministic cue "+cue);
            byte[] pan=LanAudio.synthesize(new LanSound.CueEvent(cue,100,80,1));
            if(pan.length!=pcm.length)throw new AssertionError("Pan changed duration "+cue);
        }
        // The provenance-checked NOX clips are real packaged source recordings,
        // not the synthetic importer fixture; build scripts MUST stage both.
        for(LanSound.Cue cue:new LanSound.Cue[]{LanSound.Cue.PICKUP,LanSound.Cue.DEVICE_ON,LanSound.Cue.DEVICE_OFF,
                LanSound.Cue.DROP,LanSound.Cue.SCANNER,LanSound.Cue.ELECTRIC_HUM,
                LanSound.Cue.EQUIPMENT,LanSound.Cue.DOTS_MOTOR,LanSound.Cue.MONITOR_HUM}){
            if(!RecordedSfx.isPackaged(cue))
                throw new AssertionError("Required decoded NOX recording missing: "+cue);
            byte[] real=RecordedSfx.render(new LanSound.CueEvent(cue,0,75,2),80);
            if(real==null||real.length<800||real.length>16000*4
                    ||real.length%4!=0)
                throw new AssertionError("Invalid short recorded NOX clip "+cue);
            boolean nonzero=false;
            for(byte sample:real)if(sample!=0){nonzero=true;break;}
            if(!nonzero)throw new AssertionError("Silent NOX recording "+cue);
        }
        // Three unique source-verified NOX rock-walking takes, not three names
        // pointing to the same sound or procedural fallback.
        byte[][] stone=new byte[3][];
        for(int i=0;i<stone.length;i++){
            var event=new LanSound.CueEvent(LanSound.Cue.STEP_STONE,0,65,i);
            stone[i]=RecordedSfx.render(event,80);
            if(stone[i]==null||stone[i].length<1600||stone[i].length>16000*4*4
                    ||stone[i].length%4!=0)
                throw new AssertionError("Missing or undecodable rock footstep "+i);
            boolean nonzero=false;
            for(byte value:stone[i])if(value!=0){nonzero=true;break;}
            if(!nonzero)throw new AssertionError("Silent recorded rock footstep "+i);
        }
        if(Arrays.equals(stone[0],stone[1])||Arrays.equals(stone[1],stone[2])
                ||Arrays.equals(stone[0],stone[2]))
            throw new AssertionError("Recorded rock footsteps are not distinct");
        if(!Arrays.equals(stone[0],RecordedSfx.render(
                new LanSound.CueEvent(LanSound.Cue.STEP_STONE,0,65,3),80)))
            throw new AssertionError("Stone recorded variation is not deterministic");
        // Owner-exported first-party ReverieVR Foley; not NOX or external CC0.
        for(LanSound.Cue cue:new LanSound.Cue[]{
                LanSound.Cue.UI_CLICK,LanSound.Cue.DOOR,LanSound.Cue.BOOK,LanSound.Cue.PURCHASE}){
            if(!RecordedSfx.isPackaged(cue))
                throw new AssertionError("Owner-reused recording not packaged: "+cue);
            byte[] real=RecordedSfx.render(new LanSound.CueEvent(cue,0,75,2),80);
            if(real==null||real.length<800||real.length>16000*4||real.length%4!=0)
                throw new AssertionError("Invalid owner-reused recording "+cue);
            boolean audible=false;
            for(byte sample:real)if(sample!=0){audible=true;break;}
            if(!audible)throw new AssertionError("Silent owner-reused recording "+cue);
        }
        if(Arrays.equals(
                LanAudio.synthesize(new LanSound.CueEvent(LanSound.Cue.UI_REJECT,0,70,7)),
                LanAudio.synthesize(new LanSound.CueEvent(LanSound.Cue.PURCHASE,0,70,7))))
            throw new AssertionError("Rejected transactions cannot sound like success");
        byte[] activated=RecordedSfx.render(
            new LanSound.CueEvent(LanSound.Cue.DEVICE_ON,0,75,2),80);
        byte[] deactivated=RecordedSfx.render(
            new LanSound.CueEvent(LanSound.Cue.DEVICE_OFF,0,75,2),80);
        if(Arrays.equals(activated,deactivated))
            throw new AssertionError("Device off must use later NOX transition");
        byte[] a=new byte[2048],second=new byte[2048];
        for(int at=0;at<a.length;at+=4){
            a[at]=(byte)0x70;a[at+1]=(byte)0x17;
            a[at+2]=(byte)0x70;a[at+3]=(byte)0x17; // +6000 per channel
            second[at]=(byte)0x70;second[at+1]=(byte)0x17;
            second[at+2]=(byte)0x70;second[at+3]=(byte)0x17;
        }
        byte[] overlapped=LanAudio.mixForTest(a,second);
        int mixed=(short)((overlapped[0]&255)|(overlapped[1]<<8));
        if(mixed!=12000||overlapped.length!=2048)
            throw new AssertionError("Mixer did not sum concurrent audio streams: "+mixed);
        JButton button=new JButton("PLAY");
        GameSfx.button(button);
        GameSfx.button(button);
        if(!Boolean.TRUE.equals(button.getClientProperty("beckenburg.sfx.bound")))
            throw new AssertionError("UI click binding missing");
        JTabbedPane pages=new JTabbedPane();
        pages.addTab("TEST",new javax.swing.JPanel());
        GameSfx.tabs(pages);
        GameSfx.tabs(pages);
        if(!Boolean.TRUE.equals(pages.getClientProperty("beckenburg.sfx.bound")))
            throw new AssertionError("Tab click binding missing");
        int previous=LanAudio.effectsVolume();
        try {
            LanAudio.effectsVolume(0);
            byte[] muted=LanAudio.synthesize(new LanSound.CueEvent(LanSound.Cue.UI_CLICK,0,80,1));
            for(byte b:muted)if(b!=0)throw new AssertionError("FX volume 0 leaked PCM");
            LanAudio.effectsVolume(100);
            if(Arrays.equals(muted,LanAudio.synthesize(new LanSound.CueEvent(LanSound.Cue.UI_CLICK,0,80,1))))
                throw new AssertionError("FX volume 100 also silent");
        } finally {LanAudio.effectsVolume(previous);}
        System.out.println("PASS SoundCoverageTest / "+unique.size()+" event cues, captions, stable PCM, panning, FX volume and UI idempotence");
    }
}
