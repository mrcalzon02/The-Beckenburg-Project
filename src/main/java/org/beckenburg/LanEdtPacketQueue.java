package org.beckenburg;

import javax.swing.SwingUtilities;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Hand network events to Swing without making each position update a separate
 * EDT task. Only contiguous, well-formed BODY snapshots may be replaced;
 * every non-BODY packet is an ordering barrier and is delivered exactly once.
 */
final class LanEdtPacketQueue {
    private static final int MAX_BATCH = 32;
    private static final class Packet {
        String frame;
        final int seat;
        Packet(String frame,int seat){this.frame=frame;this.seat=seat;}
    }
    private final Consumer<String> receiver;
    private final ArrayDeque<Packet> pending=new ArrayDeque<>();
    private final Packet[] trailingBodies=new Packet[6];
    private boolean scheduled;
    private long coalesced;

    LanEdtPacketQueue(Consumer<String> receiver){this.receiver=Objects.requireNonNull(receiver);}

    private static int validBodySeat(String frame){
        if(!frame.startsWith("BODY|"))return -1;
        String[] p=frame.split("\\|",-1);
        if(p.length!=7)return -1;
        try{
            int seat=Integer.parseInt(p[1]);
            int x=Integer.parseInt(p[2]),y=Integer.parseInt(p[3]),z=Integer.parseInt(p[4]);
            int heading=Integer.parseInt(p[5]),tick=Integer.parseInt(p[6]);
            if(seat<0||seat>=6||x<0||z<0||y< -50000||y>50000
                    ||heading<0||heading>=360||tick<0||tick>99999999)return -1;
            return seat;
        }catch(NumberFormatException invalid){return -1;}
    }

    void post(String frame){
        Objects.requireNonNull(frame);
        synchronized(pending){
            int seat=validBodySeat(frame);
            if(seat<0){
                Arrays.fill(trailingBodies,null); // Door/evidence/status barrier.
                pending.addLast(new Packet(frame,-1));
            }else{
                Packet existing=trailingBodies[seat];
                if(existing!=null){
                    pending.remove(existing);
                    existing.frame=frame;
                    pending.addLast(existing);
                    coalesced++;
                }else{
                    Packet packet=new Packet(frame,seat);
                    pending.addLast(packet);
                    trailingBodies[seat]=packet;
                }
            }
            if(!scheduled){scheduled=true;SwingUtilities.invokeLater(this::drain);}
        }
    }

    private void drain(){
        if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Network UI must drain on EDT");
        try{
            for(int n=0;n<MAX_BATCH;n++){
                String next;
                synchronized(pending){
                    Packet packet=pending.pollFirst();
                    if(packet==null)break;
                    if(packet.seat>=0&&trailingBodies[packet.seat]==packet)
                        trailingBodies[packet.seat]=null;
                    next=packet.frame;
                }
                receiver.accept(next);
            }
        }finally{
            synchronized(pending){
                if(pending.isEmpty())scheduled=false;
                else SwingUtilities.invokeLater(this::drain); // Yield to pending input/render events.
            }
        }
    }

    long coalesced(){synchronized(pending){return coalesced;}}
}
