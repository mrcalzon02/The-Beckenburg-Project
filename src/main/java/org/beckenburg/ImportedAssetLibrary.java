package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Set;

/**
 * User-local, bounded imported model registry. Originals are retained next to
 * the generated runtime GLB; no game executable requires a 3D authoring tool.
 */
public final class ImportedAssetLibrary {
    private ImportedAssetLibrary(){}
    private static final String ID="import_[a-f0-9]{16}";
    public static Path root(){
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","imports")
            .toAbsolutePath().normalize();
    }
    public static boolean approved(String name){
        if(name==null||!name.matches(ID))return false;
        Path mesh=root().resolve(name+".glb"),meta=root().resolve(name+".properties");
        try{
            return Files.isRegularFile(mesh,LinkOption.NOFOLLOW_LINKS)&&
                Files.isRegularFile(meta,LinkOption.NOFOLLOW_LINKS)&&
                Files.size(mesh)>28&&Files.size(mesh)<=4_000_000;
        }catch(IOException ignored){return false;}
    }
    public static Set<String> available(){
        TreeSet<String> out=new TreeSet<>();
        if(!Files.isDirectory(root(),LinkOption.NOFOLLOW_LINKS))return Set.of();
        try(var stream=Files.list(root())){
            stream.limit(4096).filter(p->p.getFileName().toString().endsWith(".glb"))
                .map(p->p.getFileName().toString().replaceFirst("\\.glb$",""))
                .filter(ImportedAssetLibrary::approved).forEach(out::add);
        }catch(IOException ignored){return Set.of();}
        return Set.copyOf(out);
    }
    public static byte[] read(String id)throws IOException{
        if(!approved(id))throw new IOException("Unregistered imported asset: "+id);
        Path path=root().resolve(id+".glb");
        byte[] data=Files.readAllBytes(path);
        if(data.length>4_000_000)throw new IOException("Imported GLB size exceeded");
        return data;
    }
    public static synchronized String publish(Path original,byte[] output,int level,
                                              int before,int after,String warnings)throws IOException{
        return publishReviewed(original,output,level,before,after,warnings,null,null);
    }
    /** Checked human publication: source bytes must still match the reviewed input.
     * Optional native authoring bytes are retained alongside the original/GLB.
     */
    public static synchronized String publishReviewed(Path original,byte[] output,int level,int before,int after,
                                                       String warnings,String expectedSourceSha,byte[] authoring)throws IOException{
        byte[] source=org.beckenburg.editor.EditorMeshPipeline.readSource(original,32*1024*1024);
        if(expectedSourceSha!=null&&!expectedSourceSha.equals(hex(sha256(source))))
            throw new IOException("Model source changed after review; reopen it before publishing");
        if(authoring!=null)org.beckenburg.editor.EditorTopologyProject.decode(authoring);
        if(output==null||output.length<28||output.length>4_000_000)
            throw new IOException("Optimized GLB does not fit game decoder");
        String ext=extension(original);
        if(!Set.of("obj","stl","glb","gltf","btop").contains(ext))throw new IOException("Unsupported source type");
        // Preserve the native source/authoring identity and main's material-loss refusal.
        if((ext.equals("glb")||ext.equals("gltf"))&&level!=0)
            throw new IOException("Textured GLB regression is unsupported: retain original UVs/materials; optimize in a material-preserving authoring tool");
        java.security.MessageDigest identity;
        try{identity=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IOException(impossible);}
        identity.update(source);identity.update(output);if(authoring!=null)identity.update(authoring);
        String id="import_"+hex(identity.digest()).substring(0,16);
        Files.createDirectories(root());
        Path mesh=root().resolve(id+".glb");
        if(Files.isRegularFile(mesh,LinkOption.NOFOLLOW_LINKS)){
            if(!Arrays.equals(Files.readAllBytes(mesh),output))
                throw new IOException("Import id collision; refusing overwrite");
        }else atomic(mesh,output);
        Path sourcePath=root().resolve(id+".source."+ext);
        if(!Files.exists(sourcePath,LinkOption.NOFOLLOW_LINKS))atomic(sourcePath,source);
        else if(!Arrays.equals(source,Files.readAllBytes(sourcePath)))throw new IOException("Original source archive collision");
        if(authoring!=null){
            Path nativePath=root().resolve(id+".authoring.btop");
            if(!Files.exists(nativePath,LinkOption.NOFOLLOW_LINKS))atomic(nativePath,authoring);
            else if(!Arrays.equals(authoring,Files.readAllBytes(nativePath)))throw new IOException("Native authoring archive collision");
        }
        Properties p=new Properties();
        if(authoring!=null){p.setProperty("authoringSha256",hex(sha256(authoring)));p.setProperty("authoringBytes",Integer.toString(authoring.length));}
        p.setProperty("version","1");p.setProperty("id",id);
        p.setProperty("sourceName",original.getFileName().toString());
        p.setProperty("sourceSha256",hex(sha256(source)));
        p.setProperty("sourceBytes",String.valueOf(source.length));
        p.setProperty("glbSha256",hex(sha256(output)));
        p.setProperty("glbBytes",String.valueOf(output.length));
        p.setProperty("level",String.valueOf(level));
        p.setProperty("trianglesOriginal",String.valueOf(before));
        p.setProperty("trianglesPublished",String.valueOf(after));
        p.setProperty("warnings",warnings==null?"":warnings);
        java.io.ByteArrayOutputStream meta=new java.io.ByteArrayOutputStream();
        p.store(meta,"User imported model; editable original retained locally");
        atomic(root().resolve(id+".properties"),meta.toByteArray());
        if(!approved(id))throw new IOException("Import registry readback failed");
        return id;
    }
    private static String extension(Path path){
        String name=path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot=name.lastIndexOf('.');return dot<0?"":name.substring(dot+1);
    }
    private static void atomic(Path target,byte[] bytes)throws IOException{
        Path temp=Files.createTempFile(root(),"import-", ".pending");
        try{
            Files.write(temp,bytes);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temp,target);}
        }finally{Files.deleteIfExists(temp);}
    }
    private static byte[] sha256(byte[] data)throws IOException{
        try{return MessageDigest.getInstance("SHA-256").digest(data);}
        catch(NoSuchAlgorithmException e){throw new IOException("SHA-256 unavailable",e);}
    }
    private static String hex(byte[] data){return java.util.HexFormat.of().formatHex(data);}
}
