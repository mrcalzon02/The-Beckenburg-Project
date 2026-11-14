package org.beckenburg.editor;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.beckenburg.editor.EditorMeshPipeline.*;

/** The shared bounded runtime GLB decoder. Source provenance is immutable, never editable scene authority. */
public final class EditorGlbReader {
    private EditorGlbReader(){}
    public static final class Material {
        private final int index;private final String name,alpha,origin,defaultOrigin;private final byte[] png;private final double[] color;private final int wrapS,wrapT;private final double cutoff;
        private Material(int index,String name,byte[] png,double[] color,int wrapS,int wrapT,String alpha,double cutoff,String origin,String defaultOrigin){this.index=index;this.name=name;this.png=png.clone();this.color=color.clone();this.wrapS=wrapS;this.wrapT=wrapT;this.alpha=alpha;this.cutoff=cutoff;this.origin=origin;this.defaultOrigin=defaultOrigin;}
        public int index(){return index;}public String name(){return name;}public byte[] png(){return png.clone();}public double[] color(){return color.clone();}public int wrapS(){return wrapS;}public int wrapT(){return wrapT;}public String alpha(){return alpha;}public double cutoff(){return cutoff;}public String explicitOrigin(){return origin;}public String defaultOrigin(){return defaultOrigin;}
        public EditorMeshMaterial nativeMaterial()throws IOException{try{return new EditorMeshMaterial(key(index),name.isBlank()?"Material "+index:name,png,new EditorMeshMaterial.Color(color[0],color[1],color[2],color[3]),wrapS,wrapT,EditorMeshMaterial.Alpha.valueOf(alpha),cutoff,EditorMeshMaterial.Origin.valueOf(origin==null?defaultOrigin:origin),Map.of("sourceMaterialIndex",Integer.toString(index)));}catch(IllegalArgumentException invalid){throw new IOException("Source material exceeds native authoring capabilities",invalid);}}
    }
    public record Primitive(int node,int primitive,int positionAccessor,List<Vertex> local,List<Vertex> world,List<Integer> indices,int material,List<Long> positionIds,List<Long> faceIds){
        public Primitive{local=List.copyOf(local);world=List.copyOf(world);indices=List.copyOf(indices);positionIds=List.copyOf(positionIds);faceIds=List.copyOf(faceIds);}
        public String part(){return "glb_node_"+node;}
    }
    public record Face(Primitive primitive,int triangle,long identity){}
    public static final class Document {
        private final byte[] original,binary;private final Map<String,Object> root;private final List<Material> materials;private final List<Primitive> primitives;private final double[][] world;private final String fingerprint;private volatile List<EditorMeshMaterial> nativeMaterials;private final Map<String,EditorMeshFrame> frames;private final List<Face> faces;
        private Document(byte[] original,byte[] binary,Map<String,Object> root,List<Material> materials,List<Primitive> primitives,double[][] world)throws IOException{this.original=original.clone();this.binary=binary.clone();this.root=freezeMap(root);this.materials=List.copyOf(materials);this.primitives=List.copyOf(primitives);this.world=world;fingerprint=EditorMeshPipeline.sourceFingerprint(this.original);var active=new HashSet<Integer>();for(var p:primitives)active.add(p.node());var found=new TreeMap<String,EditorMeshFrame>();var nodes=arr(root.get("nodes"));for(int i=0;i<nodes.size();i++){var node=obj(nodes.get(i));if(node.get("extensions") instanceof Map<?,?> extensions&&extensions.containsKey("BECK_editor_frame")){var frame=EditorMeshFrame.json(extensions.get("BECK_editor_frame"));if(active.contains(i))found.put("glb_node_"+i,frame);}}frames=Map.copyOf(found);faces=EditorMeshOrder.read(root,primitives);}
        public List<Face> faces(){return faces;}
        public Map<String,EditorMeshFrame> frames(){return frames;}
        public String fingerprint(){return fingerprint;}
        public byte[] trailing(){var view=ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN);int binHeader=20+view.getInt(12);return Arrays.copyOfRange(original,binHeader+8+view.getInt(binHeader),original.length);}
        public byte[] original(){return original.clone();}public byte[] binary(){return binary.clone();}public Map<String,Object> root(){return root;}public List<Material> materials(){return materials;}public List<Primitive> primitives(){return primitives;}public double[] world(int node){return world[node]==null?null:world[node].clone();}
        public String partLabel(String part){try{if(!part.startsWith("glb_node_"))return part;int index=Integer.parseInt(part.substring(9));return "Node "+index+": "+String.valueOf(obj(arr(root.get("nodes")).get(index)).getOrDefault("name",part));}catch(RuntimeException invalid){return part;}}
        public List<EditorMeshMaterial> nativeMaterials()throws IOException{
            var found=nativeMaterials;if(found!=null)return found;var result=new ArrayList<EditorMeshMaterial>();for(var m:materials)result.add(m.nativeMaterial());try{EditorMeshMaterial.validateTable(result);}catch(IllegalArgumentException bad){throw new IOException("GLB exceeds native material budget",bad);}nativeMaterials=List.copyOf(result);return nativeMaterials;
        }
        public void requireEditable()throws IOException{
            if(root.get("animations") instanceof List<?> a&&!a.isEmpty()||root.get("skins") instanceof List<?> skins&&!skins.isEmpty())throw new IOException("Skinned/animated GLB authoring requires the rigging stage; original retained read-only");
            var nodes=arr(root.get("nodes"));var meshes=arr(root.get("meshes"));for(var p:primitives){var node=obj(nodes.get(p.node));var mesh=obj(meshes.get(integer(node.get("mesh"))));var primitive=obj(arr(mesh.get("primitives")).get(p.primitive));if(node.containsKey("skin")||node.containsKey("weights")||mesh.containsKey("weights")||primitive.containsKey("targets"))throw new IOException("GLB skin/morph authoring requires original rig bindings");var attributes=obj(primitive.get("attributes"));if(primitive.get("extensions") instanceof Map<?,?> extensions&&!Set.of("BECK_native_mesh").containsAll(extensions.keySet()))throw new IOException("GLB primitive extensions require native editing support");for(var accessor:attributes.values())if(obj(arr(root.get("accessors")).get(integer(accessor))).containsKey("sparse"))throw new IOException("GLB sparse attributes require native editing support");if(!Set.of("POSITION","NORMAL","TEXCOORD_0").containsAll(attributes.keySet()))throw new IOException("GLB additional vertex attributes are retained read-only until native editing can preserve them");if(Math.abs(determinant(world[p.node]))<1e-12)throw new IOException("Singular GLB node transform cannot be edited reversibly");}
            var active=new HashSet<Integer>();for(var primitive:primitives)active.add(primitive.material());for(int index:active){var material=obj(arr(root.get("materials")).get(index));var info=obj(obj(material.get("pbrMetallicRoughness")).get("baseColorTexture"));if(get(info,"texCoord",0)!=0||info.get("extensions") instanceof Map<?,?> extensions&&!extensions.isEmpty())throw new IOException("GLB texture-coordinate transforms require native/runtime support before editing");}
            var buffers=arr(root.get("buffers"));if(buffers.size()!=1||obj(buffers.get(0)).containsKey("uri"))throw new IOException("External/multiple GLB buffers require preserved dependency support");
            nativeMaterials();
        }
        public Mesh mesh(String name)throws IOException{
            var triangles=new ArrayList<Triangle>();var ids=new ArrayList<Long>();var sources=new HashMap<String,Long>();
            for(var face:faces){var p=face.primitive();int i=face.triangle()*3;var a=p.indices.get(i);var b=p.indices.get(i+1);var c=p.indices.get(i+2);triangles.add(new Triangle(p.part(),p.world.get(a),p.world.get(b),p.world.get(c),key(p.material)));for(int index:new int[]{a,b,c})ids.add(sources.computeIfAbsent(p.node+":"+(p.positionIds.isEmpty()?p.positionAccessor+":"+index:"native:"+p.positionIds.get(index)),k->(long)sources.size()));}
            try{return new Mesh(name,triangles,original.length,ids,nativeMaterials(),this,java.util.stream.LongStream.range(0,triangles.size()).boxed().toList(),frames,EditorMeshQuad.readGlb(root,triangles.size()));}catch(UncheckedIOException e){throw e.getCause();}catch(IllegalArgumentException bad){throw new IOException("GLB exceeds native material/mesh budgets",bad);}
        }
        @Override public boolean equals(Object o){return o==this||o instanceof Document d&&Arrays.equals(original,d.original);}
        @Override public int hashCode(){return Arrays.hashCode(original);}
    }
    private static double determinant(double[] m){return m[0]*(m[5]*m[10]-m[6]*m[9])-m[1]*(m[4]*m[10]-m[6]*m[8])+m[2]*(m[4]*m[9]-m[5]*m[8]);}
    public static String key(int material){return "glb-material-"+material;}
    private static Map<String,Object> freezeMap(Map<String,Object> map){var copy=new LinkedHashMap<String,Object>();for(var e:map.entrySet())copy.put(e.getKey(),freeze(e.getValue()));return Collections.unmodifiableMap(copy);}
    private static Object freeze(Object value){if(value instanceof Map<?,?> m)return freezeMap(obj(m));if(value instanceof List<?> l){var out=new ArrayList<Object>();for(var item:l)out.add(freeze(item));return Collections.unmodifiableList(out);}return value;}
    public static Document read(byte[] source)throws IOException{
        if(source==null||source.length>4_000_000)throw new IOException("GLB size");
        try{return decode(source.clone());}catch(ClassCastException|IndexOutOfBoundsException|IllegalArgumentException|NullPointerException|ArithmeticException bad){throw new IOException("Invalid GLB structure",bad);}
    }
    private static Document decode(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length<28||bytes.length>4_000_000)throw new IOException("GLB size");var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getInt(0)!=0x46546c67||b.getInt(4)!=2||b.getInt(8)!=bytes.length)throw new IOException("Not glTF2 binary");int jsonLength=b.getInt(12);
        if(jsonLength<=0||jsonLength>bytes.length-28||b.getInt(16)!=0x4e4f534a)throw new IOException("Invalid GLB JSON chunk");int binaryStart=20+jsonLength;
        if(binaryStart+8>bytes.length||b.getInt(binaryStart+4)!=0x004e4942)throw new IOException("Missing GLB binary buffer");int binaryLength=b.getInt(binaryStart);binaryStart+=8;if(binaryLength<0||(long)binaryStart+binaryLength>bytes.length)throw new IOException("GLB binary bounds");
        var root=obj(EditorGlbJson.read(new String(bytes,20,jsonLength,StandardCharsets.UTF_8).trim()));if(!"2.0".equals(obj(root.get("asset")).get("version"))||root.containsKey("extensionsRequired"))throw new IOException("Unsupported glTF asset/required extension");
        int chunkEnd=binaryStart+binaryLength;for(int offset=chunkEnd;offset<bytes.length;){if(bytes.length-offset<8)throw new IOException("Truncated GLB trailing chunk");int length=b.getInt(offset),type=b.getInt(offset+4);if(length<0||length%4!=0||(long)offset+8+length>bytes.length||type==0x4e4f534a||type==0x004e4942)throw new IOException("Invalid GLB trailing chunk");offset+=8+length;}
        var buffers=arr(root.get("buffers"));if(buffers.isEmpty()||obj(buffers.get(0)).containsKey("uri"))throw new IOException("GLB buffer0 must use its actual binary chunk");int declared=get(obj(buffers.get(0)),"byteLength",-1);if(declared<0||declared>binaryLength)throw new IOException("GLB declared binary length");int storedLength=binaryLength;binaryLength=declared;
        var views=arr(root.get("bufferViews"));var accessors=arr(root.get("accessors"));var nodes=arr(root.get("nodes"));var meshes=arr(root.get("meshes"));var images=arr(root.get("images"));var sourceMaterials=arr(root.get("materials"));var textures=arr(root.get("textures"));var samplers=root.get("samplers")==null?List.<Object>of():arr(root.get("samplers"));
        if(views.size()>4096||accessors.size()>4096||nodes.size()>128||meshes.size()>128||images.size()>64||sourceMaterials.size()>256)throw new IOException("GLB budget exceeded");
        var materials=new ArrayList<Material>();long decodedPixels=0;
        for(int i=0;i<sourceMaterials.size();i++){
            var material=obj(sourceMaterials.get(i));var pbr=obj(material.get("pbrMetallicRoughness"));var base=pbr.get("baseColorTexture");if(base==null)throw new IOException("Material has no original texture");var texture=obj(textures.get(integer(obj(base).get("index"))));int sampler=get(texture,"sampler",-1);if(sampler< -1||sampler>=samplers.size())throw new IOException("Invalid glTF sampler");var sampling=sampler<0?Map.<String,Object>of():obj(samplers.get(sampler));int wrapS=get(sampling,"wrapS",10497),wrapT=get(sampling,"wrapT",10497);if(!wrap(wrapS)||!wrap(wrapT))throw new IOException("Unsupported glTF wrap");
            var image=obj(images.get(integer(texture.get("source"))));if(!"image/png".equals(image.get("mimeType"))||!image.containsKey("bufferView"))throw new IOException("Unsupported embedded image");var view=obj(views.get(integer(image.get("bufferView"))));int offset=get(view,"byteOffset",0),length=get(view,"byteLength",-1);if(get(view,"buffer",0)!=0||length<33||offset<0||(long)offset+length>binaryLength)throw new IOException("Image buffer bounds");
            byte[] png=Arrays.copyOfRange(bytes,binaryStart+offset,binaryStart+offset+length);int w=ByteBuffer.wrap(png).getInt(16),h=ByteBuffer.wrap(png).getInt(20);if(w<2||h<2||w>2048||h>2048||(decodedPixels+=(long)w*h)>67_108_864)throw new IOException("Invalid/oversized embedded PNG");var picture=ImageIO.read(new ByteArrayInputStream(png));if(picture==null||picture.getWidth()!=w||picture.getHeight()!=h)throw new IOException("Invalid PNG");
            String alpha=String.valueOf(material.getOrDefault("alphaMode","OPAQUE"));if(!Set.of("OPAQUE","MASK","BLEND").contains(alpha))throw new IOException("Unsupported glTF alpha mode");double cutoff=material.containsKey("alphaCutoff")?num(material.get("alphaCutoff")):.5;if(!Double.isFinite(cutoff)||cutoff<0||cutoff>1)throw new IOException("Invalid alpha cutoff");String origin=null;
            if(material.get("extras") instanceof Map<?,?> extra&&extra.get("beckenburgUvOrigin")!=null){origin=String.valueOf(extra.get("beckenburgUvOrigin"));if(!Set.of("TOP_LEFT","BOTTOM_LEFT").contains(origin))throw new IOException("Unknown authored material UV origin");}
            materials.add(new Material(i,String.valueOf(material.getOrDefault("name","")),png,vec(pbr.get("baseColorFactor"),1,1,1,1),wrapS,wrapT,alpha,cutoff,origin,"Beckenburg EditorAssetPipeline".equals(obj(root.get("asset")).get("generator"))?"BOTTOM_LEFT":"TOP_LEFT"));
        }
        double[][] world=sceneTransforms(root,nodes);var primitives=new ArrayList<Primitive>();int faces=0,declaredVertices=0;
        for(int ni=0;ni<nodes.size();ni++){
            var node=obj(nodes.get(ni));if(!node.containsKey("mesh")||world[ni]==null)continue;var list=arr(obj(meshes.get(integer(node.get("mesh")))).get("primitives"));
            for(int pi=0;pi<list.size();pi++){
                var primitive=obj(list.get(pi));if(get(primitive,"mode",4)!=4||!primitive.containsKey("indices"))throw new IOException("Only indexed GL triangles supported");var attrs=obj(primitive.get("attributes"));int position=integer(attrs.get("POSITION"));var pos=obj(accessors.get(position));var uv=obj(accessors.get(integer(attrs.get("TEXCOORD_0"))));var indices=obj(accessors.get(integer(primitive.get("indices"))));int count=get(pos,"count",-1),ic=get(indices,"count",-1);
                if(count<3||count>50000||get(uv,"count",-1)!=count||ic<3||ic>150000||ic%3!=0||(faces+=ic/3)>50000||(declaredVertices+=count)>500000)throw new IOException("Triangle buffer budget");if(get(pos,"componentType",-1)!=5126||get(uv,"componentType",-1)!=5126||!"VEC3".equals(pos.get("type"))||!"VEC2".equals(uv.get("type")))throw new IOException("Unsupported vertex layout");int material=get(primitive,"material",-1);if(material<0||material>=materials.size())throw new IOException("Invalid material binding");Map<String,Object> normal=null;
                if(attrs.containsKey("NORMAL")){normal=obj(accessors.get(integer(attrs.get("NORMAL"))));if(get(normal,"count",-1)!=count||get(normal,"componentType",-1)!=5126||!"VEC3".equals(normal.get("type")))throw new IOException("Invalid vertex normal");}
                var local=new ArrayList<Vertex>();var transformed=new ArrayList<Vertex>();double[] matrix=world[ni];
                for(int i=0;i<count;i++){
                    double x=component(b,binaryStart,binaryLength,views,pos,i,0,3),y=component(b,binaryStart,binaryLength,views,pos,i,1,3),z=component(b,binaryStart,binaryLength,views,pos,i,2,3),u=component(b,binaryStart,binaryLength,views,uv,i,0,2),v=component(b,binaryStart,binaryLength,views,uv,i,1,2);Normal n=normal==null?null:new Normal(component(b,binaryStart,binaryLength,views,normal,i,0,3),component(b,binaryStart,binaryLength,views,normal,i,1,3),component(b,binaryStart,binaryLength,views,normal,i,2,3));local.add(new Vertex(x,y,z,u,v,n));
                    double rx=matrix[0]*x+matrix[1]*y+matrix[2]*z+matrix[3],ry=matrix[4]*x+matrix[5]*y+matrix[6]*z+matrix[7],rz=matrix[8]*x+matrix[9]*y+matrix[10]*z+matrix[11];Normal transformedNormal=null;
                    if(n!=null){double a=matrix[0],bb=matrix[1],c=matrix[2],d=matrix[4],e=matrix[5],f=matrix[6],g=matrix[8],h=matrix[9],j=matrix[10],det=a*(e*j-f*h)-bb*(d*j-f*g)+c*(d*h-e*g);if(Math.abs(det)<1e-12)throw new IOException("Singular normal transform");double nx=((e*j-f*h)*n.x()+(f*g-d*j)*n.y()+(d*h-e*g)*n.z())/det,ny=((c*h-bb*j)*n.x()+(a*j-c*g)*n.y()+(bb*g-a*h)*n.z())/det,nz=((bb*f-c*e)*n.x()+(c*d-a*f)*n.y()+(a*e-bb*d)*n.z())/det;double length=Math.sqrt(nx*nx+ny*ny+nz*nz);if(!Double.isFinite(length)||length<1e-8)throw new IOException("Invalid normal length");transformedNormal=new Normal(nx/length,ny/length,nz/length);}
                    transformed.add(new Vertex(rx,ry,rz,u,v,transformedNormal));
                }
                int component=get(indices,"componentType",-1);if(component!=5121&&component!=5123&&component!=5125)throw new IOException("Unsupported index encoding");var sourceIndices=new ArrayList<Integer>();for(int i=0;i<ic;i++){int value=index(b,binaryStart,binaryLength,views,indices,i,component);if(value<0||value>=count)throw new IOException("Index out of range");sourceIndices.add(value);}primitives.add(new Primitive(ni,pi,position,local,transformed,sourceIndices,material,identities(primitive,"beckenburgPositionIds",count,b,binaryStart,binaryLength,views),identities(primitive,"beckenburgFaceIds",ic/3,b,binaryStart,binaryLength,views)));
            }
        }
        if(primitives.isEmpty())throw new IOException("No drawable triangles");return new Document(bytes,Arrays.copyOfRange(bytes,binaryStart,binaryStart+storedLength),root,materials,primitives,world);
    }
    static List<Long> identities(Map<String,Object> primitive,String key,int expected,ByteBuffer binary,int start,int length,List<Object> views)throws IOException{
        Map<?,?> metadata=primitive.get("extras") instanceof Map<?,?> extras?extras:Map.of();if(primitive.get("extensions") instanceof Map<?,?> extensions&&extensions.get("BECK_native_mesh") instanceof Map<?,?> nativeData)metadata=nativeData;if(!metadata.containsKey(key))return List.of();var descriptor=obj(metadata.get(key));if(get(descriptor,"componentType",-1)!=5125||get(descriptor,"count",-1)!=expected)throw new IOException("Invalid native GLB identity metadata");int view=get(descriptor,"bufferView",-1);if(view<0||view>=views.size())throw new IOException("Native GLB identity buffer missing");var accessor=new LinkedHashMap<String,Object>();accessor.put("bufferView",view);var result=new ArrayList<Long>();for(int i=0;i<expected;i++)result.add(Integer.toUnsignedLong(index(binary,start,length,views,accessor,i,5125)));return List.copyOf(result);
    }
    private static boolean wrap(int value){return value==10497||value==33071||value==33648;}
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object value){return (Map<String,Object>)value;}
    @SuppressWarnings("unchecked") static List<Object> arr(Object value){return (List<Object>)value;}
    private static double num(Object value){return ((Number)value).doubleValue();}
    private static int integer(Object value){if(value instanceof java.math.BigDecimal decimal)return decimal.intValueExact();double d=num(value);if(!Double.isFinite(d)||d!=(int)d)throw new IllegalArgumentException("Expected integral glTF index");return (int)d;}
    private static int get(Map<String,Object> map,String key,int fallback){return map.containsKey(key)?integer(map.get(key)):fallback;}
    private static double[] vec(Object value,double... fallback){if(value==null)return fallback;var values=arr(value);if(values.size()!=fallback.length)throw new IllegalArgumentException("Invalid glTF vector");double[] result=new double[fallback.length];for(int i=0;i<result.length;i++){result[i]=num(values.get(i));if(!Double.isFinite(result[i]))throw new IllegalArgumentException("Invalid glTF vector");}return result;}
    private static double[] matrix(Map<String,Object> node){
        if(node.containsKey("matrix")){
            if(node.get("matrix")==null)throw new IllegalArgumentException("Null glTF node matrix");
            if(node.containsKey("translation")||node.containsKey("rotation")||node.containsKey("scale"))throw new IllegalArgumentException("glTF matrix and TRS are mutually exclusive");
            double[] source=vec(node.get("matrix"),IDENTITY);double[] matrix=new double[16];for(int r=0;r<4;r++)for(int c=0;c<4;c++)matrix[r*4+c]=source[c*4+r];
            if(matrix[12]!=0||matrix[13]!=0||matrix[14]!=0||matrix[15]!=1)throw new IllegalArgumentException("Non-affine glTF node matrix");return matrix;
        }
        double[] t=vec(node.get("translation"),0,0,0),
            s=vec(node.get("scale"),1,1,1),
            q=vec(node.get("rotation"),0,0,0,1);
        double x=q[0],y=q[1],z=q[2],w=q[3];
        return new double[]{
            (1-2*(y*y+z*z))*s[0], 2*(x*y-z*w)*s[1], 2*(x*z+y*w)*s[2],t[0],
            2*(x*y+z*w)*s[0],(1-2*(x*x+z*z))*s[1],2*(y*z-x*w)*s[2],t[1],
            2*(x*z-y*w)*s[0],2*(y*z+x*w)*s[1],(1-2*(x*x+y*y))*s[2],t[2],
            0,0,0,1};
    }
    private static double[] combine(double[] a,double[] b){
        double[] c=new double[16];
        for(int r=0;r<4;r++)for(int col=0;col<4;col++)
            for(int k=0;k<4;k++)c[r*4+col]+=a[r*4+k]*b[k*4+col];
        return c;
    }
    private static final double[] IDENTITY={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    private static void tree(List<Object> nodes,int index,double[] parent,
                             double[][] matrices,boolean[] active,int depth)throws IOException{
        if(index<0||index>=nodes.size()||depth>nodes.size()||active[index]
                ||matrices[index]!=null)
            throw new IOException("Invalid glTF node hierarchy");
        active[index]=true;
        Map<String,Object> node=obj(nodes.get(index));
        double[] world=combine(parent,matrix(node));
        matrices[index]=world;
        if(node.containsKey("children"))for(Object child:arr(node.get("children")))
            tree(nodes,integer(child),world,matrices,active,depth+1);
        active[index]=false;
    }
    private static double[][] sceneTransforms(Map<String,Object> root,
                                               List<Object> nodes)throws IOException{
        double[][] result=new double[nodes.size()][];
        boolean[] active=new boolean[nodes.size()];
        Object scenesValue=root.get("scenes");
        if(scenesValue==null)throw new IOException("glTF missing scene");
        List<Object> scenes=arr(scenesValue);
        int chosen=get(root,"scene",0);
        if(chosen<0||chosen>=scenes.size())throw new IOException("Invalid glTF scene");
        Map<String,Object> scene=obj(scenes.get(chosen));
        if(scene.get("nodes")==null)throw new IOException("glTF scene missing root nodes");
        for(Object n:arr(scene.get("nodes")))
            tree(nodes,integer(n),IDENTITY,result,active,0);
        return result;
    }
    private static int index(ByteBuffer b,int start,int bytes,List<Object> views,
                              Map<String,Object> accessor,int n,int component)throws IOException{
        if(accessor.containsKey("sparse"))throw new IOException("Sparse indices");
        Map<String,Object> view=obj(views.get(integer(accessor.get("bufferView"))));
        int size=component==5125?4:component==5123?2:1,
            stride=get(view,"byteStride",size);
        if(get(view,"buffer",0)!=0||stride<size||stride%size!=0||get(view,"byteOffset",0)<0||get(accessor,"byteOffset",0)<0)throw new IOException("Invalid index view/stride/offset");
        long addr=(long)get(view,"byteOffset",0)+get(accessor,"byteOffset",0)+(long)n*stride;
        if(addr<0||addr+size>bytes||addr+size>(long)get(view,"byteOffset",0)+get(view,"byteLength",0))
            throw new IOException("Index buffer bounds");
        int p=start+(int)addr;
        return component==5125?b.getInt(p):component==5123?Short.toUnsignedInt(b.getShort(p)):
            Byte.toUnsignedInt(b.get(p));
    }
    private static double component(ByteBuffer b,int start,int bytes,List<Object> views,
                        Map<String,Object> accessor,int n,int c,int dims)throws IOException{
        if(accessor.containsKey("sparse"))throw new IOException("Sparse vertices");
        Map<String,Object> view=obj(views.get(integer(accessor.get("bufferView"))));
        int stride=get(view,"byteStride",dims*4);
        if(get(view,"buffer",0)!=0||stride<dims*4||stride%4!=0||get(view,"byteOffset",0)<0||get(accessor,"byteOffset",0)<0||Boolean.TRUE.equals(accessor.get("normalized")))throw new IOException("Invalid float vertex view/stride/offset");
        long addr=(long)get(view,"byteOffset",0)+get(accessor,"byteOffset",0)+(long)n*stride+c*4;
        if(addr<0||addr+4>bytes||addr+4>(long)get(view,"byteOffset",0)+get(view,"byteLength",0))
            throw new IOException("Vertex bounds");
        return b.getFloat(start+(int)addr);
    }

}
