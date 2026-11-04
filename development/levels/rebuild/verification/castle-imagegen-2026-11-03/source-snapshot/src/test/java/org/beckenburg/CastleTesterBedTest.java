package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.*;

/** Actual exported curtain alpha, source-size lodging placement and editor captures. */
public final class CastleTesterBedTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-tester-bed-").toString());
        String id="level_castle_four_poster_bed";
        var model=GlbProps.model(id);
        check(model.triangles().size()==828,"Tester exceeds reviewed car/character budget");
        // Each mitered outer end is one physical surface. The old box ends
        // overlap here and submit about19% extra coplanar surface area.
        for(double x:new double[]{-1.095,1.095}){
            double area=0;
            for(var t:model.triangles()){
                var a=t.a();var b=t.b();var c=t.c();
                if(List.of(a,b,c).stream().allMatch(v->Math.abs(v.x()-x)<1e-5&&v.y()>2.13999&&v.y()<2.32001)){
                    double uy=b.y()-a.y(),uz=b.z()-a.z(),vy=c.y()-a.y(),vz=c.z()-a.z();area+=Math.abs(uy*vz-uz*vy)/2;
                }
            }
            check(Math.abs(area-1.42*.18)<1e-5,"Canopy has overlapping end faces: "+x+" area="+area);
        }
        var curtains=model.triangles().stream().filter(t->GlbProps.gltfMaskCutoff(t.material())>=0).toList();
        check(curtains.size()==8,"Four curtain sprite faces required");
        int material=curtains.get(0).material(),opaque=model.triangles().stream().filter(t->GlbProps.gltfMaskCutoff(t.material())<0).findFirst().orElseThrow().material();
        check(GlbProps.sampleAlpha(material,.5,.5)==0,"Curtain opening paints an opaque box");
        check(GlbProps.sampleAlpha(material,.02,.5)>=128,"Gathered side panel missing");
        check(GlbProps.sampleAlpha(material,.5,.03)>=128,"Sprite curtain floats below its header");
        // Render the exported curtain surfaces, then the same geometry with an
        // opaque material. Alpha holes must leave the actual depth buffer clear.
        var r=new SceneRenderer();r.setRenderSize(400,400);
        var depth=SceneRenderer.class.getDeclaredField("depths");depth.setAccessible(true);
        r.drawNativeMaterialReview(new GlbProps.Model(curtains),0);
        double[] mask=((double[])depth.get(r)).clone();
        r.drawNativeMaterialReview(new GlbProps.Model(curtains.stream().map(t->new GlbProps.Triangle(t.a(),t.b(),t.c(),opaque)).toList()),0);
        double[] solid=(double[])depth.get(r);int holes=0,cloth=0;
        for(int i=0;i<solid.length;i++)if(Double.isFinite(solid[i])){if(Double.isInfinite(mask[i]))holes++;else cloth++;}
        check(holes>1000&&cloth>500,"Real rasterizer lacks clear window/visible cloth: "+holes+" / "+cloth);
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var beds=scene.props().stream().filter(p->p.id().startsWith("upper-bed-")).toList();
        check(beds.size()==6,"Six retained lodging beds required");
        for(var p:beds){
            check(p.mesh().equals(id)&&p.scale()==1&&p.y()==3.4,"Native bed is scaled, floated or uses rejected study");
            var room=CastleRooms.at(p.x(),p.y(),p.z());
            check(room!=null&&room.id().startsWith("lodging-"),"Bed outside lodging");
            var b=SceneRenderer.castleBounds(id);
            check(b[3]<2.21&&b[5]<1.43&&b[4]<2.35,"Source-size bed exceeds authored lodging reservation");
            check(!map.traversable(p.x(),p.z(),p.y()),"Bed lacks body collision");
            check(map.traversable(p.x()+1.6,p.z(),3.4),"Bed blocks main lodging aisle");
            for(var q:beds)if(!p.id().equals(q.id()))check(Math.abs(p.z()-q.z())>p.halfZ()+q.halfZ(),"Beds overlap");
        }
        var snapshot=scene.snapshot();scene.save();scene.reopen();check(snapshot.equals(scene.snapshot()),"Bed material/placement lost on reopen");
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            check(service.inspect().hierarchy().stream().anyMatch(n->n.id().equals("upper-bed-0")),"Editor service cannot inspect real bed");
            double[][] views={{6.6,14.7,5.02,Math.atan2(-2.35,-1.2),-.12},{4.25,15.0,4.62,Math.PI,-.05}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],3.4),"Bed capture camera inside furniture");r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("tester-bed-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic bed capture");
            }
            var sourceOut=args.length>1?Path.of(args[1]):out;Files.createDirectories(sourceOut);
            for(int i=0;i<4;i++){
                GlbProps.renderNativeMaterialReview(Files.readAllBytes(Path.of("assets/runtime/models/"+id+".glb")),r,i*Math.PI/2);
                ImageIO.write(r.image(),"png",sourceOut.resolve("tester-bed-"+i+"-source-material.png").toFile());
            }
        }
        System.out.println("PASS 828-triangle tester bed, four alpha-cutout sprite sides, actual depth holes="+holes+", cloth="+cloth+", six native supported source-size lodgings, collision/aisles and exact save/reopen");
    }
}
