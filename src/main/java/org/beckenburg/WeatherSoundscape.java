package org.beckenburg;

import javax.sound.sampled.*;
import java.util.*;
import java.io.*;

/** Low-volume procedural weather bed. Independent of ghost audio and soundtrack.
 * Every local client derives the same type/intensity from a host WEATHER frame.
 */
public final class WeatherSoundscape implements AutoCloseable {
    private static final int RATE=16000;
    private static final AudioFormat PCM=new AudioFormat(RATE,16,1,true,false);
    private volatile MissionWeather.State state;
    private volatile boolean outside,active,closed;
    private final Thread worker;
    private volatile SourceDataLine line;
    private volatile dev.mrcalzon.weather.WeatherModule module;
    public void setModule(dev.mrcalzon.weather.WeatherModule module){this.module=module;}
    // Short optimized loops derived from the original CC0 NOX captures in Beckenburg Drive.
    // They are bundled in-game; no Google Drive connection is used by players.
    private static final short[] RAIN=loadLoop("ambient_rain.wav");
    private static final short[] WIND=loadLoop("ambient_wind.wav");

    private static short[] loadLoop(String filename){
        try(java.io.InputStream resource=WeatherSoundscape.class.getResourceAsStream(
                "/org/beckenburg/sfx/"+filename)){
            if(resource==null)return null;
            try(AudioInputStream wave=AudioSystem.getAudioInputStream(
                    new java.io.BufferedInputStream(resource))){
                AudioFormat format=wave.getFormat();
                if(format.getChannels()!=1||format.getSampleRate()!=RATE
                        ||format.getSampleSizeInBits()!=16||format.isBigEndian()
                        ||format.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED)
                    throw new UnsupportedAudioFileException("Wrong NOX weather loop format: "+filename);
                byte[] bytes=wave.readNBytes(RATE*2*12+1);
                if(bytes.length<2*RATE||bytes.length>RATE*2*12||bytes.length%2!=0)
                    throw new java.io.IOException("Invalid NOX weather loop length: "+filename);
                short[] frames=new short[bytes.length/2];
                for(int i=0;i<frames.length;i++)
                    frames[i]=(short)((bytes[2*i]&255)|(bytes[2*i+1]<<8));
                return frames;
            }
        }catch(Exception invalid){
            System.err.println("Beckenburg weather recording fallback: "+invalid.getMessage());
            return null;
        }
    }
    static boolean hasRecordedLoop(EvidenceSignals.Weather weather){
        return switch(weather){
            case DRIZZLE,RAIN,HEAVY_RAIN,THUNDERSTORM,SLEET,HAIL->RAIN!=null;
            case OVERCAST,BLIZZARD,DUST_STORM,ASHFALL,WIND->WIND!=null;
            default->false;
        };
    }
    /** Real weather is mixed independently from ghosts and preserves storm thunder. */
    static short playbackSample(EvidenceSignals.Weather weather,int intensity,
            boolean outside,long index){
        short procedural=sample(weather,intensity,outside,index);
        short[] bed=switch(weather){
            case OVERCAST,BLIZZARD,DUST_STORM,ASHFALL,WIND->WIND;
            case DRIZZLE,RAIN,HEAVY_RAIN,THUNDERSTORM,SLEET,HAIL->RAIN;
            default->null;
        };
        if(bed==null)return procedural;
        double weatherGain=(outside?1:.23)*(.6+intensity*.35);
        int captured=bed[(int)(index%bed.length)];
        double synthGain=weather==EvidenceSignals.Weather.THUNDERSTORM?1:.18;
        int sound=(int)Math.round(captured*weatherGain+procedural*synthGain);
        return (short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,sound));
    }
    public WeatherSoundscape(){
        worker=new Thread(this::run,"beckenburg-weather-bed");
        worker.setDaemon(true);worker.start();
    }
    public void setWeather(MissionWeather.State weather){state=weather;}
    public void setOutside(boolean outdoor){outside=outdoor;}
    public void setActive(boolean showing){active=showing&&!closed;}
    /** Predictable, bounded samples for audio tests. Indoor shelter attenuates wind/rain.
     * Snow is deliberately quiet; thunderclaps are intermittent, never a ghost cue.
     */
    public static short sample(EvidenceSignals.Weather weather,int intensity,
            boolean outside,long sample){
        if(weather==null||intensity<1||intensity>3||sample<0)
            throw new IllegalArgumentException("invalid weather audio context");
        double t=sample/(double)RATE;
        int hash=(int)(sample*1103515245L+12345);
        hash^=hash>>>13;hash*=0x5bd1e995;hash^=hash>>>15;
        double noise=((hash&65535)/32767.5)-1;
        double gust=.5+.5*Math.sin(t*.31+Math.sin(t*.07));
        double low=Math.sin(t*2*Math.PI*39)*.16+Math.sin(t*2*Math.PI*73)*.10;
        double bed=switch(weather){
            case CLEAR ->.045*gust*noise;
            case OVERCAST ->.11*gust*noise+.15*low;
            case DRIZZLE ->.16*noise+.05*low;
            case RAIN ->.32*noise+.07*low;
            case HEAVY_RAIN ->.50*noise+.12*low;
            case FOG ->.045*noise+.10*gust*low;
            case DENSE_FOG ->.035*noise+.16*gust*low;
            case SNOW ->.09*gust*noise+.13*low;
            case BLIZZARD ->.34*gust*noise+.34*low;
            case SLEET ->.37*noise+.20*gust*low;
            case HAIL ->.42*noise+.18*Math.sin(t*2*Math.PI*117);
            case DUST_STORM ->.30*gust*noise+.28*low;
            case ASHFALL ->.14*gust*noise+.22*low;
            case MIASMA ->.07*noise+.16*Math.sin(t*2*Math.PI*31)+.07*Math.sin(t*2*Math.PI*53);
            case WIND ->.23*gust*noise+.34*low;
            case HEATWAVE ->.065*noise+.10*Math.sin(t*2*Math.PI*210)*Math.sin(t*7);
            case THUNDERSTORM ->{
                double cycle=t%17;
                double strike=cycle>11&&cycle<14?Math.exp(-(cycle-11)*1.6):0;
                yield .42*noise+.2*low+.45*strike*Math.sin(t*2*Math.PI*39);
            }
        };
        double gain=outside?1:.23;
        gain*=.055+intensity*.036;
        return (short)Math.round(Math.max(-1,Math.min(1,bed*gain))*32767);
    }
    private static final Map<String,short[]> ORIGINAL=new HashMap<>();
    /** The pinned module records 22.05kHz mono PCM; output mixes at 16kHz. */
    private static synchronized short[] original(String clip){
        short[] known=ORIGINAL.get(clip);
        if(known!=null)return known;
        byte[] wav=WeatherAssetLibrary.bytes(clip+".wav");
        try(AudioInputStream audio=AudioSystem.getAudioInputStream(
                new BufferedInputStream(new ByteArrayInputStream(wav)))){
            AudioFormat format=audio.getFormat();
            if(format.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED
                    ||format.getChannels()!=1||format.getSampleSizeInBits()!=16
                    ||format.isBigEndian()||format.getSampleRate()<8000
                    ||format.getSampleRate()>48000)
                throw new IOException("Unexpected shared weather WAV format: "+clip);
            byte[] raw=audio.readAllBytes();
            if(raw.length<1024||raw.length>750000||raw.length%2!=0)
                throw new IOException("Corrupt original weather WAV: "+clip);
            int count=raw.length/2;
            short[] source=new short[count];
            for(int i=0;i<count;i++)source[i]=(short)((raw[i*2]&255)|(raw[i*2+1]<<8));
            int length=Math.max(1,(int)Math.round(count*(double)RATE/format.getSampleRate()));
            short[] result=new short[length];
            for(int i=0;i<length;i++){
                double at=i*format.getSampleRate()/RATE;
                int first=Math.min(count-1,(int)at),second=Math.min(count-1,first+1);
                double blend=at-first;
                result[i]=(short)Math.round(source[first]*(1-blend)+source[second]*blend);
            }
            ORIGINAL.put(clip,result);
            return result;
        }catch(Exception error){
            throw new IllegalStateException("Shared PSX Weather original audio unavailable: "+clip,error);
        }
    }
    private record Thunder(short[] data,double gain,int cursor){
        Thunder advance(int next){return new Thunder(data,gain,next);}
    }
    /** Bundled WAV presence check also works in a headless packaged JAR. */
    static int originalSamples(String name){return original(name).length;}
    private static void release(SourceDataLine line){
        if(line==null)return;
        try{line.stop();}catch(RuntimeException ignored){}
        try{line.flush();}catch(RuntimeException ignored){}
        try{line.close();}catch(RuntimeException ignored){}
    }
    private void run(){
        long offset=0;
        ArrayList<Thunder> thunder=new ArrayList<>();
        boolean unavailable=false;
        try{
            while(!closed){
                try{
                MissionWeather.State w=state;
                var linked=module;
                if(!active||w==null||Boolean.getBoolean("beckenburg.audio.mute")
                        ||EnvironmentAudio.volume()==0){
                    if(linked!=null)linked.audio().drainCues();
                    thunder.clear();
                    if(line!=null){line.stop();line.flush();}
                    Thread.sleep(100);continue;
                }
                if(line==null){
                    line=AudioSystem.getSourceDataLine(PCM);
                    line.open(PCM,4096);line.start();
                }else if(!line.isRunning())line.start();
                List<dev.mrcalzon.weather.WeatherAudio.Loop> gains=linked==null
                    ?List.of():linked.audio().loops();
                if(linked!=null)for(var cue:linked.audio().drainCues()){
                    if(!cue.id().equals("thunder_near")&&!cue.id().equals("thunder_distant"))
                        throw new IllegalStateException("Unknown shared weather cue "+cue.id());
                    thunder.add(new Thunder(original(cue.id()),cue.volume(),0));
                }
                byte[] block=new byte[2048];
                int volume=EnvironmentAudio.volume();
                boolean exposed=outside;
                for(int i=0;i<block.length;i+=2){
                    double mixed;
                    if(linked==null) mixed=playbackSample(w.weather(),w.intensity(),exposed,offset);
                    else {
                        mixed=0;
                        for(var layer:gains){
                            if(!layer.id().startsWith("ambient_")||layer.gain()<=.001)continue;
                            short[] bed=original(layer.id());
                            mixed+=bed[(int)(offset%bed.length)]*layer.gain()
                                *(exposed?.57:.13);
                        }
                        for(int t=0;t<thunder.size();t++){
                            Thunder hit=thunder.get(t);
                            if(hit.cursor()<hit.data().length){
                                mixed+=hit.data()[hit.cursor()]*hit.gain()*(exposed?.90:.32);
                                thunder.set(t,hit.advance(hit.cursor()+1));
                            }
                        }
                        mixed=Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,mixed));
                    }
                    int value=(int)Math.round(Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE,mixed*volume/100.)));
                    block[i]=(byte)value;block[i+1]=(byte)(value>>8);
                    offset++;
                }
                thunder.removeIf(x->x.cursor()>=x.data().length);
                if(line.write(block,0,block.length)<=0)
                    throw new IllegalStateException("Weather output accepted no audio frames");
                if(unavailable){
                    System.err.println("Beckenburg weather audio output restored");
                    unavailable=false;
                }
                }catch(LineUnavailableException|IllegalArgumentException
                        |SecurityException failure){
                    if(!unavailable)
                        System.err.println("Beckenburg weather audio temporarily unavailable: "
                            +failure.getMessage());
                    unavailable=true;
                    thunder.clear();
                    var interruptedModule=module;
                    if(interruptedModule!=null)interruptedModule.audio().drainCues();
                    SourceDataLine stale=line;line=null;release(stale);
                    if(!closed)Thread.sleep(350);
                }
            }
        }catch(InterruptedException ex){Thread.currentThread().interrupt();}
        finally{SourceDataLine open=line;line=null;release(open);}
    }
    @Override public void close(){
        closed=true;active=false;worker.interrupt();
        SourceDataLine open=line;line=null;release(open);
    }
}
