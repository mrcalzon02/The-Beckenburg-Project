package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Render actual Castle scenery and source-derived resident sprites with the
 * normal SceneRenderer. Capture original-texture and Classic PSX versions at
 * matched cameras. Diagnostic only: no aesthetic check blocks game releases.
 */
public final class CastleResidentArtRenderAudit {
    private CastleResidentArtRenderAudit() { }

    private static final double[][] CAMERAS = {
        // The previous capture checked only the first two safe cameras:
        // both were behind/to the left of Tomas, so his front was NEVER tested.
        // Start with the front of the approved +Z-facing resident sprites.
        {0, 2.4}, {-2.4, 0}, {2.4, 0}, {0, -2.4},
        {1.8, 1.8}, {-1.8, 1.8}, {1.8, -1.8}, {-1.8, -1.8},
        {0, 2.9}, {-2.9, 0}, {2.9, 0}, {0, -2.9}
    };

    private static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0
            ? "build/castle-resident-art-render" : args[0]);
        Files.createDirectories(output);
        CastleHubMap map = new CastleHubMap();
        SceneRenderer scene = new SceneRenderer();
        scene.setRenderSize(854, 480);
        scene.lamp = true;
        int images = 0;
        int missing = 0;
        StringBuilder manifest = new StringBuilder(
            "resident,view,preset,camera_x,camera_z,render_width,render_height,file\n");

        for (CastleRelations.Resident resident : CastleRelations.Resident.values()) {
            if (resident == CastleRelations.Resident.MATTHIS_ROOK) continue;
            var npcRoom = CastleRooms.at(resident.x, 0, resident.z);
            int views = 0;
            for (double[] delta : CAMERAS) {
                double cameraX = resident.x + delta[0], cameraZ = resident.z + delta[1];
                var currentRoom = CastleRooms.at(cameraX, 0, cameraZ);
                if (!map.traversable(cameraX, cameraZ) || currentRoom == null
                    || npcRoom == null || !currentRoom.id().equals(npcRoom.id())) continue;
                scene.x = cameraX;
                scene.z = cameraZ;
                scene.eyeHeight = 1.62;
                scene.yaw = Math.atan2(resident.x-cameraX, resident.z-cameraZ);
                scene.pitch = -0.05;
                scene.setPsxPreset(PsxPipeline.Preset.OFF);
                scene.drawCastle(map, 0.5, true);

                // A valid camera is not enough: verify that the *game* actually
                // drew the resident and that its geometry/depth wasn't hidden by a wall.
                if (scene.castleResidentNameplate(resident) == null) {
                    System.out.println("CASTLE_ART_CAMERA_OCCLUDED " + resident.name
                        + " x=" + cameraX + " z=" + cameraZ);
                    continue;
                }

                for (PsxPipeline.Preset preset : new PsxPipeline.Preset[]{
                    PsxPipeline.Preset.OFF, PsxPipeline.Preset.CLASSIC
                }) {
                    scene.setPsxPreset(preset);
                    scene.drawCastle(map, 0.5, true);
                    String name = slug(resident.name) + "-angle-" + (views+1)
                        + "-" + preset.name().toLowerCase(Locale.ROOT) + ".png";
                    BufferedImage snapshot = scene.image();
                    if (!ImageIO.write(snapshot, "png", output.resolve(name).toFile()))
                        throw new IllegalStateException("Cannot write captured PNG " + name);
                    manifest.append(slug(resident.name)).append(',')
                        .append(views+1).append(',').append(preset.name()).append(',')
                        .append(cameraX).append(',').append(cameraZ).append(',')
                        .append(snapshot.getWidth()).append(',')
                        .append(snapshot.getHeight()).append(',')
                        .append(name).append('\n');
                    images++;
                    System.out.println("CASTLE_ART_SCREENSHOT " + name);
                }
                views++;
                if (views == 4) break;
            }
            if (views == 0) {
                missing++;
                System.out.println("CASTLE_ART_NO_VISIBLE_CAMERA " + resident.name);
            }
        }
        Files.writeString(output.resolve("captures.csv"), manifest.toString(),
            StandardCharsets.UTF_8);
        System.out.println("CASTLE_RESIDENT_REAL_SCENE_ART_DIAGNOSTIC captures="
            + images + " missing_residents=" + missing
            + " / source textures and PSX output are review diagnostics only");
        if (missing > 0) throw new IllegalStateException(
            "No usable in-game camera for " + missing + " castle resident(s)");
    }
}
