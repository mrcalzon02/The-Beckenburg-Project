package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Supported household records and individually authored loose belongings, not room-name scatter. */
public final class HouseRebuildStory {
    private HouseRebuildStory(){}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var base=HouseGeneratorFeed.source();var b=new Dressing(base);
        b.record("family-correspondence","LETTERS","level_story_letters",3,"dinner_table_1",19.55,11.5);
        b.record("cellar-shelter-card","EMERGENCE","level_trace_journal",3,"dinner_table_1",20.05,11.9);
        b.record("inherited-mending-tin","MENDING","level_story_sewing_tin",6,"bedside_table_1",12.6,17.0);
        b.loose("interrupted-tea",LooseClutter.Kind.MUG,3,"dinner_table_1",19.6,12.0,.12);
        b.loose("stored-water",LooseClutter.Kind.BOTTLE,3,"dinner_table_1",20.55,11.5,-.08);
        b.loose("family-tea-tin",LooseClutter.Kind.TIN,3,"dinner_table_1",20.55,12.15,0);
        b.loose("unfinished-homework",LooseClutter.Kind.BOOK,7,"level_student_desk",12.75,14.18,-.08);
        b.loose("wooden-train",LooseClutter.Kind.TOY,7,"level_student_desk",13.12,14.18,.1);
        b.loose("saved-photograph-frame",LooseClutter.Kind.FRAME,1,"bedside_table_1",14.25,19.80,-.1);
        b.loose("reading-room-cup",LooseClutter.Kind.MUG,1,"bedside_table_1",14.55,19.6,.08);
        b.loose("child-bedside-cup",LooseClutter.Kind.MUG,7,"bedside_table_1",12.5,12.4,-.12);
        b.sockets.add(new Socket("briar-authored-belongings","authored-clutter",0,19,0,19.4,0,"source-supported-v1"));
        return new LevelDefinition(base.id(),"first-principles-story-v1",base.title().replace("service fit-out","household story construction"),base.rooms(),base.portals(),base.stairs(),base.voids(),b.props,b.sockets,b.solids,base.dock(),base.ambience(),base.water(),base.sounds());
    }
    private static final class Dressing {
        final LevelDefinition base;final ArrayList<Prop> props;final ArrayList<Socket> sockets;final ArrayList<Collider> solids;
        Dressing(LevelDefinition b){base=b;props=new ArrayList<>(b.props());sockets=new ArrayList<>(b.sockets());solids=new ArrayList<>(b.colliders());}
        Prop parent(int room,String mesh){return props.stream().filter(p->p.room()==room&&p.mesh().equals(mesh)).findFirst().orElseThrow();}
        void record(String key,String lore,String mesh,int room,String parentMesh,double x,double z){
            var parent=parent(room,parentMesh);var bounds=SceneRenderer.castleBounds(mesh);double top=surface(parent,x,z),hx=bounds[3]/2,hz=bounds[5]/2;
            for(double dx:new double[]{-hx,hx})for(double dz:new double[]{-hz,hz})if(Math.abs(surface(parent,x+dx,z+dz)-top)>.003)throw new IllegalArgumentException("Unsupported story record "+key);
            var p=new Prop(mesh,room,x-bounds[0],top+.002-bounds[1],z-bounds[2],1,0,hx,hz,bounds[4],parentMesh);props.add(p);
            solids.add(new Collider(x,top+.002,z,hx,hz,bounds[4],0));sockets.add(new Socket(key,"story",room,p.x(),top+.002+bounds[4]/2,p.z(),0,lore));
        }
        void loose(String key,LooseClutter.Kind kind,int room,String parentMesh,double x,double z,double yaw){
            var p=parent(room,parentMesh);double bottom=supported(p,kind,x,z);if(!Double.isFinite(bottom))throw new IllegalArgumentException("Unsupported authored belonging "+key);
            double dx=x-p.x(),dz=z-p.z(),c=Math.cos(p.yaw()),s=Math.sin(p.yaw());
            String binding=kind.name()+"|"+parentMesh+"|"+(dx*c+dz*s)/p.scale()+"|"+(-dx*s+dz*c)/p.scale()+"|"+yaw+"|"+key;
            sockets.add(new Socket(key,"loose-object",room,x,bottom+.004,z,p.yaw()+yaw,binding));
        }
    }
    static double surface(Prop p,double x,double z){
        double c=Math.cos(p.yaw()),s=Math.sin(p.yaw()),lx=((x-p.x())*c+(z-p.z())*s)/p.scale(),lz=(-(x-p.x())*s+(z-p.z())*c)/p.scale(),best=Double.NEGATIVE_INFINITY;
        for(var t:GlbProps.model(p.mesh()).triangles()){var a=t.a();var b=t.b();var d=t.c();double den=(b.z()-d.z())*(a.x()-d.x())+(d.x()-b.x())*(a.z()-d.z());if(Math.abs(den)<1e-12)continue;
            double u=((b.z()-d.z())*(lx-d.x())+(d.x()-b.x())*(lz-d.z()))/den,v=((d.z()-a.z())*(lx-d.x())+(a.x()-d.x())*(lz-d.z()))/den;
            if(u>=-1e-7&&v>=-1e-7&&u+v<=1+1e-7)best=Math.max(best,p.y()+(a.y()*u+b.y()*v+d.y()*(1-u-v))*p.scale());}
        return best;
    }
    static double supported(Prop p,LooseClutter.Kind kind,double x,double z){
        double top=surface(p,x,z);for(double[] d:new double[][]{{kind.radius,0},{-kind.radius,0},{0,kind.radius},{0,-kind.radius}})if(Math.abs(surface(p,x+d[0],z+d[1])-top)>.003)return Double.NaN;return top;
    }
    static String description(String key){return switch(key){
        case "interrupted-tea","reading-room-cup"->"A chipped family mug,with a dried tea stain below the rim.";
        case "stored-water"->"A corked stoneware bottle left beside the cellar shelter records.";
        case "family-tea-tin"->"A dented tea tin beside the interrupted household correspondence.";
        case "unfinished-homework"->"An ordinary school exercise book left on the child's desk.";
        case "wooden-train"->"A small wooden train kept beside unfinished schoolwork.";
        case "saved-photograph-frame"->"A worn family photograph frame saved among the older belongings.";
        case "child-bedside-cup"->"A small bedside cup left near the child's alarm clock.";
        default->"An ordinary household belonging.";};}
}
