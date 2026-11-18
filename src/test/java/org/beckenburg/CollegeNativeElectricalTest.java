package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;

/** The fitted native College must pass the ordinary game and localhost host with its actual controls. */
public final class CollegeNativeElectricalTest {
    private record Aim(LooseClutter.Observer observer, InteractionFocus.Target target) { }

    private static void check(boolean yes, String reason) {
        if (!yes) throw new AssertionError(reason);
    }

    private static Aim aim(HouseMap map, Vec floor, Vec control, InteractionFocus.Kind kind, String id, double height) {
        check(map.traversable(floor.x(), floor.y(), floor.z(), height), "Operating position blocked: " + id);
        double eye = height == HunterPosture.STANDING_HEIGHT ? HunterPosture.STANDING_EYE : HunterPosture.CROUCH_EYE;
        double dx = control.x() - floor.x(), dz = control.z() - floor.z();
        double yaw = Math.atan2(dx, dz);
        double pitch = Math.atan2(control.y() - floor.y() - eye, Math.hypot(dx, dz));
        var observer = new LooseClutter.Observer(0, floor.x(), floor.y(), floor.z(), yaw, pitch, true, eye);
        var target = InteractionFocus.pick(map, observer, List.of());
        check(target != null && target.kind() == kind && target.id().equals(id), "Real aim failed: " + id + " at " + floor + " hit " + target);
        var away = new LooseClutter.Observer(0, floor.x(), floor.y(), floor.z(), yaw + Math.PI, pitch, true, eye);
        var miss = InteractionFocus.pick(map, away, List.of());
        check(miss == null || miss.kind() != kind || !miss.id().equals(id), "False aim away from " + id);
        return new Aim(observer, target);
    }

    private static List<Aim> switches(HouseMap map) {
        var result = new ArrayList<Aim>();
        for (int room = 0; room < 12; room++) {
            var s = RoomLighting.location(map, room);
            double x = s.x() + .84 * Math.sin(s.yaw()), z = s.z() + .84 * Math.cos(s.yaw());
            double y = map.groundHeight(x, z, room);
            check(Math.abs(y - (s.y() - 1.25)) < .03, "Switch approach selected another floor " + room);
            check(map.roomAt(x, y, z) == room, "Switch operating position has wrong owner " + room);
            var target = new Vec(s.x() + .01 * Math.sin(s.yaw()), s.y(), s.z() + .01 * Math.cos(s.yaw()));
            result.add(aim(map, new Vec(x, y, z), target, InteractionFocus.Kind.SWITCH,
                Integer.toString(room), HunterPosture.STANDING_HEIGHT));
            aim(map, new Vec(x, y, z), target, InteractionFocus.Kind.SWITCH,
                Integer.toString(room), HunterPosture.CROUCH_HEIGHT);
        }
        return List.copyOf(result);
    }

    private static void checkLight(HouseMap map) {
        var lighting = new FixtureLighting(map);
        var circuits = new RoomLighting();
        int fixtures = 0;
        double maxDarkEnergy = 0, weakestDifference = Double.POSITIVE_INFINITY;
        for (int r = 0; r < 13; r++) {
            final int room = r;
            var load = lighting.sources().stream().filter(source -> source.room() == room).findFirst().orElseThrow();
            fixtures += (int)lighting.sources().stream().filter(source -> source.room() == room).count();
            double receiverY = load.y() - .53;
            lighting.frame(circuits, 0);
            double on = lighting.sample(room, load.x(), receiverY, load.z()).energy();
            circuits.setSourceRunning(false);
            lighting.frame(circuits, 1);
            double off = lighting.sample(room, load.x(), receiverY, load.z()).energy();
            check(on > off + .05 && circuits.breaker(), "Mounted light does not follow its supply in room "+room+" energy "+on+" -> "+off);
            weakestDifference = Math.min(weakestDifference,on-off);
            maxDarkEnergy = Math.max(maxDarkEnergy,off);
            circuits.setSourceRunning(true);
            if (room < 12) {
                circuits.toggle(room);
                lighting.frame(circuits, 2);
                check(lighting.sample(room, load.x(), receiverY, load.z()).energy() < on-.04, "Room switch does not affect the actual lamp in room "+room);
                circuits.toggle(room);
            }
        }
        var van = map.van().world(0, 0);
        lighting.frame(circuits, 0);
        double charged = lighting.sample(14, van.x(), map.van().floorY() + 1.4, van.z()).energy();
        circuits.setBreaker(false);
        lighting.frame(circuits, 1);
        check(Math.abs(charged-lighting.sample(14, van.x(), map.van().floorY() + 1.4, van.z()).energy()) < .01 && charged > .3,
            "Independent van battery changed with the main breaker");
        System.out.printf(Locale.ROOT,"PASS %d hardwired loads, all twelve room switch effects and independent van battery; minimum energy drop %.4f, max blackout sample %.4f%n",
            fixtures, weakestDifference,maxDarkEnergy);
    }

