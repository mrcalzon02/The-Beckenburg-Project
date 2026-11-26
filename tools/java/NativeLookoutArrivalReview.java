package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
        var evidenceFolder=List.of(args).stream().filter(s->s.startsWith("--output=")).findFirst()
            .map(s->Path.of(s.substring("--output=".length()))).orElse(packet);
        var arrival=table(packet.resolve("arrival.tsv"));
        var paths=new LinkedHashMap<String,List<Vec>>();
        for(var row:arrival)if(row[0].equals("POINT"))
            paths.computeIfAbsent(row[1],ignored->new ArrayList<>()).add(point(row,2));
        var manifest=arrival.get(0);
        LookoutNativeLevel.Source source;
        try(var model=Files.newInputStream(Path.of(manifest[1]));var collision=Files.newInputStream(Path.of(manifest[3]))){
            source=LookoutNativeLevel.read(arrival,table(packet.resolve("door-survey.tsv")),
                table(packet.resolve("mission.tsv")),model,collision);
        }
        boolean packaged=List.of(args).contains("--packaged");
        var level=source.level();
        var scene=source.scene();
        var frames=source.doors();
        var host=packaged?new HouseMap(HouseMap.Site.LOOKOUT):new HouseMap(HouseMap.Site.USER,level,null,scene);
        var guest=packaged?HouseMap.forNetworkRevision(HouseMap.Site.LOOKOUT,level.revision())
            :new HouseMap(HouseMap.Site.USER,level,null,scene);
        require(host.level().equals(level),"Packaged site loaded different source coordinates");
        require(host.valid(),"Disconnected Lookout case owners");
        require(RitualSites.valid(host),"Unsupported route to an authored Lookout ritual socket");
        boolean gpu=List.of(args).contains("--gpu");
        var captures=evidenceFolder.resolve(gpu?"captures/opengl":"captures");
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

        var output=evidenceFolder.resolve("verification");
        Files.createDirectories(output);
        NativeSchoolInteriorReview.exportTriangles(scene,Path.of(args[1]));
        var evidence=new ArrayList<>(List.of("measure\tresult","native_scene_revision\t"+scene.revision(),
            "staged_case_revision\t"+level.revision(),
            "supported_case_owners\t14 supported owners and the canonical extraction van",
            "fitted_thresholds\t"+frames.size()+" supported sweeps and host/guest states with body, object, aim, sight and sound",
            "physical_routes\t"+paths.size()+" routes replayed both ways standing and crouched",
            "cargo_to_observation_deck\tComplete 15m climb and return from the physical cargo floor",
            "ritual_sites\tThree supported, separately owned physical installation points"));
        NativeSchoolInteriorReview.captureStanding(host,new Vec(28,0,72),0,captures.resolve("lookout-cargo-approach.png"));
        NativeSchoolInteriorReview.captureStanding(host,new Vec(33,15,5),0,captures.resolve("lookout-deck-view.png"));
        if(!HidingBudget.candidates(level).isEmpty())NativeSchoolInteriorReview.auditWalkInCover(host,captures,evidence,"lookout");
        if(host.hasGenerator())reviewPower(host,captures,evidence);
        if(!level.props().isEmpty())reviewRecords(host,captures,evidence);
        if(packaged){
            require(MissionSites.byId("WLD-04").location()==SiteCatalog.Location.LOOKOUT
                &&SiteCatalog.Location.LOOKOUT.playable(),"The normal mission board did not offer Lookout");
            NativeSchoolInteriorReview.auditSolo(SiteCatalog.Location.LOOKOUT,level);
            evidence.add("packaged_solo\tNamed Lookout selected through the ordinary solo launcher and loaded exact source at cargo");
        }
        if(List.of(args).contains("--lan"))live(level,scene,packaged,evidence);
        Files.write(output.resolve(gpu?"opengl.tsv":"native-arrival.tsv"),evidence);
        evidence.forEach(System.out::println);
    }

    private static void reviewRecords(HouseMap map,Path output,List<String> report)throws Exception{
        for(var socket:map.level().sockets())if(socket.type().equals("story")){
            var room=map.spawn(socket.room());
            var feet=socket.room()==10?new Vec(25.45,room.y(),3.76):new Vec(socket.x(),room.y(),socket.z()+1.33);
            replay(map,List.of(room,new Vec(feet.x(),room.y(),room.z()),feet),socket.id()+" approach");
            double yaw=Math.atan2(socket.x()-feet.x(),socket.z()-feet.z());
            double pitch=Math.atan2(socket.y()+.012-feet.y()-HunterPosture.STANDING_EYE,
                Math.hypot(socket.x()-feet.x(),socket.z()-feet.z()));
            var observer=new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
            var target=InteractionFocus.pick(map,observer,List.of());
            require(target!=null&&target.id().equals("STORY_"+socket.style()),
                "Story item cannot be aimed from supported approach: "+socket.id());
            var renderer=new SceneRenderer();
            renderer.setRenderSize(880,580);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            renderer.x=feet.x();renderer.z=feet.z();renderer.eyeHeight=observer.eyeY();
            renderer.yaw=yaw;renderer.pitch=pitch;renderer.lamp=true;
            renderer.setLampHardware(EquipmentTiers.Tier.III,false);
            NativeSchoolInteriorReview.drawFrame(renderer,map,output.resolve(socket.id()+".png"));
            var frame=renderer.image();
            var graphics=frame.createGraphics();
            try{SiteStories.draw(graphics,frame.getWidth(),frame.getHeight(),map,socket.style());}
            finally{graphics.dispose();}
            javax.imageio.ImageIO.write(frame,"png",output.resolve(socket.id()+"-open.png").toFile());
        }
        report.add("lookout_documents\tTwo supported native documents with measured standing and crouched approach, exact focus and actual readable pages");
    }

    private static void reviewPower(HouseMap map,Path captures,List<String> evidence)throws Exception{
        var plant=new Vec(36.55,.018,67.25);
        var generator=map.level().socket("generator",13);
        var panel=RoomLighting.breaker(map);
        var generatorFeet=new Vec(35.315,.018,67.45);
        var panelFeet=new Vec(37.75,.018,67.65);
        replay(map,List.of(plant,generatorFeet),"generator operator approach");
        replay(map,List.of(plant,panelFeet),"breaker operator approach");
        require(focus(map,generatorFeet,generator,0).kind()==InteractionFocus.Kind.GENERATOR,
            "Generator control lacks physical focus");
        require(focus(map,panelFeet,panel,0).kind()==InteractionFocus.Kind.BREAKER,
            "Breaker control lacks physical focus");
        for(int room:new int[]{3,5,10}){
            var sw=map.level().socket("switch",room);
            double side=sw.z()>map.spawn(room).z()?-1:1;
            var at=new Vec(sw.x(),map.spawn(room).y(),sw.z()+side);
            replay(map,List.of(map.spawn(room),at),sw.id()+" approach");
            require(focus(map,at,sw,0).kind()==InteractionFocus.Kind.SWITCH,
                "Switch lacks physical focus "+sw.id());
        }

        var lights=new RoomLighting();
        var field=new FixtureLighting(map);
        var loads=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        require(loads.size()==5,"Unexpected Lookout load count");
        for(var load:loads){
            lights.setSourceRunning(true);lights.setBreaker(true);field.frame(lights,0);
            double bright=field.sample(load.room(),load.x(),load.y()-.65,load.z()).energy();
            lights.setSourceRunning(false);field.frame(lights,0);
            double sourceOff=field.sample(load.room(),load.x(),load.y()-.65,load.z()).energy();
            lights.setBreaker(false);lights.setSourceRunning(true);field.frame(lights,0);
            double breakerOff=field.sample(load.room(),load.x(),load.y()-.65,load.z()).energy();
            require(bright>2*sourceOff&&bright>2*breakerOff,
                "Load does not respond to both state controls "+load.id()+" "+bright+" / "+sourceOff+" / "+breakerOff);
            evidence.add(load.id()+"\t"+bright+" supplied / "+sourceOff+" source disabled / "+breakerOff+" breaker tripped");
        }
        lights.setBreaker(true);
        powerFrame(map,lights,new Vec(33.5,0,64.5),.66,true,captures.resolve("lookout-power-roadhead.png"));
        powerFrame(map,lights,new Vec(36.62,.018,66.3),0,true,captures.resolve("lookout-power-plant.png"));
        powerFrame(map,lights,new Vec(36.62,.018,66.3),0,false,captures.resolve("lookout-power-plant-outage.png"));
        var hut=new Vec(12,6,20);
        powerFrame(map,lights,hut,.8,true,captures.resolve("lookout-power-hut.png"));
        powerFrame(map,lights,hut,.8,false,captures.resolve("lookout-power-hut-outage.png"));
        evidence.add("power_mounts\tTwo physical plant controls and three room switches reached by ordinary standing/crouched movement and aimed interaction");
    }

    private static InteractionFocus.Target focus(HouseMap map,Vec at,Socket socket,int seat){
        double dx=socket.x()-at.x(),dz=socket.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(socket.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var target=InteractionFocus.pick(map,new LooseClutter.Observer(seat,at.x(),at.y(),at.z(),yaw,pitch,true),List.of());
        require(target!=null,"No aimed response from "+at+" at "+socket.id());
        return target;
    }

    private static void powerFrame(HouseMap map,RoomLighting lights,Vec at,double yaw,boolean supplied,Path image)
            throws Exception{
        require(map.traversable(at.x(),at.y(),at.z()),"Unsupported power review position "+at);
        lights.setBreaker(supplied);
        var r=new SceneRenderer();
        r.setRenderSize(880,580);
        r.setPsxPreset(PsxPipeline.Preset.OFF);
        r.roomLights=lights;
        r.x=at.x();r.eyeHeight=at.y()+HunterPosture.STANDING_EYE;r.z=at.z();r.yaw=yaw;r.pitch=-.12;
        NativeSchoolInteriorReview.drawFrame(r,map,image);
    }

    private static boolean protectedAt(HouseMap map,Vec at){
        return SafeAreaBarrier.protectedAt(map,at.x(),at.y(),at.z());
    }

    private static void live(LevelDefinition level,NativeInvestigationScene scene,boolean packaged,List<String> report)throws Exception{
        var site=packaged?HouseMap.Site.LOOKOUT:HouseMap.Site.USER;
        var map=packaged?new HouseMap(site):new HouseMap(site,level,null,scene);
        var clientMap=packaged?HouseMap.forNetworkRevision(site,level.revision()):new HouseMap(site,level,null,scene);
        var contract=CustomContract.standard(3);
        try(var host=packaged?new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,false,false,site)
                :new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,contract,map);
            var scout=new Peer(host,false,"Roadhead");
            var lookout=new Peer(host,false,"Lookout");
            var witness=new Peer(host,false,"Witness");
            var ghost=new Peer(host,true,"Ghost")){
            for(var peer:List.of(scout,lookout,witness,ghost)){
                peer.awaitExact("SITE|"+site+"|"+level.revision());
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
            if(map.hasGenerator())livePower(clientMap,host,scout,witness,ghost,feet,report);
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
            if(map.level().socket("hide",3)!=null){
                for(int i=0;i<21;i++)feet=NativeSchoolInteriorReview.step(witness,2,0,-100);
                for(int i=0;i<18;i++)feet=NativeSchoolInteriorReview.step(witness,2,-100,0);
                require(!protectedAt(map,feet),"Hut chamber granted cover before the cupboard "+feet);
                for(int i=0;i<2;i++)feet=NativeSchoolInteriorReview.step(witness,2,-100,0);
                require(protectedAt(map,feet),"Live hunter crossed the cupboard threshold without protection "+feet);
                witness.client.hide();
                witness.awaitExact("HIDING|1");
                witness.client.hide();
                witness.awaitExact("HIDING|0");
                for(int i=0;i<3;i++)feet=NativeSchoolInteriorReview.step(witness,2,100,0);
                require(!protectedAt(map,feet),"Live hunter retained protection outside the cupboard "+feet);
                report.add("tcp_lookout_cover\tHunter walked into hut safe zone, hid, revealed, and physically left it");
            }
        }
        report.add("tcp_lookout_review\tFour peers on the exact "+(packaged?"named package":"staged source")
            +", real ramp exit, fitted cabin-deck crossing and south hut stair return");
    }

    private static void livePower(HouseMap map,LanHost host,Peer operator,Peer witness,Peer ghost,Vec at,
            List<String> report)throws Exception{
        ghost.client.viewGhost3D();
        ghost.awaitExact("ACCEPT|GVIEW3D");
        ghost.awaitExact("SITE|"+map.site()+"|"+map.level().revision());
        ghost.await("GPOSE|");
        NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);

        for(int n=0;n<25;n++)at=NativeSchoolInteriorReview.step(operator,0,0,-100);
        for(int n=0;n<24;n++)at=NativeSchoolInteriorReview.step(operator,0,100,0);
        for(int n=0;n<7;n++)at=NativeSchoolInteriorReview.step(operator,0,0,100);
        var generator=map.level().socket("generator",13);
        var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
        usePowerControl(map,operator,0,at,generator,InteractionFocus.Kind.GENERATOR);
        witness.awaitExact("SOURCE|0");
        require(!lights.sourceRunning()&&!lights.breaker()&&!lights.supplied(),"Generator stop changed breaker state");
        Thread.sleep(400);
        usePowerControl(map,operator,0,at,generator,InteractionFocus.Kind.GENERATOR);
        witness.awaitExact("SOURCE|1");
        require(lights.sourceRunning()&&!lights.breaker()&&!lights.supplied(),"Restart silently bypassed the tripped breaker");
        for(int n=0;n<8;n++)at=NativeSchoolInteriorReview.step(operator,0,100,0);
        usePowerControl(map,operator,0,at,RoomLighting.breaker(map),InteractionFocus.Kind.BREAKER);
        witness.awaitExact("LIGHTS|111111111111|1");
        require(lights.supplied(),"The aimed roadhead breaker reset did not restore power");
        operator.client.view3D();
        operator.awaitExact("ACCEPT|VIEW3D");
        operator.awaitExact("LIGHTS|111111111111|1");
        operator.awaitExact("SOURCE|1");
        report.add("tcp_lookout_power\tActual ghost trip, uninterrupted physical walk from cargo to the roadhead shelter, generator stop/restart, physical breaker reset and view resync");
    }

    private static void usePowerControl(HouseMap map,Peer player,int seat,Vec at,Socket socket,
            InteractionFocus.Kind expected)throws Exception{
        var target=focus(map,at,socket,seat);
        require(target.kind()==expected,"The live operator did not aim at "+expected+" from "+at+": "+target);
        double dx=socket.x()-at.x(),dz=socket.z()-at.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(socket.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        player.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        player.client.interact(target,yaw,pitch);
        player.awaitExact("ACCEPT|INTERACT|"+target.name());
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
