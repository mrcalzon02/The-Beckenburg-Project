package org.beckenburg.editor;

import java.util.ArrayList;
import java.util.List;
import org.beckenburg.editor.EditorTopologyProject.*;

final class ProfilePath {
    static UvPreviewAllocation.Review prepare(EditPreview edit, Observation base) {
        var mesh = edit.mesh();
        var bounds = new ArrayList<>(base.mesh().triangles());
        bounds.addAll(mesh.triangles());
        return new UvPreviewAllocation.Review(base, mesh, List.copyOf(bounds));
    }

    static Observation committed(EditorTopologyProject project, Observation base, UvPreviewAllocation.Review review) {
        return project.observe(base);
    }
}
