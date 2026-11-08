package org.beckenburg.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/** Exercise alternate JSON encodings of the actual textured Model Editor export. */
public final class EditorGltfFileTest {
    private static byte[] json(Object value) {
        return EditorGlbJson.write(value).getBytes(StandardCharsets.UTF_8);
    }
    private static void refused(byte[] file) throws IOException {
        try { EditorGltfFile.toGlb(file); }
        catch (IOException expected) { return; }
        throw new AssertionError("Invalid JSON glTF was accepted");
    }
    /** Rehouse each actual buffer view in a separate, reordered resource with a local offset. */
    public static byte[] splitBuffers(byte[] exported) throws IOException {
        var root=EditorGlbReader.obj(EditorGlbJson.read(new String(exported,StandardCharsets.UTF_8)));
        byte[] binary=EditorGlbReader.read(EditorGltfFile.toGlb(exported)).binary();
        var views=EditorGlbReader.arr(root.get("bufferViews"));
        var buffers=new ArrayList<Map<String,Object>>();
        for (int i=0;i<views.size();i++) {
            var view=EditorGlbReader.obj(views.get(i));
            int start=((Number)view.getOrDefault("byteOffset",0)).intValue();
            int length=((Number)view.get("byteLength")).intValue();
            byte[] resource=new byte[4+length+1];
            System.arraycopy(binary,start,resource,4,length);
            buffers.add(Map.of("uri","data:application/gltf-buffer;base64,"+
                Base64.getEncoder().encodeToString(resource),"byteLength",resource.length));
            view.put("buffer",views.size()-1-i);
            view.put("byteOffset",4);
        }
        Collections.reverse(buffers);
        root.put("buffers",buffers);
        byte[] valid=json(root);
        var first=EditorGlbReader.obj(views.get(0));
        first.put("buffer",views.size());
        refused(json(root));
        first.put("buffer",views.size()-1);
        first.put("byteOffset",6);
        refused(json(root));
        return valid;
    }
    public static void verify(EditorMeshPipeline.Mesh expected,byte[] exported,byte[] png) throws IOException {
        var root=EditorGlbReader.obj(EditorGlbJson.read(new String(exported,StandardCharsets.UTF_8)));
        var document=EditorGlbReader.read(EditorGltfFile.toGlb(exported));
        var texture=document.materials().stream().filter(m->java.util.Arrays.equals(m.png(),png)).findFirst().orElseThrow();
        var materials=EditorGlbReader.arr(root.get("materials"));
        var pbr=EditorGlbReader.obj(EditorGlbReader.obj(materials.get(texture.index())).get("pbrMetallicRoughness"));
        int index=((Number)EditorGlbReader.obj(pbr.get("baseColorTexture")).get("index")).intValue();
        var sampler=EditorGlbReader.obj(EditorGlbReader.arr(root.get("textures")).get(index));
        int source=((Number)sampler.get("source")).intValue();
        var image=EditorGlbReader.obj(EditorGlbReader.arr(root.get("images")).get(source));
        image.remove("bufferView");
        image.put("uri","data:image/png;base64,"+Base64.getEncoder().encodeToString(png));
        EditorGlbWriter.validate(expected,EditorGltfFile.toGlb(json(root)));
        image.put("uri","../other.png");
        refused(json(root));
        image.put("uri","data:image/png;base64,***");
        refused(json(root));
        var buffer=EditorGlbReader.obj(EditorGlbReader.arr(root.get("buffers")).get(0));
        buffer.put("byteLength",0);
        refused(json(root));
        buffer.put("uri","https://example.invalid/mesh.bin");
        refused(json(root));
    }
}
