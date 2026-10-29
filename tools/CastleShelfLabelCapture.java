package org.beckenburg;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Offscreen capture of the actual Castle Swing panel and actual GPU world.
 * Creates no desktop Window and consumes no user input. */
public final class CastleShelfLabelCapture {
    private static Object field(Object owner,String name)throws Exception{
        var value=owner.getClass().getDeclaredField(name);value.setAccessible(true);return value.get(owner);
    }
    public static void main(String[] args)throws Exception{
        Path output=Path.of(System.getProperty("beckenburg.label.capture","build/label-capture"));Files.createDirectories(output);
        Class<?> desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");start.setAccessible(true);
        var stop=desktop.getDeclaredMethod("stopReviewGpu");stop.setAccessible(true);
        CastleHubPanel[] panel={null};start.invoke(null);
        try{
            SwingUtilities.invokeAndWait(()->{
                panel[0]=new CastleHubPanel(new ProfileStore(output.resolve("preview.profile")),new CastleHubPanel.Actions(){
                    public void begin(){}public void network(){}public void spectral(){}public void menu(){}public void campaign(){}
                    public void cycleRole(){}public void cycleWeather(){}public String role(){return "CLERIC";}public String weather(){return "Normal";}
                });
                panel[0].setSize(1280,800);panel[0].doLayout();
            });
            var renderer=(SceneRenderer)field(panel[0],"renderer");var map=(CastleHubMap)field(panel[0],"map");
            renderer.setRenderSize(640,400);renderer.lamp=false;
            for(var tier:EquipmentTiers.Tier.values()){
                var item=CastleWarehouse.exhibits().stream().filter(e->e.rack()==2&&e.column()==1&&e.tier()==tier).findFirst().orElseThrow();
                var rack=CastleWarehouse.racks().get(item.rack());var center=rack.world(0,.326);var eye=rack.world(0,2.65);
                renderer.x=eye[0];renderer.z=eye[1];renderer.eyeHeight=1.62;
                renderer.yaw=Math.atan2(center[0]-eye[0],center[1]-eye[1]);
                renderer.pitch=Math.atan2(item.y()+.2-renderer.eyeHeight,Math.hypot(center[0]-eye[0],center[1]-eye[1]));
                if(CastleWarehouse.aimedExhibit(map,renderer.x,renderer.eyeHeight,renderer.z,renderer.yaw,renderer.pitch)!=item)
                    throw new AssertionError("Actual Castle target caption does not match its displayed tier");
                renderer.drawCastle(map,2.5,true);long expected=renderer.submittedGpuFrameId(),deadline=System.nanoTime()+30_000_000_000L;
                var gpu=GpuWorldRenderer.active();
                while(gpu.latest()==null||gpu.latest().frameId()!=expected){if(System.nanoTime()>deadline)throw new AssertionError("Label GPU capture timed out");Thread.sleep(1);}
                renderer.pinCompletedGpuFrame(gpu.latest());renderer.drawCastle(map,2.5,true);
                var image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);
                SwingUtilities.invokeAndWait(()->{var graphics=image.createGraphics();try{panel[0].paint(graphics);}finally{graphics.dispose();}});
                ImageIO.write(image,"png",output.resolve("castle-readable-label-tier-"+tier.name().toLowerCase()+".png").toFile());
            }
            System.out.println("PASS three tier captions in actual offscreen Castle UI with completed GPU worlds");
        }finally{
            if(panel[0]!=null)SwingUtilities.invokeAndWait(panel[0]::close);
            stop.invoke(null);
        }
    }
}
