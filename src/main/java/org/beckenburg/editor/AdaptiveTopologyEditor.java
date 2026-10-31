package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Human-first adaptive topology controls over the real stitched source world.
 * Split and merge edit the sparse topology document; heightmap PNGs are untouched.
 * This 2D topology inspector does NOT produce a 3D game preview.
 */
public final class AdaptiveTopologyEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final AdaptiveTerrainTopology topology;
    private final JSpinner tileX = spinner(0,-999999,999999);
    private final JSpinner tileZ = spinner(0,-999999,999999);
    private final JSpinner cellX,cellZ;
    private final JSpinner depth = spinner(0,0,AdaptiveTerrainTopology.MAX_DEPTH - 1);
    private final JSpinner subX = spinner(0,0,(1 << AdaptiveTerrainTopology.MAX_DEPTH)-1);
    private final JSpinner subZ = spinner(0,0,(1 << AdaptiveTerrainTopology.MAX_DEPTH)-1);
    private final ArrayDeque<AdaptiveTerrainTopology.Snapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<AdaptiveTerrainTopology.Snapshot> redo = new ArrayDeque<>();
    private final JLabel status = new JLabel();
    private final JPanel canvas = new CellCanvas();
    private boolean dirty;

    private static JSpinner spinner(int value,int min,int max) {
        return new JSpinner(new SpinnerNumberModel(value,min,max,1));
    }
    private static int n(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
    public static JFrame open(Window owner,StitchedHeightmapWorld world,
                              int selectedTileX,int selectedTileZ) throws IOException {
        JFrame frame = new JFrame("Beckenburg — Adaptive Topology (source authoring)");
        AdaptiveTopologyEditor editor = new AdaptiveTopologyEditor(frame, world,
            selectedTileX, selectedTileZ);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override public void windowClosing(java.awt.event.WindowEvent e){
                editor.close();
            }
        });
        frame.setContentPane(editor);
        frame.setMinimumSize(new Dimension(820,560));
        frame.setSize(1040,680);
        frame.setLocationRelativeTo(owner);
        frame.setVisible(true);
        return frame;
    }
    private AdaptiveTopologyEditor(JFrame frame,StitchedHeightmapWorld world,
                                   int initialTileX,int initialTileZ) throws IOException {
        super(new BorderLayout(6,6));
        this.frame = frame;
        this.world = world;
        this.topology = AdaptiveTerrainTopology.open(world);
        this.cellX = spinner(0,0,world.settings().tileCells()-1);
        this.cellZ = spinner(0,0,world.settings().tileCells()-1);
        this.tileX.setValue(initialTileX);
        this.tileZ.setValue(initialTileZ);
        setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JPanel top = new JPanel(new GridLayout(2,1,2,2));
        JPanel location = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
        addField(location,"Tile X",tileX);
        addField(location,"Tile Z",tileZ);
        addField(location,"Base cell X",cellX);
        addField(location,"Base cell Z",cellZ);
        top.add(location);
        JPanel target = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
        addField(target,"Depth",depth);
        addField(target,"Quadrant X",subX);
        addField(target,"Quadrant Z",subZ);
        button(target,"SPLIT",()->mutate(true));
        button(target,"MERGE",()->mutate(false));
        button(target,"UNDO",this::undo);
        button(target,"REDO",this::redo);
        button(target,"SAVE TOPOLOGY",this::save);
        top.add(target);
        add(top,BorderLayout.NORTH);
        canvas.setPreferredSize(new Dimension(600,450));
        canvas.setBackground(new Color(31,36,42));
        add(canvas,BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2,1,2,3));
        bottom.add(new JLabel("2D authored topology, NOT gameplay. Click a subcell to select it at the chosen depth;"+
            " HEIGHT values still come from the original 16-bit source."));
        bottom.add(status);
        add(bottom,BorderLayout.SOUTH);
        for (JSpinner spinner : new JSpinner[]{tileX,tileZ,cellX,cellZ,depth,subX,subZ})
            spinner.addChangeListener(e->refresh());
        refresh();
    }
    private static void addField(JPanel panel,String label,JSpinner spinner){
        panel.add(new JLabel(label));
        panel.add(spinner);
    }
    private static void button(JPanel panel,String name,Runnable handler) {
        JButton button=new JButton(name);
        button.addActionListener(e->handler.run());
        panel.add(button);
    }
    private long gx(){return (long)n(tileX)*world.settings().tileCells()+n(cellX);}
    private long gz(){return (long)n(tileZ)*world.settings().tileCells()+n(cellZ);}
    private AdaptiveTerrainTopology.Node selected() {
        return new AdaptiveTerrainTopology.Node(gx(),gz(),n(depth),n(subX),n(subZ));
    }
    private void mutate(boolean split){
        try{
            var before=topology.snapshot();
            if(split)topology.split(selected());
            else topology.merge(selected());
            undo.push(before);
            if(undo.size()>64)undo.removeLast();
            redo.clear();
            dirty=true;
            refresh();
        }catch(RuntimeException ex){error("Refinement rejected",ex);}
    }
    private void undo(){
        if(undo.isEmpty())return;
        var next=undo.pop();
        var before=topology.snapshot();
        try{
            topology.restore(next);
            redo.push(before);
            dirty=true;
            refresh();
        }catch(RuntimeException ex){
            undo.push(next);
            error("Undo failed",ex);
        }
    }
    private void redo(){
        if(redo.isEmpty())return;
        var next=redo.pop();
        var before=topology.snapshot();
        try{
            topology.restore(next);
            undo.push(before);
            dirty=true;
            refresh();
        }catch(RuntimeException ex){
            redo.push(next);
            error("Redo failed",ex);
        }
    }
    private boolean save(){
        try{
            topology.save();
            if(!AdaptiveTerrainTopology.open(world).splitNodes()
                    .equals(topology.splitNodes()))
                throw new IOException("Saved topology differs from immediate disk readback");
            dirty=false;
            refresh();
            return true;
        }catch(IOException|RuntimeException ex){
            error("Topology save/readback failed",ex);
            return false;
        }
    }
    private void close(){
        if(dirty){
            int option=JOptionPane.showConfirmDialog(frame,
                "Save adaptive topology edits before closing?",
                "Unsaved adaptive topology",JOptionPane.YES_NO_CANCEL_OPTION);
            if(option==JOptionPane.CANCEL_OPTION||option==JOptionPane.CLOSED_OPTION)return;
            if(option==JOptionPane.YES_OPTION&&!save())return;
        }
        frame.dispose();
    }
    private void error(String title,Exception failure){
        status.setText(title+": "+failure.getMessage());
        JOptionPane.showMessageDialog(frame,failure.getMessage(),title,
            JOptionPane.ERROR_MESSAGE);
    }
    private void refresh(){
        int leaves=0;
        String issue="";
        try{
            leaves=topology.leaves(gx(),gz()).size();
            // Inspect the actual requested edge split positions, when this is a leaf.
            var node=selected();
            var west=topology.edgeBreaks(node,AdaptiveTerrainTopology.Edge.WEST);
            issue=" | west edge breaks "+west.size();
        }catch(RuntimeException unavailable){
            issue=" | selected subcell is split, invalid or absent";
        }
        status.setText("Global base cell "+gx()+","+gz()+
            " | "+leaves+" leaves | "+topology.splitCount()+" split nodes"+
            " | revision "+topology.revision()+(dirty?" | UNSAVED":" | SAVED / UNMODIFIED")+issue);
        canvas.repaint();
    }
    private final class CellCanvas extends JPanel {
        CellCanvas(){
            addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent event){
                    int length=Math.max(1,Math.min(getWidth()-32,getHeight()-32));
                    int x0=(getWidth()-length)/2,z0=(getHeight()-length)/2;
                    if(event.getX()<x0||event.getX()>=x0+length
                       ||event.getY()<z0||event.getY()>=z0+length)return;
                    int cells=1<<n(depth);
                    int sx=Math.min(cells-1,(event.getX()-x0)*cells/length);
                    int sz=Math.min(cells-1,(event.getY()-z0)*cells/length);
                    subX.setValue(sx);
                    subZ.setValue(sz);
                }
            });
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D out=(Graphics2D)g.create();
            int length=Math.max(1,Math.min(getWidth()-32,getHeight()-32));
            int x0=(getWidth()-length)/2,z0=(getHeight()-length)/2;
            try{
                for(var leaf:topology.leaves(gx(),gz())){
                    int unit=1<<leaf.depth();
                    int x=x0+leaf.localX()*length/unit;
                    int z=z0+leaf.localZ()*length/unit;
                    int w=Math.max(1, (leaf.localX()+1)*length/unit
                                      -leaf.localX()*length/unit);
                    int h=Math.max(1, (leaf.localZ()+1)*length/unit
                                      -leaf.localZ()*length/unit);
                    int shade=65+Math.min(125,leaf.depth()*26);
                    out.setColor(new Color(shade,shade,shade));
                    out.fillRect(x,z,w,h);
                    out.setColor(new Color(20,30,42));
                    out.drawRect(x,z,w,h);
                }
                int units=1<<n(depth);
                int selectionX=x0+n(subX)*length/units;
                int selectionZ=z0+n(subZ)*length/units;
                int selectionWidth=Math.max(1,length/units);
                out.setColor(Color.ORANGE);
                out.drawRect(selectionX,selectionZ,selectionWidth,selectionWidth);
                out.drawRect(selectionX+1,selectionZ+1,
                    Math.max(1,selectionWidth-2),Math.max(1,selectionWidth-2));
            }catch(RuntimeException missingTile){
                out.setColor(Color.WHITE);
                out.drawString("No source tile or invalid cell selected",x0+10,z0+30);
            }
            out.setColor(Color.WHITE);
            out.drawString("AUTHORING SUBDIVISIONS — NOT 3D TERRAIN",10,16);
            out.dispose();
        }
    }
}
