package org.beckenburg;

import javax.swing.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.beckenburg.editor.*;

/** Native state may advance outside a pending render/reduction review; publication must never mix revisions. */
public final class AssetImportPublicationRevisionGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){var ready=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));if(ready.get())return;Thread.sleep(20);}throw new AssertionError("Native review worker did not complete");}
    private static boolean enabled(AssetImportDialog dock){try{return ((JButton)field(dock,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean idle(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"));}catch(Exception e){throw new RuntimeException(e);}}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-publish-revision-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        Path source=root.resolve("source.obj");Files.writeString(source,"v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n");var ui=new AtomicReference<AssetImportDialog>();var published=new AtomicReference<String>();var nativeProject=new AtomicReference<EditorTopologyProject>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,published::set);ui.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});await(()->enabled(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var project=(EditorTopologyProject)field(dock,"topologyProject");nativeProject.set(project);long id=project.snapshot().faces().get(0).a().vertex();project.moveVertex(id,-.25,0,0);((JButton)field(dock,"apply")).doClick();}catch(Exception e){throw new RuntimeException(e);}});await(()->idle(ui.get()));
            if(published.get()!=null){double actual=GlbProps.model(published.get()).triangles().get(0).a().x(),expected=nativeProject.get().mesh().triangles().get(0).a().x();System.out.println("STALE PUBLICATION CONTROL actual runtime x="+actual+" actual native x="+expected);}
            check(published.get()==null,"Publishing an unreviewed native revision must be refused instead of archiving new native geometry with an old runtime mesh");
            check(!Files.exists(nativeProject.get().documentPath()),"Stale native review cannot save or archive before validation");
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();check(!enabled(dock),"Stale review disables publication");((JComboBox<?>)field(dock,"preset")).setSelectedIndex(4);}catch(Exception e){throw new RuntimeException(e);}});await(()->enabled(ui.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ui.get();var working=(EditorMeshPipeline.Mesh)field(dock,"source");check(working.triangles().get(0).a().x()==-.25,"Human re-review observes actual native state");((JButton)field(dock,"apply")).doClick();}catch(Exception e){throw new RuntimeException(e);}});await(()->published.get()!=null);
            check(GlbProps.model(published.get()).triangles().get(0).a().x()==-.25,"Actual game model matches reviewed native revision");
            check(EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop")).snapshot().equals(nativeProject.get().snapshot()),"Native archive and actual runtime source are the same revision");
            System.out.println("PASS actual native publication revision: stale review refuses all writes, explicit human re-review refreshes live state, coherent native/runtime publication");
        }finally{SwingUtilities.invokeAndWait(()->{if(ui.get()!=null)try{((JDialog)field(ui.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});System.setProperty("user.home",oldHome);try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
