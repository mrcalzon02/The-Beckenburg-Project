package org.beckenburg.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.function.LongConsumer;
import javax.imageio.ImageIO;

/** Material-owned UV/texture workspace. All geometry and bindings are immutable native observations. */
public final class EditorMaterialWorkspace extends JPanel {
    private static final class Properties extends JPanel implements Scrollable {
        Properties(JComponent content){super(new BorderLayout());add(content,BorderLayout.NORTH);}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(290,360);}
        public int getScrollableUnitIncrement(Rectangle r,int axis,int direction){return 24;}
        public int getScrollableBlockIncrement(Rectangle r,int axis,int direction){return Math.max(24,r.height-24);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private EditorTopologyProject.State state;
    private EditorMeshMaterial material;
    private BufferedImage texture;
    private Long selectedFace;
    private boolean pinsVisible,seamsVisible;
    private int guideEdge;
    public void selectGuideEdge(int corner) {
        if (guideEdge==corner) return;
        guideEdge=corner;
        if (seamsVisible) canvas.repaint();
    }
    public void showPins(boolean visible){showUvGuides(visible,false);}
    public void showUvGuides(boolean pins,boolean seams){pinsVisible=pins;seamsVisible=seams;canvas.repaint();}
    private final JButton isolate=new JButton("Isolate"), hide=new JButton("Hide"), showAll=new JButton("Show all");
    public record Visibility(Long isolated,java.util.Set<Long> hidden) {
        public Visibility { hidden=java.util.Set.copyOf(hidden); }
        public boolean includes(long face) { return (isolated==null || isolated==face) && !hidden.contains(face); }
        public boolean filtered() { return isolated!=null || !hidden.isEmpty(); }
    }
    private static final Visibility ALL=new Visibility(null,java.util.Set.of());
    private Visibility visibility=ALL;
    private final Runnable visibilityChanged;
    public int visibleFaces() { return faces.getSize(); }
    public Visibility visibility() { return visibility; }
    public interface PencilAction { void preview(long face,List<Integer> texels,int color); }
    private PencilAction pencilAction;
    private Runnable cancelProjection=()->{};
    private Runnable viewChanged=()->{};
    private final JComboBox<String> materialView=new JComboBox<>(new String[]{"Texture / UV","Model projection"});
    private final JSpinner projectionPadding=new JSpinner(new SpinnerNumberModel(0,0,16,1));
    private final JToggleButton pencil=new JToggleButton("Pencil");
    private final JButton ink=new JButton("Color");
    private Color paintColor=new Color(255,0,255);
    private java.util.LinkedHashSet<Integer> stroke;
    private Point lastPixel;
    private final JSpinner tileU=tileSpinner(),tileV=tileSpinner();
    private final JButton faceTile=new JButton("Face tile");
    private EditorUvTile tile=EditorUvTile.ORIGIN;
    public EditorUvTile tile(){return tile;}
    private static JSpinner tileSpinner(){
        var spinner=new JSpinner(new SpinnerNumberModel(0,Integer.MIN_VALUE,Integer.MAX_VALUE,1));
        ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setColumns(5);
        return spinner;
    }
    private int strokeColor;
    private final LongConsumer selection;
    private final FaceRows faces=new FaceRows();
    private final JList<EditorTopologyProject.Face> faceList=new JList<>(faces);
    private boolean updating;
    private final JLabel status=new JLabel("Open a native model to inspect its material UVs");
    private String previewIssue;
    public void previewIssue(String message){previewIssue=message;if(message!=null)status.setText(message);}
    private double zoom=1,panX,panY;private Point panStart;
    private record PaintView(int width,int height,Visibility visibility,EditorUvTile tile,
        double zoom,double panX,double panY,boolean pins,boolean seams,int guide,Font font,Color background,RenderingHints hints) {}
    private BufferedImage paintedUv;
    private EditorTopologyProject.State paintedState;
    private EditorMeshMaterial paintedMaterial;
    private PaintView paintedView;
    private Long paintedFace,paintedRow;
    private record FaceEdit(EditorTopologyProject.Face before,EditorTopologyProject.Face after) {}
    private FaceEdit pendingPaintEdit;
    private static final boolean RETAIN_UV_PAINT=!"false".equalsIgnoreCase(System.getProperty("beckenburg.editor.uvPaintCache"));
    private final JPanel canvas=new JPanel(){protected void paintComponent(Graphics graphics){super.paintComponent(graphics);paintCanvas((Graphics2D)graphics);}};
    private void paintCanvas(Graphics2D graphics){
        var transform=graphics.getTransform();
        Object textHint=graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        boolean lcdText=textHint==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB || textHint==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR
            || textHint==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB || textHint==RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
        int width=canvas.getWidth(),height=canvas.getHeight();
        // Retain exactly one opaque, unscaled canvas. Other transforms keep Java2D's
        // original rasterization, including fractional/HiDPI and print rendering.
        boolean retain=RETAIN_UV_PAINT && state!=null && stroke==null && !lcdText && width>0 && height>0
            && !(graphics instanceof java.awt.print.PrinterGraphics)
            && (long)width*height<=4_194_304 && canvas.getBackground().getAlpha()==255
            && graphics.getComposite().equals(AlphaComposite.SrcOver)
            && transform.getScaleX()==1 && transform.getScaleY()==1
            && transform.getShearX()==0 && transform.getShearY()==0
            && transform.getTranslateX()==Math.rint(transform.getTranslateX())
            && transform.getTranslateY()==Math.rint(transform.getTranslateY());
        if(!retain){
            // An unsupported-size/transform view must not retain a large previous image.
            paintedUv=null;paintedState=null;paintedMaterial=null;paintedView=null;pendingPaintEdit=null;
            paintTexture(graphics);return;
        }
        var row=faceList.getSelectedValue();
        Long rowId=row==null?null:row.id();
        var view=new PaintView(width,height,visibility,tile,zoom,panX,panY,pinsVisible,seamsVisible,
            guideEdge,graphics.getFont(),canvas.getBackground(),graphics.getRenderingHints());
        boolean sameCanvas=paintedUv!=null && (paintedState==state || pendingPaintEdit!=null) && paintedMaterial==material && view.equals(paintedView);
        boolean sameSelection=java.util.Objects.equals(paintedFace,selectedFace) && java.util.Objects.equals(paintedRow,rowId);
        if(!sameCanvas || !sameSelection || pendingPaintEdit!=null){
            // A changed selection affects two outlines and their corner labels. Replay all
            // intersecting faces in their original order, including translucent overlaps.
            boolean local=sameCanvas && java.util.Objects.equals(paintedFace,paintedRow)
                && java.util.Objects.equals(selectedFace,rowId) && (pendingPaintEdit==null || sameSelection);
            var edit=pendingPaintEdit;pendingPaintEdit=null;
            paintedState=null;paintedMaterial=null;paintedView=null;
            if(paintedUv==null || paintedUv.getWidth()!=width || paintedUv.getHeight()!=height)
                paintedUv=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            var buffer=paintedUv.createGraphics();
            try{
                buffer.setRenderingHints(view.hints());buffer.setFont(view.font());
                if(local){
                    var oldArea=edit==null?selectionBounds(paintedFace,buffer):faceBounds(edit.before(),buffer);
                    var newArea=edit==null?selectionBounds(selectedFace,buffer):faceBounds(edit.after(),buffer);
                    if(oldArea.intersects(newArea))repaintArea(buffer,oldArea.union(newArea));
                    else{repaintArea(buffer,oldArea);repaintArea(buffer,newArea);}
                }else{
                    buffer.setColor(view.background());buffer.fillRect(0,0,width,height);
                    paintTexture(buffer);
                }
            }finally{buffer.dispose();}
            paintedState=state;paintedMaterial=material;paintedView=view;paintedFace=selectedFace;paintedRow=rowId;
        }
        graphics.drawImage(paintedUv,0,0,null);
    }
    public EditorMaterialWorkspace(JComponent settings,JComponent runtime,JComponent commands,LongConsumer selection){
        this(settings,runtime,commands,selection,()->{});
    }
    public EditorMaterialWorkspace(JComponent settings,JComponent runtime,JComponent commands,LongConsumer selection,Runnable visibilityChanged){
        super(new BorderLayout(8,8));this.selection=selection;this.visibilityChanged=visibilityChanged;setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        faceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        faceList.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                Object label=value instanceof EditorTopologyProject.Face face ? faceLabel(face) : value;
                return super.getListCellRendererComponent(list,label,index,selected,focus);
            }
        });
        faceList.addPropertyChangeListener(e -> {
            if ("font".equals(e.getPropertyName()) || "UI".equals(e.getPropertyName())) {
                faceList.setPrototypeCellValue(null);
                fitFaceRows();
            }
        });
        faceList.addListSelectionListener(e->{if(!updating&&!e.getValueIsAdjusting()&&faceList.getSelectedValue()!=null)selection.accept(faceList.getSelectedValue().id());});
        var left=new JPanel(new BorderLayout(4,4));left.add(new JLabel("NATIVE FACE ASSIGNMENTS"),BorderLayout.NORTH);left.add(new JScrollPane(faceList),BorderLayout.CENTER);left.setPreferredSize(new Dimension(165,480));
        var visibilityControls=new JPanel(new GridLayout(0,1));
        isolate.setToolTipText("Isolate selected face (I)");
        hide.setToolTipText("Hide selected face (H)");
        showAll.setToolTipText("Show all faces (Shift+H)");
        for(var control:List.of(isolate,hide,showAll)) visibilityControls.add(control);
        left.add(visibilityControls,BorderLayout.SOUTH);
        isolate.addActionListener(e->filter(new Visibility(selectedFace,java.util.Set.of())));
        hide.addActionListener(e->{
            var hidden=new java.util.HashSet<>(visibility.hidden());
            hidden.add(selectedFace);
            filter(new Visibility(visibility.isolated(),hidden));
        });
        showAll.addActionListener(e->filter(ALL));
        bind("I",isolate); bind("H",hide); bind("shift H",showAll);
        canvas.setBackground(new Color(29,33,39));canvas.setPreferredSize(new Dimension(520,500));
        var reset=new JButton("Fit texture");reset.addActionListener(e->{zoom=1;panX=panY=0;canvas.repaint();});
        var center=new JPanel(new BorderLayout(3,5));var heading=new JPanel(new BorderLayout());heading.add(materialView,BorderLayout.WEST);heading.add(reset,BorderLayout.EAST);center.add(heading,BorderLayout.NORTH);center.add(canvas,BorderLayout.CENTER);
        var tiles=new JPanel(new FlowLayout(FlowLayout.LEFT,5,1));
        tiles.add(new JLabel("Tile U"));tiles.add(tileU);tiles.add(new JLabel("V"));tiles.add(tileV);tiles.add(faceTile);
        center.add(tiles,BorderLayout.SOUTH);
        tileU.addChangeListener(e->changeTile());tileV.addChangeListener(e->changeTile());
        faceTile.setToolTipText("Show the UV tile containing the center of the selected face");
        faceTile.addActionListener(e->{
            var f=faceList.getSelectedValue();
            if(f==null)return;
            var target=new EditorUvTile(centerTile(f.a().u(),f.b().u(),f.c().u()),centerTile(f.a().v(),f.b().v(),f.c().v())).forMaterial(material);
            tileU.setValue(target.u());tileV.setValue(target.v());
        });
        var paintTools=new JPanel(new FlowLayout(FlowLayout.CENTER,4,0));
        pencil.setToolTipText("Drag to preview a one-texel stroke. Shift-drag erases the paint layer. Escape cancels the drag.");
        paintTools.add(pencil);paintTools.add(ink);
        var paddingLabel=new JLabel("Padding");paddingLabel.setLabelFor(projectionPadding);
        projectionPadding.setToolTipText("Extend this stroke into unused atlas texels. Surface ties stay unchanged.");
        paintTools.add(paddingLabel);paintTools.add(projectionPadding);
        paddingLabel.setVisible(false);projectionPadding.setVisible(false);heading.add(paintTools,BorderLayout.CENTER);
        ink.setIcon(new Icon(){
            public int getIconWidth(){return 14;}
            public int getIconHeight(){return 14;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                g.setColor(c.getForeground());g.drawRect(x,y,13,13);
                g.setColor(paintColor);g.fillRect(x+1,y+1,12,12);
            }
        });
        ink.addActionListener(e->{var color=JColorChooser.showDialog(this,"Pencil color",paintColor);
            if(color!=null){paintColor=color;ink.repaint();}});
        canvas.addMouseWheelListener(e->{zoom=Math.max(.25,Math.min(16,zoom*Math.pow(1.15,-e.getPreciseWheelRotation())));canvas.repaint();});
        var mouse=new MouseAdapter(){
            public void mousePressed(MouseEvent e){
                if(SwingUtilities.isMiddleMouseButton(e))panStart=e.getPoint();
                else if(SwingUtilities.isLeftMouseButton(e)){
                    if(pencil.isSelected() && pencil.isEnabled()){
                        strokeColor=e.isShiftDown()?0:paintColor.getRGB();
                        stroke=new java.util.LinkedHashSet<>();lastPixel=null;sample(e.getPoint());
                    }else pick(e.getPoint());
                }
            }
            public void mouseDragged(MouseEvent e){
                if(stroke!=null)sample(e.getPoint());
                else if(panStart!=null){panX+=e.getX()-panStart.x;panY+=e.getY()-panStart.y;panStart=e.getPoint();canvas.repaint();}
            }
            public void mouseReleased(MouseEvent e){
                panStart=null;
                if(stroke==null || !SwingUtilities.isLeftMouseButton(e))return;
                sample(e.getPoint());
                var points=stroke;stroke=null;lastPixel=null;
                canvas.repaint();
                if(points!=null && !points.isEmpty() && selectedFace!=null)pencilAction.preview(selectedFace,List.copyOf(points),strokeColor);
            }
        };canvas.addMouseListener(mouse);canvas.addMouseMotionListener(mouse);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"),"cancelPencil");
        getActionMap().put("cancelPencil",new AbstractAction(){public void actionPerformed(ActionEvent e){
            stroke=null;lastPixel=null;cancelProjection.run();canvas.repaint();
        }});
        var right=new JPanel(new BorderLayout(4,8));var properties=new JScrollPane(new Properties(settings));properties.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);right.add(properties,BorderLayout.CENTER);right.add(runtime,BorderLayout.SOUTH);right.setPreferredSize(new Dimension(300,500));right.setMinimumSize(new Dimension(240,200));
        var split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,center,right);split.setResizeWeight(.7);split.setBorder(null);add(left,BorderLayout.WEST);add(split,BorderLayout.CENTER);
        var thumbnail=new JPanel(new BorderLayout());thumbnail.add(new JLabel("Texture / UV"),BorderLayout.NORTH);
        thumbnail.setPreferredSize(new Dimension(280,190));
        materialView.getAccessibleContext().setAccessibleName("Material editor view");
        materialView.addActionListener(e->{
            stroke=null;lastPixel=null;cancelProjection.run();
            if(modelView()){
                center.remove(canvas);right.remove(runtime);center.add(runtime,BorderLayout.CENTER);
                thumbnail.add(canvas,BorderLayout.CENTER);right.add(thumbnail,BorderLayout.SOUTH);
            }else{
                thumbnail.remove(canvas);right.remove(thumbnail);center.remove(runtime);
                center.add(canvas,BorderLayout.CENTER);right.add(runtime,BorderLayout.SOUTH);
            }
            tiles.setVisible(!modelView());reset.setVisible(!modelView());paddingLabel.setVisible(modelView());
            projectionPadding.setVisible(modelView());revalidate();repaint();
            SwingUtilities.invokeLater(viewChanged);
        });
        var bottom=new JPanel(new BorderLayout(4,4));bottom.add(status,BorderLayout.NORTH);bottom.add(commands,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
    }
    public void setPencilAction(PencilAction action){pencilAction=action;}
    public void setProjectionCancel(Runnable cancel){cancelProjection=java.util.Objects.requireNonNull(cancel);}
    public void setViewChanged(Runnable action){viewChanged=java.util.Objects.requireNonNull(action);}
    public boolean modelView(){return materialView.getSelectedIndex()==1;}
    public boolean pencilActive(){return pencil.isSelected()&&pencil.isEnabled();}
    public int pencilColor(){return paintColor.getRGB();}
    public int projectionPadding(){return modelView()?(int)projectionPadding.getValue():0;}
    private static int centerTile(double a,double b,double c){return (int)Math.floor(a/3+b/3+c/3);}
    private void changeTile(){
        tile=new EditorUvTile((Integer)tileU.getValue(),(Integer)tileV.getValue());
        stroke=null;lastPixel=null;canvas.repaint();
    }
    private void sample(Point mouse) {
        var r=imageRect();
        int x=(int)Math.floor((mouse.x-r[0])*material.width()/r[2]),y=(int)Math.floor((mouse.y-r[1])*material.height()/r[3]);
        if(x<0||y<0||x>=material.width()||y>=material.height()){lastPixel=null;return;}
        int startX=lastPixel==null?x:lastPixel.x,startY=lastPixel==null?y:lastPixel.y;
        int steps=Math.max(1,Math.max(Math.abs(x-startX),Math.abs(y-startY)));
        for(int i=0;i<=steps;i++){
            int px=startX+(int)Math.round((x-startX)*(double)i/steps),py=startY+(int)Math.round((y-startY)*(double)i/steps);
            stroke.add(py*material.width()+px);
            if(stroke.size()>65536){stroke=null;lastPixel=null;status.setText("Stroke exceeds 65536 texels");return;}
        }
        lastPixel=new Point(x,y);
        canvas.repaint();
    }
    private void bind(String key,JButton control) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key),key);
        getActionMap().put(key,new AbstractAction(){
            public void actionPerformed(ActionEvent event){if(control.isEnabled())control.doClick();}
        });
    }
    private static String faceLabel(EditorTopologyProject.Face face) {
        return "Face " + face.id() + " / " + face.part();
    }

    /** All rows are one line. Give Swing the widest visible label so its layout never renders
     * the entire face list to measure a single UV change. */
    private void fitFaceRows() {
        var metrics=faceList.getFontMetrics(faceList.getFont());
        EditorTopologyProject.Face widest=null;
        int width=-1;
        for (int row=0; row<faces.getSize(); row++) {
            var face=faces.getElementAt(row);
            int next=metrics.stringWidth(faceLabel(face));
            if (next>width) { width=next; widest=face; }
        }
        faceList.setPrototypeCellValue(widest);
    }

    private final class FaceRows extends AbstractListModel<EditorTopologyProject.Face> {
        private List<EditorTopologyProject.Face> source=List.of();
        private Visibility filter=ALL;
        private int[] visibleOrdinals;
        private java.util.Map<Long,Integer> rowsById;

        @Override public int getSize() { return visibleOrdinals==null ? source.size() : visibleOrdinals.length; }
        @Override public EditorTopologyProject.Face getElementAt(int row) {
            return source.get(visibleOrdinals==null ? row : visibleOrdinals[row]);
        }
        private int rowForOrdinal(int ordinal) {
            return visibleOrdinals==null ? ordinal : java.util.Arrays.binarySearch(visibleOrdinals,ordinal);
        }

        private int rowOf(long id) {
            int selected=faceList.getSelectedIndex();
            if (selected>=0 && selected<getSize() && getElementAt(selected).id()==id) return selected;
            if (id>=0 && id<source.size() && source.get((int)id).id()==id) return rowForOrdinal((int)id);
            if (rowsById==null) {
                rowsById=new java.util.HashMap<>();
                for (int row=0;row<getSize();row++) rowsById.put(getElementAt(row).id(),row);
            }
            return rowsById.getOrDefault(id,-1);
        }

        private void update(List<EditorTopologyProject.Face> next, Visibility visibility, boolean trackEdit) {
            if (visibility.equals(filter)) {
                if (source==next) return;
                if (next.size()==source.size() && updateSameOrder(next,trackEdit)) return;
            }
            int oldSize=getSize();
            var prototype=faceList.getPrototypeCellValue();
            boolean widestStillVisible=next==source && !filter.filtered() && prototype!=null && visibility.includes(prototype.id());
            source=next;
            filter=visibility;
            rowsById=null;
            visibleOrdinals=null;
            if (filter.filtered()) {
                int[] ordinals=new int[filter.isolated()!=null ? 1 : source.size()];
                int count=0;
                for (int i=0;i<source.size();i++) {
                    if (filter.includes(source.get(i).id())) ordinals[count++]=i;
                }
                visibleOrdinals=count==ordinals.length ? ordinals : java.util.Arrays.copyOf(ordinals,count);
            }
            if (!widestStillVisible) fitFaceRows();
            int common=Math.min(oldSize,getSize());
            if (oldSize>common) fireIntervalRemoved(this,common,oldSize-1);
            if (getSize()>common) fireIntervalAdded(this,common,getSize()-1);
            if (common>0) fireContentsChanged(this,0,common-1);
        }

        /** The state supplies immutable faces. Stable row IDs preserve filtering and sparse-ID lookup. */
        private boolean updateSameOrder(List<EditorTopologyProject.Face> next,boolean trackEdit) {
            int first=-1, last=-1;
            boolean labelChanged=false;
            for (int i=0;i<next.size();i++) {
                var old=source.get(i);
                var face=next.get(i);
                if (old==face) continue;
                if (old.id()!=face.id()) return false;
                if (!filter.includes(face.id())) continue;
                if (first<0) first=i;
                last=i;
                if (!old.part().equals(face.part())) labelChanged=true;
            }
            if(trackEdit && first>=0 && first==last)pendingPaintEdit=new FaceEdit(source.get(first),next.get(first));
            source=next;
            if (labelChanged) fitFaceRows();
            if (first>=0) fireContentsChanged(this,rowForOrdinal(first),rowForOrdinal(last));
            return true;
        }
    }
    private void filter(Visibility next) {
        visibility=next;
        observe(state,selectedFace,material,false);
        visibilityChanged.run();
    }
    public void observe(EditorTopologyProject.State value,Long face,EditorMeshMaterial displayed,boolean pending){
        observe(value,face,displayed,pending?"":null);
    }
    public void observe(EditorTopologyProject.State value,Long face,EditorMeshMaterial displayed,String preview){
        boolean pending=preview!=null;
        boolean trackEdit=value!=null && state!=null && state!=value && paintedState==state
            && material==displayed && paintedMaterial==displayed && paintedView!=null
            && state.sceneId().equals(value.sceneId()) && state.sourceFingerprint().equals(value.sourceFingerprint());
        if(state!=value || material!=displayed){
            paintedState=null;pendingPaintEdit=null;
            if(!trackEdit){paintedMaterial=null;paintedView=null;}
            if(value==null)paintedUv=null;
        }
        if(state!=value || !java.util.Objects.equals(selectedFace,face) || pending)previewIssue=null;
        if(state!=value || !java.util.Objects.equals(selectedFace,face) || pending){stroke=null;lastPixel=null;}
        if(state==null || value==null || !state.sceneId().equals(value.sceneId()) ||
            !state.sourceFingerprint().equals(value.sourceFingerprint())){
            visibility=ALL;tileU.setValue(0);tileV.setValue(0);
        }
        state=value;
        updating=true;
        try {
            faces.update(value==null ? List.of() : value.faces(),visibility,trackEdit);
            int row=face==null || !visibility.includes(face) ? -1 : faces.rowOf(face);
            selectedFace=row<0 ? null : face;
            if (row!=faceList.getSelectedIndex()) {
                if (row<0) faceList.clearSelection();
                else faceList.setSelectedIndex(row);
            }
        } finally {
            updating=false;
        }
        isolate.setEnabled(!pending && selectedFace!=null);hide.setEnabled(isolate.isEnabled());showAll.setEnabled(!pending && visibility.filtered());
        boolean bound=selectedFace!=null && displayed!=null && faceList.getSelectedValue()!=null &&
            faceList.getSelectedValue().material().equals(displayed.key());
        pencil.setEnabled(!pending && bound && pencilAction!=null);
        projectionPadding.setEnabled(!pending && bound);
        ink.setEnabled(pencil.isEnabled());
        if(!java.util.Objects.equals(material,displayed)){material=displayed;texture=null;if(displayed!=null)try{texture=ImageIO.read(new ByteArrayInputStream(displayed.png()));}catch(IOException bad){throw new UncheckedIOException(bad);}}
        var allowed=tile.forMaterial(material);
        tileU.setValue(allowed.u());tileV.setValue(allowed.v());
        tileU.setEnabled(!pending && (material==null || material.wrapS()!=33071));
        tileV.setEnabled(!pending && (material==null || material.wrapT()!=33071));
        faceTile.setEnabled(!pending && selectedFace!=null);
        status.setText(previewIssue!=null?previewIssue:pending?"PREVIEW / "+(preview.isEmpty()?"":preview+" / ")+"Apply or Cancel":
            "NATIVE DOCUMENT | "+(selectedFace==null?"Select a face":"Face "+selectedFace)+" | "+(displayed==null?"Unbound material":displayed.name()+" / "+displayed.width()+" x "+displayed.height()+" / "+displayed.origin())+" | Wheel zoom / middle-drag pan");canvas.repaint();
    }
    private double[] imageRect(){double size=Math.max(20,Math.min(canvas.getWidth()-40,canvas.getHeight()-40))*zoom;double w=texture==null?size:size*texture.getWidth()/Math.max(texture.getWidth(),texture.getHeight()),h=texture==null?size:size*texture.getHeight()/Math.max(texture.getWidth(),texture.getHeight());return new double[]{(canvas.getWidth()-w)/2+panX,(canvas.getHeight()-h)/2+panY,w,h};}
    private int x(double u,double[] r){return (int)Math.rint(r[0]+tile.imageU(u,material)*r[2]);}
    private int y(double v,double[] r){return (int)Math.rint(r[1]+tile.imageV(v,material)*r[3]);}
    private boolean displays(EditorTopologyProject.Face face){
        return material==null ? face.material().isEmpty() : face.material().equals(material.key());
    }
    private static final Color UV_EDGE=new Color(220,235,248,140),UV_SELECTED=new Color(255,219,80),UV_SEAM=new Color(255,96,135);
    private static final BasicStroke UV_THIN=new BasicStroke(1),UV_THICK=new BasicStroke(3);
    private static EditorTopologyProject.Corner corner(EditorTopologyProject.Face face,int index){
        return switch(index){case 0->face.a();case 1->face.b();default->face.c();};
    }
    private Rectangle selectionBounds(Long id,Graphics2D g){
        Rectangle canvasBounds=new Rectangle(0,0,canvas.getWidth(),canvas.getHeight());
        if(id==null)return new Rectangle();
        int row=faces.rowOf(id);
        if(row<0)return canvasBounds;
        return faceBounds(faces.getElementAt(row),g);
    }
    private Rectangle faceBounds(EditorTopologyProject.Face face,Graphics2D g){
        Rectangle canvasBounds=new Rectangle(0,0,canvas.getWidth(),canvas.getHeight());
        if(!displays(face))return new Rectangle();
        double[] r=imageRect();
        Rectangle bounds=null;
        for(int i=0;i<3;i++){
            var c=corner(face,i);
            int px=x(c.u(),r),py=y(c.v(),r);
            // Leave extreme projections to the original painter and avoid rectangle overflow.
            if(Math.abs((long)px)>100_000_000 || Math.abs((long)py)>100_000_000)return canvasBounds;
            var point=new Rectangle(px-7,py-7,15,15);
            if(bounds==null)bounds=point;else bounds.add(point);
            if((pinsVisible||seamsVisible) && hasCornerLabel(px,py,r)){
                int tx=labelX(px,r),ty=labelY(py,r);
                bounds.add(new Rectangle(tx,ty,12,15));
                var label=new java.awt.font.TextLayout(String.valueOf((char)('A'+i)),g.getFont(),g.getFontRenderContext());
                var ink=label.getPixelBounds(g.getFontRenderContext(),tx+1,ty+12);
                ink.grow(2,2);bounds.add(ink);
            }
        }
        return bounds.intersection(canvasBounds);
    }
    private void repaintArea(Graphics2D buffer,Rectangle area){
        if(area.isEmpty())return;
        var g=(Graphics2D)buffer.create();
        try{
            g.clip(area);g.setColor(canvas.getBackground());g.fill(area);
            paintTexture(g,area);
        }finally{g.dispose();}
    }
    private static boolean hasCornerLabel(int px,int py,double[] r){
        return px>=Math.floor(r[0]) && px<=Math.ceil(r[0]+r[2]) && py>=Math.floor(r[1]) && py<=Math.ceil(r[1]+r[3]);
    }
    private static int labelX(int px,double[] r){return (int)Math.max(r[0],Math.min(px+4,r[0]+r[2]-12));}
    private static int labelY(int py,double[] r){return (int)Math.max(r[1],Math.min(py-14,r[1]+r[3]-15));}
    private void paintTexture(Graphics2D original){paintTexture(original,null);}
    private void paintTexture(Graphics2D original,Rectangle damage){var g=(Graphics2D)original.create();try{
        double[] r=imageRect();int left=(int)r[0],top=(int)r[1],w=(int)r[2],h=(int)r[3];g.clipRect(left,top,w+1,h+1);
        for(int yy=top;yy<top+h;yy+=16)for(int xx=left;xx<left+w;xx+=16){g.setColor(((xx-left)/16+(yy-top)/16)%2==0?new Color(82,86,91):new Color(56,60,65));g.fillRect(xx,yy,16,16);}
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);if(texture!=null)g.drawImage(texture,left,top,w,h,null);
        var selected=faceList.getSelectedValue();
        if(stroke!=null && material!=null && selected!=null){
            var overlay=(Graphics2D)g.create();
            try{
                var clip=new Polygon();
                for(var c:List.of(selected.a(),selected.b(),selected.c()))clip.addPoint(x(c.u(),r),y(c.v(),r));
                overlay.clip(clip);
                overlay.setColor(strokeColor==0?Color.WHITE:new Color(strokeColor,true));
                for(int p:stroke){
                    int px=p%material.width(),py=p/material.width();
                    int x0=left+px*w/material.width(),y0=top+py*h/material.height();
                    int dx=Math.max(1,left+(px+1)*w/material.width()-x0),dy=Math.max(1,top+(py+1)*h/material.height()-y0);
                    if(strokeColor==0)overlay.drawRect(x0,y0,dx,dy);else overlay.fillRect(x0,y0,dx,dy);
                }
            }finally{overlay.dispose();}
        }
        if(state==null)return;for(int row=0;row<faces.getSize();row++){var face=faces.getElementAt(row);
            if(!displays(face))continue;
            boolean chosen=selectedFace!=null&&face.id()==selectedFace;
            int ax=x(face.a().u(),r),ay=y(face.a().v(),r),bx=x(face.b().u(),r),by=y(face.b().v(),r),cx=x(face.c().u(),r),cy=y(face.c().v(),r);
            if(damage!=null && (Math.max(ax,Math.max(bx,cx))<damage.x-4 || Math.min(ax,Math.min(bx,cx))>damage.x+damage.width+4
                    || Math.max(ay,Math.max(by,cy))<damage.y-4 || Math.min(ay,Math.min(by,cy))>damage.y+damage.height+4))continue;
            g.setColor(chosen?UV_SELECTED:UV_EDGE);g.setStroke(chosen?UV_THICK:UV_THIN);
            g.drawLine(ax,ay,bx,by);g.drawLine(bx,by,cx,cy);g.drawLine(cx,cy,ax,ay);
        }
        if(pinsVisible||seamsVisible){
            g.clipRect((int)r[0],(int)r[1],(int)Math.ceil(r[2])+1,(int)Math.ceil(r[3])+1);
            for(int row=0;row<faces.getSize();row++){
                var f=faces.getElementAt(row);if(!displays(f))continue;
                for(int index=0;index<3;index++){var c=corner(f,index);
                    boolean seam=seamsVisible && f.marked(index),pin=pinsVisible && c.pinned();
                    if(!seam && !pin)continue;
                    int px=x(c.u(),r),py=y(c.v(),r);
                    if(seam){
                        var end=corner(f,(index+1)%3);g.setColor(UV_SEAM);g.setStroke(UV_THICK);
                        g.drawLine(px,py,x(end.u(),r),y(end.v(),r));
                    }
                    if(pin){g.setColor(Color.BLACK);g.fillOval(px-5,py-5,10,10);g.setColor(Color.CYAN);g.fillOval(px-3,py-3,6,6);}
                }
            }
            if(selected!=null && displays(selected)){
                var corners=List.of(selected.a(),selected.b(),selected.c());
                if (seamsVisible && guideEdge>=0 && guideEdge<3) {
                    var start=corners.get(guideEdge);
                    var end=corners.get((guideEdge+1)%3);
                    g.setColor(Color.CYAN);
                    g.setStroke(new BasicStroke(2,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,1,new float[]{6,4},0));
                    g.drawLine(x(start.u(),r),y(start.v(),r),x(end.u(),r),y(end.v(),r));
                }
                int index=0;
                for(var c:corners){
                    int px=x(c.u(),r),py=y(c.v(),r);
                    if(hasCornerLabel(px,py,r)){
                        int tx=labelX(px,r);
                        int ty=labelY(py,r);
                        g.setColor(Color.BLACK);g.fillRect(tx,ty,12,15);g.setColor(Color.WHITE);g.drawString(String.valueOf((char)('A'+index)),tx+1,ty+12);
                    }
                    index++;
                }
            }
        }
    }finally{g.dispose();}}
    private void pick(Point point){if(state==null)return;double[] r=imageRect();
        double x=(point.x-r[0])/r[2],y=(point.y-r[1])/r[3];
        if(x<0 || x>1 || y<0 || y>1)return;
        double u=tile.nativeU(x,material),v=tile.nativeV(y,material);
        for(int i=faces.getSize()-1;i>=0;i--){var f=faces.getElementAt(i);if(material==null?!f.material().isEmpty():!f.material().equals(material.key()))continue;if(EditorUvTile.contains(f,u,v)){selection.accept(f.id());return;}}
    }
}
