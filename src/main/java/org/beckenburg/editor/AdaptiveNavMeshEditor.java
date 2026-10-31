package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * Human-editable navigation overlay for the REAL stitched heightmap.
 * No editor-only rules leak into gameplay until the production nav adapter uses
 * the identical saved masks and portals. Continuous rebuild is opt-in; the
 * manual pulse never silently replaces hand-authored cuts or blocked doors.
 */
public final class AdaptiveNavMeshEditor extends JPanel {
    private static final int VIEW_CELLS=8;
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final AdaptiveNavMeshEdits edits;
    private final JSpinner tileX=spinner(0,-999999,999999);
    private final JSpinner tileZ=spinner(0,-999999,999999);
    private final JSpinner cellX,cellZ;
    private final JSpinner navResolution=new JSpinner(
        new SpinnerNumberModel(.5,.125,4.0,.125));
    private final JCheckBox continuous=new JCheckBox("Continuous rebuild");
    private final JLabel status=new JLabel();
    private final MapCanvas canvas=new MapCanvas();
    private final ArrayDeque<AdaptiveNavMeshEdits.Snapshot> undo=new ArrayDeque<>();
    private final ArrayDeque<AdaptiveNavMeshEdits.Snapshot> redo=new ArrayDeque<>();
    private final Timer watcher;
    private AdaptiveNavigationRegions.Graph graph;
    private AdaptiveNavigationRegions.Region selected;
    private double clickX,clickZ,barrierStartX,barrierStartZ;
    private boolean drawingGate,running,rerun;
    private long changeVersion,seenSignature;

