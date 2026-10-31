package org.beckenburg;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Nonblocking, actual SceneRenderer visual evidence for approved Castle resident artwork.
 * Save two render profiles; never modify original source concepts or in-game assets.
 */
public final class CastleResidentArtRenderAudit {
    private CastleResidentArtRenderAudit() { }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "build/castle-resident-art-render" : args[0]);
        Files.createDirectories(output);
        CastleHubMap map = new CastleHubMap();
        SceneRenderer scene = new SceneRenderer();
        scene.setRenderSize(854, 480);
        int images = 0;
        for (CastleRelations.Resident resident : CastleRelations.Resident.values()) {
            if (resident == CastleRelations.Resident.MATTHIS_ROOK) continue;
            var npcRoom = CastleRooms.at(resident.x, 0, resident.z);
            boolean found = false;
            for (double[] delta : new double[][] {{0, -2.4}, {-2.4, 0}, {2.4, 0}, {0, 2.4},
                    {-1.8, -1.8}, {1.8, 1.8}, {-1.8, 1.8}, {1.8, -1.8}}) {
                double cameraX = resident.x + delta[0];
                double cameraZ = resident.z + delta[1];
                var currentRoom = CastleRooms.at(cameraX, 0, cameraZ);
                if (!map.traversable(cameraX, cameraZ) || currentRoom == null
                        || npcRoom == null || !currentRoom.id().equals(npcRoom.id())) continue;
                scene.x = cameraX;
                scene.z = cameraZ;
                scene.eyeHeight = 1.62;
                scene.yaw = Math.atan2(resident.x-cameraX, resident.z-cameraZ);
                scene.pitch = -0.05;
                for (PsxPipeline.Preset preset : new PsxPipeline.Preset[] {
                        PsxPipeline.Preset.OFF, PsxPipeline.Preset.CLASSIC }) {
                    scene.setPsxPreset(preset);
                    scene.drawCastle(map, 0.5, true);
                    String basename = resident.name.toLowerCase(java.util.Locale.ROOT)
                            .replace(' ', '-') + "-" + preset.name().toLowerCase(java.util.Locale.ROOT);
                    BufferedImage snapshot = scene.image();
                    ImageIO.write(snapshot, "png", output.resolve(basename + ".png").toFile());
                    images++;
                }
                found = true;
                break;
            }
            if (!found) System.out.println("ART_DIAGNOSTIC_NO_SAFE_CAMERA " + resident.name);
        }
        System.out.println("CASTLE_RESIDENT_REAL_SCENE_ART_DIAGNOSTIC frames=" + images
                + " / nonblocking; review portraits and feet in screenshot artifacts");
    }
}
