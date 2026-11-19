package org.beckenburg;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.SwingUtilities;

/** The county overlay selects real campaign entries, never bypassing progress or production gates. */
public final class CountyCampaignMapPanelTest {
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component);
            if (component instanceof Container child) {
                T result = named(child, name, type);
                if (result != null) return result;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        try (var asset = CountyCampaignMapPanel.class.getResourceAsStream(
                "/org/beckenburg/campaign/beckenburg-county.jpg")) {
            check(asset != null, "accepted county map must be packaged");
            var image = ImageIO.read(asset);
            check(image != null && image.getWidth() == 1448 && image.getHeight() == 1086,
                "approved county image must decode at its expected resolution");
        }
        var temp = Files.createTempDirectory("beckenburg-county-map");
        var profile = new ProfileStore(temp.resolve("career.properties"));
        SwingUtilities.invokeAndWait(() -> {
            final int[] deployments = {0};
            var board = new CampaignBoardPanel(profile, new CampaignBoardPanel.Actions() {
                public void deploy(SiteCatalog.Location location, boolean assistant) { deployments[0]++; }
                public void castle() { }
                public void tutorial() { }
            });
            var map = named(board, "campaign.county.map", CountyCampaignMapPanel.class);
            var tab = named(board, "campaign.view.county", JButton.class);
            var contracts = named(board, "campaign.contracts", JList.class);
            var deploy = named(board, "campaign.deploy", JButton.class);
            check(map != null && tab != null && contracts != null && deploy != null,
                "county map must be reachable through real campaign lobby");
            tab.doClick();
            check(map.isVisible(), "county map card opens");
            check(map.chapterNodeCount() == MissionSites.campaign().size(),
                "one runtime node per saved chapter, preserving chapter order");
            check(map.selected().id().equals("HOUSE"), "initial chapter selection follows contract list");
            map.select("SCHOOL");
            check(contracts.getSelectedIndex() == 1 && !deploy.isEnabled(),
                "choosing locked School cannot dispatch it");
            map.select("WLD-06");
            check(contracts.getSelectedIndex() == 14 && !deploy.isEnabled(),
                "coastal planned node may be viewed but cannot dispatch");
            map.select("HOUSE");
            check(contracts.getSelectedIndex() == 0 && deploy.isEnabled(),
                "available House selection remains deployable");
            var zoom = named(board, "campaign.county.zoom.in", JButton.class);
            var reset = named(board, "campaign.county.zoom.reset", JButton.class);
            check(zoom != null && reset != null, "map zoom controls exist");
            zoom.doClick();
            check(map.zoomFactor() > 1, "zoom in changed map transform");
            reset.doClick();
            check(map.zoomFactor() == 1, "full-map action resets transform");
            check(deployments[0] == 0, "selection and zoom never dispatch missions");
        });
        Files.deleteIfExists(temp.resolve("career.properties"));
        Files.deleteIfExists(temp);
        System.out.println("PASS county map package, named nodes, selection sync, progression guards and zoom");
    }
}
