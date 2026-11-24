package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

final class NativeAsylumEntryReview {
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
        var source=rows.get(0);
        var placement=rows.get(1);
        var dock=new Dock(d(placement,1),d(placement,2),d(placement,3),d(placement,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var stairs=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var walks=new LinkedHashMap<String,List<Vec>>();
        for(var row:rows)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "RITUAL" -> sockets.add(new Socket("asylum-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "TREAD" -> stairs.add(row[1]);
            case "POINT" -> walks.computeIfAbsent(row[1],key->new ArrayList<>()).add(new Vec(d(row,2),d(row,3),d(row,4)));
            default -> {}
        }
        var doors=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var line:Files.readAllLines(root.resolve("door-survey.tsv"))){
            var f=line.split("\\t",-1);
            var portal=new Portal(Integer.parseInt(f[0]),Integer.parseInt(f[4]),Integer.parseInt(f[5]),
                d(f,6),d(f,7),d(f,8),d(f,9),f[10].equals("z"),"door",1);
            doors.add(new NativeInvestigationDoor.Spec(f[1],f[2],f[3],portal,d(f,11),d(f,12),d(f,13),
                Integer.parseInt(f[14]),Integer.parseInt(f[15]),Integer.parseInt(f[16]),f[17]));
        }
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(Path.of(source[1]));var collision=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(model,source[2],collision,source[4],stairs,spaces,doors);
        }
        var old=LevelPlans.buildSource(HouseMap.Site.ASYLUM);
        var mission=root.resolve("mission.tsv");
        boolean connected=Files.isRegularFile(mission);
        var portals=new ArrayList<>(doors.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var owners=connected?NativeAsylumMissionReview.bind(scene,dock,
            Files.readAllLines(mission).stream().map(line->line.split("\t",-1)).toList(),portals):old.rooms();
        var definition=new LevelDefinition("asylum",scene.revision(),old.title(),owners,
            portals,List.of(),List.of(),List.of(),sockets,List.of(),dock,old.ambience(),old.water(),old.sounds());
        var map=new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene);
        var guest=new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene);
        var report=new ArrayList<String>();
        report.add("measure\tresult");
        report.add("native_scene_revision\t"+scene.revision());
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
        if(connected){
            NativeAsylumMissionReview.audit(map,report);
            for(var guide:walks.entrySet())
                verifyWalk(map,guide.getKey().equals("L19")?vehicleApproach:guide.getValue(),"Asylum "+guide.getKey());
            report.add("supported_cargo_and_admission\t"+4*walks.size()+" direction and posture replays over "+walks.size()+" physical routes");
            if(List.of(args).contains("--lan"))
                NativeAsylumMissionReview.live(new HouseMap(HouseMap.Site.ASYLUM,definition,null,scene),report);
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

    private static void verifyWalk(HouseMap map,List<Vec> route,String name){
        var back=new ArrayList<>(route);
        Collections.reverse(back);
        for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            NativeSchoolInteriorReview.walk(map,route,height,name+" forward");
            NativeSchoolInteriorReview.walk(map,back,height,name+" return");
        }
    }
}
