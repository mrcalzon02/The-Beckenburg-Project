package org.beckenburg;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Real TCP host/guest/bot seats, private inventory and host-only campaign settlement. */
public final class CampaignCoopLobbyTest {
    static void ok(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void until(BooleanSupplier condition,String label)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(System.nanoTime()<end){if(condition.getAsBoolean())return;Thread.sleep(25);}
        throw new AssertionError("Timed out: "+label);
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("campaign-coop");
        var profile=new ProfileStore(folder.resolve("host.properties"));profile.addNetworkPayout(UUID.randomUUID(),3000,0);
        profile.reserveMissionHunters(5);var chapter=profile.dispatchStory(0);
        var guestProfile=new ProfileStore(folder.resolve("guest.properties"));guestProfile.save();byte[] before=Files.readAllBytes(folder.resolve("guest.properties"));
        var hostEvents=new ConcurrentLinkedQueue<String>();var guestEvents=new ConcurrentLinkedQueue<String>();
        try(var host=new LanHost("127.0.0.1",0,chapter.ghost(),CustomContract.standard(6),false,true,SiteCatalog.mapFor(chapter.map()))){
            host.configureCampaign(profile,chapter);
            try(var owner=new LanClient("127.0.0.1",host.port(),false,"Campaign owner","",host.localOwnerCredential(),hostEvents::add);
                var guest=new LanClient("127.0.0.1",host.port(),false,"Guest",guestEvents::add)){
                until(()->guestEvents.contains("MODE|CAMPAIGN"),"campaign guest mode advertised");
                boolean rejected=false;try(var ghost=new LanClient("127.0.0.1",host.port(),true,"Ghost",x->{})){}
                catch(java.io.IOException expected){rejected=true;}ok(rejected,"Campaign refuses human ghost");
                owner.ready();guest.ready();
                until(()->hostEvents.stream().anyMatch(x->x.startsWith("LOBBY|2|AI|2|")),"humans ready");
                host.prepareCampaignBots(5);ok(host.connected()==6,"Four bots supplement two humans");
                owner.start();until(host::running,"real campaign mission starts");
                ok(profile.missionHires()==0&&profile.currency()==2280,"Only four hires charged, displaced hire refunded");
                guest.start();until(()->guestEvents.stream().anyMatch(x->x.startsWith("REJECT|only verified")),"guest cannot start host case");
                owner.extract();until(()->!host.running(),"real retreat ends round");
                until(()->profile.archives().size()==1,"host receipt persisted");
                ok(profile.visits(chapter.map())==1&&profile.clears(chapter.map())==0&&profile.storyCheckpoint()==0,"Retreat records visit without advancing story");
                ok(guestEvents.stream().noneMatch(x->x.startsWith("PAYOUT|")),"No multiplayer payout leaks to campaign guest");
                ok(Arrays.equals(before,Files.readAllBytes(folder.resolve("guest.properties"))),"Guest campaign save remains byte-identical");
                ok(!host.retryCampaignSettlement(),"Successful settlement does not retry or duplicate");
            }
        }
        // Multiplayer can shop in its lobby, with host-enforced spending and distinct stock.
        var events=new ConcurrentLinkedQueue<String>();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1));
            var player=new LanClient("127.0.0.1",host.port(),false,"Buyer",events::add)){
            player.purchaseSupply(FieldSupplies.Kind.HEALTH,FieldSupplies.Tier.BASIC);
            until(()->events.contains("SUPPLY|350|1|0|0|0|0|0"),"pre-mission purchase acknowledged");
            player.equipAt(3,"UV_LIGHT");until(()->events.stream().anyMatch(x->x.startsWith("ACCEPT|GEAREQUIP|3|UV_LIGHT")),"pre-mission loadout acknowledged");
            ok(profile.supplyCount(FieldSupplies.Kind.HEALTH,FieldSupplies.Tier.BASIC)==0,"Multiplayer purchase cannot mutate campaign medicine");
        }
        System.out.println("PASS real TCP campaign co-op, six-seat cap, bot substitution/refund, guest isolation, host receipt and multiplayer lobby purchases");
    }
}
