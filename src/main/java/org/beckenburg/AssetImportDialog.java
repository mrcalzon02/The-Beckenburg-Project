package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorDocumentStore;
import org.beckenburg.editor.EditorTopologyWorkspace;
import org.beckenburg.editor.EditorMaterialWorkspace;
import org.beckenburg.editor.EditorMeshPipeline;
import org.beckenburg.editor.EditorMeshNormals;
import org.beckenburg.editor.EditorMeshBasis;
import org.beckenburg.editor.EditorMeshFrame;
import org.beckenburg.editor.EditorMeshMaterial;
import org.beckenburg.editor.EditorObjMaterials;
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
import java.util.LinkedHashMap;
import java.util.Map;
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
            if(source!=null)draw(p,source.triangles(),0,workingViewportWidth(),topologyProject==null?"SOURCE":"WORKING MESH");
            if(modelingMesh!=null)draw(p,modelingMesh.triangles(),getWidth()/2,getWidth()-getWidth()/2,"MODEL EDIT PREVIEW");
            else if(primitivePreview!=null)draw(p,primitivePreview.triangles(),getWidth()/2,getWidth()-getWidth()/2,"PRIMITIVE PREVIEW");
            else if(result!=null&&comparisonVisible())draw(p,result.optimized().triangles(),workingViewportWidth(),getWidth()-workingViewportWidth(),"PREVIEW");
            p.dispose();
        }
    };
    private final JSpinner vertexId=new JSpinner(new SpinnerNumberModel(0L,0L,Long.MAX_VALUE,1L));
    private final JSpinner vertexX=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexY=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexZ=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JButton moveVertex=new JButton("MOVE VERTEX"),undoMesh=new JButton("UNDO MESH"),redoMesh=new JButton("REDO MESH");
    private final JButton saveMesh=new JButton("SAVE MESH"),reopenMesh=new JButton("REOPEN MESH"),exportMesh=new JButton("EXPORT OBJ"),exportGlb=new JButton("EXPORT GLB");
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
    private EditorMeshPicking.Hit selectedHit,hoverHit;
    private EditorTopologyProject.State pickingState,hoverState;
    private EditorMeshPicking.Projector pickingCamera,hoverCamera;
    private EditorMeshPicking.Prepared preparedPicking;
    private Viewport hoverViewport;
    private int hoverMode;
    private record ViewKey(EditorMeshPicking.Projector camera,int left,int width,int height,double panX,double panY,double zoom){}
    private final java.util.IdentityHashMap<List<EditorMeshPipeline.Triangle>,java.util.Map<ViewKey,Viewport>> viewportCache=new java.util.IdentityHashMap<>();
    private Long normalAnchor;
    private final JSpinner meshSnap=new JSpinner(new SpinnerNumberModel(.05,0d,1000d,.05));
    private record GizmoHandle(int axis,int x,int y,double anchorX,double anchorY,double unitX,double unitY,double worldX,double worldY,double worldZ,double scale,EditorMeshPicking.Projector camera,EditorMeshPipeline.Normal direction){}
    private record AxisDrag(EditorTopologyProject.State before,long revision,java.util.Set<Long> ids,GizmoHandle handle,int startX,int startY,double grid){}
    private AxisDrag axisDrag;
    private double cameraYaw=35,cameraPitch=25,cameraZoom=1,cameraPanX,cameraPanY;
    private Point cameraGesture;private int cameraButton;
    private final JButton fitCamera=new JButton("Fit view");
    private List<EditorMeshPipeline.Triangle> cameraReference;
    private EditorMeshPicking.Camera cachedCamera;
    private double cachedYaw,cachedPitch;

    private double dragDelta;
    private final JToggleButton boxSelect=new JToggleButton("AREA SELECT");
    private final JComboBox<String> areaShape=new JComboBox<>(new String[]{"BOX","LASSO"});
    private final JCheckBox xraySelect=new JCheckBox("X-ray"),compareView=new JCheckBox("Compare"),normalOverlay=new JCheckBox("Normals");
    private java.util.Set<Long> selectedVertices=java.util.Set.of();
    private Point boxStart,boxEnd;
    private int selectionGeneration;
    private List<Point> selectionPath;
    private boolean lassoGesture;
    private final JComboBox<EditorTopologyProject.FaceEdit> faceOperation=new JComboBox<>(EditorTopologyProject.FaceEdit.values());
    private final JSpinner faceAmount=new JSpinner(new SpinnerNumberModel(.25,-10000d,10000d,.05));
    private final JButton selectBoundary = new JButton("SELECT BOUNDARY LOOP");
    private EditorTopologyProject.Observation boundaryObservation;
    private List<EditorTopologyProject.Vertex> boundaryVertices = List.of();
    private java.util.Set<Long> boundaryIds = java.util.Set.of();
    private final JSpinner slideFraction = new JSpinner(new SpinnerNumberModel(.25, .001, .999, .05));
    private final JCheckBox reverseSlide = new JCheckBox("Reverse edge direction");
    private final JButton previewSlide = new JButton("PREVIEW VERTEX SLIDE");
    private final JSpinner bevelDepth=new JSpinner(new SpinnerNumberModel(.1,.000001,10000d,.05));
    private final JButton previewBevel=new JButton("PREVIEW BEVEL");
    private final JButton previewFaceEdit=new JButton("PREVIEW FACE EDIT"),applyFaceEdit=new JButton("APPLY MODEL EDIT"),cancelFaceEdit=new JButton("CANCEL MODEL EDIT");
    private EditorTopologyProject.EditPreview modelingPreview;
    private EditorMeshPipeline.Mesh modelingMesh;
    private List<EditorMeshPipeline.Triangle> modelingBounds;
    private int modelingGeneration;
    private boolean modelingPreparing;
    private final JButton previewWeld=new JButton("PREVIEW WELD COINCIDENT"),previewDegenerates=new JButton("PREVIEW REMOVE DEGENERATES"),previewUnused=new JButton("PREVIEW REMOVE UNUSED");
    private final JComboBox<EditorTopologyProject.Transform> transformKind=new JComboBox<>(EditorTopologyProject.Transform.values());
    private final JComboBox<String> transformSpace=new JComboBox<>(new String[]{"World","Surface normal","Local parts"});
    private final JComboBox<String> transformAxis=new JComboBox<>(new String[]{"X","Y","Z"}),transformPivot=new JComboBox<>(new String[]{"Selection centroid","World origin"});
    private final JSpinner transformAmount=new JSpinner(new SpinnerNumberModel(90d,-36000d,1000000d,1d)),angleSnap=new JSpinner(new SpinnerNumberModel(15d,0d,360d,1d));
    private final JButton previewTransform=new JButton("PREVIEW TRANSFORM");
    private final JComboBox<EditorMeshMaterial> materialList=new JComboBox<>();
    private final JComboBox<String> materialWrap=new JComboBox<>(new String[]{"Repeat","Clamp to edge","Mirrored repeat"});
    private final JComboBox<EditorMeshMaterial.Alpha> materialAlpha=new JComboBox<>(EditorMeshMaterial.Alpha.values());
    private final JComboBox<EditorMeshMaterial.Origin> materialOrigin=new JComboBox<>(EditorMeshMaterial.Origin.values());
    private final JButton chooseMaterial=new JButton("CHOOSE PNG / PREVIEW"),previewMaterial=new JButton("PREVIEW FACE BINDING");
    private final JLabel materialSwatch=new JLabel("Select a face and a material");
    private JDialog materialDialog;
    private EditorMaterialWorkspace materialWorkspace;
    private final JButton openMaterials=new JButton("MATERIALS / UV...");
    private final JLabel materialRuntime=new JLabel("Actual runtime preview",SwingConstants.CENTER);
    private final JSpinner materialRotation=new JSpinner(new SpinnerNumberModel(0d,-360d,360d,15d));
    private final JComboBox<EditorTopologyProject.UvTransform> uvOperation=new JComboBox<>(EditorTopologyProject.UvTransform.values());
    private final JComboBox<String> uvScope=new JComboBox<>(new String[]{"Selected face","Connected UV island"}),uvPivot=new JComboBox<>(new String[]{"UV bounds center","UV origin"});
    private final JSpinner uvFirst=new JSpinner(new SpinnerNumberModel(.1,-1000000d,1000000d,.05)),uvSecond=new JSpinner(new SpinnerNumberModel(0d,-1000000d,1000000d,.05));
    private final JLabel uvFirstLabel=new JLabel("U distance"),uvSecondLabel=new JLabel("V distance");
    private final JButton previewUv=new JButton("PREVIEW UV TRANSFORM");
    private JTabbedPane materialTools;
    private EditorTopologyProject.State materialRenderState;
    private double materialRenderedRotation=Double.NaN;
    private boolean materialRendering;
    private EditorTopologyProject.State listedMaterials;
    private EditorObjMaterials.Capture sourceCapture;
    private EditorTopologyWorkspace workspace;
    private record MeshPart(String name,int vertices,int faces,String label){@Override public String toString(){return label+" / "+faces+" triangles";}}
    private final JTextField partSearch=new JTextField();
    private final DefaultListModel<MeshPart> partRows=new DefaultListModel<>();
    private final JList<MeshPart> partList=new JList<>(partRows);
    private EditorTopologyProject.State listedParts;
    private boolean updatingParts;

    private final JPanel inspectorCards=new JPanel(new CardLayout());
    private EditorTopologyProject topologyProject;
    private EditorTopologyProject.Observation workingObservation;
    private EditorTopologyProject.Vertex selectedVertex;
    private EditorHistory<EditorTopologyProject.State> meshHistory;
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
        dialog=new JDialog(parent,"Model Editor — Mesh & Topology",false);
        JPanel content=new JPanel(new BorderLayout(5,5));
        choose.addActionListener(e->openSource());
        slider.setPaintTicks(true);slider.setMajorTickSpacing(2);slider.setPaintLabels(true);
        slider.setToolTipText("Level 0 preserves source geometry; 12 intentionally collapses small parts. This is not a triangle-budget target.");
        slider.addChangeListener(e->{if(!slider.getValueIsAdjusting()&&source!=null)recalculate();});
        preset.setToolTipText("Advisory visual/style budget only; never a hard release gate");
        preset.addActionListener(e->{if(source!=null&&result!=null)updateReport();});
        JButton reset=new JButton("RESET");
        reset.addActionListener(e->slider.setValue(0));

        normalOverlay.setToolTipText("Actual per-corner normals; derived face directions where none are authored. Large meshes use a bounded display sample.");normalOverlay.addActionListener(e->preview.repaint());
        compareView.setToolTipText("Compare working geometry with the reduction preview; pending model edits always show both views.");compareView.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();preview.repaint();});
        meshView.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();cameraGesture=null;cameraZoom=1;cameraPanX=0;cameraPanY=0;fitCamera.setVisible(projection()==EditorMeshPicking.Projection.PERSPECTIVE);preview.repaint();});
        meshView.setToolTipText("Perspective: right-drag to orbit, middle-drag to pan, wheel to zoom. Fit view restores framing.");
        fitCamera.setVisible(false);fitCamera.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();cameraZoom=1;cameraPanX=0;cameraPanY=0;preview.repaint();});selectionMode.addActionListener(e->{clearBoundary();cancelAxisDrag();refreshMeshControls();preview.repaint();});
        preview.addMouseListener(new MouseAdapter(){
            @Override public void mouseExited(MouseEvent e){clearHover();}
            @Override public void mousePressed(MouseEvent e){clearHover();if(e.getButton()==MouseEvent.BUTTON1){if(!beginAxisDrag(e.getX(),e.getY())){
                if(boxSelect.isSelected()&&!busy&&modelingPreview==null&&topologyProject!=null&&e.getX()<workingViewportWidth()){boxStart=e.getPoint();boxEnd=e.getPoint();selectionPath=new ArrayList<>();selectionPath.add(e.getPoint());lassoGesture=areaShape.getSelectedIndex()==1;preview.repaint();}
                else pickMesh(e.getX(),e.getY());
            }}else {cancelAxisDrag();cancelBoxSelection();if(source!=null&&projection()==EditorMeshPicking.Projection.PERSPECTIVE){cameraGesture=e.getPoint();cameraButton=e.getButton();}}}
            @Override public void mouseReleased(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1){if(axisDrag!=null)finishAxisDrag();else finishBoxSelection();}else cameraGesture=null;}
        });
        preview.addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseMoved(MouseEvent e){hoverMesh(e.getX(),e.getY());}
            @Override public void mouseDragged(MouseEvent e){clearHover();
            if(cameraGesture!=null){int dx=e.getX()-cameraGesture.x,dy=e.getY()-cameraGesture.y;cameraGesture=e.getPoint();
                if(cameraButton==MouseEvent.BUTTON3){cameraYaw=Math.IEEEremainder(cameraYaw+dx*.5,360);cameraPitch=Math.max(-85,Math.min(85,cameraPitch+dy*.5));}
                else if(cameraButton==MouseEvent.BUTTON2){var view=viewport(workingReference(),0,workingViewportWidth());cameraPanX-=dx/view.scale();cameraPanY-=dy/view.scale();}preview.repaint();return;}
            if(axisDrag!=null)updateAxisDrag(e.getX(),e.getY());else if(boxStart!=null){boxEnd=new Point(Math.max(0,Math.min(workingViewportWidth()-1,e.getX())),Math.max(0,Math.min(preview.getHeight()-1,e.getY())));
                if(lassoGesture&&selectionPath.get(selectionPath.size()-1).distance(boxEnd)>=3&&selectionPath.size()<=2048)selectionPath.add(boxEnd);preview.repaint();}
        }});
        preview.addMouseWheelListener(e->{if(source==null||projection()!=EditorMeshPicking.Projection.PERSPECTIVE)return;cancelAxisDrag();cancelBoxSelection();cameraZoom=Math.max(.1,Math.min(20,cameraZoom*Math.pow(1.12,-e.getPreciseWheelRotation())));preview.repaint();});
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"cancel-axis-drag");
        dialog.getRootPane().getActionMap().put("cancel-axis-drag",new AbstractAction(){public void actionPerformed(ActionEvent e){cancelAxisDrag();cancelBoxSelection();cameraGesture=null;clearModelingPreview();}});
        for(var spinner:List.of(vertexId,vertexX,vertexY,vertexZ))spinner.setPreferredSize(new Dimension(65,24));
        areaShape.addActionListener(e->cancelBoxSelection());
        boxSelect.setToolTipText("Drag a box or lasso in the working mesh; unchecked X-ray selects only visible vertices.");
        meshSnap.setToolTipText("Drag colored axis endpoints; Escape or right-click cancels. Grid snaps movement distance in world units.");
        vertexId.addChangeListener(e->selectVertex());
        moveVertex.addActionListener(e->meshEdit(()->topologyProject.moveVertex(((Number)vertexId.getValue()).longValue(),
            ((Number)vertexX.getValue()).doubleValue(),((Number)vertexY.getValue()).doubleValue(),((Number)vertexZ.getValue()).doubleValue())));
        undoMesh.addActionListener(e->{if(!busy&&meshHistory!=null&&meshHistory.undo())meshChanged();});
        redoMesh.addActionListener(e->{if(!busy&&meshHistory!=null&&meshHistory.redo())meshChanged();});
        saveMesh.addActionListener(e->saveAuthoring());
        exportMesh.addActionListener(e->chooseObjExport());
        exportGlb.addActionListener(e->{if(topologyProject==null||busy||modelingPreview!=null)return;var chooser=new JFileChooser();chooser.setDialogTitle("Export source-preserving GLB copy");chooser.setSelectedFile(topologyProject.documentPath().resolveSibling("mesh-export.glb").toFile());if(chooser.showSaveDialog(dialog)==JFileChooser.APPROVE_OPTION)exportGlb(chooser.getSelectedFile().toPath());});
        reopenMesh.addActionListener(e->meshEdit(()->{try{
            topologyProject.reopen();
            if(path!=null&&path.toAbsolutePath().normalize().equals(topologyProject.documentPath()))reviewedSourceFingerprint=topologyProject.diskFingerprint();
        }catch(IOException failure){throw new java.io.UncheckedIOException(failure);}}));
        for(var item:List.of(new Object[]{"W",primitiveWidth},new Object[]{"H",primitiveHeight},new Object[]{"D",primitiveDepth},
                new Object[]{"Segments",primitiveSegments},new Object[]{"Rings/grid",primitiveRings})){
            var spinner=(JSpinner)item[1];spinner.setPreferredSize(new Dimension(58,24));
            spinner.addChangeListener(e->clearPrimitivePreview());
        }
        primitiveKind.addActionListener(e->clearPrimitivePreview());
        newMesh.addActionListener(e->newAuthoringMesh());previewPrimitive.addActionListener(e->previewPrimitive());
        applyPrimitive.addActionListener(e->{if(reviewedPrimitive!=null){var parameters=reviewedPrimitive;meshEdit(()->topologyProject.appendPrimitive(parameters));clearPrimitivePreview();}});
        faceAmount.setToolTipText("Select a FACE. Extrude: signed normal distance. Inset: fraction greater than0 and less than1.");
        faceOperation.addActionListener(e->clearModelingPreview());faceAmount.addChangeListener(e->clearModelingPreview());
        materialSwatch.setPreferredSize(new Dimension(250,112));
        materialList.setRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){putClientProperty("html.disable",true);return super.getListCellRendererComponent(list,value,index,selected,focus);}});
        materialList.addActionListener(e->{refreshMaterialSwatch();refreshMaterialWindow();});
        openMaterials.addActionListener(e->openMaterials());
        uvOperation.addActionListener(e->{clearModelingPreview();var operation=(EditorTopologyProject.UvTransform)uvOperation.getSelectedItem();uvFirstLabel.setText(operation==EditorTopologyProject.UvTransform.ROTATE?"Degrees":operation==EditorTopologyProject.UvTransform.SCALE?"U factor":"U distance");uvSecondLabel.setText(operation==EditorTopologyProject.UvTransform.SCALE?"V factor":"V distance");uvSecond.setVisible(operation!=EditorTopologyProject.UvTransform.ROTATE);uvSecondLabel.setVisible(operation!=EditorTopologyProject.UvTransform.ROTATE);uvFirst.setValue(operation==EditorTopologyProject.UvTransform.ROTATE?90d:operation==EditorTopologyProject.UvTransform.SCALE?1d:.1);uvSecond.setValue(operation==EditorTopologyProject.UvTransform.SCALE?1d:0d);});
        uvScope.addActionListener(e->clearModelingPreview());uvPivot.addActionListener(e->clearModelingPreview());uvFirst.addChangeListener(e->clearModelingPreview());uvSecond.addChangeListener(e->clearModelingPreview());
        previewUv.addActionListener(e->{if(selectedHit==null||topologyProject==null||busy)return;long face=selectedHit.faceId();boolean island=uvScope.getSelectedIndex()==1,origin=uvPivot.getSelectedIndex()==1;var operation=(EditorTopologyProject.UvTransform)uvOperation.getSelectedItem();double first=((Number)uvFirst.getValue()).doubleValue(),second=operation==EditorTopologyProject.UvTransform.ROTATE?0:((Number)uvSecond.getValue()).doubleValue();beginModelingPreview(p->p.previewUvTransform(face,island,operation,first,second,origin));});
        chooseMaterial.addActionListener(e->{
            if(busy||topologyProject==null||selectedHit==null)return;
            var chooser=new JFileChooser();chooser.setDialogTitle("Embed PNG material in native mesh");chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG texture","png"));
            if(chooser.showOpenDialog(dialog)==JFileChooser.APPROVE_OPTION)previewPngMaterial(chooser.getSelectedFile().toPath());
        });
        previewMaterial.addActionListener(e->{if(selectedHit!=null&&materialList.getSelectedItem() instanceof EditorMeshMaterial material){long face=selectedHit.faceId();beginModelingPreview(p->p.previewBindMaterial(face,material));}});
        previewFaceEdit.addActionListener(e->previewModelingEdit());cancelFaceEdit.addActionListener(e->clearModelingPreview());
        selectBoundary.addActionListener(e -> selectBoundaryLoop());
        slideFraction.addChangeListener(e -> clearModelingPreview());
        reverseSlide.addActionListener(e -> clearModelingPreview());
        previewSlide.addActionListener(e -> {
            if (busy || selectedHit == null || selectionMode.getSelectedIndex() != 1) return;
            long from = reverseSlide.isSelected() ? selectedHit.edgeB() : selectedHit.edgeA();
            long toward = reverseSlide.isSelected() ? selectedHit.edgeA() : selectedHit.edgeB();
            double fraction = ((Number) slideFraction.getValue()).doubleValue();
            beginModelingPreview(p -> p.previewVertexSlide(from, toward, fraction));
        });
        bevelDepth.addChangeListener(e->clearModelingPreview());
        previewBevel.addActionListener(e->{if(busy||topologyProject==null||selectedHit==null||selectionMode.getSelectedIndex()==2)return;var hit=selectedHit;boolean corner=selectionMode.getSelectedIndex()==0;double depth=((Number)bevelDepth.getValue()).doubleValue();beginModelingPreview(p->p.previewBevel(corner?hit.vertexId():hit.edgeA(),corner?hit.vertexId():hit.edgeB(),hit.faceId(),depth));});
        applyFaceEdit.addActionListener(e->{if(modelingPreview!=null&&!busy){var edit=modelingPreview;meshEdit(()->topologyProject.applyPreview(edit));if(topologyProject.snapshot()==edit.snapshot()){selectedHit=null;selectionLabel.setText("Model edit committed");}clearModelingPreview();}});
        previewWeld.setToolTipText("Select vertices with X-ray/area selection. Exact coincident positions weld per part; UV seams stay per corner.");
        previewWeld.addActionListener(e->{var ids=java.util.Set.copyOf(selectedVertices);beginModelingPreview(p->p.previewWeld(ids));});
        previewDegenerates.addActionListener(e->beginModelingPreview(EditorTopologyProject::previewRemoveDegenerates));
        previewUnused.addActionListener(e->beginModelingPreview(EditorTopologyProject::previewRemoveUnusedVertices));
        transformKind.addActionListener(e->{clearModelingPreview();transformAmount.setValue(transformKind.getSelectedItem()==EditorTopologyProject.Transform.ROTATE?90d:1d);});
        transformSpace.setToolTipText("Surface uses the active native face. Local uses persisted part axes/pivots, including original GLB node transforms; local movement units follow each frame axis scale.");
        transformSpace.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();clearModelingPreview();int axis=transformAxis.getSelectedIndex(),pivot=transformPivot.getSelectedIndex();transformAxis.setModel(new DefaultComboBoxModel<>(transformSpace.getSelectedIndex()!=1?new String[]{"X","Y","Z"}:new String[]{"Tangent","Bitangent","Normal"}));transformAxis.setSelectedIndex(Math.max(0,axis));transformPivot.setModel(new DefaultComboBoxModel<>(new String[]{transformSpace.getSelectedIndex()==2?"Per-part centroid":"Selection centroid",transformSpace.getSelectedIndex()==0?"World origin":transformSpace.getSelectedIndex()==1?"Active face center":"Part origin"}));transformPivot.setSelectedIndex(Math.max(0,pivot));if(transformSpace.getSelectedIndex()==1&&surfaceBasis()==null)selectionLabel.setText("Pick a triangle to anchor surface-normal axes");refreshMeshControls();preview.repaint();});
        transformAxis.addActionListener(e->clearModelingPreview());transformPivot.addActionListener(e->clearModelingPreview());transformAmount.addChangeListener(e->clearModelingPreview());angleSnap.addChangeListener(e->clearModelingPreview());
        previewTransform.addActionListener(e->{
            var ids=java.util.Set.copyOf(selectedIds());var operation=(EditorTopologyProject.Transform)transformKind.getSelectedItem();int axis=transformAxis.getSelectedIndex();boolean origin=transformPivot.getSelectedIndex()==1;
            double value=((Number)transformAmount.getValue()).doubleValue(),snap=((Number)angleSnap.getValue()).doubleValue();
            if(operation==EditorTopologyProject.Transform.ROTATE&&snap>0)value=Math.rint(value/snap)*snap;double reviewed=value;
            Long face=activeSurfaceFace();boolean surface=transformSpace.getSelectedIndex()==1,local=transformSpace.getSelectedIndex()==2;if(surface&&face==null){selectionLabel.setText("Pick a triangle to anchor surface-normal axes");return;}
            beginModelingPreview(p->local?p.previewLocalTransform(ids,operation,axis,reviewed,origin):surface?p.previewTransform(ids,operation,axis,reviewed,origin,face):p.previewTransform(ids,operation,axis,reviewed,origin));
        });
        details.setEditable(false);details.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,4));
        JButton close=new JButton("CLOSE");close.addActionListener(e->requestClose());
        apply.setEnabled(false);apply.addActionListener(e->publish());
        footer.add(new JLabel("Original retained; source and derivative are stored separately"));
        footer.add(apply);footer.add(close);
        content.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        // Existing native controls and actions, arranged around the modeling viewport.
        var menus=new JMenuBar();menus.add(EditorTopologyWorkspace.menu("File",newMesh,choose,saveMesh,reopenMesh,exportMesh,exportGlb,close));menus.add(EditorTopologyWorkspace.menu("Edit",undoMesh,redoMesh));
        EditorTopologyWorkspace.shortcut(menus.getMenu(0),1,KeyEvent.VK_O);EditorTopologyWorkspace.shortcut(menus.getMenu(0),2,KeyEvent.VK_S);EditorTopologyWorkspace.shortcut(menus.getMenu(1),0,KeyEvent.VK_Z);EditorTopologyWorkspace.shortcut(menus.getMenu(1),1,KeyEvent.VK_Y);
        var inspector=new JPanel(new BorderLayout(0,8));var toolTitle=new JLabel("SELECT");inspector.add(toolTitle,BorderLayout.NORTH);inspector.add(new JScrollPane(inspectorCards),BorderLayout.CENTER);inspector.add(EditorTopologyWorkspace.column(applyFaceEdit,cancelFaceEdit),BorderLayout.SOUTH);
        inspector.setBorder(BorderFactory.createEmptyBorder(8,5,5,5));
        inspectorCards.add("Select",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Area shape",areaShape,"Occlusion",xraySelect),boxSelect,selectBoundary,new JLabel("Drag in the working view. Escape cancels.")));
        inspectorCards.add("Move",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Vertex ID",vertexId,"X",vertexX,"Y",vertexY,"Z",vertexZ,"Grid (0 off)",meshSnap),moveVertex,EditorTopologyWorkspace.form("Slide fraction",slideFraction),reverseSlide,previewSlide));
        inspectorCards.add("Transform",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Space",transformSpace,"Operation",transformKind,"Axis",transformAxis,"Degrees / factor",transformAmount,"Pivot",transformPivot,"Angle snap",angleSnap),previewTransform));
        inspectorCards.add("Create",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Primitive",primitiveKind,"Width",primitiveWidth,"Height",primitiveHeight,"Depth",primitiveDepth,"Segments",primitiveSegments,"Rings / grid",primitiveRings),previewPrimitive,applyPrimitive));
        inspectorCards.add("Face",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Operation",faceOperation,"Distance / fraction",faceAmount),previewFaceEdit));
        inspectorCards.add("Bevel",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Cut depth",bevelDepth),previewBevel,new JLabel("Pick one EDGE or VERTEX in the working view."),new JLabel("Closed convex support; one flat cut."),new JLabel("Cap: active face material, planar UVs.")));
        inspectorCards.add("Repair",EditorTopologyWorkspace.column(previewWeld,previewDegenerates,previewUnused));
        inspectorCards.add("Review",EditorTopologyWorkspace.column(new JLabel("Geometry regression (0 = original)"),slider,reset,preset));
        var toolsBar=new JPanel(new GridLayout(0,1,2,3));ButtonGroup toolModes=new ButtonGroup();
        String[] toolNames={"Select","Move","Transform","Create","Face","Bevel","Repair","Review"};
        for(int i=0;i<toolNames.length;i++){int index=i;var button=new JToggleButton(toolNames[i]);button.setMargin(new Insets(8,5,8,5));button.setToolTipText("Show "+toolNames[i].toLowerCase(Locale.ROOT)+" settings");button.addActionListener(e->{((CardLayout)inspectorCards.getLayout()).show(inspectorCards, index==2?"Transform":toolNames[index]);toolTitle.setText(toolNames[index].toUpperCase(Locale.ROOT));workspace.setInspectorVisible(true);});toolModes.add(button);toolsBar.add(button);if(i==0)button.setSelected(true);}
        var toolsDock=new JPanel(new CardLayout());var toolsPanel=new JPanel(new BorderLayout());toolsPanel.add(toolsBar,BorderLayout.NORTH);toolsDock.add(toolsPanel,"Tools");
        var partsPanel=new JPanel(new BorderLayout(3,5));partsPanel.setBorder(BorderFactory.createEmptyBorder(3,2,3,2));var partsHead=EditorTopologyWorkspace.column(new JLabel("NATIVE MESH PARTS"),partSearch);partsPanel.add(partsHead,BorderLayout.NORTH);partsPanel.add(new JScrollPane(partList),BorderLayout.CENTER);toolsDock.add(partsPanel,"Parts");toolsDock.setPreferredSize(new Dimension(76,500));
        partSearch.setToolTipText("Filter parts from the current native mesh");partSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){refreshPartRows();}public void removeUpdate(javax.swing.event.DocumentEvent e){refreshPartRows();}public void changedUpdate(javax.swing.event.DocumentEvent e){refreshPartRows();}});
        partList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);partList.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&!updatingParts&&partList.getSelectedValue()!=null)selectPart(partList.getSelectedValue().name());});
        var partsToggle=new JButton("Mesh parts");partsToggle.addActionListener(e->{boolean show=toolsDock.getPreferredSize().width<100;((CardLayout)toolsDock.getLayout()).show(toolsDock,show?"Parts":"Tools");toolsDock.setPreferredSize(new Dimension(show?195:76,500));toolsDock.revalidate();});
        var context=new JPanel(new BorderLayout(6,0));var viewOptions=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2));viewOptions.add(meshView);viewOptions.add(fitCamera);viewOptions.add(selectionMode);viewOptions.add(compareView);viewOptions.add(normalOverlay);context.add(viewOptions,BorderLayout.WEST);context.add(selectionLabel,BorderLayout.CENTER);
        var viewportArea=new JPanel(new BorderLayout(0,3));viewportArea.add(context,BorderLayout.NORTH);viewportArea.add(preview,BorderLayout.CENTER);
        var browsers=new JTabbedPane();browsers.addTab("Diagnostics",new JScrollPane(details));
        workspace=new EditorTopologyWorkspace(viewportArea,toolsDock,inspector,browsers,290);
        var diagnostics=new JButton("Diagnostics");diagnostics.addActionListener(e->workspace.setBrowsersVisible(!workspace.browsersVisible()));
        var focus=new JButton("Focus view");focus.setToolTipText("Ctrl+Space: hide/restore tools and panels");focus.addActionListener(e->workspace.toggleFocus());
        menus.add(EditorTopologyWorkspace.menu("View",openMaterials,partsToggle,diagnostics,focus));dialog.setJMenuBar(menus);
        var commandBar=new JPanel(new BorderLayout(4,0));commandBar.add(pathLabel,BorderLayout.CENTER);var quick=new JPanel(new FlowLayout(FlowLayout.RIGHT,3,0));quick.add(openMaterials);quick.add(undoMesh);quick.add(redoMesh);quick.add(partsToggle);quick.add(diagnostics);quick.add(focus);commandBar.add(quick,BorderLayout.EAST);
        content.add(commandBar,BorderLayout.NORTH);content.add(workspace,BorderLayout.CENTER);content.add(footer,BorderLayout.SOUTH);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,InputEvent.CTRL_DOWN_MASK),"focus-workspace");
        dialog.getRootPane().getActionMap().put("focus-workspace",new AbstractAction(){public void actionPerformed(ActionEvent e){workspace.toggleFocus();}});
        dialog.setContentPane(content);dialog.setSize(1280,850);dialog.setMinimumSize(new Dimension(900,620));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){requestClose();}});
    }
    public static void show(JFrame parent,Consumer<String> published){
        new AssetImportDialog(parent,published).dialog.setVisible(true);
    }
    private boolean hasUnsavedMeshChanges(){return topologyProject!=null&&topologyProject.hasUnsavedChanges();}
    private boolean saveAuthoring(){
        if(topologyProject==null||busy)return false;
        try{
            topologyProject.save();
            if(path!=null&&path.toAbsolutePath().normalize().equals(topologyProject.documentPath()))reviewedSourceFingerprint=topologyProject.diskFingerprint();
            details.append("\nSAVED MESH "+topologyProject.documentPath());refreshMeshControls();return true;
        }catch(IOException failure){details.append("\nSAVE FAILED: "+failure.getMessage());return false;}
    }
    private void chooseObjExport(){
        if(topologyProject==null||busy||modelingPreview!=null)return;
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export native mesh as OBJ copy");
        chooser.setSelectedFile(topologyProject.documentPath().resolveSibling((topologyProject.snapshot().materials().isEmpty()?"mesh-export.obj":"mesh-export.obj.zip")).toFile());
        if(chooser.showSaveDialog(dialog)==JFileChooser.APPROVE_OPTION)exportObj(chooser.getSelectedFile().toPath());
    }
    boolean exportObj(Path destination){
        if(topologyProject==null||busy||modelingPreview!=null)return false;
        try{
            Path target=destination.toAbsolutePath().normalize();
            if(!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj")&&!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj.zip"))throw new IOException("Choose an .obj or .obj.zip destination");
            if(path!=null&&target.equals(path.toAbsolutePath().normalize()))throw new IOException("Export cannot replace the original source");
            if(!topologyProject.isCurrent(workingObservation))throw new IOException("Native mesh changed; review again before exporting");
            String previous=Files.exists(target)?EditorDocumentStore.fingerprint(EditorMeshPipeline.readSource(target,EditorDocumentStore.MAX_BYTES)):null;
            topologyProject.exportObj(workingObservation,target,previous);
            details.append("\nEXPORTED OBJ COPY "+target);return true;
        }catch(IOException failure){details.append("\nEXPORT REFUSED: "+failure.getMessage());workspace.setBrowsersVisible(true);return false;}
    }
    boolean exportGlb(Path destination){
        if(topologyProject==null||busy||modelingPreview!=null)return false;
        try{Path target=destination.toAbsolutePath().normalize();if(!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".glb"))throw new IOException("Choose a .glb destination");if(path!=null&&target.equals(path.toAbsolutePath().normalize()))throw new IOException("Export cannot replace original source");String previous=Files.exists(target)?EditorDocumentStore.fingerprint(EditorMeshPipeline.readSource(target,4_000_000)):null;topologyProject.exportGlb(workingObservation,target,previous);details.append("\nEXPORTED GLB COPY "+target);return true;}catch(IOException failure){details.append("\nGLB EXPORT REFUSED: "+failure.getMessage());workspace.setBrowsersVisible(true);return false;}
    }
    private void requestClose(){
        if(busy)return;
        if(hasUnsavedMeshChanges()){
            int answer=JOptionPane.showConfirmDialog(dialog,"Save mesh edits before closing?","Unsaved mesh",JOptionPane.YES_NO_CANCEL_OPTION);
            if(answer==JOptionPane.CANCEL_OPTION||answer==JOptionPane.CLOSED_OPTION)return;
            if(answer==JOptionPane.YES_OPTION&&!saveAuthoring())return;
        }
        dialog.dispose();
    }
    private void refreshPartRows(){
        updatingParts=true;
        try{
            partRows.clear();if(topologyProject==null)return;
            var counts=new java.util.LinkedHashMap<String,int[]>();var state=topologyProject.snapshot();
            for(var vertex:state.vertices())counts.computeIfAbsent(vertex.part(),key->new int[2])[0]++;
            for(var face:state.faces())counts.computeIfAbsent(face.part(),key->new int[2])[1]++;
            String filter=partSearch.getText().strip().toLowerCase(Locale.ROOT);
            counts.forEach((name,count)->{String label=state.glbSource()==null?name:state.glbSource().partLabel(name);if((name+" "+label).toLowerCase(Locale.ROOT).contains(filter))partRows.addElement(new MeshPart(name,count[0],count[1],label));});listedParts=state;
        }finally{updatingParts=false;}
    }
    private void selectPart(String part){
        if(busy||modelingPreview!=null||topologyProject==null)return;
        cancelAxisDrag();cancelBoxSelection();boolean needsReview=!topologyProject.isCurrent(workingObservation);if(needsReview)refreshWorkingObservation();
        var ids=new java.util.LinkedHashSet<Long>();for(var vertex:workingObservation.snapshot().vertices())if(vertex.part().equals(part))ids.add(vertex.id());
        selectionMode.setSelectedItem("VERTEX");selectedHit=null;
        if(!ids.isEmpty()){vertexId.setValue(ids.iterator().next());selectVertex();}
        selectedVertices=java.util.Set.copyOf(ids);selectionLabel.setText(part+" / "+ids.size()+" native vertices");refreshMeshControls();if(needsReview)recalculate();preview.repaint();
    }
    private JButton sharedMaterialAction(String name,JButton action){
        var button=new JButton(name);button.setEnabled(action.isEnabled());button.addActionListener(e->action.doClick());action.addPropertyChangeListener("enabled",e->button.setEnabled(action.isEnabled()));return button;
    }
    /** Separate module window, sharing the exact native model/history and existing human operations. */
    void openMaterials(){
        if(topologyProject==null||busy)return;
        selectionMode.setSelectedIndex(2);
        if(materialDialog==null){
            materialDialog=new JDialog(dialog,"Material Editor - Texture & UV",false);materialDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            materialDialog.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){if(modelingPreview!=null)clearModelingPreview();materialDialog.setVisible(false);}});
            materialRuntime.setPreferredSize(new Dimension(280,190));
            var runtime=new JPanel(new BorderLayout(4,4));runtime.add(new JLabel("ACTUAL RUNTIME PREVIEW"),BorderLayout.NORTH);runtime.add(materialRuntime,BorderLayout.CENTER);runtime.add(EditorTopologyWorkspace.form("Model rotation",materialRotation),BorderLayout.SOUTH);materialRotation.addChangeListener(e->refreshMaterialWindow());
            var bindings=EditorTopologyWorkspace.column(new JLabel("MATERIAL PROPERTIES"),EditorTopologyWorkspace.form("Material",materialList),materialSwatch,previewMaterial,new JSeparator(),EditorTopologyWorkspace.form("New PNG wrap",materialWrap,"Alpha",materialAlpha,"UV origin",materialOrigin),chooseMaterial,new JLabel("<html>PNG changes are captured in a native preview.<br>Apply commits one face assignment.</html>"));
            var values=new JPanel(new GridLayout(0,2,5,5));values.add(uvFirstLabel);values.add(uvFirst);values.add(uvSecondLabel);values.add(uvSecond);
            var uvSettings=EditorTopologyWorkspace.column(new JLabel("NATIVE CORNER UVS"),EditorTopologyWorkspace.form("Operation",uvOperation,"Scope",uvScope,"Pivot",uvPivot),values,previewUv,new JLabel("<html>Island traversal stops at UV seams<br>and material boundaries.<br>Negative scale mirrors UVs.<br>Zero scale is refused.<br>Apply / Cancel uses native history.</html>"));
            materialTools=new JTabbedPane();materialTools.addTab("Materials",bindings);materialTools.addTab("UV transform",uvSettings);
            var commands=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,3));commands.add(sharedMaterialAction("Undo",undoMesh));commands.add(sharedMaterialAction("Redo",redoMesh));commands.add(sharedMaterialAction("Save document",saveMesh));commands.add(sharedMaterialAction("Apply edit",applyFaceEdit));commands.add(sharedMaterialAction("Cancel preview",cancelFaceEdit));
            var back=new JButton("Return to modeling");back.addActionListener(e->{if(modelingPreview!=null)clearModelingPreview();materialDialog.setVisible(false);dialog.toFront();});commands.add(back);
            materialWorkspace=new EditorMaterialWorkspace(materialTools,runtime,commands,id->{
                if(busy||modelingPreview!=null||topologyProject==null)return;
                var face=topologyProject.snapshot().faces().stream().filter(f->f.id()==id).findFirst().orElse(null);if(face==null)return;
                normalAnchor=face.id();selectedHit=new EditorMeshPicking.Hit(face.id(),face.a().vertex(),face.a().vertex(),face.b().vertex(),0,0,0);selectionLabel.setText("Face "+id);refreshMeshControls();preview.repaint();
            });
            materialDialog.setContentPane(materialWorkspace);materialDialog.setSize(1160,760);materialDialog.setMinimumSize(new Dimension(900,600));materialDialog.setLocationRelativeTo(dialog);
        }
        materialDialog.setVisible(true);refreshMaterialWindow();
    }
    private void refreshMaterialWindow(){
        if(materialWorkspace==null||materialDialog==null||!materialDialog.isVisible())return;
        var state=modelingPreview!=null?modelingPreview.snapshot():topologyProject==null?null:topologyProject.snapshot();Long face=selectedHit==null?null:selectedHit.faceId();EditorMeshMaterial material=null;
        if(state!=null&&face!=null){var found=state.faces().stream().filter(f->f.id()==face).findFirst().orElse(null);if(found!=null)material=state.materials().stream().filter(m->m.key().equals(found.material())).findFirst().orElse(null);}
        if(material==null&&materialList.getSelectedItem() instanceof EditorMeshMaterial m)material=m;
        materialWorkspace.observe(state,face,material,modelingPreview!=null);
        double rotation=((Number)materialRotation.getValue()).doubleValue();
        if(state==null||state.faces().isEmpty()||materialRendering||state==materialRenderState&&rotation==materialRenderedRotation)return;
        materialRenderState=state;materialRenderedRotation=rotation;materialRendering=true;materialRuntime.setIcon(null);materialRuntime.setText("Rendering captured native state...");
        var captured=state;var mesh=modelingPreview!=null?modelingPreview.mesh():topologyProject.mesh();
        new SwingWorker<java.awt.image.BufferedImage,Void>(){
            protected java.awt.image.BufferedImage doInBackground()throws Exception{var renderer=new SceneRenderer();renderer.setRenderSize(280,190);return GlbProps.renderNativeMaterialReview(EditorGlbWriter.encode(mesh),renderer,Math.toRadians(rotation));}
            protected void done(){materialRendering=false;var current=modelingPreview!=null?modelingPreview.snapshot():topologyProject==null?null:topologyProject.snapshot();
                if(current==captured&&rotation==((Number)materialRotation.getValue()).doubleValue()){try{materialRuntime.setIcon(new ImageIcon(get()));materialRuntime.setText("");}catch(Exception failure){materialRuntime.setIcon(null);materialRuntime.setText("Preview unavailable: "+rootMessage(failure));}}
                refreshMaterialWindow();
            }
        }.execute();
    }
    private void refreshMaterialSwatch(){
        if(materialList.getSelectedItem() instanceof EditorMeshMaterial m){
            try{var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(m.png()));materialSwatch.setIcon(new ImageIcon(image.getScaledInstance(96,96,Image.SCALE_FAST)));materialSwatch.setText(m.width()+" x "+m.height()+" / "+m.alpha()+" / "+m.origin());}catch(IOException failure){throw new java.io.UncheckedIOException(failure);}
        }else{materialSwatch.setIcon(null);materialSwatch.setText("Select a face and a material");}
    }
    /** Human chooser and integration checks use the same captured PNG/native preview path. */
    void previewPngMaterial(Path png){
        if(busy||topologyProject==null||selectedHit==null||selectionMode.getSelectedIndex()!=2)return;
        long face=selectedHit.faceId();int wrap=new int[]{10497,33071,33648}[materialWrap.getSelectedIndex()];
        var alpha=(EditorMeshMaterial.Alpha)materialAlpha.getSelectedItem();var origin=(EditorMeshMaterial.Origin)materialOrigin.getSelectedItem();
        beginModelingPreview(p->{try{return p.previewBindMaterial(face,EditorMeshMaterial.fromPng(png.getFileName().toString(),EditorMeshPipeline.readSource(png,4_000_000),wrap,wrap,alpha,origin));}catch(IOException bad){throw new java.io.UncheckedIOException(bad);}});
    }
    private void refreshMeshControls(){
        if (boundaryObservation != null && !hasBoundary()) clearBoundary();
        var materialState=topologyProject==null?null:topologyProject.snapshot();
        if(listedMaterials!=materialState){listedMaterials=materialState;Object selected=materialList.getSelectedItem();materialList.removeAllItems();if(materialState!=null)for(var m:materialState.materials())materialList.addItem(m);if(selected!=null)for(int i=0;i<materialList.getItemCount();i++)if(materialList.getItemAt(i).equals(selected))materialList.setSelectedIndex(i);refreshMaterialSwatch();}

        if(topologyProject==null?listedParts!=null:listedParts!=topologyProject.snapshot()){refreshPartRows();if(topologyProject==null)listedParts=null;}

        boolean editable=topologyProject!=null&&!busy&&modelingPreview==null;partList.setEnabled(editable);partSearch.setEnabled(!busy);
        for(var component:List.of(vertexId,vertexX,vertexY,vertexZ,moveVertex,saveMesh,reopenMesh,exportMesh,exportGlb))component.setEnabled(editable);
        undoMesh.setEnabled(editable&&meshHistory.canUndo());redoMesh.setEnabled(editable&&meshHistory.canRedo());
        if(editable)saveMesh.setToolTipText("Save native topology to "+topologyProject.documentPath());
        newMesh.setEnabled(!busy);previewPrimitive.setEnabled(editable);applyPrimitive.setEnabled(editable&&reviewedPrimitive!=null);
        for(var component:List.of(primitiveKind,primitiveWidth,primitiveHeight,primitiveDepth,primitiveSegments,primitiveRings))component.setEnabled(editable);
        meshView.setEnabled(!busy);selectionMode.setEnabled(editable);compareView.setEnabled(!busy&&modelingPreview==null);
        meshSnap.setEnabled(editable);
        boxSelect.setEnabled(editable);xraySelect.setEnabled(editable);areaShape.setEnabled(editable);
        faceOperation.setEnabled(topologyProject!=null&&!busy);faceAmount.setEnabled(topologyProject!=null&&!busy);
        previewFaceEdit.setEnabled(editable&&selectionMode.getSelectedIndex()==2&&selectedHit!=null);
        selectBoundary.setEnabled(editable && selectionMode.getSelectedIndex()==1 && selectedHit!=null);
        previewSlide.setEnabled(editable && !hasBoundary() && selectionMode.getSelectedIndex()==1 && selectedHit!=null);
        previewBevel.setEnabled(editable&&!hasBoundary()&&selectionMode.getSelectedIndex()!=2&&selectedHit!=null);
        chooseMaterial.setEnabled(editable&&selectionMode.getSelectedIndex()==2&&selectedHit!=null);previewMaterial.setEnabled(chooseMaterial.isEnabled()&&materialList.getSelectedItem()!=null);
        for(var c:List.of(materialList,materialWrap,materialAlpha,materialOrigin,uvOperation,uvScope,uvPivot,uvFirst,uvSecond))c.setEnabled(editable);
        previewUv.setEnabled(editable&&selectionMode.getSelectedIndex()==2&&selectedHit!=null);
        if(topologyProject!=null)sourceTextureBytes=topologyProject.snapshot().materials().stream().mapToLong(m->(long)m.width()*m.height()*4).sum();
        previewWeld.setEnabled(editable&&selectionMode.getSelectedIndex()==0&&selectedVertices.size()>1);previewDegenerates.setEnabled(editable);previewUnused.setEnabled(editable);
        previewTransform.setEnabled(editable&&!selectedIds().isEmpty()&&(transformSpace.getSelectedIndex()!=1||activeSurfaceFace()!=null));
        for(var component:List.of(transformSpace,transformKind,transformAxis,transformPivot,transformAmount,angleSnap))component.setEnabled(topologyProject!=null&&!busy);
        applyFaceEdit.setEnabled(!busy&&modelingPreview!=null);cancelFaceEdit.setEnabled(modelingPreview!=null||modelingPreparing);
        openMaterials.setEnabled(topologyProject!=null&&!busy);refreshMaterialWindow();
        updatePublishAvailability();
    }
    private void updatePublishAvailability(){
        apply.setEnabled(!busy&&modelingPreview==null&&source!=null&&result!=null&&result.source()==source
            &&(topologyProject==null||topologyProject.isCurrent(workingObservation))&&!result.optimized().triangles().isEmpty()
            &&(!source.name().toLowerCase(Locale.ROOT).endsWith(".glb")||result.level()==0));
    }
    private boolean comparisonVisible(){return compareView.isSelected()||modelingMesh!=null||primitivePreview!=null||(result!=null&&result.level()>0);}
    int workingViewportWidth(){return comparisonVisible()?preview.getWidth()/2:preview.getWidth();}
    private void clearModelingPreview(){if(modelingPreview!=null&&topologyProject!=null)topologyProject.cancelPreview(modelingPreview);modelingGeneration++;modelingPreparing=false;modelingPreview=null;modelingMesh=null;modelingBounds=null;refreshMeshControls();preview.repaint();}
    private record ModelingReview(EditorTopologyProject.EditPreview edit,EditorMeshPipeline.Mesh mesh,List<EditorMeshPipeline.Triangle> bounds){}
    private void previewModelingEdit(){
        if(busy||topologyProject==null||selectedHit==null||selectionMode.getSelectedIndex()!=2)return;
        long face=selectedHit.faceId();var operation=(EditorTopologyProject.FaceEdit)faceOperation.getSelectedItem();double amount=((Number)faceAmount.getValue()).doubleValue();
        beginModelingPreview(p->p.previewFaceEdit(face,operation,amount));
    }
    private void beginModelingPreview(java.util.function.Function<EditorTopologyProject,EditorTopologyProject.EditPreview> editOperation){
        if(busy||topologyProject==null)return;
        if(!topologyProject.isCurrent(workingObservation)){workingObservation=topologyProject.observe();source=workingObservation.mesh();}
        cancelAxisDrag();cancelBoxSelection();clearPrimitivePreview();clearModelingPreview();
        var project=topologyProject;var state=workingObservation.snapshot();long nativeRevision=workingObservation.revision();int request=modelingGeneration;var working=source;
        busy=true;modelingPreparing=true;choose.setEnabled(false);refreshMeshControls();details.append("\nPreparing native model edit...");
        new SwingWorker<ModelingReview,Void>(){
            protected ModelingReview doInBackground(){
                var edit=editOperation.apply(project);var mesh=edit.mesh();var bounds=new ArrayList<>(working.triangles());bounds.addAll(mesh.triangles());
                return new ModelingReview(edit,mesh,List.copyOf(bounds));
            }
            protected void done(){
                busy=false;modelingPreparing=false;choose.setEnabled(true);
                try{
                    if(request!=modelingGeneration)throw new java.util.concurrent.CancellationException();var review=get();
                    if(project!=topologyProject||project.revision()!=nativeRevision||project.snapshot()!=state)throw new IOException("Mesh changed during modeling preview");
                    modelingPreview=review.edit();modelingMesh=review.mesh();modelingBounds=review.bounds();selectionLabel.setText(review.edit().description());selectionLabel.setToolTipText(review.edit().description());details.append("\nNATIVE PREVIEW: "+review.edit().description()+". Apply to commit.");
                }catch(java.util.concurrent.CancellationException canceled){details.append("\nModel preview canceled.");}
                catch(Exception failure){String message="MODEL EDIT REFUSED: "+rootMessage(failure);details.append("\n"+message);selectionLabel.setText(message);selectionLabel.setToolTipText(message);}
                refreshMeshControls();preview.repaint();
            }
        }.execute();
    }
    private void clearPrimitivePreview(){primitivePreview=null;reviewedPrimitive=null;applyPrimitive.setEnabled(false);preview.repaint();}
    private void previewPrimitive(){
        if(topologyProject==null||busy)return;
        clearModelingPreview();
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
        clearModelingPreview();
        cancelAxisDrag();
        cancelBoxSelection();
        if(hasUnsavedMeshChanges()&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before creating a new mesh?","Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{
            Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");
            var mesh=new EditorMeshPipeline.Mesh("New mesh",List.of(),0);
            var project=EditorTopologyProject.fromMesh(mesh,EditorMeshPipeline.sourceFingerprint("native-mesh-v1".getBytes(java.nio.charset.StandardCharsets.UTF_8)),document);
            project.save();clearPrimitivePreview();selectedHit=null;normalAnchor=null;selectionLabel.setText("Click a triangle in the working mesh");topologyProject=project;meshHistory=new EditorHistory<>(project,60);workingObservation=project.observe();source=workingObservation.mesh();result=null;
            path=document;sourceCapture=null;reviewedSourceFingerprint=project.diskFingerprint();authoringNote="Native primitive authoring; generated fallback atlas.";sourceTextureBytes=0;
            vertexId.setValue(0L);selectVertex();refreshMeshControls();pathLabel.setText(document.getFileName()+" | native authoring");
            if(slider.getValue()!=0)slider.setValue(0);else recalculate();
        }catch(IOException|RuntimeException failure){details.append("\nNEW MESH FAILED: "+rootMessage(failure));}
    }
    private void selectVertex(){
        preview.repaint();selectedVertex=null;selectedVertices=java.util.Set.of();if(topologyProject==null)return;long id=((Number)vertexId.getValue()).longValue();
        var observed=workingObservation==null?topologyProject.snapshot():workingObservation.snapshot();
        for(var v:observed.vertices())if(v.id()==id){selectedVertex=v;selectedVertices=java.util.Set.of(id);vertexX.setValue(v.x());vertexY.setValue(v.y());vertexZ.setValue(v.z());return;}
    }
    private void refreshWorkingObservation(){
        var selected=selectedVertices;workingObservation=topologyProject.observe();source=workingObservation.mesh();selectVertex();
        var valid=new java.util.HashSet<Long>();for(var v:workingObservation.snapshot().vertices())if(selected.contains(v.id()))valid.add(v.id());
        if(!valid.isEmpty()&&(selectedVertex==null||!valid.contains(selectedVertex.id()))){vertexId.setValue(valid.stream().mapToLong(Long::longValue).min().orElseThrow());selectVertex();}
        selectedVertices=java.util.Set.copyOf(valid);
    }
    private void meshEdit(Runnable operation){
        if(topologyProject==null||busy)return;
        try{meshHistory.execute(operation);meshChanged();}
        catch(RuntimeException failure){details.append("\nEDIT REFUSED: "+rootMessage(failure));refreshMeshControls();}
    }
    private void meshChanged(){
        clearModelingPreview();
        var selected=selectedVertices;workingObservation=topologyProject.observe();source=workingObservation.mesh();selectVertex();
        if(!selected.isEmpty()){var ids=new java.util.HashSet<Long>();for(var v:topologyProject.snapshot().vertices())if(selected.contains(v.id()))ids.add(v.id());selectedVertices=java.util.Set.copyOf(ids);}
        refreshMeshControls();
        if(slider.getValue()!=0)slider.setValue(0);else recalculate();
    }
    private static EditorMeshPipeline.Vertex glbCorner(GlbProps.Vertex v){
        var normal=v.nx()==0&&v.ny()==0&&v.nz()==0?null:new EditorMeshPipeline.Normal(v.nx(),v.ny(),v.nz());
        return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),v.u(),v.v(),normal);
    }
    private record Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String reviewedFingerprint,EditorObjMaterials.Capture capture){
        Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String reviewedFingerprint){this(mesh,project,note,reviewedFingerprint,null);}
    }
    private void openSource(){
        if(hasUnsavedMeshChanges()&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before opening another source?",
            "Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        JFileChooser file=new JFileChooser();
        file.setDialogTitle("Import a mesh source (OBJ, STL, GLB)");
        file.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Mesh files (*.obj, *.obj.zip, *.stl, *.glb, *.btop)","obj","zip","stl","glb","btop"));
        if(file.showOpenDialog(dialog)!=JFileChooser.APPROVE_OPTION)return;
        loadSource(file.getSelectedFile().toPath());
    }
    /** Same native import path for the human chooser and source-backed verification. */
    void loadSource(Path candidate){
        normalAnchor=null;
        cameraGesture=null;cameraZoom=1;cameraPanX=0;cameraPanY=0;
        clearModelingPreview();
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
                    String unsupported;
                    try{Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");var project=EditorTopologyProject.fromGlb(candidate.getFileName().toString(),bytes,document);return new Loaded(project.mesh(),project,"Original GLB hierarchy/pivots/material metadata retained; native edits own the selected node instance.",EditorMeshPipeline.sourceFingerprint(bytes));}catch(IOException notEditable){unsupported=notEditable.getMessage();}
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
                            glbCorner(a),
                            glbCorner(b),
                            glbCorner(c)));
                    }
                    return new Loaded(new EditorMeshPipeline.Mesh(candidate.getFileName().toString(),all,bytes.length),null,
                        "GLB retained read-only: "+unsupported,EditorMeshPipeline.sourceFingerprint(bytes));
                }
                sourceTextureBytes=0;
                byte[] bytes=EditorMeshPipeline.readSource(candidate,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);
                var captured=EditorObjMaterials.capture(candidate);var mesh=captured.mesh();
                String digest=captured.sourceFingerprint();
                Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");
                try{return new Loaded(mesh,EditorTopologyProject.fromMesh(mesh,digest,document),"",digest,captured);}
                catch(IllegalArgumentException unsupported){return new Loaded(mesh,null,"Vertex editing unavailable: "+unsupported.getMessage(),digest,captured);}
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;
                try{
                    var loaded=get();sourceCapture=loaded.capture();source=loaded.mesh();topologyProject=loaded.project();workingObservation=topologyProject==null?null:topologyProject.observe();if(workingObservation!=null)source=workingObservation.mesh();authoringNote=loaded.note();reviewedSourceFingerprint=loaded.reviewedFingerprint();
                    meshHistory=topologyProject==null?null:new EditorHistory<>(topologyProject,60);
                    path=candidate;vertexId.setValue(topologyProject==null||topologyProject.snapshot().vertices().isEmpty()?0L:topologyProject.snapshot().vertices().get(0).id());
                    selectVertex();refreshMeshControls();slider.setValue(0);
                    pathLabel.setText(candidate.getFileName()+" | "+source.triangles().size()+" source triangles");
                    recalculate();
                }catch(Exception e){
                    source=null;result=null;path=null;topologyProject=null;workingObservation=null;meshHistory=null;refreshMeshControls();apply.setEnabled(false);
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
        if(topologyProject!=null&&!topologyProject.isCurrent(workingObservation)){
            clearModelingPreview();refreshWorkingObservation();
        }
        final EditorMeshPipeline.Mesh mesh=source;
        final int level=slider.getValue(),request=++revision;
        final EditorTopologyProject.Observation observation=workingObservation;
        final EditorTopologyProject project=topologyProject;
        result=null;apply.setEnabled(false);details.setText("Computing regression level "+level+"...");
        if(mesh.glbSource()!=null&&level>0){details.setText("PUBLISH DISABLED: source-backed GLB reduction requires preserved node/primitive/material bindings. Use level 0 or native model edits.");return;}
        new SwingWorker<Review,Void>(){
            @Override protected Review doInBackground(){
                var reduction=EditorMeshPipeline.regress(mesh,level);
                // The synchronized project is read only if this is still the captured revision.
                var topology=level==0&&project!=null?observation.topology():EditorMeshTopology.build(reduction.optimized());
                return new Review(reduction,topology);
            }
            @Override protected void done(){
                if(request!=revision||mesh!=source)return;
                if(project!=null&&!project.isCurrent(observation)){details.setText("Native mesh changed during review; review again.");apply.setEnabled(false);return;}
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
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".obj")&&mesh.materials().isEmpty())
                        report.append("NOTE: OBJ source MTL/texture links are not yet converted; uses a generated low-res fallback atlas.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".stl"))
                        report.append("NOTE: STL contains no UV/material atlas; uses a generated low-res fallback atlas.\n");
                    boolean texturedGlb=mesh.name().toLowerCase(Locale.ROOT).endsWith(".glb");
                    if(texturedGlb&&level>0)report.append(
                        "PUBLISH DISABLED: this regression exporter would replace original GLB materials with a checkerboard. Use level 0 or a UV/material-preserving optimizer.\n");
                    details.setText(report.toString());details.setCaretPosition(0);
                    updatePublishAvailability();
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
        if(source==null||result==null||path==null||busy||modelingPreview!=null)return;
        if(topologyProject!=null&&!topologyProject.isCurrent(workingObservation)){
            details.append("\nPUBLISH REFUSED: native mesh changed since review; review again before publishing");apply.setEnabled(false);return;
        }
        final int remaining=result.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
        if(remaining==0)return;
        // This path is not a material-preserving GLB optimizer. Reject it,
        // including programmatic invocations that bypass the disabled button.
        if(source.name().toLowerCase(Locale.ROOT).endsWith(".glb")&&result.level()>0){
            JOptionPane.showMessageDialog(dialog,
                "Textured GLB reduction would discard source UV/material bindings. "+
                "Use level 0 or repair/optimize the model with a material-preserving tool.",
                "Unsafe GLB export blocked",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(result.overRegressed()){
            String msg=(result.overRegressed()?
                "OVER-REGRESSION: one or more named parts have collapsed.\n":"")+
                "The untouched source will be preserved. Publish this destructive-looking variant anyway?";
            if(JOptionPane.showConfirmDialog(dialog,msg,"Explicit lossy import warning",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        }
        final Path original=path;final EditorMeshPipeline.Result chosen=result;
        final EditorObjMaterials.Capture capturedDependencies=sourceCapture;
        final String reviewed=reviewedSourceFingerprint;final EditorTopologyProject authoringProject=topologyProject;
        final EditorTopologyProject.Observation authoringObservation=workingObservation;
        final var savedAuthoring=new java.util.concurrent.atomic.AtomicBoolean();
        final var savedSourceFingerprint=new java.util.concurrent.atomic.AtomicReference<String>();
        choose.setEnabled(false);apply.setEnabled(false);busy=true;refreshMeshControls();
        details.append("\nPublishing validated GLB derivative...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{
                byte[] sourceBytes=EditorMeshPipeline.readSource(original,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);
                if(!EditorMeshPipeline.sourceFingerprint(sourceBytes).equals(reviewed))throw new IOException("Model source changed after review; reopen it before publishing");
                if(capturedDependencies!=null)capturedDependencies.validateDependencies();
                byte[] nativeBytes=null;String expectedSource=reviewed;
                if(authoringProject!=null){
                    var saved=authoringProject.saveReviewed(authoringObservation);savedAuthoring.set(true);nativeBytes=saved.bytes();
                    if(original.toAbsolutePath().normalize().equals(authoringProject.documentPath())){
                        expectedSource=saved.diskFingerprint();savedSourceFingerprint.set(expectedSource);
                    }
                }
                byte[] output=authoringProject==null&&chosen.level()==0&&original.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".glb")
                    ?sourceBytes:EditorGlbWriter.encode(chosen.optimized());
                // Use the exact decoder that live gameplay will use. No mock preview-only mesh.
                if(authoringProject!=null)EditorGlbWriter.validate(chosen.optimized(),output);
                GlbProps.decode(output);
                int after=chosen.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                return ImportedAssetLibrary.publishReviewed(original,output,chosen.level(),
                    chosen.source().triangles().size(),after,String.join(" | ",chosen.warnings()),expectedSource,nativeBytes);
            }
            @Override protected void done(){
                choose.setEnabled(true);busy=false;refreshMeshControls();
                if(savedAuthoring.get()&&topologyProject==authoringProject){
                    if(savedSourceFingerprint.get()!=null)reviewedSourceFingerprint=savedSourceFingerprint.get();
                    details.append("\nSAVED MESH "+authoringProject.documentPath());
                    if(hasUnsavedMeshChanges())details.append("\nA newer native edit remains unsaved; review it before publishing again.");
                }
                try{
                    String id=get();published.accept(id);
                    details.append("\nPUBLISHED "+id+" to "+ImportedAssetLibrary.root()+
                        "\nSelect the model in the editor and place it with the PROP tool.");
                }catch(Exception failure){
                    details.append("\nPUBLISH FAILED: "+rootMessage(failure));
                    updatePublishAvailability();
                }
            }
        }.execute();
    }
    private static String rootMessage(Throwable failure){
        while(failure.getCause()!=null)failure=failure.getCause();
        return String.valueOf(failure.getMessage());
    }
    private EditorMeshPicking.Projection projection(){return (EditorMeshPicking.Projection)meshView.getSelectedItem();}
    private EditorMeshPicking.Projector projector(List<EditorMeshPipeline.Triangle> reference){
        if(projection()!=EditorMeshPicking.Projection.PERSPECTIVE)return projection();
        if(cameraReference!=reference||cachedCamera==null||cachedYaw!=cameraYaw||cachedPitch!=cameraPitch){
            var vertices=new ArrayList<EditorMeshPipeline.Vertex>(reference.size()*3);for(var t:reference){vertices.add(t.a());vertices.add(t.b());vertices.add(t.c());}
            cachedCamera=EditorMeshPicking.camera(vertices,cameraYaw,cameraPitch);cameraReference=reference;cachedYaw=cameraYaw;cachedPitch=cameraPitch;
        }return cachedCamera;
    }
    private List<EditorMeshPipeline.Triangle> workingReference(){return modelingBounds==null?source.triangles():modelingBounds;}
    private record Viewport(double centerX,double centerY,double scale,int left,int width,int height){
        int x(double value){return left+width/2+(int)Math.round((value-centerX)*scale);}
        int y(double value){return height/2+(int)Math.round((value-centerY)*scale);}
    }
    private Viewport viewport(List<EditorMeshPipeline.Triangle> reference,int left,int width){
        var camera=projector(reference);var key=new ViewKey(camera,left,width,getPreviewHeight(),cameraPanX,cameraPanY,cameraZoom);
        if(viewportCache.size()>4)viewportCache.clear();var cache=viewportCache.computeIfAbsent(reference,k->new java.util.HashMap<>());var cached=cache.get(key);if(cached!=null)return cached;if(cache.size()>16)cache.clear();
        double xmin=Double.POSITIVE_INFINITY,ymin=xmin,xmax=Double.NEGATIVE_INFINITY,ymax=xmax;
        for(var t:reference)for(var v:List.of(t.a(),t.b(),t.c())){
            var p=camera.project(v.x(),v.y(),v.z());xmin=Math.min(xmin,p.x());xmax=Math.max(xmax,p.x());ymin=Math.min(ymin,p.y());ymax=Math.max(ymax,p.y());
        }
        double scale=Math.min((width-26)/Math.max(1e-6,xmax-xmin),(getPreviewHeight()-36)/Math.max(1e-6,ymax-ymin));
        var result=new Viewport((xmin+xmax)*.5+(camera.perspective()?cameraPanX:0),(ymin+ymax)*.5+(camera.perspective()?cameraPanY:0),scale*(camera.perspective()?cameraZoom:1),left,width,getPreviewHeight());cache.put(key,result);return result;
    }
    private EditorMeshPicking.Hit nativePick(EditorTopologyProject.State state,EditorMeshPicking.Projector camera,double x,double y){
        if(pickingState!=state||pickingCamera!=camera){preparedPicking=EditorMeshPicking.prepare(state,camera);pickingState=state;pickingCamera=camera;}
        return preparedPicking.pick(x,y).orElse(null);
    }
    private void clearHover(){if(hoverHit!=null){hoverHit=null;preview.repaint();}}
    private void hoverMesh(int x,int y){
        if(busy||modelingPreview!=null||topologyProject==null||source==null||source.triangles().isEmpty()||x<0||x>=workingViewportWidth()||y<0||y>=getPreviewHeight()){clearHover();return;}
        var view=viewport(workingReference(),0,workingViewportWidth());var camera=projector(workingReference());var state=topologyProject.snapshot();
        var hit=nativePick(state,camera,(x-view.left()-view.width()/2)/view.scale()+view.centerX(),(y-view.height()/2)/view.scale()+view.centerY());
        hoverState=state;hoverCamera=camera;hoverViewport=view;hoverMode=selectionMode.getSelectedIndex();hoverHit=hit;preview.repaint();
    }
    private void pickMesh(int x,int y){
        if(busy||modelingPreview!=null||topologyProject==null||source.triangles().isEmpty()||x<0||x>=workingViewportWidth())return;
        var view=viewport(workingReference(),0,workingViewportWidth());
        double px=(x-view.left()-view.width()/2)/view.scale()+view.centerX(),py=(y-view.height()/2)/view.scale()+view.centerY();
        clearBoundary();
        selectedHit=nativePick(topologyProject.snapshot(),projector(workingReference()),px,py);
        if(selectedHit!=null)normalAnchor=selectedHit.faceId();
        if(selectedHit==null){selectedVertex=null;selectedVertices=java.util.Set.of();selectionLabel.setText("No triangle selected");refreshMeshControls();preview.repaint();return;}
        if(selectionMode.getSelectedIndex()==0){vertexId.setValue(selectedHit.vertexId());selectVertex();}
        selectionLabel.setText(switch(selectionMode.getSelectedIndex()){
            case 0->"Vertex "+selectedHit.vertexId()+" / face "+selectedHit.faceId();
            case 1->"Edge "+selectedHit.edgeA()+" - "+selectedHit.edgeB();
            default->"Face "+selectedHit.faceId();});
        refreshMeshControls();preview.repaint();
    }
    private void clearBoundary() {
        boundaryObservation = null;
        boundaryVertices = List.of();
        boundaryIds = java.util.Set.of();
    }
    private boolean hasBoundary() {
        return boundaryObservation != null && topologyProject != null && topologyProject.isCurrent(boundaryObservation);
    }
    private void selectBoundaryLoop() {
        if (busy || selectedHit == null || selectionMode.getSelectedIndex()!=1) return;
        var project = topologyProject;
        var observation = project.observe();
        var seed = selectedHit;
        busy = true;
        choose.setEnabled(false);
        apply.setEnabled(false);
        refreshMeshControls();
        new SwingWorker<List<EditorTopologyProject.Vertex>,Void>() {
            protected List<EditorTopologyProject.Vertex> doInBackground() {
                return observation.boundaryLoop(seed.edgeA(), seed.edgeB());
            }
            protected void done() {
                try {
                    var vertices = get();
                    if (topologyProject == project && project.isCurrent(observation) && seed == selectedHit && selectionMode.getSelectedIndex()==1) {
                        selectedVertex = null;
                        selectedVertices = java.util.Set.of();
                        boundaryVertices = vertices;
                        boundaryIds = vertices.stream().map(EditorTopologyProject.Vertex::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
                        boundaryObservation = observation;
                        selectionLabel.setText("Boundary loop / " + vertices.size() + " edges");
                    }
                } catch (Exception refused) { selectionLabel.setText(rootMessage(refused)); }
                finally { busy = false; choose.setEnabled(true); refreshMeshControls(); updatePublishAvailability(); preview.repaint(); }
            }
        }.execute();
    }
    private java.util.Set<Long> selectedIds(){
        if(topologyProject==null)return java.util.Set.of();
        if(selectionMode.getSelectedIndex()==0)return selectedVertices;
        if(selectedHit==null)return java.util.Set.of();
        if(selectionMode.getSelectedIndex()==1 && hasBoundary())return boundaryIds;
        if(selectionMode.getSelectedIndex()==1)return java.util.Set.copyOf(List.of(selectedHit.edgeA(),selectedHit.edgeB()));
        return topologyProject.snapshot().faces().stream().filter(f->f.id()==selectedHit.faceId()).findFirst()
            .map(f->java.util.Set.copyOf(List.of(f.a().vertex(),f.b().vertex(),f.c().vertex()))).orElse(java.util.Set.of());
    }
    private Long activeSurfaceFace(){
        if(topologyProject==null)return null;Long face=selectedHit==null?normalAnchor:Long.valueOf(selectedHit.faceId());return face!=null&&topologyProject.snapshot().faces().stream().anyMatch(f->f.id()==face)?face:null;
    }
    private EditorMeshBasis surfaceBasis(){Long face=activeSurfaceFace();if(face==null)return null;try{return EditorMeshBasis.face(topologyProject.snapshot(),face);}catch(IllegalArgumentException invalid){return null;}}
    private String axisLabel(int axis){return (transformSpace.getSelectedIndex()!=1?"XYZ":"TBN").substring(axis,axis+1);}
    private List<GizmoHandle> gizmoHandles(){
        if(busy||topologyProject==null||source.triangles().isEmpty())return List.of();var ids=selectedIds();if(ids.isEmpty())return List.of();boolean local=transformSpace.getSelectedIndex()==2;var basis=transformSpace.getSelectedIndex()==1?surfaceBasis():null;if(transformSpace.getSelectedIndex()==1&&basis==null)return List.of();
        var groups=new LinkedHashMap<String,double[]>();int count=0;for(var v:topologyProject.snapshot().vertices())if(ids.contains(v.id())){var center=groups.computeIfAbsent(local?v.part():"",key->new double[4]);center[0]+=v.x()/ids.size();center[1]+=v.y()/ids.size();center[2]+=v.z()/ids.size();center[3]++;count++;}if(count!=ids.size())return List.of();
        var frames=local?topologyProject.frames(groups.keySet()):Map.<String,EditorMeshFrame>of();var view=viewport(workingReference(),0,workingViewportWidth());var camera=projector(workingReference());var handles=new ArrayList<GizmoHandle>();
        for(var group:groups.entrySet()){var center=group.getValue();double factor=ids.size()/center[3],wx=center[0]*factor,wy=center[1]*factor,wz=center[2]*factor;var anchor=camera.project(wx,wy,wz);double ax=view.x(anchor.x()),ay=view.y(anchor.y());
            for(int axis=0;axis<3;axis++){var direction=local?frames.get(group.getKey()).vector(axis):basis==null?EditorMeshPicking.unitAxis(axis):basis.axis(axis);var p=camera.direction(wx,wy,wz,direction);double ux=p.x()*view.scale(),uy=p.y()*view.scale(),length=Math.hypot(ux,uy);if(length>1e-9)handles.add(new GizmoHandle(axis,(int)Math.round(ax+ux/length*48),(int)Math.round(ay+uy/length*48),ax,ay,ux,uy,wx,wy,wz,view.scale(),camera,direction));}
        }return handles;
    }
    private boolean beginAxisDrag(int x,int y){
        if(busy||modelingPreview!=null||x>=workingViewportWidth())return false;
        GizmoHandle nearest=null;double closest=Double.POSITIVE_INFINITY;
        for(var handle:gizmoHandles()){double distance=Math.hypot(x-handle.x(),y-handle.y());if(distance<=8&&distance<closest){nearest=handle;closest=distance;}}
        if(nearest==null)return false;
        axisDrag=new AxisDrag(topologyProject.snapshot(),topologyProject.revision(),selectedIds(),nearest,x,y,((Number)meshSnap.getValue()).doubleValue());dragDelta=0;preview.repaint();return true;
    }
    private void updateAxisDrag(int x,int y){
        if(axisDrag==null)return;var h=axisDrag.handle();
        double value;try{value=h.camera().deltaAlong(h.worldX(),h.worldY(),h.worldZ(),h.direction(),(x-axisDrag.startX())/h.scale(),(y-axisDrag.startY())/h.scale());}catch(IllegalArgumentException invalid){cancelAxisDrag();selectionLabel.setText("Move refused: "+invalid.getMessage());return;}
        dragDelta=axisDrag.grid()>0?Math.rint(value/axisDrag.grid())*axisDrag.grid():value;
        selectionLabel.setText("Move "+axisLabel(h.axis())+" = "+String.format(Locale.ROOT,"%.4f",dragDelta));preview.repaint();
    }
    private void cancelAxisDrag(){if(axisDrag!=null)selectionLabel.setText("Move canceled");axisDrag=null;dragDelta=0;preview.repaint();}
    private void cancelBoxSelection(){selectionGeneration++;boxStart=null;boxEnd=null;selectionPath=null;preview.repaint();}
    private void finishBoxSelection(){
        if(boxStart==null||topologyProject==null)return;var start=boxStart;var end=boxEnd;boolean lasso=lassoGesture;var path=selectionPath==null?List.<Point>of():List.copyOf(selectionPath);cancelBoxSelection();
        if(source.triangles().isEmpty())return;int request=selectionGeneration;
        var project=topologyProject;var state=project.snapshot();long nativeRevision=project.revision();var selectedProjection=projector(workingReference());boolean xray=xraySelect.isSelected();
        var view=viewport(workingReference(),0,workingViewportWidth());
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
        var direction=drag.handle().direction();boolean local=transformSpace.getSelectedIndex()==2;meshEdit(()->{if(local)topologyProject.translateLocalVertices(drag.ids(),axis,delta);else topologyProject.translateVertices(drag.ids(),direction.x()*delta,direction.y()*delta,direction.z()*delta);});
        selectionLabel.setText(topologyProject.snapshot()==drag.before()?"Move refused":"Moved "+axisLabel(axis)+" = "+String.format(Locale.ROOT,"%.4f",delta));
    }
    /** Regression panels share source bounds; the separate pending primitive has its own bounds. */
    private void draw(Graphics2D g,List<EditorMeshPipeline.Triangle> tris,int left,int width,String title){
        List<EditorMeshPipeline.Triangle> reference=title.equals("PRIMITIVE PREVIEW")?tris:workingReference();
        if(reference.isEmpty()){g.setColor(Color.LIGHT_GRAY);g.drawString(title+" / empty",left+10,18);return;}
        var view=viewport(reference,left,width);var camera=projector(reference);
        g.setColor(new Color(181,206,213));
        g.drawString(title+"  /  "+tris.size()+" triangles",left+10,18);
        int skip=Math.max(1,tris.size()/12000);
        g.setColor(new Color(113,168,188,120));
        for(int i=0;i<tris.size();i+=skip){
            var t=tris.get(i);
            int[] xs=new int[3],ys=new int[3];int k=0;
            for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
                var p=camera.project(v.x(),v.y(),v.z());xs[k]=view.x(p.x());ys[k]=view.y(p.y());k++;
            }
            g.drawPolygon(xs,ys,3);
        }
        if(normalOverlay.isSelected()){
            g.setColor(new Color(75,232,193));g.drawString("CORNER NORMALS / authored or derived / sampled",left+10,34);
            int step=Math.max(1,(tris.size()+399)/400);double screenLength=28/view.scale();
            for(int i=0;i<tris.size();i+=step){var triangle=tris.get(i);for(var v:List.of(triangle.a(),triangle.b(),triangle.c()))try{
                double length=screenLength*camera.worldScale(v.x(),v.y(),v.z());var n=EditorMeshNormals.unit(EditorMeshNormals.effective(triangle,v));var a=camera.project(v.x(),v.y(),v.z());var b=camera.project(v.x()+n.x()*length,v.y()+n.y()*length,v.z()+n.z()*length);g.drawLine(view.x(a.x()),view.y(a.y()),view.x(b.x()),view.y(b.y()));
            }catch(IllegalArgumentException degenerate){/* Degenerate geometry has no derived normal; native diagnostics retain the issue. */}}
        }
        if(title.equals("WORKING MESH")&&hoverHit!=null&&!busy&&modelingPreview==null&&topologyProject.snapshot()==hoverState&&hoverCamera==camera&&view.equals(hoverViewport)&&hoverMode==selectionMode.getSelectedIndex()){
            var face=hoverState.faces().stream().filter(f->f.id()==hoverHit.faceId()).findFirst().orElse(null);
            if(face!=null){long[] ids=hoverMode==0?new long[]{hoverHit.vertexId()}:hoverMode==1?new long[]{hoverHit.edgeA(),hoverHit.edgeB()}:new long[]{face.a().vertex(),face.b().vertex(),face.c().vertex()};int[] xs=new int[ids.length],ys=new int[ids.length];for(int i=0;i<ids.length;i++){long id=ids[i];var vertex=hoverState.vertices().stream().filter(v->v.id()==id).findFirst().orElseThrow();var point=camera.project(vertex.x(),vertex.y(),vertex.z());xs[i]=view.x(point.x());ys[i]=view.y(point.y());}g.setColor(new Color(90,220,255));g.setStroke(new BasicStroke(3));if(hoverMode==0)g.drawOval(xs[0]-6,ys[0]-6,12,12);else if(hoverMode==1)g.drawLine(xs[0],ys[0],xs[1],ys[1]);else{g.drawPolygon(xs,ys,3);g.setColor(new Color(90,220,255,45));g.fillPolygon(xs,ys,3);}g.setStroke(new BasicStroke(1));}
        }
        if(title.equals("WORKING MESH")&&selectedVertex!=null&&selectedVertices.contains(selectedVertex.id())){
            var v=selectedVertex;var p=camera.project(v.x(),v.y(),v.z());int px=view.x(p.x()),py=view.y(p.y());
            g.setColor(new Color(255,218,87));g.fillOval(px-4,py-4,8,8);g.drawString("v"+v.id(),px+7,py-6);
        }
        if(title.equals("WORKING MESH")&&selectedVertices.size()>1){
            g.setColor(new Color(255,218,87));for(var v:topologyProject.snapshot().vertices())if(selectedVertices.contains(v.id())){
                var p=camera.project(v.x(),v.y(),v.z());g.fillOval(view.x(p.x())-2,view.y(p.y())-2,4,4);
            }
        }
        if (title.equals("WORKING MESH") && hasBoundary() && selectionMode.getSelectedIndex()==1) {
            g.setColor(new Color(255,218,87));g.setStroke(new BasicStroke(3));
            var previous = boundaryVertices.get(boundaryVertices.size()-1);
            for (var vertex : boundaryVertices) {
                var a = camera.project(previous.x(),previous.y(),previous.z());
                var b = camera.project(vertex.x(),vertex.y(),vertex.z());
                g.drawLine(view.x(a.x()),view.y(a.y()),view.x(b.x()),view.y(b.y()));
                previous = vertex;
            }
            g.setStroke(new BasicStroke(1));
        }
        if(title.equals("WORKING MESH")&&selectedHit!=null&&selectionMode.getSelectedIndex()>0){
            var state=topologyProject.snapshot();var ids=new java.util.HashMap<Long,EditorTopologyProject.Vertex>();for(var v:state.vertices())ids.put(v.id(),v);
            var face=state.faces().stream().filter(f->f.id()==selectedHit.faceId()).findFirst().orElse(null);
            if(face!=null){
                long[] points=selectionMode.getSelectedIndex()==1?new long[]{selectedHit.edgeA(),selectedHit.edgeB()}:new long[]{face.a().vertex(),face.b().vertex(),face.c().vertex(),face.a().vertex()};
                g.setColor(new Color(255,218,87));g.setStroke(new BasicStroke(3));
                for(int i=1;i<points.length;i++){var a=ids.get(points[i-1]);var b=ids.get(points[i]);if(a==null||b==null)continue;
                    var pa=camera.project(a.x(),a.y(),a.z());var pb=camera.project(b.x(),b.y(),b.z());g.drawLine(view.x(pa.x()),view.y(pa.y()),view.x(pb.x()),view.y(pb.y()));}
                g.setStroke(new BasicStroke(1));
            }
        }
        if(title.equals("WORKING MESH")){
            if(boxStart!=null&&boxEnd!=null){g.setColor(new Color(255,218,87));
                if(lassoGesture&&selectionPath!=null){for(int i=1;i<selectionPath.size();i++)g.drawLine(selectionPath.get(i-1).x,selectionPath.get(i-1).y,selectionPath.get(i).x,selectionPath.get(i).y);g.drawLine(boxEnd.x,boxEnd.y,boxStart.x,boxStart.y);}
                else g.drawRect(Math.min(boxStart.x,boxEnd.x),Math.min(boxStart.y,boxEnd.y),Math.abs(boxStart.x-boxEnd.x),Math.abs(boxStart.y-boxEnd.y));}
            Color[] colors={new Color(255,100,100),new Color(100,235,130),new Color(110,165,255)};
            for(var h:gizmoHandles()){
                g.setColor(colors[h.axis()]);g.setStroke(new BasicStroke(2));g.drawLine((int)h.anchorX(),(int)h.anchorY(),h.x(),h.y());g.fillRect(h.x()-4,h.y()-4,8,8);g.drawString(axisLabel(h.axis()),h.x()+6,h.y()-5);
                if(axisDrag!=null&&axisDrag.handle().axis()==h.axis()){
                    var start=h.camera().project(h.worldX(),h.worldY(),h.worldZ());var end=h.camera().project(h.worldX()+h.direction().x()*dragDelta,h.worldY()+h.direction().y()*dragDelta,h.worldZ()+h.direction().z()*dragDelta);int dx=(int)Math.round(h.anchorX()+(end.x()-start.x())*h.scale()),dy=(int)Math.round(h.anchorY()+(end.y()-start.y())*h.scale());
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
                var p=camera.project(a,b,d);int px=view.x(p.x()),py=view.y(p.y());
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
