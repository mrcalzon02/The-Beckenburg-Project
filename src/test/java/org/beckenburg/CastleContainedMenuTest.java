package org.beckenburg;
import javax.swing.*;
import java.nio.file.*;
/** Actual Castle world owns every preparation panel; no display or Window is created. */
public final class CastleContainedMenuTest {
    public static void main(String[] args)throws Exception{
        var profile=new ProfileStore(Files.createTempDirectory("castle-menus").resolve("profile.properties"));
        Throwable[] fail={null};
        SwingUtilities.invokeAndWait(()->{CastleHubPanel castle=null;LanLobbyPanel network=null;
            try{
                castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                    public void begin(){}public void network(){}public void spectral(){}public void menu(){}public void campaign(){}
                    public void cycleRole(){}public void cycleWeather(){}public String role(){return "CLERIC";}public String weather(){return "Normal";}
                });
                var campaign=new CampaignBoardPanel(profile,new CampaignBoardPanel.Actions(){public void deploy(SiteCatalog.Location map,boolean ally){}public void castle(){}public void tutorial(){}});
                castle.openService("CAMPAIGN DESK",campaign,castle::closeService);
                if(!SwingUtilities.isDescendingFrom(campaign,castle))throw new AssertionError("Campaign is outside Castle");
                CampaignMenuWorkflowTest.capture(castle,"castle-campaign");
                network=new LanLobbyPanel();castle.openService("MULTIPLAYER DESK",network,castle::closeService);
                if(!SwingUtilities.isDescendingFrom(network,castle))throw new AssertionError("Multiplayer is outside Castle");
                if(java.awt.Window.class.isAssignableFrom(LanLobbyPanel.class)||java.awt.Window.class.isAssignableFrom(LanLobbyWindow.class))
                    throw new AssertionError("Lobby can be a separate Window");
                for(Class<?> type:BeckenburgUpdatesUI.class.getDeclaredClasses())
                    if(java.awt.Window.class.isAssignableFrom(type))throw new AssertionError("Updates can allocate a separate Window");
                CampaignMenuWorkflowTest.capture(castle,"castle-multiplayer");
                castle.closeService();
                if(SwingUtilities.isDescendingFrom(network,castle))throw new AssertionError("Closed service not removed");
            }catch(Throwable error){fail[0]=error;}finally{if(network!=null)network.close();if(castle!=null)castle.close();}
        });
        if(fail[0]!=null)throw new AssertionError(fail[0]);
        System.out.println("PASS campaign and multiplayer are children of the actual Castle viewport; no lobby Window path exists");
    }
}
