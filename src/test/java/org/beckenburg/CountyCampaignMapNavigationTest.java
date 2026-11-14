package org.beckenburg;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;

/** Reproducible interaction checks for the approved layered county world map. */
public final class CountyCampaignMapNavigationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Component> T named(Container parent, String name, Class<T> type) {
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component) && name.equals(component.getName())) return type.cast(component);
            if (component instanceof Container child) {
                T found = named(child, name, type);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void click(JComponent component, Point location) {
        long now = System.currentTimeMillis();
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, now, 0,
            location.x, location.y, 1, false, MouseEvent.BUTTON1));
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, now + 1, 0,
            location.x, location.y, 1, false, MouseEvent.BUTTON1));
    }
    private static void drag(JComponent component, Point from, Point to) {
        long now = System.currentTimeMillis();
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, now, 0,
            from.x, from.y, 1, false, MouseEvent.BUTTON1));
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, now+1,
            MouseEvent.BUTTON1_DOWN_MASK, to.x, to.y, 0, false, MouseEvent.BUTTON1));
        component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, now+2, 0,
            to.x, to.y, 1, false, MouseEvent.BUTTON1));
    }
    private static void coversViewportAtZoom(CountyCampaignMapPanel map, JComponent canvas) {
        Rectangle image = map.imageScreenBounds();
        if (image.width >= canvas.getWidth())
            check(image.x <= 0 && image.x + image.width >= canvas.getWidth(), "no horizontal overscroll");
        if (image.height >= canvas.getHeight())
            check(image.y <= 0 && image.y + image.height >= canvas.getHeight(), "no vertical overscroll");
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final AtomicInteger selections = new AtomicInteger();
            CountyCampaignMapPanel map = new CountyCampaignMapPanel(index -> index == 0);
            JComponent canvas = named(map, "campaign.county.canvas", JComponent.class);
            JComboBox<?> site = named(map, "campaign.county.site.find", JComboBox.class);
            JCheckBox names = named(map, "campaign.county.names", JCheckBox.class);
            JButton reset = named(map, "campaign.county.zoom.reset", JButton.class);
            check(canvas != null && site != null && names != null && reset != null,
                "county site search, optional names, full map controls are real widgets");
            canvas.setSize(740, 370);
            map.onSelection(entry -> selections.incrementAndGet());
            check(!names.isSelected(), "names default off: approved map stays unlabeled");
            check(map.zoomFactor() == 1, "default county zoom is fitted");
            Point house = map.nodeScreenPoint("HOUSE");
            Point broad = new Point(40, 40);
            drag(canvas, house, new Point(house.x + 2000, house.y - 2000));
            check(map.nodeScreenPoint("HOUSE").equals(house), "fitted map cannot drift out of view");
            Point crossing = map.nodeScreenPoint("WLD-02");
            check(map.nodeScreenPoint("ASYLUM").distance(crossing) < 19,
                "full-map overlap scenario stays reproducible");
            click(canvas, crossing);
            check(map.zoomFactor() > 1 && map.selected().id().equals("HOUSE") && selections.get() == 0,
                "crowded dots zoom first instead of choosing the wrong chapter");
            click(canvas, map.nodeScreenPoint("WLD-02"));
            check(map.selected().id().equals("WLD-02") && selections.get() == 1,
                "disambiguated point click selects the real chapter");
            check(site.getSelectedIndex() == 10, "site finder follows canvas selection");
            @SuppressWarnings("unchecked") JComboBox<MissionSites.Entry> picker =
                (JComboBox<MissionSites.Entry>)site;
            picker.setSelectedItem(MissionSites.byId("WLD-06"));
            check(map.selected().id().equals("WLD-06") && selections.get() == 2,
                "site finder selects the authoritative campaign site");
            check(map.zoomFactor() > 1, "site finder focuses distant places");
            drag(canvas, broad, new Point(4000, -4000));
            coversViewportAtZoom(map, canvas);
            drag(canvas, broad, new Point(-4000, 4000));
            coversViewportAtZoom(map, canvas);
            names.doClick();
            check(names.isSelected(), "names enabled only as optional runtime overlay");
            BufferedImage frame = new BufferedImage(740, 370, BufferedImage.TYPE_INT_ARGB);
            canvas.paint(frame.createGraphics());
            names.doClick();
            check(!names.isSelected(), "optional labels can be removed without editing base image");
            reset.doClick();
            check(map.zoomFactor() == 1, "reset restores full map");
            check(map.nodeScreenPoint("HOUSE").equals(house), "reset restores original marker position");
            click(canvas, map.nodeScreenPoint("CASTLE"));
            check(map.selected().id().equals("WLD-06") && selections.get() == 2,
                "castle focus cannot select or deploy a field chapter");
            check(map.zoomFactor() > 1, "castle point focuses headquarters without dispatch");
        });
        System.out.println("PASS county navigation: cluster focus, site chooser, live labels, clamped pan, reset, castle safety");
    }
}
