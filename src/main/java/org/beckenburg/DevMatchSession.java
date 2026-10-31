package org.beckenburg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Standalone, developer-only Phase-0 full-lobby smoke harness. Seven *real*
 * LanClient TCP peers take ordinary seats. ADMIN is a host-local snapshot
 * reader, never an eighth seat, network packet or hunter/ghost world body.
 *
 * Deliberately NOT a full autonomous-investigation AI or interactive UI yet.
 * Call: java -cp build/beckenburg-game.jar org.beckenburg.DevMatchSession
 *           --dev-ai-match --seconds=6
 */
public final class DevMatchSession {
    private static final Path OUTPUT=Path.of("build","dev-match");
    private static final String HOST="127.0.0.1";
    private final List<String> events=new ArrayList<>();
    private final List<String> snapshots=new ArrayList<>();
    private DevMatchSession(){}

    private static final class Peer implements AutoCloseable {
        final String label;
        final LanClient client;
        final CopyOnWriteArrayList<String> frames=new CopyOnWriteArrayList<>();
        private volatile DevMatchClientAi driver;
        Peer(int port,String name,boolean ghost,String ownerCredential)throws IOException {
            label=name;
            client=new LanClient(HOST,port,ghost,name,"",ownerCredential,frame->{
                DevMatchClientAi bot=driver;
                if(bot==null)frames.add(frame);else bot.onFrame(frame);
            });
        }
        void attach(DevMatchClientAi bot){
            driver=bot;
            for(String frame:frames)bot.onFrame(frame);
            frames.clear();
        }
        int mark(){return frames.size();}
        String waitFor(int start,Predicate<String> filter,String description,long timeoutMillis)throws Exception {
            long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            int cursor=start;
            while(System.nanoTime()<end){
                while(cursor<frames.size()){
                    String frame=frames.get(cursor++);
                    if(filter.test(frame))return frame;
                }
                Thread.sleep(15);
            }
            int tail=Math.max(0,frames.size()-8);
            throw new AssertionError(label+" ("+client.seat()+") missing "+description
                    +"; recent frames="+frames.subList(tail,frames.size()));
        }
        String waitPrefix(int start,String prefix)throws Exception {
            return waitFor(start,f->f.startsWith(prefix),prefix,6000);
        }
        @Override public void close(){client.close();}
    }

    private void trace(String actor,String command,String response,int tick){
        events.add("{\"time\":"+json(Instant.now().toString())+
            ",\"tick\":"+tick+",\"actor\":"+json(actor)+
            ",\"command\":"+json(command)+",\"response\":"+json(response)+"}");
    }
    private static String json(String str){
        return "\""+str.replace("\\","\\\\").replace("\"","\\\"")
            .replace("\n","\\n").replace("\r","\\r")+"\"";
    }
    private static void require(boolean valid,String message){
        if(!valid)throw new AssertionError(message);
    }

    private void capture(DevAdminSnapshot snapshot){
        StringBuilder b=new StringBuilder("{\"tick\":"+snapshot.tick()+
            ",\"running\":"+snapshot.running()+",\"connectedPlayers\":"+snapshot.connectedPlayers()+
            ",\"trueGhost\":"+json(snapshot.trueGhostType().name())+
            ",\"identified\":"+json(String.valueOf(snapshot.hunterIdentification()))+
            ",\"outcome\":"+json(snapshot.outcome().name())+
            ",\"ghostClient\":"+snapshot.ghost().clientConnected()+
            ",\"hostAiFallback\":"+snapshot.ghost().hostAiFallback()+
            ",\"ghostRoom\":"+snapshot.ghost().body().room()+
            ",\"ghostX\":"+snapshot.ghost().body().x()+
            ",\"ghostY\":"+snapshot.ghost().body().y()+
            ",\"ghostZ\":"+snapshot.ghost().body().z()+
            ",\"observedEvidenceCount\":"+snapshot.observedCasefile().size()+
            ",\"hunters\":[");
        for(int i=0;i<snapshot.hunters().size();i++){
            var h=snapshot.hunters().get(i);
            if(i!=0)b.append(',');
            b.append("{\"seat\":").append(json(h.seat()))
                .append(",\"connected\":").append(h.connected())
                .append(",\"ready\":").append(h.ready())
                .append(",\"room\":").append(h.body().room())
                .append(",\"x\":").append(h.body().x())
                .append(",\"y\":").append(h.body().y())
                .append(",\"z\":").append(h.body().z())
                .append(",\"health\":").append(h.health())
                .append(",\"stress\":").append(h.stress())
                .append(",\"alive\":").append(h.alive()).append('}');
        }
        snapshots.add(b.append("]}").toString());
    }

