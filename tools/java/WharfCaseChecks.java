package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;
import org.beckenburg.WharfNativeLevel.Story;

/** Replays physical approaches and reads on the named Wharf package. */
final class WharfCaseChecks {
    private WharfCaseChecks() {}

    public static void main(String[] args) throws Exception {
        var report = new java.util.ArrayList<String>();
        fieldActions(WharfNativeLevel.definition(), report);
        report.forEach(System.out::println);
    }

    static void auditDispatch(LevelDefinition level, NativeInvestigationScene scene) {
        var location = MissionSites.byId("WLD-06").location();
        require(location == SiteCatalog.Location.WHARF && List.of(SiteCatalog.soloLocations()).contains(location)
                && SiteCatalog.mapFor(location) == HouseMap.Site.WHARF, "Named Wharf dispatch is unavailable");
        var network = HouseMap.forNetworkRevision(HouseMap.Site.WHARF, level.revision());
        require(network.level().equals(level) && network.nativeInvestigation().revision().equals(scene.revision()),
                "Named network geometry differs from the inspected source");
    }

    static void audit(HouseMap map, List<Story> stories, Path captures, List<String> report) throws Exception {
        require(stories.size() == 2 && SiteCaseFiles.forSite(map.level().id()).reference().equals("BB / WLD-06"),
                "The Wharf case board is incomplete");
        for (var story : stories) {
            var route = List.of(map.spawn(story.owner()), story.feet());
            NativeSchoolInteriorReview.walk(map, route, HunterPosture.STANDING_HEIGHT, story.document());
            NativeSchoolInteriorReview.walk(map, route, HunterPosture.CROUCH_HEIGHT, story.document());
            focus(map, 0, story, story.feet());
            var renderer = new SceneRenderer();
            renderer.setRenderSize(880, 580);
            renderer.setPsxPreset(PsxPipeline.Preset.OFF);
            renderer.x = story.feet().x();
            renderer.z = story.feet().z();
            renderer.eyeHeight = story.feet().y() + HunterPosture.STANDING_EYE;
            renderer.yaw = yaw(story, story.feet());
            renderer.pitch = pitch(story, story.feet());
            renderer.lamp = true;
            renderer.setLampHardware(EquipmentTiers.Tier.III, false);
            NativeSchoolInteriorReview.drawFrame(renderer, map, captures.resolve("wharf-bench-" + story.document() + ".png"));

            var text = new BufferedImage(880, 580, BufferedImage.TYPE_INT_RGB);
            var graphics = text.createGraphics();
            try { SiteStories.draw(graphics, 880, 580, map, story.document()); }
            finally { graphics.dispose(); }
            require(ImageIO.write(text, "png", captures.resolve("wharf-record-" + story.document() + ".png").toFile()),
                    "No Wharf record image encoder");
            report.add("standing_aim_" + story.document() + "\tPASS");
        }
        report.add("named_solo_and_network_revision_match\tPASS");
    }

