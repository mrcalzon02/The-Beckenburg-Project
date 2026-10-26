package org.beckenburg;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Tests acoustic behaviour at authored boundaries rather than UI implementation details. */
public final class SoundVolumeTest {
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static double energy(byte[] pcm,int channel){double e=0;for(int i=channel*2;i<pcm.length;i+=4){short s=(short)((pcm[i]&255)|(pcm[i+1]<<8));e+=(double)s*s;}return e;}
    public static void main(String[] args)throws Exception{
        var box=SoundVolume.create("creek",0,2,0);check(box.attenuation(0,2,0,true)==.18,"Full ambient interior");check(Math.abs(box.attenuation(6,2,0,true)-.09)<1e-10,"Smooth half-falloff boundary");check(box.attenuation(8,2,0,true)==0&&box.attenuation(0,8,0,true)==0,"Horizontal and vertical exits silent");
        var sphere=new SoundVolume("motor",SoundVolume.Kind.ENVIRONMENTAL,SoundVolume.Shape.SPHERE,SoundVolume.Source.ELECTRIC_HUM,2,0,0,2,9,9,2,.5,1,.02,.2,true,false,true);check(sphere.height()==2&&sphere.depth()==2,"Actual sphere diameter");check(sphere.attenuation(2,0,0,false)==0,"Breaker-gated source silent");check(sphere.attenuation(2,2,0,true)>.2&&sphere.attenuation(2,3,0,true)==0,"Spherical attenuation");
        var d=new SoundDocument("house",List.of(sphere));var temp=Files.createTempDirectory("sound-volume-test");Path path=temp.resolve("house.sounds");d.save(path);check(SoundDocument.read(Files.newInputStream(path)).equals(d),"Atomic exact save/reload");
        try{SoundDocument.read(new ByteArrayInputStream(d.encode().replace("true|false|true","maybe|false|true").getBytes(java.nio.charset.StandardCharsets.UTF_8)));throw new AssertionError("Malformed flag accepted");}catch(IOException expected){}
        var listener=new SoundVolumeAudio.Listener(0,0,0,0,true);var mixer=new SoundVolumeAudio.Mixer(d);byte[] first=mixer.block(listener,1600,1);check(energy(first,0)>0&&energy(first,1)==0,"Directional source pans to rendered left");byte[] end=mixer.block(listener,8000,1);check(energy(Arrays.copyOfRange(end,end.length-1600,end.length),0)==0,"One-shot ends");mixer.block(new SoundVolumeAudio.Listener(20,0,0,0,true),1000,1);check(energy(mixer.block(listener,1600,1),0)>0,"One-shot retriggers only after exit/reentry");check(energy(new SoundVolumeAudio.Mixer(d).block(listener,1600,0),0)==0,"Master mute");
        var ambient=new SoundVolumeAudio.Mixer(new SoundDocument("house",List.of(box)));var stereo=ambient.block(new SoundVolumeAudio.Listener(0,2,0,0,true),4000,1);check(energy(stereo,0)==energy(stereo,1)&&energy(stereo,0)>0,"Ambient bed remains centred");
        String old=System.getProperty("beckenburg.sound.documents");try{System.setProperty("beckenburg.sound.documents",temp.toString());var map=new HouseMap(HouseMap.Site.HOUSE);String revision=map.level().revision();new SoundDocument("house",List.of(box)).save(path);var changed=new HouseMap(HouseMap.Site.HOUSE);check(!changed.level().revision().equals(revision),"Sound edits change host map revision");check(map.level().sounds().equals(d),"Existing map retains immutable document");}finally{if(old==null)System.clearProperty("beckenburg.sound.documents");else System.setProperty("beckenburg.sound.documents",old);}
        System.out.println("PASS visible sound volume model: 3D bounds/falloff, power, spatial stereo, ambient centring, fades, entry one-shots, mute, atomic save, malformed input and immutable map handshake revision");
    }
}
