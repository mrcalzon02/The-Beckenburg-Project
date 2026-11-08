package org.beckenburg.editor;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Self-contained JSON interchange for the same static mesh decoder used by the game. */
public final class EditorGltfFile {
    public static final int MAX_BYTES = 6_000_000;
    private static final String URI = "data:application/octet-stream;base64,";
    private static final String GLTF_URI = "data:application/gltf-buffer;base64,";
    private EditorGltfFile() {}

    public static byte[] fromGlb(byte[] bytes) throws IOException {
        var document = EditorGlbReader.read(bytes);
        if (document.trailing().length != 0) throw new IOException("JSON glTF cannot represent opaque GLB chunks");
        var root = new LinkedHashMap<>(document.root());
        var buffer = new LinkedHashMap<>(singleBuffer(root));
        byte[] binary = document.binary();
        int length = ((Number)buffer.get("byteLength")).intValue();
        buffer.put("uri",URI + Base64.getEncoder().encodeToString(Arrays.copyOf(binary,length)));
        root.put("buffers",List.of(buffer));
        byte[] result = EditorGlbJson.write(root).getBytes(StandardCharsets.UTF_8);
        if (result.length > MAX_BYTES) throw new IOException("JSON glTF exceeds interchange budget");
        return result;
    }

    private static Map<String,Object> singleBuffer(Map<String,Object> root) throws IOException {
        if (!(root.get("buffers") instanceof List<?> buffers) || buffers.size()!=1 ||
            !(buffers.get(0) instanceof Map<?,?>)) throw new IOException("Expected one glTF buffer");
        return EditorGlbReader.obj(buffers.get(0));
    }

    public static byte[] toGlb(byte[] json) throws IOException {
        if (json.length > MAX_BYTES) throw new IOException("JSON glTF exceeds interchange budget");
        var value = EditorGlbJson.read(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(json)).toString());
        if (!(value instanceof Map<?,?>)) throw new IOException("Expected glTF document object");
        var root = EditorGlbReader.obj(value);
        var buffer = singleBuffer(root);
        if (!(buffer.remove("uri") instanceof String uri)) throw new IOException("Missing glTF buffer URI");
        String prefix = uri.startsWith(URI) ? URI : GLTF_URI;
        if (!uri.startsWith(prefix)) throw new IOException("Expected an embedded base64 glTF buffer");
        byte[] bytes = decode(uri,prefix);
        int declared = size(buffer.get("byteLength"));
        if (declared < 1 || declared > bytes.length) throw new IOException("Truncated glTF buffer");
        if (!(root.get("bufferViews") instanceof List<?> views)) throw new IOException("Missing glTF buffer views");
        for (Object valueView : views) {
            if (!(valueView instanceof Map<?,?> view) || size(view.get("buffer"))!=0)
                throw new IOException("Invalid glTF buffer view");
            int offset = view.containsKey("byteOffset") ? size(view.get("byteOffset")) : 0;
            if ((long)offset + size(view.get("byteLength")) > declared)
                throw new IOException("glTF buffer view exceeds declared buffer");
        }
        var binary = new ByteArrayOutputStream();
        binary.write(bytes,0,declared);
        if (root.get("images") instanceof List<?> images) {
            if (images.size()>64) throw new IOException("glTF image count exceeds runtime budget");
            for (Object item : images) {
                if (!(item instanceof Map<?,?>)) throw new IOException("Invalid glTF image object");
                var image = EditorGlbReader.obj(item);
                if (!image.containsKey("uri")) continue;
                if (image.containsKey("bufferView") || !(image.remove("uri") instanceof String imageUri) ||
                    !imageUri.startsWith("data:image/png;base64,"))
                    throw new IOException("Expected an embedded PNG image URI or buffer view");
                if (image.containsKey("mimeType") && !"image/png".equals(image.get("mimeType")))
                    throw new IOException("Image MIME type differs from PNG URI");
                var png = decode(imageUri,"data:image/png;base64,");
                while ((binary.size() & 3)!=0) binary.write(0);
                if ((long)binary.size()+png.length>4_000_000) throw new IOException("glTF exceeds runtime buffer budget");
                var imageViews = EditorGlbReader.arr(root.get("bufferViews"));
                image.put("mimeType","image/png");
                image.put("bufferView",imageViews.size());
                imageViews.add(Map.of("buffer",0,"byteOffset",binary.size(),"byteLength",png.length));
                binary.write(png);
            }
        }
        buffer.put("byteLength",binary.size());
        return EditorGlbDocumentWriter.container(root,binary.toByteArray());
    }

    private static int size(Object value) throws IOException {
        if (value instanceof Number n) try {
            int result = new java.math.BigDecimal(n.toString()).intValueExact();
            if (result>=0) return result;
        } catch (ArithmeticException invalid) { throw new IOException("Invalid glTF integer",invalid); }
        throw new IOException("Expected a nonnegative glTF byte count or index");
    }

    private static byte[] decode(String uri,String prefix) throws IOException {
        try { return Base64.getDecoder().decode(uri.substring(prefix.length())); }
        catch (IllegalArgumentException invalid) { throw new IOException("Invalid glTF base64 payload",invalid); }
    }
}
