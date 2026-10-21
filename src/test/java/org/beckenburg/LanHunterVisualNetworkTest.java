package org.beckenburg;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real TCP integration: base Hunter art travels with the correct physical seat. */
public final class LanHunterVisualNetworkTest {
    private LanHunterVisualNetworkTest(){}
    private static void check(boolean condition,String why){
        if(!condition)throw new AssertionError(why);
    }
    private static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final List<String> history=new CopyOnWriteArrayList<>();
        final LanClient client;
        Peer(int port,boolean ghost,String nickname)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,nickname,packet->{
                history.add(packet);inbox.offer(packet);
            });
        }
        String await(String prefix)throws Exception{
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
            while(System.nanoTime()<deadline){
                String message=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(message!=null&&message.startsWith(prefix))return message;
            }
            throw new AssertionError("Missing "+prefix+" on "+client.seat());
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(2));
            var a=new Peer(host.port(),false,"A");
            var b=new Peer(host.port(),false,"B");
            var ghost=new Peer(host.port(),true,"Hidden")){
            check("H1".equals(a.client.seat())&&"H2".equals(b.client.seat()),
                    "Hunter art association must follow assigned seat");
            a.client.selectVisual(CharacterVisuals.Hunter.CLERIC);
            b.client.selectVisual(CharacterVisuals.Hunter.DEMONOLOGIST);
            check(a.await("VISUAL|0|cleric").equals("VISUAL|0|cleric"),
                "Host must broadcast H1's real Cleric appearance");
            check(b.await("VISUAL|1|demonologist").equals("VISUAL|1|demonologist"),
                "Host must broadcast H2's real Demonologist appearance");
            check(ghost.history.stream().noneMatch(x->x.startsWith("VISUAL|")),
                "Host must not leak investigator avatar selection to ghost");
            boolean rejected=false;
            try{ghost.client.selectVisual(CharacterVisuals.Hunter.CLERIC);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Ghost must not issue a Hunter appearance selection");
            a.client.ready();b.client.ready();ghost.client.ready();
            a.await("LOBBY|2|HUMAN|3|WAITING");
            a.client.start();a.await("INFO|mission started");
            String clericState=a.await("PROGRESS|");
            String demonState=b.await("PROGRESS|");
            check(clericState.split("\\|")[3].equals("3"),
                "Cleric must receive its third real ward at mission start");
            check(demonState.split("\\|")[3].equals("2"),
                "Demonologist must retain normal ward capacity");
            b.inbox.clear();
            b.client.view3D();b.await("ACCEPT|VIEW3D");
            check(b.await("VISUAL|0|cleric").equals("VISUAL|0|cleric"),
                    "Late world view must recover an accurate Hunter appearance");
            check(b.await("VISUAL|1|demonologist").equals("VISUAL|1|demonologist"),
                    "Late world view must recover its own appearance too");
            check(ghost.history.stream().noneMatch(x->x.startsWith("VISUAL|")),
                    "Entering gameplay must not reveal cosmetics to ghost seat");
            check(host.running(),"Cosmetic selection cannot compromise host session");
            System.out.println("PASS: real LAN art selection, two distinct teammates, world replay, ghost privacy");
        }
    }
}
