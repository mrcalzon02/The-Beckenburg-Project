package org.beckenburg.editor;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable invalidation metadata for a committed native edit, never scene storage.
 * Physics bounds describe native collider changes; render invalidation remains
 * conservative unless a host explicitly supplies complete render dependencies.
 */
public record EditorChange(String sceneId,Path document,long beforeRevision,long revision,Details details) {
    public EditorChange {
        if(sceneId==null||sceneId.isBlank())throw new IllegalArgumentException("Scene ID required");document=Objects.requireNonNull(document).toAbsolutePath().normalize();
        Objects.requireNonNull(details);
    }
    public enum Kind { ADDED, UPDATED, REMOVED }
    public enum Dependency { RENDER, PHYSICS, NAVIGATION, AUDIO_OCCLUSION }
    public record Node(String id,Kind kind,String beforeFingerprint,String fingerprint) {
        public Node {
            if(id==null||id.isBlank())throw new IllegalArgumentException("Node ID required");Objects.requireNonNull(kind);
            if((kind!=Kind.ADDED&&(beforeFingerprint==null||!beforeFingerprint.matches("[0-9a-f]{64}")))
                ||(kind!=Kind.REMOVED&&(fingerprint==null||!fingerprint.matches("[0-9a-f]{64}")))
                ||(kind==Kind.ADDED&&beforeFingerprint!=null)||(kind==Kind.REMOVED&&fingerprint!=null))
                throw new IllegalArgumentException("Invalid node fingerprints");
        }
    }
    public record Bounds(double x0,double y0,double z0,double x1,double y1,double z1) {
        public Bounds {
            for(double value:new double[]{x0,y0,z0,x1,y1,z1})
                if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite dirty bounds");
            if(x0>x1||y0>y1||z0>z1)throw new IllegalArgumentException("Reversed dirty bounds");
        }
        public Bounds union(Bounds b){return new Bounds(Math.min(x0,b.x0),Math.min(y0,b.y0),Math.min(z0,b.z0),
            Math.max(x1,b.x1),Math.max(y1,b.y1),Math.max(z1,b.z1));}
    }
    public record Details(List<Node> nodes,Set<String> assets,Set<Dependency> dependencies,
                          Bounds physicsBounds,boolean fullScene) {
        public Details {
            nodes=List.copyOf(nodes);assets=Set.copyOf(assets);dependencies=Set.copyOf(dependencies);
            if(nodes.size()>8192||assets.size()>8192)throw new IllegalArgumentException("Change budget exceeded");
            if(nodes.stream().map(Node::id).distinct().count()!=nodes.size())throw new IllegalArgumentException("Duplicate change ID");
            for(String uri:assets)EditorAssets.validateUri(uri);
        }
        public static Details full(){return new Details(List.of(),Set.of(),Set.of(Dependency.values()),null,true);}
    }
    /** Versioned host content should include every field consumed by its runtime. */
    public static String fingerprint(String content){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
