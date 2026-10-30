package org.beckenburg;

import javax.swing.*;
import org.beckenburg.editor.EditorTool;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.EditorPalettePanel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Actual Java 17, game-rendered 3D editor, not an in-game placeholder.
 * The selected object inspector writes live transforms into CastleLevelEdits.
 */
public final class DeveloperLevelEditor extends JPanel {
    private final CastleLevelEdits data=CastleLevelEdits.current();
    private EditorTool tool=EditorTool.SELECT;
    private final java.util.EnumMap<EditorTool,JToggleButton> palette=new java.util.EnumMap<>(EditorTool.class);
    private final CastleEditorProject project=new CastleEditorProject(data);
    private EditorPalettePanel assetPalette;
    private final EditorHistory<CastleLevelEdits.Snapshot> history=new EditorHistory<>(project,60);
    private final JSpinner workPlane=new JSpinner(new SpinnerNumberModel(0.,-.5,12.,.5));
    private final JSpinner surfaceHeight=new JSpinner(new SpinnerNumberModel(3.4,.1,12.,.1));
    private final JComboBox<String> gridSnap=new JComboBox<>(new String[]{"Off","0.25 m","0.5 m","1 m","2 m"});
    private Point dragOrigin;
    private Point dragEnd;
    private record Point(double x,double z){}
    private record Row(String kind,String id,String description){
        @Override public String toString(){return description;}
    }
    private final CastleHubMap map=new CastleHubMap();
    private final SceneRenderer renderer=new SceneRenderer();
    private final JPanel viewport=new JPanel(){
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            GraphicsOptions.current().draw((Graphics2D)g,renderer.image(),getWidth(),getHeight());
            if(lightVolumeOverlay.isSelected())
                LightVolumePreview.draw((Graphics2D)g,renderer,lightVolume,getWidth(),getHeight());
            g.setColor(new Color(244,226,177));g.drawLine(getWidth()/2-5,getHeight()/2,getWidth()/2+5,getHeight()/2);
            g.drawLine(getWidth()/2,getHeight()/2-5,getWidth()/2,getHeight()/2+5);
        }
    };
    private final DefaultListModel<Row> rows=new DefaultListModel<>();
    private final JList<Row> hierarchy=new JList<>(rows);
    private final JTextField filter=new JTextField(15);
    private final JComboBox<String> library=new JComboBox<>();
    private final JSpinner x=new JSpinner(new SpinnerNumberModel(13.5,-1000.,1000.,.1));
    private final JSpinner y=new JSpinner(new SpinnerNumberModel(0.,-10.,100.,.1));
    private final JSpinner z=new JSpinner(new SpinnerNumberModel(18.,-1000.,1000.,.1));
    private final JSpinner yaw=new JSpinner(new SpinnerNumberModel(0.,-360.,360.,5.));
    private final JSpinner scale=new JSpinner(new SpinnerNumberModel(1.,.01,10.,.05));
    private final JSpinner width=new JSpinner(new SpinnerNumberModel(.4,0.,44.,.05));
    private final JSpinner depth=new JSpinner(new SpinnerNumberModel(.4,0.,44.,.05));
    private final JSpinner radius=new JSpinner(new SpinnerNumberModel(9.,1.,24.,.5));
    private final JSpinner power=new JSpinner(new SpinnerNumberModel(2.5,0.,10.,.1));
    private final JComboBox<String> material=new JComboBox<>(new String[]{
        "STONE","PLASTER","PARQUET","CERAMIC","DOOR","CEILING","CARPET","BRICK",
        "WALLPAPER","CONCRETE","SOIL","IRON","FOLIAGE","AGED_WOOD","WINDOW","RITUAL"
    });
    private final JSpinner tileX=new JSpinner(new SpinnerNumberModel(13,0,CastleHubMap.WIDTH-1,1));
    private final JSpinner tileZ=new JSpinner(new SpinnerNumberModel(19,0,CastleHubMap.HEIGHT-1,1));
    private final JSpinner wallIndex=new JSpinner(new SpinnerNumberModel(0,0,CastleHubArchitecturalPlan.walls().size()-1,1));
    private final JCheckBox navOverlay=new JCheckBox("3D navigation overlay",false);
    private final JCheckBox lightVolumeOverlay=new JCheckBox("LIGHT REACH",false);
    private final java.util.concurrent.ExecutorService lightPreviewWorker=
        java.util.concurrent.Executors.newSingleThreadExecutor(task->{
            Thread thread=new Thread(task,"Beckenburg-Editor-Light-Gizmo");
            thread.setDaemon(true);return thread;
        });
    private volatile LightVolumePreview.Volume lightVolume;
    private String lightVolumeKey="";
    private volatile boolean editorClosed;
    private final JCheckBox navBlocked=new JCheckBox("Block player navigation here");
    private final JLabel status=new JLabel("WASD fly  •  Q/E altitude  •  Mouse drag look  •  Wheel forward/back  •  Ctrl+S save");
    private final JTextArea navInfo=new JTextArea(6,25);
    private final Set<Integer> keys=new HashSet<>();
    private final Timer timer;
    private Row chosen;
    private boolean modifying;
    private int lastMouseX,lastMouseY;
    private boolean mouseDrag;
    private boolean drawing;
    private int downX,downY;
    private double clock;
    private final JFrame window;
    /** Four per-session editor camera bookmarks; each preserves orientation and elevation. */
    private final double[][] cameraBookmarks=new double[4][];
    private void bookmarkCamera(int index,boolean store){
        if(store){
            cameraBookmarks[index]=new double[]{renderer.x,renderer.eyeHeight,renderer.z,renderer.yaw,renderer.pitch};
            status.setText("Saved camera bookmark "+(index+1));
        }else if(cameraBookmarks[index]!=null){
            double[] c=cameraBookmarks[index];
            renderer.x=c[0];renderer.eyeHeight=c[1];renderer.z=c[2];
            renderer.yaw=c[3];renderer.pitch=c[4];
            status.setText("Camera bookmark "+(index+1));
        }else status.setText("Camera bookmark "+(index+1)+" is empty — use Ctrl+F"+(index+1)+" to store");
        viewport.requestFocusInWindow();
    }

    public static void open(Window parent){
        var frame=new JFrame("Beckenburg — Developer Level Editor");
        var editor=new DeveloperLevelEditor(frame);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(editor);
        frame.setSize(1300,790);frame.setMinimumSize(new Dimension(900,570));
        frame.setLocationRelativeTo(parent);
        frame.addWindowListener(new WindowAdapter(){
            @Override public void windowClosed(WindowEvent e){
                editor.editorClosed=true;
                editor.timer.stop();
                editor.lightPreviewWorker.shutdownNow();
            }
        });
        frame.setVisible(true);
        SwingUtilities.invokeLater(editor.viewport::requestFocusInWindow);
    }
    private DeveloperLevelEditor(JFrame window){
        super(new BorderLayout(6,5));this.window=window;
        setBackground(new Color(18,22,26));setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        renderer.setRenderSize(560,350);renderer.lamp=false;
        double[] spawn=data.spawn();
        renderer.x=spawn[0];renderer.z=spawn[1];
        renderer.yaw=spawn[2];renderer.pitch=-.12;
        viewport.setBackground(Color.BLACK);viewport.setFocusable(true);
        viewport.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                lastMouseX=e.getX();lastMouseY=e.getY();
                downX=e.getX();downY=e.getY();
                mouseDrag=SwingUtilities.isRightMouseButton(e)||e.isShiftDown();
                drawing=!mouseDrag&&SwingUtilities.isLeftMouseButton(e);
                dragOrigin=drawing?worldOnPlane(e.getX(),e.getY()):null;
                dragEnd=dragOrigin;
                viewport.requestFocusInWindow();
            }
            @Override public void mouseReleased(MouseEvent e){
                if(drawing){
                    Point target=worldOnPlane(e.getX(),e.getY());
                    editViewport(downX,downY,dragOrigin,target);
                }
                drawing=false;mouseDrag=false;dragOrigin=null;dragEnd=null;
                viewport.repaint();
            }
        });
        viewport.addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e){
                if(drawing){dragEnd=worldOnPlane(e.getX(),e.getY());return;}
                if(!mouseDrag)return;
                renderer.yaw-=(e.getX()-lastMouseX)*.006;
                renderer.pitch=Math.max(-1.35,Math.min(1.35,
                    renderer.pitch+(e.getY()-lastMouseY)*.004));
                lastMouseX=e.getX();lastMouseY=e.getY();
            }
        });
        viewport.addMouseWheelListener(e->advance(-e.getPreciseWheelRotation()*.85));
        viewport.addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent e){
                if(e.getKeyCode()>=KeyEvent.VK_F1&&e.getKeyCode()<=KeyEvent.VK_F4){
                    bookmarkCamera(e.getKeyCode()-KeyEvent.VK_F1,e.isControlDown());return;
                }
                if(e.isControlDown()){
                    if(e.getKeyCode()==KeyEvent.VK_S){save();return;}
                    if(e.getKeyCode()==KeyEvent.VK_Z){undo();return;}
                    if(e.getKeyCode()==KeyEvent.VK_Y){redo();return;}
                    if(e.getKeyCode()==KeyEvent.VK_D){duplicate();return;}
                }
                if(e.getKeyCode()==KeyEvent.VK_DELETE){remove();return;}
                if(e.getKeyCode()==KeyEvent.VK_G){chooseTool(EditorTool.MOVE);return;}
                if(e.getKeyCode()==KeyEvent.VK_R){chooseTool(EditorTool.ROTATE);return;}
                if(e.getKeyCode()==KeyEvent.VK_T){chooseTool(EditorTool.PAINT);return;}
                keys.add(e.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}
        });
        viewport.addFocusListener(new FocusAdapter(){
            @Override public void focusLost(FocusEvent e){keys.clear();}
        });

        var header=new JPanel(new BorderLayout(6,0));
        header.add(new JLabel("DEVELOPER LEVEL EDITOR   /   CASTLE BECKENBURG  /  LIVE SCENE"),BorderLayout.WEST);
        JPanel toolStrip=new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));
        ButtonGroup tools=new ButtonGroup();
        for(EditorTool option:EditorTool.values()){
            JToggleButton button=new JToggleButton(option.name());
            button.setMargin(new Insets(3,6,3,6));button.setFocusable(false);
            button.setToolTipText(switch(option){
                case SELECT -> "Pick geometry or objects";
                case MOVE -> "Click object, then drag it to its new location";
                case ROTATE -> "Rotate selected object 90 degrees";
                case SCALE -> "Drag to change width and depth";
                case VERTEX -> "Drag a selected surface corner or wall endpoint";
                case EDGE -> "Drag the nearest floor/roof edge or translate a wall sideways";
                case WALL,FLOOR,ROOF -> "Drag in viewport on editing plane to create geometry";
                case PROP -> "Place selected asset";
                case LIGHT -> "Place a real light emitter";
                case PAINT -> "Paint selected or clicked surface with palette material";
                case DELETE -> "Remove clicked object or surface";
            });
            button.addActionListener(e->chooseTool(option));
            palette.put(option,button);tools.add(button);toolStrip.add(button);
        }
        palette.get(EditorTool.SELECT).setSelected(true);
        toolStrip.add(new JLabel("Plane Y"));toolStrip.add(workPlane);
        gridSnap.setSelectedIndex(2);
        gridSnap.setToolTipText("World-space placement grid; Off preserves centimetre precision");
        toolStrip.add(new JLabel("Grid"));toolStrip.add(gridSnap);
        JButton overhead=button("TOP");
        overhead.addActionListener(e->{
            renderer.x=21;renderer.z=18;renderer.eyeHeight=11;
            renderer.yaw=0;renderer.pitch=-1.48;viewport.requestFocusInWindow();
        });
        JButton ground=button("GROUND");
        ground.addActionListener(e->{
            double[] start=data.spawn();
            renderer.x=start[0];renderer.z=start[1];renderer.eyeHeight=1.62;
            renderer.yaw=start[2];renderer.pitch=-.12;
            workPlane.setValue(0.);viewport.requestFocusInWindow();
        });
        JButton upper=button("UPPER");
        upper.addActionListener(e->{
            renderer.x=17.5;renderer.z=15;renderer.eyeHeight=5.02;
            renderer.pitch=-.25;workPlane.setValue(3.4);
            viewport.requestFocusInWindow();
        });
        toolStrip.add(overhead);toolStrip.add(ground);toolStrip.add(upper);
        for(int i=0;i<cameraBookmarks.length;i++){
            final int slot=i;
            JButton view=button("VIEW "+(slot+1));
            view.setToolTipText("Click to restore view; Ctrl+click to store (Ctrl+F"+(slot+1)+" / F"+(slot+1)+")");
            view.addActionListener(e->bookmarkCamera(slot,(e.getModifiers()&ActionEvent.CTRL_MASK)!=0));
            toolStrip.add(view);
        }
        lightVolumeOverlay.setToolTipText("Draw selected fixture's real radius and occlusion-clipped rays; green=open, orange=blocked");
        lightVolumeOverlay.setFocusable(false);
        lightVolumeOverlay.addActionListener(e->{
            lightVolumeKey="";lightVolume=null;viewport.repaint();
        });
        toolStrip.add(lightVolumeOverlay);
        header.add(toolStrip,BorderLayout.SOUTH);
        var commands=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));
        JButton save=button("SAVE"),export=button("EXPORT"),reset=button("RESET");
        JButton undoButton=button("UNDO"),redoButton=button("REDO");
        undoButton.addActionListener(e->undo());redoButton.addActionListener(e->redo());
        commands.add(undoButton);commands.add(redoButton);
        JButton authoringGuide=button("PSX GUIDE");
        authoringGuide.setToolTipText("Offline editor engineering reference and machine manifest");
        authoringGuide.addActionListener(e->org.beckenburg.editor.EditorAuthoringReference.show(window));
        commands.add(authoringGuide);
        JButton sceneAudit=button("SCENE AUDIT");
        sceneAudit.setToolTipText("Check this live castle scene for intersecting objects and duplicate IDs; non-blocking warnings");
        sceneAudit.addActionListener(e->showSceneAudit());
        commands.add(sceneAudit);
        JButton importModel=button("IMPORT MODEL");
        importModel.setToolTipText("OBJ, STL or GLB import with live reduction preview, size warnings and part-preserving rollback");
        importModel.addActionListener(e->openAssetImport());
        commands.add(importModel);
        JButton animationLab=button("ANIMATE");
        animationLab.setToolTipText("Edit rigid-part animation clips at 15/30 fps, with stepped playback and isolated root motion");
        animationLab.addActionListener(e->org.beckenburg.editor.EditorAnimationLab.show(window));
        commands.add(animationLab);
        JButton retroSfx=button("SFX ASSETS");
        retroSfx.setToolTipText("Inspect PCM WAV memory and export separate 11/22-kHz PSX-style mono WAVs; game mixer unchanged");
        retroSfx.addActionListener(e->org.beckenburg.editor.EditorSoundLab.show(window));
        commands.add(retroSfx);
        JButton campaignLab=button("CAMPAIGN");
        campaignLab.setToolTipText("Portable authoring state graph; safe idempotent transitions, flag/dependency inspection; NOT live game control");
        campaignLab.addActionListener(e->org.beckenburg.editor.EditorCampaignLab.show(window));
        commands.add(campaignLab);
        JButton billboardLab=button("BILLBOARDS");
        billboardLab.setToolTipText("Import PNG/JPEG, compare retro palette and aspect presets, estimate memory, then bake separate image; 3D binding pending");
        billboardLab.addActionListener(e->org.beckenburg.editor.EditorBillboardLab.show(window));
        commands.add(billboardLab);
        JButton waterModules=new JButton("WATER",new dev.mrcalzon.water.WaterIcon());waterModules.setToolTipText("Reusable water modules, shapes, snap handles and weather preview");waterModules.addActionListener(e->EnvironmentModuleEditor.open(window,"castle"));commands.add(waterModules);
        save.addActionListener(e->save());
        export.addActionListener(e->export());
        reset.addActionListener(e->{
            if(JOptionPane.showConfirmDialog(window,"Reset all castle scene overrides?","Reset level",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION){
                data.reset();refresh();save();
            }
        });
        commands.add(save);commands.add(export);commands.add(reset);header.add(commands,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);

        JPanel editorSide=new JPanel(new BorderLayout(5,5));
        editorSide.setPreferredSize(new Dimension(355,400));
        JPanel search=new JPanel(new BorderLayout(4,2));
        search.add(filter,BorderLayout.CENTER);
        JLabel legend=new JLabel("Objects / fixtures / stations / spawn");
        search.add(legend,BorderLayout.NORTH);
        editorSide.add(search,BorderLayout.NORTH);
        hierarchy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hierarchy.addListSelectionListener(e->{if(!e.getValueIsAdjusting())select(hierarchy.getSelectedValue());});
        editorSide.add(new JScrollPane(hierarchy),BorderLayout.CENTER);
        filter.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){refresh();}
            public void removeUpdate(DocumentEvent e){refresh();}
            public void changedUpdate(DocumentEvent e){refresh();}
        });
        JTabbedPane tabs=new JTabbedPane();
        tabs.add("Objects & geometry",objectInspector());
        tabs.add("Paint & nav",surfaceInspector());
        assetPalette=new EditorPalettePanel(project,asset->{
            if(asset.category().startsWith("Models / ")||asset.category().equals("Models")){
                library.setSelectedItem(asset.uri().substring("assets://models/".length()));
                chooseTool(EditorTool.PROP);
            }else if(asset.category().equals("Modules")){EnvironmentModuleEditor.open(window,"castle");
            }else if(asset.category().equals("Materials")){
                material.setSelectedIndex(Integer.parseInt(
                    asset.uri().substring("assets://materials/".length())));
                chooseTool(EditorTool.PAINT);
            }
        });
        JPanel assetDock=new JPanel(new BorderLayout(3,3));
        JButton addImported=button("IMPORT MODEL...");
        addImported.addActionListener(e->openAssetImport());
        assetDock.add(addImported,BorderLayout.NORTH);
        assetDock.add(assetPalette,BorderLayout.CENTER);
        tabs.add("Assets",assetDock);
        editorSide.add(tabs,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,viewport,editorSide);
        split.setResizeWeight(1);split.setContinuousLayout(true);
        add(split,BorderLayout.CENTER);
        status.setForeground(new Color(181,218,199));
        add(status,BorderLayout.SOUTH);
        List<String> names=new ArrayList<>(GlbProps.availableModels());names.sort(String::compareTo);
        for(String name:names)library.addItem(name);
        library.setSelectedItem("sofa_1");
        navInfo.setEditable(false);navInfo.setFont(new Font(Font.MONOSPACED,Font.PLAIN,11));
        refresh();
        timer=new Timer(90,e->frame());timer.start();
    }
    /** Manual, non-blocking QA against the real saved/unsaved castle geometry. */
    private void showSceneAudit(){
        JDialog dialog=new JDialog(window,"Castle Scene Audit — advisory",false);
        DefaultListModel<CastleEditorAudit.Finding> findings=new DefaultListModel<>();
        JList<CastleEditorAudit.Finding> report=new JList<>(findings);
        report.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JLabel count=new JLabel();
        Runnable scan=()->{
            findings.clear();
            for(CastleEditorAudit.Finding f:CastleEditorAudit.inspect(data))
                findings.addElement(f);
            count.setText(findings.isEmpty()
                ?"No centre-overlap, blocked-spawn or duplicate-ID warnings detected; visual review still required."
                :findings.size()+" advisory findings — select one to inspect; refresh after edits.");
        };
        JButton update=button("REFRESH");
        update.addActionListener(e->scan.run());
        JButton jump=button("JUMP TO OBJECT");
        jump.addActionListener(e->{
            CastleEditorAudit.Finding f=report.getSelectedValue();
            if(f==null){count.setText("Select a finding first.");return;}
            // Clear hierarchy filtering so an affected scene object is selectable.
            filter.setText("");
            selectId(f.kind(),f.id());
            renderer.x=Math.max(.5,Math.min(42.5,f.x()-1.8));
            renderer.z=Math.max(.5,Math.min(35.5,f.z()-1.8));
            renderer.eyeHeight=Math.max(.7,Math.min(10.5,f.y()+1.4));
            renderer.yaw=Math.atan2(f.x()-renderer.x,f.z()-renderer.z);
            renderer.pitch=-.12;
            renderer.setEditorSelection(f.x(),f.y(),f.z());
            status.setText("AUDIT: "+f.code()+" — "+f.id()+" (advisory)");
            dialog.dispose();
            viewport.requestFocusInWindow();
        });
        report.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent event){
                if(event.getClickCount()==2)jump.doClick();
            }
        });
        JButton close=button("CLOSE");
        close.addActionListener(e->dialog.dispose());
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,4,3));
        actions.add(update);actions.add(jump);actions.add(close);
        JPanel view=new JPanel(new BorderLayout(4,4));
        view.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        view.add(count,BorderLayout.NORTH);
        view.add(new JScrollPane(report),BorderLayout.CENTER);
        view.add(actions,BorderLayout.SOUTH);
        dialog.setContentPane(view);
        dialog.setSize(930,410);
        dialog.setLocationRelativeTo(window);
        scan.run();
        dialog.setVisible(true);
    }

    private void openAssetImport(){
        AssetImportDialog.show(window,id->{
            boolean listed=false;
            for(int i=0;i<library.getItemCount();i++)
                if(id.equals(library.getItemAt(i))){listed=true;break;}
            if(!listed)library.addItem(id);
            library.setSelectedItem(id);
            if(assetPalette!=null)assetPalette.reload(project);
            chooseTool(EditorTool.PROP);
            status.setText("Imported "+id+" — PROP click places in the live scene; Ctrl+S saves placement");
            viewport.requestFocusInWindow();
        });
    }

    private static JButton button(String name){return new JButton(name);}
    private JPanel objectInspector(){
        JPanel panel=new JPanel(new BorderLayout(4,3));
        JPanel addRow=new JPanel(new BorderLayout(3,3));
        addRow.add(library,BorderLayout.CENTER);
        JButton add=button("ADD ASSET");
        add.addActionListener(e->addAsset());
        addRow.add(add,BorderLayout.EAST);panel.add(addRow,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(0,4,3,3));
        fields.add(new JLabel("X"));fields.add(x);
        fields.add(new JLabel("Y"));fields.add(y);
        fields.add(new JLabel("Z"));fields.add(z);
        fields.add(new JLabel("Yaw °"));fields.add(yaw);
        fields.add(new JLabel("Size"));fields.add(scale);
        fields.add(new JLabel("Collision X"));fields.add(width);
        fields.add(new JLabel("Collision Z"));fields.add(depth);
        fields.add(new JLabel("Light range"));fields.add(radius);
        fields.add(new JLabel("Light power"));fields.add(power);
        fields.add(new JLabel("Wall height / roof rise"));fields.add(surfaceHeight);
        for(JSpinner input:new JSpinner[]{x,y,z,yaw,scale,width,depth,radius,power,surfaceHeight})
            input.addChangeListener(e->applySelection());
        panel.add(fields,BorderLayout.CENTER);
        JPanel actions=new JPanel(new GridLayout(1,4,4,0));
        JButton here=button("PLACE AHEAD"),remove=button("REMOVE"),addLight=button("ADD LIGHT");
        JButton duplicate=button("DUPLICATE");
        duplicate.addActionListener(e->duplicate());
        here.addActionListener(e->placeAhead());
        remove.addActionListener(e->remove());
        addLight.addActionListener(e->addLamp());
        actions.add(here);actions.add(remove);actions.add(addLight);actions.add(duplicate);
        panel.add(actions,BorderLayout.SOUTH);
        return panel;
    }
    private JPanel surfaceInspector(){
        JPanel panel=new JPanel(new BorderLayout(3,4));
        JPanel paintPalette=new JPanel(new GridLayout(4,4,2,2));
        for(int i=0;i<MaterialAtlas.COUNT;i++){
            final int index=i;
            JButton swatch=new JButton(String.valueOf(i));
            swatch.setOpaque(true);
            swatch.setBackground(new Color(MaterialAtlas.sample(i,.35,.35)));
            swatch.setToolTipText(material.getItemAt(i)+" — click then paint");
            swatch.addActionListener(e->{
                material.setSelectedIndex(index);chooseTool(EditorTool.PAINT);
                if(chosen!=null)paintSelection();
            });
            paintPalette.add(swatch);
        }
        JPanel top=new JPanel(new BorderLayout(3,3));
        top.add(new JLabel("TEXTURE PALETTE  •  click swatch, then face"),BorderLayout.NORTH);
        top.add(paintPalette,BorderLayout.CENTER);
        panel.add(top,BorderLayout.SOUTH);
        JPanel tools=new JPanel(new GridLayout(0,4,3,3));
        tools.add(new JLabel("Tile X"));tools.add(tileX);
        tools.add(new JLabel("Tile Z"));tools.add(tileZ);
        tools.add(new JLabel("Material"));tools.add(material);
        JButton fill=button("PAINT FLOOR");fill.addActionListener(e->operate(()->{
            data.paintFloor(integer(tileX),integer(tileZ),material.getSelectedIndex());
        }));tools.add(fill);
        JButton wall=button("PAINT WALL");wall.addActionListener(e->operate(()->{
            data.paintWall(integer(wallIndex),material.getSelectedIndex());
        }));tools.add(wall);
        tools.add(new JLabel("Wall ID"));tools.add(wallIndex);
        JButton focusTile=button("CAMERA TILE");focusTile.addActionListener(e->{
            tileX.setValue((int)Math.max(0,Math.min(CastleHubMap.WIDTH-1,Math.floor(renderer.x))));
            tileZ.setValue((int)Math.max(0,Math.min(CastleHubMap.HEIGHT-1,Math.floor(renderer.z))));
            updateNav();
        });tools.add(focusTile);
        JButton block=button("SET NAV");
        block.addActionListener(e->operate(()->data.setNavBlocked(integer(tileX),
            integer(tileZ),navBlocked.isSelected())));
        tools.add(block);
        tools.add(navBlocked);tools.add(navOverlay);
        JButton spawn=button("SPAWN HERE");spawn.addActionListener(e->operate(()->{
            data.setSpawn(renderer.x,renderer.z,renderer.yaw);
            refresh();
        }));tools.add(spawn);
        tileX.addChangeListener(e->updateNav());tileZ.addChangeListener(e->updateNav());
        wallIndex.addChangeListener(e->updateNav());
        navOverlay.addActionListener(e->viewport.repaint());
        panel.add(tools,BorderLayout.NORTH);
        panel.add(new JScrollPane(navInfo),BorderLayout.CENTER);
        return panel;
    }
    private static int integer(JSpinner s){return ((Number)s.getValue()).intValue();}
    private static double value(JSpinner s){return ((Number)s.getValue()).doubleValue();}
    private void operate(Runnable action){
        try{
            history.execute(action);
            updateNav();status.setText("Modified scene — SAVE to persist");
        }
        catch(RuntimeException e){JOptionPane.showMessageDialog(window,e.getMessage(),
            "Invalid level edit",JOptionPane.ERROR_MESSAGE);}
    }
    private void addAsset(){
        String mesh=(String)library.getSelectedItem();if(mesh==null)return;
        double aheadX=renderer.x+Math.sin(renderer.yaw)*2.4;
        double aheadZ=renderer.z+Math.cos(renderer.yaw)*2.4;
        if(aheadX<0||aheadX>CastleHubMap.WIDTH||aheadZ<0||aheadZ>CastleHubMap.HEIGHT){
            status.setText("Move inside the level before placing an asset");return;
        }
        String id="custom"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Prop(id,mesh,aheadX,
            renderer.eyeHeight>3?CastleHubArchitecturalPlan.GROUND_CEILING:0,
            aheadZ,.8,0,.4,.4,renderer.eyeHeight>3)));
        refresh();selectId("PROP",id);
    }
    private void addLamp(){
        double px=renderer.x+Math.sin(renderer.yaw)*2;
        double pz=renderer.z+Math.cos(renderer.yaw)*2;
        String id="light"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Lamp(id,"ceiling_lamp_1_on",
            "ceiling_lamp_1_off",px,3.38,pz,.8,0,CastleHubDecor.Mount.CEILING,9.8,2.5)));
        refresh();selectId("LAMP",id);
    }
    private void remove(){
        if(chosen==null)return;
        Row row=chosen;
        if("PROP".equals(row.kind))operate(()->data.removeProp(row.id));
        else if("LAMP".equals(row.kind))operate(()->data.removeLamp(row.id));
        else if("SURFACE".equals(row.kind))operate(()->data.removeSurface(row.id));
        else{status.setText("Station/spawn cannot be deleted: move them instead");return;}
        chosen=null;refresh();
    }
    private void placeAhead(){
        if(chosen==null)return;
        modifying=true;
        x.setValue(renderer.x+Math.sin(renderer.yaw)*2.2);
        z.setValue(renderer.z+Math.cos(renderer.yaw)*2.2);
        modifying=false;applySelection();
    }
    private void select(Row row){
        if(row==null){chosen=null;renderer.clearEditorSelection();return;}
        chosen=row;modifying=true;
        try{
            if(row.kind.equals("PROP")){
                CastleLevelEdits.Prop item=data.props().stream().filter(p->p.id().equals(row.id))
                    .findFirst().orElse(null);
                if(item==null)return;
                x.setValue(item.x());y.setValue(item.y());z.setValue(item.z());
                yaw.setValue(Math.toDegrees(item.yaw()));scale.setValue(item.scale());
                width.setValue(item.halfX());depth.setValue(item.halfZ());
            }else if(row.kind.equals("LAMP")){
                CastleLevelEdits.Lamp l=data.lamps().stream().filter(p->p.id().equals(row.id))
                    .findFirst().orElse(null);
                if(l==null)return;
                x.setValue(l.x());y.setValue(l.y());z.setValue(l.z());
                yaw.setValue(Math.toDegrees(l.yaw()));scale.setValue(l.scale());
                radius.setValue(l.radius());power.setValue(l.power());
            }else if(row.kind.equals("SURFACE")){
                var surface=data.surfaces().stream().filter(p->p.id().equals(row.id))
                    .findFirst().orElse(null);
                if(surface==null)return;
                x.setValue(surface.cx());y.setValue(surface.base());z.setValue(surface.cz());
                width.setValue(surface.width());depth.setValue(surface.depth());
                surfaceHeight.setValue(surface.height());material.setSelectedIndex(surface.material());
            }else if(row.kind.equals("STATION")){
                var coords=data.station(CastleHubMap.Station.valueOf(row.id));
                x.setValue(coords[0]);y.setValue(0.);z.setValue(coords[1]);
            }else if(row.kind.equals("SPAWN")){
                double[] sp=data.spawn();
                x.setValue(sp[0]);y.setValue(0.);z.setValue(sp[1]);
                yaw.setValue(Math.toDegrees(sp[2]));
            }
            renderer.setEditorSelection(value(x),value(y),value(z));
        }finally{modifying=false;}
    }
    private void applySelection(){
        if(modifying||chosen==null)return;
        Row r=chosen;
        operate(()->{
            switch(r.kind){
                case "PROP" ->{
                    var old=data.props().stream().filter(p->p.id().equals(r.id)).findFirst().orElseThrow();
                    data.put(new CastleLevelEdits.Prop(r.id,old.mesh(),value(x),value(y),
                        value(z),value(scale),Math.toRadians(value(yaw)),
                        value(width),value(depth),old.upper()));
                }
                case "LAMP" ->{
                    var old=data.lamps().stream().filter(p->p.id().equals(r.id)).findFirst().orElseThrow();
                    data.put(new CastleLevelEdits.Lamp(r.id,old.on(),old.off(),value(x),
                        value(y),value(z),value(scale),Math.toRadians(value(yaw)),
                        old.mount(),value(radius),value(power)));
                }
                case "SURFACE" -> {
                    var old=data.surfaces().stream().filter(v->v.id().equals(r.id))
                        .findFirst().orElseThrow();
                    double w=old.kind()==CastleLevelEdits.Kind.WALL?
                        (old.width()>0?Math.max(.2,value(width)):0):Math.max(.2,value(width));
                    double d=old.kind()==CastleLevelEdits.Kind.WALL?
                        (old.depth()>0?Math.max(.2,value(depth)):0):Math.max(.2,value(depth));
                    data.put(new CastleLevelEdits.Surface(r.id,old.kind(),
                        value(x)-w/2,value(z)-d/2,value(x)+w/2,value(z)+d/2,
                        value(y),value(surfaceHeight),material.getSelectedIndex()));
                }
                case "STATION" -> data.setStation(CastleHubMap.Station.valueOf(r.id),value(x),value(z));
                case "SPAWN" -> data.setSpawn(value(x),value(z),Math.toRadians(value(yaw)));
                default -> throw new IllegalArgumentException("Unknown editor entity");
            }
            renderer.setEditorSelection(value(x),value(y),value(z));
        });
    }
    private void selectId(String kind,String id){
        for(int i=0;i<rows.size();i++)if(rows.get(i).kind.equals(kind)&&rows.get(i).id.equals(id)){
            hierarchy.setSelectedIndex(i);hierarchy.ensureIndexIsVisible(i);return;
        }
    }
    private void refresh(){
        String kind=chosen==null?"":chosen.kind,id=chosen==null?"":chosen.id;
        rows.clear();
        String q=filter.getText().strip().toLowerCase(Locale.ROOT);
        var list=new ArrayList<Row>();
        for(var p:data.props())list.add(new Row("PROP",p.id(),p.id()+"  •  "+p.mesh()));
        for(var l:data.lamps())list.add(new Row("LAMP",l.id(),l.id()+"  •  "+l.on()+"  [LIGHT]"));
        for(var surface:data.surfaces())list.add(new Row("SURFACE",surface.id(),
            surface.kind()+"  •  "+surface.id()+"  [material "+surface.material()+"]"));
        for(var station:CastleHubMap.Station.values())
            list.add(new Row("STATION",station.name(),"STATION  •  "+station.name()));
        list.add(new Row("SPAWN","start","PLAYER SPAWN"));
        list.sort(Comparator.comparing(Row::description));
        for(Row r:list)if(q.isEmpty()||r.description.toLowerCase(Locale.ROOT).contains(q))rows.addElement(r);
        selectId(kind,id);updateNav();
    }
    private void updateNav(){
        int px=integer(tileX),pz=integer(tileZ),w=integer(wallIndex);
        navBlocked.setSelected(data.navBlocked(px,pz));
        var wall=CastleHubArchitecturalPlan.walls().get(w);
        navInfo.setText("CELL "+px+", "+pz+"  solid="+map.solid(px,pz)+
            "  navigable="+map.traversable(px+.5,pz+.5)+
            "\nFloor tile="+data.floorMaterial(px,pz,MaterialAtlas.STONE)+
            "  edited blocker="+data.navBlocked(px,pz)+
            "\nUpper gallery="+CastleHubArchitecturalPlan.upperWalkable(px+.5,pz+.5)+
            "  stairs="+CastleHubArchitecturalPlan.onStairs(px+.5,pz+.5)+
            "\nWALL "+w+"  ("+wall.x0()+","+wall.z0()+") -> ("+wall.x1()+","+wall.z1()+")"+
            "\nMaterial="+data.wallMaterial(w,wall.material())+
            "  door opening="+wall.hasDoor()+"\nScene file: "+data.path());
        navInfo.setCaretPosition(0);
    }
    private void pick(int mouseX,int mouseY){
        // Closest projected registered object to the clicked viewport position.
        double best=Double.POSITIVE_INFINITY;Row hit=null;
        for(int i=0;i<rows.size();i++){
            Row r=rows.get(i);
            double px,pz,py;
            if("PROP".equals(r.kind)){
                var p=data.props().stream().filter(t->t.id().equals(r.id)).findFirst().orElse(null);
                if(p==null)continue;px=p.x();pz=p.z();py=p.y()+.8;
            }else if("LAMP".equals(r.kind)){
                var l=data.lamps().stream().filter(t->t.id().equals(r.id)).findFirst().orElse(null);
                if(l==null)continue;px=l.x();pz=l.z();py=l.y();
            }else if("SURFACE".equals(r.kind)){
                var part=data.surfaces().stream().filter(t->t.id().equals(r.id)).findFirst().orElse(null);
                if(part==null)continue;
                px=part.cx();pz=part.cz();
                py=part.base()+(part.kind()==CastleLevelEdits.Kind.FLOOR?0:part.height()*.5);
            }else continue;
            double dx=px-renderer.x,dz=pz-renderer.z,dy=py-renderer.eyeHeight;
            double sy=Math.sin(renderer.yaw),cy=Math.cos(renderer.yaw);
            double cx=-dx*cy+dz*sy,cz=dx*sy+dz*cy,sp=Math.sin(renderer.pitch),cp=Math.cos(renderer.pitch);
            double yy=dy*cp-cz*sp,zz=dy*sp+cz*cp;
            if(zz<.25)continue;
            double sx=viewport.getWidth()*.5+cx/zz*viewport.getHeight()*.75;
            double syy=viewport.getHeight()*.5-yy/zz*viewport.getHeight()*.75;
            double pixel=Math.hypot(sx-mouseX,syy-mouseY);
            if(pixel<32&&pixel<best){best=pixel;hit=r;}
        }
        if(hit!=null)hierarchy.setSelectedValue(hit,true);
    }
    private void advance(double amount){
        renderer.x+=Math.sin(renderer.yaw)*amount;
        renderer.z+=Math.cos(renderer.yaw)*amount;
        renderer.x=Math.max(.2,Math.min(43.8,renderer.x));
        renderer.z=Math.max(.2,Math.min(35.8,renderer.z));
    }
    private void frame(){
        double step=keys.contains(KeyEvent.VK_SHIFT)?.95:.34;
        if(keys.contains(KeyEvent.VK_W))advance(step);
        if(keys.contains(KeyEvent.VK_S))advance(-step);
        if(keys.contains(KeyEvent.VK_A)){
            renderer.x-=Math.cos(renderer.yaw)*step;
            renderer.z+=Math.sin(renderer.yaw)*step;
        }
        if(keys.contains(KeyEvent.VK_D)){
            renderer.x+=Math.cos(renderer.yaw)*step;
            renderer.z-=Math.sin(renderer.yaw)*step;
        }
        if(keys.contains(KeyEvent.VK_Q))renderer.eyeHeight-=step;
        if(keys.contains(KeyEvent.VK_E))renderer.eyeHeight+=step;
        if(keys.contains(KeyEvent.VK_LEFT))renderer.yaw+=.06;
        if(keys.contains(KeyEvent.VK_RIGHT))renderer.yaw-=.06;
        if(keys.contains(KeyEvent.VK_UP))renderer.pitch=Math.min(1.35,renderer.pitch+.045);
        if(keys.contains(KeyEvent.VK_DOWN))renderer.pitch=Math.max(-1.35,renderer.pitch-.045);
        renderer.eyeHeight=Math.max(.25,Math.min(11,renderer.eyeHeight));
        renderer.setEditorNavigation(navOverlay.isSelected());
        try{renderer.drawCastle(map,clock+=.05,true);
            requestLightVolumePreview();
        }
        catch(RuntimeException problem){
            timer.stop();BeckenburgDiagnostics.fail("Developer editor viewport",problem);
            status.setText("Renderer failure: "+problem.getMessage());return;
        }
        viewport.repaint();
    }
    /**
     * The editor uses exactly the same authored light radius and solid
     * Castle ray intersections as the runtime, but the gizmo is editor-only.
     * Recompute off the EDT only when the selected lamp, map geometry or
     * door revision actually changes; retain the last valid preview.
     */
    private void requestLightVolumePreview(){
        if(!lightVolumeOverlay.isSelected()||chosen==null
            ||!"LAMP".equals(chosen.kind)){
            lightVolumeKey="";lightVolume=null;return;
        }
        var lamp=data.lamps().stream().filter(v->v.id().equals(chosen.id))
            .findFirst().orElse(null);
        if(lamp==null){lightVolumeKey="";lightVolume=null;return;}
        String version=lamp+" / geometry="+data.revision()+
            " / doors="+map.doorRevision();
        if(version.equals(lightVolumeKey))return;
        lightVolumeKey=version;
        lightVolume=null; // never display stale light volumes on a changed fixture
        lightPreviewWorker.execute(()->{
            try{
                var calculated=LightVolumePreview.trace(map,lamp);
                SwingUtilities.invokeLater(()->{
                    if(!editorClosed&&version.equals(lightVolumeKey)){
                        lightVolume=calculated;viewport.repaint();
                    }
                });
            }catch(RuntimeException error){
                BeckenburgDiagnostics.fail("Editor light influence preview",error);
            }
        });
    }
    private void chooseTool(EditorTool choice){
        tool=choice;
        if(palette.containsKey(choice))palette.get(choice).setSelected(true);
        status.setText(choice+" • Left-click edit / drag walls, floors and roofs • Right-drag look"+
            " • Ctrl+Z undo • Ctrl+Y redo");
        viewport.requestFocusInWindow();
    }
    private double snap(double raw){
        double step=switch(gridSnap.getSelectedIndex()){
            case 1 -> .25;
            case 2 -> .5;
            case 3 -> 1.;
            case 4 -> 2.;
            default -> 0.;
        };
        return step>0?Math.round(raw/step)*step:Math.round(raw*100)/100.0;
    }
    private static double clamp(double value,double min,double max){
        return Math.max(min,Math.min(max,value));
    }
    /** Intersect a perspective camera ray with the user-selected 3D workplane.
     * This is world-space placement, not a fixed-distance HUD marker. */
    private Point worldOnPlane(int px,int py){
        double h=Math.max(1,viewport.getHeight()),w=Math.max(1,viewport.getWidth());
        double cameraX=(px-w*.5)/(h*.765),cameraY=(h*.5-py)/(h*.765);
        double cosY=Math.cos(renderer.yaw),sinY=Math.sin(renderer.yaw);
        double cosP=Math.cos(renderer.pitch),sinP=Math.sin(renderer.pitch);
        double dy=cameraY*cosP+sinP,forward=-cameraY*sinP+cosP;
        double dx=-cameraX*cosY+forward*sinY;
        double dz=cameraX*sinY+forward*cosY;
        double travel=(value(workPlane)-renderer.eyeHeight)/dy;
        if(!Double.isFinite(travel)||travel<.15||travel>100){
            // Keep placement deterministic when looking at the horizon.
            travel=3.5/Math.max(.15,forward);
        }
        return new Point(snap(clamp(renderer.x+dx*travel,.5,43.5)),
            snap(clamp(renderer.z+dz*travel,.5,35.5)));
    }
    private void editViewport(int fromX,int fromY,Point begin,Point end){
        if(begin==null||end==null)return;
        switch(tool){
            case SELECT -> pick(fromX,fromY);
            case WALL,FLOOR,ROOF -> drawSurface(begin,end,tool);
            case PROP -> addAsset(end);
            case LIGHT -> addLamp(end);
            case PAINT -> {
                pick(fromX,fromY);
                if(chosen!=null&&"SURFACE".equals(chosen.kind))paintSelection();
                else operate(()->data.paintFloor((int)Math.floor(end.x),(int)Math.floor(end.z),
                    material.getSelectedIndex()));
            }
            case DELETE -> {pick(fromX,fromY);remove();}
            case MOVE -> {
                pick(fromX,fromY);
                if(chosen!=null){
                    modifying=true;
                    try{
                        x.setValue(clamp(value(x)+end.x-begin.x,0,CastleHubMap.WIDTH));
                        z.setValue(clamp(value(z)+end.z-begin.z,0,CastleHubMap.HEIGHT));
                    }
                    finally{modifying=false;}
                    applySelection();
                }
            }
            case ROTATE -> {pick(fromX,fromY);rotateSelection();}
            case VERTEX,EDGE -> reshapeSurface(fromX,fromY,begin,end,tool==EditorTool.VERTEX);
            case SCALE -> {
                pick(fromX,fromY);
                if(chosen!=null){
                    modifying=true;
                    try{
                        width.setValue(clamp(value(width)+end.x-begin.x,.1,44));
                        depth.setValue(clamp(value(depth)+end.z-begin.z,.1,44));
                    }finally{modifying=false;}
                    applySelection();
                }
            }
        }
        viewport.requestFocusInWindow();
    }
    /** Vertex and edge handles modify actual persisted architecture, sharing the same
     * undo stack, collision volumes and renderer source as other editor tools. */
    private void reshapeSurface(int clickX,int clickY,Point begin,Point end,boolean corner){
        if(chosen==null||!"SURFACE".equals(chosen.kind))pick(clickX,clickY);
        if(chosen==null||!"SURFACE".equals(chosen.kind)){
            status.setText("Select a wall, floor or roof surface to edit its geometry");return;
        }
        String id=chosen.id;
        operate(()->{
            var surface=data.surfaces().stream().filter(p->p.id().equals(id))
                .findFirst().orElseThrow();
            data.put(CastleSurfaceTransforms.drag(surface,begin.x,begin.z,
                end.x-begin.x,end.z-begin.z,corner));
        });
        refresh();selectId("SURFACE",id);
    }
    private void drawSurface(Point begin,Point end,EditorTool shape){
        final CastleLevelEdits.Kind kind=CastleLevelEdits.Kind.valueOf(shape.name());
        double x0=Math.min(begin.x,end.x),x1=Math.max(begin.x,end.x);
        double z0=Math.min(begin.z,end.z),z1=Math.max(begin.z,end.z);
        if(kind==CastleLevelEdits.Kind.WALL){
            if(x1-x0>=z1-z0){
                z0=z1=begin.z;
                if(x1-x0<.2)x1=clamp(x0+2,0,CastleHubMap.WIDTH);
            }else{
                x0=x1=begin.x;
                if(z1-z0<.2)z1=clamp(z0+2,0,CastleHubMap.HEIGHT);
            }
        }else{
            if(x1-x0<.2)x1=clamp(x0+2,0,CastleHubMap.WIDTH);
            if(z1-z0<.2)z1=clamp(z0+2,0,CastleHubMap.HEIGHT);
        }
        String id="shape"+System.nanoTime();
        var part=new CastleLevelEdits.Surface(id,kind,x0,z0,x1,z1,
            value(workPlane)+(kind==CastleLevelEdits.Kind.FLOOR&&value(workPlane)==0?.025:0),
            kind==CastleLevelEdits.Kind.FLOOR?.16:value(surfaceHeight),
            material.getSelectedIndex());
        operate(()->data.put(part));
        refresh();selectId("SURFACE",id);
    }
    private void paintSelection(){
        if(chosen==null||!"SURFACE".equals(chosen.kind))return;
        String id=chosen.id;int paletteId=material.getSelectedIndex();
        operate(()->{
            var old=data.surfaces().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();
            data.put(new CastleLevelEdits.Surface(id,old.kind(),old.x0(),old.z0(),
                old.x1(),old.z1(),old.base(),old.height(),paletteId));
        });
    }
    private void rotateSelection(){
        if(chosen==null)return;
        Row row=chosen;
        if("PROP".equals(row.kind)){
            operate(()->{
                var old=data.props().stream().filter(p->p.id().equals(row.id)).findFirst().orElseThrow();
                data.put(new CastleLevelEdits.Prop(old.id(),old.mesh(),old.x(),old.y(),
                    old.z(),old.scale(),old.yaw()+Math.PI/2,old.halfZ(),old.halfX(),old.upper()));
            });
        }else if("SURFACE".equals(row.kind)){
            operate(()->{
                var old=data.surfaces().stream().filter(p->p.id().equals(row.id)).findFirst().orElseThrow();
                double w=old.width(),d=old.depth();
                data.put(new CastleLevelEdits.Surface(old.id(),old.kind(),
                    old.cx()-d*.5,old.cz()-w*.5,old.cx()+d*.5,old.cz()+w*.5,
                    old.base(),old.height(),old.material()));
            });
        }else if("LAMP".equals(row.kind)){
            operate(()->{
                var old=data.lamps().stream().filter(p->p.id().equals(row.id)).findFirst().orElseThrow();
                data.put(new CastleLevelEdits.Lamp(old.id(),old.on(),old.off(),old.x(),old.y(),
                    old.z(),old.scale(),old.yaw()+Math.PI/2,old.mount(),old.radius(),old.power()));
            });
        }else return;
        refresh();selectId(row.kind,row.id);
    }
    private void addAsset(Point target){
        String mesh=(String)library.getSelectedItem();if(mesh==null)return;
        String id="asset"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Prop(id,mesh,target.x,
            value(workPlane),target.z,.8,0,.4,.4,value(workPlane)>2)));
        refresh();selectId("PROP",id);
    }
    private void addLamp(Point target){
        String id="light"+System.nanoTime();
        operate(()->data.put(new CastleLevelEdits.Lamp(id,"ceiling_lamp_1_on",
            "ceiling_lamp_1_off",target.x,clamp(value(workPlane)+3.38,0,9),
            target.z,.8,0,CastleHubDecor.Mount.CEILING,
            value(radius),value(power))));
        refresh();selectId("LAMP",id);
    }
    private void duplicate(){
        if(chosen==null)return;
        Row selected=chosen;
        String id="copy"+System.nanoTime();
        switch(selected.kind){
            case "PROP" -> {
                var p=data.props().stream().filter(v->v.id().equals(selected.id)).findFirst().orElseThrow();
                operate(()->data.put(new CastleLevelEdits.Prop(id,p.mesh(),
                    clamp(p.x()+1,0,CastleHubMap.WIDTH),p.y(),p.z(),p.scale(),p.yaw(),
                    p.halfX(),p.halfZ(),p.upper())));
            }
            case "LAMP" -> {
                var l=data.lamps().stream().filter(v->v.id().equals(selected.id)).findFirst().orElseThrow();
                operate(()->data.put(new CastleLevelEdits.Lamp(id,l.on(),l.off(),
                    clamp(l.x()+1,0,CastleHubMap.WIDTH),l.y(),l.z(),l.scale(),
                    l.yaw(),l.mount(),l.radius(),l.power())));
            }
            case "SURFACE" -> {
                var p=data.surfaces().stream().filter(v->v.id().equals(selected.id)).findFirst().orElseThrow();
                double shift=clamp(1,0,CastleHubMap.WIDTH-p.x1());
                operate(()->data.put(new CastleLevelEdits.Surface(id,p.kind(),
                    p.x0()+shift,p.z0(),p.x1()+shift,p.z1(),p.base(),p.height(),p.material())));
            }
            default -> {return;}
        }
        refresh();selectId(selected.kind,id);
    }
    private void undo(){
        if(!history.undo())return;
        chosen=null;renderer.clearEditorSelection();refresh();
        status.setText("Undo • scene modified, SAVE to persist");
    }
    private void redo(){
        if(!history.redo())return;
        chosen=null;renderer.clearEditorSelection();refresh();
        status.setText("Redo • scene modified, SAVE to persist");
    }
    private void save(){
        try{project.save();status.setText("SAVED  "+project.documentPath());viewport.requestFocusInWindow();}
        catch(IOException e){JOptionPane.showMessageDialog(window,e.getMessage(),"Save failed",JOptionPane.ERROR_MESSAGE);}
    }
    private void export(){
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export level editor document");
        chooser.setSelectedFile(new java.io.File("castle-beckenburg.properties"));
        if(chooser.showSaveDialog(window)!=JFileChooser.APPROVE_OPTION)return;
        try{
            data.save();
            Files.copy(data.path(),chooser.getSelectedFile().toPath(),StandardCopyOption.REPLACE_EXISTING);
            status.setText("EXPORTED "+chooser.getSelectedFile());
        }catch(IOException e){JOptionPane.showMessageDialog(window,e.getMessage(),"Export failed",JOptionPane.ERROR_MESSAGE);}
    }
}
