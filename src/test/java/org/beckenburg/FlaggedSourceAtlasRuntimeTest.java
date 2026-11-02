package org.beckenburg;

/**
 * Non-release-gating verification for the five original source GLBs whose
 * embedded PNG atlas rows were read with the legacy inverted V convention.
 * This is structural/source-binding evidence; it is NOT a substitute for
 * inspecting each four-angle in-engine capture.
 */
public final class FlaggedSourceAtlasRuntimeTest {
    private FlaggedSourceAtlasRuntimeTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String[] corrected = {
            "tv_mp_1",
            "blender_health_supplies_1",
            "blender_igniter_2",
            "blender_igniter_2_lit",
            "blender_sanity_medication_2"
        };

        for (String name : corrected) {
            check(GlbProps.reviewedSourceGltfUv(name),
                "Flagged original must sample glTF top-left UV: " + name);
            GlbProps.Model model = GlbProps.model(name);
            check(!model.triangles().isEmpty(), "No triangles from original GLB: " + name);
            int textured = 0;
            for (GlbProps.Triangle triangle : model.triangles()) {
                if (!GlbProps.registeredTexture(triangle.material())) continue;
                check(!GlbProps.materialName(triangle.material()).isBlank(),
                    "Missing original glTF material name: " + name);
                textured++;
            }
            check(textured > 0, "No source material bound to triangles: " + name);
            System.out.println("SOURCE_TEXTURE_BOUND " + name + " triangles=" + textured);
        }

        // A top-left image-space atlas reads near V=0 from the top row, and
        // near V=1 from the bottom; legacy sampling inverted these two.
        check(GlbProps.imageRow(0.125, 8, true) == 1, "Top source atlas row");
        check(GlbProps.imageRow(0.875, 8, true) == 7, "Bottom source atlas row");
        check(GlbProps.imageRow(0.125, 8, false) == 7, "Legacy row remains unchanged");

        // In particular, the large fireplace and unrelated legacy GLBs must
        // not change without inspecting their individual source/screen renders.
        check(!GlbProps.reviewedSourceGltfUv("level_fireplace"), "Unreviewed fireplace changed");
        check(!GlbProps.reviewedSourceGltfUv("wardrobe_mp_1"), "Unrelated atlas changed");
        check(!GlbProps.reviewedSourceGltfUv("ceiling_lamp_2_on"), "Unrelated lamp changed");

        System.out.println("PASS five targeted glTF source bindings; rendered visual review remains advisory");
    }
}
