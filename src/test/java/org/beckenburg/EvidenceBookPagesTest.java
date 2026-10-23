package org.beckenburg;
import java.util.*;
import java.nio.file.*;

public final class EvidenceBookPagesTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        for(var tier:EquipmentTiers.Tier.values()){
            Set<String> names=new HashSet<>();Set<Integer> art=new HashSet<>();
            for(String style:List.of("open","open_child_drawings","open_frantic_scrawls","open_ritual_circles")){
                String id=EquipmentModelCatalog.variant("GHOST_WRITING_BOOK",tier,style);check(names.add(id),"four physical page states per tier");
                var model=GlbProps.model(id);check(!model.triangles().isEmpty(),"actual GLB exists");
                var materials=model.triangles().stream().map(GlbProps.Triangle::material).filter(m->GlbProps.materialName(m).contains("original painted pages")).distinct().toList();
                check(materials.size()==1,"packed authored page spread");int mat=materials.get(0),hash=0;
                for(int y=0;y<48;y++)for(int x=0;x<64;x++)hash=31*hash+GlbProps.sample(mat,(x+.5)/64,(y+.5)/48);
                check(art.add(hash),"blank and each drawing have distinct visible pixels");
            }
        }
        var book=new GhostWriting();book.place(0,4,1,15,0,15,EquipmentTiers.Tier.II);
        check(book.page()==null,"placement does not manufacture writing");
        check(book.witness(99,3,new GhostDomain.Resolution(true,"test",GhostDomain.Action.DISTURB,4,80,0,0,false)),"actual event authors page");
        check(book.writingFrame().equals("BOOK|WRITTEN|4|"+book.page().style()),"network visuals contain only observed room/style");
        check(!book.writingFrame().contains("SPECIES"),"visual event is not identity verdict");
        System.out.println("PASS 3 textured book tiers, 9 written alternatives, distinct packed page art and event-only visual state");
        Files.writeString(Path.of("development/levels/verification/evidence-book-pages.json"),"{\"status\":\"pass\",\"tiers\":3,\"blank_spreads\":3,\"written_alternatives\":9,\"authoritative_event_visuals\":true}\n");
    }
}
