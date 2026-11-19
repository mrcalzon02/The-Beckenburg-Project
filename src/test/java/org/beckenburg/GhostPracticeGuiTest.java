package org.beckenburg;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Start practice at the walkable Castle's spectral station and return through the normal ESC action. */
public final class GhostPracticeGuiTest {
    private static <T> T showing(Container parent,Class<T> type){
        if(type.isInstance(parent)&&parent.isShowing())return type.cast(parent);
        for(var child:parent.getComponents())if(child instanceof Container nested){
            T found=showing(nested,type);if(found!=null)return found;
        }
        return null;
    }
    private static void faceStation(){
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var station=CastleHubMap.Station.SPECTRAL;var position=scene.station(station);
        for(double distance:new double[]{1.4,2,2.5})for(int i=0;i<64;i++){
            double yaw=i*Math.PI/32,x=position[0]-Math.sin(yaw)*distance,z=position[1]-Math.cos(yaw)*distance;
            if(map.aimed(x,z,yaw)==station && map.aimedDoor(x,0,z,yaw)==null &&
                CastleRelations.aimed(map,x,z,yaw)==null && CastleHubDecor.aimed(map,x,0,z,yaw)==null){
                scene.setSpawn(x,z,yaw);return;
            }
        }
        throw new AssertionError("No reachable spectral station interaction");
    }
    public static void main(String[] args){
        try{verify(args);}
        catch(Throwable failure){failure.printStackTrace();System.exit(1);}
        System.out.println("PASS Castle spectral station, local gameplay and ESC return");
        System.exit(0);
    }
    private static void verify(String[] args)throws Exception{
        Path home=Files.createTempDirectory("spectral-launch-");
        System.setProperty("user.home",home.toString());
        try{
            faceStation();InvestigationGame.main(new String[0]);
            var opened=new java.util.concurrent.atomic.AtomicReference<JFrame>();
            long end=System.nanoTime()+60_000_000_000L;
            while(opened.get()==null && System.nanoTime()<end){
                SwingUtilities.invokeAndWait(()->{
                    for(var window:Window.getWindows())if(window instanceof JFrame frame && showing(frame,CastleHubPanel.class)!=null)
                        opened.set(frame);
                });
                if(opened.get()==null)Thread.sleep(100);
            }
            var window=opened.get();if(window==null)throw new AssertionError("Castle launcher did not open");
            SwingUtilities.invokeAndWait(()->{
                var hub=showing(window,CastleHubPanel.class);
                hub.keyPressed(new KeyEvent(hub,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'E'));
                var panel=showing(window,GhostPracticePanel.class);
                if(panel==null||panel.session().observation().room()<0)throw new AssertionError("Spectral station did not launch gameplay");
            });
            if(args.length>0)ImageIO.write(new Robot().createScreenCapture(window.getBounds()),"png",Path.of(args[0]).toFile());
            SwingUtilities.invokeAndWait(()->{
                var panel=showing(window,GhostPracticePanel.class);
                panel.keyPressed(new KeyEvent(panel,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,KeyEvent.CHAR_UNDEFINED));
                if(panel.isShowing()||showing(window,CastleHubPanel.class)==null)throw new AssertionError("ESC did not return to Castle");
            });
        }finally{
            SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});
            // The game's shutdown hook appends its last diagnostic before JVM delete-on-exit cleanup.
            try(var paths=Files.walk(home)){paths.sorted().forEach(path->path.toFile().deleteOnExit());}
        }
    }
}
