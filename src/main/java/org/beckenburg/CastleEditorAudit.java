package org.beckenburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * On-demand diagnostics of the exact persistent castle scene used by game collision.
 * This is advisory authoring feedback, never a game release gate.
 */
public final class CastleEditorAudit {
    private CastleEditorAudit() {}

    public record Finding(String code, String kind, String id, String message,
                          double x, double y, double z) {
        @Override public String toString() {
            return code + "  [" + kind + "/" + id + "]  " + message;
        }
    }

    public static List<Finding> inspect(CastleLevelEdits scene) {
        Objects.requireNonNull(scene);
        CastleLevelEdits.Snapshot state = scene.snapshot();
        List<Finding> result = new ArrayList<>();
        Map<String, String> stableIds = new HashMap<>();

        for (CastleLevelEdits.Surface surface : state.surfaces()) {
            duplicate(result, stableIds, surface.id(), "SURFACE",
                surface.cx(), surface.base(), surface.cz());
        }
        for (CastleLevelEdits.Prop prop : state.props()) {
            duplicate(result, stableIds, prop.id(), "PROP", prop.x(), prop.y(), prop.z());
            // Probe the occupied centre only; flush-to-wall furniture is legitimate.
            double probeY = prop.y() + 0.6 * Math.max(0.5, Math.min(prop.scale(), 1.5));
            if (scene.wallBlocks(prop.x(), probeY, prop.z(), 0.02))
                result.add(new Finding("PROP_WALL_INTERSECTION", "PROP", prop.id(),
                    "Prop centre overlaps an authored wall. Inspect mesh bounds and mounting.",
                    prop.x(), prop.y(), prop.z()));
        }
        for (CastleLevelEdits.Lamp lamp : state.lamps()) {
            duplicate(result, stableIds, lamp.id(), "LAMP", lamp.x(), lamp.y(), lamp.z());
            // A wall-mounted fixture can touch plaster; a centre inside solid masonry merits review.
            if (scene.wallBlocks(lamp.x(), lamp.y(), lamp.z(), 0.01))
                result.add(new Finding("LIGHT_WALL_INTERSECTION", "LAMP", lamp.id(),
                    "Light origin overlaps solid masonry. Verify actual wall mount and fixture mesh.",
                    lamp.x(), lamp.y(), lamp.z()));
        }
        for (CastleHubMap.Station station : CastleHubMap.Station.values()) {
            double[] point = state.stations().get(station.name());
            if (point == null) continue;
            duplicate(result, stableIds, station.name(), "STATION", point[0], 0, point[1]);
        }
        double sx = state.spawnX(), sz = state.spawnZ();
        duplicate(result, stableIds, "start", "SPAWN", sx, 0, sz);
        if (scene.wallBlocks(sx, 1.2, sz, 0.16))
            result.add(new Finding("SPAWN_WALL_INTERSECTION", "SPAWN", "start",
                "Player spawn centre is inside an authored solid wall.", sx, 0, sz));
        int tx = (int) Math.floor(sx), tz = (int) Math.floor(sz);
        if (tx >= 0 && tx < CastleHubMap.WIDTH && tz >= 0 && tz < CastleHubMap.HEIGHT &&
            scene.navBlocked(tx, tz))
            result.add(new Finding("SPAWN_BLOCKED_NAV", "SPAWN", "start",
                "Spawn sits on an editor-blocked navigation tile.", sx, 0, sz));
        return List.copyOf(result);
    }

    private static void duplicate(List<Finding> findings, Map<String, String> ids,
                                  String id, String kind, double x, double y, double z) {
        String previous = ids.putIfAbsent(id, kind);
        if (previous != null)
            findings.add(new Finding("DUPLICATE_SCENE_ID", kind, id,
                "ID also used by " + previous + ". Prefab and scene IDs must be globally stable.",
                x, y, z));
    }
}
