package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.*;

/** Actual source-size plant supports, measured terminal connections and working aisle. */
public final class CastlePlantFitTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static double area(GlbProps.Triangle t){
        double ax=t.b().x()-t.a().x(),ay=t.b().y()-t.a().y(),az=t.b().z()-t.a().z();
        double bx=t.c().x()-t.a().x(),by=t.c().y()-t.a().y(),bz=t.c().z()-t.a().z();
        double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;
        return Math.sqrt(x*x+y*y+z*z)/2;
    }
    private static double[] world(CastleLevelEdits.Prop p,GlbProps.Vertex v){
        double[] b=SceneRenderer.castleBounds(p.mesh());double x=v.x()-b[0],z=v.z()-b[2];
        return new double[]{p.x()+x*Math.cos(p.yaw())-z*Math.sin(p.yaw()),p.y()+v.y()-b[1],p.z()+x*Math.sin(p.yaw())+z*Math.cos(p.yaw())};
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-plant-").toString());
        var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        var props=scene.props().stream().filter(p->p.id().startsWith("plant-")).toList();check(props.size()==3,"Missing native plant fixtures");
        var boiler=props.stream().filter(p->p.id().equals("plant-boiler")).findFirst().orElseThrow();
        var connections=props.stream().filter(p->p.id().equals("plant-boiler-connections")).findFirst().orElseThrow();
        var bank=props.stream().filter(p->p.id().equals("plant-distribution")).findFirst().orElseThrow();
        check(GlbProps.availableModels().contains(connections.mesh())&&GlbProps.availableModels().contains(boiler.mesh()),"Plant asset approved but missing from real editor catalog");
        var original=GlbProps.model("level_boiler_modern");var repaired=GlbProps.model(boiler.mesh());
        check(original.triangles().size()==1452&&original.triangles().stream().filter(t->area(t)<1e-12).count()==32,"Original boiler/control changed");
        check(repaired.triangles().size()==1420&&repaired.triangles().stream().noneMatch(t->area(t)<1e-12),"Repaired boiler retains degenerate source faces");
        check(Math.abs(original.triangles().stream().mapToDouble(CastlePlantFitTest::area).sum()-repaired.triangles().stream().mapToDouble(CastlePlantFitTest::area).sum())<1e-8,"Attachment refit changes actual source surface area");
        check(boiler.y()==-3&&bank.y()==-2.75&&props.stream().allMatch(p->p.scale()==1),"Plant fixtures scaled or floated");
        int floor=0,wall=0,ceiling=0;
        for(var p:props)for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){
            double[] a=world(p,v);check(a[0]>=37.21-1e-6&&a[0]<=42.79+1e-6&&a[2]>26.21&&a[2]<33.79&&a[1]>=-3-1e-6&&a[1]<=-.22+1e-6,"Plant fixture penetrates actual room "+p.id()+Arrays.toString(a));
            if(p==boiler&&Math.abs(a[1]+3)<1e-6)floor++;
            if(p==bank&&Math.abs(a[0]-37.21)<1e-6)wall++;
            if(p==connections&&Math.abs(a[1]+.22)<1e-6)ceiling++;
        }
        check(floor>4&&wall>=4&&ceiling>=8,"Actual equipment floor/wall/ceiling bearing missing");
        check(GlbProps.model(connections.mesh()).triangles().size()==84,"Connection geometry exceeds measured budget");
        // Source ends retain original pivot. Recentered native connector coordinates must coincide.
        double[] boilerBounds=SceneRenderer.castleBounds(boiler.mesh()),cb=SceneRenderer.castleBounds(connections.mesh());
        for(double[] terminal:new double[][]{{-.29,-.49,2.30},{.29,-.49,2.30},{0,-.17,2.08}}){
            double bx=boiler.x()-terminal[1]+boilerBounds[2],by=boiler.y()+terminal[2]-boilerBounds[1],bz=boiler.z()+terminal[0]-boilerBounds[0];
            double cx=connections.x()-terminal[1]+cb[2],cy=connections.y()+terminal[2]-cb[1],cz=connections.z()+terminal[0]-cb[0];
            check(Math.abs(bx-cx)<1e-6&&Math.abs(by-cy)<1e-6&&Math.abs(bz-cz)<1e-6,"Authored service coordinates drift from source terminals");
        }
        // Clear swept approach from actual vault portal through service aisle and to both fronts.
        for(double x=37.7;x<=41.3;x+=.1)check(map.traversable(x,30,-3),"Plant entrance/service aisle blocked");
        for(double z=27;z<=32.4;z+=.1)check(map.traversable(40.3,z,-3),"Plant longitudinal service aisle blocked");
        check(map.traversable(40.9,28.2,-3)&&map.traversable(38.5,32.2,-3),"Equipment front maintenance clearance blocked");
        check(scene.furnishingsAt(0).stream().noneMatch(f->f.mesh().equals(connections.mesh())||f.mesh().equals(boiler.mesh())),"Lower equipment casts phantom store collision");
        var before=scene.snapshot();scene.save();scene.reopen();check(before.equals(scene.snapshot()),"Plant native save/reopen mismatch");
        if(args.length>0){
            Path out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            check(service.inspect().hierarchy().stream().filter(n->n.id().startsWith("plant-")).count()==3,"Editor misses actual plant fittings");
            double[][] views={{40.3,31.2,-1.38,Math.atan2(1.965,-3),-.10},{39.8,31.8,-1.38,-Math.PI/2,-.08},{41,29,-1.38,Math.atan2(1.265,-.8),.35}};
            for(int i=0;i<views.length;i++){
                var v=views[i];check(map.traversable(v[0],v[1],-3),"Plant capture camera lacks native support/clearance");r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];
                check(service.capture(scene.revision(),out.resolve("plant-services-"+i+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic plant capture");
            }
        }
        System.out.println("PASS native source-size plant boiler/distribution,84tri exact source-terminal wall/flue connections, real floor/wall/ceiling bearing, maintenance routes, no upper phantom collision, native save/reopen and editor captures");
    }
}
