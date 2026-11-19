package org.beckenburg;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/** Ordered, lossless gameplay control with opt-in BODY-only deduplication. */
public final class LanEdtPacketQueueTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static String body(int seat,int mm){return "BODY|"+seat+"|"+mm+"|0|1200|180|1";}
    private static void run(Runnable task)throws Exception{SwingUtilities.invokeAndWait(task);}
    public static void main(String[] args)throws Exception{
        List<String> received=new ArrayList<>();
        LanEdtPacketQueue queue=new LanEdtPacketQueue(frame->{
            check(SwingUtilities.isEventDispatchThread(),"network packets must be applied on EDT");
            received.add(frame);
        });
        run(()->{
            queue.post(body(0,100));queue.post(body(1,100));queue.post(body(0,200));
            queue.post("DOORS|open");
            queue.post(body(0,300));queue.post(body(0,400));
            queue.post("BODY|0|INVALID|0|1200|180|1"); // malformed frame must not be hidden.
            queue.post("GHEAR|evidence");queue.post("INFO|mission started");
        });
        run(()->{});
        check(received.equals(List.of(body(1,100),body(0,200),"DOORS|open",
                body(0,400),"BODY|0|INVALID|0|1200|180|1",
                "GHEAR|evidence","INFO|mission started")),
            "latest BODY per seat, non-body barriers, and malformed packets must retain order: "+received);
        check(queue.coalesced()==2,"two redundant BODY events coalesced");
        received.clear();
        run(()->{
            for(int i=0;i<100;i++)queue.post("INFO|"+i);
            SwingUtilities.invokeLater(()->received.add("INPUT-MARK"));
        });
        for(int i=0;i<6;i++)run(()->{});
        check(received.size()==101&&received.get(100).equals("INFO|99"),
            "all non-body events must be delivered without loss");
        check(received.get(32).equals("INPUT-MARK"),
            "EDT must yield after first 32 packets so an input/render callback can run");
        System.out.println("PASS: ordered status barriers, BODY coalescing, malformed frames, bounded EDT batches");
    }
}
