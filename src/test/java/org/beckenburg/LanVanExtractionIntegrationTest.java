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

    /**
     * Four real TCP seats: H2 and H3 remain outside while H1 seals the van and
     * H4 stays aboard. An optional H3 socket failure is EOF (without LEAVE):
     * the host must finalize without waiting for a reconnect reservation.
     */
    private static void runTwoOutsiders(boolean roomMode,boolean abruptLoss)throws Exception {
        try(LanHost host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,
                CustomContract.standard(4))){
            List<Wire> peers=new ArrayList<>();
            try{
                for(int i=0;i<4;i++)peers.add(new Wire(host.port(),"FourSeatHunter"+i));
                Wire leader=peers.get(0),outside=peers.get(1),lost=peers.get(2);
                for(Wire peer:peers)peer.client.ready();
                leader.expect("LOBBY|4|AI|4|WAITING");
                leader.client.start();
                leader.expect("INFO|mission started");

                // Different real rooms: neither outsider is extracted merely
                // because a teammate operates the van's doors.
                LanTestRoomTravel.move(outside.client,outside::expect,4);
                LanTestRoomTravel.move(lost.client,lost::expect,11);
                if(roomMode){
                    outside.client.extract();
                    check(outside.expect("REJECT|").contains("operate the exit from inside the van"),
                        "outside room-mode hunter cannot finish for the team");
                }else{
                    outside.client.view3D();
                    outside.expect("ACCEPT|VIEW3D");
                    outside.client.vanDoor();
                    check(outside.expect("REJECT|").contains("van door button out of reach"),
                        "outside physical hunter cannot close the van");
                }
                check(host.running(),"outside request must not finish four-seat round");

                if(abruptLoss){
                    // Same genuine unexpected TCP loss as the LAN resume tests:
                    // client.close() would send LEAVE and vacate the seat instead.
                    var socketField=LanClient.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    ((java.net.Socket)socketField.get(lost.client)).close();
                    leader.expect("PEER|LOST|2");
                    check(host.running()&&host.reservedSeats()==1,
                        "ungraceful H3 loss reserves its seat without trapping the match");
                }

                if(roomMode){
                    leader.client.extract();
                    leader.expect("ACCEPT|EXTRACT");
                }else{
                    leader.client.view3D();
                    leader.expect("ACCEPT|VIEW3D");
                    leader.client.vanDoor();
                    leader.expect("ACCEPT|VANDOOR|CLOSED");
                }
                leader.expect("RESULT|RETREATED");
                check(!host.running(),"host finalizes immediately with two outsiders");
                check(leader.saw("VANDOOR|1"),"authoritative physical doors sealed");
                check(leader.saw("EXTRACTION|0|1"),"H1 aboard");
                check(leader.saw("EXTRACTION|1|0"),"H2 outside, not extracted");
                check(leader.saw("EXTRACTION|2|0"),"H3 outside/lost, not extracted");
                check(leader.saw("EXTRACTION|3|1"),"H4 aboard, not penalized by missing seats");

                for(int i=0;i<4;i++){
                    if(abruptLoss&&i==2)continue;
                    Wire peer=peers.get(i);
                    if(i!=0)peer.expect("RESULT|RETREATED");
                    peer.expect("PAYOUT|");
                    check(peer.count("RESULT|RETREATED")==1,
                        "one final result for connected seat H"+(i+1));
                    check(peer.seen.stream().filter(s->s.startsWith("PAYOUT|")).count()==1,
                        "one settlement packet for connected seat H"+(i+1));
                }
                if(roomMode)leader.client.extract();else leader.client.vanDoor();
                leader.expect("REJECT|");
                Thread.sleep(150);
                check(leader.count("RESULT|RETREATED")==1,
                    "repeated close cannot double-finalize four-seat round");
                check(leader.seen.stream().filter(s->s.startsWith("PAYOUT|")).count()==1,
                    "repeated close cannot duplicate four-seat settlement");
            }finally{
                for(Wire peer:peers)peer.close();
            }
        }
    }

    public static void main(String[] args)throws Exception {
        for(boolean roomMode:new boolean[]{false,true}){
            run(1,false,false,roomMode);
            run(3,false,false,roomMode);
            run(3,true,false,roomMode);
            run(3,true,true,roomMode);
        }
        for(boolean roomMode:new boolean[]{false,true}){
            runTwoOutsiders(roomMode,false);
            runTwoOutsiders(roomMode,true);
        }
        System.out.println("PASS: physical/room solo, outside, departed, two-outsider and abrupt-loss host retreat, per-seat roster and once-only result");
    }
}
