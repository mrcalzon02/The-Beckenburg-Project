package org.beckenburg;

import java.util.List;

/** Real campaign partner: approved Q callouts, typed chat and raw reported scans. */
public final class CampaignAllyChatTest {
    private static void check(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
    public static void main(String[] args){
        for(HouseMap.Site site:new HouseMap.Site[]{
                HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            var session=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,53L);
            var ally=new CampaignAlly(map,session);
            var playerSpot=map.van().spawn(0);
            var player=new LanWorld.Body(playerSpot.x(),map.floorY(HouseMap.EXTRACTION_ROOM),
                playerSpot.z(),0,HouseMap.EXTRACTION_ROOM);
            TeamCallout.Board board=new TeamCallout.Board(map);
            var hold=board.submit(new TeamCallout.Request(0,TeamCallout.Phrase.HOLD,
                player.x(),player.z(),player.x(),player.z(),0),
                player.y(),player.y());
            check(hold!=null,"Use the real Q-wheel callout validation");
            check(ally.onCallout(hold,player).contains("Holding")
                &&ally.order()==CampaignAlly.Order.HOLD,
                "Ally acts on a nearby, accepted HOLD emote");
            var follow=board.submit(new TeamCallout.Request(0,TeamCallout.Phrase.FOLLOW,
                player.x(),player.z(),player.x(),player.z(),12),
                player.y(),player.y());
            check(follow!=null&&ally.onCallout(follow,player).contains("Following")
                &&ally.order()==CampaignAlly.Order.FOLLOW,
                "Next permitted Q gesture resumes following");
            check(ally.onChat("hold",LanChat.Channel.LOCAL,player).contains("Holding"),
                "Same room local chat accepted");
            check(ally.order()==CampaignAlly.Order.HOLD,"Local spoken order applied");
            var far=map.spawn(4);
            var distant=new LanWorld.Body(far.x(),far.y(),far.z(),0,4);
            check(!LanChat.localVisible(map,distant,ally.listener()),
                "Distant chat speaker is physically outside voice range");
            check(ally.onChat("follow",LanChat.Channel.LOCAL,distant)==null
                &&ally.order()==CampaignAlly.Order.HOLD,
                "Out-of-range local speech cannot remote-control the ally");
            check(ally.onCallout(follow,distant)==null,
                "The Q wheel cannot order an out-of-earshot ally");
            check(ally.onChat("follow",LanChat.Channel.RADIO,distant).contains("Following"),
                "Radio chat can order a distant ally");
            check(ally.onChat("suspect",LanChat.Channel.RADIO,distant)
                .contains("No strong location hypothesis"),
                "No hidden ghost anchor or invented deductions at mission start");
            int journal=session.journal().size(),evidence=session.evidenceCount();
            String first=ally.readThermometer();
            check(first.contains("Air ")&&ally.totalScans()==1,"Real held thermometer reading");
            List<String> chat=ally.drainChat();
            check(chat.size()==1&&chat.get(0).contains("Air "),
                "The AI publishes its collected reading in the chat channel");
            check(ally.drainChat().isEmpty(),"No duplicated unsolicited transmission");
            for(int i=0;i<34;i++)session.tick(player.x(),player.y(),player.z(),false);
            String second=ally.readEmf();
            check(second.contains("EMF ")&&ally.totalScans()==2,
                "AI also has a functioning world-based EMF reader");
            check(ally.latestEvidence().contains("EMF")&&ally.observations().size()==2,
                "Reports refer to actual stored observations");
            check(ally.drainChat().stream().anyMatch(x->x.contains("EMF")),
                "AI reports EMF findings in game chat");
            check(ally.onChat("evidence",LanChat.Channel.RADIO,distant).contains("EMF"),
                "Player can request latest collected evidence from any room by radio");
            check(session.journal().size()==journal&&session.evidenceCount()==evidence,
                "AI communication does not forge the human journal or confirm ghost evidence");
            String go=ally.onChat("check 4",LanChat.Channel.RADIO,distant);
            check(go.contains(map.roomName(4))&&ally.destination()==4,
                "A typed chat investigation command assigns actual AI navigation");
            check(ally.onChat("status",LanChat.Channel.RADIO,distant).contains("2 measurements"),
                "AI reports its real accumulated work in status chat");
            System.out.println("PASS "+site+
                ": physically audible emotes, local/radio chat, witnessed instruments and manual evidence");
        }
        FieldTeamChat transcript=new FieldTeamChat();
        for(int i=0;i<34;i++)transcript.post(LanChat.Channel.RADIO,1,i,
            "Air 3.2°C / sample "+i);
        check(transcript.messages().size()==28,"Chat panel has bounded retained history");
        check(transcript.messages().get(0).tick()==6,"Chat retains newest messages in order");
        check(transcript.messages().stream().allMatch(m->m.message().indexOf('°')<0
            &&LanChat.Entry.decode(m.frame()).equals(m)),
            "Solo partner chat reuses valid ASCII LAN chat packet semantics");
        boolean invalid=false;
        try{transcript.post(LanChat.Channel.RADIO,1,2,"out of order");}
        catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Backdated chat messages rejected");
        System.out.println("PASS team chat: bounded transcript and LAN-compatible frames");
    }
}
