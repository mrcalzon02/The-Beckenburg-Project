package org.beckenburg;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.beckenburg.GlbProps.Triangle;
import org.beckenburg.GlbProps.Vertex;
import org.beckenburg.editor.EditorGlbReader.Material;

/** Applies shared, metre-scaled finishes to plain surfaces in native architecture. */
final class LevelSurfaceFinish {
    private static final Pattern WALL = Pattern.compile(
        "(?:^|[ _-])(?:walls?|partitions?|headers?|lintels?|retaining|parapets?|facades?|ashlar|lining|cladding|envelope|bulkheads?)(?:$|[ _.\\-])");
    private static final Pattern MASONRY = Pattern.compile("masonry|(?:lime|sand)stone|ashlar|brick|crypt stone");
    private static final Pattern TIMBER = Pattern.compile("timber boarding|timber facades?|wood siding|wooden cladding");
    private static final Pattern CONCRETE = Pattern.compile("concrete|cement|tunnel dark lining");

    private static final Pattern STAIR = Pattern.compile(
        "(?:^|[ _-])(?:treads?\\d*|steps?\\d*|risers?\\d*|landings?|flights?\\d*)(?:$|[ _.\\-])");
    private static final Pattern STAIR_FITTING = Pattern.compile("handrail|guard|retaining side|bearing|jamb|lintel|soffit|stair bench");
    private static final Pattern FLOOR = Pattern.compile("(?:^|[ _-])(?:floors?|decks?|boardwalks?|slabs?)(?:$|[ _.\\-])");
    private static final Pattern TILED = Pattern.compile("wash|kitchen|bath|ward|clinic|staff-wc|hydrotherapy");
    private static final Pattern STONE_FLOOR = Pattern.compile("crypt|cave|gallery|vault|nave|concrete|stone|turbine|cellar|tunnel");

    private enum Kind { WALL, FLOOR, CEILING, DECK, STAIR }

    enum Finish {
        PLASTER(Kind.WALL, "floor_ceiling_2_1.png", 2.4, 2.4, 2.1),
        CONCRETE(Kind.WALL, "floor_ceiling_1_1.png", 2.75, 2.75, 2.0),
        MASONRY(Kind.WALL, "wall_1_plain_0.png", 3.1, 2.1, 2.8),
        TIMBER(Kind.WALL, "wall_1_1.png", 1.8, 1.8, 2.1),
        FLOORBOARDS(Kind.FLOOR, "floor_ceiling_1_0.png", 1.9, 2.8, 1.85),
        FLOOR_TILE(Kind.FLOOR, "floor_ceiling_2_0.png", 2.4, 2.4, 1.25),
        FLAGSTONES(Kind.FLOOR, "floor_ceiling_2_0.png", 4.8, 4.8, 1.0),
        CEILING(Kind.CEILING, "floor_ceiling_2_1.png", 2.8, 2.8, .8),
        BOARDWALK(Kind.DECK, "floor_ceiling_1_0.png", 2.3, 2.8, 1.85),
        TIMBER_STAIRS(Kind.STAIR, "floor_ceiling_1_0.png", 2.3, 1.9, 1.5),
        STONE_STAIRS(Kind.STAIR, "floor_ceiling_1_1.png", 1.7, 1.7, 1.45);

        private final Kind kind;
        private final String source;
        private final double width, height, contrast;
        private double[] luminance;

        Finish(Kind kind, String source, double width, double height, double contrast) {
            this.kind = kind;
            this.source = source;
            this.width = width;
            this.height = height;
            this.contrast = contrast;
        }

