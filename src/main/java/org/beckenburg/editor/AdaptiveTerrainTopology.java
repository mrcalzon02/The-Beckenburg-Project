package org.beckenburg.editor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sparse, persistent AUTHORING quadtree for stitched heightmap base cells.
 *
 * This is not a renderer, a sculpted-height overlay, or camera-dependent LOD.
 * Only split-node identities are stored. Leaf bounds and neighbor edge breaks
 * are integer dyadic coordinates shared across source PNG tile boundaries.
 */
public final class AdaptiveTerrainTopology {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_DEPTH = 6;
    public static final int MAX_SPLITS = 20_000;
    public static final int UNITS_PER_CELL = 1 << MAX_DEPTH;
    public static final double MIN_LEAF_METRES = 0.125;
    private static final int MAGIC = 0x41544f50; // ATOP
    private static final long MAX_FILE_BYTES = 1_000_000;
    private static final String FILE_NAME = "adaptive-topology.bin";
    private static final Comparator<Node> NODE_ORDER = Comparator
        .comparingLong(Node::cellZ).thenComparingLong(Node::cellX)
        .thenComparingInt(Node::depth).thenComparingInt(Node::localZ)
        .thenComparingInt(Node::localX);

    public record Node(long cellX, long cellZ, int depth, int localX, int localZ) {
        public Node {
            if (depth < 0 || depth > MAX_DEPTH || localX < 0 || localZ < 0
                    || localX >= (1 << depth) || localZ >= (1 << depth))
                throw new IllegalArgumentException("Invalid adaptive node address");
        }
        public Node parent() {
            if (depth == 0) throw new IllegalStateException("Root has no parent");
            return new Node(cellX, cellZ, depth - 1, localX >> 1, localZ >> 1);
        }
        public int units() { return UNITS_PER_CELL >> depth; }
        public long x0() { return Math.addExact(Math.multiplyExact(cellX, UNITS_PER_CELL),
                                                (long) localX * units()); }
        public long z0() { return Math.addExact(Math.multiplyExact(cellZ, UNITS_PER_CELL),
                                                (long) localZ * units()); }
        public long x1() { return x0() + units(); }
        public long z1() { return z0() + units(); }
    }

    /** Edge order follows X increasing for N/S, Z increasing for E/W. */
    public enum Edge { NORTH, SOUTH, EAST, WEST }
    public record Snapshot(List<Node> splits) {
        public Snapshot { splits = List.copyOf(splits); }
    }

    private final StitchedHeightmapWorld source;
    private final LinkedHashSet<Node> splits = new LinkedHashSet<>();
    private long revision = 1;

