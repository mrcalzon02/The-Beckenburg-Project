package org.beckenburg;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** An ordinary single-hire instrument order, with no coordinated room-count experiment. */
public final class CampaignDirectedCameraScenario {
    private static final int BEDROOM=6, QUIET_TICKS=45;
    private static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}

    private static final class Traffic {
        final LinkedBlockingQueue<String> events=new LinkedBlockingQueue<>();
        int installations,room=-1,placed=-1,accepted;
        boolean powered,holding;
        void receive(String f){
            if(f.startsWith("OWNREADY|")||f.startsWith("INFO|mission started")||f.startsWith("ACCEPT|VIEW3D")
                    ||f.startsWith("CAMERA|")||f.startsWith("CHAT|RADIO|1|")||f.startsWith("NOTE|")
                    ||f.startsWith("CASEFILE|")&&f.contains("|VIDEO_CAMERA|"))events.add(f);
        }
        void await(String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<end){
                String line=events.poll(50,TimeUnit.MILLISECONDS);
                if(line!=null&&line.startsWith(prefix))return;
            }
            throw new AssertionError("Missing "+prefix);
        }
        void update(int tick,boolean quiet){
            String line;
            while((line=events.poll())!=null){
                if(line.startsWith("CAMERA|PLACE|1|")){
                    room=Integer.parseInt(LanProtocol.parts(line).get(3));installations++;placed=tick;
                }else if(line.startsWith("CAMERA|POWER|1|"))powered=line.endsWith("|1");
                else if(line.equals("CAMERA|CLEAR|1"))powered=false;
                else if(line.startsWith("CHAT|RADIO|1|")){
                    String message=LanChat.Entry.decode(line).message();
                    if(message.equals("I'll test video camera."))accepted++;
                    if(message.equals("Holding position."))holding=true;
                    check(!message.startsWith("Joining watch ")&&!message.startsWith("Suggest watch "),
                        "A team formation took over the ordinary instrument order: "+message);
                    if(quiet)check(!message.startsWith("Video figure")&&!message.startsWith("Orb on my live"),
                        "Idle feed produced a radio sighting: "+message);
                }else if(line.startsWith("CASEFILE|"))throw new AssertionError("Camera wrote an automatic casefile entry");
                else if(quiet)check(!line.startsWith("NOTE|REMOTE_"),"Idle camera produced a figure note: "+line);
            }
        }
    }

    private static void run(boolean upstairs)throws Exception{
        var dir=Files.createTempDirectory("directed-campaign-camera");
        var file=dir.resolve("profile.properties");
        try{
            Files.writeString(file,"schema=1\ncurrency=5000\nexperience=0\ncampaign.story.seed=58\n");
            var profile=new ProfileStore(file);profile.load();
            check(profile.purchaseCatalogGear("VIDEO_CAMERA",EquipmentTiers.Tier.I),"Purchase one working video camera");
            if(upstairs)profile.equipCatalog(3,"VIDEO_CAMERA");
            check(profile.reserveMissionHunters(1),"Reserve one campaign hire");
            var chapter=profile.dispatchStory(0);
            try(var host=LanHost.investigation("127.0.0.1",0,chapter.ghost(),CustomContract.standard(2),false,
                    SiteCatalog.mapFor(chapter.map()),null,58,12)){
                host.configureCampaign(profile,chapter);
                var traffic=new Traffic();
                try(var leader=new LanClient("127.0.0.1",host.port(),false,"Incident leader","",
                        host.localOwnerCredential(),traffic::receive)){
                    leader.ready();traffic.await("OWNREADY|YES");host.prepareCampaignBots(1);
                    leader.start();traffic.await("INFO|mission started");leader.view3D();traffic.await("ACCEPT|VIEW3D");
                    exercise(host,leader,traffic,upstairs);
                }
            }
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }

    private static void exercise(LanHost host,LanClient leader,Traffic traffic,boolean upstairs)throws Exception{
        var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
        var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
        var lock=LooseClutterNetworkTest.field(host,"mutex");
        var hire=((List<?>)LooseClutterNetworkTest.field(host,"campaignBots")).get(0);
        var brain=(CampaignHunterInvestigator)LooseClutterNetworkTest.field(hire,"brain");
        var attention=(HunterCameraAttention)LooseClutterNetworkTest.field(brain,"attention");
        int ordered=-1,observing=-1,quietSince=-1,held=-1,reordered=-1;
        boolean staged=false;
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(85);
        while(System.nanoTime()<end){
            var state=host.devAdminSnapshot();
            int tick=state.tick();
            var body=state.hunters().get(1).body();
            check(state.running()&&state.hunters().get(1).alive(),"Campaign ended before the assignment finished");
            if(upstairs&&!staged&&tick>=8){
                // Stage only the starting position. H2 must choose, place and attend its own equipment.
                synchronized(lock){AuthoredTestSupport.place(map,world,1,map.spawn(BEDROOM));}
                staged=true;
                leader.say(LanChat.Channel.RADIO,"H2 search Master bedroom");
            }
            if(ordered<0&&tick>=(upstairs?24:8)&&(!upstairs||body.room()==BEDROOM)){
                leader.say(LanChat.Channel.RADIO,"H2 test video camera");ordered=tick;
            }
            boolean watching;
            // This receipt is set only by the real private ACCEPT|WATCH response on H2's connection.
            synchronized(brain){watching=attention.remote(1,body);}
            boolean idle=watching&&(!state.ghost().manifested()||state.ghost().body().room()!=traffic.room);
            traffic.update(tick,idle&&quietSince>=0&&tick-quietSince>=15);
            if(watching&&traffic.powered&&traffic.accepted>0&&observing<0){
                check(traffic.installations==1&&traffic.placed>=ordered&&ordered>=0,"Invalid camera or placement before order");
                if(upstairs)check(traffic.room==BEDROOM,"Expected upstairs installation, received "+map.roomName(traffic.room));
                else check(host.devRackExchanges().stream().anyMatch(e->e.seat()==1&&e.tool().equals("VIDEO_CAMERA")),
                    "The regular hire did not fetch the requested tool at the rack");
                observing=tick;
                System.out.println("H2 placed a powered camera in "+map.roomName(traffic.room)+" at "+traffic.placed
                    +", reached the relay facing its screen and acknowledged its own feed at "+tick);
            }
            if(observing>=0&&held<0){
                if(idle){if(quietSince<0)quietSince=tick;}else quietSince=-1;
                if(quietSince>=0&&tick-quietSince>=QUIET_TICKS){
                    if(!upstairs){System.out.println("PASS ordinary rack order and "+QUIET_TICKS+" quiet viewing ticks");return;}
                    leader.say(LanChat.Channel.RADIO,"H2 hold");held=tick;
                }
            }else if(held>=0&&reordered<0&&traffic.holding&&!watching&&tick-held>=7){
                leader.say(LanChat.Channel.RADIO,"H2 test video camera");reordered=tick;
            }else if(reordered>=0&&watching&&traffic.accepted==2){
                check(traffic.installations==1&&traffic.powered,"The re-test replaced a live camera");
                System.out.println("PASS upstairs return, "+QUIET_TICKS+" quiet ticks, canceled watch, existing powered feed reselected at "+tick);
                return;
            }
            if(tick>=4800)break;
            Thread.sleep(12);
        }
        throw new AssertionError("Video assignment timed out: ordered="+ordered+", installed="+traffic.placed
            +", room="+traffic.room+", observed="+observing+", held="+held+", reordered="+reordered);
    }

    public static void main(String[] args)throws Exception{
        if(args.length==0){run(false);run(true);}
        else if(args.length==1&&args[0].equals("--upstairs"))run(true);
        else throw new IllegalArgumentException("Use no arguments or --upstairs");
    }
}
