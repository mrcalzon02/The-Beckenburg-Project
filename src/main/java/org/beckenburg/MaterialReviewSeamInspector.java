package org.beckenburg;

import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import org.beckenburg.editor.EditorMeshTopology;
import org.beckenburg.editor.EditorTopologyProject;

/** The selected UV edge, read from the same immutable observation as the visible material. */
final class MaterialReviewSeamInspector extends JLabel {
    private EditorTopologyProject.Observation observation;
    private EditorMeshTopology graph;
    private int faceIndex = -1, corner;
    private boolean active, loading, failed;

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    void setActive(boolean next) {
        if (active == next) return;
        active = next;
        refresh();
    }

    void selectCorner(int next) {
        if (corner == next) return;
        corner = next;
        refresh();
    }

    void inspect(EditorTopologyProject.Observation next, int faceIndex) {
        if (observation == next && this.faceIndex == faceIndex) return;
        if (observation != next) {
            observation = next;
            graph = null;
            failed = false;
        }
        this.faceIndex = faceIndex;
        refresh();
    }

    private void refresh() {
        if (!active) return;
        if (observation == null || faceIndex < 0 || corner < 0) {
            setText("Select a face to inspect its edge");
            return;
        }
        if (failed) {
            setText("Edge topology unavailable");
            return;
        }
        if (graph == null) {
            setText("Reading edge topology…");
            if (!loading) load(observation);
            return;
        }
        var faces = observation.snapshot().faces();
        var face = faces.get(faceIndex);
        var edge = graph.edges().get(faceIndex * 3 + corner);
        boolean marked = face.marked(corner);
        if (edge.twin() >= 0) {
            var twin = graph.edges().get(edge.twin());
            var neighbor = faces.get(twin.face());
            marked |= neighbor.marked(edge.twin() % 3);
            String relation = (edge.uvSeam() ? "UV split" : "UVs match") + markText(marked);
            String neighborText = "Across edge · Face " + neighbor.id();
            if (!face.material().equals(neighbor.material())) neighborText += " · Different material";
            setText("<html>" + relation + "<br>" + neighborText + "</html>");
        } else {
            String relation = edge.boundary() ? "Mesh boundary" : "Unpaired edge · Check topology";
            setText(relation + markText(marked));
        }
    }

    private static String markText(boolean marked) {
        return marked ? " · Marked" : " · Unmarked";
    }

    // Coalesce requests while a graph is built. A late result belongs only to its captured revision.
    private void load(EditorTopologyProject.Observation captured) {
        loading = true;
        new SwingWorker<EditorMeshTopology,Void>() {
            protected EditorMeshTopology doInBackground() { return captured.topology(); }
            protected void done() {
                loading = false;
                if (observation == captured) {
                    try {
                        graph = get();
                    } catch (ExecutionException unavailable) {
                        failed = true;
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        failed = true;
                    }
                }
                refresh();
            }
        }.execute();
    }
}
