package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.beckenburg.LevelDefinition.Vec;

/** Ordinary socket join and supported movement across the native Hollow Creek arrival. */
public final class HollowCreekDispatchTest {
    private static final HouseMap.Site SITE = HouseMap.Site.HOLLOW_CREEK;

    private static Vec body(String frame) {
        var p = LanProtocol.parts(frame);
        return new Vec(Integer.parseInt(p.get(2)) / 1000.0, Integer.parseInt(p.get(3)) / 1000.0,
            Integer.parseInt(p.get(4)) / 1000.0);
    }

    private static void step(AuthoredLevelNetworkTest.Peer peer, int dz) throws Exception {
        Thread.sleep(125);
        peer.client.step(0, dz);
        peer.awaitExact("ACCEPT|STEP");
    }

    private static void checkSelection() {
        var entry = MissionSites.byId("WLD-02");
        HollowCreekRuntimeTest.check(entry.location() == SiteCatalog.Location.HOLLOW_CREEK && entry.playable()
            && SiteCatalog.playable(SITE) && List.of(SiteCatalog.soloLocations()).contains(entry.location()),
            "Hollow Creek is missing from the normal selectors");
        var solo = new InvestigationSession(new HouseMap(SiteCatalog.mapFor(entry.location())),
            InvestigationSession.Role.OBSERVER, 101);
        HollowCreekRuntimeTest.check(solo.anchorRoom() >= 0 && solo.anchorRoom() < HouseMap.EXTRACTION_ROOM,
            "Normal solo case did not get an investigation anchor");
    }

    public static void main(String[] args) throws Exception {
        checkSelection();
        var map = new HouseMap(SITE);
        String site = "SITE|HOLLOW_CREEK|" + map.level().revision();
        try (var host = new LanHost("127.0.0.1", 0, GhostIdentity.Type.SPIRIT,
                   CustomContract.standard(3), false, false, SITE);
             var owner = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Scout");
             var guest = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Observer");
             var hunter = new AuthoredLevelNetworkTest.Peer(host.port(), false, "Hunter");
             var ghost = new AuthoredLevelNetworkTest.Peer(host.port(), true, "Spirit")) {
            for (var peer : List.of(owner, guest, hunter, ghost)) {
                peer.awaitExact(site);
                var replica = HouseMap.forNetworkRevision(SITE, map.level().revision());
                HollowCreekRuntimeTest.check(replica.nativeSite() && replica.valid(), "No native peer geometry");
                peer.client.ready();
            }
            owner.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            owner.client.start();
            owner.await("INFO|mission started");
            owner.client.view3D();
            owner.awaitExact("ACCEPT|VIEW3D");
            var start = body(owner.await("BODY|0|"));
            HollowCreekRuntimeTest.check(map.roomAt(start.x(), start.y(), start.z()) == 14, "Missing native van spawn");
            guest.client.view3D();
            guest.awaitExact("ACCEPT|VIEW3D");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.awaitExact(site);
            ghost.await("GPOSE|");

            var outside = start;
            for (int i = 0; i < 9; i++) {
                step(owner, -100);
                outside = body(owner.await("BODY|0|"));
                HollowCreekRuntimeTest.check(map.traversable(outside.x(), outside.y(), outside.z()),
                    "Replica cannot hold the host's supported arrival pose " + outside);
            }
            HollowCreekRuntimeTest.check(map.roomAt(outside.x(), outside.y(), outside.z()) == 13,
                "Ordinary movement missed the physical van approach " + outside);
            boolean witnessed = false;
            for (int i = 0; i < 50 && !witnessed; i++) witnessed = body(guest.await("BODY|0|")).equals(outside);
            HollowCreekRuntimeTest.check(witnessed, "Guest never received the supported arrival");

            var returned = outside;
            for (int i = 0; i < 9; i++) {
                step(owner, 100);
                returned = body(owner.await("BODY|0|"));
            }
            HollowCreekRuntimeTest.check(map.roomAt(returned.x(), returned.y(), returned.z()) == 14,
                "Ordinary return could not regain the real van deck " + returned);
            System.out.println("PASS Hollow Creek four TCP peers, supported van exit and return " + start + " -> " + outside + " -> " + returned);
            System.out.println(site);
            if (args.length > 0) {
                var output = Path.of(args[0]);
                Files.createDirectories(output);
                CollegeContainmentTest.frame(map, output, "hollow-creek-review-van-arrival", outside, 32, 67, -.10);
            }
        }
    }
}
