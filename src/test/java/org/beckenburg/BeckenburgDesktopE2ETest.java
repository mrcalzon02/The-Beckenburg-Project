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
        for(Window window:Window.getWindows())if(window instanceof LanLobbyWindow frame
                &&frame.isShowing())return frame;
        return null;
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
    /** Map selection is a PSX game card, never a separate JDialog. */
    private static Container dispatchPage(Container root){
        for(Component part:root.getComponents()){
            if(part instanceof javax.swing.JPanel panel && "dispatch".equals(panel.getName())
                    &&panel.isShowing())return panel;
            if(part instanceof Container nested){
                Container found=dispatchPage(nested);
                if(found!=null)return found;
            }
        }
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
    public static void main(String[] args)throws Exception{
        if(GraphicsEnvironment.isHeadless())throw new AssertionError("Actual desktop required");
        Files.createDirectories(OUTPUT);
        try{
            InvestigationGame.main(new String[0]);
            JFrame frame=waitFor(()->{
                JFrame game=launcher();
                return game!=null&&button(game.getContentPane(),"HOST / JOIN MULTIPLAYER")!=null
                    ?game:null;
            },30);
            if(frame==null)throw new AssertionError(
                "Startup never completed: menu's multiplayer button not available (check diagnostics log)");
            if(button(frame.getContentPane(),"DIAGNOSTICS")==null)
                throw new AssertionError("Diagnostics control missing from playable launcher");
            capture("01-loaded-menu");
            System.out.println("E2E PASS packaged startup and usable PSX-themed main menu");
            SwingUtilities.invokeAndWait(()->button(frame.getContentPane(),
                "HOST / JOIN MULTIPLAYER").doClick());
            JFrame lobby=waitFor(()->{
                JFrame result=multiplayer();
                return result!=null&&button(result.getContentPane(),"Host LAN")!=null
                    &&button(result.getContentPane(),"Join by IP")!=null
                    &&button(result.getContentPane(),"Ready / Unready")!=null
                    &&button(result.getContentPane(),"Start Match")!=null?result:null;
            },15);
            if(lobby==null)throw new AssertionError("Multiplayer lobby not interactable");
            capture("02-multiplayer-lobby");
            System.out.println("E2E PASS main-menu multiplayer handoff and actionable join/host lobby");
            SwingUtilities.invokeAndWait(lobby::dispose);
            // Use the real in-game dispatch card, select House and demand an
            // advancing case. No modal window or appearance assertion is required.
            JButton start=button(frame.getContentPane(),"START SOLO INVESTIGATION");
            if(start==null||!start.isEnabled())throw new AssertionError(
                "Solo dispatch control missing or House unavailable");
            SwingUtilities.invokeLater(start::doClick);
            Container dispatch=waitForResponsive(()->dispatchPage(frame.getContentPane()),20);
            if(dispatch==null)throw new AssertionError(
                "In-game solo map-selection menu did not appear");
            capture("03-solo-dispatch-map-selection");
            SwingUtilities.invokeLater(()->{
                JComboBox<?> site=sitePicker(dispatch);
                if(site==null)throw new AssertionError("Solo location selector missing");
                site.setSelectedItem(SiteCatalog.Location.HOUSE);
                if(site.getSelectedItem()!=SiteCatalog.Location.HOUSE)
                    throw new AssertionError("Could not dispatch authored House site");
                JButton approve=button(dispatch,"BEGIN SELECTED CASE");
                if(approve==null)throw new AssertionError("Solo dispatch confirm control unavailable");
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
            capture("04-house-opening-overflight");
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
            capture("05-house-solo-hunter-handoff");
            System.out.println("E2E PASS real House dispatch, skippable flyover and "+
                advancing+" advancing gameplay frames");
            SwingUtilities.invokeAndWait(frame::dispose);
            Files.writeString(OUTPUT.resolve("desktop-load.txt"),
                Instant.now()+" PASS actual packaged launcher, menu, multiplayer lobby, "+
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
