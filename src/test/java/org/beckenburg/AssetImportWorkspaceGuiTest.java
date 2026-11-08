package org.beckenburg;

import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.beckenburg.editor.*;
import static org.beckenburg.ModelEditorGuiTestSupport.*;

/** Persist the actual modeling window and restore it in another editor instance. */
public final class AssetImportWorkspaceGuiTest {
    private interface Step{void run()throws Exception;}
    private static void edt(Step step)throws Exception{SwingUtilities.invokeAndWait(()->{try{step.run();}catch(Exception e){throw new RuntimeException(e);}});}
    private static void edges(AssetImportDialog dock)throws Exception{
        await(()->{try{return (boolean)call(dock,"hasEdgeSelection",new Class<?>[0]) && ready(dock);}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static AssetImportDialog open(Path file,AtomicReference<String> published)throws Exception{
        var ref=new AtomicReference<AssetImportDialog>();edt(()->{var dock=new AssetImportDialog(null,published::set);ref.set(dock);((JDialog)field(dock,"dialog")).setVisible(true);dock.loadSource(file);});
        await(()->ready(ref.get()));return ref.get();
    }
    private static EditorModelWorkspace actual(AssetImportDialog dock)throws Exception{return (EditorModelWorkspace)call(dock,"captureWorkspace",new Class<?>[0]);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("model-window-state-");String oldHome=System.getProperty("user.home");System.setProperty("user.home",root.toString());
        Path document=root.resolve("grid.btop"),output=Path.of("build/editor-agent-evidence");Files.createDirectories(output);
        var mesh=EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,4,1,4,8,4));
        var original=EditorTopologyProject.fromMesh(mesh,"4".repeat(64),document);original.save();
        var windows=new ArrayList<AssetImportDialog>();var published=new AtomicReference<String>();
        try{
            var first=open(document,published);windows.add(first);
            edt(()->{
                ((JComboBox<?>)field(first,"meshView")).setSelectedItem(EditorMeshPicking.Projection.TOP);
                ((JComboBox<?>)field(first,"selectionMode")).setSelectedItem("EDGE");
                var ref=call(first,"workingReference",new Class<?>[0]);var view=call(first,"viewport",new Class<?>[]{List.class,int.class,int.class},ref,0,first.workingViewportWidth());
                var point=EditorMeshPicking.Projection.TOP.project(.5,0,.015);
                int x=(Integer)call(view,"x",new Class<?>[]{double.class},point.x()),y=(Integer)call(view,"y",new Class<?>[]{double.class},point.y());
                var canvas=(JPanel)field(first,"preview");canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,x,y,1,false,1));
                ((JComboBox<?>)field(first,"edgeMode")).setSelectedItem(EditorMeshEdges.Mode.LOOP);button(first,"selectEdges");
            });edges(first);
            var saved=new AtomicReference<EditorModelWorkspace>();
            edt(()->{
                ((JComboBox<?>)field(first,"meshView")).setSelectedItem(EditorMeshPicking.Projection.PERSPECTIVE);
                var canvas=(JPanel)field(first,"preview");
                for(int mouse:List.of(3,2)){
                    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),0,210,210,1,false,mouse));
                    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_DRAGGED,System.currentTimeMillis(),InputEvent.getMaskForButton(mouse),231,218,1,false,0));
                    canvas.dispatchEvent(new MouseEvent(canvas,MouseEvent.MOUSE_RELEASED,System.currentTimeMillis(),0,231,218,1,false,mouse));
                }
                canvas.dispatchEvent(new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,System.currentTimeMillis(),0,210,210,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-2));
                tool((JDialog)field(first,"dialog"),"Transform").doClick();
                ((JComboBox<?>)field(first,"transformSpace")).setSelectedIndex(2);((JComboBox<?>)field(first,"transformKind")).setSelectedItem(EditorTopologyProject.Transform.SCALE_AXIS);
                ((JComboBox<?>)field(first,"transformAxis")).setSelectedIndex(2);((JComboBox<?>)field(first,"transformPivot")).setSelectedIndex(1);
                ((JSpinner)field(first,"transformAmount")).setValue(1.75);((JSpinner)field(first,"meshSnap")).setValue(.125);
                ((JSpinner)field(first,"angleSnap")).setValue(7d);((JSpinner)field(first,"slideFraction")).setValue(.375);
                ((AbstractButton)field(first,"normalOverlay")).doClick();
                var project=(EditorTopologyProject)field(first,"topologyProject");var observation=project.observe();
                saved.set(actual(first));button(first,"saveMesh");
                check(project.isCurrent(observation)&&project.snapshot().equals(original.snapshot()),"Workspace Save leaves model and revision unchanged");
                check(saved.get().equals(EditorTopologyProject.open(document).savedWorkspace()),"Actual file owns the complete saved view");
            });
            var second=open(document,published);windows.add(second);edges(second);
            edt(()->{
                check(actual(second).equals(saved.get()),"Fresh window restores actual orbit/pan/zoom, tool, gizmo, options and native selected loop");
                check(((Set<?>)call(second,"selectedIds",new Class<?>[0])).size()==5,"Restored loop resolves five current native vertices");
                capture(second,output.resolve("human-workspace-restored.png"));
                ((JComboBox<?>)field(second,"selectionMode")).setSelectedItem("FACE");
                ((JComboBox<?>)field(second,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);((JSpinner)field(second,"meshSnap")).setValue(1d);
                button(second,"reopenMesh");
            });edges(second);
            edt(()->{
                check(actual(second).equals(saved.get()),"Actual Reopen restores saved workspace after UI changes");
                var layout=(EditorTopologyWorkspace)field(second,"workspace");layout.setBrowsersVisible(true);layout.toggleFocus();button(second,"saveMesh");
            });
            var third=open(document,published);windows.add(third);edges(third);
            var publishedView=new AtomicReference<EditorModelWorkspace>();
            edt(()->{
                var layout=(EditorTopologyWorkspace)field(third,"workspace");check(layout.focused(),"Focus layout restored in fresh editor");layout.toggleFocus();
                check(layout.browsersVisible()&&layout.inspectorVisible(),"Leaving saved focus restores the actual prior panels");publishedView.set(actual(third));button(third,"apply");
            });await(()->published.get()!=null);
            var archived=EditorTopologyProject.open(ImportedAssetLibrary.root().resolve(published.get()+".authoring.btop"));
            check(archived.snapshot().equals(original.snapshot())&&archived.savedWorkspace().equals(publishedView.get()),"Publication archives native geometry and current loop workspace");
            byte[] runtime=ImportedAssetLibrary.read(published.get());
            EditorGlbWriter.validate(original.mesh(),runtime);
            check(Arrays.equals(EditorGlbWriter.encode(original.mesh()),runtime),"Workspace changes leave the compiled game GLB byte-identical");
            Files.copy(document,output.resolve("human-workspace.btop"),StandardCopyOption.REPLACE_EXISTING);
            System.out.println("PASS actual native model workspace: camera/tool/settings/loop/focus save, fresh-window and Reopen restoration, geometry/revision isolation and published archive");
        } finally {
            edt(()->{for(var dock:windows)((JDialog)field(dock,"dialog")).dispose();});System.setProperty("user.home",oldHome);
            try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
