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
        for(var door:doors){
            if(!NativeSchoolInteriorReview.sweep(scene.physics(),door))throw new AssertionError("Swing "+door.frameId());
            var portal=door.portal();
            int side=portal.id()==470?1:-1;
            NativeSchoolInteriorReview.audit(map,door,false);
            if(connected?portal.id()>=474:portal.id()==470||portal.id()==473)
                NativeSchoolInteriorReview.render(map,portal,captures.resolve("asylum-"+portal.id()+"-closed.png"),side);
            map.toggleDoor(portal.id());
            guest.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(guest,door,true);
            if(connected?portal.id()>=474:portal.id()==470||portal.id()==473)
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
