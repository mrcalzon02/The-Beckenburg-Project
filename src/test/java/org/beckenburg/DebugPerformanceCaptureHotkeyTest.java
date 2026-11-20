package org.beckenburg;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;

/** Capture key repeat, focus and HUD-independent recording behavior. */
public final class DebugPerformanceCaptureHotkeyTest {
    private static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
    private static KeyEvent key(int type){
        return new KeyEvent(new JPanel(),type,System.currentTimeMillis(),
            InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK,
            KeyEvent.VK_F8,KeyEvent.CHAR_UNDEFINED);
    }
    public static void main(String[] args){
        DebugPerformanceHud hud=new DebugPerformanceHud();
        boolean overlay=DebugPerformanceHud.enabled();
        var first=key(KeyEvent.KEY_PRESSED);
        check(hud.captureHotkey(first,"test-hunter"),"F8 chord must start");
        check(first.isConsumed(),"Capture hotkey should not reach world keybinds");
        check(hud.captureStatus().startsWith("RECORDING"),"Capture must confirm start");
        check(DebugPerformanceHud.enabled()==overlay,
            "Beginning a benchmark must not render an unsolicited HUD");
        check(hud.captureHotkey(key(KeyEvent.KEY_PRESSED),"test-hunter"),
            "Held key repeat should be consumed");
        check(hud.captureStatus().startsWith("RECORDING"),
            "Held key repeat cannot immediately stop the capture");
        hud.captureKeyReleased(key(KeyEvent.KEY_RELEASED));
        check(hud.captureHotkey(key(KeyEvent.KEY_PRESSED),"test-hunter"),
            "Released F8 can stop the trace");
        check(hud.captureStatus().startsWith("CAPTURE EMPTY"),
            "Empty capture must not leave bogus files");
        hud.captureFocusLost();
        hud.closeCapture();
        System.out.println("PASS capture shortcut, repeat protection, HUD neutrality and empty stop");
    }
}
