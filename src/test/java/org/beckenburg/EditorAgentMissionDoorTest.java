package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorHistory;
import org.beckenburg.editor.RuntimeSceneDocument;

/** A real editable mission doorway uses one saved structural source for the
 * renderer, player collision, line of sight and the active editor session.
 * Legacy BSCN documents remain unchanged when no override is authored.
 */
public final class EditorAgentMissionDoorTest {
    private EditorAgentMissionDoorTest(){}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void rejects(Runnable action){
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Invalid structural agent command was accepted");
    }
    private static Portal portal(LevelDefinition level,int id){
        return level.portals().stream().filter(p->p.id()==id).findFirst().orElseThrow();
    }
    private static Map<String,String> resize(int id,double width){
        return Map.of("id",Integer.toString(id),"width",Double.toString(width));
    }
    private static List<double[]> views(HouseMap map,Portal p){
        var result=new ArrayList<double[]>();
        for(double radius:new double[]{1.1,1.8,2.5}){
            for(int sign:new int[]{1,-1}){
                double cx=p.x()+(p.alongX()?0:sign*radius),
                    cz=p.z()+(p.alongX()?sign*radius:0);
                if(!map.traversable(cx,p.y(),cz))continue;
                result.add(new double[]{cx,cz});
            }
        }
        return result;
    }
    private static void point(SceneRenderer renderer,Portal p,double[] v){
        renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=p.y()+1.62;
        renderer.yaw=Math.atan2(p.x()-v[0],p.z()-v[1]);
        renderer.pitch=Math.atan2(1.20-1.62,Math.hypot(p.x()-v[0],p.z()-v[1]));
    }
    public static void main(String[] ignored)throws Exception {
        Path home=Files.createTempDirectory("mission-door-authoring-");
        String previous=System.getProperty("user.home");
        System.setProperty("user.home",home.toString());
        try{
            var site=HouseMap.Site.SCHOOL;
            var session=new LevelAuthoringSession(site);
            var source=session.sourceLevel();
            var original=session.snapshot();
            Portal selected=null;double enlarged=Double.NaN;
            var baselineMap=new HouseMap(site,session.level());
            // Select an existing real doorway whose opening can safely be widened
            // and has at least one traversable physical inspection viewpoint.
            for(var p:source.portals())if(p.door()&&p.a()!=p.b()&&p.width()<2.45){
                if(views(baselineMap,p).isEmpty())continue;
                try{session.resizeDoor(p.id(),p.width()+.30);selected=p;enlarged=p.width()+.30;break;}
                catch(IllegalArgumentException rejected){}
            }
            check(selected!=null,"School has a supported native wall doorway to widen");
            int portalId=selected.id();double targetWidth=enlarged,originalWidth=selected.width();
            session.restore(original);
            var renderer=new SceneRenderer();renderer.setRenderSize(320,200);
            var host=new LevelEditorAgent(session,()->new HouseMap(site,session.level()),renderer,()->{});
            var service=new EditorAgentService<>(host,new EditorHistory<>(session,60));
            check(service.summary().operations().contains("resize-door"),
                "Native mission agent advertises bounded real aperture edits");
            check(service.summary().tests().contains("mission-door-catalog")&&
                service.test(session.revision(),"mission-door-catalog").result().stream()
                    .anyMatch(check->check.code().equals("NATIVE_DOOR_"+portalId)&&check.passed()),
                "Native doorway IDs are discoverable through actual mission inspection");
            var baseline=new HouseMap(site,session.level());
            var cameras=views(baseline,selected);
            check(!cameras.isEmpty(),"Real traversable viewpoint next to the editable doorway");
            Path proof=Path.of("build","editor-agent-evidence","mission-door");
            Files.createDirectories(proof);
            for(int i=0;i<cameras.size();i++){
                point(renderer,selected,cameras.get(i));
                service.capture(session.revision(),proof.resolve("before-"+i+".png"));
            }
            long beforeRev=session.revision();
            var beforeSourceHash=baseline.level().revision();
            var change=resize(portalId,targetWidth);
            service.transact(beforeRev,"resize-door",change);
            check(session.revision()>beforeRev&&
                Math.abs(portal(session.level(),portalId).width()-targetWidth)<1e-9,
                "Native LevelDefinition contains the widened physical aperture");
            var widened=new HouseMap(site,session.level());
            check(Math.abs(portal(widened.level(),portalId).width()-targetWidth)<1e-9
                    &&!widened.level().revision().equals(beforeSourceHash),
                "Actual mission runtime consumes edited door size and new collision/render revision");
            boolean rendered=false;
            for(int i=0;i<cameras.size();i++){
                point(renderer,selected,cameras.get(i));
                Path before=proof.resolve("before-"+i+".png"),after=proof.resolve("after-"+i+".png");
                service.capture(session.revision(),after);
                if(!Arrays.equals(Files.readAllBytes(before),Files.readAllBytes(after))){
                    Files.copy(before,proof.resolve("before.png"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(after,proof.resolve("after.png"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Actual mission doorway rendered difference from camera "+i);
                    rendered=true;break;
                }
            }
            check(rendered,"Actual mission renderer shows doorway geometry diff from a matched traversable camera");
            check(service.test(session.revision(),"mission-runtime-parity").result().stream().allMatch(EditorAgentService.Check::passed),
                "Unrelated native mission props and collision stay in parity");
            var edited=session.snapshot();
            rejects(()->service.transact(session.revision(),"resize-door",resize(portalId,9.0)));
            rejects(()->service.transact(session.revision(),"resize-door",resize(portalId,Double.NaN)));
            rejects(()->service.transact(session.revision(),"resize-door",resize(99999,targetWidth)));
            check(edited.equals(session.snapshot()),"Invalid door commands do not mutate native geometry");
            rejects(()->service.transact(session.revision(),List.of(
                new EditorAgentService.Command("resize-door",resize(portalId,originalWidth)),
                new EditorAgentService.Command("resize-door",resize(portalId,9.0)))));
            check(edited.equals(session.snapshot()),"Invalid second operation restores whole architecture batch");
            service.undo(session.revision());
            check(Math.abs(portal(session.level(),portalId).width()-originalWidth)<1e-8,
                "Native shared Undo restores original structural opening before save");
            service.redo(session.revision());
            check(Math.abs(portal(session.level(),portalId).width()-targetWidth)<1e-8,
                "Native shared Redo restores edited structural opening before save");
            service.save(session.revision());
            check(service.verifyPosition(session.revision(),session.nodeId(0),
                session.prop(0).x(),session.prop(0).y(),session.prop(0).z()).result().diskState()
                    ==EditorAgentService.DiskState.MATCHES,"Edited structural metadata persists alongside native prop record");
            var saved = session.snapshot();
            service.reopen(session.revision());
            check(session.snapshot().equals(saved),"Door width survives native BSCN reopen");
            var fresh=new HouseMap(site);
            check(Math.abs(portal(fresh.level(),portalId).width()-targetWidth)<1e-9,
                "Separate actual gameplay load consumes saved doorway opening");
            var document=RuntimeSceneDocument.load(session.documentPath());
            check(document.find("beckenburg-door-width-"+portalId)!=null,
                "Native BSCN stores the edited portal node with exact stable ID");
            System.out.println("PASS mission agent doorway: native wall aperture, runtime revision, real rendered change, schema refusal, transaction rollback, save/reopen, independent game load and shared history");
        } finally {
            System.setProperty("user.home",previous);
            try(var files=Files.walk(home)){
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});
            }
        }
    }
}
