package org.beckenburg;

import javax.swing.*;
import java.nio.file.Files;

/** Castle is the interactive home: pause remains an overlay, gate exit requires walking through it. */
public final class CastleHomeInteractionTest {
    private CastleHomeInteractionTest(){}
    private static void check(boolean valid,String description){
        if(!valid)throw new AssertionError(description);
    }
    public static void main(String[] args)throws Exception{
        CastleHubMap map=new CastleHubMap();
        check(CastleGateExit.COUNTDOWN_SECONDS>=3,"Gate exit must be cancellable");
        check(!CastleGateExit.inExitThreshold(map,15.5,0,35.2),"Closed gate cannot exit");
        check(map.toggleDoor(CastleGateExit.GATE_ID),"Open actual portcullis");
        check(CastleGateExit.inExitThreshold(map,15.5,0,35.2),"Walking beyond gate can exit");
        check(!CastleGateExit.inExitThreshold(map,15.5,0,33.4),"Courtyard is not exit trigger");
        check(!CastleGateExit.inExitThreshold(map,20.0,0,35.2),"Outside gate width is not exit trigger");
        check(!CastleGateExit.inExitThreshold(map,15.5,3.4,35.2),"Upper gallery cannot exit");
        check(!map.toggleDoor(CastleGateExit.GATE_ID),"Gate can be closed again");
        check(!CastleGateExit.inExitThreshold(map,15.5,0,35.2),"Closed gate cancels exit");

        Throwable[] failed={null};
        var profile=new ProfileStore(Files.createTempDirectory("castle-home").resolve("profile.properties"));
        SwingUtilities.invokeAndWait(()->{
            CastleHubPanel castle=null;
            try{
                castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                    public void begin(){}public void network(){}public void spectral(){}
                    public void menu(){}public void campaign(){}
                    public void cycleRole(){}public void cycleWeather(){}
                    public String role(){return "OBSERVER";}
                    public String weather(){return "Normal";}
                });
                castle.openPauseMenu();
                check(CampaignMenuWorkflowTest.named(castle,"castle.menu.pause",JPanel.class)!=null,
                    "Escape menu mounted as Castle child");
                JButton resume=CampaignMenuWorkflowTest.named(castle,"castle.pause.resume",JButton.class);
                JButton options=CampaignMenuWorkflowTest.named(castle,"castle.pause.options",JButton.class);
                JButton quit=CampaignMenuWorkflowTest.named(castle,"castle.pause.quit",JButton.class);
                check(resume!=null&&options!=null&&quit!=null,"Pause controls are real actions");
                resume.doClick();
                check(CampaignMenuWorkflowTest.named(castle,"castle.menu.pause",JPanel.class)==null,
                    "Resume closes overlay instead of leaving the world");
                JPanel page=PsxMenuScreen.screen("options","OPTIONS","Settings",new JLabel("Graphics"));
                castle.openMenuPanel("options",page,castle::closeService);
                check(SwingUtilities.isDescendingFrom(page,castle),
                    "Options are mounted over the Castle viewport");
                check(Boolean.TRUE.equals(page.getClientProperty("castle.overlay")),
                    "Options do not replace the world with a scenic title background");
                castle.closeService();
            }catch(Throwable exception){failed[0]=exception;}
            finally{if(castle!=null)castle.close();}
        });
        if(failed[0]!=null)throw new AssertionError(failed[0]);
        System.out.println("PASS Castle pause and options overlays, open-gate exit threshold and cancellation");
    }
}
