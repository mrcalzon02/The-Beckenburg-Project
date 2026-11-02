package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorMeshPipeline;
import org.beckenburg.editor.EditorMeshTopology;
import org.beckenburg.editor.EditorTopologyProject;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.EditorMeshPrimitives;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Live asset import dock/dialog for the actual Beckenburg editor.
 * Preview-only reduction never mutates the source; only a confirmed publish
 * registers a derived GLB for the existing game rasterizer.
 */
public final class AssetImportDialog {
    private final JFrame parent;
    private final Consumer<String> published;
    private final JDialog dialog;
    private final JSlider slider=new JSlider(0,12,0);
    private final JComboBox<String> preset=new JComboBox<>(new String[]{"PSX Prop / 100 triangles","PSX Character / 800 triangles","PSX Close-up / 1200 triangles","PSX World Kit / 4000 triangles","Modern / no style budget"});
    private final JTextArea details=new JTextArea(10,40);
    private final JLabel pathLabel=new JLabel("Choose OBJ, STL, GLB or native BTOP mesh");
    private final JButton apply=new JButton("PUBLISH TO EDITOR");
    private final JButton choose=new JButton("CHOOSE MODEL");
    private final JPanel preview=new JPanel(){
        @Override public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D p=(Graphics2D)g.create();
            p.setColor(new Color(20,24,29));p.fillRect(0,0,getWidth(),getHeight());
            if(source!=null)draw(p,source.triangles(),0,getWidth()/2,topologyProject==null?"SOURCE":"WORKING MESH");
            if(primitivePreview!=null)draw(p,primitivePreview.triangles(),getWidth()/2,getWidth()-getWidth()/2,"PRIMITIVE PREVIEW");
            else if(result!=null)draw(p,result.optimized().triangles(),getWidth()/2,getWidth()-getWidth()/2,"PREVIEW");
            p.dispose();
        }
    };
    private final JSpinner vertexId=new JSpinner(new SpinnerNumberModel(0L,0L,Long.MAX_VALUE,1L));
    private final JSpinner vertexX=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexY=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexZ=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JButton moveVertex=new JButton("MOVE VERTEX"),undoMesh=new JButton("UNDO MESH"),redoMesh=new JButton("REDO MESH");
    private final JButton saveMesh=new JButton("SAVE MESH"),reopenMesh=new JButton("REOPEN MESH");
    private final JComboBox<EditorMeshPrimitives.Kind> primitiveKind=new JComboBox<>(EditorMeshPrimitives.Kind.values());
    private final JSpinner primitiveWidth=new JSpinner(new SpinnerNumberModel(1d,.0001,10000d,.1));
    private final JSpinner primitiveHeight=new JSpinner(new SpinnerNumberModel(1d,.0001,10000d,.1));
    private final JSpinner primitiveDepth=new JSpinner(new SpinnerNumberModel(1d,.0001,10000d,.1));
    private final JSpinner primitiveSegments=new JSpinner(new SpinnerNumberModel(12,3,128,1));
    private final JSpinner primitiveRings=new JSpinner(new SpinnerNumberModel(4,1,64,1));
    private final JButton newMesh=new JButton("NEW MESH"),previewPrimitive=new JButton("PREVIEW PRIMITIVE"),applyPrimitive=new JButton("APPLY PRIMITIVE");
    private EditorMeshPipeline.Mesh primitivePreview;
    private EditorMeshPrimitives.Parameters reviewedPrimitive;
    private EditorTopologyProject topologyProject;
    private EditorTopologyProject.Vertex selectedVertex;
    private EditorHistory<EditorTopologyProject.State> meshHistory;
    private boolean meshDirty;
    private String authoringNote="";
    private String reviewedSourceFingerprint;
    private Path path;
    private EditorMeshPipeline.Mesh source;
    private EditorMeshPipeline.Result result;
    private int revision;
    private boolean busy;
    private volatile long sourceTextureBytes;
    AssetImportDialog(JFrame parent,Consumer<String> published){
        this.parent=parent;this.published=published;
        dialog=new JDialog(parent,"Asset Import / PSX Regression Lab",false);
        JPanel content=new JPanel(new BorderLayout(5,5));
        JPanel head=new JPanel(new BorderLayout(5,4));
        head.add(pathLabel,BorderLayout.CENTER);
        choose.addActionListener(e->openSource());
        head.add(choose,BorderLayout.EAST);
        slider.setPaintTicks(true);slider.setMajorTickSpacing(2);slider.setPaintLabels(true);
        slider.setToolTipText("Level 0 preserves source geometry; 12 intentionally collapses small parts. This is not a triangle-budget target.");
        slider.addChangeListener(e->{if(!slider.getValueIsAdjusting()&&source!=null)recalculate();});
        preset.setToolTipText("Advisory visual/style budget only; never a hard release gate");
        preset.addActionListener(e->{if(source!=null&&result!=null)updateReport();});
        JPanel controls=new JPanel(new BorderLayout(3,3));
        controls.add(new JLabel("Geometry regression / vertex clustering (0 = original)"),BorderLayout.NORTH);
        controls.add(slider,BorderLayout.CENTER);
        JButton reset=new JButton("RESET");
        reset.addActionListener(e->slider.setValue(0));
        controls.add(reset,BorderLayout.EAST);
        JPanel vertexControls=new JPanel(new BorderLayout());
        JPanel vertexInputs=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));
        JPanel vertexActions=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));
        vertexControls.add(vertexInputs,BorderLayout.NORTH);vertexControls.add(vertexActions,BorderLayout.SOUTH);
        vertexInputs.add(new JLabel("Vertex ID"));vertexInputs.add(vertexId);
        for(var spinner:List.of(vertexId,vertexX,vertexY,vertexZ))spinner.setPreferredSize(new Dimension(65,24));
        for(var item:List.of(new Object[]{"X",vertexX},new Object[]{"Y",vertexY},new Object[]{"Z",vertexZ})){
            vertexInputs.add(new JLabel((String)item[0]));vertexInputs.add((JSpinner)item[1]);
        }
        vertexActions.add(moveVertex);vertexActions.add(undoMesh);vertexActions.add(redoMesh);vertexActions.add(saveMesh);vertexActions.add(reopenMesh);
        vertexId.addChangeListener(e->selectVertex());
        moveVertex.addActionListener(e->meshEdit(()->topologyProject.moveVertex(((Number)vertexId.getValue()).longValue(),
            ((Number)vertexX.getValue()).doubleValue(),((Number)vertexY.getValue()).doubleValue(),((Number)vertexZ.getValue()).doubleValue())));
        undoMesh.addActionListener(e->{if(!busy&&meshHistory!=null&&meshHistory.undo())meshChanged();});
        redoMesh.addActionListener(e->{if(!busy&&meshHistory!=null&&meshHistory.redo())meshChanged();});
        saveMesh.addActionListener(e->saveAuthoring());
        reopenMesh.addActionListener(e->meshEdit(()->{try{
            topologyProject.reopen();
            if(path!=null&&path.toAbsolutePath().normalize().equals(topologyProject.documentPath()))reviewedSourceFingerprint=topologyProject.diskFingerprint();
        }catch(IOException failure){throw new java.io.UncheckedIOException(failure);}}));
        JPanel primitiveInputs=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));
        primitiveInputs.add(newMesh);primitiveInputs.add(primitiveKind);
        for(var item:List.of(new Object[]{"W",primitiveWidth},new Object[]{"H",primitiveHeight},new Object[]{"D",primitiveDepth},
                new Object[]{"Segments",primitiveSegments},new Object[]{"Rings/grid",primitiveRings})){
            primitiveInputs.add(new JLabel((String)item[0]));var spinner=(JSpinner)item[1];spinner.setPreferredSize(new Dimension(58,24));primitiveInputs.add(spinner);
            spinner.addChangeListener(e->clearPrimitivePreview());
        }
        primitiveKind.addActionListener(e->clearPrimitivePreview());
        JPanel primitiveActions=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));primitiveActions.add(previewPrimitive);primitiveActions.add(applyPrimitive);
        primitiveActions.add(new JLabel("Y up; centered at origin. Preview first, then append as one undoable part."));
        JPanel primitiveControls=new JPanel(new BorderLayout());primitiveControls.add(primitiveInputs,BorderLayout.NORTH);primitiveControls.add(primitiveActions,BorderLayout.SOUTH);
        newMesh.addActionListener(e->newAuthoringMesh());previewPrimitive.addActionListener(e->previewPrimitive());
        applyPrimitive.addActionListener(e->{if(reviewedPrimitive!=null){var parameters=reviewedPrimitive;meshEdit(()->topologyProject.appendPrimitive(parameters));clearPrimitivePreview();}});
        JPanel lower=new JPanel(new BorderLayout());lower.add(preset,BorderLayout.NORTH);lower.add(primitiveControls,BorderLayout.CENTER);lower.add(vertexControls,BorderLayout.SOUTH);
        controls.add(lower,BorderLayout.SOUTH);refreshMeshControls();
        JPanel top=new JPanel(new BorderLayout(4,5));
        top.add(head,BorderLayout.NORTH);top.add(controls,BorderLayout.SOUTH);
        content.add(top,BorderLayout.NORTH);
        preview.setPreferredSize(new Dimension(780,310));
        JPanel middle=new JPanel(new BorderLayout(4,4));
        middle.add(preview,BorderLayout.CENTER);
        details.setEditable(false);details.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        middle.add(new JScrollPane(details),BorderLayout.SOUTH);
        content.add(middle,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,4));
        JButton close=new JButton("CLOSE");close.addActionListener(e->requestClose());
        apply.setEnabled(false);apply.addActionListener(e->publish());
        footer.add(new JLabel("Original retained; source and derivative are stored separately"));
        footer.add(apply);footer.add(close);
        content.add(footer,BorderLayout.SOUTH);
        content.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        dialog.setContentPane(content);dialog.setSize(1010,780);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){requestClose();}});
    }
    public static void show(JFrame parent,Consumer<String> published){
        new AssetImportDialog(parent,published).dialog.setVisible(true);
    }
    private boolean saveAuthoring(){
        if(topologyProject==null||busy)return false;
        try{
            topologyProject.save();meshDirty=false;
            if(path!=null&&path.toAbsolutePath().normalize().equals(topologyProject.documentPath()))reviewedSourceFingerprint=topologyProject.diskFingerprint();
            details.append("\nSAVED MESH "+topologyProject.documentPath());refreshMeshControls();return true;
        }catch(IOException failure){details.append("\nSAVE FAILED: "+failure.getMessage());return false;}
    }
    private void requestClose(){
        if(busy)return;
        if(meshDirty){
            int answer=JOptionPane.showConfirmDialog(dialog,"Save mesh edits before closing?","Unsaved mesh",JOptionPane.YES_NO_CANCEL_OPTION);
            if(answer==JOptionPane.CANCEL_OPTION||answer==JOptionPane.CLOSED_OPTION)return;
            if(answer==JOptionPane.YES_OPTION&&!saveAuthoring())return;
        }
        dialog.dispose();
    }
    private void refreshMeshControls(){
        boolean editable=topologyProject!=null&&!busy;
        for(var component:List.of(vertexId,vertexX,vertexY,vertexZ,moveVertex,saveMesh,reopenMesh))component.setEnabled(editable);
        undoMesh.setEnabled(editable&&meshHistory.canUndo());redoMesh.setEnabled(editable&&meshHistory.canRedo());
        if(editable)saveMesh.setToolTipText("Save native topology to "+topologyProject.documentPath());
        newMesh.setEnabled(!busy);previewPrimitive.setEnabled(editable);applyPrimitive.setEnabled(editable&&reviewedPrimitive!=null);
        for(var component:List.of(primitiveKind,primitiveWidth,primitiveHeight,primitiveDepth,primitiveSegments,primitiveRings))component.setEnabled(editable);
    }
    private void clearPrimitivePreview(){primitivePreview=null;reviewedPrimitive=null;applyPrimitive.setEnabled(false);preview.repaint();}
    private void previewPrimitive(){
        if(topologyProject==null||busy)return;
        try{
            var parameters=new EditorMeshPrimitives.Parameters((EditorMeshPrimitives.Kind)primitiveKind.getSelectedItem(),
                ((Number)primitiveWidth.getValue()).doubleValue(),((Number)primitiveHeight.getValue()).doubleValue(),((Number)primitiveDepth.getValue()).doubleValue(),
                ((Number)primitiveSegments.getValue()).intValue(),((Number)primitiveRings.getValue()).intValue());
            var mesh=EditorMeshPrimitives.preview(parameters);
            if((long)source.triangles().size()+mesh.triangles().size()>EditorMeshPipeline.MAX_TRIANGLES)throw new IllegalArgumentException("Project triangle budget exceeded");
            primitivePreview=mesh;reviewedPrimitive=parameters;applyPrimitive.setEnabled(true);preview.repaint();
            details.append("\nPRIMITIVE PREVIEW: "+mesh.name()+" / "+mesh.triangles().size()+" triangles. Apply to commit.");
        }catch(RuntimeException failure){clearPrimitivePreview();details.append("\nPRIMITIVE REFUSED: "+rootMessage(failure));}
    }
    private void newAuthoringMesh(){
        if(busy)return;
        if(meshDirty&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before creating a new mesh?","Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");
            var mesh=new EditorMeshPipeline.Mesh("New mesh",List.of(),0);
            var project=EditorTopologyProject.fromMesh(mesh,EditorMeshPipeline.sourceFingerprint("native-mesh-v1".getBytes(java.nio.charset.StandardCharsets.UTF_8)),document);
            project.save();clearPrimitivePreview();topologyProject=project;meshHistory=new EditorHistory<>(project,60);source=mesh;result=null;
            path=document;reviewedSourceFingerprint=project.diskFingerprint();authoringNote="Native primitive authoring; generated fallback atlas.";sourceTextureBytes=0;meshDirty=false;
            vertexId.setValue(0L);selectVertex();refreshMeshControls();pathLabel.setText(document.getFileName()+" | native authoring");
            if(slider.getValue()!=0)slider.setValue(0);else recalculate();
        }catch(IOException|RuntimeException failure){details.append("\nNEW MESH FAILED: "+rootMessage(failure));}
    }
    private void selectVertex(){
        selectedVertex=null;if(topologyProject==null)return;long id=((Number)vertexId.getValue()).longValue();
        for(var v:topologyProject.snapshot().vertices())if(v.id()==id){selectedVertex=v;vertexX.setValue(v.x());vertexY.setValue(v.y());vertexZ.setValue(v.z());return;}
    }
    private void meshEdit(Runnable operation){
        if(topologyProject==null||busy)return;
        try{meshHistory.execute(operation);meshChanged();}
        catch(RuntimeException failure){details.append("\nEDIT REFUSED: "+rootMessage(failure));refreshMeshControls();}
    }
    private void meshChanged(){
        meshDirty=true;source=topologyProject.mesh();selectVertex();refreshMeshControls();
        if(slider.getValue()!=0)slider.setValue(0);else recalculate();
    }
    private record Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String reviewedFingerprint){}
    private void openSource(){
        if(meshDirty&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before opening another source?",
            "Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        JFileChooser file=new JFileChooser();
        file.setDialogTitle("Import a mesh source (OBJ, STL, GLB)");
        file.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Mesh files (*.obj, *.stl, *.glb, *.btop)","obj","stl","glb","btop"));
        if(file.showOpenDialog(dialog)!=JFileChooser.APPROVE_OPTION)return;
        loadSource(file.getSelectedFile().toPath());
    }
    /** Same native import path for the human chooser and source-backed verification. */
    void loadSource(Path candidate){
        clearPrimitivePreview();
        busy=true;choose.setEnabled(false);apply.setEnabled(false);
        pathLabel.setText("Reading "+candidate.getFileName()+"...");
        refreshMeshControls();
        new SwingWorker<Loaded,Void>(){
            @Override protected Loaded doInBackground()throws Exception{
                String extension=candidate.getFileName().toString().toLowerCase(Locale.ROOT);
                if(extension.endsWith(".btop")){
                    sourceTextureBytes=0;
                    var project=EditorTopologyProject.open(candidate);return new Loaded(project.mesh(),project,"",project.diskFingerprint());
                }
                if(extension.endsWith(".glb")){
                    if(Files.size(candidate)>4_000_000)throw new IOException("Current game GLB decoder limit is 4 MiB");
                    byte[] bytes=EditorMeshPipeline.readSource(candidate,4_000_000);
                    var decoded=GlbProps.decode(bytes);
                    java.util.Set<Integer> mats=new java.util.HashSet<>();
                    long texBytes=0;
                    for(var triangle:decoded.triangles())mats.add(triangle.material());
                    for(int material:mats)
                        texBytes+=(long)GlbProps.textureWidth(material)*GlbProps.textureHeight(material)*4;
                    sourceTextureBytes=texBytes;
                    List<EditorMeshPipeline.Triangle> all=new ArrayList<>();
                    for(var triangle:decoded.triangles()){
                        var a=triangle.a();var b=triangle.b();var c=triangle.c();
                        all.add(new EditorMeshPipeline.Triangle("GLB assembly",
                            new EditorMeshPipeline.Vertex(a.x(),a.y(),a.z(),a.u(),a.v()),
                            new EditorMeshPipeline.Vertex(b.x(),b.y(),b.z(),b.u(),b.v()),
                            new EditorMeshPipeline.Vertex(c.x(),c.y(),c.z(),c.u(),c.v())));
                    }
                    return new Loaded(new EditorMeshPipeline.Mesh(candidate.getFileName().toString(),all,bytes.length),null,
                        "GLB vertex editing unavailable until original material and normal bindings can be retained.",EditorMeshPipeline.sourceFingerprint(bytes));
                }
                sourceTextureBytes=0;
                byte[] bytes=EditorMeshPipeline.readSource(candidate,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);
                var mesh=EditorMeshPipeline.read(candidate.getFileName().toString(),bytes);
                String digest=EditorMeshPipeline.sourceFingerprint(bytes);
                Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");
                try{return new Loaded(mesh,EditorTopologyProject.fromMesh(mesh,digest,document),"",digest);}
                catch(IllegalArgumentException unsupported){return new Loaded(mesh,null,"Vertex editing unavailable: "+unsupported.getMessage(),digest);}
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;
                try{
                    var loaded=get();source=loaded.mesh();topologyProject=loaded.project();authoringNote=loaded.note();reviewedSourceFingerprint=loaded.reviewedFingerprint();meshDirty=false;
                    meshHistory=topologyProject==null?null:new EditorHistory<>(topologyProject,60);
                    path=candidate;vertexId.setValue(topologyProject==null||topologyProject.snapshot().vertices().isEmpty()?0L:topologyProject.snapshot().vertices().get(0).id());
                    selectVertex();refreshMeshControls();slider.setValue(0);
                    pathLabel.setText(candidate.getFileName()+" | "+source.triangles().size()+" source triangles");
                    recalculate();
                }catch(Exception e){
                    source=null;result=null;path=null;topologyProject=null;meshHistory=null;refreshMeshControls();apply.setEnabled(false);
                    details.setText("IMPORT ERROR: "+rootMessage(e));
                    pathLabel.setText("Unable to read "+candidate.getFileName());
                    preview.repaint();
                }
            }
        }.execute();
    }
    private record Review(EditorMeshPipeline.Result reduction,EditorMeshTopology topology){}
    private void recalculate(){
        if(source==null||busy)return;
        final EditorMeshPipeline.Mesh mesh=source;
        final int level=slider.getValue(),request=++revision;
        final EditorTopologyProject.State authoringState=topologyProject==null?null:topologyProject.snapshot();
        final EditorTopologyProject project=topologyProject;
        apply.setEnabled(false);details.setText("Computing regression level "+level+"...");
        new SwingWorker<Review,Void>(){
            @Override protected Review doInBackground(){
                var reduction=EditorMeshPipeline.regress(mesh,level);
                // The synchronized project is read only if this is still the captured revision.
                var topology=level==0&&project!=null&&project.snapshot()==authoringState?project.topology():EditorMeshTopology.build(reduction.optimized());
                return new Review(reduction,topology);
            }
            @Override protected void done(){
                if(request!=revision||mesh!=source)return;
                try{
                    var review=get();result=review.reduction();
                    int after=result.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                    StringBuilder report=new StringBuilder();
                    report.append("PSX look preset: ").append(preset.getSelectedItem())
                          .append(" (advisory; the game is not running on real PS1 hardware)\n");
                    int[] limits={100,800,1200,4000,Integer.MAX_VALUE};
                    int max=limits[preset.getSelectedIndex()];
                    if(after>max)report.append("STYLE WARNING: ").append(after)
                        .append(" triangles exceeds the chosen ").append(max)
                        .append("-triangle visual target.\n");
                    if(sourceTextureBytes>512*1024)report.append("TEXTURE MEMORY WARNING: approx ")
                        .append(sourceTextureBytes/1024)
                        .append(" KiB decoded GLB RGBA texture pixels (excludes renderer/cache copies).\n");
                    report.append("Source: ").append(mesh.name()).append("  /  ").append(mesh.sourceBytes()).append(" bytes")
                          .append("\nGeometry: ").append(mesh.triangles().size()).append(" -> ").append(after)
                          .append(" triangles  |  estimated CPU triangle storage: ")
                          .append(result.sourceMemoryBytes()/1024).append(" -> ")
                          .append(result.optimizedMemoryBytes()/1024).append(" KiB\n");
                    report.append(review.topology().summary()).append("\n");
                    if(!authoringNote.isEmpty())report.append(authoringNote).append("\n");
                    for(var issue:review.topology().issues().stream().limit(12).toList())
                        report.append("TOPOLOGY: ").append(issue.code()).append(" #").append(issue.element()).append(" ").append(issue.detail()).append("\n");
                    if(review.topology().issueCount()>12)report.append("Additional topology findings omitted from this view.\n");
                    for(var part:result.parts())report.append(part.overRegression()?"!! ":"   ")
                        .append(part.name()).append(": ").append(part.before()).append(" -> ")
                        .append(part.after()).append(" triangles, ").append(part.verticesBefore())
                        .append(" -> ").append(part.verticesAfter()).append(" unique positions\n");
                    for(String warning:result.warnings())report.append("WARNING: ").append(warning).append("\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".glb")&&level>0)
                        report.append("WARNING: GLB material and named-node identities are not carried into regenerated fallback textures.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".obj"))
                        report.append("NOTE: OBJ source MTL/texture links are not yet converted; uses a generated low-res fallback atlas.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".stl"))
                        report.append("NOTE: STL contains no UV/material atlas; uses a generated low-res fallback atlas.\n");
                    details.setText(report.toString());details.setCaretPosition(0);
                    apply.setEnabled(after>0);
                    preview.repaint();
                }catch(Exception err){details.setText("REGRESSION ERROR: "+rootMessage(err));apply.setEnabled(false);}
            }
        }.execute();
    }
    /** Update budget commentary without rerunning expensive vertex clustering. */
    private void updateReport(){
        if(source==null||result==null)return;
        // Result is immutable; update the already visible budget line if preset changes
        // by recalculating in the background, not on the GUI paint thread.
        recalculate();
    }

    private void publish(){
        if(source==null||result==null||path==null||busy)return;
        final int remaining=result.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
        if(remaining==0)return;
        boolean lossyMaterial=source.name().toLowerCase(Locale.ROOT).endsWith(".glb")&&result.level()>0;
        if(result.overRegressed()||lossyMaterial){
            String msg=(result.overRegressed()?
                "OVER-REGRESSION: one or more named parts have collapsed.\n":"")+
                (lossyMaterial?"Original GLB material/part identity will be replaced by a fallback atlas.\n":"")+
                "The untouched source will be preserved. Publish this destructive-looking variant anyway?";
            if(JOptionPane.showConfirmDialog(dialog,msg,"Explicit lossy import warning",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        }
        final Path original=path;final EditorMeshPipeline.Result chosen=result;
        final String reviewed=reviewedSourceFingerprint;final EditorTopologyProject authoringProject=topologyProject;
        final EditorTopologyProject.State authoringState=authoringProject==null?null:authoringProject.snapshot();
        final var savedAuthoring=new java.util.concurrent.atomic.AtomicBoolean();
        final var savedSourceFingerprint=new java.util.concurrent.atomic.AtomicReference<String>();
        choose.setEnabled(false);apply.setEnabled(false);busy=true;refreshMeshControls();
        details.append("\nPublishing validated GLB derivative...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{
                byte[] sourceBytes=EditorMeshPipeline.readSource(original,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);
                if(!EditorMeshPipeline.sourceFingerprint(sourceBytes).equals(reviewed))throw new IOException("Model source changed after review; reopen it before publishing");
                byte[] nativeBytes=null;String expectedSource=reviewed;
                if(authoringProject!=null){
                    if(authoringProject.snapshot()!=authoringState)throw new IOException("Authoring mesh changed during publication");
                    authoringProject.save();savedAuthoring.set(true);nativeBytes=authoringProject.snapshotBytes();
                    if(original.toAbsolutePath().normalize().equals(authoringProject.documentPath())){
                        expectedSource=authoringProject.diskFingerprint();savedSourceFingerprint.set(expectedSource);
                    }
                }
                byte[] output=chosen.level()==0&&original.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".glb")
                    ?sourceBytes:EditorGlbWriter.encode(chosen.optimized());
                // Use the exact decoder that live gameplay will use. No mock preview-only mesh.
                GlbProps.decode(output);
                int after=chosen.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                return ImportedAssetLibrary.publishReviewed(original,output,chosen.level(),
                    chosen.source().triangles().size(),after,String.join(" | ",chosen.warnings()),expectedSource,nativeBytes);
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;refreshMeshControls();
                if(savedAuthoring.get()&&topologyProject==authoringProject){
                    meshDirty=false;
                    if(savedSourceFingerprint.get()!=null)reviewedSourceFingerprint=savedSourceFingerprint.get();
                    details.append("\nSAVED MESH "+authoringProject.documentPath());
                }
                try{
                    String id=get();published.accept(id);
                    details.append("\nPUBLISHED "+id+" to "+ImportedAssetLibrary.root()+
                        "\nSelect the model in the editor and place it with the PROP tool.");
                }catch(Exception failure){
                    details.append("\nPUBLISH FAILED: "+rootMessage(failure));
                    apply.setEnabled(true);
                }
            }
        }.execute();
    }
    private static String rootMessage(Throwable failure){
        while(failure.getCause()!=null)failure=failure.getCause();
        return String.valueOf(failure.getMessage());
    }
    /** Both panels use the SOURCE bounds: shrinking topology cannot trick the eye
     * by zooming the remaining stump to fill the frame. */
    private void draw(Graphics2D g,List<EditorMeshPipeline.Triangle> tris,int left,int width,String title){
        List<EditorMeshPipeline.Triangle> reference=title.equals("PRIMITIVE PREVIEW")?tris:source.triangles();
        if(reference.isEmpty()){g.setColor(Color.LIGHT_GRAY);g.drawString(title+" / empty",left+10,18);return;}
        double xmin=Double.POSITIVE_INFINITY,ymin=xmin,xmax=Double.NEGATIVE_INFINITY,ymax=xmax;
        for(var t:reference)for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            double ax=v.x()*.83-v.z()*.55,ay=-(v.y()*.95+v.x()*.24+v.z()*.35);
            xmin=Math.min(xmin,ax);xmax=Math.max(xmax,ax);
            ymin=Math.min(ymin,ay);ymax=Math.max(ymax,ay);
        }
        double scale=Math.min((width-26)/Math.max(1e-6,xmax-xmin),
                              (getPreviewHeight()-36)/Math.max(1e-6,ymax-ymin));
        double centerX=(xmin+xmax)*.5,centerY=(ymin+ymax)*.5;
        g.setColor(new Color(181,206,213));
        g.drawString(title+"  /  "+tris.size()+" triangles",left+10,18);
        int skip=Math.max(1,tris.size()/12000);
        g.setColor(new Color(113,168,188,120));
        for(int i=0;i<tris.size();i+=skip){
            var t=tris.get(i);
            int[] xs=new int[3],ys=new int[3];int k=0;
            for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                double ax=v.x()*.83-v.z()*.55,ay=-(v.y()*.95+v.x()*.24+v.z()*.35);
                xs[k]=left+width/2+(int)Math.round((ax-centerX)*scale);
                ys[k]=getPreviewHeight()/2+(int)Math.round((ay-centerY)*scale);k++;
            }
            g.drawPolygon(xs,ys,3);
        }
        if(title.equals("WORKING MESH")&&selectedVertex!=null){
            var v=selectedVertex;double ax=v.x()*.83-v.z()*.55,ay=-(v.y()*.95+v.x()*.24+v.z()*.35);
            int px=left+width/2+(int)Math.round((ax-centerX)*scale),py=getPreviewHeight()/2+(int)Math.round((ay-centerY)*scale);
            g.setColor(new Color(255,218,87));g.fillOval(px-4,py-4,8,8);g.drawString("v"+v.id(),px+7,py-6);
        }
        if(result!=null&&title.equals("PREVIEW")){
            java.util.Map<String,double[]> centroids=new java.util.LinkedHashMap<>();
            for(var part:result.parts())if(part.after()==0){
                double[] accum=new double[4];
                for(var t:reference)if(t.part().equals(part.name()))
                    for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                        accum[0]+=v.x();accum[1]+=v.y();accum[2]+=v.z();accum[3]++;
                    }
                if(accum[3]>0)centroids.put(part.name(),accum);
            }
            g.setColor(new Color(244,92,81));
            for(var part:centroids.entrySet()){
                double[] c=part.getValue();
                double a=c[0]/c[3],b=c[1]/c[3],d=c[2]/c[3];
                double ax=a*.83-d*.55,ay=-(b*.95+a*.24+d*.35);
                int px=left+width/2+(int)Math.round((ax-centerX)*scale);
                int py=getPreviewHeight()/2+(int)Math.round((ay-centerY)*scale);
                g.fillOval(px-4,py-4,8,8);
                g.drawString(part.getKey()+" — COLLAPSED",px+7,py-5);
            }
            if(tris.isEmpty()){
                g.setColor(Color.ORANGE);g.drawString("No drawable triangles remain",left+10,35);
            }
        }
    }
    private int getPreviewHeight(){return Math.max(180,preview.getHeight());}
}
