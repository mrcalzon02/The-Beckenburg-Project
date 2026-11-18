package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.editor.DefaultPlayerMotion;

import static org.beckenburg.HunterPosture.CROUCH_HEIGHT;
import static org.beckenburg.HunterPosture.STANDING_HEIGHT;

/** Replays the live College routes and independently probes the physical grade boundaries. */
public final class CollegeContainmentTest {
    private static final Path BASE=Path.of("development/levels/rebuild/college-doors-pass");
    private static final double JUMP_RISE=DefaultPlayerMotion.TUCK+DefaultPlayerMotion.TAKEOFF_SPEED*DefaultPlayerMotion.TAKEOFF_SPEED
        /(2*DefaultPlayerMotion.GRAVITY);
    private static final double[] HEIGHTS={STANDING_HEIGHT,CROUCH_HEIGHT};

    private record Point(double x,double z) {
        Point mix(Point end,double t){return new Point(x+(end.x-x)*t,z+(end.z-z)*t);}
        Point shift(double dx,double dz,double distance){return new Point(x+dx*distance,z+dz*distance);}
        Vec at(double y){return new Vec(x,y,z);}
    }
    private record Panel(String id,Point a,Point b,double gradeMin,double gradeMax,
                         double bottom,double crest,boolean gate) {}
    private record Rim(String stair,String side,Point a,Point b,double y,double dx,double dz) {}
    private record Schedule(List<Panel> panels,List<Rim> rims) {}

    private static void require(boolean condition,String failure) {
        if(!condition)throw new AssertionError(failure);
    }
    private static double number(String[] row,int index){return Double.parseDouble(row[index]);}
    private static Point point(String[] row,int index){return new Point(number(row,index),number(row,index+1));}

    private static Schedule read(Path file) throws Exception {
        var panels=new ArrayList<Panel>();
        var rims=new ArrayList<Rim>();
        for(String line:Files.readAllLines(file)){
            String[] row=line.split("\t");
            switch(row[0]){
                case "PARCEL" -> panels.add(new Panel(row[1]+"/"+row[2],point(row,3),point(row,5),
                    number(row,7),number(row,8),number(row,9),number(row,10),row[11].equals("1")));
                case "WELL" -> rims.add(new Rim(row[1],row[2],point(row,3),point(row,5),
                    number(row,7),number(row,8),number(row,9)));
                default -> throw new AssertionError("Unknown containment schedule row "+row[0]);
            }
        }
        return new Schedule(panels,rims);
    }

    private static Double support(HouseMap map,Point point,double y,double range){
        var hit=map.nativeInvestigation().physics().support(point.x,point.z,y,range);
        return hit==null?null:hit.y();
    }
    private static boolean bodyBlocked(HouseMap map,Point point,double y,double height){
        return map.nativeInvestigation().physics().bodyBlocked(point.at(y),height,.22);
    }
    private static Vec walk(HouseMap map,Vec from,Point destination,double height){
        double distance=Math.hypot(destination.x-from.x(),destination.z-from.z());
        int samples=Math.max(1,(int)Math.ceil(distance/.18));
        var start=new Point(from.x(),from.z());
        Vec at=from;
        for(int j=1;j<=samples;j++){
            var target=start.mix(destination,j/(double)samples);
            at=map.resolveMove(at,target.x,target.z,height);
            if(at==null)return null;
        }
        return at;
    }

    private static List<AuthoredSitePhysics.Triangle> terrain(HouseMap map) {
        var physics=map.nativeInvestigation().physics();
        var soil=new ArrayList<AuthoredSitePhysics.Triangle>();
        for(int i=0;i<physics.triangleCount();i++){
            var tri=physics.triangle(i);
            if(physics.componentName(tri.component()).equals("terrain-site:surface"))soil.add(tri);
        }
        require(!soil.isEmpty(),"Missing saved terrain in native scene");
        return soil;
    }

