package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Authoritative human mesh authoring state. Triangle buffers and adjacency are
 * derived from stable indexed vertices/corner UVs. No game scene is duplicated.
 */
public final class EditorTopologyProject implements EditorProject<EditorTopologyProject.State> {
    private static final int MAGIC=0x42544f50,VERSION=1,MAX_VERTICES=EditorMeshPipeline.MAX_TRIANGLES*3;
    public record Vertex(long id,String part,double x,double y,double z){
        public Vertex {
            text(part);if(id<0||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid topology vertex");
        }
    }
    public record Corner(long vertex,double u,double v){
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
    private String diskFingerprint;
    private long revision=1;
    private static void text(String value){if(value==null||value.isBlank()||value.length()>512)throw new IllegalArgumentException("Invalid topology text");}
    private EditorTopologyProject(Path path,State state){this.path=path.toAbsolutePath().normalize();this.state=state;id=state.sceneId();sourceFingerprint=state.sourceFingerprint();}
    public static EditorTopologyProject fromMesh(EditorMeshPipeline.Mesh mesh,String sourceFingerprint,Path path){
        var graph=EditorMeshTopology.build(mesh);var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();
        for(int i=0;i<graph.vertices().size();i++){var v=graph.vertices().get(i);vertices.add(new Vertex(i,v.part(),v.x(),v.y(),v.z()));}
        for(int i=0;i<mesh.triangles().size();i++){
            var triangle=mesh.triangles().get(i);var uv=List.of(triangle.a(),triangle.b(),triangle.c());var corners=new ArrayList<Corner>();
            for(int j=0;j<3;j++)corners.add(new Corner(graph.edges().get(i*3+j).origin(),uv.get(j).u(),uv.get(j).v()));
            faces.add(new Face(i,triangle.part(),corners.get(0),corners.get(1),corners.get(2)));
        }
        return new EditorTopologyProject(path,new State("mesh-"+UUID.randomUUID(),sourceFingerprint,mesh.name(),mesh.sourceBytes(),vertices,faces));
    }
    public String id(){return id;}
    public synchronized String title(){return state.name();}
    public Path documentPath(){return path;}
    public synchronized long revision(){return revision;}
    public synchronized State snapshot(){return state;}
    public synchronized void restore(State value){
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IllegalArgumentException("Topology belongs to another source/project");
        state=value;revision++;
    }
    public List<Asset> assets(){return List.of();}
    public synchronized List<Node> hierarchy(){return state.vertices().stream().map(v->new Node("vertex-"+v.id(),"VERTEX",v.part()+" / "+v.id(),v.x(),v.y(),v.z())).toList();}
    private static Map<Long,Vertex> vertices(State state){var out=new HashMap<Long,Vertex>();for(var v:state.vertices())out.put(v.id(),v);return out;}
    private static EditorMeshPipeline.Vertex corner(Corner corner,Map<Long,Vertex> vertices){
        var v=vertices.get(corner.vertex());return new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),corner.u(),corner.v());
    }
    private static EditorMeshPipeline.Mesh mesh(State state){
        var vertices=vertices(state);var triangles=new ArrayList<EditorMeshPipeline.Triangle>();
        for(var face:state.faces())triangles.add(new EditorMeshPipeline.Triangle(face.part(),corner(face.a(),vertices),corner(face.b(),vertices),corner(face.c(),vertices)));
        return new EditorMeshPipeline.Mesh(state.name(),triangles,state.sourceBytes());
    }
    public synchronized EditorMeshPipeline.Mesh mesh(){return mesh(state);}
    public synchronized EditorMeshTopology topology(){
        var positions=new ArrayList<EditorMeshTopology.Vertex>();var ids=new HashMap<Long,Integer>();
        for(var v:state.vertices()){ids.put(v.id(),positions.size());positions.add(new EditorMeshTopology.Vertex(v.part(),v.x(),v.y(),v.z()));}
        int[] origins=new int[state.faces().size()*3];int i=0;
        for(var face:state.faces())for(var corner:List.of(face.a(),face.b(),face.c()))origins[i++]=ids.get(corner.vertex());
        return EditorMeshTopology.buildIndexed(mesh(),positions,origins);
    }
    private static double area(Face face,Map<Long,Vertex> vertices){
        var a=vertices.get(face.a().vertex());var b=vertices.get(face.b().vertex());var c=vertices.get(face.c().vertex());
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z(),u=c.x()-a.x(),v=c.y()-a.y(),w=c.z()-a.z();
        return Math.hypot(Math.hypot(y*w-z*v,z*u-x*w),x*v-y*u);
    }
    public synchronized void moveVertex(long id,double x,double y,double z){
        var old=vertices(state);var vertex=old.get(id);if(vertex==null)throw new IllegalArgumentException("Unknown vertex ID");
        var replacement=new Vertex(id,vertex.part(),x,y,z);var next=new HashMap<>(old);next.put(id,replacement);
        for(var face:state.faces())if(face.a().vertex()==id||face.b().vertex()==id||face.c().vertex()==id){
            double area=area(face,next);
            if(!Double.isFinite(area)||(area==0&&area(face,old)>0))throw new IllegalArgumentException("Vertex move degenerates a face");
        }
        var values=new ArrayList<Vertex>();for(var v:state.vertices())values.add(v.id()==id?replacement:v);
        state=new State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),values,state.faces());revision++;
    }
    private static void write(State state,OutputStream out)throws IOException{
        var data=new DataOutputStream(out);data.writeInt(MAGIC);data.writeInt(VERSION);
        data.writeUTF(state.sceneId());data.writeUTF(state.sourceFingerprint());data.writeUTF(state.name());data.writeLong(state.sourceBytes());
        data.writeInt(state.vertices().size());for(var v:state.vertices()){
            data.writeLong(v.id());data.writeUTF(v.part());data.writeDouble(v.x());data.writeDouble(v.y());data.writeDouble(v.z());
        }
        data.writeInt(state.faces().size());for(var f:state.faces()){
            data.writeLong(f.id());data.writeUTF(f.part());for(var c:List.of(f.a(),f.b(),f.c())){data.writeLong(c.vertex());data.writeDouble(c.u());data.writeDouble(c.v());}
        }data.flush();
    }
    private static int count(DataInputStream data,int max)throws IOException{int count=data.readInt();if(count<0||count>max)throw new IOException("Topology count budget exceeded");return count;}
    private static State read(Path path)throws IOException{return decode(EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES));}
    public static State decode(byte[] bytes)throws IOException{
        if(bytes.length>EditorDocumentStore.MAX_BYTES)throw new IOException("Topology file exceeds byte budget");
        try{
            var data=new DataInputStream(new ByteArrayInputStream(bytes));
            if(data.readInt()!=MAGIC||data.readInt()!=VERSION)throw new IOException("Unsupported native topology version");
            String id=data.readUTF(),source=data.readUTF(),name=data.readUTF();long size=data.readLong();
            int count=count(data,MAX_VERTICES);var vertices=new ArrayList<Vertex>();
            for(int i=0;i<count;i++)vertices.add(new Vertex(data.readLong(),data.readUTF(),data.readDouble(),data.readDouble(),data.readDouble()));
            count=count(data,EditorMeshPipeline.MAX_TRIANGLES);var faces=new ArrayList<Face>();
            for(int i=0;i<count;i++){
                long face=data.readLong();String part=data.readUTF();var corners=new ArrayList<Corner>();
                for(int j=0;j<3;j++)corners.add(new Corner(data.readLong(),data.readDouble(),data.readDouble()));
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
    }
    public static EditorTopologyProject open(Path path)throws IOException{
        EditorDocumentStore.recoverPending(path,EditorTopologyProject::read);
        byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);
        var project=new EditorTopologyProject(path,decode(bytes));project.diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);return project;
    }
    public synchronized void reopen()throws IOException{
        EditorDocumentStore.recoverPending(path,this::owned);byte[] bytes=EditorMeshPipeline.readSource(path,EditorDocumentStore.MAX_BYTES);State value=decode(bytes);
        if(!id.equals(value.sceneId())||!sourceFingerprint.equals(value.sourceFingerprint()))throw new IOException("Foreign topology project/source");
        restore(value);diskFingerprint=EditorMeshPipeline.sourceFingerprint(bytes);
    }
    public synchronized void recoverPrevious()throws IOException{EditorDocumentStore.restorePrevious(path,this::owned);reopen();}
}
