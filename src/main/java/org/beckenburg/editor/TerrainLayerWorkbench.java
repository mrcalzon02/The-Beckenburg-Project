package org.beckenburg.editor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Real 2D authoring viewport for the independent terrain-layer engine.
 * This is a source-data preview, not a simulated game renderer.
 */
public final class TerrainLayerWorkbench extends JPanel {
    private final JFrame frame;
    private TerrainLayerStack stack;
    private final StitchedHeightmapWorld sourceWorld;
    private final int sourceX,sourceZ;
    private final JLabel status=new JLabel("Layered terrain editor");
    private final JPanel canvas=new Canvas();
    private final JComboBox<String> view=new JComboBox<>(new String[]{"HEIGHT","MATERIALS","SLOPES","FROZEN MASK"});
    private final JComboBox<String> layers=new JComboBox<>();
    private final JCheckBox visible=new JCheckBox("Layer on",true);
    private final JSlider opacity=new JSlider(0,100,100);
    private final JComboBox<String> action=new JComboBox<>(new String[]{
        "SCULPT","PAINT MATERIAL","FREEZE","THAW","GRADE RAIL","GRADE ROAD","GRADE RIVER","STAMP"});
    private final JComboBox<TerrainLayerStack.Brush> brush=new JComboBox<>(TerrainLayerStack.Brush.values());
    private final JComboBox<TerrainLayerStack.Material> material=
        new JComboBox<>(TerrainLayerStack.Material.values());
    private final JComboBox<String> falloff=new JComboBox<>(new String[]{"LINEAR","SMOOTH","SHARP"});
    private final JSpinner radius=spin(6,.5,512,.5);
    private final JSpinner strength=spin(2,.01,1000,.5);
    private final JSpinner target=spin(0,-100000,100000,1);
    private final JSpinner inner=spin(.2,0,.95,.05);
    private final JCheckBox sampleFlatten=new JCheckBox("Sample flatten height on press",false);
    private final JComboBox<TerrainLayerStack.Noise> noise=new JComboBox<>(TerrainLayerStack.Noise.values());
    private final JCheckBox liveNoise=new JCheckBox("LIVE LOW-RES PREVIEW (uncommitted)",false);
    private final JSpinner cycles=spin(4,.05,512,.5);
    private final JSpinner amplitude=spin(15,-10000,10000,1);
    private final JSpinner octaves=spin(4,1,8,1);
    private final JSpinner seed=spin(1234,-2147483647,2147483647,1);
    private final JSpinner iterations=spin(5,1,100,1);
    private final JSpinner talus=spin(35,1,88,1);
    private final JSpinner erosionRate=spin(.12,.01,.25,.01);
    private final JButton bake=new JButton("BAKE SOURCE...");
    private final JButton save=new JButton("SAVE LAYERS");
    private final JButton undo=new JButton("UNDO"),redo=new JButton("REDO");
    private BufferedImage image,stampImage;
    private boolean loadingLayer,busy;
    private final TerrainInfrastructureSplines routes;
    private final java.util.ArrayList<TerrainLayerStack.Point> gradeControls=new java.util.ArrayList<>();
    private final JTextField routeName=new JTextField("route-1",14);
    private boolean drawing;

