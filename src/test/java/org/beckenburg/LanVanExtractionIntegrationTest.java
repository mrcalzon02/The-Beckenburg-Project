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
    private static void run(int hunters,boolean outside,boolean departed)throws Exception {
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
                    }else{
                        missing.client.view3D();
                        missing.expect("ACCEPT|VIEW3D");
                        missing.client.vanDoor();
                        check(missing.expect("REJECT|").contains("van door button out of reach"),
                            "remote field hunter cannot close the van");
                        check(host.running(),"rejected outside request must not end the round");
                    }
                }
                leader.client.view3D();
                leader.expect("ACCEPT|VIEW3D");
                leader.client.vanDoor();
                leader.expect("ACCEPT|VANDOOR|CLOSED");
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
                leader.client.vanDoor();
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
    public static void main(String[] args)throws Exception {
        run(1,false,false);
        run(3,false,false);
        run(3,true,false);
        run(3,true,true);
        System.out.println("PASS: solo/all-aboard/outside/departed host van extraction, status and once-only result");
    }
}
