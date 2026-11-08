package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

final class ModelEditorGuiTestSupport {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception{var m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,args);}
    static void button(Object o,String name)throws Exception{((JButton)field(o,name)).doClick();}
    static boolean ready(AssetImportDialog ui){try{return !((Boolean)field(ui,"busy"))&&((JButton)field(ui,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    static boolean reviewed(AssetImportDialog ui){try{return !((Boolean)field(ui,"busy"))&&field(ui,"modelingPreview")!=null;}catch(Exception e){throw new RuntimeException(e);}}
    static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){var done=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->done.set(condition.getAsBoolean()));if(done.get())return;Thread.sleep(20);}throw new AssertionError("Human model worker did not complete");}
    static JToggleButton tool(java.awt.Container parent,String name){for(var child:parent.getComponents()){if(child instanceof JToggleButton b&&b.getText().equals(name))return b;if(child instanceof java.awt.Container nested){var found=tool(nested,name);if(found!=null)return found;}}return null;}
    static void capture(AssetImportDialog ui,Path path)throws Exception{var content=((JDialog)field(ui,"dialog")).getContentPane();var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());}
}
