package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Actual wall-mounted source mesh, original image sampling and native editor capture. */
public final class CastleTapestryTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-tapestry-").toString());
        String id="level_castle_tapestry";
        var model=GlbProps.model(id);check(model.triangles().size()==160,"Tapestry exceeds reviewed low-count construction");
        var source=EditorGlbReader.read(Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb")));
        var cloth=source.materials().stream().filter(m->m.name().contains("woven tapestry")).findFirst().orElseThrow();
        var image=ImageIO.read(new java.io.ByteArrayInputStream(cloth.png()));
        check(image.getWidth()==512&&image.getHeight()==768,"Missing budget-sized woven texture");
        int material=model.triangles().get(0).material();
        for(double[] uv:new double[][]{{.17,.13},{.43,.38},{.68,.72},{.82,.91}}){
            int expected=image.getRGB((int)(uv[0]*image.getWidth()),(int)(uv[1]*image.getHeight()))&0xffffff;
            check(GlbProps.sample(material,uv[0],uv[1])==expected,"Tapestry atlas origin/UV sampling changed");
        }
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var p=scene.props().stream().filter(q->q.id().equals("heritage-archive-tapestry")).findFirst().orElseThrow();
        check(p.scale()==1&&p.y()==.55&&p.yaw()==0,"Hanging was scaled or placed on the floor");
        var room=CastleRooms.at(p.x(),0,p.z());check(room!=null&&room.id().equals("archive"),"Collection hanging outside archive");
        var b=SceneRenderer.castleBounds(id);double wall=4+CastleDoorSlots.MASONRY_THICKNESS/2;int bearingVertices=0;
        for(var t:model.triangles())for(var a:List.of(t.a(),t.b(),t.c())){
            double x=p.x()+a.x()-b[0],y=p.y()+a.y()-b[1],z=p.z()+a.z()-b[2];
            check(x>room.x0()+.21&&x<room.x1()-.21&&z>=wall-1e-6&&z<room.z1()-.21,"Hanging penetrates actual room masonry");
            check(y>=.55-1e-6&&y<room.ceiling()-CastleRooms.SLAB,"Hanging crosses floor/ceiling");
            if(Math.abs(z-wall)<1e-6)bearingVertices++;
        }
        check(bearingVertices>=8,"Wall plates float away from actual masonry");
        check(map.traversable(p.x(),p.z()+.85,0),"Archive viewing approach blocked");
        check(CastleArchive.aimedKeeper(map,36,9,0),"Hanging blocks existing Lore Master approach");
        for(var q:scene.props())if(!q.id().equals(p.id())&&q.y()<2.8){
            check(Math.abs(p.x()-q.x())>=p.halfX()+q.halfX()||Math.abs(p.z()-q.z())>=p.halfZ()+q.halfZ(),"Hanging overlaps retained furniture "+q.id());
        }
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Hanging lost on native save/reopen");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals(p.id())),"Actual editor service misses hanging");
            double[][] views={{36.5,7.3,1.62,Math.PI,.02},{39,7.5,1.62,Math.atan2(-2.5,-3.2375),.02}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],0),"Review camera inside furniture");r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("archive-tapestry-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic tapestry capture");
            }
            if(args.length>1){var pre=Path.of(args[1]);Files.createDirectories(pre);for(int i=0;i<4;i++){
                GlbProps.renderNativeMaterialReview(Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb")),r,i*Math.PI/2);
                ImageIO.write(r.image(),"png",pre.resolve("tapestry-"+i+"-source-material.png").toFile());
            }}
        }
        System.out.println("PASS160-triangle mounted tapestry, original512x768 weave/UV sampling, actual wall-plate bearing, room/fixture clearance, keeper access, native save/reopen and editor captures");
    }
}
