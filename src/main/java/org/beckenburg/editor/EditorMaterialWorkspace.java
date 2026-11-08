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
    private final JButton isolate=new JButton("Isolate"), hide=new JButton("Hide"), showAll=new JButton("Show all");
    public record Visibility(Long isolated,java.util.Set<Long> hidden) {
        public Visibility { hidden=java.util.Set.copyOf(hidden); }
        public boolean includes(long face) { return (isolated==null || isolated==face) && !hidden.contains(face); }
        public boolean filtered() { return isolated!=null || !hidden.isEmpty(); }
    }
    private static final Visibility ALL=new Visibility(null,java.util.Set.of());
    private Visibility visibility=ALL;
    private boolean refreshRows;
    private final Runnable visibilityChanged;
    public int visibleFaces() { return faces.size(); }
    public Visibility visibility() { return visibility; }
    public interface PencilAction { void preview(long face,List<Integer> texels,int color); }
    private PencilAction pencilAction;
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
    private final DefaultListModel<EditorTopologyProject.Face> faces=new DefaultListModel<>();
    private final JList<EditorTopologyProject.Face> faceList=new JList<>(faces);
    private boolean updating;
    private final JLabel status=new JLabel("Open a native model to inspect its material UVs");
    private String previewIssue;
    public void previewIssue(String message){previewIssue=message;if(message!=null)status.setText(message);}
    private double zoom=1,panX,panY;private Point panStart;
    private final JPanel canvas=new JPanel(){protected void paintComponent(Graphics graphics){super.paintComponent(graphics);paintTexture((Graphics2D)graphics);}};
    public EditorMaterialWorkspace(JComponent settings,JComponent runtime,JComponent commands,LongConsumer selection){
        this(settings,runtime,commands,selection,()->{});
    }
    public EditorMaterialWorkspace(JComponent settings,JComponent runtime,JComponent commands,LongConsumer selection,Runnable visibilityChanged){
        super(new BorderLayout(8,8));this.selection=selection;this.visibilityChanged=visibilityChanged;setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        faceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);faceList.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){var label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);if(value instanceof EditorTopologyProject.Face f)label.setText("Face "+f.id()+" / "+f.part());return label;}});
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
        var center=new JPanel(new BorderLayout(3,5));var heading=new JPanel(new BorderLayout());heading.add(new JLabel("TEXTURE / UV VIEW"),BorderLayout.WEST);heading.add(reset,BorderLayout.EAST);center.add(heading,BorderLayout.NORTH);center.add(canvas,BorderLayout.CENTER);
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
        paintTools.add(pencil);paintTools.add(ink);heading.add(paintTools,BorderLayout.CENTER);
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
            stroke=null;lastPixel=null;canvas.repaint();
        }});
        var right=new JPanel(new BorderLayout(4,8));var properties=new JScrollPane(new Properties(settings));properties.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);right.add(properties,BorderLayout.CENTER);right.add(runtime,BorderLayout.SOUTH);right.setPreferredSize(new Dimension(300,500));right.setMinimumSize(new Dimension(240,200));
        var split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,center,right);split.setResizeWeight(.7);split.setBorder(null);add(left,BorderLayout.WEST);add(split,BorderLayout.CENTER);
        var bottom=new JPanel(new BorderLayout(4,4));bottom.add(status,BorderLayout.NORTH);bottom.add(commands,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
    }
    public void setPencilAction(PencilAction action){pencilAction=action;}
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
    private void filter(Visibility next) {
        visibility=next;
        refreshRows=true;
        observe(state,selectedFace,material,false);
        visibilityChanged.run();
    }
    public void observe(EditorTopologyProject.State value,Long face,EditorMeshMaterial displayed,boolean pending){
        if(state!=value || !java.util.Objects.equals(selectedFace,face) || pending)previewIssue=null;
        if(state!=value || !java.util.Objects.equals(selectedFace,face) || pending){stroke=null;lastPixel=null;}
        if(state==null || value==null || !state.sceneId().equals(value.sceneId()) ||
            !state.sourceFingerprint().equals(value.sourceFingerprint())){
            visibility=ALL;tileU.setValue(0);tileV.setValue(0);
        }
        if(state!=value || refreshRows){state=value;refreshRows=false;updating=true;faces.clear();if(value!=null)for(var f:value.faces())if(visibility.includes(f.id()))faces.addElement(f);updating=false;}
        selectedFace=face!=null && visibility.includes(face) ? face : null;
        updating=true;faceList.clearSelection();for(int i=0;i<faces.size();i++)if(selectedFace!=null&&faces.get(i).id()==selectedFace)faceList.setSelectedIndex(i);updating=false;
        isolate.setEnabled(!pending && selectedFace!=null);hide.setEnabled(isolate.isEnabled());showAll.setEnabled(!pending && visibility.filtered());
        boolean bound=selectedFace!=null && displayed!=null && faceList.getSelectedValue()!=null &&
            faceList.getSelectedValue().material().equals(displayed.key());
        pencil.setEnabled(!pending && bound && pencilAction!=null);
        ink.setEnabled(pencil.isEnabled());
        if(!java.util.Objects.equals(material,displayed)){material=displayed;texture=null;if(displayed!=null)try{texture=ImageIO.read(new ByteArrayInputStream(displayed.png()));}catch(IOException bad){throw new UncheckedIOException(bad);}}
        var allowed=tile.forMaterial(material);
        tileU.setValue(allowed.u());tileV.setValue(allowed.v());
        tileU.setEnabled(!pending && (material==null || material.wrapS()!=33071));
        tileV.setEnabled(!pending && (material==null || material.wrapT()!=33071));
        faceTile.setEnabled(!pending && selectedFace!=null);
        status.setText(previewIssue!=null?previewIssue:(pending?"PREVIEW / Apply or Cancel":"NATIVE DOCUMENT")+" | "+(selectedFace==null?"Select a face":"Face "+selectedFace)+" | "+(displayed==null?"Unbound material":displayed.name()+" / "+displayed.width()+" x "+displayed.height()+" / "+displayed.origin())+" | Wheel zoom / middle-drag pan");canvas.repaint();
    }
    private double[] imageRect(){double size=Math.max(20,Math.min(canvas.getWidth()-40,canvas.getHeight()-40))*zoom;double w=texture==null?size:size*texture.getWidth()/Math.max(texture.getWidth(),texture.getHeight()),h=texture==null?size:size*texture.getHeight()/Math.max(texture.getWidth(),texture.getHeight());return new double[]{(canvas.getWidth()-w)/2+panX,(canvas.getHeight()-h)/2+panY,w,h};}
    private int x(double u,double[] r){return (int)Math.rint(r[0]+tile.imageU(u,material)*r[2]);}
    private int y(double v,double[] r){return (int)Math.rint(r[1]+tile.imageV(v,material)*r[3]);}
    private void paintTexture(Graphics2D original){var g=(Graphics2D)original.create();try{
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
        if(state==null)return;for(int row=0;row<faces.size();row++){var face=faces.get(row);
            if(material==null?!face.material().isEmpty():!face.material().equals(material.key()))continue;
            boolean chosen=selectedFace!=null&&face.id()==selectedFace;g.setColor(chosen?new Color(255,219,80):new Color(220,235,248,140));g.setStroke(new BasicStroke(chosen?3:1));var corners=List.of(face.a(),face.b(),face.c());for(int i=0;i<3;i++){var a=corners.get(i);var b=corners.get((i+1)%3);g.drawLine(x(a.u(),r),y(a.v(),r),x(b.u(),r),y(b.v(),r));}
        }
    }finally{g.dispose();}}
    private void pick(Point point){if(state==null)return;double[] r=imageRect();
        double x=(point.x-r[0])/r[2],y=(point.y-r[1])/r[3];
        if(x<0 || x>1 || y<0 || y>1)return;
        double u=tile.nativeU(x,material),v=tile.nativeV(y,material);
        for(int i=faces.size()-1;i>=0;i--){var f=faces.get(i);if(material==null?!f.material().isEmpty():!f.material().equals(material.key()))continue;if(EditorUvTile.contains(f,u,v)){selection.accept(f.id());return;}}
    }
}
