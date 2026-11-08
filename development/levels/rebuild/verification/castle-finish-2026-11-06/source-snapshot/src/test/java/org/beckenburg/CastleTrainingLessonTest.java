package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Native protected lessons, shared rite names, real contained UI and zero campaign writes. */
public final class CastleTrainingLessonTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static <T extends Component>T named(Container root,String name,Class<T> type){
        for(Component c:root.getComponents()){
            if(name.equals(c.getName())&&type.isInstance(c))return type.cast(c);
            if(c instanceof Container child){T found=named(child,name,type);if(found!=null)return found;}
        }
        return null;
    }
    private static void click(Container root,String name){JButton b=named(root,name,JButton.class);check(b!=null&&b.isEnabled(),"Missing/disabled lesson button "+name);b.doClick(0);}
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container child)layout(child);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-lessons-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();var original=scene.snapshot();
        check(CastleTraining.BAYS.size()==3,"Three protected lesson bindings missing");
        var profilePath=Path.of(System.getProperty("user.home"),"lesson-profile.properties");var profile=new ProfileStore(profilePath);profile.save();byte[] profileBefore=Files.readAllBytes(profilePath);var actions=new AtomicInteger();
        for(var bay:CastleTraining.BAYS){
            var table=scene.props().stream().filter(p->p.id().equals(bay.tableId())).findFirst().orElseThrow();
            var marker=scene.props().stream().filter(p->p.id().equals(bay.markerId())).findFirst().orElseThrow();
            check(table.mesh().equals("level_workbench")&&table.scale()==1&&table.y()==0,"Lesson desk substitutes source/scale");
            check(GlbProps.model(marker.mesh()).triangles().size()==160+12*(bay.rite().ordinal()+3),"Rite inlay source budget changed");
            for(var p:java.util.List.of(table,marker)){
                double[] b=SceneRenderer.castleBounds(p.mesh());var room=CastleRooms.at(p.x(),0,p.z());check(room!=null&&room.id().equals(bay.room()),"Native teaching fixture outside actual bay");
                for(var t:GlbProps.model(p.mesh()).triangles())for(var v:java.util.List.of(t.a(),t.b(),t.c())){
                    double x=p.x()+v.x()-b[0],z=p.z()+v.z()-b[2],y=p.y()+v.y()-b[1];
                    check(x>room.x0()+.21&&x<room.x1()-.21&&z>room.z0()+.21&&z<room.z1()-.21&&y>=-1e-6&&y<room.ceiling()-.22,"Teaching fixture penetrates masonry/floor/ceiling");
                    check(scene.floorSupports(x,z,0),"Teaching fixture over unsupported floor");
                }
            }
            check(scene.furnishingsAt(0).stream().noneMatch(f->f.mesh().equals(marker.mesh())),"Thin floor inlay blocks bodies");
            for(double z=18;z<=23.5;z+=.2)check(map.traversable(bay.x(),z,0),"Lesson/inlay blocks real bay circulation");
            check(CastleTraining.aimed(map,bay.x(),0,18.5,Math.PI)==bay,"Actual native table cannot be aimed");
            check(CastleTraining.aimed(map,bay.x(),0,18.5,0)==null,"Lesson activates behind player");
            check(CastleTraining.aimed(map,bay.x(),3.4,18.5,Math.PI)==null&&CastleTraining.aimed(map,bay.x(),-3,18.5,Math.PI)==null,"Lesson leaks to other floors");
            var before=scene.snapshot();scene.removeProp(table.id());scene.save();scene.reopen();check(CastleTraining.aimed(map,bay.x(),0,18.5,Math.PI)==null,"Deleted/saved desk retains phantom lesson");scene.restore(before);scene.save();
            scene.removeProp(marker.id());check(CastleTraining.aimed(map,bay.x(),0,18.5,Math.PI)==null,"Missing installation retains lesson");scene.restore(before);
            scene.removeSurface("annex-floor-"+bay.room());check(CastleTraining.aimed(map,bay.x(),0,18.5,Math.PI)==null,"Missing native bay floor retains lesson");scene.restore(before);
            var moved=new CastleLevelEdits.Prop(table.id(),table.mesh(),table.x()+1.5,table.y(),table.z(),table.scale(),table.yaw(),table.halfX(),table.halfZ(),false);scene.put(moved);
            check(CastleTraining.aimed(map,bay.x(),0,18.5,Math.PI)==null&&CastleTraining.aimed(map,bay.x()+1.5,0,18.5,Math.PI)==bay,"Lesson does not follow native desk pose");scene.restore(before);
            var lesson=new CastleTraining.Lesson(bay);var wrong=Arrays.stream(GhostIdentity.Rite.values()).filter(r->r!=bay.rite()).findFirst().orElseThrow();
            check(!lesson.ward().accepted()&&!lesson.installation(bay.rite()).accepted(),"Lesson accepts out-of-order actions");
            check(!lesson.identify(wrong).accepted()&&lesson.step()==CastleTraining.Step.IDENTIFY,"Incorrect rite advances lesson");
            check(lesson.identify(bay.rite()).accepted()&&lesson.ward().accepted(),"Canonical rite/ward lesson fails");
            check(!lesson.installation(wrong).accepted()&&lesson.step()==CastleTraining.Step.INSTALLATION,"Wrong installation completes lesson");
            check(lesson.installation(bay.rite()).accepted()&&lesson.step()==CastleTraining.Step.COMPLETE,"Protected rehearsal cannot complete");lesson.reset();check(lesson.step()==CastleTraining.Step.IDENTIFY,"Lesson cannot replay");
            // The actual negative-X native spawn must roundtrip; invalid bounds must not alter it.
            scene.setSpawn(bay.x(),18.5,Math.PI);scene.save();check(new CastleLevelEdits(scene.path()).snapshot().equals(scene.snapshot()),"Signed annex spawn/fixtures fail native reopen");
            var saved=scene.snapshot();try{scene.setSpawn(-18.1,18.5,0);throw new AssertionError("Out-of-bounds spawn accepted");}catch(IllegalArgumentException expected){}check(saved.equals(scene.snapshot()),"Rejected spawn mutates scene");
            Throwable[] failure={null};SwingUtilities.invokeAndWait(()->{CastleHubPanel castle=null;try{
                castle=new CastleHubPanel(profile,new CastleHubPanel.Actions(){
                    public void begin(){actions.incrementAndGet();}public void network(){actions.incrementAndGet();}public void spectral(){actions.incrementAndGet();}public void menu(){actions.incrementAndGet();}public void campaign(){actions.incrementAndGet();}
                    public void cycleRole(){actions.incrementAndGet();}public void cycleWeather(){actions.incrementAndGet();}public String role(){return "CLERIC";}public String weather(){return "Normal";}
                });castle.setSize(1000,700);check(castle.openAimedTraining(),"Actual Castle interaction does not open lesson");
                CastleTrainingPanel panel=named(castle,"castle.training.panel",CastleTrainingPanel.class);check(panel!=null&&SwingUtilities.isDescendingFrom(panel,castle),"Lesson escaped Castle viewport");
                click(panel,"castle.training.rite."+wrong);check(panel.step()==CastleTraining.Step.IDENTIFY,"UI wrong answer advances state");
                click(panel,"castle.training.rite."+bay.rite());click(panel,"castle.training.ward");click(panel,"castle.training.installation."+bay.rite());check(panel.step()==CastleTraining.Step.COMPLETE,"Actual UI cannot complete lesson");
                if(args.length>1){layout(castle);var image=new BufferedImage(1000,700,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();castle.paint(g);g.dispose();Path out=Path.of(args[1]);Files.createDirectories(out);ImageIO.write(image,"png",out.resolve("lesson-"+bay.room()+"-ui.png").toFile());}
                click(panel,"castle.training.reset");check(panel.step()==CastleTraining.Step.IDENTIFY,"Actual UI cannot restart");click(panel,"castle.training.return");
                check(castle.openAimedTraining(),"Cannot reopen lesson");panel=named(castle,"castle.training.panel",CastleTrainingPanel.class);check(panel.step()==CastleTraining.Step.IDENTIFY,"Closed rehearsal persists unintended progress");castle.closeService();castle.startOrientation();check(!castle.openAimedTraining(),"Rite lesson interrupts active orientation");
            }catch(Throwable t){failure[0]=t;}finally{if(castle!=null)castle.close();}});
            if(failure[0]!=null)throw new AssertionError("Contained native lesson UI",failure[0]);
            check(actions.get()==0&&Arrays.equals(profileBefore,Files.readAllBytes(profilePath)),"Training launches case/changes campaign or inventory");
        }
        check(CastleTraining.aimed(map,-8,0,18.5,Math.atan2(-6,-2.2))==null,"Lesson aim crosses bay partition");
        scene.restore(original);scene.save();scene.reopen();check(original.equals(scene.snapshot()),"Native training snapshot not preserved");
        if(args.length>0){
            Path out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            for(var bay:CastleTraining.BAYS){
                check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals(bay.tableId()))&&service.inspect().hierarchy().stream().anyMatch(n->n.id().equals(bay.markerId())),"Editor misses native lesson fixtures");
                r.x=bay.x()+1;r.z=23;r.eyeHeight=1.62;r.yaw=Math.atan2(-1,-4);r.pitch=-.25;
                check(map.traversable(r.x,r.z,0),"Training capture camera lacks physical clearance");
                check(service.capture(scene.revision(),out.resolve("training-"+bay.room()+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic training capture");
            }
        }
        System.out.println("PASS3 protected native rite lessons, source-size supported desks/inlays, real aim/move/delete/floor negatives, signed spawn save/reopen, actual contained UI/reset/return, zero case/campaign/inventory actions and editor captures");
    }
}
