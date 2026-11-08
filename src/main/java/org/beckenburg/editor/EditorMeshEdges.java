package org.beckenburg.editor;

import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Edge selections derived from native face IDs and explicitly authored quad fans. */
public final class EditorMeshEdges {
    private EditorMeshEdges() {}
    public enum Mode {
        BOUNDARY("Boundary loop"), LOOP("Quad loop"), RING("Quad ring");
        private final String label;
        Mode(String label) { this.label = label; }
        public String toString() { return label; }
    }
    public record Segment(Vertex a, Vertex b) {}
    private record Edge(long a, long b) {
        static Edge of(long a, long b) {return new Edge(Math.min(a,b),Math.max(a,b));}
    }
    private record Wedge(long left, long right) {}

    public static List<Segment> select(Observation observation, Mode mode, long first, long second) {
        Objects.requireNonNull(mode);
        if (mode == Mode.BOUNDARY) {
            var vertices = observation.boundaryLoop(first,second);
            var segments = new ArrayList<Segment>(vertices.size());
            for (int i=0;i<vertices.size();i++) segments.add(new Segment(vertices.get(i),vertices.get((i+1)%vertices.size())));
            return List.copyOf(segments);
        }
        var state = observation.snapshot();
        if (state.quads().isEmpty()) throw new IllegalArgumentException("This mesh has no authored quads");
        if (!observation.topology().manifold()) throw new IllegalArgumentException("Repair invalid topology before selecting edges");
        var faces = new HashMap<Long,Face>();
        for (var face : state.faces()) faces.put(face.id(),face);
        var opposites = new HashMap<Edge,List<Edge>>();
        var wedges = new HashMap<Long,List<Wedge>>();
        var seed = Edge.of(first,second);
        boolean found = false;
        for (var quad : state.quads()) {
            var a = faces.remove(quad.first()); var b = faces.remove(quad.second());
            long[] rim = {a.a().vertex(),a.b().vertex(),a.c().vertex(),b.c().vertex()};
            for (int i=0;i<4;i++) {
                long vertex=rim[i], next=rim[(i+1)%4];
                var edge=Edge.of(vertex,next);
                if (edge.equals(seed)) found=true;
                if (mode==Mode.RING) {
                    opposites.computeIfAbsent(edge,k->new ArrayList<>(2)).add(Edge.of(rim[(i+2)%4],rim[(i+3)%4]));
                } else {
                    wedges.computeIfAbsent(vertex,k->new ArrayList<>(4)).add(new Wedge(rim[(i+3)%4],next));
                }
            }
        }
        if (!found) throw new IllegalArgumentException("Pick an authored quad side; a triangulation diagonal cannot seed this selection");
        // A triangle or ngon outside the explicit quad set ends a loop at that vertex.
        if (mode==Mode.LOOP) for (var face : faces.values()) {
            wedges.remove(face.a().vertex());wedges.remove(face.b().vertex());wedges.remove(face.c().vertex());
        }
        var selected = new LinkedHashSet<Edge>();
        var queue = new ArrayDeque<Edge>();selected.add(seed);queue.add(seed);
        while (!queue.isEmpty()) {
            var edge=queue.removeFirst();
            if (mode==Mode.RING) {
                for (var opposite:opposites.get(edge)) if(selected.add(opposite)) queue.addLast(opposite);
            } else {
                extend(edge.a,edge.b,wedges,selected,queue);
                extend(edge.b,edge.a,wedges,selected,queue);
            }
        }
        var positions = new HashMap<Long,Vertex>();
        for (var vertex:state.vertices()) positions.put(vertex.id(),vertex);
        return selected.stream().map(e->new Segment(positions.get(e.a),positions.get(e.b))).toList();
    }

    private static void extend(long center,long incoming,Map<Long,List<Wedge>> fans,Set<Edge> visited,Deque<Edge> queue) {
        var fan=fans.get(center);
        if (fan==null || (fan.size()!=2 && fan.size()!=4)) return;
        var spokes=new HashSet<Long>();var neighbors=new HashSet<Long>();
        for (var wedge:fan) {
            spokes.add(wedge.left);spokes.add(wedge.right);
            if (wedge.left==incoming) neighbors.add(wedge.right);
            if (wedge.right==incoming) neighbors.add(wedge.left);
        }
        if (spokes.size()!=(fan.size()==4?4:3)) return;
        spokes.remove(incoming);spokes.removeAll(neighbors);
        if (spokes.size()==1) {
            var next=Edge.of(center,spokes.iterator().next());
            if (visited.add(next)) queue.addLast(next);
        }
    }
}
