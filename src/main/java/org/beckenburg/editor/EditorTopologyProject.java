package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Authoritative human mesh authoring state. Triangle buffers and adjacency are
 * derived from stable indexed vertices/corner UVs. No game scene is duplicated.
 */
public final class EditorTopologyProject implements EditorProject<EditorTopologyProject.State> {
    private static final int MAGIC=0x42544f50,VERSION=10,MAX_VERTICES=EditorMeshPipeline.MAX_TRIANGLES*3;
    public record Vertex(long id,String part,double x,double y,double z){
        public Vertex {
            text(part);if(id<0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid topology vertex");
        }
    }
    public record Corner(long vertex,double u,double v,EditorMeshPipeline.Normal normal){
        public Corner(long vertex,double u,double v){this(vertex,u,v,null);}
        public Corner {if(vertex<0||!Double.isFinite(u)||!Double.isFinite(v))throw new IllegalArgumentException("Invalid corner");}
    }
    public record Face(long id,String part,Corner a,Corner b,Corner c,String material){
        public Face(long id,String part,Corner a,Corner b,Corner c){this(id,part,a,b,c,"");}
        public Face {text(part);if(id<0||material==null)throw new IllegalArgumentException("Invalid face ID/material");Objects.requireNonNull(a);Objects.requireNonNull(b);Objects.requireNonNull(c);}
    }
    public record State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,Map<String,EditorMeshFrame> frames,List<EditorMeshQuad> quads){
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,Map<String,EditorMeshFrame> frames){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,materials,glbSource,frames,List.of());}
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,materials,glbSource,Map.of());}
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,materials,null);}
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,List.of());}
        public State {
            text(sceneId);text(name);frames=frames.isEmpty()?Map.of():EditorMeshFrame.validate(frames,vertices.stream().map(Vertex::part).toList());vertices=List.copyOf(vertices);faces=List.copyOf(faces);materials=List.copyOf(materials);EditorMeshMaterial.validateTable(materials);var materialKeys=new HashSet<String>();for(var material:materials)materialKeys.add(material.key());
            if(sourceFingerprint==null||!sourceFingerprint.matches("[0-9a-f]{64}")||sourceBytes<0||vertices.size()>MAX_VERTICES||faces.size()>EditorMeshPipeline.MAX_TRIANGLES)
                throw new IllegalArgumentException("Invalid topology state");
            if(glbSource!=null){if(!glbSource.fingerprint().equals(sourceFingerprint))throw new IllegalArgumentException("Foreign GLB source provenance");try{for(var original:glbSource.nativeMaterials())if(!materials.contains(original))throw new IllegalArgumentException("Original GLB material provenance must be retained");}catch(IOException invalid){throw new IllegalArgumentException("Invalid GLB source materials",invalid);}}
            quads=EditorMeshQuad.validate(quads,faces);
            var ids=new HashMap<Long,Vertex>();for(var vertex:vertices)if(ids.putIfAbsent(vertex.id(),vertex)!=null)throw new IllegalArgumentException("Duplicate vertex ID");
            var faceIds=new HashSet<Long>();for(var face:faces){
                if(!faceIds.add(face.id()))throw new IllegalArgumentException("Duplicate face ID");if(!face.material().isEmpty()&&!materialKeys.contains(face.material()))throw new IllegalArgumentException("Unresolved native face material");
                for(var corner:List.of(face.a(),face.b(),face.c())){
                    var vertex=ids.get(corner.vertex());if(vertex==null||!vertex.part().equals(face.part()))throw new IllegalArgumentException("Foreign/missing face vertex");
                }
            }
        }
    }
    private final Path path;
    private final String id,sourceFingerprint;
    private final EditorGlbReader.Document sourceGlb;
    private State state;
    private State cleanState;
    private EditorModelWorkspace savedWorkspace;
    private String diskFingerprint;
    private long revision=1;
    private static void text(String value){if(value==null||value.isBlank()||value.length()>512)throw new IllegalArgumentException("Invalid topology text");}
    private EditorTopologyProject(Path path,State state){this.path=path.toAbsolutePath().normalize();this.state=state;this.cleanState=state;id=state.sceneId();sourceFingerprint=state.sourceFingerprint();sourceGlb=state.glbSource();}
    /** Preserve supplied corner identities; unindexed triangle sources use geometric connectivity per part. */
    public static EditorTopologyProject fromMesh(EditorMeshPipeline.Mesh mesh,String sourceFingerprint,Path path){
        var graph=EditorMeshTopology.build(mesh);var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();
        for(int i=0;i<graph.vertices().size();i++){var v=graph.vertices().get(i);vertices.add(new Vertex(i,v.part(),v.x(),v.y(),v.z()));}
        for(int i=0;i<mesh.triangles().size();i++){
            var triangle=mesh.triangles().get(i);var uv=List.of(triangle.a(),triangle.b(),triangle.c());var corners=new ArrayList<Corner>();
            for(int j=0;j<3;j++)corners.add(new Corner(graph.edges().get(i*3+j).origin(),uv.get(j).u(),uv.get(j).v(),uv.get(j).normal()));
            faces.add(new Face(mesh.faceIds().isEmpty()?i:mesh.faceIds().get(i),triangle.part(),corners.get(0),corners.get(1),corners.get(2),triangle.material()));
        }
        return new EditorTopologyProject(path,new State("mesh-"+UUID.randomUUID(),sourceFingerprint,mesh.name(),mesh.sourceBytes(),vertices,faces,mesh.materials(),mesh.glbSource(),mesh.frames(),mesh.quads()));
    }
    public static EditorTopologyProject fromGlb(String name,byte[] original,Path path)throws IOException{var document=EditorGlbReader.read(original);document.requireEditable();try{var project=fromMesh(document.mesh(name),document.fingerprint(),path);project.snapshotBytes();return project;}catch(IllegalArgumentException invalid){throw new IOException("GLB topology exceeds supported native authoring capabilities",invalid);}}
    public String id(){return id;}
    public EditorHistory<State> createHistory(int capacity){
        return new EditorHistory<>(this,capacity,8L*1024*1024,
            (before,after)->EditorMeshMaterial.paintHistoryBytes(before.materials(),after.materials()));
    }
    public synchronized String title(){return state.name();}
    public Path documentPath(){return path;}
    public synchronized long revision(){return revision;}
    private static EditorMeshFrame frame(State state,String part){
        var stored=state.frames().get(part);if(stored!=null)return stored;
        if(state.glbSource()!=null&&part.startsWith("glb_node_")){try{var matrix=state.glbSource().world(Integer.parseInt(part.substring(9)));if(matrix!=null)return EditorMeshFrame.matrix(matrix);}catch(NumberFormatException|IndexOutOfBoundsException invalid){throw new IllegalArgumentException("Invalid source part frame",invalid);}}
        return EditorMeshFrame.IDENTITY;
    }
    public synchronized EditorMeshFrame frame(String part){if(state.vertices().stream().noneMatch(v->v.part().equals(part)))throw new IllegalArgumentException("Unknown native part");return frame(state,part);}
    public synchronized Map<String,EditorMeshFrame> frames(Collection<String> parts){var known=new HashSet<String>();for(var v:state.vertices())known.add(v.part());var result=new LinkedHashMap<String,EditorMeshFrame>();for(String part:parts){if(!known.contains(part))throw new IllegalArgumentException("Unknown native part");result.put(part,frame(state,part));}return Map.copyOf(result);}

    private static State framed(State state,Map<String,EditorMeshFrame> frames){return new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),state.vertices(),state.faces(),state.materials(),state.glbSource(),frames,state.quads());}
    private static State transformFrames(State before,State after,Set<Long> selected,java.util.function.Function<String,EditorMeshFrame> delta){
        var counts=new HashMap<String,int[]>();for(var v:before.vertices()){var count=counts.computeIfAbsent(v.part(),k->new int[2]);count[0]++;if(selected.contains(v.id()))count[1]++;}
        Map<String,EditorMeshFrame> frames=null;for(var e:counts.entrySet())if(e.getValue()[0]==e.getValue()[1]){if(frames==null)frames=new TreeMap<>(before.frames());frames.put(e.getKey(),delta.apply(e.getKey()).multiply(frame(before,e.getKey())));}
        return frames==null?after:framed(after,frames);
    }

    public synchronized State snapshot(){return state;}
    public synchronized boolean hasUnsavedChanges(){return !state.equals(cleanState);}
    public static final class Observation {
        private final EditorTopologyProject owner;
        private final State state;
        private final long revision;
        private Observation(EditorTopologyProject owner,State state,long revision){this.owner=owner;this.state=state;this.revision=revision;}
        private Map<Long,Vertex> vertexIndex;
        private volatile EditorMeshPipeline.Mesh derivedMesh;
        private volatile EditorMeshTopology derivedTopology;
        /** Lookup in this immutable native observation. Built once per gesture when needed. */
        public synchronized Vertex vertex(long id) {
            if (vertexIndex==null) vertexIndex=indexVertices(state.vertices());
            return vertexIndex.get(id);
        }
        public State snapshot(){return state;}
        public long revision(){return revision;}
        public synchronized EditorMeshPipeline.Mesh mesh(){
            if(derivedMesh==null) derivedMesh=EditorTopologyProject.mesh(state);
            return derivedMesh;
        }
        public synchronized EditorMeshTopology topology(){
            if (derivedTopology==null) derivedTopology=EditorTopologyProject.topology(state,mesh());
            return derivedTopology;
        }
        /** Ordered native vertices on the manifold boundary containing the picked edge. */
        public List<Vertex> boundaryLoop(long first, long second) {
            var graph = topology();
            if (!graph.manifold()) throw new IllegalArgumentException("Repair invalid topology before selecting a boundary");
            int[] outgoing = new int[state.vertices().size()];
            Arrays.fill(outgoing, -1);
            int start = -1;
            boolean reverse = false;
            for (var edge : graph.edges()) {
                if (!edge.boundary()) continue;
                int origin = edge.origin(), destination = edge.destination();
                if (outgoing[origin] != -1) throw new IllegalArgumentException("Branched native boundary");
                outgoing[origin] = destination;
                long a = state.vertices().get(origin).id(), b = state.vertices().get(destination).id();
                if (a == first && b == second) start = origin;
                if (a == second && b == first) { start = destination; reverse = true; }
            }
            if (start < 0) throw new IllegalArgumentException("Pick an edge on an open mesh boundary");
            var loop = new ArrayList<Vertex>();
            int current = start;
            do {
                if (current < 0 || loop.size() >= outgoing.length) throw new IllegalArgumentException("Incomplete native boundary");
                loop.add(state.vertices().get(current));
                current = outgoing[current];
            } while (current != start);
            if (reverse) Collections.reverse(loop.subList(1, loop.size()));
            return List.copyOf(loop);
        }
    }
    public synchronized Observation observe(){return new Observation(this,state,revision);}
    /** Reuse unchanged immutable render triangles when advancing the human viewport. */
    public synchronized Observation observe(Observation previous) {
        var next=observe();
        if (previous!=null && previous.owner==this && previous.derivedMesh!=null) {
            next.derivedMesh=state==previous.state ? previous.derivedMesh : updateMesh(previous.derivedMesh,state);
            if (previous.derivedTopology!=null)
                next.derivedTopology=previous.derivedTopology.update(previous.derivedMesh,next.derivedMesh,
                    previous.state.vertices(),state.vertices());
        }
        return next;
    }
    public synchronized boolean isCurrent(Observation observation){return observation!=null&&observation.owner==this&&observation.state==state&&observation.revision==revision;}
    public record SavedReview(byte[] bytes,String diskFingerprint){
        public SavedReview {bytes=bytes.clone();}
        @Override public byte[] bytes(){return bytes.clone();}
    }
    /** The saved bytes and revision check share the same native monitor as edits. */
    public synchronized SavedReview saveReviewed(Observation observation)throws IOException{
        return saveReviewed(observation,savedWorkspace);
    }
    public synchronized SavedReview saveReviewed(Observation observation,EditorModelWorkspace workspace)throws IOException{
        if(!isCurrent(observation))throw new IOException("Native mesh changed since review; review again before publishing");
        save(workspace);return new SavedReview(snapshotBytes(),diskFingerprint);
    }
    public synchronized void exportObj(Observation observation,Path target,String expectedTargetSha)throws IOException{
        if(!isCurrent(observation))throw new IOException("Native mesh changed since export review");
        if(state.glbSource()!=null)throw new IOException("OBJ cannot preserve original GLB hierarchy/pivots; save BTOP or export a GLB copy");
        if(target.toAbsolutePath().normalize().equals(path))throw new IOException("OBJ export cannot replace the native authoring document");
        State expected=state;
        if(target.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".obj.zip")){
            EditorDocumentStore.saveIfUnchanged(target,out->EditorObjBundle.write(expected,out),file->EditorObjBundle.read("export.obj.zip",EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),file->EditorObjBundle.validate(expected,EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),expectedTargetSha);return;
        }
        EditorDocumentStore.saveIfUnchanged(target,out->EditorObjWriter.write(expected,out),
            file->EditorMeshPipeline.read("export.obj",EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),
            file->EditorObjWriter.validate(expected,EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),expectedTargetSha);
    }
    public synchronized void exportGlb(Observation observation,Path target,String expectedTargetSha)throws IOException{
        if(!isCurrent(observation))throw new IOException("Native mesh changed since GLB export review");if(target.toAbsolutePath().normalize().equals(path))throw new IOException("GLB copy cannot replace the native authoring document");
        var mesh=mesh(state);byte[] bytes=EditorGlbWriter.encode(mesh);EditorGlbWriter.validate(mesh,bytes);
        if (target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gltf")) {
            byte[] json=EditorGltfFile.fromGlb(bytes);
            EditorDocumentStore.saveIfUnchanged(target,out->out.write(json),
                file->EditorGlbReader.read(EditorGltfFile.toGlb(EditorMeshPipeline.readSource(file,EditorGltfFile.MAX_BYTES))),
                file->EditorGlbWriter.validate(mesh,EditorGltfFile.toGlb(EditorMeshPipeline.readSource(file,EditorGltfFile.MAX_BYTES))),
                expectedTargetSha);
            return;
        }
        EditorDocumentStore.saveIfUnchanged(target,out->out.write(bytes),file->EditorGlbReader.read(EditorMeshPipeline.readSource(file,4_000_000)),file->EditorGlbWriter.validate(mesh,EditorMeshPipeline.readSource(file,4_000_000)),expectedTargetSha);
    }
    public synchronized void restore(State value){
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IllegalArgumentException("Topology belongs to another source/project");
        if(!Objects.equals(sourceGlb,value.glbSource()))throw new IllegalArgumentException("Native restore cannot replace/drop original GLB provenance");
        state=value;revision++;
    }
    public List<Asset> assets(){return List.of();}
    public synchronized List<Node> hierarchy(){return state.vertices().stream().map(v->new Node("vertex-"+v.id(),"VERTEX",v.part()+" / "+v.id(),v.x(),v.y(),v.z())).toList();}
    private static Map<Long,Vertex> vertices(State state){var out=new HashMap<Long,Vertex>();for(var v:state.vertices())out.put(v.id(),v);return out;}
    private static EditorMeshPipeline.Vertex corner(Corner corner,Map<Long,Vertex> vertices){
        var v=vertices.get(corner.vertex());return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),corner.u(),corner.v(),corner.normal());
    }
    private static EditorMeshPipeline.Mesh mesh(State state){
        var vertices=vertices(state);var triangles=new ArrayList<EditorMeshPipeline.Triangle>();
        for(var face:state.faces())triangles.add(new EditorMeshPipeline.Triangle(face.part(),corner(face.a(),vertices),corner(face.b(),vertices),corner(face.c(),vertices),face.material()));
        var ids=new ArrayList<Long>();for(var face:state.faces()){ids.add(face.a().vertex());ids.add(face.b().vertex());ids.add(face.c().vertex());}
        var parts=new HashSet<String>();for(var t:triangles)parts.add(t.part());var visibleFrames=new TreeMap<String,EditorMeshFrame>();for(var e:state.frames().entrySet())if(parts.contains(e.getKey()))visibleFrames.put(e.getKey(),e.getValue());
        return new EditorMeshPipeline.Mesh(state.name(),triangles,state.sourceBytes(),ids,state.materials(),state.glbSource(),state.faces().stream().map(Face::id).toList(),visibleFrames,state.quads());
    }
    private static Map<Long,Vertex> indexVertices(List<Vertex> values){var map=new HashMap<Long,Vertex>();for(var v:values)map.put(v.id(),v);return map;}
    private static EditorMeshPipeline.Mesh updateMesh(EditorMeshPipeline.Mesh old,State state) {
        if (old.triangles().size()!=state.faces().size()) return mesh(state);
        var vertices=indexVertices(state.vertices());
        var triangles=new ArrayList<EditorMeshPipeline.Triangle>(state.faces().size());
        for (int i=0;i<state.faces().size();i++) {
            var face=state.faces().get(i);
            var prior=old.triangles().get(i);
            if (face.id()!=old.faceIds().get(i) || !face.part().equals(prior.part()) ||
                face.a().vertex()!=old.cornerVertexIds().get(3*i) || face.b().vertex()!=old.cornerVertexIds().get(3*i+1) ||
                face.c().vertex()!=old.cornerVertexIds().get(3*i+2)) return mesh(state);
            var a=updatedCorner(face.a(),prior.a(),vertices);
            var b=updatedCorner(face.b(),prior.b(),vertices);
            var c=updatedCorner(face.c(),prior.c(),vertices);
            triangles.add(a==prior.a() && b==prior.b() && c==prior.c() && face.material().equals(prior.material())
                ? prior : new EditorMeshPipeline.Triangle(face.part(),a,b,c,face.material()));
        }
        var visibleFrames=new TreeMap<String,EditorMeshFrame>();
        if (!state.frames().isEmpty()) for (var face:state.faces()) {
            var frame=state.frames().get(face.part());
            if (frame!=null) visibleFrames.put(face.part(),frame);
        }
        return new EditorMeshPipeline.Mesh(state.name(),triangles,state.sourceBytes(),old.cornerVertexIds(),
            state.materials(),state.glbSource(),old.faceIds(),visibleFrames,state.quads());
    }
    private static EditorMeshPipeline.Vertex updatedCorner(Corner c,EditorMeshPipeline.Vertex prior,Map<Long,Vertex> vertices) {
        var v=vertices.get(c.vertex());
        if (Double.compare(v.x(),prior.x())==0 && Double.compare(v.y(),prior.y())==0 && Double.compare(v.z(),prior.z())==0 &&
            Double.compare(c.u(),prior.u())==0 && Double.compare(c.v(),prior.v())==0 && Objects.equals(c.normal(),prior.normal()))
            return prior;
        return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),c.u(),c.v(),c.normal());
    }
    private static EditorMeshPipeline.Triangle triangle(Face f,Map<Long,Vertex> values){return new EditorMeshPipeline.Triangle(f.part(),corner(f.a(),values),corner(f.b(),values),corner(f.c(),values),f.material());}
    private static Corner normalCorner(Corner c,EditorMeshPipeline.Normal n){return new Corner(c.vertex(),c.u(),c.v(),n);}
    private static Face normalFace(Face f,EditorMeshPipeline.Triangle t){return new Face(f.id(),f.part(),normalCorner(f.a(),t.a().normal()),normalCorner(f.b(),t.b().normal()),normalCorner(f.c(),t.c().normal()),f.material());}
    public synchronized EditorMeshPipeline.Mesh mesh(){return mesh(state);}
    public synchronized EditorMeshTopology topology(){return topology(state);}
    private static EditorMeshTopology topology(State state){
        return topology(state,mesh(state));
    }
    private static EditorMeshTopology topology(State state,EditorMeshPipeline.Mesh mesh){
        var positions=new ArrayList<EditorMeshTopology.Vertex>();var ids=new HashMap<Long,Integer>();
        for(var v:state.vertices()){ids.put(v.id(),positions.size());positions.add(new EditorMeshTopology.Vertex(v.part(),v.x(),v.y(),v.z()));}
        int[] origins=new int[state.faces().size()*3];int i=0;
        for(var face:state.faces())for(var corner:List.of(face.a(),face.b(),face.c()))origins[i++]=ids.get(corner.vertex());
        return EditorMeshTopology.buildIndexed(mesh,positions,origins);
    }
    public enum FaceEdit { EXTRUDE, INSET }
    /** A validated candidate in this project's native schema; it owns no editor/runtime authority. */
    public static final class EditPreview {
        private final EditorTopologyProject owner;
        private final State before,after;
        private final long revision;
        private final String description;
        private boolean canceled;
        private EditPreview(EditorTopologyProject owner,State before,State after,long revision,String description){this.owner=owner;this.before=before;this.after=after;this.revision=revision;this.description=description;}
        public State snapshot(){return after;}
        public EditorMeshPipeline.Mesh mesh(){return EditorTopologyProject.mesh(after);}
        public EditorMeshTopology topology(){return EditorTopologyProject.topology(after);}
        public String description(){return description;}
    }
    /** Single-triangle region edit. Existing vertices/faces retain IDs; cap retains the selected face ID. */
    public synchronized EditPreview previewFaceEdit(long faceId,FaceEdit operation,double amount){
        Objects.requireNonNull(operation);
        if(!Double.isFinite(amount)||operation==FaceEdit.INSET&&(amount<=0||amount>=1)
            ||operation==FaceEdit.EXTRUDE&&(amount==0||Math.abs(amount)>10000))
            throw new IllegalArgumentException("Extrude needs a nonzero distance up to10000; inset needs a fraction between0 and1");
        var face=state.faces().stream().filter(f->f.id()==faceId).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown native face ID"));
        if(state.vertices().size()>MAX_VERTICES-3||state.faces().size()>EditorMeshPipeline.MAX_TRIANGLES-6)
            throw new IllegalArgumentException("Model edit exceeds topology budget");
        long vertexMax=state.vertices().stream().mapToLong(Vertex::id).max().orElse(-1),faceMax=state.faces().stream().mapToLong(Face::id).max().orElse(-1);
        if(vertexMax>Long.MAX_VALUE-3||faceMax>Long.MAX_VALUE-6)throw new IllegalArgumentException("Model edit exceeds ID budget");
        var byId=vertices(state);var corners=List.of(face.a(),face.b(),face.c());var points=corners.stream().map(c->byId.get(c.vertex())).toList();
        var a=points.get(0);var b=points.get(1);var c=points.get(2);
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),u=c.x()-a.x(),v=c.y()-a.y(),w=c.z()-a.z();
        double nx=y*w-z*v,ny=z*u-x*w,nz=x*v-y*u,length=Math.hypot(Math.hypot(nx,ny),nz);
        if(length==0||!Double.isFinite(length))throw new IllegalArgumentException("Selected face has no finite normal");
        if(!topology(state).manifold())throw new IllegalArgumentException("Model edit requires oriented manifold input; inspect or repair topology first");
        nx/=length;ny/=length;nz/=length;
        double cx=a.x()/3+b.x()/3+c.x()/3,cy=a.y()/3+b.y()/3+c.y()/3,cz=a.z()/3+b.z()/3+c.z()/3;
        double cu=face.a().u()/3+face.b().u()/3+face.c().u()/3,cv=face.a().v()/3+face.b().v()/3+face.c().v()/3;
        var newVertices=new ArrayList<>(state.vertices());var newCorners=new ArrayList<Corner>();
        for(int i=0;i<3;i++){
            var p=points.get(i);var old=corners.get(i);long id=vertexMax+1+i;
            boolean inset=operation==FaceEdit.INSET;
            newVertices.add(new Vertex(id,face.part(),inset?p.x()+(cx-p.x())*amount:p.x()+nx*amount,
                inset?p.y()+(cy-p.y())*amount:p.y()+ny*amount,inset?p.z()+(cz-p.z())*amount:p.z()+nz*amount));
            newCorners.add(new Corner(id,inset?old.u()+(cu-old.u())*amount:old.u(),inset?old.v()+(cv-old.v())*amount:old.v(),old.normal()));
        }
        var cap=new Face(face.id(),face.part(),newCorners.get(0),newCorners.get(1),newCorners.get(2),face.material());
        if(operation==FaceEdit.INSET){
            var updated=EditorMeshNormals.transport(triangle(face,byId),triangle(cap,indexVertices(newVertices)));
            cap=normalFace(cap,updated);
            newCorners=new ArrayList<>(List.of(cap.a(),cap.b(),cap.c()));
        }
        var newFaces=new ArrayList<Face>();for(var old:state.faces())newFaces.add(old.id()==face.id()?cap:old);
        for(int i=0;i<3;i++){
            int j=(i+1)%3;var oldA=corners.get(i);var oldB=corners.get(j);var newA=newCorners.get(i);var newB=newCorners.get(j);
            if(operation==FaceEdit.EXTRUDE){
                var pa=points.get(i);var pb=points.get(j);double edge=Math.hypot(Math.hypot(pb.x()-pa.x(),pb.y()-pa.y()),pb.z()-pa.z());
                oldA=new Corner(oldA.vertex(),0,0);oldB=new Corner(oldB.vertex(),edge,0);newA=new Corner(newA.vertex(),0,Math.abs(amount));newB=new Corner(newB.vertex(),edge,Math.abs(amount));
            }
            newFaces.add(new Face(faceMax+1+i*2,face.part(),oldA,oldB,newB,face.material()));newFaces.add(new Face(faceMax+2+i*2,face.part(),oldA,newB,newA,face.material()));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces,state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),newVertices),EditorMeshQuad.retained(state,newFaces));
        if(!topology(after).manifold())throw new IllegalArgumentException("Model edit would introduce invalid topology");
        return new EditPreview(this,state,after,revision,operation+" face "+faceId+" / +3 vertices, +6 triangles");
    }
    /** Slide one endpoint along an incident edge while preserving the surface and its open outline. */
    public synchronized EditPreview previewVertexSlide(long from, long toward, double fraction) {
        if (!topology(state).manifold()) throw new IllegalArgumentException("Repair invalid topology before sliding");
        return previewSlide(Map.of(from,toward),fraction,"Slide vertex " + from + " toward " + toward);
    }

    public synchronized EditPreview previewLoopSlide(Observation selection,long first,long second,long activeFace,
                                                     boolean reverse,double fraction) {
        if (!isCurrent(selection)) throw new IllegalArgumentException("Pick the loop again after a native revision change");
        var rails = EditorMeshEdges.loopRails(selection,first,second,activeFace,reverse);
        return previewSlide(rails,fraction,"Slide loop / " + rails.size() + " vertices");
    }

    private EditPreview previewSlide(Map<Long,Long> rails,double fraction,String label) {
        if (!Double.isFinite(fraction) || fraction<=0 || fraction>=1) throw new IllegalArgumentException("Slide fraction must be strictly between 0 and 1");
        var points=indexVertices(state.vertices());var next=new HashMap<>(points);
        var directions=new HashMap<Long,EditorMeshPipeline.Normal>();var neighbors=new HashMap<Long,Map<Long,Integer>>();
        for(var rail:rails.entrySet()) {
            var start=points.get(rail.getKey());var end=points.get(rail.getValue());
            if(start==null || end==null || start==end || !start.part().equals(end.part())) throw new IllegalArgumentException("Pick distinct endpoints within one native part");
            double dx=end.x()-start.x(),dy=end.y()-start.y(),dz=end.z()-start.z();
            directions.put(start.id(),EditorMeshNormals.unit(new EditorMeshPipeline.Normal(dx,dy,dz)));
            var moved=new Vertex(start.id(),start.part(),start.x()+dx*fraction,start.y()+dy*fraction,start.z()+dz*fraction);
            if(moved.equals(start)) throw new IllegalArgumentException("Slide is below native coordinate precision");
            next.put(start.id(),moved);neighbors.put(start.id(),new HashMap<>());
        }
        for(var face:state.faces()) {
            if(!rails.containsKey(face.a().vertex()) && !rails.containsKey(face.b().vertex()) && !rails.containsKey(face.c().vertex())) continue;
            var normal=EditorMeshNormals.face(triangle(face,points));var corners=List.of(face.a(),face.b(),face.c());
            for(var corner:corners) {
                long id=corner.vertex();var direction=directions.get(id);if(direction==null) continue;
                if(Math.abs(dot(normal,direction))>1e-12) throw new IllegalArgumentException("Slide would change the incident surface");
                for(var other:corners) if(other.vertex()!=id) neighbors.get(id).merge(other.vertex(),1,Integer::sum);
            }
            if(!(dot(normal,EditorMeshNormals.face(triangle(face,next)))>0)) throw new IllegalArgumentException("Slide would collapse or reverse a triangle");
        }
        for(var rail:rails.entrySet()) {
            var local=neighbors.get(rail.getKey());
            if(!local.containsKey(rail.getValue())) throw new IllegalArgumentException("Slide target is not a connected native vertex");
            var start=points.get(rail.getKey());var direction=directions.get(start.id());
            for(var neighbor:local.entrySet()) {
                if(neighbor.getValue()!=1) continue;
                var v=points.get(neighbor.getKey());
                var outline=EditorMeshNormals.unit(new EditorMeshPipeline.Normal(v.x()-start.x(),v.y()-start.y(),v.z()-start.z()));
                double cx=outline.y()*direction.z()-outline.z()*direction.y();
                double cy=outline.z()*direction.x()-outline.x()*direction.z();
                double cz=outline.x()*direction.y()-outline.y()*direction.x();
                if(Math.hypot(Math.hypot(cx,cy),cz)>1e-12) throw new IllegalArgumentException("Slide would move the open outline");
            }
        }
        var vertices=new ArrayList<Vertex>(state.vertices().size());
        for(var v:state.vertices()) vertices.add(next.get(v.id()));
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),vertices,state.faces(),state.materials(),state.glbSource(),state.frames(),state.quads());
        return new EditPreview(this,state,after,revision,label+" by "+fraction);
    }
    private static double dot(EditorMeshPipeline.Normal a, EditorMeshPipeline.Normal b) {
        return a.x()*b.x() + a.y()*b.y() + a.z()*b.z();
    }
    public synchronized EditPreview previewBevel(long first,long second,long activeFace,double depth){
        var selected=first==second?Set.of(first):Set.of(first,second);var beforeTopology=topology(state);if(!beforeTopology.manifold())throw new IllegalArgumentException("Bevel requires oriented manifold input; inspect or repair topology first");
        String part=state.vertices().stream().filter(v->v.id()==first).map(Vertex::part).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown native bevel vertex"));for(var edge:beforeTopology.edges())if(edge.boundary()&&beforeTopology.faces().get(edge.face()).part().equals(part))throw new IllegalArgumentException("Bevel requires a closed native part");
        var after=EditorMeshBevel.build(state,selected,activeFace,depth);var checked=topology(after);if(!checked.manifold()||checked.boundaryEdges()!=beforeTopology.boundaryEdges())throw new IllegalArgumentException("Native bevel would create invalid or open topology");return new EditPreview(this,state,after,revision,(first==second?"CORNER":"EDGE")+" BEVEL / "+first+(first==second?"":" - "+second)+" / cut depth "+depth+" / "+after.faces().size()+" faces");
    }
    public synchronized void applyPreview(EditPreview preview){
        Objects.requireNonNull(preview);
        if(preview.canceled||preview.owner!=this||preview.before!=state||preview.revision!=revision)throw new IllegalArgumentException("Model preview is canceled or belongs to a stale or foreign native revision");
        state=preview.after;revision++;preview.canceled=true;
    }
    public synchronized void cancelPreview(EditPreview preview){if(preview.owner!=this)throw new IllegalArgumentException("Foreign native preview");preview.canceled=true;}
    private record WeldPosition(String part,double x,double y,double z){}
    /** Explicit exact-position weld within each selected part; corner UVs never become vertex attributes. */
    public synchronized EditPreview previewWeld(Collection<Long> selection){
        Objects.requireNonNull(selection);var selected=new HashSet<>(selection);
        if(selected.size()<2||selected.size()>MAX_VERTICES)throw new IllegalArgumentException("Weld needs at least two selected native vertices");
        int found=0;for(var v:state.vertices())if(selected.contains(v.id()))found++;
        if(found!=selected.size())throw new IllegalArgumentException("Unknown selected vertex ID");
        var groups=new HashMap<WeldPosition,Long>();var replacements=new HashMap<Long,Long>();
        for(var v:state.vertices())if(selected.contains(v.id())){
            var key=new WeldPosition(v.part(),v.x()==0?0:v.x(),v.y()==0?0:v.y(),v.z()==0?0:v.z());
            groups.merge(key,v.id(),Math::min);
        }
        for(var v:state.vertices())if(selected.contains(v.id())){
            long target=groups.get(new WeldPosition(v.part(),v.x()==0?0:v.x(),v.y()==0?0:v.y(),v.z()==0?0:v.z()));
            if(target!=v.id())replacements.put(v.id(),target);
        }
        if(replacements.isEmpty())throw new IllegalArgumentException("Selected vertices have no coincident positions within a part");
        var newVertices=new ArrayList<Vertex>();for(var v:state.vertices())if(!replacements.containsKey(v.id()))newVertices.add(v);
        var newFaces=new ArrayList<Face>();int removed=0;
        for(var f:state.faces()){
            var a=weldCorner(f.a(),replacements);var b=weldCorner(f.b(),replacements);var c=weldCorner(f.c(),replacements);
            boolean changed=a.vertex()!=f.a().vertex()||b.vertex()!=f.b().vertex()||c.vertex()!=f.c().vertex();
            if(changed&&(a.vertex()==b.vertex()||b.vertex()==c.vertex()||c.vertex()==a.vertex())){removed++;continue;}
            newFaces.add(new Face(f.id(),f.part(),a,b,c,f.material()));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces,state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),newVertices),EditorMeshQuad.retained(state,newFaces));
        if(!topology(after).manifold())throw new IllegalArgumentException("Weld would leave invalid topology; select complete seams or repair degenerate faces first");
        return new EditPreview(this,state,after,revision,"WELD COINCIDENT / -"+replacements.size()+" vertices, -"+removed+" collapsed triangles");
    }
    private static Corner weldCorner(Corner value,Map<Long,Long> replacements){return new Corner(replacements.getOrDefault(value.vertex(),value.vertex()),value.u(),value.v(),value.normal());}
    /** Remove invalid zero/non-finite-area triangles explicitly, retaining every surviving stable ID/UV. */
    public synchronized EditPreview previewRemoveDegenerates(){
        var positions=vertices(state);var remaining=new ArrayList<Face>();
        for(var face:state.faces()){double size=area(face,positions);if(size>0&&Double.isFinite(size))remaining.add(face);}
        int removed=state.faces().size()-remaining.size();if(removed==0)throw new IllegalArgumentException("No degenerate native faces to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),remaining,state.materials(),state.glbSource(),state.frames(),EditorMeshQuad.retained(state,remaining));
        if(!topology(after).manifold())throw new IllegalArgumentException("Other topology errors remain; repair requires a valid resulting mesh");
        return new EditPreview(this,state,after,revision,"REMOVE DEGENERATES / -"+removed+" triangles; vertex IDs retained");
    }
    public synchronized EditPreview previewRemoveUnusedVertices(){
        var used=new HashSet<Long>();for(var f:state.faces()){used.add(f.a().vertex());used.add(f.b().vertex());used.add(f.c().vertex());}
        var retained=state.vertices().stream().filter(v->used.contains(v.id())).toList();int removed=state.vertices().size()-retained.size();
        if(removed==0)throw new IllegalArgumentException("No unused native vertices to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),retained,state.faces(),state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),retained),state.quads());
        return new EditPreview(this,state,after,revision,"REMOVE UNUSED / -"+removed+" vertices; surviving IDs and faces retained");
    }
    private static double area(Face face,Map<Long,Vertex> vertices){
        var a=vertices.get(face.a().vertex());var b=vertices.get(face.b().vertex());var c=vertices.get(face.c().vertex());
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),u=c.x()-a.x(),v=c.y()-a.y(),w=c.z()-a.z();
        return Math.hypot(Math.hypot(y*w-z*v,z*u-x*w),x*v-y*u);
    }
    public synchronized void moveVertex(long id,double x,double y,double z){
        var old=vertices(state);var vertex=old.get(id);if(vertex==null)throw new IllegalArgumentException("Unknown vertex ID");
        var replacement=new Vertex(id,vertex.part(),x,y,z);var next=new HashMap<>(old);next.put(id,replacement);
        replaceVertices(old,next,Set.of(id),EditorMeshFrame.translation(x-old.get(id).x(),y-old.get(id).y(),z-old.get(id).z()));
    }
    /** Atomic global-axis translation of native IDs; no coordinate-based welding or partial edit. */
    /** Align a selected anchor to a stationary native vertex without welding their identities. */
    public synchronized EditPreview previewVertexSnap(Observation observation,Collection<Long> ids,long anchorId,long targetId) {
        if (!isCurrent(observation)) throw new IllegalArgumentException("Pick the snap target again after a native revision change");
        if (ids.isEmpty() || ids.size()>MAX_VERTICES) throw new IllegalArgumentException("Select native vertices to snap");
        var selected=Set.copyOf(ids);
        if (!selected.contains(anchorId) || selected.contains(targetId)) throw new IllegalArgumentException("Pick an anchor in the selection and a target outside it");
        var old=indexVertices(state.vertices());
        if (!old.keySet().containsAll(selected) || !old.containsKey(targetId)) throw new IllegalArgumentException("Unknown native snap vertex");
        var anchor=old.get(anchorId);var target=old.get(targetId);
        double dx=target.x()-anchor.x(),dy=target.y()-anchor.y(),dz=target.z()-anchor.z();
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) throw new IllegalArgumentException("Snap translation exceeds native coordinate range");
        if (dx==0 && dy==0 && dz==0) throw new IllegalArgumentException("Anchor and target are already aligned");
        var next=new HashMap<>(old);
        for (long id:selected) {
            var v=old.get(id);
            next.put(id,id==anchorId?new Vertex(id,v.part(),target.x(),target.y(),target.z())
                    :new Vertex(id,v.part(),snapCoordinate(v.x(),anchor.x(),target.x()),
                            snapCoordinate(v.y(),anchor.y(),target.y()),snapCoordinate(v.z(),anchor.z(),target.z())));
        }
        var after=replacementState(old,next,selected);
        for (var face:state.faces()) {
            int count=(selected.contains(face.a().vertex())?1:0)+(selected.contains(face.b().vertex())?1:0)+(selected.contains(face.c().vertex())?1:0);
            if (count==0 || count==3 || area(face,old)==0) continue;
            if (!(dot(EditorMeshNormals.face(triangle(face,old)),EditorMeshNormals.face(triangle(face,next)))>0))
                throw new IllegalArgumentException("Snapping would reverse a native face");
        }
        var translation=EditorMeshFrame.translation(dx,dy,dz);
        after=transformFrames(state,after,selected,part->translation);
        return new EditPreview(this,state,after,revision,"Snap v"+anchorId+" to v"+targetId+" / "+selected.size()+" selected");
    }
    private static double snapCoordinate(double value,double anchor,double target) {
        return target==anchor ? value : target+(value-anchor);
    }
    public synchronized void translateVertices(Collection<Long> ids,double dx,double dy,double dz){
        Objects.requireNonNull(ids);
        if(ids.isEmpty()||ids.size()>MAX_VERTICES||!Double.isFinite(dx)||!Double.isFinite(dy)||!Double.isFinite(dz))
            throw new IllegalArgumentException("Invalid native translation");
        var selected=new HashSet<>(ids);var old=vertices(state);var next=new HashMap<>(old);
        for(var id:selected){
            var v=old.get(id);if(v==null)throw new IllegalArgumentException("Unknown vertex ID");
            next.put(id,new Vertex(id,v.part(),v.x()+dx,v.y()+dy,v.z()+dz));
        }
        replaceVertices(old,next,selected,EditorMeshFrame.translation(dx,dy,dz));
    }
    public synchronized EditPreview previewBindMaterial(long faceId,EditorMeshMaterial material){
        Objects.requireNonNull(material);if(material.key().isEmpty())throw new IllegalArgumentException("Cannot bind an unnamed material");
        var materials=new ArrayList<>(state.materials());var existing=materials.stream().filter(m->m.key().equals(material.key())).findFirst();
        if(existing.isPresent()&&!existing.get().equals(material))throw new IllegalArgumentException("Material identity collision");
        if(existing.isEmpty())materials.add(material);
        var faces=new ArrayList<Face>();boolean found=false;for(var face:state.faces())if(face.id()==faceId){found=true;faces.add(new Face(face.id(),face.part(),face.a(),face.b(),face.c(),material.key()));}else faces.add(face);
        if(!found)throw new IllegalArgumentException("Unknown native face");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,materials,state.glbSource(),state.frames(),state.quads());
        if(after.equals(state))throw new IllegalArgumentException("Face already uses this material");
        return new EditPreview(this,state,after,revision,"BIND MATERIAL / face "+faceId+" / "+material.name());
    }
    public enum UvTransform { MOVE, ROTATE, SCALE }
    public synchronized EditPreview previewFixFaceStretch(long faceId){
        var material=boundPaintMaterial(faceId);
        var faces=new ArrayList<>(state.faces());
        for(int i=0;i<faces.size();i++)if(faces.get(i).id()==faceId){
            var face=faces.get(i);
            faces.set(i,EditorUvDensity.correct(face,triangle(face,indexVertices(state.vertices())),material.width(),material.height()));
            var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,state.materials(),
                state.glbSource(),state.frames(),state.quads());
            return new EditPreview(this,state,after,revision,"FIX UV STRETCH / face "+faceId);
        }
        throw new IllegalArgumentException("Unknown native face");
    }
    /** Paint the selected face's shared base-color texture in one reviewable native edit. */
    public synchronized EditPreview previewPencil(long faceId,Collection<Integer> texels,int color) {
        return previewPencil(faceId,null,texels,color);
    }
    public synchronized EditPreview previewPencil(long faceId,String layer,Collection<Integer> texels,int color) {
        return previewPencil(faceId,layer,texels,color,EditorUvTile.ORIGIN);
    }
    public synchronized EditPreview previewPencil(long faceId,String layer,Collection<Integer> texels,int color,EditorUvTile tile) {
        var face=state.faces().stream().filter(f->f.id()==faceId).findFirst().orElseThrow();
        var material=state.materials().stream().filter(m->m.key().equals(face.material())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Bind a PNG before painting"));
        if(texels.isEmpty() || texels.size()>65536)throw new IllegalArgumentException("Stroke exceeds texel budget");
        if(!tile.equals(tile.forMaterial(material)))throw new IllegalArgumentException("Clamped texture axis requires tile zero");
        var inside=new ArrayList<Integer>();
        for(int p:texels){
            if(p<0 || p>=material.width()*material.height())throw new IllegalArgumentException("Paint outside texture");
            double u=tile.nativeU((p%material.width()+.5)/material.width(),material);
            double v=tile.nativeV((p/material.width()+.5)/material.height(),material);
            if(EditorUvTile.contains(face,u,v))inside.add(p);
        }
        if(inside.isEmpty())throw new IllegalArgumentException("No texel centers inside the selected face");
        var painted=material.pencil(material.key(),layer==null?material.layers().get(0).id():layer,inside,color);
        return previewMaterialChange(material,painted,"BASE COLOR PENCIL / "+inside.size()+" texels");
    }
    private EditPreview previewMaterialChange(EditorMeshMaterial material,EditorMeshMaterial painted,String label){
        if(painted==material)throw new IllegalArgumentException("No paint layer change");
        boolean fork=false;
        if(state.glbSource()!=null)try{fork=state.glbSource().nativeMaterials().contains(material);}
        catch(IOException invalid){throw new UncheckedIOException(invalid);}
        String key=fork?paintKey(material.key()):material.key();
        var materials=new ArrayList<>(state.materials());
        List<Face> faces=state.faces();
        if(fork){
            materials.add(painted.withKey(key));
            faces=faces.stream().map(f->f.material().equals(material.key())?
                new Face(f.id(),f.part(),f.a(),f.b(),f.c(),key):f).toList();
        }else materials.set(materials.indexOf(material),painted);
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,materials,
            state.glbSource(),state.frames(),state.quads());
        return new EditPreview(this,state,after,revision,label);
    }
    private String paintKey(String original){
        String prefix="paint-"+EditorChange.fingerprint(original).substring(0,32);
        var keys=new HashSet<String>();
        for(var m:state.materials())keys.add(m.key());
        String key=prefix;
        for(int n=2;keys.contains(key);n++)key=prefix+"-"+n;
        return key;
    }
    private EditorMeshMaterial boundPaintMaterial(long faceId){
        var face=state.faces().stream().filter(f->f.id()==faceId).findFirst().orElseThrow();
        return state.materials().stream().filter(m->m.key().equals(face.material())).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Select a bound face"));
    }
    public synchronized EditPreview previewPaintStyle(long faceId,EditorMeshMaterial.PaintStyle style){
        var material=boundPaintMaterial(faceId);
        return previewMaterialChange(material,material.withStyle(style),"BASE COLOR / "+style.blend()+" / "+style.opacity()+"%");
    }
    public enum LayerEdit { ADD, CONFIGURE, UP, DOWN, REMOVE }
    public synchronized EditPreview previewPaintLayer(long faceId,String id,LayerEdit edit,String name,EditorMeshMaterial.PaintStyle style){
        var material=boundPaintMaterial(faceId);
        var stack=material.stack();
        var next=switch(edit){
            case ADD -> stack.add(id,name);
            case CONFIGURE -> stack.configure(id,name,style);
            case UP -> stack.move(id,1);
            case DOWN -> stack.move(id,-1);
            case REMOVE -> stack.remove(id);
        };
        return previewMaterialChange(material,material.withStack(next),"BASE COLOR / "+edit);
    }
    /** UV islands follow actual half-edge twins, authored seams and material boundaries. */
    private int[] uvFaces(long faceId,boolean island){
        int start=-1;
        for(int i=0;i<state.faces().size();i++)if(state.faces().get(i).id()==faceId){start=i;break;}
        if(start<0)throw new IllegalArgumentException("Unknown native UV face");
        if(!island)return new int[]{start};
        var graph=topology(state);
        int[] queue=new int[state.faces().size()];int count=1;queue[0]=start;
        var selected=new BitSet(queue.length);selected.set(start);
        for(int head=0;head<count;head++){
            int index=queue[head];var face=state.faces().get(index);
            for(int i=0;i<3;i++){
                var edge=graph.edges().get(index*3+i);
                if(edge.twin()<0 || edge.uvSeam())continue;
                int other=graph.edges().get(edge.twin()).face();
                if(!selected.get(other) && face.material().equals(state.faces().get(other).material())){
                    selected.set(other);queue[count++]=other;
                }
            }
        }
        return Arrays.copyOf(queue,count);
    }
    public synchronized EditPreview previewBalanceIsland(long faceId){
        var material=boundPaintMaterial(faceId);
        int[] island=uvFaces(faceId,true);
        var vertices=indexVertices(state.vertices());
        var faces=EditorUvDensity.balance(state.faces(),island,f->triangle(f,vertices),material.width(),material.height());
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,state.materials(),
            state.glbSource(),state.frames(),state.quads());
        return new EditPreview(this,state,after,revision,"BALANCE UV ISLAND / "+island.length+" faces");
    }
    public synchronized EditPreview previewUvTransform(long faceId,boolean island,UvTransform operation,double first,double second,boolean origin){
        Objects.requireNonNull(operation);
        if(!Double.isFinite(first)||!Double.isFinite(second)||Math.abs(first)>1_000_000||Math.abs(second)>1_000_000
            ||operation==UvTransform.SCALE&&(Math.abs(first)<.000001||Math.abs(second)<.000001)
            ||operation==UvTransform.ROTATE&&Math.abs(first)>36000)throw new IllegalArgumentException("Invalid native UV transform");
        if(operation==UvTransform.MOVE&&first==0&&second==0||operation==UvTransform.SCALE&&first==1&&second==1||operation==UvTransform.ROTATE&&Math.IEEEremainder(first,360)==0)throw new IllegalArgumentException("UV transform does not change selected corners");
        int[] selected=uvFaces(faceId,island);
        // Bounds center is stable under triangle tessellation and per-corner UV seam duplication.
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=Double.NEGATIVE_INFINITY,maxV=maxU;
        for(int i:selected)for(var c:List.of(state.faces().get(i).a(),state.faces().get(i).b(),state.faces().get(i).c())){minU=Math.min(minU,c.u());maxU=Math.max(maxU,c.u());minV=Math.min(minV,c.v());maxV=Math.max(maxV,c.v());}
        double pu=origin?0:minU*.5+maxU*.5,pv=origin?0:minV*.5+maxV*.5;
        double radians=StrictMath.toRadians(first),sin=StrictMath.sin(radians),cos=StrictMath.cos(radians);var faces=new ArrayList<>(state.faces());
        for(int i:selected){
            var f=state.faces().get(i);var corners=new ArrayList<Corner>(3);
            for(var c:List.of(f.a(),f.b(),f.c())){double u=c.u()-pu,v=c.v()-pv;
                if(operation==UvTransform.MOVE){u=c.u()+first;v=c.v()+second;}
                else if(operation==UvTransform.ROTATE){double nextU=cos*u-sin*v;v=sin*u+cos*v+pv;u=nextU+pu;}
                else{u=u*first+pu;v=v*second+pv;}
                if(!Double.isFinite(u)||!Double.isFinite(v)||Math.abs(u)>1e9||Math.abs(v)>1e9)throw new IllegalArgumentException("UV transform exceeds native coordinate budget");corners.add(new Corner(c.vertex(),u,v,c.normal()));
            }
            faces.set(i,new Face(f.id(),f.part(),corners.get(0),corners.get(1),corners.get(2),f.material()));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,state.materials(),state.glbSource(),state.frames(),state.quads());
        if(after.equals(state))throw new IllegalArgumentException("UV transform changes no native corner");
        return new EditPreview(this,state,after,revision,"UV "+operation+" / "+selected.length+" faces / "+(origin?"UV origin":"UV bounds center"));
    }
    public synchronized void translateLocalVertices(Collection<Long> ids,int axis,double amount){
        if(ids==null||ids.isEmpty()||axis<0||axis>2||!Double.isFinite(amount)||amount==0)throw new IllegalArgumentException("Invalid local translation");var selected=new HashSet<>(ids);var old=vertices(state);var next=new HashMap<>(old);for(long id:selected)if(!old.containsKey(id))throw new IllegalArgumentException("Unknown vertex ID");var directions=new HashMap<String,EditorMeshPipeline.Normal>();
        for(var v:state.vertices())if(selected.contains(v.id())){var direction=directions.computeIfAbsent(v.part(),part->frame(state,part).vector(axis));next.put(v.id(),new Vertex(v.id(),v.part(),v.x()+direction.x()*amount,v.y()+direction.y()*amount,v.z()+direction.z()*amount));}
        var after=replacementState(old,next,selected);state=transformFrames(state,after,selected,part->{var direction=directions.get(part);return EditorMeshFrame.translation(direction.x()*amount,direction.y()*amount,direction.z()*amount);});revision++;
    }
    public enum Transform { ROTATE, SCALE_AXIS, SCALE_UNIFORM }
    /** Global axes, explicit origin/selection-centroid pivot. Candidate only; shared Apply owns mutation. */
    public synchronized EditPreview previewTransform(Collection<Long> ids,Transform operation,int axis,double amount,boolean origin){
        Objects.requireNonNull(ids);Objects.requireNonNull(operation);
        if(ids.isEmpty()||ids.size()>MAX_VERTICES||axis<0||axis>2||!Double.isFinite(amount)
                ||(operation==Transform.ROTATE?Math.abs(amount)>36000:amount<.000001||amount>1000000))
            throw new IllegalArgumentException("Invalid native transform parameters");
        if(operation==Transform.ROTATE?Math.IEEEremainder(amount,360)==0:amount==1)throw new IllegalArgumentException("Transform does not change selected vertices");
        var selected=new HashSet<>(ids);var old=vertices(state);var next=new HashMap<>(old);
        for(long id:selected)if(!old.containsKey(id))throw new IllegalArgumentException("Unknown vertex ID");
        double px=0,py=0,pz=0;
        if(!origin)for(var v:state.vertices())if(selected.contains(v.id())){px+=v.x()/selected.size();py+=v.y()/selected.size();pz+=v.z()/selected.size();}
        double radians=StrictMath.toRadians(amount),sin=StrictMath.sin(radians),cos=StrictMath.cos(radians);
        for(var v:state.vertices())if(selected.contains(v.id())){
            double x=v.x()-px,y=v.y()-py,z=v.z()-pz;
            if(operation==Transform.ROTATE){
                if(axis==0){double yy=cos*y-sin*z;z=sin*y+cos*z;y=yy;}
                else if(axis==1){double xx=cos*x+sin*z;z=-sin*x+cos*z;x=xx;}
                else{double xx=cos*x-sin*y;y=sin*x+cos*y;x=xx;}
            }else{if(operation==Transform.SCALE_UNIFORM||axis==0)x*=amount;if(operation==Transform.SCALE_UNIFORM||axis==1)y*=amount;if(operation==Transform.SCALE_UNIFORM||axis==2)z*=amount;}
            next.put(v.id(),new Vertex(v.id(),v.part(),x+px,y+py,z+pz));
        }
        var after=replacementState(old,next,selected,n->operation==Transform.ROTATE?EditorMeshNormals.rotate(n,axis,radians):operation==Transform.SCALE_AXIS?EditorMeshNormals.scale(n,axis,amount):n);
        var delta=EditorMeshFrame.around(EditorMeshFrame.operation(operation,axis,amount),px,py,pz);after=transformFrames(state,after,selected,part->delta);
        if(after.equals(state))throw new IllegalArgumentException("Transform does not change selected vertices");
        return new EditPreview(this,state,after,revision,operation+" / "+selected.size()+" vertices / global "+"XYZ".charAt(axis)+" / "+amount+" / "+(origin?"world origin":"selection centroid"));
    }
    /** Surface-normal axes come from the active native face; no authored-normal shading value defines geometry axes. */
    public synchronized EditPreview previewTransform(Collection<Long> ids,Transform operation,int axis,double amount,boolean faceCenter,long activeFace){
        Objects.requireNonNull(ids);Objects.requireNonNull(operation);
        if(ids.isEmpty()||ids.size()>MAX_VERTICES||axis<0||axis>2||!Double.isFinite(amount)||(operation==Transform.ROTATE?Math.abs(amount)>36000:amount<.000001||amount>1000000))throw new IllegalArgumentException("Invalid native transform parameters");
        if(operation==Transform.ROTATE?Math.IEEEremainder(amount,360)==0:amount==1)throw new IllegalArgumentException("Transform does not change selected vertices");
        var basis=EditorMeshBasis.face(state,activeFace);var selected=new HashSet<>(ids);var old=vertices(state);var next=new HashMap<>(old);for(long id:selected)if(!old.containsKey(id))throw new IllegalArgumentException("Unknown vertex ID");
        double px=faceCenter?basis.x():0,py=faceCenter?basis.y():0,pz=faceCenter?basis.z():0;
        if(!faceCenter)for(var v:state.vertices())if(selected.contains(v.id())){px+=v.x()/selected.size();py+=v.y()/selected.size();pz+=v.z()/selected.size();}
        double radians=StrictMath.toRadians(amount),sin=StrictMath.sin(radians),cos=StrictMath.cos(radians);
        for(var v:state.vertices())if(selected.contains(v.id())){
            double a=basis.component(v.x()-px,v.y()-py,v.z()-pz,0),b=basis.component(v.x()-px,v.y()-py,v.z()-pz,1),c=basis.component(v.x()-px,v.y()-py,v.z()-pz,2);
            if(operation==Transform.ROTATE){if(axis==0){double value=cos*b-sin*c;c=sin*b+cos*c;b=value;}else if(axis==1){double value=cos*a+sin*c;c=-sin*a+cos*c;a=value;}else{double value=cos*a-sin*b;b=sin*a+cos*b;a=value;}}
            else{if(operation==Transform.SCALE_UNIFORM||axis==0)a*=amount;if(operation==Transform.SCALE_UNIFORM||axis==1)b*=amount;if(operation==Transform.SCALE_UNIFORM||axis==2)c*=amount;}
            next.put(v.id(),new Vertex(v.id(),v.part(),basis.world(a,b,c,0)+px,basis.world(a,b,c,1)+py,basis.world(a,b,c,2)+pz));
        }
        var after=replacementState(old,next,selected,n->basis.transformNormal(n,operation,axis,amount));var basisMatrix=new EditorMeshFrame(basis.tangent().x(),basis.bitangent().x(),basis.normal().x(),0,basis.tangent().y(),basis.bitangent().y(),basis.normal().y(),0,basis.tangent().z(),basis.bitangent().z(),basis.normal().z(),0);var delta=EditorMeshFrame.around(basisMatrix.multiply(EditorMeshFrame.operation(operation,axis,amount)).multiply(basisMatrix.inverse()),px,py,pz);after=transformFrames(state,after,selected,part->delta);if(after.equals(state))throw new IllegalArgumentException("Transform does not change selected vertices");
        return new EditPreview(this,state,after,revision,operation+" / "+selected.size()+" vertices / surface "+"TBN".charAt(axis)+" / face "+activeFace+" / "+amount+" / "+(faceCenter?"face center":"selection centroid"));
    }
    /** Local operations conjugate the exact persisted part frame, including original ancestor scale/shear. */
    public synchronized EditPreview previewLocalTransform(Collection<Long> ids,Transform operation,int axis,double amount,boolean partOrigin){
        Objects.requireNonNull(ids);Objects.requireNonNull(operation);if(ids.isEmpty()||ids.size()>MAX_VERTICES||axis<0||axis>2||!Double.isFinite(amount)||(operation==Transform.ROTATE?Math.abs(amount)>36000:amount<.000001||amount>1000000))throw new IllegalArgumentException("Invalid native local transform parameters");
        if(operation==Transform.ROTATE?Math.IEEEremainder(amount,360)==0:amount==1)throw new IllegalArgumentException("Transform does not change selected vertices");
        var selected=new HashSet<>(ids);var old=vertices(state);var next=new HashMap<>(old);for(long id:selected)if(!old.containsKey(id))throw new IllegalArgumentException("Unknown vertex ID");
        var counts=new LinkedHashMap<String,Integer>();for(var v:state.vertices())if(selected.contains(v.id()))counts.merge(v.part(),1,Integer::sum);var pivots=new HashMap<String,double[]>();for(var e:counts.entrySet()){var f=frame(state,e.getKey());pivots.put(e.getKey(),partOrigin?new double[]{f.ox(),f.oy(),f.oz()}:new double[3]);}
        if(!partOrigin)for(var v:state.vertices())if(selected.contains(v.id())){var p=pivots.get(v.part());int count=counts.get(v.part());p[0]+=v.x()/count;p[1]+=v.y()/count;p[2]+=v.z()/count;}
        var linear=new HashMap<String,EditorMeshFrame>();var deltas=new HashMap<String,EditorMeshFrame>();for(String part:counts.keySet()){var basis=frame(state,part).linear();var transform=basis.multiply(EditorMeshFrame.operation(operation,axis,amount)).multiply(basis.inverse());linear.put(part,transform);var p=pivots.get(part);deltas.put(part,EditorMeshFrame.around(transform,p[0],p[1],p[2]));}
        for(var v:state.vertices())if(selected.contains(v.id())){var p=pivots.get(v.part());var transform=linear.get(v.part());double x=v.x()-p[0],y=v.y()-p[1],z=v.z()-p[2];next.put(v.id(),new Vertex(v.id(),v.part(),transform.direction(x,y,z,0)+p[0],transform.direction(x,y,z,1)+p[1],transform.direction(x,y,z,2)+p[2]));}
        var after=replacementByPart(old,next,selected,(java.util.function.Function<String,java.util.function.UnaryOperator<EditorMeshPipeline.Normal>>)(part->n->operation==Transform.SCALE_UNIFORM?n:linear.get(part).normal(n,operation==Transform.ROTATE)));after=transformFrames(state,after,selected,deltas::get);if(after.equals(state))throw new IllegalArgumentException("Transform does not change selected vertices");
        return new EditPreview(this,state,after,revision,operation+" / local "+"XYZ".charAt(axis)+" / "+counts.size()+" parts / "+amount+" / "+(partOrigin?"part origins":"part selection centroids"));
    }
    private void replaceVertices(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected,EditorMeshFrame delta){
        state=transformFrames(state,replacementState(old,next,selected),selected,part->delta);revision++;
    }
    private State replacementState(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected){return replacementState(old,next,selected,n->n);}
    private State replacementState(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected,java.util.function.UnaryOperator<EditorMeshPipeline.Normal> transform){return replacementByPart(old,next,selected,part->transform);}
    private State replacementByPart(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected,java.util.function.Function<String,java.util.function.UnaryOperator<EditorMeshPipeline.Normal>> transforms){
        for(var face:state.faces())if(selected.contains(face.a().vertex())||selected.contains(face.b().vertex())||selected.contains(face.c().vertex())){
            double area=area(face,next);
            if(!Double.isFinite(area)||(area==0&&area(face,old)>0))throw new IllegalArgumentException("Vertex move degenerates a face");
        }
        var values=new ArrayList<Vertex>();for(var v:state.vertices())values.add(next.get(v.id()));
        var faces=new ArrayList<Face>(state.faces().size());
        for(var face:state.faces()){
            boolean touched=selected.contains(face.a().vertex())||selected.contains(face.b().vertex())||selected.contains(face.c().vertex());
            if(!touched||face.a().normal()==null&&face.b().normal()==null&&face.c().normal()==null){faces.add(face);continue;}
            var transform=transforms.apply(face.part());if(selected.contains(face.a().vertex())&&selected.contains(face.b().vertex())&&selected.contains(face.c().vertex()))faces.add(new Face(face.id(),face.part(),normalCorner(face.a(),transform.apply(face.a().normal())),normalCorner(face.b(),transform.apply(face.b().normal())),normalCorner(face.c(),transform.apply(face.c().normal())),face.material()));
            else faces.add(normalFace(face,EditorMeshNormals.transport(triangle(face,old),triangle(face,next))));
        }
        return new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),values,faces,state.materials(),state.glbSource(),state.frames(),state.quads());
    }
    /** Append a reviewed primitive without welding existing vertices or reassigning their IDs. */
    public synchronized void appendPrimitive(EditorMeshPrimitives.Parameters parameters){
        var mesh=EditorMeshPrimitives.preview(parameters);
        if((long)state.faces().size()+mesh.triangles().size()>EditorMeshPipeline.MAX_TRIANGLES)
            throw new IllegalArgumentException("Project triangle budget exceeded");
        var graph=EditorMeshTopology.build(mesh);
        if(graph.issueCount()!=0)throw new IllegalArgumentException("Invalid primitive topology");
        long nextVertex=state.vertices().stream().mapToLong(Vertex::id).max().orElse(-1)+1;
        long nextFace=state.faces().stream().mapToLong(Face::id).max().orElse(-1)+1;
        if(nextVertex<0||nextFace<0||nextVertex>Long.MAX_VALUE-graph.vertices().size()||nextFace>Long.MAX_VALUE-mesh.triangles().size())
            throw new IllegalArgumentException("Primitive ID budget exceeded");
        String part="primitive-"+nextFace+"-"+parameters.kind().name().toLowerCase(Locale.ROOT);
        var vertices=new ArrayList<>(state.vertices());var faces=new ArrayList<>(state.faces());
        for(var v:graph.vertices())vertices.add(new Vertex(nextVertex++,part,v.x(),v.y(),v.z()));
        long base=nextVertex-graph.vertices().size();
        var quads=new ArrayList<>(state.quads());
        for(var pair:mesh.quads())quads.add(new EditorMeshQuad(nextFace+pair.first(),nextFace+pair.second()));
        for(int i=0;i<mesh.triangles().size();i++){
            var t=mesh.triangles().get(i);var uv=List.of(t.a(),t.b(),t.c());var corners=new ArrayList<Corner>();
            for(int j=0;j<3;j++)corners.add(new Corner(base+graph.edges().get(i*3+j).origin(),uv.get(j).u(),uv.get(j).v(),uv.get(j).normal()));
            faces.add(new Face(nextFace++,part,corners.get(0),corners.get(1),corners.get(2)));
        }
        state=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),vertices,faces,state.materials(),state.glbSource(),state.frames(),quads);revision++;
    }
    private static void write(State state,EditorModelWorkspace workspace,OutputStream out)throws IOException{
        boolean normals=state.faces().stream().anyMatch(f->f.a().normal()!=null||f.b().normal()!=null||f.c().normal()!=null);
        int paintEncoding=EditorMeshMaterial.paintEncoding(state.materials());
        int version=paintEncoding>1?6+paintEncoding:workspace!=null?7:!state.quads().isEmpty()?6:!state.frames().isEmpty()?5:state.glbSource()!=null?4:state.materials().isEmpty()?(normals?2:1):3;
        var data=new DataOutputStream(out);data.writeInt(MAGIC);data.writeInt(version);
        data.writeUTF(state.sceneId());data.writeUTF(state.sourceFingerprint());data.writeUTF(state.name());data.writeLong(state.sourceBytes());
        data.writeInt(state.vertices().size());for(var v:state.vertices()){
            data.writeLong(v.id());data.writeUTF(v.part());data.writeDouble(v.x());data.writeDouble(v.y());data.writeDouble(v.z());
        }
        data.writeInt(state.faces().size());for(var f:state.faces()){
            data.writeLong(f.id());data.writeUTF(f.part());for(var c:List.of(f.a(),f.b(),f.c())){data.writeLong(c.vertex());data.writeDouble(c.u());data.writeDouble(c.v());if(version>=2){data.writeBoolean(c.normal()!=null);if(c.normal()!=null){data.writeDouble(c.normal().x());data.writeDouble(c.normal().y());data.writeDouble(c.normal().z());}}}if(version>=3)data.writeUTF(f.material());
        }
        if(version>=3)EditorMeshMaterial.writeTable(data,state.materials());
        if(version>=4){byte[] glb=state.glbSource()==null?new byte[0]:state.glbSource().original();data.writeInt(glb.length);data.write(glb);}if(version>=5)EditorMeshFrame.write(data,state.frames());if(version>=6){data.writeInt(state.quads().size());for(var pair:state.quads()){data.writeLong(pair.first());data.writeLong(pair.second());}}
        if(version>=8){EditorMeshMaterial.writePaintEdits(data,state.materials(),paintEncoding);data.writeBoolean(workspace!=null);}
        if(version>=7&&workspace!=null)workspace.write(data);
        data.flush();
    }
    private static int count(DataInputStream data,int max)throws IOException{int count=data.readInt();if(count<0||count>max)throw new IOException("Topology count budget exceeded");return count;}
    private static State read(Path path)throws IOException{return decode(EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES));}
    private record Document(State state,EditorModelWorkspace workspace) {}
    public static State decode(byte[] bytes)throws IOException{return decodeDocument(bytes).state();}
    private static Document decodeDocument(byte[] bytes)throws IOException{
        if(bytes.length>EditorDocumentStore.MAX_BYTES)throw new IOException("Topology file exceeds byte budget");
        try{
            var data=new DataInputStream(new ByteArrayInputStream(bytes));
            if(data.readInt()!=MAGIC)throw new IOException("Invalid native topology magic");int version=data.readInt();if(version<1||version>VERSION)throw new IOException("Unsupported native topology version");
            String id=data.readUTF(),source=data.readUTF(),name=data.readUTF();long size=data.readLong();
            int count=count(data,MAX_VERTICES);var vertices=new ArrayList<Vertex>();
            for(int i=0;i<count;i++)vertices.add(new Vertex(data.readLong(),data.readUTF(),data.readDouble(),data.readDouble(),data.readDouble()));
            count=count(data,EditorMeshPipeline.MAX_TRIANGLES);var faces=new ArrayList<Face>();
            for(int i=0;i<count;i++){
                long face=data.readLong();String part=data.readUTF();var corners=new ArrayList<Corner>();
                for(int j=0;j<3;j++){
                    long vertex=data.readLong();double u=data.readDouble(),v=data.readDouble();EditorMeshPipeline.Normal normal=null;
                    if(version>=2){int present=data.readUnsignedByte();if(present>1)throw new IOException("Invalid native corner-normal flag");if(present==1)normal=new EditorMeshPipeline.Normal(data.readDouble(),data.readDouble(),data.readDouble());}
                    corners.add(new Corner(vertex,u,v,normal));
                }
                faces.add(new Face(face,part,corners.get(0),corners.get(1),corners.get(2),version>=3?data.readUTF():""));
            }
            var materials=version>=3?EditorMeshMaterial.readTable(data):List.<EditorMeshMaterial>of();
            EditorGlbReader.Document glb=null;if(version>=4){int length=data.readInt();if((length<28&&!(version>=5&&length==0))||length>4_000_000)throw new IOException("Native GLB source byte budget exceeded");if(length>0){byte[] original=data.readNBytes(length);if(original.length!=length)throw new EOFException("Truncated GLB source");glb=EditorGlbReader.read(original);glb.requireEditable();}}
            var frames=version>=5?EditorMeshFrame.read(data):Map.<String,EditorMeshFrame>of();
            var quads=new ArrayList<EditorMeshQuad>();if(version>=6){int pairs=count(data,faces.size()/2);for(int i=0;i<pairs;i++)quads.add(new EditorMeshQuad(data.readLong(),data.readLong()));}
            if(version>=8)materials=EditorMeshMaterial.readPaintEdits(data,materials,version-6);
            var state=new State(id,source,name,size,vertices,faces,materials,glb,frames,quads);
            var workspace=version>=7&&(version==7||data.readBoolean())?EditorModelWorkspace.read(data,vertices.size()):null;
            if(workspace!=null && workspace.forState(state)!=workspace)throw new IOException("Workspace references missing native components");
            if(data.read()!=-1)throw new IOException("Trailing native topology data");
            return new Document(state,workspace);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid native topology",invalid);}
    }
    private State owned(Path file)throws IOException{
        State value=read(file);if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint())||!Objects.equals(sourceGlb,value.glbSource()))throw new IOException("Foreign topology project/source provenance");return value;
    }
    public synchronized byte[] snapshotBytes()throws IOException{
        return snapshotBytes(savedWorkspace());
    }
    private byte[] snapshotBytes(EditorModelWorkspace workspace)throws IOException{
        return snapshotBytes(state,workspace);
    }
    static byte[] snapshotBytes(State state,EditorModelWorkspace workspace)throws IOException{
        var buffer=new ByteArrayOutputStream();var bounded=new FilterOutputStream(buffer){
            int count;
            private void reserve(int length)throws IOException{if(length<0||length>EditorDocumentStore.MAX_BYTES-count)throw new IOException("Topology output exceeds byte budget");count+=length;}
            public void write(int value)throws IOException{reserve(1);out.write(value);}
            public void write(byte[] bytes,int offset,int length)throws IOException{reserve(length);out.write(bytes,offset,length);}
        };
        write(state,workspace,bounded);return buffer.toByteArray();
    }
    public synchronized String diskFingerprint(){return diskFingerprint;}
    public synchronized EditorModelWorkspace savedWorkspace(){return savedWorkspace==null||cleanState==state?savedWorkspace:savedWorkspace.forState(state);}
    public synchronized void save()throws IOException{save(savedWorkspace);}
    public synchronized void save(EditorModelWorkspace workspace)throws IOException{
        State expected=state;
        var view=workspace==null?null:workspace.forState(state);
        byte[] bytes=snapshotBytes(view);String previous=diskFingerprint;
        EditorDocumentStore.saveIfUnchanged(path,out->out.write(bytes),this::owned,file->{
            var prepared=decodeDocument(EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES));
            if(!expected.equals(prepared.state()) || !Objects.equals(view,prepared.workspace()))throw new IOException("Native geometry or workspace readback differs");
        },previous);
        diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);
        cleanState=expected;savedWorkspace=view;
    }
    public static EditorTopologyProject open(Path path)throws IOException{
        EditorDocumentStore.recoverPending(path,EditorTopologyProject::read);
        byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);
        var document=decodeDocument(bytes);var project=new EditorTopologyProject(path,document.state());project.savedWorkspace=document.workspace();project.diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);return project;
    }
    public synchronized void reopen()throws IOException{
        EditorDocumentStore.recoverPending(path,this::owned);byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);var document=decodeDocument(bytes);State value=document.state();
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint())||!Objects.equals(sourceGlb,value.glbSource()))throw new IOException("Foreign topology project/source provenance");
        restore(value);diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);cleanState=value;savedWorkspace=document.workspace();
    }
    public synchronized void recoverPrevious()throws IOException{EditorDocumentStore.restorePrevious(path,this::owned);reopen();}
}
