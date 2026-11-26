package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;

/** Blackpine review selection and the repair shed's installed controls over ordinary TCP. */
public final class BlackpineDispatchTest {
    private static final HouseMap.Site SITE = HouseMap.Site.BLACKPINE;

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }

    private static Vec body(String frame) {
        var p = LanProtocol.parts(frame);
        return new Vec(Integer.parseInt(p.get(2)) / 1000.0, Integer.parseInt(p.get(3)) / 1000.0,
            Integer.parseInt(p.get(4)) / 1000.0);
    }

    private static Vec step(AuthoredLevelNetworkTest.Peer peer, int seat, int dx, int dz) throws Exception {
        Thread.sleep(125);
        peer.client.step(dx, dz);
        peer.awaitExact("ACCEPT|STEP");
        return body(peer.await("BODY|" + seat + "|"));
    }

    private static void use(HouseMap map, AuthoredLevelNetworkTest.Peer peer, Vec at, Socket socket,
                            InteractionFocus.Kind kind, String response) throws Exception {
        double dx = socket.x() - at.x(), dz = socket.z() - at.z();
        double yaw = Math.atan2(dx, dz);
        double center = socket.y() + (kind == InteractionFocus.Kind.GENERATOR ? .10 : 0);
        double pitch = Math.atan2(center - at.y() - HunterPosture.STANDING_EYE, Math.hypot(dx, dz));
        var eye = new LooseClutter.Observer(1, at.x(), at.y(), at.z(), yaw, pitch, true);
        var focus = InteractionFocus.pick(map, eye, List.of());
        check(focus != null && focus.kind() == kind && focus.id().equals(socket.id()),
            "The actual control cannot be targeted from " + at + " " + socket);
        peer.client.turn((int) Math.floorMod(Math.round(Math.toDegrees(yaw)), 360));
        Thread.sleep(375);
        peer.client.interact(focus, yaw, pitch);
        peer.await(response);
    }

    private static HouseMap select() throws Exception {
        var location = SiteCatalog.Location.BLACKPINE;
        var entry = MissionSites.byId("WLD-01");
        check(SiteCatalog.playable(SITE) && List.of(SiteCatalog.soloLocations()).contains(location)
            && entry.location() == location && entry.playable(), "Blackpine is missing from the normal selectors");
        var map = new HouseMap(SiteCatalog.mapFor(location));
        check(map.valid() && map.hasGenerator() && RitualSites.valid(map)
            && HidingBudget.candidates(map.level()).size() == HidingBudget.MAXIMUM,
            "Packaged camp lost its physical mission or services");
        var solo = new InvestigationSession(map, InvestigationSession.Role.OBSERVER, 101);
        var feet = map.spawn(solo.anchorRoom());
        check(map.roomAt(feet.x(), feet.y(), feet.z()) == solo.anchorRoom()
            && map.traversable(feet.x(), feet.y(), feet.z()), "Solo camp has no supported investigation anchor");
        AuthoredTestSupport.checkSoloLauncher(location, map);
        System.out.println("PASS regular Blackpine selection and solo case " + map.level().revision());
        return map;
    }

    private static void checkSavedTerrainRevision() throws Exception {
        var oldHome = System.getProperty("user.home");
        var home = Files.createTempDirectory("blackpine-terrain-revision");
        System.setProperty("user.home", home.toString());
        var path = org.beckenburg.editor.TerrainZoneDocument.defaultPath("blackpine");
        try {
            var original = new HouseMap(SITE);
            var terrain = original.terrain();
            var grid = new org.beckenburg.editor.TerrainZoneDocument.Grid(terrain.x0, terrain.z0,
                SiteTerrain.STEP, terrain.nx, terrain.nz);
            var document = new org.beckenburg.editor.TerrainZoneDocument("blackpine", grid);
            // An isolated corner edit leaves dispatch spawns and routes untouched.
            document.setDelta(0, 0, .15);
            document.save(path);
            check(SiteTerrain.of(original.level()) == terrain,
                "An active case replaced its existing heightfield after a save");
            var edited = new HouseMap(SITE);
            check(Math.abs(edited.terrain().vertexHeight(0, 0) - terrain.vertexHeight(0, 0) - .15) < 1e-6,
                "Saved terrain did not reach ordinary Blackpine");
            try {
                HouseMap.forNetworkRevision(SITE, original.level().revision());
                throw new AssertionError("Peer accepted the unedited heightfield revision");
            } catch (IllegalArgumentException expected) {
                System.out.println("PASS saved Blackpine terrain mismatch is rejected at network selection");
            }
        } finally {
            System.setProperty("user.home", oldHome);
            for (var entry = path; entry != null && entry.startsWith(home); entry = entry.getParent())
                Files.deleteIfExists(entry);
        }
    }

    private static void checkLoads(HouseMap map) {
        var field = new FixtureLighting(map);
        var lights = new RoomLighting();
        var loads = map.level().sockets().stream().filter(s -> s.type().equals("light")).toList();
        field.frame(lights, 0);
        var supplied = loads.stream().mapToDouble(s -> sample(field, s)).toArray();
        lights.setSourceRunning(false);
        field.frame(lights, 1);
        for (int i = 0; i < loads.size(); i++)
            check(supplied[i] > sample(field, loads.get(i)) + .01, "Fixture failed local blackout " + loads.get(i));
        check(lights.breaker() && !lights.supplied(), "Source stop changed the breaker");
        System.out.println("PASS all " + loads.size() + " placed electrical fixtures respond to loss of source");
    }

    private static double sample(FixtureLighting field, Socket socket) {
        return field.sampleNormal(socket.room(), socket.x(), socket.y() - .4, socket.z(), 0, 1, 0).energy();
    }

    private static void trip(LanHost host, AuthoredLevelNetworkTest.Peer ghost) throws Exception {
        var mutex = LanSafeAreaBarrierTest.field(host, "mutex");
        int room;
        synchronized (mutex) {
            room = ((GhostDomain) LanSafeAreaBarrierTest.field(host, "ghost")).room();
            long seed = 0;
            while (!new GhostHauntingFolio(seed).choose(GhostDomain.Action.DISTURB, GhostIdentity.Type.SPIRIT, room, 1)
                    .entry().id().equals("lightning_static")) seed++;
            var folio = LanSafeAreaBarrierTest.field(host, "hauntingFolio");
            ((Random) LanSafeAreaBarrierTest.field(folio, "random")).setSeed(seed);
        }
        ghost.client.ghost(GhostDomain.Action.DISTURB, room);
        ghost.await("ACCEPT|GHOST|DISTURB");
    }

    private static void verifyHost(HouseMap map) throws Exception {
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT, CustomContract.standard(3), false, false, SITE);
             var owner = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Scout");
             var technician = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Technician");
             var observer = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Observer");
             var ghost = new AuthoredLevelNetworkTest.Peer(host.port(), true, "Spirit")) {
            var site = "SITE|" + SITE.name() + "|" + map.level().revision();
            for (var peer : List.of(owner, technician, observer, ghost)) {
                peer.awaitExact(site);
                var replica = HouseMap.forNetworkRevision(SITE, map.level().revision());
                check(replica.terrain().height(34, 65) == map.terrain().height(34, 65),
                    "Peer resolved a different camp approach");
                peer.client.ready();
            }
            owner.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();
            owner.await("INFO|mission started");
            owner.client.view3D();
            owner.awaitExact("ACCEPT|VIEW3D");
            owner.awaitExact("SOURCE|1");
            var deck = body(owner.await("BODY|0|"));
            check(map.roomAt(deck.x(), deck.y(), deck.z()) == HouseMap.EXTRACTION_ROOM, "Canonical cargo spawn missed");
            var road = deck;
            for (int i = 0; i < 9; i++) road = step(owner, 0, -100, 0);
            check(map.traversable(road.x(), road.y(), road.z()) && map.roomAt(road.x(), road.y(), road.z()) == 13,
                "The ordinary walker missed the supported logging road " + road);
            System.out.println("PASS four Blackpine peers and physical van departure " + deck + " -> " + road);

            for (int room : AuthoredLevelNetworkTest.path(map, HouseMap.EXTRACTION_ROOM, 8)) {
                technician.client.move(room);
                technician.awaitExact("ACCEPT|MOVE|" + room);
            }
            technician.client.view3D();
            technician.awaitExact("ACCEPT|VIEW3D");
            var at = body(technician.await("BODY|1|"));
            for (int i = 0; i < 8; i++) at = step(technician, 1, -100, 0);
            Thread.sleep(125);
            technician.client.step(-100, 0);
            check(technician.await("REJECT|").contains("blocked by world collision"),
                "Closed west shed door did not stop the host walker");
            var door = map.level().portalsFrom(8).stream().filter(p -> p.door() && p.x() == 44).findFirst().orElseThrow();
            double yaw = -Math.PI / 2;
            var eye = new LooseClutter.Observer(1, at.x(), at.y(), at.z(), yaw, 0, true);
            var focus = InteractionFocus.pick(map, eye, List.of());
            check(focus != null && focus.kind() == InteractionFocus.Kind.DOOR
                && focus.id().equals(Integer.toString(door.id())), "Shed threshold missed its actual door leaf");
            technician.client.turn(270);
            technician.client.interact(focus, yaw, 0);
            technician.await("ACCEPT|INTERACT|Open door");
            owner.awaitExact("DOORS|" + door.id());
            for (int i = 0; i < 4; i++) at = step(technician, 1, -100, 0);
            check(map.roomAt(at.x(), at.y(), at.z()) == 12, "Door did not reach the real timber yard " + at);
            for (int i = 0; i < 7; i++) at = step(technician, 1, 100, 0);
            for (int i = 0; i < 3; i++) at = step(technician, 1, 0, -100);
            System.out.println("PASS TCP fitted shed door, graded yard crossing and re-entry");
            check(map.roomAt(at.x(), at.y(), at.z()) == 8, "Control approach left the shed");
            var starter = map.level().socket("generator", 8);
            var breaker = RoomLighting.breaker(map);

            use(map, technician, at, starter, InteractionFocus.Kind.GENERATOR, "ACCEPT|INTERACT");
            owner.awaitExact("SOURCE|0");
            use(map, technician, at, starter, InteractionFocus.Kind.GENERATOR, "ACCEPT|INTERACT");
            owner.awaitExact("SOURCE|1");
            trip(host, ghost);
            owner.awaitExact("LIGHTS|111111111111|0");

            var ticket = observer.client.resumeTicket();
            check(ticket != null, "Host issued no recovery ticket");
            ((java.net.Socket) LanSafeAreaBarrierTest.field(observer.client, "socket")).close();
            owner.await("PEER|LOST|2");
            try (var returned = new AuthoredLevelNetworkTest.Peer(host.port(), ticket)) {
                returned.await("INFO|mission resumed");
                returned.client.view3D();
                returned.awaitExact("ACCEPT|VIEW3D");
                returned.awaitExact("LIGHTS|111111111111|0");
                returned.awaitExact("SOURCE|1");

                use(map, technician, at, starter, InteractionFocus.Kind.GENERATOR, "ACCEPT|INTERACT");
                returned.awaitExact("SOURCE|0");
                use(map, technician, at, starter, InteractionFocus.Kind.GENERATOR, "ACCEPT|INTERACT");
                returned.awaitExact("SOURCE|1");
                var state = (RoomLighting) LanSafeAreaBarrierTest.field(host, "lights");
                check(!state.supplied() && !state.breaker(), "Generator restart incorrectly repaired the tripped breaker");
                use(map, technician, at, breaker, InteractionFocus.Kind.BREAKER, "ACCEPT|INTERACT");
                returned.awaitExact("LIGHTS|111111111111|1");
                check(state.supplied(), "Physical panel reset failed");
                System.out.println("PASS TCP starter, ghost trip, seat recovery, restart isolation and physical breaker reset from " + at);
            }
        }
    }

    private static void writeSchedule(HouseMap map, Path output) throws Exception {
        var lines = new java.util.ArrayList<String>();
        lines.add("type\tid\troom\tx_m\ty_m\tz_m\tcircuit");
        for (var socket : map.level().sockets()) {
            String circuit = switch (socket.type()) {
                case "generator", "breaker" -> "BPK-MAIN";
                case "light", "switch" -> "BPK-" + (socket.room() + 1);
                default -> null;
            };
            if (circuit != null) lines.add(String.format(java.util.Locale.ROOT, "%s\t%s\t%s\t%.5f\t%.5f\t%.5f\t%s",
                socket.type(), socket.id(), map.level().room(socket.room()).name(),
                socket.x(), socket.y(), socket.z(), circuit));
        }
        Files.write(output, lines);
    }

    public static void main(String[] args) throws Exception {
        var previous = System.getProperty(BlackpineLevel.REVIEW_PROPERTY);
        try {
            var map = select();
            checkSavedTerrainRevision();
            checkLoads(map);
            verifyHost(map);
            if (args.length > 1) writeSchedule(map, Path.of(args[1]));
            if (args.length > 0) {
                var folder = Path.of(args[0]);
                Files.createDirectories(folder);
                var floor = map.floorY(8);
                CollegeContainmentTest.frame(map, folder, "blackpine-repair-shed-generator",
                    new Vec(46.6, floor, 49.65), 45.02, 47.8, -.30);
            }
        } finally {
            if (previous == null) System.clearProperty(BlackpineLevel.REVIEW_PROPERTY);
            else System.setProperty(BlackpineLevel.REVIEW_PROPERTY, previous);
        }
    }
}