    private static Double grade(List<AuthoredSitePhysics.Triangle> soil,Point p){
        for(var td:soil){
            var a=td.a();var b=td.b();var c=td.c();
            double dx=p.x-a.x(),dz=p.z-a.z(),bx=b.x()-a.x(),bz=b.z()-a.z(),cx=c.x()-a.x(),cz=c.z()-a.z();
            double det=bx*cz-bz*cx;
            if(Math.abs(det)<1e-12)continue;
            double u=(dx*cz-dz*cx)/det,v=(bx*dz-bz*dx)/det;
            if(u>=-1e-5&&v>=-1e-5&&u+v<=1.00001)return a.y()+u*(b.y()-a.y())+v*(c.y()-a.y());
        }
        return null;
    }

    private static void perimeter(HouseMap base,HouseMap candidate,List<Panel> panels) {
        int groundBody=0,jumpBody=0,walkSweeps=0,baselineOpen=0,ends=0,sightGaps=0;
        int gates=0,gateUnder=0;
        var soil=terrain(base);
        for(int i=0;i<panels.size();i++){
            var p=panels.get(i);
            var next=panels.get((i+1)%panels.size());
            require(Math.hypot(p.b.x-next.a.x,p.b.z-next.a.z)<1e-8,"Parcel ring opens after "+p.id);
            double span=Math.hypot(p.b.x-p.a.x,p.b.z-p.a.z);
            double ux=(p.b.x-p.a.x)/span,uz=(p.b.z-p.a.z)/span;
            double nx=-uz,nz=ux;
            if(p.gate)gates++;
            int stations=Math.max(3,(int)Math.ceil(span/.17));
            for(int j=0;j<=stations;j++){
                var at=p.a.mix(p.b,j/(double)stations);
                Double y=grade(soil,at);
                require(y!=null,"Parcel lost surveyed grade at "+at);
                require(p.bottom<y-.12&&p.crest>y+2.12
                    &&p.gradeMin-.035<=y&&y<=p.gradeMax+.035,
                    "Unsupported toe, short crest or surveyed grade drift "+p.id);
                for(double posture:HEIGHTS){
                    require(bodyBlocked(candidate,at,y,posture),"Ground crossing at "+p.id+" "+at);
                    groundBody++;
                    require(bodyBlocked(candidate,at,y+JUMP_RISE,posture),"Jump crossing at "+p.id+" "+at);
                    jumpBody++;
                    if(!bodyBlocked(base,at,y,posture))baselineOpen++;
                    if(p.gate){
                        require(bodyBlocked(candidate,at,y-.11,posture),"Under-gate body gap at "+at);
                        gateUnder++;
                    }
                    if(j==0||j==stations)ends++;
                    for(double direction:new double[]{1,-1}){
                        Vec from=null;
                        for(double distance:new double[]{.62,.78,.95,1.14}){
                            var approach=at.shift(nx,nz,direction*distance);
                            var support=support(base,approach,y,.25);
                            if(support!=null&&candidate.traversable(approach.x,support,approach.z,posture)){
                                from=approach.at(support);
                                break;
                            }
                        }
                        if(from==null)continue;
                        require(walk(candidate,from,at.shift(nx,nz,-direction*.21),posture)==null,
                            "Supported traversal crossed "+p.id+" "+at);
                        walkSweeps++;
                    }
                }
                var a=at.shift(nx,nz,1).at(y+1.55);
                var b=at.shift(nx,nz,-1).at(y+1.55);
                if(candidate.clearLineOfSight(a.x(),a.y(),a.z(),b.x(),b.y(),b.z()))sightGaps++;
            }
        }
        require(gates==2&&baselineOpen>500&&walkSweeps>280&&sightGaps>35,
            "Incomplete perimeter controls: gates="+gates+" baseline="+baselineOpen+" movers="+walkSweeps+" optical="+sightGaps);
        System.out.printf("PARCEL bays=%d ground_body=%d jump_body=%d joins=%d supported_sweeps=%d baseline_unblocked=%d open_iron_rays=%d gate_under=%d%n",
            panels.size(),groundBody,jumpBody,ends,walkSweeps,baselineOpen,sightGaps,gateUnder);
    }

