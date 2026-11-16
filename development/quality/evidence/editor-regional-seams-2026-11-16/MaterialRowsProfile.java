package org.beckenburg.editor;

import com.sun.management.ThreadMXBean;
import java.awt.Container;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.*;

/** Measure only the real Swing EDT call, with candidate construction outside the sample. */
public final class MaterialRowsProfile implements ListDataListener {
    private static final int TARGET = 60300;
    private static final int WARMUPS = 3;
    private static final int SAMPLES = 6;
    private final ThreadMXBean allocation = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private final String variant;
    private int adds, removes, changes, covered, selections, callbacks;

    private MaterialRowsProfile(String variant) { this.variant = variant; }
    private void cover(ListDataEvent e) { covered += e.getIndex1() - e.getIndex0() + 1; }
    @Override public void intervalAdded(ListDataEvent e) { adds++; cover(e); }
    @Override public void intervalRemoved(ListDataEvent e) { removes++; cover(e); }
    @Override public void contentsChanged(ListDataEvent e) { changes++; cover(e); }

    private void measure(int sample, String operation, Runnable action) {
        adds = removes = changes = covered = selections = callbacks = 0;
        long startBytes = allocation.getCurrentThreadAllocatedBytes();
        long startTime = System.nanoTime();
        action.run();
        long nanos = System.nanoTime() - startTime;
        long bytes = allocation.getCurrentThreadAllocatedBytes() - startBytes;
        if (callbacks != 0) throw new AssertionError("Programmatic refresh emitted a selection callback");
        if (sample >= 0) {
            System.out.printf(Locale.ROOT, "%s\t%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%.3f%n",
                variant, sample, operation, adds, removes, changes, covered, selections, bytes, nanos / 1e6);
        }
    }

    private static <T> T find(Container root, Class<T> type, String text) {
        for (var child : root.getComponents()) {
            if (type.isInstance(child) && (text == null || child instanceof JButton b && text.equals(b.getText())))
                return type.cast(child);
            if (child instanceof Container nested) {
                T found = find(nested, type, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void run() {
        if (!SwingUtilities.isEventDispatchThread()) throw new AssertionError("Expected Swing EDT");
        allocation.setThreadAllocatedMemoryEnabled(true);
        var project = UvPreviewAllocation.grid(false);
        var base = project.snapshot();
        var edit = project.previewUvTransform(TARGET, false, EditorTopologyProject.UvTransform.MOVE, .125, 0, true);
        var candidate = edit.snapshot();
        var view = new EditorMaterialWorkspace(new JPanel(), new JPanel(), new JPanel(), id -> callbacks++);
        var list = find(view, JList.class, null);
        list.getModel().addListDataListener(this);
        list.addListSelectionListener(event -> selections++);

        measure(0, "open", () -> view.observe(base, (long)TARGET, null, false));
        for (int i = 0; i < WARMUPS + SAMPLES; i++) {
            int sample = i < WARMUPS ? -1 : i - WARMUPS + 1;
            measure(sample, "select-other", () -> view.observe(base, TARGET + 1L, null, false));
            measure(sample, "select-target", () -> view.observe(base, (long)TARGET, null, false));
            measure(sample, "unchanged", () -> view.observe(base, (long)TARGET, null, false));
            measure(sample, "preview", () -> view.observe(candidate, (long)TARGET, null, "UV"));
            if (list.getSelectedValue() != candidate.faces().get(TARGET)) throw new AssertionError("Stale preview face");
            measure(sample, "cancel", () -> view.observe(base, (long)TARGET, null, false));
            if (list.getSelectedValue() != base.faces().get(TARGET)) throw new AssertionError("Stale canceled preview");
        }
        measure(0, "hide", () -> find(view, JButton.class, "Hide").doClick(0));
        if (view.visibleFaces() != 119999 || list.getSelectedValue() != null || view.visibility().includes(TARGET))
            throw new AssertionError("Hide filter lost");
        measure(0, "filtered-preview", () -> view.observe(candidate, TARGET + 1L, null, "UV"));
        if (list.getSelectedValue() != candidate.faces().get(TARGET+1) || view.visibleFaces() != 119999 || view.visibility().includes(TARGET))
            throw new AssertionError("Filtered preview lost selection or filter");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected variant label");
        SwingUtilities.invokeAndWait(() -> new MaterialRowsProfile(args[0]).run());
    }
}
