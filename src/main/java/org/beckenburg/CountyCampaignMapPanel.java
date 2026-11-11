package org.beckenburg;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Layered, navigable county map: the approved artwork contains no baked names or mission markers. */
public final class CountyCampaignMapPanel extends JPanel {
    private static final String IMAGE = "/org/beckenburg/campaign/beckenburg-county.jpg";
    private static final String LOCATIONS = "/org/beckenburg/campaign/locations.tsv";
    private static final Color PAPER = new Color(230, 219, 193);
    private static final Color GOLD = new Color(255, 213, 125);
    private record Marker(String id, double x, double y) { }

    private final BufferedImage background;
    private final List<MissionSites.Entry> chapters = MissionSites.campaign();
    private final List<Marker> markers;
    private final IntPredicate unlocked;
    private final Map<String, Integer> chapterIndex = new HashMap<>();
    private final JLabel title = new JLabel();
    private final JTextArea details = new JTextArea();
    private final MapCanvas canvas = new MapCanvas();
    private Consumer<MissionSites.Entry> selectionListener = entry -> { };
    private int selected = 0;

    public CountyCampaignMapPanel(IntPredicate chapterUnlocked) {
        super(new BorderLayout(8, 8));
        unlocked = java.util.Objects.requireNonNull(chapterUnlocked);
        try (InputStream in = CountyCampaignMapPanel.class.getResourceAsStream(IMAGE)) {
            if (in == null) throw new IOException("Missing packaged county-map image: " + IMAGE);
            background = ImageIO.read(in);
            if (background == null) throw new IOException("Cannot decode county-map artwork");
        } catch (IOException error) {
            throw new IllegalStateException("County map is an essential campaign asset", error);
        }
        for (int i = 0; i < chapters.size(); i++) chapterIndex.put(chapters.get(i).id(), i);
        markers = loadMarkers();
        setName("campaign.county.map");
        setBackground(new Color(22, 26, 25));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        canvas.setName("campaign.county.canvas");
        canvas.setPreferredSize(new Dimension(740, 370));
        canvas.setToolTipText(" ");
        add(canvas, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8, 6));
        bottom.setOpaque(false);
        title.setForeground(GOLD);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 20));
        details.setEditable(false);
        details.setFocusable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setRows(3);
        details.setBackground(new Color(28, 32, 29));
        details.setForeground(PAPER);
        details.setMargin(new Insets(7, 10, 7, 10));
        details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        bottom.add(title, BorderLayout.NORTH);
        bottom.add(new JScrollPane(details), BorderLayout.CENTER);
        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        zoom.setOpaque(false);
        JButton minus = button("−", "campaign.county.zoom.out");
        JButton plus = button("+", "campaign.county.zoom.in");
        JButton reset = button("FULL MAP", "campaign.county.zoom.reset");
        JButton focus = button("FOCUS SITE", "campaign.county.zoom.focus");
        minus.addActionListener(e -> canvas.zoomAt(1 / 1.25, canvas.getWidth() / 2.0, canvas.getHeight() / 2.0));
        plus.addActionListener(e -> canvas.zoomAt(1.25, canvas.getWidth() / 2.0, canvas.getHeight() / 2.0));
        reset.addActionListener(e -> canvas.reset());
        focus.addActionListener(e -> canvas.focus(chapters.get(selected).id()));
        for (JButton button : new JButton[]{minus, plus, reset, focus}) zoom.add(button);
        bottom.add(zoom, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        refreshDetails();
    }

    private static JButton button(String text, String name) {
        JButton button = new JButton(text);
        button.setName(name);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.setFocusPainted(true);
        return button;
    }

    private List<Marker> loadMarkers() {
        List<Marker> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        try (InputStream in = CountyCampaignMapPanel.class.getResourceAsStream(LOCATIONS)) {
            if (in == null) throw new IOException("Missing packaged node manifest: " + LOCATIONS);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                    String[] cols = line.split("\\t", -1);
                    if (cols.length != 3) throw new IOException("Bad county node entry: " + line);
                    String id = cols[0];
                    if (!id.equals("CASTLE") && !chapterIndex.containsKey(id))
                        throw new IOException("County node is not a campaign site: " + id);
                    double x = Double.parseDouble(cols[1]);
                    double y = Double.parseDouble(cols[2]);
                    if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 1 || y < 0 || y > 1)
                        throw new IOException("Invalid normalized node coordinates: " + id);
                    if (!ids.add(id)) throw new IOException("Duplicate node: " + id);
                    result.add(new Marker(id, x, y));
                }
            }
        } catch (IOException | NumberFormatException problem) {
            throw new IllegalStateException("Invalid county map node registry", problem);
        }
        if (result.size() != chapters.size() + 1 || !ids.contains("CASTLE") || !ids.containsAll(chapterIndex.keySet()))
            throw new IllegalStateException("County map must contain the castle and every existing campaign site");
        return List.copyOf(result);
    }

    public int chapterNodeCount() { return chapters.size(); }
    public double zoomFactor() { return canvas.zoom; }
    public MissionSites.Entry selected() { return chapters.get(selected); }
    public void onSelection(Consumer<MissionSites.Entry> listener) {
        selectionListener = java.util.Objects.requireNonNull(listener);
    }
    /** Synchronizes map, chapter list and dossier without changing save-game chapter order. */
    public void select(String siteId) {
        Integer index = chapterIndex.get(siteId);
        if (index == null) throw new IllegalArgumentException("No selectable chapter node: " + siteId);
        if (selected == index) return;
        selected = index;
        refreshDetails();
        canvas.repaint();
        selectionListener.accept(chapters.get(index));
    }
    private void refreshDetails() {
        MissionSites.Entry site = selected();
        title.setText(site.title());
        String gate = !unlocked.test(selected) ? "LOCKED — finish earlier chapters" :
            !site.playable() ? "IN DEVELOPMENT — cannot deploy" : "CHAPTER AVAILABLE";
        details.setText(site.region() + "  •  " + gate + "\n" + site.description());
        details.setCaretPosition(0);
    }

    private final class MapCanvas extends JComponent {
        private double zoom = 1, panX, panY;
        private Point pressed;
        private double pressPanX, pressPanY;
        private boolean dragged;
        MapCanvas() {
            setOpaque(true);
            setBackground(new Color(16, 23, 25));
            setFocusable(true);
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    pressed = e.getPoint();
                    pressPanX = panX; pressPanY = panY; dragged = false;
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (pressed == null) return;
                    int dx = e.getX() - pressed.x, dy = e.getY() - pressed.y;
                    if (dx * dx + dy * dy > 25) dragged = true;
                    if (dragged) { panX = pressPanX + dx; panY = pressPanY + dy; repaint(); }
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (pressed == null) return;
                    if (!dragged) {
                        Marker hit = hit(e.getX(), e.getY());
                        if (hit != null && !hit.id.equals("CASTLE")) {
                            select(hit.id());
                            if (e.getClickCount() >= 2) focus(hit.id());
                        }
                    }
                    pressed = null;
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    zoomAt(Math.pow(1.18, -e.getPreciseWheelRotation()), e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
            bind("PLUS", "zoom-in", () -> zoomAt(1.25, getWidth() / 2.0, getHeight() / 2.0));
            bind("EQUALS", "zoom-equals", () -> zoomAt(1.25, getWidth() / 2.0, getHeight() / 2.0));
            bind("MINUS", "zoom-out", () -> zoomAt(0.8, getWidth() / 2.0, getHeight() / 2.0));
            bind("HOME", "full-map", this::reset);
            bind("LEFT", "pan-left", () -> {panX += 55; repaint();});
            bind("RIGHT", "pan-right", () -> {panX -= 55; repaint();});
            bind("UP", "pan-up", () -> {panY += 55; repaint();});
            bind("DOWN", "pan-down", () -> {panY -= 55; repaint();});
        }
        private void bind(String key, String action, Runnable command) {
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), action);
            getActionMap().put(action, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent event) {command.run();}
            });
        }
        private double imageScale() {
            if (getWidth() <= 0 || getHeight() <= 0) return 1;
            return Math.min(getWidth() / (double) background.getWidth(), getHeight() / (double) background.getHeight()) * zoom;
        }
        private Point position(Marker marker) {
            double scale = imageScale();
            return new Point((int)Math.round(getWidth() / 2.0 + panX + (marker.x - .5) * background.getWidth() * scale),
                (int)Math.round(getHeight() / 2.0 + panY + (marker.y - .5) * background.getHeight() * scale));
        }
        void zoomAt(double factor, double mouseX, double mouseY) {
            double old = imageScale();
            double imageX = (mouseX - getWidth() / 2.0 - panX) / old;
            double imageY = (mouseY - getHeight() / 2.0 - panY) / old;
            double next = Math.max(1, Math.min(4.5, zoom * factor));
            if (next == zoom) return;
            zoom = next;
            double scale = imageScale();
            panX = mouseX - getWidth() / 2.0 - imageX * scale;
            panY = mouseY - getHeight() / 2.0 - imageY * scale;
            repaint();
        }
        void reset() {zoom = 1; panX = panY = 0; repaint();}
        void focus(String id) {
            for (Marker marker : markers) if (marker.id.equals(id)) {
                zoom = 2.4;
                panX = (.5 - marker.x) * background.getWidth() * imageScale();
                panY = (.5 - marker.y) * background.getHeight() * imageScale();
                repaint();
                break;
            }
        }
        private Marker hit(int x, int y) {
            Marker nearest = null;
            int distanceSquared = 19 * 19;
            for (Marker marker : markers) {
                Point where = position(marker);
                int dx = where.x - x, dy = where.y - y, d = dx * dx + dy * dy;
                if (d < distanceSquared) {nearest = marker; distanceSquared = d;}
            }
            return nearest;
        }
        @Override public String getToolTipText(MouseEvent event) {
            Marker marker = hit(event.getX(), event.getY());
            if (marker == null) return null;
            if (marker.id.equals("CASTLE")) return "Castle Beckenburg — headquarters";
            MissionSites.Entry site = MissionSites.byId(marker.id);
            int index = chapterIndex.get(marker.id);
            return site.title() + (!unlocked.test(index) ? " (locked)" : !site.playable() ? " (in development)" : " (available)");
        }
        @Override protected void paintComponent(Graphics base) {
            super.paintComponent(base);
            Graphics2D g = (Graphics2D) base.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double s = imageScale();
                int width = (int)Math.round(background.getWidth() * s);
                int height = (int)Math.round(background.getHeight() * s);
                int x = (int)Math.round((getWidth() - width) / 2.0 + panX);
                int y = (int)Math.round((getHeight() - height) / 2.0 + panY);
                g.drawImage(background, x, y, width, height, null);
                for (Marker marker : markers) {
                    Point p = position(marker);
                    if (p.x < -24 || p.y < -24 || p.x > getWidth() + 24 || p.y > getHeight() + 24) continue;
                    boolean castle = marker.id.equals("CASTLE");
                    int index = castle ? -1 : chapterIndex.get(marker.id);
                    boolean active = !castle && index == selected;
                    boolean accessible = !castle && unlocked.test(index);
                    boolean playable = accessible && chapters.get(index).playable();
                    Color fill = castle ? new Color(190, 115, 56) : playable ? new Color(108, 181, 138) :
                        accessible ? new Color(160, 146, 112) : new Color(89, 94, 97);
                    int r = active ? 10 : castle ? 9 : 7;
                    g.setColor(new Color(18, 22, 21, 210));
                    g.fill(new Ellipse2D.Double(p.x - r - 4, p.y - r - 4, 2 * r + 8, 2 * r + 8));
                    g.setColor(active ? GOLD : PAPER);
                    g.setStroke(new BasicStroke(active ? 2.8f : 1.7f));
                    g.draw(new Ellipse2D.Double(p.x - r - 2, p.y - r - 2, 2 * r + 4, 2 * r + 4));
                    g.setColor(fill);
                    g.fill(new Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
                    if (active) {
                        g.setColor(GOLD);
                        g.draw(new Ellipse2D.Double(p.x - 17, p.y - 17, 34, 34));
                    }
                    if (castle) {
                        g.setColor(GOLD);
                        g.setFont(new Font(Font.SERIF, Font.BOLD, 16));
                        g.drawString("✦", p.x - 6, p.y + 6);
                    }
                }
            } finally {g.dispose();}
        }
    }
}
