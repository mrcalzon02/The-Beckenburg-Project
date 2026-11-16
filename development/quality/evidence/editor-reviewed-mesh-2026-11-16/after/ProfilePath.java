package org.beckenburg.editor;

import java.util.ArrayList;
import java.util.List;
import org.beckenburg.editor.EditorTopologyProject.*;

final class ProfilePath {
    static UvPreviewAllocation.Review prepare(EditPreview edit, Observation base) {
        var candidate = edit.review(base);
        var mesh = candidate.mesh();
        var working = base.mesh();
        if (base.snapshot().vertices() == candidate.snapshot().vertices() && working.cornerVertexIds() == mesh.cornerVertexIds())
            return new UvPreviewAllocation.Review(candidate, mesh, working.triangles());
        var bounds = new ArrayList<>(working.triangles());
        bounds.addAll(mesh.triangles());
        return new UvPreviewAllocation.Review(candidate, mesh, List.copyOf(bounds));
    }

    static Observation committed(EditorTopologyProject project, Observation base, UvPreviewAllocation.Review review) {
        return project.isCurrent(review.seed()) ? review.seed() : project.observe(base);
    }
}
