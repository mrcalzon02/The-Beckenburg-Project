package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Java 17 deterministic, source-preserving authored adaptive topology acceptance. */
public final class AdaptiveTerrainTopologyTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    private static void delete(Path dir) throws Exception {
        try (var paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);
        }
    }
    private static void fail(Runnable task, String why) {
        boolean rejected = false;
        try { task.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, why);
    }
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("adaptive-topology-");
        try {
            var settings = new StitchedHeightmapWorld.Settings(16, 4, 10, -20,
                -250, 1800, 71L, 3);
            var source = StitchedHeightmapWorld.create(temp.resolve("world"), settings);
            source.generateRegion(-1, 0, 3, 2);
            check(source.verifySeams().isEmpty(), "Source tiles are exactly stitched");
            var tileBytes = Files.readAllBytes(source.tilePath(0, 0));
            var topo = AdaptiveTerrainTopology.open(source);
            check(topo.splitCount() == 0 && topo.leaves(15, 7).size() == 1,
                "Sparse empty topology uses source base cells");

            var root = new AdaptiveTerrainTopology.Node(15, 7, 0, 0, 0);
            var easternChild = new AdaptiveTerrainTopology.Node(15, 7, 1, 1, 0);
            var farRoot = new AdaptiveTerrainTopology.Node(16, 7, 0, 0, 0);
            var before = topo.snapshot();
            long rev = topo.revision();
            topo.split(root);
            topo.split(easternChild);
            check(topo.revision() == rev + 2 && topo.splitCount() == 2,
                "Each valid edit advances topology revision");
            check(topo.leaves(15, 7).size() == 7,
                "Root split plus single child split produces seven leaves");
            check(topo.leaves(16, 7).size() == 1,
                "Adjacent base cell remains coarse and unmodified");
            List<Long> expected = List.of(7L * 64, 7L * 64 + 16,
                7L * 64 + 32, 8L * 64);
            check(topo.edgeBreaks(farRoot, AdaptiveTerrainTopology.Edge.WEST)
                    .equals(expected),
                "Neighbor coarse cell discovers ALL fine edge breaks across base-cell boundary");
            fail(() -> topo.edgeBreaks(easternChild, AdaptiveTerrainTopology.Edge.EAST),
                "A split parent cannot be queried as a leaf");
            fail(() -> topo.split(root), "Repeated split rejected without mutation");
            check(topo.splitCount() == 2, "Failed split leaves topology unchanged");
            topo.save();
            var reopened = AdaptiveTerrainTopology.open(source);
            check(reopened.splitNodes().equals(topo.splitNodes()),
                "Save/load preserves exact refinement addresses");
            check(reopened.edgeBreaks(farRoot, AdaptiveTerrainTopology.Edge.WEST)
                    .equals(expected), "Readback rebuilds border breaks without cache");

            var savedSnapshot = reopened.snapshot();
            reopened.merge(root);
            check(reopened.splitCount() == 0 && reopened.leaves(15, 7).size() == 1,
                "Merge collapses all descendants without changing heightmap");
            reopened.restore(savedSnapshot);
            check(reopened.splitCount() == 2, "Undo snapshot restores nested splits");
            reopened.restore(before);
            check(reopened.splitCount() == 0, "Restore previous snapshot clears splits");
            reopened.restore(savedSnapshot);
            check(Arrays.equals(tileBytes, Files.readAllBytes(source.tilePath(0, 0))),
                "Topology edits never mutate 16-bit PNG source bytes");

            // Genuine removed faces; NOT alpha-only, zero-height or fake floor geometry.
            var mouth = new AdaptiveTerrainTopology.Node(15, 7, 1, 0, 1);
            reopened.punch(mouth);
            check(reopened.isCutout(mouth) && reopened.cutoutCount() == 1,
                "Selected subcell is an open surface hole");
            fail(() -> reopened.merge(root),
                "Merging mixed solid/open nodes must not silently fill the cave mouth");
            check(reopened.isCutout(mouth) && reopened.splitCount() == 2,
                "Rejected merge is non-destructive");
            reopened.split(mouth);
            check(reopened.cutoutCount() == 4 && reopened.splitCount() == 3,
                "Refining a hole preserves the removed area as four children");
            var nested = new AdaptiveTerrainTopology.Node(15, 7, 2, 0, 2);
            reopened.fill(nested);
            check(!reopened.isCutout(nested) && reopened.cutoutCount() == 3,
                "Filling one subquadrant does not fill other holes");
            reopened.save();
            var holesReopened = AdaptiveTerrainTopology.open(source);
            check(holesReopened.snapshot().equals(reopened.snapshot()),
                "Cutout and split state survives atomic save/readback");
            var holeSnapshot = holesReopened.snapshot();
            holesReopened.fill(new AdaptiveTerrainTopology.Node(15, 7, 2, 1, 2));
            holesReopened.restore(holeSnapshot);
            check(holesReopened.cutoutCount() == 3,
                "Undo snapshot restores the same deleted faces");
            holesReopened.restore(savedSnapshot);
            check(holesReopened.cutoutCount() == 0 && holesReopened.splitCount() == 2,
                "Restoring old topology snapshot removes all cutouts");
            check(Arrays.equals(tileBytes, Files.readAllBytes(source.tilePath(0, 0))),
                "Cutting terrain does not modify source PNG bytes");
            var seamLeft = new AdaptiveTerrainTopology.Node(15, 8, 0, 0, 0);
            var seamRight = new AdaptiveTerrainTopology.Node(16, 8, 0, 0, 0);
            reopened.punch(seamLeft);
            reopened.punch(seamRight);
            check(reopened.isCutout(seamLeft) && reopened.isCutout(seamRight),
                "Independent openings may span a stitched PNG tile boundary");
            reopened.fill(seamLeft);
            check(!reopened.isCutout(seamLeft) && reopened.isCutout(seamRight),
                "Filling one side never restores the other side");
            reopened.restore(savedSnapshot);
            // Older v1 documents from previous builds must still load.
            reopened.save();
            byte[] modern = Files.readAllBytes(reopened.path());
            byte[] legacy = Arrays.copyOf(modern, modern.length - 4);
            java.nio.ByteBuffer.wrap(legacy).putInt(4, 1);
            Files.write(reopened.path(), legacy);
            check(AdaptiveTerrainTopology.open(source).snapshot().equals(savedSnapshot),
                "Previous format v1 opens without fabricated holes");
            Files.write(reopened.path(), modern);

            var negativeRoot = new AdaptiveTerrainTopology.Node(-1, 3, 0, 0, 0);
            reopened.split(negativeRoot);
            check(reopened.leaves(-1, 3).size() == 4,
                "Signed global cells use correct negative tile indexing");
            fail(() -> reopened.split(new AdaptiveTerrainTopology.Node(9999, 0, 0, 0, 0)),
                "Unknown sparse tile must never invent source terrain");
            fail(() -> reopened.split(new AdaptiveTerrainTopology.Node(15, 7, 2, 3, 3)),
                "Cannot skip an unrefined parent");

            reopened.save();
            byte[] good = Files.readAllBytes(reopened.path());
            Files.write(reopened.path(), Arrays.copyOf(good, 11));
            boolean corruptedRejected = false;
            try { AdaptiveTerrainTopology.open(source); }
            catch (java.io.IOException expectedFailure) { corruptedRejected = true; }
            check(corruptedRejected, "Truncated topology file fails clearly");
            Files.write(reopened.path(), good);
            check(AdaptiveTerrainTopology.open(source).splitCount() == 3,
                "Uncorrupted document remains readable");

            // The source-world settings fingerprint must not accept unrelated maps.
            var different = StitchedHeightmapWorld.create(temp.resolve("different"),
                new StitchedHeightmapWorld.Settings(16, 4, 10, -20,
                    -250, 1800, 72L, 3));
            different.generateTile(0, 0);
            Files.copy(reopened.path(), different.directory().resolve("adaptive-topology.bin"));
            boolean alienRejected = false;
            try { AdaptiveTerrainTopology.open(different); }
            catch (java.io.IOException expectedFailure) { alienRejected = true; }
            check(alienRejected, "Different source seed invalidates topology document");
            check(source.verifySeams().isEmpty(), "Source seam authority unchanged");
            System.out.println("PASS adaptive topology: sparse recursive splits, "+
                "cross-tile edge breaks, negative indices, save/reload, undo and fail-safe validation");
        } finally {
            delete(temp);
        }
    }
}
