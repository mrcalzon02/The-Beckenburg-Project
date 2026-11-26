package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

final class NativePrisonEntryReview {
    private static void check(boolean pass,String message){
        if(!pass)throw new AssertionError(message);
    }

    private static List<String[]> table(Path path)throws Exception{
        if(!Files.isRegularFile(path))return List.of();
        return Files.readAllLines(path).stream().map(line->line.split("\t",-1)).toList();
    }

    private static double d(String[] row,int i){
        return Double.parseDouble(row[i]);
    }

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

    private static void renderCupboard(HouseMap map,LevelDefinition.Socket cabinet,Path captures)throws Exception{
        var camera=LevelClosets.world(cabinet,.15,0,1.9);
        check(map.traversable(camera.x(),camera.y(),camera.z()),"No standing view of "+cabinet.id());
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=camera.x();
        renderer.z=camera.z();
        renderer.eyeHeight=camera.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(cabinet.x()-camera.x(),cabinet.z()-camera.z());
        renderer.pitch=-.08;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,captures.resolve(cabinet.id()+".png"));
    }

    private static void review(String[] args)throws Exception{
        var packet=Path.of(args[0]);
        var rows=table(packet.resolve("arrival.tsv"));
        var schedule=table(packet.resolve("door-survey.tsv"));
        var records=table(packet.resolve("mission.tsv"));
        var gate=table(packet.resolve("gate.tsv"));
        var vehicle=table(packet.resolve("vehicle-gate.tsv"));
        var services=table(packet.resolve("services.tsv"));
        var source=rows.get(0);
        var path=new ArrayList<Vec>();
        for(var row:rows)if(row[0].equals("POINT"))path.add(new Vec(d(row,1),d(row,2),d(row,3)));
        PrisonNativeLevel.Source staged;
        try(var model=Files.newInputStream(Path.of(source[1]));
            var collision=Files.newInputStream(Path.of(source[3]));
            var leaf=gate.isEmpty()?null:Files.newInputStream(Path.of(gate.get(0)[14]));
            var vehicleLeaf=vehicle.isEmpty()?null:Files.newInputStream(Path.of(vehicle.get(0)[14]))){
            var gates=new ArrayList<PrisonNativeLevel.GateSource>();
            if(leaf!=null)gates.add(new PrisonNativeLevel.GateSource(gate,leaf));
            if(vehicleLeaf!=null)gates.add(new PrisonNativeLevel.GateSource(vehicle,vehicleLeaf));
            staged=PrisonNativeLevel.read(rows,schedule,records,gates,services,model,collision);
        }
        var scene=staged.scene();
        var definition=staged.definition();
        var dock=definition.dock();
        var specs=staged.swingingDoors();
        boolean packaged=List.of(args).contains("--packaged");
        if(packaged){
            check(!new HouseMap(HouseMap.Site.PRISON).level().equals(definition),"Prison review must be opt-in");
            System.setProperty(PrisonNativeLevel.REVIEW_PROPERTY,"true");
        }
        var map=packaged?new HouseMap(HouseMap.Site.PRISON):new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
        var guest=packaged?HouseMap.forNetworkRevision(HouseMap.Site.PRISON,definition.revision())
            :new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
        if(packaged){
            check(map.level().equals(definition)&&guest.level().equals(definition),"Prison package is not the surveyed source");
            NativeSchoolInteriorReview.auditSolo(SiteCatalog.Location.PRISON,definition);
        }
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
        if(packaged)report.add("packaged_solo_case\tRegular Prison selection and supported cargo spawn");
        var captures=List.of(args).contains("--gpu")?packet.resolve("captures/opengl"):packet.resolve("captures");
        Files.createDirectories(captures);
        for(var spec:specs){
            check(NativeSchoolInteriorReview.sweep(scene.physics(),spec),"Obstructed hinge sweep "+spec.frameId());
            var portal=spec.portal();
            var side=portal.id()==450?1:-1;
            NativeSchoolInteriorReview.audit(map,spec,false);
            if(gate.isEmpty())NativeSchoolInteriorReview.render(map,portal,captures.resolve("prison-entry-"+portal.id()+"-closed.png"),side);
            map.toggleDoor(spec.portal().id(),System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,spec,true);
            NativeSchoolInteriorReview.audit(guest,spec,true);
            if(gate.isEmpty())NativeSchoolInteriorReview.render(map,portal,captures.resolve("prison-entry-"+portal.id()+"-open.png"),side);
            report.add(spec.frameId()+"\tClosed/open/guest body, sight, aim, acoustics and object checks passed");
        }
        if(!gate.isEmpty())NativePrisonGateReview.audit(map,guest,captures,report);
        if(!vehicle.isEmpty())NativePrisonVehicleReview.audit(map,guest,captures,report);
        NativeSchoolInteriorReview.auditHiding(map,report);
        for(var cabinet:HidingBudget.candidates(map.level()))renderCupboard(map,cabinet,captures);
        if(!services.isEmpty())
            NativeSchoolInteriorReview.auditPower(map,captures,report,"prison",new Vec(38,-3,24.95),Math.PI);
        if(!records.isEmpty()){
            NativePrisonMissionReview.audit(map,records,report);
            if(List.of(args).contains("--lan")){
                var fresh=packaged?new HouseMap(HouseMap.Site.PRISON):new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
                NativePrisonGateReview.liveRelease(fresh,packaged,report);
                if(!vehicle.isEmpty())NativePrisonVehicleReview.live(definition,scene,packaged,report);
            }
        }
        var lip=map.van().world(0,VanStaging.FRONT);
        var grade=map.van().world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.6);
        var deck=new Vec(dock.x(),dock.y()+VanStaging.FLOOR_Y,dock.z());
        var outside=new Vec(grade.x(),dock.y(),grade.z());
        check(map.roomAt(deck.x(),deck.y(),deck.z())==14&&map.roomAt(outside.x(),outside.y(),outside.z())==13,
            "Prison van and public approach ownership");
        path.add(0,new Vec(outside.x(),outside.y(),path.get(0).z()));
        path.add(0,outside);
        path.add(0,new Vec(lip.x(),deck.y(),lip.z()));
        path.add(0,deck);
        Files.write(packet.resolve("verified-cargo-guide.tsv"),path.stream()
            .map(v->v.x()+"\t"+v.y()+"\t"+v.z()).toList());
        var reverse=new ArrayList<>(path);
        Collections.reverse(reverse);
        for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            NativeSchoolInteriorReview.walk(map,path,height,"Prison cargo to reception");
            NativeSchoolInteriorReview.walk(map,reverse,height,"Prison reception to cargo");
        }
        report.add("supported_cargo_arrival\tVan to reception and return in both postures");
        var verification=packet.resolve("verification");
        Files.createDirectories(verification);
        Files.write(verification.resolve(List.of(args).contains("--gpu")?"opengl.tsv":"staged-approach.tsv"),report);
        NativeSchoolInteriorReview.exportTriangles(scene,Path.of(args[1]));
        report.forEach(System.out::println);
    }
}