    private void execute(int seconds,boolean admin,boolean continuousBots)throws Exception {
        List<Peer> peers=new ArrayList<>();
        try(LanHost host=new LanHost(HOST,0,GhostIdentity.Type.SPIRIT,
                CustomContract.standard(6),false,true,HouseMap.Site.HOUSE)){
            host.enableAiDiagnostics(true);
            try {
                for(int i=1;i<=6;i++){
                    Peer p=new Peer(host.port(),"DevHunter"+i,false,
                            i==1?host.localOwnerCredential():"");
                    peers.add(p);
                    require(p.client.seat().equals("H"+i),"hunter seat "+i+" was "+p.client.seat());
                }
                Peer ghost=new Peer(host.port(),"DevGhost",true,"");
                peers.add(ghost);
                require(ghost.client.seat().equals("GHOST"),"ghost did not occupy ghost seat");
                require(host.connected()==7,"host must own exactly seven ordinary client seats");
                DevAdminSnapshot before=host.devAdminSnapshot();
                require(before.connectedHunters()==6&&before.ghost().clientConnected(),
                        "host-local snapshot missing players");
                require(!before.ghost().hostAiFallback(),"occupied ghost seat must disable fallback");
                capture(before);

                for(Peer p:peers){
                    int mark=p.mark();
                    p.client.ready();
                    trace(p.client.seat(),"READY",p.waitPrefix(mark,"OWNREADY|YES"),host.devAdminSnapshot().tick());
                }
                Peer owner=peers.get(0);
                owner.waitFor(0,f->f.startsWith("LOBBY|6|HUMAN|7|WAITING"),
                        "seven ready ordinary roles in lobby",6000);
                int start=owner.mark();
                owner.client.start();
                owner.waitPrefix(start,"INFO|mission started");
                for(Peer p:peers.subList(1,peers.size()))
                    p.waitFor(0,f->f.startsWith("INFO|mission started"),
                            "shared launch",6000);
                require(host.running(),"normal owner START must launch real authoritative host");
                capture(host.devAdminSnapshot());

                for(int i=0;i<6;i++){
                    Peer p=peers.get(i);
                    int mark=p.mark();
                    p.client.view3D();
                    trace(p.client.seat(),"VIEW3D",p.waitPrefix(mark,"ACCEPT|VIEW3D"),
                            host.devAdminSnapshot().tick());
                }
                int mark=ghost.mark();
                ghost.client.viewGhost3D();
                trace("GHOST","GVIEW3D",ghost.waitPrefix(mark,"ACCEPT|GVIEW3D"),
                        host.devAdminSnapshot().tick());

                // One host-validated evidence measurement per real hunter; no
                // admin truth is passed into the hunter clients.
                for(int i=0;i<6;i++){
                    Peer p=peers.get(i);
                    mark=p.mark();
                    p.client.scan(LanCase.Tool.THERMOMETER);
                    String response=p.waitFor(mark,f->f.startsWith("ACCEPT|SCAN|")
                            ||f.startsWith("REJECT|SCAN|")||f.startsWith("REJECT|"),
                            "host-validated thermometer scan",6000);
                    trace(p.client.seat(),"SCAN THERMOMETER",response,host.devAdminSnapshot().tick());
                }

                // Actual spatial input from each client; a collision rejection
                // is recorded, not bypassed with host-only world mutation.
                for(int i=0;i<6;i++){
                    Peer p=peers.get(i);
                    mark=p.mark();
                    p.client.step(0,100);
                    String response=p.waitFor(mark,f->f.startsWith("ACCEPT|STEP")
                            ||f.startsWith("REJECT|"),"ordinary hunter STEP response",6000);
                    trace(p.client.seat(),"STEP 0 100",response,host.devAdminSnapshot().tick());
                }

                // The ghost AI prototype uses only its own spectral client.
                // Do not invoke LanGhostPursuit or mutate GhostDomain directly.
                boolean ghostMoved=false;
                for(int[] v:new int[][]{{100,0},{-100,0},{0,100},{0,-100}}){
                    Thread.sleep(130); // authoritative movement limit is one per host tick
                    mark=ghost.mark();
                    ghost.client.ghostStep(v[0],v[1]);
                    String response=ghost.waitFor(mark,f->f.startsWith("ACCEPT|GSTEP")
                        ||f.startsWith("REJECT|"),"ordinary ghost GSTEP response",6000);
                    trace("GHOST","GSTEP "+v[0]+" "+v[1],response,host.devAdminSnapshot().tick());
                    if(response.startsWith("ACCEPT|GSTEP")){ghostMoved=true;break;}
                }
                require(ghostMoved,"real ghost client could not make an accepted spatial step");
                mark=ghost.mark();
                ghost.client.ghost(GhostDomain.Action.DRIFT,1);
                trace("GHOST","GHOST DRIFT 1",
                        ghost.waitFor(mark,f->f.startsWith("ACCEPT|GHOST|")
                            ||f.startsWith("REJECT|"),"ordinary major ghost command",6000),
                        host.devAdminSnapshot().tick());

                Map<String,String> botIntents=new ConcurrentHashMap<>();
                List<DevMatchClientAi> drivers=new ArrayList<>();
                DevAdminPanel adminPanel=null;
                try{
                    if(continuousBots){
                        for(int i=0;i<peers.size();i++){
                            Peer p=peers.get(i);
                            DevMatchClientAi bot=new DevMatchClientAi(p.client,
                                HouseMap.Site.HOUSE,botIntents,i+1);
                            p.attach(bot);
                            drivers.add(bot);
                            bot.begin();
                        }
                    }
                    if(admin){
                        adminPanel=DevAdminPanel.open(host,HouseMap.Site.HOUSE,
                            botIntents,()->{});
                        System.out.println("ADMIN (YOU): six hunter sockets and one ghost socket; ADMIN off-seat.");
                    }
                    long until=seconds==0?Long.MAX_VALUE:
                        System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
                    while(System.nanoTime()<until && (adminPanel==null||adminPanel.isOpen())){
                        DevAdminSnapshot now=host.devAdminSnapshot();
                        require(now.connectedPlayers()==7,"lost a real TCP player connection");
                        require(!now.ghost().hostAiFallback(),"host fallback enabled while ghost occupied");
                        capture(now);
                        if(snapshots.size()>4000)snapshots.subList(0,2000).clear();
                        Thread.sleep(250);
                    }
                }finally{
                    if(adminPanel!=null)adminPanel.close();
                    for(DevMatchClientAi driver:drivers)driver.close();
                }
                DevAdminSnapshot after=host.devAdminSnapshot();
                capture(after);
                require(after.tick()>before.tick(),"authoritative simulation did not advance");
                require(after.hunters().size()==6&&after.connectedPlayers()==7,
                        "admin was added to participant counts");
                Files.writeString(OUTPUT.resolve("result.txt"),
                    "PASS: seven ordinary LAN clients connected, ready and entered match.\n"+
                    "Six hunter clients issued scans and spatial inputs; ghost client issued spatial and major inputs.\n"+
                    "Admin is a separate host-local read-only snapshot, not a player.\n"+
                    (admin?"Human developer admin viewer ran; free-fly, follow and evidence inspection enabled.\\n":
                      continuousBots?"Continuous ordinary-client basic AI drivers operated.\\n":
                      "Phase 0 smoke only; no sustained AI drivers.\\n")+
                    "Full autonomous investigation, warding and rituals NOT YET IMPLEMENTED.\\n"+
                    "Latest tick: "+after.tick()+"; outcome: "+after.outcome()+"\n",
                    StandardCharsets.UTF_8);
                Files.writeString(OUTPUT.resolve("host-profiling.csv"),
                    host.aiDiagnosticsCsv(),StandardCharsets.UTF_8);
                System.out.println("DEV MATCH PHASE 0 PASS: 6 hunter TCP clients + ghost TCP client; admin off-seat");
            }finally{
                for(int i=peers.size()-1;i>=0;i--)peers.get(i).close();
            }
        }
    }

