package org.beckenburg.editor;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned, engine-neutral RuntimeScene persistence. Does not duplicate the
 * game's world: it serializes whatever RuntimeScene adapter the caller owns.
 * No arbitrary Java deserialization or script execution.
 */
public final class RuntimeSceneDocument {
    private static final int MAGIC=0x4253434e; // BSCN
    public static final int VERSION=1;
    private static final int MAX_NODES=4096,MAX_COMPONENTS=32,MAX_PROPS=128;
    private RuntimeSceneDocument(){}
    private static String checked(String value,int max,String what)throws IOException {
        if(value==null||value.length()>max)
            throw new IOException("Invalid "+what+" length");
        return value;
    }
    private static String read(DataInputStream in,int limit,String what)throws IOException{
        return checked(in.readUTF(),limit,what);
    }
    private static int count(DataInputStream in,int maximum,String what)throws IOException{
        int n=in.readInt();
        if(n<0||n>maximum)throw new IOException("Invalid "+what+" count");
        return n;
    }
    private static void validate(RuntimeScene scene)throws IOException{
        var findings=RuntimeSceneDiagnostics.inspect(scene);
        if(!findings.isEmpty())
            throw new IOException("Scene hierarchy invalid: "+findings.get(0));
        if(scene.nodes().size()>MAX_NODES)throw new IOException("Scene node budget exceeded");
        checked(scene.sceneId(),128,"scene ID");
        for(var node:scene.nodes()){
            checked(node.id(),128,"node ID");
            checked(node.name(),256,"node name");
            checked(node.layer(),128,"layer");
            if(node.parentId()!=null)checked(node.parentId(),128,"parent ID");
            if(node.components().size()>MAX_COMPONENTS)
                throw new IOException("Component budget exceeded on "+node.id());
            for(var component:node.components()){
                checked(component.type(),128,"component type");
                if(component.properties().size()>MAX_PROPS)
                    throw new IOException("Properties budget exceeded on "+node.id());
                for(var e:component.properties().entrySet()){
                    checked(e.getKey(),128,"property name");
                    checked(e.getValue(),3000,"property value");
                }
            }
        }
    }
    public static void save(Path destination,RuntimeScene scene)throws IOException{save(destination,scene,null,false);}
    /** Return the exact saved revision; null expected requires a new document. */
    public static String saveIfUnchanged(Path destination,RuntimeScene scene,String expectedSha256)throws IOException{
        return save(destination,scene,expectedSha256,true);
    }
    private static String save(Path destination,RuntimeScene scene,String expectedSha256,boolean guarded)throws IOException{
        Objects.requireNonNull(destination);Objects.requireNonNull(scene);
        validate(scene);
        java.security.MessageDigest digest;
        try{digest=java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        EditorDocumentStore.Writer writer=output->{
            try(var out=new DataOutputStream(new BufferedOutputStream(new java.security.DigestOutputStream(output,digest)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeUTF(scene.sceneId());
                out.writeInt(scene.nodes().size());
                for(var n:scene.nodes()){
                    out.writeUTF(n.id());out.writeUTF(n.name());
                    out.writeBoolean(n.parentId()!=null);
                    if(n.parentId()!=null)out.writeUTF(n.parentId());
                    out.writeUTF(n.layer());
                    var t=n.transform();
                    for(double d:new double[]{t.x(),t.y(),t.z(),t.pitch(),t.yaw(),
                        t.roll(),t.scaleX(),t.scaleY(),t.scaleZ()})out.writeDouble(d);
                    out.writeInt(n.components().size());
                    for(var component:n.components()){
                        out.writeUTF(component.type());
                        out.writeInt(component.properties().size());
                        // Stable key ordering gives deterministic bytes on repeated saves.
                        var entries=new java.util.TreeMap<>(component.properties());
                        for(var e:entries.entrySet()){
                            out.writeUTF(e.getKey());out.writeUTF(e.getValue());
                        }
                    }
                }
            }
        };
        EditorDocumentStore.Validator prepared=temp->{
            var check=readRaw(temp);
            if(!check.sceneId().equals(scene.sceneId())
                ||!check.nodes().equals(scene.nodes()))
                throw new IOException("Scene save/readback did not preserve nodes");
        };
        if(guarded)EditorDocumentStore.saveIfUnchanged(destination,writer,RuntimeSceneDocument::readRaw,prepared,expectedSha256);
        else EditorDocumentStore.save(destination,writer,RuntimeSceneDocument::readRaw,prepared);
        return java.util.HexFormat.of().formatHex(digest.digest());
    }
    public record Loaded(InMemoryRuntimeScene scene,String fingerprint){}
    public static InMemoryRuntimeScene load(Path path)throws IOException{return loadRevision(path).scene();}
    public static Loaded loadRevision(Path path)throws IOException{
        Objects.requireNonNull(path);recoverPending(path);return readStoredRevision(path);
    }
    /** Read only the currently installed bytes; an observation cannot perform journal recovery. */
    public static Loaded readStoredRevision(Path path)throws IOException{
        byte[] bytes=EditorDocumentStore.readDocument(path);
        return new Loaded(decode(bytes),EditorDocumentStore.fingerprint(bytes));
    }
    public static void recoverPending(Path path)throws IOException{
        EditorDocumentStore.recoverPending(path,RuntimeSceneDocument::readRaw);
    }
    public static void restorePrevious(Path path,EditorDocumentStore.Validator hostValidator)throws IOException{
        EditorDocumentStore.restorePrevious(path,RuntimeSceneDocument::readRaw,hostValidator);
    }
    static InMemoryRuntimeScene readRaw(Path path)throws IOException{return decode(EditorDocumentStore.readDocument(path));}
    private static InMemoryRuntimeScene decode(byte[] bytes)throws IOException{
        try(var in=new DataInputStream(new java.io.ByteArrayInputStream(bytes))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)
                throw new IOException("Unsupported editor scene document");
            String id=read(in,128,"scene ID");
            int count=count(in,MAX_NODES,"nodes");
            var nodes=new ArrayList<RuntimeScene.Node>();
            var ids=new HashSet<String>();
            for(int i=0;i<count;i++){
                String nodeId=read(in,128,"node ID");
                if(!ids.add(nodeId))throw new IOException("Duplicate scene node ID "+nodeId);
                String name=read(in,256,"node name");
                String parent=in.readBoolean()?read(in,128,"parent ID"):null;
                String layer=read(in,128,"layer");
                double[] t=new double[9];
                for(int j=0;j<9;j++)t[j]=in.readDouble();
                int components=count(in,MAX_COMPONENTS,"components");
                var values=new ArrayList<RuntimeScene.Component>();
                for(int j=0;j<components;j++){
                    String type=read(in,128,"component type");
                    int n=count(in,MAX_PROPS,"properties");
                    var properties=new LinkedHashMap<String,String>();
                    for(int k=0;k<n;k++){
                        String key=read(in,128,"property name");
                        String value=read(in,3000,"property value");
                        if(properties.putIfAbsent(key,value)!=null)
                            throw new IOException("Duplicate scene property key");
                    }
                    values.add(new RuntimeScene.Component(type,properties));
                }
                nodes.add(new RuntimeScene.Node(nodeId,name,parent,layer,
                    new RuntimeScene.Transform(t[0],t[1],t[2],t[3],t[4],t[5],
                        t[6],t[7],t[8]),values));
            }
            if(in.read()!=-1)throw new IOException("Trailing data in editor scene");
            InMemoryRuntimeScene scene=new InMemoryRuntimeScene(id);
            // Parent-first order; reject orphans and cycles instead of silently
            // dropping children or manufacturing missing parents.
            var remaining=new ArrayList<>(nodes);
            while(!remaining.isEmpty()){
                int before=remaining.size();
                var iter=remaining.iterator();
                while(iter.hasNext()){
                    var node=iter.next();
                    if(node.parentId()==null||scene.find(node.parentId())!=null){
                        scene.replace(node);iter.remove();
                    }
                }
                if(remaining.size()==before)
                    throw new IOException("Scene contains orphaned or cyclic parent IDs");
            }
            validate(scene);
            return scene;
        }catch(IllegalArgumentException problem){
            throw new IOException("Invalid scene contents",problem);
        }
    }
    /** Apply to the *existing authoritative adapter*; rollback on a bad parent,
     * changed scene identity or incompatible snapshot. */
    public static void restoreInto(Path path,RuntimeScene target)throws IOException{
        Objects.requireNonNull(target);
        var loaded=load(path);
        if(!loaded.sceneId().equals(target.sceneId()))
            throw new IOException("Scene identity mismatch; cannot overwrite another level");
        var prior=target.snapshot();
        try{
            for(var node:target.nodes())target.remove(node.id());
            for(var node:loaded.nodes())target.replace(node);
            validate(target);
        }catch(RuntimeException|IOException failure){
            target.restore(prior);
            if(failure instanceof IOException io)throw io;
            throw new IOException("Could not restore scene into runtime adapter",failure);
        }
    }
}
