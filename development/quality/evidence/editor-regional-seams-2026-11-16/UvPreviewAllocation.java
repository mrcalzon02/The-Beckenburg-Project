package org.beckenburg.editor;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.beckenburg.editor.EditorTopologyProject.*;

public final class UvPreviewAllocation {
    private static final int COLUMNS = 300, ROWS = 200, FACE = 60300;
    private static final int WARMUP = 4, SAMPLES = 8;
    private static final ThreadMXBean ALLOCATION = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final long THREAD = Thread.currentThread().getId();

    private static long vertexId(int ordinal, boolean sparse) {
        return sparse ? (1L << 40) + 17L * ((COLUMNS + 1) * (ROWS + 1) - 1 - ordinal) : ordinal;
    }

    private static long faceId(int ordinal, boolean sparse) {
        return sparse ? (1L << 43) + 19L * (2 * COLUMNS * ROWS - 1 - ordinal) : ordinal;
    }

    private static Corner corner(int vertex, boolean sparse) {
        return new Corner(vertexId(vertex, sparse), vertex % (COLUMNS + 1), vertex / (COLUMNS + 1));
    }

    static EditorTopologyProject grid(boolean sparse) {
        var project = EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("grid", List.of(), 0),
                "0".repeat(64), Path.of("profile.btop"));
        var vertices = new ArrayList<Vertex>();
        var faces = new ArrayList<Face>();
        for (int y = 0; y <= ROWS; y++) {
            for (int x = 0; x <= COLUMNS; x++) {
                int v = vertices.size();
                vertices.add(new Vertex(vertexId(v, sparse), "grid", x, y, 0));
                if (x < COLUMNS && y < ROWS) {
                    faces.add(new Face(faceId(faces.size(), sparse), "grid", corner(v, sparse), corner(v + 1, sparse), corner(v + COLUMNS + 2, sparse)));
                    faces.add(new Face(faceId(faces.size(), sparse), "grid", corner(v, sparse), corner(v + COLUMNS + 2, sparse), corner(v + COLUMNS + 1, sparse)));
                }
            }
        }
        var seed = project.snapshot();
        project.restore(new State(seed.sceneId(), seed.sourceFingerprint(), "grid", 0, vertices, faces));
        return project;
    }

    private static long bytes() {
        return ALLOCATION.getThreadAllocatedBytes(THREAD);
    }

    public static void main(String[] args) {
        if (!ALLOCATION.isThreadAllocatedMemorySupported()) throw new AssertionError("Allocation counter unavailable");
        ALLOCATION.setThreadAllocatedMemoryEnabled(true);
        boolean sparse = args.length == 1 && args[0].equals("--sparse");
        if (args.length != 0 && !sparse) throw new IllegalArgumentException("Expected no option or --sparse");
        var project = grid(sparse);
        var history = project.createHistory(60);
        var committed = project.observe();
        if (committed.topology().uvSeams() != 0 || committed.mesh().triangles().size() != 120000)
            throw new AssertionError("Unexpected initial grid");
        var target = committed.snapshot().faces().get(FACE);
        if (target.id() != faceId(FACE, sparse) || sparse && target.a().vertex() <= Integer.MAX_VALUE)
            throw new AssertionError("Unexpected stable IDs on target face");
        double original = target.a().u();
        var samples = new long[SAMPLES][5];
        var affected = new BitSet();
        affected.set(FACE);
        for (int i = 0; i < WARMUP + SAMPLES; i++) {
            var base = committed;
            double shift = i % 2 == 0 ? .125 : -.125;
            long start = bytes();
            var edit = project.previewUvTransform(target.id(), false, UvTransform.MOVE, shift, 0, true);
            long created = bytes();
            var review = ProfilePath.prepare(edit, committed);
            long reviewed = bytes();
            history.execute(() -> project.applyPreview(edit));
            committed = ProfilePath.committed(project, committed, review);
            var mesh = committed.mesh();
            var topology = committed.topology();
            long applied = bytes();
            double expected = original + (i % 2 == 0 ? .125 : 0);
            if (!project.isCurrent(committed) || committed.snapshot() != edit.snapshot()
                    || review.bounds().get(FACE).a().x() != mesh.triangles().get(FACE).a().x()
                    || review.mesh().triangles().get(FACE).a().u() != expected
                    || mesh.triangles().get(FACE).a().u() != expected
                    || topology.uvSeams() != (i % 2 == 0 ? 3 : 0))
                throw new AssertionError("Reviewed or committed UV / seam mismatch");
            // Repeat only the topology owner's update, with the same warm base graph and immutable meshes.
            // Setup and independent equivalence checks are outside both measurement windows.
            var baseGraph = base.topology();
            var beforeMesh = base.mesh();
            var beforeVertices = base.snapshot().vertices();
            var afterVertices = committed.snapshot().vertices();
            long topologyStart = bytes();
            var separate = baseGraph.update(beforeMesh, mesh, beforeVertices, afterVertices, affected);
            long topologyBytes = bytes() - topologyStart;
            if (separate == null || !topology.edges().equals(separate.edges())
                    || separate.uvSeams() != topology.uvSeams() || separate.edges().size() != 360000
                    || !topology.faces().equals(separate.faces()) || !topology.vertices().equals(separate.vertices()))
                throw new AssertionError("Isolated update differs from native observation");
            if (i >= WARMUP) samples[i - WARMUP] = new long[]{
                created - start, reviewed - created, applied - reviewed, applied - start, topologyBytes};
        }
        // Independent full native derivation, outside all measured intervals.
        if (!committed.mesh().equals(project.mesh()))
            throw new AssertionError("Final reviewed geometry differs from full native derivation");
        var graph = committed.topology();
        var fresh = project.topology();
        if (!fresh.edges().equals(graph.edges()) || !fresh.faces().equals(graph.faces())
                || !fresh.vertices().equals(graph.vertices()) || !fresh.issues().equals(graph.issues()))
            throw new AssertionError("Final reviewed adjacency differs from full native derivation");
        System.out.println("PASS exact UV and seam checks on every edit; full mesh and topology parity after final commit");
        System.out.println("java=" + Runtime.version() + " maxHeap=" + Runtime.getRuntime().maxMemory()
                + " triangles=120000 vertices=60501 warmups=" + WARMUP + " samples=" + SAMPLES);
        if (sparse) System.out.println("sparse descending vertex IDs from 2^40, step 17; face IDs from 2^43, step 19");
        System.out.println("preview-create,review-geometry,apply-observe,total,isolated-topology (thread-allocated bytes)");
        long[] totals = new long[5];
        for (var sample : samples) {
            System.out.println(Arrays.toString(sample));
            for (int i = 0; i < totals.length; i++) totals[i] += sample[i];
        }
        for (int i = 0; i < totals.length; i++) totals[i] /= SAMPLES;
        System.out.println("mean " + Arrays.toString(totals));
    }

    record Review(Observation seed, EditorMeshPipeline.Mesh mesh, List<EditorMeshPipeline.Triangle> bounds) {}
}
