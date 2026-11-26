package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

public final class CollegeNativeServicesTest {
    private static final Path CONTAINED=Path.of("development/levels/rebuild/college-containment-pass");

    private static void check(boolean good,String message){
        if(!good)throw new AssertionError(message);
    }

    private static HouseMap readBaseline()throws Exception{
        var assets=CONTAINED.resolve("college");
        return CollegeNativeRuntimeReview.load(CONTAINED.resolve("verification/runtime.tsv"),
            assets.resolve("college-containment.glb"),assets.resolve("college-containment.hcs2")).map();
    }

    private static CollegeNativeRuntimeReview.Campus read(Path source,Path manifest)throws Exception{
        return CollegeNativeRuntimeReview.load(manifest,source.resolve("college-services.glb"),source.resolve("college-services.hcs2"));
    }

    private static void verifyInstalledDoor(HouseMap map) {
        int collision=0,sight=0,objects=0;
        for(double x:new double[]{15.68,15.94,16.12,16.38,16.64,16.83,17.08}){
            for(double depth:new double[]{.5,-.5}){
                var a=new Vec(x,-3,24+depth);
                var b=new Vec(x,-3,24-depth);
                for(double body:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
                    check(map.resolveMove(a,b.x(),b.z(),body)==null
                        &&map.nativeInvestigation().physics().bodyBlocked(new Vec(x,-3,24),body,.22),
                        "Staff door failed to contain a body "+x);
                    collision++;
                }
                for(double y:new double[]{-2.84,-2.37,-1.38,-.91}){
                    check(!map.clearLineOfSight(x,y,a.z(),x,y,b.z()),"Staff door left a visual gap "+x+" at "+y);
                    sight++;
                }
                check(!map.clearObjectPath(a.x(),-1.72,a.z(),b.x(),-1.72,b.z(),.07,.06),
                    "Staff door allows an object to pass");
                objects++;
            }
        }
        for(double x:new double[]{16.16,16.38,16.62}){
            check(map.traversable(x,-3,25.05),"Plant door lacks occupied maintenance approach");
            check(map.roomAt(x,-3,25.05)==11&&map.roomAt(x,-3,23.25)<0,"Plant ownership changed");
        }
        System.out.printf("STAFF_DOOR body=%d sight=%d object=%d supported_approaches=3%n",collision,sight,objects);
    }

    private static void verifyFixtureSupports(HouseMap map,Path schedule)throws Exception{
        var physics=map.nativeInvestigation().physics();
        int supports=0,assets=0,contacts=0;
        for(String line:Files.readAllLines(schedule)){
            var row=line.split("\t");
            if(row[0].equals("VENT")){
                for(int i=4;i<8;i++){
                    double y=Double.parseDouble(row[i]);
                    check(Double.isFinite(y)&&y>3.5,"Illegal measured duct curb "+row[1]);
                }
                continue;
            }
            check(row[0].equals("FIXTURE"),"Unknown service row");
            double floor=Double.parseDouble(row[2]),x0=Double.parseDouble(row[3]),z0=Double.parseDouble(row[4]);
            double x1=Double.parseDouble(row[5]),z1=Double.parseDouble(row[6]);
            for(double x:new double[]{x0+.035,x1-.035})for(double z:new double[]{z0+.035,z1-.035}){
                if(!map.clearLineOfSight(x,floor-.06,z,x,floor+.04,z))supports++;
                else throw new AssertionError("Fixture straddles two unsupported floors "+row[1]+" at "+x+","+z);
            }
            double x=(x0+x1)/2,z=(z0+z1)/2;
            if(physics.bodyBlocked(new Vec(x,floor,z),HunterPosture.STANDING_HEIGHT,.22))contacts++;
            assets++;
        }
        check(assets>=29&&contacts>=24,"Incomplete physical fixture set "+assets+"/"+contacts);
        System.out.printf("SERVICES fixtures=%d floor_bearings=%d fixture_body_contacts=%d%n",assets,supports,contacts);
    }

