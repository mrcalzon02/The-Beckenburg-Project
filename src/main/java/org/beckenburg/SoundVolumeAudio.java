package org.beckenburg;
import javax.sound.sampled.*;
import java.util.*;

/** Local cosmetic ambience only; respects independent environment volume and global mute. */
public final class SoundVolumeAudio implements AutoCloseable {
    public record Listener(double x,double y,double z,double yaw,boolean power){}
    /** Pure mixer used by both the device worker and device-free verification. */
    public static final class Mixer {
        private final SoundDocument document;
        private final double[] fades;private final boolean[] inside;private final long[] starts;
        private long frame;
        public Mixer(SoundDocument d){document=d;fades=new double[d.volumes().size()];inside=new boolean[fades.length];starts=new long[fades.length];Arrays.fill(starts,Long.MIN_VALUE);}
        public byte[] block(Listener listener,int count,double master){
            if(count<0||count>16000||!Double.isFinite(master)||master<0||master>1)throw new IllegalArgumentException("Sound mixer block");
            byte[] pcm=new byte[count*4];double[] target=new double[fades.length],pan=new double[fades.length];
            for(int j=0;j<target.length;j++){var v=document.volumes().get(j);target[j]=v.attenuation(listener.x,listener.y,listener.z,listener.power);pan[j]=v.pan(listener.x,listener.z,listener.yaw);boolean enter=target[j]>0;if(enter&&!inside[j])starts[j]=frame;inside[j]=enter;}
            for(int i=0;i<count;i++,frame++){double left=0,right=0;for(int j=0;j<target.length;j++){
                var v=document.volumes().get(j);long elapsed=starts[j]==Long.MIN_VALUE?Long.MAX_VALUE:frame-starts[j];double gain=target[j];if(!v.loop())gain*=Math.max(0,Math.min(1,(v.duration()-elapsed/16000d)/v.fade()));
                double step=1/(v.fade()*16000);fades[j]+=Math.max(-step,Math.min(step,gain-fades[j]));
                if(fades[j]<1e-8)continue;long source=(long)((v.loop()?frame:elapsed)*v.pitch());double sample=v.sample(source)*fades[j]*master;left+=sample*Math.sqrt((1-pan[j])/2);right+=sample*Math.sqrt((1+pan[j])/2);
            }int l=(int)Math.round(Math.max(-27852,Math.min(27852,left))),r=(int)Math.round(Math.max(-27852,Math.min(27852,right))),p=i*4;pcm[p]=(byte)l;pcm[p+1]=(byte)(l>>8);pcm[p+2]=(byte)r;pcm[p+3]=(byte)(r>>8);}
            return pcm;
        }
    }
    private volatile SoundDocument document;private volatile Listener listener=new Listener(0,0,0,0,true);private volatile boolean active,closed;private volatile SourceDataLine line;private final Thread worker;
    public SoundVolumeAudio(SoundDocument document){this.document=document;worker=new Thread(this::run,"beckenburg-authored-sound-volumes");worker.setDaemon(true);worker.start();}
    public void document(SoundDocument d){document=Objects.requireNonNull(d);}
    public void listener(double x,double y,double z,double yaw,boolean power){listener=new Listener(x,y,z,yaw,power);}
    public void active(boolean value){active=value&&!closed;}
    private static final AudioFormat OUTPUT=new AudioFormat(16000,16,2,true,false);
    private static void release(SourceDataLine open){
        if(open==null)return;
        try{open.stop();}catch(RuntimeException ignored){}
        try{open.flush();}catch(RuntimeException ignored){}
        try{open.close();}catch(RuntimeException ignored){}
    }
    private void run(){
        SoundDocument loaded=null;
        Mixer mixer=null;
        boolean unavailable=false;
        try{
            while(!closed){
                try{
                    SoundDocument d=document;
                    if(!active||d.volumes().isEmpty()
                            ||Boolean.getBoolean("beckenburg.audio.mute")
                            ||EnvironmentAudio.volume()==0){
                        if(line!=null){line.stop();line.flush();}
                        Thread.sleep(80);
                        continue;
                    }
                    if(d!=loaded){mixer=new Mixer(d);loaded=d;}
                    if(line==null){
                        SourceDataLine fresh=AudioSystem.getSourceDataLine(OUTPUT);
                        line=fresh;
                        fresh.open(OUTPUT,4096);
                    }
                    if(!line.isRunning())line.start();
                    if(unavailable){
                        System.err.println("Beckenburg environment audio output restored");
                        unavailable=false;
                    }
                    byte[] block=mixer.block(listener,512,EnvironmentAudio.volume()/100d);
                    line.write(block,0,block.length);
                }catch(LineUnavailableException|IllegalArgumentException
                        |IllegalStateException|SecurityException problem){
                    if(!unavailable)
                        System.err.println("Beckenburg environment audio temporarily unavailable: "
                            +problem.getMessage());
                    unavailable=true;
                    SourceDataLine stale=line;
                    line=null;
                    release(stale);
                    if(!closed)Thread.sleep(350);
                }
            }
        }catch(InterruptedException done){Thread.currentThread().interrupt();}
        finally{SourceDataLine open=line;line=null;release(open);}
    }
    public void close(){
        closed=true;
        active=false;
        worker.interrupt();
        SourceDataLine open=line;
        line=null;
        release(open);
    }
}
