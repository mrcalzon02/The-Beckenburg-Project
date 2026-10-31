package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * World-space authoring UI for independent underground support slabs,
 * axis-aligned solid collisions, and explicit cross-layer traversal anchors.
 * This intentionally edits query geometry: mesh/cave visuals are separate.
 */
public final class LayeredGeometryEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final LayeredTerrainDocument document;
    private final DefaultListModel<Object> rows=new DefaultListModel<>();
    private final JList<Object> list=new JList<>(rows);
    private final ArrayDeque<LayeredTerrainDocument.Snapshot> undo=new ArrayDeque<>();
    private final ArrayDeque<LayeredTerrainDocument.Snapshot> redo=new ArrayDeque<>();
    private final JLabel status=new JLabel();
    public static JFrame open(Window owner,StitchedHeightmapWorld world)throws IOException {
        JFrame f=new JFrame("Beckenburg — Underground Layers, Solids & Links");
        f.setContentPane(new LayeredGeometryEditor(f,world));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setMinimumSize(new Dimension(740,460));
        f.setSize(1000,640);
        f.setLocationRelativeTo(owner);
        f.setVisible(true);
        return f;
    }
    private LayeredGeometryEditor(JFrame frame,StitchedHeightmapWorld world)throws IOException {
        super(new BorderLayout(6,6));
        this.frame=frame;this.world=world;
        document=LayeredTerrainDocument.open(world);
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel toolbar=new JPanel(new FlowLayout(FlowLayout.LEFT,4,3));
        button(toolbar,"ADD FLOOR",()->editFloor(null));
        button(toolbar,"SMOOTH RAMP ENTRANCES",()->{
            try{RampEntranceEditor.open(frame,world);}
            catch(IOException|RuntimeException failure){error("Ramp editor failed",failure);}
        });
        button(toolbar,"ADD SOLID",()->editSolid(null));
        button(toolbar,"ADD CONNECTION",()->editLink(null));
        button(toolbar,"EDIT SELECTED",this::editSelected);
        button(toolbar,"DELETE SELECTED",this::deleteSelected);
        button(toolbar,"UNDO",()->history(false));
        button(toolbar,"REDO",()->history(true));
        button(toolbar,"VERIFY SAVE",this::save);
        add(toolbar,BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel foot=new JPanel(new GridLayout(2,1,3,2));
        foot.add(new JLabel("World-space floors and obstacles. Stair/ladder links are explicit nav connections, "+
            "not yet generated 3D geometry or gameplay collision."));
        foot.add(status);
        add(foot,BorderLayout.SOUTH);
        refresh("Layer authoring ready");
    }
    private static void button(JPanel target,String title,Runnable handler){
        JButton b=new JButton(title);b.addActionListener(e->handler.run());target.add(b);
    }
    private static JPanel form(String[] labels,JTextField[] fields){
        JPanel p=new JPanel(new GridLayout(0,2,5,4));
        for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);}
        return p;
    }
    private static JTextField[] values(String... defaults){
        JTextField[] result=new JTextField[defaults.length];
        for(int i=0;i<defaults.length;i++)result[i]=new JTextField(defaults[i],13);
        return result;
    }
    private double num(JTextField field){
        double value=Double.parseDouble(field.getText().trim());
        if(!Double.isFinite(value))throw new IllegalArgumentException("Coordinates must be finite");
        return value;
    }
    private String label(JTextField field){
        String value=field.getText().trim();
        if(!value.matches("[A-Za-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Use a 1–48 character identifier [A-Za-z0-9_-]");
        return value;
    }
    private void mutate(Runnable mutation){
        var before=document.snapshot();
        try{
            mutation.run();
            if(before.equals(document.snapshot()))return;
            document.save();
            if(!document.snapshot().equals(LayeredTerrainDocument.open(world).snapshot()))
                throw new IOException("Layer authoring save/readback mismatch");
            undo.push(before);if(undo.size()>80)undo.removeLast();
            redo.clear();
            refresh("Saved and verified authored geometry");
        }catch(IOException|RuntimeException failure){
            document.restore(before);
            error("Cannot edit layered geometry",failure);
        }
    }
    private void editFloor(LayeredTerrainQueries.Floor old){
        var f=values(old==null?"sewer":old.layer(),
            old==null?"0":String.valueOf(old.x0()),
            old==null?"0":String.valueOf(old.z0()),
            old==null?"8":String.valueOf(old.x1()),
            old==null?"8":String.valueOf(old.z1()),
            old==null?"-3":String.valueOf(old.y0()),
            old==null?"0":String.valueOf(old.riseX()),
            old==null?"0":String.valueOf(old.riseZ()));
        if(JOptionPane.showConfirmDialog(frame,form(new String[]{
            "Layer ID (not surface)","Min X (m)","Min Z (m)","Max X (m)","Max Z (m)",
            "Floor height at min X/Z (m)","Rise per meter in X","Rise per meter in Z"
        },f),"Author floor",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            var floor=new LayeredTerrainQueries.Floor(label(f[0]),num(f[1]),num(f[2]),
                num(f[3]),num(f[4]),num(f[5]),num(f[6]),num(f[7]));
            mutate(()->{
                var floors=new ArrayList<>(document.floors());
                if(old!=null)floors.remove(old);
                floors.add(floor);
                document.restore(new LayeredTerrainDocument.Snapshot(
                    floors,document.solids(),document.links()));
            });
        }catch(RuntimeException failure){error("Invalid authored geometry",failure);}
    }
    private void editSolid(LayeredTerrainQueries.Solid old){
        var f=values(old==null?"wall-1":old.id(),
            old==null?"0":String.valueOf(old.x0()),
            old==null?"-3":String.valueOf(old.y0()),
            old==null?"0":String.valueOf(old.z0()),
            old==null?"1":String.valueOf(old.x1()),
            old==null?"0":String.valueOf(old.y1()),
            old==null?"1":String.valueOf(old.z1()));
        if(JOptionPane.showConfirmDialog(frame,form(new String[]{
            "Unique solid ID","Min X","Min Y","Min Z","Max X","Max Y","Max Z"
        },f),"Author solid collision",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            var solid=new LayeredTerrainQueries.Solid(label(f[0]),num(f[1]),num(f[2]),
                num(f[3]),num(f[4]),num(f[5]),num(f[6]));
            mutate(()->{
                var solids=new ArrayList<>(document.solids());
                if(old!=null)solids.remove(old);
                solids.add(solid);
                document.restore(new LayeredTerrainDocument.Snapshot(
                    document.floors(),solids,document.links()));
            });
        }catch(RuntimeException failure){error("Invalid authored geometry",failure);}
    }
    private void editLink(AdaptiveNavigationRegions.Transition old){
        var f=values(old==null?"surface":old.from(),
            old==null?"sewer":old.to(),
            old==null?"4":String.valueOf(old.fromX()),
            old==null?"4":String.valueOf(old.fromZ()),
            old==null?"4":String.valueOf(old.toX()),
            old==null?"4":String.valueOf(old.toZ()),
            old==null?"STAIRS":old.connection().name());
        if(JOptionPane.showConfirmDialog(frame,form(new String[]{
            "From layer","To layer","From X","From Z","To X","To Z",
            "Traversal (STAIRS / LADDER / DROP)"
        },f),"Author cross-layer connection",JOptionPane.OK_CANCEL_OPTION)
                !=JOptionPane.OK_OPTION)return;
        try{
            var link=new AdaptiveNavigationRegions.Transition(
                label(f[0]),label(f[1]),num(f[2]),num(f[3]),num(f[4]),num(f[5]),
                AdaptiveNavigationRegions.Connection.valueOf(f[6].getText().trim().toUpperCase()));
            mutate(()->{
                var links=new ArrayList<>(document.links());
                if(old!=null)links.remove(old);
                links.add(link);
                document.restore(new LayeredTerrainDocument.Snapshot(
                    document.floors(),document.solids(),links));
            });
        }catch(RuntimeException failure){error("Invalid authored geometry",failure);}
    }
    private void editSelected(){
        Object value=list.getSelectedValue();
        if(value instanceof LayeredTerrainQueries.Floor floor)editFloor(floor);
        else if(value instanceof LayeredTerrainQueries.Solid solid)editSolid(solid);
        else if(value instanceof AdaptiveNavigationRegions.Transition link)editLink(link);
        else refresh("Select an authored object");
    }
    private void deleteSelected(){
        Object value=list.getSelectedValue();
        if(value==null){refresh("Select an authored object");return;}
        if(JOptionPane.showConfirmDialog(frame,"Delete "+value+"?",
            "Confirm authored geometry deletion",JOptionPane.YES_NO_OPTION)
                !=JOptionPane.YES_OPTION)return;
        mutate(()->{
            var floors=new ArrayList<>(document.floors());
            var solids=new ArrayList<>(document.solids());
            var links=new ArrayList<>(document.links());
            floors.remove(value);solids.remove(value);links.remove(value);
            document.restore(new LayeredTerrainDocument.Snapshot(floors,solids,links));
        });
    }
    private void history(boolean forward){
        var stack=forward?redo:undo;
        var target=stack.peek();
        if(target==null)return;
        var before=document.snapshot();
        try{
            document.restore(target);
            document.save();
            if(!document.snapshot().equals(LayeredTerrainDocument.open(world).snapshot()))
                throw new IOException("History save/readback mismatch");
            stack.pop();
            (forward?undo:redo).push(before);
            refresh("Restored authored geometry revision");
        }catch(IOException|RuntimeException failure){
            document.restore(before);error("History failed",failure);
        }
    }
    private void save(){
        try{
            document.save();
            if(!document.snapshot().equals(LayeredTerrainDocument.open(world).snapshot()))
                throw new IOException("Save readback mismatch");
            refresh("Authored geometry verified on disk");
        }catch(IOException|RuntimeException failure){error("Save failed",failure);}
    }
    private void refresh(String message){
        rows.clear();
        for(var f:document.floors())rows.addElement(f);
        for(var s:document.solids())rows.addElement(s);
        for(var l:document.links())rows.addElement(l);
        status.setText(message+" | floors "+document.floors().size()+
            " | solids "+document.solids().size()+" | traversal links "+
            document.links().size()+" | revision "+document.revision());
    }
    private void error(String message,Exception e){
        status.setText(message+": "+e.getMessage());
        JOptionPane.showMessageDialog(frame,e.getMessage(),message,
            JOptionPane.ERROR_MESSAGE);
    }
}
