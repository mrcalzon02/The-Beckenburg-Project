package org.beckenburg;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/** Anchor selection and compulsory return, then a real TCP hunter against the default AI. */
public final class LanGhostHomeAndThreatTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    private static void returnAndRest(){
        HouseMap map=new HouseMap();
        var anchors=new HashSet<Integer>();
        for(long seed=0;seed<24;seed++){
            int anchor=LanHost.chooseAnchor(map,seed);
            check(anchor==LanHost.chooseAnchor(map,seed),"retry changed anchor");
            anchors.add(anchor);
        }
        check(anchors.size()>1,"every investigation started in the same room");
        var ghost=new GhostDomain(map,4,GhostIdentity.Type.POLTERGEIST);
        long sequence=0;
        int homeTick=-1,lastRoom=ghost.room(),hops=0;
        for(int t=1;t<=480;t++){
            ghost.advance(t,-1,0);
            int next=t==1?5:t==40?8:ghost.homewardStep();
            if(t==60)check(ghost.spendFrenzyEnergy(ghost.energy()),"exhaust presence feeder");
            if(t==170)ghost.slowFor(70);
            if(t>=181&&t<240)check(next<0,"recall bypassed salt movement slow");
            if(next>=0){
                var result=ghost.dispatch(new GhostDomain.Command(GhostDomain.Action.DRIFT,
                    next,++sequence,GhostDomain.Source.AI));
                check(result.applied()&&map.ghostExits(lastRoom).contains(next),"illegal recall hop");
                if(t>40)hops++;
                lastRoom=next;
                if(next==4)homeTick=t;
            }
            if(homeTick>0&&t<homeTick+GhostDomain.HOME_DWELL_TICKS)
                check(!ghost.observe().legalActions().contains(GhostDomain.Action.DRIFT),
                    "ghost abandoned home before observation window");
        }
        check(homeTick>=240&&hops>=2&&lastRoom==4,"exhausted ghost did not navigate home");
        check(ghost.observe().exits().size()>0,"home dwell never ended");
        System.out.println("Anchor selection "+anchors+"; depleted/slow ghost returned at tick "
            +homeTick+" in "+hops+" hops and stayed "+GhostDomain.HOME_DWELL_TICKS+" ticks");
    }
    private static void attackSoundingHunter()throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1));
            var hunter=new LanGhostHearingIntegrationTest.Peer(host.port(),false,"Sounding hunter")){
            hunter.client.ready();hunter.await("OWNREADY|YES");
            hunter.client.start();hunter.await("INFO|mission started");
            // Set up via public room travel, then opt into ordinary 3-D collision and combat.
            LanTestRoomTravel.move(hunter.client,hunter::await,4);
            hunter.client.view3D();hunter.await("ACCEPT|VIEW3D");
            int lastEmote=-100;
            var observed=new HashSet<String>();
            boolean warned=false;
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(50);
            while(System.nanoTime()<end){
                var state=host.devAdminSnapshot();
                check(state.ghost().hostAiFallback(),"test attached a ghost controller");
                if(state.tick()-lastEmote>=9){
                    hunter.client.emote(HunterEmote.WAVE);
                    lastEmote=state.tick();
                }
                String line;
                while((line=hunter.inbox.poll())!=null){
                    if(line.startsWith("THREAT|START|"))warned=true;
                    if(line.startsWith("LIGHTFX|"))observed.add(LanProtocol.parts(line).get(2));
                }
                var hit=state.combatEvents().stream().filter(e->e.seat().equals("H1")
                    &&e.action().equals("STRIKE")&&e.damage()>0).findFirst();
                if(hit.isPresent()){
                    check(state.hunters().get(0).health()<100,"no host health change");
                    check(warned,"normal spectral strike skipped its wind-up");
                    System.out.println("Live 3-D hunter H1: telegraph then "+hit.get().damage()
                        +" HP lost to AI STRIKE at tick "+hit.get().tick()+"; effects "+observed);
                    return;
                }
                Thread.sleep(70);
            }
            throw new AssertionError("AI did not strike a sounding 3-D hunter: "+host.devAdminSnapshot());
        }
    }
    public static void main(String[] args)throws Exception{
        returnAndRest();
        attackSoundingHunter();
    }
}
