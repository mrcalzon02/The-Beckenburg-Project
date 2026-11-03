package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.*;

/** Real topological editor chrome, native state identity, accessible tools/menus and panel geometry. */
public final class EditorTopologyWorkspaceGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static AbstractButton button(Container parent,String name){for(var child:parent.getComponents()){if(child instanceof AbstractButton b&&b.getText().equals(name))return b;if(child instanceof Container c){var found=button(c,name);if(found!=null)return found;}}return null;}
    private static void capture(JRootPane root,Path target)throws Exception{var image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{root.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",target.toFile());}
    private static double fraction(Component view,Component root){return (double)view.getWidth()*view.getHeight()/root.getWidth()/root.getHeight();}
    private static void flush()throws Exception{SwingUtilities.invokeAndWait(()->{});}
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("editor-workspace-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",home.toString());Path output=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(output);
        var mesh=new AtomicReference<AssetImportDialog>();
        try{
            Path source=home.resolve("workspace.obj");Files.writeString(source,"v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n");
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,id->{});mesh.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});
            long end=System.nanoTime()+30_000_000_000L;while(true){var ready=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->{try{ready.set(field(mesh.get(),"topologyProject")!=null&&!(Boolean)field(mesh.get(),"busy")&&((JButton)field(mesh.get(),"apply")).isEnabled());}catch(Exception e){throw new RuntimeException(e);}});if(ready.get())break;if(System.nanoTime()>end)throw new AssertionError("Actual mesh editor did not open");Thread.sleep(20);}
            var nativeState=new AtomicReference<EditorTopologyProject.State>();
            SwingUtilities.invokeAndWait(()->{try{var dock=mesh.get();var window=(JDialog)field(dock,"dialog");var project=(EditorTopologyProject)field(dock,"topologyProject");nativeState.set(project.snapshot());check(dock.workingViewportWidth()==((Component)field(dock,"preview")).getWidth(),"Ordinary model editing uses a single full-width working view");button(window,"Mesh parts").doClick();((JList<?>)field(dock,"partList")).setSelectedIndex(0);check(((Set<?>)field(dock,"selectedVertices")).size()==3&&project.snapshot()==nativeState.get(),"Native part outliner selects actual stable IDs without mutating geometry");check(((JButton)field(dock,"apply")).isEnabled(),"Pure outliner selection retains the reviewed native publication");button(window,"Mesh parts").doClick();window.validate();((JCheckBox)field(dock,"compareView")).doClick();window.validate();check(dock.workingViewportWidth()==((Component)field(dock,"preview")).getWidth()/2,"Explicit comparison uses two real mesh views");((JCheckBox)field(dock,"compareView")).doClick();window.validate();var viewport=(JPanel)field(dock,"preview");check(fraction(viewport,window.getContentPane())>.55,"Mesh viewport is primary workspace");check(window.getJMenuBar().getMenu(0).getItem(2).getAccelerator()!=null,"Native Save has discoverable shortcut");button(window,"Transform").doClick();window.validate();check(((JSpinner)field(dock,"transformAmount")).isShowing()&&!((JSpinner)field(dock,"primitiveWidth")).isShowing(),"Contextual inspector shows only active tool settings");capture(window.getRootPane(),output.resolve("workspace-mesh.png"));button(window,"Diagnostics").doClick();}catch(Exception e){throw new RuntimeException(e);}});flush();
            SwingUtilities.invokeAndWait(()->{try{var dock=mesh.get();var window=(JDialog)field(dock,"dialog");var workspace=(EditorTopologyWorkspace)field(dock,"workspace");check(workspace.browsersVisible()&&((JTextArea)field(dock,"details")).isShowing(),"Diagnostics opens in bottom browser");button(window,"Focus view").doClick();}catch(Exception e){throw new RuntimeException(e);}});flush();
            SwingUtilities.invokeAndWait(()->{try{var dock=mesh.get();var window=(JDialog)field(dock,"dialog");capture(window.getRootPane(),output.resolve("workspace-mesh-focus.png"));check(fraction((Component)field(dock,"preview"),window.getContentPane())>.8,"Focus view maximizes actual viewport: "+((Component)field(dock,"preview")).getSize()+" / "+window.getContentPane().getSize()+" focus="+((EditorTopologyWorkspace)field(dock,"workspace")).focused());button(window,"Focus view").doClick();}catch(Exception e){throw new RuntimeException(e);}});flush();
            SwingUtilities.invokeAndWait(()->{try{var dock=mesh.get();var workspace=(EditorTopologyWorkspace)field(dock,"workspace");check(workspace.browsersVisible()&&workspace.inspectorVisible(),"Focus restores prior panel visibility");workspace.setBrowsersVisible(false);var window=(JDialog)field(dock,"dialog");window.setSize(900,620);window.validate();button(window,"Create").doClick();check(((JSpinner)field(dock,"primitiveWidth")).isShowing()&&((Component)field(dock,"preview")).getHeight()>180,"Small window retains viewport and reachable contextual controls");check(((EditorTopologyProject)field(dock,"topologyProject")).snapshot()==nativeState.get(),"Layout changes never modify native scene authority");capture(window.getRootPane(),output.resolve("workspace-mesh-small.png"));}catch(Exception e){throw new RuntimeException(e);}});flush();
            System.out.println("PASS actual topological mesh workspace: contextual tools, menus/shortcuts, viewport allocation, diagnostics/focus restoration, small-window layouts and unchanged native authority");
        }finally{SwingUtilities.invokeAndWait(()->{if(mesh.get()!=null)try{((JDialog)field(mesh.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(home)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
