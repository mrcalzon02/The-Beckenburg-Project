package org.beckenburg;

import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import org.beckenburg.LevelDefinition.Vec;

/** Real versioned sockets, host tick path, ghost privacy, reconnect state and no cue replay. */
public final class HouseEncounterNetworkTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object field(Object object,String name)throws Exception{return LanSafeAreaBarrierTest.field(object,name);}
    private static String await(BlockingQueue<String> inbox,String prefix)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline){var row=inbox.poll(50,TimeUnit.MILLISECONDS);if(row!=null&&row.startsWith(prefix))return row;}
        throw new AssertionError("Missing real packet "+prefix);
    }
    private static void ticks(LanHost host,int count)throws Exception{
        var method=LanHost.class.getDeclaredMethod("tickSafely");method.setAccessible(true);
        synchronized(field(host,"mutex")){for(int i=0;i<count;i++)method.invoke(host);}
    }
    private static void place(LanHost host,HouseMap map,int seat,LooseClutter.Observer observer)throws Exception{
        synchronized(field(host,"mutex")){AuthoredTestSupport.place(map,(LanWorld)field(host,"world"),seat,new Vec(observer.x(),observer.y(),observer.z()));}
    }
    private static void disconnected(LanHost host,int count)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end){if(host.connected()==count)return;Thread.sleep(10);}
        throw new AssertionError("Host did not observe actual transport loss");
    }
    private static LanClient.ResumeTicket persist(LanClient client,java.nio.file.Path path)throws Exception{
        var ticket=client.resumeTicket();check(ticket!=null,"Actual host issued reconnect capability");
        LanResumeStore.save(path,client.address(),client.port(),ticket.seat(),ticket.token(),ticket.nextSequence());
        check(LanResumeStore.load(path).orElseThrow().token().equals(ticket.token()),"Recovery capability persisted before transport loss/rotation acknowledgement");return ticket;
    }
    private static void scenario(boolean disconnectOwner)throws Exception{
        var map=HouseRebuildFurnishings.map();
        var folder=java.nio.file.Files.createTempDirectory("house-encounter-reconnect-");
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map)){
            for(String old:new String[]{"JOIN|2|HUNTER|Legacy","RESUME|2|H1|"+"0".repeat(48)})try(var socket=new Socket("127.0.0.1",host.port())){
                socket.setSoTimeout(3000);var writer=new OutputStreamWriter(socket.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8);
                LanProtocol.write(writer,old);var reader=new InputStreamReader(socket.getInputStream(),java.nio.charset.StandardCharsets.UTF_8);
                check(LanProtocol.read(reader).startsWith("REJECT|protocol version"),"Old clients must be rejected before unknown authored cue vocabulary");
            }
            try(var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Cleric");var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
                var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Scholar");var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
                a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
                for(var peer:List.of(a,b,c)){peer.client.view3D();peer.await("ACCEPT|VIEW3D");}
                ghost.client.viewGhost3D();ghost.await("ACCEPT|GVIEW3D");
                var old=map.clutter().authoredItem("wooden-train");ghost.client.clutter("TAKE",old.id(),0);ghost.await("REJECT|");
                check(map.clutter().authoredItem("wooden-train").equals(old),"Ghost cannot seize or force the location object");
                // Advance every real subsystem clock; never jump the host tick
                // past the ghost/mission authority just to reach eligibility.
                ticks(host,HouseEncounter.EARLIEST);
                var near=HouseEncounterTest.study(map,0);place(host,map,0,near);
                ticks(host,HouseEncounter.NOTICE_TICKS);
                var controller=(HouseEncounter)field(host,"houseEncounter");check(controller.phase()==HouseEncounter.Phase.NOTICED,"Real host tick arms source location");
                var ticket=persist(disconnectOwner?a.client:b.client,folder.resolve("initial.properties"));
                if(disconnectOwner){
                    ((Socket)field(a.client,"socket")).close();disconnected(host,3);ticks(host,2);
                    check(controller.phase()==HouseEncounter.Phase.ABORTED,"Actual owner disconnect aborts pending host event");
                    var inbox=new LinkedBlockingQueue<String>();try(var resumed=new LanClient("127.0.0.1",host.port(),ticket,inbox::offer)){
                        persist(resumed,folder.resolve("rotated.properties"));resumed.acknowledgeStoredResume();await(inbox,"ACCEPT|RESUMEACK");resumed.view3D();await(inbox,"ACCEPT|VIEW3D");ticks(host,HouseEncounter.EMPTY_TICKS+2);
                        check(controller.phase()==HouseEncounter.Phase.ABORTED&&Math.abs(map.clutter().authoredItem("wooden-train").yaw()-old.yaw())<1e-8,"Reconnect cannot replay an aborted sequence");
                    }return;
                }
                var door=map.level().portals().stream().filter(p->p.door()&&p.joins(5,7)).findFirst().orElseThrow();
                synchronized(field(host,"mutex")){if(!map.doorOpen(door.id()))map.toggleDoor(door.id());}
                // Stand around the actual doorway corner: invisible to the toy,
                // but the open acoustic portal still carries its quiet click.
                var away=new LooseClutter.Observer(0,18.35,2.8,14.9,0,0,true);place(host,map,0,away);
                check(!controller.visible(away,old)&&LanSound.project(map,((LanWorld)field(host,"world")).body(0),7,old.x(),old.y(),old.z(),LanSound.Cue.TOY_WINDUP,1)!=null,"Real occluded yet acoustically audible doorway fixture");
                ticks(host,HouseEncounter.EMPTY_TICKS);check(controller.phase()==HouseEncounter.Phase.TURNED,"Actual host owns supported rigid turn");
                // The cue precedes the pose broadcast. Preserve that ordering
                // rather than discarding it while awaiting a later object row.
                var sound=LanSound.decode(a.await("SOUND|TOY_WINDUP|"));check(sound.volume()>=12&&sound.volume()<=46&&sound.pan()%25==0,"Real source/door/elevation audio projection");
                String prefix="CLUTTER|"+old.id()+",TOY,";
                for(var peer:List.of(a,b,c,ghost)){
                    String row=null;for(int attempts=0;attempts<12;attempts++){var frame=peer.await(prefix);var fields=frame.substring(8).split(",");
                        if(Math.abs(Double.parseDouble(fields[5])/1000-old.yaw()-Math.PI/2)<.002){row=frame;break;}}
                    check(row!=null&&row.endsWith(",-1"),"Each real role receives the same public unheld pose");
                }
                var lights=(RoomLighting)field(host,"lights");check(lights.breaker()&&lights.sourceRunning(),"Ambient sequence does not trip normal power");
                ((Socket)field(b.client,"socket")).close();disconnected(host,3);
                var inbox=new LinkedBlockingQueue<String>();try(var resumed=new LanClient("127.0.0.1",host.port(),ticket,inbox::offer)){
                    persist(resumed,folder.resolve("rotated.properties"));resumed.acknowledgeStoredResume();await(inbox,"ACCEPT|RESUMEACK");resumed.view3D();await(inbox,"ACCEPT|VIEW3D");
                    var pose=await(inbox,prefix);check(Math.abs(Double.parseDouble(pose.substring(8).split(",")[5])/1000-old.yaw()-Math.PI/2)<.002,"Actual reconnect receives current source pose");
                    ticks(host,5);var remaining=new ArrayList<String>();inbox.drainTo(remaining);
                    check(remaining.stream().noneMatch(frame->frame.startsWith("SOUND|TOY_WINDUP|")),"Reconnect/view snapshots never replay the one-shot cue");
                }
                var publicFrames=new ArrayList<String>();ghost.inbox.drainTo(publicFrames);
                check(publicFrames.stream().noneMatch(frame->frame.startsWith("ENCOUNTER|")||frame.contains("noticedAt")||frame.contains("owner=")),"No encounter trigger/owner/observer state leaks into ghost packets");
                HouseEncounterTest.support(map);
            }
        }finally{try(var paths=java.nio.file.Files.walk(folder)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(path);}}
    }
    public static void main(String[] args)throws Exception{
        check(LanProtocol.VERSION>2,"Authored cue vocabulary needs explicit protocol compatibility");scenario(false);scenario(true);
        System.out.println("PASS actual4TCP host train cue/public pose, legacy JOIN/RESUME rejection, possession/privacy/power negatives, reconnect snapshot/no replay and real owner-disconnect abort");
    }
}