    private static void vertical(HouseMap base,HouseMap candidate,List<Rim> rims) {
        require(rims.size()==15,"Five stairwells need three protected rim edges each");
        int blocked=0,baselineOpen=0,approaches=0;
        for(var rim:rims){
            int segmentCount=(int)Math.ceil(Math.hypot(rim.b.x-rim.a.x,rim.b.z-rim.a.z)/.16);
            for(int i=0;i<=segmentCount;i++){
                var at=rim.a.mix(rim.b,i/(double)segmentCount);
                for(double posture:HEIGHTS){
                    require(bodyBlocked(candidate,at,rim.y,posture),"Open guard base "+rim+" "+at);
                    require(bodyBlocked(candidate,at,rim.y+JUMP_RISE,posture),"Jump crossed guard "+rim+" "+at);
                    blocked+=2;
                    if(!bodyBlocked(base,at,rim.y+JUMP_RISE,posture))baselineOpen++;
                    var start=at.shift(rim.dx,rim.dz,-.52);
                    if(candidate.traversable(start.x,rim.y,start.z,posture)){
                        require(walk(candidate,start.at(rim.y),at.shift(rim.dx,rim.dz,.32),posture)==null,
                            "Supported body entered the stair void alongside the guard "+rim+" "+at);
                        approaches++;
                    }
                }
            }
        }
        require(baselineOpen>100&&approaches>100,"Missing baseline or guard approaches "+baselineOpen+"/"+approaches);
        System.out.printf("WELLS guard_spans=%d blocked_ground_jump=%d baseline_unblocked=%d supported_sweeps=%d%n",
            rims.size(),blocked,baselineOpen,approaches);
    }

    private static void jumpAtGuard(HouseMap map,Rim rim){
        var edge=rim.a.mix(rim.b,.46);
        var start=edge.shift(rim.dx,rim.dz,-.68);
        require(map.traversable(start.x,rim.y,start.z),"Jump start lacks deck bearing "+rim);
        var motion=new DefaultPlayerMotion();
        var collision=new DefaultPlayerMotion.Collision(){
            public boolean clear(double x,double y,double z,double height){return map.airborneClear(x,y,z,height);}
            public double landing(double x,double z,double from,double to){return map.jumpLanding(x,z,from,to);}
        };
        motion.jump(start.x,rim.y,start.z, rim.dx*3.1,rim.dz*3.1,CROUCH_HEIGHT,collision);
        boolean advanced=false;
        for(int step=0;step<12&&motion.active();step++){
            motion.advance(.1,CROUCH_HEIGHT,collision);
            double side=(motion.x()-edge.x)*rim.dx+(motion.z()-edge.z)*rim.dz;
            require(side<-.05,"Live jump passed beyond protected stair rim "+rim);
            advanced|=side>-.58;
        }
        require(advanced&&!motion.airborne()&&Math.abs(motion.y()-rim.y)<.05,
            "Guard did not return the normal jump to its starting deck "+rim+" advanced="+advanced+" airborne="+motion.airborne()+" xyz="+motion.x()+","+motion.y()+","+motion.z());
    }

    private static void verifyGhost(HouseMap baseline,HouseMap physical){
        var old=new LanGhostSpatial(baseline,13);
        var guarded=new LanGhostSpatial(physical,13);
        for(int tick=0;tick<5;tick++){
            old.step(100,0,tick);
            guarded.step(100,0,tick);
        }
        boolean physicalStop=false;
        for(int tick=5;tick<86;tick++){
            try{guarded.step(0,100,tick);}
            catch(IllegalArgumentException wall){
                require(wall.getMessage().startsWith("spectral room boundary"),wall.getMessage());
                physicalStop=true;
            }
            old.step(0,100,tick);
            if(physicalStop)break;
        }
        require(physicalStop&&guarded.pose().z()>100.3&&guarded.pose().z()<103
            &&old.pose().z()>guarded.pose().z()+.25,"Ghost approach failed away from the physical return");
        System.out.printf(java.util.Locale.US,
            "GHOST native outdoor mover stopped at %.3f m, prior ground accepts %.3f m%n",
            guarded.pose().z(),old.pose().z());
    }

