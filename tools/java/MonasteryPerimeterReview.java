package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

/** Verifies the exported terrain and founded perimeter through the runtime collision kernel. */
public final class MonasteryPerimeterReview {
    private MonasteryPerimeterReview() {}

    public static void main(String[] args) throws Exception {
        Path stage = Path.of(args[0]);
        var model = stage.resolve("monastery-with-perimeter.hcs2");
        try (var source = Files.newInputStream(model)) {
            var physics = AuthoredSitePhysics.read(source, args[1], List.of());
            var evidence = audit(physics, stage.resolve("boundary-survey.tsv"));
            var previous=Path.of(args[2]);
            try(var oldSource=Files.newInputStream(previous)){
                var before=AuthoredSitePhysics.read(oldSource,args[3],List.of());
                require(before.clearSight(new Vec(32,1.2,93),new Vec(32,1.2,95)),
                    "Negative control expected open air at the previous end of the road");
                require(before.support(32,97,0,2)==null,"Negative control expected no old exterior road");
            }
            evidence.forEach(System.out::println);
        }
    }

    static List<String> audit(AuthoredSitePhysics physics, Path survey) throws Exception {
        int sight = 0, capsule = 0, swept = 0, seams = 0;
        var rows=Files.readAllLines(survey);
        for(var row:rows.subList(1,rows.size())){
            var fields=row.split("\t");
            String span=fields[0];
            double x0=Double.parseDouble(fields[1]), z0=Double.parseDouble(fields[2]);
            double x1=Double.parseDouble(fields[3]), z1=Double.parseDouble(fields[4]);
            double length=Math.hypot(x1-x0,z1-z0);
            double nx=-(z1-z0)/length, nz=(x1-x0)/length;
            int intervals=Math.max(6,(int)Math.ceil(length/.25));
            for(int i=0;i<=intervals;i++){
                double t=(double)i/intervals;
                double x=x0+t*(x1-x0), z=z0+t*(z1-z0);
                double y=Math.max(ground(physics,x+nx*.96,z+nz*.96),ground(physics,x-nx*.96,z-nz*.96));
                for(double eye:new double[]{.64,1.34}){
                    var from=new Vec(x+nx*.94,y+eye,z+nz*.94);
                    var to=new Vec(x-nx*.94,y+eye,z-nz*.94);
                    var intercept=physics.firstOpticalHit(from,to);
                    require(intercept!=null && (intercept.component().startsWith("Monastery estate ")
                            ||intercept.component().startsWith("Monastery outer ")),
                        "Open perimeter ray at "+span+" "+from+" to "+to+" hit="+intercept);
                    sight++;
                }
                var contact=new Vec(x,y+.44,z);
                for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                    require(physics.bodyBlocked(contact,height,AuthoredSitePhysics.RADIUS),
                        "Open perimeter body at "+span+" "+contact);
                    capsule++;
                    for(int side:new int[]{-1,1}){
                        double ax=x+side*nx*.92, az=z+side*nz*.92;
                        double bx=x-side*nx*.82, bz=z-side*nz*.82;
                        var bearing=physics.support(ax,az,y,1.8);
                        if(bearing==null)continue;
                        var feet=new Vec(ax,bearing.y(),az);
                        if(!physics.traversable(feet,height))continue;
                        require(physics.resolveMove(feet,bx,bz,height)==null,
                            "Walkable perimeter crossing at "+span+" "+feet);
                        swept++;
                    }
                }
                if(i==0 || i==intervals)seams++;
            }
        }
        // The surveyed estate barrier sits past the actual staged van and the
        // old terrain seam. Both the retained road and its new extension bear.
        int road=0;
        for(double z=85;z<=100;z+=.1){
            if(Math.abs(z-94.18)<.46)continue;
            var bearing=physics.support(32,z,-.32,.75);
            require(bearing!=null && physics.traversable(new Vec(32,bearing.y(),z),HunterPosture.STANDING_HEIGHT),
                "Missing road support at station "+z);
            road++;
        }
        require(!physics.clearSight(new Vec(32,1.2,93),new Vec(32,1.2,95)),"Outer gate did not close the road");
        require(swept>500,"Too few supported approach controls");
        return List.of("perimeter_optical_rays\t"+sight,"perimeter_body_probes\t"+capsule,
            "supported_perimeter_sweeps\t"+swept,"perimeter_span_endpoints\t"+seams,"road_support_probes\t"+road);
    }

    static String playBoundary(HouseMap map){
        double x=32,z=93.20,y=map.groundHeight(x,z,13);
        require(map.traversable(x,y,z),"Hunter cannot stand at the outer gate approach");
        require(map.airborneClear(x,y+.73,z,HunterPosture.CROUCH_HEIGHT),
            "Invisible room bounds prevented a jump on the outer road");
        var motion=new org.beckenburg.editor.DefaultPlayerMotion();
        var physical=new org.beckenburg.editor.DefaultPlayerMotion.Collision(){
            public boolean clear(double px,double py,double pz,double height){return map.airborneClear(px,py,pz,height);}
            public double landing(double px,double pz,double from,double to){return map.jumpLanding(px,pz,from,to);}
        };
        motion.jump(x,y,z,0,4.65,HunterPosture.CROUCH_HEIGHT,physical);
        for(int i=0;i<18 && motion.active();i++)motion.advance(.1,HunterPosture.CROUCH_HEIGHT,physical);
        require(!motion.active() && motion.z()>z+.20 && motion.z()<94,
            "Running jump did not stop and land inside the gate: "+motion.z());
        var ghost=new LanGhostSpatial(map,13);
        var start=ghost.pose();
        boolean stopped=false;
        // Move around the protected van before following the carriage road south.
        for(int tick=1;tick<=6;tick++)ghost.step(100,0,tick);
        for(int tick=7;tick<=65;tick++){
            try{ghost.step(0,100,tick);}
            catch(IllegalArgumentException blocked){
                require(blocked.getMessage().equals("spectral room boundary; use DRIFT"),blocked.getMessage());
                stopped=true;
                break;
            }
        }
        var end=ghost.pose();
        require(stopped && end.z()>start.z()+.62 && end.z()<94.18 && end.room()==13,
            "Spectral arrival boundary did not contain normal movement: "+start+" -> "+end);
        return "perimeter_runtime_jump_and_spectral_pose\tPASS running jump landed inside the gate and spectral travel stayed inside its authored room";
    }

    private static double ground(AuthoredSitePhysics physics,double x,double z){
        var hit=physics.firstOpticalHit(new Vec(x,10,z),new Vec(x,-14,z));
        require(hit!=null,"Missing grade ray at "+x+","+z);
        return 10-24*hit.fraction();
    }

    private static void require(boolean ok,String reason){
        if(!ok)throw new AssertionError(reason);
    }
}
