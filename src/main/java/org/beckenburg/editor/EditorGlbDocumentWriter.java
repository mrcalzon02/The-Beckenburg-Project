package org.beckenburg.editor;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.beckenburg.editor.EditorMeshPipeline.*;

/** Source-backed GLB compilation. Original nodes/pivots/material metadata and binary data remain intact. */
final class EditorGlbDocumentWriter {
    private EditorGlbDocumentWriter(){}
    private record Origin(EditorGlbReader.Primitive primitive,int triangle){}
    private record Face(long id,Triangle triangle,List<Long> positionIds,Origin origin){}
    private record Run(int sourcePrimitive,String material,List<Face> faces){}
    private static Map<String,Object> obj(Object... values){var result=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)result.put((String)values[i],values[i+1]);return result;}
    private static Object copy(Object value){if(value instanceof Map<?,?> m){var result=new LinkedHashMap<String,Object>();for(var e:m.entrySet())result.put((String)e.getKey(),copy(e.getValue()));return result;}if(value instanceof List<?> l){var result=new ArrayList<Object>();for(var item:l)result.add(copy(item));return result;}return value;}
    static byte[] encode(Mesh mesh)throws IOException{
        var source=mesh.glbSource();source.requireEditable();var baseline=source.mesh(mesh.name());
        if(mesh.faceIds().size()!=mesh.triangles().size()||mesh.cornerVertexIds().size()!=mesh.triangles().size()*3)throw new IOException("Source GLB export requires authoritative native face/position bindings");
        if(mesh.triangles().equals(baseline.triangles())&&mesh.materials().equals(baseline.materials())&&mesh.cornerVertexIds().equals(baseline.cornerVertexIds())&&mesh.faceIds().equals(baseline.faceIds())&&mesh.frames().equals(baseline.frames()))return source.original();
        for(var material:source.nativeMaterials())if(!mesh.materials().contains(material))throw new IOException("Source GLB original material definitions must remain available");
        var root=EditorGlbReader.obj(copy(source.root()));var nodes=EditorGlbReader.arr(root.get("nodes"));var meshes=EditorGlbReader.arr(root.get("meshes"));var views=EditorGlbReader.arr(root.get("bufferViews"));var accessors=EditorGlbReader.arr(root.get("accessors"));
        var binary=new ByteArrayOutputStream();binary.write(source.binary());align(binary);
        var materialIndices=new LinkedHashMap<String,Integer>();for(var material:source.materials())materialIndices.put(EditorGlbReader.key(material.index()),material.index());
        var materials=EditorGlbReader.arr(root.get("materials"));var images=EditorGlbReader.arr(root.get("images"));var textures=EditorGlbReader.arr(root.get("textures"));var samplers=root.get("samplers")==null?new ArrayList<Object>():EditorGlbReader.arr(root.get("samplers"));root.put("samplers",samplers);
        var additions=new ArrayList<>(mesh.materials());if(mesh.triangles().stream().anyMatch(t->t.material().isEmpty()))additions.add(EditorMeshMaterial.fallback());
        for(var material:additions)if(!materialIndices.containsKey(material.key())){
            int image=images.size(),view=appendView(binary,views,material.png());images.add(obj("bufferView",view,"mimeType","image/png"));int sampler=samplers.size();samplers.add(obj("wrapS",material.wrapS(),"wrapT",material.wrapT(),"magFilter",9728,"minFilter",9728));int texture=textures.size();textures.add(obj("source",image,"sampler",sampler));var color=material.color();
            materialIndices.put(material.key(),materials.size());materials.add(obj("name",material.name(),"pbrMetallicRoughness",obj("baseColorTexture",obj("index",texture),"baseColorFactor",List.of(color.r(),color.g(),color.b(),color.a()),"metallicFactor",0,"roughnessFactor",1),"alphaMode",material.alpha().name(),"alphaCutoff",material.cutoff(),"extras",obj("beckenburgMaterialKey",material.key(),"beckenburgUvOrigin",material.origin().name(),"beckenburgSourceProperties",material.sourceProperties())));
        }
        var originalParts=new LinkedHashMap<String,Integer>();var origins=new HashMap<Long,Origin>();long ordinal=0;
        for(var primitive:source.primitives())originalParts.put(primitive.part(),primitive.node());for(var face:source.faces())origins.put(face.identity(),new Origin(face.primitive(),face.triangle()));
        var parts=new LinkedHashMap<String,List<Face>>();for(String part:originalParts.keySet())parts.put(part,new ArrayList<>());
        for(int i=0;i<mesh.triangles().size();i++){var triangle=mesh.triangles().get(i);long id=mesh.faceIds().get(i);parts.computeIfAbsent(triangle.part(),k->new ArrayList<>()).add(new Face(id,triangle,mesh.cornerVertexIds().subList(i*3,i*3+3),origins.get(id)));}
        var originalNodes=EditorGlbReader.arr(source.root().get("nodes"));var sourceMeshes=EditorGlbReader.arr(source.root().get("meshes"));
        for(var part:parts.entrySet()){
            String name=part.getKey();List<Face> faces=part.getValue();Integer nodeIndex=originalParts.get(name);
            if(nodeIndex!=null){var before=new ArrayList<Triangle>();var beforeIds=new ArrayList<Long>();var beforeFaces=new ArrayList<Long>();for(int i=0;i<baseline.triangles().size();i++)if(baseline.triangles().get(i).part().equals(name)){before.add(baseline.triangles().get(i));beforeIds.addAll(baseline.cornerVertexIds().subList(i*3,i*3+3));beforeFaces.add(baseline.faceIds().get(i));}
                var current=faces.stream().map(Face::triangle).toList();var currentIds=faces.stream().flatMap(f->f.positionIds().stream()).toList();if(current.equals(before)&&currentIds.equals(beforeIds)&&faces.stream().map(Face::id).toList().equals(beforeFaces))continue;
            }
            if(nodeIndex!=null&&faces.isEmpty()){EditorGlbReader.obj(nodes.get(nodeIndex)).remove("mesh");continue;}if(faces.isEmpty())continue;
            double[] world=nodeIndex==null?new double[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}:source.world(nodeIndex);var inverse=inverse(world);
            var runs=new ArrayList<Run>();for(var face:faces){int primitive=face.origin()!=null&&face.origin().primitive().part().equals(name)?face.origin().primitive().primitive():-1;if(!runs.isEmpty()){var previous=runs.get(runs.size()-1);if(previous.sourcePrimitive()==primitive&&previous.material().equals(face.triangle().material())){previous.faces().add(face);continue;}}var list=new ArrayList<Face>();list.add(face);runs.add(new Run(primitive,face.triangle().material(),list));}
            var output=new ArrayList<Object>();for(var run:runs){Map<String,Object> template=Map.of();if(nodeIndex!=null&&run.sourcePrimitive()>=0){int originalMesh=((Number)EditorGlbReader.obj(originalNodes.get(nodeIndex)).get("mesh")).intValue();template=EditorGlbReader.obj(EditorGlbReader.arr(EditorGlbReader.obj(sourceMeshes.get(originalMesh)).get("primitives")).get(run.sourcePrimitive()));}output.add(primitive(run,template,world,inverse,binary,views,accessors,materialIndices));}
            Map<String,Object> edited;
            if(nodeIndex==null){edited=obj("name",name,"primitives",output);int meshIndex=meshes.size();meshes.add(edited);int index=nodes.size();nodes.add(obj("name",name,"mesh",meshIndex));originalParts.put(name,index);int scene=((Number)root.getOrDefault("scene",0)).intValue();EditorGlbReader.arr(EditorGlbReader.obj(EditorGlbReader.arr(root.get("scenes")).get(scene)).get("nodes")).add(index);}
            else{int originalMesh=((Number)EditorGlbReader.obj(originalNodes.get(nodeIndex)).get("mesh")).intValue();edited=EditorGlbReader.obj(copy(sourceMeshes.get(originalMesh)));edited.put("primitives",output);int users=0;for(var node:originalNodes)if(EditorGlbReader.obj(node).get("mesh") instanceof Number n&&n.intValue()==originalMesh)users++;if(users==1)meshes.set(originalMesh,edited);else{EditorGlbReader.obj(nodes.get(nodeIndex)).put("mesh",meshes.size());meshes.add(edited);}}
        }
        for(var entry:mesh.frames().entrySet()){Integer node=originalParts.get(entry.getKey());if(node==null)throw new IOException("Local frame has no original/generated GLB node");EditorMeshFrame.attach(root,node,entry.getValue());}
        for(var entry:source.frames().entrySet())if(!mesh.frames().containsKey(entry.getKey())){Integer node=originalParts.get(entry.getKey());if(node!=null&&EditorGlbReader.obj(nodes.get(node)).containsKey("mesh")){var target=EditorGlbReader.obj(nodes.get(node));var extensions=EditorGlbReader.obj(target.get("extensions"));extensions.remove("BECK_editor_frame");if(extensions.isEmpty())target.remove("extensions");}}
        if(nodes.size()>128||meshes.size()>128||views.size()>4096||accessors.size()>4096||images.size()>64)throw new IOException("Edited GLB exceeds existing runtime resource budget");
        var used=root.get("extensionsUsed")==null?new ArrayList<Object>():EditorGlbReader.arr(root.get("extensionsUsed"));if(!used.contains("BECK_native_mesh"))used.add("BECK_native_mesh");root.put("extensionsUsed",used);
        align(binary);EditorMeshOrder.source(root,binary.toByteArray(),mesh,originalParts);var buffers=EditorGlbReader.arr(root.get("buffers"));if(buffers.size()!=1)throw new IOException("Source GLB external/multiple buffer export unsupported");EditorGlbReader.obj(buffers.get(0)).put("byteLength",binary.size());return container(root,binary.toByteArray(),source.trailing());
    }
    private static Map<String,Object> primitive(Run run,Map<String,Object> template,double[] world,double[] inverse,ByteArrayOutputStream binary,List<Object> views,List<Object> accessors,Map<String,Integer> materials)throws IOException{
        record Key(Vertex vertex,long identity){}
        var vertices=new ArrayList<Vertex>();var ids=new ArrayList<Long>();var indices=new ArrayList<Integer>();var unique=new LinkedHashMap<Key,Integer>();boolean normals=run.faces().stream().anyMatch(f->f.triangle().a().normal()!=null||f.triangle().b().normal()!=null||f.triangle().c().normal()!=null);
        for(var face:run.faces()){
            var triangle=face.triangle();Normal flat=null;try{flat=EditorMeshNormals.face(triangle);}catch(IllegalArgumentException bad){throw new IOException("Source GLB export needs nondegenerate native geometry",bad);}var corners=List.of(triangle.a(),triangle.b(),triangle.c());
            for(int c=0;c<3;c++){
                var v=corners.get(c);Vertex originalWorld=null,originalLocal=null;if(face.origin()!=null){var origin=face.origin();int source=origin.primitive().indices().get(origin.triangle()*3+c);originalWorld=origin.primitive().world().get(source);originalLocal=origin.primitive().local().get(source);}
                double x=inverse[0]*v.x()+inverse[1]*v.y()+inverse[2]*v.z()+inverse[3],y=inverse[4]*v.x()+inverse[5]*v.y()+inverse[6]*v.z()+inverse[7],z=inverse[8]*v.x()+inverse[9]*v.y()+inverse[10]*v.z()+inverse[11];if(originalWorld!=null&&v.x()==originalWorld.x()&&v.y()==originalWorld.y()&&v.z()==originalWorld.z()){x=originalLocal.x();y=originalLocal.y();z=originalLocal.z();}
                Normal n=null;if(normals){if(originalWorld!=null&&Objects.equals(v.normal(),originalWorld.normal())&&originalLocal.normal()!=null)n=originalLocal.normal();else{var direction=v.normal()==null?flat:v.normal();n=EditorMeshNormals.unit(new Normal(world[0]*direction.x()+world[4]*direction.y()+world[8]*direction.z(),world[1]*direction.x()+world[5]*direction.y()+world[9]*direction.z(),world[2]*direction.x()+world[6]*direction.y()+world[10]*direction.z()));}}
                var local=new Vertex(x,y,z,v.u(),v.v(),n);long id=face.positionIds().get(c);var key=new Key(local,id);Integer index=unique.get(key);if(index==null){index=vertices.size();unique.put(key,index);vertices.add(local);ids.add(id);}indices.add(index);
            }
        }
        if(vertices.size()>50000||indices.size()>150000)throw new IOException("Edited GLB primitive exceeds actual vertex/index limits");var attrs=new LinkedHashMap<String,Object>();attrs.put("POSITION",attribute(vertices,0,binary,views,accessors));attrs.put("TEXCOORD_0",attribute(vertices,1,binary,views,accessors));if(normals)attrs.put("NORMAL",attribute(vertices,2,binary,views,accessors));
        var index=ByteBuffer.allocate(indices.size()*2).order(ByteOrder.LITTLE_ENDIAN);for(int value:indices)index.putShort((short)value);int indexView=appendView(binary,views,index.array()),indexAccessor=accessors.size();accessors.add(obj("bufferView",indexView,"componentType",5123,"count",indices.size(),"type","SCALAR"));
        var positionIds=ByteBuffer.allocate(ids.size()*4).order(ByteOrder.LITTLE_ENDIAN);for(long id:ids){if(id>0xffffffffL)throw new IOException("Native position ID exceeds source GLB identity encoding");positionIds.putInt((int)id);}int positionView=appendView(binary,views,positionIds.array());
        var faceIds=ByteBuffer.allocate(run.faces().size()*4).order(ByteOrder.LITTLE_ENDIAN);for(var face:run.faces()){if(face.id()>0xffffffffL)throw new IOException("Native face ID exceeds GLB identity encoding");faceIds.putInt((int)face.id());}int faceView=appendView(binary,views,faceIds.array());
        var result=EditorGlbReader.obj(copy(template));result.put("attributes",attrs);result.put("indices",indexAccessor);result.put("material",materials.get(run.material()));result.put("mode",4);Map<String,Object> extensions=result.get("extensions") instanceof Map<?,?> m?EditorGlbReader.obj(m):new LinkedHashMap<>();extensions.put("BECK_native_mesh",obj("beckenburgPositionIds",obj("bufferView",positionView,"componentType",5125,"count",ids.size()),"beckenburgFaceIds",obj("bufferView",faceView,"componentType",5125,"count",run.faces().size())));result.put("extensions",extensions);return result;
    }
    private static int attribute(List<Vertex> vertices,int attribute,ByteArrayOutputStream binary,List<Object> views,List<Object> accessors)throws IOException{
        int dims=attribute==1?2:3;var bytes=ByteBuffer.allocate(vertices.size()*dims*4).order(ByteOrder.LITTLE_ENDIAN);double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var vertex:vertices){double[] values=attribute==0?new double[]{vertex.x(),vertex.y(),vertex.z()}:attribute==1?new double[]{vertex.u(),vertex.v()}:new double[]{vertex.normal().x(),vertex.normal().y(),vertex.normal().z()};for(int i=0;i<dims;i++){float value=(float)values[i];if(!Float.isFinite(value))throw new IOException("Edited GLB attribute exceeds float range");bytes.putFloat(value);min[i]=Math.min(min[i],value);max[i]=Math.max(max[i],value);}}
        int view=appendView(binary,views,bytes.array()),accessor=accessors.size();var result=obj("bufferView",view,"componentType",5126,"count",vertices.size(),"type",dims==2?"VEC2":"VEC3");if(attribute==0){result.put("min",List.of(min[0],min[1],min[2]));result.put("max",List.of(max[0],max[1],max[2]));}accessors.add(result);return accessor;
    }
    private static double[] inverse(double[] m)throws IOException{
        double a=m[0],b=m[1],c=m[2],d=m[4],e=m[5],f=m[6],g=m[8],h=m[9],j=m[10],det=a*(e*j-f*h)-b*(d*j-f*g)+c*(d*h-e*g);if(!Double.isFinite(det)||Math.abs(det)<1e-12)throw new IOException("Singular original GLB node transform");double[] r={(e*j-f*h)/det,(c*h-b*j)/det,(b*f-c*e)/det,0,(f*g-d*j)/det,(a*j-c*g)/det,(c*d-a*f)/det,0,(d*h-e*g)/det,(b*g-a*h)/det,(a*e-b*d)/det,0,0,0,0,1};r[3]=-(r[0]*m[3]+r[1]*m[7]+r[2]*m[11]);r[7]=-(r[4]*m[3]+r[5]*m[7]+r[6]*m[11]);r[11]=-(r[8]*m[3]+r[9]*m[7]+r[10]*m[11]);return r;
    }
    private static void align(ByteArrayOutputStream stream){while((stream.size()&3)!=0)stream.write(0);}
    private static int appendView(ByteArrayOutputStream binary,List<Object> views,byte[] bytes)throws IOException{align(binary);int offset=binary.size();binary.write(bytes);int index=views.size();views.add(obj("buffer",0,"byteOffset",offset,"byteLength",bytes.length));return index;}
    static byte[] container(Map<String,Object> root,byte[] binary)throws IOException{return container(root,binary,new byte[0]);}
    static byte[] container(Map<String,Object> root,byte[] binary,byte[] trailing)throws IOException{
        byte[] json=EditorGlbJson.write(root).getBytes(StandardCharsets.UTF_8);int jsonLength=(json.length+3)&~3,binLength=(binary.length+3)&~3,total=28+jsonLength+binLength+trailing.length;if(total>4_000_000)throw new IOException("Edited GLB exceeds actual4MiB runtime budget");var result=ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);result.putInt(0x46546c67).putInt(2).putInt(total).putInt(jsonLength).putInt(0x4e4f534a).put(json);while(result.position()<20+jsonLength)result.put((byte)' ');result.putInt(binLength).putInt(0x004e4942).put(binary);while(result.position()<28+jsonLength+binLength)result.put((byte)0);result.put(trailing);return result.array();
    }
}
