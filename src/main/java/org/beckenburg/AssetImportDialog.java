package org.beckenburg;

import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorGltfFile;
import org.beckenburg.editor.EditorGltfSource;
import org.beckenburg.editor.EditorPaintRecovery;
import org.beckenburg.editor.EditorUvDensity;
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
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorObservationBridge;
import org.beckenburg.editor.EditorMeshPrimitives;
import org.beckenburg.editor.EditorModelWorkspace;
import org.beckenburg.editor.EditorModelWorkspace.Field;
import org.beckenburg.editor.EditorMeshPicking;
import org.beckenburg.editor.EditorMeshEdges;

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
    private final List<JToggleButton> modelTools=new ArrayList<>();
    private int activeTool;
    private boolean restoringWorkspace;
    private final JSlider slider=new JSlider(0,12,0);
    private final JComboBox<String> preset=new JComboBox<>(new String[]{"PSX Prop / 100 triangles","PSX Character / 800 triangles","PSX Close-up / 1200 triangles","PSX World Kit / 4000 triangles","Modern / no style budget"});
    private final JTextArea details=new JTextArea(10,40);
    private final JLabel pathLabel=new JLabel("Choose OBJ, STL, GLB or native BTOP mesh");
    private final JButton apply=new JButton("PUBLISH TO EDITOR");
    private final JButton choose=new JButton("CHOOSE MODEL");
    private final ModelWireframeBuffer workingWireframe = new ModelWireframeBuffer();
    private final ModelWireframeBuffer previewWireframe = new ModelWireframeBuffer();
    private final JPanel preview=new JPanel(){
        @Override public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D p=(Graphics2D)g.create();
            p.setColor(new Color(20,24,29));p.fillRect(0,0,getWidth(),getHeight());
            if(source!=null)draw(p,source.triangles(),0,workingViewportWidth(),topologyProject==null?"SOURCE":"WORKING MESH");
            else workingWireframe.clear();
            if(modelingMesh!=null)draw(p,modelingMesh.triangles(),getWidth()/2,getWidth()-getWidth()/2,"MODEL EDIT PREVIEW");
            else if(primitivePreview!=null)draw(p,primitivePreview.triangles(),getWidth()/2,getWidth()-getWidth()/2,"PRIMITIVE PREVIEW");
            else if(result!=null&&comparisonVisible())draw(p,result.optimized().triangles(),workingViewportWidth(),getWidth()-workingViewportWidth(),"PREVIEW");
            else previewWireframe.clear();
            p.dispose();
        }
    };
    private final JSpinner vertexId=new JSpinner(new SpinnerNumberModel(0L,0L,Long.MAX_VALUE,1L));
    private final JSpinner vertexX=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexY=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JSpinner vertexZ=new JSpinner(new SpinnerNumberModel(0d,-1e9,1e9,.05));
    private final JButton moveVertex=new JButton("MOVE VERTEX"),undoMesh=new JButton("UNDO MESH"),redoMesh=new JButton("REDO MESH");
    private final JButton saveMesh=new JButton("SAVE MESH"),reopenMesh=new JButton("REOPEN MESH"),exportMesh=new JButton("EXPORT OBJ"),exportGlb=new JButton("EXPORT glTF / GLB");
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
    private EditorTopologyProject.State hoverState;
    private EditorMeshPicking.Projector hoverCamera;
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
    private final JButton selectEdges = new JButton("SELECT EDGES");
    private final JComboBox<EditorMeshEdges.Mode> edgeMode = new JComboBox<>(EditorMeshEdges.Mode.values());
    private EditorTopologyProject.Observation edgeObservation;
    private List<EditorMeshEdges.Segment> edgeSegments = List.of();
    private java.util.Set<Long> edgeIds = java.util.Set.of();
    private final JToggleButton snapToVertex = new JToggleButton("SNAP TO VERTEX");
    private record SnapPick(EditorTopologyProject.Observation observation,java.util.Set<Long> ids,EditorTopologyProject.Vertex anchor) {}
    private record SnapGuide(EditorTopologyProject.Observation observation,EditorTopologyProject.Vertex anchor,EditorTopologyProject.Vertex target) {}
    private SnapPick snapPick;
    private SnapGuide snapGuide;
    private EditorTopologyProject.Vertex snapHover;
    private final JSpinner slideFraction = new JSpinner(new SpinnerNumberModel(.25, .001, .999, .05));
    private final JCheckBox reverseSlide = new JCheckBox("Reverse edge direction");
    private final JButton previewSlide = new JButton("PREVIEW VERTEX SLIDE");
    private final JSpinner bevelDepth=new JSpinner(new SpinnerNumberModel(.1,.000001,10000d,.05));
    private final JButton previewBevel=new JButton("PREVIEW BEVEL");
    private final JButton previewFaceEdit=new JButton("PREVIEW FACE EDIT"),applyFaceEdit=new JButton("APPLY MODEL EDIT"),cancelFaceEdit=new JButton("CANCEL MODEL EDIT");
    private EditorTopologyProject.EditPreview modelingPreview;
    private boolean materialEditPreview;
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
    private final JLabel materialRuntime=new JLabel("Actual runtime preview",SwingConstants.CENTER){
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            var frame=projectedFrame;
            if(frame!=null && selectedHit!=null && materialWorkspace!=null && materialWorkspace.modelView() &&
                modelingPreview==null && !materialWorkspace.pencilActive()){
                graphics.drawImage(frame.outline(selectedHit.faceId()),(getWidth()-frame.image.getWidth())/2,(getHeight()-frame.image.getHeight())/2,null);
            }
            var stroke=projectedStroke;if(stroke==null||stroke.frame!=projectedFrame)return;
            int width=stroke.frame.image.getWidth();
            int dx=(getWidth()-width)/2,dy=(getHeight()-stroke.frame.image.getHeight())/2;
            graphics.setColor(stroke.color==0?Color.WHITE:new Color(stroke.color,true));
            for(int i=stroke.coverage.nextSetBit(0);i>=0;){
                int end=Math.min(stroke.coverage.nextClearBit(i),(i/width+1)*width);
                graphics.fillRect(dx+i%width,dy+i/width,end-i,1);i=stroke.coverage.nextSetBit(end);
            }
        }
    };
    private MaterialReviewFrame projectedFrame;
    private EditorObservationBridge materialObservation;
    private final JMenuItem materialAccess=new JMenuItem("Start local observation…");
    private final JMenuItem materialEditAccess=new JMenuItem("Start local material editing…");
    private MaterialReviewMip.Cache reviewMips=new MaterialReviewMip.Cache();
    private MaterialReviewStroke projectedStroke;
    private int reviewWidth,reviewHeight;
    private final JSpinner materialRotation=new JSpinner(new SpinnerNumberModel(0d,-360d,360d,15d));
    private final JComboBox<EditorTopologyProject.UvTransform> uvOperation=new JComboBox<>(EditorTopologyProject.UvTransform.values());
    private final JComboBox<String> uvScope=new JComboBox<>(new String[]{"Selected face","Connected UV island"}),uvPivot=new JComboBox<>(new String[]{"UV bounds center","UV origin"});
    private final JSpinner uvFirst=new JSpinner(new SpinnerNumberModel(.1,-1000000d,1000000d,.05)),uvSecond=new JSpinner(new SpinnerNumberModel(0d,-1000000d,1000000d,.05));
    private final JLabel uvFirstLabel=new JLabel("U distance"),uvSecondLabel=new JLabel("V distance");
    private final JButton previewUv=new JButton("PREVIEW UV TRANSFORM");
    private JTabbedPane materialTools;
    private final JComboBox<EditorMeshMaterial.Blend> paintBlend=new JComboBox<>(EditorMeshMaterial.Blend.values());
    private final JSpinner paintOpacity=new JSpinner(new SpinnerNumberModel(100,0,100,1));
    private final JButton previewPaintStyle=new JButton("Preview layer settings");
    private EditorMeshMaterial shownPaintMaterial;
    private final DefaultListModel<EditorMeshMaterial.LayerInfo> paintLayers=new DefaultListModel<>();
    private final JList<EditorMeshMaterial.LayerInfo> paintLayer=new JList<>(paintLayers);
    private final JTextField paintLayerName=new JTextField();
    private final JButton addPaintLayer=new JButton("Add"),removePaintLayer=new JButton("Remove"),
        raisePaintLayer=new JButton("Up"),lowerPaintLayer=new JButton("Down");
    private boolean updatingPaintLayers;
    private EditorTopologyProject.State materialRenderState;
    private EditorMaterialWorkspace.Visibility materialRenderedVisibility;
    private final JComboBox<String> materialReviewMode=new JComboBox<>(new String[]{"Material","Checker · 1 texel","Checker · 8 texels","Checker · 32 texels","Mip 1","Mip 2","Mip 3","Mip 4"});
    private static final int[] CHECKER_SIZES={0,1,8,32};
    private int renderedMode;
    private final JLabel uvDensity=new JLabel("Select a bound face to measure UVs");
    private final JButton fixUvStretch=new JButton("Fix selected face stretch");
    private final JButton relaxUvInterior=new JButton("Relax island interior");
    private final JButton packMaterialUvs=new JButton("Preview material packing");
    private final JComboBox<String> pinScope=new JComboBox<>(new String[]{"Corner A","Corner B","Corner C","Entire island"});
    private final JButton pinUvs=new JButton("Pin"),unpinUvs=new JButton("Unpin");
    private final JComboBox<String> seamEdge=new JComboBox<>(new String[]{"A–B","B–C","C–A"});
    private final JButton markSeam=new JButton("Mark seam"),clearSeam=new JButton("Clear mark");
    private final JButton unwrapIsland=new JButton("Unwrap island (LSCM)");
    private final JSpinner packPadding=new JSpinner(new SpinnerNumberModel(1,0,1024,1));
    private final JCheckBox keepPackDensity=new JCheckBox("Keep current texel density");
    private double materialRenderedRotation=Double.NaN;
    private boolean materialRendering;
    private EditorTopologyProject.State listedMaterials;
    private Long listedMaterialFace;
    private boolean updatingMaterialList;
    private EditorObjMaterials.Capture sourceCapture;
    private EditorGltfSource gltfSource;
    private EditorPaintRecovery paintRecovery;
    private final JLabel paintRecoveryStatus=new JLabel();
    private final JButton openPaintRecovery=new JButton("Open paint recovery…");
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
        fitCamera.setVisible(false);fitCamera.addActionListener(e->{cancelAxisDrag();cancelBoxSelection();cameraZoom=1;cameraPanX=0;cameraPanY=0;preview.repaint();});selectionMode.addActionListener(e->{stopSnap();clearEdgeSelection();cancelAxisDrag();refreshMeshControls();preview.repaint();});
        preview.addMouseListener(new MouseAdapter(){
            @Override public void mouseExited(MouseEvent e){clearHover();snapHover=null;preview.repaint();}
            @Override public void mousePressed(MouseEvent e){clearHover();if(e.getButton()==MouseEvent.BUTTON1){if(snapPick!=null){pickSnap(e.getX(),e.getY());return;}if(!beginAxisDrag(e.getX(),e.getY())){
                if(boxSelect.isSelected()&&!busy&&modelingPreview==null&&topologyProject!=null&&e.getX()<workingViewportWidth()){boxStart=e.getPoint();boxEnd=e.getPoint();selectionPath=new ArrayList<>();selectionPath.add(e.getPoint());lassoGesture=areaShape.getSelectedIndex()==1;preview.repaint();}
                else pickMesh(e.getX(),e.getY());
            }}else {cancelAxisDrag();cancelBoxSelection();if(source!=null&&projection()==EditorMeshPicking.Projection.PERSPECTIVE){cameraGesture=e.getPoint();cameraButton=e.getButton();}}}
            @Override public void mouseReleased(MouseEvent e){if(e.getButton()==MouseEvent.BUTTON1){if(axisDrag!=null)finishAxisDrag();else finishBoxSelection();}else cameraGesture=null;}
        });
        preview.addMouseMotionListener(new MouseMotionAdapter(){@Override public void mouseMoved(MouseEvent e){if(snapPick!=null){hoverSnap(e.getX(),e.getY());}else hoverMesh(e.getX(),e.getY());}
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
        exportGlb.addActionListener(e->{if(topologyProject==null||busy||modelingPreview!=null)return;var chooser=new JFileChooser();chooser.setDialogTitle("Export a .gltf or .glb copy");chooser.setSelectedFile(topologyProject.documentPath().resolveSibling("mesh-export.glb").toFile());if(chooser.showSaveDialog(dialog)==JFileChooser.APPROVE_OPTION)exportGlb(chooser.getSelectedFile().toPath());});
        reopenMesh.addActionListener(e->{boolean opened=meshEdit(()->{try{
            reopenAuthoringDocument();
        }catch(IOException failure){throw new java.io.UncheckedIOException(failure);}});if(opened)restoreWorkspace(topologyProject.savedWorkspace());});
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
        materialList.addActionListener(e->{if(!updatingMaterialList){refreshMaterialSwatch();refreshMaterialWindow();}});
        openMaterials.addActionListener(e->openMaterials());
        uvOperation.addActionListener(e->{clearModelingPreview();var operation=(EditorTopologyProject.UvTransform)uvOperation.getSelectedItem();uvFirstLabel.setText(operation==EditorTopologyProject.UvTransform.ROTATE?"Degrees":operation==EditorTopologyProject.UvTransform.SCALE?"U factor":"U distance");uvSecondLabel.setText(operation==EditorTopologyProject.UvTransform.SCALE?"V factor":"V distance");uvSecond.setVisible(operation!=EditorTopologyProject.UvTransform.ROTATE);uvSecondLabel.setVisible(operation!=EditorTopologyProject.UvTransform.ROTATE);uvFirst.setValue(operation==EditorTopologyProject.UvTransform.ROTATE?90d:operation==EditorTopologyProject.UvTransform.SCALE?1d:.1);uvSecond.setValue(operation==EditorTopologyProject.UvTransform.SCALE?1d:0d);});
        uvScope.addActionListener(e->clearModelingPreview());uvPivot.addActionListener(e->clearModelingPreview());uvFirst.addChangeListener(e->clearModelingPreview());uvSecond.addChangeListener(e->clearModelingPreview());
        previewUv.addActionListener(e->{if(selectedHit==null||topologyProject==null||busy)return;long face=selectedHit.faceId();boolean island=uvScope.getSelectedIndex()==1,origin=uvPivot.getSelectedIndex()==1;var operation=(EditorTopologyProject.UvTransform)uvOperation.getSelectedItem();double first=((Number)uvFirst.getValue()).doubleValue(),second=operation==EditorTopologyProject.UvTransform.ROTATE?0:((Number)uvSecond.getValue()).doubleValue();beginMaterialPreview(p->p.previewUvTransform(face,island,operation,first,second,origin));});
        chooseMaterial.addActionListener(e->{
            if(busy||topologyProject==null||selectedHit==null)return;
            var chooser=new JFileChooser();chooser.setDialogTitle("Embed PNG material in native mesh");chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG texture","png"));
            if(chooser.showOpenDialog(dialog)==JFileChooser.APPROVE_OPTION)previewPngMaterial(chooser.getSelectedFile().toPath());
        });
        previewMaterial.addActionListener(e->{if(selectedHit!=null&&materialList.getSelectedItem() instanceof EditorMeshMaterial material){long face=selectedHit.faceId();beginMaterialPreview(p->p.previewBindMaterial(face,material));}});
        previewFaceEdit.addActionListener(e->previewModelingEdit());cancelFaceEdit.addActionListener(e->clearModelingPreview());
        edgeMode.addActionListener(e -> {
            clearEdgeSelection();
            if (selectedHit != null && selectionMode.getSelectedIndex()==1) selectionLabel.setText("Edge "+selectedHit.edgeA()+" - "+selectedHit.edgeB());
            refreshMeshControls();preview.repaint();
        });
        selectEdges.addActionListener(e -> selectEdgeGroup());
        snapToVertex.addActionListener(e -> {
            if (!snapToVertex.isSelected()) {stopSnap();preview.repaint();return;}
            var ids=java.util.Set.copyOf(selectedIds());
            if (ids.isEmpty() || busy || topologyProject==null) {stopSnap();return;}
            clearPrimitivePreview();cancelAxisDrag();cancelBoxSelection();clearHover();
            var observation=topologyProject.observe();
            var anchor=ids.size()==1 ? observation.vertex(ids.iterator().next()) : null;
            snapPick=new SnapPick(observation,ids,anchor);snapGuide=null;snapHover=null;
            selectionLabel.setText(anchor==null?"Pick an anchor vertex in the selection":"Pick a target for v"+anchor.id());preview.repaint();
        });
        slideFraction.addChangeListener(e -> clearModelingPreview());
        reverseSlide.addActionListener(e -> clearModelingPreview());
        previewSlide.addActionListener(e -> {
            if (busy || selectedHit == null || selectionMode.getSelectedIndex() != 1) return;
            var hit=selectedHit;boolean reverse=reverseSlide.isSelected();
            double fraction=((Number)slideFraction.getValue()).doubleValue();
            if(edgeObservation!=null) {
                if(!hasEdgeSelection()) {
                    clearEdgeSelection();refreshMeshControls();selectionLabel.setText("Pick the loop again after the model changed");return;
                }
                if(edgeMode.getSelectedItem()!=EditorMeshEdges.Mode.LOOP) return;
                var selection=edgeObservation;
                beginModelingPreview(p->p.previewLoopSlide(selection,hit.edgeA(),hit.edgeB(),hit.faceId(),reverse,fraction));
            } else {
                long from=reverse?hit.edgeB():hit.edgeA(), toward=reverse?hit.edgeA():hit.edgeB();
                beginModelingPreview(p->p.previewVertexSlide(from,toward,fraction));
            }
        });
        bevelDepth.addChangeListener(e->clearModelingPreview());
        previewBevel.addActionListener(e->{if(busy||topologyProject==null||selectedHit==null||selectionMode.getSelectedIndex()==2)return;var hit=selectedHit;boolean corner=selectionMode.getSelectedIndex()==0;double depth=((Number)bevelDepth.getValue()).doubleValue();beginModelingPreview(p->p.previewBevel(corner?hit.vertexId():hit.edgeA(),corner?hit.vertexId():hit.edgeB(),hit.faceId(),depth));});
        applyFaceEdit.addActionListener(e->{if(modelingPreview!=null&&!busy){var edit=modelingPreview;var kept=materialEditPreview?selectedHit:null;
            meshEdit(()->topologyProject.applyPreview(edit));
            if(topologyProject.snapshot()==edit.snapshot()){
                if(kept!=null)selectMaterialFace(kept.faceId());
                else{selectedHit=null;selectionLabel.setText("Model edit committed");}
            }clearModelingPreview();}});
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
        openPaintRecovery.addActionListener(e->choosePaintRecovery());
        var menus=new JMenuBar();menus.add(EditorTopologyWorkspace.menu("File",newMesh,choose,saveMesh,reopenMesh,openPaintRecovery,exportMesh,exportGlb,close));menus.add(EditorTopologyWorkspace.menu("Edit",undoMesh,redoMesh));
        EditorTopologyWorkspace.shortcut(menus.getMenu(0),1,KeyEvent.VK_O);EditorTopologyWorkspace.shortcut(menus.getMenu(0),2,KeyEvent.VK_S);EditorTopologyWorkspace.shortcut(menus.getMenu(1),0,KeyEvent.VK_Z);EditorTopologyWorkspace.shortcut(menus.getMenu(1),1,KeyEvent.VK_Y);
        var inspector=new JPanel(new BorderLayout(0,8));var toolTitle=new JLabel("SELECT");inspector.add(toolTitle,BorderLayout.NORTH);inspector.add(new JScrollPane(inspectorCards),BorderLayout.CENTER);inspector.add(EditorTopologyWorkspace.column(applyFaceEdit,cancelFaceEdit),BorderLayout.SOUTH);
        inspector.setBorder(BorderFactory.createEmptyBorder(8,5,5,5));
        inspectorCards.add("Select",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Area shape",areaShape,"Occlusion",xraySelect),boxSelect,EditorTopologyWorkspace.form("Edge traversal",edgeMode),selectEdges,new JLabel("Drag in the working view. Escape cancels.")));
        inspectorCards.add("Move",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Vertex ID",vertexId,"X",vertexX,"Y",vertexY,"Z",vertexZ,"Grid (0 off)",meshSnap),moveVertex,snapToVertex,EditorTopologyWorkspace.form("Slide fraction",slideFraction),reverseSlide,previewSlide));
        inspectorCards.add("Transform",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Space",transformSpace,"Operation",transformKind,"Axis",transformAxis,"Degrees / factor",transformAmount,"Pivot",transformPivot,"Angle snap",angleSnap),previewTransform));
        inspectorCards.add("Create",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Primitive",primitiveKind,"Width",primitiveWidth,"Height",primitiveHeight,"Depth",primitiveDepth,"Segments",primitiveSegments,"Rings / grid",primitiveRings),previewPrimitive,applyPrimitive));
        inspectorCards.add("Face",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Operation",faceOperation,"Distance / fraction",faceAmount),previewFaceEdit));
        inspectorCards.add("Bevel",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Cut depth",bevelDepth),previewBevel,new JLabel("Pick one EDGE or VERTEX in the working view."),new JLabel("Closed convex support; one flat cut."),new JLabel("Cap: active face material, planar UVs.")));
        inspectorCards.add("Repair",EditorTopologyWorkspace.column(previewWeld,previewDegenerates,previewUnused));
        inspectorCards.add("Review",EditorTopologyWorkspace.column(new JLabel("Geometry regression (0 = original)"),slider,reset,preset));
        var toolsBar=new JPanel(new GridLayout(0,1,2,3));ButtonGroup toolModes=new ButtonGroup();
        String[] toolNames={"Select","Move","Transform","Create","Face","Bevel","Repair","Review"};
        for(int i=0;i<toolNames.length;i++){int index=i;var button=new JToggleButton(toolNames[i]);button.setMargin(new Insets(8,5,8,5));button.setToolTipText("Show "+toolNames[i].toLowerCase(Locale.ROOT)+" settings");button.addActionListener(e->{activeTool=index;((CardLayout)inspectorCards.getLayout()).show(inspectorCards, index==2?"Transform":toolNames[index]);toolTitle.setText(toolNames[index].toUpperCase(Locale.ROOT));workspace.setInspectorVisible(true);});toolModes.add(button);toolsBar.add(button);modelTools.add(button);if(i==0)button.setSelected(true);}
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
    private java.util.Map<Field,JComponent> workspaceControls() {
        var controls=new java.util.EnumMap<Field,JComponent>(Field.class);
        controls.put(Field.MODE,selectionMode); controls.put(Field.TRAVERSAL,edgeMode); controls.put(Field.AREA,areaShape);
        controls.put(Field.XRAY,xraySelect); controls.put(Field.NORMALS,normalOverlay); controls.put(Field.COMPARE,compareView);
        controls.put(Field.SPACE,transformSpace); controls.put(Field.TRANSFORM,transformKind); controls.put(Field.AXIS,transformAxis);
        controls.put(Field.PIVOT,transformPivot); controls.put(Field.AMOUNT,transformAmount); controls.put(Field.GRID,meshSnap);
        controls.put(Field.ANGLE,angleSnap); controls.put(Field.PRIMITIVE,primitiveKind); controls.put(Field.WIDTH,primitiveWidth);
        controls.put(Field.HEIGHT,primitiveHeight); controls.put(Field.DEPTH,primitiveDepth); controls.put(Field.SEGMENTS,primitiveSegments);
        controls.put(Field.RINGS,primitiveRings); controls.put(Field.FACE,faceOperation); controls.put(Field.FACE_AMOUNT,faceAmount);
        controls.put(Field.BEVEL,bevelDepth); controls.put(Field.SLIDE,slideFraction); controls.put(Field.REVERSE,reverseSlide);
        controls.put(Field.PRESET,preset); controls.put(Field.LEVEL,slider);
        return controls;
    }
    private EditorModelWorkspace captureWorkspace() {
        var values=new java.util.EnumMap<Field,Double>(Field.class);
        workspaceControls().forEach((field,control)->{
            double value;
            if(control instanceof JComboBox<?> combo)value=combo.getSelectedIndex();
            else if(control instanceof JSpinner spinner)value=((Number)spinner.getValue()).doubleValue();
            else if(control instanceof AbstractButton button)value=button.isSelected()?1:0;
            else value=((JSlider)control).getValue();
            values.put(field,value);
        });
        values.put(Field.TOOL,(double)activeTool);values.put(Field.INSPECTOR,workspace.savedInspectorVisible()?1d:0d);
        values.put(Field.BROWSERS,workspace.savedBrowsersVisible()?1d:0d);values.put(Field.FOCUS,workspace.focused()?1d:0d);
        Long primary=selectedVertex!=null && selectedVertices.contains(selectedVertex.id())?selectedVertex.id():null;
        var pick=selectedHit!=null && selectedHit.edgeA()!=selectedHit.edgeB()?selectedHit:null;
        return new EditorModelWorkspace(projection(),cameraYaw,cameraPitch,cameraZoom,cameraPanX,cameraPanY,values,
                List.copyOf(selectedVertices),primary,pick,normalAnchor,pick!=null&&hasEdgeSelection());
    }
    private void restoreWorkspace(EditorModelWorkspace view) {
        if(view==null)return;
        restoringWorkspace=true;
        try {
            if(workspace.focused())workspace.toggleFocus();
            meshView.setSelectedItem(view.projection());cameraYaw=view.yaw();cameraPitch=view.pitch();cameraZoom=view.zoom();cameraPanX=view.panX();cameraPanY=view.panY();
            var settings=view.settings();var controls=workspaceControls();
            for(var field:Field.values()){
                var value=settings.get(field);var control=controls.get(field);if(value==null||control==null)continue;
                if(control instanceof JComboBox<?> combo)combo.setSelectedIndex(value.intValue());
                else if(control instanceof JSpinner spinner){if(spinner.getValue() instanceof Integer)spinner.setValue(value.intValue());else spinner.setValue(value);}
                else if(control instanceof AbstractButton button)button.setSelected(value==1);
                else ((JSlider)control).setValue(value.intValue());
            }
            int tool=settings.getOrDefault(Field.TOOL,0d).intValue();modelTools.get(tool).doClick(0);
            workspace.setInspectorVisible(settings.getOrDefault(Field.INSPECTOR,1d)==1);
            workspace.setBrowsersVisible(settings.getOrDefault(Field.BROWSERS,0d)==1);
            if(settings.getOrDefault(Field.FOCUS,0d)==1)workspace.toggleFocus();
            clearEdgeSelection();selectedHit=view.hit();normalAnchor=view.surfaceFace();
            if(view.activeVertex()!=null){vertexId.setValue(view.activeVertex());selectVertex();}else selectedVertex=null;
            selectedVertices=java.util.Set.copyOf(view.vertices());
            selectionLabel.setText(selectedHit==null?selectedVertices.size()+" selected vertices":"Face "+selectedHit.faceId()+" / "+selectionMode.getSelectedItem());
        } finally {restoringWorkspace=false;}
        refreshMeshControls();recalculate();preview.repaint();
        if(view.edgeGroup() && selectedHit!=null)selectEdgeGroup();
    }
    private boolean saveAuthoring(){
        if(topologyProject==null||busy)return false;
        try{
            saveAuthoringDocument();return true;
        }catch(IOException failure){details.append("\nSAVE FAILED: "+failure.getMessage());return false;}
    }
    private void updateReviewedFingerprint(){
        if(path!=null&&path.toAbsolutePath().normalize().equals(topologyProject.documentPath()))reviewedSourceFingerprint=topologyProject.diskFingerprint();
    }
    private void saveAuthoringDocument()throws IOException{
        topologyProject.save(captureWorkspace());updateReviewedFingerprint();
        details.append("\nSAVED MESH "+topologyProject.documentPath());refreshMeshControls();
    }
    private void reopenAuthoringDocument()throws IOException{
        topologyProject.reopen();updateReviewedFingerprint();
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
    private static boolean gltfModel(String name) {
        String lower=name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".glb") || lower.endsWith(".gltf");
    }
    boolean exportGlb(Path destination){
        if(topologyProject==null||busy||modelingPreview!=null)return false;
        try{Path target=destination.toAbsolutePath().normalize();if(!gltfModel(target.getFileName().toString()))throw new IOException("Choose a .glb or .gltf destination");if(path!=null&&target.equals(path.toAbsolutePath().normalize()))throw new IOException("Export cannot replace original source");String previous=Files.exists(target)?EditorDocumentStore.fingerprint(EditorMeshPipeline.readSource(target,EditorGltfFile.MAX_BYTES)):null;topologyProject.exportGlb(workingObservation,target,previous);details.append("\nEXPORTED MODEL COPY "+target);return true;}catch(IOException failure){details.append("\nGLB EXPORT REFUSED: "+failure.getMessage());workspace.setBrowsersVisible(true);return false;}
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
            materialDialog.addComponentListener(new ComponentAdapter(){
                @Override public void componentHidden(ComponentEvent event){stopMaterialObservation();reviewMips=new MaterialReviewMip.Cache();}
            });
            materialDialog.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e){hideMaterialWindow();}
                public void windowClosed(WindowEvent e){stopMaterialObservation();}
            });
            materialRuntime.setPreferredSize(new Dimension(280,190));
            materialRuntime.setMinimumSize(new Dimension(160,120));
            materialRuntime.addComponentListener(new ComponentAdapter(){
                @Override public void componentResized(ComponentEvent e){refreshMaterialWindow();}
            });
            installProjectionInput();
            materialDialog.addWindowFocusListener(new WindowAdapter(){
                @Override public void windowLostFocus(WindowEvent e){projectedStroke=null;materialRuntime.repaint();}
            });
            var runtime=new JPanel(new BorderLayout(4,4));runtime.add(new JLabel("ACTUAL RUNTIME PREVIEW"),BorderLayout.NORTH);runtime.add(materialRuntime,BorderLayout.CENTER);runtime.add(EditorTopologyWorkspace.column(uvDensity,EditorTopologyWorkspace.form("Preview",materialReviewMode,"Model rotation",materialRotation)),BorderLayout.SOUTH);materialReviewMode.addActionListener(e->refreshMaterialWindow());materialRotation.addChangeListener(e->refreshMaterialWindow());
            var bindings=EditorTopologyWorkspace.column(new JLabel("MATERIAL PROPERTIES"),EditorTopologyWorkspace.form("Material",materialList),materialSwatch,previewMaterial,new JSeparator(),EditorTopologyWorkspace.form("New PNG wrap",materialWrap,"Alpha",materialAlpha,"UV origin",materialOrigin),chooseMaterial,new JLabel("<html>PNG changes are captured in a native preview.<br>Apply commits one face assignment.</html>"));
            var values=new JPanel(new GridLayout(0,2,5,5));values.add(uvFirstLabel);values.add(uvFirst);values.add(uvSecondLabel);values.add(uvSecond);
            var seamButtons=new JPanel(new GridLayout(1,2,3,0));seamButtons.add(markSeam);seamButtons.add(clearSeam);
            var uvSettings=EditorTopologyWorkspace.column(new JLabel("NATIVE CORNER UVS"),EditorTopologyWorkspace.form("Operation",uvOperation,"Scope",uvScope,"Pivot",uvPivot),values,previewUv,fixUvStretch,relaxUvInterior);
            markSeam.addActionListener(e->previewSeam(true));clearSeam.addActionListener(e->previewSeam(false));
            clearSeam.setToolTipText("Remove the authored mark. Existing UV coordinates stay in place.");
            fixUvStretch.setToolTipText("Preview uniform texel scale on this face while preserving its UV area and center");
            fixUvStretch.addActionListener(e->{
                if(selectedHit==null)return;
                long id=selectedHit.faceId();boolean island=uvScope.getSelectedIndex()==1;
                beginMaterialPreview(p->island?p.previewBalanceIsland(id):p.previewFixFaceStretch(id));
            });
            relaxUvInterior.setToolTipText("Preview a relaxed UV interior with seam and boundary coordinates fixed");
            relaxUvInterior.addActionListener(e->{
                if(selectedHit==null)return;
                long id=selectedHit.faceId();beginMaterialPreview(p->p.previewRelaxInterior(id));
            });
            materialTools=new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);materialTools.addTab("Material",bindings);materialTools.addTab("UV edit",uvSettings);
            var layerButtons=new JPanel(new GridLayout(1,4,2,0));
            for(var b:List.of(addPaintLayer,removePaintLayer,raisePaintLayer,lowerPaintLayer)){
                b.setMargin(new Insets(3,3,3,3));layerButtons.add(b);
            }
            paintLayer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            paintLayer.setVisibleRowCount(4);
            paintLayer.setCellRenderer(new DefaultListCellRenderer(){
                @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){
                    putClientProperty("html.disable",true);
                    return super.getListCellRendererComponent(list,value,index,selected,focus);
                }
            });
            materialTools.addTab("Layers",EditorTopologyWorkspace.column(new JLabel("BASE-COLOR LAYERS"),
                new JScrollPane(paintLayer),layerButtons,
                EditorTopologyWorkspace.form("Name",paintLayerName,"Blend",paintBlend,"Opacity (%)",paintOpacity),previewPaintStyle));
            materialTools.addTab("Pack",EditorTopologyWorkspace.column(new JLabel("SELECTED MATERIAL ISLANDS"),
                EditorTopologyWorkspace.form("Gutter (texels)",packPadding),keepPackDensity,packMaterialUvs,
                EditorTopologyWorkspace.form("Pin selection",pinScope),pinUvs,unpinUvs));
            materialTools.addTab("Seams",EditorTopologyWorkspace.column(EditorTopologyWorkspace.form("Face edge",seamEdge),seamButtons,unwrapIsland));
            unwrapIsland.setToolTipText("Unfold this chart in texture pixels, holding its pinned UVs in place.");
            unwrapIsland.addActionListener(e->{if(selectedHit!=null){long id=selectedHit.faceId();beginMaterialPreview(p->p.previewUnwrap(id));}});
            pinScope.setToolTipText("Packing holds any island with a pinned UV in place.");
            pinUvs.addActionListener(e->previewPin(true));
            unpinUvs.addActionListener(e->previewPin(false));
            materialTools.addChangeListener(e->{if(materialWorkspace!=null){
                String tab=materialTools.getTitleAt(materialTools.getSelectedIndex());
                materialWorkspace.showUvGuides(tab.equals("Pack"),tab.equals("Seams"));
            }});
            packMaterialUvs.setToolTipText("Place all islands of the selected material in tile zero. The source image stays fixed.");
            packMaterialUvs.addActionListener(e->{
                if(selectedHit==null)return;
                long id=selectedHit.faceId();int padding=(int)packPadding.getValue();boolean keep=keepPackDensity.isSelected();
                beginMaterialPreview(p->p.previewPackMaterial(id,padding,keep));
            });
            paintLayer.addListSelectionListener(e->{if(!e.getValueIsAdjusting() && !updatingPaintLayers){showLayerSettings();refreshMaterialWindow();}});
            previewPaintStyle.addActionListener(e->previewLayerEdit(EditorTopologyProject.LayerEdit.CONFIGURE));
            addPaintLayer.addActionListener(e->previewLayerEdit(EditorTopologyProject.LayerEdit.ADD));
            removePaintLayer.addActionListener(e->previewLayerEdit(EditorTopologyProject.LayerEdit.REMOVE));
            raisePaintLayer.addActionListener(e->previewLayerEdit(EditorTopologyProject.LayerEdit.UP));
            lowerPaintLayer.addActionListener(e->previewLayerEdit(EditorTopologyProject.LayerEdit.DOWN));
            var commands=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,3));commands.add(sharedMaterialAction("Undo",undoMesh));commands.add(sharedMaterialAction("Redo",redoMesh));commands.add(sharedMaterialAction("Save document",saveMesh));commands.add(sharedMaterialAction("Apply edit",applyFaceEdit));commands.add(sharedMaterialAction("Cancel preview",cancelFaceEdit));
            var back=new JButton("Return to modeling");back.addActionListener(e->{hideMaterialWindow();dialog.toFront();});commands.add(back);
            materialWorkspace=new EditorMaterialWorkspace(materialTools,runtime,commands,id->{
                if(busy||modelingPreview!=null||topologyProject==null)return;
                selectMaterialFace(id);
            },()->{
                if(selectedHit!=null && !materialWorkspace.visibility().includes(selectedHit.faceId())){
                    selectedHit=null;normalAnchor=null;selectionLabel.setText("Select a visible face");
                }
                refreshMeshControls();preview.repaint();
            });
            materialWorkspace.setPencilAction((face,texels,color)->{
                var layer=paintLayer.getSelectedValue();
                var tile=materialWorkspace.tile();
                if(!busy && modelingPreview==null && layer!=null && materialWorkspace.visibility().includes(face))
                    beginMaterialPreview(p->p.previewPencil(face,layer.id(),texels,color,tile));
            });
            materialWorkspace.setProjectionCancel(()->{projectedStroke=null;materialRuntime.repaint();});
            materialWorkspace.setViewChanged(this::refreshMaterialWindow);
            var verify=new JButton("Verify visible material");
            verify.addActionListener(e->{try{
                var service=materialService();var checks=service.test(topologyProject.revision(),"visible-material").result();
                var report=new StringBuilder();
                for(var check:checks)report.append(check.passed()?"PASS  ":"FAIL  ").append(check.detail()).append('\n');
                JOptionPane.showMessageDialog(materialDialog,report.toString(),"Visible material",JOptionPane.INFORMATION_MESSAGE);
            }catch(RuntimeException failure){materialWorkspace.previewIssue(rootMessage(failure));}});
            materialAccess.addActionListener(e->{
                if(materialObservation!=null){stopMaterialObservation();return;}
                showMaterialAccess(EditorObservationBridge.Scope.OBSERVE);
            });
            materialEditAccess.addActionListener(e->{
                var disk=new JCheckBox("Also allow saving and reopening this mesh document");
                if(JOptionPane.showConfirmDialog(materialDialog,new Object[]{"Allow local clients to edit layer opacity and use this document's undo history?",disk},
                    "Local material editing",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)
                    showMaterialAccess(disk.isSelected()?EditorObservationBridge.Scope.EDIT_AND_SAVE:EditorObservationBridge.Scope.EDIT);
            });
            var fileMenu=new JMenuBar();var file=EditorTopologyWorkspace.menu("File",openPaintRecovery,verify);
            file.add(materialAccess);file.add(materialEditAccess);fileMenu.add(file);materialDialog.setJMenuBar(fileMenu);
            var panel=new JPanel(new BorderLayout());panel.add(materialWorkspace,BorderLayout.CENTER);panel.add(paintRecoveryStatus,BorderLayout.SOUTH);
            materialDialog.setContentPane(panel);materialDialog.setSize(1160,760);materialDialog.setMinimumSize(new Dimension(900,600));materialDialog.setLocationRelativeTo(dialog);
        }
        materialDialog.setVisible(true);refreshMaterialWindow();
    }
    private void hideMaterialWindow(){
        stopMaterialObservation();
        if(modelingPreview!=null)clearModelingPreview();
        materialDialog.setVisible(false);
    }
    private void showMaterialAccess(EditorObservationBridge.Scope scope){
        try{
            var access=startMaterialAccess(scope);
            var info=new JTextArea("Address  "+access.url()+"\nAuthorization: Bearer "+access.token(),3,68);
            info.setEditable(false);
            JOptionPane.showMessageDialog(materialDialog,info,scope==EditorObservationBridge.Scope.OBSERVE?
                "Read-only material observation":"Local material editing and shared undo/redo",JOptionPane.INFORMATION_MESSAGE);
        }catch(IOException|RuntimeException failure){materialWorkspace.previewIssue(rootMessage(failure));}
    }
    private EditorObservationBridge startMaterialAccess(EditorObservationBridge.Scope scope)throws IOException{
        if(materialObservation!=null)throw new IllegalStateException("Material observation is already active");
        java.util.Objects.requireNonNull(scope);
        var document=topologyProject;
        materialObservation=new EditorObservationBridge(materialService(),scope,task->SwingUtilities.invokeLater(()->{
            try{task.run();}finally{
                // Failed native batches restore their before-state at a new revision.
                if(document==topologyProject&&!busy&&!document.isCurrent(workingObservation)){
                    refreshWorkingObservation();refreshMeshControls();
                }
            }
        }));
        materialAccess.setText("Stop local access");materialEditAccess.setEnabled(false);
        return materialObservation;
    }
    private void stopMaterialObservation(){
        if(materialObservation!=null){materialObservation.close();materialObservation=null;}
        materialAccess.setText("Start local observation…");materialEditAccess.setEnabled(true);
    }
    private EditorAgentService<EditorTopologyProject.State> materialService(){
        var host=new MaterialHost(topologyProject);host.checkAccess();
        return new EditorAgentService<>(host,meshHistory,this::meshChanged,()->{});
    }
    private final class MaterialHost extends MaterialEditorAgent {
        MaterialHost(EditorTopologyProject document){super(document);}
        public void checkAccess(){
            if(!SwingUtilities.isEventDispatchThread()||document==null||document!=topologyProject||
                materialDialog==null||!materialDialog.isShowing()||busy||modelingPreparing||modelingPreview!=null||projectedStroke!=null)
                throw new IllegalStateException("Material document is unavailable or has an uncommitted preview");
        }
        Long selectedFaceId(){return selectedHit==null?null:selectedHit.faceId();}
        EditorMeshMaterial.LayerInfo selectedLayer(EditorMeshMaterial material){
            return material==shownPaintMaterial?paintLayer.getSelectedValue():null;
        }
        public void save()throws IOException{saveAuthoringDocument();}
        public void reopen()throws IOException{reopenAuthoringDocument();}
        public void didReopen(){restoreWorkspace(document.savedWorkspace());}
        MaterialReviewFrame frame(){
            if(materialRendering||projectedFrame==null||materialRenderState!=document.snapshot()||
                !(materialRuntime.getIcon() instanceof ImageIcon icon)||icon.getImage()!=projectedFrame.image)
                throw new IllegalStateException("Wait for the current native material render");
            return projectedFrame;
        }
    }
    private void selectMaterialFace(long id){
        var face=topologyProject.snapshot().faces().stream().filter(f->f.id()==id).findFirst().orElse(null);
        if(face==null)return;
        normalAnchor=id;
        selectedHit=new EditorMeshPicking.Hit(id,face.a().vertex(),face.a().vertex(),face.b().vertex(),0,face.a().u(),face.a().v());
        selectionLabel.setText("Face "+id);refreshMeshControls();preview.repaint();
    }
    private Point reviewPoint(MouseEvent event,MaterialReviewFrame frame){
        return new Point(event.getX()-(materialRuntime.getWidth()-frame.image.getWidth())/2,
            event.getY()-(materialRuntime.getHeight()-frame.image.getHeight())/2);
    }
    private void installProjectionInput(){
        materialRuntime.setToolTipText("Click a visible face to select it. With Pencil enabled, drag to paint this material.");
        var mouse=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                projectedStroke=null;var frame=projectedFrame;
                if(!SwingUtilities.isLeftMouseButton(e)||frame==null||busy||modelingPreview!=null||topologyProject==null ||
                    topologyProject.snapshot()!=materialRenderState)return;
                var point=reviewPoint(e,frame);
                long hit=frame.face(frame.pixel(point.x,point.y));
                if(!materialWorkspace.pencilActive()){if(hit>=0)selectMaterialFace(hit);return;}
                if(!frame.sourceTexels){materialWorkspace.previewIssue("Select Material to paint base-level texels.");return;}
                var layer=paintLayer.getSelectedValue();
                if(selectedHit==null||layer==null||shownPaintMaterial==null)return;
                projectedStroke=new MaterialReviewStroke(frame,topologyProject.observe(),selectedHit.faceId(),layer.id(),
                    e.isShiftDown()?0:materialWorkspace.pencilColor(),shownPaintMaterial.key(),materialWorkspace.projectionPadding());
                projectedStroke.sample(point);
                materialRuntime.repaint();
            }
            @Override public void mouseDragged(MouseEvent e){
                if(projectedStroke!=null){projectedStroke.sample(reviewPoint(e,projectedStroke.frame));materialRuntime.repaint();}
            }
            @Override public void mouseReleased(MouseEvent e){
                var stroke=projectedStroke;projectedStroke=null;materialRuntime.repaint();
                if(stroke==null||stroke.frame!=projectedFrame||busy||modelingPreview!=null||!SwingUtilities.isLeftMouseButton(e)||
                    selectedHit==null || selectedHit.faceId()!=stroke.anchor || paintLayer.getSelectedValue()==null ||
                    !paintLayer.getSelectedValue().id().equals(stroke.layer) || !materialWorkspace.pencilActive())return;
                stroke.sample(reviewPoint(e,stroke.frame));
                if(stroke.overflow){materialWorkspace.previewIssue("Projected stroke exceeds the 65536 face/texel sample limit");return;}
                if(!stroke.samples.isEmpty())beginMaterialPreview(p->p.previewProjectedPencil(stroke.observed,stroke.anchor,stroke.layer,stroke.samples,stroke.color,stroke.padding));
            }
        };
        materialRuntime.addMouseListener(mouse);materialRuntime.addMouseMotionListener(mouse);
    }
    private void showLayerSettings(){
        var layer=paintLayer.getSelectedValue();
        var style=layer==null?EditorMeshMaterial.PaintStyle.DEFAULT:layer.style();
        paintBlend.setSelectedItem(style.blend());paintOpacity.setValue(style.opacity());paintLayerName.setText(layer==null?"":layer.name());
    }
    private void previewLayerEdit(EditorTopologyProject.LayerEdit action){
        var layer=paintLayer.getSelectedValue();
        if(selectedHit==null || layer==null)return;
        long face=selectedHit.faceId();
        String name=action==EditorTopologyProject.LayerEdit.ADD?"Layer "+(paintLayers.size()+1):paintLayerName.getText();
        var style=new EditorMeshMaterial.PaintStyle((EditorMeshMaterial.Blend)paintBlend.getSelectedItem(),(int)paintOpacity.getValue());
        beginMaterialPreview(p->p.previewPaintLayer(face,layer.id(),action,name,style));
    }
    private void refreshMaterialWindow(){
        if(materialWorkspace==null||materialDialog==null||!materialDialog.isVisible())return;
        var state=modelingPreview!=null?modelingPreview.snapshot():topologyProject==null?null:topologyProject.snapshot();Long face=selectedHit==null?null:selectedHit.faceId();EditorMeshMaterial material=null;
        int faceIndex=-1;
        if(state!=null && face!=null)for(int i=0;i<state.faces().size();i++){
            var f=state.faces().get(i);
            if(f.id()!=face)continue;
            faceIndex=i;material=state.materials().stream().filter(m->m.key().equals(f.material())).findFirst().orElse(null);break;
        }
        boolean bound=material!=null;
        if(material==null&&materialList.getSelectedItem() instanceof EditorMeshMaterial m)material=m;
        materialWorkspace.observe(state,face,material,modelingPreview==null?null:modelingPreview.description());
        materialRuntime.repaint();
        if(material!=shownPaintMaterial){
            var prior=paintLayer.getSelectedValue();
            updatingPaintLayers=true;paintLayers.clear();
            var previous=shownPaintMaterial==null?List.<EditorMeshMaterial.LayerInfo>of():shownPaintMaterial.layers();
            var current=material==null?List.<EditorMeshMaterial.LayerInfo>of():material.layers();
            EditorMeshMaterial.LayerInfo active=null;
            boolean adding=modelingPreview!=null && !previous.isEmpty() && current.size()==previous.size()+1;
            for(int i=current.size()-1;i>=0;i--){
                var l=current.get(i);
                paintLayers.addElement(l);
                if(adding && previous.stream().noneMatch(old->old.id().equals(l.id())))active=l;
                else if(active==null && prior!=null && prior.id().equals(l.id()))active=l;
            }
            if(active!=null)paintLayer.setSelectedValue(active,true);
            else if(!paintLayers.isEmpty())paintLayer.setSelectedIndex(0);
            updatingPaintLayers=false;shownPaintMaterial=material;showLayerSettings();
        }
        boolean layerEditable=!busy && modelingPreview==null && face!=null && materialWorkspace.visibility().includes(face) &&
            bound;
        previewPaintStyle.setEnabled(layerEditable);paintBlend.setEnabled(layerEditable);paintOpacity.setEnabled(layerEditable);
        packMaterialUvs.setEnabled(layerEditable);packPadding.setEnabled(layerEditable);keepPackDensity.setEnabled(layerEditable);
        pinScope.setEnabled(layerEditable);pinUvs.setEnabled(layerEditable);unpinUvs.setEnabled(layerEditable);
        seamEdge.setEnabled(layerEditable);markSeam.setEnabled(layerEditable);clearSeam.setEnabled(layerEditable);
        unwrapIsland.setEnabled(layerEditable);
        paintLayer.setEnabled(layerEditable);paintLayerName.setEnabled(layerEditable);
        addPaintLayer.setEnabled(layerEditable && paintLayers.size()<16);removePaintLayer.setEnabled(layerEditable && paintLayers.size()>1);
        raisePaintLayer.setEnabled(layerEditable && paintLayer.getSelectedIndex()>0);
        lowerPaintLayer.setEnabled(layerEditable && paintLayer.getSelectedIndex()+1<paintLayers.size());
        double rotation=((Number)materialRotation.getValue()).doubleValue();
        var visibility=materialWorkspace.visibility();
        fixUvStretch.setEnabled(false);
        boolean island=uvScope.getSelectedIndex()==1;
        relaxUvInterior.setVisible(island);relaxUvInterior.setEnabled(island && layerEditable);
        fixUvStretch.setText(island?"Balance island stretch":"Fix selected face stretch");
        fixUvStretch.setToolTipText(island?"Balance directional stretch across the connected UV island while preserving signed areas":
            "Preview uniform texel scale on this face while preserving its UV area and center");
        if(bound && visibility.includes(face) && (modelingPreview!=null ||
            workingObservation!=null && workingObservation.snapshot()==state)){
            var nativeMesh=modelingPreview!=null?modelingMesh:workingObservation.mesh();
            var density=EditorUvDensity.measure(nativeMesh.triangles().get(faceIndex),material.width(),material.height());
            fixUvStretch.setEnabled(layerEditable && density.filter(d->d.texelsPerUnit()>0 && (island || d.stretch()>1+1e-10)).isPresent());
            uvDensity.setText(density.map(d->d.texelsPerUnit()==0?"Collapsed UV triangle":
                String.format(Locale.ROOT,"%.2f texels/unit · %.2f× stretch",d.texelsPerUnit(),d.stretch())).orElse("Degenerate model triangle"));
        }else uvDensity.setText("Select a bound face to measure UVs");
        int mode=materialReviewMode.getSelectedIndex();
        int checker=mode<CHECKER_SIZES.length?CHECKER_SIZES[mode]:0;
        int mip=Math.max(0,mode-CHECKER_SIZES.length+1);
        if(materialWorkspace.visibleFaces()==0){
            materialRenderState=null;projectedFrame=null;projectedStroke=null;materialRuntime.setIcon(null);materialRuntime.setText("No visible faces");return;
        }
        int width=materialWorkspace.modelView()?Math.max(160,Math.min(840,materialRuntime.getWidth())):280;
        int height=materialWorkspace.modelView()?Math.max(120,Math.min(600,materialRuntime.getHeight())):190;
        if(materialRendering||state==materialRenderState&&rotation==materialRenderedRotation&&mode==renderedMode&&visibility.equals(materialRenderedVisibility)&&width==reviewWidth&&height==reviewHeight)return;
        reviewWidth=width;reviewHeight=height;
        materialRenderState=state;materialRenderedRotation=rotation;materialRenderedVisibility=visibility;renderedMode=mode;materialRendering=true;projectedFrame=null;projectedStroke=null;materialRuntime.setIcon(null);materialRuntime.setText("Rendering captured native state...");
        var captured=state;var mesh=modelingPreview!=null?modelingMesh:
            workingObservation!=null && workingObservation.snapshot()==state ? workingObservation.mesh() : topologyProject.mesh();
        var mipCache=reviewMips;
        new SwingWorker<MaterialReviewFrame,Void>(){
            protected MaterialReviewFrame doInBackground()throws Exception{var renderer=new SceneRenderer();renderer.setRenderSize(width,height);return GlbProps.materialReviewFrame(mipCache.encode(mesh),renderer,Math.toRadians(rotation),mesh,visibility.filtered()?visibility::includes:null,checker,mip,mipCache);}
            protected void done(){materialRendering=false;var current=modelingPreview!=null?modelingPreview.snapshot():topologyProject==null?null:topologyProject.snapshot();
                if(current==captured&&mode==materialReviewMode.getSelectedIndex()&&visibility.equals(materialWorkspace.visibility())&&rotation==((Number)materialRotation.getValue()).doubleValue()){try{projectedFrame=get();materialRuntime.setIcon(new ImageIcon(projectedFrame.image));materialRuntime.setText("");}catch(Exception failure){projectedFrame=null;materialRuntime.setIcon(null);materialRuntime.setText("Preview unavailable: "+rootMessage(failure));}}
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
        beginMaterialPreview(p->{try{return p.previewBindMaterial(face,EditorMeshMaterial.fromPng(png.getFileName().toString(),EditorMeshPipeline.readSource(png,4_000_000),wrap,wrap,alpha,origin));}catch(IOException bad){throw new java.io.UncheckedIOException(bad);}});
    }
    private void refreshMeshControls(){
        if(restoringWorkspace)return;
        if (edgeObservation != null && !hasEdgeSelection()) clearEdgeSelection();
        var materialState=topologyProject==null?null:topologyProject.snapshot();
        Long faceId=selectedHit==null?null:selectedHit.faceId();
        if(listedMaterials!=materialState || !java.util.Objects.equals(faceId,listedMaterialFace)){
            var previous=(EditorMeshMaterial)materialList.getSelectedItem();
            String key=previous==null?"":previous.key();
            if(materialState!=null && faceId!=null)for(var face:materialState.faces())if(face.id()==faceId){
                if(!face.material().isEmpty())key=face.material();
                break;
            }
            updatingMaterialList=true;
            try{
                if(listedMaterials!=materialState){
                    materialList.removeAllItems();
                    if(materialState!=null)for(var m:materialState.materials())materialList.addItem(m);
                }
                for(int i=0;i<materialList.getItemCount();i++)if(materialList.getItemAt(i).key().equals(key)){
                    materialList.setSelectedIndex(i);break;
                }
                listedMaterials=materialState;listedMaterialFace=faceId;
            }finally{updatingMaterialList=false;}
            refreshMaterialSwatch();
        }

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
        edgeMode.setEnabled(editable);
        if(snapPick!=null && (topologyProject==null || !topologyProject.isCurrent(snapPick.observation()))) stopSnap();
        snapToVertex.setEnabled(editable && !selectedIds().isEmpty());
        selectEdges.setEnabled(editable && selectionMode.getSelectedIndex()==1 && selectedHit!=null);
        boolean loopSlide=hasEdgeSelection() && edgeMode.getSelectedItem()==EditorMeshEdges.Mode.LOOP;
        previewSlide.setText(loopSlide?"PREVIEW LOOP SLIDE":"PREVIEW VERTEX SLIDE");
        reverseSlide.setText(loopSlide?"Use opposite quad side":"Reverse edge direction");
        previewSlide.setEnabled(editable && (!hasEdgeSelection() || loopSlide) && selectionMode.getSelectedIndex()==1 && selectedHit!=null);
        previewBevel.setEnabled(editable&&!hasEdgeSelection()&&selectionMode.getSelectedIndex()!=2&&selectedHit!=null);
        boolean visibleMaterialFace=selectedHit!=null && (materialWorkspace==null || !materialDialog.isVisible() || materialWorkspace.visibility().includes(selectedHit.faceId()));
        chooseMaterial.setEnabled(editable&&selectionMode.getSelectedIndex()==2&&visibleMaterialFace);previewMaterial.setEnabled(chooseMaterial.isEnabled()&&materialList.getSelectedItem()!=null);
        for(var c:List.of(materialList,materialWrap,materialAlpha,materialOrigin,uvOperation,uvScope,uvPivot,uvFirst,uvSecond))c.setEnabled(editable);
        previewUv.setEnabled(editable&&selectionMode.getSelectedIndex()==2&&visibleMaterialFace);
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
            &&(!gltfModel(source.name())||result.level()==0));
    }
    private boolean comparisonVisible(){return compareView.isSelected()||modelingMesh!=null||primitivePreview!=null||(result!=null&&result.level()>0);}
    int workingViewportWidth(){return comparisonVisible()?preview.getWidth()/2:preview.getWidth();}
    private void clearModelingPreview(){if(restoringWorkspace)return;stopSnap();snapGuide=null;if(modelingPreview!=null&&topologyProject!=null)topologyProject.cancelPreview(modelingPreview);modelingGeneration++;modelingPreparing=false;modelingPreview=null;modelingMesh=null;modelingBounds=null;refreshMeshControls();preview.repaint();}
    private record ModelingReview(EditorTopologyProject.EditPreview edit,EditorMeshPipeline.Mesh mesh,List<EditorMeshPipeline.Triangle> bounds){}
    private void previewModelingEdit(){
        if(busy||topologyProject==null||selectedHit==null||selectionMode.getSelectedIndex()!=2)return;
        long face=selectedHit.faceId();var operation=(EditorTopologyProject.FaceEdit)faceOperation.getSelectedItem();double amount=((Number)faceAmount.getValue()).doubleValue();
        beginModelingPreview(p->p.previewFaceEdit(face,operation,amount));
    }
    private void previewPin(boolean pin){
        if(selectedHit==null)return;
        long face=selectedHit.faceId();int index=pinScope.getSelectedIndex();
        beginMaterialPreview(p->p.previewUvPins(face,index==3?-1:index,pin));
    }
    private void previewSeam(boolean mark){
        if(selectedHit==null)return;
        long id=selectedHit.faceId();int edge=seamEdge.getSelectedIndex();
        beginMaterialPreview(p->p.previewSeam(id,edge,mark));
    }
    private void beginMaterialPreview(java.util.function.Function<EditorTopologyProject,EditorTopologyProject.EditPreview> operation){
        if(materialWorkspace!=null)materialWorkspace.previewIssue(null);
        if(selectedHit!=null && (materialWorkspace==null || !materialDialog.isVisible() || materialWorkspace.visibility().includes(selectedHit.faceId())))
            beginModelingPreview(operation,true);
    }
    private void beginModelingPreview(java.util.function.Function<EditorTopologyProject,EditorTopologyProject.EditPreview> operation){
        beginModelingPreview(operation,false);
    }
    private void beginModelingPreview(java.util.function.Function<EditorTopologyProject,EditorTopologyProject.EditPreview> editOperation,boolean keepMaterialFace){
        if(busy||topologyProject==null)return;
        if(!topologyProject.isCurrent(workingObservation)){workingObservation=topologyProject.observe(workingObservation);source=workingObservation.mesh();}
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
                    modelingPreview=review.edit();materialEditPreview=keepMaterialFace;modelingMesh=review.mesh();modelingBounds=review.bounds();selectionLabel.setText(review.edit().description());selectionLabel.setToolTipText(review.edit().description());details.append("\nNATIVE PREVIEW: "+review.edit().description()+". Apply to commit.");
                }catch(java.util.concurrent.CancellationException canceled){details.append("\nModel preview canceled.");}
                catch(Exception failure){String message="MODEL EDIT REFUSED: "+rootMessage(failure);details.append("\n"+message);selectionLabel.setText(message);selectionLabel.setToolTipText(message);
                    if(keepMaterialFace && materialWorkspace!=null)materialWorkspace.previewIssue("Preview refused · "+rootMessage(failure));}
                if(modelingPreview==null)snapGuide=null;
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
            project.save();stopMaterialObservation();clearPrimitivePreview();selectedHit=null;normalAnchor=null;selectionLabel.setText("Click a triangle in the working mesh");topologyProject=project;meshHistory=project.createHistory(60);workingObservation=project.observe();source=workingObservation.mesh();result=null;
            path=document;sourceCapture=null;gltfSource=null;paintRecovery=null;paintRecoveryStatus.setText("");reviewedSourceFingerprint=project.diskFingerprint();authoringNote="Native primitive authoring; generated fallback atlas.";sourceTextureBytes=0;
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
        var selected=selectedVertices;workingObservation=topologyProject.observe(workingObservation);source=workingObservation.mesh();selectVertex();
        var valid=new java.util.HashSet<Long>();for(var v:workingObservation.snapshot().vertices())if(selected.contains(v.id()))valid.add(v.id());
        if(!valid.isEmpty()&&(selectedVertex==null||!valid.contains(selectedVertex.id()))){vertexId.setValue(valid.stream().mapToLong(Long::longValue).min().orElseThrow());selectVertex();}
        selectedVertices=java.util.Set.copyOf(valid);
    }
    private boolean meshEdit(Runnable operation){
        if(topologyProject==null||busy)return false;
        try{meshHistory.execute(operation);meshChanged();return true;}
        catch(RuntimeException failure){details.append("\nEDIT REFUSED: "+rootMessage(failure));refreshMeshControls();return false;}
    }
    private void meshChanged(){
        clearModelingPreview();
        boolean previouslyPainted=workingObservation!=null && workingObservation.snapshot().materials().stream()
            .anyMatch(EditorMeshMaterial::hasLayerEdits);
        var selected=selectedVertices;workingObservation=topologyProject.observe(workingObservation);source=workingObservation.mesh();selectVertex();
        if(!selected.isEmpty()){var ids=new java.util.HashSet<Long>();for(var v:topologyProject.snapshot().vertices())if(selected.contains(v.id()))ids.add(v.id());selectedVertices=java.util.Set.copyOf(ids);}
        refreshMeshControls();
        checkpointPaint(previouslyPainted);
        if(slider.getValue()!=0)slider.setValue(0);else recalculate();
    }
    private void checkpointPaint(boolean previouslyPainted){
        if(paintRecovery==null || !paintRecovery.owns(topologyProject)){
            if(!previouslyPainted && workingObservation.snapshot().materials().stream().noneMatch(EditorMeshMaterial::hasLayerEdits))return;
            var project=topologyProject;
            paintRecovery=new EditorPaintRecovery(project,result->SwingUtilities.invokeLater(()->{
                if(project!=topologyProject || paintRecovery==null || !paintRecovery.file().equals(result.file()))return;
                paintRecoveryStatus.setText(result.error()==null?"Paint recovery saved · revision "+result.revision():
                    "Paint recovery failed · "+result.error());
                paintRecoveryStatus.setToolTipText(result.file().toString());
            }));
        }
        paintRecovery.request(workingObservation,captureWorkspace());
    }
    private void choosePaintRecovery(){
        if(busy)return;
        if(hasUnsavedMeshChanges() && JOptionPane.showConfirmDialog(dialog,"Leave current edits and open a recovered copy?",
            "Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        var chooser=new JFileChooser(EditorPaintRecovery.directory().toFile());
        chooser.setDialogTitle("Open a paint recovery copy");
        chooser.setFileHidingEnabled(false);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Paint recovery document or pending journal","btop","journal"));
        if(chooser.showOpenDialog(dialog)==JFileChooser.APPROVE_OPTION)loadPaintRecovery(chooser.getSelectedFile().toPath());
    }
    void loadPaintRecovery(Path selected){
        String name=selected.getFileName().toString();
        if(name.startsWith(".") && name.endsWith(".btop.editor.journal"))
            selected=selected.resolveSibling(name.substring(1,name.length()-".editor.journal".length()));
        loadSource(selected,true);
    }
    private static EditorMeshPipeline.Vertex glbCorner(GlbProps.Vertex v){
        var normal=v.nx()==0&&v.ny()==0&&v.nz()==0?null:new EditorMeshPipeline.Normal(v.nx(),v.ny(),v.nz());
        return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),v.u(),v.v(),normal);
    }
    private record Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String reviewedFingerprint,EditorObjMaterials.Capture capture,EditorGltfSource gltf){
        Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String fingerprint,EditorObjMaterials.Capture capture){this(mesh,project,note,fingerprint,capture,null);}
        Loaded(EditorMeshPipeline.Mesh mesh,EditorTopologyProject project,String note,String reviewedFingerprint){this(mesh,project,note,reviewedFingerprint,null);}
    }
    private void openSource(){
        if(hasUnsavedMeshChanges()&&JOptionPane.showConfirmDialog(dialog,"Discard unsaved mesh edits before opening another source?",
            "Unsaved mesh",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        JFileChooser file=new JFileChooser();
        file.setDialogTitle("Import a mesh source (OBJ, STL, glTF, GLB, BTOP)");
        file.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Mesh files (*.obj, *.obj.zip, *.stl, *.gltf, *.glb, *.btop)","obj","zip","stl","gltf","glb","btop"));
        if(file.showOpenDialog(dialog)!=JFileChooser.APPROVE_OPTION)return;
        loadSource(file.getSelectedFile().toPath());
    }
    /** Same native import path for the human chooser and source-backed verification. */
    void loadSource(Path candidate){
        boolean draft=EditorPaintRecovery.directory().equals(candidate.toAbsolutePath().normalize().getParent());
        if(draft)loadPaintRecovery(candidate);else loadSource(candidate,false);
    }
    private void loadSource(Path candidate,boolean recovering){
        stopMaterialObservation();
        var previousProject=topologyProject;
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
                if(recovering && !extension.endsWith(".btop"))throw new IOException("Choose a native paint recovery document");
                if(extension.endsWith(".btop")){
                    sourceTextureBytes=0;
                    var project=recovering?EditorPaintRecovery.openCopy(candidate,Path.of(System.getProperty("user.home"),
                        ".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop")):EditorTopologyProject.open(candidate);
                    return new Loaded(project.mesh(),project,recovering?"Opened a separate copy of the paint recovery document.":"",project.diskFingerprint());
                }
                if(gltfModel(extension)){
                    boolean json=extension.endsWith(".gltf");
                    byte[] original=EditorMeshPipeline.readSource(candidate,json?EditorGltfFile.MAX_BYTES:4_000_000);
                    String fingerprint=EditorMeshPipeline.sourceFingerprint(original);
                    var gltf=json?new EditorGltfSource(candidate,original):null;
                    byte[] bytes=json?gltf.glb():original;
                    String unsupported;
                    try{Path document=Path.of(System.getProperty("user.home"),".beckenburg","editor","topology",java.util.UUID.randomUUID()+".btop");var project=EditorTopologyProject.fromGlb(candidate.getFileName().toString(),bytes,document);return new Loaded(project.mesh(),project,"Original GLB hierarchy/pivots/material metadata retained; native edits own the selected node instance.",fingerprint,null,gltf);}catch(IOException notEditable){unsupported=notEditable.getMessage();}
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
                        "GLB retained read-only: "+unsupported,fingerprint,null,gltf);
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
                    var loaded=get();sourceCapture=loaded.capture();gltfSource=loaded.gltf();source=loaded.mesh();topologyProject=loaded.project();workingObservation=topologyProject==null?null:topologyProject.observe();if(workingObservation!=null)source=workingObservation.mesh();authoringNote=loaded.note();reviewedSourceFingerprint=loaded.reviewedFingerprint();
                    meshHistory=topologyProject==null?null:topologyProject.createHistory(60);
                    paintRecovery=null;paintRecoveryStatus.setText("");path=recovering?topologyProject.documentPath():candidate;vertexId.setValue(topologyProject==null||topologyProject.snapshot().vertices().isEmpty()?0L:topologyProject.snapshot().vertices().get(0).id());
                    selectVertex();refreshMeshControls();slider.setValue(0);
                    pathLabel.setText(candidate.getFileName()+" | "+source.triangles().size()+" source triangles");
                    if(topologyProject!=null && topologyProject.savedWorkspace()!=null)restoreWorkspace(topologyProject.savedWorkspace());else recalculate();
                }catch(Exception e){
                    if(recovering && previousProject!=null && previousProject==topologyProject){
                        refreshMeshControls();updatePublishAvailability();
                        pathLabel.setText(path==null?"Model Editor":path.getFileName().toString());
                        paintRecoveryStatus.setText("Recovery refused · "+rootMessage(e));
                        details.append("\nPAINT RECOVERY REFUSED: "+rootMessage(e));
                        return;
                    }
                    source=null;result=null;path=null;sourceCapture=null;gltfSource=null;topologyProject=null;workingObservation=null;meshHistory=null;refreshMeshControls();apply.setEnabled(false);
                    details.setText("IMPORT ERROR: "+rootMessage(e));
                    pathLabel.setText("Unable to read "+candidate.getFileName());
                    preview.repaint();
                }
            }
        }.execute();
    }
    private record Review(EditorMeshPipeline.Result reduction,EditorMeshTopology topology){}
    private void recalculate(){
        if(source==null||busy||restoringWorkspace)return;
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
                    if(gltfModel(mesh.name())&&level>0)
                        report.append("WARNING: GLB material and named-node identities are not carried into regenerated fallback textures.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".obj")&&mesh.materials().isEmpty())
                        report.append("NOTE: OBJ source MTL/texture links are not yet converted; uses a generated low-res fallback atlas.\n");
                    if(mesh.name().toLowerCase(Locale.ROOT).endsWith(".stl"))
                        report.append("NOTE: STL contains no UV/material atlas; uses a generated low-res fallback atlas.\n");
                    boolean texturedGlb=gltfModel(mesh.name());
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
        if(gltfModel(source.name())&&result.level()>0){
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
        final EditorGltfSource reviewedGltf=gltfSource;
        final String reviewed=reviewedSourceFingerprint;final EditorTopologyProject authoringProject=topologyProject;
        final EditorTopologyProject.Observation authoringObservation=workingObservation;
        final EditorModelWorkspace authoringView=authoringProject==null?null:captureWorkspace();
        final var savedAuthoring=new java.util.concurrent.atomic.AtomicBoolean();
        final var savedSourceFingerprint=new java.util.concurrent.atomic.AtomicReference<String>();
        choose.setEnabled(false);apply.setEnabled(false);busy=true;refreshMeshControls();
        details.append("\nPublishing validated GLB derivative...");
        new SwingWorker<String,Void>(){
            @Override protected String doInBackground()throws Exception{
                byte[] sourceBytes=EditorMeshPipeline.readSource(original,(int)EditorMeshPipeline.MAX_SOURCE_BYTES);
                if(!EditorMeshPipeline.sourceFingerprint(sourceBytes).equals(reviewed))throw new IOException("Model source changed after review; reopen it before publishing");
                if(capturedDependencies!=null)capturedDependencies.validateDependencies();
                byte[] sourceArchive=reviewedGltf==null?null:reviewedGltf.reviewedArchive(sourceBytes);
                byte[] nativeBytes=null;String expectedSource=reviewed;
                if(authoringProject!=null){
                    var saved=authoringProject.saveReviewed(authoringObservation,authoringView);savedAuthoring.set(true);nativeBytes=saved.bytes();
                    if(original.toAbsolutePath().normalize().equals(authoringProject.documentPath())){
                        expectedSource=saved.diskFingerprint();savedSourceFingerprint.set(expectedSource);
                    }
                }
                byte[] output;
                String filename=original.getFileName().toString().toLowerCase(Locale.ROOT);
                if (authoringProject==null && chosen.level()==0 && gltfModel(filename)) {
                    output=reviewedGltf==null ? sourceBytes : reviewedGltf.glb();
                } else {
                    output=EditorGlbWriter.encode(chosen.optimized());
                }
                // Use the exact decoder that live gameplay will use. No mock preview-only mesh.
                if(authoringProject!=null)EditorGlbWriter.validate(chosen.optimized(),output);
                GlbProps.decode(output);
                int after=chosen.parts().stream().mapToInt(EditorMeshPipeline.PartStats::after).sum();
                return ImportedAssetLibrary.publishReviewed(original,output,chosen.level(),
                    chosen.source().triangles().size(),after,String.join(" | ",chosen.warnings()),expectedSource,nativeBytes,sourceArchive);
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
            var fitted = EditorMeshPicking.cameraForTriangles(reference, cameraYaw, cameraPitch);
            if (!fitted.sameProjection(cachedCamera)) cachedCamera = fitted;
            cameraReference = reference; cachedYaw = cameraYaw; cachedPitch = cameraPitch;
        }return cachedCamera;
    }
    private List<EditorMeshPipeline.Triangle> workingReference(){return modelingBounds==null?source.triangles():modelingBounds;}
    private record Viewport(double centerX,double centerY,double scale,int left,int width,int height) implements ModelWireframeBuffer.View {
        public int x(double value){return left+width/2+(int)Math.round((value-centerX)*scale);}
        public int y(double value){return height/2+(int)Math.round((value-centerY)*scale);}
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
        preparedPicking=preparedPicking==null ? EditorMeshPicking.prepare(state,camera) : preparedPicking.update(state,camera);
        return preparedPicking.pick(x,y).orElse(null);
    }
    private void clearHover(){if(hoverHit!=null){hoverHit=null;preview.repaint();}}
    private void hoverSnap(int x,int y) {
        var vertex=snapVertexAt(x,y);
        snapHover=vertex!=null && snapPick!=null && (snapPick.anchor()==null)==snapPick.ids().contains(vertex.id()) ? vertex : null;
        preview.repaint();
    }
    private void stopSnap() {
        if (snapPick!=null) selectionLabel.setText(snapPick.ids().size()==1?"Vertex "+snapPick.ids().iterator().next():snapPick.ids().size()+" vertices selected");
        snapPick=null;snapHover=null;snapToVertex.setSelected(false);
    }
    private EditorTopologyProject.Vertex snapVertexAt(int x,int y) {
        var pick=snapPick;
        if (pick==null || busy || source==null || x<0 || x>=workingViewportWidth() || y<0 || y>=preview.getHeight()) return null;
        if (topologyProject==null || !topologyProject.isCurrent(pick.observation())) {stopSnap();selectionLabel.setText("Select the anchor again after the model or selection changed");return null;}
        var view=viewport(workingReference(),0,workingViewportWidth());var camera=projector(workingReference());
        var hit=nativePick(pick.observation().snapshot(),camera,(x-view.width()/2)/view.scale()+view.centerX(),(y-view.height()/2)/view.scale()+view.centerY());
        if(hit==null)return null;
        var vertex=pick.observation().vertex(hit.vertexId());var projected=camera.project(vertex.x(),vertex.y(),vertex.z());
        return Math.hypot(x-view.x(projected.x()),y-view.y(projected.y()))<=12?vertex:null;
    }
    private void pickSnap(int x,int y) {
        var pick=snapPick;
        if (!pick.ids().equals(selectedIds())) {stopSnap();selectionLabel.setText("Select the anchor again after the selection changed");return;}
        var vertex=snapVertexAt(x,y);if(vertex==null)return;
        if(pick.anchor()==null) {
            if(!pick.ids().contains(vertex.id())) {selectionLabel.setText("Choose an anchor inside the current selection");return;}
            snapPick=new SnapPick(pick.observation(),pick.ids(),vertex);snapHover=null;
            selectionLabel.setText("Pick a target for v"+vertex.id());preview.repaint();return;
        }
        if(pick.ids().contains(vertex.id())) {selectionLabel.setText("Choose a target outside the moving selection");return;}
        beginModelingPreview(p->p.previewVertexSnap(pick.observation(),pick.ids(),pick.anchor().id(),vertex.id()));
        snapGuide=new SnapGuide(pick.observation(),pick.anchor(),vertex);preview.repaint();
    }
    private void drawSnapGuide(Graphics2D g,EditorMeshPicking.Projector camera,Viewport view) {
        if(snapPick!=null && !topologyProject.isCurrent(snapPick.observation()))return;
        var anchor=snapPick==null?null:snapPick.anchor();var target=snapHover;
        if(snapGuide!=null && topologyProject.isCurrent(snapGuide.observation())) {anchor=snapGuide.anchor();target=snapGuide.target();}
        if(anchor==null && target==null)return;
        g.setColor(new Color(255,150,65));g.setStroke(new BasicStroke(2));
        int ax=0,ay=0;
        if(anchor!=null){var a=camera.project(anchor.x(),anchor.y(),anchor.z());ax=view.x(a.x());ay=view.y(a.y());g.drawOval(ax-5,ay-5,10,10);}
        if(target!=null){var t=camera.project(target.x(),target.y(),target.z());int tx=view.x(t.x()),ty=view.y(t.y());
            if(anchor!=null)g.drawLine(ax,ay,tx,ty);g.drawOval(tx-7,ty-7,14,14);g.drawString("v"+target.id(),tx+9,ty-6);}
        g.setStroke(new BasicStroke(1));
    }
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
        clearEdgeSelection();
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
    private void clearEdgeSelection() {
        edgeObservation = null;
        edgeSegments = List.of();
        edgeIds = java.util.Set.of();
    }
    private boolean hasEdgeSelection() {
        return edgeObservation != null && topologyProject != null && topologyProject.isCurrent(edgeObservation);
    }
    private void selectEdgeGroup() {
        if (busy || selectedHit == null || selectionMode.getSelectedIndex()!=1) return;
        var project = topologyProject;
        var observation = project.observe();
        var seed = selectedHit;
        var mode = (EditorMeshEdges.Mode)edgeMode.getSelectedItem();
        busy = true;
        choose.setEnabled(false);
        apply.setEnabled(false);
        refreshMeshControls();
        new SwingWorker<List<EditorMeshEdges.Segment>,Void>() {
            protected List<EditorMeshEdges.Segment> doInBackground() {
                return EditorMeshEdges.select(observation,mode,seed.edgeA(),seed.edgeB());
            }
            protected void done() {
                try {
                    var segments = get();
                    if (topologyProject == project && project.isCurrent(observation) && seed == selectedHit && edgeMode.getSelectedItem()==mode && selectionMode.getSelectedIndex()==1 && dialog.isDisplayable()) {
                        selectedVertex = null;
                        selectedVertices = java.util.Set.of();
                        edgeSegments = segments;
                        var endpoints = new java.util.HashSet<Long>();
                        for (var segment : segments) {endpoints.add(segment.a().id());endpoints.add(segment.b().id());}
                        edgeIds = java.util.Set.copyOf(endpoints);
                        edgeObservation = observation;
                        selectionLabel.setText(mode + " / " + segments.size() + " edges");
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
        if(selectionMode.getSelectedIndex()==1 && hasEdgeSelection())return edgeIds;
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
            private final EditorMeshPicking.Prepared prior=preparedPicking;
            private EditorMeshPicking.Prepared query;
            protected java.util.Set<Long> doInBackground(){
                query=prior==null ? EditorMeshPicking.prepare(state,selectedProjection) : prior.update(state,selectedProjection);
                return lasso?query.verticesInLasso(polygon,xray):query.verticesInBox(minX,minY,maxX,maxY,xray);
            }
            protected void done(){
                busy=false;choose.setEnabled(true);
                try{
                    var ids=get();if(request!=selectionGeneration)throw new java.util.concurrent.CancellationException("Selection canceled");
                    if(topologyProject!=project||project.revision()!=nativeRevision||project.snapshot()!=state)throw new IOException("Mesh changed during selection");
                    preparedPicking=query;
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
        g.setColor(new Color(113,168,188,120));
        var wireframe = left == 0 ? workingWireframe : previewWireframe;
        wireframe.draw(g, tris, camera, view);
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
        if (title.equals("WORKING MESH") && hasEdgeSelection() && selectionMode.getSelectedIndex()==1) {
            g.setColor(new Color(255,218,87));g.setStroke(new BasicStroke(3));
            for (var segment : edgeSegments) {
                var first = segment.a(); var second = segment.b();
                var a = camera.project(first.x(),first.y(),first.z());
                var b = camera.project(second.x(),second.y(),second.z());
                g.drawLine(view.x(a.x()),view.y(a.y()),view.x(b.x()),view.y(b.y()));
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
            drawSnapGuide(g,camera,view);
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
