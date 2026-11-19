package org.beckenburg;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Independent 50Hz network-input pump. The EDT publishes immutable input
 * snapshots on keyboard/mouse changes; costly Swing painting never drives
 * the host movement clock. No camera, world or Swing state is read off the EDT.
 */
final class LanRealtimeInput implements AutoCloseable {
    record Controls(boolean enabled,int forward,int strafe,double yaw,
                    double pitch,boolean sprint,long updatedNs){}
    private final AtomicReference<Controls> controls=new AtomicReference<>(
        new Controls(false,0,0,0,0,false,0));
    private final ScheduledExecutorService pump;
    private final LanClient client;
    private final boolean ghost;
    private final Consumer<IOException> failure;
    private volatile boolean closed;
    private int lastHeading=-1,lastPitch=Integer.MIN_VALUE;

    LanRealtimeInput(LanClient client,boolean ghost,Consumer<IOException> failure){
        this.client=Objects.requireNonNull(client);
        this.ghost=ghost;
        this.failure=Objects.requireNonNull(failure);
        pump=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,ghost?"beckenburg-ghost-motion":"beckenburg-hunter-motion");
            t.setDaemon(true);return t;
        });
        pump.scheduleAtFixedRate(this::sendLatest,0,20,TimeUnit.MILLISECONDS);
    }
    void publish(boolean enabled,int forward,int strafe,double yaw,double pitch,boolean sprint){
        controls.set(new Controls(enabled,forward,strafe,yaw,pitch,sprint,System.nanoTime()));
    }
    /** Ordered host commands, never UI or geometry operations. */
    private void sendLatest(){
        if(closed)return;
        Controls state=controls.get();
        if(!state.enabled()||System.nanoTime()-state.updatedNs()>500_000_000L){
            lastHeading=-1;lastPitch=Integer.MIN_VALUE;
            return;
        }
        try{
            int forward=state.forward(),strafe=state.strafe();
            if(forward!=0||strafe!=0){
                double x=Math.sin(state.yaw())*forward-Math.cos(state.yaw())*strafe;
                double z=Math.cos(state.yaw())*forward+Math.sin(state.yaw())*strafe;
                double magnitude=Math.hypot(x,z);
                if(magnitude>0){
                    int mx=(int)Math.round(100*x/magnitude);
                    int mz=(int)Math.round(100*z/magnitude);
                    if(mx!=0||mz!=0){
                        if(ghost)client.ghostMoveInput(mx,mz,state.sprint());
                        else client.moveInput(mx,mz,state.sprint());
                    }
                }
            }
            int heading=Math.floorMod((int)Math.round(Math.toDegrees(state.yaw())),360);
            int pitch=(int)Math.round(state.pitch()*1000);
            if(ghost){
                if(heading!=lastHeading){client.ghostTurn(heading);lastHeading=heading;}
            }else if(heading!=lastHeading||pitch!=lastPitch){
                client.turn(heading,pitch);
                lastHeading=heading;lastPitch=pitch;
            }
        }catch(IOException ex){
            close();
            SwingUtilities.invokeLater(()->failure.accept(ex));
        }
    }
    @Override public void close(){closed=true;pump.shutdownNow();}
}