    static void live(LevelDefinition level, List<Story> stories, List<String> report) throws Exception {
        var map = HouseMap.forNetworkRevision(HouseMap.Site.WHARF, level.revision());
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT, CustomContract.standard(3), map);
             var first = new Peer(host, false, "Departure tally");
             var second = new Peer(host, false, "Crossing sheet");
             var witness = new Peer(host, false, "Wharf witness");
             var ghost = new Peer(host, true, "Wharf ghost")) {
            start(map, first, List.of(first, second, witness, ghost));
            for (int seat = 0; seat < stories.size(); seat++) {
                var peer = seat == 0 ? first : second;
                var story = stories.get(seat);
                var feet = arrive(map, peer, seat, story.owner());
                feet = walk(peer, seat, feet, story.feet());
                var target = focus(map, seat, story, feet);
                double yaw = yaw(story, feet);
                peer.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)), 360));
                peer.client.interact(target, yaw, pitch(story, feet));
                peer.awaitExact("INTERACTED|INTERFACE|STORY_" + story.document());
            }
        }
        report.add("named_four_peer_authored_record_interactions\tPASS");
    }

    static void fieldActions(LevelDefinition level, List<String> report) throws Exception {
        var map = HouseMap.forNetworkRevision(HouseMap.Site.WHARF, level.revision());
        camera(map, report);
        retreat(map, report);
        for (var ghost : List.of(GhostIdentity.Type.POLTERGEIST, GhostIdentity.Type.SPECTER, GhostIdentity.Type.SPIRIT))
            rite(map, ghost, report);
    }

    private static void camera(HouseMap map, List<String> report) throws Exception {
        try (var host = host(GhostIdentity.Type.SPIRIT);
             var operator = new Peer(host, false, "Fish house camera");
             var feed = new Peer(host, false, "Van camera observer");
             var witness = new Peer(host, false, "Crew witness");
             var ghost = new Peer(host, true, "Ghost")) {
            start(map, operator, List.of(operator, feed, witness, ghost), true);
            var feet = arrive(map, operator, 0, 4);
            feet = walk(operator, 0, feet, new Vec(26, 0, 16));
            int heading = 270;
            var camera = new LanVideoCameras.Camera(0, 4, feet.x(), feet.y(), feet.z(), heading, EquipmentTiers.Tier.I);
            var inside = new LanGhostSpatial.Pose(4, 24.8, 0, feet.z(), 0);
            var outside = new LanGhostSpatial.Pose(8, 22.5, 0, feet.z(), 0);
            require(map.roomAt(inside.x(), inside.y(), inside.z()) == 4
                && map.roomAt(outside.x(), outside.y(), outside.z()) == 8
                && LanCameraSight.sees(map, camera, inside, true, GhostIdentity.Type.SPIRIT, "a")
                && !LanCameraSight.sees(map, camera, outside, true, GhostIdentity.Type.SPIRIT, "a"),
                "Fish-house exterior wall did not separate two in-cone camera targets");
            operator.client.turn(heading);
            operator.client.useHeldEquipment();
            String placement = operator.await("CAMERA|PLACE|0|4|");
            operator.awaitExact("CAMERA|POWER|0|1");
            operator.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            arrive(map, feed, 1, 14);
            feed.awaitExact(placement);
            feed.awaitExact("CAMERA|POWER|0|1");
        }
        report.add("named_fish_house_camera\tPASS floor mount, local target, blocked west-wall sight, and van feed");
    }

    private static void retreat(HouseMap map, List<String> report) throws Exception {
        try (var host = host(GhostIdentity.Type.SPIRIT);
             var leader = new Peer(host, false, "Ramp crew");
             var outside = new Peer(host, false, "Shore crew");
             var cargo = new Peer(host, false, "Cargo crew");
             var ghost = new Peer(host, true, "Ghost")) {
            start(map, leader, List.of(leader, outside, cargo, ghost));
            Vec lead = arrive(map, leader, 0, 14);
            Vec trail = arrive(map, outside, 1, 14);
            for (int step = 0; step < 15; step++) {
                lead = NativeSchoolInteriorReview.step(leader, 0, 0, -100);
                trail = NativeSchoolInteriorReview.step(outside, 1, 0, -100);
            }
            require(map.roomAt(lead.x(), lead.y(), lead.z()) == 13 && map.roomAt(trail.x(), trail.y(), trail.z()) == 13,
                    "Both hunters must reach the actual shore apron");
            leader.client.vanDoor();
            leader.awaitExact("REJECT|van door button out of reach");
            for (int step = 0; step < 15; step++) lead = NativeSchoolInteriorReview.step(leader, 0, 0, 100);
            require(map.roomAt(lead.x(), lead.y(), lead.z()) == 14, "The returning hunter missed the cargo floor");
            leader.client.vanDoor();
            leader.awaitExact("ACCEPT|VANDOOR|CLOSED");
            cargo.awaitExact("EXTRACTION|0|1");
            cargo.awaitExact("EXTRACTION|1|0");
            cargo.awaitExact("EXTRACTION|2|1");
            cargo.await("RESULT|RETREATED");
            require(!host.running(), "The named Wharf did not settle the van retreat");
        }
        report.add("named_shore_apron_and_van_retreat\tPASS with the exact extraction roster");
    }

    private static void rite(HouseMap map, GhostIdentity.Type type, List<String> report) throws Exception {
        var rite = type.rite();
        var site = RitualSites.site(map, rite);
        try (var host = host(type);
             var anchor = new Peer(host, false, "Anchor investigator");
             var remote = new Peer(host, false, "Van observer");
             var witness = new Peer(host, false, "Ritual operator");
             var ghost = new Peer(host, true, "Ghost")) {
            var peers = List.of(anchor, remote, witness, ghost);
            start(map, anchor, peers);
            int evidenceRoom = 4;
            arrive(map, anchor, 0, evidenceRoom);
            arrive(map, remote, 1, 14);
            if (type == GhostIdentity.Type.POLTERGEIST) {
                ghost.client.ghost(GhostDomain.Action.DISTURB, evidenceRoom);
                ghost.await("ACCEPT|GHOST|DISTURB|");
                anchor.client.scan(LanCase.Tool.EMF);
                int near = emf(anchor.await("ACCEPT|SCAN|READING EMF in " + evidenceRoom));
                remote.client.scan(LanCase.Tool.EMF);
                int far = emf(remote.await("ACCEPT|SCAN|READING EMF in 14"));
                require(near >= 8 && far <= 7, "Wharf evidence did not distinguish the fish house from the van");
                report.add("named_spatial_emf\tFish house " + near + "/10; van " + far + "/10 after ghost disturbance");
            }
            anchor.client.identify(type);
            anchor.await("ACCEPT|IDENTIFY|");
            anchor.client.ward();
            anchor.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            String operation = switch (rite) {
                case BANISHMENT -> "BANISH";
                case CLEANSING -> "CLEANSE";
                case ABSOLUTION -> "ABSOLVE";
            };
            sendRite(remote.client, rite);
            remote.awaitExact("REJECT|" + operation + "|go to the correct ritual installation");
            var at = arrive(map, witness, 2, site.room());
            require(site.near(new LanWorld.Body(at.x(), at.y(), at.z(), 0, map.roomAt(at.x(), at.y(), at.z()))),
                    "The ritual operator missed the physical installation");
            sendRite(witness.client, rite);
            witness.await("ACCEPT|" + operation + "|RITUAL STARTED");
            remote.awaitExact("RITUAL|STARTED|" + rite);
            String result = switch (rite) {
                case BANISHMENT -> "BANISHED";
                case CLEANSING -> "CLEANSED";
                case ABSOLUTION -> "ABSOLVED";
            };
            for (var peer : peers) peer.await("RESULT|" + result, 8500);
            require(!host.running(), "Accepted Wharf rite did not settle");
            report.add("named_" + rite.name().toLowerCase(java.util.Locale.ROOT) + "\t" + map.roomName(site.room())
                       + ", van refusal, " + result + " on all four peers");
        }
    }

    private static LanHost host(GhostIdentity.Type type) throws Exception {
        return new LanHost("127.0.0.1", 0, type, CustomContract.standard(3), false, false, HouseMap.Site.WHARF);
    }

    private static void start(HouseMap map, Peer first, List<Peer> peers) throws Exception {
        start(map, first, peers, false);
    }

    private static void start(HouseMap map, Peer first, List<Peer> peers, boolean videoCamera) throws Exception {
        for (var peer : peers) {
            peer.awaitExact("SITE|WHARF|" + map.level().revision());
            peer.client.ready();
        }
        first.awaitExact("LOBBY|3|HUMAN|4|WAITING");
        if (videoCamera) {
            first.client.equipAt(0,"VIDEO_CAMERA");
            first.awaitExact("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
        }
        first.client.start();
        first.await("INFO|mission started");
    }

    private static Vec arrive(HouseMap map, Peer peer, int seat, int destination) throws Exception {
        for (int owner : NativeSchoolInteriorReview.missionRoute(map, destination)) {
            peer.client.move(owner);
            peer.awaitExact("ACCEPT|MOVE|" + owner);
        }
        peer.client.view3D();
        peer.awaitExact("ACCEPT|VIEW3D");
        var feet = NativeSchoolInteriorReview.body(peer.await("BODY|" + seat + "|"));
        if (destination == HouseMap.EXTRACTION_ROOM) {
            var pad = map.van().spawn(seat);
            require(map.roomAt(feet.x(), feet.y(), feet.z()) == destination
                && Math.hypot(feet.x() - pad.x(), feet.z() - pad.z()) <= .002
                && Math.abs(feet.y() - map.van().floorY()) <= .002, "Incorrect original cargo seat " + feet);
        } else {
            require(feet.equals(map.spawn(destination)), "Wharf player missed the authored spawn " + destination);
        }
        return feet;
    }

    private static void sendRite(LanClient client, GhostIdentity.Rite rite) throws Exception {
        switch (rite) {
            case BANISHMENT -> client.banish();
            case CLEANSING -> client.cleanse();
            case ABSOLUTION -> client.absolve();
        }
    }

    private static int emf(String reading) {
        int value = reading.lastIndexOf("EMF ") + 4;
        return Integer.parseInt(reading.substring(value, reading.indexOf('/', value)));
    }

    private static Vec walk(Peer peer, int seat, Vec at, Vec target) throws Exception {
        for (int step = 0; step < 60; step++) {
            double dx = target.x() - at.x(), dz = target.z() - at.z();
            double distance = Math.hypot(dx, dz);
            if (distance < .19) return at;
            double gain = Math.min(100 / distance, 310);
            at = NativeSchoolInteriorReview.step(peer, seat, (int)Math.round(dx * gain), (int)Math.round(dz * gain));
        }
        throw new AssertionError("Wharf player stopped at " + at + " on the way to " + target);
    }

    private static InteractionFocus.Target focus(HouseMap map, int seat, Story story, Vec at) {
        var observer = new LooseClutter.Observer(seat, at.x(), at.y(), at.z(), yaw(story, at), pitch(story, at), true);
        var target = InteractionFocus.pick(map, observer, List.of());
        require(target != null && target.kind() == InteractionFocus.Kind.INTERFACE
                && target.id().equals("STORY_" + story.document()), "Cannot read " + story.document() + " from " + at);
        return target;
    }

    private static double yaw(Story story, Vec at) {
        return Math.atan2(story.aim().x() - at.x(), story.aim().z() - at.z());
    }

    private static double pitch(Story story, Vec at) {
        return Math.atan2(story.aim().y() - at.y() - HunterPosture.STANDING_EYE,
                          Math.hypot(story.aim().x() - at.x(), story.aim().z() - at.z()));
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
