package org.beckenburg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real TCP: closing the van is a once-only host retreat, not an all-hunters-ready vote. */
public final class LanVanExtractionIntegrationTest {
    private static final class Wire implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final List<String> seen=new CopyOnWriteArrayList<>();
        final LanClient client;
        Wire(int port,String name)throws Exception {
            client=new LanClient("127.0.0.1",port,false,name,line->{
                seen.add(line);
                inbox.offer(line);
            });
        }
        String expect(String prefix)throws Exception {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<end){
                String line=inbox.poll(100,TimeUnit.MILLISECONDS);
                if(line!=null&&line.startsWith(prefix))return line;
            }
            throw new AssertionError(client.seat()+" missing "+prefix+"; seen="+seen);
        }
        boolean saw(String frame){return seen.contains(frame);}
        long count(String frame){return seen.stream().filter(frame::equals).count();}
        @Override public void close(){client.close();}
    }
    private static void check(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    private static void run(int hunters,boolean outside,boolean departed,boolean roomMode)throws Exception {
        try(LanHost host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,
                CustomContract.standard(hunters))){
            List<Wire> peers=new ArrayList<>();
            try{
                for(int i=0;i<hunters;i++)peers.add(new Wire(host.port(),"VanHunter"+i));
                Wire leader=peers.get(0);
                for(Wire peer:peers)peer.client.ready();
                leader.expect("LOBBY|"+hunters+"|AI|"+hunters+"|WAITING");
                leader.client.start();
                leader.expect("INFO|mission started");
                if(outside){
                    Wire missing=peers.get(1);
                    LanTestRoomTravel.move(missing.client,missing::expect,4);
                    if(departed){
                        missing.close();
                        leader.expect("PEER|LEFT|1");
                    }else if(roomMode){
                        missing.client.extract();
                        check(missing.expect("REJECT|").contains("operate the exit from inside the van"),
                            "room-mode field hunter cannot extract remotely");
                        check(host.running(),"rejected remote room-mode extraction must not end the round");
                    }else{
                        missing.client.view3D();
                        missing.expect("ACCEPT|VIEW3D");
                        missing.client.vanDoor();
                        check(missing.expect("REJECT|").contains("van door button out of reach"),
                            "remote field hunter cannot close the van");
                        check(host.running(),"rejected outside request must not end the round");
                    }
                }
                if(roomMode){
                    // A mixed party must count the physically present hunter, too.
                    if(!outside&&hunters>1){
                        peers.get(1).client.view3D();
                        peers.get(1).expect("ACCEPT|VIEW3D");
                    }
                    leader.client.extract();
                    leader.expect("ACCEPT|EXTRACT");
                }else{
                    leader.client.view3D();
                    leader.expect("ACCEPT|VIEW3D");
                    leader.client.extract();
                    check(leader.expect("REJECT|").contains("physical hunters must use the van door control"),
                        "legacy EXTRACT must not bypass physical van control");
                    check(host.running(),"rejected physical EXTRACT cannot finish the round");
                    leader.client.vanDoor();
                    leader.expect("ACCEPT|VANDOOR|CLOSED");
                }
                leader.expect("RESULT|RETREATED");
                check(!host.running(),"host ends the investigation immediately");
                check(leader.saw("VANDOOR|1"),"physical door close broadcast");
                check(leader.saw("EXTRACTION|0|1"),"initiating hunter marked aboard");
                for(int i=1;i<hunters;i++)
                    check(leader.saw("EXTRACTION|"+i+"|"+
                            (outside&&i==1?"0":"1")),
                        "accurate host extraction snapshot for H"+(i+1));
                for(int i=0;i<hunters;i++)if(!(departed&&i==1)){
                    Wire peer=peers.get(i);
                    if(i!=0)peer.expect("RESULT|RETREATED"); // leader already consumed it
                    check(peer.count("RESULT|RETREATED")==1,
                        "exactly one result per connected hunter");
                }
                if(roomMode)leader.client.extract();else leader.client.vanDoor();
                leader.expect("REJECT|");
                Thread.sleep(150);
                check(leader.count("RESULT|RETREATED")==1,"repeat input cannot settle twice");
                check(leader.seen.stream().filter(s->s.startsWith("PAYOUT|")).count()==1,
                    "host sends one settlement to the initiating hunter");
            }finally{
                for(Wire peer:peers)peer.close();
            }
        }
    }
    private static void deskDeparture()throws Exception {
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1));
                var leader=new Wire(host.port(),"DeskHunter")){
            leader.client.ready();leader.client.start();leader.expect("INFO|mission started");
            leader.client.view3D();leader.expect("ACCEPT|VIEW3D");
            var mapField=LanHost.class.getDeclaredField("map");mapField.setAccessible(true);
            var worldField=LanHost.class.getDeclaredField("world");worldField.setAccessible(true);
            var map=(HouseMap)mapField.get(host);var world=(LanWorld)worldField.get(host);
            double yaw=map.van().placement().yaw();
            // Walk from the real spawn into the front desk aisle over TCP.
            for(int n=0;n<7;n++){
                Thread.sleep(120);
                leader.client.step((int)Math.round(-100*Math.sin(yaw)),(int)Math.round(100*Math.cos(yaw)));
                leader.expect("ACCEPT|STEP");
            }
            var body=world.body(0);
            check(map.van().local(body.x(),body.z()).z()>.9,"hunter reached front desk");
            var observer=VanCargoDoorTest.aimButton(map,body.x(),body.y(),body.z());
            var target=InteractionFocus.pick(map,observer,List.of());
            leader.client.interact(target,observer.yaw(),observer.pitch());
            leader.expect("ACCEPT|INTERACT|Close doors and end mission");
            leader.expect("RESULT|RETREATED");
            check(map.van().cargoDoorClosed()&&!host.running(),"desk interaction closes doors and settles mission");
            check(leader.count("RESULT|RETREATED")==1,"desk departure settles once");
        }
    }
    public static void main(String[] args)throws Exception {
        deskDeparture();
        for(boolean roomMode:new boolean[]{false,true}){
            run(1,false,false,roomMode);
            run(3,false,false,roomMode);
            run(3,true,false,roomMode);
            run(3,true,true,roomMode);
        }
        System.out.println("PASS: physical and legacy room-mode solo/all-aboard/outside/departed retreat, roster and once-only result");
    }
}
