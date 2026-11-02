package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Authoritative human mesh authoring state. Triangle buffers and adjacency are
 * derived from stable indexed vertices/corner UVs. No game scene is duplicated.
 */
public final class EditorTopologyProject implements EditorProject<EditorTopologyProject.State> {
    private static final int MAGIC=0x42544f50,VERSION=2,MAX_VERTICES=EditorMeshPipeline.MAX_TRIANGLES*3;
    public record Vertex(long id,String part,double x,double y,double z){
        public Vertex {
            text(part);if(id<0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid topology vertex");
        }
    }
    public record Corner(long vertex,double u,double v,EditorMeshPipeline.Normal normal){
        public Corner(long vertex,double u,double v){this(vertex,u,v,null);}
        public Corner {if(vertex<0||!Double.isFinite(u)||!Double.isFinite(v))throw new IllegalArgumentException("Invalid corner");}
    }
    public record Face(long id,String part,Corner a,Corner b,Corner c){
        public Face {text(part);if(id<0)throw new IllegalArgumentException("Invalid face ID");Objects.requireNonNull(a);Objects.requireNonNull(b);Objects.requireNonNull(c);}
    }
    public record State(String sceneId,String sourceFingerprint,String name,long sourceBytes,List<Vertex> vertices,List<Face> faces){
        public State {
            text(sceneId);text(name);vertices=List.copyOf(vertices);faces=List.copyOf(faces);
            if(sourceFingerprint==null||!sourceFingerprint.matches("[0-9a-f]{64}")||sourceBytes<0||vertices.size()>MAX_VERTICES||faces.size()>EditorMeshPipeline.MAX_TRIANGLES)
                throw new IllegalArgumentException("Invalid topology state");
            var ids=new HashMap<Long,Vertex>();for(var vertex:vertices)if(ids.putIfAbsent(vertex.id(),vertex)!=null)throw new IllegalArgumentException("Duplicate vertex ID");
            var faceIds=new HashSet<Long>();for(var face:faces){
                if(!faceIds.add(face.id()))throw new IllegalArgumentException("Duplicate face ID");
                for(var corner:List.of(face.a(),face.b(),face.c())){
                    var vertex=ids.get(corner.vertex());if(vertex==null||!vertex.part().equals(face.part()))throw new IllegalArgumentException("Foreign/missing face vertex");
                }
            }
        }
    }
    private final Path path;
    private final String id,sourceFingerprint;
    private State state;
    private State cleanState;
    private String diskFingerprint;
    private long revision=1;
    private static void text(String value){if(value==null||value.isBlank()||value.length()>512)throw new IllegalArgumentException("Invalid topology text");}
    private EditorTopologyProject(Path path,State state){this.path=path.toAbsolutePath().normalize();this.state=state;this.cleanState=state;id=state.sceneId();sourceFingerprint=state.sourceFingerprint();}
    /** Preserve supplied corner identities; unindexed triangle sources use geometric connectivity per part. */
    public static EditorTopologyProject fromMesh(EditorMeshPipeline.Mesh mesh,String sourceFingerprint,Path path){
        var graph=EditorMeshTopology.build(mesh);var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();
        for(int i=0;i<graph.vertices().size();i++){var v=graph.vertices().get(i);vertices.add(new Vertex(i,v.part(),v.x(),v.y(),v.z()));}
        for(int i=0;i<mesh.triangles().size();i++){
            var triangle=mesh.triangles().get(i);var uv=List.of(triangle.a(),triangle.b(),triangle.c());var corners=new ArrayList<Corner>();
            for(int j=0;j<3;j++)corners.add(new Corner(graph.edges().get(i*3+j).origin(),uv.get(j).u(),uv.get(j).v(),uv.get(j).normal()));
            faces.add(new Face(i,triangle.part(),corners.get(0),corners.get(1),corners.get(2)));
        }
        return new EditorTopologyProject(path,new State("mesh-"+UUID.randomUUID(),sourceFingerprint,mesh.name(),mesh.sourceBytes(),vertices,faces));
    }
    public String id(){return id;}
    public synchronized String title(){return state.name();}
    public Path documentPath(){return path;}
    public synchronized long revision(){return revision;}
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
        if(target.toAbsolutePath().normalize().equals(path))throw new IOException("OBJ export cannot replace the native authoring document");
        State expected=state;
        EditorDocumentStore.saveIfUnchanged(target,out->EditorObjWriter.write(expected,out),
            file->EditorMeshPipeline.read("export.obj",EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),
            file->EditorObjWriter.validate(expected,EditorMeshPipeline.readSource(file,EditorDocumentStore.MAX_BYTES)),expectedTargetSha);
    }
    public synchronized void restore(State value){
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IllegalArgumentException("Topology belongs to another source/project");
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
        for(var face:state.faces())triangles.add(new EditorMeshPipeline.Triangle(face.part(),corner(face.a(),vertices),corner(face.b(),vertices),corner(face.c(),vertices)));
        return new EditorMeshPipeline.Mesh(state.name(),triangles,state.sourceBytes());
    }
    private static Map<Long,Vertex> indexVertices(List<Vertex> values){var map=new HashMap<Long,Vertex>();for(var v:values)map.put(v.id(),v);return map;}
    private static EditorMeshPipeline.Triangle triangle(Face f,Map<Long,Vertex> values){return new EditorMeshPipeline.Triangle(f.part(),corner(f.a(),values),corner(f.b(),values),corner(f.c(),values));}
    private static Corner normalCorner(Corner c,EditorMeshPipeline.Normal n){return new Corner(c.vertex(),c.u(),c.v(),n);}
    private static Face normalFace(Face f,EditorMeshPipeline.Triangle t){return new Face(f.id(),f.part(),normalCorner(f.a(),t.a().normal()),normalCorner(f.b(),t.b().normal()),normalCorner(f.c(),t.c().normal()));}
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
        var cap=new Face(face.id(),face.part(),newCorners.get(0),newCorners.get(1),newCorners.get(2));
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
            newFaces.add(new Face(faceMax+1+i*2,face.part(),oldA,oldB,newB));newFaces.add(new Face(faceMax+2+i*2,face.part(),oldA,newB,newA));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces);
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
            newFaces.add(new Face(f.id(),f.part(),a,b,c));
        }
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),newVertices,newFaces);
        if(!topology(after).manifold())throw new IllegalArgumentException("Weld would leave invalid topology; select complete seams or repair degenerate faces first");
        return new EditPreview(this,state,after,revision,"WELD COINCIDENT / -"+replacements.size()+" vertices, -"+removed+" collapsed triangles");
    }
    private static Corner weldCorner(Corner value,Map<Long,Long> replacements){return new Corner(replacements.getOrDefault(value.vertex(),value.vertex()),value.u(),value.v(),value.normal());}
    /** Remove invalid zero/non-finite-area triangles explicitly, retaining every surviving stable ID/UV. */
    public synchronized EditPreview previewRemoveDegenerates(){
        var positions=vertices(state);var remaining=new ArrayList<Face>();
        for(var face:state.faces()){double size=area(face,positions);if(size>0&&Double.isFinite(size))remaining.add(face);}
        int removed=state.faces().size()-remaining.size();if(removed==0)throw new IllegalArgumentException("No degenerate native faces to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),state.vertices(),remaining);
        if(!topology(after).manifold())throw new IllegalArgumentException("Other topology errors remain; repair requires a valid resulting mesh");
        return new EditPreview(this,state,after,revision,"REMOVE DEGENERATES / -"+removed+" triangles; vertex IDs retained");
    }
    public synchronized EditPreview previewRemoveUnusedVertices(){
        var used=new HashSet<Long>();for(var f:state.faces()){used.add(f.a().vertex());used.add(f.b().vertex());used.add(f.c().vertex());}
        var retained=state.vertices().stream().filter(v->used.contains(v.id())).toList();int removed=state.vertices().size()-retained.size();
        if(removed==0)throw new IllegalArgumentException("No unused native vertices to remove");
        var after=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),retained,state.faces());
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
        replaceVertices(old,next,Set.of(id));
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
        replaceVertices(old,next,selected);
    }
    public enum Transform { ROTATE, SCALE_AXIS, SCALE_UNIFORM }
    /** Global axes, explicit origin/selection-centroid pivot. Candidate only; shared Apply owns mutation. */
    public synchronized EditPreview previewTransform(Collection<Long> ids,Transform operation,int axis,double amount,boolean origin){
        Objects.requireNonNull(ids);Objects.requireNonNull(operation);
        if(ids.isEmpty()||ids.size()>MAX_VERTICES||axis<0||axis>2||!Double.isFinite(amount)
                ||(operation==Transform.ROTATE?Math.abs(amount)>36000:amount<.000001||amount>1000000))
            throw new IllegalArgumentException("Invalid native transform parameters");
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
        if(after.equals(state))throw new IllegalArgumentException("Transform does not change selected vertices");
        return new EditPreview(this,state,after,revision,operation+" / "+selected.size()+" vertices / global "+"XYZ".charAt(axis)+" / "+amount+" / "+(origin?"world origin":"selection centroid"));
    }
    private void replaceVertices(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected){
        state=replacementState(old,next,selected);revision++;
    }
    private State replacementState(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected){return replacementState(old,next,selected,n->n);}
    private State replacementState(Map<Long,Vertex> old,Map<Long,Vertex> next,Set<Long> selected,java.util.function.UnaryOperator<EditorMeshPipeline.Normal> transform){
        for(var face:state.faces())if(selected.contains(face.a().vertex())||selected.contains(face.b().vertex())||selected.contains(face.c().vertex())){
            double area=area(face,next);
            if(!Double.isFinite(area)||(area==0&&area(face,old)>0))throw new IllegalArgumentException("Vertex move degenerates a face");
        }
        var values=new ArrayList<Vertex>();for(var v:state.vertices())values.add(next.get(v.id()));
        var faces=new ArrayList<Face>(state.faces().size());
        for(var face:state.faces()){
            boolean touched=selected.contains(face.a().vertex())||selected.contains(face.b().vertex())||selected.contains(face.c().vertex());
            if(!touched||face.a().normal()==null&&face.b().normal()==null&&face.c().normal()==null){faces.add(face);continue;}
            if(selected.contains(face.a().vertex())&&selected.contains(face.b().vertex())&&selected.contains(face.c().vertex()))faces.add(new Face(face.id(),face.part(),normalCorner(face.a(),transform.apply(face.a().normal())),normalCorner(face.b(),transform.apply(face.b().normal())),normalCorner(face.c(),transform.apply(face.c().normal()))));
            else faces.add(normalFace(face,EditorMeshNormals.transport(triangle(face,old),triangle(face,next))));
        }
        return new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),values,faces);
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
        state=new State(id,sourceFingerprint,state.name(),state.sourceBytes(),vertices,faces);revision++;
    }
    private static void write(State state,OutputStream out)throws IOException{
        boolean normals=state.faces().stream().anyMatch(f->f.a().normal()!=null||f.b().normal()!=null||f.c().normal()!=null);
        var data=new DataOutputStream(out);data.writeInt(MAGIC);data.writeInt(normals?VERSION:1);
        data.writeUTF(state.sceneId());data.writeUTF(state.sourceFingerprint());data.writeUTF(state.name());data.writeLong(state.sourceBytes());
        data.writeInt(state.vertices().size());for(var v:state.vertices()){
            data.writeLong(v.id());data.writeUTF(v.part());data.writeDouble(v.x());data.writeDouble(v.y());data.writeDouble(v.z());
        }
        data.writeInt(state.faces().size());for(var f:state.faces()){
            data.writeLong(f.id());data.writeUTF(f.part());for(var c:List.of(f.a(),f.b(),f.c())){data.writeLong(c.vertex());data.writeDouble(c.u());data.writeDouble(c.v());if(normals){data.writeBoolean(c.normal()!=null);if(c.normal()!=null){data.writeDouble(c.normal().x());data.writeDouble(c.normal().y());data.writeDouble(c.normal().z());}}}
        }data.flush();
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
                faces.add(new Face(face,part,corners.get(0),corners.get(1),corners.get(2)));
            }
            if(data.read()!=-1)throw new IOException("Trailing native topology data");
            return new State(id,source,name,size,vertices,faces);
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid native topology",invalid);}
    }
    private State owned(Path file)throws IOException{
        State value=read(file);if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IOException("Foreign topology project/source");return value;
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
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IOException("Foreign topology project/source");
        restore(value);diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);cleanState=value;
    }
    public synchronized void recoverPrevious()throws IOException{EditorDocumentStore.restorePrevious(path,this::owned);reopen();}
}
