package org.beckenburg.editor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.DefaultListModel;
import java.awt.event.ActionEvent;

/**
 * Human-first terrain/zone authoring UI. Draws an explicitly labeled 2D
 * height-delta editor, NOT a fabricated live 3D terrain screenshot.
 */
public final class TerrainZoneEditor extends JPanel {
    private enum Tool { RAISE, LOWER, SMOOTH, FLATTEN, DRAW_ZONE, SELECT_ZONE }
    private final JFrame frame;
    private final JComboBox<String> maps;
    private final JComboBox<Tool> tool=new JComboBox<>(Tool.values());
    private final JComboBox<String> destination;
    private final JComboBox<TerrainZoneDocument.Trigger> trigger=
        new JComboBox<>(TerrainZoneDocument.Trigger.values());
    private final JSpinner radius=spin(4,.25,64,.25);
    private final JSpinner strength=spin(.12,.01,10,.05);
    private final JSpinner flat=spin(0,-20,20,.1);
    private final JSpinner minY=spin(-1,-1000,1000,.5);
    private final JSpinner maxY=spin(3,-1000,1000,.5);
    private final JSpinner spawnX=spin(0,-10000,10000,.5);
    private final JSpinner spawnY=spin(0,-10000,10000,.5);
    private final JSpinner spawnZ=spin(0,-10000,10000,.5);
    private final JSpinner spawnYaw=spin(0,-360,360,5);
    private final JLabel status=new JLabel("Terrain + loading-zone editor");
    private final DefaultListModel<TerrainZoneDocument.Zone> zoneRows=new DefaultListModel<>();
    private final JList<TerrainZoneDocument.Zone> zoneList=new JList<>(zoneRows);
    private final ArrayDeque<TerrainZoneDocument.Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final MapCanvas canvas=new MapCanvas();
    private TerrainZoneDocument doc;
    private String currentLevel;
    private boolean dirty,switching;
    private TerrainZoneDocument.Snapshot strokeBefore;
    private long strokeRevision;
    private double dragX,dragZ,endX,endZ;
    private String selectedZone;

