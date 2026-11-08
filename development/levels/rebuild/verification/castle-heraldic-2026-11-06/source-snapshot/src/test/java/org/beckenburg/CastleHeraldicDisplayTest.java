package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Actual original-atlas display, supporting west masonry and native save/capture. */
public final class CastleHeraldicDisplayTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-heraldic-").toString());
        String id="level_castle_heraldic_arms";var model=GlbProps.model(id);
        check(model.triangles().size()==316,"Heraldic display exceeds inspected low-count budget");
        var source=EditorGlbReader.read(Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb")));
        var image=ImageIO.read(new java.io.ByteArrayInputStream(source.materials().get(0).png()));
        check(image.getWidth()==512&&image.getHeight()==512,"Missing original budget-sized atlas");
        for(double[] uv:new double[][]{{.2,.2},{.7,.2},{.2,.7},{.7,.7}}){
            int expected=image.getRGB((int)(uv[0]*512),(int)(uv[1]*512))&0xffffff;
            check(GlbProps.sample(model.triangles().get(0).material(),uv[0],uv[1])==expected,"Wrong heraldic atlas orientation");
        }
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var p=scene.props().stream().filter(q->q.id().equals("heritage-archive-arms")).findFirst().orElseThrow();
        check(p.scale()==1&&p.y()==.8&&p.yaw()==-Math.PI/2,"Display scaled or elevated incorrectly");
        var b=SceneRenderer.castleBounds(id);double wall=28.5+CastleDoorSlots.MASONRY_THICKNESS/2;int bearing=0;
        for(var t:model.triangles())for(var v:List.of(t.a(),t.b(),t.c())){
            double x=p.x()+v.z()-b[2],z=p.z()-v.x()+b[0],y=p.y()+v.y()-b[1];
            check(x>=wall-1e-6&&x<29&&z>4.21&&z<17.29&&y>=.8-1e-6&&y<3.18,"Display penetrates archive masonry/floor/ceiling");
            if(Math.abs(x-wall)<1e-6){
                bearing++;
                check(v.y()>=.49-1e-6&&v.y()<=1.19+1e-6,"Mounting spine protrudes beyond shield silhouette");
            }
        }
        check(bearing>=4,"Rear spine floats away from real wall");
        for(var q:scene.props())if(!q.id().equals(p.id())&&q.y()<2.8)
            check(Math.abs(p.x()-q.x())>=p.halfX()+q.halfX()||Math.abs(p.z()-q.z())>=p.halfZ()+q.halfZ(),"Display overlaps retained furniture "+q.id());
        check(map.traversable(29.8,12.5,0),"Display approach blocked");
        check(CastleArchive.aimedKeeper(map,36,9,0),"Display blocks Lore Master");
        var saved=scene.snapshot();scene.save();scene.reopen();check(saved.equals(scene.snapshot()),"Display does not survive native reopen");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals(p.id())),"Editor misses native display");
            double[][] views={{30.8,12.5,1.62,-Math.PI/2,.02},{30.8,14,1.62,Math.atan2(-2.,-1.5),.02}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],0),"Display camera lacks floor/clearance");
                r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("archive-heraldic-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic display capture");
            }
        }
        System.out.println("PASS316-triangle heraldic exhibit,512px atlas/orientation, actual west wall bearing, retained furniture/keeper/approach, native save/reopen and editor inspect/captures");
    }
}
