package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Campaign social progression, authored branches, host snapshot and local save. */
public final class CastleRelationsTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static InvestigationSession actualCase(CampaignStory.Chapter chapter,long seed){
        return new InvestigationSession(new HouseMap(SiteCatalog.mapFor(chapter.map())),
            InvestigationSession.Role.OBSERVER,
            CampaignStory.missionSeed(seed,chapter.ordinal()),1,chapter.ghost());
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("beckenburg-relations");
        Path save=dir.resolve("host.properties");
        try{
            ProfileStore host=new ProfileStore(save);
            var rook=CastleRelations.Resident.MATTHIS_ROOK;
            var elspeth=CastleRelations.Resident.ELSBETH_VALE;
            check(CastleRelations.Resident.values().length==5,
                "All five named Castle residents must be in the dialogue registry");
            var early=CastleRelations.context(host);
            check(early.completed()==0&&early.hunterLevel()==1,
                "Story conversation starts from the real new-game profile");
            check(CastleRelations.page(rook,early,0,CastleRelations.Topic.GREETING)
                .choices().stream().anyMatch(c->c.destination()==CastleRelations.Topic.NEXT_LOCATION),
                "Dialogue offers real branching, not a one-line menu");
            String firstHint=CastleRelations.page(rook,early,0,
                CastleRelations.Topic.NEXT_LOCATION).spoken();
            check(firstHint.contains(CampaignBoardPanel.catalog().get(0).name()),
                "The first chapter hint refers to the actual campaign site");
            check(host.recordDialogue(rook,CastleRelations.Topic.NEXT_LOCATION),
                "A first helpful discussion increases relationship once");
            check(!host.recordDialogue(rook,CastleRelations.Topic.NEXT_LOCATION),
                "Repeated dialogue must never farm unlimited trust");
            check(host.relationship(rook)==1&&host.relationship(elspeth)==0,
                "Relationship grows independently for each resident");
            check(host.recordDialogue(rook,CastleRelations.Topic.PERSONAL)
                &&host.relationship(rook)==3,
                "Personal branch contributes more trust than casual field advice");
            check(!host.recordDialogue(rook,CastleRelations.Topic.GREETING),
                "Opening conversation is not a repeatable reward");
            var chapter=host.dispatchStory(0);
            long seed=host.storySeed();
            var mission=actualCase(chapter,seed);
            var place=mission.map().spawn(mission.anchorRoom());
            check(mission.placeWard(place.x(),place.y(),place.z())
                &&mission.cleanse(place.x(),place.y(),place.z()),
                "Story progress requires an actual completed ritual");
            check(host.settleStory(mission,0)&&host.storyCheckpoint()==1,
                "Host chapter advances after a genuinely successful campaign mission");
            var updated=CastleRelations.context(host);
            check(updated.completed()==1,"Dialogue progress immediately follows settled story");
            String newHint=CastleRelations.page(rook,updated,host.relationship(rook),
                CastleRelations.Topic.NEXT_LOCATION).spoken();
            check(!newHint.equals(firstHint)&&
                newHint.contains(CampaignBoardPanel.catalog().get(1).name()),
                "Post-mission conversations dynamically reveal the next assigned location");
            check(host.recordDialogue(rook,CastleRelations.Topic.NEXT_LOCATION)
                &&host.relationship(rook)==4,
                "New chapter resets the conversation-topic opportunity exactly once");
            ProfileStore loaded=new ProfileStore(save);loaded.load();
            check(loaded.relationship(rook)==4
                &&!loaded.recordDialogue(rook,CastleRelations.Topic.NEXT_LOCATION),
                "No duplicate trust after disk reload");
            check(loaded.relationship(elspeth)==0&&loaded.storyCheckpoint()==1,
                "Other social relationships and campaign state survive reload");
            var view=CastleRelations.hostView(loaded);
            var wire=CastleRelations.decodeHostView(view.frame());
            check(wire.equals(view)&&wire.context().completed()==1&&wire.trust(rook)==4,
                "Network host social snapshot uses correct story and relationship standing");
            check(!wire.context().equals(early),
                "Co-op dialogue is based on the current host, not a joining guest");
            boolean bad=false;
            try{CastleRelations.decodeHostView("STORYSOCIAL|24|3|1,1,1,1,1");}
            catch(IllegalArgumentException expected){bad=true;}
            check(bad,"Reject forged impossible campaign progress");
            SwingUtilities.invokeAndWait(()->{
                CastleDialoguePanel player=new CastleDialoguePanel(loaded,rook,null,null);
                check(player.spoken().contains("I have read the report"),
                    "Castle dialogue loads the current story when opened");
                player.select(CastleRelations.Topic.NEXT_LOCATION);
                check(player.spoken().contains(CampaignBoardPanel.catalog().get(1).name()),
                    "Live in-world conversation shows actual next-location lore");
                int ownerTrust=loaded.relationship(rook);
                CastleDialoguePanel guest=new CastleDialoguePanel(rook,wire.context(),wire.trust(rook));
                guest.select(CastleRelations.Topic.PERSONAL);
                check(loaded.relationship(rook)==ownerTrust,
                    "Guest can explore host dialogue without writing the host or guest profile");
                check(guest.spoken().equals(CastleRelations.page(rook,wire.context(),
                    wire.trust(rook),CastleRelations.Topic.PERSONAL).spoken()),
                    "The co-op preview uses the same graph and the exact host social snapshot");
            });
            check(CastleRelations.page(elspeth,updated,loaded.relationship(elspeth),
                CastleRelations.Topic.QUEST).spoken().contains("SUPPLY LEAD"),
                "NPCs provide distinct actionable lore leads, not phantom paid quests");
            check(CastleRelations.page(CastleRelations.Resident.MIRELLE_ASH,updated,0,
                CastleRelations.Topic.FIELD_ADVICE).spoken().contains("Wards"),
                "Ritual scholar gives distinctive field advice");
            System.out.println("PASS Castle relationships: persisted branches, actual mission unlocks, host-owned co-op dialogue and useful leads");
        }finally{
            Files.deleteIfExists(save);Files.deleteIfExists(dir);
        }
    }
}
