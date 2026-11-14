package org.beckenburg.editor;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Original binary/JSON, node transforms, instances and material preservation through actual native edits. */
public final class EditorGlbAuthoringTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-5,"Coordinate "+a+" != "+b);}
    private interface Action{void run()throws Exception;}
    private static void refuse(Action action)throws Exception{try{action.run();throw new AssertionError("Expected GLB refusal");}catch(IOException|IllegalArgumentException expected){}}
    private static Map<String,Object> map(Object... fields){var result=new LinkedHashMap<String,Object>();for(int i=0;i<fields.length;i+=2)result.put((String)fields[i],fields[i+1]);return result;}
    public static byte[] fixture()throws Exception{
        var png=EditorMeshMaterialTest.png();var material=EditorMeshMaterial.fromPng("Source paint",png,33071,10497,EditorMeshMaterial.Alpha.BLEND,EditorMeshMaterial.Origin.TOP_LEFT);
        var normal=new EditorMeshPipeline.Normal(0,0,2);var a=new EditorMeshPipeline.Vertex(0,0,0,0,0,normal);var b=new EditorMeshPipeline.Vertex(1,0,0,1,0,normal);var c=new EditorMeshPipeline.Vertex(0,1,0,0,1,normal);var d=new EditorMeshPipeline.Vertex(1,1,0,1,1,normal);
        var mesh=new EditorMeshPipeline.Mesh("Original panel",List.of(new EditorMeshPipeline.Triangle("panel",a,b,c,material.key()),new EditorMeshPipeline.Triangle("panel",c,b,d,material.key())),0,List.of(0L,1L,2L,2L,1L,3L),List.of(material));
        var source=EditorGlbReader.read(EditorGlbWriter.encode(mesh));var root=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(source.root())));double angle=Math.sqrt(.5);
        root.put("nodes",List.of(map("name","Assembly parent","translation",List.of(3,0,2),"rotation",List.of(0,angle,0,angle),"children",List.of(1,2),"extras",map("tag","original parent")),map("name","Panel A","mesh",0,"matrix",List.of(2,0,0,0,0,3,0,0,0,0,1,0,1,0,0,1)),map("name","Panel B","mesh",0,"translation",List.of(-2,0,0))));root.put("scenes",List.of(map("name","Original scene","nodes",List.of(0))));root.put("extras",map("longId",new java.math.BigDecimal("900719925474099312345"),"custom",List.of("keep","source")));
        var raw=EditorGlbReader.obj(EditorGlbReader.arr(root.get("materials")).get(0));EditorGlbReader.obj(raw.get("pbrMetallicRoughness")).put("roughnessFactor",new java.math.BigDecimal("0.12345678901234567890123456789"));raw.put("extras",map("beckenburgUvOrigin","TOP_LEFT","ownerMetadata",new java.math.BigDecimal("900719925474099312345")));
        var primitive=EditorGlbReader.obj(EditorGlbReader.arr(EditorGlbReader.obj(EditorGlbReader.arr(root.get("meshes")).get(0)).get("primitives")).get(0));primitive.put("extras",7);
        return EditorGlbDocumentWriter.container(root,source.binary());
    }
    public static byte[] externalFixture()throws Exception{var source=EditorGlbReader.read(fixture());var root=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(source.root())));EditorGlbReader.obj(root.get("asset")).put("generator","External artist exporter");EditorGlbReader.obj(EditorGlbReader.obj(EditorGlbReader.arr(root.get("materials")).get(0)).get("extras")).remove("beckenburgUvOrigin");return EditorGlbDocumentWriter.container(root,source.binary());}
    private static void sourcePreserved(EditorGlbReader.Document before,EditorGlbReader.Document after){
        check(before.root().get("extras").equals(after.root().get("extras")),"Arbitrary source metadata retains exact decimal/integer values");check(before.root().get("scenes").equals(after.root().get("scenes")),"Original scene roots preserved");
        var original=EditorGlbReader.arr(before.root().get("nodes"));var changed=EditorGlbReader.arr(after.root().get("nodes"));check(original.size()==changed.size(),"Native vertex edits do not invent/flatten source nodes");
        for(int i=0;i<original.size();i++){var a=new LinkedHashMap<>(EditorGlbReader.obj(original.get(i)));var b=new LinkedHashMap<>(EditorGlbReader.obj(changed.get(i)));a.remove("mesh");b.remove("mesh");var oldExtensions=a.remove("extensions") instanceof Map<?,?> e?new LinkedHashMap<>(EditorGlbReader.obj(e)):new LinkedHashMap<String,Object>();var newExtensions=b.remove("extensions") instanceof Map<?,?> e?new LinkedHashMap<>(EditorGlbReader.obj(e)):new LinkedHashMap<String,Object>();for(String key:List.of("BECK_native_order","BECK_editor_frame")){oldExtensions.remove(key);newExtensions.remove(key);}check(oldExtensions.equals(newExtensions),"Arbitrary source node extensions retained");check(a.equals(b),"Node names, original pivots, matrix/TRS and parent/child data remain exact");}
        var oldMesh=EditorGlbReader.arr(before.root().get("meshes")).get(0);check(oldMesh.equals(EditorGlbReader.arr(after.root().get("meshes")).get(0)),"Unchanged instance keeps exact original mesh JSON");
        check(EditorGlbReader.arr(before.root().get("materials")).get(0).equals(EditorGlbReader.arr(after.root().get("materials")).get(0)),"Original PBR/extra material properties retained, not replaced by diffuse preview data");check(Arrays.equals(before.binary(),Arrays.copyOf(after.binary(),before.binary().length)),"Every original binary byte, PNG and unused source buffer remains intact");
        var node=EditorGlbReader.obj(changed.get(1));var cloned=EditorGlbReader.obj(EditorGlbReader.arr(after.root().get("meshes")).get(((Number)node.get("mesh")).intValue()));for(var p:EditorGlbReader.arr(cloned.get("primitives")))if(EditorGlbReader.obj(p).containsKey("extras"))check(new java.math.BigDecimal("7").equals(EditorGlbReader.obj(p).get("extras")),"Scalar primitive extras cannot be overwritten by native identity metadata");
    }
    private static void guards(byte[] original,Path root)throws Exception{
        var document=EditorGlbReader.read(original);var p=EditorTopologyProject.fromGlb("guards.glb",original,root.resolve("guards.btop"));var state=p.snapshot();long revision=p.revision();
        refuse(()->p.restore(new EditorTopologyProject.State(state.sceneId(),state.sourceFingerprint(),state.name(),state.sourceBytes(),state.vertices(),state.faces(),state.materials())));check(p.snapshot()==state&&p.revision()==revision,"Refused provenance removal preserves native identity/epoch");
        for(String field:List.of("animations","skins")){var json=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(document.root())));json.put(field,List.of(map("name","future rig")));byte[] bytes=EditorGlbDocumentWriter.container(json,document.binary());EditorGlbReader.read(bytes);refuse(()->EditorTopologyProject.fromGlb("rig.glb",bytes,root.resolve("rig.btop")));}
        var extra=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(document.root())));var primitive=EditorGlbReader.obj(EditorGlbReader.arr(EditorGlbReader.obj(EditorGlbReader.arr(extra.get("meshes")).get(0)).get("primitives")).get(0));EditorGlbReader.obj(primitive.get("attributes")).put("COLOR_0",0);byte[] additional=EditorGlbDocumentWriter.container(extra,document.binary());EditorGlbReader.read(additional);refuse(()->EditorTopologyProject.fromGlb("attributes.glb",additional,root.resolve("attributes.btop")));
        var malformed=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(document.root())));EditorGlbReader.obj(EditorGlbReader.arr(malformed.get("nodes")).get(1)).put("translation",List.of(0,0,0));refuse(()->EditorGlbReader.read(EditorGlbDocumentWriter.container(malformed,document.binary())));
        var badBuffer=EditorGlbReader.obj(EditorGlbJson.read(EditorGlbJson.write(document.root())));EditorGlbReader.obj(EditorGlbReader.arr(badBuffer.get("buffers")).get(0)).put("byteLength",1);byte[] tooShort=EditorGlbDocumentWriter.container(badBuffer,document.binary());refuse(()->EditorGlbReader.read(tooShort));
        for(String text:List.of("{\"a\":1,\"a\":2}","[01]","[+1]","[1.]","[1e]","[".repeat(65)+"0"+"]".repeat(65),"[\"raw\ncontrol\"]"))refuse(()->EditorGlbJson.read(text));check("\0".equals(EditorGlbJson.read("\"\\u0000\"")),"Escaped control metadata remains valid JSON");
        byte[] tail=ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(4).putInt(0x12345678).putInt(42).array();byte[] opaque=EditorGlbDocumentWriter.container(document.root(),document.binary(),tail);var retained=EditorTopologyProject.fromGlb("opaque.glb",opaque,root.resolve("opaque.btop"));var v=retained.snapshot().vertices().get(0);retained.moveVertex(v.id(),v.x()+.125,v.y(),v.z());check(Arrays.equals(tail,EditorGlbReader.read(EditorGlbWriter.encode(retained.mesh())).trailing()),"Unrecognized optional binary chunks retain exact bytes after editing");
        check(EditorGlbReader.read(externalFixture()).materials().get(0).defaultOrigin().equals("TOP_LEFT"),"External GLB defaults to original image-space UVs");
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-glb-authoring-");try{
            byte[] original=fixture();guards(original,root);var decoded=EditorGlbReader.read(original);var mesh=decoded.mesh("assembly.glb");check(mesh.triangles().size()==4&&mesh.cornerVertexIds().stream().distinct().count()==8,"Actual shared decoder retains two instances and source vertex connectivity");var point=decoded.primitives().get(0).world().get(0);near(point.x(),3);near(point.z(),1);near(decoded.primitives().get(0).world().get(1).z(),-1);near(point.normal().x(),1);
            byte[] copy=decoded.original();copy[0]=0;check(Arrays.equals(decoded.original(),original),"Source bytes are immutable");try{decoded.root().put("scene",7);throw new AssertionError("Mutable source metadata");}catch(UnsupportedOperationException expected){}double[] transform=decoded.world(1);transform[3]=999;near(decoded.world(1)[3],3);
            var p=EditorTopologyProject.fromGlb("assembly.glb",original,root.resolve("assembly.btop"));var initial=p.snapshot();check(p.snapshot().vertices().size()==8&&Arrays.equals(EditorGlbWriter.encode(p.mesh()),original),"No-edit GLB roundtrip retains exact original bytes and native topology");EditorGlbWriter.validate(p.mesh(),original);p.save();check(ByteBuffer.wrap(Files.readAllBytes(p.documentPath())).getInt(4)==4&&EditorTopologyProject.open(p.documentPath()).snapshot().equals(initial),"Actual native v4 fresh reopen retains source-backed authoring state");
            var history=new EditorHistory<>(p,20);long vertex=initial.vertices().stream().filter(v->v.part().equals("glb_node_1")).findFirst().orElseThrow().id();var selected=initial.vertices().stream().filter(v->v.id()==vertex).findFirst().orElseThrow();history.execute(()->p.moveVertex(vertex,selected.x()+.25,selected.y(),selected.z()+.1));var moved=p.snapshot();check(moved.glbSource()==initial.glbSource(),"Native edit shares immutable source provenance, not another editable model");byte[] edited=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),edited);sourcePreserved(decoded,EditorGlbReader.read(edited));check(history.undo()&&Arrays.equals(EditorGlbWriter.encode(p.mesh()),original)&&history.redo()&&p.snapshot().equals(moved),"Shared undo restores exact source bytes; redo restores edited instance");
            Path exported=root.resolve("edited.glb");p.exportGlb(p.observe(),exported,null);EditorGlbWriter.validate(p.mesh(),Files.readAllBytes(exported));var fresh=EditorTopologyProject.fromGlb("edited.glb",Files.readAllBytes(exported),root.resolve("fresh.btop"));check(fresh.snapshot().vertices().size()==8,"Reimport uses native source IDs across new primitive accessors, not accidental welding");
            var observation=p.observe();p.applyPreview(p.previewUvTransform(0,false,EditorTopologyProject.UvTransform.MOVE,.25,0,false));refuse(()->p.exportGlb(observation,root.resolve("stale.glb"),null));check(!Files.exists(root.resolve("stale.glb")),"Stale GLB export cannot create an artifact");byte[] uv=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),uv);sourcePreserved(decoded,EditorGlbReader.read(uv));
            var paint=EditorMeshMaterial.fromPng("Replacement paint",EditorMeshMaterialTest.png(),10497,10497,EditorMeshMaterial.Alpha.OPAQUE,EditorMeshMaterial.Origin.BOTTOM_LEFT);p.applyPreview(p.previewBindMaterial(0,paint));byte[] painted=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),painted);sourcePreserved(decoded,EditorGlbReader.read(painted));
            p.applyPreview(p.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,.2));byte[] extruded=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),extruded);sourcePreserved(decoded,EditorGlbReader.read(extruded));var cap=p.snapshot().faces().stream().filter(f->f.id()==0).findFirst().orElseThrow();p.applyPreview(p.previewTransform(Set.of(cap.a().vertex(),cap.b().vertex(),cap.c().vertex()),EditorTopologyProject.Transform.ROTATE,2,15,true,0));byte[] surface=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),surface);sourcePreserved(decoded,EditorGlbReader.read(surface));p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(p.snapshot()),"Modeled/material/UV native v4 reopens exactly with original source");refuse(()->p.exportObj(p.observe(),root.resolve("lossy.obj.zip"),null));
            var appended=EditorTopologyProject.fromGlb("assembly.glb",original,root.resolve("appended.btop"));appended.appendPrimitive(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,1,1,1,12,4));byte[] withPrimitive=EditorGlbWriter.encode(appended.mesh());EditorGlbWriter.validate(appended.mesh(),withPrimitive);check(EditorGlbReader.arr(EditorGlbReader.read(withPrimitive).root().get("nodes")).size()==4,"Appending a native primitive retains original hierarchy and adds one independent root");
            byte[] future=Files.readAllBytes(p.documentPath());ByteBuffer.wrap(future).putInt(4,8);refuse(()->EditorTopologyProject.decode(future));
            if(args.length>0){Path out=Path.of(args[0]);Files.createDirectories(out);Files.write(out.resolve("original.glb"),original);Files.write(out.resolve("edited.glb"),surface);Files.copy(p.documentPath(),out.resolve("edited.btop"),StandardCopyOption.REPLACE_EXISTING);}
            System.out.println("PASS source-backed GLB: shared matrix/ancestor decoder, exact source bytes/metadata/PNG, native v4/history, independent instance edits, original hierarchy/pivots/materials, UV/modeling and real export/reimport validation");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    /** Real glTF data-URI, split-buffer and external-sidecar fixtures for G2 GUI acceptance. */
    public static final class GltfFixture {
        private static final String DATA="data:application/octet-stream;base64,";
        private GltfFixture(){}
        private static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
        private static int number(Object n)throws IOException{
            if(!(n instanceof Number v))throw new IOException("Expected glTF number");
            try{return new java.math.BigDecimal(v.toString()).intValueExact();}
            catch(ArithmeticException bad){throw new IOException("Invalid glTF index",bad);}
        }
        private static Map<String,Object> obj(Object value)throws IOException{
            if(!(value instanceof Map<?,?> map))throw new IOException("Expected glTF object");
            var result=new LinkedHashMap<String,Object>();
            for(var item:map.entrySet()){
                if(!(item.getKey() instanceof String key))throw new IOException("Invalid glTF key");
                result.put(key,item.getValue());
            }
            return result;
        }
        private static List<Object> arr(Object value)throws IOException{
            if(!(value instanceof List<?> a))throw new IOException("Expected glTF array");
            return new ArrayList<>(a);
        }
        private static Map<String,Object> root(byte[] json)throws IOException{
            return obj(EditorGlbJson.read(new String(json,StandardCharsets.UTF_8)));
        }
        private static byte[] payload(Map<String,Object> buffer)throws IOException{
            if(!(buffer.get("uri") instanceof String uri)||!uri.startsWith(DATA))
                throw new IOException("Expected embedded glTF binary");
            try{
                byte[] data=Base64.getDecoder().decode(uri.substring(DATA.length()));
                int n=number(buffer.get("byteLength"));
                if(n<=0||n>data.length)throw new IOException("Truncated glTF buffer");
                return Arrays.copyOf(data,n);
            }catch(IllegalArgumentException bad){throw new IOException("Invalid base64",bad);}
        }
        private static byte[] json(Map<String,Object> tree)throws IOException{
            byte[] encoded=EditorGlbJson.write(tree).getBytes(StandardCharsets.UTF_8);
            if(encoded.length>EditorGltfFile.MAX_BYTES)throw new IOException("Interchange exceeds budget");
            return encoded;
        }
        public static void verify(EditorMeshPipeline.Mesh expected,byte[] source,byte[] png)throws IOException{
            var tree=root(source);
            check(arr(tree.get("buffers")).size()==1,"Expected one native data buffer");
            check(payload(obj(arr(tree.get("buffers")).get(0))).length>0,"Empty native data buffer");
            var decoded=EditorGlbReader.read(EditorGltfFile.toGlb(source));
            EditorGlbWriter.validate(expected,decoded.original());
            check(decoded.materials().stream().anyMatch(m->Arrays.equals(m.png(),png)),
                "GLB-to-glTF roundtrip changed the original PNG material");
        }
        /** Each actual bufferView receives its own buffer, forcing the runtime decoder to merge views. */
        public static byte[] splitBuffers(byte[] source)throws IOException{
            var tree=root(source);
            var original=arr(tree.get("buffers"));
            if(original.size()!=1)throw new IOException("Expected one source buffer");
            byte[] data=payload(obj(original.get(0)));
            var views=arr(tree.get("bufferViews"));
            if(views.size()<2)throw new IOException("No independently addressable bufferViews");
            var split=new ArrayList<Object>();
            var updated=new ArrayList<Object>();
            for(Object value:views){
                var view=obj(value);
                if(number(view.get("buffer"))!=0)throw new IOException("Foreign source buffer");
                int start=view.containsKey("byteOffset")?number(view.get("byteOffset")):0;
                int size=number(view.get("byteLength"));
                if(size<=0||start<0||(long)start+size>data.length)
                    throw new IOException("Invalid source bufferView span");
                byte[] segment=Arrays.copyOfRange(data,start,start+size);
                view.put("buffer",split.size());
                view.put("byteOffset",0);
                updated.add(view);
                split.add(Map.of("uri",DATA+Base64.getEncoder().encodeToString(segment),"byteLength",size));
            }
            tree.put("buffers",split);
            tree.put("bufferViews",updated);
            byte[] result=json(tree);
            EditorGltfFile.toGlb(result);
            return result;
        }
        /** Produce the exact three-file source package checked by the native editor's dependency guard. */
        public static Path externalFiles(Path directory,byte[] source,byte[] png)throws IOException{
            Files.createDirectories(directory.resolve("textures"));
            var tree=root(source);
            var buffers=arr(tree.get("buffers"));
            if(buffers.size()!=1)throw new IOException("Expected one reviewed buffer");
            var buffer=obj(buffers.get(0));
            byte[] data=payload(buffer);
            var images=arr(tree.get("images"));
            if(images.size()!=1)throw new IOException("Expected one material image");
            var image=obj(images.get(0));
            if(!"image/png".equals(image.get("mimeType")))throw new IOException("Expected PNG image");
            var views=arr(tree.get("bufferViews"));
            int index=number(image.get("bufferView"));
            if(index<0||index>=views.size())throw new IOException("Invalid image view");
            var view=obj(views.get(index));
            int offset=view.containsKey("byteOffset")?number(view.get("byteOffset")):0;
            int count=number(view.get("byteLength"));
            if(offset<0||count!=png.length||(long)offset+count>data.length||
               !Arrays.equals(Arrays.copyOfRange(data,offset,offset+count),png))
                throw new IOException("Review PNG disagrees with embedded material");
            buffer.put("uri","box%20data.bin");
            tree.put("buffers",List.of(buffer));
            image.remove("bufferView");
            image.put("uri","textures/checker.png");
            tree.put("images",List.of(image));
            Files.write(directory.resolve("box data.bin"),data);
            Files.write(directory.resolve("textures/checker.png"),png);
            Path path=directory.resolve("box.gltf");
            byte[] rewritten=json(tree);
            Files.write(path,rewritten);
            // The actual source-capture resolver must recognize both real sidecars.
            new EditorGltfSource(path,rewritten).glb();
            return path;
        }
    }

}
