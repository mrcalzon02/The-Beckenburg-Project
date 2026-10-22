package org.beckenburg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Regression for catalog selection, neutral art, signature authorization and room FX. */
public final class GhostHauntingFolioTest {
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        check(GhostHauntingFolio.entries().size()==50,"50 distinct authored haunting entries");
        Set<String> names=new HashSet<>();
        for(var entry:GhostHauntingFolio.entries())
            check(names.add(entry.id()),"unique event ID");
        var samples=new GhostHauntingFolio(12345L);
        for(GhostIdentity.Type type:GhostIdentity.Type.values()){
            var event=samples.choose(GhostDomain.Action.SIGNATURE,type,4,100);
            check(event.entry().signatureType()==type.baseType(),"signature profile "+type);
            check(GhostHauntingFolio.decode(event.frame()).equals(event),"packet roundtrip");
            check(event.active(110,4)&&!event.active(110,3),"local room scope");
            check(!event.active(event.untilTick(),4),"finite event");
        }
        var left=new GhostHauntingFolio(789L);
        var right=new GhostHauntingFolio(789L);
        for(GhostDomain.Action action:GhostDomain.Action.values()){
            if(action==GhostDomain.Action.SIGNATURE)continue;
            for(int i=0;i<30;i++){
                var a=left.choose(action,GhostIdentity.Type.SPIRIT,4,i*100);
                var b=right.choose(action,GhostIdentity.Type.ELDER_NOTHIC,4,i*100);
                check(a.entry().signatureType()==null,"ordinary events shared across identities");
                check(a.entry().id().equals(b.entry().id()),"seeded event variety, species-blind");
            }
        }
        var event=samples.choose(GhostDomain.Action.MANIFEST,
            GhostIdentity.Type.ELDER_NOTHIC,4,120);
        int[] pixels=new int[64*40];
        Arrays.fill(pixels,0x303030);
        GhostHauntingFolio.paint(pixels,64,40,List.of(event),3,135);
        check(Arrays.stream(pixels).allMatch(v->v==0x303030),
            "no visual hallucination in other rooms");
        GhostHauntingFolio.paint(pixels,64,40,List.of(event),4,135);
        check(Arrays.stream(pixels).anyMatch(v->v!=0x303030),
            "the selected event must visibly affect the correct scene");
        for(LanGhostAnimation.Phase phase:LanGhostAnimation.Phase.values()){
            String art=GhostManifestationVisual.appearance("elder_nothic",phase);
            check(art.equals(phase==LanGhostAnimation.Phase.SIGNATURE
                ?"elder_nothic":GhostManifestationVisual.UNREVEALED),
                "only signature permits identifying character art");
        }
        var animation=new LanGhostAnimation();
        animation.manifested(100);
        animation.signature(101);
        animation.windup(102);
        check(animation.signatureVisible(102)
            &&animation.project(true,true,0,102).phase()==LanGhostAnimation.Phase.SIGNATURE,
            "signature reveal takes priority over an overlapping attack pose");
        check(!animation.signatureVisible(112),"reveal window expires");
        boolean rejected=false;
        try{GhostHauntingFolio.decode("HAUNT|unknown|4|100");}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"unknown events rejected");
        System.out.println("PASS: 50 real haunting entries, deterministic variety, timed room FX, signature-only art");
    }
}
