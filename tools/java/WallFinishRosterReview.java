package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.beckenburg.GlbProps.Vertex;
import org.beckenburg.editor.EditorGlbReader;
import org.beckenburg.editor.EditorMeshPipeline;

/** Audits actual wall materials and proves the finish pass leaves source surfaces intact. */
final class WallFinishRosterReview {
    private WallFinishRosterReview() {}

    public static void main(String[] args) throws Exception {
        System.out.println("site\ttriangles\tfinished_wall_triangles\twall_materials\texisting_textured_triangles");
        for (int i = 0; i < args.length; i += 2) {
            audit(args[i], Path.of(args[i + 1]));
        }
    }

    private static void audit(String name, Path glb) throws Exception {
        byte[] bytes = Files.readAllBytes(glb);
        var original = EditorGlbReader.readLevel(bytes);
        var textured = GlbProps.decodeLevel(bytes).triangles();
        check(original.faces().size() == textured.size(), name + " triangle count changed");
        var finishes = new HashSet<Integer>();
        int applied = 0, retained = 0;
        for (int i = 0; i < textured.size(); i++) {
            var source = original.faces().get(i);
            var primitive = source.primitive();
            var material = original.materials().get(primitive.material());
            var triangle = textured.get(i);
            var before = primitive.world();
            int offset = source.triangle() * 3;
            var corners = List.of(triangle.a(), triangle.b(), triangle.c());
            boolean finished = GlbProps.materialName(triangle.material()).startsWith("Native architectural wall ");
            for (int j = 0; j < 3; j++) {
                var prior = before.get(primitive.indices().get(offset + j));
                var next = corners.get(j);
                check(samePosition(prior, next), name + " surface moved");
                if (!finished) check(prior.u() == next.u() && prior.v() == next.v(), name + " source UV changed");
            }
            if (!material.hasTexture() && material.name().startsWith("Neutral shell") && wallSized(triangle))
                check(finished, name + " plain structural wall remains at " + original.nodeName(primitive.node()));
            if (finished) {
                check(!material.hasTexture(), name + " replaced an authored texture");
                check(GlbProps.textureWidth(triangle.material()) == 128, name + " wall image absent");
                finishes.add(triangle.material());
                applied++;
            } else {
                check(GlbProps.materialName(triangle.material()).equals(material.name()), name + " source material changed");
                if (material.hasTexture()) retained++;
            }
        }
        check(applied > 0, name + " has no finished walls");
        for (int id : finishes) {
            var samples = new HashSet<Integer>();
            for (int i = 0; i < 16; i++) samples.add(GlbProps.sample(id, i / 16.0, i / 19.0));
            check(samples.size() > 3, name + " wall texture is flat");
        }
        System.out.printf("%s\t%d\t%d\t%d\t%d%n", name, textured.size(), applied, finishes.size(), retained);
    }

    private static boolean wallSized(GlbProps.Triangle face) {
        var a = face.a();
        var b = face.b();
        var c = face.c();
        double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
        double ex = c.x() - a.x(), ey = c.y() - a.y(), ez = c.z() - a.z();
        return Math.max(a.y(), Math.max(b.y(), c.y())) - Math.min(a.y(), Math.min(b.y(), c.y())) >= .5
            && Math.abs(dz * ex - dx * ez) <= 1e-6
            && Math.hypot(dy * ez - dz * ey, dx * ey - dy * ex) > 1e-8;
    }

    private static boolean samePosition(EditorMeshPipeline.Vertex before, Vertex after) {
        return before.x() == after.x() && before.y() == after.y() && before.z() == after.z();
    }

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
}