    public static JFrame open(Window owner,StitchedHeightmapWorld world,int tx,int tz)throws IOException {
        JFrame f=new JFrame("Level Editor Kit — Terrain Layers");
        TerrainLayerWorkbench panel=new TerrainLayerWorkbench(f,new TerrainLayerStack(world,tx,tz));
        f.setContentPane(panel);f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(WindowEvent e){
                if(panel.busy)return;
                if(panel.stack.dirty()){
                    int answer=JOptionPane.showConfirmDialog(f,
                        "Unsaved terrain layers. Close without saving?","Unsaved changes",
                        JOptionPane.YES_NO_OPTION);
                    if(answer!=JOptionPane.YES_OPTION)return;
                }
                f.dispose();
            }
        });
        f.setSize(1110,760);f.setMinimumSize(new Dimension(790,520));
        f.setLocationRelativeTo(owner);f.setVisible(true);return f;
    }
    private static JSpinner spin(double v,double min,double max,double step){
        return new JSpinner(new SpinnerNumberModel(v,min,max,step));
    }
    private static double val(JSpinner s){return ((Number)s.getValue()).doubleValue();}
    private TerrainLayerWorkbench(JFrame frame,TerrainLayerStack stack)throws IOException{
        super(new BorderLayout(5,5));this.frame=frame;this.stack=stack;
        this.sourceWorld=stack.worldForWorkbench();this.sourceX=stack.tileX();this.sourceZ=stack.tileZ();
        this.routes=TerrainInfrastructureSplines.open(sourceWorld);
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));
        bar.add(new JLabel("Layer"));bar.add(layers);bar.add(visible);
        bar.add(new JLabel("Opacity"));opacity.setPreferredSize(new Dimension(100,22));bar.add(opacity);
        JButton add=new JButton("+ LAYER");bar.add(add);bar.add(undo);bar.add(redo);
        bar.add(save);bar.add(bake);
        add(bar,BorderLayout.NORTH);
        JPanel sidebar=new JPanel();sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));
        section(sidebar,"BRUSH / PAINT");
        field(sidebar,"Operation",action);field(sidebar,"Brush",brush);
        field(sidebar,"Radius (grid units)",radius);field(sidebar,"Strength (metres)",strength);
        field(sidebar,"Target height / terrace step",target);
        field(sidebar,"Inner radius",inner);field(sidebar,"Edge falloff",falloff);
        sidebar.add(sampleFlatten);
        field(sidebar,"Surface material",material);
        section(sidebar,"PROCEDURAL");
        field(sidebar,"Noise",noise);field(sidebar,"Cycles / tile",cycles);
        field(sidebar,"Amplitude (metres)",amplitude);field(sidebar,"Octaves",octaves);
        field(sidebar,"Seed",seed);
        sidebar.add(liveNoise);
        button(sidebar,"APPLY NOISE TO LAYER",()->background("Procedural noise",
            ()->stack.noise((TerrainLayerStack.Noise)noise.getSelectedItem(),
                (long)val(seed),val(cycles),val(amplitude),(int)val(octaves))));
        section(sidebar,"EROSION");
        field(sidebar,"Iterations",iterations);field(sidebar,"Talus angle",talus);
        field(sidebar,"Thermal transfer",erosionRate);
        button(sidebar,"THERMAL EROSION",()->background("Thermal erosion",
            ()->stack.thermal((int)val(iterations),val(talus),val(erosionRate))));
        button(sidebar,"HYDRAULIC EROSION",()->background("Hydraulic droplets",
            ()->stack.hydraulic((int)val(iterations)*150,32,(long)val(seed),.10,.25)));
        button(sidebar,"WIND EROSION +X",()->background("Wind sediment transport",
            ()->stack.wind((int)val(iterations),1,0,val(erosionRate))));
        section(sidebar,"MASKS / INFRASTRUCTURE");
        button(sidebar,"AUTO BIOME MATERIALS",()->background("Autopainting materials",
            ()->stack.autoBiome(val(target),val(target)+300,45)));
        button(sidebar,"LOCK CURRENT MATERIAL",()->{
            stack.lockMaterial((TerrainLayerStack.Material)material.getSelectedItem(),true);
            refresh("Material locked: "+material.getSelectedItem());
        });
        button(sidebar,"UNLOCK CURRENT MATERIAL",()->{
            stack.lockMaterial((TerrainLayerStack.Material)material.getSelectedItem(),false);
            refresh("Material unlocked: "+material.getSelectedItem());
        });
        button(sidebar,"LOAD HEIGHT STAMP PNG",this::loadStamp);
        field(sidebar,"Route name / stable ID",routeName);
        button(sidebar,"APPLY AND SAVE ROUTE",this::applyRoute);
        button(sidebar,"CLEAR ROUTE POINTS",()->{gradeControls.clear();refresh("Route draft cleared");});
        JScrollPane scroll=new JScrollPane(sidebar);
        scroll.setPreferredSize(new Dimension(290,500));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll,BorderLayout.WEST);
        JPanel center=new JPanel(new BorderLayout(4,4));
        JPanel views=new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
        views.add(new JLabel("Inspect"));views.add(view);
        views.add(new JLabel("Click + drag to edit; grade paths by clicking two endpoints"));
        center.add(views,BorderLayout.NORTH);
        canvas.setBackground(new Color(25,29,34));
        center.add(canvas,BorderLayout.CENTER);
        add(center,BorderLayout.CENTER);
        JPanel footer=new JPanel(new GridLayout(2,1));
        footer.add(new JLabel("Real authored 2D data. Source tiles stay untouched until explicit bake. Shared borders locked."));
        footer.add(status);add(footer,BorderLayout.SOUTH);
        add.addActionListener(e->addLayer());
        layers.addActionListener(e->selectLayer());
        visible.addActionListener(e->configureLayer());
        opacity.addChangeListener(e->{if(!opacity.getValueIsAdjusting())configureLayer();});
        undo.addActionListener(e->{if(stack.undo())refresh("Undid terrain stroke");});
        redo.addActionListener(e->{if(stack.redo())refresh("Redid terrain stroke");});
        save.addActionListener(e->save());
        bake.addActionListener(e->bake());
        view.addActionListener(e->render());
        liveNoise.addActionListener(e->render());
        noise.addActionListener(e->{if(liveNoise.isSelected())render();});
        for(JSpinner field:new JSpinner[]{cycles,amplitude,octaves,seed})
            field.addChangeListener(e->{if(liveNoise.isSelected())render();});
        action.addActionListener(e->{gradeControls.clear();});
        rebuildLayers();render();
        refresh("Editing tile "+stack.tileX()+","+stack.tileZ()+" — unsaved edits are separate from source");
    }
    private static void section(JPanel into,String name){
        JLabel label=new JLabel(name);label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(10,3,2,0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);into.add(label);
    }
    private static void field(JPanel into,String title,JComponent control){
        JPanel row=new JPanel(new BorderLayout(4,1));
        row.add(new JLabel(title),BorderLayout.WEST);row.add(control,BorderLayout.EAST);
        row.setMaximumSize(new Dimension(600,30));row.setAlignmentX(Component.LEFT_ALIGNMENT);
        into.add(row);
    }
    private static void button(JPanel into,String text,Runnable task){
        JButton b=new JButton(text);b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e->task.run());into.add(b);
    }
    private void rebuildLayers(){
        loadingLayer=true;layers.removeAllItems();
        for(var layer:stack.layers())layers.addItem(layer.name());
        layers.setSelectedIndex(stack.activeLayer());
        var current=stack.layers().get(stack.activeLayer());
        visible.setSelected(current.enabled());opacity.setValue((int)Math.round(current.opacity()*100));
        loadingLayer=false;
    }
    private void selectLayer(){
        if(loadingLayer||layers.getSelectedIndex()<0||busy)return;
        stack.selectLayer(layers.getSelectedIndex());rebuildLayers();refresh("Active layer changed");
    }
    private void configureLayer(){
        if(loadingLayer||layers.getSelectedIndex()<0||busy)return;
        stack.configureLayer(layers.getSelectedIndex(),visible.isSelected(),opacity.getValue()/100.0);
        refresh("Layer settings updated");
    }
    private void addLayer(){
        String name=JOptionPane.showInputDialog(frame,"Name for new non-destructive layer","New Layer");
        if(name==null)return;
        try{stack.addLayer(name.trim());rebuildLayers();refresh("Layer added");}
        catch(RuntimeException ex){error(ex);}
    }
    private void refresh(String message){
        status.setText(message+(stack.dirty()?"  • UNSAVED":"  • SAVED"));
        render();
    }
    private void render(){
        int res=Math.min(256,stack.cells()+1);
        BufferedImage bitmap=new BufferedImage(res,res,BufferedImage.TYPE_INT_RGB);
        int n=stack.cells();
        double[][] temporary=liveNoise.isSelected()&&view.getSelectedIndex()==0
            ?stack.previewNoise((TerrainLayerStack.Noise)noise.getSelectedItem(),
                (long)val(seed),val(cycles),val(amplitude),(int)val(octaves),res)
            :null;
        for(int z=0;z<res;z++)for(int x=0;x<res;x++){
            int sx=x*n/(res-1),sz=z*n/(res-1),rgb;
            switch(view.getSelectedIndex()){
                case 1 -> rgb=new int[]{0x648b48,0x777b82,0x80644b,0xc5b47e,
                    0x554638,0xb7c4cf,0x93928e,0x526a6f}[stack.material(sx,sz)];
                case 2 -> {int s=(int)Math.min(255,stack.slope(sx,sz)*4);
                    rgb=(s<<16)|((255-s)<<8)|50;}
                case 3 -> rgb=stack.frozen(sx,sz)?0xff2020:0x4e6a6f;
                default -> {
                    double elevation=temporary==null?stack.height(sx,sz):temporary[z][x];
                    double norm=(elevation-stack.minimumElevation())/
                        (stack.maximumElevation()-stack.minimumElevation());
                    int grey=(int)Math.max(0,Math.min(255,norm*255));
                    rgb=(grey<<16)|(grey<<8)|grey;
                }
            }
            bitmap.setRGB(x,z,rgb);
        }
        image=bitmap;canvas.repaint();
    }
    @FunctionalInterface private interface Job{void run()throws Exception;}
    private void background(String name,Job job){
        if(busy)return;busy=true;status.setText(name+" running (editable source remains intact)...");
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{job.run();return null;}
            protected void done(){
                busy=false;
                try{get();if(name.startsWith("Baking"))rebuildLayers();
                    if(name.equals("Procedural noise"))liveNoise.setSelected(false);
                    refresh(name+" completed");}
                catch(InterruptedException ex){Thread.currentThread().interrupt();error(ex);}
                catch(ExecutionException ex){error(ex);}
            }
        }.execute();
    }
    private void save(){
        if(busy)return;
        background("Saving non-destructive terrain layers",stack::save);
    }
    private void bake(){
        if(busy)return;
        int reply=JOptionPane.showConfirmDialog(frame,
            "Replace source tile "+stack.tileX()+","+stack.tileZ()+" with the composed terrain? "+
            "This is irreversible. Adjacent seams remain protected.","Explicit terrain bake",
            JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(reply!=JOptionPane.YES_OPTION)return;
        background("Baking authoritative 16-bit PNG",()->{
            stack.bake();stack=new TerrainLayerStackSource().reopen();
        });
    }
    private final class TerrainLayerStackSource {
        TerrainLayerStack reopen()throws IOException {
            // Reopening from the same metadata avoids accidentally persisting an old source checksum.
            var world=sourceWorld;
            return new TerrainLayerStack(world,sourceX,sourceZ);
        }
    }
    private void applyRoute(){
        if(busy)return;
        try{
            if(gradeControls.size()<2)throw new IllegalArgumentException(
                "Click at least two control points on the terrain first");
            String tool=(String)action.getSelectedItem();
            TerrainInfrastructureSplines.Kind kind=switch(tool){
                case "GRADE RAIL" -> TerrainInfrastructureSplines.Kind.RAIL;
                case "GRADE ROAD" -> TerrainInfrastructureSplines.Kind.ROAD;
                case "GRADE RIVER" -> TerrainInfrastructureSplines.Kind.RIVER;
                default -> throw new IllegalArgumentException("Choose a GRADE operation");
            };
            double maximum=kind==TerrainInfrastructureSplines.Kind.RAIL?4:
                kind==TerrainInfrastructureSplines.Kind.ROAD?12:1000;
            double controlLimit=kind==TerrainInfrastructureSplines.Kind.RAIL?3:
                kind==TerrainInfrastructureSplines.Kind.ROAD?9:1000;
            var controls=kind==TerrainInfrastructureSplines.Kind.RIVER?
                List.copyOf(gradeControls):
                TerrainInfrastructureSplines.limitGrade(gradeControls,
                    stack.metresPerCell(),controlLimit);
            var alignment=new TerrainInfrastructureSplines.Alignment(
                routeName.getText().trim(),kind,controls,val(radius),
                val(radius)*(kind==TerrainInfrastructureSplines.Kind.RAIL?2:1),
                maximum);
            // Check geometric viability before touching either layer or route file.
            TerrainInfrastructureSplines.evaluate(alignment,stack.metresPerCell());
            routes.apply(stack,alignment);
            routes.put(alignment);routes.save();
            gradeControls.clear();routeName.setText("route-"+(routes.list().size()+1));
            refresh("Applied and persisted "+alignment.kind()+" "+alignment.id());
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private void loadStamp(){
        JFileChooser picker=new JFileChooser();
        if(picker.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try{
            stampImage=ImageIO.read(picker.getSelectedFile());
            if(stampImage==null||stampImage.getWidth()<2||stampImage.getHeight()<2
                ||stampImage.getWidth()>2048||stampImage.getHeight()>2048)
                throw new IOException("Stamp must be a 2..2048 pixel PNG/image");
            action.setSelectedItem("STAMP");refresh("Loaded height stamp — click terrain to apply");
        }catch(IOException ex){error(ex);}
    }
    private float[][] stamp(){
        int h=Math.min(129,stampImage.getHeight()),w=Math.min(129,stampImage.getWidth());
        float[][] result=new float[h][w];
        var raster=stampImage.getRaster();
        for(int z=0;z<h;z++)for(int x=0;x<w;x++){
            int sx=x*(stampImage.getWidth()-1)/(w-1),sz=z*(stampImage.getHeight()-1)/(h-1);
            result[z][x]=raster.getSample(sx,sz,0);
        }
        return result;
    }
    private void error(Throwable failure){
        while(failure.getCause()!=null)failure=failure.getCause();
        status.setText("FAILED: "+failure.getMessage());
        JOptionPane.showMessageDialog(frame,failure.getMessage(),"Terrain tool failure",
            JOptionPane.ERROR_MESSAGE);
    }
    private void applyAt(int x,int z,boolean pressed){
        if(busy)return;
        try{
            String mode=(String)action.getSelectedItem();
            double r=val(radius);
            switch(mode){
                case "SCULPT" -> {
                    if(pressed&&sampleFlatten.isSelected()
                        &&brush.getSelectedItem()==TerrainLayerStack.Brush.FLATTEN)
                        target.setValue(stack.height(x,z));
                    stack.brush((TerrainLayerStack.Brush)brush.getSelectedItem(),
                        x,z,r,val(strength),val(target),val(inner),falloff.getSelectedIndex());
                }
                case "PAINT MATERIAL" -> stack.paintMaterial(x,z,r,
                    (TerrainLayerStack.Material)material.getSelectedItem(),90);
                case "FREEZE","THAW" -> stack.freezeRect((int)(x-r),(int)(z-r),
                    (int)(x+r),(int)(z+r),mode.equals("FREEZE"));
                case "STAMP" -> {
                    if(pressed&&stampImage!=null)
                        stack.stamp(stamp(),x,z,r,val(strength)/100);
                }
                case "GRADE RAIL","GRADE ROAD","GRADE RIVER" -> {
                    if(pressed){
                        gradeControls.add(new TerrainLayerStack.Point(
                            (long)stack.tileX()*stack.cells()+x,
                            (long)stack.tileZ()*stack.cells()+z,stack.height(x,z)));
                        status.setText("Route controls: "+gradeControls.size()+
                            " — add more or APPLY AND SAVE ROUTE");
                    }
                }
            }
            if(!mode.equals("SCULPT"))stack.finishStroke();
            render();
        }catch(RuntimeException ex){drawing=false;stack.finishStroke();error(ex);}
    }
    private final class Canvas extends JPanel {
        Canvas(){
            MouseAdapter mouse=new MouseAdapter(){
                private int posX(MouseEvent e){return Math.max(0,Math.min(stack.cells(),
                    (int)Math.round(e.getX()*(double)stack.cells()/Math.max(1,getWidth()-1))));}
                private int posZ(MouseEvent e){return Math.max(0,Math.min(stack.cells(),
                    (int)Math.round(e.getY()*(double)stack.cells()/Math.max(1,getHeight()-1))));}
                @Override public void mousePressed(MouseEvent e){
                    if(!SwingUtilities.isLeftMouseButton(e)||busy)return;
                    drawing=true;applyAt(posX(e),posZ(e),true);
                }
                @Override public void mouseDragged(MouseEvent e){
                    if(drawing&&action.getSelectedItem().equals("SCULPT"))
                        applyAt(posX(e),posZ(e),false);
                    else if(drawing&&action.getSelectedItem().equals("PAINT MATERIAL"))
                        applyAt(posX(e),posZ(e),false);
                }
                @Override public void mouseReleased(MouseEvent e){
                    if(drawing){drawing=false;stack.finishStroke();refresh("Stroke completed");}
                }
            };
            addMouseListener(mouse);addMouseMotionListener(mouse);
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(image!=null)g.drawImage(image,0,0,getWidth(),getHeight(),null);
            if(!gradeControls.isEmpty()){
                g.setColor(Color.YELLOW);
                int lastX=0,lastY=0;boolean have=false;
                for(var p:gradeControls){
                    int x=(int)Math.round((p.x()-(long)stack.tileX()*stack.cells())*
                        getWidth()/stack.cells());
                    int y=(int)Math.round((p.z()-(long)stack.tileZ()*stack.cells())*
                        getHeight()/stack.cells());
                    if(have)g.drawLine(lastX,lastY,x,y);
                    g.fillOval(x-4,y-4,8,8);lastX=x;lastY=y;have=true;
                }
            }
            g.setColor(new Color(220,220,220,100));
            g.drawRect(0,0,getWidth()-1,getHeight()-1);
        }
    }
}
