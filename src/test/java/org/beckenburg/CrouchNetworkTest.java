package org.beckenburg;

/** Real TCP posture round trip, shared snapshots and ghost-role rejection. */
public final class CrouchNetworkTest {
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");
            var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Demonologist");
            var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();
            a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            a.client.view3D();a.await("ACCEPT|VIEW3D");b.client.view3D();b.await("ACCEPT|VIEW3D");
            a.client.crouch(true);a.await("POSTURE|0|1|");b.await("POSTURE|0|1|");
            var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            if(!world.crouched(0))throw new AssertionError("host did not own posture change");
            double feet=world.body(0).y();
            a.client.crouch(false);a.await("POSTURE|0|0|");
            if(world.crouched(0)||world.body(0).y()!=feet)throw new AssertionError("posture changed feet or did not stand");
            final LanWorldPanel[] panel=new LanWorldPanel[1];
            javax.swing.SwingUtilities.invokeAndWait(()->panel[0]=new LanWorldPanel(a.client));
            try{
                LanWorldControlsTest.press(panel[0],java.awt.event.KeyEvent.VK_CONTROL);
                String lowPacket=a.await("POSTURE|0|1|");
                // The lobby normally forwards authenticated server packets to this viewport.
                javax.swing.SwingUtilities.invokeAndWait(()->panel[0].onEvent(lowPacket));
                if(!world.crouched(0))throw new AssertionError("Ctrl did not dispatch real crouch intent");
                javax.swing.SwingUtilities.invokeAndWait(()->panel[0].keyReleased(new java.awt.event.KeyEvent(panel[0],java.awt.event.KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_CONTROL,java.awt.event.KeyEvent.CHAR_UNDEFINED)));
                LanWorldControlsTest.press(panel[0],java.awt.event.KeyEvent.VK_CONTROL);a.await("POSTURE|0|0|");
            }finally{javax.swing.SwingUtilities.invokeAndWait(panel[0]::close);}
            ghost.client.crouch(true);ghost.await("REJECT|");
            System.out.println("PASS real TCP crouch/stand snapshots, unchanged authoritative feet and ghost-role rejection");
        }
    }
}
