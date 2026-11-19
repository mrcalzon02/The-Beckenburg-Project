package org.beckenburg;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import org.beckenburg.LevelDefinition.Vec;

/** Acceptance of the packaged Battery through ordinary solo selection and LAN dispatch. */
public final class SeacliffDispatchTest {
    private static final HouseMap.Site SITE = HouseMap.Site.SEACLIFF;
    private static final String ROOT = "/org/beckenburg/sites/";

    private static void check(boolean condition, String failure) {
        if (!condition) throw new AssertionError(failure);
    }

    private static HouseMap select() {
        var location = SiteCatalog.Location.SEACLIFF;
        System.clearProperty(SeacliffNativeLevel.REVIEW_PROPERTY);
        check(!SiteCatalog.playable(SITE) && !List.of(SiteCatalog.soloLocations()).contains(location),
            "Public selection exposed the held Battery");
        System.setProperty(SeacliffNativeLevel.REVIEW_PROPERTY, "true");
        check(SiteCatalog.playable(SITE) && List.of(SiteCatalog.soloLocations()).contains(location),
            "Review selection omitted the Battery");
        check(MissionSites.campaign().stream().noneMatch(entry -> entry.id().equals("WLD-17")), "Review selection unlocked the campaign chapter");

        var map = new HouseMap(SiteCatalog.mapFor(location));
        check(map.level().id().equals("wld-17") && map.valid() && RitualSites.valid(map), "Battery mission is invalid");
        check(map.nativeInvestigation().doors().size() == 12, "Installed package is missing fitted architecture");
        check(RoomAcousticPresets.forRoom(map.level(), 0) == RoomAcousticPresets.Type.STONE_HALL
            && RoomAcousticPresets.forRoom(map.level(), 9) == RoomAcousticPresets.Type.MECHANICAL
            && RoomAcousticPresets.forRoom(map.level(), 10) == RoomAcousticPresets.Type.LIBRARY,
            "Battery gallery, plant and records soundscapes disagree with their use");
        var solo = new InvestigationSession(map, InvestigationSession.Role.OBSERVER, 101);
        var anchor = map.spawn(solo.anchorRoom());
        check(map.roomAt(anchor.x(), anchor.y(), anchor.z()) == solo.anchorRoom()
            && map.traversable(anchor.x(), anchor.y(), anchor.z()), "Solo case anchor is unsupported");
        System.out.println("PASS packaged review selection and native solo case " + map.level().revision());
        try {
            HouseMap.forNetworkRevision(SITE, "unavailable-battery-revision");
        } catch (IllegalArgumentException expected) {
            return map;
        }
        throw new AssertionError("Peer accepted a different level revision");
    }

    private static void auditGuides(HouseMap map) throws Exception {
        try (var manifest = SeacliffDispatchTest.class.getResourceAsStream(ROOT + "seacliff-runtime.tsv");
             var model = SeacliffDispatchTest.class.getResourceAsStream(ROOT + "seacliff-native.glb");
             var collision = SeacliffDispatchTest.class.getResourceAsStream(ROOT + "seacliff-native.hcs2")) {
            check(manifest != null && model != null && collision != null, "Seacliff resources are absent from the JAR");
            var original = new String(manifest.readAllBytes(), StandardCharsets.UTF_8);
            var records = original.lines().filter(row -> !row.startsWith("LINK\t") && !row.startsWith("POINT\t")).toList();
            check(records.size() < original.lines().count(), "Negative control did not remove any review guides");
            var altered = String.join("\n", records) + "\nLINK\tthrough-wall\t32\t0\t26.8\n"
                + "LINK\tthrough-wall\t32\t0\t25.2\nPOINT\tthrough-wall\t32\t0\t25.2\n";
            var packet = SeacliffNativeLevel.read(new ByteArrayInputStream(altered.getBytes(StandardCharsets.UTF_8)),
                model, collision);
            check(packet.definition().equals(map.level()) && packet.scene().revision().equals(map.nativeInvestigation().revision()),
                "Edited guide changed packaged architecture or network revision");
            var negative = new HouseMap(SITE, packet.definition(), null, packet.scene());
            var side = new Vec(32, 0, 26.8);
            check(negative.traversable(32, 0, 25.2) && negative.traversable(side.x(), side.y(), side.z()),
                "Negative control has unusable floor on one side of the partition");
            for (int step = 1; step <= 16 && side != null; step++)
                side = negative.resolveMove(side, 32, 26.8 - step * .10);
            check(side == null && !negative.clearLineOfSight(32, 1.25, 26.8, 32, 1.25, 25.2),
                "A guide across solid masonry granted sight or movement");
        }
        System.out.println("PASS misleading guides cannot change the native revision or pierce the fitted partition");
    }

