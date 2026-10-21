package org.beckenburg;

/**
 * Roaming Twins sibling acts in a different room, but pays from the single
 * shared ghost's energy and uses the exact same action cooldown. Nothing here
 * depends on hiding a packet from a player who inspects network traffic.
 */
public final class TwinCompanionSharedActionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        GhostDomain twins=new GhostDomain(map,4,GhostIdentity.Type.TWINS);
        twins.advance(1,4,1);
        var action=new GhostDomain.Command(GhostDomain.Action.WHISPER,5,1,GhostDomain.Source.AI);
        var result=twins.dispatchCompanionMinor(action,true);
        check(result.applied(),"AI sister must be allowed to haunt a valid different room");
        check(result.room()==5&&twins.room()==4,
            "Minor Twin event must occur at sister location without teleporting player Twin");
        check(result.fear()>0&&result.harm()==0,"Minor Twin action must not add second damage source");
        int after=twins.energy();
        check(after<100,"Companion must spend real shared GhostDomain energy");
        var human=twins.dispatch(new GhostDomain.Command(
            GhostDomain.Action.WHISPER,4,1,GhostDomain.Source.HUMAN));
        check(!human.applied(),"Companion must consume main Twin's action cooldown");
        var duplicate=twins.dispatchCompanionMinor(action,true);
        check(!duplicate.applied()&&twins.energy()==after,
            "Repeated minor event must not spend energy or replay effects");
        var wrong=twins.dispatchCompanionMinor(new GhostDomain.Command(
            GhostDomain.Action.STRIKE,5,2,GhostDomain.Source.AI),true);
        check(!wrong.applied(),"AI sibling cannot independently execute another strike");
        for(int tick=2;tick<=40;tick++)twins.advance(tick,4,1);
        check(twins.observe().legalActions().contains(GhostDomain.Action.WHISPER),
            "Shared cooldown must recover normally");
        human=twins.dispatch(new GhostDomain.Command(
            GhostDomain.Action.WHISPER,4,2,GhostDomain.Source.HUMAN));
        check(human.applied(),"Human Twin must regain the shared action after cooldown");
        for(var identity:new GhostIdentity.Type[]{
                GhostIdentity.Type.SPIRIT,GhostIdentity.Type.ELDER_NOTHIC}){
            GhostDomain stranger=new GhostDomain(map,4,identity);
            stranger.advance(1,4,1);
            var rejected=stranger.dispatchCompanionMinor(action,true);
            check(!rejected.applied()&&stranger.energy()==100,
                "Other species must not spawn an independent Twin actor budget");
        }
        GhostDomain ancient=new GhostDomain(map,4,GhostIdentity.Type.ANCIENT_TWINS);
        ancient.advance(1,4,1);
        check(ancient.dispatchCompanionMinor(action,false).applied(),
            "Approved Ancient Twins must share the same independent sister behavior");
        check(ancient.room()==4,"Ancient linked ghost anchor must not teleport");
        System.out.println("PASS: AI Twin room-local haunt, shared energy/cooldown, no duplicate strikes, Ancient parity");
    }
}
