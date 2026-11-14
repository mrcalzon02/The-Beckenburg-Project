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

    private record RailSide(long firstFace,long secondFace,long targetA,long targetB) {}

    /** Pick the rail on the clicked quad, then follow its shared endpoint around the entire loop. */
    static Map<Long,Long> loopRails(Observation observation,long first,long second,long activeFace,boolean reverse) {
        var loop=select(observation,Mode.LOOP,first,second);
        if (loop.size()<2) throw new IllegalArgumentException("Pick a loop with at least two edges");
        var incident=new HashMap<Long,List<Edge>>();
        var choices=new HashMap<Edge,List<RailSide>>();
        for (var segment:loop) {
            var edge=Edge.of(segment.a().id(),segment.b().id());
            choices.put(edge,new ArrayList<>(2));
            incident.computeIfAbsent(edge.a,k->new ArrayList<>(2)).add(edge);
            incident.computeIfAbsent(edge.b,k->new ArrayList<>(2)).add(edge);
        }
        if (incident.values().stream().anyMatch(edges->edges.size()>2)) throw new IllegalArgumentException("Slide requires one unbranched loop");
        var faces=new HashMap<Long,Face>();
        for (var face:observation.snapshot().faces()) faces.put(face.id(),face);
        for (var quad:observation.snapshot().quads()) {
            var a=faces.get(quad.first());var b=faces.get(quad.second());
            long[] rim={a.a().vertex(),a.b().vertex(),a.c().vertex(),b.c().vertex()};
            for (int i=0;i<4;i++) {
                long from=rim[i],to=rim[(i+1)%4];var key=Edge.of(from,to);var sides=choices.get(key);
                if (sides==null) continue;
                long oppositeFrom=rim[(i+3)%4],oppositeTo=rim[(i+2)%4];
                sides.add(new RailSide(a.id(),b.id(),key.a==from?oppositeFrom:oppositeTo,key.b==to?oppositeTo:oppositeFrom));
            }
        }
        var seed=Edge.of(first,second);var seedChoices=choices.get(seed);
        var picked=seedChoices.stream().filter(s->s.firstFace==activeFace || s.secondFace==activeFace).findFirst()
                .orElseThrow(()->new IllegalArgumentException("The picked face does not supply a quad rail"));
        if(reverse) picked=seedChoices.stream().filter(s->s.firstFace!=activeFace && s.secondFace!=activeFace).findFirst()
                .orElseThrow(()->new IllegalArgumentException("The opposite side has no authored quad rail"));
        var targets=new HashMap<Long,Long>();targets.put(seed.a,picked.targetA);targets.put(seed.b,picked.targetB);
        var queue=new ArrayDeque<Edge>();var visited=new HashSet<Edge>();queue.add(seed);visited.add(seed);
        while (!queue.isEmpty()) {
            var edge=queue.removeFirst();
            for(long endpoint:new long[]{edge.a,edge.b}) for(var next:incident.get(endpoint)) {
                if(!visited.add(next)) continue;
                var candidates=choices.get(next);RailSide matched=null;
                for(var side:candidates) {
                    if (!targets.getOrDefault(next.a,side.targetA).equals(side.targetA) || !targets.getOrDefault(next.b,side.targetB).equals(side.targetB)) continue;
                    if(matched!=null) throw new IllegalArgumentException("Ambiguous quad rail at loop junction");
                    matched=side;
                }
                if(matched==null)throw new IllegalArgumentException("The selected side has no continuous quad rail");
                targets.put(next.a,matched.targetA);targets.put(next.b,matched.targetB);queue.addLast(next);
            }
        }
        if(visited.size()!=loop.size() || targets.values().stream().anyMatch(targets::containsKey)
                || new HashSet<>(targets.values()).size()!=targets.size()) throw new IllegalArgumentException("Slide rails intersect or merge");
        return targets;
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
