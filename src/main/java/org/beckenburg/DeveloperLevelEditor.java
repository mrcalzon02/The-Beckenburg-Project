package org.beckenburg;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Actual Java 17, game-rendered 3D editor, not an in-game placeholder.
 * The selected object inspector writes live transforms into CastleLevelEdits.
 */
public final class DeveloperLevelEditor extends JPanel {
    private record Row(String kind,String id,String description){
        @Override public String toString(){return description;}
    }
    private final CastleLevelEdits data=CastleLevelEdits.current();
    private final CastleHubMap map=new CastleHubMap();
    private final SceneRenderer renderer=new SceneRenderer();
    private final JPanel viewport=new JPanel(){
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            GraphicsOptions.current().draw((Graphics2D)g,renderer.image(),getWidth(),getHeight());
            g.setColor(new Color(244,226,177));g.drawLine(getWidth()/2-5,getHeight()/2,getWidth()/2+5,getHeight()/2);
            g.drawLine(getWidth()/2,getHeight()/2-5,getWidth()/2,getHeight()/2+5);
        }
    };
    private final DefaultListModel<Row> rows=new DefaultListModel<>();
    private final JList<Row> hierarchy=new JList<>(rows);
    private final JTextField filter=new JTextField(15);
    private final JComboBox<String> library=new JComboBox<>();
    private final JSpinner x=new JSpinner(new SpinnerNumberModel(13.5,-1000.,1000.,.1));
    private final JSpinner y=new JSpinner(new SpinnerNumberModel(0.,-10.,100.,.1));
    private final JSpinner z=new JSpinner(new SpinnerNumberModel(18.,-1000.,1000.,.1));
    private final JSpinner yaw=new JSpinner(new SpinnerNumberModel(0.,-360.,360.,5.));
    private final JSpinner scale=new JSpinner(new SpinnerNumberModel(1.,.01,10.,.05));
    private final JSpinner width=new JSpinner(new SpinnerNumberModel(.4,0.,10.,.05));
    private final JSpinner depth=new JSpinner(new SpinnerNumberModel(.4,0.,10.,.05));
    private final JSpinner radius=new JSpinner(new SpinnerNumberModel(9.,1.,24.,.5));
    private final JSpinner power=new JSpinner(new SpinnerNumberModel(2.5,0.,10.,.1));
    private final JComboBox<String> material=new JComboBox<>(new String[]{
        "STONE","PLASTER","PARQUET","CERAMIC","DOOR","CEILING","CARPET","BRICK",
        "WALLPAPER","CONCRETE","SOIL","IRON","FOLIAGE","AGED_WOOD","WINDOW","RITUAL"
    });
    private final JSpinner tileX=new JSpinner(new SpinnerNumberModel(13,0,CastleHubMap.WIDTH-1,1));
    private final JSpinner tileZ=new JSpinner(new SpinnerNumberModel(19,0,CastleHubMap.HEIGHT-1,1));
    private final JSpinner wallIndex=new JSpinner(new SpinnerNumberModel(0,0,CastleHubArchitecturalPlan.walls().size()-1,1));
    private final JCheckBox navOverlay=new JCheckBox("3D navigation overlay",false);
    private final JCheckBox navBlocked=new JCheckBox("Block player navigation here");
    private final JLabel status=new JLabel("WASD fly  •  Q/E altitude  •  Mouse drag look  •  Wheel forward/back  •  Ctrl+S save");
    private final JTextArea navInfo=new JTextArea(6,25);
    private final Set<Integer> keys=new HashSet<>();
    private final Timer timer;
    private Row chosen;
    private boolean modifying;
    private int lastMouseX,lastMouseY;
    private boolean mouseDrag;
    private double clock;
    private final JFrame window;

    public static void open(Window parent){
        var frame=new JFrame("Beckenburg — Developer Level Editor");
        var editor=new DeveloperLevelEditor(frame);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(editor);
        frame.setSize(1300,790);frame.setMinimumSize(new Dimension(900,570));
        frame.setLocationRelativeTo(parent);
        frame.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){editor.timer.stop();}
        });
        frame.setVisible(true);
        SwingUtilities.invokeLater(editor.viewport::requestFocusInWindow);
    }
    private DeveloperLevelEditor(JFrame window){
        super(new BorderLayout(6,5));this.window=window;
        setBackground(new Color(18,22,26));setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        renderer.setRenderSize(560,350);renderer.lamp=false;
        double[] spawn=data.spawn();
        renderer.x=spawn[0];renderer.z=spawn[1];
        renderer.yaw=spawn[2];renderer.pitch=-.12;
        viewport.setBackground(Color.BLACK);viewport.setFocusable(true);
        viewport.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                lastMouseX=e.getX();lastMouseY=e.getY();mouseDrag=true;viewport.requestFocusInWindow();
            }
            @Override public void mouseReleased(MouseEvent e){mouseDrag=false;}
            @Override public void mouseClicked(MouseEvent e){
                if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==2)
                    pick(e.getX(),e.getY());
            }
        });
        viewport.addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                if(!mouseDrag)return;
                renderer.yaw-=(e.getX()-lastMouseX)*.006;
                renderer.pitch=Math.max(-1.35,Math.min(1.35,
                    renderer.pitch+(e.getY()-lastMouseY)*.004));
                lastMouseX=e.getX();lastMouseY=e.getY();
            }
        });
        viewport.addMouseWheelListener(e->advance(-e.getPreciseWheelRotation()*.85));
        viewport.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){
                if(e.isControlDown()&&e.getKeyCode()==KeyEvent.VK_S){save();return;}
                keys.add(e.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}
        });
        viewport.addFocusListener(new FocusAdapter(){
            @Override public void focusLost(FocusEvent e){keys.clear();}
        });

        var header=new JPanel(new BorderLayout(6,0));
        header.add(new JLabel("DEVELOPER LEVEL EDITOR   /   CASTLE BECKENBURG  /  LIVE SCENE"),BorderLayout.WEST);
        var commands=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));
        JButton save=button("SAVE"),export=button("EXPORT"),reset=button("RESET");
        save.addActionListener(e->save());
        export.addActionListener(e->export());
        reset.addActionListener(e->{
            if(JOptionPane.showConfirmDialog(window,"Reset all castle scene overrides?","Reset level",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION){
                data.reset();refresh();save();
            }
        });
        commands.add(save);commands.add(export);commands.add(reset);header.add(commands,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);

        JPanel editorSide=new JPanel(new BorderLayout(5,5));
        editorSide.setPreferredSize(new Dimension(355,400));
        JPanel search=new JPanel(new BorderLayout(4,2));
        search.add(filter,BorderLayout.CENTER);
        JLabel legend=new JLabel("Objects / fixtures / stations / spawn");
        search.add(legend,BorderLayout.NORTH);
        editorSide.add(search,BorderLayout.NORTH);
        hierarchy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hierarchy.addListSelectionListener(e->{if(!e.getValueIsAdjusting())select(hierarchy.getSelectedValue());});
        editorSide.add(new JScrollPane(hierarchy),BorderLayout.CENTER);
        filter.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){refresh();}
            public void removeUpdate(DocumentEvent e){refresh();}
            public void changedUpdate(DocumentEvent e){refresh();}
        });
        JTabbedPane tabs=new JTabbedPane();
        tabs.add("3D objects",objectInspector());
        tabs.add("Surfaces & nav",surfaceInspector());
        editorSide.add(tabs,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,viewport,editorSide);
        split.setResizeWeight(1);split.setContinuousLayout(true);
        add(split,BorderLayout.CENTER);
        status.setForeground(new Color(181,218,199));
        add(status,BorderLayout.SOUTH);
        List<String> names=new ArrayList<>(GlbProps.availableModels());names.sort(String::compareTo);
        for(String name:names)library.addItem(name);
        library.setSelectedItem("sofa_1");
        navInfo.setEditable(false);navInfo.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        refresh();
        timer=new Timer(90,e->frame());timer.start();
    }
    private static JButton button(String name){return new JButton(name);}
    private JPanel objectInspector(){
        JPanel panel=new JPanel(new BorderLayout(4,3));
        JPanel addRow=new JPanel(new BorderLayout(3,3));
        addRow.add(library,BorderLayout.CENTER);
        JButton add=button("ADD ASSET");
        add.addActionListener(e->addAsset());
        addRow.add(add,BorderLayout.EAST);panel.add(addRow,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(0,4,3,3));
        fields.add(new JLabel("X"));fields.add(x);
        fields.add(new JLabel("Y"));fields.add(y);
        fields.add(new JLabel("Z"));fields.add(z);
        fields.add(new JLabel("Yaw °"));fields.add(yaw);
        fields.add(new JLabel("Size"));fields.add(scale);
        fields.add(new JLabel("Collision X"));fields.add(width);
        fields.add(new JLabel("Collision Z"));fields.add(depth);
        fields.add(new JLabel("Light range"));fields.add(radius);
        fields.add(new JLabel("Light power"));fields.add(power);
        for(JSpinner input:new JSpinner[]{x,y,z,yaw,scale,width,depth,radius,power})
            input.addChangeListener(e->applySelection());
        panel.add(fields,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(1,3,4,0));
        JButton here=button("PLACE AHEAD"),remove=button("REMOVE"),addLight=button("ADD LIGHT");
        here.addActionListener(e->placeAhead());
        remove.addActionListener(e->remove());
        addLight.addActionListener(e->addLamp());
        actions.add(here);actions.add(remove);actions.add(addLight);
        panel.add(actions,BorderLayout.SOUTH);
        return panel;
    }
    private JPanel surfaceInspector(){
        JPanel panel=new JPanel(new BorderLayout(3,4));
        JPanel tools=new JPanel(new GridLayout(0,4,3,3));
        tools.add(new JLabel("Tile X"));tools.add(tileX);
        tools.add(new JLabel("Tile Z"));tools.add(tileZ);
        tools.add(new JLabel("Material"));tools.add(material);
        JButton fill=button("PAINT FLOOR");fill.addActionListener(e->operate(()->{
            data.paintFloor(integer(tileX),integer(tileZ),material.getSelectedIndex());
        }));tools.add(fill);
        JButton wall=button("PAINT WALL");wall.addActionListener(e->operate(()->{
            data.paintWall(integer(wallIndex),material.getSelectedIndex());
        }));tools.add(wall);
        tools.add(new JLabel("Wall ID"));tools.add(wallIndex);
        JButton focusTile=button("CAMERA TILE");focusTile.addActionListener(e->{
            tileX.setValue((int)Math.max(0,Math.min(CastleHubMap.WIDTH-1,Math.floor(renderer.x))));
            tileZ.setValue((int)Math.max(0,Math.min(CastleHubMap.HEIGHT-1,Math.floor(renderer.z))));
            updateNav();
        });tools.add(focusTile);
        JButton block=button("SET NAV");
        block.addActionListener(e->operate(()->data.setNavBlocked(integer(tileX),
            integer(tileZ),navBlocked.isSelected())));
        tools.add(block);
        tools.add(navBlocked);tools.add(navOverlay);
        JButton spawn=button("SPAWN HERE");spawn.addActionListener(e->operate(()->{
            data.setSpawn(renderer.x,renderer.z,renderer.yaw);
            refresh();
        }));tools.add(spawn);
        tileX.addChangeListener(e->updateNav());tileZ.addChangeListener(e->updateNav());
        wallIndex.addChangeListener(e->updateNav());
        navOverlay.addActionListener(e->viewport.repaint());
        panel.add(tools,BorderLayout.NORTH);
        panel.add(new JScrollPane(navInfo),BorderLayout.CENTER);
        return panel;
    }
    private static int integer(JSpinner s){return ((Number)s.getValue()).intValue();}
    private static double value(JSpinner s){return ((Number)s.getValue()).doubleValue();}
    private void operate(Runnable action){
        try{action.run();updateNav();status.setText("Modified scene — SAVE to persist");}
        catch(RuntimeException e){JOptionPane.showMessageDialog(window,e.getMessage(),
            "Invalid level edit",JOptionPane.ERROR_MESSAGE);}
    }
    private void addAsset(){
        String mesh=(String)library.getSelectedItem();if(mesh==null)return;
        double aheadX=renderer.x+Math.sin(renderer.yaw)*2.4;
        double aheadZ=renderer.z+Math.cos(renderer.yaw)*2.4;
        if(aheadX<0||aheadX>CastleHubMap.WIDTH||aheadZ<0||aheadZ>CastleHubMap.HEIGHT){
            status.setText("Move inside the level before placing an asset");return;
        }
        String id="custom"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Prop(id,mesh,aheadX,
            renderer.eyeHeight>3?CastleHubArchitecturalPlan.GROUND_CEILING:0,
            aheadZ,.8,0,.4,.4,renderer.eyeHeight>3)));
        refresh();selectId("PROP",id);
    }
    private void addLamp(){
        double px=renderer.x+Math.sin(renderer.yaw)*2;
        double pz=renderer.z+Math.cos(renderer.yaw)*2;
        String id="light"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Lamp(id,"ceiling_lamp_1_on",
            "ceiling_lamp_1_off",px,3.38,pz,.8,0,CastleHubDecor.Mount.CEILING,9.8,2.5)));
        refresh();selectId("LAMP",id);
    }
    private void remove(){
        if(chosen==null)return;
        Row row=chosen;
        if("PROP".equals(row.kind))operate(()->data.removeProp(row.id));
        else if("LAMP".equals(row.kind))operate(()->data.removeLamp(row.id));
        else{status.setText("Station/spawn cannot be deleted: move them instead");return;}
        chosen=null;refresh();
    }
    private void placeAhead(){
        if(chosen==null)return;
        modifying=true;
        x.setValue(renderer.x+Math.sin(renderer.yaw)*2.2);
        z.setValue(renderer.z+Math.cos(renderer.yaw)*2.2);
        modifying=false;applySelection();
    }
    private void select(Row row){
        if(row==null){chosen=null;renderer.clearEditorSelection();return;}
        chosen=row;modifying=true;
        try{
            if(row.kind.equals("PROP")){
                CastleLevelEdits.Prop item=data.props().stream().filter(p->p.id().equals(row.id))
                    .findFirst().orElse(null);
                if(item==null)return;
                x.setValue(item.x());y.setValue(item.y());z.setValue(item.z());
                yaw.setValue(Math.toDegrees(item.yaw()));scale.setValue(item.scale());
                width.setValue(item.halfX());depth.setValue(item.halfZ());
            }else if(row.kind.equals("LAMP")){
                CastleLevelEdits.Lamp l=data.lamps().stream().filter(p->p.id().equals(row.id))
                    .findFirst().orElse(null);
                if(l==null)return;
                x.setValue(l.x());y.setValue(l.y());z.setValue(l.z());
                yaw.setValue(Math.toDegrees(l.yaw()));scale.setValue(l.scale());
                radius.setValue(l.radius());power.setValue(l.power());
            }else if(row.kind.equals("STATION")){
                var coords=data.station(CastleHubMap.Station.valueOf(row.id));
                x.setValue(coords[0]);y.setValue(0.);z.setValue(coords[1]);
            }else if(row.kind.equals("SPAWN")){
                double[] sp=data.spawn();
                x.setValue(sp[0]);y.setValue(0.);z.setValue(sp[1]);
                yaw.setValue(Math.toDegrees(sp[2]));
            }
            renderer.setEditorSelection(value(x),value(y),value(z));
        }finally{modifying=false;}
    }
    private void applySelection(){
        if(modifying||chosen==null)return;
        Row r=chosen;
        operate(()->{
            switch(r.kind){
                case "PROP" ->{
                    var old=data.props().stream().filter(p->p.id().equals(r.id)).findFirst().orElseThrow();
                    data.put(new CastleLevelEdits.Prop(r.id,old.mesh(),value(x),value(y),
                        value(z),value(scale),Math.toRadians(value(yaw)),
                        value(width),value(depth),old.upper()));
                }
                case "LAMP" ->{
                    var old=data.lamps().stream().filter(p->p.id().equals(r.id)).findFirst().orElseThrow();
                    data.put(new CastleLevelEdits.Lamp(r.id,old.on(),old.off(),value(x),
                        value(y),value(z),value(scale),Math.toRadians(value(yaw)),
                        old.mount(),value(radius),value(power)));
                }
                case "STATION" -> data.setStation(CastleHubMap.Station.valueOf(r.id),value(x),value(z));
                case "SPAWN" -> data.setSpawn(value(x),value(z),Math.toRadians(value(yaw)));
                default -> throw new IllegalArgumentException("Unknown editor entity");
            }
            renderer.setEditorSelection(value(x),value(y),value(z));
        });
    }
    private void selectId(String kind,String id){
        for(int i=0;i<rows.size();i++)if(rows.get(i).kind.equals(kind)&&rows.get(i).id.equals(id)){
            hierarchy.setSelectedIndex(i);hierarchy.ensureIndexIsVisible(i);return;
        }
    }
    private void refresh(){
        String kind=chosen==null?"":chosen.kind,id=chosen==null?"":chosen.id;
        rows.clear();
        String q=filter.getText().strip().toLowerCase(Locale.ROOT);
        var list=new ArrayList<Row>();
        for(var p:data.props())list.add(new Row("PROP",p.id(),p.id()+"  •  "+p.mesh()));
        for(var l:data.lamps())list.add(new Row("LAMP",l.id(),l.id()+"  •  "+l.on()+"  [LIGHT]"));
        for(var station:CastleHubMap.Station.values())
            list.add(new Row("STATION",station.name(),"STATION  •  "+station.name()));
        list.add(new Row("SPAWN","start","PLAYER SPAWN"));
        list.sort(Comparator.comparing(Row::description));
        for(Row r:list)if(q.isEmpty()||r.description.toLowerCase(Locale.ROOT).contains(q))rows.addElement(r);
        selectId(kind,id);updateNav();
    }
    private void updateNav(){
        int px=integer(tileX),pz=integer(tileZ),w=integer(wallIndex);
        navBlocked.setSelected(data.navBlocked(px,pz));
        var wall=CastleHubArchitecturalPlan.walls().get(w);
        navInfo.setText("CELL "+px+", "+pz+"  solid="+map.solid(px,pz)+
            "  navigable="+map.traversable(px+.5,pz+.5)+
            "\nFloor tile="+data.floorMaterial(px,pz,MaterialAtlas.STONE)+
            "  edited blocker="+data.navBlocked(px,pz)+
            "\nUpper gallery="+CastleHubArchitecturalPlan.upperWalkable(px+.5,pz+.5)+
            "  stairs="+CastleHubArchitecturalPlan.onStairs(px+.5,pz+.5)+
            "\nWALL "+w+"  ("+wall.x0()+","+wall.z0()+") -> ("+wall.x1()+","+wall.z1()+")"+
            "\nMaterial="+data.wallMaterial(w,wall.material())+
            "  door opening="+wall.hasDoor()+"\nScene file: "+data.path());
        navInfo.setCaretPosition(0);
    }
    private void pick(int mouseX,int mouseY){
        // Closest projected registered object to the clicked viewport position.
        double best=Double.POSITIVE_INFINITY;Row hit=null;
        for(int i=0;i<rows.size();i++){
            Row r=rows.get(i);
            double px,pz,py;
            if("PROP".equals(r.kind)){
                var p=data.props().stream().filter(t->t.id().equals(r.id)).findFirst().orElse(null);
                if(p==null)continue;px=p.x();pz=p.z();py=p.y()+.8;
            }else if("LAMP".equals(r.kind)){
                var l=data.lamps().stream().filter(t->t.id().equals(r.id)).findFirst().orElse(null);
                if(l==null)continue;px=l.x();pz=l.z();py=l.y();
            }else continue;
            double dx=px-renderer.x,dz=pz-renderer.z,dy=py-renderer.eyeHeight;
            double sy=Math.sin(renderer.yaw),cy=Math.cos(renderer.yaw);
            double cx=-dx*cy+dz*sy,cz=dx*sy+dz*cy,sp=Math.sin(renderer.pitch),cp=Math.cos(renderer.pitch);
            double yy=dy*cp-cz*sp,zz=dy*sp+cz*cp;
            if(zz<.25)continue;
            double sx=viewport.getWidth()*.5+cx/zz*viewport.getHeight()*.75;
            double syy=viewport.getHeight()*.5-yy/zz*viewport.getHeight()*.75;
            double pixel=Math.hypot(sx-mouseX,syy-mouseY);
            if(pixel<32&&pixel<best){best=pixel;hit=r;}
        }
        if(hit!=null)hierarchy.setSelectedValue(hit,true);
    }
    private void advance(double amount){
        renderer.x+=Math.sin(renderer.yaw)*amount;
        renderer.z+=Math.cos(renderer.yaw)*amount;
        renderer.x=Math.max(.2,Math.min(43.8,renderer.x));
        renderer.z=Math.max(.2,Math.min(35.8,renderer.z));
    }
    private void frame(){
        double step=keys.contains(KeyEvent.VK_SHIFT)?.95:.34;
        if(keys.contains(KeyEvent.VK_W))advance(step);
        if(keys.contains(KeyEvent.VK_S))advance(-step);
        if(keys.contains(KeyEvent.VK_A)){
            renderer.x-=Math.cos(renderer.yaw)*step;
            renderer.z+=Math.sin(renderer.yaw)*step;
        }
        if(keys.contains(KeyEvent.VK_D)){
            renderer.x+=Math.cos(renderer.yaw)*step;
            renderer.z-=Math.sin(renderer.yaw)*step;
        }
        if(keys.contains(KeyEvent.VK_Q))renderer.eyeHeight-=step;
        if(keys.contains(KeyEvent.VK_E))renderer.eyeHeight+=step;
        if(keys.contains(KeyEvent.VK_LEFT))renderer.yaw+=.06;
        if(keys.contains(KeyEvent.VK_RIGHT))renderer.yaw-=.06;
        if(keys.contains(KeyEvent.VK_UP))renderer.pitch=Math.min(1.35,renderer.pitch+.045);
        if(keys.contains(KeyEvent.VK_DOWN))renderer.pitch=Math.max(-1.35,renderer.pitch-.045);
        renderer.eyeHeight=Math.max(.25,Math.min(11,renderer.eyeHeight));
        renderer.setEditorNavigation(navOverlay.isSelected());
        try{renderer.drawCastle(map,clock+=.05,true);}
        catch(RuntimeException problem){
            timer.stop();BeckenburgDiagnostics.fail("Developer editor viewport",problem);
            status.setText("Renderer failure: "+problem.getMessage());return;
        }
        viewport.repaint();
    }
    private void save(){
        try{data.save();status.setText("SAVED  "+data.path());viewport.requestFocusInWindow();}
        catch(IOException e){JOptionPane.showMessageDialog(window,e.getMessage(),"Save failed",JOptionPane.ERROR_MESSAGE);}
    }
    private void export(){
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export level editor document");
        chooser.setSelectedFile(new java.io.File("castle-beckenburg.properties"));
        if(chooser.showSaveDialog(window)!=JFileChooser.APPROVE_OPTION)return;
        try{
            data.save();
            Files.copy(data.path(),chooser.getSelectedFile().toPath(),StandardCopyOption.REPLACE_EXISTING);
            status.setText("EXPORTED "+chooser.getSelectedFile());
        }catch(IOException e){JOptionPane.showMessageDialog(window,e.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}
    }
}
