package org.beckenburg;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real campaign hire uses issued UV, walks back to the van on a radio order and changes instrument. */
public final class CampaignHunterRackScenario {
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static void await(LinkedBlockingQueue<String> frames,String prefix)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<end){
            var f=frames.poll(100,TimeUnit.MILLISECONDS);
            if(f!=null&&f.startsWith(prefix))return;
        }
        throw new AssertionError("Missing "+prefix);
    }
    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("hired-hunter-rack");var file=dir.resolve("profile.properties");
        try{
            Files.writeString(file,"schema=1\ncurrency=5000\nexperience=0\ncampaign.story.seed=58\n");
            var profile=new ProfileStore(file);profile.load();
            for(String id:new String[]{"UV_LIGHT","RESONANCE_TUNER"})
                if(!profile.catalogEquipment().containsKey(id))require(profile.purchaseCatalogGear(id,EquipmentTiers.Tier.I),"Cannot buy "+id);
            var original=profile.catalogLoadout();require(profile.reserveMissionHunters(1),"Reserve hired seat");
            var chapter=profile.dispatchStory(0);var site=SiteCatalog.mapFor(chapter.map());
            var inbox=new LinkedBlockingQueue<String>();
            try(var host=LanHost.investigation("127.0.0.1",0,chapter.ghost(),CustomContract.standard(2),false,site,null,58,12)){
                host.configureCampaign(profile,chapter);
                try(var leader=new LanClient("127.0.0.1",host.port(),false,"Operator","",host.localOwnerCredential(),f->{
                    if(f.startsWith("OWNREADY|")||f.startsWith("INFO|mission started")||f.startsWith("ACCEPT|VIEW3D")
                        ||f.startsWith("CASEFILE|")||f.startsWith("CHAT|RADIO|"))inbox.add(f);
                })){
                    leader.ready();await(inbox,"OWNREADY|YES");host.prepareCampaignBots(1);
                    leader.start();await(inbox,"INFO|mission started");leader.view3D();await(inbox,"ACCEPT|VIEW3D");
                    boolean uv=false,van=false,resonance=false,ack=false;
                    var trail=new java.util.ArrayDeque<String>();
                    int lastTraceTick=-1000;
                    long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(65);
                    int latest=-1;
                    while(System.nanoTime()<end&&!resonance){
                        var state=host.devAdminSnapshot();latest=state.tick();
                        if(uv&&state.hunters().get(1).body().room()==HouseMap.EXTRACTION_ROOM)van=true;
                        if(latest-lastTraceTick>=65){
                            lastTraceTick=latest;
                            var body=state.hunters().get(1).body();
                            var diagnostic=host.devCampaignBotDiagnostics().stream()
                                .filter(d->d.seat().equals("H2")).findFirst().orElse(null);
                            trail.addLast(String.format(java.util.Locale.ROOT,
                                "tick=%d room=%d x=%.3f y=%.3f z=%.3f act=%s at=%d rejects=%d faults=%d uv=%s ack=%s van=%s",
                                latest,body.room(),body.x(),body.y(),body.z(),
                                diagnostic==null?"UNKNOWN":diagnostic.action(),
                                diagnostic==null?-1:diagnostic.actionTick(),
                                diagnostic==null?-1:diagnostic.rejections(),
                                diagnostic==null?-1:diagnostic.updateFailures(),uv,ack,van));
                            if(trail.size()>30)trail.removeFirst();
                        }
                        String f=inbox.poll(15,TimeUnit.MILLISECONDS);if(f==null)continue;
                        if(f.startsWith("CHAT|RADIO|1|")&&f.endsWith("|I'll test resonance tuner."))ack=true;
                        if(!f.startsWith("CASEFILE|")||f.equals("CASEFILE|RESET"))continue;
                        var entry=LanEvidenceJournal.Entry.decode(f);
                        if(entry.seat()!=1||entry.room()>=HouseMap.INDOOR_ROOMS)continue;
                        if(entry.tool().equals("UV_LIGHT")&&!uv){
                            uv=true;System.out.println("H2 UV reading in R"+entry.room()+" at tick "+entry.tick()+": "+entry.reading());
                            leader.say(LanChat.Channel.RADIO,"h2 test resonance tuner");
                        }else if(entry.tool().equals("RESONANCE_TUNER")&&van&&ack){
                            resonance=true;System.out.println("H2 tuner reading in R"+entry.room()+" at tick "+entry.tick()+": "+entry.reading());
                        }
                    }
                    if(!(uv&&van&&ack&&resonance))
                        System.err.println("RACK ROUTE TRACE: "+String.join(" | ",trail));
                    require(uv&&van&&ack&&resonance,"Field UV="+uv+", radio ACK="+ack+", returned="+van+", field tuner="+resonance+" at "+latest);
                    require(Arrays.equals(original,profile.catalogLoadout()),"Hired seat changed human's saved belt");
                }
            }
            System.out.println("PASS hired hunter physically returned to rack and used the requested owned replacement tool");
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(dir);}
    }
}
