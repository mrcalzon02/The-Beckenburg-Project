package org.beckenburg;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import java.util.Objects;

/** Only operational notices belong in lobby history; frame telemetry stays in the viewport. */
final class LanLobbyLog {
    static final int MAX_CHARS=16384,MAX_LINES=240;
    static boolean recordUnrecognized(String frame){
        return frame.startsWith("INFO|")||frame.startsWith("REJECT|")
            ||frame.startsWith("ERROR|")||frame.startsWith("ACCEPT|")
            ||frame.startsWith("RESULT|")||frame.startsWith("LOBBY|");
    }
    static void append(JTextArea area,String message){
        Objects.requireNonNull(area);Objects.requireNonNull(message);
        area.append(message);
        var document=area.getDocument();
        var lines=document.getDefaultRootElement();
        int trim=0;
        if(lines.getElementCount()>MAX_LINES)
            trim=lines.getElement(lines.getElementCount()-MAX_LINES).getStartOffset();
        if(document.getLength()>MAX_CHARS)
            trim=Math.max(trim,document.getLength()-MAX_CHARS);
        if(trim>0)try{document.remove(0,trim);}
        catch(BadLocationException impossible){throw new IllegalStateException("Invalid lobby log boundary",impossible);}
    }
}
