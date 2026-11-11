package org.beckenburg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Event-only spectral echolocation. The host sends a bounded, audible hunter
 * SOUND LOCATION, not a BODY/seat/journal stream. The LAN ghost renders rings
 * in world-space over the finished 3D scene, deliberately without the scene's
 * depth test so a sound may be sensed through walls.
 *
 * Ripples do not chase their source after the emission and expire quickly.
 */
public final class GhostSoundRipples {
    public static final int LIFE_TICKS=22,MAX_ACTIVE=12;
    private static final int STEPS=48;
    private record Timed(Ripple pulse,long receivedAt){}
    private final Deque<Timed> active=new ArrayDeque<>();

    public record Ripple(LanGhostPursuit.Sound kind,double x,double y,double z,int tick){
        public Ripple{
            Objects.requireNonNull(kind);
            if(!Double.isFinite(x+y+z)||tick<0||tick>99999999)
                throw new IllegalArgumentException("Invalid spectral sound emission");
        }
        /** Six bounded fields. No hunter ID, team map, health or private clue. */
        public String frame(){
            return "GRIPPLE|"+kind+"|"+Math.round(x*1000)+"|"+
                Math.round(y*1000)+"|"+Math.round(z*1000)+"|"+tick;
        }
        public static Ripple decode(String raw,HouseMap map){
            Objects.requireNonNull(map);
            List<String> p=LanProtocol.parts(raw);
            if(p.size()!=6||!p.get(0).equals("GRIPPLE"))
                throw new IllegalArgumentException("Invalid spectral ripple packet");
            LanGhostPursuit.Sound kind=LanGhostPursuit.Sound.valueOf(p.get(1));
            double x=LanProtocol.integer(p.get(2),0,map.width()*1000)/1000.0;
            double y=LanProtocol.integer(p.get(3),-50000,50000)/1000.0;
            double z=LanProtocol.integer(p.get(4),0,map.height()*1000)/1000.0;
            int tick=LanProtocol.integer(p.get(5),0,99999999);
            int room=map.roomAt(x,y,z);
            if(room<0||room>=HouseMap.EXTRACTION_ROOM)
                throw new IllegalArgumentException("Ripple source outside an authorized investigation zone");
            return new Ripple(kind,x,y,z,tick);
        }
    }

    public void clear(){active.clear();}
    public int size(){return active.size();}
    /** The UI accepts events only while the host says the ghost is listening. */
    public void accept(Ripple pulse,int currentTick,long receivedAt){
        Objects.requireNonNull(pulse);
        if(pulse.tick()>currentTick+3||currentTick-pulse.tick()>LIFE_TICKS)return;
        prune(receivedAt);
        if(active.size()>=MAX_ACTIVE)active.removeFirst();
        active.addLast(new Timed(pulse,receivedAt));
    }
    private void prune(long now){
        while(!active.isEmpty()&&now-active.peekFirst().receivedAt()>LIFE_TICKS*100_000_000L)
            active.removeFirst();
    }

    private record Point(double x,double y){}
    private static Point project(SceneRenderer cam,int width,int height,
                                 double worldX,double worldY,double worldZ){
        double dx=worldX-cam.x,dy=worldY-cam.eyeHeight,dz=worldZ-cam.z;
        double sin=Math.sin(cam.yaw),cos=Math.cos(cam.yaw);
        double right=-dx*cos+dz*sin,forward=dx*sin+dz*cos;
        double sp=Math.sin(cam.pitch),cp=Math.cos(cam.pitch);
        double vertical=dy*cp-forward*sp,depth=dy*sp+forward*cp;
        if(depth<=.08)return null;
        // Mirror SceneRenderer's fixed vertical lens and widescreen peripheral
        // projection; do not use a separate overhead minimap camera.
        double nativeFocal=160/Math.tan(Math.toRadians(cam.fieldOfView())*.5);
        double focal=nativeFocal*height/200.0;
        return new Point(width*.5+right/depth*focal,height*.5-vertical/depth*focal);
    }
    private static Color tint(LanGhostPursuit.Sound kind,int alpha){
        return switch(kind){
            case FOOTSTEP->new Color(116,234,255,alpha);
            case VOICE->new Color(216,164,255,alpha);
            case DOOR->new Color(255,200,116,alpha);
            case EQUIPMENT->new Color(151,255,186,alpha);
            case RITUAL->new Color(255,147,193,alpha);
        };
    }
    /** Draw AFTER scene depth/shading, BEFORE HUD: x-ray auditory rings. */
    public void draw(Graphics2D g,SceneRenderer camera,int width,int height,long now){
        if(active.isEmpty()||width<=0||height<=0)return;
        prune(now);
        if(active.isEmpty())return;
        Object previousAA=g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        java.awt.Stroke oldStroke=g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(Timed t:active){
            double age=Math.max(0,now-t.receivedAt())/1_000_000_000.0;
            double life=LIFE_TICKS*.1;
            if(age>=life)continue;
            Ripple p=t.pulse();
            // Two-to-three staggered expanding world-space rings per emission.
            for(int band=0;band<3;band++){
                double phase=age-band*.28;
                if(phase<0||phase>=life-.28*band)continue;
                double radius=.10+phase*2.0;
                int alpha=(int)Math.max(0,Math.min(210,
                    185*(1-phase/(life-.28*band))));
                Path2D.Double ring=new Path2D.Double();
                boolean open=false;
                for(int i=0;i<=STEPS;i++){
                    double theta=i*2*Math.PI/STEPS;
                    Point point=project(camera,width,height,
                        p.x()+Math.cos(theta)*radius,p.y()+1.08,
                        p.z()+Math.sin(theta)*radius);
                    if(point==null){open=false;continue;}
                    if(!open){ring.moveTo(point.x(),point.y());open=true;}
                    else ring.lineTo(point.x(),point.y());
                }
                g.setStroke(new BasicStroke(4.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(tint(p.kind(),Math.max(0,alpha/4)));
                g.draw(ring);
                g.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(tint(p.kind(),alpha));
                g.draw(ring);
            }
            // Brief source-point shimmer: never a persistent hunter marker.
            Point centre=project(camera,width,height,p.x(),p.y()+1.08,p.z());
            if(centre!=null&&age<.65){
                int alpha=(int)(150*(1-age/.65));
                g.setColor(tint(p.kind(),Math.max(0,alpha)));
                g.fillOval((int)centre.x()-3,(int)centre.y()-3,6,6);
            }
        }
        g.setStroke(oldStroke);
        if(previousAA!=null)g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,previousAA);
        else g.getRenderingHints().remove(RenderingHints.KEY_ANTIALIASING);
    }
}
