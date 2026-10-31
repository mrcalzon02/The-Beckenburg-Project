package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Editor-owned physical topology connector authoring for all three sketch methods.
 * Methods 1/2 maintain split-boundary source ownership, method 3 reassigns
 * a cut-out source patch to a distinct walkable tab layer. */
public final class EntranceSeamEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final EntranceSeamDocument doc;
    private final DefaultListModel<Object> rows=new DefaultListModel<>();
    private final JList<Object> list=new JList<>(rows);
    private final JLabel status=new JLabel();
    private final ArrayDeque<EntranceSeamDocument.Snapshot> undo=new ArrayDeque<>();
    private final ArrayDeque<EntranceSeamDocument.Snapshot> redo=new ArrayDeque<>();
    public static JFrame open(Window owner,StitchedHeightmapWorld world)throws IOException {
        JFrame f=new JFrame("Beckenburg — Entrance Seam and Detached Tab Editor");
        f.setContentPane(new EntranceSeamEditor(f,world));
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setMinimumSize(new Dimension(870,510));f.setSize(1080,630);
        f.setLocationRelativeTo(owner);f.setVisible(true);return f;
    }
    private EntranceSeamEditor(JFrame frame,StitchedHeightmapWorld world)throws IOException{
        super(new BorderLayout(5,5));
        this.frame=frame;this.world=world;doc=EntranceSeamDocument.open(world);
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
        button(top,"1 NEW INTERIOR SEAM",()->editEdge(null,EntranceSeamDocument.Method.EXTRA_SEAM));
        button(top,"2 DE-STITCH TILE EDGE",()->editEdge(null,EntranceSeamDocument.Method.TILE_DESTITCH));
        button(top,"3 DETACHED TAB / PUNCH",()->editTab(null));
        button(top,"EDIT SELECTED",this::editSelected);
        button(top,"DELETE",this::removeSelected);
        button(top,"CONFORM LEAVES + PUNCH TABS",this::conform);
        button(top,"UNDO",()->history(false));button(top,"REDO",()->history(true));
        button(top,"VERIFY SAVE",this::save);
        add(top,BorderLayout.NORTH);add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel foot=new JPanel(new GridLayout(2,1,2,3));
        foot.add(new JLabel("1: new interior seam; 2: existing tile border; 3: punch surface and preserve separate nav tab. Source height PNG unchanged."));
        foot.add(status);add(foot,BorderLayout.SOUTH);
        refresh("Entrance seam editor ready");
    }
    private static void button(JPanel parent,String title,Runnable action){
        JButton b=new JButton(title);b.addActionListener(e->action.run());parent.add(b);
    }
    private static JTextField[] fields(String... defaults){
        var f=new JTextField[defaults.length];
        for(int i=0;i<f.length;i++)f[i]=new JTextField(defaults[i],15);
        return f;
    }
    private static JPanel fieldGrid(JTextField[] f,String... names){
        JPanel p=new JPanel(new GridLayout(0,2,4,4));
        for(int i=0;i<f.length;i++){p.add(new JLabel(names[i]));p.add(f[i]);}
        return p;
    }
    private static double n(JTextField field){
        double d=Double.parseDouble(field.getText().trim());
        if(!Double.isFinite(d))throw new IllegalArgumentException("Nonfinite coordinate");
        return d;
    }
    private static String id(JTextField field){
        String value=field.getText().trim();
        if(!value.matches("[A-Za-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("Invalid 1–48 character ID");
        return value;
    }
    private void mutate(Runnable action){
        var before=doc.snapshot();
        try{
            action.run();
            if(before.equals(doc.snapshot()))return;
            doc.save();
            if(!doc.snapshot().equals(EntranceSeamDocument.open(world).snapshot()))
                throw new IOException("Entrance seam save/readback mismatch");
            undo.push(before);redo.clear();refresh("Authoring saved and verified");
        }catch(IOException|RuntimeException failure){
            doc.restore(before);error("Could not save seam",failure);
        }
    }
    private void editEdge(EntranceSeamDocument.EdgeCut old,EntranceSeamDocument.Method method){
        double border=world.settings().tileCells()*world.settings().metresPerCell();
        var f=fields(old==null?(method==EntranceSeamDocument.Method.EXTRA_SEAM?"cave-cut-1":"tile-destitch-1"):old.id(),
            old==null?"bunker":old.destination(),
            old==null?Double.toString(method==EntranceSeamDocument.Method.EXTRA_SEAM?4:border):Double.toString(old.x0()),
            old==null?"4":Double.toString(old.z0()),
            old==null?Double.toString(method==EntranceSeamDocument.Method.EXTRA_SEAM?4:border):Double.toString(old.x1()),
            old==null?"8":Double.toString(old.z1()));
        if(JOptionPane.showConfirmDialog(frame,fieldGrid(f,
            "Unique seam ID","Underground destination/ramp group","Start X (m)",
            "Start Z (m)","End X (m)","End Z (m)"),
            method==EntranceSeamDocument.Method.EXTRA_SEAM?
                "Method 1 — additional source tile seam":"Method 2 — de-stitch existing tile edge",
            JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            var edge=new EntranceSeamDocument.EdgeCut(id(f[0]),method,id(f[1]),
                n(f[2]),n(f[3]),n(f[4]),n(f[5]));
            mutate(()->{
                if(old!=null)doc.removeEdge(old);
                doc.addEdge(edge);
            });
        }catch(RuntimeException failure){error("Invalid entrance edge",failure);}
    }
    private void editTab(EntranceSeamDocument.DetachedTab old){
        var f=fields(old==null?"cave-tab-1":old.id(),
            old==null?"cave-tab-layer":old.layer(),
            old==null?"bunker":old.destination(),
            old==null?"4":Double.toString(old.x0()),
            old==null?"4":Double.toString(old.z0()),
            old==null?"6":Double.toString(old.x1()),
            old==null?"6":Double.toString(old.z1()));
        if(JOptionPane.showConfirmDialog(frame,fieldGrid(f,
            "Unique tab ID","New detached navigation layer ID","Destination/ramp group",
            "Min X","Min Z","Max X","Max Z"),
            "Method 3 — punch detached navigation tab (needs Method 1 seam)",
            JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            var tab=new EntranceSeamDocument.DetachedTab(id(f[0]),id(f[1]),
                id(f[2]),n(f[3]),n(f[4]),n(f[5]),n(f[6]));
            mutate(()->{
                if(old!=null)doc.removeTab(old);
                doc.addTab(tab);
            });
        }catch(RuntimeException failure){error("Invalid detached tab",failure);}
    }
    private void editSelected(){
        var value=list.getSelectedValue();
        if(value instanceof EntranceSeamDocument.EdgeCut edge)editEdge(edge,edge.method());
        else if(value instanceof EntranceSeamDocument.DetachedTab tab)editTab(tab);
        else status.setText("Select a seam or detached tab");
    }
    private void removeSelected(){
        var value=list.getSelectedValue();
        if(value==null)return;
        if(JOptionPane.showConfirmDialog(frame,"Delete "+value+"?",
            "Confirm entrance topology deletion",JOptionPane.YES_NO_OPTION)!=
                JOptionPane.YES_OPTION)return;
        mutate(()->{
            if(value instanceof EntranceSeamDocument.EdgeCut edge)doc.removeEdge(edge);
            if(value instanceof EntranceSeamDocument.DetachedTab tab)doc.removeTab(tab);
        });
    }
    private void conform(){
        if(JOptionPane.showConfirmDialog(frame,
            "Conform adaptive source leaves to the authored interior seams and PUNCH "+
            "the outer surface underneath detached tabs?\n"+
            "The source height PNG stays intact. This changes persistent adaptive topology.",
            "Apply authored entrance topology",JOptionPane.YES_NO_OPTION)
            !=JOptionPane.YES_OPTION)return;
        try{
            var topology=AdaptiveTerrainTopology.open(world);
            var result=EntranceSeamTopology.apply(world,topology,doc);
            refresh("Applied: "+result.newSplits()+" adaptive splits, "+
                result.newCutouts()+" source punches; disk verified");
        }catch(IOException|RuntimeException failure){error("Could not conform entrance",failure);}
    }
    private void history(boolean forward){
        var input=forward?redo:undo;
        if(input.isEmpty())return;
        var before=doc.snapshot();
        var target=input.peek();
        try{
            doc.restore(target);doc.save();
            if(!doc.snapshot().equals(EntranceSeamDocument.open(world).snapshot()))
                throw new IOException("Entrance undo/redo save readback failed");
            input.pop();(forward?undo:redo).push(before);
            refresh("Entrance authoring history restored; rerun CONFORM if necessary");
        }catch(IOException|RuntimeException problem){doc.restore(before);error("Undo/redo failed",problem);}
    }
    private void save(){
        try{
            doc.save();
            if(!doc.snapshot().equals(EntranceSeamDocument.open(world).snapshot()))
                throw new IOException("Readback differs");
            refresh("Entrance topology document verified on disk");
        }catch(IOException|RuntimeException problem){error("Save failed",problem);}
    }
    private void refresh(String reason){
        rows.clear();
        for(var edge:doc.edges())rows.addElement(edge);
        for(var tab:doc.tabs())rows.addElement(tab);
        status.setText(reason+" | seams "+doc.edges().size()+" | tabs "+doc.tabs().size());
    }
    private void error(String title,Exception problem){
        status.setText(title+": "+problem.getMessage());
        JOptionPane.showMessageDialog(frame,problem.getMessage(),title,
            JOptionPane.ERROR_MESSAGE);
    }
}
