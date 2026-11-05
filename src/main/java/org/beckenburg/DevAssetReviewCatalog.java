package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Single source of showroom membership: actual runtime model approvals and
 * actual character identities. It deliberately owns NO duplicate GLB manifest.
 * Reopening the showroom picks up new local imports automatically.
 */
public final class DevAssetReviewCatalog {
    private DevAssetReviewCatalog(){}
    /** Graybox studies are reviewable in a source checkout only; never runtime-approved. */
    private static final java.nio.file.Path PRIMITIVE_PROTOTYPES =
        java.nio.file.Path.of("development","levels","primitive-props-v1","models");
    private static final java.nio.file.Path PRIMITIVE_REFINEMENTS =
        java.nio.file.Path.of("development","levels","primitive-props-v1","refinement");
    private static final java.nio.file.Path PRIMITIVE_LAYOUT_PREVIEWS =
        java.nio.file.Path.of("development","levels","primitive-props-v1","previews");
    private static final String LAYOUT_REVIEW_PREFIX="level_blockout_layout_";
    public enum Kind { HUNTER, GHOST, CASTLE_NPC, EQUIPMENT, IMPORTED, WORLD_MODEL, SOURCE_ONLY }
    public record Entry(Kind kind,String id) {
        public String key(){return kind.name()+"/"+id;}
        public boolean sprite(){return kind==Kind.HUNTER||kind==Kind.GHOST||kind==Kind.CASTLE_NPC;}
    }
    public record Audit(String state,int triangles,int materials,int textureSamples,
                        double minX,double minY,double minZ,double maxX,double maxY,
                        double maxZ,String details) {
        public boolean loaded(){return state.equals("LOADED")||state.equals("REVIEW_UV")||state.equals("REVIEW_TEXTURE");}
    }
    public static List<Entry> entries(){
        List<Entry> out=new ArrayList<>();
        for(String h:new TreeSet<>(CharacterVisuals.HUNTERS))out.add(new Entry(Kind.HUNTER,h));
        for(GhostIdentity.Type type:GhostIdentity.Type.values())
            out.add(new Entry(Kind.GHOST,type.name().toLowerCase(Locale.ROOT)));
        for(String name:List.of("twins_a","twins_b"))
            out.add(new Entry(Kind.GHOST,name));
        for(String person:List.of("lore_master","elsbeth_vale","rowan_pike",
                "tomas_greaves","sister_mirelle_ash"))
            out.add(new Entry(Kind.CASTLE_NPC,person));
        Set<String> equipment=EquipmentModelCatalog.approvedModels();
        Set<String> local=ImportedAssetLibrary.available();
        for(String id:GlbProps.availableModels()){
            Kind kind=local.contains(id)?Kind.IMPORTED:
                equipment.contains(id)?Kind.EQUIPMENT:Kind.WORLD_MODEL;
            out.add(new Entry(kind,id));
        }
        // Any model in the source checkout remains reviewable. Match an
        // already approved Hollow Creek instance against its real runtime
        // registry; only genuinely unapproved meshes remain SOURCE_ONLY.
        java.nio.file.Path source=java.nio.file.Path.of("assets","runtime","models");
        if(java.nio.file.Files.isDirectory(source))try(var paths=java.nio.file.Files.list(source)){
            Set<String> known=new java.util.HashSet<>();
            for(var item:out)if(!item.sprite())known.add(item.id());
            for(var file:paths.filter(p->p.getFileName().toString().endsWith(".glb")).toList()){
                String filename=file.getFileName().toString();
                String id=filename.substring(0,filename.length()-4);
                if(!known.contains(id)&&id.matches("[A-Za-z0-9_-]{1,90}")){
                    // Hollow Creek native scene parts are shipped and consumed
                    // through HollowCreekScene's verified instance registry;
                    // they are not unapproved loose source models. Keep a
                    // single catalog entry without altering game approvals.
                    // Never call an asset "unapproved" when the production
                    // LevelAssets allowlist already permits it. In particular
                    // Matthis Rook's authored proxy is a shipped model rather
                    // than an editor-only source; optional site terrain remains
                    // intentionally review-only until separately approved.
                    Kind discovered=LevelAssets.approved(id)?
                        Kind.WORLD_MODEL:Kind.SOURCE_ONLY;
                    out.add(new Entry(discovered,id));
                }
            }
        }catch(java.io.IOException failure){
            System.err.println("DEV ROOM source catalog inspection: "+failure.getMessage());
        }
        // Editor/dev-room review of newly generated future-map primitive studies.
        // This directory is deliberately separate from assets/runtime/models so
        // normal missions cannot mistake untextured blocking props for finished art.
        if(java.nio.file.Files.isDirectory(PRIMITIVE_PROTOTYPES,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
            try(var files=java.nio.file.Files.list(PRIMITIVE_PROTOTYPES)){
                Set<String> known=new java.util.HashSet<>();
                for(var existing:out)known.add(existing.id());
                for(var file:files.filter(p->p.getFileName().toString()
                        .startsWith("level_blockout_") && p.getFileName().toString()
                        .endsWith(".glb")).toList()){
                    String fileName=file.getFileName().toString();
                    String id=fileName.substring(0,fileName.length()-4);
                    if(id.matches("[A-Za-z0-9_-]{1,90}")&&!known.contains(id)){
                        out.add(new Entry(Kind.SOURCE_ONLY,id));
                        known.add(id);
                    }
                }
            }catch(java.io.IOException failure){
                System.err.println("DEV ROOM primitive source catalog: "+failure.getMessage());
            }
        // Additional detailed studies are discoverable in the source-only
        // showroom but never enter LevelAssets, GlbProps approvals or missions.
        if(java.nio.file.Files.isDirectory(PRIMITIVE_REFINEMENTS,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
            try(var files=java.nio.file.Files.list(PRIMITIVE_REFINEMENTS)){
                Set<String> known=new java.util.HashSet<>();
                for(var existing:out)known.add(existing.id());
                for(var file:files.filter(p->p.getFileName().toString()
                        .startsWith("level_blockout_") && p.getFileName().toString()
                        .endsWith(".glb")).toList()){
                    String filename=file.getFileName().toString();
                    String id=filename.substring(0,filename.length()-4);
                    if(id.matches("[A-Za-z0-9_-]{1,90}")&&!known.contains(id)){
                        out.add(new Entry(Kind.SOURCE_ONLY,id));
                        known.add(id);
                    }
                }
            }catch(java.io.IOException failure){
                System.err.println("DEV ROOM refinement source catalog: "+failure.getMessage());
            }
        // Complete multi-prop layout studies are separate source-only review meshes.
        // They are NOT Castle/runtime scene data, nor individually placed props.
        if(java.nio.file.Files.isDirectory(PRIMITIVE_LAYOUT_PREVIEWS,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
            try(var files=java.nio.file.Files.list(PRIMITIVE_LAYOUT_PREVIEWS)){
                Set<String> known=new java.util.HashSet<>();
                for(var existing:out)known.add(existing.id());
                for(var file:files.filter(p->p.getFileName().toString()
                        .matches("[a-z0-9-]{1,40}\\.glb")).toList()){
                    String fileName=file.getFileName().toString();
                    String id=LAYOUT_REVIEW_PREFIX+fileName.substring(0,fileName.length()-4);
                    if(id.length()<=90&&!known.contains(id)){
                        out.add(new Entry(Kind.SOURCE_ONLY,id));
                        known.add(id);
                    }
                }
            }catch(java.io.IOException failure){
                System.err.println("DEV ROOM layout preview catalog: "+failure.getMessage());
            }
        out.sort(Comparator.comparing((Entry e)->e.kind().ordinal()).thenComparing(Entry::id));
        return List.copyOf(out);
    }

    private static final java.util.Map<String,GlbProps.Model> SOURCE_REVIEW_CACHE=
        new java.util.concurrent.ConcurrentHashMap<>();
    /** Reload original source bytes after a GLB generator produces a revision.
     * Only the isolated DEV room has this cache, not the runtime model registry.
     */
    static void invalidateSourceReviewCache(){
        synchronized(SOURCE_REVIEW_CACHE){SOURCE_REVIEW_CACHE.clear();}
    }
    /** Development-only source loading. Does NOT alter GlbProps approvals. */
    public static GlbProps.Model sourceModel(String id)throws java.io.IOException{
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,90}"))
            throw new java.io.IOException("invalid review-only model name");
        java.nio.file.Path file=id.startsWith(LAYOUT_REVIEW_PREFIX)
            ? PRIMITIVE_LAYOUT_PREVIEWS.resolve(id.substring(LAYOUT_REVIEW_PREFIX.length())+".glb")
            : id.startsWith("level_blockout_refined_") ||
              id.startsWith("level_blockout_kit_refined_")
                ? PRIMITIVE_REFINEMENTS.resolve(id+".glb")
                : id.startsWith("level_blockout_")
                    ? PRIMITIVE_PROTOTYPES.resolve(id+".glb")
                : java.nio.file.Path.of("assets","runtime","models",id+".glb");
        if(!java.nio.file.Files.isRegularFile(file,java.nio.file.LinkOption.NOFOLLOW_LINKS)
            ||java.nio.file.Files.size(file)>4_000_000)
            throw new java.io.IOException("source-only GLB unavailable or over budget");
        GlbProps.Model cached=SOURCE_REVIEW_CACHE.get(id);
        if(cached!=null)return cached;
        synchronized(SOURCE_REVIEW_CACHE){
            cached=SOURCE_REVIEW_CACHE.get(id);
            if(cached==null){
                cached=GlbProps.decodeForReview(java.nio.file.Files.readAllBytes(file),id);
                SOURCE_REVIEW_CACHE.put(id,cached);
            }
            return cached;
        }
    }
    /** Decoder/material presence is an objective diagnostic, not texture-art approval. */
    public static Audit audit(Entry entry){
        try {
            if(entry.sprite()){
                BufferedImage image=DirectionalSprites.frame(entry.id(),0,LanGhostAnimation.Phase.LURK,0);
                if(image==null||image.getWidth()<1||image.getHeight()<1)
                    return new Audit("MISSING",0,0,0,0,0,0,0,0,0,"No frame");
                int alpha=0,opaque=0;
                for(int y=0;y<image.getHeight();y+=Math.max(1,image.getHeight()/17))
                    for(int x=0;x<image.getWidth();x+=Math.max(1,image.getWidth()/13)){
                        int a=image.getRGB(x,y)>>>24;
                        if(a==0)alpha++;else if(a>64)opaque++;
                    }
                String state=opaque==0?"REVIEW_TEXTURE":"LOADED";
                return new Audit(state,0,1,opaque,0,0,0,
                    image.getWidth(),image.getHeight(),0,
                    "source sprite "+image.getWidth()+"x"+image.getHeight()+
                    " alphaPixels="+alpha+" visibleSamples="+opaque);
            }
            var model=entry.kind()==Kind.SOURCE_ONLY
                ?sourceModel(entry.id()):GlbProps.model(entry.id());
            int n=model.triangles().size();
            if(n==0)return new Audit("MISSING",0,0,0,0,0,0,0,0,0,"No geometry");
            var materials=new TreeSet<Integer>();
            double lx=Double.POSITIVE_INFINITY,ly=lx,lz=lx;
            double hx=Double.NEGATIVE_INFINITY,hy=hx,hz=hx;
            int textureSamples=0,uvProblems=0;
            Set<Integer> colors=new java.util.HashSet<>();
            for(var tri:model.triangles()){
                materials.add(tri.material());
                for(var v:new GlbProps.Vertex[]{tri.a(),tri.b(),tri.c()}){
                    if(!Double.isFinite(v.x())||!Double.isFinite(v.y())||!Double.isFinite(v.z())
                        ||!Double.isFinite(v.u())||!Double.isFinite(v.v())){
                        uvProblems++;continue;
                    }
                    lx=Math.min(lx,v.x());ly=Math.min(ly,v.y());lz=Math.min(lz,v.z());
                    hx=Math.max(hx,v.x());hy=Math.max(hy,v.y());hz=Math.max(hz,v.z());
                    if(textureSamples<192){
                        try{colors.add(GlbProps.sample(tri.material(),v.u(),v.v()));textureSamples++;}
                        catch(RuntimeException failure){uvProblems++;}
                    }
                }
            }
            if(!Double.isFinite(lx))return new Audit("MISSING",n,materials.size(),0,0,0,0,0,0,0,"Nonfinite mesh");
            // Original alpha-masked native foliage cards have only two
            // triangles. Their six corner vertices all sample the same clear
            // PNG border; testing those corners alone reports one black color
            // and falsely calls a genuinely textured tree defective.
            // Sample INSIDE the actual source UV triangles, ignoring fully
            // transparent source texels. An entirely empty or flat source
            // still remains flagged, with no cosmetic release gate.
            int interiorVisible=0,interiorClear=0;
            if(colors.size()<2&&n<=2&&materials.stream().anyMatch(GlbProps::gltfBlended)){
                for(var t:model.triangles()){
                    if(!GlbProps.gltfBlended(t.material()))continue;
                    for(int row=1;row<17&&textureSamples<192;row++)
                        for(int col=1;col<17-row&&textureSamples<192;col++){
                            double b=(row+.5)/17,c=(col+.5)/17,a=1-b-c;
                            double u=a*t.a().u()+b*t.b().u()+c*t.c().u();
                            double v=a*t.a().v()+b*t.b().v()+c*t.c().v();
                            int rgba=GlbProps.sampleRgba(t.material(),u,v);
                            if((rgba>>>24)<24)interiorClear++;
                            else{interiorVisible++;colors.add(rgba&0xffffff);}
                            textureSamples++;
                        }
                }
            }
            String state=uvProblems>0||materials.isEmpty()?"REVIEW_UV":
                colors.size()<2?"REVIEW_TEXTURE":
                entry.kind()==Kind.SOURCE_ONLY?"REVIEW_REGISTRATION":"LOADED";
            String details="uniqueSampleColors="+colors.size()+"; invalidUV/material="+uvProblems+
                "; source geometry preserved"+
                (interiorVisible+interiorClear>0
                    ?"; interior original glTF BLEND UV samples visible="+interiorVisible+
                      " transparent="+interiorClear:"")+
                (entry.kind()==Kind.SOURCE_ONLY?
                "; not approved for normal gameplay":"");
            return new Audit(state,n,materials.size(),textureSamples,lx,ly,lz,hx,hy,hz,details);
        }catch(Exception | LinkageError issue){
            return new Audit("MISSING",0,0,0,0,0,0,0,0,0,
                issue.getClass().getSimpleName()+": "+String.valueOf(issue.getMessage())
                    .replace('\n',' ').replace('\r',' '));
        }
    }
}