    private static JSpinner spin(double initial,double low,double high,double step){
        return new JSpinner(new SpinnerNumberModel(initial,low,high,step));
    }
    private static double value(JSpinner item){return ((Number)item.getValue()).doubleValue();}
    private static TerrainZoneDocument.Grid gridFor(String id){
        // Blackpine's actual SiteTerrain is 131 x 158 vertices, spaced 0.5 m.
        if("blackpine".equals(id))return new TerrainZoneDocument.Grid(3,1,.5,131,158);
        return new TerrainZoneDocument.Grid(0,0,1,129,129);
    }
    public static JFrame open(Window owner,String initial,List<String> mapIds){
        if(mapIds==null||mapIds.isEmpty())throw new IllegalArgumentException("Editor map roster required");
        JFrame frame=new JFrame("Beckenburg — Terrain & Loading Zones");
        TerrainZoneEditor editor=new TerrainZoneEditor(frame,mapIds,initial);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setContentPane(editor);
        frame.setMinimumSize(new Dimension(860,620));
        frame.setSize(1200,780);
        frame.setLocationRelativeTo(owner);
        frame.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override public void windowClosing(java.awt.event.WindowEvent e){editor.close();}
        });
        frame.setVisible(true);
        return frame;
    }
    private TerrainZoneEditor(JFrame frame,List<String> mapIds,String initial){
        super(new BorderLayout(5,5));
        this.frame=frame;
        setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        maps=new JComboBox<>(mapIds.toArray(String[]::new));
        destination=new JComboBox<>(mapIds.toArray(String[]::new));
        JPanel header=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
        header.add(new JLabel("MAP"));header.add(maps);
        header.add(new JLabel("TOOL"));header.add(tool);
        header.add(new JLabel("Brush radius (m)"));header.add(radius);
        header.add(new JLabel("Amount (m)"));header.add(strength);
        header.add(new JLabel("Flatten delta (m)"));header.add(flat);
        JButton undoButton=new JButton("UNDO"),redoButton=new JButton("REDO"),save=new JButton("SAVE");
        undoButton.addActionListener(e->undo());
        redoButton.addActionListener(e->redo());
        save.addActionListener(e->save());
        header.add(undoButton);header.add(redoButton);header.add(save);
        add(header,BorderLayout.NORTH);
        add(canvas,BorderLayout.CENTER);
        JPanel side=new JPanel(new BorderLayout(5,4));
        side.setPreferredSize(new Dimension(320,500));
        JPanel fields=new JPanel(new java.awt.GridLayout(0,2,5,5));
        fields.add(new JLabel("Destination map"));fields.add(destination);
        fields.add(new JLabel("Trigger"));fields.add(trigger);
        fields.add(new JLabel("Zone min feet Y"));fields.add(minY);
        fields.add(new JLabel("Zone max feet Y"));fields.add(maxY);
        fields.add(new JLabel("Destination X"));fields.add(spawnX);
        fields.add(new JLabel("Destination feet Y"));fields.add(spawnY);
        fields.add(new JLabel("Destination Z"));fields.add(spawnZ);
        fields.add(new JLabel("Destination yaw degrees"));fields.add(spawnYaw);
        side.add(fields,BorderLayout.NORTH);
        JPanel list=new JPanel(new BorderLayout(4,4));
        list.add(new JLabel("Authored loading zones (select to inspect/edit)"),BorderLayout.NORTH);
        zoneList.setCellRenderer((jlist,value,index,selected,focus)->{
            JLabel label=new JLabel(value.id()+"  ->  "+value.destination()+"  ["+value.trigger()+"]");
            label.setOpaque(true);
            label.setBackground(selected?jlist.getSelectionBackground():jlist.getBackground());
            label.setForeground(selected?jlist.getSelectionForeground():jlist.getForeground());
            return label;
        });
        zoneList.addListSelectionListener(e->{
            if(e.getValueIsAdjusting())return;
            TerrainZoneDocument.Zone zone=zoneList.getSelectedValue();
            if(zone!=null){selectedZone=zone.id();editFields(zone);canvas.repaint();}
        });
        list.add(new JScrollPane(zoneList),BorderLayout.CENTER);
        JButton modify=new JButton("UPDATE SELECTED"),remove=new JButton("DELETE SELECTED");
        modify.addActionListener(e->modify());
        remove.addActionListener(e->remove());
        JPanel actions=new JPanel(new java.awt.GridLayout(1,2,4,0));
        actions.add(modify);actions.add(remove);list.add(actions,BorderLayout.SOUTH);
        side.add(list,BorderLayout.CENTER);
        JTextArea info=new JTextArea(
            "RAISE / LOWER / SMOOTH / FLATTEN: drag on the height-delta grid.\n"+
            "DRAW_ZONE: drag out a 3D loading volume using Y bounds at right.\n"+
            "SELECT_ZONE: click a zone; edit its destination or elevation.\n\n"+
            "The terrain canvas shows authored height OFFSETS, not a game render.\n"+
            "Blackpine offsets are consumed by SiteTerrain on map reload.\n"+
            "Loading-zone triggers are authored and queryable; automatic mission "+
            "handoff is deliberately NOT enabled in existing gameplay.");
        info.setLineWrap(true);info.setWrapStyleWord(true);info.setEditable(false);
        info.setRows(8);info.setBackground(getBackground());
        side.add(info,BorderLayout.SOUTH);
        add(side,BorderLayout.EAST);add(status,BorderLayout.SOUTH);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("control S"),"save");
        getActionMap().put("save",new javax.swing.AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){save();}
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("control Z"),"undo");
        getActionMap().put("undo",new javax.swing.AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){undo();}
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("control Y"),"redo");
        getActionMap().put("redo",new javax.swing.AbstractAction(){
            @Override public void actionPerformed(ActionEvent e){redo();}
        });
        maps.addActionListener(e->{
            if(!switching)changeMap((String)maps.getSelectedItem());
        });
        String first=mapIds.contains(initial)?initial:mapIds.get(0);
        switching=true;maps.setSelectedItem(first);switching=false;
        if(!load(first))throw new IllegalStateException("Cannot open terrain document "+first);
    }
    private void editFields(TerrainZoneDocument.Zone zone){
        destination.setSelectedItem(zone.destination());
        trigger.setSelectedItem(zone.trigger());
        minY.setValue(zone.y0());maxY.setValue(zone.y1());
        spawnX.setValue(zone.spawnX());spawnY.setValue(zone.spawnY());
        spawnZ.setValue(zone.spawnZ());spawnYaw.setValue(Math.toDegrees(zone.spawnYaw()));
    }
    private boolean load(String level){
        try{
            TerrainZoneDocument read=TerrainZoneDocument.load(
                TerrainZoneDocument.defaultPath(level),level,gridFor(level));
            doc=read;currentLevel=level;dirty=false;undo.clear();redo.clear();
            selectedZone=null;refresh();
            return true;
        }catch(IOException|IllegalArgumentException failure){
            JOptionPane.showMessageDialog(frame,failure.getMessage(),
                "Cannot load terrain/zone document",JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    private boolean save(){
        try{
            doc.save(TerrainZoneDocument.defaultPath(currentLevel));
            dirty=false;refresh();return true;
        }catch(IOException failure){
            JOptionPane.showMessageDialog(frame,failure.getMessage(),
                "Terrain/zone save failed",JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    private boolean mayDiscard(){
        if(!dirty)return true;
        int answer=JOptionPane.showConfirmDialog(frame,
            "Save terrain and loading-zone edits before leaving this map?",
            "Unsaved module edits",JOptionPane.YES_NO_CANCEL_OPTION);
        if(answer==JOptionPane.CANCEL_OPTION||answer==JOptionPane.CLOSED_OPTION)return false;
        return answer!=JOptionPane.YES_OPTION||save();
    }
    private void changeMap(String next){
        if(next==null||next.equals(currentLevel))return;
        if(!mayDiscard()||!load(next)){
            switching=true;maps.setSelectedItem(currentLevel);switching=false;
        }
    }
    private void close(){if(mayDiscard())frame.dispose();}
    private void record(TerrainZoneDocument.Snapshot before){
        undo.push(before);if(undo.size()>60)undo.removeLast();
        redo.clear();dirty=true;refresh();
    }
    private void undo(){
        if(undo.isEmpty())return;
        redo.push(doc.snapshot());doc.restore(undo.pop());dirty=true;refresh();
    }
    private void redo(){
        if(redo.isEmpty())return;
        undo.push(doc.snapshot());doc.restore(redo.pop());dirty=true;refresh();
    }
    private void modify(){
        if(selectedZone==null)return;
        TerrainZoneDocument.Zone old=doc.zones().stream()
            .filter(z->z.id().equals(selectedZone)).findFirst().orElse(null);
        if(old==null)return;
        try{
            TerrainZoneDocument.Zone next=new TerrainZoneDocument.Zone(
                old.id(),old.x0(),value(minY),old.z0(),old.x1(),value(maxY),old.z1(),
                (String)destination.getSelectedItem(),value(spawnX),value(spawnY),
                value(spawnZ),Math.toRadians(value(spawnYaw)),
                (TerrainZoneDocument.Trigger)trigger.getSelectedItem());
            TerrainZoneDocument.Snapshot before=doc.snapshot();
            doc.putZone(next);record(before);
        }catch(RuntimeException failure){showInvalid(failure);}
    }
    private void remove(){
        if(selectedZone==null)return;
        TerrainZoneDocument.Snapshot before=doc.snapshot();
        if(doc.removeZone(selectedZone)){selectedZone=null;record(before);}
    }
    private void refresh(){
        if(doc==null)return;
        zoneRows.clear();
        for(var zone:doc.zones())zoneRows.addElement(zone);
        if(selectedZone!=null)for(int i=0;i<zoneRows.size();i++)
            if(selectedZone.equals(zoneRows.get(i).id())){zoneList.setSelectedIndex(i);break;}
        status.setText(currentLevel+"  •  "+doc.grid().columns()+" × "+doc.grid().rows()+
            " height-delta vertices  •  "+doc.zones().size()+" loading zones  •  "+
            (dirty?"UNSAVED":"SAVED / UNMODIFIED")+"  •  "+
            ("blackpine".equals(currentLevel)?"Blackpine runtime terrain adapter":"authoring data only"));
        canvas.repaint();
    }
    private void showInvalid(RuntimeException failure){
        JOptionPane.showMessageDialog(frame,failure.getMessage(),
            "Invalid terrain/zone edit",JOptionPane.ERROR_MESSAGE);
    }
    private final class MapCanvas extends JPanel {
        MapCanvas(){
            setBackground(new Color(30,35,39));
            setPreferredSize(new Dimension(600,600));
            MouseAdapter input=new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){
                    if(!javax.swing.SwingUtilities.isLeftMouseButton(e))return;
                    double[] where=world(e.getX(),e.getY());
                    dragX=endX=where[0];dragZ=endZ=where[1];
                    if(tool.getSelectedItem()==Tool.SELECT_ZONE){
                        selectedZone=null;
                        List<TerrainZoneDocument.Zone> zones=doc.zones();
                        for(int i=zones.size()-1;i>=0;i--){
                            var zone=zones.get(i);
                            if(where[0]>=zone.x0()&&where[0]<=zone.x1()
                                &&where[1]>=zone.z0()&&where[1]<=zone.z1()){
                                selectedZone=zone.id();editFields(zone);break;
                            }
                        }
                        refresh();return;
                    }
                    strokeBefore=doc.snapshot();strokeRevision=doc.revision();
                    apply(e);
                }
                @Override public void mouseDragged(MouseEvent e){
                    if(strokeBefore!=null)apply(e);
                }
                @Override public void mouseReleased(MouseEvent e){
                    if(strokeBefore==null)return;
                    apply(e);
                    if(tool.getSelectedItem()==Tool.DRAW_ZONE){
                        double x0=Math.min(dragX,endX),x1=Math.max(dragX,endX);
                        double z0=Math.min(dragZ,endZ),z1=Math.max(dragZ,endZ);
                        if(x1-x0>=.25&&z1-z0>=.25)try{
                            String id="zone"+Long.toUnsignedString(System.nanoTime(),36);
                            var zone=new TerrainZoneDocument.Zone(id,x0,value(minY),z0,
                                x1,value(maxY),z1,(String)destination.getSelectedItem(),
                                value(spawnX),value(spawnY),value(spawnZ),
                                Math.toRadians(value(spawnYaw)),
                                (TerrainZoneDocument.Trigger)trigger.getSelectedItem());
                            doc.putZone(zone);selectedZone=id;
                        }catch(RuntimeException failure){showInvalid(failure);}
                    }
                    if(doc.revision()!=strokeRevision)record(strokeBefore);
                    strokeBefore=null;repaint();
                }
            };
            addMouseListener(input);addMouseMotionListener(input);
        }
        private double scale(){
            TerrainZoneDocument.Grid grid=doc.grid();
            return Math.max(.01,Math.min((getWidth()-32)/(grid.x1()-grid.x0()),
                                       (getHeight()-32)/(grid.z1()-grid.z0())));
        }
        private int left(){return (int)((getWidth()-(doc.grid().x1()-doc.grid().x0())*scale())/2);}
        private int top(){return (int)((getHeight()-(doc.grid().z1()-doc.grid().z0())*scale())/2);}
        private double[] world(int x,int y){
            var g=doc.grid();
            return new double[]{Math.max(g.x0(),Math.min(g.x1(),g.x0()+(x-left())/scale())),
                                Math.max(g.z0(),Math.min(g.z1(),g.z0()+(y-top())/scale()))};
        }
        private void apply(MouseEvent e){
            double[] at=world(e.getX(),e.getY());
            endX=at[0];endZ=at[1];
            Tool active=(Tool)tool.getSelectedItem();
            if(active!=Tool.DRAW_ZONE&&active!=Tool.SELECT_ZONE)try{
                doc.brush(TerrainZoneDocument.Brush.valueOf(active.name()),
                    endX,endZ,value(radius),value(strength),value(flat));
            }catch(RuntimeException failure){showInvalid(failure);}
            repaint();
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(doc==null)return;
            Graphics2D graphics=(Graphics2D)g.create();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            TerrainZoneDocument.Grid grid=doc.grid();
            double s=scale();int ox=left(),oy=top();
            for(int iz=0;iz<grid.rows()-1;iz++)for(int ix=0;ix<grid.columns()-1;ix++){
                double delta=doc.delta(ix,iz);
                int tone=(int)Math.max(0,Math.min(255,120+delta*15));
                graphics.setColor(new Color(tone,tone,Math.max(0,Math.min(255,tone+10))));
                int px=(int)Math.round(ox+ix*grid.step()*s);
                int pz=(int)Math.round(oy+iz*grid.step()*s);
                int pw=Math.max(1,(int)Math.ceil(grid.step()*s)+1);
                graphics.fillRect(px,pz,pw,pw);
            }
            graphics.setStroke(new BasicStroke(2));
            for(var zone:doc.zones()){
                graphics.setColor(zone.id().equals(selectedZone)?new Color(255,188,81):new Color(98,230,170));
                int px=(int)(ox+(zone.x0()-grid.x0())*s),py=(int)(oy+(zone.z0()-grid.z0())*s);
                int pw=(int)Math.max(1,(zone.x1()-zone.x0())*s);
                int ph=(int)Math.max(1,(zone.z1()-zone.z0())*s);
                graphics.drawRect(px,py,pw,ph);
                graphics.drawString(zone.id(),px+4,py+14);
            }
            if(strokeBefore!=null&&tool.getSelectedItem()==Tool.DRAW_ZONE){
                graphics.setColor(Color.WHITE);
                int px=(int)(ox+(Math.min(dragX,endX)-grid.x0())*s);
                int py=(int)(oy+(Math.min(dragZ,endZ)-grid.z0())*s);
                graphics.drawRect(px,py,(int)(Math.abs(endX-dragX)*s),
                                            (int)(Math.abs(endZ-dragZ)*s));
            }
            graphics.setColor(Color.WHITE);
            graphics.drawString("2D HEIGHT OFFSETS (metres) / zone boundaries — NOT a 3D gameplay capture",
                12,Math.max(15,oy-7));
            graphics.dispose();
        }
    }
}
