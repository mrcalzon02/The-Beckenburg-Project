package org.beckenburg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real TCP match: Spirit Box must emit static, real speech, and honest raw notes.
 * No debug-only evidence and no self-fabricated replay from repeated scans. */
public final class LanSpiritBoxAudioIntegrationTest {
    private LanSpiritBoxAudioIntegrationTest(){}
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final LanClient client;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,inbox::offer);
        }
        String await(String prefix)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(9);
            while(System.nanoTime()<until){
                String frame=inbox.poll(80,TimeUnit.MILLISECONDS);
                if(frame!=null&&frame.startsWith(prefix))return frame;
            }
            throw new AssertionError(client.seat()+" missing "+prefix);
        }
        boolean heardPhrase(long millis)throws Exception{
            long until=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
            while(System.nanoTime()<until){
                String frame=inbox.poll(40,TimeUnit.MILLISECONDS);
                if(frame!=null&&frame.startsWith("SOUND|SPIRIT_BOX_")
                        &&!frame.startsWith("SOUND|SPIRIT_BOX_STATIC|"))return true;
            }
            return false;
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(LanHost host=new LanHost("127.0.0.1",0);
                Peer h=new Peer(host.port(),false,"Receiver");
                Peer observer=new Peer(host.port(),false,"Observer");
                Peer observer2=new Peer(host.port(),false,"Backup");
                Peer ghost=new Peer(host.port(),true,"Ghost")){
            h.client.ready();observer.client.ready();observer2.client.ready();
            ghost.client.ready();
            h.await("LOBBY|3|HUMAN|4|WAITING");
            h.client.start();h.await("INFO|mission started");
            for(int room:new int[]{11,8,5,4})LanTestRoomTravel.move(h.client,h::await,room);
            h.client.view3D();h.await("ACCEPT|VIEW3D");
            observer.client.view3D();observer.await("ACCEPT|VIEW3D");
            h.client.selectGearSlot(2);
            check(h.await("GEARSTATE|").contains("SPIRIT_BOX:I"),
                "must actually equip a tiered handheld Spirit Box");
            h.client.useHeldEquipment();
            String first=h.await("ACCEPT|GEARUSE|SPIRIT_BOX|");
            check(first.contains("Radio static; no intelligible voice"),
                "proximity alone must not generate a reply: "+first);
            LanSound.CueEvent firstHiss=LanSound.decode(h.await("SOUND|SPIRIT_BOX_STATIC|"));
            check(firstHiss.volume()>0,"the no-response scan has real radio audio");
            check(!observer.heardPhrase(300),
                "distant hunter in the van cannot hear a ghost from a Spirit Box");

            // The actual ghost player's permitted WHISPER action establishes a
            // host-authorized voice event; a client cannot simply claim evidence.
            Thread.sleep(1450); // Tier I 12-tick hardware refresh.
            ghost.client.ghost(GhostDomain.Action.WHISPER,4);
            ghost.await("ACCEPT|GHOST|WHISPER");
            h.client.useHeldEquipment();
            String spoken=h.await("ACCEPT|GEARUSE|SPIRIT_BOX|");
            check(spoken.contains("Vocal fragment heard:"),
                "accepted real vocal event must yield heard words, not yes/no: "+spoken);
            h.await("SOUND|SPIRIT_BOX_STATIC|");
            String voice=h.await("SOUND|SPIRIT_BOX_");
            LanSound.CueEvent actual=LanSound.decode(voice);
            check(actual.cue()!=LanSound.Cue.SPIRIT_BOX_STATIC
                &&actual.cue().caption().startsWith("SPIRIT BOX / "),
                "voice event must identify an actual playable line");
            check(spoken.contains("\""+SpiritBoxResponses.Phrase.valueOf(
                    actual.cue().name().substring("SPIRIT_BOX_".length())).words+"\""),
                "spoken clip and recorded observation must agree");
            check(!observer.heardPhrase(250),
                "no global audio broadcast to van occupants");
            check(host.running(),"audio never changes live ghost or match authority");
            System.out.println("PASS real-socket Spirit Box static, host vocal reply, raw notes, no remote broadcast");
        }
    }
}
