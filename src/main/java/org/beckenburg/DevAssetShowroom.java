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
 * Unlimited inventory is arranged into paged six-stand rooms. A selected prop
 * can be viewed at any angle from a free-flight camera and captured as PNG.
 */
public final class DevAssetShowroom implements AutoCloseable {
    private static final int BAY=6;
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
    private List<DevAssetReviewCatalog.Entry> all=List.of(),visible=List.of(),bay=List.of();
    private int page,selected=-1,dragX,dragY;
    private boolean closed;
    private double clock,rotation;

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
                int i=catalog.getSelectedIndex();page=i/BAY;showPage();
                selected=i%BAY;teleport(selected);inspect(visible.get(i));
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
        JButton shot=new JButton("Save actual renderer PNG");
        JButton refresh=new JButton("Rescan registry");
        JButton inspect=new JButton("Inspect selected");
        JButton approve=new JButton("Mark looks correct");
        JButton needsWork=new JButton("Flag texture/model defect");
        prev.addActionListener(e->changePage(-1));
        next.addActionListener(e->changePage(1));
        shot.addActionListener(e->saveCurrentCapture());
        refresh.addActionListener(e->reload());
        inspect.addActionListener(e->{
            if(catalog.getSelectedValue()!=null)inspect(catalog.getSelectedValue());
        });
        approve.addActionListener(e->recordManualReview("VISUAL_APPROVED"));
        needsWork.addActionListener(e->recordManualReview("NEEDS_REPAIR"));
        for(JButton b:new JButton[]{prev,next,shot,refresh,inspect,approve,needsWork})buttons.add(b);
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
    private void reload(){all=DevAssetReviewCatalog.entries();filter();}
    private void filter(){
        String kind=String.valueOf(type.getSelectedItem());
        String q=search.getText().trim().toLowerCase(Locale.ROOT);
        visible=all.stream().filter(e->kind.equals("ALL")||e.kind().name().equals(kind))
            .filter(e->q.isEmpty()||e.key().toLowerCase(Locale.ROOT).contains(q)).toList();
        listModel.clear();
        for(var e:visible)listModel.addElement(e);
        page=0;selected=-1;showPage();
    }
    private void changePage(int delta){
        if(visible.isEmpty())return;
        int max=(visible.size()-1)/BAY;
        page=Math.max(0,Math.min(max,page+delta));
        selected=-1;showPage();
    }
    private void showPage(){
        int start=Math.min(visible.size(),page*BAY);
        bay=List.copyOf(visible.subList(start,Math.min(visible.size(),start+BAY)));
        renderer.x=0;renderer.z=-5;renderer.yaw=0;renderer.pitch=-.06;renderer.eyeHeight=1.65;
        status.setText("DEV ROOM • "+all.size()+" registered • bay "+
            (visible.isEmpty()?0:page+1)+"/"+Math.max(1,(visible.size()+BAY-1)/BAY)+
            " • PgUp/PgDn bays • 1–6 teleport • WASD/Q/E fly • drag look • R rotate • F12 capture");
    }
    private void teleport(int slot){
        if(slot<0||slot>=bay.size())return;
        selected=slot;
        renderer.x=SceneRenderer.devAssetX(slot);
        renderer.z=SceneRenderer.devAssetZ(slot)-3.15;
        renderer.eyeHeight=1.35;renderer.yaw=0;renderer.pitch=-.05;
        inspect(bay.get(slot));
        viewport.requestFocusInWindow();
    }
    private void inspect(DevAssetReviewCatalog.Entry asset){
        var a=DevAssetReviewCatalog.audit(asset);
        details.setText("REAL RUNTIME ASSET\n"+asset.key()+
            "\nDecoder: "+a.state()+
            "\nTriangles: "+a.triangles()+"; materials: "+a.materials()+
            "\nOriginal texture samples: "+a.textureSamples()+
            "\nBounds: "+a.minX()+","+a.minY()+","+a.minZ()+
            " -> "+a.maxX()+","+a.maxY()+","+a.maxZ()+
            "\n"+a.details()+
            "\n\nSTATUS IS DIAGNOSTIC ONLY. Inspect screenshot and source UVs before approving art.");
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
        try{renderer.drawDevAssetRoom(bay,clock+=.06,rotation);}
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
            d.drawString("ISOLATED DEV ASSET LEVEL / ORIGINAL TEXTURES",18,29);
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
        DevAssetReviewCatalog.Entry e=catalog.getSelectedValue();
        if(e==null&&selected>=0&&selected<bay.size())e=bay.get(selected);
        if(e==null){status.setText("Choose an asset first");return;}
        String comment=JOptionPane.showInputDialog(window,
            verdict.equals("NEEDS_REPAIR")?"Describe the visible issue (UV/texture/sprite/model):":
                "Visual review note (original source fidelity verified):",
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
            String asset=selected>=0&&selected<bay.size()?bay.get(selected).key().replace('/','_'):"bay_"+page;
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
    private static String csv(String value){return "\""+value.replace("\"","\"\"").replace("\n"," ")+"\"";}
    public static void main(String[] args)throws Exception{
        boolean selected=false,capture=false;
        int limit=0;Path output=Path.of("build","dev-room");
        String focus=null;
        for(String arg:args){
            if(arg.equals("--dev-room"))selected=true;
            else if(arg.equals("--capture"))capture=true;
            else if(arg.startsWith("--limit="))limit=Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--output="))output=Path.of(arg.substring(9));
            else if(arg.startsWith("--asset="))focus=arg.substring(8);
            else throw new IllegalArgumentException("Unknown dev-room option "+arg);
        }
        if(!selected)throw new IllegalArgumentException("Explicit --dev-room required");
        if(limit<0||limit>10000)throw new IllegalArgumentException("Invalid review limit");
        if(capture){capture(output,limit,focus);return;}
        if(GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("Interactive dev room requires a desktop; use --capture for headless");
        SwingUtilities.invokeLater(DevAssetShowroom::new);
    }
}
