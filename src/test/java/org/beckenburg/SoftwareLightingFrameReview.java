package org.beckenburg;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

/** Matched software House route for allocation, elapsed time, and RGB/depth parity. */
public final class SoftwareLightingFrameReview {
    private SoftwareLightingFrameReview() {}

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        var map = new HouseMap(HouseMap.Site.HOUSE);
        var portal = map.level().portals().stream()
            .filter(p -> p.door() && (p.a() == 0 || p.b() == 0)).findFirst().orElseThrow();
        var view = new SceneRenderer();
        view.setRenderSize(320, 200);
        view.roomLights = new RoomLighting();
        view.roomLights.setBreaker(true);
        view.eyeHeight = portal.y() + 1.62;
        double direction = portal.alongX() ? Math.PI : -Math.PI / 2;
        var thread = ManagementFactory.getPlatformMXBean(com.sun.management.ThreadMXBean.class);
        if (!thread.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Thread allocation counter unavailable");
        thread.setThreadAllocatedMemoryEnabled(true);
        boolean cpuTiming=thread.isCurrentThreadCpuTimeSupported();
        if(cpuTiming)thread.setThreadCpuTimeEnabled(true);
        var data = ByteBuffer.allocate(view.pixels().length * (Integer.BYTES + Long.BYTES));
        var sha = MessageDigest.getInstance("SHA-256");
        try (var csv = Files.newBufferedWriter(output.resolve("frames.csv"))) {
            csv.write("phase,frame,elapsed_ns,geometry_ns,allocated_bytes,shadow_rays,lit_samples,rgb_depth_sha256,render_cpu_ns\n");
            for (int i = -24; i < 72; i++) {
                int phase = Math.max(i, 0) / 12;
                if (i == 12 || i == 60) {
                    // Compare settled geometry and its shadow cache. Swing time is
                    // covered separately by DoorOpeningFrameProfile on its real clock.
                    map.toggleDoor(portal.id(), System.nanoTime() - HouseMap.DOOR_SWING_NANOS);
                }
                double offset = phase == 4 ? .04 : 1.4;
                view.x = portal.x() + (portal.alongX() ? 0 : offset);
                view.z = portal.z() + (portal.alongX() ? offset : 0);
                view.yaw = direction + .08 * Math.sin(i * .07);
                view.pitch = -.03;
                view.lamp = phase == 2 || phase == 3;
                view.setLampHardware(EquipmentTiers.Tier.I, phase == 3);
                view.uvLamp = phase == 5;
                double clock = (i + 24) / 30d;
                long bytes = thread.getCurrentThreadAllocatedBytes();
                long cpuStart = cpuTiming ? thread.getCurrentThreadCpuTime() : 0;
                long start = System.nanoTime();
                view.drawPractice(map, List.of(), clock);
                long elapsed = System.nanoTime() - start;
                long cpuElapsed = cpuTiming ? thread.getCurrentThreadCpuTime() - cpuStart : -1;
                bytes = thread.getCurrentThreadAllocatedBytes() - bytes;
                if (i < 0) continue;
                var d = view.frameDiagnostics();
                // Hash every measured world, including every raw depth bit, outside the timed interval.
                data.clear();
                for (int p : view.pixels()) data.putInt(p);
                int depthOffset = data.position();
                for (double z : view.depths()) data.putLong(Double.doubleToRawLongBits(z));
                String digest = HexFormat.of().formatHex(sha.digest(data.array()));
                if (i % 12 == 11) {
                    ImageIO.write(view.image(), "png", output.resolve("pose-" + phase + ".png").toFile());
                    try (var depths = new GZIPOutputStream(Files.newOutputStream(output.resolve("pose-" + phase + ".depth.gz")))) {
                        depths.write(data.array(), depthOffset, data.position() - depthOffset);
                    }
                }
                csv.write(String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%s,%d%n", phase, i, elapsed,
                    d.geometryNs(), bytes, d.lighting().shadowRays(), d.lighting().samples(), digest, cpuElapsed));
            }
        }
    }
}
