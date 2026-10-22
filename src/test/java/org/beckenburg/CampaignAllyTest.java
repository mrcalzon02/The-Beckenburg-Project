package org.beckenburg;

/** Physical AI assistant may navigate, hold and report raw, unconfirmed readings. */
public final class CampaignAllyTest {
    private static void check(boolean valid,String why){if(!valid)throw new AssertionError(why);}
    public static void main(String[] args){
        for(HouseMap.Site site:new HouseMap.Site[]{HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,
                HouseMap.Site.SCHOOL}){
            HouseMap map=new HouseMap(site);
            var investigation=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,41);
            var assistant=new CampaignAlly(map,investigation);
            check(assistant.room()==HouseMap.EXTRACTION_ROOM,
                "Campaign recruit must enter with a physical van spawn on "+site);
            int notes=investigation.journal().size();
            int scanned=investigation.evidenceCount();
            String sample=assistant.readThermometer();
            check(sample.contains("Air ")&&!sample.contains("confirmed"),
                "Assistant must produce an actual local raw air measurement");
            check(investigation.journal().size()==notes
                &&investigation.evidenceCount()==scanned,
                "Ally observations may not automatically determine or log ghost evidence");
            assistant.hold();var held=assistant.position();
            for(int i=0;i<20;i++)assistant.advance(map.centerX(4),map.floorY(4),map.centerZ(4));
            check(assistant.position().equals(held),"Hold order must stop actual movement");
            int neighbor=map.adjacentRooms(HouseMap.EXTRACTION_ROOM).stream()
                .filter(room->room!=HouseMap.EXTRACTION_ROOM).findFirst().orElseThrow();
            assistant.investigate(neighbor);
            boolean moved=false;
            for(int i=0;i<900&&assistant.order()==CampaignAlly.Order.INVESTIGATE;i++){
                var before=assistant.position();
                assistant.advance(held.x(),held.y(),held.z());
                var after=assistant.position();
                double step=Math.hypot(after.x()-before.x(),after.z()-before.z());
                check(step<=.211,"Ally must never teleport or skip swept geometry");
                check(map.traversable(after.x(),after.y(),after.z()),
                    "Ally must not step into a wall, closed door or unsupported surface");
                if(step>.035)moved=true;
            }
            check(moved,"Autonomous destination order must produce real movement on "+site);
            check(assistant.room()==neighbor,
                "Autonomous investigator must reach adjacent authored zone on "+site+
                "; ended in "+assistant.room()+" / "+assistant.report());
            check(investigation.journal().size()==notes,
                "Automatic zone examination never writes evidence into player journal");
            assistant.follow();check(assistant.order()==CampaignAlly.Order.FOLLOW,
                "Regroup must resume player-follow order");
            System.out.println("PASS "+site+": real swept ally movement, orders, issued thermometer and manual journal");
        }
    }
}
