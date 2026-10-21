package org.beckenburg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;

/** Real TCP session: one human Twin seat, one independent host AI sister. */
public final class LanTwinPlayerCompanionTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private static final class Peer implements AutoCloseable{
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);
        }
        String await(String prefix,int seconds)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
            while(System.nanoTime()<until){
                String msg=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(msg!=null&&msg.startsWith(prefix))return msg;
                if(msg!=null&&msg.startsWith("REJECT|"))
                    throw new AssertionError("Live Twins command rejected: "+msg);
            }
            throw new AssertionError("No "+prefix+" for "+client.seat());
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,
                    GhostIdentity.Type.TWINS,CustomContract.standard(2));
            var h1=new Peer(host.port(),false,"Investigator1");
            var h2=new Peer(host.port(),false,"Investigator2");
            var player=new Peer(host.port(),true,"Playable Twin A")){
            h1.client.ready();h2.client.ready();player.client.ready();
            h1.await("LOBBY|2|HUMAN|3|WAITING",13);
            h1.client.start();h1.await("INFO|mission started",13);
            player.client.viewGhost3D();
            player.await("ACCEPT|GVIEW3D",13);
            check(player.await("GTYPE|TWINS|",6).contains("TWINS"),
                "Ghost player must remain one Twins case identity");
            String first=player.await("GCOMPANION|",6);
            String[] columns=first.split("\\|");
            check(columns.length==7,"Companion has independent 3D coordinates");
            Set<String> coordinates=new HashSet<>();
            coordinates.add(columns[1]+"|"+columns[2]+"|"+columns[3]);
            for(int i=0;i<18;i++){
                String packet=player.await("GCOMPANION|",8);
                String[] fields=packet.split("\\|");
                check(fields.length==7,"Well-formed AI sister pose");
                coordinates.add(fields[1]+"|"+fields[2]+"|"+fields[3]);
            }
            check(coordinates.size()>2,
                "AI sister must continue moving even when player Twin stands idle");
            player.client.ghostTurn(90);
            player.await("GPOSE|",8);
            player.await("GCOMPANION|",8);
            check(host.running(),"AI sister may not create a second ghost seat or stop match");
            System.out.println("PASS: human controls Twin A, Twin B autonomously patrols and streams live positions over TCP");
        }
    }
}
