package org.beckenburg.editor;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.beckenburg.editor.EditorMeshPipeline.*;

/** Compile actual native face/material bindings into the existing runtime GLB contract. */
final class EditorMaterialGlbWriter {
    private EditorMaterialGlbWriter(){}
    private record Position(String part,long source){}
    private record Attributes(Position position,double u,double v,Normal normal){}
    private record Run(String part,String material,int start,int count){}
    private static Map<String,Object> obj(Object... pairs){var out=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    private static int pad4(int value){return (value+3)&~3;}
    static byte[] encode(Mesh mesh)throws IOException{
        if(mesh.triangles().isEmpty()||mesh.triangles().size()>16000)throw new IOException("Material export needs1..16000 triangles under existing runtime limits");
        var definitions=new ArrayList<>(mesh.materials());if(mesh.triangles().stream().anyMatch(t->t.material().isEmpty()))definitions.add(EditorMeshMaterial.fallback());
        var materialIndex=new LinkedHashMap<String,Integer>();for(var material:definitions)materialIndex.put(material.key(),materialIndex.size());
        var sourcePositions=new LinkedHashMap<Position,Vertex>();var unique=new LinkedHashMap<Attributes,Integer>();var vertices=new ArrayList<Vertex>();var positionIds=new ArrayList<Integer>();var densePositions=new LinkedHashMap<Position,Integer>();var indices=new ArrayList<Integer>();var runs=new ArrayList<Run>();int ordinal=0;
        try{
            for(var triangle:mesh.triangles()){
                var flat=EditorMeshNormals.face(triangle);int start=indices.size();
                for(var corner:List.of(triangle.a(),triangle.b(),triangle.c())){
                    long source=mesh.cornerVertexIds().isEmpty()?ordinal:mesh.cornerVertexIds().get(ordinal);ordinal++;
                    var position=new Position(triangle.part(),source);var prior=sourcePositions.putIfAbsent(position,corner);
                    if(prior!=null&&(prior.x()!=corner.x()||prior.y()!=corner.y()||prior.z()!=corner.z()))throw new IOException("Conflicting native position identity");
                    var normal=EditorMeshNormals.unit(corner.normal()==null?flat:corner.normal());var key=new Attributes(position,corner.u(),corner.v(),normal);Integer index=unique.get(key);
                    if(index==null){index=vertices.size();unique.put(key,index);vertices.add(new Vertex(corner.x(),corner.y(),corner.z(),corner.u(),corner.v(),normal));positionIds.add(densePositions.computeIfAbsent(position,k->densePositions.size()));}indices.add(index);
                }
                if(!runs.isEmpty()){var last=runs.get(runs.size()-1);if(last.part().equals(triangle.part())&&last.material().equals(triangle.material())){runs.set(runs.size()-1,new Run(last.part(),last.material(),last.start(),last.count()+3));continue;}}
                runs.add(new Run(triangle.part(),triangle.material(),start,3));
            }
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid material mesh geometry/normal",invalid);}
        if(vertices.size()>50000||runs.size()>806)throw new IOException("Material primitive/accessor budget exceeded; split the model");
        var binary=new ByteArrayOutputStream();var views=new ArrayList<Object>();var accessors=new ArrayList<Object>();var primitives=new ArrayList<Object>();var drawOrders=new IdentityHashMap<Object,List<Integer>>();
        // Primitive-local accessors keep decoder work proportional to actual corners, rather than
        // repeatedly decoding the entire model for each alternating material/part run.
        for(var run:runs){
            var local=new LinkedHashMap<Integer,Integer>();var localVertices=new ArrayList<Vertex>();var localIds=new ArrayList<Integer>();var localIndices=new ArrayList<Integer>();
            for(int i=run.start();i<run.start()+run.count();i++){int global=indices.get(i);Integer index=local.get(global);if(index==null){index=local.size();local.put(global,index);localVertices.add(vertices.get(global));localIds.add(positionIds.get(global));}localIndices.add(index);}
            int positionAccessor=accessors.size();addVertices(binary,views,accessors,localVertices,0);addVertices(binary,views,accessors,localVertices,1);addVertices(binary,views,accessors,localVertices,2);
            align(binary);int indexStart=binary.size();var indexData=ByteBuffer.allocate(localIndices.size()*2).order(ByteOrder.LITTLE_ENDIAN);for(int index:localIndices)indexData.putShort((short)index);binary.write(indexData.array());int indexView=views.size();views.add(obj("buffer",0,"byteOffset",indexStart,"byteLength",indexData.capacity()));
            int indexAccessor=accessors.size();accessors.add(obj("bufferView",indexView,"componentType",5123,"count",localIndices.size(),"type","SCALAR"));
            align(binary);int identityStart=binary.size();var identities=ByteBuffer.allocate(localIds.size()*4).order(ByteOrder.LITTLE_ENDIAN);for(int id:localIds)identities.putInt(id);binary.write(identities.array());int identityView=views.size();views.add(obj("buffer",0,"byteOffset",identityStart,"byteLength",identities.capacity()));
            var primitive=obj("attributes",obj("POSITION",positionAccessor,"TEXCOORD_0",positionAccessor+1,"NORMAL",positionAccessor+2),"indices",indexAccessor,"material",materialIndex.get(run.material()),"mode",4,"extras",obj("beckenburgPart",run.part(),"beckenburgPositionIds",obj("bufferView",identityView,"componentType",5125,"count",localIds.size())));
            if(!mesh.frames().isEmpty()){align(binary);int offset=binary.size();var faces=ByteBuffer.allocate(run.count()/3*4).order(ByteOrder.LITTLE_ENDIAN);for(int i=run.start()/3;i<(run.start()+run.count())/3;i++){long id=mesh.faceIds().isEmpty()?i:mesh.faceIds().get(i);if(id>0xffffffffL)throw new IOException("Local-frame GLB native face identity exceeds uint32");faces.putInt((int)id);}binary.write(faces.array());int view=views.size();views.add(obj("buffer",0,"byteOffset",offset,"byteLength",faces.capacity()));primitive.put("extensions",obj("BECK_native_mesh",obj("beckenburgPositionIds",obj("bufferView",identityView,"componentType",5125,"count",localIds.size()),"beckenburgFaceIds",obj("bufferView",view,"componentType",5125,"count",run.count()/3))));}
            var draw=new ArrayList<Integer>();for(int index=run.start()/3;index<(run.start()+run.count())/3;index++)draw.add(index);drawOrders.put(primitive,draw);primitives.add(primitive);
        }
        var images=new ArrayList<Object>();var textures=new ArrayList<Object>();var samplers=new ArrayList<Object>();var materials=new ArrayList<Object>();var imageIndices=new LinkedHashMap<String,Integer>();
        for(var material:definitions){
            byte[] png=material.png();String hash=EditorMeshPipeline.sourceFingerprint(png);Integer image=imageIndices.get(hash);
            if(image==null){if(images.size()>=64)throw new IOException("Actual runtime image count exceeded");align(binary);int offset=binary.size();binary.write(png);int view=views.size();views.add(obj("buffer",0,"byteOffset",offset,"byteLength",png.length));image=images.size();images.add(obj("bufferView",view,"mimeType","image/png"));imageIndices.put(hash,image);}
            int sampler=samplers.size();samplers.add(obj("wrapS",material.wrapS(),"wrapT",material.wrapT(),"magFilter",9728,"minFilter",9728));int texture=textures.size();textures.add(obj("source",image,"sampler",sampler));var color=material.color();
            materials.add(obj("name",material.name(),"pbrMetallicRoughness",obj("baseColorTexture",obj("index",texture),"baseColorFactor",List.of(color.r(),color.g(),color.b(),color.a()),"metallicFactor",0,"roughnessFactor",1),"alphaMode",material.alpha().name(),"alphaCutoff",material.cutoff(),"extras",obj("beckenburgMaterialKey",material.key(),"beckenburgUvOrigin",material.origin().name(),"beckenburgSourceProperties",material.sourceProperties())));
        }
        align(binary);var root=obj("asset",obj("version","2.0","generator","Beckenburg Native Material Compiler"),"scene",0,"scenes",List.of(obj("nodes",List.of(0))),"nodes",List.of(obj("name",mesh.name(),"mesh",0)),"meshes",List.of(obj("name",mesh.name(),"primitives",primitives)),"buffers",List.of(obj("byteLength",binary.size())),"bufferViews",views,"accessors",accessors,"images",images,"textures",textures,"samplers",samplers,"materials",materials);
        if(!mesh.frames().isEmpty()){
            var groups=new LinkedHashMap<String,List<Object>>();for(var primitive:primitives){String part=(String)EditorGlbReader.obj(EditorGlbReader.obj(primitive).get("extras")).get("beckenburgPart");groups.computeIfAbsent(part,k->new ArrayList<>()).add(primitive);}if(groups.size()>128)throw new IOException("Local-frame GLB exceeds actual128-node runtime budget");
            var nodes=new ArrayList<Object>();var meshes=new ArrayList<Object>();var roots=new ArrayList<Integer>();for(var group:groups.entrySet()){roots.add(nodes.size());nodes.add(obj("name",group.getKey(),"mesh",meshes.size()));meshes.add(obj("name",group.getKey(),"primitives",group.getValue()));}root.put("nodes",nodes);root.put("meshes",meshes);root.put("scenes",List.of(obj("nodes",roots)));root.put("extensionsUsed",new ArrayList<>(List.of("BECK_native_mesh")));int node=0;for(var group:groups.entrySet()){var frame=mesh.frames().get(group.getKey());if(frame!=null)EditorMeshFrame.attach(root,node,frame);var draw=new ArrayList<Integer>();for(var primitive:group.getValue())draw.addAll(drawOrders.get(primitive));EditorMeshOrder.attach(root,node,draw);node++;}
        }
        if(views.size()>4096||accessors.size()>4096)throw new IOException("Material GLB exceeds actual buffer/accessor budget");
        byte[] json=EditorGlbJson.write(root).getBytes(StandardCharsets.UTF_8);int jsonSize=pad4(json.length),size=12+8+jsonSize+8+binary.size();if(size>4_000_000)throw new IOException("Material GLB exceeds actual4MiB runtime budget");
        var result=ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);result.putInt(0x46546c67).putInt(2).putInt(size).putInt(jsonSize).putInt(0x4e4f534a).put(json);while(result.position()<20+jsonSize)result.put((byte)' ');result.putInt(binary.size()).putInt(0x004e4942).put(binary.toByteArray());return result.array();
    }
    private static void addVertices(ByteArrayOutputStream bytes,List<Object> views,List<Object> accessors,List<Vertex> vertices,int attribute)throws IOException{
        align(bytes);int start=bytes.size(),dims=attribute==1?2:3;var buffer=ByteBuffer.allocate(vertices.size()*dims*4).order(ByteOrder.LITTLE_ENDIAN);double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var v:vertices){double[] values=attribute==0?new double[]{v.x(),v.y(),v.z()}:attribute==1?new double[]{v.u(),v.v()}:new double[]{v.normal().x(),v.normal().y(),v.normal().z()};for(int i=0;i<dims;i++){float value=(float)values[i];if(!Float.isFinite(value))throw new IOException("Vertex attribute exceeds actual float runtime range");buffer.putFloat(value);min[i]=Math.min(min[i],value);max[i]=Math.max(max[i],value);}}
        bytes.write(buffer.array());int view=views.size();views.add(obj("buffer",0,"byteOffset",start,"byteLength",buffer.capacity()));var accessor=obj("bufferView",view,"componentType",5126,"count",vertices.size(),"type",dims==2?"VEC2":"VEC3");if(attribute==0){accessor.put("min",List.of(min[0],min[1],min[2]));accessor.put("max",List.of(max[0],max[1],max[2]));}accessors.add(accessor);
    }
    private static void align(ByteArrayOutputStream bytes){while((bytes.size()&3)!=0)bytes.write(0);}
}
