package org.beckenburg;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

/**
 * Loads the actual packaged Swing launcher, waits for the real menu and
 * opens the multiplayer UI. A splash-only process never counts as loaded.
 * Run on a desktop runner, not in headless synthetic graphics mode.
 */
public final class BeckenburgDesktopE2ETest {
    private static final Path OUTPUT=Path.of("build","e2e");
    private BeckenburgDesktopE2ETest(){}
    private static JButton button(Container container,String label){
        for(Component child:container.getComponents()){
            if(child instanceof JButton found&&label.equals(found.getText()))return found;
            if(child instanceof Container nested){
                JButton found=button(nested,label);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static JFrame launcher(){
        for(Window window:Window.getWindows())if(window instanceof JFrame frame
                &&frame.isShowing()&&frame.getTitle().startsWith("The Beckenburg Project"))
            return frame;
        return null;
    }
    private static JFrame multiplayer(){
        JFrame frame=launcher();
        var lobby=frame==null?null:CampaignMenuWorkflowTest.named(frame.getContentPane(),"network",LanLobbyPanel.class);
        return lobby!=null&&lobby.isShowing()?frame:null;
    }
    private static void capture(String label){
        try{
            Files.createDirectories(OUTPUT);
            if(!GraphicsEnvironment.isHeadless()){
                Rectangle bounds=new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screenshot=new Robot().createScreenCapture(bounds);
                ImageIO.write(screenshot,"png",OUTPUT.resolve(label+".png").toFile());
            }
        }catch(Exception failed){System.err.println("Screenshot unavailable: "+failed);}
    }
    /** A bounded EDT probe. Unlike invokeAndWait, a frozen render/UI cannot
     * make the acceptance driver itself hang forever without diagnostic failure.
     * Keep at most one pending EDT callback during the wait.
     */
    private static <T> T waitForResponsive(java.util.concurrent.Callable<T> check,long seconds)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        FutureTask<T> pending=null;
        while(System.nanoTime()<deadline){
            if(pending==null){
                pending=new FutureTask<>(check);
                SwingUtilities.invokeLater(pending);
            }
            try {
                T value=pending.get(250,TimeUnit.MILLISECONDS);
                pending=null;
                if(value!=null)return value;
            }catch(TimeoutException stillRendering){ /* preserve the single queued probe */ }
            Thread.sleep(80);
        }
        return null;
    }
    private static CastleHubPanel visibleCastle(Container surface){
        for(Component component:surface.getComponents()){
            if(component instanceof CastleHubPanel castle&&castle.isShowing())return castle;
            if(component instanceof Container nested){
                CastleHubPanel candidate=visibleCastle(nested);
                if(candidate!=null)return candidate;
            }
        }
        return null;
    }
    private static boolean visibleUnifiedStore(Container surface){
        for(Component component:surface.getComponents()){
            if(component instanceof javax.swing.JList<?> items
                    &&"equipment.catalog.items".equals(items.getName())&&items.isShowing())
                return true;
            if(component instanceof Container nested&&visibleUnifiedStore(nested))
                return true;
        }
        return false;
    }
    private static Component visibleSoloCase(Container surface){
        for(Component c:surface.getComponents()){
            if(c.getClass().getSimpleName().equals("GamePanel") && c.isShowing())return c;
            if(c instanceof Container nested){
                Component found=visibleSoloCase(nested);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static boolean soloOpeningActive(Component game)throws Exception{
        Field field=game.getClass().getDeclaredField("opening");
        field.setAccessible(true);
        return field.get(game)!=null;
    }
    private static int soloFrameCount(Component game)throws Exception{
        Field field=game.getClass().getDeclaredField("frameCount");
        field.setAccessible(true);
        return field.getInt(game);
    }
    private static boolean soloInitialFrameReady(Component game)throws Exception{
        Field field=game.getClass().getDeclaredField("initialFrameReady");
        field.setAccessible(true);
        return field.getBoolean(game);
    }
    private static JComboBox<?> sitePicker(Container container){
        for(Component c:container.getComponents()){
            if(c instanceof JComboBox<?> box && box.getItemCount()>0
                    &&box.getItemAt(0) instanceof SiteCatalog.Location)return box;
            if(c instanceof Container nested){
                JComboBox<?> found=sitePicker(nested);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static JDialog dispatchDialog(){
        for(Window window:Window.getWindows())if(window instanceof JDialog dialog
                &&dialog.isShowing()&&dialog.getTitle().startsWith("EXPEDITION DISPATCH"))
            return dialog;
        return null;
    }
    private static <T> T waitFor(java.util.concurrent.Callable<T> check,long seconds)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(System.nanoTime()<deadline){
            // The game is a real Swing event loop. Inspect components on the EDT.
            final Object[] slot=new Object[1];
            SwingUtilities.invokeAndWait(()->{
                try{slot[0]=check.call();}catch(Exception ex){throw new RuntimeException(ex);}
            });
            if(slot[0]!=null){
                @SuppressWarnings("unchecked") T value=(T)slot[0];
                return value;
            }
            Thread.sleep(80);
        }
        return null;
    }
    /** Uses the actual physical station handler, audited independently for look-to-use. */
    private static void useStation(CastleHubPanel castle,CastleHubMap.Station station){
        try{
            var method=CastleHubPanel.class.getDeclaredMethod("use",CastleHubMap.Station.class);
            method.setAccessible(true);method.invoke(castle,station);
        }catch(ReflectiveOperationException failed){throw new AssertionError("Station "+station+" failed",failed);}
    }
    public static void main(String[] args)throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new AssertionError("Actual desktop required");
        Files.createDirectories(OUTPUT);
        try{
            InvestigationGame.main(new String[0]);
            JFrame frame=waitForResponsive(()->{
                JFrame game=launcher();
                return game!=null&&visibleCastle(game.getContentPane())!=null?game:null;
            },60);
            if(frame==null)throw new AssertionError(
                "Startup never displayed the real Castle home; inspect diagnostics");
            CastleHubPanel castle=waitForResponsive(()->visibleCastle(frame.getContentPane()),10);
            if(castle==null)throw new AssertionError("Castle home not visible");
            JButton hiddenEntry=button(frame.getContentPane(),"ENTER CASTLE");
            if(hiddenEntry!=null&&hiddenEntry.isShowing())
                throw new AssertionError("A redundant Enter Castle menu replaced the Castle home");
            capture("01-castle-home");
            SwingUtilities.invokeAndWait(castle::openPauseMenu);
            JButton pauseOptions=CampaignMenuWorkflowTest.named(castle,"castle.pause.options",JButton.class);
            if(pauseOptions==null||!pauseOptions.isShowing())
                throw new AssertionError("Escape overlay does not contain a working Options action");
            SwingUtilities.invokeAndWait(pauseOptions::doClick);
            var options=waitForResponsive(()->{
                var found=CampaignMenuWorkflowTest.named(castle,"options",javax.swing.JPanel.class);
                return found!=null&&found.isShowing()?found:null;
            },10);
            if(options==null||!SwingUtilities.isDescendingFrom(options,castle))
                throw new AssertionError("Options escaped Castle or replaced its world");
            capture("00-options-in-castle");
            SwingUtilities.invokeAndWait(()->button(options,"BACK").doClick());
            if(!castle.isShowing())throw new AssertionError("Back from Options did not resume Castle");
            SwingUtilities.invokeLater(castle::openQuartermaster);
            Boolean embedded=waitForResponsive(
                ()->visibleUnifiedStore(castle)?Boolean.TRUE:null,15);
            if(embedded==null)throw new AssertionError(
                "Quartermaster not mounted in the real Castle viewport");
            for(Window popup:Window.getWindows())if(popup instanceof JDialog&&popup.isShowing())
                throw new AssertionError("Castle inventory created a top-level dialog");
            capture("02-castle-store-all-equipment");
            SwingUtilities.invokeAndWait(()->button(castle,"RETURN TO CASTLE  [ESC]").doClick());
            SwingUtilities.invokeAndWait(()->useStation(castle,CastleHubMap.Station.MULTIPLAYER));
            var networkPanel=waitForResponsive(()->{
                var found=CampaignMenuWorkflowTest.named(castle,"network",LanLobbyPanel.class);
                return found!=null&&found.isShowing()?found:null;
            },20);
            if(networkPanel==null||SwingUtilities.getAncestorOfClass(CastleHubPanel.class,networkPanel)==null)
                throw new AssertionError("Multiplayer lobby did not stay in Castle workstation");
            if(button(networkPanel,"Host LAN")==null||button(networkPanel,"Join by IP")==null
                    ||button(networkPanel,"READY")==null||button(networkPanel,"START ROUND")==null)
                throw new AssertionError("Multiplayer workstation is not actionable");
            capture("03-castle-multiplayer-workstation");
            System.out.println("E2E PASS Castle-first startup, pause/options overlay, Quartermaster and multiplayer workstation");
            SwingUtilities.invokeAndWait(()->button(networkPanel,"LEAVE LOBBY").doClick());
            SwingUtilities.invokeAndWait(()->useStation(castle,CastleHubMap.Station.EXPEDITION));
            // The previous smoke stopped at the menu and could never detect
            // BECK-LIVE-012 (case start locked the user's process). Open the
            // actual dispatch dialog, explicitly choose HOUSE, and demand an
            // advancing, first-frame-complete case on the live Swing UI.
            Container dispatch=waitForResponsive(()->{
                var panel=CampaignMenuWorkflowTest.named(frame.getContentPane(),"solo",javax.swing.JPanel.class);
                return panel!=null&&panel.isShowing()?panel:null;
            },20);
            if(dispatch==null)throw new AssertionError(
                "Solo map-selection dispatch did not appear");
            capture("04-castle-solo-dispatch-map-selection");
            SwingUtilities.invokeLater(()->{
                JComboBox<?> site=sitePicker(dispatch);
                if(site==null)throw new AssertionError("Solo location selector missing");
                site.setSelectedItem(SiteCatalog.Location.HOUSE);
                if(site.getSelectedItem()!=SiteCatalog.Location.HOUSE)
                    throw new AssertionError("Could not dispatch authored House site");
                JButton approve=button(dispatch,"START ROUND");
                if(approve==null)throw new AssertionError("Solo START ROUND button unavailable");
                approve.doClick();
            });
            Component solo=waitForResponsive(()->visibleSoloCase(frame.getContentPane()),25);
            if(solo==null)throw new AssertionError(
                "Case dispatch never displayed an actual GamePanel");
            Component rendered=waitForResponsive(
                ()->soloInitialFrameReady(solo)?solo:null,65);
            if(rendered==null)throw new AssertionError(
                "Solo case first actual sized scene never completed; inspect diagnostics");
            Boolean opening=waitForResponsive(
                ()->soloOpeningActive(solo)?Boolean.TRUE:null,3);
            if(opening==null)throw new AssertionError(
                "House first frame completed without initiating the authored flyover");
            capture("05-house-opening-overflight");
            SwingUtilities.invokeAndWait(()->{
                KeyEvent skip=new KeyEvent(solo,KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),0,KeyEvent.VK_SPACE,' ');
                for(KeyListener listener:solo.getKeyListeners())listener.keyPressed(skip);
            });
            Boolean skipped=waitForResponsive(
                ()->!soloOpeningActive(solo)?Boolean.TRUE:null,5);
            if(skipped==null)throw new AssertionError(
                "Skip did not hand off the cinematic camera to the playable Hunter");
            Integer advancing=waitForResponsive(
                ()->soloFrameCount(solo)>=3?soloFrameCount(solo):null,20);
            if(advancing==null)throw new AssertionError(
                "Solo case first frame completed but gameplay frames stopped advancing");
            capture("06-house-solo-hunter-handoff");
            System.out.println("E2E PASS real House dispatch, skippable flyover and "+
                advancing+" advancing gameplay frames");
            SwingUtilities.invokeAndWait(frame::dispose);
            Files.writeString(OUTPUT.resolve("desktop-load.txt"),
                Instant.now()+" PASS Castle-first packaged launcher, embedded options, multiplayer workstation, "+
                "solo map selection and responsive House case first frames\n");
            System.out.println("PASS Beckenburg packaged desktop, multiplayer UI and solo case startup");
            System.exit(0);
        }catch(Throwable error){
            capture("FAILED-desktop-startup");
            Files.writeString(OUTPUT.resolve("desktop-load.txt"),
                Instant.now()+" FAIL "+error+"\nphase="+BeckenburgDiagnostics.stageName()+"\n");
            error.printStackTrace();
            System.exit(1);
        }
    }
}
