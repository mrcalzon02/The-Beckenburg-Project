package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.beckenburg.LevelDefinition.Dock;
import org.beckenburg.LevelDefinition.Portal;
import org.beckenburg.LevelDefinition.Room;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** The inspected Lookout in the native review harness before public dispatch. */
public final class NativeLookoutArrivalReview {
    private NativeLookoutArrivalReview(){}

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
        var arrival=table(packet.resolve("arrival.tsv"));
        var source=arrival.get(0);
        var vanRow=arrival.get(1);
        var dock=new Dock(d(vanRow,1),d(vanRow,2),d(vanRow,3),d(vanRow,4));
        var spaces=new ArrayList<NativeInvestigationScene.Space>();
        var treads=new HashSet<String>();
        var sockets=new ArrayList<Socket>();
        var paths=new LinkedHashMap<String,List<Vec>>();
        for(var row:arrival)switch(row[0]){
            case "SPACE" -> spaces.add(new NativeInvestigationScene.Space(row[1],Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),d(row,6),d(row,7),d(row,8),
                Boolean.parseBoolean(row[9]),Boolean.parseBoolean(row[10])));
            case "TREAD" -> treads.add(row[1]);
            case "RITUAL" -> sockets.add(new Socket("lookout-ritual-"+row[1],"ritual",Integer.parseInt(row[2]),
                d(row,3),d(row,4),d(row,5),0,row[1]));
            case "POINT" -> paths.computeIfAbsent(row[1],ignored->new ArrayList<>()).add(point(row,2));
            case "SOURCE","DOCK" -> {}
            default -> throw new IOException("Unknown Lookout arrival record "+row[0]);
        }
        var frames=new ArrayList<NativeInvestigationDoor.Spec>();
        for(var row:table(packet.resolve("door-survey.tsv"))){
            var portal=new Portal(Integer.parseInt(row[0]),Integer.parseInt(row[4]),Integer.parseInt(row[5]),
                d(row,6),d(row,7),d(row,8),d(row,9),row[10].equals("z"),"door",1);
            frames.add(new NativeInvestigationDoor.Spec(row[1],row[2],row[3],portal,d(row,11),d(row,12),d(row,13),
                Integer.parseInt(row[14]),Integer.parseInt(row[15]),Integer.parseInt(row[16]),row[17]));
        }
        NativeInvestigationScene scene;
        try(var model=Files.newInputStream(Path.of(source[1]));var collision=Files.newInputStream(Path.of(source[3]))){
            scene=NativeInvestigationScene.read(model,source[2],collision,source[4],treads,spaces,frames);
        }
        var portals=new ArrayList<>(frames.stream().map(NativeInvestigationDoor.Spec::portal).toList());
        var rooms=owners(scene,dock,table(packet.resolve("mission.tsv")),portals);
        var level=new LevelDefinition("wld-04",scene.revision(),"Widow's Peak Fire Lookout",rooms,portals,
            List.of(),List.of(),List.of(),sockets,List.of(),dock,"woodland");
        var host=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var guest=new HouseMap(HouseMap.Site.USER,level,null,scene);
        require(host.valid(),"Disconnected Lookout case owners");
        require(RitualSites.valid(host),"Unsupported route to an authored Lookout ritual socket");
        boolean gpu=List.of(args).contains("--gpu");
        var captures=packet.resolve(gpu?"captures/opengl":"captures");
        Files.createDirectories(captures);
        for(var frame:frames){
            int id=frame.portal().id();
            boolean photograph=id==601||id==603||id==606;
            int side=id==603?-1:1;
            require(NativeSchoolInteriorReview.sweep(scene.physics(),frame),"Unsupported leaf sweep "+frame.frameId());
            NativeSchoolInteriorReview.audit(host,frame,false);
            if(photograph)NativeSchoolInteriorReview.render(host,frame.portal(),
                captures.resolve("lookout-"+id+"-closed.png"),side);
            host.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            guest.applyDoorSnapshot(host.doorSnapshot());
            NativeSchoolInteriorReview.audit(host,frame,true);
            NativeSchoolInteriorReview.audit(guest,frame,true);
            if(photograph)NativeSchoolInteriorReview.render(host,frame.portal(),
                captures.resolve("lookout-"+id+"-open.png"),side);
        }

        var van=host.van();
        var cargo=van.world(0,0);
        var lip=van.world(0,VanStaging.FRONT);
        var deck=new Vec(cargo.x(),van.floorY(),cargo.z());
        var ramp=new Vec(lip.x(),van.floorY(),lip.z());
        var arrivalGuide=new ArrayList<>(List.of(deck,ramp));
        arrivalGuide.addAll(paths.get("L01"));
        paths.put("L01",arrivalGuide);
        require(host.roomAt(deck.x(),deck.y(),deck.z())==14,"Van cargo lacks extraction owner");
        var failures=new ArrayList<String>();
        for(var route:paths.entrySet()){
            try{replay(host,route.getValue(),route.getKey());}
            catch(AssertionError failure){failures.add(failure.getMessage());}
        }
        require(failures.isEmpty(),String.join("\n",failures));
        var towerJourney=new ArrayList<Vec>();
        for(String route:List.of("L01","L02","L03","L04","L12","L13","L14","L14B","L15")){
            var used=paths.get(route);
            if(!towerJourney.isEmpty())require(towerJourney.get(towerJourney.size()-1).equals(used.get(0)),
                "Broken joined Lookout approach at "+route);
            towerJourney.addAll(towerJourney.isEmpty()?used:used.subList(1,used.size()));
        }
        replay(host,towerJourney,"cargo to observation deck");

        var output=packet.resolve("verification");
        Files.createDirectories(output);
        NativeSchoolInteriorReview.exportTriangles(scene,Path.of(args[1]));
        var evidence=new ArrayList<>(List.of("measure\tresult","native_scene_revision\t"+scene.revision(),
            "staged_case_revision\t"+level.revision(),
            "supported_case_owners\t14 supported owners and the canonical extraction van",
            "fitted_thresholds\t"+frames.size()+" supported sweeps and host/guest states with body, object, aim, sight and sound",
            "physical_routes\t"+paths.size()+" routes replayed both ways standing and crouched",
            "cargo_to_observation_deck\tComplete 15m climb and return from the physical cargo floor",
            "ritual_sites\tThree supported, separately owned physical installation points"));
        captureStanding(host,new Vec(28,0,72),0,captures.resolve("lookout-cargo-approach.png"));
        captureStanding(host,new Vec(33,15,5),0,captures.resolve("lookout-deck-view.png"));
        if(List.of(args).contains("--lan"))live(level,scene,evidence);
        Files.write(output.resolve(gpu?"opengl.tsv":"native-arrival.tsv"),evidence);
        evidence.forEach(System.out::println);
    }

    private static void captureStanding(HouseMap map,Vec at,double yaw,Path output)throws Exception{
        require(map.traversable(at.x(),at.y(),at.z()),"Unsupported standing view "+at);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.x=at.x();
        renderer.z=at.z();
        renderer.eyeHeight=at.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=yaw;
        renderer.pitch=-.18;
        renderer.lamp=true;
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(renderer,map,output);
    }

    private static void live(LevelDefinition level,NativeInvestigationScene scene,List<String> report)throws Exception{
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        var clientMap=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var scout=new Peer(host,false,"Roadhead");
            var lookout=new Peer(host,false,"Lookout");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")){
            for(var peer:List.of(scout,lookout,witness,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            scout.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            scout.client.start();
            scout.await("INFO|mission started");
            scout.client.view3D();
            scout.awaitExact("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(scout.await("BODY|0|"));
            require(map.roomAt(feet.x(),feet.y(),feet.z())==14,"Lookout case started off cargo");
            for(int i=0;i<13;i++)feet=NativeSchoolInteriorReview.step(scout,0,0,-100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==13&&Math.abs(feet.y())<.03,"Cargo ramp missed roadhead "+feet);
            for(int room:NativeSchoolInteriorReview.missionRoute(map,10)){
                lookout.client.move(room);
                lookout.awaitExact("ACCEPT|MOVE|"+room);
            }
            lookout.client.view3D();
            lookout.awaitExact("ACCEPT|VIEW3D");
            feet=NativeSchoolInteriorReview.body(lookout.await("BODY|1|"));
            require(feet.equals(map.spawn(10)),"Lookout peer missed supported cabin spawn "+feet);
            for(int i=0;i<8;i++)feet=NativeSchoolInteriorReview.step(lookout,1,100,0);
            Thread.sleep(125);
            lookout.client.step(100,0);
            lookout.awaitExact("REJECT|blocked by world collision");
            double yaw=Math.PI/2,pitch=-.11;
            var eye=new LooseClutter.Observer(1,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
            var target=InteractionFocus.pick(clientMap,eye,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals("601"),
                "No aimed focus on the fitted deck door from "+feet);
            lookout.client.turn(90);
            lookout.client.interact(target,yaw,pitch);
            lookout.awaitExact("ACCEPT|INTERACT|Open door");
            var state=witness.awaitExact("DOORS|601");
            clientMap.applyDoorSnapshot(state.substring("DOORS|".length()));
            require(clientMap.doorOpen(601),"Witness missed the fitted cab leaf state");
            for(int i=0;i<5;i++)feet=NativeSchoolInteriorReview.step(lookout,1,100,0);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==11&&Math.abs(feet.y()-15)<.03,"Peer missed observation deck "+feet);
            for(int i=0;i<5;i++)feet=NativeSchoolInteriorReview.step(lookout,1,-100,0);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==10,"Peer missed cabin return "+feet);
            for(int room:NativeSchoolInteriorReview.missionRoute(map,3)){
                witness.client.move(room);
                witness.awaitExact("ACCEPT|MOVE|"+room);
            }
            witness.client.view3D();
            witness.awaitExact("ACCEPT|VIEW3D");
            feet=NativeSchoolInteriorReview.body(witness.await("BODY|2|"));
            require(feet.equals(map.spawn(3)),"Witness missed the hut spawn "+feet);
            for(int i=0;i<18;i++)feet=NativeSchoolInteriorReview.step(witness,2,0,100);
            Thread.sleep(125);
            witness.client.step(0,100);
            witness.awaitExact("REJECT|blocked by world collision");
            eye=new LooseClutter.Observer(2,feet.x(),feet.y(),feet.z(),0,pitch,true);
            target=InteractionFocus.pick(clientMap,eye,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.DOOR&&target.id().equals("606"),
                "No aimed focus on the south hut door from "+feet);
            witness.client.turn(0);
            witness.client.interact(target,0,pitch);
            witness.awaitExact("ACCEPT|INTERACT|Open door");
            lookout.awaitExact("DOORS|601,606");
            for(int i=0;i<11;i++)feet=NativeSchoolInteriorReview.step(witness,2,0,100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==2&&feet.y()<5.69&&feet.z()>28.7,
                "Witness missed the supported terrace below the hut steps "+feet);
            for(int i=0;i<11;i++)feet=NativeSchoolInteriorReview.step(witness,2,0,-100);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==3&&Math.abs(feet.y()-6)<.025,
                "Witness missed the hut return "+feet);
        }
        report.add("tcp_lookout_review\tFour peers on the exact staged source, real ramp exit, fitted cabin-deck crossing and south hut stair return");
    }

    private static List<Room> owners(NativeInvestigationScene scene,Dock dock,List<String[]> rows,List<Portal> portals)
            throws IOException{
        var result=new Room[HouseMap.ROOM_COUNT];
        for(var row:rows)switch(row[0]){
            case "ANCHOR" -> {
                int id=Integer.parseInt(row[1]);
                var at=point(row,3);
                var space=scene.spaceAt(at.x(),at.y(),at.z());
                if(space==null||space.owner()!=id||!space.id().equals(row[2])
                        ||!scene.physics().traversable(at,HunterPosture.STANDING_HEIGHT))
                    throw new IOException("Unsupported Lookout owner "+row[2]+" at "+at+" / "+space);
                result[id]=new Room(id,space.id(),space.x0(),space.z0(),space.x1(),space.z1(),space.floor(),space.height(),
                    space.outside()?"native-outdoor":"wood",space.outside()?"outdoor":"timber",at);
            }
            case "TRANSITION" -> {
                var at=point(row,4);
                var floor=scene.physics().support(at.x(),at.z(),at.y(),.07);
                if(floor==null||Math.abs(floor.y()-at.y())>.06)
                    throw new IOException("Unsupported Lookout transition "+row[1]+" "+at);
                int a=Integer.parseInt(row[2]),b=Integer.parseInt(row[3]);
                boolean along=row[8].equals("z");
                if(row[9].equals("trail")){
                    double dx=along?0:.68,dz=along?.68:0;
                    int near=scene.ownerAt(at.x()-dx,at.y(),at.z()-dz);
                    int far=scene.ownerAt(at.x()+dx,at.y(),at.z()+dz);
                    if(!(a==near&&b==far||a==far&&b==near))
                        throw new IOException("No Lookout owner interface at "+row[1]+": "+near+" and "+far);
                }
                portals.add(new Portal(Integer.parseInt(row[1]),a,b,at.x(),at.y(),at.z(),d(row,7),along,row[9],1));
            }
            default -> throw new IOException("Unknown Lookout mission record "+row[0]);
        }
        var van=new VanStaging(new VanStaging.Placement(dock.x(),dock.y(),dock.z(),dock.yaw()));
        var lip=van.world(0,VanStaging.FRONT);
        double y=van.floorY();
        portals.add(new Portal(619,13,14,lip.x(),y,lip.z(),VanStaging.RAMP_HALF_WIDTH*2,true,"trail",1));
        result[14]=new Room(14,"Expedition van",dock.x()-1.26,dock.z()-3,dock.x()+1.26,dock.z()+2.4,
            y,VanStaging.INTERIOR_HEIGHT,"van","metal",new Vec(dock.x(),y,dock.z()));
        return List.of(result);
    }

    private static void replay(HouseMap map,List<Vec> guide,String label){
        var reverse=new ArrayList<>(guide);
        Collections.reverse(reverse);
        for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}){
            NativeSchoolInteriorReview.walk(map,guide,height,"Lookout "+label);
            NativeSchoolInteriorReview.walk(map,reverse,height,"Lookout return "+label);
        }
    }

    private static void require(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
    }
    private static List<String[]> table(Path path)throws IOException{
        return Files.readAllLines(path).stream().map(line->line.split("\\t",-1)).toList();
    }
    private static double d(String[] row,int column){return Double.parseDouble(row[column]);}
    private static Vec point(String[] row,int column){return new Vec(d(row,column),d(row,column+1),d(row,column+2));}
}
