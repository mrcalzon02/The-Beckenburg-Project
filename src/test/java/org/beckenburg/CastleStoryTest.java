package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;

/** Real exhibit aim/occlusion, edited scene identity and actual contained E interaction. */
public final class CastleStoryTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private record View(double x,double feet,double z,double yaw,double pitch){}
    private static View view(CastleHubMap map,CastleLevelEdits.Prop p,CastleStory.Record record){
        var triangles=GlbProps.model(p.mesh()).triangles();double[] b=SceneRenderer.castleBounds(p.mesh());
        for(double radius:new double[]{1.6,2.1,2.6})for(int a=0;a<24;a++){
            double x=p.x()+Math.sin(a*Math.PI/12)*radius,z=p.z()+Math.cos(a*Math.PI/12)*radius,feet=p.y()>=3?3.4:p.y()<0?-3:0;
            if(!map.traversable(x,z,feet))continue;
            for(int i=0;i<triangles.size();i+=Math.max(1,triangles.size()/24)){
                var t=triangles.get(i);double lx=(t.a().x()+t.b().x()+t.c().x())/3-b[0],ly=(t.a().y()+t.b().y()+t.c().y())/3-b[1],lz=(t.a().z()+t.b().z()+t.c().z())/3-b[2];
                double tx=p.x()+p.scale()*(lx*Math.cos(p.yaw())-lz*Math.sin(p.yaw())),tz=p.z()+p.scale()*(lx*Math.sin(p.yaw())+lz*Math.cos(p.yaw())),ty=p.y()+ly*p.scale();
                double yaw=Math.atan2(tx-x,tz-z),pitch=Math.atan2(ty-feet-1.62,Math.hypot(tx-x,tz-z));
                if(CastleStory.aimed(map,x,feet,z,yaw,pitch,1.62)==record&&map.aimedDoor(x,feet,z,yaw)==null&&CastleHubDecor.aimed(map,x,feet,z,yaw)==null&&CastleRelations.aimed(map,x,z,yaw)==null&&CastleTraining.aimed(map,x,feet,z,yaw)==null)return new View(x,feet,z,yaw,pitch);
            }
        }throw new AssertionError("No real accessible view of "+p.id());
    }
    private static Component named(Container parent,String name){for(var c:parent.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container child){var v=named(child,name);if(v!=null)return v;}}return null;}
    private static void layout(Container parent){parent.doLayout();for(var c:parent.getComponents())if(c instanceof Container child)layout(child);}
    public static void main(String[] args)throws Exception{
        Path home=Files.createTempDirectory("castle-story-");System.setProperty("user.home",home.toString());
        var scene=CastleLevelEdits.current();var original=scene.snapshot();var map=new CastleHubMap();
        var profilePath=home.resolve("profile.properties");var profile=new ProfileStore(profilePath);profile.save();byte[] profileBefore=Files.readAllBytes(profilePath);
        var actions=new java.util.concurrent.atomic.AtomicInteger();var views=new LinkedHashMap<String,View>();
        for(var e:CastleStory.RECORDS.entrySet()){
            var p=scene.props().stream().filter(prop->prop.id().equals(e.getKey())).findFirst().orElseThrow();var v=view(map,p,e.getValue());views.put(p.id(),v);
            check(CastleStory.aimed(map,v.x,v.feet,v.z,v.yaw+Math.PI,v.pitch,1.62)!=e.getValue(),"Record opens while looking away");
            scene.removeProp(p.id());check(CastleStory.aimed(map,v.x,v.feet,v.z,v.yaw,v.pitch,1.62)!=e.getValue(),"Deleted native exhibit remains readable");scene.restore(original);
            // An actual nearer duplicate mesh with an unrelated native ID occludes the record.
            var blocker=new CastleLevelEdits.Prop("story-occlusion-control",p.mesh(),p.x()-Math.sin(v.yaw)*.55,p.y()-Math.tan(v.pitch)*.55,p.z()-Math.cos(v.yaw)*.55,p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.upper());scene.put(blocker);
            check(map.traversable(v.x,v.z,v.feet),"Occlusion control invalidates camera instead of blocking sight");
            check(CastleStory.aimed(map,v.x,v.feet,v.z,v.yaw,v.pitch,1.62)!=e.getValue(),"Record reads through nearer actual native mesh");scene.restore(original);
            double mx=(v.x+p.x())/2,mz=(v.z+p.z())/2;
            boolean horizontal=Math.abs(Math.cos(v.yaw))>=Math.abs(Math.sin(v.yaw));
            scene.put(new CastleLevelEdits.Surface("story-wall-control",CastleLevelEdits.Kind.WALL,horizontal?mx-1:mx,horizontal?mz:mz-1,horizontal?mx+1:mx,horizontal?mz:mz+1,v.feet,2.8,MaterialAtlas.STONE));
            check(map.traversable(v.x,v.z,v.feet),"Wall control invalidates camera instead of occluding record");
            check(CastleStory.aimed(map,v.x,v.feet,v.z,v.yaw,v.pitch,1.62)!=e.getValue(),"Record reads through actual opaque native masonry");scene.restore(original);
            Throwable[] failure={null};SwingUtilities.invokeAndWait(()->{CastleHubPanel castle=null;JFrame window=null;try{
                castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                    public void begin(){actions.incrementAndGet();}public void network(){actions.incrementAndGet();}public void spectral(){actions.incrementAndGet();}public void menu(){actions.incrementAndGet();}public void campaign(){actions.incrementAndGet();}public void cycleRole(){actions.incrementAndGet();}public void cycleWeather(){actions.incrementAndGet();}public String role(){return "CLERIC";}public String weather(){return "Normal";}
                });castle.setSize(1000,700);
                if(!GraphicsEnvironment.isHeadless()){window=new JFrame("Castle museum record verification");window.setContentPane(castle);window.setSize(1020,740);window.setVisible(true);}
                var field=CastleHubPanel.class.getDeclaredField("renderer");field.setAccessible(true);var renderer=(SceneRenderer)field.get(castle);renderer.x=v.x;renderer.z=v.z;renderer.eyeHeight=v.feet+1.62;renderer.yaw=v.yaw;renderer.pitch=v.pitch;
                var height=CastleHubPanel.class.getDeclaredField("elevation");height.setAccessible(true);height.setDouble(castle,v.feet);renderer.drawCastle(map,0);
                if(args.length>0){layout(castle);var image=new BufferedImage(1000,700,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();castle.paint(g);g.dispose();Path out=Path.of(args[0]);Files.createDirectories(out);ImageIO.write(image,"png",out.resolve(p.id()+"-record-world.png").toFile());}
                if(GraphicsEnvironment.isHeadless())check(castle.openAimedStory(),"Contained native story action unavailable");
                else castle.keyPressed(new KeyEvent(castle,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_E,'e'));
                check(named(castle,"castle.story.panel") instanceof CastleStoryPanel,"Native interaction does not open museum record "+p.id());
                check(((JLabel)named(castle,"castle.story.title")).getText().equals(e.getValue().title()),"Wrong record opened");
                var text=(JTextArea)named(castle,"castle.story.text");check(!text.isEditable()&&text.getText().contains(e.getValue().paragraphs().get(1)),"Collection text missing or editable");
                if(args.length>0){layout(castle);var image=new BufferedImage(1000,700,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();castle.paint(g);g.dispose();Path out=Path.of(args[0]);Files.createDirectories(out);ImageIO.write(image,"png",out.resolve(p.id()+"-record-ui.png").toFile());}
                ((JButton)named(castle,"castle.story.close")).doClick(0);check(named(castle,"castle.story.panel")==null,"Record cannot return to Castle");
                castle.startOrientation();check(!castle.openAimedStory(),"Museum record interrupts protected orientation");
            }catch(Throwable t){failure[0]=t;}finally{if(castle!=null)castle.close();if(window!=null)window.dispose();}});
            if(failure[0]!=null)throw new AssertionError("Native collection record UI",failure[0]);
        }
        check(CastleStory.aimed(map,0,0,0,Double.NaN,0,1.62)==null,"Nonfinite aim accepted");
        check(actions.get()==0&&Arrays.equals(profileBefore,Files.readAllBytes(profilePath))&&original.equals(scene.snapshot()),"Collection reading changes campaign/actions/native scene");
        System.out.println("PASS six actual exhibit records: accessible native mesh aim, nearer-object and actual masonry occlusion, deletion, "+(GraphicsEnvironment.isHeadless()?"contained action":"actual E-key")+"/UI/return, orientation protection and zero profile/case/scene writes");
    }
}
