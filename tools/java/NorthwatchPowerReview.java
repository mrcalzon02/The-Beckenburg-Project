package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Commissions the fitted Northwatch shed against the native level and LAN host. */
final class NorthwatchPowerReview {
    private static final int SHED = 5;

    private NorthwatchPowerReview() {}

    static void audit(HouseMap map, List<Vec> route, Path captures, List<String> report) throws Exception {
        var generator = map.level().socket("generator", SHED);
        var breaker = RoomLighting.breaker(map);
        var control = map.level().socket("switch", SHED);
        var lamp = map.level().socket("light", SHED);
        require(generator != null && breaker != null && breaker.room() == SHED && control != null && lamp != null,
                "Missing shed equipment bindings");
        for (Socket socket : List.of(generator, breaker, control)) {
            Vec feet = operator(socket);
            require("generator".equals(map.architecturalRoomAt(feet.x(), feet.y(), feet.z())),
                    "Operating stance must be inside the shed " + socket.id());
            Vec aisle = new Vec(feet.x(), 0, 11.03);
            for (double height : new double[]{HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
                NativeSchoolInteriorReview.walk(map, List.of(route.get(2), aisle, feet), height, socket.id());
            }
            var target = WharfPowerChecks.focus(map, feet, socket);
            String expected = socket == control ? Integer.toString(SHED) : socket.id();
            require(target.id().equals(expected), "Aiming missed " + socket.id() + " / " + target);
        }

        var lighting = new RoomLighting();
        var fixtures = new FixtureLighting(map);
        double supplied = energy(fixtures, lighting, lamp);
        lighting.setSourceRunning(false);
        double stopped = energy(fixtures, lighting, lamp);
        lighting.setSourceRunning(true);
        lighting.setBreaker(false);
        double tripped = energy(fixtures, lighting, lamp);
        lighting.setBreaker(true);
        require(!lighting.toggle(SHED), "Could not open the shed's switch");
        double switchedOff = energy(fixtures, lighting, lamp);
        require(supplied > 2 * stopped && Math.abs(stopped-tripped) < .001 && Math.abs(tripped-switchedOff) < .001,
                "Shed load did not return to ambient after each independent cutoff");
        require(lighting.toggle(SHED), "Could not close the shed's switch");
        report.add("shed_task_light_energy\t" + supplied + " supplied, " + stopped + " stopped, "
                + tripped + " tripped, " + switchedOff + " switched off");

        Path suppliedImage = captures.resolve("northwatch-shed-supplied.png");
        Path trippedImage = captures.resolve("northwatch-shed-tripped.png");
        render(map, lighting, suppliedImage);
        lighting.setBreaker(false);
        render(map, lighting, trippedImage);
        var on = ImageIO.read(suppliedImage.toFile());
        var off = ImageIO.read(trippedImage.toFile());
        require(on.getWidth() == off.getWidth() && on.getHeight() == off.getHeight(), "Capture sizes differ");
        int changed = 0;
        long gained = 0;
        for (int y = 0; y < on.getHeight(); y++) {
            for (int x = 0; x < on.getWidth(); x++) {
                int a = on.getRGB(x, y);
                int b = off.getRGB(x, y);
                if (a != b) changed++;
                for (int channel = 0; channel < 24; channel += 8) {
                    gained += ((a >> channel) & 255) - ((b >> channel) & 255);
                }
            }
        }
        double mean = gained / (3.0 * on.getWidth() * on.getHeight());
        require(changed > 25_000 && mean > .25, "Shed lacks visible blackout contrast " + changed + " / " + mean);
        report.add("shed_visible_response\t" + changed + " changed pixels; " + mean + " mean RGB increase");
    }

    static void live(HouseMap map, LanHost host, Peer leader, Peer witness, Peer crew, Vec at,
                     List<Vec> route, List<String> report) throws Exception {
        var generator = map.level().socket("generator", SHED);
        var breaker = RoomLighting.breaker(map);
        var control = map.level().socket("switch", SHED);
        var lighting = (RoomLighting) NativeSchoolInteriorReview.field(host, "lights");
        require(!lighting.supplied() && !lighting.breaker() && lighting.sourceRunning(), "Lost ghost trip on the way to the shed");
        at = walk(leader, at, route.get(1), route.get(2), operator(generator));
        WharfPowerChecks.use(map, leader, at, generator);
        witness.awaitExact("SOURCE|0");
        require(!lighting.sourceRunning() && !lighting.breaker(), "Source stop changed the breaker");
        Thread.sleep(400);
        WharfPowerChecks.use(map, leader, at, generator);
        witness.awaitExact("SOURCE|1");
        require(lighting.sourceRunning() && !lighting.supplied() && !lighting.breaker(), "Source restart reset the breaker");

        var ticket = crew.client.resumeTicket();
        require(ticket != null, "Missing Northwatch reconnect ticket");
        ((java.net.Socket) NativeSchoolInteriorReview.field(crew.client, "socket")).close();
        witness.await("PEER|LOST|2");
        var frames = new LinkedBlockingQueue<String>();
        try (var resumed = new LanClient("127.0.0.1", host.port(), ticket, frames::offer)) {
            WharfPowerChecks.await(frames, "INFO|mission resumed");
            resumed.view3D();
            WharfPowerChecks.await(frames, "ACCEPT|VIEW3D");
            WharfPowerChecks.await(frames, "SITE|USER|" + map.level().revision());
            WharfPowerChecks.await(frames, "LIGHTS|111111111111|0");
            WharfPowerChecks.await(frames, "SOURCE|1");
        }
        at = walk(leader, at, route.get(2), route.get(3), operator(breaker));
        WharfPowerChecks.use(map, leader, at, breaker);
        witness.awaitExact("LIGHTS|111111111111|1");
        require(lighting.supplied(), "The live breaker did not restore power");
        at = walk(leader, at, route.get(3), new Vec(control.x(), 0, 11.03), operator(control));
        var target = WharfPowerChecks.focus(map, at, control);
        require(target.kind() == InteractionFocus.Kind.SWITCH && target.id().equals(Integer.toString(SHED)),
                "Live aim missed the shed switch");
        double dx = control.x()-at.x();
        double dz = control.z()-at.z();
        double yaw = Math.atan2(dx, dz);
        double pitch = Math.atan2(control.y()-at.y()-HunterPosture.STANDING_EYE, Math.hypot(dx,dz));
        leader.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        leader.client.interact(target, yaw, pitch);
        leader.awaitExact("ACCEPT|INTERACT|" + target.name());
        witness.awaitExact("LIGHTS|111110111111|1");
        require(lighting.supplied() && !lighting.on(SHED), "Shed switch changed the main supply");
        report.add("northwatch_live_shed_supply\tPASS ghost trip, van and two doors in blackout, aimed source stop/start, reconnect, breaker and branch switch");
    }

    private static double energy(FixtureLighting fixtures, RoomLighting lighting, Socket lamp) {
        fixtures.frame(lighting, 0);
        return fixtures.sample(lamp.room(), lamp.x(), lamp.y()-.62, lamp.z()).energy();
    }

    private static void render(HouseMap map, RoomLighting lighting, Path image) throws Exception {
        var renderer = new SceneRenderer();
        renderer.setRenderSize(880, 580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights = lighting;
        renderer.x = 7.65;
        renderer.z = 13.25;
        renderer.eyeHeight = HunterPosture.STANDING_EYE;
        renderer.yaw = Math.PI;
        renderer.pitch = -.08;
        NativeSchoolInteriorReview.drawFrame(renderer, map, image);
    }

    private static Vec operator(Socket socket) {
        return new Vec(socket.x(), 0, socket.z()+1.18);
    }

    private static Vec walk(Peer leader, Vec at, Vec... points) throws Exception {
        for (var point : points) at = CemeteryRefugeReview.walk(leader, 0, at, point);
        return at;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
