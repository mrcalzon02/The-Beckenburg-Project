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
    static String lobbyLog(LanLobbyPanel panel)throws Exception{
        var field=LanLobbyPanel.class.getDeclaredField("log");
        field.setAccessible(true);
        return ((JTextArea)field.get(panel)).getText();
    }
    static void untilLogged(java.util.concurrent.Callable<Boolean> condition,
                            String label,LanLobbyPanel panel)throws Exception{
        try{until(condition,label);}
        catch(AssertionError failure){
            // Preserve the actual GUI-received protocol transcript in Windows CI.
            // A timeout alone cannot distinguish a missing packet from a different
            // host rejection or a lost/late Swing dispatch.
            throw new AssertionError(label+"; lobby transcript:\n"+edt(()->lobbyLog(panel)),failure);
        }
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
            LanHost[] owner={null};LanClient[] hunter={null};
            edt(()->{int windows=0;for(Window w:Window.getWindows())if(w.isShowing())windows++;
                if(windows!=1)throw new AssertionError("Campaign escaped the game window");
                var hostField=LanLobbyPanel.class.getDeclaredField("hosted");hostField.setAccessible(true);
                owner[0]=(LanHost)hostField.get(lobby[0]);
                var clientField=LanLobbyPanel.class.getDeclaredField("connection");clientField.setAccessible(true);
                hunter[0]=(LanClient)clientField.get(lobby[0]);
                // Entering the 3D scene switches the hunter to physical controls.
                // Legacy room-mode EXTRACT must be rejected rather than silently paying out.
                return null;});
            untilLogged(()->lobbyLog(lobby[0]).contains("ACCEPT|VIEW3D"),
                "Host acknowledged physical mode through the campaign GUI",lobby[0]);
            edt(()->{hunter[0].extract();return null;});
            untilLogged(()->lobbyLog(lobby[0]).contains(
                    "REJECT|physical hunters must use the van door control"),
                "Physical campaign hunter cannot use legacy EXTRACT",lobby[0]);
            if(!owner[0].running()||!profile.archives().isEmpty())
                throw new AssertionError("Rejected extraction changed host campaign state");
            // Exercise the same host-validated cargo control used by real 3D gameplay.
            edt(()->{hunter[0].vanDoor();return null;});
            until(()->{var logField=LanLobbyPanel.class.getDeclaredField("log");logField.setAccessible(true);
                String messages=((JTextArea)logField.get(lobby[0])).getText();
                return messages.contains("ACCEPT|VANDOOR|CLOSED")&&messages.contains("RESULT|RETREATED");
            },"Physical van door closes and host sends retreat result");
            until(()->!owner[0].running()&&profile.archives().size()==1,"Host campaign result saved");
            // Reopen the actual profile file: an in-memory result is not durable acceptance.
            // The host writes atomically, but its in-memory archive may update first.
            until(()->{ProfileStore reloaded=new ProfileStore(dir.resolve("profile.properties"));
                reloaded.load();
                return reloaded.archives().size()==1
                    &&reloaded.archives().get(0).outcome()==InvestigationSession.Outcome.RETREATED;
            },"Host campaign retreat durably archived exactly once");
            edt(()->{hunter[0].vanDoor();return null;});
            until(()->{var logField=LanLobbyPanel.class.getDeclaredField("log");logField.setAccessible(true);
                return ((JTextArea)logField.get(lobby[0])).getText().contains(
                    "REJECT|hunter at cargo button required");
            },"Repeated van-door command must be rejected");
            ProfileStore afterRepeat=new ProfileStore(dir.resolve("profile.properties"));afterRepeat.load();
            if(afterRepeat.archives().size()!=1)
                throw new AssertionError("Repeated van-door command duplicated the campaign receipt");
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
