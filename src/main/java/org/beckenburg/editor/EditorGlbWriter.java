package org.beckenburg.editor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/**
 * Game-compatible glTF 2.0 export for native geometry and source-backed GLB.
 * Original materials are retained; geometry-only imports use an explicit fallback.
 * Runtime validation uses the shared authoritative decoder.
 */
public final class EditorGlbWriter {
    private EditorGlbWriter(){}
    /** Readback goes through the same decoder used by actual gameplay. */
    public static void validate(EditorMeshPipeline.Mesh expected,byte[] bytes)throws IOException{
        var actual=EditorGlbReader.read(bytes);var faces=new java.util.HashMap<Long,EditorMeshPipeline.Triangle>();var ids=new java.util.HashMap<Long,java.util.List<Long>>();
        for(int i=0;i<expected.triangles().size();i++){long id=expected.faceIds().isEmpty()?i:expected.faceIds().get(i);faces.put(id,expected.triangles().get(i));if(!expected.cornerVertexIds().isEmpty())ids.put(id,expected.cornerVertexIds().subList(i*3,i*3+3));}
        var originalOffsets=new java.util.HashMap<String,Long>();int originalAccessors=0;long sourceFace=0;
        if(expected.glbSource()!=null){originalAccessors=EditorGlbReader.arr(expected.glbSource().root().get("accessors")).size();for(var p:expected.glbSource().primitives()){originalOffsets.put(p.node()+":"+p.primitive(),sourceFace);sourceFace+=p.indices().size()/3;}var before=EditorGlbReader.arr(expected.glbSource().root().get("materials"));var after=EditorGlbReader.arr(actual.root().get("materials"));for(int i=0;i<before.size();i++)if(!before.get(i).equals(after.get(i)))throw new IOException("GLB readback changed original material metadata");}
        var seen=new java.util.HashSet<Long>();long ordinal=0;
        for(var primitive:actual.primitives())for(int i=0;i<primitive.indices().size();i+=3){
            boolean generated=expected.glbSource()!=null&&primitive.positionAccessor()>=originalAccessors;
            long id=expected.glbSource()==null?ordinal++:generated?primitive.faceIds().get(i/3):originalOffsets.get(primitive.node()+":"+primitive.primitive())+i/3;
            var triangle=faces.get(id);if(triangle==null||!seen.add(id))throw new IOException("GLB readback lost/duplicated native face identity");var corners=java.util.List.of(triangle.a(),triangle.b(),triangle.c());
            for(int c=0;c<3;c++){int index=primitive.indices().get(i+c);var a=primitive.world().get(index);var b=corners.get(c);if(!near(a.x(),b.x())||!near(a.y(),b.y())||!near(a.z(),b.z())||!near(a.u(),b.u())||!near(a.v(),b.v()))throw new IOException("GLB readback changed native position/UV/winding");
                if(generated&&ids.containsKey(id)&&!primitive.positionIds().get(index).equals(ids.get(id).get(c)))throw new IOException("GLB readback changed native position connectivity");
                if(b.normal()!=null){var n=EditorMeshNormals.unit(b.normal());if(a.normal()==null||!near(a.normal().x(),n.x())||!near(a.normal().y(),n.y())||!near(a.normal().z(),n.z()))throw new IOException("GLB readback changed transformed corner normal");}
            }
            var material=expected.materials().stream().filter(m->m.key().equals(triangle.material())).findFirst().orElse(EditorMeshMaterial.fallback());var sample=actual.materials().get(primitive.material());var color=sample.color();var factor=material.color();String origin=sample.explicitOrigin()==null?sample.defaultOrigin():sample.explicitOrigin();
            if(!java.util.Arrays.equals(sample.png(),material.png())||!near(color[0],factor.r())||!near(color[1],factor.g())||!near(color[2],factor.b())||!near(color[3],factor.a())||sample.wrapS()!=material.wrapS()||sample.wrapT()!=material.wrapT()||!sample.alpha().equals(material.alpha().name())||!near(sample.cutoff(),material.cutoff())||!origin.equals(material.origin().name()))throw new IOException("GLB readback changed original PNG/material sampling");
        }
        if(seen.size()!=faces.size())throw new IOException("GLB readback face count differs from authoritative native state");
    }
    private static boolean near(double a,double b){return Math.abs(a-b)<=1e-5*Math.max(1,Math.max(Math.abs(a),Math.abs(b)));}
    public static byte[] encode(EditorMeshPipeline.Mesh mesh)throws IOException {
        if(mesh.glbSource()!=null)try{return EditorGlbDocumentWriter.encode(mesh);}catch(IllegalArgumentException|IndexOutOfBoundsException|ClassCastException invalid){throw new IOException("Invalid source-backed GLB candidate",invalid);}
        if(!mesh.materials().isEmpty())return EditorMaterialGlbWriter.encode(mesh);
        int count=mesh.triangles().size()*3;
        if(count<3||count>48000)throw new IOException(
            "Export needs 1..16000 triangles per model under existing 4 MiB GLB budget; reduce or split first");
        BufferedImage checker=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)
            checker.setRGB(x,y,(((x/4+y/4)&1)==0?0xff969c9d:0xff606d74));
        ByteArrayOutputStream image=new ByteArrayOutputStream();
        if(!ImageIO.write(checker,"png",image))throw new IOException("PNG image encoder unavailable");
        byte[] png=image.toByteArray();
        boolean normals=mesh.triangles().stream().anyMatch(t->t.a().normal()!=null||t.b().normal()!=null||t.c().normal()!=null);
        int posBytes=count*12,uvBytes=count*8,indexBytes=count*2,normalStart=pad4(posBytes+uvBytes+indexBytes),normalBytes=normals?count*12:0;
        int imageStart=pad4(normalStart+normalBytes);
        int binSize=pad4(imageStart+png.length);
        ByteBuffer binary=ByteBuffer.allocate(binSize).order(ByteOrder.LITTLE_ENDIAN);
        for(var t:mesh.triangles())for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            binary.putFloat((float)v.x());binary.putFloat((float)v.y());binary.putFloat((float)v.z());
        }
        for(var t:mesh.triangles())for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            binary.putFloat((float)v.u());binary.putFloat((float)v.v());
        }
        for(int i=0;i<count;i++)binary.putShort((short)i);
        if(normals){binary.position(normalStart);for(var t:mesh.triangles())for(var v:new EditorMeshPipeline.Vertex[]{t.a(),t.b(),t.c()}){
            var n=EditorMeshNormals.unit(EditorMeshNormals.effective(t,v));binary.putFloat((float)n.x());binary.putFloat((float)n.y());binary.putFloat((float)n.z());
        }}
        binary.position(imageStart);binary.put(png);
        String json="{\"asset\":{\"version\":\"2.0\",\"generator\":\"Beckenburg EditorAssetPipeline\"},"+
            "\"scene\":0,\"scenes\":[{\"nodes\":[0]}],\"nodes\":[{\"name\":\"Imported Model\",\"mesh\":0}],"+
            "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0,\"TEXCOORD_0\":1"+(normals?",\"NORMAL\":3":"")+"},"+
            "\"indices\":2,\"material\":0,\"mode\":4}]}],"+
            "\"buffers\":[{\"byteLength\":"+binSize+"}],"+
            "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":"+posBytes+"},"+
            "{\"buffer\":0,\"byteOffset\":"+posBytes+",\"byteLength\":"+uvBytes+"},"+
            "{\"buffer\":0,\"byteOffset\":"+(posBytes+uvBytes)+",\"byteLength\":"+indexBytes+"},"+
            "{\"buffer\":0,\"byteOffset\":"+imageStart+",\"byteLength\":"+png.length+"}"+(normals?",{\"buffer\":0,\"byteOffset\":"+normalStart+",\"byteLength\":"+normalBytes+"}":"")+"],"+
            "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":"+count+",\"type\":\"VEC3\"},"+
            "{\"bufferView\":1,\"componentType\":5126,\"count\":"+count+",\"type\":\"VEC2\"},"+
            "{\"bufferView\":2,\"componentType\":5123,\"count\":"+count+",\"type\":\"SCALAR\"}"+(normals?",{\"bufferView\":4,\"componentType\":5126,\"count\":"+count+",\"type\":\"VEC3\"}":"")+"],"+
            "\"images\":[{\"bufferView\":3,\"mimeType\":\"image/png\"}],"+
            "\"textures\":[{\"source\":0}],"+
            "\"materials\":[{\"name\":\"PSX preview fallback atlas (replace with licensed art)\","+
            "\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0}}}]}";
        byte[] jsonRaw=json.getBytes(StandardCharsets.UTF_8);
        int jsonSize=pad4(jsonRaw.length),size=12+8+jsonSize+8+binSize;
        if(size>4_000_000)throw new IOException("Export exceeds the active 4 MiB game loader limit");
        ByteBuffer glb=ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546c67).putInt(2).putInt(size);
        glb.putInt(jsonSize).putInt(0x4e4f534a).put(jsonRaw);
        for(int i=jsonRaw.length;i<jsonSize;i++)glb.put((byte)' ');
        glb.putInt(binSize).putInt(0x004e4942).put(binary.array());
        return glb.array();
    }
    private static int pad4(int value){return (value+3)&~3;}
}
