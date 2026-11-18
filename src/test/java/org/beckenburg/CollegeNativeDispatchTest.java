package org.beckenburg;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

/** Exercises the packaged College from ordinary map selection and a real TCP spawn. */
public final class CollegeNativeDispatchTest {
    private static final HouseMap.Site SITE = HouseMap.Site.COLLEGE;
    private static final String RESOURCES = "/org/beckenburg/sites/";

    private static void check(boolean valid, String why) {
        if (!valid) throw new AssertionError(why);
    }

    private static void rejectRevision(String revision) {
        try {
            HouseMap.forNetworkRevision(SITE, revision);
        } catch (IllegalArgumentException rejected) {
            return;
        }
        throw new AssertionError("Accepted an unavailable College revision " + revision);
    }

    private static void verifyGuidesHaveNoAuthority(HouseMap map) throws Exception {
        try (var metadata = CollegeNativeDispatchTest.class.getResourceAsStream(RESOURCES + "college-runtime.tsv");
             var model = CollegeNativeDispatchTest.class.getResourceAsStream(RESOURCES + "college-native.glb");
             var collision = CollegeNativeDispatchTest.class.getResourceAsStream(RESOURCES + "college-native.hcs2")) {
            check(metadata != null && model != null && collision != null, "Missing exact College package contents");
            var text = new String(metadata.readAllBytes(), StandardCharsets.UTF_8);
            var stripped = text.lines().filter(line -> !line.startsWith("ARRIVAL\t") && !line.startsWith("ROUTE\t")).toList();
            check(stripped.size() < text.lines().count(), "Fixture lacks the review guides");
            var misleading = String.join("\n", stripped) + "\nROUTE\tthrough-wall\tunused\t20.8\t0\t60\t20.8\t0\t64"
                + "\nARRIVAL\t20.8\t0\t60\t20.8\t0\t64\n";
            var packet = CollegeNativeLevel.read(new ByteArrayInputStream(misleading.getBytes(StandardCharsets.UTF_8)),
                model, collision);
            var rebuilt = new HouseMap(SITE, packet.definition(), null, packet.scene());
            check(rebuilt.level().revision().equals(map.level().revision()), "Guides affected the advertised revision");
            check(packet.scene().revision().equals(map.nativeInvestigation().revision()), "Guides affected authored geometry");
            var feet = new Vec(20.8, 0, 60);
            check(rebuilt.traversable(feet.x(), feet.y(), feet.z()), "Negative-control approach has no usable floor");
            for (int step = 1; step <= 40 && feet != null; step++)
                feet = rebuilt.resolveMove(feet, 20.8, 60 + step * .10);
            check(feet == null && !rebuilt.clearLineOfSight(20.8, 1.3, 60, 20.8, 1.3, 64),
                "A guide through the teaching facade granted passage");
        }
    }

    private static Vec body(String frame) {
        var parts = LanProtocol.parts(frame);
        return new Vec(Integer.parseInt(parts.get(2)) / 1000.0, Integer.parseInt(parts.get(3)) / 1000.0,
            Integer.parseInt(parts.get(4)) / 1000.0);
    }

