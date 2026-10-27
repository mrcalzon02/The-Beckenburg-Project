package org.beckenburg;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real sockets: ownership, team skill use, cooldown, and ghost observation boundaries. */
public final class LanHunterAbilitiesNetworkTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Peer implements AutoCloseable {
        final BlockingQueue<String> inbox=new LinkedBlockingQueue<>();
        final List<String> history=new CopyOnWriteArrayList<>();
        final LanClient client;
        Peer(int port,boolean ghost,String label)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,label,frame->{
                history.add(frame);inbox.offer(frame);
            });
        }
        String await(String prefix)throws Exception {
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<deadline){
                String line=inbox.poll(100,TimeUnit.MILLISECONDS);
                if(line!=null&&line.startsWith(prefix))return line;
            }
            throw new AssertionError(client.seat()+" missing "+prefix);
        }
        @Override public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        freshSeatDoesNotInheritClass();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3));
            var cleric=new Peer(host.port(),false,"Chaplain");
            var observer=new Peer(host.port(),false,"Archivist");
            var demon=new Peer(host.port(),false,"Occultist");
            var ghost=new Peer(host.port(),true,"Specter")){
            cleric.client.selectVisual(CharacterVisuals.Hunter.CLERIC);
            observer.client.selectVisual(CharacterVisuals.Hunter.OBSERVER);
            demon.client.selectVisual(CharacterVisuals.Hunter.DEMONOLOGIST);
            cleric.await("VISUAL|0|cleric");
            observer.await("VISUAL|1|observer");
            demon.await("VISUAL|2|demonologist");
            cleric.client.ready();observer.client.ready();
            demon.client.ready();ghost.client.ready();
            cleric.await("LOBBY|3|HUMAN|4|WAITING");
            cleric.client.start();cleric.await("INFO|mission started");
            cleric.client.skill(HunterDiscipline.Ability.COMFORT);
            check(cleric.await("ACCEPT|SKILL|Comfort activated").contains("Comfort"),
                "Cleric comfort must be accepted through host");
            cleric.client.skill(HunterDiscipline.Ability.COMFORT);
            check(cleric.await("REJECT|SKILL|Comfort recharging").contains("recharging"),
                "Cooldown must reject a repeated skill");
            cleric.client.skill(HunterDiscipline.Ability.INVOKE);
            check(cleric.await("REJECT|SKILL|ability belongs").contains("DEMONOLOGIST"),
                "An investigator must not borrow another class's action");
            observer.client.skill(HunterDiscipline.Ability.SURVEY);
            check(observer.await("ACCEPT|SKILL|Survey activated").contains("Survey"),
                "Observer must activate real room survey");
            check(observer.await("INFO|SURVEY READING EMF").contains("EMF"),
                "Survey must report a real environmental EMF reading");
            check(observer.await("INFO|SURVEY READING THERMOMETER").contains("Air"),
                "Survey must report a real temperature reading");
            demon.client.skill(HunterDiscipline.Ability.SIGIL);
            check(demon.await("ACCEPT|SKILL|Binding Sigil activated").contains("Sigil"),
                "Demonologist must lay protective binding");
            demon.client.skill(HunterDiscipline.Ability.INVOKE);
            check(demon.await("ACCEPT|SKILL|Invocation activated").contains("Invocation"),
                "Demonologist must invoke the threat at a sanity cost");
            ghost.client.skill(HunterDiscipline.Ability.HALLOW);
            check(ghost.await("REJECT|").contains("hunter"),"Ghost cannot trigger Hunter skills");
            check(ghost.history.stream().noneMatch(line->line.startsWith("CLASSFX|")
                    ||line.startsWith("INFO|SURVEY")||line.startsWith("PROGRESS|")
                    ||line.startsWith("CASEFILE|")),
                "Personal abilities and observed clues must not leak to spectral controller");
            check(observer.history.stream().noneMatch(line->line.startsWith("CASEFILE|")
                    &&line.contains("SURVEY")),"Survey must never forge evidence");
            check(host.running(),"Class actions must not corrupt the running match");
            System.out.println("PASS: multiplayer class ownership, live sensor survey, cooldowns, ghost isolation");
        }
    }
    private static void freshSeatDoesNotInheritClass()throws Exception{
        try(var host=new LanHost("127.0.0.1",0);
            var owner=new Peer(host.port(),false,"Owner")){
            try(var departing=new Peer(host.port(),false,"Departing")){
                departing.client.selectVisual(CharacterVisuals.Hunter.CLERIC);
                departing.await("VISUAL|1|cleric");
            }
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(host.connected()!=1&&System.nanoTime()<deadline)Thread.sleep(10);
            check(host.connected()==1,"departed lobby seat released before replacement");
            try(var replacement=new Peer(host.port(),false,"Replacement")){
                check(replacement.client.seat().equals("H2"),"new occupant reuses actual vacant seat");
                replacement.await("VISUAL|1|observer");
                var field=LanHost.class.getDeclaredField("mission");field.setAccessible(true);
                check(((LanCase)field.get(host)).discipline(1)==CharacterVisuals.Hunter.OBSERVER,
                    "fresh Observer seat cannot inherit departed Cleric authority");
            }
        }
    }
}
