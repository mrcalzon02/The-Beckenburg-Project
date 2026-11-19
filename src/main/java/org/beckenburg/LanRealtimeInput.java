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
    // Pump-thread only. A neutral packet ends each active movement epoch.
    private boolean movementSent;
    private static final long STALE_INPUT_NS=500_000_000L;

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
    /** Pump-thread-only movement sender; client serializes wire ordering. */
    private void sendMovement(int dx,int dz,boolean sprint)throws IOException {
        if(ghost)client.ghostMoveInput(dx,dz,sprint);
        else client.moveInput(dx,dz,sprint);
    }
    /** Ordered host commands, never UI or geometry operations. */
    private void sendLatest(){
        if(closed)return;
        Controls state=controls.get();
        boolean active=state.enabled()&&System.nanoTime()-state.updatedNs()<=STALE_INPUT_NS;
        try{
            if(!active){
                // A paused, hidden or unresponsive EDT must never leave an
                // unbounded held movement intention on the authoritative host.
                if(movementSent){sendMovement(0,0,false);movementSent=false;}
                lastHeading=-1;lastPitch=Integer.MIN_VALUE;
                return;
            }
            int forward=state.forward(),strafe=state.strafe();
            if(forward!=0||strafe!=0){
                double x=Math.sin(state.yaw())*forward-Math.cos(state.yaw())*strafe;
                double z=Math.cos(state.yaw())*forward+Math.sin(state.yaw())*strafe;
                double magnitude=Math.hypot(x,z);
                if(magnitude>0){
                    int mx=(int)Math.round(100*x/magnitude);
                    int mz=(int)Math.round(100*z/magnitude);
                    if(mx!=0||mz!=0){
                        sendMovement(mx,mz,state.sprint());
                        movementSent=true;
                    }else if(movementSent){
                        sendMovement(0,0,false);movementSent=false;
                    }
                }
            }else if(movementSent){
                // Keep STEP/GSTEP legacy callers unchanged. Only HINPUT/GINPUT
                // zero vectors are neutral state transitions, not travel.
                sendMovement(0,0,false);movementSent=false;
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
