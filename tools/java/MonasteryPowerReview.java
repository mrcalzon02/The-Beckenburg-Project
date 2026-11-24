package org.beckenburg;

import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/** Commissions the fitted generator, breaker and two existing room circuits. */
public final class MonasteryPowerReview {
    private static final int TOWER=6, COURT=11;
    private static final Vec AISLE = new Vec(62.54,0,22.93);

    private MonasteryPowerReview() {}

    public static void main(String[] args) throws Exception {
        var packet=Path.of(args[0]);
        if (!List.of(args).contains("--gpu")) {
            review(packet,false);
            return;
        }
        var desktop=Class.forName("org.beckenburg.gpu.GpuDesktop");
        var start=desktop.getDeclaredMethod("startGpu");
        var stop=desktop.getDeclaredMethod("stopReviewGpu");
        start.setAccessible(true);
        stop.setAccessible(true);
        start.invoke(null);
        try {
            review(packet,true);
        } finally {
            stop.invoke(null);
        }
    }

    static MonasteryDoorReview.Scene read(Path packet) throws Exception {
        return read(packet,"monastery-with-service-power");
    }

    static MonasteryDoorReview.Scene read(Path packet,String stem) throws Exception {
        var base=MonasteryDoorReview.read(packet,packet,stem);
        var level=base.level();
        var sockets=new ArrayList<>(level.sockets());
        for (var line:Files.readAllLines(packet.resolve("electrical-schedule.tsv"))) {
            var p=line.split("\t");
            require(p.length==8,"Invalid mounted electrical socket "+line);
            sockets.add(new Socket(p[0],p[1],Integer.parseInt(p[2]),
                Double.parseDouble(p[3]),Double.parseDouble(p[4]),Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),p[7]));
        }
        var powered=new LevelDefinition(level.id(),base.nativeScene().revision(),level.title(),
            level.rooms(),level.portals(),level.stairs(),level.voids(),level.props(),sockets,
            level.colliders(),level.dock(),level.ambience(),level.water(),level.sounds());
        return new MonasteryDoorReview.Scene(powered,base.nativeScene(),base.paths());
    }

    private static void review(Path packet,boolean gpu) throws Exception {
        var data=read(packet);
        var map=data.map();
        require(map.valid()&&RitualSites.valid(map),"Power blocked room ownership, rites or spawns");
        var report=new ArrayList<>(List.of("check\tresult","native_scene_revision\t"+data.nativeScene().revision(),
            "collision_sha256\t"+data.nativeScene().physics().sha256(),"level_revision\t"+data.level().revision(),
            "renderer\t"+(gpu?GpuWorldRenderer.active().rendererDescription():"Software")));
        for (var door:data.nativeScene().doors()) {
            var frame=door.spec();
            require(NativeSchoolInteriorReview.sweep(data.nativeScene().physics(),frame),
                "Power equipment obstructed "+frame.frameId());
            NativeSchoolInteriorReview.audit(map,frame,false);
            map.toggleDoor(frame.portal().id(),System.nanoTime()-2_000_000_000L);
            NativeSchoolInteriorReview.audit(map,frame,true);
        }
        int walks=0;
        for (var entry:data.paths().entrySet()) {
            var back=new ArrayList<>(entry.getValue());
            Collections.reverse(back);
            for (double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map,entry.getValue(),posture,entry.getKey());
                NativeSchoolInteriorReview.walk(map,back,posture,entry.getKey()+" reverse");
                walks+=2;
            }
        }
        report.add("original_route_replays\t"+walks);
        report.add("retained_doors\t"+data.nativeScene().doors().size());
        var generator=map.level().socket("generator",COURT);
        var breaker=RoomLighting.breaker(map);
        var courtSwitch=map.level().socket("switch",COURT);
        var towerSwitch=map.level().socket("switch",TOWER);
        for (var control:List.of(generator,breaker,courtSwitch,towerSwitch)) {
            var feet=operator(control);
            require(map.traversable(feet.x(),0,feet.z())&&map.roomAt(feet.x(),0,feet.z())==control.room(),
                "Missing supported operator space "+control.id());
            require(focus(map,feet,control).id().equals(focusId(control)),"Wrong mounted focus "+control.id());
            for (double posture:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT}) {
                var approach=new Vec(control.room()==TOWER?59.03:62.54,0,control.z());
                NativeSchoolInteriorReview.walk(map,List.of(approach,feet),posture,control.id()+" approach");
                NativeSchoolInteriorReview.walk(map,List.of(feet,approach),posture,control.id()+" leave");
            }
        }
        report.add("supported_control_positions\t4");
        var lamps=map.level().sockets().stream().filter(s->s.type().equals("light")).toList();
        require(lamps.size()==4,"Incorrect powered load count");
        var lighting=new RoomLighting();
        var fixture=new FixtureLighting(map);
        for (var lamp:lamps) {
            double on=energy(fixture,lighting,lamp);
            lighting.setSourceRunning(false);
            double noSource=energy(fixture,lighting,lamp);
            lighting.setSourceRunning(true);
            lighting.setBreaker(false);
            double tripped=energy(fixture,lighting,lamp);
            lighting.setBreaker(true);
            lighting.toggle(lamp.room());
            double switched=energy(fixture,lighting,lamp);
            require(lighting.on(lamp.room()==COURT?TOWER:COURT),"One switch changed the other branch");
            lighting.toggle(lamp.room());
            require(on>tripped*1.25 && on-tripped>.01 && Math.abs(tripped-noSource)<.0001
                && Math.abs(tripped-switched)<.0001,"Missing power contrast "+lamp.id()+" "+on+" / "+tripped);
            report.add(lamp.id()+"\t"+on+" supplied / "+tripped+" isolated");
        }
        var captures=packet.resolve(gpu?"captures/opengl":"captures/software");
        Files.createDirectories(captures);
        pair(map,lighting,captures,"service-court",new Vec(64.3,0,26.7),new Vec(60.2,1.95,26.8),COURT,report);
        pair(map,lighting,captures,"tower-base",new Vec(58.79,0,25.7),new Vec(59.7,1.91,25.7),TOWER,report);
        capture(map,lighting,new Vec(63.8,0,19.45),new Vec(60.82,1.43,19.38),true,captures.resolve("generator-and-main-board.png"));
        NativeSchoolInteriorReview.auditHiding(map,report);
        if (!gpu) live(data,report);
        Files.write(packet.resolve(gpu?"power-opengl.tsv":"power-software.tsv"),report);
        report.forEach(System.out::println);
    }

    private static double energy(FixtureLighting fixtures,RoomLighting lighting,Socket lamp) {
        fixtures.frame(lighting,0);
        double x=lamp.x()+(lamp.room()==COURT?.64:-.64);
        return fixtures.sample(lamp.room(),x,lamp.y()-.39,lamp.z()).energy();
    }

    private static void pair(HouseMap map,RoomLighting lighting,Path out,String name,Vec feet,Vec target,int owner,
                             List<String> report) throws Exception {
        var on=out.resolve(name+"-supplied.png");
        var off=out.resolve(name+"-isolated.png");
        capture(map,lighting,feet,target,false,on);
        if (owner==COURT) lighting.setBreaker(false); else lighting.toggle(owner);
        capture(map,lighting,feet,target,false,off);
        if (owner==COURT) lighting.setBreaker(true); else lighting.toggle(owner);
        var a=ImageIO.read(on.toFile());
        var b=ImageIO.read(off.toFile());
        int changed=0;
        long brightness=0;
        for (int y=0;y<a.getHeight();y++) for (int x=0;x<a.getWidth();x++) {
            int before=a.getRGB(x,y),after=b.getRGB(x,y);
            if (before!=after) changed++;
            for (int shift=0;shift<24;shift+=8) brightness+=((before>>shift)&255)-((after>>shift)&255);
        }
        require(changed>12000 && brightness>0,"Same-camera lighting did not respond "+name+" "+changed+" "+brightness);
        report.add(name+"_visible_light_response\t"+changed+" pixels brightened in aggregate");
    }

    private static void capture(HouseMap map,RoomLighting lighting,Vec feet,Vec target,boolean torch,Path path) throws Exception {
        require(map.traversable(feet.x(),feet.y(),feet.z()),"No standing floor at "+feet);
        var renderer=new SceneRenderer();
        renderer.setRenderSize(920,620);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=lighting;
        renderer.lamp=torch;
        if (torch) renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        renderer.x=feet.x();
        renderer.z=feet.z();
        renderer.eyeHeight=feet.y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(target.x()-feet.x(),target.z()-feet.z());
        renderer.pitch=Math.atan2(target.y()-renderer.eyeHeight,Math.hypot(target.x()-feet.x(),target.z()-feet.z()));
        renderer.weatherModule().setTimeOfDay(.08);
        NativeSchoolInteriorReview.drawFrame(renderer,map,path);
    }

    private static Vec operator(Socket control) {
        double floor=control.type().equals("switch")?control.y()-1.25:0;
        return new Vec(control.x()+Math.sin(control.yaw())*1.18,floor,control.z()+Math.cos(control.yaw())*1.18);
    }

    private static String focusId(Socket socket) {
        return switch (socket.type()) {
            case "switch" -> Integer.toString(socket.room());
            case "native_story" -> "STORY_"+socket.style();
            default -> socket.id();
        };
    }

    private static InteractionFocus.Target focus(HouseMap map,Vec feet,Socket socket) {
        double dx=socket.x()-feet.x(), dz=socket.z()-feet.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(socket.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var aim=InteractionFocus.pick(map,new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true),List.of());
        require(aim!=null,"No mounted control focus "+socket.id()+" at "+feet);
        return aim;
    }

    private static void use(HouseMap map,Peer peer,Vec feet,Socket socket) throws Exception {
        var aim=focus(map,feet,socket);
        require(aim.id().equals(focusId(socket)),"Wrong live control "+aim+" for "+socket.id());
        double dx=socket.x()-feet.x(), dz=socket.z()-feet.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(socket.y()-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        peer.client.interact(aim,yaw,pitch);
        if (socket.type().equals("native_story")) peer.awaitExact("INTERACTED|INTERFACE|"+aim.id());
        peer.awaitExact("ACCEPT|INTERACT|"+aim.name());
    }

    private static void read(HouseMap map,Peer peer,Vec feet,Socket record,List<String> report) throws Exception {
        require(map.roomAt(feet.x(),feet.y(),feet.z())==record.room(),"Reading stance left "+record.id());
        use(map,peer,feet,record);
        report.add(record.style()+"_live_reading\tPASS walked to record and opened its reading interface");
    }

    static void live(MonasteryDoorReview.Scene data,List<String> report) throws Exception {
        live(data,report,HouseMap.Site.USER);
    }

    static void live(MonasteryDoorReview.Scene data,List<String> report,HouseMap.Site site) throws Exception {
        boolean named=site!=HouseMap.Site.USER;
        var map=named?new HouseMap(site):data.map();
        var replica=named?HouseMap.forNetworkRevision(site,map.level().revision()):data.map();
        var generator=map.level().socket("generator",COURT);
        var breaker=RoomLighting.breaker(map);
        var courtSwitch=map.level().socket("switch",COURT);
        var towerSwitch=map.level().socket("switch",TOWER);
        try (var host=named
                ?new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),false,false,site)
                :new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
             var engineer=new Peer(host,false,"Monastery electrician");
             var witness=new Peer(host,false,"Court observer");
             var crew=new Peer(host,false,"Returning investigator");
             var ghost=new Peer(host,true,"Monastery spirit")) {
            var authoritative=(HouseMap)NativeSchoolInteriorReview.field(host,"map");
            require(authoritative.nativeInvestigation()==data.nativeScene(),"Host changed the selected map authority");
            for (var peer:List.of(engineer,witness,crew,ghost)) {
                peer.awaitExact("SITE|"+site+"|"+data.level().revision());
                peer.client.ready();
            }
            engineer.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            engineer.client.start();
            engineer.await("INFO|mission started");
            engineer.client.view3D();
            engineer.awaitExact("ACCEPT|VIEW3D");
            var at=NativeSchoolInteriorReview.body(engineer.await("BODY|0|"));
            require(map.roomAt(at.x(),at.y(),at.z())==14,"Live power case did not start in cargo");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            for (int i=0;i<15;i++) at=NativeSchoolInteriorReview.step(engineer,0,0,-100);
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(32,0,68));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,930);
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(32,0,59));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,931);
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(32,0,54.9),new Vec(47,0,53),new Vec(47,0,44),new Vec(47,0,36),new Vec(47,0,26));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,942);
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(52.97,0,24.5),new Vec(52.97,0,27),
                new Vec(59.03,0,27),new Vec(59.03,0,23));
            at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,943);
            at=NorthwatchDoorRuntime.walk(engineer,at,AISLE,new Vec(62.54,0,generator.z()),operator(generator));

            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            use(map,engineer,at,generator);
            witness.awaitExact("SOURCE|0");
            require(!lights.sourceRunning()&&!lights.breaker(),"A stopped generator changed the tripped breaker");
            Thread.sleep(400);
            use(map,engineer,at,generator);
            witness.awaitExact("SOURCE|1");
            require(lights.sourceRunning()&&!lights.breaker()&&!lights.supplied(),"Restart incorrectly reset the breaker");
            var ticket=crew.client.resumeTicket();
            require(ticket!=null,"No reconnect ticket");
            ((java.net.Socket)NativeSchoolInteriorReview.field(crew.client,"socket")).close();
            witness.await("PEER|LOST|2");
            var events=new LinkedBlockingQueue<String>();
            try (var resumed=new LanClient("127.0.0.1",host.port(),ticket,events::offer)) {
                await(events,"INFO|mission resumed");
                resumed.view3D();
                await(events,"ACCEPT|VIEW3D");
                await(events,"SITE|"+site+"|"+data.level().revision());
                await(events,"LIGHTS|111111111111|0");
                await(events,"SOURCE|1");
            }
            at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(62.54,0,breaker.z()),operator(breaker));
            use(map,engineer,at,breaker);
            witness.awaitExact("LIGHTS|111111111111|1");
            require(lights.supplied(),"Physical breaker reset did not restore supply");
            at=NorthwatchDoorRuntime.walk(engineer,at,AISLE,new Vec(62.54,0,courtSwitch.z()),operator(courtSwitch));
            use(map,engineer,at,courtSwitch);
            witness.awaitExact("LIGHTS|111111111110|1");
            require(!lights.on(COURT)&&lights.on(TOWER),"Court circuit affected tower circuit");
            at=NorthwatchDoorRuntime.walk(engineer,at,AISLE,new Vec(59.03,0,23),operator(towerSwitch));
            use(map,engineer,at,towerSwitch);
            witness.awaitExact("LIGHTS|111111011110|1");
            require(!lights.on(COURT)&&!lights.on(TOWER)&&lights.supplied(),"Tower circuit affected main supply");
            var refectorySwitch=map.level().socket("switch",4);
            if (refectorySwitch!=null) {
                var cloisterSwitch=map.level().socket("switch",2);
                require(lights.on(4)&&lights.on(2),"Commissioned source failed to feed both new circuits");
                at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(59.03,0,23),new Vec(64,0,23),new Vec(64,0,29));
                at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,945);
                at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(64,0,32.6),new Vec(62.36,0,32.6),new Vec(62.36,0,31.3));
                use(map,engineer,at,refectorySwitch);
                witness.awaitExact("LIGHTS|111101011110|1");
                require(!lights.on(4)&&lights.on(2),"Refectory switch interrupted the unswitched cloister feed");
                var rationSheet=map.level().socket("native_story",4);
                if (rationSheet!=null) {
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(62.36,0,36.95),new Vec(54.45,0,37.8),
                        new Vec(54.45,0,34.64),new Vec(54.88,0,34.64));
                    read(map,engineer,at,rationSheet,report);
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(54.45,0,34.64),new Vec(54.45,0,38.15),
                        new Vec(59,0,40));
                } else {
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(62.36,0,35),new Vec(59,0,40));
                }
                at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,944);
                at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(47,0,40),operator(cloisterSwitch));
                use(map,engineer,at,cloisterSwitch);
                witness.awaitExact("LIGHTS|110101011110|1");
                require(!lights.on(2)&&!lights.on(4)&&lights.supplied(),"Cloister switch affected the main supply");
                report.add("live_east_range_independent_switches\tPASS");
                var vestry=map.level().socket("switch",10);
                if (vestry!=null) {
                    var archive=map.level().socket("switch",3);
                    require(lights.on(3)&&lights.on(10),"East switch interrupted the unswitched north feed");
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(47,0,40),new Vec(47,0,32),new Vec(47,0,24),
                        new Vec(47,0,17),new Vec(36,0,17));
                    at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,939);
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(36,0,14),operator(vestry));
                    use(map,engineer,at,vestry);
                    witness.awaitExact("LIGHTS|110101011100|1");
                    require(!lights.on(10)&&lights.on(3),"Vestry switch affected archive supply");
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(34.42,0,9),new Vec(30,0,9));
                    at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,940);
                    at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(18,0,9),new Vec(16.22,0,12.3),operator(archive));
                    use(map,engineer,at,archive);
                    witness.awaitExact("LIGHTS|110001011100|1");
                    require(!lights.on(3)&&lights.supplied(),"Archive switch changed the main supply");
                    var bellRegister=map.level().socket("native_story",3);
                    if (bellRegister!=null) {
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(19.75,0,14.62),new Vec(19.75,0,13.73));
                        read(map,engineer,at,bellRegister,report);
                    }
                    report.add("live_north_range_independent_switches\tPASS");
                    var dorm=map.level().socket("switch",5);
                    if (dorm!=null) {
                        var west=map.level().socket("switch",1);
                        var gate=map.level().socket("switch",0);
                        require(lights.on(0)&&lights.on(1)&&lights.on(5),"North room switches interrupted the west main feeder");
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(18,0,14.2));
                        at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,938);
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(18,0,17),new Vec(11,0,17),new Vec(11,0,24),
                            new Vec(11,0,32),new Vec(11,0,37.75),operator(west));
                        use(map,engineer,at,west);
                        witness.awaitExact("LIGHTS|100001011100|1");
                        require(lights.on(5)&&lights.on(0),"West cloister switch interrupted dormitory or gatehouse supply");
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(11,0,37.75),new Vec(11,0,36));
                        at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,941);
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(5.2,0,36),operator(dorm));
                        use(map,engineer,at,dorm);
                        witness.awaitExact("LIGHTS|100000011100|1");
                        require(lights.on(0),"Dormitory switch interrupted gatehouse supply");
                        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(6.45,0,36),new Vec(11,0,36),
                            new Vec(11,0,45),new Vec(11,0,53),new Vec(20,0,53),new Vec(29,0,53),
                            new Vec(32,0,54.9),new Vec(32,0,58.7),new Vec(29.42,0,58.7),operator(gate));
                        use(map,engineer,at,gate);
                        witness.awaitExact("LIGHTS|000000011100|1");
                        require(lights.supplied()&&!lights.on(0)&&!lights.on(1)&&!lights.on(5),"Western switches altered main supply");
                        report.add("live_west_range_independent_switches\tPASS");
                        if (map.level().socket("switch",7)!=null)
                            liveVertical(data,map,replica,engineer,witness,lights,at,report);
                    }
                }
            }
        }
        report.add("live_four_player_blackout\tPASS actual van route, four operating doors, ghost trip, independent generator and breaker, reconnect and both switches");
    }

    private static void liveVertical(MonasteryDoorReview.Scene data,HouseMap map,HouseMap replica,
                                     Peer engineer,Peer witness,RoomLighting lights,Vec start,
                                     List<String> report) throws Exception {
        var bell=map.level().socket("switch",7);
        var passage=map.level().socket("switch",8);
        var chamber=map.level().socket("switch",9);
        require(lights.on(7)&&lights.on(8)&&lights.on(9),"A ground switch cut a vertical feeder");
        var at=NorthwatchDoorRuntime.walk(engineer,start,new Vec(29.42,0,58.7),new Vec(32,0,58.7),
            new Vec(32,0,54.9),new Vec(47,0,53),new Vec(47,0,44),new Vec(47,0,35),new Vec(47,0,26),
            new Vec(51.4,0,26),new Vec(52.97,0,24.5));
        var bellStair=data.paths().get("L16");
        for (var knot:bellStair) at=CemeteryRefugeReview.walk(engineer,0,at,knot);
        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(58.8,6,22.3),new Vec(58.36,6,26.8));
        use(map,engineer,at,bell);
        witness.awaitExact("LIGHTS|000000001100|1");
        require(lights.on(8)&&lights.on(9),"Bell switch cut the lower rooms");
        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(58.8,6,22.3),new Vec(58.86,6,17));
        var descent=new ArrayList<>(bellStair);
        Collections.reverse(descent);
        for (var knot:descent) at=CemeteryRefugeReview.walk(engineer,0,at,knot);
        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(51.4,0,26),new Vec(47,0,26),new Vec(47,0,17),new Vec(29,0,17));
        for (var knot:data.paths().get("L19")) at=CemeteryRefugeReview.walk(engineer,0,at,knot);
        at=NorthwatchDoorRuntime.walk(engineer,at,operator(passage));
        use(map,engineer,at,passage);
        witness.awaitExact("LIGHTS|000000000100|1");
        require(lights.on(9),"Passage switch cut the memorial chamber");
        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(23,-3,17.4),new Vec(21.25,-3,17.2),new Vec(18,-3,17.2));
        at=NorthwatchDoorRuntime.open(map,replica,data.nativeScene(),engineer,witness,at,946);
        at=NorthwatchDoorRuntime.walk(engineer,at,new Vec(18,-3,13.8),new Vec(20.25,-3,13.9),operator(chamber));
        use(map,engineer,at,chamber);
        witness.awaitExact("LIGHTS|000000000000|1");
        require(lights.supplied()&&!lights.on(7)&&!lights.on(8)&&!lights.on(9),"Vertical switches altered main supply");
        report.add("live_vertical_range_independent_switches\tPASS two bell flights and existing catacomb stairs");
    }

    private static void await(LinkedBlockingQueue<String> events,String frame) throws Exception {
        long until=System.nanoTime()+8_000_000_000L;
        while (System.nanoTime()<until) {
            String next=events.poll(200,java.util.concurrent.TimeUnit.MILLISECONDS);
            if (next!=null&&next.startsWith(frame)) return;
        }
        throw new AssertionError("Reconnect missing "+frame);
    }

    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }
}
