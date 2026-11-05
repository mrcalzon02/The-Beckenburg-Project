package org.beckenburg;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Standalone, real-renderer DEV ROOM (not a normal campaign/lobby level).
 * Unlimited inventory is arranged into reusable three-storey gallery wings:
 * four six-model bays per floor, 72 independent positions per wing.
 * A selected model can be viewed from any angle and captured as a real PNG.
 */
public final class DevAssetShowroom implements AutoCloseable {
    private static final int BAY=DevAssetGalleryLayout.SLOTS_PER_BAY;
    private final SceneRenderer renderer=new SceneRenderer();
    private final JFrame window=new JFrame("BECKENBURG | MODEL AND CHARACTER REVIEW ROOM");
    private final JPanel viewport=new View();
    private final DefaultListModel<DevAssetReviewCatalog.Entry> listModel=new DefaultListModel<>();
    private final JList<DevAssetReviewCatalog.Entry> catalog=new JList<>(listModel);
    private final JLabel status=new JLabel();
    private final JComboBox<String> type=new JComboBox<>(new String[]{
        "ALL","HUNTER","GHOST","CASTLE_NPC","EQUIPMENT","IMPORTED","WORLD_MODEL","SOURCE_ONLY"});
    private final JTextField search=new JTextField();
    private final JTextArea details=new JTextArea();
    private final Set<Integer> keys=new HashSet<>();
    private final javax.swing.Timer timer;
    private List<DevAssetReviewCatalog.Entry> all=List.of(),visible=List.of(),
        wing=List.of(),bay=List.of();
    private int page,floor,bayNumber,selected=-1,dragX,dragY;
    private boolean closed;
    private double clock,rotation;
    private DevTerrainTopologyLab topologyLab;
    private DevInfrastructureShowcase infrastructureShowcase;

