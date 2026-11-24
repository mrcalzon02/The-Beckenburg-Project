package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

final class NativeAsylumEntryReview {
    private static List<String[]> table(Path path)throws Exception{
        if(!Files.isRegularFile(path))return List.of();
        return Files.readAllLines(path).stream().map(line->line.split("\t",-1)).toList();
    }

    private static double d(String[] row,int index){return Double.parseDouble(row[index]);}

    public static void main(String[] args)throws Exception{
        if(!List.of(args).contains("--gpu")){review(args);return;}
        var launcher=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=launcher.getDeclaredMethod("startGpu");
        var stop=launcher.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try{review(args);}finally{stop.invoke(null);}
    }

    private static void review(String[] args)throws Exception{
        var root=Path.of(args[0]);
        var output=root.resolve("verification");
        boolean gpu=List.of(args).contains("--gpu");
        var captures=root.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(output);
        Files.createDirectories(captures);
        var rows=Files.readAllLines(root.resolve("arrival.tsv")).stream().map(line->line.split("\\t",-1)).toList();
        var file=rows.get(0);
        var mission=table(root.resolve("mission.tsv"));
        var services=table(root.resolve("services.tsv"));
        boolean connected=!mission.isEmpty(),powered=!services.isEmpty();
        var walks=new LinkedHashMap<String,List<Vec>>();
        for(var row:rows)if(row[0].equals("POINT"))
            walks.computeIfAbsent(row[1],key->new ArrayList<>()).add(new Vec(d(row,2),d(row,3),d(row,4)));
        AsylumNativeLevel.Source packet;
        try(var model=Files.newInputStream(Path.of(file[1]));var collision=Files.newInputStream(Path.of(file[3]))){
            packet=AsylumNativeLevel.read(rows,table(root.resolve("door-survey.tsv")),mission,services,model,collision);
        }
        var scene=packet.scene();
        var definition=packet.definition();
        var doors=packet.doors();
        var dock=definition.dock();
        boolean packaged=List.of(args).contains("--packaged");
        if(packaged){
            if(new HouseMap(HouseMap.Site.ASYLUM).level().equals(definition))
                throw new AssertionError("Asylum review must be opt-in");
            System.setProperty(AsylumNativeLevel.REVIEW_PROPERTY,"true");
        }
        var map=packaged?new HouseMap(HouseMap.Site.ASYLUM):new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene);
        var guest=packaged?HouseMap.forNetworkRevision(HouseMap.Site.ASYLUM,definition.revision())
            :new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene);
        if(packaged){
            if(!map.level().equals(definition)||!guest.level().equals(definition))
                throw new AssertionError("Asylum package differs from surveyed source");
            NativeSchoolInteriorReview.auditSolo(SiteCatalog.Location.ASYLUM,definition);
        }
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
        if(packaged)report.add("packaged_solo_case\tOrdinary Asylum selection and supported cargo spawn");
        boolean interiorFittings=doors.stream().anyMatch(door->door.portal().id()>=476);
        for(var door:doors){
            if(!NativeSchoolInteriorReview.sweep(scene.physics(),door))throw new AssertionError("Swing "+door.frameId());
            var portal=door.portal();
            int side=portal.id()==470?1:-1;
            boolean capture=interiorFittings?portal.id()>=476:connected?portal.id()>=474:portal.id()==470||portal.id()==473;
            NativeSchoolInteriorReview.audit(map,door,false);
            if(capture)
                NativeSchoolInteriorReview.render(map,portal,captures.resolve("asylum-"+portal.id()+"-closed.png"),side);
            map.toggleDoor(portal.id());
            guest.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(guest,door,true);
            if(capture)
                NativeSchoolInteriorReview.render(map,portal,captures.resolve("asylum-"+portal.id()+"-open.png"),side);
            report.add(door.frameId()+"\tFitted sweep, movement, acoustics, sight, objects and second staged world");
        }
        var lip=map.van().world(0,VanStaging.FRONT);
        var beyond=map.van().world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.60);
        var cargo=new Vec(dock.x(),dock.y()+VanStaging.FLOOR_Y,dock.z());
        var outside=new Vec(beyond.x(),dock.y(),beyond.z());
        if(map.roomAt(cargo.x(),cargo.y(),cargo.z())!=14||map.roomAt(outside.x(),outside.y(),outside.z())!=13)
            throw new AssertionError("Van vs apron ownership");
        var vehicleApproach=new ArrayList<>(List.of(cargo,new Vec(lip.x(),cargo.y(),lip.z()),outside,
            new Vec(outside.x(),0,72),new Vec(32,0,72)));
        vehicleApproach.addAll(walks.get("L19").subList(1,walks.get("L19").size()));
        vehicleApproach.addAll(walks.get("L20").subList(1,walks.get("L20").size()));
        NativeSchoolInteriorReview.auditHiding(map,report);
        auditCoverThresholds(map,report);
        var masonry=table(root.resolve("wall-runs.tsv"));
        auditMasonry(map,masonry,captures,report);
        boolean upperMasonry=masonry.size()==6;
        if(upperMasonry){
            auditMainStairGuards(map,table(root.resolve("guard-runs.tsv")),report);
            auditMainStair(map,captures,report);
            auditFlightHandrails(map,table(root.resolve("handrail-supports.tsv")),captures,report);
        }
        for(var cupboard:HidingBudget.candidates(map.level()))captureCupboard(map,cupboard,captures);
        if(powered)NativeSchoolInteriorReview.auditPower(map,captures,report,"asylum",new Vec(40,-3,27.25),Math.PI);
        if(connected){
            NativeAsylumMissionReview.audit(map,report);
            for(var guide:walks.entrySet())
                verifyWalk(map,guide.getKey().equals("L19")?vehicleApproach:guide.getValue(),"Asylum "+guide.getKey());
            report.add("supported_cargo_and_admission\t"+4*walks.size()+" direction and posture replays over "+walks.size()+" physical routes");
            if(List.of(args).contains("--lan"))
                NativeAsylumMissionReview.live(packaged?new HouseMap(HouseMap.Site.ASYLUM)
                    :new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene),packaged,report);
            if(upperMasonry&&packaged&&List.of(args).contains("--lan"))
                NativeAsylumMissionReview.liveMainStair(report);
        }else{
            verifyWalk(map,vehicleApproach,"Asylum van and admission");
            for(String guide:List.of("L22","L23","L24"))verifyWalk(map,walks.get(guide),"Asylum "+guide);
            report.add("supported_cargo_and_admission\t16 direction and posture replays over four physical routes");
        }
        report.add("extraction_owner\tVan interior 14 and paved forecourt 13");
        Files.write(output.resolve(gpu?"opengl.tsv":"staged-admission.tsv"),report);
        Files.write(root.resolve("verified-cargo-guide.tsv"),vehicleApproach.stream()
            .map(v->v.x()+"\t"+v.y()+"\t"+v.z()).toList());
        NativeSchoolInteriorReview.exportTriangles(scene,Path.of(args[1]));
        report.forEach(System.out::println);
    }

    private static void auditMasonry(HouseMap map,List<String[]> rows,Path captures,List<String> report)throws Exception{
        if(rows.isEmpty())return;
        if(!List.of(rows.get(0)).equals(List.of("side","x0","x1","z0","z1","sill","head"))
                ||rows.size()!=11&&rows.size()!=6)
            throw new AssertionError("Incomplete spine wall schedule");
        boolean upper=rows.size()==6;
        double floor=upper?3.4:0;
        String storey=upper?"upper":"ground";
        var physics=map.nativeInvestigation().physics();
        int bearing=0,occlusion=0;
        for(var row:rows.subList(1,rows.size())){
            boolean west=row[0].equals("west");
            double x0=d(row,1),x1=d(row,2),start=d(row,3),end=d(row,4);
            double sill=d(row,5),head=d(row,6);
            if(Math.abs(sill-floor)>.001||Math.abs(head-sill-3.4)>.001)
                throw new AssertionError("Wrong spine wall datum "+row[0]+" "+start);
            int stations=(int)Math.ceil((end-start)/.24);
            for(int i=0;i<=stations;i++){
                double z=start+.02+(end-start-.04)*i/stations;
                var support=physics.support(west?x1+.025:x0-.025,z,sill,.035);
                if(support==null||Math.abs(support.y()-sill)>.03)
                    throw new AssertionError("Missing "+storey+" spine slab at "+row[0]+" "+z);
                bearing++;
                for(double height:new double[]{.07,.45,1.2,2.15,2.25,head-sill-.08}){
                    var near=new Vec(x0-.04,sill+height,z);
                    var far=new Vec(x1+.04,sill+height,z);
                    if(physics.clearSight(near,far)||physics.clearSight(far,near))
                        throw new AssertionError("Spine masonry leaks at "+row[0]+" "+z+" "+height);
                    occlusion+=2;
                }
            }
        }
        String caption=upper?"asylum-upper-spine-":"asylum-spine-";
        captureStanding(map,new Vec(27.5,floor,upper?38.7:46.8),Math.PI,-.06,captures.resolve(caption+"north.png"));
        captureStanding(map,new Vec(27.5,floor,upper?14.3:17.4),0,-.06,captures.resolve(caption+"south.png"));
        report.add("continuous_"+storey+"_spine\t"+(rows.size()-1)+" solid wall runs; "+bearing
            +" adjacent slab contacts and "+occlusion+" bidirectional wall sight checks");
    }

    private static void auditMainStairGuards(HouseMap map,List<String[]> rows,List<String> report){
        if(rows.size()!=4)throw new AssertionError("Expected all three exposed slab edges guarded");
        var physics=map.nativeInvestigation().physics();
        int stations=0,bays=0,bearings=0;
        for(var row:rows.subList(1,rows.size())){
            double x0=d(row,1),z0=d(row,2),x1=d(row,3),z1=d(row,4),sill=d(row,5);
            int elements=Integer.parseInt(row[7]);
            bays+=elements;
            bearings+=Integer.parseInt(row[8]);
            int checks=(int)Math.ceil(Math.hypot(x1-x0,z1-z0)/.14);
            for(int i=1;i<checks;i++){
                double f=(double)i/checks;
                var at=new Vec(x0+(x1-x0)*f,sill,z0+(z1-z0)*f);
                for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT})
                    if(!physics.bodyBlocked(at,height,.22))throw new AssertionError("Guard hull gap "+at);
                stations++;
            }
        }
        if(bays!=7||bearings<12)throw new AssertionError("Missing guard bays or supported feet");
        report.add("upper_main_stair_guards\t"+bays+" supported bays on three sides, "+bearings
            +" slab-bearing vertices and "+stations+" blocking stations in both postures");
    }

    private static void auditMainStair(HouseMap map,Path captures,List<String> report)throws Exception{
        for(double[] opening:new double[][]{{26,20.5},{26,24},{29,28}}){
            Vec left=new Vec(opening[0]-.55,3.4,opening[1]);
            Vec right=new Vec(opening[0]+.55,3.4,opening[1]);
            verifyWalk(map,List.of(left,right),"upper-spine threshold "+left.z());
        }
        var physics=map.nativeInvestigation().physics();
        var at=new Vec(32,0,21.1);
        int probes=0;
        while(at.z()<27.89){
            var next=map.resolveMove(at,32,Math.min(27.9,at.z()+.12),HunterPosture.STANDING_HEIGHT);
            if(next==null||next.z()<=at.z())
                throw new AssertionError("Main stair interrupted "+at);
            for(double x:new double[]{31.68,32,32.32}){
                var foot=new Vec(x,next.y()+.025,next.z());
                var above=new Vec(x,next.y()+2.20,next.z());
                if(!physics.clearSight(foot,above))
                    throw new AssertionError("Main stair headroom below 2.20 m at "+foot);
                probes++;
            }
            at=next;
        }
        if(Math.abs(at.y()-3.4)>.025||map.roomAt(at.x(),at.y(),at.z())!=7)
            throw new AssertionError("Main stair misses upper landing "+at);
        captureStanding(map,new Vec(32,3.4,28.7),Math.PI,-.36,captures.resolve("asylum-upper-main-stair.png"));
        report.add("upper_main_stair_headroom\t3 supported upper openings and "+probes+" clear 2.20 m vertical probes");
    }

    private static void auditFlightHandrails(HouseMap map,List<String[]> rows,Path captures,List<String> report)throws Exception{
        if(rows.isEmpty())return;
        if(rows.size()!=13)throw new AssertionError("Expected six mounted posts per stair side");
        var physics=map.nativeInvestigation().physics();
        for(var row:rows.subList(1,rows.size())){
            double x=d(row,2),y=d(row,3),z=d(row,4);
            var support=physics.support(x,z,y,.04);
            if(support==null||Math.abs(support.y()-y)>.02||!physics.bodyBlocked(new Vec(x,y+.04,z),1.1,.20))
                throw new AssertionError("Flight post lacks support or collision at "+x+" "+y+" "+z);
        }
        for(double x:new double[]{31.62,32.38})
            verifyWalk(map,List.of(new Vec(x,0,21.1),new Vec(x,3.4,28.2)),"full flight inside rail at X"+x);
        captureStanding(map,new Vec(32,0,21.1),0,.20,captures.resolve("asylum-main-stair-lower.png"));
        report.add("flight_handrails\t12 collidable tread-mounted posts; 8 full flight replays inside both rail lines");
    }

    private static void captureStanding(HouseMap map,Vec camera,double yaw,double pitch,Path target)throws Exception{
        if(!map.traversable(camera.x(),camera.y(),camera.z()))
            throw new AssertionError("No standing view of the spine at "+camera);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;
        renderer.pitch=pitch;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,target);
    }

    private static void auditCoverThresholds(HouseMap map,List<String> report){
        var cupboards=HidingBudget.candidates(map.level());
        if(cupboards.isEmpty())return;
        map.configureHiding(100);
        for(var socket:cupboards){
            // Points are just before and after the leaf plane, well outside the old central pocket.
            var outside=LevelClosets.world(socket,0,0,.68);
            var entry=LevelClosets.world(socket,0,0,.51);
            var rear=LevelClosets.world(socket,0,0,-.44);
            var airborne=LevelClosets.world(socket,0,.55,.51);
            var above=LevelClosets.world(socket,0,2.15,.51);
            if(SafeAreaBarrier.protectedAt(map,outside.x(),outside.y(),outside.z())
                    ||!SafeAreaBarrier.protectedAt(map,entry.x(),entry.y(),entry.z())
                    ||!SafeAreaBarrier.protectedAt(map,rear.x(),rear.y(),rear.z())
                    ||!SafeAreaBarrier.protectedAt(map,airborne.x(),airborne.y(),airborne.z())
                    ||SafeAreaBarrier.protectedAt(map,above.x(),above.y(),above.z())
                    ||!SafeAreaBarrier.blocks(map,outside,entry))
                throw new AssertionError("Incorrect cupboard protection at actual threshold "+socket.id());
            var interior=LevelClosets.world(socket,0,0,-.24);
            verifyWalk(map,List.of(outside,entry,interior),socket.id()+" protected physical volume");
        }
        map.configureHiding(0);
        for(var socket:cupboards){
            var entry=LevelClosets.world(socket,0,0,.51);
            var outside=LevelClosets.world(socket,0,0,.68);
            if(SafeAreaBarrier.protectedAt(map,entry.x(),entry.y(),entry.z())||SafeAreaBarrier.blocks(map,outside,entry))
                throw new AssertionError("Disabled cupboard still protects "+socket.id());
        }
        map.configureHiding(70);
        report.add("full_cupboard_safe_thresholds\tThree usable interiors, full protection height, hunter crossing both ways and zero protection with cover disabled");
    }

    private static void captureCupboard(HouseMap map,LevelDefinition.Socket cupboard,Path captures)throws Exception{
        var camera=LevelClosets.world(cupboard,.14,0,1.9);
        if(!map.traversable(camera.x(),camera.y(),camera.z()))
            throw new AssertionError("No supported standing view of "+cupboard.id());
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(cupboard.x()-camera.x(),cupboard.z()-camera.z());
        renderer.pitch=-.08;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,captures.resolve(cupboard.id()+".png"));
    }

    private static void verifyWalk(HouseMap map,List<Vec> route,String name){
        var back=new ArrayList<>(route);
        Collections.reverse(back);
        for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            NativeSchoolInteriorReview.walk(map,route,height,name+" forward");
            NativeSchoolInteriorReview.walk(map,back,height,name+" return");
        }
    }
}
