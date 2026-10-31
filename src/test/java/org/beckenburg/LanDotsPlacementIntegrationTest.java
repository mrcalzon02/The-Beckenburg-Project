package org.beckenburg;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.swing.SwingUtilities;

/** Real TCP + actual 3D input: rack -> DOTS preview -> host placement -> replicated projector. */
public final class LanDotsPlacementIntegrationTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Peer implements AutoCloseable {
        final List<String> frames=new CopyOnWriteArrayList<>();
        final LanClient client;
        Peer(int port,String name)throws Exception {
            client=new LanClient("127.0.0.1",port,false,name,frames::add);
        }
        int mark(){return frames.size();}
        String await(int from,Predicate<String> match,String what)throws Exception {
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
            while(System.nanoTime()<until){
                for(int at=from;at<frames.size();at++)if(match.test(frames.get(at)))
                    return frames.get(at);
                Thread.sleep(15);
            }
            throw new AssertionError("Missing "+what+"; frames "+frames.subList(from,frames.size()));
        }
        String await(int from,String prefix)throws Exception{
            return await(from,s->s.startsWith(prefix),prefix);
        }
        @Override public void close(){client.close();}
    }
    private static void press(LanWorldPanel panel,int key)throws Exception {
        SwingUtilities.invokeAndWait(()->panel.keyPressed(
            new KeyEvent(panel,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),
                0,key,KeyEvent.CHAR_UNDEFINED)));
    }
    private static void click(LanWorldPanel panel)throws Exception {
        SwingUtilities.invokeAndWait(()->{
            MouseEvent click=new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),0,310,240,1,false,MouseEvent.BUTTON1);
            for(var listener:panel.getMouseListeners())listener.mousePressed(click);
        });
    }
    private static void rotate(LanWorldPanel panel,int steps)throws Exception {
        SwingUtilities.invokeAndWait(()->{
            MouseWheelEvent wheel=new MouseWheelEvent(panel,MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),0,310,240,0,false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,1,steps);
            for(var listener:panel.getMouseWheelListeners())listener.mouseWheelMoved(wheel);
        });
    }
    private static void feed(LanWorldPanel panel,String frame)throws Exception{
        SwingUtilities.invokeAndWait(()->panel.onEvent(frame));
    }
    private static boolean state(LanWorldPanel panel,String field)throws Exception {
        return (boolean)LooseClutterNetworkTest.field(panel,field);
    }

    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0);
            var a=new Peer(host.port(),"Projector");
            var b=new Peer(host.port(),"Observer");
            var c=new Peer(host.port(),"Witness")){
            a.client.ready();b.client.ready();c.client.ready();
            a.await(0,"LOBBY|3|AI|3|WAITING");
            int started=a.mark();a.client.start();a.await(started,"INFO|mission started");
            int view=a.mark();a.client.view3D();a.await(view,"ACCEPT|VIEW3D");
            int observerView=b.mark();b.client.view3D();b.await(observerView,"ACCEPT|VIEW3D");
            HouseMap map=(HouseMap)LooseClutterNetworkTest.field(host,"map");
            LanWorld world=(LanWorld)LooseClutterNetworkTest.field(host,"world");
            Object mutex=LooseClutterNetworkTest.field(host,"mutex");
            var rack=map.van().location(VanStaging.Station.EQUIPMENT);
            synchronized(mutex){
                AuthoredTestSupport.place(map,world,0,
                    new LevelDefinition.Vec(rack.x(),VanStaging.FLOOR_Y,rack.z()));
            }
            int equipped=a.mark();a.client.equipAt(0,"DOTS_PROJECTOR");
            a.await(equipped,"ACCEPT|GEAREQUIP|0|DOTS_PROJECTOR");
            int stateMark=a.mark();a.client.equipmentState();
            String equipment=a.await(stateMark,"GEARSTATE|");
            check(equipment.contains("DOTS_PROJECTOR:I"),
                "host must include projector in selected four-slot belt");
            final LanWorldPanel[] holder=new LanWorldPanel[2];
            SwingUtilities.invokeAndWait(()->{
                holder[0]=new LanWorldPanel(a.client);
                holder[1]=new LanWorldPanel(b.client);
            });
            LanWorldPanel panel=holder[0],observerPanel=holder[1];
            try{
                feed(panel,equipment);
                press(panel,KeyEvent.VK_G);
                check(state(panel,"toolPlacement"),"G must open physical placement preview");
                // A rejected van placement must not dismiss the preview.
                int deniedAt=a.mark();click(panel);
                String rejected=a.await(deniedAt,s->s.startsWith("REJECT|")
                    ||s.startsWith("ACCEPT|GEARUSE|"),"van placement response");
                check(rejected.startsWith("REJECT|")
                    &&rejected.contains("requires a room"),
                    "host must reject projector placement in van: "+rejected);
                feed(panel,rejected);
                check(state(panel,"toolPlacement")&&!state(panel,"placementPending"),
                    "failed placement must stay aimable/retryable");
                // Slot changes are allowed after a host rejection and must
                // clear any obsolete aim/ack state before the next attempt.
                press(panel,KeyEvent.VK_2);
                check(!state(panel,"toolPlacement")&&!state(panel,"placementPending"),
                    "switching away must cancel the prior projector preview");
                press(panel,KeyEvent.VK_1);
                press(panel,KeyEvent.VK_G);
                check(state(panel,"toolPlacement"),
                    "reselecting the projector must allow a fresh placement");
                LanDotsProjectors dots=(LanDotsProjectors)LooseClutterNetworkTest.field(host,"dots");
                check(dots.at(0)==null,
                    "rejected van placement must not create host-owned projector geometry");

                synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(4));}
                rotate(panel,6); // Six 15-degree wheel steps => +90 degrees relative to hunter facing.
                final int expectedHeading;
                synchronized(mutex){
                    expectedHeading=Math.floorMod(world.body(0).heading()+90,360);
                    check(expectedHeading!=world.body(0).heading(),
                        "wheel rotation must change the actual host-relative heading");
                }
                int sent=a.mark(),observer=b.mark();
                click(panel);
                String outcome=a.await(sent,s->s.startsWith("ACCEPT|GEARUSE|")
                    ||s.startsWith("REJECT|"),"host placed projector");
                check(outcome.startsWith("ACCEPT|GEARUSE|DOTS_PROJECTOR|")
                    &&outcome.contains("Projector placed"),"actual multiplayer deployment: "+outcome);
                String placement=a.await(sent,"DOTS|PLACE|0|4|");
                String powered=a.await(sent,"DOTS|POWER|0|1");
                check(b.await(observer,"DOTS|PLACE|0|4|").equals(placement),
                    "teammate must receive same deployed projector pose");
                check(c.await(0,"DOTS|PLACE|0|4|").equals(placement),
                    "third hunter must receive the same authoritative projector transform");
                check(placement.endsWith("|I|"+expectedHeading),
                    "projector orientation must add wheel rotation to host facing: "+placement);
                feed(panel,placement);feed(panel,powered);feed(panel,outcome);
                check(!state(panel,"toolPlacement")&&!state(panel,"placementPending"),
                    "accepted placement dismisses the aim preview");
                SceneRenderer renderer=(SceneRenderer)LooseClutterNetworkTest.field(panel,"renderer");
                feed(observerPanel,placement);feed(observerPanel,powered);
                SceneRenderer observerRenderer=(SceneRenderer)LooseClutterNetworkTest.field(observerPanel,"renderer");
                check(renderer.dotsPositions.containsKey(0)
                    &&renderer.dotsPowered.getOrDefault(0,false),
                    "placing hunter's 3D renderer must show the powered projector");
                check(observerRenderer.dotsPositions.containsKey(0)
                    &&observerRenderer.dotsPowered.getOrDefault(0,false)
                    &&observerRenderer.dotsPositions.get(0).equals(renderer.dotsPositions.get(0))
                    &&observerRenderer.dotsPositions.get(0).heading()==expectedHeading,
                    "second hunter's 3D renderer must show the host-relative rotated pose");
                check(dots.at(0)!=null&&dots.at(0).room()==4
                    &&dots.at(0).heading()==expectedHeading,
                    "host must own real positioned, rotated DOTS geometry");
                // Every placed model is the real approved tier-specific deployed
                // equipment, never the old generic vape prop.
                for(var tier:EquipmentTiers.Tier.values()){
                    String mesh=SceneRenderer.dotsDeployedMesh(tier);
                    check(mesh.equals(EquipmentModelCatalog.variant("DOTS_PROJECTOR",tier,"deployed"))
                        &&EquipmentModelCatalog.approved(mesh)
                        &&!GlbProps.model(mesh).triangles().isEmpty()
                        &&Double.isFinite(SceneRenderer.castleBounds(mesh)[1]),
                        "actual deployed DOTS GLB must exist and have measurable floor bounds: "+tier);
                }

                // A hunter may not remotely reclaim a deployed projector from another room.
                synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(5));}
                press(panel,KeyEvent.VK_G);
                int remoteAt=a.mark(),remoteObserver=b.mark();
                click(panel);
                String remoteDenied=a.await(remoteAt,s->s.startsWith("REJECT|")
                    ||s.startsWith("ACCEPT|GEARUSE|"),"out-of-reach retrieval response");
                check(remoteDenied.startsWith("REJECT|")
                    &&remoteDenied.contains("physical location"),
                    "host must refuse remote projector retrieval: "+remoteDenied);
                feed(panel,remoteDenied);
                check(state(panel,"toolPlacement")&&!state(panel,"placementPending"),
                    "remote retrieval rejection must preserve the aiming preview");
                synchronized(mutex){
                    FieldEquipment[] kits=(FieldEquipment[])LooseClutterNetworkTest.field(host,"equipment");
                    check(dots.at(0)!=null&&dots.at(0).room()==4
                        &&kits[0].powered("DOTS_PROJECTOR")
                        &&kits[0].at(0).equals("DOTS_PROJECTOR"),
                        "rejected remote retrieval must preserve host geometry, power and belt ownership");
                    AuthoredTestSupport.place(map,world,0,map.spawn(4));
                }
                check(b.frames.subList(remoteObserver,b.frames.size()).stream()
                    .noneMatch(s->s.startsWith("DOTS|CLEAR|0")),
                    "rejected remote retrieval must never broadcast a phantom clear");

                // Empty batteries must not trap deployed evidence hardware in the world.
                synchronized(mutex){
                    FieldEquipment[] kits=(FieldEquipment[])LooseClutterNetworkTest.field(host,"equipment");
                    @SuppressWarnings("unchecked")
                    java.util.Map<String,Integer> battery=(java.util.Map<String,Integer>)
                        LooseClutterNetworkTest.field(kits[0],"batteryTicks");
                    battery.put("DOTS_PROJECTOR",0);
                }
                // Preview is still open from the denied remote retrieval; return and click.
                int retrieveAt=a.mark(),retrieveObserver=b.mark();
                click(panel);
                String retrieval=a.await(retrieveAt,s->s.startsWith("ACCEPT|GEARUSE|")
                    ||s.startsWith("REJECT|"),"recover drained placed projector");
                check(retrieval.startsWith("ACCEPT|GEARUSE|DOTS_PROJECTOR|")
                    &&retrieval.contains("Projector retrieved"),
                    "even an exhausted projector remains physically retrievable: "+retrieval);
                String cleared=a.await(retrieveAt,"DOTS|CLEAR|0");
                check(b.await(retrieveObserver,"DOTS|CLEAR|0").equals(cleared),
                    "second hunter must receive the authoritative retrieval event");
                feed(panel,cleared);feed(panel,retrieval);
                feed(observerPanel,cleared);
                synchronized(mutex){
                    FieldEquipment[] kits=(FieldEquipment[])LooseClutterNetworkTest.field(host,"equipment");
                    check(dots.at(0)==null&&!kits[0].powered("DOTS_PROJECTOR")
                        &&kits[0].at(0).equals("DOTS_PROJECTOR")
                        &&kits[0].owned("DOTS_PROJECTOR")!=null,
                        "retrieval must conserve the original belt item and turn off its host power");
                }
                check(!renderer.dotsPositions.containsKey(0)
                    &&!observerRenderer.dotsPositions.containsKey(0),
                    "retrieved projector must disappear from both hunters' real 3D renderers");
            }finally{SwingUtilities.invokeAndWait(()->{panel.close();observerPanel.close();});}
            System.out.println("PASS: LAN DOTS rack/equip, rejected placement/remote retrieval, mouse rotation, three-client replication, two renderers and exhausted recovery");
        }
    }
}
