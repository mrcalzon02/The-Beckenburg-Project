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
    private final JComboBox<TerrainLayerStack.FlattenLimit> flattenLimit=
        new JComboBox<>(TerrainLayerStack.FlattenLimit.values());
    private final JComboBox<TerrainLayerStack.Material> material=
        new JComboBox<>(TerrainLayerStack.Material.values());
    private final JComboBox<String> falloff=new JComboBox<>(new String[]{"LINEAR","SMOOTH","SHARP"});
    private final JSpinner radius=spin(6,.5,512,.5);
    private final JSpinner strength=spin(2,.01,1000,.5);
    private final JSpinner target=spin(0,-100000,100000,1);
    private final JSpinner inner=spin(.2,0,.95,.05);
    private final JSpinner brushSpacing=spin(1,.1,128,.25);
    private final JSpinner brushRotation=spin(0,-360,360,5);
    private final JSpinner minBrushHeight=spin(-1000000,-1000000,1000000,10);
    private final JSpinner maxBrushHeight=spin(1000000,-1000000,1000000,10);
    private final JSpinner maxBrushSlope=spin(90,0,90,1);
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
    private final JCheckBox localizedErosion=new JCheckBox("LOCAL EROSION (right-click center)",true);
    private int erosionCenterX,erosionCenterZ;
    private final JButton bake=new JButton("BAKE SOURCE...");
    private final JButton save=new JButton("SAVE LAYERS");
    private final JButton undo=new JButton("UNDO"),redo=new JButton("REDO");
    private BufferedImage image,stampImage;
    private TerrainBrushProfile.Alpha brushAlpha;
    private TerrainBrushProfile.Stroke brushStroke;
    private boolean loadingLayer,busy;
    private final TerrainInfrastructureSplines routes;
    private final TerrainBrushPresets presets;
    private final JComboBox<String> presetNames=new JComboBox<>();
    private final JTextField presetName=new JTextField("brush-1",12);
    private final java.util.ArrayList<TerrainLayerStack.Point> gradeControls=new java.util.ArrayList<>();
    private final JTextField routeName=new JTextField("route-1",14);
    private final JComboBox<String> savedRoutes=new JComboBox<>();
    private int draggedRouteControl=-1;
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
        this.presets=new TerrainBrushPresets(sourceWorld);
        erosionCenterX=stack.cells()/2;erosionCenterZ=stack.cells()/2;
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
        field(sidebar,"Dab spacing (grid)",brushSpacing);
        field(sidebar,"Alpha rotation (degrees)",brushRotation);
        field(sidebar,"Minimum brush elevation",minBrushHeight);
        field(sidebar,"Maximum brush elevation",maxBrushHeight);
        field(sidebar,"Maximum slope (degrees)",maxBrushSlope);
        button(sidebar,"LOAD BRUSH ALPHA PNG",this::loadBrushAlpha);
        button(sidebar,"CLEAR BRUSH ALPHA",()->{
            brushAlpha=null;refresh("Custom alpha cleared; circular falloff active");
        });
        field(sidebar,"Saved brush preset",presetNames);
        field(sidebar,"New preset name",presetName);
        button(sidebar,"SAVE BRUSH PRESET",this::saveBrushPreset);
        button(sidebar,"LOAD BRUSH PRESET",this::loadBrushPreset);
        button(sidebar,"DELETE PRESET RECORD...",this::deleteBrushPreset);
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
        sidebar.add(localizedErosion);
        localizedErosion.addActionListener(e->render());
        button(sidebar,"THERMAL EROSION",()->{
            int count=(int)val(iterations);double angle=val(talus),rate=val(erosionRate);
            var area=erosionArea();
            background("Thermal erosion",()->stack.thermal(count,angle,rate,area));
        });
        button(sidebar,"HYDRAULIC EROSION",()->{
            int count=(int)val(iterations)*150;long rng=(long)val(seed);
            var area=erosionArea();
            background("Hydraulic droplets",()->stack.hydraulic(count,32,rng,.10,.25,area));
        });
        button(sidebar,"WIND EROSION +X",()->{
            int count=(int)val(iterations);double rate=val(erosionRate);
            var area=erosionArea();
            background("Wind sediment transport",()->stack.wind(count,1,0,rate,area));
        });
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
        field(sidebar,"Saved route",savedRoutes);
        button(sidebar,"LOAD ROUTE FOR EDITING",this::loadRoute);
        button(sidebar,"DELETE ROUTE RECORD...",this::deleteRoute);
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
        views.add(new JLabel("Click/drag sculpt; add multiple route controls, then apply"));
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
        undo.addActionListener(e->{if(stack.undo())refresh("Undid terrain edit");});
        redo.addActionListener(e->{if(stack.redo())refresh("Redid terrain edit");});
        save.addActionListener(e->save());
        bake.addActionListener(e->bake());
        view.addActionListener(e->render());
        liveNoise.addActionListener(e->render());
        noise.addActionListener(e->{if(liveNoise.isSelected())render();});
        for(JSpinner field:new JSpinner[]{cycles,amplitude,octaves,seed})
            field.addChangeListener(e->{if(liveNoise.isSelected())render();});
        action.addActionListener(e->{gradeControls.clear();draggedRouteControl=-1;});
        refreshSavedRoutes();
        refreshBrushPresets();
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
    private TerrainLayerStack.Region erosionArea(){
        return localizedErosion.isSelected()?
            TerrainLayerStack.Region.around(erosionCenterX,erosionCenterZ,
                val(radius),stack.cells()):
            new TerrainLayerStack.Region(1,1,stack.cells()-1,stack.cells()-1);
    }
    private void refreshSavedRoutes(){
        String selection=(String)savedRoutes.getSelectedItem();
        savedRoutes.removeAllItems();
        for(var route:routes.list())savedRoutes.addItem(route.id());
        if(selection!=null) savedRoutes.setSelectedItem(selection);
    }
    private TerrainInfrastructureSplines.Alignment selectedRoute(){
        String id=(String)savedRoutes.getSelectedItem();
        if(id==null)return null;
        for(var route:routes.list())if(route.id().equals(id))return route;
        return null;
    }
    private void loadRoute(){
        var route=selectedRoute();
        if(route==null){status.setText("No saved route selected");return;}
        gradeControls.clear();gradeControls.addAll(route.controls());
        routeName.setText(route.id());
        radius.setValue(Math.min(512,Math.max(.5,route.halfWidth())));
        action.setSelectedItem(switch(route.kind()){
            case RAIL -> "GRADE RAIL";
            case ROAD -> "GRADE ROAD";
            case RIVER -> "GRADE RIVER";
        });
        // The operation selection resets draft handles; restore the selected route.
        gradeControls.clear();gradeControls.addAll(route.controls());
        refresh("Loaded "+route.id()+" for manual control-point adjustment");
    }
    private void deleteRoute(){
        var route=selectedRoute();
        if(route==null)return;
        if(JOptionPane.showConfirmDialog(frame,
            "Remove saved alignment "+route.id()+"? Existing graded terrain will NOT be restored.",
            "Delete route record only",JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{
            routes.remove(route.id());routes.save();gradeControls.clear();
            refreshSavedRoutes();refresh("Removed route record "+route.id()+"; terrain layers unchanged");
        }catch(IOException ex){error(ex);}
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
            gradeControls.clear();refreshSavedRoutes();
            savedRoutes.setSelectedItem(alignment.id());
            int next=routes.list().size()+1;
            while(hasRoute("route-"+next))next++;
            routeName.setText("route-"+next);
            refresh("Applied and persisted "+alignment.kind()+" "+alignment.id());
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private boolean hasRoute(String id){
        for(var route:routes.list())if(route.id().equals(id))return true;
        return false;
    }
    private void refreshBrushPresets(){
        try{
            String selection=(String)presetNames.getSelectedItem();
            presetNames.removeAllItems();
            for(String name:presets.list())presetNames.addItem(name);
            if(selection!=null)presetNames.setSelectedItem(selection);
        }catch(IOException ex){error(ex);}
    }
    private void saveBrushPreset(){
        if(busy)return;
        String name=presetName.getText().trim();
        try{
            boolean existing=presets.list().contains(name);
            if(existing&&JOptionPane.showConfirmDialog(frame,
                "Replace brush preset "+name+"?","Replace brush preset",
                JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            presets.save(name,new TerrainBrushPresets.Preset(
                (TerrainLayerStack.Brush)brush.getSelectedItem(),
                precisionSettings(),brushAlpha),existing);
            refreshBrushPresets();presetNames.setSelectedItem(name);
            refresh("Saved self-contained brush preset: "+name);
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private void loadBrushPreset(){
        if(busy)return;
        String name=(String)presetNames.getSelectedItem();
        if(name==null)return;
        try{
            var p=presets.open(name);var settings=p.settings();
            if(settings.radius()>512||settings.strength()<.01
                ||settings.strength()>1000||settings.spacing()<.1
                ||settings.spacing()>128||settings.minElevation()<-1000000
                ||settings.maxElevation()>1000000)
                throw new IOException("Preset exceeds supported editor control ranges");
            brush.setSelectedItem(p.brush());radius.setValue(settings.radius());
            strength.setValue(settings.strength());target.setValue(settings.target());
            inner.setValue(settings.inner());falloff.setSelectedIndex(settings.curve());
            brushSpacing.setValue(settings.spacing());
            brushRotation.setValue(settings.rotationDegrees());
            minBrushHeight.setValue(settings.minElevation());
            maxBrushHeight.setValue(settings.maxElevation());
            maxBrushSlope.setValue(settings.maxSlopeDegrees());
            flattenLimit.setSelectedItem(settings.flattenLimit());
            brushAlpha=p.alpha();presetName.setText(name);
            refresh("Restored brush preset: "+name);
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private void deleteBrushPreset(){
        if(busy)return;
        String name=(String)presetNames.getSelectedItem();
        if(name==null)return;
        if(JOptionPane.showConfirmDialog(frame,"Delete saved brush preset "+name+"?",
            "Delete preset only",JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{
            presets.delete(name);refreshBrushPresets();
            refresh("Deleted brush preset record: "+name);
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private void loadBrushAlpha(){
        JFileChooser picker=new JFileChooser();
        if(picker.showOpenDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        try {
            BufferedImage image=ImageIO.read(picker.getSelectedFile());
            if(image==null)throw new IOException("Cannot read selected brush-alpha image");
            brushAlpha=TerrainBrushProfile.Alpha.fromImage(image);
            refresh("Custom brush alpha loaded (not yet saved as preset)");
        }catch(IOException|RuntimeException ex){error(ex);}
    }
    private TerrainBrushProfile.Settings precisionSettings(){
        return new TerrainBrushProfile.Settings(
            val(radius),val(strength),val(target),val(inner),
            falloff.getSelectedIndex(),val(brushSpacing),val(brushRotation),
            val(minBrushHeight),val(maxBrushHeight),val(maxBrushSlope),
            (TerrainLayerStack.FlattenLimit)flattenLimit.getSelectedItem());
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
        int bits=Math.min(16,raster.getSampleModel().getSampleSize(0));
        double fullScale=(1<<bits)-1.0;
        for(int z=0;z<h;z++)for(int x=0;x<w;x++){
            int sx=x*(stampImage.getWidth()-1)/(w-1),sz=z*(stampImage.getHeight()-1)/(h-1);
            result[z][x]=(float)(raster.getSample(sx,sz,0)/fullScale);
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
                    stack.brushPrecision((TerrainLayerStack.Brush)brush.getSelectedItem(),
                        x,z,precisionSettings(),brushAlpha);
                }
                case "PAINT MATERIAL" -> stack.paintMaterial(x,z,r,
                    (TerrainLayerStack.Material)material.getSelectedItem(),90);
                case "FREEZE","THAW" -> stack.freezeRect((int)(x-r),(int)(z-r),
                    (int)(x+r),(int)(z+r),mode.equals("FREEZE"));
                case "STAMP" -> {
                    if(pressed&&stampImage!=null)
                        stack.stamp(stamp(),x,z,r,val(strength));
                }
                case "GRADE RAIL","GRADE ROAD","GRADE RIVER" -> {
                    if(pressed){
                        draggedRouteControl=-1;
                        double gx=(long)stack.tileX()*stack.cells()+x;
                        double gz=(long)stack.tileZ()*stack.cells()+z;
                        double near=2.0;
                        for(int k=0;k<gradeControls.size();k++){
                            var p=gradeControls.get(k);
                            double d=Math.hypot(p.x()-gx,p.z()-gz);
                            if(d<near){near=d;draggedRouteControl=k;}
                        }
                        if(draggedRouteControl<0){
                            gradeControls.add(new TerrainLayerStack.Point(gx,gz,
                                stack.height(x,z)));
                            status.setText("Route controls: "+gradeControls.size()+
                                " — drag a control to move, or save alignment");
                        }else status.setText("Dragging route control "+
                            (draggedRouteControl+1)+" (elevation preserved)");
                    }
                }
            }
            if(!mode.equals("SCULPT")&&!mode.equals("PAINT MATERIAL"))stack.finishStroke();
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
                    if(busy)return;
                    if(SwingUtilities.isRightMouseButton(e)){
                        erosionCenterX=posX(e);erosionCenterZ=posZ(e);
                        refresh("Local erosion centered at "+erosionCenterX+","+erosionCenterZ);
                        return;
                    }
                    if(!SwingUtilities.isLeftMouseButton(e))return;
                    drawing=true;
                    int x=posX(e),z=posZ(e);
                    if("SCULPT".equals(action.getSelectedItem())){
                        try{
                            if(sampleFlatten.isSelected()
                                &&brush.getSelectedItem()==TerrainLayerStack.Brush.FLATTEN)
                                target.setValue(stack.height(x,z));
                            var profile=precisionSettings();
                            var alpha=brushAlpha;
                            var mode=(TerrainLayerStack.Brush)brush.getSelectedItem();
                            brushStroke=new TerrainBrushProfile.Stroke(profile.spacing(),
                                (px,pz)->stack.brushPrecision(mode,px,pz,profile,alpha));
                            brushStroke.begin(x,z);render();
                        }catch(RuntimeException ex){drawing=false;brushStroke=null;error(ex);}
                    }else applyAt(x,z,true);
                }
                @Override public void mouseDragged(MouseEvent e){
                    if(!drawing)return;
                    if("SCULPT".equals(action.getSelectedItem())&&brushStroke!=null){
                        try{brushStroke.move(posX(e),posZ(e));render();}
                        catch(RuntimeException ex){drawing=false;brushStroke.end();
                            brushStroke=null;stack.finishStroke();error(ex);}
                    }else if("PAINT MATERIAL".equals(action.getSelectedItem()))
                        applyAt(posX(e),posZ(e),false);
                    else if(draggedRouteControl>=0&&
                        ((String)action.getSelectedItem()).startsWith("GRADE ")){
                        var old=gradeControls.get(draggedRouteControl);
                        gradeControls.set(draggedRouteControl,
                            new TerrainLayerStack.Point(
                                (long)stack.tileX()*stack.cells()+posX(e),
                                (long)stack.tileZ()*stack.cells()+posZ(e),
                                old.elevation()));
                        render();
                    }
                }
                @Override public void mouseReleased(MouseEvent e){
                    if(drawing){
                        drawing=false;draggedRouteControl=-1;
                        if(brushStroke!=null){brushStroke.end();brushStroke=null;}
                        stack.finishStroke();refresh("Stroke completed");
                    }
                }
            };
            addMouseListener(mouse);addMouseMotionListener(mouse);
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(image!=null)g.drawImage(image,0,0,getWidth(),getHeight(),null);
            if(localizedErosion.isSelected()){
                int n=stack.cells();
                double r=val(radius);
                int x=(int)Math.round((erosionCenterX-r)*getWidth()/n);
                int y=(int)Math.round((erosionCenterZ-r)*getHeight()/n);
                int w=(int)Math.round(2*r*getWidth()/n);
                int h=(int)Math.round(2*r*getHeight()/n);
                g.setColor(new Color(250,179,67,170));
                g.drawRect(x,y,w,h);
            }
            // Stored alignments remain inspectable even when no draft is active.
            g.setColor(new Color(68,206,214,145));
            for(var route:routes.list()){
                boolean have=false;int px=0,py=0;
                for(var p:route.controls()){
                    int x=(int)Math.round((p.x()-(long)stack.tileX()*stack.cells())*
                        getWidth()/stack.cells());
                    int y=(int)Math.round((p.z()-(long)stack.tileZ()*stack.cells())*
                        getHeight()/stack.cells());
                    if(have)g.drawLine(px,py,x,y);
                    px=x;py=y;have=true;
                }
            }
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
