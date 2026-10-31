package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayDeque;
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

/** World-space authoring for a physical C1-continuous cave/bunker entrance.
 * The generated height profile is shared by physical queries and mesh triangles.
 * No opening is silently carved in the source heightmap. */
public final class RampEntranceEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final RampEntranceDocument document;
    private final JList<LayeredTerrainQueries.Ramp> list=new JList<>();
    private final JLabel status=new JLabel();
    private final ArrayDeque<RampEntranceDocument.Snapshot> undo=new ArrayDeque<>();
    private final ArrayDeque<RampEntranceDocument.Snapshot> redo=new ArrayDeque<>();
    private final ArrayDeque<AdaptiveTerrainTopology.Snapshot> cutUndo=new ArrayDeque<>();

    public static JFrame open(Window owner,StitchedHeightmapWorld world)throws IOException{
        JFrame f=new JFrame("Beckenburg — Smooth Surface Entrances");
        f.setContentPane(new RampEntranceEditor(f,world));
        f.setMinimumSize(new Dimension(770,460));f.setSize(1030,630);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setLocationRelativeTo(owner);f.setVisible(true);return f;
    }
    private RampEntranceEditor(JFrame frame,StitchedHeightmapWorld world)throws IOException{
        super(new BorderLayout(5,5));
        this.frame=frame;this.world=world;this.document=RampEntranceDocument.open(world);
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,5,3));
        button(buttons,"ADD RAMP",()->edit(null));
        button(buttons,"ENTRANCE SEAMS 1/2/3",()->{
            try{EntranceSeamEditor.open(frame,world);}
            catch(IOException|RuntimeException failure){error("Entrance seam editor failed",failure);}
        });
        button(buttons,"EDIT",()->{
            var item=list.getSelectedValue();
            if(item!=null)edit(item);else status.setText("Select a ramp");
        });
        button(buttons,"DELETE",this::remove);
        button(buttons,"AUDIT ENTRANCE",this::audit);
        button(buttons,"CARVE OPENING",this::carve);
        button(buttons,"UNDO CARVE",this::undoCarve);
        button(buttons,"UNDO",()->history(false));
        button(buttons,"REDO",()->history(true));
        button(buttons,"VERIFY SAVE",this::save);
        add(buttons,BorderLayout.NORTH);add(new JScrollPane(list),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new GridLayout(2,1,3,2));
        bottom.add(new JLabel("Smooth grade matches authored end tangents. Use topology CUT HOLE for descending underground headroom."));
        bottom.add(status);add(bottom,BorderLayout.SOUTH);
        refresh("Ramp entrance editor ready");
    }
    private static void button(JPanel into,String name,Runnable action){
        JButton b=new JButton(name);b.addActionListener(e->action.run());into.add(b);
    }
    private static JTextField[] fields(String... values){
        var result=new JTextField[values.length];
        for(int i=0;i<values.length;i++)result[i]=new JTextField(values[i],15);
        return result;
    }
    private static double number(JTextField f){
        double d=Double.parseDouble(f.getText().trim());
        if(!Double.isFinite(d))throw new IllegalArgumentException("Non-finite coordinate");
        return d;
    }
    private static String id(JTextField f){
        String s=f.getText().trim();
        if(!s.matches("[A-Za-z0-9_-]{1,48}"))
            throw new IllegalArgumentException("ID must use 1..48 letters/digits/-/_");
        return s;
    }
    private void edit(LayeredTerrainQueries.Ramp original){
        var fields=fields(
            original==null?"entrance-1":original.id(),
            original==null?"entrance-ramp-1":original.layer(),
            original==null?"bunker":original.targetLayer(),
            original==null?"EAST":original.direction().name(),
            original==null?"1":String.valueOf(original.x0()),
            original==null?"4":String.valueOf(original.z0()),
            original==null?"9":String.valueOf(original.x1()),
            original==null?"6":String.valueOf(original.z1()),
            original==null?"0":String.valueOf(original.mouthY()),
            original==null?"-2":String.valueOf(original.interiorY()),
            original==null?"0":String.valueOf(original.mouthGrade()),
            original==null?"0":String.valueOf(original.interiorGrade()));
        String[] labels={"Unique entrance ID","Unique ramp layer ID","Interior destination layer",
            "Heading (EAST/WEST/NORTH/SOUTH)","West X (m)","North Z (m)",
            "East X (m)","South Z (m)","Surface lip elevation (m)",
            "Interior end elevation (m)","Mouth tangent grade (rise/metre)",
            "Interior tangent grade (rise/metre)"};
        JPanel grid=new JPanel(new GridLayout(0,2,4,4));
        for(int i=0;i<labels.length;i++){
            grid.add(new JLabel(labels[i]));grid.add(fields[i]);
        }
        JButton sample=new JButton("SAMPLE SURFACE HEIGHT AT MOUTH");
        sample.addActionListener(e->{
            try{
                var direction=LayeredTerrainQueries.Ramp.Direction.valueOf(
                    fields[3].getText().trim().toUpperCase());
                double x0=number(fields[4]),z0=number(fields[5]);
                double x1=number(fields[6]),z1=number(fields[7]);
                double x=direction==LayeredTerrainQueries.Ramp.Direction.EAST?x0:
                    direction==LayeredTerrainQueries.Ramp.Direction.WEST?x1:(x0+x1)*.5;
                double z=direction==LayeredTerrainQueries.Ramp.Direction.SOUTH?z0:
                    direction==LayeredTerrainQueries.Ramp.Direction.NORTH?z1:(z0+z1)*.5;
                fields[8].setText(Double.toString(world.height(x,z)));
            }catch(IOException|RuntimeException failure){error("Mouth sample failed",failure);}
        });
        JPanel form=new JPanel(new BorderLayout(3,4));
        form.add(new JScrollPane(grid),BorderLayout.CENTER);
        form.add(sample,BorderLayout.SOUTH);
        if(JOptionPane.showConfirmDialog(frame,form,"Author continuous ramp entrance",
                JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{
            var ramp=new LayeredTerrainQueries.Ramp(
                id(fields[0]),id(fields[1]),id(fields[2]),
                LayeredTerrainQueries.Ramp.Direction.valueOf(
                    fields[3].getText().trim().toUpperCase()),
                number(fields[4]),number(fields[5]),number(fields[6]),number(fields[7]),
                number(fields[8]),number(fields[9]),number(fields[10]),number(fields[11]));
            var before=document.snapshot();
            try{
                if(original!=null)document.remove(original);
                document.add(ramp);
                document.save();
                if(!document.snapshot().equals(RampEntranceDocument.open(world).snapshot()))
                    throw new IOException("Saved ramp differs from disk readback");
                undo.push(before);redo.clear();
            }catch(IOException|RuntimeException failure){
                document.restore(before);
                throw failure;
            }
            refresh("Ramp saved; use AUDIT ENTRANCE to check source continuity and clearance");
        }catch(IOException|RuntimeException failure){error("Ramp authoring failed",failure);}
    }
    private void remove(){
        var selected=list.getSelectedValue();
        if(selected==null)return;
        if(JOptionPane.showConfirmDialog(frame,"Remove "+selected.id()+"?",
                "Delete smooth entrance",JOptionPane.YES_NO_OPTION)
            !=JOptionPane.YES_OPTION)return;
        var before=document.snapshot();
        try{
            document.remove(selected);document.save();
            if(!document.snapshot().equals(RampEntranceDocument.open(world).snapshot()))
                throw new IOException("Delete failed disk readback");
            undo.push(before);redo.clear();refresh("Ramp removed");
        }catch(IOException|RuntimeException failure){
            document.restore(before);error("Ramp deletion failed",failure);
        }
    }
    private void history(boolean isRedo){
        var from=isRedo?redo:undo;
        if(from.isEmpty())return;
        var target=from.peek();
        var before=document.snapshot();
        try{
            document.restore(target);document.save();
            if(!document.snapshot().equals(RampEntranceDocument.open(world).snapshot()))
                throw new IOException("Undo/redo failed disk readback");
            from.pop();(isRedo?undo:redo).push(before);refresh("Ramp history restored");
        }catch(IOException|RuntimeException failure){
            document.restore(before);error("Ramp history failed",failure);
        }
    }
    private void carve(){
        var ramp=list.getSelectedValue();
        if(ramp==null){status.setText("Select a ramp entrance to carve");return;}
        if(JOptionPane.showConfirmDialog(frame,
            "Carve adaptive terrain only where source ground obstructs "+ramp.id()
            +"? The heightmap PNG will remain unchanged. The mouth lip and deep roof are preserved.",
            "Confirm source topology cutouts",JOptionPane.YES_NO_OPTION)
            !=JOptionPane.YES_OPTION)return;
        try{
            var topology=AdaptiveTerrainTopology.open(world);
            var before=topology.snapshot();
            var result=RampApertureCarver.carve(world,topology,ramp,
                new LayeredTerrainQueries.Agent(.1,1.65,42,.24));
            if(result.changed()){
                topology.save();
                if(!topology.snapshot().equals(AdaptiveTerrainTopology.open(world).snapshot()))
                    throw new IOException("Carved terrain did not persist to disk");
                cutUndo.push(before);
            }
            refresh("Ramp aperture: "+result.newCutouts()+" new removed terrain patches, "
                +result.newSplits()+" refinements");
        }catch(IOException|RuntimeException failure){error("Ramp aperture carve failed",failure);}
    }
    private void undoCarve(){
        if(cutUndo.isEmpty())return;
        var before=cutUndo.peek();
        try{
            var topology=AdaptiveTerrainTopology.open(world);
            topology.restore(before);topology.save();
            if(!topology.snapshot().equals(AdaptiveTerrainTopology.open(world).snapshot()))
                throw new IOException("Cutout undo failed disk readback");
            cutUndo.pop();refresh("Restored topology before last ramp carving");
        }catch(IOException|RuntimeException failure){error("Cutout undo failed",failure);}
    }
    private void save(){
        try{
            document.save();
            if(!document.snapshot().equals(RampEntranceDocument.open(world).snapshot()))
                throw new IOException("Ramp save readback mismatch");
            refresh("Ramp authoring verified on disk");
        }catch(IOException|RuntimeException failure){error("Ramp verification failed",failure);}
    }
    private void audit(){
        var r=list.getSelectedValue();
        if(r==null){status.setText("Select a ramp for physical verification");return;}
        try{
            var problems=RampEntranceDocument.audit(world,
                AdaptiveTerrainTopology.open(world),LayeredTerrainDocument.open(world),
                r,new LayeredTerrainQueries.Agent(.1,1.65,42,.24));
            status.setText(problems.isEmpty()?"PASS: mouth, continuous slope, exit and clearance":
                "OPENING/GEOMETRY ISSUE: "+String.join("; ",problems));
        }catch(IOException|RuntimeException failure){error("Ramp audit failed",failure);}
    }
    private void refresh(String message){
        var values=new javax.swing.DefaultListModel<LayeredTerrainQueries.Ramp>();
        for(var r:document.ramps())values.addElement(r);
        list.setModel(values);
        status.setText(message+" | entrances "+document.ramps().size()
            +" | revision "+document.revision());
    }
    private void error(String title,Exception cause){
        status.setText(title+": "+cause.getMessage());
        JOptionPane.showMessageDialog(frame,cause.getMessage(),title,
            JOptionPane.ERROR_MESSAGE);
    }
}
