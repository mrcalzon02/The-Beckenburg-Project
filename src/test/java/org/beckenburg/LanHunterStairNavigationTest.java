package org.beckenburg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** The real investigator navigator must walk the authored House from a cold start, in both directions. */
public final class LanHunterStairNavigationTest {
    private static final int RELAY=HouseMap.EXTRACTION_ROOM;
    private static final class Route {
        final HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        final HunterFieldNavigation navigation=new HunterFieldNavigation(map);
        final LinkedBlockingQueue<String> frames=new LinkedBlockingQueue<>();
        LanWorld.Body body;
        int tick,doors,lastDecision=-5;

        void receive(String line){
            if(line.startsWith("BODY|0|")||line.startsWith("STATE|")||line.startsWith("DOORS|")
                    ||line.startsWith("LOBBY|")||line.startsWith("INFO|mission started")
                    ||line.startsWith("ACCEPT|DOOR")||line.startsWith("REJECT|"))frames.add(line);
        }
        String next()throws Exception{
            String line=frames.poll(4,TimeUnit.SECONDS);
            if(line==null||line.startsWith("REJECT|"))throw new AssertionError("Route interrupted at "+body+" by "+line);
            var p=LanProtocol.parts(line);
            switch(p.get(0)){
                case "BODY"->{
                    double x=Integer.parseInt(p.get(2))/1000d,y=Integer.parseInt(p.get(3))/1000d,z=Integer.parseInt(p.get(4))/1000d;
                    body=new LanWorld.Body(x,y,z,Integer.parseInt(p.get(5)),map.roomAt(x,y,z));
                }
                case "STATE"->tick=Integer.parseInt(p.get(1));
                case "DOORS"->map.applyDoorSnapshot(p.get(1));
                case "ACCEPT"->doors++;
            }
            return line;
        }
        void waitFor(String prefix)throws Exception{
            while(!next().startsWith(prefix)){}
        }
        void travel(LanClient client,int goal,int limit)throws Exception{
            var relay=map.van().location(VanStaging.Station.RELAY);
            double x=goal==RELAY?relay.x():map.centerX(goal),z=goal==RELAY?relay.z():map.centerZ(goal);
            double y=goal==RELAY?map.van().floorY():map.floorY(goal);
            int start=tick,opened=doors;
            var rooms=new java.util.LinkedHashSet<Integer>();
            navigation.reset();
            while(tick-start<=limit){
                next();
                if(body==null)continue;
                rooms.add(body.room());
                boolean reached=body.room()==goal&&(goal==RELAY
                    ?map.van().stationAt(body.x(),body.z())==VanStaging.Station.RELAY
                    :Math.abs(body.y()-y)<.45&&Math.hypot(body.x()-x,body.z()-z)<=1.15);
                if(reached){
                    System.out.println("Arrived "+map.roomName(goal)+" in "+(tick-start)+" ticks through "+rooms+"; doors opened "+(doors-opened));
                    return;
                }
                if(tick-lastDecision<5)continue;
                lastDecision=tick;
                var command=navigation.to(tick,body,goal,x,y,z,goal==RELAY?.11:1.15);
                command.send(client);
            }
            throw new AssertionError("Could not reach "+map.roomName(goal)+" within "+limit+" ticks; rooms="+rooms+", stopped at "+body);
        }
    }
    static void roundTrip(int room,String expected)throws Exception{
        var route=new Route();
        if(!route.map.roomName(room).equals(expected))throw new AssertionError("Wrong physical room "+room);
        try(var host=LanHost.investigation("127.0.0.1",0,GhostIdentity.Type.BANSHEE,
                    CustomContract.standard(1),false,HouseMap.Site.HOUSE,null,1061,5);
            var hunter=new LanClient("127.0.0.1",host.port(),false,"Route observer","",host.localOwnerCredential(),route::receive);
            var ghost=new LanClient("127.0.0.1",host.port(),true,"Idle ghost",line->{})){
            hunter.ready();ghost.ready();route.waitFor("LOBBY|1|HUMAN|2|WAITING");hunter.start();
            route.waitFor("INFO|mission started");hunter.view3D();
            route.travel(hunter,RELAY,150);
            route.travel(hunter,room,1500);
            if(route.doors==0)throw new AssertionError("Reached "+expected+" without operating its closed doors");
            route.travel(hunter,RELAY,1500);
        }
    }
    private static void approachFromBelow(){
        HouseMap map=new HouseMap(HouseMap.Site.HOUSE);
        LanWorld world=new LanWorld(map);
        var nav=new HunterFieldNavigation(map);
        AuthoredTestSupport.place(map,world,0,new LevelDefinition.Vec(22.497,0,16.025));
        for(int tick=5;tick<500;tick+=5){
            var body=world.body(0);
            if(body.y()>=1.5)return;
            var action=nav.to(tick,body,6,map.centerX(6),map.centerZ(6),1.15);
            if(action.action()==CampaignGuestBot.Action.WALK)world.move(0,action.dx(),action.dz(),tick);
        }
        throw new AssertionError("Hunter kept walking under the upper flight: "+world.body(0));
    }
    public static void main(String[] args)throws Exception{
        approachFromBelow();
        roundTrip(6,"Master bedroom");
        roundTrip(9,"Cellar stair landing");
        roundTrip(0,"Foyer");
    }
}
