package org.beckenburg;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Socket seat with the same inventory, movement, radio and case commands as a human. */
final class CampaignGuestBot implements AutoCloseable {
    enum Action { WAIT, WALK, TURN, DOOR, SELECT, WATCH, SHUTTER, SAMPLE, EQUIP, SERVICE, RADIO, NOTE, IDENTIFY, WARD, CROSS, RITE }
    record Input(Action action,int dx,int dz,int slot,String radio,
                 GhostIdentity.Type identifiedType,GhostIdentity.Rite rite,int heading,String equipment,boolean releaseView) {
        Input(Action action,int dx,int dz,int slot,String radio,GhostIdentity.Type type,GhostIdentity.Rite rite,int heading,String equipment){
            this(action,dx,dz,slot,radio,type,rite,heading,equipment,false);
        }
        Input releasingView(){return new Input(action,dx,dz,slot,radio,identifiedType,rite,heading,equipment,true);}
        Input(Action action,int dx,int dz,int slot,String radio,GhostIdentity.Type type,GhostIdentity.Rite rite){
            this(action,dx,dz,slot,radio,type,rite,-1,null);
        }
        Input facingFrom(int previous){
            int heading=Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(dx,dz))),360);
            if(Math.abs(Math.floorMod(heading-previous+180,360)-180)<20)return this;
            return new Input(action,dx,dz,slot,radio,identifiedType,rite,heading,equipment);
        }
        static Input simple(Action action){return new Input(action,0,0,-1,null,null,null);}
        static Input walk(int dx,int dz){return new Input(Action.WALK,dx,dz,-1,null,null,null);}
        static Input turn(int heading){return new Input(Action.TURN,0,0,-1,null,null,null,heading,null);}
        static Input cross(int heading){return new Input(Action.CROSS,0,0,-1,null,null,null,heading,null);}
        static Input equip(int slot,String equipment){return new Input(Action.EQUIP,0,0,slot,null,null,null,-1,equipment);}
        static Input watch(int target){return new Input(Action.WATCH,0,0,target,null,null,null);}
        static Input select(int slot){return new Input(Action.SELECT,0,0,slot,null,null,null);}
        static Input sample(int slot){return new Input(Action.SAMPLE,0,0,slot,null,null,null);}
        static Input sampleFacing(int slot,int heading){return new Input(Action.SAMPLE,0,0,slot,null,null,null,heading,null);}
        static Input note(GhostFieldGuide.Clue clue){return new Input(Action.NOTE,0,0,-1,null,null,null,-1,clue.name());}
        static Input radio(String words){return new Input(Action.RADIO,0,0,-1,words,null,null);}
        static Input identify(GhostIdentity.Type type){return new Input(Action.IDENTIFY,0,0,-1,null,type,null);}
        static Input rite(GhostIdentity.Rite rite){return new Input(Action.RITE,0,0,-1,null,null,rite);}
        void send(LanClient client)throws IOException{
            if(releaseView)client.watchCamera(-1);
            switch(action){
                case WAIT -> { }
                case WALK -> {if(heading>=0)client.turn(heading);client.step(dx,dz);}
                case TURN -> client.turn(heading);
                case DOOR -> client.openDoor();
                case EQUIP -> client.equipAt(slot,equipment);
                case SERVICE -> client.serviceEquipment();
                case WATCH -> {if(slot==HunterCameraAttention.LENS)client.watchHeldLens();else client.watchCamera(slot);}
                case SHUTTER -> client.useHeldEquipment();
                case SELECT -> client.selectGearSlot(slot);
                case SAMPLE -> {if(heading>=0)client.turn(heading);client.selectGearSlot(slot);client.useHeldEquipment();}
                case NOTE -> client.note(GhostFieldGuide.Clue.valueOf(equipment),FieldJournal.Status.SEEN);
                case RADIO -> client.say(LanChat.Channel.RADIO,radio);
                case IDENTIFY -> client.identify(identifiedType);
                case WARD -> client.ward();
                case CROSS -> {if(heading>=0)client.turn(heading);client.deploy(HunterCounters.Kind.CROSS);}
                case RITE -> {
                    switch(rite){
                        case BANISHMENT -> client.banish();
                        case CLEANSING -> client.cleanse();
                        case ABSOLUTION -> client.absolve();
                    }
                }
            }
        }
    }
    private final java.util.concurrent.CompletableFuture<String> issuedGear=new java.util.concurrent.CompletableFuture<>();
    private final LanClient client;
    private final ScheduledExecutorService clock;
    private final ConcurrentLinkedQueue<String> inbox=new ConcurrentLinkedQueue<>();
    private final HouseMap map;
    private final CampaignHunterInvestigator brain;
    private final LanWorld.Body[] bodies=new LanWorld.Body[6];
    private boolean entered,started,finished;
    private final CampaignBotUpdateGuard updateGuard=new CampaignBotUpdateGuard();
    private volatile LanWorld.Body currentBody;
    private volatile String lastAction="BOOT",lastReject="NONE";
    private volatile int actionTick=-1,rejections,decisions;
    /** Read-only host-local diagnostic. Never part of any client packet. */
    record LocalDiagnostic(String seat,int tick,LanWorld.Body body,String action,
                           int actionTick,int decisions,int rejections,
                           long updateFailures,String failureType){}
    LocalDiagnostic localDiagnostic(){
        return new LocalDiagnostic(client.seat(),tick,currentBody,lastAction,
            actionTick,decisions,rejections,updateGuard.failures(),
            updateGuard.lastFailureType());
    }
    private int tick,health=100;
    CampaignGuestBot(int port,String name,String password,HouseMap.Site site,int number,int tickPeriodMillis)throws IOException{
        map=new HouseMap(site);
        brain=new CampaignHunterInvestigator(map,number);
        client=new LanClient("127.0.0.1",port,false,name,password,"",frame->{
            inbox.add(frame);
            if(frame.startsWith("ACCEPT|GEAREQUIP|")||frame.startsWith("REJECT|"))issuedGear.complete(frame);
        });
        brain.assignSeat(seat());
        clock=Executors.newSingleThreadScheduledExecutor(r->{
            Thread t=new Thread(r,"campaign-hire");t.setDaemon(true);return t;
        });
        clock.scheduleWithFixedDelay(()->updateGuard.run(this::update),
            tickPeriodMillis,tickPeriodMillis,TimeUnit.MILLISECONDS);
    }
    void issueSpecialistTool(ProfileStore profile,int number)throws IOException{
        var loadout=java.util.List.of(profile.catalogLoadout());
        int slot=loadout.indexOf(FieldEquipment.EMPTY);
        if(slot<0)slot=loadout.indexOf("PHOTO_CAMERA");
        if(slot<0)return;
        var owned=profile.catalogEquipment();
        var choices=java.util.List.of("UV_LIGHT","OBJECT_IMPRINT_SCANNER","ECTOPLASM_SAMPLER",
            "BAROMETRIC_PROBE","RESONANCE_TUNER","SPECTRAL_PRISM","GHOST_WRITING_BOOK")
            .stream().filter(tool->owned.containsKey(tool)&&!loadout.contains(tool)).toList();
        if(number>choices.size())return;
        String tool=choices.get(number-1);
        client.equipAt(slot,tool);
        try{
            String actual=issuedGear.get(3,TimeUnit.SECONDS);
            if(!actual.equals("ACCEPT|GEAREQUIP|"+slot+"|"+tool))
                throw new IOException("Hired investigator equipment issue declined: "+actual);
        }catch(InterruptedException interrupted){
            Thread.currentThread().interrupt();throw new IOException(interrupted);
        }catch(java.util.concurrent.ExecutionException|java.util.concurrent.TimeoutException failed){
            throw new IOException("Timed out preparing hired investigator equipment",failed);
        }
    }
    private void update(){
        String frame;
        while((frame=inbox.poll())!=null){
            brain.accept(frame);
            if(frame.startsWith("INFO|mission started"))started=true;
            else if(frame.startsWith("BODY|")){
                var p=LanProtocol.parts(frame);
                int id=Integer.parseInt(p.get(1));
                double x=Integer.parseInt(p.get(2))/1000d,y=Integer.parseInt(p.get(3))/1000d,z=Integer.parseInt(p.get(4))/1000d;
                bodies[id]=new LanWorld.Body(x,y,z,Integer.parseInt(p.get(5)),map.roomAt(x,y,z));
                if(id==seat())currentBody=bodies[id];
                tick=Math.max(tick,Integer.parseInt(p.get(6)));
            }else if(frame.startsWith("STATE|"))tick=Math.max(tick,Integer.parseInt(LanProtocol.parts(frame).get(1)));
            else if(frame.startsWith("REJECT|")){
                rejections++;
                lastReject=frame.substring(0,Math.min(85,frame.length()));
            }
            else if(frame.startsWith("DOORS|"))map.applyDoorSnapshot(LanProtocol.parts(frame).get(1));
            else if(frame.startsWith("PROGRESS|")){
                var p=LanProtocol.parts(frame);health=Integer.parseInt(p.get(1));finished=!p.get(7).equals("ACTIVE");
            }
        }
        if(!started||finished||health<=0)return;
        try{
            if(!entered){client.view3D();entered=true;return;}
            var action=brain.decide(tick,bodies[seat()],bodies[brain.directorSeat()]);
            lastAction=action.action().name();actionTick=tick;decisions++;
            action.send(client);
        }catch(IOException disconnected){
            lastAction="TRANSPORT_ENDED";
            clock.shutdown();
        }
    }
    int seat(){return Integer.parseInt(client.seat().substring(1))-1;}
    @Override public void close(){clock.shutdownNow();client.close();}
}