    private static void verifyPriorSeamStations(HouseMap before,HouseMap after){
        var spaces=before.nativeInvestigation().spaces();
        int originalStations=0, adjustedStations=0;
        for(var open:spaces){
            if(!open.accessible()||open.outside())continue;
            for(var context:spaces){
                if(context.accessible()||context.outside()||Math.abs(open.floor()-context.floor())>.001)continue;
                for(boolean horizontal:new boolean[]{true,false}){
                    double a0=horizontal?open.z0():open.x0(),a1=horizontal?open.z1():open.x1();
                    double b0=horizontal?context.z0():context.x0(),b1=horizontal?context.z1():context.x1();
                    double low=horizontal?Math.max(open.x0(),context.x0()):Math.max(open.z0(),context.z0());
                    double high=horizontal?Math.min(open.x1(),context.x1()):Math.min(open.z1(),context.z1());
                    if(high-low<1)continue;
                    double wall, direction;
                    if(Math.abs(a1-b0)<.001){wall=a1;direction=1;}
                    else if(Math.abs(a0-b1)<.001){wall=a0;direction=-1;}
                    else continue;
                    for(double along=low+.5;along<high-.49;along+=.25){
                        double y=open.floor();
                        Vec old=at(horizontal,along,wall-direction*.58,y);
                        Vec target=at(horizontal,along,wall+direction*.58,y);
                        if(!before.traversable(old.x(),y,old.z())
                            ||!context.id().equals(before.architecturalRoomAt(target.x(),y+1.25,target.z())))continue;
                        originalStations++;
                        if(after.traversable(old.x(),y,old.z()))continue;
                        Vec from=null;
                        for(double depth:new double[]{.80,1.03,1.25,1.40}){
                            var alternative=at(horizontal,along,wall-direction*depth,y);
                            if(after.traversable(alternative.x(),y,alternative.z())){
                                from=alternative;
                                break;
                            }
                        }
                        check(from!=null,"Missing displaced standable seam probe "+open.id()+" "+context.id()+" "+old);
                        check(!after.clearLineOfSight(from.x(),y+1.25,from.z(),target.x(),y+1.25,target.z()),
                            "Displaced eye probe sees past the sealed context");
                        check(after.resolveMove(from,target.x(),target.z())==null,
                            "Displaced body probe crossed the sealed context");
                        adjustedStations++;
                    }
                }
            }
        }
        check(originalStations==1283,"Prior exact College context sample count drift");
        System.out.printf("PRIOR_CONTEXT original=%d displaced_replayed=%d%n",originalStations,adjustedStations);
    }

    private static Vec at(boolean alongX,double along,double across,double y){
        return alongX?new Vec(along,y,across):new Vec(across,y,along);
    }

    private record View(String id,Vec feet,double targetX,double targetZ,double pitch,boolean playable) {}
    private static void capture(HouseMap map,Path output,boolean gpu)throws Exception {
        Files.createDirectories(output);
        java.lang.reflect.Method stop=null;
        if(gpu){
            var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start=launcher.getDeclaredMethod("startGpu");
            start.setAccessible(true);
            start.invoke(null);
            stop=launcher.getDeclaredMethod("stopReviewGpu");
            stop.setAccessible(true);
        }
        try{
            for(var camera:List.of(
                new View("maintenance-staff-approach",new Vec(17.60,-3,26.76),16.38,24,.02,true),
                new View("lecture-heating",new Vec(10.2,0,43.5),6.45,40,-.20,true),
                new View("lab-fume-extract",new Vec(12.8,0,13.65),12.8,10.65,.10,true),
                new View("sealed-plant-boiler",new Vec(16.85,-3,16.42),16.85,13.45,.03,false),
                new View("sealed-plant-manifold",new Vec(15.83,-3,20.15),13.22,20.5,-.10,false),
                new View("sealed-residence-showers",new Vec(59.89,3.6,25.15),59.97,22.73,.03,false),
                new View("sealed-kitchen-extract",new Vec(58.7,0,54.6),59.1,50.75,.03,false))){
                var p=camera.feet();
                check((map.roomAt(p.x(),p.y(),p.z())>=0)==camera.playable(),"Wrong standing review classification "+camera.id());
                CollegeContainmentTest.frame(map,output,camera.id(),p,camera.targetX(),camera.targetZ(),camera.pitch());
            }
        }finally{
            if(stop!=null)stop.invoke(null);
        }
    }

    public static void main(String[] args)throws Exception{
        Path source=Path.of(args[0]),manifest=Path.of(args[1]);
        var candidate=read(source,manifest);
        verifyInstalledDoor(candidate.map());
        verifyFixtureSupports(candidate.map(),Path.of(args[2]));
        CollegeContainmentTest.verifyGeometry(CollegeContainmentTest.openBeforeContainment(),candidate.map(),
            CONTAINED.resolve("verification/containment.tsv"));
        CollegeNativeDoorsTest.audit(candidate);
        if(List.of(args).contains("--geometry-only"))return;
        CollegeNativeDoorsTest.peers(candidate.map(),read(source,manifest).map());
        CollegeNativeElectricalTest.verifyControlsAndPower(candidate.map());
        var before=readBaseline();
        check(before.nativeInvestigation().spaces().equals(candidate.map().nativeInvestigation().spaces()),
            "Native services changed the existing room and context topology");
        verifyPriorSeamStations(before,candidate.map());
        if(args.length>3)capture(candidate.map(),Path.of(args[3]),List.of(args).contains("--gpu"));
    }
}
