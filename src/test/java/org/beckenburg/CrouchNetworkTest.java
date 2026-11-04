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
                javax.swing.SwingUtilities.invokeAndWait(()->panel[0].onEvent(world.bodyFrame(0,10)));
                var renderer=(SceneRenderer)LooseClutterNetworkTest.field(panel[0],"renderer");
                double standingEye=renderer.eyeHeight;
                LanWorldControlsTest.press(panel[0],java.awt.event.KeyEvent.VK_CONTROL);
                String lowPacket=a.await("POSTURE|0|1|");
                // The lobby normally forwards authenticated server packets to this viewport.
                javax.swing.SwingUtilities.invokeAndWait(()->panel[0].onEvent(lowPacket));
                if(!world.crouched(0))throw new AssertionError("Ctrl did not dispatch real crouch intent");
                if(renderer.eyeHeight!=standingEye)throw new AssertionError("accepted posture must not snap camera");
                for(int i=0;i<30;i++){
                    double oldEye=renderer.eyeHeight;
                    javax.swing.SwingUtilities.invokeAndWait(()->panel[0].advanceCrouchCamera(1.0/30));
                    if(renderer.eyeHeight>oldEye||renderer.eyeHeight<feet+HunterPosture.CROUCH_EYE-.00001)
                        throw new AssertionError("camera must ease monotonically to crouch without overshoot");
                    var camera=(CrouchCamera)LooseClutterNetworkTest.field(panel[0],"crouchCamera");
                    if(Math.abs(renderer.eyeHeight-camera.eye()-feet)>.000001)
                        throw new AssertionError("easing must preserve accepted feet");
                }
                if(Math.abs(renderer.eyeHeight-feet-HunterPosture.CROUCH_EYE)>.0001)
                    throw new AssertionError("camera did not settle at crouched eye");
                if(((javax.swing.Timer)LooseClutterNetworkTest.field(panel[0],"timer")).getDelay()!=110)
                    throw new AssertionError("camera smoothing changed gameplay packet cadence");
                javax.swing.SwingUtilities.invokeAndWait(()->panel[0].keyReleased(new java.awt.event.KeyEvent(panel[0],java.awt.event.KeyEvent.KEY_RELEASED,System.currentTimeMillis(),0,java.awt.event.KeyEvent.VK_CONTROL,java.awt.event.KeyEvent.CHAR_UNDEFINED)));
                LanWorldControlsTest.press(panel[0],java.awt.event.KeyEvent.VK_CONTROL);a.await("POSTURE|0|0|");
            }finally{javax.swing.SwingUtilities.invokeAndWait(panel[0]::close);}
            ghost.client.crouch(true);ghost.await("REJECT|");
            System.out.println("PASS real TCP crouch/stand, continuous camera easing, fixed feet, unchanged network cadence and ghost-role rejection");
        }
    }
}
