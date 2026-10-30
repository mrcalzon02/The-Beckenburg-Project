package org.beckenburg;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Real TCP test fixture for powers whose authorization depends on *heard* hunter
 * activity. No game or GhostDomain rules are weakened for an integration test.
 * The host observation is read under its simulation mutex only to coordinate
 * the real clients; the command still traverses the authenticated wire path.
 */
public final class LanTestGhostActions {
    @FunctionalInterface public interface Ack { void await(String prefix) throws Exception; }
    private LanTestGhostActions(){}

    public static void manifest(LanHost server,LanClient hunter,Ack hunterAck,
            LanClient spirit,Ack spiritAck)throws Exception {
        Objects.requireNonNull(server);
        Objects.requireNonNull(hunter);
        Objects.requireNonNull(hunterAck);
        Objects.requireNonNull(spirit);
        Objects.requireNonNull(spiritAck);
        Object mutex=LooseClutterNetworkTest.field(server,"mutex");
        GhostDomain domain=(GhostDomain)LooseClutterNetworkTest.field(server,"ghost");
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);
        int lastScan=-1000;
        GhostDomain.Observation observed=null;
        while(System.nanoTime()<deadline){
            synchronized(mutex){observed=domain.observe();}
            if(observed.legalActions().contains(GhostDomain.Action.MANIFEST)){
                spirit.ghost(GhostDomain.Action.MANIFEST,observed.room());
                spiritAck.await("ACCEPT|GHOST|MANIFEST");
                return;
            }
            // Wait for the actual >=80 tick gate. No fake ghost hearing, force
            // reveal, reset of cooldowns, or camera/role privilege is permitted.
            if(observed.tick()>=79 && observed.tick()-lastScan>=4){
                hunter.scan(LanCase.Tool.EMF);
                hunterAck.await("ACCEPT|SCAN|");
                lastScan=observed.tick();
            }
            Thread.sleep(35);
        }
        throw new AssertionError("No legal audible MANIFEST window within 30s: "+observed);
    }
}
