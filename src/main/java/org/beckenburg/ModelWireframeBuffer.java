package org.beckenburg;

import java.awt.Graphics2D;
import java.util.List;
import org.beckenburg.editor.EditorMeshPicking;
import org.beckenburg.editor.EditorMeshPipeline.Triangle;
import org.beckenburg.editor.EditorMeshPipeline.Vertex;

/** Display-only projections of the native model triangles sampled in one viewport. */
final class ModelWireframeBuffer {
    interface View {
        int x(double projectedX);
        int y(double projectedY);
    }

    private static final class Polygon {
        Triangle source;
        final double[] projectedX = new double[3], projectedY = new double[3];
        final int[] x = new int[3], y = new int[3];
    }

    private Polygon[] polygons = new Polygon[0];
    private List<Triangle> source;
    private EditorMeshPicking.Projector camera;
    private View view;

    void clear() {
        if (source == null) return;
        polygons = new Polygon[0];
        source = null;
        camera = null;
        view = null;
    }

    void draw(Graphics2D graphics, List<Triangle> triangles, EditorMeshPicking.Projector nextCamera, View nextView) {
        int stride = Math.max(1, triangles.size() / 12000);
        int count = (triangles.size() + stride - 1) / stride;
        if (polygons.length != count) {
            polygons = new Polygon[count];
            for (int i = 0; i < count; i++) polygons[i] = new Polygon();
        }
        boolean newCamera = camera != nextCamera;
        boolean newView = !nextView.equals(view);
        boolean newSource = source != triangles;
        for (int i = 0; i < count; i++) {
            var polygon = polygons[i];
            if (newSource || newCamera || newView) {
                var triangle = triangles.get(i * stride);
                for (int corner = 0; corner < 3; corner++) {
                    Vertex vertex = corner(triangle, corner);
                    boolean project = newCamera || polygon.source == null || !samePosition(vertex, corner(polygon.source, corner));
                    if (project) {
                        var point = nextCamera.project(vertex.x(), vertex.y(), vertex.z());
                        polygon.projectedX[corner] = point.x();
                        polygon.projectedY[corner] = point.y();
                    }
                    if (project || newView) {
                        polygon.x[corner] = nextView.x(polygon.projectedX[corner]);
                        polygon.y[corner] = nextView.y(polygon.projectedY[corner]);
                    }
                }
                polygon.source = triangle;
            }
            graphics.drawPolygon(polygon.x, polygon.y, 3);
        }
        source = triangles;
        camera = nextCamera;
        view = nextView;
    }

    private static Vertex corner(Triangle triangle, int index) {
        return index == 0 ? triangle.a() : index == 1 ? triangle.b() : triangle.c();
    }

    private static boolean samePosition(Vertex a, Vertex b) {
        return a.x() == b.x() && a.y() == b.y() && a.z() == b.z();
    }
}
