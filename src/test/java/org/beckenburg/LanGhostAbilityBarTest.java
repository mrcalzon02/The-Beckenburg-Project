package org.beckenburg;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** Two real seats. An ability keeps its key when other slots are locked or cooling down. */
public final class LanGhostAbilityBarTest {
    private static String await(LinkedBlockingQueue<String> frames,String prefix)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<deadline){
            String frame=frames.poll(100,TimeUnit.MILLISECONDS);
            if(frame!=null&&frame.startsWith(prefix))return frame;
        }
        throw new AssertionError("Missing "+prefix);
    }
    public static void main(String[] args)throws Exception{
        var network=new LinkedBlockingQueue<String>();
        var leak=new AtomicBoolean();
        var view=new AtomicReference<LanGhostPanel>();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SHADE,CustomContract.standard(1));
            var hunter=new LanClient("127.0.0.1",host.port(),false,"Hunter",line->{
                if(line.startsWith("GABILITIES|"))leak.set(true);
            });
            var ghost=new LanClient("127.0.0.1",host.port(),true,"Ghost",line->{
                SwingUtilities.invokeLater(()->{if(view.get()!=null)view.get().onEvent(line);});
                network.offer(line);
            })){
            SwingUtilities.invokeAndWait(()->view.set(new LanGhostPanel(ghost)));
            try{
                hunter.ready();ghost.ready();await(network,"LOBBY|1|HUMAN|2|WAITING");
                hunter.start();await(network,"INFO|mission started");
                ghost.viewGhost3D();await(network,"GPOSE|");
                var initial=GhostAbilityBar.decode(LanProtocol.parts(await(network,"GABILITIES|")));
                if(!initial.slots().stream().map(e->e.slot().key()).toList().equals(List.of(1,2,3,4,5,6,8)))
                    throw new AssertionError("Shade loadout, absent Claw must not shift Dread");
                SwingUtilities.invokeAndWait(()->view.get().keyPressed(new KeyEvent(view.get(),
                    KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_2,'2')));
                await(network,"ACCEPT|GHOST|DISTURB|");
                var after=GhostAbilityBar.decode(LanProtocol.parts(await(network,"GABILITIES|")));
                var disturb=after.slots().get(1);
                if(!disturb.used()||disturb.remaining()<1||disturb.state()!=GhostAbilityBar.State.COOLDOWN)
                    throw new AssertionError("Host must report executed ability and actual cooldown");
                if(leak.get())throw new AssertionError("Private ability bar sent to a hunter");
            }finally{SwingUtilities.invokeAndWait(()->view.getAndSet(null).close());}
        }
        System.out.println("PASS ghost-only Shade loadout, stable action key, successful cast and cooldown");
    }
}
