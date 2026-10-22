package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** True story progression: saved campaign seed, chapter gates and host-owned rewards. */
public final class CampaignStoryProgressionTest {
    private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static InvestigationSession round(CampaignStory.Chapter chapter,long seed){
        return new InvestigationSession(new HouseMap(SiteCatalog.mapFor(chapter.map())),
            InvestigationSession.Role.OBSERVER,CampaignStory.missionSeed(seed,chapter.ordinal()),
            1,chapter.ghost());
    }
    private static void win(InvestigationSession session){
        var anchor=session.map().spawn(session.anchorRoom());
        check(session.placeWard(anchor.x(),anchor.y(),anchor.z()),
            "A real ward must be prepared in the physical attachment room");
        check(session.cleanse(anchor.x(),anchor.y(),anchor.z()),
            "Only a completed physical rite can count as campaign victory");
        check(session.outcome()==InvestigationSession.Outcome.CLEANSED,"Real outcome");
    }
    public static void main(String[] args)throws Exception{
        for(long seed:new long[]{4L,9001L,-7L}){
            var chapters=CampaignStory.generate(seed);
            check(chapters.size()==22&&CampaignStory.generate(seed).equals(chapters),
                "Exactly 22 deterministic named chapters");
            check(chapters.stream().map(CampaignStory.Chapter::name).distinct().count()==22,
                "The story visits 22 unique named destinations");
            check(chapters.subList(0,13).stream().map(CampaignStory.Chapter::ghost)
                    .distinct().count()==13
                &&chapters.subList(0,13).stream().noneMatch(c->c.ghost().ancient()||c.elder()),
                "All thirteen original ghosts appear once before the ancient chapters");
            check(chapters.subList(13,19).stream().allMatch(c->c.ghost().ancient())
                &&chapters.subList(13,19).stream().map(CampaignStory.Chapter::ghost)
                    .distinct().count()==6,
                "The next six chapters draw distinct ancient variants");
            check(chapters.get(19).ghost()==GhostIdentity.Type.ELDER_BLADE_DEMON
                &&chapters.get(20).ghost()==GhostIdentity.Type.ELDER_PLAGUE_DEMON
                &&chapters.get(21).ghost()==GhostIdentity.Type.ELDER_NOTHIC,
                "Canonical final three Elders occur in the required order");
            check(chapters.stream().allMatch(c->!c.arrival().isBlank()
                &&!c.firstSurvey().isBlank()&&!c.escalation().isBlank()),
                "Every chapter has staged arrival, investigation and escalation narration");
        }
        int elders=0,ancients=0;
        for(int i=0;i<12000;i++){
            var ghost=CampaignStory.rollFreePlay(i*17L+3);
            if(ghost.elder())elders++;
            if(ghost.ancient())ancients++;
        }
        check(elders>0&&elders<70&&ancients>600,
            "Elders must be genuinely rare (<0.6%) in ordinary play, unlike mandatory story bosses");

        Path directory=Files.createTempDirectory("beckenburg-story");
        Path file=directory.resolve("host.properties");
        Path network=directory.resolve("network.properties");
        try{
            ProfileStore host=new ProfileStore(file);
            check(host.storyCheckpoint()==0&&!host.hasCampaignStory(),
                "New story profile has no predetermined outcomes before first dispatch");
            check(!host.chapterUnlocked(1),"Second chapter is initially inaccessible");
            boolean blocked=false;
            try{host.dispatchStory(1);}catch(IllegalArgumentException ok){blocked=true;}
            check(blocked,"Cannot bypass the first story mission");
            var first=host.dispatchStory(0);
            check(first.built()&&first.map()==SiteCatalog.Location.HOUSE,
                "Chapter one uses only validated House geometry");
            long pinned=host.storySeed();
            check(pinned!=0&&first.equals(host.dispatchStory(0)),
                "Replaying the first case never rerolls a ghost assignment");
            var retreat=round(first,pinned);
            var van=retreat.map().van().spawn(0);
            check(retreat.extract(van.x(),VanStaging.FLOOR_Y,van.z()),
                "Hunter actually withdraws inside the van");
            check(host.settleStory(retreat,0),"Real retreat may settle its partial wages");
            check(host.storyCheckpoint()==0&&host.visits(first.map())==1&&host.clears(first.map())==0,
                "Survival and income do NOT unlock the second chapter");
            check(!host.settleStory(retreat,0),"A second receipt must never pay twice");
            ProfileStore loaded=new ProfileStore(file);loaded.load();
            check(loaded.storySeed()==pinned&&loaded.campaignChapter(0).equals(first),
                "Saved assignment survives process restart");
            var successful=round(first,pinned);
            var same=round(first,pinned);
            check(successful.anchorRoom()==same.anchorRoom()
                &&successful.ghostVisualType()==same.ghostVisualType(),
                "Retry uses identical ghost type AND deterministic physical attachment");
            win(successful);
            check(loaded.settleStory(successful,0)&&loaded.storyCheckpoint()==1,
                "Only verified cleansing advances host story to School");
            var second=loaded.dispatchStory(1);
            check(second.map()==SiteCatalog.Location.SCHOOL,
                "Authored story moves to Ravenshade School second");
            check(!loaded.chapterUnlocked(2),"Farmhouse remains locked until School success");
            var replay=round(first,pinned);win(replay);
            int money=loaded.currency();
            check(loaded.settleStory(replay,0)&&loaded.currency()>money&&loaded.storyCheckpoint()==1,
                "Earlier cleared levels can earn money without advancing story");
            var schoolRound=round(second,pinned);win(schoolRound);
            check(loaded.settleStory(schoolRound,1)&&loaded.storyCheckpoint()==2,
                "School victory unlocks Farmhouse");
            var third=loaded.dispatchStory(2);
            check(third.map()==SiteCatalog.Location.FARMHOUSE,"Third chapter uses Farmhouse");
            var farmRound=round(third,pinned);win(farmRound);
            check(loaded.settleStory(farmRound,2)&&loaded.storyCheckpoint()==3,
                "Three genuine victories unlock the fourth chapter index");
            blocked=false;
            try{loaded.dispatchStory(3);}catch(IllegalArgumentException unavailable){blocked=true;}
            check(blocked,"Chapter four cannot silently use House geometry as a placeholder");
            ProfileStore persisted=new ProfileStore(file);persisted.load();
            check(persisted.storySeed()==pinned&&persisted.storyCheckpoint()==3
                &&persisted.archives().size()==5,
                "Story seed, victories, replay income and archives remain intact on disk");
            ProfileStore multiplayer=new ProfileStore(network);
            multiplayer.addNetworkPayout(UUID.randomUUID(),4000,50);
            check(multiplayer.currency()==4000&&persisted.currency()!=4000,
                "Multiplayer progression stored in a separate wallet");
            check(!multiplayer.hasCampaignStory()&&multiplayer.storyCheckpoint()==0,
                "Multiplayer progress never overwrites the host's story campaign");
            check(ProfileStore.multiplayerPath().equals(ProfileStore.defaultPath()
                .resolveSibling("multiplayer.properties")),
                "Production LAN payouts use a different profile filename");
            System.out.println("PASS 22-chapter seeded campaign, three playable victory gates, replay rewards and separate LAN wallet");
        }finally{
            Files.deleteIfExists(file);Files.deleteIfExists(network);
            Files.deleteIfExists(directory);
        }
    }
}
