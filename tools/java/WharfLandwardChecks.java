package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;

import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.DefaultPlayerMotion;

/** Installed barriers checked independently of ground reach and room ownership. */
final class WharfLandwardChecks {
    private WharfLandwardChecks() {}

    record Station(double x, double z, double outwardX, double outwardZ, double grade) {
        Vec relative(double distance,double y) {
            return new Vec(x+outwardX*distance,y,z+outwardZ*distance);
        }
    }

    static void audit(HouseMap map,AuthoredSitePhysics physics,List<Station> stations,List<Vec> perimeter,List<String> report)
            throws IOException {
        int body = 0;
        int air = 0;
        int walk = 0;
        for (var station : stations) {
            for (double height : new double[] {HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                require(physics.bodyBlocked(station.relative(0,station.grade()),height,.22),
                        "Physical landward seam "+station);
                body++;
                double apex=DefaultPlayerMotion.TAKEOFF_SPEED*DefaultPlayerMotion.TAKEOFF_SPEED
                            /(2*DefaultPlayerMotion.GRAVITY);
                require(physics.bodyBlocked(station.relative(0,station.grade()+apex),height,.22),
                        "Jump-height opening at boundary "+station);
                air++;

                var inside=station.relative(-.70,station.grade());
                var support=physics.support(inside.x(),inside.z(),inside.y(),.35);
                if (support!=null && map.traversable(inside.x(),support.y(),inside.z(),height)) {
                    var target=station.relative(.70,station.grade());
                    require(map.resolveMove(new Vec(inside.x(),support.y(),inside.z()),target.x(),target.z(),height)==null,
                            "Walk through boundary "+station);
                    walk++;
                }
            }
        }

        // The previous collision authority supports these exact walking lines
        // all the way past the new boundary. The new geometry must stop them.
        var prior=Path.of("development/levels/rebuild/wharf-tidal-pass/tidal-wharf.hcs2");
        AuthoredSitePhysics before;
        try(var in=Files.newInputStream(prior)) {
            before=AuthoredSitePhysics.read(in,
                "af8f8b4474406b1fcb215a9cde8b50745c2e02008a6a2bc3e52ac091465eb145",List.of());
        }
        int paired=0;
        for (double[] line : new double[][] {{25,75,25,86},{39,75,39,86},{32,80,32,86},
                                             {27,73,20.15,73},{38,73,44.7,73}}) {
            for (double height : new double[] {HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                require(reaches(before,line,height),"Original dry escape control was blocked");
                require(!reaches(physics,line,height),"Previously open route still reaches dry exterior");
                paired++;
            }
        }
        double barX=28.21+4*((31.989-28.21)/20);
        double gapX=28.21+4.5*((31.989-28.21)/20);
        require(!map.clearLineOfSight(barX,1.27,82.93,barX,1.27,84.0), "Gate bar has no optical collision");
        require(map.clearLineOfSight(gapX,1.27,82.93,gapX,1.27,84.0), "An invisible solid fills the gate");
        require(map.clearObjectPath(gapX,1.27,82.93,gapX,1.27,84.0,.024,.038),"Small object cannot pass the real gap");
        require(!map.clearObjectPath(gapX,1.27,82.93,gapX,1.27,84.0,.101,.038),"Larger object crossed the real bars");
        for (double x : new double[]{4.42,59.58}) {
            require(!map.clearLineOfSight(x-.37,1.15,54.12,x+.37,1.15,54.12),
                    "Cottage masonry did not close the boundary return at "+x);
        }

        require(walk >= stations.size(),"Insufficient supported landward approaches");
        report.add("retaining_and_gate_body_contacts\t"+body);
        report.add("jump_apex_body_contacts\t"+air);
        report.add("supported_boundary_crossings_blocked\t"+walk);
        report.add("formerly_open_crossings_closed\t"+paired);
        report.add("gate_bar_gap_and_bonded_return_controls\t6");
        report.add("connected_walkable_samples_inside_landward_perimeter\t"+flood(map,perimeter));
    }

    private record Cell(int x, int y, int z) {}

    private static int flood(HouseMap map,List<Vec> perimeter) {
        require(perimeter.size()>=4,"Missing surveyed containment polygon");
        var visited=new HashSet<Cell>();
        var queue=new ArrayDeque<Vec>();
        var seeds=List.of(new Vec(32,0,61),new Vec(12,0,48),new Vec(52,0,48),
                new Vec(32,0,8),new Vec(22,0,23),new Vec(42,0,23),new Vec(12,0,19),
                new Vec(50,0,17),new Vec(32,0,45),new Vec(32,-2.5,30));
        double height=HunterPosture.CROUCH_HEIGHT;
        for(var seed:seeds){
            require(map.traversable(seed.x(),seed.y(),seed.z(),height),"No seed support "+seed);
            queue.add(seed);
            visited.add(cell(seed));
        }
        while(!queue.isEmpty()){
            Vec current=queue.remove();
            require(current.x()>=-2&&current.x()<=66&&current.z()>=4&&current.z()<=89,
                    "Walkable route out of tested site near "+current);
            require(current.z()<54.45||inside(perimeter,current),"Walkable route around landward wall near "+current);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                if(dx==0&&dz==0)continue;
                var next=map.resolveMove(current,current.x()+dx*.5,current.z()+dz*.5,height);
                if(next!=null&&visited.add(cell(next)))queue.add(next);
            }
        }
        return visited.size();
    }

    private static Cell cell(Vec p) {
        return new Cell((int)Math.round(p.x()*2),(int)Math.round(p.y()*4),(int)Math.round(p.z()*2));
    }

    private static boolean inside(List<Vec> polygon,Vec p) {
        boolean inside=false;
        Vec a=polygon.get(polygon.size()-1);
        for(Vec b:polygon){
            if((a.z()>p.z())!=(b.z()>p.z())
                    &&p.x()<(b.x()-a.x())*(p.z()-a.z())/(b.z()-a.z())+a.x())inside=!inside;
            a=b;
        }
        return inside;
    }

    private static boolean reaches(AuthoredSitePhysics physics,double[] line,double height) {
        var floor=physics.support(line[0],line[1],0,.6);
        if(floor==null)return false;
        var at=new Vec(line[0],floor.y(),line[1]);
        int steps=(int)Math.ceil(Math.hypot(line[2]-line[0],line[3]-line[1])/.08);
        for(int i=1;i<=steps;i++){
            double t=(double)i/steps;
            at=physics.resolveMove(at,line[0]+t*(line[2]-line[0]),line[1]+t*(line[3]-line[1]),height);
            if(at==null)return false;
        }
        return true;
    }

    private static void require(boolean ok,String reason) {
        if(!ok)throw new AssertionError(reason);
    }
}
