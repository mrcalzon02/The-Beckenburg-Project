package org.beckenburg;

/** Actual client intent and host clock replication to two independent TCP peers. */
public final class PlayerJumpNetworkTest {
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");
            var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Demonologist");
            var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();
            a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            a.client.view3D();a.await("ACCEPT|VIEW3D");b.client.view3D();b.await("ACCEPT|VIEW3D");
            var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            world.setRoom(0,4);double floor=world.body(0).y();
            final LanWorldPanel[] panel=new LanWorldPanel[1];
            javax.swing.SwingUtilities.invokeAndWait(()->panel[0]=new LanWorldPanel(a.client));
            try{
                LanWorldControlsTest.press(panel[0],GameControls.JUMP);
                a.await("ACCEPT|JUMP");b.await("POSTURE|0|1|");
            }finally{javax.swing.SwingUtilities.invokeAndWait(panel[0]::close);}
            boolean raised=false,landed=false;long end=System.nanoTime()+4_000_000_000L;
            while(System.nanoTime()<end){
                var p=LanProtocol.parts(b.await("BODY|0|"));double y=Integer.parseInt(p.get(3))/1000.0;
                raised|=y>floor+.7;
                if(raised&&Math.abs(y-floor)<.002){landed=true;break;}
            }
            if(!raised||!landed)throw new AssertionError("peer did not receive airborne and landed host transforms");
            b.await("POSTURE|0|0|");
            ghost.client.jump();ghost.await("REJECT|");
            System.out.println("PASS real TCP jump, remote tucked pose, autonomous gravity/landing and ghost rejection");
        }
    }
}
