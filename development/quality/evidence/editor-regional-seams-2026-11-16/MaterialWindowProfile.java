package org.beckenburg.editor;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Showing-window UV workspace paint on a normalized 300 by 200 grid. Run both revisions on the same display. */
public final class MaterialWindowProfile {
    private static final int FACE = 60300;
    private final EditorMaterialWorkspace workspace = new EditorMaterialWorkspace(new JPanel(), new JPanel(), new JPanel(), id -> {});
    private final JFrame frame = new JFrame("Material Editor paint measurement");
    private final JList<?> faceList;
    private final JComponent canvas;

    private MaterialWindowProfile() {
        frame.setContentPane(workspace);
        frame.setSize(1100, 700);
        frame.setVisible(true);
        faceList = find(workspace, JList.class);
        var view = find(workspace, JComboBox.class);
        if (view == null || !"Texture / UV".equals(view.getSelectedItem()) || faceList == null)
            throw new AssertionError("Expected the native face list and UV view");
        var texturePanel = view.getParent().getParent();
        canvas = (JComponent)((BorderLayout)texturePanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    }

    private static <T> T find(Container root, Class<T> type) {
        for (var child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T found = find(nested, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void paintVisible(JComponent component) {
        Rectangle region = component.getVisibleRect();
        if (!component.isShowing() || region.isEmpty()) throw new AssertionError("Expected a showing paint region");
        component.paintImmediately(region);
        Toolkit.getDefaultToolkit().sync();
    }

    private void measure(String variant, int sample, State state, String preview) {
        long start = System.nanoTime();
        workspace.observe(state, (long)FACE, null, preview);
        frame.validate();
        long observed = System.nanoTime();
        paintVisible(workspace);
        long full = System.nanoTime();
        // Repeat only these component clips, on the same displayed state, outside the full-workspace interval.
        paintVisible(canvas);
        long uv = System.nanoTime();
        paintVisible(faceList);
        long list = System.nanoTime();
        if (workspace.visibleFaces() != 120000 || faceList.getSelectedValue() != state.faces().get(FACE))
            throw new AssertionError("Stale native selection after showing-window paint");
        if (sample > 0) {
            int visibleRows = faceList.getLastVisibleIndex() - faceList.getFirstVisibleIndex() + 1;
            System.out.printf(Locale.ROOT,"%s\t%d\t%s\t%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f%n",
                variant, sample, preview == null ? "cancel" : "preview", canvas.getWidth(), canvas.getHeight(), visibleRows,
                (observed-start)/1e6, (full-observed)/1e6, (full-start)/1e6, (uv-full)/1e6, (list-uv)/1e6);
        }
    }

    private static Corner normalize(Corner corner) {
        return new Corner(corner.vertex(), corner.u()/300, corner.v()/200, corner.normal(), corner.pinned());
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || GraphicsEnvironment.isHeadless()) throw new IllegalArgumentException("Expected variant label and a display");
        var project = UvPreviewAllocation.grid(false);
        var grid = project.snapshot();
        var normalized = new ArrayList<Face>(grid.faces().size());
        for (var f : grid.faces())
            normalized.add(f.withCorners(normalize(f.a()), normalize(f.b()), normalize(f.c())));
        project.restore(new State(grid.sceneId(),grid.sourceFingerprint(),grid.name(),0,grid.vertices(),normalized));
        var base = project.snapshot();
        var candidate = project.previewUvTransform(FACE,false,EditorTopologyProject.UvTransform.MOVE,.001,0,true).snapshot();
        SwingUtilities.invokeAndWait(() -> {
            var view = new MaterialWindowProfile();
            try {
                view.measure(args[0],0,base,null);
                for (int sample = -1; sample <= 5; sample++) {
                    view.measure(args[0], sample, candidate, "UV");
                    view.measure(args[0], sample, base, null);
                }
            } finally {
                view.frame.dispose();
            }
        });
    }
}
