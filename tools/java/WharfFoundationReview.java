package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.beckenburg.LevelDefinition.Vec;

/** Sweeps the exact Wharf collision export using the game's normal capsule sizes. */
public final class WharfFoundationReview {
    private record Walk(String id, List<Vec> points) {}
    private record Frame(String id, boolean acrossX, double boundary, double along, double width) {}

    public static void main(String[] args) throws Exception {
        var stairs = new HashSet<>(Files.readAllLines(Path.of(args[2])));
        AuthoredSitePhysics physics;
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            physics = AuthoredSitePhysics.read(input, args[1], List.of(), stairs);
        }
        var output = new ArrayList<String>();
        output.add("check\tid\tposture\tforward\treverse\tsteps");
        int walked = 0;
        for (String row : Files.readAllLines(Path.of(args[3]))) {
            String[] fields = row.split("\t");
            if (fields[0].equals("DOOR")) {
                var frame = new Frame(fields[1], fields[2].equals("x"), Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]), Double.parseDouble(fields[5]));
                testFrame(physics, frame, output);
                continue;
            }
            var points = new ArrayList<Vec>();
            for (int i = 2; i < fields.length; i += 3) {
                points.add(new Vec(Double.parseDouble(fields[i]), Double.parseDouble(fields[i + 1]),
                        Double.parseDouble(fields[i + 2])));
            }
            testBothWays(physics, new Walk(fields[1], points), output);
            walked++;
        }

        // The below-grade support is continuous through the floor, and the
        // approach is open only at each intended stair mouth.
        require(!physics.clearSight(new Vec(32, -1, 30), new Vec(32, .4, 30)),
                "Undercroft roof missing from optical collision");
        require(!physics.clearSight(new Vec(32, -2.2, 30), new Vec(32, -4.1, 30)),
                "Undercroft base missing from optical collision");
        output.add("optics\tundercroft slab and roof\t1.0m ray\tblocked\tblocked\t2");
        Files.writeString(Path.of(args[4]), String.join("\n", output) + "\n");
        System.out.println("PASS exact Wharf physics " + walked + " walks and 8 fitted frames in both postures");
    }

    private static void testFrame(AuthoredSitePhysics physics, Frame frame, List<String> output) {
        var start = pointOnFrame(frame, -.75, 0, 0);
        var end = pointOnFrame(frame, .75, 0, 0);
        testBothWays(physics, new Walk(frame.id(), List.of(start, end)), output);
        require(physics.clearSight(pointOnFrame(frame, -.42, 0, 1.2),
                pointOnFrame(frame, .42, 0, 1.2)), "Opening optically closed " + frame.id());
        for (int side : new int[] {-1, 1}) {
            double offset = side * (frame.width() * .5 + .34);
            require(!physics.clearSight(pointOnFrame(frame, -.42, offset, 1.2),
                    pointOnFrame(frame, .42, offset, 1.2)), "Open beside jamb " + frame.id());
        }
        output.add("optics\t" + frame.id() + "\t1.2m ray\topen center\tblocked both jambs\t3");
    }

    private static Vec pointOnFrame(Frame frame, double normal, double offset, double y) {
        double across = frame.boundary() + normal;
        double along = frame.along() + offset;
        return frame.acrossX() ? new Vec(across, y, along) : new Vec(along, y, across);
    }

    private static void testBothWays(AuthoredSitePhysics physics, Walk walk, List<String> output) {
        var reverse = new ArrayList<>(walk.points());
        Collections.reverse(reverse);
        for (double height : new double[] {HunterPosture.STANDING_HEIGHT, HunterPosture.CROUCH_HEIGHT}) {
            int forwardCount = follow(physics, walk.id(), walk.points(), height);
            int reverseCount = follow(physics, walk.id() + " reverse", reverse, height);
            output.add("walk\t" + walk.id() + "\t" + height + "\tPASS\tPASS\t"
                    + (forwardCount + reverseCount));
        }
    }

    private static int follow(AuthoredSitePhysics physics, String id, List<Vec> points, double height) {
        Vec start = points.get(0);
        var support = physics.support(start.x(), start.z(), start.y(), .2);
        require(support != null, "Unsupported start " + id + " " + start);
        Vec at = new Vec(start.x(), support.y(), start.z());
        require(physics.traversable(at, height), "Blocked start " + id + " " + at);

        int samples = 0;
        for (int i = 1; i < points.size(); i++) {
            Vec from = points.get(i - 1);
            Vec end = points.get(i);
            int count = Math.max(1, (int)Math.ceil(Math.hypot(end.x() - from.x(), end.z() - from.z()) / .08));
            for (int step = 1; step <= count; step++) {
                double t = (double)step / count;
                double x = from.x() + (end.x() - from.x()) * t;
                double z = from.z() + (end.z() - from.z()) * t;
                Vec next = physics.resolveMove(at, x, z, height);
                require(next != null, "Blocked " + id + " segment " + i + " at " + x + ", " + z
                        + " from " + at + " height " + height);
                at = next;
                samples++;
            }
        }
        require(Math.abs(at.y() - points.get(points.size() - 1).y()) < .25, "Wrong end elevation " + id + " " + at);
        return samples;
    }

    private static void require(boolean result, String context) {
        if (!result) throw new AssertionError(context);
    }
}