    private static void verifyHostArrival(HouseMap local) throws Exception {
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT, CustomContract.standard(3), false, false, SITE);
             var owner = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Surveyor");
             var guest = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Observer");
             var hunter = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Investigator");
             var ghost = new AuthoredLevelNetworkTest.Peer(host.port(), true, "Spirit")) {
            var peers = List.of(owner, guest, hunter, ghost);
            for (var peer : peers) {
                var fields = LanProtocol.parts(peer.await("SITE|"));
                check(fields.get(1).equals(SITE.name()) && fields.get(2).equals(local.level().revision()),
                    "Ordinary host advertised a different College revision");
                var replica = HouseMap.forNetworkRevision(HouseMap.Site.valueOf(fields.get(1)), fields.get(2));
                check(replica.nativeInvestigation().revision().equals(local.nativeInvestigation().revision()),
                    "Client did not load the host's physical College source");
                peer.client.ready();
            }
            owner.await("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();
            owner.await("INFO|mission started");
            owner.client.view3D();
            owner.await("ACCEPT|VIEW3D");
            var first = body(owner.await("BODY|0|"));
            check(local.roomAt(first.x(), first.y(), first.z()) == HouseMap.EXTRACTION_ROOM,
                "Ordinary hunter did not spawn on the canonical van deck " + first);
            // The normal room-level entry point establishes the corridor spawn.
            // All motion and the door interaction after VIEW3D use physical host inputs.
            for (int room : AuthoredLevelNetworkTest.path(local, HouseMap.EXTRACTION_ROOM, 9)) {
                guest.client.move(room);
                guest.awaitExact("ACCEPT|MOVE|" + room);
            }
            guest.client.view3D();
            guest.await("ACCEPT|VIEW3D");
            var corridorSpawn = body(guest.await("BODY|1|"));
            ghost.client.viewGhost3D();
            ghost.await("ACCEPT|GVIEW3D");
            ghost.awaitExact("SITE|COLLEGE|" + local.level().revision());
            ghost.await("GPOSE|");

            var last = first;
            for (int i = 0; i < 9; i++) {
                last = step(owner, 0, -100, 0);
                check(local.traversable(last.x(), last.y(), last.z()), "Peer rejected the ordinary host's step " + last);
                check(Math.abs(last.x() - (first.x() - .31 * (i + 1))) < .003,
                    "Host step did not follow the backed van axis " + last);
            }
            check(local.roomAt(last.x(), last.y(), last.z()) == 13 && Math.abs(last.y()) < .002,
                "Ordinary host failed to leave the van ramp onto the forecourt " + last);
            final var destination = last;
            boolean guestSawDestination = false;
            for (int i = 0; i < 50 && !guestSawDestination; i++) {
                var acknowledged = body(guest.await("BODY|0|"));
                guestSawDestination = acknowledged.equals(destination);
            }
            check(guestSawDestination, "Guest never received the ordinary host's supported forecourt pose");
            System.out.printf("PASS ordinary College TCP dispatch to four peers, supported cargo ramp from %s to %s%n",
                first, last);
            verifyBedroom(local, guest, corridorSpawn);
        }
    }

    private static Vec step(AuthoredLevelNetworkTest.Peer peer, int seat, int dx, int dz) throws Exception {
        Thread.sleep(125);
        peer.client.step(dx, dz);
        peer.await("ACCEPT|STEP");
        return body(peer.await("BODY|" + seat + "|"));
    }

    private static void verifyBedroom(HouseMap map, AuthoredLevelNetworkTest.Peer guest, Vec spawn) throws Exception {
        check(Math.abs(spawn.y()-3.6)<.002 && Math.abs(spawn.x()-52)<.002 && Math.abs(spawn.z()-44)<.002,
            "Missing ordinary supported residence corridor spawn " + spawn);
        var at = spawn;
        for (int i = 0; i < 22; i++) at = step(guest, 1, 0, -100);
        for (int i = 0; i < 5; i++) at = step(guest, 1, -100, 0);
        var portal = map.nativeInvestigation().doors().stream().filter(d -> d.spec().frameId().equals("res-west-1-door"))
            .findFirst().orElseThrow().spec().portal();
        check(Math.abs(at.z()-portal.z()) < .25 && at.x()>portal.x() && at.x()<portal.x()+.5,
            "Ordinary walk missed the fitted west bedroom jamb " + at);

        Thread.sleep(125);
        guest.client.step(-100, 0);
        check(guest.await("REJECT|").contains("blocked by world collision"), "A closed bedroom door did not stop the host walker");
        double yaw = -Math.PI/2;
        var observer = new LooseClutter.Observer(1, at.x(), at.y(), at.z(), yaw, 0, true);
        var focus = InteractionFocus.pick(map, observer, List.of());
        check(focus != null && focus.kind() == InteractionFocus.Kind.DOOR
            && focus.id().equals(Integer.toString(portal.id())), "Ordinary corridor aim missed the bedroom leaf");
        guest.client.turn(270);
        guest.client.interact(focus, yaw, 0);
        guest.await("ACCEPT|INTERACT|Open door");
        guest.awaitExact("DOORS|" + portal.id());
        for (int i = 0; i < 5; i++) at = step(guest, 1, -100, 0);
        check("res-west-1".equals(map.architecturalRoomAt(at.x(),at.y()+1.2,at.z())),
            "Normal host world did not enter the bedroom through its open door " + at);
        System.out.println("PASS ordinary host closed-leaf stop, aimed interaction and west twin room arrival " + at);
    }

    private static void capture(HouseMap map, Path output, boolean gpu) throws Exception {
        Files.createDirectories(output);
        java.lang.reflect.Method stop = null;
        if (gpu) {
            var launcher = Class.forName("org.beckenburg.gpu.GpuDesktop");
            var start = launcher.getDeclaredMethod("startGpu");
            start.setAccessible(true);
            stop = launcher.getDeclaredMethod("stopReviewGpu");
            stop.setAccessible(true);
            start.invoke(null);
        }
        try {
            CollegeContainmentTest.frame(map, output, "dispatched-teaching-entrance", new Vec(24, 0, 65), 24, 60, -.04);
            CollegeContainmentTest.frame(map, output, "dispatched-lecture-heating", new Vec(10.2, 0, 43.5),
                6.45, 40, -.20);
        } finally {
            if (stop != null) stop.invoke(null);
        }
    }

    public static void main(String[] args) throws Exception {
        var home = System.getProperty("user.home");
        var setting = System.getProperty(CollegeNativeLevel.REVIEW_PROPERTY);
        var isolated = Files.createTempDirectory("beckenburg-college-dispatch-");
        System.setProperty("user.home", isolated.toString());
        try {
            System.clearProperty(CollegeNativeLevel.REVIEW_PROPERTY);
            var stock = new HouseMap(SITE);
            check(!stock.nativeSite(), "Unrequested replacement of the current College mission");
            check(HouseMap.forNetworkRevision(SITE, stock.level().revision()).level().equals(stock.level()),
                "Legacy College revision stopped dispatching");
            var editor = new LevelAuthoringSession(SITE);
            check(editor.props().equals(stock.level().props()), "Legacy editor changed coordinate authority");

            System.setProperty(CollegeNativeLevel.REVIEW_PROPERTY, "true");
            var candidate = new HouseMap(SITE);
            check(candidate.nativeSite() && candidate.valid() && RitualSites.valid(candidate), "Candidate has invalid gameplay bindings");
            check(candidate.nativeInvestigation().doors().size() == 20
                && candidate.nativeInvestigation().spaces().stream().filter(NativeInvestigationScene.Space::accessible).count() == 40,
                "Packaged door count or searchable envelope differs from the reviewed packet");
            rejectRevision(stock.level().revision());
            rejectRevision("unverified-college-revision");
            verifyGuidesHaveNoAuthority(candidate);
            verifyHostArrival(candidate);
            if (args.length > 0) capture(candidate, Path.of(args[0]), List.of(args).contains("--gpu"));

            editor.save();
            check(Files.isRegularFile(editor.documentPath()), "Legacy editor test did not write a local scene");
            var saved = new HouseMap(SITE);
            check(!saved.nativeSite() && saved.level().props().equals(editor.props()),
                "Native review selection discarded the saved, differently positioned College authoring source");
            rejectRevision(candidate.level().revision());

            System.out.println("PASS legacy authoring, mismatch rejection and nonphysical route metadata");
            System.out.println("COLLEGE LEVEL " + candidate.level().revision());
            System.out.println("COLLEGE SCENE " + candidate.nativeInvestigation().revision());
        } finally {
            if (setting == null) System.clearProperty(CollegeNativeLevel.REVIEW_PROPERTY);
            else System.setProperty(CollegeNativeLevel.REVIEW_PROPERTY, setting);
            System.setProperty("user.home", home);
            try (var entries = Files.walk(isolated)) {
                for (var path : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
