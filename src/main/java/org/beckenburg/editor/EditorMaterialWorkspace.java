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
    private final LongConsumer selection;
    private final DefaultListModel<EditorTopologyProject.Face> faces=new DefaultListModel<>();
    private final JList<EditorTopologyProject.Face> faceList=new JList<>(faces);
    private boolean updating;
    private final JLabel status=new JLabel("Open a native model to inspect its material UVs");
    private double zoom=1,panX,panY;private Point panStart;
    private final JPanel canvas=new JPanel(){protected void paintComponent(Graphics graphics){super.paintComponent(graphics);paintTexture((Graphics2D)graphics);}};
    public EditorMaterialWorkspace(JComponent settings,JComponent runtime,JComponent commands,LongConsumer selection){
        super(new BorderLayout(8,8));this.selection=selection;setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        faceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);faceList.setCellRenderer(new DefaultListCellRenderer(){public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){var label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);if(value instanceof EditorTopologyProject.Face f)label.setText("Face "+f.id()+" / "+f.part());return label;}});
        faceList.addListSelectionListener(e->{if(!updating&&!e.getValueIsAdjusting()&&faceList.getSelectedValue()!=null)selection.accept(faceList.getSelectedValue().id());});
        var left=new JPanel(new BorderLayout(4,4));left.add(new JLabel("NATIVE FACE ASSIGNMENTS"),BorderLayout.NORTH);left.add(new JScrollPane(faceList),BorderLayout.CENTER);left.setPreferredSize(new Dimension(165,480));
        canvas.setBackground(new Color(29,33,39));canvas.setPreferredSize(new Dimension(520,500));
        var reset=new JButton("Fit texture");reset.addActionListener(e->{zoom=1;panX=panY=0;canvas.repaint();});
        var center=new JPanel(new BorderLayout(3,5));var heading=new JPanel(new BorderLayout());heading.add(new JLabel("TEXTURE / UV VIEW"),BorderLayout.WEST);heading.add(reset,BorderLayout.EAST);center.add(heading,BorderLayout.NORTH);center.add(canvas,BorderLayout.CENTER);
        canvas.addMouseWheelListener(e->{zoom=Math.max(.25,Math.min(16,zoom*Math.pow(1.15,-e.getPreciseWheelRotation())));canvas.repaint();});
        var mouse=new MouseAdapter(){public void mousePressed(MouseEvent e){if(SwingUtilities.isMiddleMouseButton(e))panStart=e.getPoint();else if(SwingUtilities.isLeftMouseButton(e))pick(e.getPoint());}public void mouseDragged(MouseEvent e){if(panStart!=null){panX+=e.getX()-panStart.x;panY+=e.getY()-panStart.y;panStart=e.getPoint();canvas.repaint();}}public void mouseReleased(MouseEvent e){panStart=null;}};canvas.addMouseListener(mouse);canvas.addMouseMotionListener(mouse);
        var right=new JPanel(new BorderLayout(4,8));var properties=new JScrollPane(new Properties(settings));properties.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);right.add(properties,BorderLayout.CENTER);right.add(runtime,BorderLayout.SOUTH);right.setPreferredSize(new Dimension(300,500));right.setMinimumSize(new Dimension(240,200));
        var split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,center,right);split.setResizeWeight(.7);split.setBorder(null);add(left,BorderLayout.WEST);add(split,BorderLayout.CENTER);
        var bottom=new JPanel(new BorderLayout(4,4));bottom.add(status,BorderLayout.NORTH);bottom.add(commands,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
    }
    public void observe(EditorTopologyProject.State value,Long face,EditorMeshMaterial displayed,boolean pending){
        if(state!=value){state=value;updating=true;faces.clear();if(value!=null)for(var f:value.faces())faces.addElement(f);updating=false;}
        selectedFace=face;updating=true;faceList.clearSelection();for(int i=0;i<faces.size();i++)if(face!=null&&faces.get(i).id()==face)faceList.setSelectedIndex(i);updating=false;
        if(!java.util.Objects.equals(material,displayed)){material=displayed;texture=null;if(displayed!=null)try{texture=ImageIO.read(new ByteArrayInputStream(displayed.png()));}catch(IOException bad){throw new UncheckedIOException(bad);}}
        status.setText((pending?"PREVIEW / Apply or Cancel":"NATIVE DOCUMENT")+" | "+(face==null?"Select a face":"Face "+face)+" | "+(displayed==null?"Unbound material":displayed.name()+" / "+displayed.width()+" x "+displayed.height()+" / "+displayed.origin())+" | Wheel zoom / middle-drag pan");canvas.repaint();
    }
    private double[] imageRect(){double size=Math.max(20,Math.min(canvas.getWidth()-40,canvas.getHeight()-40))*zoom;double w=texture==null?size:size*texture.getWidth()/Math.max(texture.getWidth(),texture.getHeight()),h=texture==null?size:size*texture.getHeight()/Math.max(texture.getWidth(),texture.getHeight());return new double[]{(canvas.getWidth()-w)/2+panX,(canvas.getHeight()-h)/2+panY,w,h};}
    private int x(double u,double[] r){return (int)Math.round(r[0]+u*r[2]);}
    private int y(double v,double[] r){return (int)Math.round(r[1]+(material!=null&&material.origin()==EditorMeshMaterial.Origin.TOP_LEFT?v:1-v)*r[3]);}
    private void paintTexture(Graphics2D original){var g=(Graphics2D)original.create();try{
        double[] r=imageRect();int left=(int)r[0],top=(int)r[1],w=(int)r[2],h=(int)r[3];g.clipRect(left,top,w+1,h+1);
        for(int yy=top;yy<top+h;yy+=16)for(int xx=left;xx<left+w;xx+=16){g.setColor(((xx-left)/16+(yy-top)/16)%2==0?new Color(82,86,91):new Color(56,60,65));g.fillRect(xx,yy,16,16);}
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);if(texture!=null)g.drawImage(texture,left,top,w,h,null);
        if(state==null)return;for(var face:state.faces()){
            if(material==null?!face.material().isEmpty():!face.material().equals(material.key()))continue;
            boolean chosen=selectedFace!=null&&face.id()==selectedFace;g.setColor(chosen?new Color(255,219,80):new Color(220,235,248,140));g.setStroke(new BasicStroke(chosen?3:1));var corners=List.of(face.a(),face.b(),face.c());for(int i=0;i<3;i++){var a=corners.get(i);var b=corners.get((i+1)%3);g.drawLine(x(a.u(),r),y(a.v(),r),x(b.u(),r),y(b.v(),r));}
        }
    }finally{g.dispose();}}
    private void pick(Point point){if(state==null)return;double[] r=imageRect();double u=(point.x-r[0])/r[2],v=(point.y-r[1])/r[3];if(material==null||material.origin()==EditorMeshMaterial.Origin.BOTTOM_LEFT)v=1-v;
        for(int i=state.faces().size()-1;i>=0;i--){var f=state.faces().get(i);if(material==null?!f.material().isEmpty():!f.material().equals(material.key()))continue;double a=cross(f.a(),f.b(),u,v),b=cross(f.b(),f.c(),u,v),c=cross(f.c(),f.a(),u,v);double area=cross(f.a(),f.b(),f.c().u(),f.c().v());if(Math.abs(area)>1e-15&&(a>=0&&b>=0&&c>=0||a<=0&&b<=0&&c<=0)){selection.accept(f.id());return;}}
    }
    private static double cross(EditorTopologyProject.Corner a,EditorTopologyProject.Corner b,double u,double v){return (b.u()-a.u())*(v-a.v())-(b.v()-a.v())*(u-a.u());}
}
