package org.beckenburg;
import java.util.ArrayList;
import org.beckenburg.LevelDefinition.*;

/** First remote construction graybox. Intentionally separate from selectable-site unlocks. */
public final class BlackpineLevel {
    private BlackpineLevel(){}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.FARMHOUSE,build());}
    public static LevelDefinition build(){
        var b=new LevelPlans.B("blackpine","Blackpine Logging Camp","woodland");
        b.r("Sawmill floor",40,20,52,38,0,6,"wood",43,25);
        b.r("Mill office",40,38,46,44,0,3,"wood");
        b.r("Mess hall",14,36,22,46,0,3,"wood");
        b.r("Mess kitchen",14,30,22,36,0,3,"tile");
        b.r("Workers bunkhouse",8,14,16,26,0,3,"wood");
        b.r("Washhouse",6,36,12,42,0,3,"tile");
        b.r("Foreman cabin",24,8,30,16,0,3,"wood");
        b.r("Logging cabin",34,4,40,12,0,3,"wood");
        b.r("Equipment repair shed",44,46,50,54,0,3,"wood");
        b.r("Payroll and stores office",18,4,24,12,0,3,"wood");
        b.r("Timber tool store",56,20,62,26,0,3,"wood");
        b.r("Mill inspection loft",46,30,52,38,3,3,"wood",48,35);
        b.outdoors(3,1,68,60,66,78,"Timber yard and cabin paths","Logging-road approach");
        b.door(0,12,40,0,25,2.4,false);b.door(0,12,52,0,34,2.4,false);
        b.door(0,1,43,0,38,1.2,true);b.door(1,12,40,0,41,1.2,false);
        b.door(2,12,18,0,46,1.3,true);b.door(2,12,14,0,40,1.2,false);
        b.door(2,3,18,0,36,1.2,true);b.door(3,12,18,0,30,1.2,true);
        b.door(4,12,8,0,20,1.2,false);b.door(4,12,12,0,26,1.2,true);
        b.door(5,12,9,0,42,1.2,true);
        b.door(6,12,27,0,16,1.2,true);b.door(6,12,30,0,12,1.2,false);
        b.door(7,12,37,0,12,1.2,true);b.door(7,12,40,0,8,1.2,false);
        b.door(8,12,44,0,50,1.2,false);b.door(8,12,47,0,54,1.2,true);
        b.door(9,12,21,0,12,1.2,true);b.door(9,12,18,0,8,1.2,false);
        b.door(10,12,56,0,23,1.2,false);b.door(10,12,62,0,23,1.2,false);
        b.flight(0,11,50.5,26.7,50.5,31.5,0,3);
        b.window(6,12,24,0,13.5,false);b.window(6,12,30,0,9.5,false);
        b.window(7,12,34,0,8,false);b.window(4,12,16,0,18,false);
        b.window(2,12,22,0,42,false);b.window(3,12,14,0,33,false);
        b.furnish();
        // Refuge is sparse outdoor foliage; this working camp does not acquire twelve cupboards.
        b.sockets.removeIf(s->s.type().equals("hide"));
        // Furniture stays outside the future defended rite footprints.
        b.props.removeIf(p->p.room()==0||p.room()==2||p.room()==5);
        for(double[] rail:new double[][]{{46,30,49.5,30},{51.5,30,52,30},{46,30,46,38},{52,30,52,38},{46,38,52,38}}){
            double dx=rail[2]-rail[0],dz=rail[3]-rail[1],length=Math.hypot(dx,dz),angle=Math.atan2(dz,dx);
            b.socket("enclosure",11,(rail[0]+rail[2])/2,3,(rail[1]+rail[3])/2,-angle,"field:"+length);
            b.colliders.add(new Collider((rail[0]+rail[2])/2,3,(rail[1]+rail[3])/2,length/2,.075,1.4,angle));
        }
        for(double[] tree:new double[][]{{6,18},{15,8},{52,7},{64,36},{22,53}}){
            b.socket("landscape_tree",12,tree[0],0,tree[1],0,"level_tree_conifer_worn");
            b.colliders.add(new Collider(tree[0],0,tree[1],.26,.26,2.5,0));
        }
        var level=b.finish(8,0);
        var sockets=new ArrayList<>(level.sockets());sockets.removeIf(s->s.type().equals("ritual"));
        int[] rooms={0,5,2};String[] rites={"banishment","cleansing","absolution"};
        for(int i=0;i<3;i++){var r=level.room(rooms[i]);sockets.add(new Socket("blackpine-ritual-"+rites[i],"ritual",r.id(),(r.x0()+r.x1())/2,r.y(),(r.z0()+r.z1())/2,0,rites[i]));}
        return SiteTerrain.gradeLevel(BlackpineDressing.apply(new LevelDefinition(level.id(),"blackpine-graybox-v1",level.title(),level.rooms(),level.portals(),level.stairs(),level.voids(),level.props(),sockets,level.colliders(),level.dock(),level.ambience())));
    }
}
