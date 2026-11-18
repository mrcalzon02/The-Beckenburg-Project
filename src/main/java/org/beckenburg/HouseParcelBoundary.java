package org.beckenburg;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.beckenburg.LevelDefinition.Collider;
import org.beckenburg.LevelDefinition.Socket;

/** One placed library mesh owns the rebuilt House parcel's rendering and physical boundary. */
final class HouseParcelBoundary {
    static final String MODEL = "level_enclosure_garden_worn";
    private static final String BINDING = "parcel-boundary-mesh";
    private static final Map<List<Socket>, Geometry> CACHE = new ConcurrentHashMap<>();

    record Geometry(GlbProps.Model model, AuthoredSitePhysics physics) { }

    private HouseParcelBoundary() { }

    static boolean primaryPanel(Socket socket) {
        return socket.type().equals("enclosure") && socket.id().startsWith("parcel-")
            && socket.style().startsWith("garden:");
    }

    static double panelLength(Socket socket) {
        double length = Double.parseDouble(socket.style().substring("garden:".length()));
        if (!Double.isFinite(length) || length <= 0 || length > 2)
            throw new IllegalArgumentException("Invalid House fence panel length for " + socket.id());
        return length;
    }

    static void bind(List<Socket> sockets, List<Collider> colliders) {
        var panels = sockets.stream().filter(HouseParcelBoundary::primaryPanel).toList();
        if (panels.isEmpty()) throw new IllegalArgumentException("House parcel has no physical panels");
        for (Socket panel : panels) {
            var formerBox = new Collider(panel.x(), panel.y(), panel.z(),
                panelLength(panel) / 2 + .012, .15, 1.96, -panel.yaw());
            if (!colliders.remove(formerBox))
                throw new IllegalArgumentException("Missing source fence collider for " + panel.id());
        }
        sockets.add(new Socket("briar-parcel-geometry", BINDING, 13, 0, 0, 0, 0, Canonical.STYLE));
    }

    static Geometry geometry(LevelDefinition level) {
        var binding = level.sockets().stream().filter(socket -> socket.type().equals(BINDING)).toList();
        if (binding.isEmpty()) return null;
        if (binding.size() != 1 || !level.id().equals("briar-rebuild") || !binding.get(0).style().equals(Canonical.STYLE))
            throw new IllegalArgumentException("Invalid House parcel mesh binding");
        var panels = level.sockets().stream().filter(HouseParcelBoundary::primaryPanel).toList();
        return CACHE.computeIfAbsent(panels, HouseParcelBoundary::place);
    }

    private static Geometry place(List<Socket> panels) {
        var source = GlbProps.model(MODEL);
        var faces = new ArrayList<GlbProps.Triangle>(panels.size() * source.triangles().size());
        for (Socket panel : panels) {
            double angle = -panel.yaw();
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double halfLength = panelLength(panel) / 2;
            for (var face : source.triangles()) {
                faces.add(new GlbProps.Triangle(place(face.a(), panel, halfLength, cos, sin),
                    place(face.b(), panel, halfLength, cos, sin),
                    place(face.c(), panel, halfLength, cos, sin), face.material()));
            }
        }
        var model = new GlbProps.Model(List.copyOf(faces));
        try {
            return new Geometry(model, AuthoredSitePhysics.meshObstacles("Briar parcel fence", model));
        } catch (IOException invalid) {
            throw new UncheckedIOException("Invalid placed House parcel mesh", invalid);
        }
    }

    private static GlbProps.Vertex place(GlbProps.Vertex vertex, Socket panel, double scale, double cos, double sin) {
        double x = vertex.x() * scale;
        double z = vertex.z();
        double nx = vertex.nx() / scale;
        double normal = Math.hypot(Math.hypot(nx, vertex.ny()), vertex.nz());
        if (normal == 0) normal = 1;
        return new GlbProps.Vertex((float)(panel.x() + x*cos - z*sin), (float)(panel.y() + vertex.y()),
            (float)(panel.z() + x*sin + z*cos), vertex.u(), vertex.v(),
            (nx*cos - vertex.nz()*sin) / normal, vertex.ny() / normal, (nx*sin + vertex.nz()*cos) / normal);
    }

    private static final class Canonical {
        static final String STYLE = "garden-panels-v1#" + digest();

        private static String digest() {
            try (var input = GlbProps.class.getResourceAsStream("/org/beckenburg/models/" + MODEL + ".glb")) {
                if (input == null) throw new IOException("Missing packaged House fence source");
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
            } catch (IOException invalid) {
                throw new UncheckedIOException(invalid);
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }
}