        private double[] luminance() {
            if (luminance != null) return luminance;
            try (var input = LevelSurfaceFinish.class.getResourceAsStream("/org/beckenburg/level-textures/" + source)) {
                if (input == null) throw new IOException("Missing surface finish " + source);
                var image = ImageIO.read(input);
                if (image == null) throw new IOException("Invalid surface finish " + source);
                var samples = new double[128 * 128];
                double mean = 0;
                for (int y = 0; y < 128; y++) {
                    for (int x = 0; x < 128; x++) {
                        int rgb = image.getRGB(x * image.getWidth() / 128, y * image.getHeight() / 128);
                        double light = (((rgb >>> 16) & 255) * .2126 + ((rgb >>> 8) & 255) * .7152 + (rgb & 255) * .0722);
                        samples[y * 128 + x] = light;
                        mean += light / samples.length;
                    }
                }
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = Math.max(.55, Math.min(1.42, 1 + contrast * (samples[i] / mean - 1)));
                }
                luminance = samples;
                return samples;
            } catch (IOException bad) {
                throw new UncheckedIOException(bad);
            }
        }
    }

    private record Surface(Finish finish, int r, int g, int b) {}
    private static final java.util.Map<Surface, Integer> FINISH_MATERIALS = new java.util.HashMap<>();

    private LevelSurfaceFinish() {}

    record Selection(Finish horizontal, Finish wall) {}

    static Selection finish(String nodeName, Material material) {
        if (material.hasTexture() || !material.alpha().equals("OPAQUE")) return null;
        String node = nodeName.toLowerCase(Locale.ROOT);
        String name = material.name().toLowerCase(Locale.ROOT);
        Finish horizontal = horizontalFinish(node, name);
        Finish wall = wallFinish(node, name);
        return horizontal == null && wall == null ? null : new Selection(horizontal, wall);
    }

    private static Finish horizontalFinish(String node, String name) {
        boolean wall = WALL.matcher(node).find() || name.contains("timber facades");
        if (node.contains("terrain") || name.startsWith("cyan supported route")) return null;
        boolean timber = name.contains("timber") || name.contains("plank") || node.contains("timber");
        if (!wall && STAIR.matcher(node).find() && !STAIR_FITTING.matcher(node).find())
            return timber ? Finish.TIMBER_STAIRS : Finish.STONE_STAIRS;
        if (!wall && (node.contains("ceiling") || node.contains("roof"))) return Finish.CEILING;
        if (!wall && FLOOR.matcher(node).find()) {
            if (node.contains("boardwalk") || name.contains("settlement boardwalk")) return Finish.BOARDWALK;
            if (TILED.matcher(node).find()) return Finish.FLOOR_TILE;
            if (STONE_FLOOR.matcher(node).find() || STONE_FLOOR.matcher(name).find()) return Finish.FLAGSTONES;
            return Finish.FLOORBOARDS;
        }
        return null;
    }

    private static Finish wallFinish(String node, String name) {
        if (name.startsWith("neutral shell")) {
            if (node.matches(".*(?:crypt|vault|nave|ossuary|retaining).*")) return Finish.MASONRY;
            if (node.matches(".*(?:barn|stall|stores|shack|cabin|shed).*")) return Finish.TIMBER;
            if (node.matches(".*(?:shaft|tunnel|cellar|sally|cell-|turbine|spillway).*")) return Finish.CONCRETE;
            return Finish.PLASTER;
        }
        if (TIMBER.matcher(name).find()) return Finish.TIMBER;
        if (MASONRY.matcher(name).find()) return Finish.MASONRY;
        boolean concreteShell = name.contains("reinforced concrete") || name.contains("architectural concrete")
            || name.contains("cast concrete") || name.contains("tunnel dark lining");
        if (concreteShell || name.contains("retained envelope")
            || WALL.matcher(node).find() && CONCRETE.matcher(name).find()) return Finish.CONCRETE;
        return name.contains("wall finish") || name.contains("plaster") ? Finish.PLASTER : null;
    }

    static int material(Finish finish, Material source) {
        double[] tint = source.color();
        var surface = new Surface(finish, channel(tint[0]), channel(tint[1]), channel(tint[2]));
        return FINISH_MATERIALS.computeIfAbsent(surface, LevelSurfaceFinish::register);
    }

    private static int register(Surface surface) {
        var image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        var luminance = surface.finish.luminance();
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                double value = luminance[y * 128 + x];
                image.setRGB(x, y, (channel(surface.r * value / 255) << 16)
                    | (channel(surface.g * value / 255) << 8) | channel(surface.b * value / 255));
            }
        }
        String kind = surface.finish.kind.name().toLowerCase(Locale.ROOT);
        return GlbProps.surfaceMaterial("Native architectural " + kind + " " + surface, image);
    }

    private static int channel(double value) {
        return Math.min(255, (int) Math.round(value * 255));
    }

    static Triangle apply(Triangle triangle, Selection selection, Material source) {
        var primary = selection.horizontal;
        var textured = primary == null ? triangle : applyFinish(triangle, primary, source);
        return textured == triangle && selection.wall != null ? applyFinish(triangle, selection.wall, source) : textured;
    }

    private static Triangle applyFinish(Triangle triangle, Finish finish, Material source) {
        var a = triangle.a();
        var b = triangle.b();
        var c = triangle.c();
        double abX = b.x() - a.x(), abY = b.y() - a.y(), abZ = b.z() - a.z();
        double acX = c.x() - a.x(), acY = c.y() - a.y(), acZ = c.z() - a.z();
        double nx = abY * acZ - abZ * acY;
        double ny = abZ * acX - abX * acZ;
        double nz = abX * acY - abY * acX;
        double verticalArea = Math.hypot(nx, nz);
        if (verticalArea + Math.abs(ny) < 1e-8) return triangle;
        double spanY = Math.max(a.y(), Math.max(b.y(), c.y())) - Math.min(a.y(), Math.min(b.y(), c.y()));
        boolean horizontal = Math.abs(ny) > 2 * verticalArea;
        switch (finish.kind) {
            case WALL -> {
                if (Math.abs(ny) > .20 * verticalArea
                    || spanY < .26 && !WALL.matcher(source.name().toLowerCase(Locale.ROOT)).find()) return triangle;
            }
            case FLOOR -> {
                if (!horizontal) return triangle;
                if (ny < 0) finish = Finish.CEILING;
            }
            case CEILING -> {
                if (ny >= 0 || Math.abs(ny) < .45 * verticalArea) return triangle;
            }
            case DECK -> {
                if (!horizontal) return triangle;
            }
            case STAIR -> {
                if (!horizontal && (spanY < .03 || spanY > .42)) return triangle;
            }
        }
        boolean alongX = Math.abs(nz) >= Math.abs(nx);
        boolean planar = horizontal || finish.kind == Kind.CEILING;
        return new Triangle(project(a, finish, alongX, planar), project(b, finish, alongX, planar),
            project(c, finish, alongX, planar), material(finish, source));
    }

    private static Vertex project(Vertex vertex, Finish finish, boolean alongX, boolean planar) {
        return new Vertex(vertex.x(), vertex.y(), vertex.z(),
            (planar || alongX ? vertex.x() : vertex.z()) / finish.width,
            (planar ? vertex.z() : vertex.y()) / finish.height,
            vertex.nx(), vertex.ny(), vertex.nz());
    }
}