    public static void main(String[] args)throws Exception {
        boolean selected=false;
        int seconds=-1;
        boolean admin=false,continuousBots=false;
        for(String arg:args){
            if(arg.equals("--dev-ai-match"))selected=true;
            else if(arg.equals("--admin")){admin=true;continuousBots=true;}
            else if(arg.equals("--bots"))continuousBots=true;
            else if(arg.startsWith("--seconds="))seconds=Integer.parseInt(arg.substring(10));
            else throw new IllegalArgumentException("unexpected developer option: "+arg);
        }
        if(!selected)throw new IllegalArgumentException("explicit --dev-ai-match required; not a public menu mode");
        if(seconds<0)seconds=admin?0:6;
        if(seconds==0&&!admin)throw new IllegalArgumentException("unbounded duration requires --admin");
        if(seconds<0||seconds>3600||(seconds>0&&seconds<2))
            throw new IllegalArgumentException("duration must be 2..3600 seconds, or 0 for ADMIN");
        if(admin&&java.awt.GraphicsEnvironment.isHeadless())
            throw new IllegalStateException("--admin needs an interactive desktop (use --bots for CI)");
        Files.createDirectories(OUTPUT);
        DevMatchSession session=new DevMatchSession();
        String status="STARTUP_FAILURE";
        try{
            session.execute(seconds,admin,continuousBots);
            status=admin?"DEV_ADMIN_CLOSED":continuousBots?"DEV_BOTS_PASS":"PHASE_0_PASS";
        }catch(Throwable error){
            status="FAIL: "+error;
            Files.writeString(OUTPUT.resolve("result.txt"),status+"\n",StandardCharsets.UTF_8);
            throw error;
        }finally{
            Files.writeString(OUTPUT.resolve("timeline.jsonl"),
                    String.join("\n",session.events)+"\n",StandardCharsets.UTF_8);
            Files.writeString(OUTPUT.resolve("admin-snapshots.jsonl"),
                    String.join("\n",session.snapshots)+"\n",StandardCharsets.UTF_8);
            Files.writeString(OUTPUT.resolve("match-manifest.json"),
                    "{\"mode\":\"development-only\",\"stage\":0,\"status\":"+json(status)+
                    ",\"players\":7,\"hunters\":6,\"ghosts\":1,\"adminSeats\":0,"+
                    "\"seed\":\"host-random (not deterministic yet)\",\"seconds\":"+seconds+"}\n",
                    StandardCharsets.UTF_8);
        }
    }
}
