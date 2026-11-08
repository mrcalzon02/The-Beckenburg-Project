package org.beckenburg.editor;

import java.io.IOException;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.Face;

/** Two canonical triangles explicitly authored as one quadrilateral, in fan order. */
public record EditorMeshQuad(long first, long second) {
    public EditorMeshQuad {
        if (first < 0 || second < 0 || first == second) throw new IllegalArgumentException("Invalid quad face IDs");
    }

    static List<EditorMeshQuad> validate(List<EditorMeshQuad> quads, List<Face> faces) {
        if (quads.isEmpty()) return List.of();
        if (quads.size() > faces.size()/2) throw new IllegalArgumentException("Quad count exceeds face budget");
        var byId = new HashMap<Long,Face>();
        for (var face : faces) byId.put(face.id(), face);
        var used = new HashSet<Long>();
        for (var quad : quads) {
            var a = byId.get(quad.first); var b = byId.get(quad.second);
            if (a == null || b == null || !used.add(a.id()) || !used.add(b.id()) || !a.part().equals(b.part())
                    || a.a().vertex() != b.a().vertex() || a.c().vertex() != b.b().vertex()
                    || new HashSet<>(List.of(a.a().vertex(), a.b().vertex(), a.c().vertex(), b.c().vertex())).size() != 4) {
                throw new IllegalArgumentException("Quad must identify two distinct native triangles with one reversed diagonal");
            }
        }
        return ordered(quads);
    }

    /** Topology-changing edits invalidate only pairs whose original corners changed. */
    static List<EditorMeshQuad> retained(EditorTopologyProject.State before, List<Face> faces) {
        if (before.quads().isEmpty()) return List.of();
        var after = new HashMap<Long,Face>();
        for (var face : faces) after.put(face.id(), face);
        var unchanged = new HashSet<Long>();
        for (var old : before.faces()) {
            var face = after.get(old.id());
            if (face != null && face.part().equals(old.part()) && face.a().vertex() == old.a().vertex()
                    && face.b().vertex() == old.b().vertex() && face.c().vertex() == old.c().vertex()) unchanged.add(old.id());
        }
        return before.quads().stream().filter(q -> unchanged.contains(q.first) && unchanged.contains(q.second)).toList();
    }

    static List<EditorMeshQuad> ordered(List<EditorMeshQuad> quads) {
        return quads.isEmpty() ? List.of() : quads.stream().sorted(Comparator.comparingLong(EditorMeshQuad::first)
                .thenComparingLong(EditorMeshQuad::second)).toList();
    }
    static List<EditorMeshQuad> ordinals(List<EditorMeshQuad> quads, List<Long> ids) {
        if (quads.isEmpty() || ids.isEmpty()) return ordered(quads);
        var indices = new HashMap<Long,Integer>();
        for (int i=0; i<ids.size(); i++) indices.put(ids.get(i),i);
        var result = new ArrayList<EditorMeshQuad>(quads.size());
        for (var q : quads) {
            var a = indices.get(q.first); var b = indices.get(q.second);
            if (a == null || b == null) throw new IllegalArgumentException("Quad references an unknown native face");
            result.add(new EditorMeshQuad(a,b));
        }
        return ordered(result);
    }

    private static final String EXTENSION = "BECK_authored_quads";
    static void writeGlb(Map<String,Object> root, EditorMeshPipeline.Mesh mesh) {
        var extensions = root.get("extensions") instanceof Map<?,?> ? EditorGlbReader.obj(root.get("extensions")) : new LinkedHashMap<String,Object>();
        var used = root.get("extensionsUsed") instanceof List<?> list ? new ArrayList<Object>(list) : new ArrayList<Object>();
        if (mesh.quads().isEmpty()) {
            if (extensions.remove(EXTENSION) != null) {
                used.remove(EXTENSION);
                if (used.isEmpty()) root.remove("extensionsUsed"); else root.put("extensionsUsed",used);
                if (extensions.isEmpty()) root.remove("extensions");
            }
            return;
        }
        var pairs = ordinals(mesh.quads(),mesh.faceIds()).stream().map(q -> List.of(q.first,q.second)).toList();
        extensions.put(EXTENSION,Map.of("version",1,"pairs",pairs));root.put("extensions",extensions);
        if (!used.contains(EXTENSION)) used.add(EXTENSION);
        root.put("extensionsUsed",used);
    }
    static List<EditorMeshQuad> readGlb(Map<String,Object> root, int count) throws IOException {
        if (!(root.get("extensions") instanceof Map<?,?> extensions) || !extensions.containsKey(EXTENSION)) return List.of();
        if (!(extensions.get(EXTENSION) instanceof Map<?,?> data)) throw new IOException("Invalid authored quad metadata");
        if (!(data.get("version") instanceof Number version) || !"1".equals(version.toString()) || !(data.get("pairs") instanceof List<?> pairs) || pairs.size()>count/2)
            throw new IOException("Unsupported authored quad metadata");
        var quads=new ArrayList<EditorMeshQuad>();var used=new HashSet<Long>();
        for(var entry:pairs){
            if(!(entry instanceof List<?> pair) || pair.size()!=2 || !(pair.get(0) instanceof Number) || !(pair.get(1) instanceof Number)) throw new IOException("Invalid authored quad pair");
            try {
                long a=Long.parseLong(pair.get(0).toString()), b=Long.parseLong(pair.get(1).toString());
                if(a>=count || b>=count || !used.add(a) || !used.add(b)) throw new IOException("Invalid authored quad face reference");
                quads.add(new EditorMeshQuad(a,b));
            } catch (IllegalArgumentException bad) {throw new IOException("Invalid authored quad index",bad);}
        }
        return ordered(quads);
    }
}
