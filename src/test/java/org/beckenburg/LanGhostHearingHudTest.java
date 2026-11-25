package org.beckenburg;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

/** The spectral hearing caption works when audio is muted and ages out on a host tick. */
public final class LanGhostHearingHudTest {
    static void ok(boolean value,String label){if(!value)throw new AssertionError(label);}
    private static void checkSourceUpdates(LanGhostPanel panel,RoomLighting circuits){
        panel.onEvent("LIGHTS|111111111111|1");
        panel.onEvent("SOURCE|0");
        ok(circuits.breaker()&&!circuits.supplied()&&circuits.brightness(0,0)<.1,
            "Ghost view goes dark when the generator stops");
        panel.onEvent("LIGHTS|111111111111|0");
        panel.onEvent("SOURCE|1");
        ok(circuits.sourceRunning()&&!circuits.supplied(),
            "Generator restart keeps a tripped breaker open in the ghost view");
        panel.onEvent("LIGHTS|111111111111|1");
        ok(circuits.supplied()&&circuits.brightness(0,0)>.9,
            "Breaker reset restores lighting from the running generator");
    }
    private static void checkSignedSite(LanGhostPanel ghostView,LanClient hunter,SceneRenderer ghostCamera,
                                        java.lang.reflect.Field hunterCameraField){
        var map=new HouseMap(HouseMap.Site.GREYWATER);
        var spawn=map.spawn(9);
        ok(spawn.x()<0&&map.roomAt(spawn.x(),spawn.y(),spawn.z())==9,"Expected Greywater forest return");
        ok(map.airborneClear(spawn.x(),spawn.y()+.35,spawn.z(),HunterPosture.STANDING_HEIGHT),
            "Authored forest return has clearance above its negative-coordinate floor");
        ok(!map.airborneClear(-31,.35,spawn.z(),HunterPosture.STANDING_HEIGHT),
            "Opening signed coordinates does not remove the outer bound");
        String site="SITE|"+map.site()+"|"+map.level().revision();
        ghostView.onEvent(site);
        ghostView.onEvent(new LanGhostSpatial(map,9).frame(64));
        ok(Math.abs(ghostCamera.x-spawn.x())<.001,"Ghost view accepts negative authored X");
        var source=new GhostSoundRipples.Ripple(LanGhostPursuit.Sound.FOOTSTEP,
            spawn.x(),spawn.y(),spawn.z(),64);
        ok(GhostSoundRipples.Ripple.decode(source.frame(),map).x()<0,"Sound ripple retains signed X");
        var hunterView=new LanWorldPanel(hunter);
        try{
            var hunterCamera=(SceneRenderer)hunterCameraField.get(hunterView);
            hunterView.onEvent(site);
            hunterView.onEvent("BODY|0|"+Math.round(spawn.x()*1000)+"|"+Math.round(spawn.y()*1000)
                +"|"+Math.round(spawn.z()*1000)+"|0|64");
            ok(Math.abs(hunterCamera.x-spawn.x())<.001,"Hunter view accepts negative authored X");
            hunterView.onEvent("BODY|0|-999999|0|44000|0|65");
            ok(Math.abs(hunterCamera.x-spawn.x())<.001,"Hunter view rejects out-of-map positions");
        }catch(IllegalAccessException impossible){
            throw new AssertionError(impossible);
        }finally{hunterView.close();}
    }

    public static void main(String[] args)throws Exception{
        System.setProperty("beckenburg.audio.mute","true");
        try(LanHost server=new LanHost("127.0.0.1",0);
            LanClient ghost=new LanClient("127.0.0.1",server.port(),true,"Spirit",s->{});
            LanClient hunter=new LanClient("127.0.0.1",server.port(),false,"Hunter",s->{})){
            final LanGhostPanel[] view=new LanGhostPanel[1];
            SwingUtilities.invokeAndWait(()->{
                var panel=new LanGhostPanel(ghost);view[0]=panel;panel.setSize(850,555);
                panel.onEvent(new LanGhostSpatial(new HouseMap(),4).frame(17));
                // Until host-authenticated GLISTEN, even a valid cue is private.
                panel.onEvent("GHEAR|DOOR|LEFT|NEAR|17");
                ok(panel.currentHearing().isEmpty(),"unopened hearing gate rejects cues");
                panel.onEvent("GLISTEN|1|17");
                panel.onEvent("GHEAR|DOOR|LEFT|NEAR|17");
            });
            try{
                var field=LanGhostPanel.class.getDeclaredField("lights");
                field.setAccessible(true);
                var circuits=(RoomLighting)field.get(view[0]);
                var ghostCameraField=LanGhostPanel.class.getDeclaredField("camera");
                ghostCameraField.setAccessible(true);
                var ghostCamera=(SceneRenderer)ghostCameraField.get(view[0]);
                var hunterCameraField=LanWorldPanel.class.getDeclaredField("renderer");
                hunterCameraField.setAccessible(true);
                SwingUtilities.invokeAndWait(()->checkSourceUpdates(view[0],circuits));
                // Let the real Swing/host-clock hearing envelope advance; never
                // sleep on EDT, because that would prevent the 110ms timer.
                boolean visible=false;
                for(int n=0;n<15&&!visible;n++){
                    Thread.sleep(110);
                    final boolean[] heard={false};
                    SwingUtilities.invokeAndWait(()->{
                        heard[0]=view[0].currentHearing().contains("DOOR MOVEMENT")
                            &&view[0].currentHearing().contains("LEFT");
                    });
                    visible=heard[0];
                }
                ok(visible,"directional caption appears after authenticated stillness fade-in");
                SwingUtilities.invokeAndWait(()->{
                    LanGhostPanel panel=view[0];
                    BufferedImage image=new BufferedImage(850,555,BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();panel.paint(graphics);graphics.dispose();
                    panel.onEvent(new LanGhostSpatial(new HouseMap(),4).frame(40));
                    ok(panel.currentHearing().isEmpty(),"cue expires by host tick");
                    panel.onEvent("GHEAR|DOOR|LEFT|NEAR|17");
                    ok(panel.currentHearing().isEmpty(),"stale delayed cue is ignored");
                    panel.onEvent("GLISTEN|0|40");
                    panel.onEvent("GHEAR|RITUAL|LEFT|LOUD|40");
                    ok(panel.currentHearing().isEmpty(),"host-muted listener rejects new cues");
                    checkSignedSite(panel,hunter,ghostCamera,hunterCameraField);
                });
            }finally{
                SwingUtilities.invokeAndWait(()->view[0].close());
            }
        }
        System.out.println("PASS: mute-safe spectral sound overlay, EDT, stale-event expiry, ghost-view power and signed Greywater poses");
    }
}