    private static void frame(HouseMap map,Path folder,String name,Vec eye,double tx,double tz,double pitch)throws Exception {
        require(map.traversable(eye.x(),eye.y(),eye.z()),"Unsupported standing view "+name+" "+eye);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(1120,700);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=new RoomLighting();
        renderer.weatherModule().setTimeOfDay(.52);
        renderer.weatherModule().pauseClock(true);
        renderer.x=eye.x();renderer.z=eye.z();renderer.eyeHeight=eye.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(tx-eye.x(),tz-eye.z());renderer.pitch=pitch;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        renderer.drawNetwork(map,List.of(),-1,0);
        var gpu=GpuWorldRenderer.active();
        if(gpu!=null){
            require(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No OpenGL containment frame");
            renderer.pinCompletedGpuFrame(gpu.latest());
            renderer.drawNetwork(map,List.of(),-1,0);
            require(renderer.frameDiagnostics().gpuReadbackAvailable(),"No hardware readback for "+name);
        }
        require(ImageIO.write(renderer.image(),"png",folder.resolve(name+".png").toFile()),"No rendered capture "+name);
    }

    private static void captures(HouseMap before,HouseMap after,Path output,boolean gpu)throws Exception {
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
            var gate=new Point(37.7,97.8);
            var yard=new Point(28.4,100.7);
            var returnView=new Point(67.4,44.5);
            for(var sample:List.of(gate,yard,returnView))
                require(support(after,sample,0,.65)!=null,"No yard support at "+sample);
            frame(after,output,"service-gate",gate.at(support(after,gate,0,.65)),31.9,103,-.06);
            frame(after,output,"arrival-van",yard.at(support(after,yard,0,.65)),33,85,-.04);
            frame(after,output,"east-fence-return",returnView.at(support(after,returnView,0,.65)),70,41,-.05);
            frame(after,output,"north-tower-guard",new Vec(63.4,3.6,33.9),65,34.3,-.26);
            frame(after,output,"service-cellar-guard",new Vec(44,0,69.9),46,71,-.23);
            var camera=new Vec(20.8,0,27.74);
            for(var map:List.of(before,after)){
                map.applyDoorSnapshot("");
                int id=map.nativeInvestigation().doors().stream().filter(d->d.spec().frameId().equals("L06"))
                    .findFirst().orElseThrow().spec().portal().id();
                map.toggleDoor(id,System.nanoTime()-2_000_000_000L);
                frame(map,output,map==before?"seminar-before":"seminar-bearing",camera,20,28,-.10);
            }
        }finally{
            if(stop!=null)stop.invoke(null);
        }
    }

    public static void main(String[] args)throws Exception {
        Path packet=Path.of(args[0]);
        var baseline=CollegeNativeRuntimeReview.load(BASE.resolve("verification/runtime.tsv"),
            BASE.resolve("college/college-doors.glb"),BASE.resolve("college/college-doors.hcs2"));
        var manifest=Path.of(args[1]);
        var glb=packet.resolve("college-containment.glb");
        var hcs=packet.resolve("college-containment.hcs2");
        var candidate=CollegeNativeRuntimeReview.load(manifest,glb,hcs);
        var schedule=read(Path.of(args[2]));
        perimeter(baseline.map(),candidate.map(),schedule.panels);
        vertical(baseline.map(),candidate.map(),schedule.rims);
        int jumps=0;
        for(var rim:schedule.rims){
            // The west cellar's retained hallway masonry screens its west
            // side. The open deck approach is on the east side of that well.
            if(!rim.side.equals(rim.stair.equals("L84")?"east rim":"west rim"))continue;
            jumpAtGuard(candidate.map(),rim);
            jumps++;
        }
        System.out.println("JUMP host motion replays above deck="+jumps);
        verifyGhost(baseline.map(),candidate.map());
        CollegeNativeDoorsTest.audit(candidate);
        if(args.length>3&&args[3].equals("--geometry-only"))return;
        CollegeNativeDoorsTest.peers(candidate.map(),CollegeNativeRuntimeReview.load(manifest,glb,hcs).map());
        CollegeNativeElectricalTest.verifyControlsAndPower(candidate.map());
        if(args.length>3)captures(baseline.map(),candidate.map(),Path.of(args[3]),List.of(args).contains("--gpu"));
    }
}
