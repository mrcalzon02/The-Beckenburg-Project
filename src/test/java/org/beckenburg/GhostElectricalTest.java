package org.beckenburg;
import java.util.*;
/** Accepted ordinary haunts, independent source/breaker state and actual host TCP propagation. */
public final class GhostElectricalTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static GhostHauntingFolio.Occurrence event(String id,int room,int tick){return new GhostHauntingFolio.Occurrence(GhostHauntingFolio.entry(id),room,tick);}
    public static void main(String[] args)throws Exception{
        boolean furnishings=args.length>0&&args[0].equals("FURNISHINGS");boolean services=args.length>0&&args[0].equals("SERVICES");var map=furnishings?HouseRebuildFurnishings.map():services?HouseRebuildServices.map():HouseRebuildFitout.map();int combinations=0;
        for(var type:GhostIdentity.Type.values())for(var source:GhostDomain.Source.values()){
            var ghost=new GhostDomain(map,4,type);ghost.advance(1,4);var accepted=ghost.dispatch(new GhostDomain.Command(GhostDomain.Action.DISTURB,4,1,source));
            check(accepted.applied(),"Ordinary disturb rejected "+type+" / "+source);var lights=new RoomLighting();
            check(GhostElectrical.apply(map,lights,accepted,event("lightning_static",4,1),1),"Accepted electrical arc must trip ordinary breaker");
            check(!lights.breaker()&&lights.sourceRunning()&&!lights.supplied(),"Ghost trip must preserve running source");
            check(!GhostElectrical.apply(map,lights,accepted,event("lightning_static",4,1),1),"Tripped breaker cannot re-trigger");
            lights.setSourceRunning(false);lights.setSourceRunning(true);check(!lights.supplied(),"Generator restart must not undo ghost trip");lights.setBreaker(true);check(lights.supplied(),"Ordinary allowed breaker reset");
            check(!GhostElectrical.apply(map,lights,accepted,event("lamp_convulsion",4,1),1)&&lights.breaker(),"Other approved disturbances must retain their pulse behavior");
            lights.toggle(4);check(!GhostElectrical.apply(map,lights,accepted,event("lightning_static",4,1),1),"Unpowered room load cannot arc");lights.toggle(4);
            lights.setSourceRunning(false);check(!GhostElectrical.apply(map,lights,accepted,event("lightning_static",4,1),1)&&lights.breaker(),"Stopped source must not cause a mains trip");lights.setSourceRunning(true);
            check(!GhostElectrical.apply(map,lights,accepted,event("lightning_static",3,1),1),"Mismatched event room cannot trip");
            check(!GhostElectrical.apply(map,lights,accepted,event("lightning_static",4,0),1),"Stale event cannot trip");
            var rejected=new GhostDomain.Resolution(false,"rejected",GhostDomain.Action.DISTURB,4,100,0,0,false);
            check(!GhostElectrical.apply(map,lights,rejected,event("lightning_static",4,1),1),"Rejected command has no electrical effect");combinations++;
        }
        long soloSeed=-1;
        for(long seed=0;seed<1000&&soloSeed<0;seed++){
            int anchor=new Random(seed).nextInt(map.interiorRoomCount());var domain=new GhostDomain(map,anchor,GhostIdentity.Type.SPIRIT);
            var ai=new GhostDomain.AiController(seed^0x4245434b454eL);var folio=new GhostHauntingFolio(seed^0x4841554e54534cL);
            for(int tick=1;tick<=41;tick++){domain.advance(tick,-1,0);var command=ai.decide(domain.observe());if(command==null)continue;var result=domain.dispatch(command);if(!result.applied())continue;
                var occurrence=folio.choose(result.action(),GhostIdentity.Type.SPIRIT,result.room(),tick);
                if(occurrence!=null&&occurrence.entry().id().equals("lightning_static")&&result.room()<12){soloSeed=seed;break;}}
        }
        check(soloSeed>=0,"Deterministic actual AI electrical-event fixture");var soloMap=furnishings?HouseRebuildFurnishings.map():services?HouseRebuildServices.map():HouseRebuildFitout.map();var soloLights=new RoomLighting();
        var solo=new InvestigationSession(soloMap,InvestigationSession.Role.OBSERVER,soloSeed,1,GhostIdentity.Type.SPIRIT);solo.connectLighting(soloLights);
        var station=soloMap.van().world(0,0);for(int tick=1;tick<=41;tick++)solo.tick(station.x(),soloMap.van().floorY(),station.z(),false);
        check(solo.hauntings().stream().anyMatch(e->e.entry().id().equals("lightning_static")),"Actual solo AI emitted approved electrical event");
        check(!soloLights.breaker()&&soloLights.sourceRunning(),"Actual connected solo session trips the same persistent breaker");
        // Deliberate isolated circuit fixture: outdoor room IDs must not bypass a real blackout.
        var definition=map.level();var sockets=new ArrayList<>(definition.sockets());
        sockets.add(new org.beckenburg.LevelDefinition.Socket("electrical-test-yard","light",13,4,3,29,0,"utility"));
        sockets.add(new org.beckenburg.LevelDefinition.Socket("electrical-test-candle","light",13,8,3,29,0,"candle-chandelier"));
        var testSource=new org.beckenburg.LevelDefinition(definition.id(),"electrical-outdoor-control-fixture",definition.title(),definition.rooms(),definition.portals(),definition.stairs(),definition.voids(),definition.props(),sockets,definition.colliders(),definition.dock(),definition.ambience(),definition.water(),definition.sounds());
        var fixture=new HouseMap(HouseMap.Site.HOUSE,testSource);var circuits=new RoomLighting();var field=new FixtureLighting(fixture);field.frame(circuits,0);
        double yardOn=field.sample(13,4,1,29).energy(),candleOn=field.sample(13,8,1,29).energy();var van=fixture.van().world(0,0);double vanOn=field.sample(14,van.x(),fixture.van().floorY()+1.2,van.z()).energy();
        circuits.setBreaker(false);field.frame(circuits,0);double yardOff=field.sample(13,4,1,29).energy(),candleOff=field.sample(13,8,1,29).energy(),vanOff=field.sample(14,van.x(),fixture.van().floorY()+1.2,van.z()).energy();
        check(yardOn>yardOff+.1,"Connected outdoor light must lose electrical supply");check(candleOff>=candleOn-.02&&candleOff>0,"Independent candle remains lit");check(Math.abs(vanOn-vanOff)<.01&&vanOff>0,"Van battery lighting remains independent");
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Scholar");var g=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();g.client.ready();a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            long seed=0;while(!new GhostHauntingFolio(seed).choose(GhostDomain.Action.DISTURB,GhostIdentity.Type.SPIRIT,4,1).entry().id().equals("lightning_static"))seed++;
            Object mutex=LanSafeAreaBarrierTest.field(host,"mutex");synchronized(mutex){
                var folio=(GhostHauntingFolio)LanSafeAreaBarrierTest.field(host,"hauntingFolio");((Random)LanSafeAreaBarrierTest.field(folio,"random")).setSeed(seed);
            }
            g.client.ghost(GhostDomain.Action.DISTURB,4);g.await("ACCEPT|GHOST|DISTURB");
            String frame=b.await("LIGHTS|");check(frame.endsWith("|0"),"Real accepted ghost action must broadcast breaker trip");
            var authority=(RoomLighting)LanSafeAreaBarrierTest.field(host,"lights");check(!authority.breaker()&&authority.sourceRunning(),"Host source remains running after actual trip");
            a.client.view3D();a.await("ACCEPT|VIEW3D");check(a.await("LIGHTS|").endsWith("|0"),"View-entry snapshot preserves ghost-tripped breaker");a.await("SOURCE|1");
            var world=(LanWorld)LanSafeAreaBarrierTest.field(host,"world");var breaker=HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.BREAKER);
            HouseRebuildPowerTest.interact(host,map,world,a,breaker);a.await("ACCEPT|INTERACT|");check(authority.supplied(),"Physical hunter breaker reset restores actual shared source");
            check(host.running(),"Electrical haunt leaves the real case active");
        }
        System.out.println("PASS "+combinations+"species/controller accepted electrical arcs; ordinary source/breaker independence,non-electrical/off/stale/rejected negatives;4TCP ghost trip,view-resync and physical reset");
    }
}
