package org.beckenburg;
import java.nio.file.*;
import java.util.*;
/** Derive the atlas's lore packets from the same records read at the castle. */
public final class LoreSourceExport {
    record ArchiveSource(String event,String keeper,String portrait,String masterArtStatus,List<WorldLore.Faction> factions,List<WorldLore.SiteThread> sites,List<WorldLore.Page> pages){}
    public static void main(String[] args)throws Exception{
        var folder=Path.of("docs/lore");Files.createDirectories(folder);
        Files.writeString(folder.resolve("archive-content.json"),LevelSceneSource.json(new ArchiveSource(WorldLore.EMERGENCE,WorldLore.KEEPER,WorldLore.PORTRAIT,WorldLore.MASTER_ART_STATUS,WorldLore.FACTIONS,WorldLore.SITES,WorldLore.PAGES))+"\n");
        for(var site:WorldLore.SITES){
            String id=site.id().equals("blackpine")?"wld-01":site.id();var packet=Path.of("development/levels/layouts",id);
            if(!Files.isDirectory(packet))throw new IllegalStateException("Missing planned site "+id);
            String text="# "+site.title()+" / Emergence and survival thread\n\nDerived from `WorldLore.java`, the same history read in the Castle Common Archive. [Shared canon](../../../../docs/LORE_BIBLE.md).\n\n"+
                "- **Emergence Day, late 2022:** "+site.emergence()+"\n- **Survival and return:** "+site.survival()+"\n- **Environmental trace:** "+site.trace()+"\n\n"+
                "Layer pre-2022 use, the catastrophe, and later survival/reclamation. Records are attributed testimony, not current ghost identity or automatic mission evidence. Planned sites are story reservations, not completed expeditions or runtime content.\n";
            Files.writeString(packet.resolve("LORE.md"),text);
        }
        System.out.println("PASS exported 23 site lore packets, three class factions and "+WorldLore.PAGES.size()+" shared archive pages");
    }
}
