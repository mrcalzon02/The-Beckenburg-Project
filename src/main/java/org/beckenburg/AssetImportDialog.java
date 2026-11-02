package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorMeshPipeline;
import org.beckenburg.editor.EditorMeshTopology;
import org.beckenburg.editor.EditorTopologyProject;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.EditorMeshPrimitives;
import org.beckenburg.editor.EditorMeshPicking;

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
    private final JComboBox<EditorMeshPicking.Projection> meshView=new JComboBox<>(EditorMeshPicking.Projection.values());
    private final JComboBox<String> selectionMode=new JComboBox<>(new String[]{"VERTEX","EDGE","FACE"});
    private final JLabel selectionLabel=new JLabel("Click a triangle in the working mesh");
    private EditorMeshPicking.Hit selectedHit;
    private final JSpinner meshSnap=new JSpinner(new SpinnerNumberModel(.05,0d,1000d,.05));
    private record GizmoHandle(int axis,int x,int y,double anchorX,double anchorY,double unitX,double unitY){}
    private record AxisDrag(EditorTopologyProject.State before,long revision,java.util.Set<Long> ids,GizmoHandle handle,int startX,int startY,double grid){}
    private AxisDrag axisDrag;
    private double dragDelta;
    private final JToggleButton boxSelect=new JToggleButton("AREA SELECT");
    private final JComboBox<String> areaShape=new JComboBox<>(new String[]{"BOX","LASSO"});
    private final JCheckBox xraySelect=new JCheckBox("X-ray");
    private java.util.Set<Long> selectedVertices=java.util.Set.of();
    private Point boxStart,boxEnd;
    private int selectionGeneration;
    private List<Point> selectionPath;
    private boolean lassoGesture;
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
        JPanel actionRows=new JPanel(new BorderLayout());actionRows.add(vertexActions,BorderLayout.NORTH);
        vertexControls.add(vertexInputs,BorderLayout.NORTH);vertexControls.add(actionRows,BorderLayout.SOUTH);
        vertexInputs.add(new JLabel("Vertex ID"));vertexInputs.add(vertexId);
        vertexInputs.add(meshView);vertexInputs.add(selectionMode);vertexInputs.add(selectionLabel);
        meshView.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();preview.repaint();});selectionMode.addActionListener(e->{cancelAxisDrag();preview.repaint();});
        preview.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1){if(!beginAxisDrag(e.getX(),e.getY())){
                if(boxSelect.isSelected()&&!busy&&topologyProject!=null&&e.getX()<preview.getWidth()/2){boxStart=e.getPoint();boxEnd=e.getPoint();selectionPath=new ArrayList<>();selectionPath.add(e.getPoint());lassoGesture=areaShape.getSelectedIndex()==1;preview.repaint();}
                else pickMesh(e.getX(),e.getY());
            }}else {cancelAxisDrag();cancelBoxSelection();}}
            @Override public void mouseReleased(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1){if(axisDrag!=null)finishAxisDrag();else finishBoxSelection();}}
        });
        preview.addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseDragged(MouseEvent e){
            if(axisDrag!=null)updateAxisDrag(e.getX(),e.getY());else if(boxStart!=null){boxEnd=new Point(Math.max(0,Math.min(preview.getWidth()/2-1,e.getX())),Math.max(0,Math.min(preview.getHeight()-1,e.getY())));
                if(lassoGesture&&selectionPath.get(selectionPath.size()-1).distance(boxEnd)>=3&&selectionPath.size()<=2048)selectionPath.add(boxEnd);preview.repaint();}
        }});
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"cancel-axis-drag");
        dialog.getRootPane().getActionMap().put("cancel-axis-drag",new AbstractAction(){public void actionPerformed(ActionEvent e){cancelAxisDrag();cancelBoxSelection();}});
        for(var spinner:List.of(vertexId,vertexX,vertexY,vertexZ))spinner.setPreferredSize(new Dimension(65,24));
        for(var item:List.of(new Object[]{"X",vertexX},new Object[]{"Y",vertexY},new Object[]{"Z",vertexZ})){
            vertexInputs.add(new JLabel((String)item[0]));vertexInputs.add((JSpinner)item[1]);
        }
        vertexActions.add(moveVertex);vertexActions.add(undoMesh);vertexActions.add(redoMesh);vertexActions.add(saveMesh);vertexActions.add(reopenMesh);
        vertexActions.add(new JLabel("Move grid (0 off)"));meshSnap.setPreferredSize(new Dimension(65,24));vertexActions.add(meshSnap);
        JPanel areaActions=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));areaActions.add(boxSelect);areaActions.add(areaShape);areaActions.add(xraySelect);
        areaActions.add(new JLabel("Drag in the working mesh; Escape cancels. Selected vertices share the move gizmo."));actionRows.add(areaActions,BorderLayout.SOUTH);
        areaShape.addActionListener(e->cancelBoxSelection());
        boxSelect.setToolTipText("Drag a box or lasso in the working mesh; unchecked X-ray selects only visible vertices.");
        meshSnap.setToolTipText("Drag colored axis endpoints; Escape or right-click cancels. Grid snaps movement distance in world units.");
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
        meshView.setEnabled(!busy);selectionMode.setEnabled(editable);
        meshSnap.setEnabled(editable);
        boxSelect.setEnabled(editable);xraySelect.setEnabled(editable);areaShape.setEnabled(editable);
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
        cancelAxisDrag();
        cancelBoxSelection();
        if(meshDirty&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before creating a new mesh?","Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");
            var mesh=new EditorMeshPipeline.Mesh("New mesh",List.of(),0);
            var project=EditorTopologyProject.fromMesh(mesh,EditorMeshPipeline.sourceFingerprint("native-mesh-v1".getBytes(java.nio.charset.StandardCharsets.UTF_8)),document);
            project.save();clearPrimitivePreview();selectedHit=null;selectionLabel.setText("Click a triangle in the working mesh");topologyProject=project;meshHistory=new EditorHistory<>(project,60);source=mesh;result=null;
            path=document;reviewedSourceFingerprint=project.diskFingerprint();authoringNote="Native primitive authoring; generated fallback atlas.";sourceTextureBytes=0;meshDirty=false;
            vertexId.setValue(0L);selectVertex();refreshMeshControls();pathLabel.setText(document.getFileName()+" | native authoring");
            if(slider.getValue()!=0)slider.setValue(0);else recalculate();
        }catch(IOException|RuntimeException failure){details.append("\nNEW MESH FAILED: "+rootMessage(failure));}
    }
    private void selectVertex(){
        preview.repaint();selectedVertex=null;selectedVertices=java.util.Set.of();if(topologyProject==null)return;long id=((Number)vertexId.getValue()).longValue();
        for(var v:topologyProject.snapshot().vertices())if(v.id()==id){selectedVertex=v;selectedVertices=java.util.Set.of(id);vertexX.setValue(v.x());vertexY.setValue(v.y());vertexZ.setValue(v.z());return;}
    }
    private void meshEdit(Runnable operation){
        if(topologyProject==null||busy)return;
        try{meshHistory.execute(operation);meshChanged();}
        catch(RuntimeException failure){details.append("\nEDIT REFUSED: "+rootMessage(failure));refreshMeshControls();}
    }
    private void meshChanged(){
        var selected=selectedVertices;meshDirty=true;source=topologyProject.mesh();selectVertex();
        if(!selected.isEmpty()){var ids=new java.util.HashSet<Long>();for(var v:topologyProject.snapshot().vertices())if(selected.contains(v.id()))ids.add(v.id());selectedVertices=java.util.Set.copyOf(ids);}
        refreshMeshControls();
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
        cancelAxisDrag();
        cancelBoxSelection();
        selectedHit=null;selectionLabel.setText("Click a triangle in the working mesh");
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
    private EditorMeshPicking.Projection projection(){return (EditorMeshPicking.Projection)meshView.getSelectedItem();}
    private record Viewport(double centerX,double centerY,double scale,int left,int width,int height){
        int x(double value){return left+width/2+(int)Math.round((value-centerX)*scale);}
        int y(double value){return height/2+(int)Math.round((value-centerY)*scale);}
    }
    private Viewport viewport(List<EditorMeshPipeline.Triangle> reference,int left,int width){
        double xmin=Double.POSITIVE_INFINITY,ymin=xmin,xmax=Double.NEGATIVE_INFINITY,ymax=xmax;
        for(var t:reference)for(var v:List.of(t.a(),t.b(),t.c())){
            var p=projection().project(v.x(),v.y(),v.z());xmin=Math.min(xmin,p.x());xmax=Math.max(xmax,p.x());ymin=Math.min(ymin,p.y());ymax=Math.max(ymax,p.y());
        }
        double scale=Math.min((width-26)/Math.max(1e-6,xmax-xmin),(getPreviewHeight()-36)/Math.max(1e-6,ymax-ymin));
        return new Viewport((xmin+xmax)*.5,(ymin+ymax)*.5,scale,left,width,getPreviewHeight());
    }
    private void pickMesh(int x,int y){
        if(busy||topologyProject==null||source.triangles().isEmpty()||x<0||x>=preview.getWidth()/2)return;
        var view=viewport(source.triangles(),0,preview.getWidth()/2);
        double px=(x-view.left()-view.width()/2)/view.scale()+view.centerX(),py=(y-view.height()/2)/view.scale()+view.centerY();
        selectedHit=EditorMeshPicking.pick(topologyProject.snapshot(),projection(),px,py).orElse(null);
        if(selectedHit==null){selectedVertex=null;selectedVertices=java.util.Set.of();selectionLabel.setText("No triangle selected");preview.repaint();return;}
        if(selectionMode.getSelectedIndex()==0){vertexId.setValue(selectedHit.vertexId());selectVertex();}
        selectionLabel.setText(switch(selectionMode.getSelectedIndex()){
            case 0->"Vertex "+selectedHit.vertexId()+" / face "+selectedHit.faceId();
            case 1->"Edge "+selectedHit.edgeA()+" - "+selectedHit.edgeB();
            default->"Face "+selectedHit.faceId();});
        preview.repaint();
    }
    private java.util.Set<Long> selectedIds(){
        if(topologyProject==null)return java.util.Set.of();
        if(selectionMode.getSelectedIndex()==0)return selectedVertices;
        if(selectedHit==null)return java.util.Set.of();
        if(selectionMode.getSelectedIndex()==1)return java.util.Set.copyOf(List.of(selectedHit.edgeA(),selectedHit.edgeB()));
        return topologyProject.snapshot().faces().stream().filter(f->f.id()==selectedHit.faceId()).findFirst()
            .map(f->java.util.Set.copyOf(List.of(f.a().vertex(),f.b().vertex(),f.c().vertex()))).orElse(java.util.Set.of());
    }
    private List<GizmoHandle> gizmoHandles(){
        if(busy||topologyProject==null||source.triangles().isEmpty())return List.of();
        var ids=selectedIds();if(ids.isEmpty())return List.of();double x=0,y=0,z=0;int count=0;
        for(var v:topologyProject.snapshot().vertices())if(ids.contains(v.id())){x+=v.x();y+=v.y();z+=v.z();count++;}
        if(count!=ids.size())return List.of();
        var view=viewport(source.triangles(),0,preview.getWidth()/2);var anchor=projection().project(x/count,y/count,z/count);
        double ax=view.x(anchor.x()),ay=view.y(anchor.y());var zero=projection().project(0,0,0);var handles=new ArrayList<GizmoHandle>();
        for(int axis=0;axis<3;axis++){
            var p=projection().project(axis==0?1:0,axis==1?1:0,axis==2?1:0);double ux=(p.x()-zero.x())*view.scale(),uy=(p.y()-zero.y())*view.scale(),length=Math.hypot(ux,uy);
            if(length>1e-9)handles.add(new GizmoHandle(axis,(int)Math.round(ax+ux/length*48),(int)Math.round(ay+uy/length*48),ax,ay,ux,uy));
        }
        return handles;
    }
    private boolean beginAxisDrag(int x,int y){
        if(busy||x>=preview.getWidth()/2)return false;
        for(var h:gizmoHandles())if(Math.hypot(x-h.x(),y-h.y())<=8){
            axisDrag=new AxisDrag(topologyProject.snapshot(),topologyProject.revision(),selectedIds(),h,x,y,((Number)meshSnap.getValue()).doubleValue());dragDelta=0;preview.repaint();return true;
        }return false;
    }
    private void updateAxisDrag(int x,int y){
        if(axisDrag==null)return;var h=axisDrag.handle();
        double value=((x-axisDrag.startX())*h.unitX()+(y-axisDrag.startY())*h.unitY())/(h.unitX()*h.unitX()+h.unitY()*h.unitY());
        dragDelta=axisDrag.grid()>0?Math.rint(value/axisDrag.grid())*axisDrag.grid():value;
        selectionLabel.setText("Move "+"XYZ".charAt(h.axis())+" = "+String.format(Locale.ROOT,"%.4f",dragDelta));preview.repaint();
    }
    private void cancelAxisDrag(){if(axisDrag!=null)selectionLabel.setText("Move canceled");axisDrag=null;dragDelta=0;preview.repaint();}
    private void cancelBoxSelection(){selectionGeneration++;boxStart=null;boxEnd=null;selectionPath=null;preview.repaint();}
    private void finishBoxSelection(){
        if(boxStart==null||topologyProject==null)return;var start=boxStart;var end=boxEnd;boolean lasso=lassoGesture;var path=selectionPath==null?List.<Point>of():List.copyOf(selectionPath);cancelBoxSelection();
        if(source.triangles().isEmpty())return;int request=selectionGeneration;
        var project=topologyProject;var state=project.snapshot();long nativeRevision=project.revision();var selectedProjection=projection();boolean xray=xraySelect.isSelected();
        var view=viewport(source.triangles(),0,preview.getWidth()/2);
        double minX=(Math.min(start.x,end.x)-view.width()/2)/view.scale()+view.centerX(),maxX=(Math.max(start.x,end.x)-view.width()/2)/view.scale()+view.centerX();
        double minY=(Math.min(start.y,end.y)-view.height()/2)/view.scale()+view.centerY(),maxY=(Math.max(start.y,end.y)-view.height()/2)/view.scale()+view.centerY();
        var polygon=path.stream().map(p->new EditorMeshPicking.Point((p.x-view.width()/2)/view.scale()+view.centerX(),(p.y-view.height()/2)/view.scale()+view.centerY(),0)).toList();
        busy=true;choose.setEnabled(false);apply.setEnabled(false);refreshMeshControls();selectionLabel.setText("Selecting vertices...");
        new SwingWorker<java.util.Set<Long>,Void>(){
            protected java.util.Set<Long> doInBackground(){var query=EditorMeshPicking.prepare(state,selectedProjection);return lasso?query.verticesInLasso(polygon,xray):query.verticesInBox(minX,minY,maxX,maxY,xray);}
            protected void done(){
                busy=false;choose.setEnabled(true);
                try{
                    var ids=get();if(request!=selectionGeneration)throw new java.util.concurrent.CancellationException("Selection canceled");
                    if(topologyProject!=project||project.revision()!=nativeRevision||project.snapshot()!=state)throw new IOException("Mesh changed during selection");
                    selectionMode.setSelectedItem("VERTEX");selectedHit=null;
                    if(!ids.isEmpty()){vertexId.setValue(ids.iterator().next());selectVertex();}else selectedVertex=null;
                    selectedVertices=ids;selectionLabel.setText(ids.size()+" vertices / "+(xray?"X-ray":"visible"));
                }catch(java.util.concurrent.CancellationException canceled){selectionLabel.setText("Selection canceled");}
                catch(Exception failure){selectionLabel.setText("Selection refused");details.append("\nSELECTION REFUSED: "+rootMessage(failure));}
                refreshMeshControls();recalculate();preview.repaint();
            }
        }.execute();
    }
    private void finishAxisDrag(){
        if(axisDrag==null)return;var drag=axisDrag;double delta=dragDelta;axisDrag=null;dragDelta=0;preview.repaint();
        if(topologyProject.revision()!=drag.revision()||topologyProject.snapshot()!=drag.before()){selectionLabel.setText("Move refused");details.append("\nMOVE REFUSED: mesh changed during drag");return;}
        if(delta==0)return;int axis=drag.handle().axis();
        meshEdit(()->topologyProject.translateVertices(drag.ids(),axis==0?delta:0,axis==1?delta:0,axis==2?delta:0));
        selectionLabel.setText(topologyProject.snapshot()==drag.before()?"Move refused":"Moved "+"XYZ".charAt(axis)+" = "+String.format(Locale.ROOT,"%.4f",delta));
    }
    /** Regression panels share source bounds; the separate pending primitive has its own bounds. */
    private void draw(Graphics2D g,List<EditorMeshPipeline.Triangle> tris,int left,int width,String title){
        List<EditorMeshPipeline.Triangle> reference=title.equals("PRIMITIVE PREVIEW")?tris:source.triangles();
        if(reference.isEmpty()){g.setColor(Color.LIGHT_GRAY);g.drawString(title+" / empty",left+10,18);return;}
        var view=viewport(reference,left,width);
        g.setColor(new Color(181,206,213));
        g.drawString(title+"  /  "+tris.size()+" triangles",left+10,18);
        int skip=Math.max(1,tris.size()/12000);
        g.setColor(new Color(113,168,188,120));
        for(int i=0;i<tris.size();i+=skip){
            var t=tris.get(i);
            int[] xs=new int[3],ys=new int[3];int k=0;
            for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                var p=projection().project(v.x(),v.y(),v.z());xs[k]=view.x(p.x());ys[k]=view.y(p.y());k++;
            }
            g.drawPolygon(xs,ys,3);
        }
        if(title.equals("WORKING MESH")&&selectedVertex!=null&&selectedVertices.contains(selectedVertex.id())){
            var v=selectedVertex;var p=projection().project(v.x(),v.y(),v.z());int px=view.x(p.x()),py=view.y(p.y());
            g.setColor(new Color(255,218,87));g.fillOval(px-4,py-4,8,8);g.drawString("v"+v.id(),px+7,py-6);
        }
        if(title.equals("WORKING MESH")&&selectedVertices.size()>1){
            g.setColor(new Color(255,218,87));for(var v:topologyProject.snapshot().vertices())if(selectedVertices.contains(v.id())){
                var p=projection().project(v.x(),v.y(),v.z());g.fillOval(view.x(p.x())-2,view.y(p.y())-2,4,4);
            }
        }
        if(title.equals("WORKING MESH")&&selectedHit!=null&&selectionMode.getSelectedIndex()>0){
            var state=topologyProject.snapshot();var ids=new java.util.HashMap<Long,EditorTopologyProject.Vertex>();for(var v:state.vertices())ids.put(v.id(),v);
            var face=state.faces().stream().filter(f->f.id()==selectedHit.faceId()).findFirst().orElse(null);
            if(face!=null){
                long[] points=selectionMode.getSelectedIndex()==1?new long[]{selectedHit.edgeA(),selectedHit.edgeB()}:new long[]{face.a().vertex(),face.b().vertex(),face.c().vertex(),face.a().vertex()};
                g.setColor(new Color(255,218,87));g.setStroke(new BasicStroke(3));
                for(int i=1;i<points.length;i++){var a=ids.get(points[i-1]);var b=ids.get(points[i]);if(a==null||b==null)continue;
                    var pa=projection().project(a.x(),a.y(),a.z());var pb=projection().project(b.x(),b.y(),b.z());g.drawLine(view.x(pa.x()),view.y(pa.y()),view.x(pb.x()),view.y(pb.y()));}
                g.setStroke(new BasicStroke(1));
            }
        }
        if(title.equals("WORKING MESH")){
            if(boxStart!=null&&boxEnd!=null){g.setColor(new Color(255,218,87));
                if(lassoGesture&&selectionPath!=null){for(int i=1;i<selectionPath.size();i++)g.drawLine(selectionPath.get(i-1).x,selectionPath.get(i-1).y,selectionPath.get(i).x,selectionPath.get(i).y);g.drawLine(boxEnd.x,boxEnd.y,boxStart.x,boxStart.y);}
                else g.drawRect(Math.min(boxStart.x,boxEnd.x),Math.min(boxStart.y,boxEnd.y),Math.abs(boxStart.x-boxEnd.x),Math.abs(boxStart.y-boxEnd.y));}
            Color[] colors={new Color(255,100,100),new Color(100,235,130),new Color(110,165,255)};
            for(var h:gizmoHandles()){
                g.setColor(colors[h.axis()]);g.setStroke(new BasicStroke(2));g.drawLine((int)h.anchorX(),(int)h.anchorY(),h.x(),h.y());g.fillRect(h.x()-4,h.y()-4,8,8);g.drawString("XYZ".substring(h.axis(),h.axis()+1),h.x()+6,h.y()-5);
                if(axisDrag!=null&&axisDrag.handle().axis()==h.axis()){
                    int dx=(int)Math.round(h.anchorX()+h.unitX()*dragDelta),dy=(int)Math.round(h.anchorY()+h.unitY()*dragDelta);
                    g.drawOval(dx-6,dy-6,12,12);g.drawString("release to commit / Esc cancels",dx+10,dy+12);
                }
            }g.setStroke(new BasicStroke(1));
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
                var p=projection().project(a,b,d);int px=view.x(p.x()),py=view.y(p.y());
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
