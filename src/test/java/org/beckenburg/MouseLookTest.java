package org.beckenburg;

import javax.swing.JPanel;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

/** Headless safety checks: motion without capture is inert; angles stay bounded. */
public final class MouseLookTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        SceneRenderer camera=new SceneRenderer();
        double oldYaw=camera.yaw;
        var defaultMouse=new MousePreferences.Settings(100,false);
        MouseLook.rotate(camera,100,-100,defaultMouse);
        check(Math.abs(camera.yaw-(oldYaw-.4))<.0001,"original 100% yaw sensitivity");
        check(camera.pitch>0,"moving mouse up raises gaze");
        MouseLook.rotate(camera,0,-1000,defaultMouse);
        check(camera.pitch==1.05,"upper pitch clamp");
        MouseLook.rotate(camera,0,1000,defaultMouse);
        check(camera.pitch==-1.05,"lower pitch clamp");
        var slowCamera=new SceneRenderer();
        double before=slowCamera.yaw;
        MouseLook.rotate(slowCamera,100,-100,new MousePreferences.Settings(50,false));
        check(Math.abs(slowCamera.yaw-(before-.2))<1e-8&&slowCamera.pitch>0,
            "50% mouse sensitivity must halve yaw and pitch");
        var invertedCamera=new SceneRenderer();
        MouseLook.rotate(invertedCamera,0,-100,new MousePreferences.Settings(100,true));
        check(invertedCamera.pitch<0,"inverted vertical mouse movement looks down");
        try{new MousePreferences.Settings(0,false);throw new AssertionError("invalid sensitivity accepted");}
        catch(IllegalArgumentException expected){}
        try{new MousePreferences.Settings(201,false);throw new AssertionError("out-of-range sensitivity accepted");}
        catch(IllegalArgumentException expected){}
        JPanel panel=new JPanel();
        AtomicInteger observed=new AtomicInteger();
        MouseLook look=new MouseLook(panel,(dx,dy)->observed.addAndGet(Math.abs(dx)+Math.abs(dy)));
        look.mouseMoved(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,0,0,10,11,0,false));
        check(observed.get()==0,"free desktop pointer must not rotate game view");
        look.capture();check(!look.captured(),"invisible component may not lock cursor");
        look.focusLost(new FocusEvent(panel,FocusEvent.FOCUS_LOST));
        check(!look.captured(),"focus loss releases capture");
        look.close();check(panel.getCursor()!=null,"restored cursor");
        System.out.println("PASS: mouse-look directions, clamps, uncaptured safety and focus release");
    }
}