    private AdaptiveTerrainTopology(StitchedHeightmapWorld source) {
        this.source = Objects.requireNonNull(source);
    }
    public static AdaptiveTerrainTopology open(StitchedHeightmapWorld source) throws IOException {
        AdaptiveTerrainTopology topology = new AdaptiveTerrainTopology(source);
        Path file = topology.path();
        if (!Files.isRegularFile(file)) return topology;
        if (Files.size(file) > MAX_FILE_BYTES)
            throw new IOException("Oversized adaptive topology file");
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT_VERSION)
                throw new IOException("Unrecognized adaptive topology file");
            var settings = source.settings();
            if (in.readInt() != settings.tileCells()
                || Double.compare(in.readDouble(), settings.metresPerCell()) != 0
                || Double.compare(in.readDouble(), settings.originX()) != 0
                || Double.compare(in.readDouble(), settings.originZ()) != 0
                || Double.compare(in.readDouble(), settings.minElevation()) != 0
                || Double.compare(in.readDouble(), settings.maxElevation()) != 0
                || in.readLong() != settings.seed())
                throw new IOException("Adaptive topology belongs to a different heightmap world");
            int count = in.readInt();
            if (count < 0 || count > MAX_SPLITS)
                throw new IOException("Unsafe adaptive topology node count");
            List<Node> loaded = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                loaded.add(new Node(in.readLong(), in.readLong(),
                    in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte()));
            if (in.read() != -1) throw new IOException("Trailing adaptive topology data");
            topology.validate(loaded);
            topology.splits.addAll(loaded);
            return topology;
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid adaptive topology document", invalid);
        }
    }
    public Path path() { return source.directory().resolve(FILE_NAME); }
    public long revision() { return revision; }
    public int splitCount() { return splits.size(); }
    public List<Node> splitNodes() {
        return splits.stream().sorted(NODE_ORDER).toList();
    }
    public Snapshot snapshot() { return new Snapshot(splitNodes()); }

    private void sourceCell(long gx, long gz) {
        long tx = Math.floorDiv(gx, source.settings().tileCells());
        long tz = Math.floorDiv(gz, source.settings().tileCells());
        if (tx < -1_000_000 || tx > 1_000_000
                || tz < -1_000_000 || tz > 1_000_000
                || !source.hasTile((int) tx, (int) tz))
            throw new IllegalArgumentException("Source height tile missing or cell out of bounds: "
                + gx + "," + gz);
    }
    private void validate(List<Node> nodes) {
        if (nodes.size() > MAX_SPLITS)
            throw new IllegalArgumentException("Adaptive topology node budget exceeded");
        Set<Node> seen = new HashSet<>();
        for (Node n : nodes) {
            sourceCell(n.cellX(), n.cellZ());
            if (n.depth() >= MAX_DEPTH
                    || source.settings().metresPerCell() / (1 << (n.depth() + 1))
                        < MIN_LEAF_METRES)
                throw new IllegalArgumentException("Subdivision would be too deep or too small");
            if (!seen.add(n)) throw new IllegalArgumentException("Duplicate split node");
        }
        for (Node n : nodes)
            if (n.depth() > 0 && !seen.contains(n.parent()))
                throw new IllegalArgumentException("Adaptive split missing its parent");
    }
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot);
        validate(snapshot.splits());
        if (!splits.equals(new HashSet<>(snapshot.splits()))) {
            splits.clear();
            splits.addAll(snapshot.splits());
            revision++;
        }
    }
    private boolean existsAsLeaf(Node node) {
        if (splits.contains(node)) return false;
        Node current = node;
        while (current.depth() > 0) {
            current = current.parent();
            if (!splits.contains(current)) return false;
        }
        return true;
    }
    /** Explicit human-controlled split; no hidden world-wide neighbor recursion. */
    public void split(Node node) {
        Objects.requireNonNull(node);
        sourceCell(node.cellX(), node.cellZ());
        if (node.depth() >= MAX_DEPTH
            || source.settings().metresPerCell() / (1 << (node.depth() + 1))
                < MIN_LEAF_METRES)
            throw new IllegalArgumentException("Maximum refinement or minimum edge size reached");
        if (!existsAsLeaf(node))
            throw new IllegalArgumentException("Split requires an existing unsplit leaf");
        if (splits.size() >= MAX_SPLITS)
            throw new IllegalStateException("Adaptive split budget exceeded");
        splits.add(node);
        revision++;
    }
    /** Collapse a branch: children are topology metadata only at this milestone. */
    public void merge(Node node) {
        Objects.requireNonNull(node);
        if (!splits.contains(node))
            throw new IllegalArgumentException("Merge requires an existing split node");
        splits.removeIf(child -> descendantOrSelf(child, node));
        revision++;
    }
    private static boolean descendantOrSelf(Node child, Node parent) {
        if (child.cellX() != parent.cellX() || child.cellZ() != parent.cellZ()
                || child.depth() < parent.depth()) return false;
        int difference = child.depth() - parent.depth();
        return (child.localX() >> difference) == parent.localX()
            && (child.localZ() >> difference) == parent.localZ();
    }
    public List<Node> leaves(long cellX, long cellZ) {
        sourceCell(cellX, cellZ);
        List<Node> result = new ArrayList<>();
        collect(new Node(cellX, cellZ, 0, 0, 0), result);
        return List.copyOf(result);
    }
    private void collect(Node node, List<Node> result) {
        if (!splits.contains(node)) {
            result.add(node);
            return;
        }
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++)
            collect(new Node(node.cellX(), node.cellZ(), node.depth() + 1,
                2 * node.localX() + x, 2 * node.localZ() + z), result);
    }
    /**
     * Integer lattice breakpoints along a leaf edge, including its endpoints.
     * The later mesh builder MUST use these points on this edge for conformity.
     * No mesh, physics or true crack-free rendered frame is claimed here.
     */
    public List<Long> edgeBreaks(Node leaf, Edge edge) {
        Objects.requireNonNull(leaf);
        Objects.requireNonNull(edge);
        sourceCell(leaf.cellX(), leaf.cellZ());
        if (!existsAsLeaf(leaf)) throw new IllegalArgumentException("Not a leaf");
        boolean vertical = edge == Edge.EAST || edge == Edge.WEST;
        long fixed = switch (edge) {
            case EAST -> leaf.x1();
            case WEST -> leaf.x0();
            case NORTH -> leaf.z0();
            case SOUTH -> leaf.z1();
        };
        long start = vertical ? leaf.z0() : leaf.x0();
        long end = vertical ? leaf.z1() : leaf.x1();
        TreeSet<Long> points = new TreeSet<>();
        points.add(start);
        points.add(end);
        long neighborX = vertical
            ? Math.floorDiv(edge == Edge.WEST ? fixed - 1 : fixed, UNITS_PER_CELL)
            : Math.floorDiv(start, UNITS_PER_CELL);
        long neighborZ = vertical
            ? Math.floorDiv(start, UNITS_PER_CELL)
            : Math.floorDiv(edge == Edge.NORTH ? fixed - 1 : fixed, UNITS_PER_CELL);
        try {
            for (Node other : leaves(neighborX, neighborZ)) {
                long boundary = switch (edge) {
                    case EAST -> other.x0();
                    case WEST -> other.x1();
                    case NORTH -> other.z1();
                    case SOUTH -> other.z0();
                };
                if (boundary != fixed) continue;
                long a = vertical ? other.z0() : other.x0();
                long b = vertical ? other.z1() : other.x1();
                if (a < end && b > start) {
                    points.add(Math.max(start, a));
                    points.add(Math.min(end, b));
                }
            }
        } catch (IllegalArgumentException missingAdjacentSource) {
            // A sparse border next to an absent tile has no neighbor edge breaks.
            // It is NOT treated as fabricated terrain or a successful seam proof.
        }
        return List.copyOf(points);
    }
    public void save() throws IOException {
        List<Node> nodes = splitNodes();
        validate(nodes);
        Path destination = path();
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".adaptive-", ".tmp");
        try {
            var s = source.settings();
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                out.writeInt(MAGIC);
                out.writeInt(FORMAT_VERSION);
                out.writeInt(s.tileCells());
                out.writeDouble(s.metresPerCell());
                out.writeDouble(s.originX());
                out.writeDouble(s.originZ());
                out.writeDouble(s.minElevation());
                out.writeDouble(s.maxElevation());
                out.writeLong(s.seed());
                out.writeInt(nodes.size());
                for (Node n : nodes) {
                    out.writeLong(n.cellX());
                    out.writeLong(n.cellZ());
                    out.writeByte(n.depth());
                    out.writeByte(n.localX());
                    out.writeByte(n.localZ());
                }
            }
            Files.move(temporary, destination,
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
