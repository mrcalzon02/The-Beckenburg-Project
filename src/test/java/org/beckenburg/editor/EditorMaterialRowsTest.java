package org.beckenburg.editor;

import java.awt.Container;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import static javax.swing.event.ListDataEvent.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Material Editor face selection, filtering and Swing event scope on the real EDT. */
public final class EditorMaterialRowsTest {
    private record Event(int type, int start, int end) { }
    private static final class Events implements ListDataListener {
        final List<Event> items=new ArrayList<>();
        private void add(ListDataEvent e) { items.add(new Event(e.getType(),e.getIndex0(),e.getIndex1())); }
        public void intervalAdded(ListDataEvent e) { add(e); }
        public void intervalRemoved(ListDataEvent e) { add(e); }
        public void contentsChanged(ListDataEvent e) { add(e); }
        void expect(Event... expected) {
            check(items.equals(List.of(expected)),"Wrong list event ranges: " + items);
            items.clear();
        }
    }
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    private static <T> T find(Container root, Class<T> type, String text) {
        for (var child:root.getComponents()) {
            if (type.isInstance(child) && (text==null || child instanceof JButton button && text.equals(button.getText())))
                return type.cast(child);
            if (child instanceof Container nested) {
                T found=find(nested,type,text);
                if (found!=null) return found;
            }
        }
        return null;
    }
    private static State withFaces(State base, List<Face> faces) {
        return new State(base.sceneId(),base.sourceFingerprint(),base.name(),0,base.vertices(),faces);
    }
    @SuppressWarnings("unchecked")
    private static void test() {
        int n=1024;
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("rows",List.of(),0),
            "0".repeat(64),Path.of("rows.btop"));
        var seed=doc.snapshot();
        var vertices=new ArrayList<Vertex>();
        var faces=new ArrayList<Face>();
        for (int i=0;i<n;i++) {
            int v=3*i;
            vertices.add(new Vertex(v,"panel",i,0,0));
            vertices.add(new Vertex(v+1,"panel",i+1,0,0));
            vertices.add(new Vertex(v+2,"panel",i,1,0));
            long id=(1L << 43)+(n-1-i)*17L;
            faces.add(new Face(id,"panel",new Corner(v,0,0),new Corner(v+1,1,0),new Corner(v+2,0,1)));
        }
        var base=new State(seed.sceneId(),seed.sourceFingerprint(),"rows",0,vertices,faces);
        doc.restore(base);
        var selected=new ArrayList<Long>();
        var view=new EditorMaterialWorkspace(new JPanel(),new JPanel(),new JPanel(),selected::add);
        JList<Face> list=find(view,JList.class,null);
        var events=new Events();
        list.getModel().addListDataListener(events);
        int[] selections={0}, renders={0};
        list.addListSelectionListener(e->{if (!e.getValueIsAdjusting()) selections[0]++;});
        var renderer=list.getCellRenderer();
        list.setCellRenderer((owner,value,index,active,focus)->{
            renders[0]++;
            return renderer.getListCellRendererComponent(owner,value,index,active,focus);
        });

        long target=faces.get(600).id(), other=faces.get(100).id();
        view.observe(base,other,null,false);
        events.expect(new Event(INTERVAL_ADDED,0,n-1));
        view.observe(base,target,null,false);
        int changes=selections[0];
        view.observe(base,target,null,false);
        events.expect();
        check(selections[0]==changes && list.getSelectedIndex()==600,"Same-state selection churned or lost sparse ID");
        int height=list.getCellBounds(0,0).height;
        list.setFont(list.getFont().deriveFont(list.getFont().getSize2D()*2));
        check(list.getCellBounds(0,0).height>height,"Font enlargement kept clipped row height");

        var edit=doc.previewUvTransform(target,false,UvTransform.MOVE,.25,0,false);
        var candidate=edit.snapshot();
        renders[0]=0;
        view.observe(candidate,target,null,"UV");
        events.expect(new Event(CONTENTS_CHANGED,600,600));
        check(list.getSelectedValue()==candidate.faces().get(600),"Selected row retained pre-preview native face");
        view.observe(base,target,null,false);
        events.expect(new Event(CONTENTS_CHANGED,600,600));
        list.getPreferredSize();
        check(list.getSelectedValue()==base.faces().get(600) && renders[0]<8,"Cancel changed selection or remeasured every row");
        check(selected.isEmpty(),"Programmatic selection fed back to native project");
        list.setSelectedIndex(100);
        check(selected.equals(List.of(other)),"User selection did not report its stable face ID");

        view.observe(base,target,null,false);
        find(view,JButton.class,"Hide").doClick(0);
        events.expect(new Event(INTERVAL_REMOVED,n-1,n-1),new Event(CONTENTS_CHANGED,0,n-2));
        check(view.visibleFaces()==n-1 && list.isSelectionEmpty() && !view.visibility().includes(target),"Hide lost its filter");
        view.observe(candidate,other,null,"UV");
        events.expect();
        check(list.getSelectedValue()==candidate.faces().get(100),"Hidden-face preview changed the selected item");
        view.observe(base,other,null,false);
        events.expect();
        find(view,JButton.class,"Isolate").doClick(0);
        events.expect(new Event(INTERVAL_REMOVED,1,n-2),new Event(CONTENTS_CHANGED,0,0));
        check(view.visibleFaces()==1 && list.getSelectedValue().id()==other,"Isolation lost stable ID");
        find(view,JButton.class,"Show all").doClick(0);
        events.expect(new Event(INTERVAL_ADDED,1,n-1),new Event(CONTENTS_CHANGED,0,0));
        check(view.visibleFaces()==n && list.getSelectedIndex()==100,"Show all did not restore selected sparse ID");

        var reverse=new ArrayList<>(base.faces());
        Collections.reverse(reverse);
        var reordered=withFaces(base,reverse);
        view.observe(reordered,other,null,false);
        events.expect(new Event(CONTENTS_CHANGED,0,n-1));
        check(list.getSelectedIndex()==n-1-100 && list.getSelectedValue().id()==other,"Identity cache retained the old order");
        var shortened=withFaces(base,reverse.subList(0,8));
        view.observe(shortened,other,null,false);
        events.expect(new Event(INTERVAL_REMOVED,8,n-1),new Event(CONTENTS_CHANGED,0,7));
        check(list.isSelectionEmpty() && !find(view,JButton.class,"Hide").isEnabled(),"Removed face remained selected");
        view.observe(null,null,null,false);
        events.expect(new Event(INTERVAL_REMOVED,0,7));
        check(list.getModel().getSize()==0 && selected.equals(List.of(other)),"Closing a document retained rows or sent a callback");
        System.out.println("PASS Material Editor rows: bounded EDT events, candidate/cancel, sparse selection, filtering, reorder, removal and font sizing");
    }
    public static void main(String[] args) throws Exception { SwingUtilities.invokeAndWait(EditorMaterialRowsTest::test); }
}
