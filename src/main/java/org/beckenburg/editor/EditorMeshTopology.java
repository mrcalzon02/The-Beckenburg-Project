package org.beckenburg.editor;

import java.util.*;

/** Bounded adjacency derived from the exact human import mesh. Named parts stay
 * separate; per-corner UVs remain in the source. This graph never repairs,
 * welds, reorders or replaces authored triangles implicitly.
 */
public final class EditorMeshTopology {
    public enum Code { DEGENERATE_FACE, NON_MANIFOLD_EDGE, EDGE_ORIENTATION, NON_MANIFOLD_VERTEX }
    public record Issue(Code code,int element,String detail) {}
    public record Vertex(String part,double x,double y,double z) {
        public Vertex {
            if(part==null||part.isBlank()||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))
                throw new IllegalArgumentException("Invalid topology position");
        }
    }
    public record HalfEdge(int origin,int destination,int face,int next,int previous,int twin,
                           boolean boundary,boolean uvSeam) {}
    public record Face(String part,int edge,int island) {}
    private final List<Vertex> vertices;
    private final List<HalfEdge> edges;
    private final List<Face> faces;
    private final List<Issue> issues;
    private final int issueCount,islands,boundaryEdges,uvSeams;
    private EditorMeshTopology(List<Vertex> vertices,List<HalfEdge> edges,List<Face> faces,
                               List<Issue> issues,int issueCount,int islands,int boundaries,int seams){
        this.vertices=List.copyOf(vertices);this.edges=List.copyOf(edges);this.faces=List.copyOf(faces);
        this.issues=List.copyOf(issues);this.issueCount=issueCount;this.islands=islands;boundaryEdges=boundaries;uvSeams=seams;
    }
    public List<Vertex> vertices(){return vertices;}
    public List<HalfEdge> edges(){return edges;}
    public List<Face> faces(){return faces;}
    public List<Issue> issues(){return issues;}
    public int issueCount(){return issueCount;}
    public int islands(){return islands;}
    public int boundaryEdges(){return boundaryEdges;}
    /** Undirected paired edges with discontinuous UVs. */
    public int uvSeams(){return uvSeams;}
    public boolean manifold(){return issueCount==0;}
    private static final class Union {
        final int[] parent,rank;
        Union(int count){parent=new int[count];rank=new int[count];for(int i=0;i<count;i++)parent[i]=i;}
        int root(int i){while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;}
        void join(int a,int b){a=root(a);b=root(b);if(a==b)return;
            if(rank[a]<rank[b])parent[a]=b;else{parent[b]=a;if(rank[a]==rank[b])rank[a]++;}}
    }
    private static final class Findings {
        final ArrayList<Issue> issues=new ArrayList<>();int count;
        void add(Code code,int id,String detail){count++;if(issues.size()<4096)issues.add(new Issue(code,id,detail));}
    }
    private static double zero(double value){return value==0?0:value;}
    private static Vertex point(String part,EditorMeshPipeline.Vertex v){return new Vertex(part,zero(v.x()),zero(v.y()),zero(v.z()));}
    private static boolean sameUv(EditorMeshPipeline.Vertex a,EditorMeshPipeline.Vertex b){return a.u()==b.u()&&a.v()==b.v();}
    private static int next(int edge){return edge/3*3+(edge%3+1)%3;}
    public static EditorMeshTopology build(EditorMeshPipeline.Mesh mesh){return build(mesh,null,null);}
    /** Explicit topology keeps distinct vertex IDs distinct even at coincident positions. */
    public static EditorMeshTopology buildIndexed(EditorMeshPipeline.Mesh mesh,List<Vertex> vertices,int[] origins){
        Objects.requireNonNull(mesh);
        if(vertices.size()>EditorMeshPipeline.MAX_TRIANGLES*3||origins.length!=mesh.triangles().size()*3)
            throw new IllegalArgumentException("Indexed topology budget/count mismatch");
        return build(mesh,List.copyOf(vertices),origins.clone());
    }
    private static EditorMeshTopology build(EditorMeshPipeline.Mesh mesh,List<Vertex> explicit,int[] explicitOrigins){
        Objects.requireNonNull(mesh);int count=mesh.triangles().size(),total=Math.multiplyExact(count,3);
        if(explicitOrigins!=null&&explicitOrigins.length!=total)throw new IllegalArgumentException("Corner count mismatch");
        var vertices=explicit==null?new ArrayList<Vertex>():new ArrayList<>(explicit);var indices=new HashMap<Vertex,Integer>();
        int[] origin=new int[total],twin=new int[total];Arrays.fill(twin,-1);
        boolean[] boundary=new boolean[total],seam=new boolean[total];
        // Three 19-bit indices fit in one positive long at the import budget.
        // Primitive sorting avoids a list/map allocation for every triangle edge.
        if(total>=(1<<19))throw new IllegalArgumentException("Topology index budget exceeded");
        final long mask=(1L<<19)-1;long[] incident=new long[total];
        var corners=new EditorMeshPipeline.Vertex[total];
        var faceGroups=new Union(count);var fans=new Union(total);var findings=new Findings();
        for(int i=0;i<count;i++){
            var t=mesh.triangles().get(i);var values=new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()};
            for(int j=0;j<3;j++){
                int e=i*3+j;corners[e]=values[j];var point=point(t.part(),values[j]);
                if(explicitOrigins!=null){
                    int id=explicitOrigins[e];
                    if(id<0||id>=vertices.size())throw new IllegalArgumentException("Unknown indexed vertex");
                    var expected=vertices.get(id);
                    if(!point.part().equals(expected.part())||point.x()!=expected.x()||point.y()!=expected.y()||point.z()!=expected.z())
                        throw new IllegalArgumentException("Indexed corner differs from authoritative vertex");
                    origin[e]=id;
                }else{
                    Integer id=indices.get(point);if(id==null){id=vertices.size();indices.put(point,id);vertices.add(point);}origin[e]=id;
                }
            }
            double ax=t.b().x()-t.a().x(),ay=t.b().y()-t.a().y(),az=t.b().z()-t.a().z();
            double bx=t.c().x()-t.a().x(),by=t.c().y()-t.a().y(),bz=t.c().z()-t.a().z();
            double area=Math.hypot(Math.hypot(ay*bz-az*by,az*bx-ax*bz),ax*by-ay*bx);
            if(area==0||!Double.isFinite(area))findings.add(Code.DEGENERATE_FACE,i,"Zero or non-finite triangle area");
            for(int j=0;j<3;j++){
                int e=i*3+j,a=origin[e],b=origin[next(e)];
                long key=((long)Math.min(a,b)<<19)|Math.max(a,b);
                incident[e]=(key<<19)|e;
            }
        }
        int boundaries=0,seams=0;Arrays.sort(incident);
        for(int start=0;start<total;){
            int end=start+1;long key=incident[start]>>>19;
            while(end<total&&(incident[end]>>>19)==key)end++;
            int groupStart=start;start=end;
            int first=(int)(incident[groupStart]&mask),a=origin[first],b=origin[next(first)];
            if(end-groupStart==1){boundary[first]=true;boundaries++;continue;}
            for(int i=groupStart+1;i<end;i++){
                int other=(int)(incident[i]&mask);faceGroups.join(first/3,other/3);
                // A vertex fan joins only through an incident edge, not a remote face path.
                fans.join(first,origin[other]==a?other:next(other));
                fans.join(next(first),origin[other]==b?other:next(other));
            }
            if(end-groupStart>2){findings.add(Code.NON_MANIFOLD_EDGE,first,"Edge has "+(end-groupStart)+" incident faces");continue;}
            int other=(int)(incident[groupStart+1]&mask);
            if(a==b||origin[other]!=b||origin[next(other)]!=a){
                findings.add(Code.EDGE_ORIENTATION,first,"Shared edge is not oppositely oriented");continue;
            }
            twin[first]=other;twin[other]=first;
            if(!sameUv(corners[first],corners[next(other)])||!sameUv(corners[next(first)],corners[other])){
                seam[first]=seam[other]=true;seams++;
            }
        }
        int[] vertexFan=new int[vertices.size()];Arrays.fill(vertexFan,-1);boolean[] reported=new boolean[vertices.size()];
        for(int e=0;e<total;e++){
            int v=origin[e],fan=fans.root(e);
            if(vertexFan[v]<0)vertexFan[v]=fan;
            else if(vertexFan[v]!=fan&&!reported[v]){reported[v]=true;findings.add(Code.NON_MANIFOLD_VERTEX,v,"Vertex has disconnected face fans");}
        }
        var islandIds=new LinkedHashMap<Integer,Integer>();var faces=new ArrayList<Face>(count);var edges=new ArrayList<HalfEdge>(total);
        for(int i=0;i<count;i++){
            int root=faceGroups.root(i);Integer island=islandIds.get(root);
            if(island==null){island=islandIds.size();islandIds.put(root,island);}
            faces.add(new Face(mesh.triangles().get(i).part(),i*3,island));
        }
        for(int e=0;e<total;e++)edges.add(new HalfEdge(origin[e],origin[next(e)],e/3,next(e),e/3*3+(e%3+2)%3,twin[e],boundary[e],seam[e]));
        return new EditorMeshTopology(vertices,edges,faces,findings.issues,findings.count,islandIds.size(),boundaries,seams);
    }
    public String summary(){return "Topology: "+vertices.size()+" vertices / "+edges.size()+" half-edges / "+islands
        +" islands / "+boundaryEdges+" boundary edges / "+uvSeams+" UV seams / "+issueCount+" issues";}
}