    private static void host(HouseMap map, Aim start, Aim breaker, Aim cafeSwitch) throws Exception {
        check(map.valid() && RitualSites.valid(map), "Candidate must satisfy the production case validator");
        try (var host = new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
             var owner = new AuthoredLevelNetworkTest.Peer(host.port(),false,"Surveyor");
             var observer = new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
             var hunter = new AuthoredLevelNetworkTest.Peer(host.port(),false,"Investigator");
             var ghost = new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")) {
            check(owner.await("SITE|").equals("SITE|COLLEGE|"+map.level().revision()),"Host served a different site revision");
            owner.client.ready();
            observer.client.ready();
            hunter.client.ready();
            ghost.client.ready();
            owner.await("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();
            owner.await("INFO|mission started");
            owner.client.view3D();
            owner.await("ACCEPT|VIEW3D");
            owner.await("SOURCE|1");
            observer.client.view3D();
            observer.await("ACCEPT|VIEW3D");
            observer.await("SOURCE|1");
            var world = (LanWorld)LanSafeAreaBarrierTest.field(host,"world");
            var state = (RoomLighting)LanSafeAreaBarrierTest.field(host,"lights");
            owner.client.step(-100,0);
            owner.await("ACCEPT|STEP");
            useControl(host,map,world,owner,cafeSwitch);
            observer.await("LIGHTS|111101111111|1");
            check(!state.on(4) && state.supplied(),"Room switch should leave the other loads supplied");
            useControl(host,map,world,owner,cafeSwitch);
            observer.await("LIGHTS|111111111111|1");
            useControl(host,map,world,owner,start);
            observer.await("SOURCE|0");
            check(!state.sourceRunning() && state.breaker(),"Ordinary source stop changed the breaker");
            useControl(host,map,world,owner,start);
            observer.await("SOURCE|1");

            long seed = 0;
            while (!new GhostHauntingFolio(seed).choose(GhostDomain.Action.DISTURB,GhostIdentity.Type.SPIRIT,4,1)
                    .entry().id().equals("lightning_static")) seed++;
            Object guard = LanSafeAreaBarrierTest.field(host,"mutex");
            synchronized (guard) {
                var folio = LanSafeAreaBarrierTest.field(host,"hauntingFolio");
                ((Random)LanSafeAreaBarrierTest.field(folio,"random")).setSeed(seed);
            }
            ghost.client.ghost(GhostDomain.Action.DISTURB,4);
            ghost.await("ACCEPT|GHOST|DISTURB");
            observer.await("LIGHTS|111111111111|0");
            check(state.sourceRunning() && !state.breaker() && !state.supplied(),"Real electrical haunt changed source state");
            owner.client.view3D();
            owner.await("ACCEPT|VIEW3D");
            owner.await("LIGHTS|111111111111|0");
            owner.await("SOURCE|1");
            var ticket = hunter.client.resumeTicket();
            check(ticket != null, "Host issued no seat recovery ticket");
            ((java.net.Socket)LanSafeAreaBarrierTest.field(hunter.client,"socket")).close();
            owner.await("PEER|LOST|2");
            try (var returned = new AuthoredLevelNetworkTest.Peer(host.port(),ticket)) {
                returned.await("INFO|mission resumed");
                returned.client.view3D();
                returned.await("ACCEPT|VIEW3D");
                returned.await("LIGHTS|111111111111|0");
                returned.await("SOURCE|1");
                check(returned.client.seat().equals("H3") && state.sourceRunning() && !state.breaker(),
                    "Rejoined hunter lost the staged breaker state");
            }
            useControl(host,map,world,owner,start);
            observer.await("SOURCE|0");
            useControl(host,map,world,owner,start);
            observer.await("SOURCE|1");
            check(!state.supplied(),"Restart repaired the independent tripped breaker");
            useControl(host,map,world,owner,breaker);
            observer.await("LIGHTS|111111111111|1");
            ghost.client.interact(start.target(),start.observer().yaw(),start.observer().pitch());
            ghost.await("REJECT|");
            synchronized (guard) {
                AuthoredTestSupport.place(map,world,0,map.spawn(13));
            }
            owner.client.interact(start.target(),start.observer().yaw(),start.observer().pitch());
            owner.await("REJECT|");
            check(state.sourceRunning() && state.supplied() && host.running(), "Invalid input changed the running case");
        }
        System.out.println("PASS four real TCP peers: exact College revision, supported move, room switching, source stop/start, authorized ghost trip, blackout view and reconnect resync, physical reset and rejected distant/ghost control");
    }

    private static void useControl(LanHost host, HouseMap map, LanWorld world,
                                 AuthoredLevelNetworkTest.Peer peer, Aim aim) throws Exception {
        HouseRebuildPowerTest.interact(host,map,world,peer,aim.observer());
        peer.await("ACCEPT|INTERACT|"+aim.target().name());
    }

    private static void capture(HouseMap map, Path folder) throws Exception {
        Files.createDirectories(folder);
        var renderer = new SceneRenderer();
        renderer.setRenderSize(960,600);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights = new RoomLighting();
        renderer.setLampHardware(EquipmentTiers.Tier.III,false);
        renderer.weatherModule().setTimeOfDay(.52);
        renderer.weatherModule().pauseClock(true);
        double[][] points = {{28.7,0,27.7,27.46,.70,30.13},
                             {17.7,-3,31.1,15.8,-1.90,33.54},
                             {35.5,-3,39.4,42,-1.6,40}};
        String[] views = {"generator","maintenance-board","cross-tunnel"};
        for (int i = 0; i < points.length; i++) {
            var p = points[i];
            double y = map.groundHeight(p[0],p[2],map.roomAt(p[0],p[1],p[2]));
            check(map.traversable(p[0],y,p[2]),"Unsupported standing camera: " +views[i]);
            renderer.x = p[0]; renderer.z = p[2]; renderer.eyeHeight = y + HunterPosture.STANDING_EYE;
            renderer.yaw = Math.atan2(p[3]-p[0],p[5]-p[2]);
            renderer.pitch = Math.atan2(p[4]-renderer.eyeHeight,Math.hypot(p[3]-p[0],p[5]-p[2]));
            int[] supplied = null;
            for (String power : List.of("supplied","blackout","handheld")) {
                renderer.roomLights.setBreaker(power.equals("supplied"));
                renderer.lamp = i==0 || power.equals("handheld");
                renderer.drawNetwork(map,List.of(),-1,0);
                var backend = GpuWorldRenderer.active();
                if (backend != null) {
                    check(renderer.awaitSubmittedGpuPrewarm(30_000_000_000L,()->false),"No hardware frame");
                    renderer.pinCompletedGpuFrame(backend.latest());
                    renderer.drawNetwork(map,List.of(),-1,0);
                    check(renderer.frameDiagnostics().gpu() && renderer.frameDiagnostics().gpuReadbackAvailable(),
                        "Native electrical capture used software fallback");
                }
                check(renderer.frameDiagnostics().frustumFaces()>100,"Empty runtime view " + views[i]);
                BufferedImage frame = renderer.image();
                check(ImageIO.write(frame,"png",folder.resolve(views[i]+"-"+power+".png").toFile()),"Capture PNG write failed");
                if (i>0 && power.equals("supplied"))
                    supplied = frame.getRGB(0,0,frame.getWidth(),frame.getHeight(),null,0,frame.getWidth());
                if (i>0 && power.equals("blackout")) {
                    int[] blackout = frame.getRGB(0,0,frame.getWidth(),frame.getHeight(),null,0,frame.getWidth());
                    int brighter = 0;
                    for (int j=0;j<blackout.length;j++) {
                        if (channels(supplied[j])-channels(blackout[j])>=15)brighter++;
                    }
                    check(brighter>1200,"No meaningful fixture response in actual "+views[i]+" pixels ("+brighter+")");
                    System.out.println("PIXELS "+views[i]+" "+(backend==null?"software":"OpenGL")+" supplied over blackout="+brighter);
                }
            }
        }
    }

    private static int channels(int rgb) {
        return (rgb>>>16 & 255) + (rgb>>>8 & 255) + (rgb & 255);
    }

    public static void main(String[] args) throws Exception {
        Path stage = Path.of(args[0]);
        var campus = CollegeNativeRuntimeReview.load(Path.of(args[1]),stage.resolve("college-electrical.glb"),stage.resolve("college-electrical.hcs2"));
        var map = campus.map();
        CollegeNativeRuntimeReview.audit(campus);
        CollegeNativeRuntimeReview.replayRoutes(campus,HunterPosture.CROUCH_HEIGHT);
        System.out.println("PASS all 88 native route directions crouched");
        var roomSwitches = switches(map);
        var starter = new Vec(27.465,.565,29.993);
        var primary = new Vec(15.80,-2.10,33.529);
        var north = new Vec(27.465,0,29.023);
        var panel = new Vec(15.80,-3,32.51);
        var supply = aim(map,north,starter,InteractionFocus.Kind.GENERATOR,"college-generator",HunterPosture.STANDING_HEIGHT);
        var main = aim(map,panel,primary,InteractionFocus.Kind.BREAKER,"college-breaker",HunterPosture.STANDING_HEIGHT);
        aim(map,north,starter,InteractionFocus.Kind.GENERATOR,"college-generator",HunterPosture.CROUCH_HEIGHT);
        aim(map,panel,primary,InteractionFocus.Kind.BREAKER,"college-breaker",HunterPosture.CROUCH_HEIGHT);
        check(RoomLighting.breakerAimed(map,panel.x(),panel.y(),panel.z(),0),"The ordinary breaker action must reach the real cabinet face");
        checkLight(map);
        host(map,supply,main,roomSwitches.get(4));
        if (args.length > 2) {
            java.lang.reflect.Method stop = null;
            if (List.of(args).contains("--gpu")) {
                var launcher = Class.forName("org.beckenburg.gpu.GpuDesktop");
                var start = launcher.getDeclaredMethod("startGpu");
                start.setAccessible(true);
                start.invoke(null);
                stop = launcher.getDeclaredMethod("stopReviewGpu");
                stop.setAccessible(true);
            }
            try {
                capture(map,Path.of(args[2]));
            } finally {
                if (stop!=null)stop.invoke(null);
            }
        }
    }
}
