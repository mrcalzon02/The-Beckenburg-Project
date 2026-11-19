package org.beckenburg;

import java.nio.file.Files;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A van operator directs three real hired hunters through each authored entry-room camera experiment. */
public final class CampaignWitnessWatchScenario {
    private static void require(boolean condition,String why){if(!condition)throw new AssertionError(why);}

    private static final class CrewLog {
        final LinkedBlockingQueue<String> events=new LinkedBlockingQueue<>();
        final boolean[] powered=new boolean[6];
        final int[] cameraRooms={-1,-1,-1,-1,-1,-1};

        void receive(String packet){
            if(packet.startsWith("CAMERA|")||packet.startsWith("CHAT|RADIO|")
                    ||packet.startsWith("OWNREADY|")||packet.startsWith("INFO|mission started")
                    ||packet.startsWith("ACCEPT|VIEW3D"))events.add(packet);
        }
        String next(long end)throws Exception{
            String frame=events.poll(Math.max(1,end-System.nanoTime()),TimeUnit.NANOSECONDS);
            if(frame!=null&&frame.startsWith("CAMERA|")){
                var parts=LanProtocol.parts(frame);
                if(parts.get(1).equals("RESET")){
                    java.util.Arrays.fill(cameraRooms,-1);java.util.Arrays.fill(powered,false);return frame;
                }
                int seat=Integer.parseInt(parts.get(2));
                switch(parts.get(1)){
                    case "PLACE" -> cameraRooms[seat]=Integer.parseInt(parts.get(3));
                    case "POWER" -> powered[seat]=parts.get(3).equals("1");
                    case "CLEAR" -> {cameraRooms[seat]=-1;powered[seat]=false;}
                }
            }
            return frame;
        }
        void await(String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<end){
                var frame=next(end);
                if(frame!=null&&frame.startsWith(prefix))return;
            }
            throw new AssertionError("Missing "+prefix);
        }
        void watch(LanHost host,HouseMap map,LanClient leader,String mode,int people)throws Exception{
            String entryRoom=map.roomName(0);
            leader.say(LanChat.Channel.RADIO,"team watch "+mode+" "+entryRoom);
            var traffic=new java.util.ArrayDeque<String>();
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(50);
            while(System.nanoTime()<end){
                String frame=next(end);
                if(frame==null||!frame.startsWith("CHAT|RADIO|"))continue;
                var entry=LanChat.Entry.decode(frame);
                traffic.addLast(entry.message());
                if(traffic.size()>12)traffic.removeFirst();
                if(!entry.message().equals("Watching "+entryRoom+" "+mode+" on van video."))continue;
                var state=host.devAdminSnapshot();
                int observer=entry.seat();
                require(observer>0,"Human sent the AI observer report");
                var body=state.hunters().get(observer).body();
                require(body.room()==HouseMap.EXTRACTION_ROOM&&map.van().stationAt(body.x(),body.z())==VanStaging.Station.RELAY,
                    "Hired observer reported away from the van receiver: "+body);
                var screen=VanInteriorLayout.FIXTURES.stream().filter(f->f.purpose().equals("camera feed")).findFirst().orElseThrow();
                var monitor=map.van().world(screen.localX(),screen.localZ());
                double bearing=Math.atan2(monitor.x()-body.x(),monitor.z()-body.z());
                require(Math.cos(bearing-Math.toRadians(body.heading()))>=Math.cos(Math.toRadians(15)),
                    "Hired observer reported while facing away from the camera screen");
                require(state.hunters().get(0).body().room()==HouseMap.EXTRACTION_ROOM,"Human left the van");
                require(state.hunters().stream().filter(h->h.connected()&&h.body().room()==0).count()==people,
                    "Physical positions contradict the watch report: "+state.hunters());
                boolean live=false;
                for(int seat=1;seat<cameraRooms.length;seat++)if(cameraRooms[seat]==0&&powered[seat])live=true;
                require(live,"Readiness report without a hired player's deployed, powered "+entryRoom+" camera");
                System.out.println("PASS "+mode+": H"+(observer+1)+" at van relay, "+people
                    +" investigators in "+entryRoom+", powered hired camera, tick "+state.tick());
                return;
            }
            throw new AssertionError(mode+" watch missing, recent radio "+traffic+", bodies "+host.devAdminSnapshot().hunters());
        }
    }

    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("campaign-witness-watch");
        var file=dir.resolve("profile.properties");
        try{
            Files.writeString(file,"schema=1\ncurrency=5000\nexperience=0\ncampaign.story.seed=58\n");
            var profile=new ProfileStore(file);profile.load();
            if(!profile.catalogEquipment().containsKey("VIDEO_CAMERA"))
                require(profile.purchaseCatalogGear("VIDEO_CAMERA",EquipmentTiers.Tier.I),"Purchase a working camera");
            require(profile.reserveMissionHunters(3),"Reserve three available hunters");
            var chapter=profile.dispatchStory(0);
            var log=new CrewLog();
            try(var host=LanHost.investigation("127.0.0.1",0,chapter.ghost(),CustomContract.standard(4),false,
                    SiteCatalog.mapFor(chapter.map()),null,58,15)){
                host.configureCampaign(profile,chapter);
                var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
                require(!map.outside(0)&&map.level().portalsFrom(0).stream()
                        .anyMatch(portal->portal.joins(0,13)&&portal.door()),
                    "Camera experiment needs a real authored indoor entrance room with a physical front door");
                require(map.roomAt(map.spawn(0).x(),map.spawn(0).y(),map.spawn(0).z())==0,
                    "Watch target must correspond to a real walkable interior room");
                try(var leader=new LanClient("127.0.0.1",host.port(),false,"Incident leader","",
                        host.localOwnerCredential(),log::receive)){
                    leader.ready();log.await("OWNREADY|YES");host.prepareCampaignBots(3);
                    leader.start();log.await("INFO|mission started");leader.view3D();log.await("ACCEPT|VIEW3D");
                    log.watch(host,map,leader,"empty",0);
                    log.watch(host,map,leader,"solo",1);
                    log.watch(host,map,leader,"group",2);
                    log.watch(host,map,leader,"empty",0);
                }
            }
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }
}
