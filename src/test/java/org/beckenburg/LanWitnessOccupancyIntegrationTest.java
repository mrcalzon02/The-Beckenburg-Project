package org.beckenburg;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A remote operator watches the same live manifestation while two field seats cross a doorway. */
public final class LanWitnessOccupancyIntegrationTest {
    private static final int ROOM=4;
    private static final String FEED="CAMGHOST", LENS="LENSGHOST", EYES="APPARITION";

    private static void require(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }

    private static final class Player implements AutoCloseable {
        final LanClient client;
        final LinkedBlockingQueue<String> commands=new LinkedBlockingQueue<>();
        final ConcurrentHashMap<String,ConcurrentSkipListMap<Integer,String>> optics=new ConcurrentHashMap<>();
        final java.util.concurrent.CopyOnWriteArrayList<String> findings=new java.util.concurrent.CopyOnWriteArrayList<>();
        Player(LanHost host,boolean ghost,String name,boolean owner)throws Exception{
            client=new LanClient("127.0.0.1",host.port(),ghost,name,"",owner?host.localOwnerCredential():"",this::receive);
        }
        volatile String shutter;
        void receive(String frame){
            String kind=frame.substring(0,frame.indexOf('|')<0?frame.length():frame.indexOf('|'));
            if(frame.startsWith("NOTE|")||frame.startsWith("CASEFILE|")||frame.startsWith("CHAT|RADIO|")
                    ||frame.equals("TASK|DONE|PHOTO_EVENT"))findings.add(frame);
            if(kind.equals(FEED)||kind.equals(LENS)||kind.equals(EYES)){
                var parts=LanProtocol.parts(frame);
                boolean direct=kind.equals(EYES);
                boolean shown=parts.get(direct?1:3).equals("VISIBLE");
                int tick=Integer.parseInt(parts.get(direct?(shown?5:2):(shown?10:4)));
                String stream=kind.equals(FEED)&&!parts.get(1).equals("0")?kind+"|"+parts.get(1):kind;
                optics.computeIfAbsent(stream,k->new ConcurrentSkipListMap<>()).put(tick,frame);
            }else if(kind.equals("SHUTTERGHOST")){
                shutter=frame;
            }else if(kind.equals("ACCEPT")||kind.equals("REJECT")||kind.equals("CAMERA")
                    ||kind.equals("LOBBY")||kind.equals("INFO")){
                commands.add(frame);
            }
        }
        String await(String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<end){
                String frame=commands.poll(50,TimeUnit.MILLISECONDS);
                if(frame==null)continue;
                require(!frame.startsWith("REJECT|"),client.seat()+" "+frame+" awaiting "+prefix);
                if(frame.startsWith(prefix))return frame;
            }
            throw new AssertionError(client.seat()+" missing "+prefix);
        }
        void sample(String kind,int tick,int people,boolean expected)throws Exception{
            var samples=optics.computeIfAbsent(kind,k->new ConcurrentSkipListMap<>());
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(samples.get(tick)==null&&System.nanoTime()<end)Thread.sleep(5);
            String frame=samples.get(tick);
            require(frame!=null,client.seat()+" missing "+kind+" at "+tick+", latest "+samples.lastEntry());
            var parts=LanProtocol.parts(frame);
            boolean direct=kind.equals(EYES);
            require(parts.get(direct?1:3).equals(expected?"VISIBLE":"HIDDEN"),
                "Expected "+expected+" with "+people+" occupants: "+frame);
            if(expected){
                require(parts.size()==(direct?8:13)&&parts.get(parts.size()-2).equals(""+ROOM)
                    &&parts.get(parts.size()-1).equals(""+people),"Wrong sighting context: "+frame);
                if(!direct)require(parts.get(9).equals(GhostManifestationVisual.UNREVEALED),
                    "Ordinary camera figure disclosed an identity: "+frame);
            }else{
                require(parts.size()==(direct?3:5),"Hidden frame carried subject data: "+frame);
            }
        }
        void watch(int camera)throws Exception{
            client.watchCamera(camera);
            await(camera<0?"ACCEPT|WATCH|NONE":"ACCEPT|WATCH|CAMERA|"+camera);
        }
        void expose(int count,boolean visible)throws Exception{
            shutter=null;
            client.useHeldEquipment();
            await("ACCEPT|GEARUSE|PHOTO_CAMERA|");
            require(findings.stream().noneMatch(f->f.equals("TASK|DONE|PHOTO_EVENT")
                ||f.startsWith("CASEFILE|")&&f.contains("|PHOTO_CAMERA|")),
                "The shutter created an automatic casefile entry or photo objective");
            if(!visible){
                require(shutter==null,"A blank exposure received an apparition: "+shutter);
                return;
            }
            require(shutter!=null,"A valid exposure lost the observed subject with "+count+" occupants");
            var p=LanProtocol.parts(shutter);
            require(p.size()==13&&p.get(1).equals("1")&&p.get(3).equals("VISIBLE")
                &&p.get(9).equals(GhostManifestationVisual.UNREVEALED)
                &&p.get(11).equals(""+ROOM)&&p.get(12).equals(""+count),"Wrong captured apparition: "+shutter);
        }
        @Override public void close(){client.close();}
    }

    private static void place(Object lock,HouseMap map,LanWorld world,int seat,LevelDefinition.Vec p){
        synchronized(lock){AuthoredTestSupport.place(map,world,seat,p);}
    }

    private static void cross(Player field,int sign)throws Exception{
        field.client.step(0,sign*100);
        field.await("ACCEPT|STEP");
    }

    private static int occupants(LanHost host){
        return (int)host.devAdminSnapshot().hunters().stream()
            .filter(h->h.connected()&&h.body().room()==ROOM).count();
    }

    private record Capture(int count,int tick,String frame,List<SceneRenderer.RemoteBody> bodies){
        void write(SceneRenderer r,Path folder,GhostIdentity.Type species,HouseMap map,LevelDefinition.Vec lens)throws Exception{
            r.x=lens.x();r.z=lens.z();r.eyeHeight=lens.y()+1.62;r.yaw=0;r.pitch=-.035;
            r.lamp=false;
            r.setFieldOfView(EquipmentOptics.forTier(EquipmentTiers.Tier.I).videoFieldOfView());
            var p=LanProtocol.parts(frame);
            SceneRenderer.Apparition body=null;
            if(p.get(3).equals("VISIBLE"))body=new SceneRenderer.Apparition(
                Integer.parseInt(p.get(4))/1000d,Integer.parseInt(p.get(5))/1000d,Integer.parseInt(p.get(6))/1000d,
                LanGhostAnimation.Phase.LURK,Integer.parseInt(p.get(7)),0,false,p.get(8).equals("GREEN"),p.get(9));
            r.drawNetwork(map,bodies,body,tick*.1);
            Files.createDirectories(folder);
            var file=folder.resolve(species.name().toLowerCase(java.util.Locale.ROOT)+"-"+count+".png");
            javax.imageio.ImageIO.write(r.image(),"png",file.toFile());
        }
    }

    // Align a legal, paid manifestation with five successive monitor refreshes.
    private static int manifest(Player camera,Player ghost,GhostDomain domain,Object lock,boolean needsSound)throws Exception{
        int cast=-1,lastScan=-100;
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(25);
        while(System.nanoTime()<end){
            GhostDomain.Observation o;
            synchronized(lock){o=domain.observe();}
            if(o.tick()%10==5&&o.legalActions().contains(GhostDomain.Action.MANIFEST)){
                ghost.client.ghost(GhostDomain.Action.MANIFEST,ROOM);ghost.await("ACCEPT|GHOST|MANIFEST");
                synchronized(lock){cast=domain.lastUsed(GhostDomain.Action.MANIFEST);}
                break;
            }
            if(needsSound&&o.tick()>=79&&o.tick()-lastScan>=4){
                camera.client.scan(LanCase.Tool.EMF);camera.await("ACCEPT|SCAN|");lastScan=o.tick();
            }
            Thread.sleep(8);
        }
        require(cast>=0,"No legal manifestation in room "+ROOM);
        return cast;
    }

    private static void attentionControl(Player van,Player camera,Player partner,Player ghost,GhostDomain domain,Object lock)throws Exception{
        van.findings.clear();
        int cast=manifest(camera,ghost,domain,lock,false);
        int first=(cast/10+1)*10;
        // An eyes frame on a later tick is a wire-order barrier for the earlier monitor refresh.
        van.sample(EYES,first+5,0,false);camera.sample(EYES,first+5,0,false);
        require(!van.optics.get(FEED).containsKey(first)&&!camera.optics.get(LENS).containsKey(first),
            "The unwatched receiver or stowed viewfinder received an optical sample");
        van.watch(2);
        van.sample(FEED+"|2",first+10,0,false);
        van.sample(EYES,first+15,0,false);
        require(!van.optics.get(FEED).containsKey(first+10),"Viewing camera 3 also disclosed camera 1");
        require(van.findings.isEmpty(),"An unattended camera reported or recorded the ghost: "+van.findings);
        van.watch(0);
        van.sample(FEED,first+20,0,true);
        require(van.findings.isEmpty(),"Simply seeing video wrote a finding: "+van.findings);
        van.client.note(GhostFieldGuide.Clue.REMOTE_EMPTY,FieldJournal.Status.SEEN);
        van.await("ACCEPT|NOTE|REMOTE_EMPTY");
        require(van.findings.stream().anyMatch(f->f.startsWith("NOTE|REMOTE_EMPTY|SEEN|0|")),
            "The human operator's manual notebook mark did not reach the team");
        partner.client.journal();partner.await("ACCEPT|JOURNAL");
        require(partner.findings.stream().anyMatch(f->f.startsWith("NOTE|REMOTE_EMPTY|SEEN|0|")),
            "The human operator's mark was not shared with a field investigator");
        require(!van.findings.contains("TASK|DONE|PHOTO_EVENT"),
            "Manually marking van video credited a still-photo objective");
        camera.client.watchHeldLens();camera.await("ACCEPT|WATCH|LENS");
        System.out.println("PASS two powered cameras: unattended and wrong feed silent, active observer saw figure and manually marked journal");
    }

    private static void checkPrintReport(LanHost host,Object lock,Player operator,Player photographer)throws Exception{
        var reported=(java.util.Set<?>)LooseClutterNetworkTest.field(host,"reportedPrints");
        var empty=GhostFieldGuide.Clue.PHOTO_EMPTY;
        var solo=GhostFieldGuide.Clue.PHOTO_SOLO;
        synchronized(lock){require(reported.isEmpty(),"The shutter credited an unreported print");}
        operator.client.note(empty,FieldJournal.Status.SEEN);operator.await("ACCEPT|NOTE|PHOTO_EMPTY");
        synchronized(lock){require(reported.isEmpty(),"A different seat claimed the photographer's exposure");}
        photographer.client.note(solo,FieldJournal.Status.SEEN);photographer.await("ACCEPT|NOTE|PHOTO_SOLO");
        synchronized(lock){require(reported.isEmpty(),"The wrong headcount claimed the print");}
        photographer.client.note(empty,FieldJournal.Status.SEEN);photographer.await("ACCEPT|NOTE|PHOTO_EMPTY");
        synchronized(lock){require(reported.equals(java.util.Set.of(empty)),"Missing matching photographer report");}
        photographer.client.note(empty,FieldJournal.Status.UNKNOWN);photographer.await("ACCEPT|NOTE|PHOTO_EMPTY");
        synchronized(lock){require(reported.isEmpty(),"Withdrawing the photographer's report retained credit");}
        require(!photographer.findings.contains("TASK|DONE|PHOTO_EVENT"),"The notebook graded a print live");
        System.out.println("PASS print provenance, incorrect observer/count, manual report and withdrawal");
    }

    private static void exercise(GhostIdentity.Type species,int qualifying,Path folder)throws Exception{
        // House seed 2 haunts the room beside this authored doorway. Only wall-clock speed is accelerated.
        try(var host=LanHost.investigation("127.0.0.1",0,species,CustomContract.standard(3),false,HouseMap.Site.HOUSE,null,2,45);
            var van=new Player(host,false,"Van receiver",true);
            var camera=new Player(host,false,"Doorway photographer",false);
            var partner=new Player(host,false,"Second investigator",false);
            var ghost=new Player(host,true,"Haunting actor",false)){
            var humans=List.of(van,camera,partner);
            for(var p:humans)p.client.ready();
            ghost.client.ready();
            van.await("LOBBY|3|HUMAN|4|WAITING");
            van.client.start();van.await("INFO|mission started");
            for(var p:humans){p.client.view3D();p.await("ACCEPT|VIEW3D");}
            ghost.client.viewGhost3D();ghost.await("ACCEPT|GVIEW3D");
            var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
            var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            var domain=(GhostDomain)LooseClutterNetworkTest.field(host,"ghost");
            var lock=LooseClutterNetworkTest.field(host,"mutex");
            var rack=map.van().location(VanStaging.Station.EQUIPMENT);
            var vanPos=new LevelDefinition.Vec(rack.x(),VanStaging.FLOOR_Y,rack.z());
            place(lock,map,world,0,vanPos);place(lock,map,world,1,vanPos);place(lock,map,world,2,vanPos);
            van.client.equipAt(0,"VIDEO_CAMERA");van.await("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
            camera.client.buyEquipment("PHOTO_CAMERA",EquipmentTiers.Tier.II);camera.await("ACCEPT|GEARBUY|PHOTO_CAMERA|II");
            camera.client.equipAt(1,"PHOTO_CAMERA");camera.await("ACCEPT|GEAREQUIP|1|PHOTO_CAMERA");
            if(species==GhostIdentity.Type.SHADE){
                partner.client.equipAt(0,"VIDEO_CAMERA");partner.await("ACCEPT|GEAREQUIP|0|VIDEO_CAMERA");
                place(lock,map,world,2,map.spawn(3));partner.client.turn(180);partner.client.useHeldEquipment();
                partner.await("CAMERA|PLACE|2|3|");partner.await("CAMERA|POWER|2|1");
                partner.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
                place(lock,map,world,2,vanPos);
            }
            partner.client.equipAt(0,"SPECTRAL_PRISM");partner.await("ACCEPT|GEAREQUIP|0|SPECTRAL_PRISM");

            var target=map.spawn(ROOM);
            var mount=new LevelDefinition.Vec(target.x(),target.y(),target.z()-2.55);
            place(lock,map,world,0,mount);van.client.turn(0);van.client.useHeldEquipment();
            van.await("CAMERA|PLACE|0|"+ROOM+"|");van.await("CAMERA|POWER|0|1");
            van.await("ACCEPT|GEARUSE|VIDEO_CAMERA|");
            var relay=map.van().location(VanStaging.Station.RELAY);
            place(lock,map,world,0,new LevelDefinition.Vec(relay.x(),VanStaging.FLOOR_Y,relay.z()));

            van.client.turn(new HunterCameraAttention(map).screenHeading(world.body(0)));
            var door=map.level().portals().stream().filter(p->p.joins(3,ROOM)).findFirst().orElseThrow();
            var approach=AuthoredTestSupport.side(map,door,3,.7);
            if(door.door()&&!map.doorOpen(door.id())){
                place(lock,map,world,1,approach);camera.client.openDoor();camera.await("ACCEPT|DOOR");
            }
            var threshold=AuthoredTestSupport.side(map,door,3,.15);
            place(lock,map,world,1,threshold);
            place(lock,map,world,2,new LevelDefinition.Vec(threshold.x()+.28,threshold.y(),threshold.z()));
            camera.client.turn(0);partner.client.turn(0);
            if(qualifying>0)cross(camera,1);
            if(qualifying==2)cross(partner,1);
            require(occupants(host)==qualifying,"Incorrect starting room occupancy");

            van.watch(0);camera.client.watchHeldLens();camera.await("ACCEPT|WATCH|LENS");
            require(van.findings.stream().noneMatch(f->f.startsWith("CASEFILE|")&&f.contains("|VIDEO_CAMERA|")),
                "Deploying a video camera created an automatic casefile observation");
            int idleTick=(host.devAdminSnapshot().tick()/10+1)*10;
            van.sample(FEED,idleTick,qualifying,false);
            camera.sample(LENS,idleTick,qualifying,false);
            camera.sample(EYES,idleTick,qualifying,false);
            camera.expose(qualifying,false);

            if(species==GhostIdentity.Type.SHADE){
                van.watch(-1);camera.watch(-1);
                attentionControl(van,camera,partner,ghost,domain,lock);
            }
            int cast=manifest(camera,ghost,domain,lock,qualifying>0);
            if(qualifying>0)cross(camera,-1);
            if(qualifying==2)cross(partner,-1);
            int first=(cast/10+1)*10;
            var captures=new java.util.ArrayList<Capture>();
            int[] counts={0,1,2,1,0};
            for(int i=0;i<counts.length;i++){
                if(i==1)cross(camera,1);
                if(i==2)cross(partner,1);
                if(i==3)cross(partner,-1);
                if(i==4)cross(camera,-1);
                int count=counts[i],tick=first+i*10;
                require(occupants(host)==count,"Door crossing did not yield "+count+" investigators");
                boolean allowed=count==qualifying;
                van.sample(FEED,tick,count,allowed);
                camera.sample(LENS,tick,count,allowed);
                camera.sample(EYES,tick,count,allowed&&qualifying>0);
                partner.sample(EYES,tick,count,allowed&&qualifying==2);
                van.sample(EYES,tick,count,false);
                camera.expose(count,allowed);
                if(species==GhostIdentity.Type.SHADE&&i<2){
                    partner.client.useHeldEquipment();
                    String reading=partner.await("ACCEPT|GEARUSE|SPECTRAL_PRISM|");
                    require(reading.contains(allowed?"Silhouette area ":"Prism clear / no silhouette"),
                        "Prism reading did not match the manifested Shade at occupancy "+count+": "+reading);
                }
                if(folder!=null&&i<3){
                    var snapshot=host.devAdminSnapshot();
                    var bodies=new java.util.ArrayList<SceneRenderer.RemoteBody>();
                    for(int seat=0;seat<humans.size();seat++){
                        var b=snapshot.hunters().get(seat).body();
                        bodies.add(new SceneRenderer.RemoteBody(b.x(),b.y(),b.z(),seat,b.heading()));
                    }
                    captures.add(new Capture(count,tick,van.optics.get(FEED).get(tick),bodies));
                }
                synchronized(lock){require(domain.manifested(),"Timed out between samples for "+species);}
            }
            require(!camera.optics.containsKey(FEED)&&!partner.optics.containsKey(FEED)
                &&ghost.optics.isEmpty(),"Remote optics reached an unauthorized seat");
            if(species==GhostIdentity.Type.SHADE)checkPrintReport(host,lock,van,camera);
            if(!captures.isEmpty()){
                var renderer=new SceneRenderer();
                for(var capture:captures)capture.write(renderer,folder,species,map,mount);
            }
            System.out.println("PASS "+species+" video feed, doorway photo lens, direct sight and idle control; "
                +"0 -> 1 -> 2 -> 1 -> 0 investigators during one manifestation at tick "+cast);
        }
    }
    public static void main(String[] args)throws Exception{
        Path output=args.length==0?null:Path.of(args[0]);
        var species=List.of(GhostIdentity.Type.SHADE,GhostIdentity.Type.BANSHEE,GhostIdentity.Type.POLTERGEIST);
        if(args.length>1){
            var selected=GhostIdentity.Type.valueOf(args[1]);
            require(species.contains(selected),"Unsupported capture profile "+selected);
            exercise(selected,species.indexOf(selected),output);
        }else{
            for(int count=0;count<species.size();count++)exercise(species.get(count),count,output);
        }
    }
}
