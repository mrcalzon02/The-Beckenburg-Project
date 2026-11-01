package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Scene packaging front end. A saved scene and authored OBJ assets share a
 * world-owned package folder. The editable world source and game runtime
 * remain untouched; a host renderer must explicitly consume the mesh nodes.
 */
public final class TerrainScenePackageEditor extends JPanel {
    private final JFrame frame;
    private final StitchedHeightmapWorld world;
    private final TerrainScenePackage module;
    private final EnvironmentModules modules=new EnvironmentModules();
    private InMemoryRuntimeScene scene;
    private final JSpinner tileX=new JSpinner(new SpinnerNumberModel(0,-999999,999999,1));
    private final JSpinner tileZ=new JSpinner(new SpinnerNumberModel(0,-999999,999999,1));
    private final JSpinner cellX=new JSpinner(new SpinnerNumberModel(0,0,1023,1));
    private final JSpinner cellZ=new JSpinner(new SpinnerNumberModel(0,0,1023,1));
    private final JSpinner cells=new JSpinner(new SpinnerNumberModel(8,1,32,1));
    private final JTextField chunk=new JTextField("tile_0_0_patch",18);
    private final JTextField railRoute=new JTextField("",14);
    private final JTextArea inventory=new JTextArea(15,76);
    private final JLabel status=new JLabel();
    private final JButton publish=new JButton("PUBLISH PATCH TO SCENE");
    private final JButton publishRail=new JButton("PUBLISH SAVED RAIL TO SCENE");
    private final JButton inspectRail=new JButton("3D RAIL PREVIEW");
    private final JButton preview=new JButton("3D MESH PREVIEW");
    private final JButton verify=new JButton("VERIFY PACKAGE");
    private final JButton reload=new JButton("RELOAD SCENE");

