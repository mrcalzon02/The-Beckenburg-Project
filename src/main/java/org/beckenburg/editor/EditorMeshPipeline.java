package org.beckenburg.editor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Portable bounded mesh import and non-destructive, part-aware vertex clustering.
 * No Blender, GUI, game or third-party dependencies. Source files are never modified.
 */
public final class EditorMeshPipeline {
    public static final long MAX_SOURCE_BYTES=32L*1024*1024;
    public static final int MAX_TRIANGLES=120000;
    /** Authored corner direction; magnitude is retained in native/OBJ, normalized for glTF/runtime. */
    public record Normal(double x,double y,double z){
        public Normal{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Double.isFinite(Math.hypot(Math.hypot(x,y),z))||Math.hypot(Math.hypot(x,y),z)==0)throw new IllegalArgumentException("Invalid corner normal");}
    }
    public record Vertex(double x,double y,double z,double u,double v,Normal normal) {
        public Vertex(double x,double y,double z,double u,double v){this(x,y,z,u,v,null);}
        public Vertex {
            if(!Double.isFinite(x+y+z+u+v))throw new IllegalArgumentException("Nonfinite vertex");
        }
    }
    public record Triangle(String part,Vertex a,Vertex b,Vertex c,String material) {
        public Triangle(String part,Vertex a,Vertex b,Vertex c){this(part,a,b,c,"");}
        public Triangle {
            if(part==null||part.isBlank()||a==null||b==null||c==null||material==null)
                throw new IllegalArgumentException("Invalid triangle");
        }
    }
    /** Optional per-corner source position identities. Empty means the source supplied no indices. */
    public record Mesh(String name,List<Triangle> triangles,long sourceBytes,List<Long> cornerVertexIds,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,List<Long> faceIds,Map<String,EditorMeshFrame> frames,List<EditorMeshQuad> quads) {
        public Mesh(String name,List<Triangle> triangles,long sourceBytes,List<Long> cornerVertexIds,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,List<Long> faceIds,Map<String,EditorMeshFrame> frames){this(name,triangles,sourceBytes,cornerVertexIds,materials,glbSource,faceIds,frames,List.of());}
        public Mesh(String name,List<Triangle> triangles,long sourceBytes,List<Long> cornerVertexIds,List<EditorMeshMaterial> materials,EditorGlbReader.Document glbSource,List<Long> faceIds){this(name,triangles,sourceBytes,cornerVertexIds,materials,glbSource,faceIds,Map.of());}
        public Mesh(String name,List<Triangle> triangles,long sourceBytes,List<Long> cornerVertexIds,List<EditorMeshMaterial> materials){this(name,triangles,sourceBytes,cornerVertexIds,materials,null,List.of());}
        public Mesh(String name,List<Triangle> triangles,long sourceBytes,List<Long> cornerVertexIds){this(name,triangles,sourceBytes,cornerVertexIds,List.of());}
        public Mesh(String name,List<Triangle> triangles,long sourceBytes){this(name,triangles,sourceBytes,List.of());}
        public Mesh {
            if(name==null||name.isBlank()||triangles==null
                ||triangles.size()>MAX_TRIANGLES||sourceBytes<0)throw new IllegalArgumentException("Invalid mesh");
            frames=frames.isEmpty()?Map.of():EditorMeshFrame.validate(frames,triangles.stream().map(Triangle::part).toList());triangles=List.copyOf(triangles);materials=List.copyOf(materials);EditorMeshMaterial.validateTable(materials);var known=new HashSet<String>();for(var m:materials)known.add(m.key());for(var t:triangles)if(!t.material().isEmpty()&&!known.contains(t.material()))throw new IllegalArgumentException("Unresolved face material");
            if(cornerVertexIds==null||!cornerVertexIds.isEmpty()&&cornerVertexIds.size()!=triangles.size()*3)
                throw new IllegalArgumentException("Source topology corner count mismatch");
            cornerVertexIds=List.copyOf(cornerVertexIds);faceIds=List.copyOf(faceIds);if(!faceIds.isEmpty()&&(faceIds.size()!=triangles.size()||new HashSet<>(faceIds).size()!=faceIds.size()||faceIds.stream().anyMatch(id->id<0)))throw new IllegalArgumentException("Invalid native face IDs");
            quads=EditorMeshQuad.ordered(quads);
            if(!quads.isEmpty()){
                if(quads.size()>triangles.size()/2 || cornerVertexIds.isEmpty())throw new IllegalArgumentException("Quad needs native corner bindings");
                var used=new HashSet<Long>();
                for(var pair:EditorMeshQuad.ordinals(quads,faceIds)){
                    if(pair.first()>=triangles.size()||pair.second()>=triangles.size()||!used.add(pair.first())||!used.add(pair.second()))throw new IllegalArgumentException("Invalid quad face references");
                    int a=(int)pair.first()*3,b=(int)pair.second()*3;
                    if(!triangles.get(a/3).part().equals(triangles.get(b/3).part())||!cornerVertexIds.get(a).equals(cornerVertexIds.get(b))||!cornerVertexIds.get(a+2).equals(cornerVertexIds.get(b+1))||new HashSet<>(List.of(cornerVertexIds.get(a),cornerVertexIds.get(a+1),cornerVertexIds.get(a+2),cornerVertexIds.get(b+2))).size()!=4)throw new IllegalArgumentException("Invalid quad topology");
                }
            }
            for(long id:cornerVertexIds)if(id<0)throw new IllegalArgumentException("Invalid source position ID");
        }
    }
    public record PartStats(String name,int before,int after,int verticesBefore,int verticesAfter,
                            boolean overRegression) {}
    public record Result(Mesh source,Mesh optimized,int level,List<PartStats> parts,
                         List<String> warnings,long sourceMemoryBytes,long optimizedMemoryBytes) {
        public Result {
            parts=List.copyOf(parts);warnings=List.copyOf(warnings);
        }
        public boolean overRegressed(){return parts.stream().anyMatch(PartStats::overRegression);}
    }
    private EditorMeshPipeline(){}

    public static byte[] readSource(Path file,int maximum)throws IOException{
        if(maximum<1||maximum>MAX_SOURCE_BYTES)throw new IllegalArgumentException("Invalid source budget");
        if(!Files.isRegularFile(file,java.nio.file.LinkOption.NOFOLLOW_LINKS))throw new IOException("Not a regular model file");
        if(Files.size(file)>maximum)throw new IOException("Model source exceeds byte budget");
        try(var input=Files.newInputStream(file)){
            byte[] raw=input.readNBytes(maximum+1);
            if(raw.length==0||raw.length>maximum)throw new IOException("Model source exceeds byte budget");return raw;
        }
    }
    public static String sourceFingerprint(byte[] bytes){
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static Mesh read(Path file)throws IOException{return EditorObjMaterials.capture(file).mesh();}
    /** Parse exactly the captured source bytes whose digest is shown/reviewed by the host. */
    public static Mesh read(String name,byte[] source)throws IOException{
        if(source.length==0||source.length>MAX_SOURCE_BYTES)throw new IOException("Model source exceeds byte budget");
        return decode(name,source.clone());
    }
    private static Mesh decode(String name,byte[] raw)throws IOException{
        String lower=name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".obj.zip"))return EditorObjBundle.read(name,raw);
        if(lower.endsWith(".obj"))return obj(name,new String(raw,StandardCharsets.UTF_8),raw.length,List.of(),Map.of());
        if(lower.endsWith(".stl"))return stl(name,raw,raw.length);
        throw new IOException("Portable mesh reader supports OBJ and STL; GLB uses the game's glTF decoder");
    }

    static Mesh obj(String name,String raw,long size,List<EditorMeshMaterial> materials,Map<String,String> aliases)throws IOException{
        List<double[]> xyz=new ArrayList<>(),uv=new ArrayList<>();List<Normal> normals=new ArrayList<>();
        List<Triangle> triangles=new ArrayList<>();
        List<Long> sourceIds=new ArrayList<>();var quads=new ArrayList<EditorMeshQuad>();var frames=new TreeMap<String,EditorMeshFrame>();
        String part="Object",material="";
        int lineNumber=0;
        try(var input=new java.io.BufferedReader(new java.io.StringReader(raw))){
            for(String line;(line=input.readLine())!=null;){
                lineNumber++;String text=line.strip();
                if(text.startsWith("# beck_authored_quad")){
                    var fields=text.split("\\s+");
                    if(fields.length!=4||!fields[1].equals("beck_authored_quad_v1")||quads.size()>=MAX_TRIANGLES/2)throw new IOException("Invalid native OBJ quad metadata");
                    quads.add(new EditorMeshQuad(Long.parseLong(fields[2]),Long.parseLong(fields[3])));continue;
                }
                if(text.startsWith("# beck_native_frame")){String[] fields=text.split("\\s+");if(fields.length!=15||!fields[1].equals("beck_native_frame_v1"))throw new IOException("Unsupported native OBJ frame metadata");if(frames.size()>=MAX_TRIANGLES*3)throw new IOException("OBJ frame count budget exceeded");double[] matrix=new double[12];for(int i=0;i<12;i++)matrix[i]=Double.parseDouble(fields[i+3]);if(frames.putIfAbsent(fields[2],EditorMeshFrame.fromValues(matrix))!=null)throw new IOException("Duplicate native OBJ frame");continue;}
                int comment=text.indexOf('#');if(comment>=0)text=text.substring(0,comment).strip();
                if(text.isEmpty()||text.startsWith("#"))continue;
                String[] tokens=text.split("\\s+",258);
                switch(tokens[0]){
                    case "mtllib" -> {if(aliases.isEmpty())throw new IOException("OBJ material library requires source-file context");}
                    case "usemtl" -> {String label=text.substring(6).strip();material=aliases.get(label);if(material==null)throw new IOException("Unresolved OBJ material: "+label);}
                    case "o","g" -> {if(tokens.length>1)part=tokens[1];}
                    case "v" -> {if(tokens.length<4)throw new IOException("Invalid OBJ position");
                        if(xyz.size()>=MAX_TRIANGLES*3)throw new IOException("OBJ position declaration budget exceeded");
                        xyz.add(new double[]{Double.parseDouble(tokens[1]),Double.parseDouble(tokens[2]),Double.parseDouble(tokens[3])});}
                    case "vt" -> {if(tokens.length<3)throw new IOException("Invalid OBJ UV");
                        if(uv.size()>=MAX_TRIANGLES*3)throw new IOException("OBJ UV declaration budget exceeded");
                        uv.add(new double[]{Double.parseDouble(tokens[1]),Double.parseDouble(tokens[2])});}
                    case "vn" -> {if(tokens.length!=4)throw new IOException("Invalid OBJ normal");
                        if(normals.size()>=MAX_TRIANGLES*3)throw new IOException("OBJ normal declaration budget exceeded");
                        normals.add(new Normal(Double.parseDouble(tokens[1]),Double.parseDouble(tokens[2]),Double.parseDouble(tokens[3])));}
                    case "f" -> {
                        if(tokens.length<4||tokens.length>257)throw new IOException("Invalid OBJ face degree");
                        ObjCorner first=faceVertex(tokens[1],xyz,uv,normals),previous=faceVertex(tokens[2],xyz,uv,normals);
                        int firstFace=triangles.size();
                        for(int k=3;k<tokens.length;k++){
                            ObjCorner next=faceVertex(tokens[k],xyz,uv,normals);
                            triangles.add(new Triangle(part,first.vertex(),previous.vertex(),next.vertex(),material));
                            sourceIds.add((long)first.index());sourceIds.add((long)previous.index());sourceIds.add((long)next.index());previous=next;
                            if(triangles.size()>MAX_TRIANGLES)throw new IOException("OBJ triangle budget exceeded");
                        }
                        if(tokens.length==5 && new HashSet<>(sourceIds.subList(firstFace*3,sourceIds.size())).size()==4)quads.add(new EditorMeshQuad(firstFace,firstFace+1));
                    }
                }
            }
        }catch(IllegalArgumentException|IndexOutOfBoundsException err){
            throw new IOException("Invalid OBJ at line "+lineNumber,err);
        }
        if(triangles.isEmpty())throw new IOException("OBJ has no faces");
        try{return new Mesh(name,triangles,size,sourceIds,materials,null,List.of(),frames,quads);}catch(IllegalArgumentException invalid){throw new IOException("Invalid OBJ native frames",invalid);}
    }
    private static int positionIndex(String raw,int count)throws IOException{
        int value=Integer.parseInt(raw);
        int index=value>0?value-1:count+value;
        if(value==0||index<0||index>=count)throw new IOException("OBJ index out of bounds");
        return index;
    }
    private record ObjCorner(Vertex vertex,int index){}
    private static ObjCorner faceVertex(String s,List<double[]> xyz,List<double[]> uv,List<Normal> normals)throws IOException{
        String[] indices=s.split("/",-1);
        if(indices.length>3)throw new IOException("Invalid OBJ face reference");
        int index=positionIndex(indices[0],xyz.size());double[] p=xyz.get(index);
        double u=0,v=0;
        if(indices.length>1&&!indices[1].isEmpty()){
            double[] t=uv.get(positionIndex(indices[1],uv.size()));
            u=t[0];v=t[1];
        }
        Normal normal=indices.length==3&&!indices[2].isEmpty()?normals.get(positionIndex(indices[2],normals.size())):null;
        return new ObjCorner(new Vertex(p[0],p[1],p[2],u,v,normal),index);
    }
    private static Normal optionalNormal(double x,double y,double z)throws IOException{try{return x==0&&y==0&&z==0?null:new Normal(x,y,z);}catch(IllegalArgumentException invalid){throw new IOException("Invalid STL normal",invalid);}}
    private static Mesh stl(String name,byte[] raw,long size)throws IOException{
        List<Triangle> triangles=new ArrayList<>();
        if(raw.length>=84){
            long declared=Integer.toUnsignedLong(ByteBuffer.wrap(raw,80,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
            if(declared<=MAX_TRIANGLES&&84L+declared*50==raw.length){
                ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
                for(int i=0;i<declared;i++){
                    int off=84+i*50;Vertex[] p=new Vertex[3];Normal normal=optionalNormal(b.getFloat(off),b.getFloat(off+4),b.getFloat(off+8));
                    for(int v=0;v<3;v++){
                        int at=off+12+v*12;
                        p[v]=new Vertex(b.getFloat(at),b.getFloat(at+4),b.getFloat(at+8),0,0,normal);
                    }
                    triangles.add(new Triangle("Solid",p[0],p[1],p[2]));
                }
                if(triangles.isEmpty())throw new IOException("Empty STL");
                return new Mesh(name,triangles,size);
            }
        }
        String part="Solid";List<Vertex> points=new ArrayList<>(3);Normal facetNormal=null;
        int lineNumber=0;
        try{
            for(String line:new String(raw,StandardCharsets.US_ASCII).split("\\R")){
                lineNumber++;String[] v=line.trim().split("\\s+");
                if(v.length>1&&v[0].equalsIgnoreCase("solid"))part=v[1];
                if(v.length==5&&v[0].equalsIgnoreCase("facet")&&v[1].equalsIgnoreCase("normal"))facetNormal=optionalNormal(Double.parseDouble(v[2]),Double.parseDouble(v[3]),Double.parseDouble(v[4]));
                if(v[0].equalsIgnoreCase("endfacet"))facetNormal=null;
                if(v.length>=4&&v[0].equalsIgnoreCase("vertex")){
                    points.add(new Vertex(Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]),0,0,facetNormal));
                    if(points.size()==3){
                        triangles.add(new Triangle(part,points.get(0),points.get(1),points.get(2)));points.clear();
                        if(triangles.size()>MAX_TRIANGLES)throw new IOException("STL triangle budget exceeded");
                    }
                }
            }
        }catch(IllegalArgumentException e){throw new IOException("Invalid STL at line "+lineNumber,e);}
        if(triangles.isEmpty()||!points.isEmpty())throw new IOException("No complete STL triangles");
        return new Mesh(name,triangles,size);
    }

    /** Level 0 is the exact input. Larger levels coarsen a world-space grid; limbs
     * and heads must remain named parts, never merged into an anonymous mesh. */
    public static Result regress(Mesh source,int level){
        if(level<0||level>12)throw new IllegalArgumentException("Regression level 0..12");
        if(source.glbSource()!=null&&level>0)throw new IllegalArgumentException("Source-backed GLB reduction requires preserved node/primitive bindings; use native model edits");
        if(level==0){
            Map<String,List<Triangle>> groups=new LinkedHashMap<>();
            for(Triangle t:source.triangles())
                groups.computeIfAbsent(t.part(),k->new ArrayList<>()).add(t);
            List<PartStats> exact=new ArrayList<>();
            for(var entry:groups.entrySet()){
                int vertices=uniqueVertices(entry.getValue());
                exact.add(new PartStats(entry.getKey(),entry.getValue().size(),
                    entry.getValue().size(),vertices,vertices,false));
            }
            long bytes=estimatedBytes(source.triangles());
            List<String> warnings=new ArrayList<>();
            if(source.sourceBytes()>4_000_000)
                warnings.add("SOURCE SIZE: >4 MiB; the current GLB decoder has a 4 MiB per-model ceiling");
            if(bytes>16L*1024*1024)
                warnings.add("MEMORY: estimated CPU triangle data >16 MiB (not full process heap)");
            return new Result(source,source,0,exact,warnings,bytes,bytes);
        }
        double[] bounds={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,
                         Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        Map<String,List<Triangle>> before=new LinkedHashMap<>();
        for(Triangle t:source.triangles()){
            before.computeIfAbsent(t.part(),k->new ArrayList<>()).add(t);
            for(Vertex v:new Vertex[]{t.a(),t.b(),t.c()}){
                bounds[0]=Math.min(bounds[0],v.x());bounds[1]=Math.min(bounds[1],v.y());bounds[2]=Math.min(bounds[2],v.z());
                bounds[3]=Math.max(bounds[3],v.x());bounds[4]=Math.max(bounds[4],v.y());bounds[5]=Math.max(bounds[5],v.z());
            }
        }
        double diagonal=Math.sqrt(Math.pow(bounds[3]-bounds[0],2)+Math.pow(bounds[4]-bounds[1],2)+Math.pow(bounds[5]-bounds[2],2));
        // Levels 1..12 range from fine snapping to deliberate whole-part destruction;
        // the preview always retains its source for immediate rollback.
        double cell=level==0?0:Math.max(1e-10,diagonal*Math.pow(2,level-9));
        Map<String,List<Triangle>> after=new LinkedHashMap<>();
        List<String> warnings=new ArrayList<>();
        for(var entry:before.entrySet()){
            List<Triangle> output=new ArrayList<>();Set<String> seen=new HashSet<>();
            for(Triangle t:entry.getValue()){
                Vertex a=snap(t.a(),cell),b=snap(t.b(),cell),c=snap(t.c(),cell);
                if(area2(a,b,c)<1e-20)continue;
                String key=t.material()+"|"+a+"|"+b+"|"+c;
                if(seen.add(key))output.add(EditorMeshNormals.transport(t,new Triangle(entry.getKey(),a,b,c,t.material())));
            }
            after.put(entry.getKey(),output);
        }
        List<Triangle> combined=new ArrayList<>();
        List<PartStats> stats=new ArrayList<>();
        for(var e:before.entrySet()){
            List<Triangle> original=e.getValue(),reduced=after.get(e.getKey());
            combined.addAll(reduced);
            int b=uniqueVertices(original),a=uniqueVertices(reduced);
            boolean broken=level>0&&(reduced.isEmpty()||(original.size()>=4&&reduced.size()<Math.max(2,original.size()*.08))
                                      ||(b>=4&&a<=2));
            stats.add(new PartStats(e.getKey(),original.size(),reduced.size(),b,a,broken));
            if(broken)warnings.add("OVER-REGRESSION: '"+e.getKey()+"' collapsed from "+original.size()+
                                  " triangles / "+b+" corners to "+reduced.size()+" triangles / "+a+" corners");
        }
        if(source.sourceBytes()>4_000_000)warnings.add("SOURCE SIZE: >4 MiB; existing Beckenburg GLB decoder has a 4 MiB per-model ceiling");
        long beforeMemory=estimatedBytes(source.triangles()),afterMemory=estimatedBytes(combined);
        if(beforeMemory>16L*1024*1024)warnings.add("MEMORY: estimated source triangle data >16 MiB (not full process heap)");
        if(combined.isEmpty())warnings.add("INVALID RESULT: reduction removed all drawable triangles. Publishing is disabled.");
        var retainedParts=new HashSet<String>();for(var triangle:combined)retainedParts.add(triangle.part());var retainedFrames=new TreeMap<String,EditorMeshFrame>();for(var entry:source.frames().entrySet())if(retainedParts.contains(entry.getKey()))retainedFrames.put(entry.getKey(),entry.getValue());
        Mesh result=new Mesh(source.name(),combined,source.sourceBytes(),List.of(),source.materials(),null,List.of(),retainedFrames);
        // The over-regressed preview is represented by stats even when no drawable triangles survive.
        return new Result(source,result,level,stats,warnings,beforeMemory,afterMemory);
    }
    private static long estimatedBytes(List<Triangle> triangles){long bytes=(long)triangles.size()*3*5*8;for(var t:triangles)for(var v:List.of(t.a(),t.b(),t.c()))if(v.normal()!=null)bytes+=3*8;return bytes;}
    private static int uniqueVertices(List<Triangle> list){
        Set<String> values=new HashSet<>();
        for(Triangle t:list){values.add(corner(t.a()));values.add(corner(t.b()));values.add(corner(t.c()));}
        return values.size();
    }
    private static String corner(Vertex v){return v.x()+":"+v.y()+":"+v.z();}
    private static Vertex snap(Vertex v,double cell){
        if(cell==0)return v;
        return new Vertex(Math.rint(v.x()/cell)*cell,Math.rint(v.y()/cell)*cell,
            Math.rint(v.z()/cell)*cell,v.u(),v.v(),v.normal());
    }
    private static double area2(Vertex a,Vertex b,Vertex c){
        double x=b.x()-a.x(),y=b.y()-a.y(),z=b.z()-a.z();
        double p=c.x()-a.x(),q=c.y()-a.y(),r=c.z()-a.z();
        return Math.pow(y*r-z*q,2)+Math.pow(z*p-x*r,2)+Math.pow(x*q-y*p,2);
    }
}
