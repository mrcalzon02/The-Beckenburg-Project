package org.beckenburg;

import java.nio.file.Files;

/** On-demand PSX authoring checks must inspect live collision, never a fake preview. */
public final class CastleEditorAuditTest {
    private static void check(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }

    public static void main(String[] args) throws Exception {
        var directory = Files.createTempDirectory("beckenburg-editor-audit-");
        var scene = new CastleLevelEdits(directory.resolve("castle.properties"));
        var saved = scene.snapshot();
        try {
            scene.put(new CastleLevelEdits.Surface("auditWall",
                CastleLevelEdits.Kind.WALL, 24, 25, 26, 25, 0, 3.4, MaterialAtlas.STONE));
            check(scene.wallBlocks(25, 1.2, 25, .02), "Fixture must touch actual game collision");
            var sample = scene.props().get(0);
            scene.put(new CastleLevelEdits.Prop("auditProp", sample.mesh(),
                25, 0, 25, 1, 0, .3, .3, false));
            var light = scene.lamps().get(0);
            scene.put(new CastleLevelEdits.Lamp("auditLamp", light.on(), light.off(),
                25, 1.8, 25, 1, 0, light.mount(), 7, 2));
            var a = CastleEditorAudit.inspect(scene);
            check(a.stream().anyMatch(f -> f.code().equals("PROP_WALL_INTERSECTION")
                  && f.id().equals("auditProp")), "Wall-penetrating prop flagged");
            check(a.stream().anyMatch(f -> f.code().equals("LIGHT_WALL_INTERSECTION")
                  && f.id().equals("auditLamp")), "Embedded lamp flagged");
            check(a.stream().anyMatch(f -> f.code().equals("PROP_WALL_INTERSECTION")
                  && f.kind().equals("PROP")), "Jump-to-object target retained");

            scene.removeSurface("auditWall");
            var noWall = CastleEditorAudit.inspect(scene);
            check(noWall.stream().noneMatch(f ->
                  (f.id().equals("auditProp") || f.id().equals("auditLamp")) &&
                  f.code().endsWith("WALL_INTERSECTION")), "Removing physical wall clears warning");

            scene.put(new CastleLevelEdits.Lamp("auditProp", light.on(), light.off(),
                25, 1.8, 25, 1, 0, light.mount(), 7, 2));
            check(CastleEditorAudit.inspect(scene).stream().anyMatch(f ->
                f.code().equals("DUPLICATE_SCENE_ID") && f.id().equals("auditProp")),
                "Cross-layer duplicate stable ID warned");
            scene.setSpawn(25, 25, 0);
            scene.setNavBlocked(25, 25, true);
            check(CastleEditorAudit.inspect(scene).stream().anyMatch(f ->
                f.code().equals("SPAWN_BLOCKED_NAV")), "Blocked spawn warned");
            System.out.println("PASS castle editor audit: live walls, fixtures, IDs, spawn navigation");
        } finally {
            scene.restore(saved);
            try (var walk = Files.walk(directory)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }
}
