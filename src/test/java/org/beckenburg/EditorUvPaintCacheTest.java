package org.beckenburg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.beckenburg.editor.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Compare actual retained Swing paints against the direct Java2D painter after live view changes. */
public final class EditorUvPaintCacheTest {
    private static Field field(String name)throws Exception {
        var f=EditorMaterialWorkspace.class.getDeclaredField(name);f.setAccessible(true);return f;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static EditorMeshMaterial material(int color,EditorMeshMaterial.Origin origin)throws Exception {
        var image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)image.setRGB(x,y,((x+y)%2==0?0xff_102030:color));
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        return new EditorMeshMaterial("paint","paint",out.toByteArray(),new EditorMeshMaterial.Color(1,1,1,1),
            33648,10497,EditorMeshMaterial.Alpha.OPAQUE,.5,origin,Map.of());
    }
    private static State state(EditorMeshMaterial material)throws Exception {
        var seed=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("paint",List.of(),0),
            "0".repeat(64),Path.of("paint-test.btop")).snapshot();
        var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();
        for(int i=0;i<12;i++){
            int base=i*3;double u=(i%3)*.32-.2,v=(i/3)*.19;
            vertices.add(new Vertex(base,"test",i,0,0));vertices.add(new Vertex(base+1,"test",i,1,0));vertices.add(new Vertex(base+2,"test",i,0,1));
            faces.add(new Face(1000+i*17,"test",new Corner(base,u,v,null,i%2==0),new Corner(base+1,u+.7,v),
                new Corner(base+2,u+.3,v+.8),"paint",i%8));
        }
        return new State(seed.sceneId(),seed.sourceFingerprint(),"paint",0,vertices,faces,List.of(material));
    }
    private static <T> T find(Container root,Class<T> type,String text){
        for(var c:root.getComponents()){
            if(type.isInstance(c)&&(text==null||c instanceof JButton b&&text.equals(b.getText())))return type.cast(c);
            if(c instanceof Container nested){T found=find(nested,type,text);if(found!=null)return found;}
        }return null;
    }
    private static int comparisons;
    private static boolean smooth,lcd;
    private static void parity(EditorMaterialWorkspace view,String name,double scale,double offset,boolean partial)throws Exception {
        var canvas=(JPanel)field("canvas").get(view);
        var direct=EditorMaterialWorkspace.class.getDeclaredMethod("paintTexture",Graphics2D.class);direct.setAccessible(true);
        for(int type:new int[]{BufferedImage.TYPE_INT_ARGB,BufferedImage.TYPE_INT_RGB}){
            var actual=new BufferedImage(720,660,type);var expected=new BufferedImage(720,660,type);
            for(int arm=0;arm<2;arm++){
                var g=(arm==0?actual:expected).createGraphics();
                try{
                    g.translate(offset,offset);g.scale(scale,scale);
                    if(smooth)g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    if(lcd)g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                    if(partial)g.setClip(61,53,131,127);
                    if(arm==0)canvas.paint(g);
                    else{
                        g.clipRect(0,0,canvas.getWidth(),canvas.getHeight());
                        g.setFont(canvas.getFont());g.setColor(canvas.getBackground());g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
                        direct.invoke(view,g);
                    }
                }finally{g.dispose();}
            }
            check(Arrays.equals(actual.getRGB(0,0,720,660,null,0,720),expected.getRGB(0,0,720,660,null,0,720)),
                "Retained/direct mismatch: "+name+" scale="+scale+" offset="+offset+" partial="+partial+" type="+type);
            comparisons++;
        }
    }
    private static void run()throws Exception {
        var material=material(0xff_a05020,EditorMeshMaterial.Origin.BOTTOM_LEFT);var base=state(material);
        var view=new EditorMaterialWorkspace(new JPanel(),new JPanel(),new JPanel(),id->{});
        var canvas=(JPanel)field("canvas").get(view);canvas.setSize(520,500);canvas.setFont(new Font(Font.DIALOG,Font.PLAIN,12));
        view.observe(base,1000L,material,false);
        parity(view,"first",1,0,false);Object retained=field("paintedUv").get(view);
        check(retained!=null,"No retained unscaled canvas");
        parity(view,"reuse",1,0,false);check(retained==field("paintedUv").get(view),"Unchanged repaint replaced its image");
        view.showUvGuides(true,true);view.selectGuideEdge(2);parity(view,"pins/seams/guide",1,0,false);
        view.observe(base,1017L,material,false);parity(view,"selection",1,0,false);
        find(view,JButton.class,"Hide").doClick(0);parity(view,"hidden selection",1,0,false);
        view.observe(base,1034L,material,false);find(view,JButton.class,"Isolate").doClick(0);parity(view,"isolate",1,0,false);
        find(view,JButton.class,"Show all").doClick(0);parity(view,"show all",1,0,false);
        var faces=new ArrayList<>(base.faces());var f=faces.get(2);
        faces.set(2,new Face(f.id(),f.part(),new Corner(f.a().vertex(),f.a().u()+.13,f.a().v(),f.a().normal(),true),f.b(),f.c(),f.material(),f.seamMask()));
        var preview=new State(base.sceneId(),base.sourceFingerprint(),base.name(),0,base.vertices(),faces,base.materials());
        view.observe(preview,1034L,material,"UV");check(field("paintedState").get(view)==null,"Preview retained old native state");
        parity(view,"preview",1,0,false);view.observe(base,1034L,material,false);parity(view,"cancel",1,0,false);
        field("zoom").setDouble(view,1.75);field("panX").setDouble(view,-43.25);field("panY").setDouble(view,12.5);
        parity(view,"pan/zoom",1,0,false);field("tile").set(view,new EditorUvTile(1,-1));parity(view,"mirrored tile",1,0,false);
        var replacement=material(0xff_20a050,EditorMeshMaterial.Origin.TOP_LEFT);
        view.observe(base,1034L,replacement,false);parity(view,"texture/origin",1,0,false);
        field("stroke").set(view,new LinkedHashSet<>(List.of(0,1,8,9)));field("strokeColor").setInt(view,0xff00ffff);
        parity(view,"active pencil",1,0,false);check(field("paintedUv").get(view)==null,"Active pencil retained stale paint");
        field("stroke").set(view,null);parity(view,"pencil cancel",1,0,false);
        canvas.setSize(480,460);canvas.setFont(new Font(Font.MONOSPACED,Font.BOLD,16));canvas.setBackground(new Color(7,13,19));
        parity(view,"resize/font/background",1,0,false);
        parity(view,"integer translation",1,11,false);parity(view,"partial clip",1,11,true);
        smooth=true;parity(view,"antialias hint change",1,0,false);smooth=false;parity(view,"restore rendering hints",1,0,false);
        lcd=true;parity(view,"LCD text fallback",1,0,false);check(field("paintedUv").get(view)==null,"LCD text did not retain direct rendering");lcd=false;
        parity(view,"HiDPI fallback",1.25,0,false);check(field("paintedUv").get(view)==null,"HiDPI did not fall back");
        parity(view,"fractional fallback",1,.5,false);parity(view,"restored unscaled",1,0,false);
        canvas.setSize(2049,2048);parity(view,"oversize fallback",1,0,false);check(field("paintedUv").get(view)==null,"Oversize image retained");
        view.observe(null,null,null,false);check(field("paintedUv").get(view)==null&&field("paintedState").get(view)==null,"Closed document retained native state/image");
        System.out.println("PASS "+comparisons+" retained/direct whole-image pairs: sparse faces, overlaps, guides, filters, preview/cancel, texture, pencil, pan/zoom, tile, resize, clips, HiDPI fallback and close");
    }
    public static void main(String[] args)throws Exception {SwingUtilities.invokeAndWait(()->{try{run();}catch(Exception e){throw new RuntimeException(e);}});}
}
