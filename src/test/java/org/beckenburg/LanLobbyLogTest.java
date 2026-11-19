package org.beckenburg;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Operational notices remain visible without overflowing the Swing document. */
public final class LanLobbyLogTest {
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        check(LanLobbyLog.recordUnrecognized("INFO|mission started"),"mission notices retained");
        check(LanLobbyLog.recordUnrecognized("REJECT|blocked"),"rejections retained");
        check(LanLobbyLog.recordUnrecognized("RESULT|won"),"results retained");
        for(String raw:new String[]{"BODY|0|1000|0|2000|90|1","GPOSE|1|0|0|0|0|1",
            "DOORS|open","GHEAR|hidden", "CAMERA|PLACE|0", "STAMINA|10|40|0"})
            check(!LanLobbyLog.recordUnrecognized(raw),"world-only protocol must not flood lobby log: "+raw);
        JTextArea area=new JTextArea();
        SwingUtilities.invokeAndWait(()->{
            for(int i=0;i<700;i++)LanLobbyLog.append(area,"MESSAGE "+i+" "+"x".repeat(90)+"\n");
        });
        check(area.getDocument().getLength()<=LanLobbyLog.MAX_CHARS,"bounded log memory");
        check(area.getDocument().getDefaultRootElement().getElementCount()<=LanLobbyLog.MAX_LINES,
            "bounded log scroll history");
        check(area.getText().contains("MESSAGE 699")&&!area.getText().contains("MESSAGE 0 "),
            "newest operational information retained");
        System.out.println("PASS: non-telemetry lobby log policy and bounded Swing document");
    }
}
