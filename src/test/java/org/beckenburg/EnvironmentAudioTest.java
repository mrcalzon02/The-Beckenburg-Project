package org.beckenburg;

import java.util.Arrays;

/** Headless coverage for independent ambience volume and real castle sound resources. */
public final class EnvironmentAudioTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        int oldEnvironment=EnvironmentAudio.volume();
        int oldEffects=LanAudio.effectsVolume(),oldMusic=SoundtrackAudio.volume();
        try{
            EnvironmentAudio.volume(0);
            check(EnvironmentAudio.volume()==0,"Environment mute");
            check(LanAudio.effectsVolume()==oldEffects&&SoundtrackAudio.volume()==oldMusic,
                "Ambience settings changed music or critical effects");
            EnvironmentAudio.volume(100);
            for(int illegal:new int[]{-1,101}){
                boolean rejected=false;
                try{EnvironmentAudio.volume(illegal);}
                catch(IllegalArgumentException expected){rejected=true;}
                check(rejected&&EnvironmentAudio.volume()==100,"Out-of-range volume accepted");
            }
            try(var input=SoundDocument.class.getResourceAsStream("/org/beckenburg/sounds/castle.sounds")){
                check(input!=null,"Castle ambience layout missing from packaged classpath");
                var doc=SoundDocument.read(input);
                check(doc.level().equals("castle")&&doc.volumes().size()==10,
                    "Castle should load ten physically placed sound beds");
                for(var volume:doc.volumes()){
                    check(volume.enabled()&&volume.loop(),"Inactive ambience volume "+volume.id());
                    check(volume.attenuation(volume.x(),volume.y(),volume.z(),true)>0,
                        "Emitter silent at its actual coordinate: "+volume.id());
                    check(volume.attenuation(volume.x()+100,volume.y(),volume.z(),true)==0,
                        "Emitter leaks across the map: "+volume.id());
                }
                var ops=doc.volumes().stream().filter(v->v.id().equals("ops-electrics"))
                    .findFirst().orElseThrow();
                check(ops.requiresPower()&&ops.attenuation(ops.x(),ops.y(),ops.z(),false)==0,
                    "Operations monitor hum ignores its power state");
                var wind=doc.volumes().stream().filter(v->v.id().equals("court-wind"))
                    .findFirst().orElseThrow();
                check(CastleRooms.at(wind.x(),0,wind.z()).outside(),
                    "Courtyard wind placed inside a castle wall");
                var mix=new SoundVolumeAudio.Mixer(doc);
                var listener=new SoundVolumeAudio.Listener(wind.x(),wind.y(),wind.z(),0,true);
                byte[] muted=mix.block(listener,1024,0);
                for(byte sample:muted)check(sample==0,"Muted bed leaked PCM");
                byte[] sound=mix.block(listener,1024,1);
                check(!Arrays.equals(muted,sound),"Environment bed rendered silence");
                for(var source:new SoundVolume.Source[]{SoundVolume.Source.WOOD_CREAK,
                        SoundVolume.Source.STONE_SETTLE}){
                    var volume=doc.volumes().stream().filter(v->v.source()==source)
                        .findFirst().orElseThrow();
                    boolean heard=false;
                    for(int frame=0;frame<16000*32;frame+=73)heard|=volume.sample(frame)!=0;
                    check(heard,"Original architectural texture silent: "+source);
                }
            }
        }finally{
            EnvironmentAudio.volume(oldEnvironment);
        }
        check(LanAudio.effectsVolume()==oldEffects&&SoundtrackAudio.volume()==oldMusic,
            "Restoring environment setting changed music/effects");
        System.out.println("PASS independent environment gain, ten castle emitters and original spatial ambience");
    }
}
