package org.beckenburg.editor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ValidatorFixtures {
    public static void main(String[] args) throws Exception {
        Path output = Files.createDirectories(Path.of(args[0]));
        byte[] cleanSource = Files.readAllBytes(Path.of(args[1]));
        var original = EditorGlbReader.read(cleanSource);
        for (var alpha : EditorMeshMaterial.Alpha.values()) {
            var material = new EditorMeshMaterial("mode", "Mode " + alpha, EditorMeshMaterialTest.png(),
                new EditorMeshMaterial.Color(1,1,1,1), 10497,10497,alpha,.375,
                EditorMeshMaterial.Origin.TOP_LEFT, Map.of());
            var triangle = original.mesh("panel").triangles().get(0);
            var nativeMesh = new EditorMeshPipeline.Mesh("alpha", List.of(new EditorMeshPipeline.Triangle("panel",
                triangle.a(), triangle.b(), triangle.c(), material.key())), 0,List.of(0L,1L,2L),List.of(material));
            var generated = EditorTopologyProject.fromMesh(nativeMesh, "a".repeat(64), output.resolve("native.btop"));
            var imported = EditorTopologyProject.fromGlb("source.glb", cleanSource, output.resolve("source.btop"));
            imported.applyPreview(imported.previewBindMaterial(0, material));
            export(generated, output, "new-" + alpha);
            export(imported, output, "added-" + alpha);
        }

        // Use the editor regression's packed-UV source fixture without adding a production API.
        var packed = EditorGlbAuthoringTest.class.getDeclaredMethod("packedUvFixture", int.class);
        packed.setAccessible(true);
        for (int width = 0; width <= 2; width++) {
            String name = width == 0 ? "float-control" : "packed" + (8 * width);
            byte[] source = width == 0 ? EditorGlbAuthoringTest.fixture() : (byte[])packed.invoke(null, width);
            Files.write(output.resolve(name + "-source.glb"), source);
            var project = EditorTopologyProject.fromGlb(name + ".glb", source, output.resolve(name + ".btop"));
            project.applyPreview(project.previewUvTransform(0, false, EditorTopologyProject.UvTransform.MOVE, .25, 0, false));
            project.save();
            export(EditorTopologyProject.open(project.documentPath()), output, name + "-edited");
        }
    }

    private static void export(EditorTopologyProject project, Path output, String name) throws Exception {
        for (String suffix : List.of(".glb", ".gltf")) {
            project.exportGlb(project.observe(), output.resolve(name + suffix), null);
        }
    }
}
