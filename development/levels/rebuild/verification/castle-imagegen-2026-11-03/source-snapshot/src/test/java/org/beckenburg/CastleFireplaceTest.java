package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Owner-budget fireplace with actual masonry bearing and original generated atlas. */
public final class CastleFireplaceTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-fireplace-").toString());
        String id="level_castle_great_fireplace";var model=GlbProps.model(id);
        check(model.triangles().size()==56,"26 authored faces must export the reviewed56 triangles");
        var scene=CastleLevelEdits.current();
        var map=new CastleHubMap();
        for(var slot:CastleDoorSlots.all())if(scene.doorType(slot.id())!=CastleDoorSlots.Type.EMPTY)map.toggleDoor(slot.id());
        var p=scene.props().stream().filter(q->q.id().equals("heritage-common-fireplace")).findFirst().orElseThrow();
        check(p.scale()==1&&p.y()==0&&p.yaw()==0,"Fireplace scale/floor/orientation changed");
        check(CastleRooms.at(p.x(),0,p.z()).id().equals("common"),"Fireplace outside common room");
        var b=SceneRenderer.castleBounds(id);
        // The back face of the chimney mass bears on the actual north wall.
        // The hearth's rear edge is deliberately let into its stone skirting.
        double back=p.z()+(-.35-b[2]);
        check(Math.abs(back-(22+CastleDoorSlots.MASONRY_THICKNESS/2))<.002,"Fireplace floats away from its bearing wall: "+back);
        check(CastleRooms.slabCells(CastleRooms.structuralDecks()).stream().anyMatch(s->s.top()==0&&p.x()>=s.x0()&&p.x()<=s.x1()&&p.z()>=s.z0()&&p.z()<=s.z1()),"Fireplace hearth lacks floor support");
        check(!map.traversable(p.x(),p.z(),0),"Fireplace lacks body collision");
        for(var q:scene.props())if(!q.id().equals(p.id())&&q.y()<2.85){
            check(Math.abs(p.x()-q.x())>=p.halfX()+q.halfX()||Math.abs(p.z()-q.z())>=p.halfZ()+q.halfZ(),"Fireplace overlaps native furnishing "+q.id());
        }
        double[][] routePoints={{5.5,22},{5.5,24.5},{1,25},{5.5,25}};
        boolean[] with=new boolean[routePoints.length];
        for(int i=0;i<routePoints.length;i++)with[i]=map.traversable(routePoints[i][0],routePoints[i][1],0);
        scene.removeProp(p.id());
        for(int i=0;i<routePoints.length;i++){
            boolean before=map.traversable(routePoints[i][0],routePoints[i][1],0);
            System.out.println("ROUTE "+Arrays.toString(routePoints[i])+" before="+before+" with="+with[i]);
            check(before==with[i],"Fireplace changes existing common/training clearance");
        }
        scene.put(p);
        // A cold retained firebox must be backed, never a see-through hollow facade.
        check(!FixtureLighting.castleRayClear(map,p.x(),1.1,24.4,p.x(),1.1,21.8),"Firebox permits optical leak through its rear masonry");
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Fireplace lost on native reopen");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            double[][] views={{8.4,26,1.62,Math.atan2(-.8,-3.34),-.02},{6.1,25.5,1.62,Math.atan2(1.5,-2.84),-.02}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],0),"Capture inside furnishing");r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("fireplace-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic fireplace capture");
            }
            if(args.length>1){var pre=Path.of(args[1]);Files.createDirectories(pre);for(int i=0;i<4;i++){
                GlbProps.renderNativeMaterialReview(Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb")),r,i*Math.PI/2);
                ImageIO.write(r.image(),"png",pre.resolve("fireplace-"+i+"-source-material.png").toFile());
            }}
        }
        System.out.println("PASS native26-face/56-triangle fireplace, real hearth/wall bearing, backed cold firebox, preserved circulation, collision and exact save/reopen");
    }
}
