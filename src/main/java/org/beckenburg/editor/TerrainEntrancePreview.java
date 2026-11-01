package org.beckenburg.editor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Orbiting inspection preview drawn from actual emitted mesh triangles.
 * This is explicitly an authoring preview, not an in-game frame/render pass.
 * OBJ export writes geometry/UVs only, never an invented fallback texture.
 */
public final class TerrainEntrancePreview extends JPanel {
    private final TerrainEntranceMesh.Output output;
    private final MeshCanvas canvas;
    private final JCheckBox wires=new JCheckBox("Wireframe",true);
    private TerrainEntrancePreview(TerrainEntranceMesh.Output output){
        super(new BorderLayout(4,4));
        this.output=output;
        this.canvas=new MeshCanvas();
        setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
        JButton export=new JButton("EXPORT ACTUAL OBJ GEOMETRY");
        export.addActionListener(e->chooseExport(this,output));
        top.add(export);top.add(wires);
        wires.addActionListener(e->canvas.repaint());
        JButton reset=new JButton("RESET CAMERA");
        reset.addActionListener(e->{canvas.yaw=Math.toRadians(42);
            canvas.pitch=Math.toRadians(26);canvas.zoom=1;canvas.repaint();});
        top.add(reset);add(top,BorderLayout.NORTH);
        canvas.setPreferredSize(new Dimension(820,600));
        add(canvas,BorderLayout.CENTER);
        var m=output.mesh();
        String details;
        if(m.name().startsWith("Railway_")){
            int ballast=0,rails=0,sleepers=0;
            for(var tri:m.triangles()){
                switch(tri.part()){
                    case "rail_ballast" -> ballast++;
                    case "rail_left","rail_right" -> rails++;
                    case "rail_sleepers" -> sleepers++;
                }
            }
            details=" | ballast faces "+ballast+" | rail faces "+rails+
                " | sleeper faces "+sleepers;
        }else details=" | source leaves "+output.sourceLeaves()
            +" | ramp "+output.rampTriangles()+" | tab "+output.tabTriangles()
            +" | cave wall segments "+output.exposedWallSegments();
        JLabel foot=new JLabel("REAL TRIANGLES — editor geometry inspection, not a gameplay screenshot"
            +" | faces "+m.triangles().size()+details);
        add(foot,BorderLayout.SOUTH);
    }
    public static JFrame open(Window owner,TerrainEntranceMesh.Output output){
        return open(owner,output,"Authored Terrain Mesh Inspection");
    }
    public static JFrame open(Window owner,TerrainEntranceMesh.Output output,String title){
        JFrame frame=new JFrame("Level Editor Kit — "+title);
        frame.setContentPane(new TerrainEntrancePreview(output));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(660,440));frame.setSize(1040,730);
        frame.setLocationRelativeTo(owner);frame.setVisible(true);
        return frame;
    }
    private static String outputFileName(String meshName){
        String clean=meshName.replaceAll("[^A-Za-z0-9_-]","_");
        return (clean.isBlank()?"authored-terrain":clean)+".obj";
    }
    public static void chooseExport(java.awt.Component owner,TerrainEntranceMesh.Output result){
        JFileChooser chooser=new JFileChooser();
        chooser.setDialogTitle("Export real authored mesh (geometry-only OBJ)");
        chooser.setSelectedFile(new java.io.File(outputFileName(result.mesh().name())));
        if(chooser.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(!path.getFileName().toString().toLowerCase().endsWith(".obj"))
            path=path.resolveSibling(path.getFileName()+".obj");
        if(java.nio.file.Files.exists(path)&&JOptionPane.showConfirmDialog(owner,
            "Replace existing OBJ?\n"+path,
            "Confirm non-destructive export",JOptionPane.YES_NO_OPTION)
            !=JOptionPane.YES_OPTION)return;
        try{
            TerrainEntranceMesh.writeObj(result.mesh(),path);
            var imported=EditorMeshPipeline.read(path);
            if(imported.triangles().size()!=result.mesh().triangles().size())
                throw new IOException("OBJ failed final disk readback");
            JOptionPane.showMessageDialog(owner,"Verified "+imported.triangles().size()
                +" genuine mesh triangles at\n"+path+"\nNo source assets were modified.",
                "OBJ geometry exported",JOptionPane.INFORMATION_MESSAGE);
        }catch(IOException|RuntimeException failure){
            JOptionPane.showMessageDialog(owner,failure.getMessage(),
                "OBJ export failed",JOptionPane.ERROR_MESSAGE);
        }
    }
    private final class MeshCanvas extends JPanel {
        private double yaw=Math.toRadians(42),pitch=Math.toRadians(26),zoom=1;
        private int dragX,dragY;
        private final double cx,cy,cz,span;
        MeshCanvas(){
            setBackground(new Color(20,25,30));
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0;
            double x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            for(var tri:output.mesh().triangles()){
                for(var v:new EditorMeshPipeline.Vertex[]{tri.a(),tri.b(),tri.c()}){
                    x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());
                    y0=Math.min(y0,v.y());y1=Math.max(y1,v.y());
                    z0=Math.min(z0,v.z());z1=Math.max(z1,v.z());
                }
            }
            cx=(x0+x1)*.5;cy=(y0+y1)*.5;cz=(z0+z1)*.5;
            span=Math.max(1,Math.max(x1-x0,Math.max((y1-y0)*1.5,z1-z0)));
            MouseAdapter mouse=new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){dragX=e.getX();dragY=e.getY();}
                @Override public void mouseDragged(MouseEvent e){
                    yaw+=(e.getX()-dragX)*.012;
                    pitch=Math.max(-1.25,Math.min(1.25,pitch+(e.getY()-dragY)*.010));
                    dragX=e.getX();dragY=e.getY();repaint();
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e){
                    zoom=Math.max(.15,Math.min(8,zoom*Math.pow(1.12,-e.getPreciseWheelRotation())));
                    repaint();
                }
            };
            addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
        }
        private double[] camera(EditorMeshPipeline.Vertex v,double scale){
            double dx=v.x()-cx,dy=v.y()-cy,dz=v.z()-cz;
            double x=dx*Math.cos(yaw)-dz*Math.sin(yaw);
            double ahead=dx*Math.sin(yaw)+dz*Math.cos(yaw);
            double y=dy*Math.cos(pitch)-ahead*Math.sin(pitch);
            double depth=dy*Math.sin(pitch)+ahead*Math.cos(pitch);
            return new double[]{getWidth()*.5+x*scale,getHeight()*.51-y*scale,depth};
        }
        @Override protected void paintComponent(Graphics base){
            super.paintComponent(base);
            Graphics2D g=(Graphics2D)base.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            double scale=Math.min(getWidth(),getHeight())*.72*zoom/span;
            record Face(int[] xs,int[] ys,double depth,String part,double shade){}
            List<Face> faces=new ArrayList<>();
            for(var tri:output.mesh().triangles()){
                double[] a=camera(tri.a(),scale),b=camera(tri.b(),scale),
                         c=camera(tri.c(),scale);
                double cross=(b[0]-a[0])*(c[1]-a[1])
                    -(b[1]-a[1])*(c[0]-a[0]);
                double shade=Math.max(.58,Math.min(1.05,.75+Math.signum(cross)*.15));
                faces.add(new Face(new int[]{(int)a[0],(int)b[0],(int)c[0]},
                    new int[]{(int)a[1],(int)b[1],(int)c[1]},
                    (a[2]+b[2]+c[2])/3,tri.part(),shade));
            }
            faces.sort(Comparator.comparingDouble(Face::depth).reversed());
            for(var face:faces){
                Color color=face.part().startsWith("surface")?new Color(86,123,94):
                    face.part().startsWith("tab_")?new Color(166,139,78):
                    face.part().startsWith("ramp_")?new Color(93,137,164):
                    face.part().startsWith("rock_edge")?new Color(132,105,90):
                    new Color(115,118,130);
                g.setColor(new Color((int)Math.min(255,color.getRed()*face.shade()),
                    (int)Math.min(255,color.getGreen()*face.shade()),
                    (int)Math.min(255,color.getBlue()*face.shade())));
                g.fillPolygon(face.xs(),face.ys(),3);
                if(wires.isSelected()){
                    g.setColor(new Color(22,27,32,110));g.setStroke(new BasicStroke(.5f));
                    g.drawPolygon(face.xs(),face.ys(),3);
                }
            }
            g.setColor(new Color(210,216,219));
            g.drawString("Drag: orbit    Wheel: zoom    Mesh from saved terrain/query geometry",12,20);
            if(!output.warnings().isEmpty())
                g.drawString("Diagnostics: "+String.join("; ",output.warnings()),12,
                    getHeight()-13);
            g.dispose();
        }
    }
}
