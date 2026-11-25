package org.beckenburg.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Owns the exact file resources reviewed when a JSON glTF document was opened. */
public final class EditorGltfSource {
    private static final int MAX_RESOURCES=128, MAX_RESOURCE_BYTES=8_000_000;
    private final Path directory;
    private final String filename;
    private final Map<String,byte[]> files=new TreeMap<>();
    private int totalBytes;
    private final byte[] glb;

    public EditorGltfSource(Path source,byte[] json) throws IOException {
        directory=source.toAbsolutePath().normalize().getParent().toRealPath();
        filename=source.getFileName().toString();
        glb=EditorGltfFile.toGlb(json,this::capture);
    }
    public byte[] glb() { return glb.clone(); }

    private Path resolve(String name) throws IOException {
        var path=directory.resolve(name).normalize();
        Path real=path.toRealPath();
        if (!real.startsWith(directory)) throw new IOException("glTF resource escapes the source directory");
        return real;
    }
    private byte[] capture(String text) throws IOException {
        URI uri;
        try { uri=new URI(text); }
        catch (URISyntaxException invalid) { throw new IOException("Invalid glTF resource URI",invalid); }
        String name=uri.getPath();
        if (uri.isAbsolute() || uri.getRawAuthority()!=null || uri.getQuery()!=null || uri.getFragment()!=null ||
            name==null || name.isBlank() || name.startsWith("/") || name.contains("\\") || name.contains(":") ||
            Arrays.asList(name.split("/")).contains("..")) throw new IOException("Expected a local relative glTF resource");
        try { name=Path.of(name).normalize().toString().replace('\\','/'); }
        catch (InvalidPathException invalid) { throw new IOException("Invalid local glTF resource name",invalid); }
        if (name.isEmpty() || name.equalsIgnoreCase(filename)) throw new IOException("Invalid glTF sidecar name");
        byte[] cached=files.get(name);
        if (cached!=null) return cached;
        if (files.size()>=MAX_RESOURCES || totalBytes>=MAX_RESOURCE_BYTES)
            throw new IOException("glTF sidecar capture budget exceeded");
        byte[] bytes=EditorMeshPipeline.readSource(resolve(name),MAX_RESOURCE_BYTES-totalBytes);
        files.put(name,bytes);
        totalBytes+=bytes.length;
        return bytes;
    }

    /** Recheck disk dependencies and return an independently reopenable source package. */
    public byte[] reviewedArchive(byte[] json) throws IOException {
        if (files.isEmpty()) return null;
        var output=new ByteArrayOutputStream();
        try (var zip=new ZipOutputStream(output)) {
            entry(zip,filename,json);
            for (var file : files.entrySet()) {
                byte[] current=EditorMeshPipeline.readSource(resolve(file.getKey()),MAX_RESOURCE_BYTES);
                if (!Arrays.equals(current,file.getValue())) throw new IOException("Reviewed glTF sidecar changed: "+file.getKey());
                entry(zip,file.getKey(),file.getValue());
            }
        }
        return output.toByteArray();
    }
    private static void entry(ZipOutputStream zip,String name,byte[] bytes) throws IOException {
        var entry=new ZipEntry(name);
        entry.setTimeLocal(java.time.LocalDateTime.of(2000,1,1,0,0));
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }
}
