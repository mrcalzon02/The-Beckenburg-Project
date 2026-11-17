package org.beckenburg;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A van operator directs two real hired hunter sockets against the ordinary host ghost.
 * The inspector observes the match. No player receives an inspector snapshot. */
public final class CampaignAiInvestigationScenario {
    private static final String LOOPBACK="127.0.0.1";
    private final LinkedBlockingQueue<String> messages=new LinkedBlockingQueue<>();
    private final Set<String> radios=new LinkedHashSet<>();
    private final Set<Integer> rooms=new LinkedHashSet<>();
    private final Set<String> tools=new LinkedHashSet<>();
    private final Set<GhostAbilityBar.Slot> abilities=EnumSet.noneOf(GhostAbilityBar.Slot.class);
    private final Set<String> samples=new HashSet<>();
    private final int[] lastRooms={-1,-1,-1};
    private int lastGhostRoom=-1,returns,home;
    private boolean holdWorked,calledBack,resumed,warded;
    private boolean autonomousOwner,stocked;
    private int firstInterior=-1,firstEvidence=-1,firstWard=-1,firstId=-1,firstRite=-1;
    private final java.util.concurrent.ConcurrentLinkedQueue<String> ownerStartup=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private DevMatchClientAi ownerDriver;

    public static void main(String[] args)throws Exception {
        boolean pace=args.length>0&&(args[0].equals("--pace")||args[0].equals("--starter"));
        int offset=pace?1:0;
        long storySeed=args.length>offset?Long.parseLong(args[offset]):21;
        int seconds=args.length>offset+1?Integer.parseInt(args[offset+1]):pace?2100:120;
        int period=pace&&args.length>3?Integer.parseInt(args[3]):pace?10:100;
        if(storySeed==0||seconds<15||seconds>5400||period<5||period>100)
            throw new IllegalArgumentException("Nonzero seed, 15..5400 game seconds, wall interval 5..100ms");
        Path out=Path.of("build",pace?(args[0].equals("--starter")?"campaign-ai-starter":"campaign-ai-pace"):"campaign-ai",Long.toString(storySeed));
        Files.createDirectories(out);
        var scenario=new CampaignAiInvestigationScenario();
        scenario.autonomousOwner=pace;
        scenario.stocked=pace&&args[0].equals("--pace");
        scenario.run(storySeed,seconds,period,out);
    }
    private static void require(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    private synchronized void startDriver(LanClient owner,HouseMap.Site site,int period){
        ownerDriver=new DevMatchClientAi(owner,site,new java.util.concurrent.ConcurrentHashMap<>(),1);
        String frame;while((frame=ownerStartup.poll())!=null)ownerDriver.onFrame(frame);
        ownerDriver.begin(period);
    }
    private synchronized void receive(String frame){
        if(autonomousOwner){
            if(ownerDriver==null)ownerStartup.offer(frame);else ownerDriver.onFrame(frame);
        }
        if(frame.startsWith("OWNREADY|")||frame.startsWith("INFO|mission started")||frame.startsWith("ACCEPT|VIEW3D")
                ||frame.startsWith("CHAT|RADIO|")||frame.startsWith("PROGRESS|")||frame.startsWith("IDENTIFIED|")
                ||frame.startsWith("RITUAL|")||frame.startsWith("CASEFILE|")||frame.startsWith("REJECT|"))
            messages.offer(frame);
    }
    private void await(String prefix)throws Exception{
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<until){
            String frame=messages.poll(100,TimeUnit.MILLISECONDS);
            if(frame==null)continue;
            if(frame.startsWith(prefix))return;
            require(!frame.startsWith("REJECT|"),"Setup rejected: "+frame);
        }
        throw new IOException("No "+prefix+" response");
    }
    private void inspect(DevAdminSnapshot state,PrintWriter log){
        require(state.connectedHunters()==3&&state.ghost().hostAiFallback(),"Unexpected seat change at tick "+state.tick()+": "+state.hunters());
        int tick=state.tick(),ghostRoom=state.ghost().body().room();
        if(ghostRoom!=lastGhostRoom){
            log.printf("%d ghost R%d%n",tick,ghostRoom);
            if(ghostRoom==home&&lastGhostRoom>=0)returns++;
            lastGhostRoom=ghostRoom;
        }
        for(var entry:state.ghost().abilities().slots())
            if(entry.used()&&abilities.add(entry.slot()))log.printf("%d ghost used %s%n",tick,entry.slot());
        for(int i=0;i<3;i++){
            int r=state.hunters().get(i).body().room();
            if(i==0&&!autonomousOwner)require(r==HouseMap.EXTRACTION_ROOM,"Van operator left staging");
            if(r!=lastRooms[i]){
                log.printf("%d H%d R%d%n",tick,i+1,r);
                if((i>0||autonomousOwner)&&r>=0&&r<HouseMap.INDOOR_ROOMS){
                    rooms.add(r);if(firstInterior<0)firstInterior=tick;
                }
                lastRooms[i]=r;
            }
        }
        String frame;
        while((frame=messages.poll())!=null){
            if(frame.startsWith("CHAT|")){
                var msg=LanChat.Entry.decode(frame);
                if(msg.seat()>0){radios.add(frame);if(msg.seat()==1&&msg.message().equals("Holding position."))calledBack=true;
                    if(msg.seat()==1&&msg.message().equals("Resuming room sweep."))resumed=true;}
            }else if(frame.startsWith("PROGRESS|")){
                warded|=LanProtocol.parts(frame).get(5).equals("1");
                if(warded&&firstWard<0)firstWard=tick;
                continue;
            }else if(frame.startsWith("CASEFILE|")){
                if(frame.equals("CASEFILE|RESET"))continue;
                var sample=LanEvidenceJournal.Entry.decode(frame);
                if((!autonomousOwner&&sample.seat()==0)||!samples.add(frame))continue;
                if(firstEvidence<0)firstEvidence=tick;
                tools.add(sample.tool());
            }
            if(frame.startsWith("IDENTIFIED|")&&firstId<0)firstId=tick;
            if(frame.startsWith("RITUAL|STARTED")&&firstRite<0)firstRite=tick;
            log.printf("%d %s%n",tick,frame);
        }
        log.flush();
    }
    private void run(long seed,int seconds,int period,Path output)throws Exception{
        Path profileFile=Files.createTempDirectory("campaign-ai").resolve("profile.properties");
        try{
            Files.writeString(profileFile,"schema=1\ncurrency="+(stocked?25000:2000)+"\nexperience=0\ncampaign.story.seed="+seed+"\n");
            var profile=new ProfileStore(profileFile);profile.load();
            if(stocked){
                for(String tool:new String[]{"UV_LIGHT","GHOST_WRITING_BOOK","ECTOPLASM_SAMPLER","RESONANCE_TUNER",
                        "BAROMETRIC_PROBE","SEISMIC_RECORDER","SPECTRAL_PRISM","LASER_GRID","OBJECT_IMPRINT_SCANNER",
                        "VIDEO_CAMERA","DOTS_PROJECTOR"}){
                    require(profile.purchaseCatalogGear(tool,EquipmentTiers.Tier.I),"Unable to stock "+tool);
                }
            }
            require(profile.reserveMissionHunters(2),"Hire reservation failed");
            var chapter=profile.dispatchStory(0);
            var site=SiteCatalog.mapFor(chapter.map());
            try(var host=LanHost.investigation(LOOPBACK,0,chapter.ghost(),CustomContract.standard(3),false,site,null,
                        CampaignStory.missionSeed(seed,chapter.ordinal()),period);
                var log=new PrintWriter(Files.newBufferedWriter(output.resolve("events.txt")));
                var energy=new PrintWriter(Files.newBufferedWriter(output.resolve("ghost-energy.csv")))){
                host.configureCampaign(profile,chapter);
                energy.println("tick,balance,gained,spent,ordinary,frenzy,repelled,home_gain,frenzy_starts,claws,dreads,drift,whisper,disturb,manifest,strike,signature");
                home=host.devAdminSnapshot().ghost().body().room();
                try(var owner=new LanClient(LOOPBACK,host.port(),false,"Van operator","",host.localOwnerCredential(),this::receive)){
                    owner.ready();await("OWNREADY|YES");host.prepareCampaignBots(2);
                    owner.start();await("INFO|mission started");owner.view3D();await("ACCEPT|VIEW3D");
                    if(autonomousOwner)startDriver(owner,site,period);
                    int start=host.devAdminSnapshot().tick();
                    boolean sentHold=false,sentSearch=false;
                    LanWorld.Body holdPosition=null;
                    long lastAdvance=System.nanoTime();
                    int lastTick=start,lastEnergy=-600;
                    DevAdminSnapshot state;
                    do{
                        state=host.devAdminSnapshot();
                        int elapsed=state.tick()-start;
                        if(state.tick()!=lastTick){lastTick=state.tick();lastAdvance=System.nanoTime();}
                        require(System.nanoTime()-lastAdvance<TimeUnit.SECONDS.toNanos(12),"Host stopped advancing at "+lastTick);
                        inspect(state,log);
                        if(state.tick()-lastEnergy>=600||state.outcome()!=LanCase.Outcome.ACTIVE){
                            var budget=host.devGhostBudget();
                            require(budget.energy()==100+budget.gained()-budget.spent(),"Ghost energy ledger mismatch");
                            energy.printf("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",state.tick(),budget.energy(),
                                budget.gained(),budget.spent(),budget.ordinarySpent(),budget.frenzySpent(),budget.repelled(),
                                budget.homeGained(),budget.frenzyStarts(),budget.claws(),budget.dreads());
                            for(var action:GhostDomain.Action.values())energy.print(","+budget.actions().getOrDefault(action,0));
                            energy.println();energy.flush();lastEnergy=state.tick();
                        }
                        if(!autonomousOwner&&!sentHold&&elapsed>=5){
                            owner.say(LanChat.Channel.RADIO,"h2 hold");sentHold=true;
                        }
                        if(sentHold&&elapsed>=20&&holdPosition==null)holdPosition=state.hunters().get(1).body();
                        if(!sentSearch&&elapsed>=37&&holdPosition!=null){
                            var h=state.hunters().get(1).body();
                            holdWorked=calledBack&&Math.hypot(h.x()-holdPosition.x(),h.z()-holdPosition.z())<.01;
                            owner.say(LanChat.Channel.RADIO,"h2 search");sentSearch=true;
                        }
                        if(state.outcome()!=LanCase.Outcome.ACTIVE)break;
                        Thread.sleep(autonomousOwner?period*3L:period);
                    }while(state.tick()-start<seconds*10);
                    int damage=state.combatEvents().stream().mapToInt(DevAdminSnapshot.CombatEvent::damage).sum();
                    String summary=String.format("campaign seed %d, %s, %s, anchor R%d, tick %d%n"+
                        "hold acknowledged and stationary %s, search ordered %s, hire radio messages %d%n"+
                        "interior rooms entered %s, instrument observations %d, instruments %s%n"+
                        "ghost returns observed %d, abilities observed %s, host-recorded HP damage %d%n"+
                        "identified %s, anchor ward prepared %s, outcome %s%n",
                        seed,site,chapter.ghost(),home,state.tick(),holdWorked,sentSearch,radios.size(),rooms,samples.size(),tools,
                        returns,abilities,damage,state.hunterIdentification(),warded,state.outcome());
                    var exchanges=host.devRackExchanges();
                    try(var rack=new PrintWriter(Files.newBufferedWriter(output.resolve("rack.csv")))){
                        rack.println("tick,hunter,slot,tool");
                        for(var event:exchanges)rack.printf("%d,%d,%d,%s%n",event.tick(),event.seat()+1,event.slot()+1,event.tool());
                    }
                    summary+="mid-case equipment changes at the van rack "+exchanges.size()+"\n";
                    summary+="rack "+(stocked?"stocked with eleven purchased tier-I specialist tools":"standard starting equipment")+"\n";
                    summary+=String.format("wall interval %d ms, seats %s, elapsed %.2f game minutes%n"+
                        "first ticks: interior %d, reading %d, anchor ward %d, identification %d, rite %d%n",
                        period,autonomousOwner?"three autonomous":"van operator plus two hires",(state.tick()-start)/600.0,
                        firstInterior,firstEvidence,firstWard,firstId,firstRite);
                    Files.writeString(output.resolve("result.csv"),
                        "seed,species,site,anchor,field_crew,tick_ms,elapsed_ticks,observed_outcome,interior_tick,reading_tick,ward_tick,identify_tick,rite_tick,returns,damage\n"+
                        String.format("%d,%s,%s,%d,%s,%d,%d,%s,%d,%d,%d,%d,%d,%d,%d%n",seed,chapter.ghost(),site,home,
                            autonomousOwner,period,state.tick()-start,state.outcome(),firstInterior,firstEvidence,firstWard,firstId,firstRite,returns,damage));
                    System.out.print(summary);
                    Files.writeString(output.resolve("summary.txt"),summary);
                    if(!autonomousOwner)require(holdWorked&&sentSearch&&resumed,"H2 did not hold and resume on radio orders");
                    if(seconds>=90){
                        require(!rooms.isEmpty(),"Hires never entered the building");
                        require(!tools.isEmpty(),"Hires never delivered an instrument reading");
                        require(returns>0,"Ghost never returned home after roaming");
                    }
                }
            }
        }finally{
            if(ownerDriver!=null)ownerDriver.close();
            Files.deleteIfExists(profileFile);Files.deleteIfExists(profileFile.getParent());
        }
    }
}
