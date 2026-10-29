package org.beckenburg;
import java.awt.*;
import java.nio.file.*;
import java.util.UUID;
import javax.swing.*;
/** Actual solo campaign lobby -> shared LAN renderer -> result -> same lobby/window. */
public final class CampaignEmbeddedDesktopTest {
    static <T>T edt(java.util.concurrent.Callable<T> call)throws Exception{
        var task=new java.util.concurrent.FutureTask<T>(call);SwingUtilities.invokeLater(task);return task.get(20,java.util.concurrent.TimeUnit.SECONDS);
    }
    static JButton button(Container root,String text){
        for(Component c:root.getComponents()){
            if(c instanceof JButton b&&text.equals(b.getText()))return b;
            if(c instanceof Container nested){var found=button(nested,text);if(found!=null)return found;}
        }return null;
    }
    static void until(java.util.concurrent.Callable<Boolean> condition,String label)throws Exception{
        long end=System.nanoTime()+15_000_000_000L;
        while(System.nanoTime()<end){if(edt(condition))return;Thread.sleep(50);}throw new AssertionError(label);
    }
    public static void main(String[] args)throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new AssertionError("Desktop required");
        Path dir=Files.createTempDirectory("embedded-campaign");var profile=new ProfileStore(dir.resolve("profile.properties"));
        profile.addNetworkPayout(UUID.randomUUID(),3000,0);profile.reserveMissionHunters(2);
        profile.purchaseCatalogGear("DOTS_PROJECTOR",EquipmentTiers.Tier.I);profile.equipCatalog(3,"DOTS_PROJECTOR");
        var chapter=profile.dispatchStory(0);JFrame[] frame={null};LanLobbyPanel[] lobby={null};boolean[] left={false};
        try{
            edt(()->{frame[0]=new JFrame("Campaign workflow acceptance");
                lobby[0]=new LanLobbyPanel(active->{frame[0].setContentPane(active);frame[0].revalidate();},()->left[0]=true,
                    new LanLobbyPanel.CampaignLaunch(profile,chapter,2,false));
                frame[0].setContentPane(lobby[0]);frame[0].setSize(1060,725);frame[0].setVisible(true);
                return null;});
            // Single-player dispatch must host, ready, launch, and show the actual
            // 3D scene without any more button presses.
            until(()->find(frame[0].getContentPane())!=null,
                "Solo campaign dispatch did not automatically open 3D");
            until(()->{
                var view=find(frame[0].getContentPane());if(view==null||!view.isShowing())return false;
                var tick=LanWorldPanel.class.getDeclaredField("tick");tick.setAccessible(true);return tick.getInt(view)>=3;
            },"Embedded campaign receives advancing world frames");
            edt(()->{int windows=0;for(Window w:Window.getWindows())if(w.isShowing())windows++;
                if(windows!=1)throw new AssertionError("Campaign escaped the game window");
                var field=LanLobbyPanel.class.getDeclaredField("connection");field.setAccessible(true);((LanClient)field.get(lobby[0])).extract();return null;});
            until(()->profile.archives().size()==1,"Host campaign result saved");
            edt(()->{button(frame[0].getContentPane(),"RETURN TO CASTLE LOBBY").doClick();return null;});
            until(()->lobby[0].isShowing(),"Return to same embedded lobby");
            edt(()->{button(lobby[0],"LEAVE LOBBY").doClick();return null;});
            if(!left[0]||profile.missionHires()!=0||profile.storyCheckpoint()!=0)throw new AssertionError("Incorrect team settlement / navigation");
            System.out.println("PASS real embedded solo campaign: two bots, saved catalog loadout, advancing 3D frames, host result, same-window return");
        }finally{edt(()->{if(lobby[0]!=null)lobby[0].close();if(frame[0]!=null)frame[0].dispose();return null;});}
        System.exit(0);
    }
    static LanWorldPanel find(Container root){for(Component c:root.getComponents()){
        if(c instanceof LanWorldPanel view)return view;
        if(c instanceof Container nested){var found=find(nested);if(found!=null)return found;}}return null;}
}
