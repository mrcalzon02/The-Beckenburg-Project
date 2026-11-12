package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Vec;

/**
 * Differential check of the old allocation-heavy architectural ray loop
 * against the allocation-free production loop. No collision, light, portal
 * or acoustic semantics are relaxed; closed/open doors are checked directly.
 */
public final class ArchitecturalLightOcclusionAllocationParityTest {
    private static final double HALF=LevelDefinition.WALL_HALF_THICKNESS;
    private ArchitecturalLightOcclusionAllocationParityTest(){}
    private static double at(double a,double b,double plane){
        return Math.abs(b-a)<1e-10?-1:(plane-a)/(b-a);
    }
    private static boolean hit(double t){return t>1e-7&&t<1-1e-7;}
    private static boolean aperture(HouseMap map,int room,boolean alongX,
            double plane,double tangent,double y){
        for(var p:map.level().portals())if((p.a()==room||p.b()==room)
                &&p.alongX()==alongX&&!p.kind().equals("stair")
                &&Math.abs((alongX?p.z():p.x())-plane)<.01){
            if(Math.abs(tangent-(alongX?p.x():p.z()))>=p.width()/2-.001)continue;
            if(p.kind().equals("window")){
                if(y>p.y()+1.0&&y<p.y()+2.35)return true;
            }else if((!p.door()||map.doorOpen(p.id()))
                    &&y>=p.y()-.10&&y<=p.y()+2.45)return true;
        }
        return false;
    }
    /** Independent copy of the previous production visibility loop. */
    private static boolean legacy(HouseMap map,double ax,double ay,double az,
            double bx,double by,double bz){
        for(var r:map.level().rooms())if(!r.outside()){
            for(double deck:new double[]{r.y(),r.y()+r.height()-.04}){
                double t=at(ay,by,deck);
                if(hit(t)){
                    double x=ax+(bx-ax)*t,z=az+(bz-az)*t;
                    double voidY=Math.abs(deck-r.y())<.001?r.y():r.y()+r.height();
                    if(x>=r.x0()-1e-7&&x<=r.x1()+1e-7
                            &&z>=r.z0()-1e-7&&z<=r.z1()+1e-7
                            &&!map.level().floorVoid(x,voidY,z))return false;
                }
            }
            for(boolean alongX:new boolean[]{true,false}){
                double low=alongX?r.z0():r.x0(),high=alongX?r.z1():r.x1();
                for(double centre:new double[]{low,high})for(double side:new double[]{-HALF,HALF}){
                    double t=at(alongX?az:ax,alongX?bz:bx,centre+side);
                    if(!hit(t))continue;
                    double y=ay+(by-ay)*t,tangent=alongX?ax+(bx-ax)*t:az+(bz-az)*t;
                    double start=alongX?r.x0():r.z0(),end=alongX?r.x1():r.z1();
                    if(y<r.y()-1e-7||y>r.y()+r.height()+1e-7
                            ||tangent<start-HALF||tangent>end+HALF)continue;
                    if(!aperture(map,r.id(),alongX,centre,tangent,y))return false;
                }
            }
        }
        return true;
    }
    private static List<Vec> endpoints(HouseMap map){
        List<Vec> points=new ArrayList<>();
        for(Room r:map.level().rooms())if(!r.outside()){
            double y=r.y()+Math.min(1.6,r.height()*.5),cx=(r.x0()+r.x1())/2,
                cz=(r.z0()+r.z1())/2;
            points.add(new Vec(cx,y,cz));
            points.add(new Vec(cx,r.y()+.006,cz));
            points.add(new Vec(cx,r.y()+r.height()-.043,cz));
            points.add(new Vec(r.x0()+HALF+.002,y,cz));
            points.add(new Vec(r.x1()-HALF-.002,y,cz));
            points.add(new Vec(cx,y,r.z0()+HALF+.002));
            points.add(new Vec(cx,y,r.z1()-HALF-.002));
        }
        for(var p:map.level().portals()){
            if(p.kind().equals("stair")||p.kind().equals("window"))continue;
            points.add(new Vec(p.x(),p.y()+1.55,p.z()));
        }
        for(var s:map.level().stairs()){
            points.add(new Vec(s.x0(),s.y0()+1.62,s.z0()));
            points.add(new Vec(s.x1(),s.y1()+1.62,s.z1()));
        }
        return points;
    }
    private static void check(HouseMap map,Random random,String state){
        var points=endpoints(map);
        int clear=0,blocked=0;
        for(int i=0;i<3200;i++){
            var a=points.get(random.nextInt(points.size()));
            var b=i%19==0?a:points.get(random.nextInt(points.size()));
            boolean want=legacy(map,a.x(),a.y(),a.z(),b.x(),b.y(),b.z());
            boolean got=ArchitecturalLightOcclusion.architecturalClear(map,
                a.x(),a.y(),a.z(),b.x(),b.y(),b.z());
            if(want!=got)throw new AssertionError("Changed architectural shadow result "
                +map.level().id()+" "+state+" ray "+i+" "+a+" -> "+b
                +" expected="+want+" got="+got);
            if(got)clear++;else blocked++;
        }
        if(clear==0||blocked==0)throw new AssertionError("No diverse physical rays "
            +map.level().id()+" "+state+" clear="+clear+" blocked="+blocked);
        System.out.println("PASS architectural shadow differential "+map.level().id()
            +" "+state+" 3200 rays ("+clear+" clear, "+blocked+" blocked)");
    }
    private static void indexMatchesOriginalPortalOwnership(HouseMap map){
        for(int room=0;room<map.level().rooms().size();room++){
            final int owner=room;
            var expected=map.level().portals().stream()
                .filter(p->p.a()==owner||p.b()==owner).toList();
            var actual=map.lightPortals(room);
            if(!actual.equals(expected))
                throw new AssertionError("Shadow portal index differs from authored order "
                    +map.level().id()+" room "+room+" expected "+expected.size()
                    +" got "+actual.size());
        }
    }
    private static void exercise(HouseMap map,int seed){
        indexMatchesOriginalPortalOwnership(map);
        var random=new Random(0xB3CC61EEL+seed);
        check(map,random,"closed");
        var doors=map.level().portals().stream().filter(p->p.door()).limit(2).toList();
        if(!doors.isEmpty()){
            map.toggleDoor(doors.get(0).id());
            check(map,random,"first-door-open");
            if(doors.size()>1){
                map.toggleDoor(doors.get(1).id());
                check(map,random,"two-doors-open");
            }
        }
    }
    public static void main(String[] args){
        exercise(new HouseMap(HouseMap.Site.HOUSE),1);
        exercise(new HouseMap(HouseMap.Site.FARMHOUSE),2);
        exercise(new HouseMap(HouseMap.Site.SCHOOL),3);
        exercise(new HouseMap(HouseMap.Site.CHURCH),4);
        exercise(new HouseMap(HouseMap.Site.MANSION),5);
        exercise(BlackpineLevel.map(),6);
        System.out.println("PASS allocation-free architectural shadow parity: "
            +"cross-storey slabs, real apertures and actual door states");
    }
}
