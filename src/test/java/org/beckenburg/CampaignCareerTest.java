package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Durable campaign dispatch, idempotent case settlement and paid companion roster. */
public final class CampaignCareerTest {
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("beckenburg-career");
        Path save=folder.resolve("profile.properties");
        try{
            ProfileStore profile=new ProfileStore(save);
            profile.save(); // Legacy no-campaign file remains readable.
            check(!profile.hireAssistant(),"A zero-credit recruit must be refused");
            UUID receipt=UUID.randomUUID();
            check(profile.addNetworkPayout(receipt,600,0),"Real credits received");
            check(profile.hireAssistant(),"Paid assistant can be hired");
            check(!profile.hireAssistant(),"Cannot double-charge a permanent hire");
            check(profile.currency()==600-ProfileStore.ASSISTANT_HIRE_COST,
                "Actual credits deducted exactly once");
            long seedA=profile.nextCampaignSeed(SiteCatalog.Location.HOUSE);
            long seedB=profile.nextCampaignSeed(SiteCatalog.Location.HOUSE);
            check(seedA!=seedB,"Successive campaigns must have distinct mission seeds");
            ProfileStore reopened=new ProfileStore(save);reopened.load();
            check(reopened.assistantHired()&&
                reopened.currency()==600-ProfileStore.ASSISTANT_HIRE_COST,
                "Recruit and balance persist through a real disk reload");
            check(reopened.nextCampaignSeed(SiteCatalog.Location.HOUSE)!=seedB,
                "Dispatch serial persists through restarts");
            HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
            var session=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,seedA);
            var at=map.van().spawn(0);
            check(session.extract(at.x(),VanStaging.FLOOR_Y,at.z()),
                "The solo case must end through the actual van extraction");
            check(reopened.addPayout(session,SiteCatalog.Location.HOUSE),
                "Resolved campaign mission can be settled");
            int credits=reopened.currency(),experience=reopened.experience();
            check(!reopened.addPayout(session,SiteCatalog.Location.HOUSE),
                "Same resolved case cannot be paid or counted twice");
            check(reopened.visits(SiteCatalog.Location.HOUSE)==1&&
                reopened.clears(SiteCatalog.Location.HOUSE)==0,
                "A retreat is a visited but uncleared dossier");
            ProfileStore again=new ProfileStore(save);again.load();
            check(again.visits(SiteCatalog.Location.HOUSE)==1
                &&again.currency()==credits&&again.experience()==experience,
                "Campaign ledger and payouts survive atomic save");
            var invalid=new ProfileStore(folder.resolve("broken.properties"));
            Files.writeString(folder.resolve("broken.properties"),
                "schema=1\ncurrency=0\nexperience=0\ncampaign.visits.HOUSE=-7\n");
            boolean refused=false;
            try{invalid.load();}catch(java.io.IOException expected){refused=true;}
            check(refused,"Invalid campaign ledger cannot silently repair its own corruption");
            System.out.println("PASS career: paid hire, reload, durable unique dispatch, idempotent visit/outcome and payout");
        }finally{
            Files.deleteIfExists(save);Files.deleteIfExists(folder.resolve("broken.properties"));
            Files.deleteIfExists(folder);
        }
    }
}
