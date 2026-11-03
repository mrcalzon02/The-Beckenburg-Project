package org.beckenburg;

import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Actual human multi-selection and native transform/runtime readback, with workspace-only fixture outputs. */
public final class AssetImportBoxSelectionGuiTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object owner,String name)throws Exception{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static Object call(Object owner,String name)throws Exception{var m=owner.getClass().getDeclaredMethod(name);m.setAccessible(true);return m.invoke(owner);}
    private static void await(java.util.function.BooleanSupplier condition)throws Exception{
        long end=System.nanoTime()+30_000_000_000L;
        while(System.nanoTime()<end){var ready=new java.util.concurrent.atomic.AtomicBoolean();SwingUtilities.invokeAndWait(()->ready.set(condition.getAsBoolean()));if(ready.get())return;Thread.sleep(20);}
        throw new AssertionError("Human box selection did not complete");
    }
    private static void event(JPanel canvas,int kind,int x,int y){canvas.dispatchEvent(new MouseEvent(canvas,kind,System.currentTimeMillis(),kind==MouseEvent.MOUSE_DRAGGED?InputEvent.BUTTON1_DOWN_MASK:0,x,y,kind==MouseEvent.MOUSE_DRAGGED?0:1,false,kind==MouseEvent.MOUSE_DRAGGED?MouseEvent.NOBUTTON:MouseEvent.BUTTON1));}
    private static void box(AssetImportDialog dock,boolean xray)throws Exception{
        ((JCheckBox)field(dock,"xraySelect")).setSelected(xray);var canvas=(JPanel)field(dock,"preview");
        event(canvas,MouseEvent.MOUSE_PRESSED,10,10);event(canvas,MouseEvent.MOUSE_DRAGGED,dock.workingViewportWidth()-10,canvas.getHeight()-10);event(canvas,MouseEvent.MOUSE_RELEASED,dock.workingViewportWidth()-10,canvas.getHeight()-10);
    }
    private static void lasso(AssetImportDialog dock,boolean xray)throws Exception{
        ((JComboBox<?>)field(dock,"areaShape")).setSelectedItem("LASSO");((JCheckBox)field(dock,"xraySelect")).setSelected(xray);
        var canvas=(JPanel)field(dock,"preview");int half=dock.workingViewportWidth(),height=Math.max(180,canvas.getHeight());double scale=Math.min(half-26,height-36);
        int x=half/2-(int)Math.round(scale*.5),y=height/2+(int)Math.round(scale*.5);
        event(canvas,MouseEvent.MOUSE_PRESSED,x-8,y);event(canvas,MouseEvent.MOUSE_DRAGGED,x,y-8);event(canvas,MouseEvent.MOUSE_DRAGGED,x+8,y);event(canvas,MouseEvent.MOUSE_DRAGGED,x,y+8);event(canvas,MouseEvent.MOUSE_DRAGGED,x-8,y);event(canvas,MouseEvent.MOUSE_RELEASED,x-8,y);
    }
    private static boolean ready(AssetImportDialog dock){try{return !((Boolean)field(dock,"busy"))&&((JButton)field(dock,"apply")).isEnabled();}catch(Exception e){throw new RuntimeException(e);}}
    public static void main(String[] args)throws Exception{
        Path root=Path.of("build/editor-agent-evidence").toAbsolutePath();Files.createDirectories(root);Path source=root.resolve("box-selection-native.obj");
        Files.writeString(source,"""
            o back
            v 0 0 1
            v 1 0 1
            v 0 1 1
            v 0 0 -1
            v 1 0 -1
            v 0 1 -1
            vt 0 0
            vt 1 0
            vt 0 1
            f 1/1 2/2 3/3
            o front
            f 4/1 5/2 6/3
            """);
        var ref=new AtomicReference<AssetImportDialog>();
        try{
            SwingUtilities.invokeAndWait(()->{var dock=new AssetImportDialog(null,id->{});ref.set(dock);try{((JDialog)field(dock,"dialog")).setVisible(true);}catch(Exception e){throw new RuntimeException(e);}dock.loadSource(source);});
            await(()->ready(ref.get()));var projectRef=new AtomicReference<EditorTopologyProject>();var initial=new AtomicReference<EditorTopologyProject.State>();
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ref.get();var project=(EditorTopologyProject)field(dock,"topologyProject");projectRef.set(project);initial.set(project.snapshot());
                ((JComboBox<?>)field(dock,"meshView")).setSelectedItem(EditorMeshPicking.Projection.FRONT);((JToggleButton)field(dock,"boxSelect")).setSelected(true);box(dock,false);
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ref.get();var selected=(Set<?>)field(dock,"selectedVertices");var front=new HashSet<Long>();for(var v:initial.get().vertices())if(v.z()<0)front.add(v.id());
                check(selected.equals(front),"Actual visible-only rectangle excludes overlapping back vertices");check(projectRef.get().snapshot()==initial.get(),"Human selection leaves native geometry untouched");
                var history=field(dock,"meshHistory");check(((ArrayDeque<?>)field(history,"undo")).isEmpty(),"Selection adds no history entry");box(dock,true);
                var rootPane=((JDialog)field(dock,"dialog")).getRootPane();var key=rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0));
                rootPane.getActionMap().get(key).actionPerformed(new ActionEvent(dock,0,"Escape"));
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ref.get();check(((Set<?>)field(dock,"selectedVertices")).size()==3,"Escape cancels pending X-ray result without replacing visible selection");
                box(dock,true);projectRef.get().restore(projectRef.get().snapshot());
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ref.get();check(((Set<?>)field(dock,"selectedVertices")).size()==3,"Native revision change refuses stale asynchronous selection");lasso(dock,false);
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ref.get();check(((Set<?>)field(dock,"selectedVertices")).size()==1,"Real visible lasso selects only front native corner");lasso(dock,true);}catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{var dock=ref.get();check(((Set<?>)field(dock,"selectedVertices")).size()==2,"Real X-ray lasso selects overlapping front/back corner IDs");((JComboBox<?>)field(dock,"areaShape")).setSelectedItem("BOX");box(dock,true);}catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{
                var dock=ref.get();check(((Set<?>)field(dock,"selectedVertices")).size()==6,"Human X-ray rectangle selects both native layers");
                var canvas=(JPanel)field(dock,"preview");var handle=((List<?>)call(dock,"gizmoHandles")).stream().filter(h->{try{return ((Integer)field(h,"axis"))==0;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
                int x=(Integer)field(handle,"x"),y=(Integer)field(handle,"y"),end=x+(int)Math.round((Double)field(handle,"unitX")*.1);
                event(canvas,MouseEvent.MOUSE_PRESSED,x,y);event(canvas,MouseEvent.MOUSE_DRAGGED,end,y);check(projectRef.get().snapshot()==initial.get(),"Multi-selection drag is preview only");
                double delta=(Double)field(dock,"dragDelta");check(delta!=0,"Nonzero real group drag");event(canvas,MouseEvent.MOUSE_RELEASED,end,y);var after=projectRef.get().snapshot();
                check(after.faces().equals(initial.get().faces()),"Group transform retains native connectivity and corner UVs");
                for(int i=0;i<after.vertices().size();i++){var a=initial.get().vertices().get(i);var b=after.vertices().get(i);check(b.id()==a.id()&&b.x()==a.x()+delta&&b.y()==a.y()&&b.z()==a.z(),"Each selected native ID moves exactly once");}
                check(((Set<?>)field(dock,"selectedVertices")).size()==6,"Group remains selected after native commit");
                ((JButton)field(dock,"undoMesh")).doClick();check(projectRef.get().snapshot().equals(initial.get()),"One native undo reverses entire box-selected group");
                ((JButton)field(dock,"redoMesh")).doClick();check(projectRef.get().snapshot().equals(after),"One native redo restores group");
                var mesh=projectRef.get().mesh();var runtime=GlbProps.decode(EditorGlbWriter.encode(mesh)).triangles();check(runtime.size()==mesh.triangles().size(),"Actual game decoder triangle count");
                for(int i=0;i<runtime.size();i++){var a=runtime.get(i);var b=mesh.triangles().get(i);var actual=List.of(a.a(),a.b(),a.c());var expected=List.of(b.a(),b.b(),b.c());for(int j=0;j<3;j++){var p=actual.get(j);var q=expected.get(j);check(p.x()==(double)(float)q.x()&&p.y()==(double)(float)q.y()&&p.z()==(double)(float)q.z()&&p.u()==(double)(float)q.u()&&p.v()==(double)(float)q.v(),"Every group-transformed runtime corner and UV");}}
            }catch(Exception e){throw new RuntimeException(e);}});
            await(()->ready(ref.get()));
            SwingUtilities.invokeAndWait(()->{try{var content=((JDialog)field(ref.get(),"dialog")).getContentPane();var image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();try{content.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",root.resolve("box-selection-human.png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            System.out.println("PASS actual human visible/X-ray boxes and lassos, canceled/stale asynchronous selection, immutable native state, multi-vertex axis drag, one-step undo/redo and every real game-decoder corner");
        }finally{SwingUtilities.invokeAndWait(()->{if(ref.get()!=null)try{((JDialog)field(ref.get(),"dialog")).dispose();}catch(Exception e){throw new RuntimeException(e);}});}
    }
}