    private static void auditHiding(HouseMap map) {
        var solo = new InvestigationSession(map, InvestigationSession.Role.OBSERVER, 101);
        var cupboards = HidingBudget.candidates(map.level());
        check(cupboards.size() == HidingBudget.MAXIMUM, "Battery lacks three physical hiding places");
        for (var cupboard : cupboards) {
            var pocket = LevelClosets.world(cupboard, 0, 0, 0);
            for (int density : new int[]{0, 100}) {
                map.configureHiding(density);
                check(map.roomAt(pocket.x(), pocket.y(), pocket.z()) == cupboard.room(), "Cupboard is in the wrong room");
                for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                    var feet = LevelClosets.world(cupboard, 0, 0, 1.3);
                    for (int i = 12; i >= 0 && feet != null; i--)
                        feet = map.resolveMove(feet, pocket.x(), pocket.z() + i * .1, height);
                    check(feet != null && map.traversable(feet.x(), feet.y(), feet.z(), height),
                        "Cupboard opening is blocked in normal movement " + cupboard.id());
                }
                check(!map.clearLineOfSight(pocket.x(), pocket.y() + 1.2, pocket.z(),
                    pocket.x(), pocket.y() + 1.2, pocket.z() - 1.1), "Cupboard back does not occlude sight " + cupboard.id());
                check(solo.toggleHide(pocket.x(), pocket.y(), pocket.z()) == (density == 100),
                    "Difficulty does not control effective hiding " + cupboard.id());
                solo.leaveHiding();
            }
        }
        map.configureHiding(70);
        System.out.println("PASS three physical cupboards admit standing and crouched entry, retain backs and honor zero cover");
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

    private static void auditDoor(HouseMap map, AuthoredLevelNetworkTest.Peer walker,
                                  AuthoredLevelNetworkTest.Peer observer) throws Exception {
        for (int room : AuthoredLevelNetworkTest.path(map, HouseMap.EXTRACTION_ROOM, 8)) {
            walker.client.move(room);
            walker.awaitExact("ACCEPT|MOVE|" + room);
        }
        walker.client.view3D();
        walker.awaitExact("ACCEPT|VIEW3D");
        var at = body(walker.await("BODY|2|"));
        check(at.equals(map.spawn(8)), "Normal approach did not start in the lower gallery " + at);
        for (int i = 0; i < 16; i++) at = step(walker, 2, -100, 0);
        for (int i = 0; i < 12; i++) at = step(walker, 2, 0, -100);
        Thread.sleep(125);
        walker.client.step(0, -100);
        check(walker.await("REJECT|").contains("blocked by world collision"), "Closed records leaf did not stop walking");

        double yaw = Math.PI;
        var focus = InteractionFocus.pick(map, new LooseClutter.Observer(2, at.x(), at.y(), at.z(), yaw, 0, true), List.of());
        check(focus != null && focus.kind() == InteractionFocus.Kind.DOOR && focus.id().equals("424"),
            "Normal aim did not find the fitted records leaf " + at);
        walker.client.turn(180);
        walker.client.interact(focus, yaw, 0);
        walker.await("ACCEPT|INTERACT|Open door");
        observer.awaitExact("DOORS|424");
        for (int i = 0; i < 5; i++) at = step(walker, 2, 0, -100);
        check("records".equals(map.architecturalRoomAt(at.x(), at.y() + 1.2, at.z())),
            "Walker did not reach the records room through the real leaf " + at);
        System.out.println("PASS packaged TCP records door blocks, opens, replicates and admits a walker " + at);
        for (int i = 0; i < 8; i++) at = step(walker, 2, -100, 0);
        for (int i = 0; i < 30; i++) at = step(walker, 2, 0, -100);
        var socket = map.level().socket("hide", 10);
        check(LevelClosets.inside(socket, at.x(), at.y(), at.z()), "Host missed the fitted records cupboard " + at);
        walker.client.hide();
        walker.awaitExact("HIDING|1");
        walker.client.hide();
        walker.awaitExact("HIDING|0");
        System.out.println("PASS ordinary LAN movement and hide input from the records cupboard " + at);
    }

