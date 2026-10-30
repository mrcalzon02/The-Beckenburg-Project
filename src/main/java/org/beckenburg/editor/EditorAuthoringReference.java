package org.beckenburg.editor;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

/** Lightweight offline reference, deliberately independent of Castle and of web access. */
public final class EditorAuthoringReference {
    private static final String WIKI =
        "https://github.com/mrcalzon02/The-Beckenburg-Project/blob/main/docs/wiki/psx-editor-engineering-guide.md";
    private static final String MACHINE =
        "https://raw.githubusercontent.com/mrcalzon02/The-Beckenburg-Project/main/docs/development/psx-editor-reference.json";
    private EditorAuthoringReference() {}

    private static JTextArea page(String body) {
        JTextArea view = new JTextArea(body, 20, 66);
        view.setEditable(false);
        view.setLineWrap(true);
        view.setWrapStyleWord(true);
        view.setCaretPosition(0);
        return view;
    }
    private static void browse(String url, JTextArea status) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                throw new IllegalStateException("No desktop browser is available");
            Desktop.getDesktop().browse(URI.create(url));
            status.setText("Opened " + url);
        } catch (Exception failure) {
            status.setText("Browser unavailable. Open this address manually:\n" + url);
        }
    }
    public static void show(JFrame parent) {
        JDialog dialog = new JDialog(parent, "PSX Editor - Authoring Reference", false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Scene", new JScrollPane(page(
            "RUNTIME FIRST / EDITOR SECOND\n\n" +
            "Use exactly the live game scene, renderer, assets, collision and simulation. " +
            "Keep stable object IDs, versioned saves, real layered transforms, Undo/Redo and safe playtest restoration.\n\n" +
            "Build geometry around genuine wall/floor/roof collision; basement stairs need a real opening. " +
            "Keep the same canonical van prefab in castle and investigations. " +
            "Use authoring gizmos, surface snapping and warnings, not blind edits.\n\n" +
            "Reference: Level Editor gold standards + PSX level building.")));
        tabs.addTab("Rendering / Light", new JScrollPane(page(
            "PSX LOOK / MODERN CORRECTNESS\n\n" +
            "Nearest-neighbor textures, limited palettes, quantized light, Gouraud gradients, affine warp, " +
            "vertex jitter and dithering are stylistic profiles, not actual PS1 hardware limits. " +
            "Never warp editor widgets, text or real collision coordinates.\n\n" +
            "The unified LightingModule, FixtureLighting and PsxPipeline own actual rendering. " +
            "Keep light reach separate from diffuse-edge width; respect occluding walls, doors and power circuits. " +
            "Use the real lighting reach overlay and profile door-opening invalidation. " +
            "Weather alone owns fog banks.\n\n" +
            "Reference: PSX lighting engine + PSX level building.")));
        tabs.addTab("AI / Nav", new JScrollPane(page(
            "AUTHOR PATHS / DO NOT CHEAT\n\n" +
            "Use valid portal and stair connections, real collision, door edge invalidation, " +
            "optional cached routes and tactical waypoint tags. Diagnose unreachable rooms.\n\n" +
            "The host remains authoritative; ghost hearing uses legitimate events and door/material/weather attenuation. " +
            "Neither player nor AI ghost may enter the van or hiding pockets, acquire omniscient player locations, " +
            "teleport through walls or bypass cooldowns. Measure CPU/GC before throttling optional decisions.\n\n" +
            "Reference: PSX NPC-AI-1 + Beckenburg AI adoption review.")));
        tabs.addTab("UI / Tests", new JScrollPane(page(
            "AUTHENTIC PRESENTATION / NO ARTIFICIAL FRICTION\n\n" +
            "Compact menus, readable pixel styling and D-pad navigation are useful. Keep remapping, " +
            "mouse/keyboard/controller parity, accessible scale/contrast, readable text and instant input. " +
            "Never fake CD seek hangs, force memory-card mechanics or lock widescreen away.\n\n" +
            "Profile p50/p95/p99 frame time, draw work, postprocessing, light probes, heap/GC and AI. " +
            "Quality/aesthetic warnings remain diagnostic: only actual broken build, launch, required game play " +
            "or multiplayer may block release.\n\n" +
            "Reference: PSX ui guide and user + Level Editor gold standards.")));
        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.add(tabs, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        JPanel buttons = new JPanel();
        JTextArea status = page("Offline summary is bundled in the standalone editor kit. Full sources: project wiki / machine JSON.");
        status.setRows(2);
        JButton wiki = new JButton("Human guide");
        wiki.addActionListener(e -> browse(WIKI, status));
        JButton data = new JButton("Machine JSON");
        data.addActionListener(e -> browse(MACHINE, status));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        buttons.add(wiki); buttons.add(data); buttons.add(close);
        footer.add(buttons, BorderLayout.NORTH);
        footer.add(status, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.setSize(660, 510);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