    private DevAssetShowroom(){
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("Dev room requires a graphical desktop");
        renderer.x=0;renderer.z=-5;renderer.eyeHeight=1.7;renderer.yaw=0;
        renderer.pitch=-.06;renderer.lamp=false;
        renderer.setRenderSize(640,400);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setSize(1300,815);window.setMinimumSize(new Dimension(940,580));
        window.setLocationRelativeTo(null);
        viewport.setFocusable(true);viewport.setBackground(Color.BLACK);
        viewport.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){
                keys.add(e.getKeyCode());
                if(e.getKeyCode()>=KeyEvent.VK_1&&e.getKeyCode()<=KeyEvent.VK_6)
                    teleport(e.getKeyCode()-KeyEvent.VK_1);
                if(e.getKeyCode()==KeyEvent.VK_PAGE_DOWN)changePage(1);
                if(e.getKeyCode()==KeyEvent.VK_PAGE_UP)changePage(-1);
                if(e.getKeyCode()==KeyEvent.VK_F9)changeFloor(-1);
                if(e.getKeyCode()==KeyEvent.VK_F10)changeFloor(1);
                if(e.getKeyCode()==KeyEvent.VK_OPEN_BRACKET)changeWing(-1);
                if(e.getKeyCode()==KeyEvent.VK_CLOSE_BRACKET)changeWing(1);
                if(e.getKeyCode()==KeyEvent.VK_R)rotation+=Math.PI/8;
                if(e.getKeyCode()==KeyEvent.VK_F12)saveCurrentCapture();
            }
            @Override public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}
        });
        viewport.addFocusListener(new FocusAdapter(){
            @Override public void focusLost(FocusEvent e){keys.clear();}
        });
        viewport.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                dragX=e.getX();dragY=e.getY();viewport.requestFocusInWindow();
            }
        });
        viewport.addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                renderer.yaw+=Math.toRadians((e.getX()-dragX)*.25);
                renderer.pitch=Math.max(-1.3,Math.min(1.3,
                    renderer.pitch-Math.toRadians((e.getY()-dragY)*.25)));
                dragX=e.getX();dragY=e.getY();
            }
        });
        viewport.addMouseWheelListener(e->{
            double distance=-e.getPreciseWheelRotation()*.75;
            renderer.x+=Math.sin(renderer.yaw)*distance;
            renderer.z+=Math.cos(renderer.yaw)*distance;
        });
        JPanel controls=new JPanel(new BorderLayout(4,6));
        controls.setBorder(BorderFactory.createEmptyBorder(7,9,7,9));
        JPanel filter=new JPanel(new BorderLayout(4,4));
        filter.add(type,BorderLayout.WEST);filter.add(search,BorderLayout.CENTER);
        controls.add(filter,BorderLayout.NORTH);
        catalog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        catalog.setCellRenderer((list,value,index,chosen,focus)->{
            JLabel label=new JLabel(value.kind()+"  /  "+value.id());
            label.setOpaque(true);label.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            label.setBackground(chosen?new Color(50,83,104):new Color(24,27,34));
            label.setForeground(chosen?Color.WHITE:new Color(224,225,223));
            label.setBorder(BorderFactory.createEmptyBorder(5,6,5,4));
            return label;
        });
        catalog.addListSelectionListener(e->{
            if(!e.getValueIsAdjusting()&&catalog.getSelectedIndex()>=0){
                int i=catalog.getSelectedIndex();
                var address=DevAssetGalleryLayout.address(i);
                page=address.wing();floor=address.floor();bayNumber=address.bay();
                showPage();
                selected=address.slot();teleport(selected);inspect(visible.get(i));
            }
        });
        controls.add(new JScrollPane(catalog),BorderLayout.CENTER);
        details.setEditable(false);details.setLineWrap(true);details.setWrapStyleWord(true);
        details.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        details.setRows(8);
        controls.add(new JScrollPane(details),BorderLayout.SOUTH);
        controls.setPreferredSize(new Dimension(355,760));
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,5,3));
        JButton prev=new JButton("< Previous bay"),next=new JButton("Next bay >");
        JButton lower=new JButton("Floor -"),upper=new JButton("Floor +");
        JButton olderWing=new JButton("Wing -"),newerWing=new JButton("Wing +");
        JButton shot=new JButton("Save actual renderer PNG");
        JButton refresh=new JButton("Rescan registry");
        JButton inspect=new JButton("Inspect selected");
        JButton approve=new JButton("Mark looks correct");
        JButton needsWork=new JButton("Flag texture/model defect");
        JButton topologyButton=new JButton("Topology QA on/off");
        JButton topologyEdit=new JButton("Edit QA topology");
        JButton topologyReload=new JButton("Reload QA topology");
        JButton demo=new JButton("Infrastructure demo on/off");
        JButton demoWater=new JButton("Edit river snap/water...");
        JButton exportDemoWater=new JButton("Export demo water...");
        prev.addActionListener(e->changePage(-1));
        next.addActionListener(e->changePage(1));
        lower.addActionListener(e->changeFloor(-1));
        upper.addActionListener(e->changeFloor(1));
        olderWing.addActionListener(e->changeWing(-1));
        newerWing.addActionListener(e->changeWing(1));
        shot.addActionListener(e->saveCurrentCapture());
        refresh.addActionListener(e->reload());
        inspect.addActionListener(e->{
            if(catalog.getSelectedValue()!=null)inspect(catalog.getSelectedValue());
        });
        approve.addActionListener(e->recordManualReview("VISUAL_APPROVED"));
        needsWork.addActionListener(e->recordManualReview("NEEDS_REPAIR"));
        topologyButton.addActionListener(e->toggleTopology());
        topologyEdit.addActionListener(e->editTopology());
        topologyReload.addActionListener(e->reloadTopology());
        demo.addActionListener(e->toggleInfrastructureShowcase());
        demoWater.addActionListener(e->editInfrastructureWater());
        exportDemoWater.addActionListener(e->exportInfrastructureWater());
        for(JButton b:new JButton[]{olderWing,newerWing,lower,upper,prev,next,shot,refresh,inspect,approve,needsWork,
                                    topologyButton,topologyEdit,topologyReload,demo,demoWater,
                                    exportDemoWater})
            buttons.add(b);
        JPanel right=new JPanel(new BorderLayout());
        right.add(buttons,BorderLayout.NORTH);right.add(viewport,BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(6,12,7,12));
        right.add(status,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,controls,right);
        split.setResizeWeight(.26);
        window.add(split);
        type.addActionListener(e->filter());
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filter();}
            public void removeUpdate(DocumentEvent e){filter();}
            public void changedUpdate(DocumentEvent e){filter();}
        });
        window.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){close();}
        });
        reload();
        timer=new javax.swing.Timer(135,e->frame());
        timer.start();
        window.setVisible(true);
        viewport.requestFocusInWindow();
    }
    private void reload(){
        DevAssetReviewCatalog.invalidateSourceReviewCache();
        all=DevAssetReviewCatalog.entries();
        filter();
    }
    private void filter(){
        String kind=String.valueOf(type.getSelectedItem());
        String q=search.getText().trim().toLowerCase(Locale.ROOT);
        visible=all.stream().filter(e->kind.equals("ALL")||e.kind().name().equals(kind))
            .filter(e->q.isEmpty()||e.key().toLowerCase(Locale.ROOT).contains(q)).toList();
        listModel.clear();
        for(var e:visible)listModel.addElement(e);
        page=0;floor=0;bayNumber=0;selected=-1;catalog.clearSelection();showPage();
    }
    /** Next/previous physical bay; automatically continues onto another floor/wing. */
    private void changePage(int delta){
        if(visible.isEmpty())return;
        int capacity=DevAssetGalleryLayout.BAYS_PER_FLOOR*DevAssetGalleryLayout.FLOORS;
        int current=page*capacity+floor*DevAssetGalleryLayout.BAYS_PER_FLOOR+bayNumber;
        int max=(visible.size()-1)/BAY;
        int destination=Math.max(0,Math.min(max,current+delta));
        page=destination/capacity;
        floor=(destination%capacity)/DevAssetGalleryLayout.BAYS_PER_FLOOR;
        bayNumber=destination%DevAssetGalleryLayout.BAYS_PER_FLOOR;
        selected=-1;catalog.clearSelection();showPage();
    }
    private void changeFloor(int delta){
        if(visible.isEmpty())return;
        int destination=DevAssetGalleryLayout.clampFloor(floor+delta);
        if(destination*DevAssetGalleryLayout.SLOTS_PER_FLOOR>=wing.size())return;
        floor=destination;selected=-1;catalog.clearSelection();showPage();
    }
    private void changeWing(int delta){
        if(visible.isEmpty())return;
        int last=(visible.size()-1)/DevAssetGalleryLayout.SLOTS_PER_WING;
        page=Math.max(0,Math.min(last,page+delta));
        selected=-1;catalog.clearSelection();showPage();
    }
    private void showPage(){
        wing=DevAssetGalleryLayout.wing(visible,page);
        if(!wing.isEmpty()){
            int desired=floor*DevAssetGalleryLayout.SLOTS_PER_FLOOR+bayNumber*BAY;
            if(desired>=wing.size()){
                int lastBay=(wing.size()-1)/BAY;
                floor=lastBay/DevAssetGalleryLayout.BAYS_PER_FLOOR;
                bayNumber=lastBay%DevAssetGalleryLayout.BAYS_PER_FLOOR;
            }
        }
        bay=DevAssetGalleryLayout.bay(wing,floor,bayNumber);
        renderer.x=0;renderer.z=DevAssetGalleryLayout.bayEntranceZ(bayNumber);
        renderer.yaw=0;renderer.pitch=-.06;
        renderer.eyeHeight=DevAssetGalleryLayout.eyeY(floor);
        updateGalleryStatus();
    }
    private void updateGalleryStatus(){
        status.setText("DEV ROOM • "+visible.size()+" filtered • wing "+(page+1)+
            "/"+Math.max(1,(visible.size()+DevAssetGalleryLayout.SLOTS_PER_WING-1)/
                DevAssetGalleryLayout.SLOTS_PER_WING)+
            " • floor "+(floor+1)+"/"+DevAssetGalleryLayout.FLOORS+
            " • bay "+(bayNumber+1)+"/"+DevAssetGalleryLayout.BAYS_PER_FLOOR+
            " • PgUp/PgDn bays • F9/F10 floors • [ ] wings • 1–6 inspect • WASD/Q/E fly • R rotate • F12 PNG");
    }
    private void teleport(int slot){
        if(slot<0||slot>=bay.size())return;
        selected=slot;
        renderer.x=DevAssetGalleryLayout.standX(slot);
        renderer.z=DevAssetGalleryLayout.standZ(bayNumber,slot)-3.15;
        renderer.eyeHeight=DevAssetGalleryLayout.floorY(floor)+1.35;
        renderer.yaw=0;renderer.pitch=-.05;
        inspect(bay.get(slot));
        viewport.requestFocusInWindow();
    }
    private void inspect(DevAssetReviewCatalog.Entry asset){
        var a=DevAssetReviewCatalog.audit(asset);
        String context=asset.id().startsWith("level_blockout_")
            ?"SOURCE-ONLY PRIMITIVE STUDY / NOT GAMEPLAY APPROVED"
            :asset.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY
                ?"SOURCE-ONLY MODEL / NOT GAMEPLAY APPROVED"
                :"REAL RUNTIME ASSET";
        details.setText(context+"\n"+asset.key()+
            "\nDecoder: "+a.state()+
            "\nTriangles: "+a.triangles()+"; materials: "+a.materials()+
            "\nOriginal texture samples: "+a.textureSamples()+
            "\nBounds: "+a.minX()+","+a.minY()+","+a.minZ()+
            " -> "+a.maxX()+","+a.maxY()+","+a.maxZ()+
            "\n"+a.details()+
            "\n\nSTATUS IS DIAGNOSTIC ONLY. Inspect screenshot and source UVs before approving art.");
    }
    private void toggleInfrastructureShowcase(){
        if(infrastructureShowcase!=null){
            infrastructureShowcase=null;
            status.setText("DEV ROOM: infrastructure material review off.");
            return;
        }
        try {
            // The two demos occupy the same side aisle, not two different
            // development maps. Explicitly choose one rather than overlap.
            topologyLab=null;
            infrastructureShowcase=DevInfrastructureShowcase.create();
            renderer.x=8.2;renderer.z=.45;renderer.eyeHeight=2.1;
            renderer.yaw=0;renderer.pitch=-.23;
            var demo=infrastructureShowcase;
            status.setText("DEV INFRASTRUCTURE: "+demo.railFaces()+
                " rail faces, "+demo.roadFaces()+" road, "+demo.poleFaces()+
                " pole/wire, "+demo.riverFaces()+" banks, "+demo.waterFaces()+
                " snap-connected water faces; textured GLB trees/rocks");
            viewport.requestFocusInWindow();
        }catch(RuntimeException failure){
            infrastructureShowcase=null;
            status.setText("Cannot open infrastructure demo: "+failure.getMessage());
        }
    }
    private void editInfrastructureWater(){
        if(infrastructureShowcase==null){
            status.setText("Enable the infrastructure demo to edit river sockets.");
            return;
        }
        try{
            // This is the actual portable water editor and WaterCanvas,
            // connected to the same model as the runtime module. No fake UI.
            var existing=WorldWater.load("dev-infrastructure-demo");
            var initial=existing.placements().isEmpty()?
                infrastructureShowcase.water():existing;
            var model=new dev.mrcalzon.water.WaterEditorModel(initial);
            model.setTerrainSockets(DevInfrastructureShowcase.riverSockets());
            model.select(List.of("dev-river-downstream"));
            JFrame editor=new JFrame("DEV ROOM — RIVER MODULE AND TERRAIN SNAP");
            var panel=new dev.mrcalzon.water.WaterEditorPanel(model,
                ()->new dev.mrcalzon.water.WaterEditorPanel.Origin(8.1,.075,16.05),
                ()->{
                    try{
                        WorldWater.save(model.document());
                        var check=WorldWater.load("dev-infrastructure-demo");
                        if(!check.equals(model.document()))
                            throw new IOException("Water save/readback mismatch");
                        infrastructureShowcase=DevInfrastructureShowcase.create(check);
                        model.markSaved();
                        status.setText("River water module saved and DEV exhibit refreshed");
                    }catch(IOException failure){
                        throw new java.io.UncheckedIOException(failure);
                    }
                });
            var canvas=new dev.mrcalzon.water.WaterCanvas(model,
                panel::snappingEnabled,panel::angleSnappingEnabled,
                ()->.075);
            JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                canvas,panel);
            split.setResizeWeight(.70);
            editor.setContentPane(split);
            editor.setSize(1170,760);
            editor.setMinimumSize(new Dimension(780,490));
            editor.setLocationRelativeTo(window);
            editor.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            editor.setVisible(true);
            status.setText("Native water editor opened with visible river snap socket");
        }catch(RuntimeException failure){
            status.setText("Cannot open river water editor: "+failure.getMessage());
        }
    }
    private void exportInfrastructureWater(){
        if(infrastructureShowcase==null){
            status.setText("Enable the infrastructure demo before exporting its water.");
            return;
        }
        JFileChooser picker=new JFileChooser();
        picker.setDialogTitle("Export real water module document from DEV river snaps");
        picker.setSelectedFile(new java.io.File("dev-infrastructure-demo.water"));
        if(picker.showSaveDialog(window)!=JFileChooser.APPROVE_OPTION)return;
        Path selected=picker.getSelectedFile().toPath();
        if(Files.exists(selected)&&JOptionPane.showConfirmDialog(window,
                "Replace the selected water module document?",
                "Confirm water export",JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{
            infrastructureShowcase.exportWater(selected);
            status.setText("Water module exported and read back: "+selected);
        }catch(IOException|RuntimeException failure){
            status.setText("Water export failed: "+failure.getMessage());
        }
    }
    private void toggleTopology(){
        if(topologyLab!=null){
            topologyLab=null;
            status.setText("DEV ROOM: topology QA off; original showroom unchanged.");
            return;
        }
        try{
            infrastructureShowcase=null;
            topologyLab=DevTerrainTopologyLab.open();
            renderer.x=8.1;renderer.z=0.2;renderer.eyeHeight=1.45;
            renderer.yaw=0;renderer.pitch=-.26;
            var nav=topologyLab.navigation();
            status.setText("3D TOPOLOGY QA: "+topologyLab.splitCount()+
                " splits, "+topologyLab.cutoutCount()+" holes, "+
                topologyLab.triangles().size()+" triangles | NAV: "+
                (nav.passed()?nav.regions()+" regions, "+nav.connections()+
                    " edges, "+nav.routeSteps()+" route points":"FAILED "+nav.error())+
                " | Collision/LOD NOT connected to gameplay.");
            viewport.requestFocusInWindow();
        }catch(IOException|RuntimeException failure){
            status.setText("Topology QA unavailable: "+failure.getMessage());
        }
    }
    private void editTopology(){
        if(topologyLab==null){
            status.setText("Enable Topology QA to create/open isolated test source world first.");
            return;
        }
        try{
            JFrame editor=org.beckenburg.editor.AdaptiveTopologyEditor.open(
                window,topologyLab.source(),0,0);
            editor.addWindowListener(new WindowAdapter(){
                @Override public void windowClosed(WindowEvent e){reloadTopology();}
            });
        }catch(IOException|RuntimeException failure){
            status.setText("Cannot open topology editor: "+failure.getMessage());
        }
    }
    private void reloadTopology(){
        if(topologyLab==null){status.setText("Topology QA is off.");return;}
        try{
            topologyLab.refresh();
            var nav=topologyLab.navigation();
            status.setText("QA readback: "+topologyLab.splitCount()+" splits, "+
                topologyLab.cutoutCount()+" holes, "+topologyLab.triangles().size()+
                " triangles | NAV: "+(nav.passed()?nav.regions()+" regions, "+
                nav.connections()+" edges, "+nav.routeSteps()+" route points":
                "FAILED "+nav.error())+
                "; NO game collision/nav adapter yet.");
        }catch(IOException|RuntimeException failure){
            status.setText("Topology readback rejected; previous QA mesh retained: "+
                failure.getMessage());
        }
    }
    private void frame(){
        if(closed)return;
        double step=keys.contains(KeyEvent.VK_SHIFT)?.7:.27;
        if(keys.contains(KeyEvent.VK_W)){renderer.x+=Math.sin(renderer.yaw)*step;renderer.z+=Math.cos(renderer.yaw)*step;}
        if(keys.contains(KeyEvent.VK_S)){renderer.x-=Math.sin(renderer.yaw)*step;renderer.z-=Math.cos(renderer.yaw)*step;}
        if(keys.contains(KeyEvent.VK_A)){renderer.x-=Math.cos(renderer.yaw)*step;renderer.z+=Math.sin(renderer.yaw)*step;}
        if(keys.contains(KeyEvent.VK_D)){renderer.x+=Math.cos(renderer.yaw)*step;renderer.z-=Math.sin(renderer.yaw)*step;}
        if(keys.contains(KeyEvent.VK_Q))renderer.eyeHeight-=step;
        if(keys.contains(KeyEvent.VK_E))renderer.eyeHeight+=step;
        if(keys.contains(KeyEvent.VK_LEFT))renderer.yaw-=.065;
        if(keys.contains(KeyEvent.VK_RIGHT))renderer.yaw+=.065;
        try{
            clock+=.06;
            if(topologyLab!=null||infrastructureShowcase!=null){
                // Preserve the historical side-aisle test geometry and its
                // existing six-stand renderer/capture semantics.
                renderer.drawDevAssetRoom(bay,clock,rotation,
                    topologyLab!=null?topologyLab.triangles():
                        infrastructureShowcase.triangles(),
                    infrastructureShowcase==null?List.of():infrastructureShowcase.props());
            }else{
                int cameraFloor=DevAssetGalleryLayout.floorAtEye(renderer.eyeHeight);
                int cameraBay=DevAssetGalleryLayout.bayAtZ(renderer.z);
                int start=cameraFloor*DevAssetGalleryLayout.SLOTS_PER_FLOOR+
                    cameraBay*DevAssetGalleryLayout.SLOTS_PER_BAY;
                if(start<wing.size()&&(floor!=cameraFloor||bayNumber!=cameraBay)){
                    floor=cameraFloor;bayNumber=cameraBay;selected=-1;
                    catalog.clearSelection();
                    bay=DevAssetGalleryLayout.bay(wing,floor,bayNumber);
                    updateGalleryStatus();
                }
                renderer.drawDevAssetGallery(wing,clock,rotation);
            }
        }
        catch(RuntimeException problem){
            details.setText("DEV ROOM RENDER FAILURE\n"+problem);
            timer.stop();BeckenburgDiagnostics.fail("Developer asset review",problem);
            return;
        }
        viewport.repaint();
    }
    private final class View extends JPanel{
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            g.drawImage(renderer.image(),0,0,getWidth(),getHeight(),null);
            Graphics2D d=(Graphics2D)g.create();
            d.setColor(new Color(0,0,0,165));
            d.fillRect(8,8,Math.min(getWidth()-16,570),Math.min(getHeight()-16,115));
            d.setColor(Color.WHITE);d.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
            d.drawString("ISOLATED 3D DEV GALLERY / WING "+(page+1)+
                " / FLOOR "+(floor+1)+" / BAY "+(bayNumber+1),18,29);
            for(int i=0;i<bay.size();i++){
                var item=bay.get(i);
                String name=(i+1)+" "+item.id();
                if(name.length()>53)name=name.substring(0,50)+"...";
                d.setColor(i==selected?new Color(255,222,138):new Color(220,226,220));
                d.drawString(name,18,48+i*12);
            }
            d.dispose();
        }
    }
    /** Human signoff is stored separately from automatically checked-in source art. */
    private void recordManualReview(String verdict){
        // Manual review follows the *visible selected stand*, never an
        // old selection from another physical floor or catalog page.
        DevAssetReviewCatalog.Entry e=selected>=0&&selected<bay.size()
            ?bay.get(selected):null;
        if(e==null){status.setText("Select a current gallery stand (1–6) first");return;}
        // A deliberately unfinished graybox may be reviewed for geometry,
        // but must never be logged as visually production-approved.
        if(e.id().startsWith("level_blockout_")&&verdict.equals("VISUAL_APPROVED"))
            verdict="GRAYBOX_REVIEWED";
        String comment=JOptionPane.showInputDialog(window,
            verdict.equals("NEEDS_REPAIR")?"Describe the visible issue (UV/texture/sprite/model):":
                verdict.equals("GRAYBOX_REVIEWED")
                    ?"Graybox proportions/placement review note (not production approval):"
                    :"Visual review note (original source fidelity verified):",
            "DEV ROOM / "+e.key(),JOptionPane.PLAIN_MESSAGE);
        if(comment==null)return;
        try{
            Path file=outputRoot().getParent().resolve("manual-review.csv");
            Files.createDirectories(file.getParent());
            if(!Files.exists(file))Files.writeString(file,
                "time,asset,decision,comment\n",StandardCharsets.UTF_8);
            Files.writeString(file,csv(java.time.Instant.now().toString())+","+
                csv(e.key())+","+verdict+","+csv(comment)+"\n",
                StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.APPEND);
            status.setText("Review decision saved for "+e.key()+" → "+file);
        }catch(IOException failure){status.setText("Could not save review: "+failure.getMessage());}
    }
    private static Path outputRoot(){
        return Path.of(System.getProperty("user.home"),".beckenburg","dev-room","captures");
    }
    private void saveCurrentCapture(){
        try{
            Path folder=outputRoot();Files.createDirectories(folder);
            String asset=selected>=0&&selected<bay.size()
                ?bay.get(selected).key().replace('/','_')
                :"wing_"+page+"_floor_"+floor+"_bay_"+bayNumber;
            Path file=folder.resolve(asset+"-"+System.currentTimeMillis()+".png");
            ImageIO.write(renderer.image(),"png",file.toFile());
            status.setText("ACTUAL RENDERER CAPTURE SAVED: "+file);
        }catch(IOException error){status.setText("Capture error: "+error);}
    }
    @Override public void close(){
        if(closed)return;closed=true;
        timer.stop();
        if(window.isDisplayable())window.dispose();
    }
    /**
     * Headless development-review workflow: enumerate complete current library,
     * save metadata and actual renderer screenshots. Missing/flat visuals are
     * findings and must NOT gate release. Source files are never rewritten.
     */
    public static void capture(Path folder,int maxEntries,String focus)throws IOException {
        Files.createDirectories(folder);
        List<DevAssetReviewCatalog.Entry> entries=DevAssetReviewCatalog.entries();
        if(maxEntries>0)entries=entries.subList(0,Math.min(maxEntries,entries.size()));
        // An explicit focus capture must not decode all ~1,000 unrelated
        // models or waste time generating whole-catalog art diagnostics.
        if(focus!=null){
            String requested=focus;
            entries=entries.stream().filter(e->e.id().equals(requested)
                ||e.key().equalsIgnoreCase(requested)).toList();
            if(entries.isEmpty())throw new IOException("Requested review asset not registered: "+requested);
        }
        StringBuilder report=new StringBuilder(
            "kind,id,state,triangles,materials,textureSamples,minX,minY,minZ,maxX,maxY,maxZ,details\n");
        int loaded=0,review=0,missing=0;
        for(var e:entries){
            var a=DevAssetReviewCatalog.audit(e);
            if(a.state().equals("MISSING"))missing++;
            else if(!a.state().equals("LOADED"))review++;
            else loaded++;
            report.append(e.kind()).append(',').append(csv(e.id())).append(',')
                .append(a.state()).append(',').append(a.triangles()).append(',')
                .append(a.materials()).append(',').append(a.textureSamples()).append(',')
                .append(a.minX()).append(',').append(a.minY()).append(',')
                .append(a.minZ()).append(',').append(a.maxX()).append(',')
                .append(a.maxY()).append(',').append(a.maxZ()).append(',')
                .append(csv(a.details())).append('\n');
        }
        // Inventory existence checks do not require changing the game registry.
        // A loose imported model that is not approved is flagged, not inserted
        // into the normal game's allowlist by an automated tool.
        Path source=Path.of("assets","runtime","models");
        if(Files.isDirectory(source))try(var paths=Files.list(source)){
            Set<String> known=new HashSet<>();
            for(var e:entries)if(!e.sprite())known.add(e.id());
            for(Path mesh:paths.filter(p->p.getFileName().toString().endsWith(".glb")).toList()){
                String id=mesh.getFileName().toString().replaceFirst("\\.glb$","");
                if(!known.contains(id)){
                    review++;
                    report.append("UNREGISTERED,").append(csv(id))
                        .append(",REVIEW_REGISTRATION,0,0,0,0,0,0,0,0,0,")
                        .append(csv("Packaged source not in GlbProps.availableModels()")).append('\n');
                }
            }
        }
        Files.writeString(folder.resolve("inventory.csv"),report,StandardCharsets.UTF_8);
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(640,400);
        scene.lamp=false;
        int imageCount=0;
        for(int start=0;start<entries.size()&&focus==null;start+=BAY){
            var exhibits=entries.subList(start,Math.min(start+BAY,entries.size()));
            scene.x=0;scene.z=-5;scene.eyeHeight=1.7;scene.yaw=0;scene.pitch=-.06;
            scene.drawDevAssetRoom(exhibits,0,0);
            // Contact-sheet caption lives BELOW the unmodified real renderer
            // frame. Never paint labels over imported textures/silhouettes.
            BufferedImage sheet=new BufferedImage(640,493,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();
            try{
                g.setColor(new Color(16,18,24));g.fillRect(0,0,640,493);
                g.drawImage(scene.image(),0,0,null);
                g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
                for(int i=0;i<exhibits.size();i++){
                    var e=exhibits.get(i);
                    String label=(start+i)+" / "+e.kind()+" / "+e.id();
                    if(label.length()>30)label=label.substring(0,27)+"...";
                    g.setColor(Color.WHITE);
                    g.drawString(label,10+(i%3)*213,424+(i/3)*25);
                }
            }finally{g.dispose();}
            Path image=folder.resolve(String.format("bay-%04d.png",start/BAY));
            ImageIO.write(sheet,"png",image.toFile());imageCount++;
        }
        // An actual close-range 640x400 shot for EVERY exhibit makes the
        // gallery useful for broken UVs, not just distant silhouettes.
        if(focus==null)for(int i=0;i<entries.size();i++){
            var e=entries.get(i);
            scene.x=SceneRenderer.devAssetX(0);
            scene.z=SceneRenderer.devAssetZ(0)-3.15;
            scene.eyeHeight=1.30;scene.yaw=0;scene.pitch=-.06;
            scene.drawDevAssetRoom(List.of(e),i*.03,0);
            Path filename=folder.resolve(String.format("asset-%04d.png",i));
            ImageIO.write(scene.image(),"png",filename.toFile());
            imageCount++;
        }
        // Owner-flagged assets get extra angles in the ACTUAL renderer.
        // Stable KIND/id keys survive catalog insertion and sorting changes;
        // numeric asset-#### PNG ordinals do not. Cosmetic QA is advisory.
        if(focus==null){
            Path reviewFile=Path.of("development","assets","texture-review-focus.txt");
            if(Files.isRegularFile(reviewFile)){
                Set<String> flagged=new HashSet<>();
                for(String line:Files.readAllLines(reviewFile,StandardCharsets.UTF_8)){
                    String key=line.trim();
                    if(!key.isEmpty()&&!key.startsWith("#"))flagged.add(key);
                }
                int reviewed=0;
                for(var e:entries)if(flagged.contains(e.key())){
                    for(int angle=0;angle<4;angle++){
                        scene.x=SceneRenderer.devAssetX(0);
                        scene.z=SceneRenderer.devAssetZ(0)-3.4;
                        scene.eyeHeight=1.35;scene.yaw=0;scene.pitch=-.08;
                        scene.drawDevAssetRoom(List.of(e),angle*.12,angle*Math.PI/2);
                        Path out=folder.resolve("review-"+e.kind()+"-"+e.id()+"-"+angle+".png");
                        ImageIO.write(scene.image(),"png",out.toFile());
                        imageCount++;
                        if(e.sprite()){
                            // The standard 3.4 m review camera makes faces
                            // less than a few dozen pixels high. Preserve
                            // that baseline, and ALSO capture close real
                            // render views for source/alpha defect inspection.
                            scene.z=SceneRenderer.devAssetZ(0)-2.25;
                            scene.eyeHeight=1.75;scene.pitch=-.08;
                            scene.drawDevAssetRoom(List.of(e),angle*.12,angle*Math.PI/2);
                            ImageIO.write(scene.image(),"png",
                                folder.resolve("review-close-"+e.kind()+"-"+e.id()+
                                    "-"+angle+".png").toFile());
                            imageCount++;
                        }
                    }
                    reviewed++;
                }
                Files.writeString(folder.resolve("flagged-review-summary.txt"),
                    "source="+reviewFile+"\nrequested="+flagged.size()+
                    "\nmatched="+reviewed+"\nanglesPerAsset=4\n"+
                    "Rendered images are visual evidence, not automatic texture-art approval.\n",
                    StandardCharsets.UTF_8);
            }
        }
        if(focus!=null){
            DevAssetReviewCatalog.Entry chosen=null;
            for(var e:entries)if(e.id().equals(focus)||e.key().equalsIgnoreCase(focus)){
                chosen=e;break;
            }
            if(chosen==null)throw new IOException("Requested review asset not registered: "+focus);
            for(int angle=0;angle<4;angle++){
                scene.x=SceneRenderer.devAssetX(0);
                scene.z=SceneRenderer.devAssetZ(0)-3.4;
                scene.eyeHeight=1.35;scene.yaw=0;scene.pitch=-.08;
                scene.drawDevAssetRoom(List.of(chosen),angle*.12,angle*Math.PI/2);
                ImageIO.write(scene.image(),"png",
                    folder.resolve("focused-"+chosen.kind()+"-"+chosen.id()+"-"+angle+".png").toFile());
                imageCount++;
                if(chosen.sprite()){
                    scene.z=SceneRenderer.devAssetZ(0)-2.25;
                    scene.eyeHeight=1.75;scene.pitch=-.08;
                    scene.drawDevAssetRoom(List.of(chosen),angle*.12,angle*Math.PI/2);
                    ImageIO.write(scene.image(),"png",
                        folder.resolve("focused-close-"+chosen.kind()+"-"+
                            chosen.id()+"-"+angle+".png").toFile());
                    imageCount++;
                }
            }
        }
        String status="Dev room: "+entries.size()+" registered assets, "+loaded+
            " loaded, "+review+" needs review, "+missing+" missing; "+imageCount+
            " actual renderer PNGs (bay overviews + individual close-ups).\n"+
            "Important: LOADED means decodable and sampled, NOT visually approved.\n"+
            "Missing/flat/UV and registry warnings are diagnostic; no cosmetic release gate.\n";
        Files.writeString(folder.resolve("review-summary.txt"),status,StandardCharsets.UTF_8);
        System.out.println(status);
    }
    /**
     * Reusable headless inspection of an actual multi-floor wing. Filters stay
     * outside the runtime approval registry and only select showroom entries.
     * Each screenshot is drawn by the same multi-storey SceneRenderer method
     * used by the interactive room, not a generated model illustration.
     */
    public static void captureGallery(Path folder,int maxEntries,String kind,
                                      String query,int wingNumber)throws IOException{
        if(wingNumber<0||wingNumber>999)
            throw new IllegalArgumentException("Gallery wing index must be 0..999");
        Files.createDirectories(folder);
        List<DevAssetReviewCatalog.Entry> candidates=DevAssetReviewCatalog.entries();
        if(kind!=null){
            String requested=kind.toUpperCase(Locale.ROOT);
            if(!Set.of("ALL","HUNTER","GHOST","CASTLE_NPC","EQUIPMENT",
                "IMPORTED","WORLD_MODEL","SOURCE_ONLY").contains(requested))
                throw new IllegalArgumentException("Unknown gallery kind: "+kind);
            if(!requested.equals("ALL"))
                candidates=candidates.stream()
                    .filter(e->e.kind().name().equals(requested)).toList();
        }
        if(query!=null&&!query.isBlank()){
            String needle=query.toLowerCase(Locale.ROOT);
            candidates=candidates.stream().filter(e->e.key().toLowerCase(Locale.ROOT)
                .contains(needle)).toList();
        }
        if(maxEntries>0)
            candidates=candidates.subList(0,Math.min(maxEntries,candidates.size()));
        List<DevAssetReviewCatalog.Entry> wing=DevAssetGalleryLayout.wing(candidates,wingNumber);
        if(wing.isEmpty())
            throw new IOException("No matching models for gallery wing "+wingNumber);
        StringBuilder locations=new StringBuilder(
            "kind,id,wing,floor,bay,slot,worldX,worldY,worldZ,reviewOnly\n");
        for(int i=0;i<wing.size();i++){
            var item=wing.get(i);
            var address=DevAssetGalleryLayout.address(i);
            locations.append(item.kind()).append(',').append(csv(item.id())).append(',')
                .append(wingNumber).append(',').append(address.floor()+1).append(',')
                .append(address.bay()+1).append(',').append(address.slot()+1).append(',')
                .append(DevAssetGalleryLayout.standX(address.slot())).append(',')
                .append(DevAssetGalleryLayout.floorY(address.floor())).append(',')
                .append(DevAssetGalleryLayout.standZ(address.bay(),address.slot()))
                .append(',').append(item.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY)
                .append('\n');
        }
        Files.writeString(folder.resolve("gallery-placements.csv"),locations,
            StandardCharsets.UTF_8);
        // These diagnostics use the native decoder, not source-file presence.
        // A red error plinth may still produce pixels, so no gallery may call
        // such a stand a successfully decoded model.
        int failedModels=0;
        StringBuilder audits=new StringBuilder(
            "kind,id,state,triangles,materials,textureSamples,sourceOnly\n");
        for(var member:wing){
            var audit=DevAssetReviewCatalog.audit(member);
            audits.append(member.kind()).append(',').append(csv(member.id()))
                .append(',').append(audit.state()).append(',')
                .append(audit.triangles()).append(',').append(audit.materials())
                .append(',').append(audit.textureSamples()).append(',')
                .append(member.kind()==DevAssetReviewCatalog.Kind.SOURCE_ONLY)
                .append('\n');
            if(!audit.loaded()||audit.triangles()==0&&!member.sprite())
                failedModels++;
        }
        Files.writeString(folder.resolve("gallery-model-audits.csv"),audits,
            StandardCharsets.UTF_8);
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(640,400);scene.lamp=false;
        int images=0,failedBays=0;
        StringBuilder visibility=new StringBuilder(
            "wing,floor,bay,stands,changedPixels,visible\n");
        for(int floor=0;floor<DevAssetGalleryLayout.FLOORS;floor++)
            for(int bayIndex=0;bayIndex<DevAssetGalleryLayout.BAYS_PER_FLOOR;bayIndex++){
                List<DevAssetReviewCatalog.Entry> members=
                    DevAssetGalleryLayout.bay(wing,floor,bayIndex);
                if(members.isEmpty())continue;
                scene.x=0;scene.z=DevAssetGalleryLayout.bayEntranceZ(bayIndex);
                scene.eyeHeight=DevAssetGalleryLayout.eyeY(floor);
                scene.yaw=0;scene.pitch=-.06;
                // A full scene capture can contain plinths and walls but no
                // actual models. Compare the SAME camera with empty staging
                // to verify a distinguishable model silhouette exists.
                scene.drawDevAssetGallery(List.of(),0,0);
                int[] baseline=scene.pixels().clone();
                scene.drawDevAssetGallery(wing,0,0);
                int changed=0;
                int[] visiblePixels=scene.pixels();
                for(int n=0;n<baseline.length;n++)
                    if(baseline[n]!=visiblePixels[n])changed++;
                if(changed<=20)failedBays++;
                visibility.append(wingNumber+1).append(',').append(floor+1)
                    .append(',').append(bayIndex+1).append(',')
                    .append(members.size()).append(',').append(changed)
                    .append(',').append(changed>20).append('\n');
                // Caption below the actual unmodified render so it never
                // conceals subtle UV seams or ground-contact problems.
                BufferedImage sheet=new BufferedImage(640,490,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=sheet.createGraphics();
                try{
                    g.setColor(new Color(16,18,24));g.fillRect(0,0,640,490);
                    g.drawImage(scene.image(),0,0,null);
                    g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
                    g.drawString("SOURCE REVIEW WING "+(wingNumber+1)+
                        " / FLOOR "+(floor+1)+" / BAY "+(bayIndex+1)+
                        " (NOT GAMEPLAY APPROVED)",12,416);
                    for(int slot=0;slot<members.size();slot++){
                        var item=members.get(slot);
                        String label=(slot+1)+" "+item.id();
                        if(label.length()>33)label=label.substring(0,30)+"...";
                        g.drawString(label,10+(slot%3)*213,440+(slot/3)*20);
                    }
                }finally{g.dispose();}
                Path image=folder.resolve(String.format(Locale.ROOT,
                    "gallery-wing-%03d-floor-%02d-bay-%02d.png",
                    wingNumber+1,floor+1,bayIndex+1));
                if(!ImageIO.write(sheet,"png",image.toFile()))
                    throw new IOException("PNG capture writer unavailable");
                images++;
            }
        Files.writeString(folder.resolve("gallery-visibility.csv"),visibility,
            StandardCharsets.UTF_8);
        String status="renderer=SceneRenderer.drawDevAssetGallery\n"+
            "mode=3 real floor decks / 4 bays each / 6 individually reviewable stands\n"+
            "wing="+(wingNumber+1)+"\nmodels="+wing.size()+
            "\nimages="+images+"\nundecodableModels="+failedModels+
            "\ninvisibleBays="+failedBays+"\nsourceOnlyNotGameApproved=true\n"+
            "Models shown on each floor are selected by world camera altitude; "+
            "captures are source-review evidence, not gameplay approval.\n";
        Files.writeString(folder.resolve("gallery-summary.txt"),status,StandardCharsets.UTF_8);
        System.out.print(status);
        // This is a DEV inspection regression, never a game-release blocker.
        // Keep the PNGs and CSVs for diagnosis even when a bay is unreadable.
        if(failedModels>0||failedBays>0)
            throw new IOException("DEV gallery model visibility failed: "+
                failedModels+" undecodable models, "+failedBays+
                " empty/invisible bays; see gallery CSV and PNG artifact");
    }

    /** Real renderer capture of the opt-in topology source patch, not concept art. */
    public static void captureTopology(Path folder)throws IOException {
        DevTerrainTopologyLab lab=DevTerrainTopologyLab.open();
        Files.createDirectories(folder);
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(640,400);
        scene.x=8.1;scene.z=.2;scene.eyeHeight=1.45;scene.yaw=0;
        scene.pitch=-.26;scene.lamp=false;
        scene.drawDevAssetRoom(List.of(),0,0);
        int[] ordinaryRoom=scene.pixels().clone();
        scene.drawDevAssetRoom(List.of(),0,0,lab.triangles());
        int changedPixels=0;
        int[] actual=scene.pixels();
        for(int i=0;i<actual.length;i++)if(actual[i]!=ordinaryRoom[i])changedPixels++;
        if(changedPixels<40)
            throw new IOException("Topology QA patch not visible in real scene renderer: "+changedPixels+" changed pixels");
        Path image=folder.resolve("terrain-topology-devroom-real-render.png");
        if(!ImageIO.write(scene.image(),"png",image.toFile()))
            throw new IOException("No real PNG renderer encoder available");
        Files.writeString(folder.resolve("terrain-topology-devroom-report.txt"),
            "source="+lab.source().directory()+"\n"+
            "splits="+lab.splitCount()+"\n"+
            "actualSourceDerivedTriangles="+lab.triangles().size()+"\n"+
            "cutoutLeaves="+lab.cutoutCount()+"\n"+
            "navigationRegions="+lab.navigation().regions()+"\n"+
            "navigationEdges="+lab.navigation().connections()+"\n"+
            "navigationRoutePoints="+lab.navigation().routeSteps()+"\n"+
            "navigationDiagnostic="+(lab.navigation().passed()?"PASS":lab.navigation().error())+"\n"+
            "changedPixelsVersusOrdinaryStudio="+changedPixels+"\n"+
            "renderer=SceneRenderer.drawDevAssetRoom\n"+
            "status=DEV QA patch only; gameplay collision/LOD not wired\n",
            StandardCharsets.UTF_8);
        System.out.println("Rendered actual DEV ROOM terrain QA PNG: "+image);
    }
    /**
     * Real screenshots from the EXISTING showroom renderer, using the same
     * import pipeline as the six-object asset bays. Not a separate level and
     * not an invented water shader. Exported WaterDocument is the same native
     * module consumed by WorldWater.
     */
    public static void captureInfrastructure(Path folder)throws IOException {
        Files.createDirectories(folder);
        var demo=DevInfrastructureShowcase.create();
        if(demo.railFaces()<=0||demo.roadFaces()<=0||demo.poleFaces()<=0||
           demo.riverFaces()<=0||demo.waterFaces()<=0||demo.props().size()<5)
            throw new IOException("DEV infrastructure scene is missing real geometry");
        Path nativeWater=demo.exportWater(folder.resolve("dev-infrastructure-demo.water"));
        // Instantiate through the actual game adapter; failure is reported,
        // not hidden behind a painted water rectangle.
        try{
            var module=WorldWater.build(demo.water());
            if(module.bodies().size()!=2)
                throw new IOException("Actual water module did not contain both snapped bodies");
        }catch(RuntimeException invalid){
            throw new IOException("DEV river does not build in actual water module",invalid);
        }
        SceneRenderer scene=new SceneRenderer();
        scene.setRenderSize(640,400);scene.lamp=false;
        double[][] shots={
            {8.15,2.0,.4,0,-.22},
            {8.1,1.8,7.25,0,-.25},
            {8.1,2.0,12.15,0,-.27}
        };
        int totalChanged=0;
        for(int i=0;i<shots.length;i++){
            double[] view=shots[i];
            scene.x=view[0];scene.eyeHeight=view[1];scene.z=view[2];
            scene.yaw=view[3];scene.pitch=view[4];
            scene.drawDevAssetRoom(List.of(),i*.02,0);
            int[] empty=scene.pixels().clone();
            scene.drawDevAssetRoom(List.of(),i*.02,0,demo.triangles(),demo.props());
            int altered=0;int[] actual=scene.pixels();
            for(int pixel=0;pixel<actual.length;pixel++)
                if(actual[pixel]!=empty[pixel])altered++;
            totalChanged+=altered;
            Path file=folder.resolve(String.format("dev-infrastructure-actual-%02d.png",i));
            if(!ImageIO.write(scene.image(),"png",file.toFile()))
                throw new IOException("Actual renderer PNG encoding unavailable");
        }
        if(totalChanged<100)
            throw new IOException("Infrastructure geometry invisible in developer renderer: "+
                totalChanged+" changed pixels");
        Files.writeString(folder.resolve("dev-infrastructure-report.txt"),
            "level=EXISTING DEV ASSET SHOWROOM\\n"+
            "renderer=SceneRenderer.drawDevAssetRoom\\n"+
            "railFaces="+demo.railFaces()+"\\n"+
            "roadFaces="+demo.roadFaces()+"\\n"+
            "telephonePoleAndWireFaces="+demo.poleFaces()+"\\n"+
            "riverBankFaces="+demo.riverFaces()+"\\n"+
            "waterFootprintFaces="+demo.waterFaces()+"\\n"+
            "importedOriginalGLBProps="+demo.props().size()+"\\n"+
            "realWaterModuleDocument="+nativeWater+"\\n"+
            "snapModule=WaterTerrainSnap+WaterSnap\\n"+
            "actualChangedPixels="+totalChanged+"\\n"+
            "note=Water polygons are a footprint diagnostic, not the game's animated water shader\\n"+
            "note=Gameplay world physics/nav adapters not claimed by this DEV exhibit\\n",
            StandardCharsets.UTF_8);
        System.out.println("Captured DEV infrastructure via actual SceneRenderer: "+
            totalChanged+" changed pixels in "+folder);
    }
    private static String csv(String value){return "\""+value.replace("\"","\"\"").replace("\n"," ")+"\"";}
    public static void main(String[] args)throws Exception{
        boolean selected=false,capture=false,galleryCapture=false,
            terrainCapture=false,infrastructureCapture=false;
        int limit=0,wingNumber=0;Path output=Path.of("build","dev-room");
        String focus=null,kind=null,query=null;
        for(String arg:args){
            if(arg.equals("--dev-room"))selected=true;
            else if(arg.equals("--capture"))capture=true;
            else if(arg.equals("--gallery-capture"))galleryCapture=true;
            else if(arg.equals("--terrain-qa-capture"))terrainCapture=true;
            else if(arg.equals("--infrastructure-demo-capture"))infrastructureCapture=true;
            else if(arg.startsWith("--limit="))limit=Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--output="))output=Path.of(arg.substring(9));
            else if(arg.startsWith("--asset="))focus=arg.substring(8);
            else if(arg.startsWith("--kind="))kind=arg.substring(7);
            else if(arg.startsWith("--filter="))query=arg.substring(9);
            else if(arg.startsWith("--wing="))wingNumber=Integer.parseInt(arg.substring(7));
            else throw new IllegalArgumentException("Unknown dev-room option "+arg);
        }
        if(!selected)throw new IllegalArgumentException("Explicit --dev-room required");
        if(limit<0||limit>10000)throw new IllegalArgumentException("Invalid review limit");
        if((capture?1:0)+(galleryCapture?1:0)+(terrainCapture?1:0)+
            (infrastructureCapture?1:0)>1)
            throw new IllegalArgumentException("Choose only one DEV room capture mode");
        if((kind!=null||query!=null||wingNumber!=0)&&!galleryCapture)
            throw new IllegalArgumentException("--kind, --filter and --wing require --gallery-capture");
        if(galleryCapture){captureGallery(output,limit,kind,query,wingNumber);return;}
        if(infrastructureCapture){captureInfrastructure(output);return;}
        if(terrainCapture){captureTopology(output);return;}
        if(capture){capture(output,limit,focus);return;}
        if(GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("Interactive dev room requires a desktop; use --capture for headless");
        SwingUtilities.invokeLater(DevAssetShowroom::new);
    }
}
