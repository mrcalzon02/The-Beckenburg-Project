package org.beckenburg;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;

/** Real TCP world drop, belt conservation and E-key recovery. */
public final class LanDroppedGearIntegrationTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static final class Peer implements AutoCloseable {
        final List<String> frames=new CopyOnWriteArrayList<>();
        final LanClient client;
        Peer(int port,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,false,name,frames::add);
        }
        int mark(){return frames.size();}
        String await(int from,String prefix)throws Exception{
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(9);
            while(System.nanoTime()<until){
                for(int i=from;i<frames.size();i++)if(frames.get(i).startsWith(prefix))return frames.get(i);
                Thread.sleep(15);
            }
            throw new AssertionError("Missing "+prefix+" in "+frames);
        }
        public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception {
        try(var host=new LanHost("127.0.0.1",0);
            var a=new Peer(host.port(),"DropOwner");var b=new Peer(host.port(),"Observer")){
            a.client.ready();b.client.ready();a.await(0,"LOBBY|");
            a.client.start();a.await(0,"INFO|mission started");
            a.client.view3D();a.await(0,"ACCEPT|VIEW3D");
            b.client.view3D();b.await(0,"ACCEPT|VIEW3D");
            int van=a.mark();a.client.dropHeldEquipment();
            check(a.await(van,"REJECT|").contains("indoor floor"),"van is not a valid drop floor");
            HouseMap map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
            LanWorld world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            Object mutex=LooseClutterNetworkTest.field(host,"mutex");
            synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(4));}
            int mark=a.mark(),observer=b.mark();a.client.dropHeldEquipment();
            String packet=a.await(mark,"GEARDROP|ITEM|");
            var item=LanDroppedGear.Drop.decode(LanProtocol.parts(packet));
            check(item.owner()==0&&item.id()==1&&item.tool().equals("EMF_READER"),
                "stable unique identity and actual floor position");
            check(b.await(observer,"GEARDROP|ITEM|").equals(packet),
                "second TCP hunter receives the same physical pose");
            check(a.await(mark,"GEARSTATE|").contains("EMPTY:NONE"),
                "authoritative belt becomes empty");
            FieldEquipment[] kits=(FieldEquipment[])LooseClutterNetworkTest.field(host,"equipment");
            synchronized(mutex){check(kits[0].at(0).equals(FieldEquipment.EMPTY),"no phantom belt copy");}
            int use=a.mark();a.client.useHeldEquipment();
            check(a.await(use,"REJECT|").contains("equipment"),"empty slot cannot sample evidence");
            int thief=b.mark();b.client.pickupDroppedEquipment(1);
            check(b.await(thief,"REJECT|").contains("owns"),"other hunter cannot steal it");
            int bad=a.mark();a.client.pickupDroppedEquipment(2);
            check(a.await(bad,"REJECT|").contains("not found"),"unknown identity is rejected");
            synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(5));}
            int far=a.mark();a.client.pickupDroppedEquipment(1);
            check(a.await(far,"REJECT|").contains("physical reach"),"remote pickup is rejected");
            synchronized(mutex){check(kits[0].at(0).equals(FieldEquipment.EMPTY),
                "failed retrieval must preserve vacant slot");AuthoredTestSupport.place(map,world,0,map.spawn(4));}
            final LanWorldPanel[] holder=new LanWorldPanel[1];
            SwingUtilities.invokeAndWait(()->{
                holder[0]=new LanWorldPanel(a.client);
                holder[0].onEvent(world.bodyFrame(0,0));holder[0].onEvent(packet);
            });
            try{
                int take=a.mark(),seen=b.mark();
                SwingUtilities.invokeAndWait(()->holder[0].keyPressed(
                    new KeyEvent(holder[0],KeyEvent.KEY_PRESSED,System.currentTimeMillis(),
                        0,KeyEvent.VK_E,'E')));
                check(a.await(take,"ACCEPT|GEARPICK|").equals("ACCEPT|GEARPICK|1"),
                    "actual E key recovers item via TCP");
                check(b.await(seen,"GEARDROP|TAKE|").equals("GEARDROP|TAKE|1"),
                    "second hunter sees authoritative removal");
                check(a.await(take,"GEARSTATE|").contains("EMF_READER:I"),
                    "same instrument returns to a real belt slot");
            }finally{SwingUtilities.invokeAndWait(()->holder[0].close());}
            int twice=a.mark();a.client.pickupDroppedEquipment(1);
            check(a.await(twice,"REJECT|").contains("not found"),"duplicate pickup rejected");
            int again=a.mark();a.client.dropHeldEquipment();
            check(a.await(again,"GEARDROP|ITEM|1|").contains("|EMF_READER|"),
                "redrop preserves original identity");
            System.out.println("PASS: TCP EMF drop, empty slot, E pickup, ownership, reach, rollback and duplicate rejection");
        }
    }
}
