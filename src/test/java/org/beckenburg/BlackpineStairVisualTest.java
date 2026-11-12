package org.beckenburg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Stair;
import org.beckenburg.LevelDefinition.Vec;

/** Real game renderer + swept collision proof of the authored Blackpine loft staircase.
 * No concept art, geometric placeholder, synthetic screenshot, or render-only fake opening.
 */
public final class BlackpineStairVisualTest {
    private BlackpineStairVisualTest() { }
    private static void check(boolean b,String reason) {
        if(!b)throw new AssertionError(reason);
    }
    private static BufferedImage capture(HouseMap map,Vec feet,double yaw,double pitch){
        SceneRenderer camera=new SceneRenderer();
        camera.setRenderSize(480,300);
        camera.x=feet.x();camera.z=feet.z();
        camera.eyeHeight=feet.y()+1.62;
        camera.yaw=yaw;camera.pitch=pitch;
        camera.lamp=true;camera.lampIntensity=1.6;
        camera.renderExactFrame(()->camera.drawPractice(map,List.of(),0));
        BufferedImage image=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.drawImage(camera.image(),0,0,480,300,null);
        g.dispose();
        return image;
    }
    private static double coord(Object triangle,String corner,String axis)throws Exception{
        return ((Number)LevelSceneSource.read(LevelSceneSource.read(triangle,corner),axis)).doubleValue();
    }
    /** Inspect *emitted* native rail triangles rather than assuming an unbroken
     * line of handrail on paper still leaves the actual staircase accessible. */
    private static boolean railAt(List<?> triangles,double x)throws Exception{
        for(Object triangle:triangles){
            double a=coord(triangle,"a","x"),b=coord(triangle,"b","x"),c=coord(triangle,"c","x");
            if(Math.min(a,Math.min(b,c))<=x+.02&&Math.max(a,Math.max(b,c))>=x-.02)return true;
        }
        return false;
    }
    public static void main(String[] args)throws Exception{
        HouseMap map=BlackpineLevel.map();
        var level=map.level();
        Stair stair=level.stairs().get(0);
        check(stair.a()==0&&stair.b()==11,"real mill stair joins the work floor to inspection loft");
        double rise=stair.y1()-stair.y0();
        check(Math.abs(rise-3)<.001,"inspection loft remains a true raised storey");
        Vec at=new Vec(stair.x0(),stair.y0(),stair.z0()),mid=null;
        for(int i=1;i<=120;i++){
            double t=i/120d;
            at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,
                stair.z0()+(stair.z1()-stair.z0())*t);
            check(at!=null,"real capsule must traverse every stair section, step "+i);
            if(i==48)mid=at;
        }
        check(mid!=null&&Math.abs(at.y()-level.room(11).y())<.02,
            "actual collision path reaches loft floor at the correct height");
        check(level.floorVoid(stair.x1(),stair.y1(),stair.z1()-.25)
            &&level.floorVoid(stair.x1(),stair.y1(),stair.z1()),
            "final riser and stair endpoint are exposed by real architectural opening");
        check(!level.floorVoid(stair.x1(),stair.y1(),stair.z1()+.25),
            "upper landing retains solid structural floor");
        check(level.sockets().stream().noneMatch(s->s.room()==11&&s.type().equals("enclosure")),
            "loft must not add a second ring of fence models/colliders");
        check(level.guards().size()==2,"only the two supported upper-deck opening guards remain");
        for(var guard:level.guards()){
            check(guard.y()==level.room(11).y(),"guard elevation matches its supporting loft");
            check(Math.min(guard.z0(),guard.z1())>=level.room(11).z0()-.001,
                "upper-deck guard must not hang above the unsupported lower stairs");
            check(Math.max(guard.z0(),guard.z1())<=level.room(11).z1()+.001,
                "guard must end on the upper deck");
        }
        var southRail=new java.util.ArrayList<Object>();
        var wall=SceneRenderer.class.getDeclaredMethod("levelWall",List.class,
            LevelDefinition.class,LevelDefinition.Room.class,boolean.class,
            double.class,double.class,double.class);
        wall.setAccessible(true);
        var loft=level.room(11);
        wall.invoke(null,southRail,level,loft,true,loft.z0(),loft.x0(),loft.x1());
        check(railAt(southRail,47.0)&&railAt(southRail,51.7),
            "real renderer keeps rails at both exposed sections of the loft edge");
        check(!railAt(southRail,stair.x1()),
            "real renderer must leave the stairwell entrance open, not bridge it with a rail");
        for(double offset:new double[]{-.15,.15}){
            Vec lane=new Vec(stair.x0()+offset,stair.y0(),stair.z0());
            for(int i=1;i<=120;i++){
                double t=i/120d;
                lane=map.resolveMove(lane,stair.x1()+offset,
                    stair.z0()+(stair.z1()-stair.z0())*t);
                check(lane!=null,"off-centre stair lane remains accessible, offset "+offset+" step "+i);
            }
        }
        for(int i=119;i>=0;i--){
            double t=i/120d;
            at=map.resolveMove(at,stair.x0()+(stair.x1()-stair.x0())*t,
                stair.z0()+(stair.z1()-stair.z0())*t);
            check(at!=null,"descending path remains supported, step "+i);
        }
        var beneath=capture(map,mid,0,-.10);
        var landing=capture(map,new Vec(stair.x1(),stair.y1(),stair.z1()),Math.PI,-.16);
        int diff=0;
        int[] a=beneath.getRGB(0,0,480,300,null,0,480);
        int[] b=landing.getRGB(0,0,480,300,null,0,480);
        for(int i=0;i<a.length;i++)if(a[i]!=b[i])diff++;
        check(diff>4000,"actual native renderer must show distinct below/above viewpoints");
        Path out=Path.of("build","e2e","blackpine-stair-evidence");
        Files.createDirectories(out);
        check(ImageIO.write(beneath,"png",out.resolve("stair-ascent-real-render.png").toFile()),
            "ascent image encoder");
        check(ImageIO.write(landing,"png",out.resolve("loft-landing-real-render.png").toFile()),
            "landing image encoder");
        BufferedImage contact=new BufferedImage(960,332,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=contact.createGraphics();
        g.setColor(new Color(18,18,24));g.fillRect(0,0,960,332);
        g.drawImage(beneath,0,32,null);g.drawImage(landing,480,32,null);
        g.setColor(Color.WHITE);g.setFont(new Font(Font.MONOSPACED,Font.BOLD,13));
        g.drawString("NATIVE 3D: ASCENDING AUTHORED STAIR",12,22);
        g.drawString("NATIVE 3D: LOFT LANDING LOOKING DOWN",492,22);
        g.dispose();
        check(ImageIO.write(contact,"png",out.resolve("blackpine-stair-native-proof.png").toFile()),
            "native side-by-side evidence");
        System.out.println("PASS Blackpine authored stairs: continuous bidirectional physical route, real floor void, intact loft landing, and two distinct native renderer views at "+out);
    }
}
