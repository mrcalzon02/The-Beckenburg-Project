package org.beckenburg;

import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/** Run in separate JVMs with the original and candidate sampler on the classpath. */
public final class LevelMaterialSamplerProfile {
    private static final int MATERIAL_COUNT = LevelMaterials.ROOF - LevelMaterials.BASE + 1;
    private static final int SAMPLES = 1 << 20;

    private static void appendColor(MessageDigest digest, int id, double u, double v) {
        int rgb = LevelMaterials.sample(id, u, v);
        digest.update((byte) (rgb >> 16));
        digest.update((byte) (rgb >> 8));
        digest.update((byte) rgb);
    }

    private static void verify() throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        // This grid covers every native texel, including the non-square 682x517 and 675x427 PNGs.
        double[] edges = {-2, Math.nextDown(-1d), -1, Math.nextUp(-1d), -.5, -.0,
                Math.nextUp(0d), .005, .01, .125, .25, .5, Math.nextDown(1d), 1, Math.nextUp(1d), 2};
        for (int id = LevelMaterials.BASE; id <= LevelMaterials.ROOF; id++) {
            for (int y = 0; y < 768; y++) {
                for (int x = 0; x < 1024; x++) {
                    appendColor(digest, id, (x + .5) / 1024 - 1, (y + .5) / 768 + 1);
                }
            }
            for (double u : edges) {
                for (double v : edges) appendColor(digest, id, u, v);
            }
        }
        System.out.println("samples=" + MATERIAL_COUNT * (1024 * 768 + edges.length * edges.length)
                + " rgb_sha256=" + HexFormat.of().formatHex(digest.digest()));
    }

    private static long samplePass() {
        long checksum = 0;
        for (int i = 0; i < SAMPLES; i++) {
            // Short runs of a single material resemble spans in the software rasterizer.
            int id = LevelMaterials.BASE + ((i >>> 6) % MATERIAL_COUNT);
            double u = (i & 2047) * .00390625 - 2.375;
            double v = (i >>> 11) * .003125 + (i & 63) * .0007;
            checksum += LevelMaterials.sample(id, u, v);
        }
        return checksum;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("verify")) {
            verify();
            return;
        }
        if (args.length != 1 || !args[0].equals("benchmark"))
            throw new IllegalArgumentException("Expected verify or benchmark");
        var thread = ManagementFactory.getThreadMXBean();
        if (!thread.isCurrentThreadCpuTimeSupported()) throw new IllegalStateException("Thread CPU timing unavailable");
        if (!thread.isThreadCpuTimeEnabled()) thread.setThreadCpuTimeEnabled(true);
        var allocation = thread instanceof com.sun.management.ThreadMXBean bean && bean.isThreadAllocatedMemorySupported() ? bean : null;
        if (allocation != null && !allocation.isThreadAllocatedMemoryEnabled()) allocation.setThreadAllocatedMemoryEnabled(true);
        long expected = samplePass();
        System.out.println("trial,samples,elapsed_ns,cpu_ns,allocated_bytes,checksum");
        for (int trial = -8; trial < 15; trial++) {
            long bytes = allocation == null ? -1 : allocation.getCurrentThreadAllocatedBytes();
            long cpu = thread.getCurrentThreadCpuTime();
            long start = System.nanoTime();
            long actual = samplePass();
            long elapsed = System.nanoTime() - start;
            cpu = thread.getCurrentThreadCpuTime() - cpu;
            if (allocation != null) bytes = allocation.getCurrentThreadAllocatedBytes() - bytes;
            if (actual != expected) throw new AssertionError("Sampler result changed between passes");
            if (trial >= 0) System.out.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d%n", trial, SAMPLES, elapsed, cpu, bytes, actual);
        }
    }
}
