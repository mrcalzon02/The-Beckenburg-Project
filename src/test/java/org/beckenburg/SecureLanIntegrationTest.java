package org.beckenburg;

import hbnet.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual encrypted player intents, private ghost state and identity-bound reconnect. */
public final class SecureLanIntegrationTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static String await(BlockingQueue<String> q,String prefix)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);while(System.nanoTime()<end){String s=q.poll(50,TimeUnit.MILLISECONDS);if(s!=null&&s.startsWith(prefix))return s;}throw new AssertionError("Missing "+prefix);
    }
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("beckenburg-tls-");
        try{
            Hbnet.Server tls=Setup.initialize(dir,null);char[] key=Files.readString(dir.resolve("room.secret")).toCharArray();
            Hbnet.Client owner=new Hbnet.Client(tls.fingerprint,"beckenburg",Hbnet.Identity.create(),key),guest=new Hbnet.Client(tls.fingerprint,"beckenburg",Hbnet.Identity.create(),key),ghost=new Hbnet.Client(tls.fingerprint,"beckenburg",Hbnet.Identity.create(),key);
            BlockingQueue<String> a=new LinkedBlockingQueue<>(),b=new LinkedBlockingQueue<>(),g=new LinkedBlockingQueue<>();
            try(LanHost host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(2),false,HouseMap.Site.HOUSE,tls);
                LanClient first=new LanClient("localhost",host.port(),false,"Owner",host.localOwnerCredential(),a::offer,owner);
                LanClient second=new LanClient("localhost",host.port(),false,"Guest","",b::offer,guest);
                LanClient spirit=new LanClient("localhost",host.port(),true,"Unknown","",g::offer,ghost)){
                check(host.voicePort()==0,"secure sessions cannot open plaintext voice");
                second.start();check(await(b,"REJECT|").contains("host"),"guest cannot start");
                first.ready();second.ready();spirit.ready();await(a,"LOBBY|2|HUMAN|3|WAITING");first.start();await(a,"INFO|mission started");
                first.view3D();await(a,"ACCEPT|VIEW3D");String before=await(b,"BODY|0|");Thread.sleep(150);first.step(0,-100);await(a,"ACCEPT|STEP");
                boolean changed=false;for(int i=0;i<30&&!changed;i++){String next=await(b,"BODY|0|");String[] x=before.split("\\|"),y=next.split("\\|");changed=!x[2].equals(y[2])||!x[4].equals(y[4]);}check(changed,"second TLS client observes authoritative movement");
                check(g.stream().noneMatch(s->s.startsWith("BODY|")||s.startsWith("CASEFILE|")||s.startsWith("PLAYERNAME|")),"ghost private state boundary");
                LanClient.ResumeTicket ticket=second.resumeTicket();var socket=LanClient.class.getDeclaredField("socket");socket.setAccessible(true);((Socket)socket.get(second)).close();
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(host.reservedSeats()==0&&System.nanoTime()<deadline)Thread.sleep(20);
                try{new LanClient("localhost",host.port(),ticket,s->{},ghost).close();throw new AssertionError("stolen token accepted for another signed identity");}catch(java.io.IOException expected){}
                try(LanClient resumed=new LanClient("localhost",host.port(),ticket,b::offer,guest)){check(resumed.seat().equals(ticket.seat()),"same signed identity resumes seat");resumed.view3D();await(b,"ACCEPT|VIEW3D");}
                check(host.running(),"malformed/unauthorized requests preserve match");
            }
            owner.clear();guest.clear();ghost.clear();Arrays.fill(key,'\0');
            System.out.println("PASS SecureLanIntegrationTest: TLS gameplay, two hunters+ghost, owner role, no UDP, signed-seat reconnect and stolen-token rejection");
        }finally{try(var files=Files.walk(dir)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
