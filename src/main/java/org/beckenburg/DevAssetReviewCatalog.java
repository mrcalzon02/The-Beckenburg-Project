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
    public enum Kind { HUNTER, GHOST, CASTLE_NPC, EQUIPMENT, IMPORTED, WORLD_MODEL }
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
        out.sort(Comparator.comparing((Entry e)->e.kind().ordinal()).thenComparing(Entry::id));
        return List.copyOf(out);
    }

    /** Decoder/material presence is an objective diagnostic, not texture-art approval. */
    public static Audit audit(Entry entry){
        try {
            if(entry.sprite()){
                BufferedImage image=DirectionalSprites.frame(entry.id(),0);
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
            var model=GlbProps.model(entry.id());
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
            String state=uvProblems>0||materials.isEmpty()?"REVIEW_UV":
                colors.size()<2?"REVIEW_TEXTURE":"LOADED";
            String details="uniqueSampleColors="+colors.size()+"; invalidUV/material="+uvProblems+
                "; source geometry preserved";
            return new Audit(state,n,materials.size(),textureSamples,lx,ly,lz,hx,hy,hz,details);
        }catch(Exception | LinkageError issue){
            return new Audit("MISSING",0,0,0,0,0,0,0,0,0,
                issue.getClass().getSimpleName()+": "+String.valueOf(issue.getMessage())
                    .replace('\n',' ').replace('\r',' '));
        }
    }
}
