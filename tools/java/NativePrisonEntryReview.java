package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

final class NativePrisonEntryReview {
    private static void check(boolean pass,String message){
        if(!pass)throw new AssertionError(message);
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

    private static void review(String[] args)throws Exception{
        var packet=Path.of(args[0]);
        var rows=Files.readAllLines(packet.resolve("arrival.tsv")).stream().map(line->line.split("\\t",-1)).toList();
        var source=rows.get(0);
        var staging=rows.get(1);
        var dock=new Dock(d(staging,1),d(staging,2),d(staging,3),d(staging,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var path=new ArrayList<Vec>();
        var sockets=new ArrayList<Socket>();
        for(var row:rows)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),d(row,3),
                d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("prison-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "POINT" -> path.add(new Vec(d(row,1),d(row,2),d(row,3)));
            default -> {}
        }
        var specs=new ArrayList<NativeInvestigationDoor.Spec>();
        for(String line:Files.readAllLines(packet.resolve("door-survey.tsv"))){
            var f=line.split("\\t",-1);
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),d(f,6),d(f,7),d(f,8),
                d(f,9),f[10].equals("z"),"door",1);
            specs.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        var records=NativePrisonMissionReview.read(packet);
        var stairParts=records.stream().filter(r->r[0].equals("TREAD")).map(r->r[1])
            .collect(java.util.stream.Collectors.toSet());
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(Path.of(source[1]));var collision=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairParts,spaces,specs);
        }
        var gatePath=packet.resolve("gate.tsv");
        if(Files.isRegularFile(gatePath)){
            var gateRows=Files.readAllLines(gatePath).stream().map(line->line.split("\t",-1)).toList();
            var f=gateRows.get(0);
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            var spec=new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),1,1,1,f[15]);
            try(var leaf=Files.newInputStream(Path.of(f[14]))){
                scene=scene.withSlidingDoor(spec,leaf,d(f,16));
            }
            for(var r:gateRows.subList(1,gateRows.size()))
                sockets.add(new Socket(r[0],"gate_release",Integer.parseInt(r[1]),d(r,2),d(r,3),d(r,4),d(r,5),r[6]));
        }
        var existing=LevelPlans.buildSource(HouseMap.Site.PRISON);
        var portals=new ArrayList<>(scene.doors().stream().map(door->door.spec().portal()).toList());
        var anchors=records.isEmpty()?existing.rooms():NativePrisonMissionReview.bind(scene,dock,records,portals);
        var definition=new LevelDefinition("prison",scene.revision(),existing.title(),anchors,
            portals,List.of(),List.of(),List.of(),
            sockets,List.of(),dock,existing.ambience(),existing.water(),existing.sounds());
        var map=new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
        var guest=new HouseMap(HouseMap.Site.PRISON,definition,null,scene);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
        var captures=List.of(args).contains("--gpu")?packet.resolve("captures/opengl"):packet.resolve("captures");
        Files.createDirectories(captures);
        for(var spec:specs){
            check(NativeSchoolInteriorReview.sweep(scene.physics(),spec),"Obstructed hinge sweep "+spec.frameId());
            var portal=spec.portal();
            var side=portal.id()==450?1:-1;
            NativeSchoolInteriorReview.audit(map,spec,false);
            if(!Files.isRegularFile(gatePath))NativeSchoolInteriorReview.render(map,portal,captures.resolve("prison-entry-"+portal.id()+"-closed.png"),side);
            map.toggleDoor(spec.portal().id(),System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(map.doorSnapshot());
            NativeSchoolInteriorReview.audit(map,spec,true);
            NativeSchoolInteriorReview.audit(guest,spec,true);
            if(!Files.isRegularFile(gatePath))NativeSchoolInteriorReview.render(map,portal,captures.resolve("prison-entry-"+portal.id()+"-open.png"),side);
            report.add(spec.frameId()+"\tClosed/open/guest body, sight, aim, acoustics and object checks passed");
        }
        if(Files.isRegularFile(gatePath))NativePrisonGateReview.audit(map,guest,captures,report);
        if(!records.isEmpty()){
            NativePrisonMissionReview.audit(map,records,report);
            if(List.of(args).contains("--lan"))
                NativePrisonGateReview.liveRelease(new HouseMap(HouseMap.Site.PRISON,definition,null,scene),report);
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
