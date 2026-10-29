package org.beckenburg;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Real socket proof of the START ROUND prerequisites and actual 3D transition. */
public final class LanOwnerReadyAckTest {
    private static String until(LinkedBlockingQueue<String> frames,String prefix)throws Exception{
        long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<limit){
            String message=frames.poll(150,TimeUnit.MILLISECONDS);
            if(message!=null&&message.startsWith(prefix))return message;
        }
        throw new AssertionError("Host never delivered "+prefix);
    }
    public static void main(String[] args)throws Exception {
        var frames=new LinkedBlockingQueue<String>();
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(1));
            var owner=new LanClient("127.0.0.1",host.port(),false,"Solo hunter",frames::offer)){
            if(!"H1".equals(owner.seat()))throw new AssertionError("No real owner seat");
            owner.ready();
            if(!"OWNREADY|YES".equals(until(frames,"OWNREADY|")))
                throw new AssertionError("No authoritative owner readiness acknowledgement");
            owner.start();
            until(frames,"INFO|mission started");
            if(!host.running())throw new AssertionError("Start Round never started the simulation");
            owner.view3D();
            until(frames,"ACCEPT|VIEW3D");
            until(frames,"SITE|HOUSE");
            System.out.println("PASS host acknowledged READY, START launched and actual 3D world entered");
        }
    }
}
