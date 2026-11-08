package org.beckenburg;
import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.*;
/** Actual final native fixtures, source geometry, support, circulation and reviewed views. */
public final class CastleFinishPlacementTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static double[] world(CastleLevelEdits.Prop p,GlbProps.Vertex v){
        var b=SceneRenderer.castleBounds(p.mesh());double x=(v.x()-b[0])*p.scale(),z=(v.z()-b[2])*p.scale();
        return new double[]{p.x()+x*Math.cos(p.yaw())-z*Math.sin(p.yaw()),p.y()+(v.y()-b[1])*p.scale(),p.z()+x*Math.sin(p.yaw())+z*Math.cos(p.yaw())};
    }
    private static double[] bounds(CastleLevelEdits.Prop p){
        double[] b={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c())){var a=world(p,v);for(int j=0;j<3;j++){b[j]=Math.min(b[j],a[j]);b[j+3]=Math.max(b[j+3],a[j]);}}
        return b;
    }
    private static void line(CastleHubMap map,double x0,double z0,double x1,double z1,double feet,String label){
        for(int i=0;i<=100;i++){double t=i/100.;check(map.traversable(x0+(x1-x0)*t,z0+(z1-z0)*t,feet),"Blocked "+label+" at "+t);}
    }
    public static void main(String[] args)throws Exception{
        System.setProperty("user.home",Files.createTempDirectory("castle-finish-").toString());var scene=CastleLevelEdits.current();var map=new CastleHubMap();
        check(CastleFinish.FITTINGS.size()==17,"Missing final fit-out programme");
        for(var f:CastleFinish.FITTINGS){
            var p=scene.props().stream().filter(a->a.id().equals(f.id())).findFirst().orElseThrow();var room=CastleRooms.at(p.x(),p.y(),p.z());
            check(room!=null&&room.id().equals(f.room()),"Wrong room "+p.id());check(p.scale()==1,"Source scale changed "+p.id());
            var b=bounds(p);check(b[0]>room.x0()+.21&&b[3]<room.x1()-.21&&b[2]>room.z0()+.21&&b[5]<room.z1()-.21&&Math.abs(b[1]-room.floor())<1e-5&&b[4]<room.ceiling()-.22,"Room penetration/floating "+p.id()+Arrays.toString(b));
            for(double x:new double[]{b[0],b[3]})for(double z:new double[]{b[2],b[5]})check(scene.floorSupports(x,z,p.y())||(p.y()==0&&CastleRooms.ground(x,z)&&!CastleCellar.opening(x,z)),"Unsupported fixture "+p.id());
            for(var other:scene.props())if(!other.id().equals(p.id())&&Math.abs(other.y()-p.y())<.01){
                var c=bounds(other);if(c[4]-c[1]<.03)continue;
                check(Math.min(b[3],c[3])-Math.max(b[0],c[0])<.002||Math.min(b[5],c[5])-Math.max(b[2],c[2])<.002,"Overlapping fixtures "+p.id()+" / "+other.id());
            }
        }
        var statue=GlbProps.model("level_castle_keeper_statue");check(statue.triangles().size()<=1100,"Keeper exceeds car/character-scale prop budget");
        line(map,33,26.5,33,28.6,-3,"vault doorway approach");line(map,35.8,28,35.8,31.7,-3,"vault to plant service aisle");
        line(map,4,28.6,4,32,0,"kitchen work aisle");line(map,8,23.5,8,27,0,"dining circulation");line(map,38,16.8,42,16.8,0,"gallery return");
        for(double z:new double[]{17.5,24.5,31.5})check(map.traversable(5.65,z,3.4),"Lodging chest closes bed aisle");
        var before=scene.snapshot();scene.save();scene.reopen();check(before.equals(scene.snapshot()),"Finish native save/reopen differs");
        if(args.length>0){
            Path out=Path.of(args[0]);Files.createDirectories(out);var r=new SceneRenderer();r.setRenderSize(1000,625);r.lamp=false;
            var project=new CastleEditorProject(scene);var service=new EditorAgentService<>(new CastleEditorAgent(project,map,r,()->{}),new EditorHistory<>(project,60));
            double[][] views={{33,27.6,-1.38,0,-.05},{3.8,29.6,1.62,.735,-.1},{8,26,1.62,-2.048,-.1},{5.65,31.5,5.02,1.5708,-.9},{39.2,12.7,1.62,.312,-.1}};
            String[] names={"vault","kitchen","dining","lodging","keeper"};
            for(int i=0;i<views.length;i++){var v=views[i];check(map.traversable(v[0],v[1],v[2]-1.62),"Unsupported review camera "+names[i]);r.x=v[0];r.z=v[1];r.eyeHeight=v[2];r.yaw=v[3];r.pitch=v[4];check(service.capture(scene.revision(),out.resolve("finish-"+names[i]+"-runtime.png")).result().renderer().equals("SceneRenderer.drawCastle"),"Synthetic capture");}
        }
        System.out.println("PASS17 native finishing fixtures: source scale, whole-footprint support, actual room bounds, nonoverlap, vault/kitchen/dining/gallery/bed circulation, low-count keeper, save/reopen and actual editor captures");
    }
}
