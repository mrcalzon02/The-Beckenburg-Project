package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Verifies shipped source-art-only four-view roster in the real packaged classpath. */
public final class CharacterDirectionalVisualTest {
    private static void check(boolean valid,String message) {
        if(!valid)throw new AssertionError(message);
    }
    private static long signature(BufferedImage image){
        long hash=1469598103934665603L;
        for(int y=0;y<image.getHeight();y+=2)
            for(int x=0;x<image.getWidth();x+=2)
                hash=(hash^image.getRGB(x,y))*1099511628211L;
        return hash;
    }
    public static void main(String[] args){
        check(CharacterVisuals.HUNTERS.size()==3,"Three approved hunter classes");
        check(CharacterVisuals.ELDERS.size()==3,"Three distinct approved Elder entities");
        check(DirectionalSprites.frameCount()==41,
            "All legacy and newly packaged master direction files must be distinct");
        long observer=signature(DirectionalSprites.frame("observer",0));
        long[] heads=new long[6];
        int idx=0;
        for(String id:new String[]{"cleric","observer","demonologist",
                "elder_blade_demon","elder_plague_demon","elder_nothic"}){
            BufferedImage front=DirectionalSprites.frame(id,0);
            check(front.getWidth()==128&&front.getHeight()==224,"Incorrect native art size "+id);
            long[] facing=new long[4];
            for(int d=0;d<4;d++){
                int sector=new int[]{0,2,4,5}[d];
                BufferedImage image=DirectionalSprites.frame(id,sector);
                check(image.getColorModel().hasAlpha(),"No original alpha "+id);
                int visible=0,clear=0;
                for(int y=0;y<image.getHeight();y++)
                    for(int x=0;x<image.getWidth();x++){
                        int alpha=image.getRGB(x,y)>>>24;
                        if(alpha>=24)visible++;if(alpha==0)clear++;
                    }
                check(visible>800&&clear>700,"Invalid silhouette/clear matte "+id+" "+d);
                facing[d]=signature(image);
            }
            check(Arrays.stream(facing).distinct().count()==4,
                "The four master-sheet views must not be duplicates: "+id);
            heads[idx++]=facing[0];
        }
        check(Arrays.stream(heads).distinct().count()==6,"The six approved people/bosses must not share art");
        for(GhostIdentity.Type type:GhostIdentity.Type.values()){
            String id=CharacterVisuals.ghost(type);
            for(int facing:new int[]{0,2,4,6}){
                BufferedImage art=DirectionalSprites.frame(id,facing,LanGhostAnimation.Phase.WALK,3);
                check(art!=null&&art.getColorModel().hasAlpha(),
                    "Missing static approved ghost identity/facing: "+id+"/"+facing);
            }
        }
        var shaped=CharacterVisuals.shape("elder_nothic");
        check(shaped.height()>CharacterVisuals.shape("observer").height(),
            "Elder must not render as Hunter-sized");
        check(!CharacterVisuals.isHunter("banshee"),"Ghost cannot masquerade as selectable Hunter");
        for(CharacterVisuals.Hunter hunter:CharacterVisuals.Hunter.values()){
            var base=CharacterVisuals.hunterAppearance(hunter,CharacterVisuals.Variant.BASE);
            var missing=CharacterVisuals.hunterAppearance(hunter,CharacterVisuals.Variant.ALTERNATE_GENDER);
            check(base.runtimeIdentity().equals(missing.runtimeIdentity())
                    &&!base.inheritedBase()&&missing.inheritedBase(),
                "Unfinished gender variant must use that class's own base art");
            check(signature(DirectionalSprites.frame(missing.runtimeIdentity(),0))
                    ==signature(DirectionalSprites.frame(base.runtimeIdentity(),0)),
                "Fallback art must retain Hunter identity");
        }
        for(GhostIdentity.Type ghost:GhostIdentity.Type.values())if(!ghost.elder()){
            var alt=CharacterVisuals.ghostAppearance(ghost,CharacterVisuals.Variant.ALTERNATE_GENDER);
            check(alt.inheritedBase()&&alt.runtimeIdentity().equals(CharacterVisuals.ghost(ghost)),
                "Missing ghost gender variant must preserve its own identity");
        }
        var elderBase=CharacterVisuals.ghostAppearance(GhostIdentity.Type.ELDER_NOTHIC,
                CharacterVisuals.Variant.BASE);
        var elderAlt=CharacterVisuals.ghostAppearance(GhostIdentity.Type.ELDER_NOTHIC,
                CharacterVisuals.Variant.ALTERNATE_GENDER);
        check(elderAlt.inheritedBase()&&elderAlt.runtimeIdentity().equals(elderBase.runtimeIdentity()),
                "Incomplete Elder gender art must render its own approved baseline");
        BufferedImage ancient=DirectionalSprites.frame("ancient_banshee",0,
                LanGhostAnimation.Phase.WALK,2);
        BufferedImage original=DirectionalSprites.frame("banshee",0,
                LanGhostAnimation.Phase.WALK,2);
        check(signature(ancient)!=signature(original),
                "Ancient sprite must use its actual green source variant, not a base copy");
        boolean invalid=false;
        try{CharacterVisuals.hunter("unknown");}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Unapproved Hunter identity must be rejected");
        check(signature(DirectionalSprites.frame("cleric",0))!=observer,
            "Cleric must never be an Observer placeholder");
        System.out.println("PASS: six separately authored four-view masters; 26 static ghost identities; four camera facings and alpha masks");
    }
}
