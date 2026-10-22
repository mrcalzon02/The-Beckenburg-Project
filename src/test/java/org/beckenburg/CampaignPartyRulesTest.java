package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;

/** Physical AI squadmates increase threat and pay the owning host, not guests. */
public final class CampaignPartyRulesTest {
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
    private static InvestigationSession cleared(int party){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var session=new InvestigationSession(map,InvestigationSession.Role.CLERIC,778812L);
        session.configureParty(party);
        var anchor=map.spawn(session.anchorRoom());
        check(session.placeWard(anchor.x(),anchor.y(),anchor.z()),"Real ward required");
        check(session.cleanse(anchor.x(),anchor.y(),anchor.z()),"Real cleansing required");
        return session;
    }
    public static void main(String[] args)throws Exception{
        check(CampaignPartyRules.recoveryRate(1)==100&&
            CampaignPartyRules.recoveryRate(2)==115&&
            CampaignPartyRules.recoveryRate(6)==175,
            "Every additional deployed investigator materially raises ghost energy recovery");
        check(CampaignPartyRules.additionalHarm(1)==0&&
            CampaignPartyRules.additionalHarm(6)==5,
            "Ghost wounds scale with real team size");
        boolean rejected=false;
        try{CampaignPartyRules.hostCredits(100,7);}
        catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"No unsupported seventh Hunter");
        var solo=cleared(1);
        var duo=cleared(2);
        check(duo.payout()==solo.payout()*2,
            "Two person campaign awards exactly two personal shares to the host");
        check(solo.hunterLevel()==duo.hunterLevel(),"Party members do not mint extra class XP");
        var squad=cleared(6);
        check(squad.payout()==solo.payout()*6,"Six member ceiling awards six earned shares");
        var live=new InvestigationSession(new HouseMap(HouseMap.Site.HOUSE),
            InvestigationSession.Role.OBSERVER,88);
        live.configureParty(2);
        var at=live.map().van().spawn(0);
        live.tick(at.x(),live.map().floorY(HouseMap.EXTRACTION_ROOM),at.z(),false);
        rejected=false;
        try{live.configureParty(3);}
        catch(IllegalStateException expected){rejected=true;}
        check(rejected,"Joining mid-round cannot retroactively change payout or ghost power");
        Path dir=Files.createTempDirectory("beckenburg-party");
        Path file=dir.resolve("campaign.properties");
        try{
            ProfileStore host=new ProfileStore(file);
            var first=host.dispatchStory(0);
            var mission=new InvestigationSession(new HouseMap(SiteCatalog.mapFor(first.map())),
                InvestigationSession.Role.OBSERVER,
                CampaignStory.missionSeed(host.storySeed(),0),1,first.ghost());
            mission.configureParty(2);
            var centre=mission.map().spawn(mission.anchorRoom());
            check(mission.placeWard(centre.x(),centre.y(),centre.z())&&
                mission.cleanse(centre.x(),centre.y(),centre.z()),"Host crew completes true rite");
            int before=host.currency();
            check(host.settleStory(mission,0),"Real campaign host receives the group reward");
            check(host.currency()==before+mission.payout()&&host.storyCheckpoint()==1,
                "All shares and chapter advancement belong to only the host");
            int oldXp=host.disciplineExperience(InvestigationSession.Role.OBSERVER);
            check(oldXp==100,"One completed mission awards class XP once, regardless of teammates");
            check(!host.settleStory(mission,0),"No second guest-triggered host payment");
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(dir);}
        System.out.println("PASS party: AI Hunter counts toward threat and host rewards without extra class XP");
    }
}
