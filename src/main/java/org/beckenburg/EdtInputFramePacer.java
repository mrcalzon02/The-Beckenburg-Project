package org.beckenburg;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/**
 * Give real queued keys/mouse look a chance between slow synchronous 3D frames.
 * This is presentation-only: it never changes the host simulation, input
 * pump's 50Hz cadence, evidence, collision, or frame contents.
 */
final class EdtInputFramePacer {
    static final long SLOW_RENDER_NS=25_000_000L;
    static final int MAX_YIELDS=2;
    private static final int[] INPUT_EVENTS={
        KeyEvent.KEY_PRESSED,KeyEvent.KEY_RELEASED,
        MouseEvent.MOUSE_MOVED,MouseEvent.MOUSE_DRAGGED,
        MouseEvent.MOUSE_PRESSED,MouseEvent.MOUSE_RELEASED
    };
    private long lastRenderNs;
    private int consecutiveYields;
    private long totalYields;

    /** Called only by the Swing renderer before any expensive map or GL work. */
    boolean shouldYield(Component root){
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Render pacing must be on Swing EDT");
        if(lastRenderNs<=SLOW_RENDER_NS)return decide(false);
        EventQueue events=Toolkit.getDefaultToolkit().getSystemEventQueue();
        for(int id:INPUT_EVENTS){
            AWTEvent waiting=events.peekEvent(id);
            if(waiting!=null && waiting.getSource() instanceof Component source
                    && (source==root||SwingUtilities.isDescendingFrom(source,root)))
                return decide(true);
        }
        return decide(false);
    }
    /** Deterministic, testable policy. Pending input cannot starve drawing. */
    boolean decide(boolean inputPending){
        if(!inputPending||lastRenderNs<=SLOW_RENDER_NS){
            consecutiveYields=0;
            return false;
        }
        if(consecutiveYields>=MAX_YIELDS){
            consecutiveYields=0;
            return false;
        }
        consecutiveYields++;
        totalYields++;
        return true;
    }
    void completed(long durationNs){
        lastRenderNs=Math.max(0,durationNs);
        consecutiveYields=0;
    }
    void reset(){lastRenderNs=0;consecutiveYields=0;}
    long yields(){return totalYields;}
}
