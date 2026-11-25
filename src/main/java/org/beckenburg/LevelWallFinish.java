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

/** Adds a measured, repeatable diffuse finish to plain native architectural walls. */
final class LevelWallFinish {
    private static final Pattern WALL = Pattern.compile(
        "(?:^|[ _-])(?:walls?|partitions?|headers?|lintels?|retaining|parapets?|facades?|ashlar|lining|cladding|envelope|bulkheads?)(?:$|[ _.\\-])");
    private static final Pattern MASONRY = Pattern.compile("masonry|(?:lime|sand)stone|ashlar|brick|crypt stone");
    private static final Pattern TIMBER = Pattern.compile("timber boarding|wood siding|wooden cladding");
    private static final Pattern CONCRETE = Pattern.compile("concrete|cement|tunnel dark lining");

    enum Finish {
        PLASTER("floor_ceiling_2_1.png", 2.4, 2.4, 2.1),
        CONCRETE("floor_ceiling_1_1.png", 2.75, 2.75, 2.0),
        MASONRY("wall_1_plain_0.png", 3.1, 2.1, 2.8),
        TIMBER("wall_1_1.png", 1.8, 1.8, 2.1);

        private final String source;
        private final double width, height, contrast;
        private double[] luminance;

        Finish(String source, double width, double height, double contrast) {
            this.source = source;
            this.width = width;
            this.height = height;
            this.contrast = contrast;
        }

        private double[] luminance() {
            if (luminance != null) return luminance;
            try (var input = LevelWallFinish.class.getResourceAsStream("/org/beckenburg/level-textures/" + source)) {
                if (input == null) throw new IOException("Missing wall finish " + source);
                var image = ImageIO.read(input);
                if (image == null) throw new IOException("Invalid wall finish " + source);
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

    private LevelWallFinish() {}

    static Finish finish(String nodeName, Material material) {
        if (material.hasTexture() || !material.alpha().equals("OPAQUE")) return null;
        String node = nodeName.toLowerCase(Locale.ROOT);
        String name = material.name().toLowerCase(Locale.ROOT);
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
        return FINISH_MATERIALS.computeIfAbsent(surface, LevelWallFinish::register);
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
        return GlbProps.surfaceMaterial("Native architectural wall " + surface, image);
    }

    private static int channel(double value) {
        return Math.min(255, (int) Math.round(value * 255));
    }

    static Triangle apply(Triangle triangle, Finish finish, Material source) {
        var a = triangle.a();
        var b = triangle.b();
        var c = triangle.c();
        double abX = b.x() - a.x(), abY = b.y() - a.y(), abZ = b.z() - a.z();
        double acX = c.x() - a.x(), acY = c.y() - a.y(), acZ = c.z() - a.z();
        double nx = abY * acZ - abZ * acY;
        double ny = abZ * acX - abX * acZ;
        double nz = abX * acY - abY * acX;
        double verticalArea = Math.hypot(nx, nz);
        if (verticalArea < 1e-8 || Math.abs(ny) > .20 * verticalArea) return triangle;
        double spanY = Math.max(a.y(), Math.max(b.y(), c.y())) - Math.min(a.y(), Math.min(b.y(), c.y()));
        if (spanY < .26 && !WALL.matcher(source.name().toLowerCase(Locale.ROOT)).find()) return triangle;
        boolean alongX = Math.abs(nz) >= Math.abs(nx);
        return new Triangle(project(a, finish, alongX), project(b, finish, alongX),
            project(c, finish, alongX), material(finish, source));
    }

    private static Vertex project(Vertex vertex, Finish finish, boolean alongX) {
        return new Vertex(vertex.x(), vertex.y(), vertex.z(),
            (alongX ? vertex.x() : vertex.z()) / finish.width, vertex.y() / finish.height,
            vertex.nx(), vertex.ny(), vertex.nz());
    }
}
