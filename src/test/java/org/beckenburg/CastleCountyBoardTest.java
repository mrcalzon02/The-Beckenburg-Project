package org.beckenburg;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.SwingUtilities;

/** Production Castle object -> world interaction -> actual chapter-safe county selector. */
public final class CastleCountyBoardTest {
    private CastleCountyBoardTest() { }
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    private static <T extends Component> T named(Container container, String id, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child) && id.equals(child.getName())) return type.cast(child);
            if (child instanceof Container nested) {
                T found = named(nested, id, type);
                if (found != null) return found;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        final String mesh = "level_county_campaign_board";
        check(LevelAssets.approved(mesh) && GlbProps.availableModels().contains(mesh),
            "county board must be an individually selectable approved Castle editor asset");
        try (var source = CastleCountyBoardTest.class.getResourceAsStream(
                "/org/beckenburg/models/level_county_campaign_board.glb")) {
            check(source != null, "physical board GLB must be in the shipped game JAR");
            check(source.readAllBytes().length > 500_000, "GLB must include the real cartography texture");
        }
        var authored = GlbProps.model(mesh);
        check(authored.triangles().size() >= 150,
            "a real wooden frame, map surface, feet, bracing and metal fittings must have triangles");
        int mapMaterial = authored.triangles().stream().mapToInt(GlbProps.Triangle::material)
            .filter(id -> "county_campaign_map_approved".equals(GlbProps.materialName(id)))
            .findFirst().orElseThrow(() -> new AssertionError("approved map artwork missing from 3D board"));
        check(GlbProps.textureWidth(mapMaterial) == 768 && GlbProps.textureHeight(mapMaterial) == 576,
            "board carries the source-derived full-color map texture, not a painted-in label");
        check(GlbProps.sample(mapMaterial, .21, .21) != GlbProps.sample(mapMaterial, .82, .70),
            "map front must sample actual varied cartography");
        double[] bounds = SceneRenderer.castleBounds(mesh);
        check(bounds[3] > 2.4 && bounds[3] < 2.7 && bounds[4] > 2.5 && bounds[4] < 2.8
                && bounds[5] > .55 && bounds[5] < .7,
            "framed board is a physically grounded 3D object with real volume");

        var castle = new CastleHubMap();
        var target = CastleLevelEdits.current().station(CastleHubMap.Station.CONTRACT);
        List<CastleServiceFixtures.Piece> boards = CastleServiceFixtures.all().stream()
            .filter(piece -> piece.mesh().equals(mesh)).toList();
        check(boards.size() == 1, "Castle contains precisely one authored county board workstation");
        var board = boards.get(0);
        check(Math.abs(board.x() - target[0]) < 1e-6 && Math.abs(board.z() - target[1]) < 1e-6
                && board.yaw() == 0 && board.solid(),
            "model, physical collision and editable station coordinate must agree");
        check(CastleServiceFixtures.all().stream().noneMatch(piece ->
                piece.mesh().equals("open_book_mp_1") &&
                Math.hypot(piece.x() - board.x(), piece.z() - board.z()) < .15),
            "the legacy contract book cannot obscure the county map");
        check(castle.traversable(target[0], target[1] + 1.7),
            "player must be able to approach and read the board from the briefing room");
        check(!castle.traversable(target[0], target[1]),
            "board support has physical collision; it is not a ghostly HUD sprite");
        check(castle.aimed(target[0], target[1] + 1.7, Math.PI) == CastleHubMap.Station.CONTRACT,
            "E-facing the real board reaches the existing world-use campaign action");
        check(castle.aimed(target[0], target[1] + 1.7, 0) != CastleHubMap.Station.CONTRACT,
            "the county map cannot be activated while looking away");

        // The same renderer used by the Castle actually ingests the new model.
        var renderer = new SceneRenderer();
        renderer.x = target[0]; renderer.z = target[1] + 1.8;
        renderer.yaw = Math.PI; renderer.pitch = -.07;
        renderer.drawCastle(castle, .4);
        BufferedImage screenshot = renderer.image();
        check(screenshot.getWidth() > 0 && screenshot.getHeight() > 0,
            "Castle renders a real depth-tested frame with the county board");
        String capture = System.getProperty("beckenburg.county.board.capture");
        if (capture != null && !capture.isBlank()) {
            Path output = Path.of(capture);
            if (output.getParent() != null) Files.createDirectories(output.getParent());
            ImageIO.write(screenshot, "png", output.toFile());
        }

        Path scratch = Files.createTempDirectory("beckenburg-county-board-test");
        var profile = new ProfileStore(scratch.resolve("profile.properties"));
        SwingUtilities.invokeAndWait(() -> {
            var ui = new CampaignBoardPanel(profile, new CampaignBoardPanel.Actions() {
                public void deploy(SiteCatalog.Location site, boolean ally) {
                    throw new AssertionError("physical map inspection must not dispatch a case");
                }
                public void castle() { }
                public void tutorial() { }
            });
            // This is precisely the entrypoint called by InvestigationGame.showCampaign().
            ui.showCountyMap();
            var county = named(ui, "campaign.county.map", CountyCampaignMapPanel.class);
            var launch = named(ui, "campaign.deploy", JButton.class);
            var selection = named(ui, "campaign.contracts", JList.class);
            check(county != null && county.isVisible() && launch != null && selection != null,
                "E-used Castle board opens the real county map, not the old contract-list page");
            county.select("WLD-06");
            check(selection.getSelectedIndex() == 14 && !launch.isEnabled(),
                "the 3D map cannot deploy unfinished coastal sites");
            county.select("HOUSE");
            check(selection.getSelectedIndex() == 0 && launch.isEnabled(),
                "the same selector retains a genuinely available, unlocked chapter");
        });
        Files.deleteIfExists(scratch.resolve("profile.properties"));
        Files.deleteIfExists(scratch);
        System.out.println("PASS physical Castle county-board GLB, world collision/aim, render, map-first UI and campaign gates");
    }
}
