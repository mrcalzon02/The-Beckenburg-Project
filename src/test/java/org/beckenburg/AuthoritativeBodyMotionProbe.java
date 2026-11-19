package org.beckenburg;

import java.util.List;

/**
 * Physical benchmark acceptance from the host's own BODY packets, not from
 * deprecated per-step acknowledgments or locally predicted camera positions.
 * Network callback owns this state; the test reader gets synchronized snapshots.
 */
final class AuthoritativeBodyMotionProbe {
    private final int seat;
    private boolean known;
    private double x,z,metres;
    private int samples;
    AuthoritativeBodyMotionProbe(int seat){
        if(seat<0||seat>5)throw new IllegalArgumentException("Invalid measured hunter seat");
        this.seat=seat;
    }
    synchronized void accept(String frame,boolean moving){
        if(!frame.startsWith("BODY|"))return;
        List<String> parts=LanProtocol.parts(frame);
        if(parts.size()!=7)throw new IllegalArgumentException("Invalid authoritative BODY");
        int sender=LanProtocol.integer(parts.get(1),0,5);
        if(sender!=seat)return;
        double nx=LanProtocol.integer(parts.get(2),0,1_000_000)/1000.0;
        double nz=LanProtocol.integer(parts.get(4),0,1_000_000)/1000.0;
        LanProtocol.integer(parts.get(3),-50000,50000);
        LanProtocol.integer(parts.get(5),0,359);
        LanProtocol.integer(parts.get(6),0,99999999);
        if(known&&moving){
            double distance=Math.hypot(nx-x,nz-z);
            // Exclude unchanged host snapshots and site-changing teleports.
            // A legal 100ms sprint step is <.5m; allow some accumulated
            // packet interval, without counting a 5m teleport as walking.
            if(distance>=.006&&distance<=.90){
                metres+=distance;
                samples++;
            }
        }
        known=true;x=nx;z=nz;
    }
    synchronized int samples(){return samples;}
    synchronized double metres(){return metres;}
}
