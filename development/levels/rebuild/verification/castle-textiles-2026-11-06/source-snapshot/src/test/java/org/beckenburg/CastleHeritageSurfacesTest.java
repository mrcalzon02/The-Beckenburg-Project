package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.*;

/** Preserved licensed mesh/UVs, new original art, physical wall/floor placement. */
public final class CastleHeritageSurfacesTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-heritage-").toString());
        for(String[] pair:new String[][]{{"painting_1","level_castle_restoration_painting"},{"carpet_mp_1","level_castle_library_rug"}}){
            var source=EditorGlbReader.read(Files.readAllBytes(Path.of("development/assets/castle-museum/references/reused-models/"+pair[0]+".glb")));
            var target=EditorGlbReader.read(Files.readAllBytes(Path.of("assets/runtime/models/"+pair[1]+".glb")));
            int triangles=pair[0].equals("painting_1")?92:18;
            check(source.faces().size()==triangles&&target.faces().size()==triangles,"Original triangle budget changed");
            for(int i=0;i<source.faces().size();i++){
                var a=source.faces().get(i).primitive();var b=target.faces().get(i).primitive();
                check(a.world().equals(b.world())&&a.indices().equals(b.indices()),"Reused mesh/normal/UV/index data changed");
            }
            check(!Arrays.equals(source.materials().get(0).png(),target.materials().get(0).png()),"New art was not installed");
            check(source.materials().get(0).alpha().equals(target.materials().get(0).alpha()),"Original material alpha policy changed");
            check(GlbProps.model(pair[1]).triangles().size()==triangles,"Runtime source selection lost");
        }
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var painting=scene.props().stream().filter(p->p.id().equals("heritage-restoration-painting")).findFirst().orElseThrow();
        var rug=scene.props().stream().filter(p->p.id().equals("heritage-library-rug")).findFirst().orElseThrow();
        check(painting.scale()==1&&painting.y()==1.2&&rug.scale()==1&&rug.y()==3.4&&rug.upper(),"Scaled or misplaced reused furnishing");
        double[] b=SceneRenderer.castleBounds(painting.mesh());double wall=43-CastleDoorSlots.MASONRY_THICKNESS/2;int contact=0;
        for(var t:GlbProps.model(painting.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
            double x=painting.x()-(v.z()-b[2]),y=painting.y()+v.y()-b[1],z=painting.z()+v.x()-b[0];
            check(x<=wall+1e-6&&x>28.71&&z>4.21&&z<17.29&&y>=1.2-1e-6&&y<3.18,"Picture penetrates archive construction");
            if(Math.abs(x-wall)<1e-6)contact++;
        }
        check(contact>=4,"Picture back floats away from wall");
        check(map.traversable(41.5,11,0),"Picture approach obstructed");
        double[] rb=SceneRenderer.castleBounds(rug.mesh());check(rb[4]<.011,"Rug became a raised platform");
        var slabs=CastleRooms.slabCells(CastleRooms.structuralDecks());
        for(var t:GlbProps.model(rug.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
            double x=rug.x()+v.x()-rb[0],z=rug.z()+v.z()-rb[2];
            check(slabs.stream().anyMatch(s->s.top()==3.4&&x>=s.x0()&&x<=s.x1()&&z>=s.z0()&&z<=s.z1()),"Rug extends over floor void");
        }
        for(double x:new double[]{13.6,14.6,15.6})for(double z:new double[]{8.6,9.2,9.8})
            check(map.traversable(x,z,3.4),"Decorative rug obstructs real walking floor");
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Art/floor dressing lost on reopen");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            double[][] views={{41.5,11,1.62,Math.PI/2,-.03},{14.6,9.8,5.02,Math.PI,-1.20}};
            String[] names={"archive-restoration-painting-runtime","library-rug-runtime"};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],i==0?0:3.4),"Capture camera lacks support/clearance");
                r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve(names[i]+".png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic heritage capture");
            }
        }
        System.out.println("PASS original frame92/rug18 geometry normals UVs indices and alpha preserved, new art, actual wall/floor support, walkable rug, native save/reopen and editor capture");
    }
}