    public static JFrame open(Window owner,StitchedHeightmapWorld world,int tx,int tz)
            throws IOException{
        JFrame f=new JFrame("Beckenburg — Terrain to Scene Package");
        var panel=new TerrainScenePackageEditor(f,world,tx,tz);
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){panel.modules.detach();}
        });
        f.setMinimumSize(new Dimension(790,480));f.setSize(1120,670);
        f.setLocationRelativeTo(owner);f.setVisible(true);return f;
    }
    private TerrainScenePackageEditor(JFrame frame,StitchedHeightmapWorld world,
                                      int tx,int tz)throws IOException{
        super(new BorderLayout(5,5));
        this.frame=frame;this.world=world;this.module=new TerrainScenePackage(world);
        this.scene=module.openScene();
        modules.register(module);modules.attach(scene);
        tileX.setValue(tx);tileZ.setValue(tz);
        chunk.setText("tile_"+tx+"_"+tz+"_patch");
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel form=new JPanel(new FlowLayout(FlowLayout.LEFT,5,4));
        form.add(new JLabel("Tile X"));form.add(tileX);
        form.add(new JLabel("Tile Z"));form.add(tileZ);
        form.add(new JLabel("Start cell X"));form.add(cellX);
        form.add(new JLabel("Z"));form.add(cellZ);
        form.add(new JLabel("Patch side (cells)"));form.add(cells);
        form.add(new JLabel("Stable chunk ID"));form.add(chunk);
        form.add(new JLabel("Saved rail route ID"));form.add(railRoute);
        add(form,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
        actions.add(publish);actions.add(publishRail);actions.add(inspectRail);actions.add(preview);actions.add(verify);actions.add(reload);
        publish.addActionListener(e->publish());
        publishRail.addActionListener(e->publishRail());
        inspectRail.addActionListener(e->inspectRail());
        preview.addActionListener(e->preview());
        verify.addActionListener(e->verify());
        reload.addActionListener(e->reload());
        JPanel center=new JPanel(new BorderLayout(4,4));
        center.add(actions,BorderLayout.NORTH);
        inventory.setEditable(false);
        inventory.setLineWrap(false);
        center.add(new JScrollPane(inventory),BorderLayout.CENTER);
        add(center,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new GridLayout(2,1,2,2));
        bottom.add(new JLabel("Only publishes geometry-only OBJ and durable scene nodes. "+
            "No substitute textures, in-game renderer, or collision adapter are enabled."));
        bottom.add(status);add(bottom,BorderLayout.SOUTH);
        refresh("Scene package loaded");
    }
    private static int number(JSpinner s){return ((Number)s.getValue()).intValue();}
    private TerrainEntranceMesh.Bounds selectedBounds(){
        var s=world.settings();
        long x=(long)number(tileX)*s.tileCells()+number(cellX);
        long z=(long)number(tileZ)*s.tileCells()+number(cellZ);
        if(number(cellX)>=s.tileCells()||number(cellZ)>=s.tileCells())
            throw new IllegalArgumentException("Start cell exceeds tile dimensions");
        if(number(cellX)+number(cells)>s.tileCells()
            ||number(cellZ)+number(cells)>s.tileCells())
            throw new IllegalArgumentException("Patch crosses tile edge: select a smaller region or next tile");
        double x0=s.originX()+x*s.metresPerCell();
        double z0=s.originZ()+z*s.metresPerCell();
        return new TerrainEntranceMesh.Bounds(x0,z0,
            x0+number(cells)*s.metresPerCell(),
            z0+number(cells)*s.metresPerCell());
    }
    private void busy(boolean busy){
        publish.setEnabled(!busy);publishRail.setEnabled(!busy);
        inspectRail.setEnabled(!busy);preview.setEnabled(!busy);
        verify.setEnabled(!busy);reload.setEnabled(!busy);
    }
    private void publish(){
        final String name=chunk.getText().trim();
        final TerrainEntranceMesh.Bounds area;
        try{
            TerrainScenePackage.idForChunk(name);
            area=selectedBounds();
            if(scene.find(TerrainScenePackage.idForChunk(name))!=null
                &&JOptionPane.showConfirmDialog(frame,
                    "Rebuild the previously published geometry for "+name+"?",
                    "Replace owned scene chunk",JOptionPane.YES_NO_OPTION)
                    !=JOptionPane.YES_OPTION)return;
        }catch(RuntimeException failure){error("Invalid chunk selection",failure);return;}
        busy(true);status.setText("Generating and verifying genuine geometry...");
        new SwingWorker<TerrainScenePackage.Published,Void>(){
            @Override protected TerrainScenePackage.Published doInBackground()throws Exception{
                return module.publish(name,area);
            }
            @Override protected void done(){
                busy(false);
                try{
                    var result=get();
                    refresh("Published "+result.nodeId()+": "+result.triangles()+
                        " OBJ triangles, "+result.parts()+" real mesh groups");
                }catch(Exception failure){error("Publishing failed",failure);}
            }
        }.execute();
    }
    private void publishRail(){
        final String route=railRoute.getText().trim();
        try{
            TerrainRailScenePackage.idForRail(route);
            if(scene.find(TerrainRailScenePackage.idForRail(route))!=null
                &&JOptionPane.showConfirmDialog(frame,
                    "Rebuild the published railway for "+route+"?",
                    "Replace owned rail mesh",JOptionPane.YES_NO_OPTION)
                    !=JOptionPane.YES_OPTION)return;
        }catch(RuntimeException failure){error("Invalid rail selection",failure);return;}
        busy(true);status.setText("Publishing actual saved railway alignment...");
        new SwingWorker<TerrainRailScenePackage.Published,Void>(){
            @Override protected TerrainRailScenePackage.Published doInBackground()throws Exception{
                return TerrainRailScenePackage.publish(world,scene,route);
            }
            @Override protected void done(){
                busy(false);
                try{
                    var result=get();
                    refresh("Published "+result.nodeId()+": "+
                        result.triangles()+" real triangles, "+result.sleepers()+" sleepers");
                }catch(Exception failure){error("Rail publication failed",failure);}
            }
        }.execute();
    }
    private void inspectRail(){
        final String route=railRoute.getText().trim();
        try{TerrainRailScenePackage.idForRail(route);}
        catch(RuntimeException invalid){error("Invalid rail selection",invalid);return;}
        busy(true);status.setText("Building genuine railway preview...");
        new SwingWorker<TerrainRailMesh.Output,Void>(){
            @Override protected TerrainRailMesh.Output doInBackground()throws Exception{
                var saved=TerrainInfrastructureSplines.open(world).list().stream()
                    .filter(a->a.id().equals(route)).findFirst()
                    .orElseThrow(()->new IOException("Save the railway alignment first: "+route));
                return TerrainRailMesh.build(world.settings(),saved,
                    TerrainRailMesh.Profile.standard());
            }
            @Override protected void done(){
                busy(false);
                try{
                    var mesh=get();
                    TerrainEntrancePreview.openRail(frame,mesh);
                    refresh("Rail preview: "+mesh.mesh().triangles().size()+
                        " real triangles, "+mesh.sleepers()+" sleepers");
                }catch(Exception failure){error("Rail preview failed",failure);}
            }
        }.execute();
    }
    private void preview(){
        final TerrainEntranceMesh.Bounds area;
        try{area=selectedBounds();}
        catch(RuntimeException failure){error("Invalid mesh bounds",failure);return;}
        busy(true);status.setText("Generating selected 3D terrain patch...");
        new SwingWorker<TerrainEntranceMesh.Output,Void>(){
            @Override protected TerrainEntranceMesh.Output doInBackground()throws Exception{
                return TerrainEntranceMesh.build(world,area);
            }
            @Override protected void done(){
                busy(false);
                try{
                    var result=get();
                    TerrainEntrancePreview.open(frame,result);
                    refresh("3D preview generated from authored terrain: "+
                        result.mesh().triangles().size()+" faces");
                }catch(Exception failure){error("Preview failed",failure);}
            }
        }.execute();
    }
    private void verify(){
        busy(true);status.setText("Checking actual published OBJ assets...");
        new SwingWorker<List<String>,Void>(){
            @Override protected List<String> doInBackground()throws Exception{
                var issues=new java.util.ArrayList<String>(TerrainScenePackage.inspect(world,scene));
                issues.addAll(TerrainRailScenePackage.inspect(world,scene));
                return List.copyOf(issues);
            }
            @Override protected void done(){
                busy(false);
                try{
                    var problems=get();
                    refresh(problems.isEmpty()?"All owned OBJ references verify":
                        "PACKAGE ERRORS: "+String.join("; ",problems));
                }catch(Exception failure){error("Package inspection failed",failure);}
            }
        }.execute();
    }
    private void reload(){
        try{
            var fresh=module.openScene();
            modules.detach();
            scene=fresh;modules.attach(scene);
            refresh("Scene document reloaded from disk");
        }catch(IOException|RuntimeException failure){error("Scene reload failed",failure);}
    }
    private void refresh(String message){
        var s=new StringBuilder("WORLD: "+world.directory()+"\n");
        s.append("Scene: ").append(scene.sceneId()).append('\n');
        s.append("Document: ").append(module.scenePath()).append(" | present: ")
            .append(Files.exists(module.scenePath())).append('\n');
        s.append("Published or existing objects: ").append(scene.nodes().size()).append("\n\n");
        for(var node:scene.nodes()){
            s.append(node.id()).append("  [").append(node.layer()).append("]");
            for(var c:node.components())if(c.type().equals(TerrainScenePackage.COMPONENT)){
                s.append("\n    OBJ: ").append(c.properties().get("uri"));
                s.append(" | triangles ").append(c.properties().get("triangles"));
                s.append(" | texture ").append(c.properties().get("materialStatus"));
            }
            for(var c:node.components())if(c.type().equals(TerrainRailScenePackage.COMPONENT)){
                s.append("\n    RAIL OBJ: ").append(c.properties().get("uri"));
                s.append(" | sleepers ").append(c.properties().get("sleepers"));
                s.append(" | renderer ").append(c.properties().get("rendererStatus"));
            }
            s.append('\n');
        }
        try{
            var routes=TerrainInfrastructureSplines.open(world).list();
            s.append("\nSAVED RAIL ALIGNMENTS:\n");
            for(var route:routes)if(route.kind()==TerrainInfrastructureSplines.Kind.RAIL)
                s.append("  ").append(route.id()).append(" — ")
                    .append(route.controls().size()).append(" controls\n");
        }catch(IOException problem){
            s.append("\nCannot read saved routes: ").append(problem.getMessage()).append('\n');
        }
        inventory.setText(s.toString());inventory.setCaretPosition(0);
        status.setText(message);
    }
    private void error(String title,Exception failure){
        Throwable cause=failure;
        while(cause.getCause()!=null)cause=cause.getCause();
        status.setText(title+": "+cause.getMessage());
        JOptionPane.showMessageDialog(frame,cause.getMessage(),title,
            JOptionPane.ERROR_MESSAGE);
    }
}
