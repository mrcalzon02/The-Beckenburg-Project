package org.beckenburg;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;

/** The spectral hearing caption works when audio is muted and ages out on a host tick. */
public final class LanGhostHearingHudTest {
    static void ok(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        System.setProperty("beckenburg.audio.mute","true");
        try(LanHost server=new LanHost("127.0.0.1",0);
            LanClient ghost=new LanClient("127.0.0.1",server.port(),true,"Spirit",s->{})){
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
                });
            }finally{
                SwingUtilities.invokeAndWait(()->view[0].close());
            }
        }
        System.out.println("PASS: mute-safe spectral sound overlay, EDT and stale-event expiry");
    }
}
