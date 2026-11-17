package org.beckenburg;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Real campaign socket, physical navigation and ordinary powered ticks through a flat camera recovery. */
public final class LanCameraRecoveryIntegrationTest {
    private static void check(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
    private static final class Observer {
        final HouseMap map=new HouseMap();
        final HunterFieldNavigation route=new HunterFieldNavigation(map);
        final CampaignHunterInvestigator hire=new CampaignHunterInvestigator(map,1);
        final List<String> events=new ArrayList<>();
        LanWorld.Body body;
        int tick;
        Observer(){hire.assignSeat(0);}
        synchronized void frame(String frame){
            hire.accept(frame);
            if(frame.startsWith("STATE|"))tick=Math.max(tick,Integer.parseInt(LanProtocol.parts(frame).get(1)));
            else if(frame.startsWith("BODY|0|")){
                var p=LanProtocol.parts(frame);
                double x=Integer.parseInt(p.get(2))/1000d,y=Integer.parseInt(p.get(3))/1000d,z=Integer.parseInt(p.get(4))/1000d;
                body=new LanWorld.Body(x,y,z,Integer.parseInt(p.get(5)),map.roomAt(x,y,z));
            }else if(frame.startsWith("DOORS|"))map.applyDoorSnapshot(LanProtocol.parts(frame).get(1));
            else if(frame.startsWith("CAMERA|")||frame.startsWith("ACCEPT|GEAR")||frame.startsWith("REJECT|")
                    ||frame.startsWith("OWNREADY|")||frame.startsWith("INFO|mission started")||frame.startsWith("ACCEPT|VIEW3D"))
                events.add(frame);
            notifyAll();
        }
        synchronized int mark(){return events.size();}
        synchronized boolean received(int mark,String prefix){
            for(int i=mark;i<events.size();i++)if(events.get(i).startsWith(prefix))return true;
            return false;
        }
        synchronized void await(int mark,String prefix,int seconds)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
            while(!received(mark,prefix)&&System.nanoTime()<end)wait(50);
            check(received(mark,prefix),"Missing "+prefix+" at "+tick+" from "+body+"; recent events "+
                events.subList(Math.max(mark,events.size()-8),events.size()));
        }
        void walk(LanClient client,boolean toVan)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
            int previous=-1;route.reset();
            var rack=map.van().location(VanStaging.Station.EQUIPMENT);
            while(System.nanoTime()<end){
                synchronized(this){
                    if(body!=null&&previous!=tick){
                        previous=tick;
                        if(toVan&&body.room()==HouseMap.EXTRACTION_ROOM&&map.van().stationAt(body.x(),body.z())==VanStaging.Station.EQUIPMENT
                                ||!toVan&&body.room()==0)return;
                        var step=toVan?route.to(tick,body,HouseMap.EXTRACTION_ROOM,rack.x(),map.van().floorY(),rack.z(),.11)
                            :route.to(tick,body,0,map.centerX(0),map.centerZ(0),1.0);
                        step.send(client);
                    }
                }
                Thread.sleep(5);
            }
            throw new AssertionError("Route failed from "+body+" to "+(toVan?"rack":"Foyer"));
        }
        void recover(LanClient client,int start)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(55);
            int previous=-1,requests=0,radio=0,pickedUp=-1;
            while(System.nanoTime()<end){
                synchronized(this){
                    if(previous!=tick){
                        previous=tick;
                        if(pickedUp<0&&received(start,"CAMERA|CLEAR|0")){
                            pickedUp=tick;
                            check(body.room()==0,"Owner did not physically collect the camera in the Foyer");
                        }
                        if(received(start,"ACCEPT|GEARREFILL")){
                            check(pickedUp>=0&&requests==1&&radio<10,"Expected a single acknowledged refill after pickup");
                            check(body.room()==HouseMap.EXTRACTION_ROOM&&map.van().stationAt(body.x(),body.z())==VanStaging.Station.EQUIPMENT,
                                "Refilling away from rack");
                            check(!received(start,"REJECT|retrieve deployed camera"),"Repeated rack request before collection");
                            System.out.println("PASS camera collected at "+pickedUp+", acknowledged refill at "+tick+
                                ", physical service requests "+requests+", radio transmissions "+radio);
                            return;
                        }
                        var input=hire.decide(tick,body,null);
                        if(input.action()==CampaignGuestBot.Action.SERVICE){requests++;check(pickedUp>=0,"Serviced before CLEAR");}
                        if(input.action()==CampaignGuestBot.Action.RADIO)radio++;
                        input.send(client);
                    }
                }
                Thread.sleep(5);
            }
            throw new AssertionError("Autonomous pickup/recharge timed out at "+body+" tick "+tick);
        }
    }
    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("campaign-flat-camera");var file=dir.resolve("profile.properties");
        try{
            Files.writeString(file,"schema=1\ncurrency=6000\nexperience=0\ncampaign.story.seed=58\n");
            var profile=new ProfileStore(file);profile.load();
            if(!profile.catalogEquipment().containsKey("VIDEO_CAMERA"))
                check(profile.purchaseCatalogGear("VIDEO_CAMERA",EquipmentTiers.Tier.I),"Own the camera");
            check(profile.reserveMissionHunters(2),"Hire two campaign partners");
            var chapter=profile.dispatchStory(0);var observer=new Observer();
            try(var host=LanHost.investigation("127.0.0.1",0,chapter.ghost(),CustomContract.standard(3),false,
                    SiteCatalog.mapFor(chapter.map()),null,58,5)){
                host.configureCampaign(profile,chapter);
                try(var owner=new LanClient("127.0.0.1",host.port(),false,"Camera operator","",host.localOwnerCredential(),observer::frame)){
                    owner.equipAt(3,"VIDEO_CAMERA");observer.await(0,"ACCEPT|GEAREQUIP|3|VIDEO_CAMERA",8);
                    owner.ready();observer.await(0,"OWNREADY|YES",8);host.prepareCampaignBots(2);owner.start();
                    observer.await(0,"INFO|mission started",8);owner.view3D();observer.await(0,"ACCEPT|VIEW3D",8);
                    owner.say(LanChat.Channel.RADIO,"team hold");observer.walk(owner,false);
                    owner.selectGearSlot(3);owner.useHeldEquipment();observer.await(0,"CAMERA|PLACE|0|0|",8);
                    observer.walk(owner,true);
                    observer.await(0,"CAMERA|POWER|0|0",45);
                    int far=observer.mark();owner.useHeldEquipment();
                    observer.await(far,"REJECT|retrieve camera beside the device",8);
                    check(!observer.received(far,"CAMERA|CLEAR|0"),"A flat camera must remain beyond reach");
                    int equip=observer.mark();owner.equipAt(3,"PHOTO_CAMERA");
                    observer.await(equip,"ACCEPT|GEAREQUIP|3|PHOTO_CAMERA",8);
                    int recovering=observer.mark();
                    observer.recover(owner,recovering);
                    check(observer.received(recovering,"ACCEPT|GEAREQUIP|3|VIDEO_CAMERA"),"Recovery re-equipped the camera control at the rack");
                    observer.walk(owner,false);
                    int recovered=observer.mark();owner.selectGearSlot(3);owner.useHeldEquipment();
                    observer.await(recovered,"CAMERA|POWER|0|1",8);
                    System.out.println("PASS actual depleted-camera range rejection, investigator pickup, rack recharge and powered redeployment");
                }
            }
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }
}
