package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Authoritative human mesh authoring state. Triangle buffers and adjacency are
 * derived from stable indexed vertices/corner UVs. No game scene is duplicated.
 */
public final class EditorTopologyProject implements EditorProject<EditorTopologyProject.State> {
    private static final int MAGIC=0x42544f50,VERSION=5,MAX_VERTICES=EditorMeshPipeline.MAX_TRIANGLES*3;
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
    public record State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,Map<String,EditorMeshFrame> frames){
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,materials,glbSource,Map.of());}
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces,List<EditorMeshMaterial> materials){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,materials,null);}
        public State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces){this(sceneId,sourceFingerprint,name,sourceBytes,vertices,faces,List.of());}
        public State {
            text(sceneId);text(name);frames=frames.isEmpty()?Map.of():EditorMeshFrame.validate(frames,vertices.stream().map(Vertex::part).toList());vertices=List.copyOf(vertices);faces=List.copyOf(faces);materials=List.copyOf(materials);EditorMeshMaterial.validateTable(materials);var materialKeys=new HashSet<String>();for(var material:materials)materialKeys.add(material.key());
            if(sourceFingerprint==null||!sourceFingerprint.matches("[0-9a-f]{64}")||sourceBytes<0||vertices.size()>MAX_VERTICES||faces.size()>EditorMeshPipeline.MAX_TRIANGLES)
                throw new IllegalArgumentException("Invalid topology state");
            if(glbSource!=null){if(!glbSource.fingerprint().equals(sourceFingerprint))throw new IllegalArgumentException("Foreign GLB source provenance");try{for(var original:glbSource.nativeMaterials())if(!materials.contains(original))throw new IllegalArgumentException("Original GLB material provenance must be retained");}catch(IOException invalid){throw new IllegalArgumentException("Invalid GLB source materials",invalid);}}
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
        return new EditorTopologyProject(path,new State("mesh-"+UUID.randomUUID(),sourceFingerprint,mesh.name(),mesh.sourceBytes(),vertices,faces,mesh.materials(),mesh.glbSource(),mesh.frames()));
    }
    public static EditorTopologyProject fromGlb(String name,byte[] original,Path path)throws IOException{var document=EditorGlbReader.read(original);document.requireEditable();try{var project=fromMesh(document.mesh(name),document.fingerprint(),path);project.snapshotBytes();return project;}catch(IllegalArgumentException invalid){throw new IOException("GLB topology exceeds supported native authoring capabilities",invalid);}}
    public String id(){return id;}
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

    private static State framed(State state,Map<String,EditorMeshFrame> frames){return new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),state.vertices(),state.faces(),state.materials(),state.glbSource(),frames);}
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
        public State snapshot(){return state;}
        public long revision(){return revision;}
        public EditorMeshPipeline.Mesh mesh(){return EditorTopologyProject.mesh(state);}
        public EditorMeshTopology topology(){return EditorTopologyProject.topology(state);}
    }
    public synchronized Observation observe(){return new Observation(this,state,revision);}
    public synchronized boolean isCurrent(Observation observation){return observation!=null&&observation.owner==this&&observation.state==state&&observation.revision==revision;}
    public record SavedReview(byte[] bytes,String diskFingerprint){
        public SavedReview {bytes=bytes.clone();}
        @Override public byte[] bytes(){return bytes.clone();}
    }
    /** The saved bytes and revision check share the same native monitor as edits. */
    public synchronized SavedReview saveReviewed(Observation observation)throws IOException{
        if(!isCurrent(observation))throw new IOException("Native mesh changed since review; review again before publishing");
        save();return new SavedReview(snapshotBytes(),diskFingerprint);
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
        return new EditorMeshPipeline.Mesh(state.name(),triangles,state.sourceBytes(),ids,state.materials(),state.glbSource(),state.faces().stream().map(Face::id).toList(),visibleFrames);
    }
    private static Map<Long,Vertex> indexVertices(List<Vertex> values){var map=new HashMap<Long,Vertex>();for(var v:values)map.put(v.id(),v);return map;}
    private static EditorMeshPipeline.Triangle triangle(Face f,Map<Long,Vertex> values){return new EditorMeshPipeline.Triangle(f.part(),corner(f.a(),values),corner(f.b(),values),corner(f.c(),values),f.material());}
    private static Corner normalCorner(Corner c,EditorMeshPipeline.Normal n){return new Corner(c.vertex(),c.u(),c.v(),n);}
    private static Face normalFace(Face f,EditorMeshPipeline.Triangle t){return new Face(f.id(),f.part(),normalCorner(f.a(),t.a().normal()),normalCorner(f.b(),t.b().normal()),normalCorner(f.c(),t.c().normal()),f.material());}
    public synchronized EditorMeshPipeline.Mesh mesh(){return mesh(state);}
    public synchronized EditorMeshTopology topology(){return topology(state);}
    private static EditorMeshTopology topology(State state){
        var positions=new ArrayList<EditorMeshTopology.Vertex>();var ids=new HashMap<Long,Integer>();
        for(var v:state.vertices()){ids.put(v.id(),positions.size());positions.add(new EditorMeshTopology.Vertex(v.part(),v.x(),v.y(),v.z()));}
        int[] origins=new int[state.faces().size()*3];int i=0;
        for(var face:state.faces())for(var corner:List.of(face.a(),face.b(),face.c()))origins[i++]=ids.get(corner.vertex());
        return EditorMeshTopology.buildIndexed(mesh(state),positions,origins);
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
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces,state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),newVertices));
        if(!topology(after).manifold())throw new IllegalArgumentException("Model edit would introduce invalid topology");
        return new EditPreview(this,state,after,revision,operation+" face "+faceId+" / +3 vertices, +6 triangles");
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
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces,state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),newVertices));
        if(!topology(after).manifold())throw new IllegalArgumentException("Weld would leave invalid topology; select complete seams or repair degenerate faces first");
        return new EditPreview(this,state,after,revision,"WELD COINCIDENT / -"+replacements.size()+" vertices, -"+removed+" collapsed triangles");
    }
    private static Corner weldCorner(Corner value,Map<Long,Long> replacements){return new Corner(replacements.getOrDefault(value.vertex(),value.vertex()),value.u(),value.v(),value.normal());}
    /** Remove invalid zero/non-finite-area triangles explicitly, retaining every surviving stable ID/UV. */
    public synchronized EditPreview previewRemoveDegenerates(){
        var positions=vertices(state);var remaining=new ArrayList<Face>();
        for(var face:state.faces()){double size=area(face,positions);if(size>0&&Double.isFinite(size))remaining.add(face);}
        int removed=state.faces().size()-remaining.size();if(removed==0)throw new IllegalArgumentException("No degenerate native faces to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),remaining,state.materials(),state.glbSource(),state.frames());
        if(!topology(after).manifold())throw new IllegalArgumentException("Other topology errors remain; repair requires a valid resulting mesh");
        return new EditPreview(this,state,after,revision,"REMOVE DEGENERATES / -"+removed+" triangles; vertex IDs retained");
    }
    public synchronized EditPreview previewRemoveUnusedVertices(){
        var used=new HashSet<Long>();for(var f:state.faces()){used.add(f.a().vertex());used.add(f.b().vertex());used.add(f.c().vertex());}
        var retained=state.vertices().stream().filter(v->used.contains(v.id())).toList();int removed=state.vertices().size()-retained.size();
        if(removed==0)throw new IllegalArgumentException("No unused native vertices to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),retained,state.faces(),state.materials(),state.glbSource(),EditorMeshFrame.retain(state.frames(),retained));
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
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,materials,state.glbSource(),state.frames());
        if(after.equals(state))throw new IllegalArgumentException("Face already uses this material");
        return new EditPreview(this,state,after,revision,"BIND MATERIAL / face "+faceId+" / "+material.name());
    }
    public enum UvTransform { MOVE, ROTATE, SCALE }
    /** UV islands follow actual half-edge twins, authored seams and material boundaries. */
    public synchronized EditPreview previewUvTransform(long faceId,boolean island,UvTransform operation,double first,double second,boolean origin){
        Objects.requireNonNull(operation);
        if(!Double.isFinite(first)||!Double.isFinite(second)||Math.abs(first)>1_000_000||Math.abs(second)>1_000_000
            ||operation==UvTransform.SCALE&&(Math.abs(first)<.000001||Math.abs(second)<.000001)
            ||operation==UvTransform.ROTATE&&Math.abs(first)>36000)throw new IllegalArgumentException("Invalid native UV transform");
        if(operation==UvTransform.MOVE&&first==0&&second==0||operation==UvTransform.SCALE&&first==1&&second==1||operation==UvTransform.ROTATE&&Math.IEEEremainder(first,360)==0)throw new IllegalArgumentException("UV transform does not change selected corners");
        int start=-1;for(int i=0;i<state.faces().size();i++)if(state.faces().get(i).id()==faceId){start=i;break;}
        if(start<0)throw new IllegalArgumentException("Unknown native UV face");
        boolean[] selected=new boolean[state.faces().size()];selected[start]=true;int count=1;
        if(island){
            var graph=topology(state);int[] queue=new int[state.faces().size()];queue[0]=start;int head=0;
            while(head<count){int face=queue[head++];var source=state.faces().get(face);for(int i=0;i<3;i++){
                var edge=graph.edges().get(face*3+i);if(edge.twin()<0||edge.uvSeam())continue;int other=graph.edges().get(edge.twin()).face();
                if(!selected[other]&&source.material().equals(state.faces().get(other).material())){selected[other]=true;queue[count++]=other;}
            }}
        }
        // Bounds center is stable under triangle tessellation and per-corner UV seam duplication.
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=Double.NEGATIVE_INFINITY,maxV=maxU;
        for(int i=0;i<selected.length;i++)if(selected[i])for(var c:List.of(state.faces().get(i).a(),state.faces().get(i).b(),state.faces().get(i).c())){minU=Math.min(minU,c.u());maxU=Math.max(maxU,c.u());minV=Math.min(minV,c.v());maxV=Math.max(maxV,c.v());}
        double pu=origin?0:minU*.5+maxU*.5,pv=origin?0:minV*.5+maxV*.5;
        double radians=StrictMath.toRadians(first),sin=StrictMath.sin(radians),cos=StrictMath.cos(radians);var faces=new ArrayList<Face>(state.faces().size());
        for(int i=0;i<selected.length;i++){
            var f=state.faces().get(i);if(!selected[i]){faces.add(f);continue;}var corners=new ArrayList<Corner>(3);
            for(var c:List.of(f.a(),f.b(),f.c())){double u=c.u()-pu,v=c.v()-pv;
                if(operation==UvTransform.MOVE){u=c.u()+first;v=c.v()+second;}
                else if(operation==UvTransform.ROTATE){double nextU=cos*u-sin*v;v=sin*u+cos*v+pv;u=nextU+pu;}
                else{u=u*first+pu;v=v*second+pv;}
                if(!Double.isFinite(u)||!Double.isFinite(v)||Math.abs(u)>1e9||Math.abs(v)>1e9)throw new IllegalArgumentException("UV transform exceeds native coordinate budget");corners.add(new Corner(c.vertex(),u,v,c.normal()));
            }
            faces.add(new Face(f.id(),f.part(),corners.get(0),corners.get(1),corners.get(2),f.material()));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),faces,state.materials(),state.glbSource(),state.frames());
        if(after.equals(state))throw new IllegalArgumentException("UV transform changes no native corner");
        return new EditPreview(this,state,after,revision,"UV "+operation+" / "+count+" faces / "+(origin?"UV origin":"UV bounds center"));
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
        return new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),values,faces,state.materials(),state.glbSource(),state.frames());
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
        for(int i=0;i<mesh.triangles().size();i++){
            var t=mesh.triangles().get(i);var uv=List.of(t.a(),t.b(),t.c());var corners=new ArrayList<Corner>();
            for(int j=0;j<3;j++)corners.add(new Corner(base+graph.edges().get(i*3+j).origin(),uv.get(j).u(),uv.get(j).v(),uv.get(j).normal()));
            faces.add(new Face(nextFace++,part,corners.get(0),corners.get(1),corners.get(2)));
        }
        state=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),vertices,faces,state.materials(),state.glbSource(),state.frames());revision++;
    }
    private static void write(State state,OutputStream out)throws IOException{
        boolean normals=state.faces().stream().anyMatch(f->f.a().normal()!=null||f.b().normal()!=null||f.c().normal()!=null);
        int version=!state.frames().isEmpty()?5:state.glbSource()!=null?4:state.materials().isEmpty()?(normals?2:1):3;
        var data=new DataOutputStream(out);data.writeInt(MAGIC);data.writeInt(version);
        data.writeUTF(state.sceneId());data.writeUTF(state.sourceFingerprint());data.writeUTF(state.name());data.writeLong(state.sourceBytes());
        data.writeInt(state.vertices().size());for(var v:state.vertices()){
            data.writeLong(v.id());data.writeUTF(v.part());data.writeDouble(v.x());data.writeDouble(v.y());data.writeDouble(v.z());
        }
        data.writeInt(state.faces().size());for(var f:state.faces()){
            data.writeLong(f.id());data.writeUTF(f.part());for(var c:List.of(f.a(),f.b(),f.c())){data.writeLong(c.vertex());data.writeDouble(c.u());data.writeDouble(c.v());if(version>=2){data.writeBoolean(c.normal()!=null);if(c.normal()!=null){data.writeDouble(c.normal().x());data.writeDouble(c.normal().y());data.writeDouble(c.normal().z());}}}if(version>=3)data.writeUTF(f.material());
        }if(version>=3)EditorMeshMaterial.writeTable(data,state.materials());if(version>=4){byte[] glb=state.glbSource()==null?new byte[0]:state.glbSource().original();data.writeInt(glb.length);data.write(glb);}if(version>=5)EditorMeshFrame.write(data,state.frames());data.flush();
    }
    private static int count(DataInputStream data,int max)throws IOException{int count=data.readInt();if(count<0||count>max)throw new IOException("Topology count budget exceeded");return count;}
    private static State read(Path path)throws IOException{return decode(EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES));}
    public static State decode(byte[] bytes)throws IOException{
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
            if(data.read()!=-1)throw new IOException("Trailing native topology data");
            return new State(id,source,name,size,vertices,faces,materials,glb,frames);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid native topology",invalid);}
    }
    private State owned(Path file)throws IOException{
        State value=read(file);if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint())||!Objects.equals(sourceGlb,value.glbSource()))throw new IOException("Foreign topology project/source provenance");return value;
    }
    public synchronized byte[] snapshotBytes()throws IOException{
        var buffer=new ByteArrayOutputStream();var bounded=new FilterOutputStream(buffer){
            int count;
            private void reserve(int length)throws IOException{if(length<0||length>EditorDocumentStore.MAX_BYTES-count)throw new IOException("Topology output exceeds byte budget");count+=length;}
            public void write(int value)throws IOException{reserve(1);out.write(value);}
            public void write(byte[] bytes,int offset,int length)throws IOException{reserve(length);out.write(bytes,offset,length);}
        };
        write(state,bounded);return buffer.toByteArray();
    }
    public synchronized String diskFingerprint(){return diskFingerprint;}
    public synchronized void save()throws IOException{
        State expected=state;byte[] bytes=snapshotBytes();String previous=diskFingerprint;
        EditorDocumentStore.saveIfUnchanged(path,out->out.write(bytes),this::owned,
            file->{if(!expected.equals(owned(file)))throw new IOException("Topology readback differs from authoring state");},previous);
        diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);
        cleanState=expected;
    }
    public static EditorTopologyProject open(Path path)throws IOException{
        EditorDocumentStore.recoverPending(path,EditorTopologyProject::read);
        byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);
        var project=new EditorTopologyProject(path,decode(bytes));project.diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);return project;
    }
    public synchronized void reopen()throws IOException{
        EditorDocumentStore.recoverPending(path,this::owned);byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);State value=decode(bytes);
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint())||!Objects.equals(sourceGlb,value.glbSource()))throw new IOException("Foreign topology project/source provenance");
        restore(value);diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);cleanState=value;
    }
    public synchronized void recoverPrevious()throws IOException{EditorDocumentStore.restorePrevious(path,this::owned);reopen();}
}
