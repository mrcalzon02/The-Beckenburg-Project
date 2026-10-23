package org.beckenburg.gpu;

import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.InvestigationGame;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Live GPU + Swing acceptance: actually selects a map and enters a solo
 * investigation through the installed game's real main-menu controls.
 */
public final class GpuUiSmoke {
 private GpuUiSmoke(){}
 private static JButton button(Container owner,String label){
  for(Component c:owner.getComponents()){
   if(c instanceof JButton b&&b.getText().equals(label))return b;
   if(c instanceof Container child){
    JButton b=button(child,label);if(b!=null)return b;
   }
  }
  return null;
 }
 private static <T> T waitFor(Supplier<T> check,long seconds)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
  while(System.nanoTime()<end){
   final Object[] result=new Object[1];
   SwingUtilities.invokeAndWait(()->result[0]=check.get());
   if(result[0]!=null){
    @SuppressWarnings("unchecked") T answer=(T)result[0];
    return answer;
   }
   Thread.sleep(85);
  }
  return null;
 }
 private static JFrame launcher(){
  for(Window w:Window.getWindows())
   if(w instanceof JFrame f&&f.isShowing()
       &&f.getTitle().startsWith("The Beckenburg Project"))return f;
  return null;
 }
 private static JDialog dispatch(){
  for(Window w:Window.getWindows())
   if(w instanceof JDialog d&&d.isShowing()
       &&d.getTitle().contains("EXPEDITION DISPATCH"))return d;
  return null;
 }
 private static JPanel game(Container root){
  for(Component c:root.getComponents()){
   if(c instanceof JPanel panel&&c.getClass().getName().contains("$GamePanel")
       &&panel.isShowing())return panel;
   if(c instanceof Container inner){
    JPanel found=game(inner);if(found!=null)return found;
   }
  }
  return null;
 }
 public static void execute(GpuWorldRenderer gpu)throws Exception{
  InvestigationGame.main(new String[0]);
  JFrame window=waitFor(()->{
   JFrame candidate=launcher();
   return candidate!=null&&button(candidate.getContentPane(),
       "START SOLO INVESTIGATION")!=null?candidate:null;
  },60);
  if(window==null)throw new AssertionError("GPU-loaded real main menu unavailable");
  System.out.println("GPU UI PASS real main menu loaded");
  SwingUtilities.invokeLater(()->{
   var start=button(window.getContentPane(),"START SOLO INVESTIGATION");
   if(start!=null)start.doClick();
  });
  JDialog chooser=waitFor(GpuUiSmoke::dispatch,15);
  if(chooser==null)throw new AssertionError("GPU launcher did not request location selection");
  SwingUtilities.invokeLater(()->{
   JButton ok=button(chooser.getContentPane(),"OK");
   if(ok!=null)ok.doClick();
  });
  JPanel inGame=waitFor(()->game(window.getContentPane()),55);
  if(inGame==null)throw new AssertionError("Selecting a mission never entered the live GamePanel");
  var first=gpu.latest();
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
  boolean rendered=false;
  while(System.nanoTime()<deadline){
   var frame=gpu.latest();
   if(frame!=null&&frame!=first){
    int visible=0;for(int argb:frame.argb())if((argb>>>24)>127)visible++;
    if(visible>400){rendered=true;break;}
   }
   Thread.sleep(70);
  }
  if(!rendered)throw new AssertionError(
      "The live investigation never delivered a new OpenGL framebuffer");
  if(!inGame.isShowing())throw new AssertionError("The live GPU game panel disappeared");
  System.out.println("GPU UI PASS actual map selection / live solo frame / interactive Swing HUD");
  SwingUtilities.invokeAndWait(window::dispose);
 }
}
