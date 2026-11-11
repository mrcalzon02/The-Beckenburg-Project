package org.beckenburg;

import java.util.concurrent.*;

/** Four sockets: public architecture is shared; hunter tools, evidence, bodies and sound telemetry remain private. */
public final class LanGhostHearingIntegrationTest {
    static void ok(boolean b,String label){if(!b)throw new AssertionError(label);}
    static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);
        }
        String await(String expected)throws Exception{
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<deadline){
                String s=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(s==null)continue;
                if("GHOST".equals(client.seat())&&(s.startsWith("BODY|")
                    ||s.startsWith("TEAMMAP|")||s.startsWith("PROGRESS|")
                    ||s.startsWith("EVIDENCE|")
                    ||s.startsWith("APPARITION|")||s.startsWith("MOTION|")
                    ||s.startsWith("SOUND|")))
                    throw new AssertionError("hunter private state leaked to ghost: "+s);
                if(s.startsWith(expected))return s;
            }
            throw new AssertionError("missing "+expected+" for "+client.seat());
        }
        public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(LanHost server=new LanHost("127.0.0.1",0);
            Peer a=new Peer(server.port(),false,"Cleric");
            Peer b=new Peer(server.port(),false,"Observer");
            Peer c=new Peer(server.port(),false,"Demonologist");
            Peer g=new Peer(server.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();g.client.ready();
            a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            g.client.viewGhost3D();g.await("ACCEPT|GVIEW3D");g.await("GPOSE|");
            g.await("GLISTEN|1|");
            for(int r:AuthoredLevelNetworkTest.path(new HouseMap(),14,4)){a.client.move(r);a.await("ACCEPT|MOVE|"+r);}
            LanGhostHearing.Event step=LanGhostHearing.decode(g.await("GHEAR|FOOTSTEP|"));
            ok(step.source()==LanGhostPursuit.Sound.FOOTSTEP,"validated movement made sound");
            ok(step.frame().split("\\|").length==5,"GHEAR remains a coarse five-field cue");
            var sourceRipple=GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|FOOTSTEP|"),new HouseMap());
            ok(sourceRipple.kind()==LanGhostPursuit.Sound.FOOTSTEP,
                "Stationary ghost receives a transient positional footstep ripple");
            ok(sourceRipple.frame().split("\\|",-1).length==6,
                "Ripple carries event location, not a hunter seat, health or team telemetry");
            a.client.scan(LanCase.Tool.EMF);a.await("ACCEPT|SCAN");
            ok(LanGhostHearing.decode(g.await("GHEAR|EQUIPMENT|")).source()
                ==LanGhostPursuit.Sound.EQUIPMENT,"legal scanner sent equipment cue");
            ok(GhostSoundRipples.Ripple.decode(g.await("GRIPPLE|EQUIPMENT|"),new HouseMap())
                .kind()==LanGhostPursuit.Sound.EQUIPMENT,
                "The real hunter tool emits a source-anchored sound ring");
            g.client.ghostStep(100,0);
            g.await("ACCEPT|GSTEP");g.await("GLISTEN|0|");
            // A ghost cannot forge its own audible source or request
            // another team's live BODY/telemetry frames.
            g.client.send("GRIPPLE|FOOTSTEP|1000|0|1000|20");
            g.await("REJECT|unknown command");
            g.client.send("GHEAR|RITUAL|LEFT|LOUD|20");
            g.await("REJECT|unknown command");
            ok(server.running(),"forged cue cannot change mission");
            System.out.println("PASS: four-socket ghost hearing and real source ripples, move-to-mute host control, bounded sound packets, no hunter BODY leakage or forged ripple authority");
        }
    }
}