    private static JSpinner spinner(int value,int min,int max){
        return new JSpinner(new SpinnerNumberModel(value,min,max,1));
    }
    private static int n(JSpinner s){return ((Number)s.getValue()).intValue();}
    public static JFrame open(Window owner,StitchedHeightmapWorld world,int tx,int tz)
            throws IOException {
        JFrame f=new JFrame("Beckenburg — Adaptive Nav Flood Fill & Manual Masks");
        var panel=new AdaptiveNavMeshEditor(f,world,tx,tz);
        f.setContentPane(panel);
        f.setMinimumSize(new Dimension(800,560));
        f.setSize(1060,740);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override public void windowClosed(java.awt.event.WindowEvent e){
                panel.watcher.stop();
            }
        });
        f.setLocationRelativeTo(owner);
        f.setVisible(true);
        return f;
    }
    private AdaptiveNavMeshEditor(JFrame frame,StitchedHeightmapWorld world,int tx,int tz)
            throws IOException {
        super(new BorderLayout(5,5));
        this.frame=frame;this.world=world;
        this.edits=AdaptiveNavMeshEdits.open(world);
        cellX=spinner(0,0,world.settings().tileCells()-1);
        cellZ=spinner(0,0,world.settings().tileCells()-1);
        tileX.setValue(tx);tileZ.setValue(tz);
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));

        JPanel controls=new JPanel(new java.awt.GridLayout(3,1,3,2));
        JPanel location=new JPanel(new FlowLayout(FlowLayout.LEFT,4,1));
        addSpinner(location,"Tile X",tileX);addSpinner(location,"Z",tileZ);
        addSpinner(location,"Cell X",cellX);addSpinner(location,"Z",cellZ);
        location.add(new JLabel("Min nav region (m)"));
        location.add(navResolution);
        location.add(new JLabel("Viewport: up to 8×8 source cells"));
        controls.add(location);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,4,1));
        continuous.setSelected(edits.continuous());
        continuous.addActionListener(e->modify(()->edits.setContinuous(continuous.isSelected())));
        buttons.add(continuous);
        button(buttons,"FLOOD FILL PULSE",this::pulse);
        button(buttons,"SEED AT CLICK",()->modify(()->edits.addSeed(
            new AdaptiveNavMeshEdits.Seed("surface",clickX,clickZ))));
        button(buttons,"DRAW BLOCKED GATE",()->{
            drawingGate=true;
            barrierStartX=Double.NaN;barrierStartZ=Double.NaN;
            status.setText("Choose first and second gate endpoints on the map");
        });
        button(buttons,"TRIM SELECTED",()->trimSelected(false));
        button(buttons,"TRIM CONNECTED",()->trimSelected(true));
        controls.add(buttons);
        JPanel editsRow=new JPanel(new FlowLayout(FlowLayout.LEFT,4,1));
        button(editsRow,"UNTRIM LAST",()->modify(()->{
            var list=edits.exclusions();
            if(!list.isEmpty())edits.removeExclusion(list.get(list.size()-1));
        }));
        button(editsRow,"REMOVE LAST GATE",()->modify(()->{
            var list=edits.barriers();
            if(!list.isEmpty())edits.removeBarrier(list.get(list.size()-1));
        }));
        button(editsRow,"REMOVE LAST SEED",()->modify(()->{
            var list=edits.seeds();
            if(!list.isEmpty())edits.removeSeed(list.get(list.size()-1));
        }));
        button(editsRow,"UNDO",()->history(false));
        button(editsRow,"REDO",()->history(true));
        button(editsRow,"SAVE MASKS",this::save);
        controls.add(editsRow);
        add(controls,BorderLayout.NORTH);

        canvas.setPreferredSize(new Dimension(670,480));
        canvas.setBackground(new Color(19,22,27));
        add(canvas,BorderLayout.CENTER);
        JPanel footer=new JPanel(new java.awt.GridLayout(2,1,3,2));
        footer.add(new JLabel("Click any region (including grey, unreachable regions). Blue=reachable; grey=regress candidate; "+
            "red=gate/trim; orange=seed. All trims persist."));
        footer.add(status);
        add(footer,BorderLayout.SOUTH);

        for(JSpinner field:new JSpinner[]{tileX,tileZ,cellX,cellZ,navResolution})
            field.addChangeListener(e->{
                selected=null;
                updateStatus("Viewport changed; pulse to regenerate");
                if(continuous.isSelected())pulse();
            });
        var bounds=view();
        clickX=(bounds.x0()+bounds.x1())*.5;
        clickZ=(bounds.z0()+bounds.z1())*.5;
        seenSignature=fileSignature();
        watcher=new Timer(1250,e->{
            if(!continuous.isSelected())return;
            long signature=fileSignature();
            if(signature!=seenSignature){
                seenSignature=signature; pulse();
            }
        });
        watcher.start();
        updateStatus("No mesh generated: click FLOOD FILL PULSE");
        if(continuous.isSelected())pulse();
    }
    private static void addSpinner(JPanel p,String name,JSpinner spinner){
        p.add(new JLabel(name));p.add(spinner);
    }
    private static void button(JPanel p,String name,Runnable action){
        JButton b=new JButton(name);b.addActionListener(e->action.run());p.add(b);
    }
    private AdaptiveNavigationRegions.Area view(){
        int n=world.settings().tileCells(),x=Math.floorMod(n(cellX),n),z=Math.floorMod(n(cellZ),n);
        double m=world.settings().metresPerCell();
        double ox=world.settings().originX(),oz=world.settings().originZ();
        long ix=(long)n(tileX)*n+x,iz=(long)n(tileZ)*n+z;
        return new AdaptiveNavigationRegions.Area("surface",
            ox+ix*m,oz+iz*m,
            ox+(ix+Math.min(VIEW_CELLS,n-x))*m,
            oz+(iz+Math.min(VIEW_CELLS,n-z))*m);
    }
    private static long stamp(Path p){
        try{return Files.exists(p)?Files.getLastModifiedTime(p).toMillis():-1;}
        catch(IOException e){return -1;}
    }
    private long fileSignature(){
        int tx=n(tileX),tz=n(tileZ);
        return 31*(31*stamp(world.directory().resolve("adaptive-topology.bin"))
            +stamp(edits.path()))+stamp(world.tilePath(tx,tz));
    }
    private void modify(Runnable action){
        var before=edits.snapshot();
        try{
            action.run();
            if(before.equals(edits.snapshot()))return;
            edits.save();
            if(!edits.snapshot().equals(AdaptiveNavMeshEdits.open(world).snapshot()))
                throw new IOException("Navigation masks failed disk readback");
            undo.push(before);if(undo.size()>80)undo.removeLast();redo.clear();
            changeVersion++;seenSignature=fileSignature();
            selected=null;
            updateStatus("Authored navigation masks saved; graph needs fresh flood pulse");
            if(continuous.isSelected())pulse();
        }catch(IOException|RuntimeException failure){
            edits.restore(before);
            continuous.setSelected(edits.continuous());
            error("Cannot change navigation mask",failure);
        }
    }
    private void save(){
        try{
            edits.save();seenSignature=fileSignature();updateStatus("Navigation edits saved");
        }catch(IOException|RuntimeException ex){error("Mask save failed",ex);}
    }
    private void history(boolean forward){
        var input=forward?redo:undo;
        var output=forward?undo:redo;
        if(input.isEmpty())return;
        var target=input.peek();
        var prior=edits.snapshot();
        try{
            edits.restore(target);edits.save();
            if(!edits.snapshot().equals(AdaptiveNavMeshEdits.open(world).snapshot()))
                throw new IOException("Undo/redo readback mismatch");
            input.pop();output.push(prior);
            continuous.setSelected(edits.continuous());
            changeVersion++;seenSignature=fileSignature();
            selected=null;
            if(continuous.isSelected())pulse();
            else updateStatus("Authoring changed; manual pulse required");
        }catch(IOException|RuntimeException failure){
            edits.restore(prior);error("Undo/redo failed",failure);
        }
    }
    private void trimSelected(boolean connected){
        if(selected==null){updateStatus("Click a generated region first");return;}
        if(!connected){
            var r=selected;
            modify(()->edits.addExclusion(new AdaptiveNavMeshEdits.Exclusion(
                r.layer(),r.x0(),r.z0(),r.x1(),r.z1())));
            return;
        }
        if(graph==null||graph.stale()){updateStatus("Pulse an up-to-date mesh first");return;}
        // Selection may belong to the unreachable side of a blocked level gate.
        var candidates=graph.candidates();
        boolean[] seen=new boolean[candidates.regions().size()];
        var queue=new ArrayDeque<Integer>();
        queue.add(selected.id());seen[selected.id()]=true;
        while(!queue.isEmpty()){
            int id=queue.removeFirst();
            for(var edge:candidates.connections(id))if(!seen[edge.to()]){
                seen[edge.to()]=true;queue.addLast(edge.to());
            }
        }
        int count=0;
        for(boolean b:seen)if(b)count++;
        if(count>AdaptiveNavMeshEdits.MAX_ENTRIES-edits.exclusions().size()){
            updateStatus("Connected branch too large for authored trim budget");return;
        }
        if(JOptionPane.showConfirmDialog(frame,
            "Permanently trim "+count+" connected navigation regions? "+
            "A flood pulse cannot restore them. UNDO can.",
            "Confirm connected trim",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        var regions=candidates.regions();
        modify(()->{
            for(int i=0;i<seen.length;i++)if(seen[i]){
                var r=regions.get(i);
                edits.addExclusion(new AdaptiveNavMeshEdits.Exclusion(
                    r.layer(),r.x0(),r.z0(),r.x1(),r.z1()));
            }
        });
    }
    private void pulse(){
        if(running){rerun=true;return;}
        final long issued=changeVersion;
        final var area=view();
        final double resolution=((Number)navResolution.getValue()).doubleValue();
        running=true;
        updateStatus("Building adaptive nav regions and seeded flood fill...");
        new SwingWorker<AdaptiveNavigationRegions.Graph,Void>(){
            @Override protected AdaptiveNavigationRegions.Graph doInBackground()
                    throws Exception {
                // Read a stable, saved copy; never race an active editor mutation.
                var copy=AdaptiveNavMeshEdits.open(world);
                var physical=new LayeredTerrainQueries(world,
                    AdaptiveTerrainTopology.open(world),List.of(),List.of());
                var profile=new AdaptiveNavigationRegions.Profile(
                    new LayeredTerrainQueries.Agent(.10,1.65,42,.24),false,false);
                return AdaptiveNavigationRegions.build(physical,profile,
                    List.of(area),List.of(),copy,resolution);
            }
            @Override protected void done(){
                running=false;
                try{
                    var built=get();
                    if(changeVersion==issued){
                        graph=built;selected=null;
                        updateStatus("Flood pulse complete");
                    }else rerun=true;
                }catch(InterruptedException interrupted){
                    Thread.currentThread().interrupt();error("Navigation pulse interrupted",interrupted);
                }catch(ExecutionException failure){
                    graph=null;selected=null;
                    Throwable root=failure.getCause();
                    error("Navigation pulse failed",new IOException(root==null?
                        failure.toString():root.toString(),failure));
                }
                if(rerun){rerun=false;pulse();}
            }
        }.execute();
    }
    private void updateStatus(String reason){
        var m=graph==null?null:graph.metrics();
        status.setText(reason+" | mode "+(continuous.isSelected()?"continuous":"manual")+
            " | seeds "+edits.seeds().size()+" | gates "+edits.barriers().size()+
            " | trims "+edits.exclusions().size()+
            (m==null?" | NO NAV GRAPH": " | regions "+m.acceptedRegions()+
            " | links "+m.edges()+" | probes "+m.evaluatedCells()));
        canvas.repaint();
    }
    private void error(String message,Exception failure){
        updateStatus(message+": "+failure.getMessage());
        JOptionPane.showMessageDialog(frame,failure.getMessage(),message,
            JOptionPane.ERROR_MESSAGE);
    }
    private final class MapCanvas extends JPanel {
        private int left,top,side;
        MapCanvas(){
            addMouseListener(new MouseAdapter(){
                @Override public void mouseClicked(MouseEvent e){
                    if(side<=0||e.getX()<left||e.getY()<top||
                        e.getX()>=left+side||e.getY()>=top+side)return;
                    var a=view();
                    double x=a.x0()+(e.getX()-left)*(a.x1()-a.x0())/side;
                    double z=a.z0()+(e.getY()-top)*(a.z1()-a.z0())/side;
                    if(drawingGate){
                        if(!Double.isFinite(barrierStartX)){
                            barrierStartX=x;barrierStartZ=z;
                            updateStatus("Gate start set; click other end");
                        }else{
                            drawingGate=false;
                            double dx=Math.abs(x-barrierStartX);
                            double dz=Math.abs(z-barrierStartZ);
                            if(Math.max(dx,dz)<.125){
                                updateStatus("Gate length must be at least 0.125m");
                            }else{
                                double x0=barrierStartX,z0=barrierStartZ;
                                if(dx>=dz)z=z0;else x=x0;
                                double finalX=x,finalZ=z;
                                modify(()->edits.addBarrier(new AdaptiveNavMeshEdits.Barrier(
                                    "surface",x0,z0,finalX,finalZ)));
                            }
                        }
                        return;
                    }
                    clickX=x;clickZ=z;selected=null;
                    if(graph!=null)for(var r:graph.candidates().regions())
                        if(r.layer().equals("surface")&&r.contains(x,z)){
                            selected=r;break;
                        }
                    updateStatus(selected==null?"No navigable region under cursor":
                        "Selected region "+selected.id()+"; trim or seed here");
                }
            });
            barrierStartX=Double.NaN;barrierStartZ=Double.NaN;
        }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D out=(Graphics2D)g.create();
            side=Math.max(1,Math.min(getWidth()-25,getHeight()-25));
            left=(getWidth()-side)/2;top=(getHeight()-side)/2;
            var a=view();
            out.setColor(new Color(46,49,53));
            out.fillRect(left,top,side,side);
            if(graph!=null){
                // Candidate graph includes unreachable cells beyond author-blocked
                // doors, so they can still be selected for permanent regression.
                record Bounds(String layer,double x0,double z0,double x1,double z1){}
                Set<Bounds> reachable=new HashSet<>();
                for(var r:graph.regions())reachable.add(new Bounds(
                    r.layer(),r.x0(),r.z0(),r.x1(),r.z1()));
                for(var r:graph.candidates().regions()){
                    if(!r.layer().equals("surface"))continue;
                    int x=px(r.x0(),a),z=pz(r.z0(),a);
                    int w=Math.max(1,px(r.x1(),a)-x),h=Math.max(1,pz(r.z1(),a)-z);
                    boolean active=reachable.contains(new Bounds(
                        r.layer(),r.x0(),r.z0(),r.x1(),r.z1()));
                    out.setColor(r.equals(selected)?new Color(219,166,63):
                        active?new Color(77,122,157):new Color(85,88,96));
                    out.fillRect(x,z,w,h);
                    out.setColor(new Color(28,48,60));out.drawRect(x,z,w,h);
                }
            }
            for(var ex:edits.exclusions())if(ex.layer().equals("surface")){
                out.setColor(new Color(80,21,30,180));
                int x=px(ex.x0(),a),z=pz(ex.z0(),a);
                out.fillRect(x,z,Math.max(1,px(ex.x1(),a)-x),
                    Math.max(1,pz(ex.z1(),a)-z));
            }
            out.setColor(new Color(226,69,72));
            for(var b:edits.barriers())if(b.layer().equals("surface")){
                out.drawLine(px(b.x0(),a),pz(b.z0(),a),
                    px(b.x1(),a),pz(b.z1(),a));
            }
            out.setColor(new Color(249,175,65));
            for(var seed:edits.seeds())if(seed.layer().equals("surface")){
                int x=px(seed.x(),a),z=pz(seed.z(),a);
                out.fillOval(x-4,z-4,9,9);
            }
            out.setColor(new Color(218,225,227));
            out.drawRect(left,top,side,side);
            out.drawString("NAV FLOOD AUTHORING: REAL HEIGHTMAP, NOT GAMEPLAY",12,17);
            out.dispose();
        }
        private int px(double x,AdaptiveNavigationRegions.Area a){
            return left+(int)Math.round((x-a.x0())/(a.x1()-a.x0())*side);
        }
        private int pz(double z,AdaptiveNavigationRegions.Area a){
            return top+(int)Math.round((z-a.z0())/(a.z1()-a.z0())*side);
        }
    }
}
