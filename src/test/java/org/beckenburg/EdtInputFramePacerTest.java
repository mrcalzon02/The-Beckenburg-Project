package org.beckenburg;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/** Slow frame callbacks never outrank indefinitely queued player input. */
public final class EdtInputFramePacerTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        var pacer=new EdtInputFramePacer();
        ok(!pacer.decide(true),"first frame is never postponed");
        pacer.completed(19_000_000);
        ok(!pacer.decide(true),"20ms render needs no priority deferral");
        pacer.completed(110_000_000);
        ok(!pacer.decide(false),"no input means no unnecessary delay");
        ok(pacer.decide(true)&&pacer.decide(true),"slow frame yields twice");
        ok(!pacer.decide(true),"third slow-frame callback must render");
        pacer.completed(125_000_000);
        ok(pacer.decide(true),"next frame may yield again to actual input");
        ok(pacer.yields()==3,"all render yields are counted");
        pacer.reset();
        ok(!pacer.decide(true),"hidden/ended view loses stale pacing state");
        SwingUtilities.invokeAndWait(()->{
            JPanel player=new JPanel();
            JPanel other=new JPanel();
            var events=Toolkit.getDefaultToolkit().getSystemEventQueue();
            pacer.completed(80_000_000);
            events.postEvent(new KeyEvent(player,KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),0,KeyEvent.VK_W,'w'));
            ok(pacer.shouldYield(player),"queued gameplay keyboard input is prioritized");
            ok(!pacer.shouldYield(other),"other panels cannot starve this viewport");
            // Mouse look is as important as movement when a 3D frame overruns.
            pacer.completed(90_000_000);
            events.postEvent(new MouseEvent(player,MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),0,10,10,0,false));
            ok(pacer.shouldYield(player),"queued camera mouse input is prioritized");
        });
        System.out.println("PASS: bounded EDT keyboard/mouse priority under slow frames");
    }
}