    private static void auditLan(HouseMap map) throws Exception {
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT,
                   new CustomContract(3, 100, 150, 100, 100, 100, 100, 100,
                       CustomContract.Sprint.LIMITED, 100, true, true), false, false, SITE);
             var owner = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Scout");
             var guest = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Surveyor");
             var hunter = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Hunter");
             var ghost = new AuthoredLevelNetworkTest.Peer(host.port(), true, "Spirit")) {
            var site = "SITE|" + SITE.name() + "|" + map.level().revision();
            for (var peer : List.of(owner, guest, hunter, ghost)) {
                peer.awaitExact(site);
                var replica = HouseMap.forNetworkRevision(SITE, map.level().revision());
                check(replica.nativeInvestigation().revision().equals(map.nativeInvestigation().revision()),
                    "Peer resolved a different native scene");
                peer.client.ready();
            }
            owner.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();
            owner.await("INFO|mission started");
            owner.client.view3D();
            owner.awaitExact("ACCEPT|VIEW3D");
            var deck = body(owner.await("BODY|0|"));
            check(map.roomAt(deck.x(), deck.y(), deck.z()) == HouseMap.EXTRACTION_ROOM,
                "Normal host missed the raised van deck");
            guest.client.view3D();
            guest.awaitExact("ACCEPT|VIEW3D");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.awaitExact(site);
            ghost.await("GPOSE|");

            var outside = deck;
            for (int i = 0; i < 9; i++) {
                outside = step(owner, 0, 0, -100);
                check(map.traversable(outside.x(), outside.y(), outside.z()), "Replica rejected the host's ramp pose " + outside);
            }
            check(map.roomAt(outside.x(), outside.y(), outside.z()) == 13 && Math.abs(outside.y()) < .002,
                "Normal host missed the physical forecourt");
            boolean seen = false;
            for (int i = 0; i < 50 && !seen; i++) seen = body(guest.await("BODY|0|")).equals(outside);
            check(seen, "Guest missed the supported forecourt pose");
            System.out.println("PASS packaged four-peer TCP arrival " + deck + " -> " + outside);
            auditDoor(map, hunter, owner);
            auditHauntAndReconnect(host, owner, guest, hunter, ghost);
        }
    }

    private static void auditHauntAndReconnect(LanHost host, AuthoredLevelNetworkTest.Peer owner,
            AuthoredLevelNetworkTest.Peer observer, AuthoredLevelNetworkTest.Peer hunter,
            AuthoredLevelNetworkTest.Peer ghost) throws Exception {
        // Select the electrical variant deterministically while leaving the host action and broadcast intact.
        var guard = LanSafeAreaBarrierTest.field(host, "mutex");
        int room;
        synchronized (guard) {
            room = ((GhostDomain) LanSafeAreaBarrierTest.field(host, "ghost")).room();
            long seed = 0;
            while (!new GhostHauntingFolio(seed).choose(GhostDomain.Action.DISTURB, GhostIdentity.Type.SPIRIT, room, 1)
                    .entry().id().equals("lightning_static")) seed++;
            var folio = LanSafeAreaBarrierTest.field(host, "hauntingFolio");
            ((Random) LanSafeAreaBarrierTest.field(folio, "random")).setSeed(seed);
        }
        ghost.client.ghost(GhostDomain.Action.DISTURB, room);
        ghost.await("ACCEPT|GHOST|DISTURB");
        observer.awaitExact("LIGHTS|111111111111|0");
        var power = (RoomLighting) LanSafeAreaBarrierTest.field(host, "lights");
        check(power.sourceRunning() && !power.breaker() && !power.supplied(),
            "A ghost trip incorrectly stopped the independent source");

        var ticket = hunter.client.resumeTicket();
        check(ticket != null, "Native case failed to issue a recovery ticket");
        ((java.net.Socket) LanSafeAreaBarrierTest.field(hunter.client, "socket")).close();
        owner.await("PEER|LOST|2");
        try (var returned = new AuthoredLevelNetworkTest.Peer(host.port(), ticket)) {
            returned.await("INFO|mission resumed");
            returned.client.view3D();
            returned.awaitExact("ACCEPT|VIEW3D");
            returned.awaitExact("LIGHTS|111111111111|0");
            returned.awaitExact("SOURCE|1");
            returned.awaitExact("DOORS|424");
            check(returned.client.seat().equals("H3"), "Reconnect changed the hunter seat");
        }
        System.out.println("PASS live ghost trips the breaker and reconnect preserves the running source and open door");
    }

    public static void main(String[] args) throws Exception {
        var previous = System.getProperty(SeacliffNativeLevel.REVIEW_PROPERTY);
        try {
            var map = select();
            auditGuides(map);
            auditHiding(map);
            auditLan(map);
            if (args.length > 0) {
                var output = Path.of(args[0]);
                Files.createDirectories(output);
                map.toggleDoor(424, System.nanoTime() - 2_000_000_000L);
                CollegeContainmentTest.frame(map, output, "seacliff-packaged-records-threshold",
                    new Vec(29.04, 0, 27.8), 29.04, 22, -.06);
                map.configureHiding(100);
                CollegeContainmentTest.frame(map, output, "seacliff-packaged-records-cupboard",
                    new Vec(27.35, 0, 18.05), 26.5, 15.45, -.15);
            }
        } finally {
            if (previous == null) System.clearProperty(SeacliffNativeLevel.REVIEW_PROPERTY);
            else System.setProperty(SeacliffNativeLevel.REVIEW_PROPERTY, previous);
        }
    }
}
