package org.beckenburg;

import javax.sound.sampled.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Tests a failing and then recovering playback device without requiring a sound card. */
public final class AudioDeviceRecoveryTest {
    private AudioDeviceRecoveryTest(){}

    private static void check(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }
    private static void await(BooleanSupplier condition,String failure)throws InterruptedException{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        while(!condition.getAsBoolean()&&System.nanoTime()<deadline)
            Thread.sleep(20);
        check(condition.getAsBoolean(),failure);
    }

    private static final class FakeOutput implements SourceDataLine {
        private static final AudioFormat FORMAT=new AudioFormat(16000f,16,2,true,false);
        private volatile boolean opened,running;
        private final AtomicInteger bytes=new AtomicInteger();
        @Override public void open(AudioFormat format,int size){
            check(format.getChannels()==2&&format.getSampleRate()==16000f,
                "Mixer requested wrong device format");
            opened=true;
        }
        @Override public void open(AudioFormat format){open(format,4096);}
        @Override public void open(){opened=true;}
        @Override public int write(byte[] sound,int offset,int length){
            if(!opened||!running)throw new IllegalStateException("Fake device not active");
            if(length<0||offset<0||offset+length>sound.length)
                throw new IllegalArgumentException("Bad device write");
            bytes.addAndGet(length);
            return length;
        }
        @Override public void start(){if(!opened)throw new IllegalStateException();running=true;}
        @Override public void stop(){running=false;}
        @Override public void drain(){}
        @Override public void flush(){}
        @Override public void close(){running=false;opened=false;}
        @Override public boolean isOpen(){return opened;}
        @Override public boolean isRunning(){return running;}
        @Override public boolean isActive(){return running;}
        @Override public AudioFormat getFormat(){return FORMAT;}
        @Override public int getBufferSize(){return 4096;}
        @Override public int available(){return 4096;}
        @Override public int getFramePosition(){return bytes.get()/4;}
        @Override public long getLongFramePosition(){return bytes.get()/4L;}
        @Override public long getMicrosecondPosition(){return bytes.get()*1000000L/(16000L*4);}
        @Override public float getLevel(){return 0;}
        @Override public Line.Info getLineInfo(){return new DataLine.Info(SourceDataLine.class,FORMAT);}
        @Override public Control[] getControls(){return new Control[0];}
        @Override public boolean isControlSupported(Control.Type type){return false;}
        @Override public Control getControl(Control.Type type){
            throw new IllegalArgumentException("No simulated mixer controls");
        }
        @Override public void addLineListener(LineListener listener){}
        @Override public void removeLineListener(LineListener listener){}
    }

    public static void main(String[] args)throws Exception{
        final AtomicInteger attempts=new AtomicInteger();
        final FakeOutput recovered=new FakeOutput();
        try(LanAudio audio=new LanAudio(()->{
            if(attempts.incrementAndGet()==1)
                throw new LineUnavailableException("intentional device failure");
            return recovered;
        })){
            audio.play(new LanSound.CueEvent(LanSound.Cue.DOOR,0,85,1));
            await(()->attempts.get()>=1,"First output opening never attempted");
            // Allow the worker to discard its old voice/queue and enter recovery.
            Thread.sleep(450);
            for(int i=0;i<25&&recovered.bytes.get()==0;i++){
                audio.play(new LanSound.CueEvent(LanSound.Cue.UI_CLICK,0,85,2+i));
                Thread.sleep(75);
            }
            await(()->recovered.bytes.get()>0,
                "Positional effects worker did not resume after device restored");
            check(attempts.get()>=2,"Device was never reopened");
        }
        check(!recovered.isOpen(),"Recovered audio line not closed on shutdown");
        System.out.println("PASS simulated output failure, recovered positional PCM and clean shutdown");
    }
}
