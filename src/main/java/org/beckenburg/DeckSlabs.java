package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Opt-in structural deck depth, shared by rendering and physical collision.
 * The authored walking elevation stays fixed; thickness extends downward.
 */
public final class DeckSlabs {
    private DeckSlabs(){}
    public record Cell(double x0,double z0,double x1,double z1,double top,double bottom){}
    public static double thickness(LevelDefinition l,Room r){
        double depth=l.sockets().stream().filter(s->s.room()==r.id()&&s.type().equals("deck_slab"))
            .mapToDouble(s->Double.parseDouble(s.style())).findFirst().orElse(0);
        if(!Double.isFinite(depth)||depth<0||depth>.5)throw new IllegalArgumentException("deck slab depth");
        return depth;
    }
    public static boolean solid(LevelDefinition l,Room r,double x,double z){
        if(x<=r.x0()||x>=r.x1()||z<=r.z0()||z>=r.z1()||l.floorVoid(x,r.y(),z))return false;
        for(var s:l.sockets())if(s.room()==r.id()&&s.type().equals("floor_cutout")){
            var shape=s.style().split(":");
            if(Math.abs(x-s.x())<Double.parseDouble(shape[0])/2&&Math.abs(z-s.z())<Double.parseDouble(shape[1])/2)return false;
        }
        return true;
    }
    public static List<Cell> cells(LevelDefinition l,Room r){
        double depth=thickness(l,r);if(depth==0)return List.of();
        var xs=new TreeSet<Double>();var zs=new TreeSet<Double>();
        xs.add(r.x0());xs.add(r.x1());zs.add(r.z0());zs.add(r.z1());
        for(var v:l.voids())if(Math.abs(v.y()-r.y())<.05){
            for(double x:new double[]{v.x0(),v.x1()})if(x>r.x0()&&x<r.x1())xs.add(x);
            for(double z:new double[]{v.z0(),v.z1()})if(z>r.z0()&&z<r.z1())zs.add(z);
        }
        for(var s:l.sockets())if(s.room()==r.id()&&s.type().equals("floor_cutout")){
            var shape=s.style().split(":");double hx=Double.parseDouble(shape[0])/2,hz=Double.parseDouble(shape[1])/2;
            for(double x:new double[]{s.x()-hx,s.x()+hx})if(x>r.x0()&&x<r.x1())xs.add(x);
            for(double z:new double[]{s.z()-hz,s.z()+hz})if(z>r.z0()&&z<r.z1())zs.add(z);
        }
        var xx=new ArrayList<>(xs);var zz=new ArrayList<>(zs);var out=new ArrayList<Cell>();
        for(int i=0;i<xx.size()-1;i++)for(int j=0;j<zz.size()-1;j++){
            double x0=xx.get(i),x1=xx.get(i+1),z0=zz.get(j),z1=zz.get(j+1);
            if(solid(l,r,(x0+x1)/2,(z0+z1)/2))out.add(new Cell(x0,z0,x1,z1,r.y(),r.y()-depth));
        }
        return List.copyOf(out);
    }
    /** During the final tread the player capsule overlaps the solid landing
     * before its feet can rise to deck height. The exception is confined to a
     * genuine authored stair, its supporting ramp, and its immediately adjacent
     * floor opening (18 cm rearward, not a free pass through the deck).
     */
    private static boolean stairLandingContact(LevelDefinition level,Room room,
                                               double x,double feetY,double z,double radius){
        double top=room.y();
        if(feetY<top-.205||feetY>top+.015)return false;
        for(Stair stair:level.stairs()){
            if(stair.b()!=room.id()||Math.abs(stair.y1()-top)>.025
                    ||!stair.contains(x,z,radius)
                    ||Math.abs(stair.yAt(x,z)-feetY)>=.035)continue;
            double dx=stair.x1()-stair.x0(),dz=stair.z1()-stair.z0();
            double length=Math.hypot(dx,dz);
            if(length>.01&&(level.floorVoid(x,top,z)
                    ||level.floorVoid(x-.18*dx/length,top,z-.18*dz/length)))
                return true;
        }
        return false;
    }
    /**
     * Actual body collision against the same authored deck thickness and
     * physical cutout cells consumed by levelDeck(). No decorative-only floor.
     * Test cardinal capsule probes as well as the centre to prevent a player
     * squeezing through a slab edge or floor opening.
     */
    public static boolean blocksBody(LevelDefinition level,double x,double feetY,
                                     double z,double bodyHeight,double radius){
        for(var socket:level.sockets()){
            if(!socket.type().equals("deck_slab"))continue;
            Room room=level.room(socket.room());
            double depth=thickness(level,room),top=room.y(),bottom=top-depth;
            if(depth<=0||feetY>=top-.02||feetY+bodyHeight<=bottom+.02)continue;
            // Within the last 20 cm of a real riser, the capsule's 22 cm
            // radial footprint necessarily crosses the first intact deck
            // cells before the feet reach the landing. Permit that narrow
            // overlap only while the body is on a source-authored stair,
            // supported by its actual sloped surface, inside the real deck
            // aperture. A nearby unsupported player still collides with it.
            boolean terminalTransition=stairLandingContact(
                level,room,x,feetY,z,radius);
            if(terminalTransition)continue;
            for(double[] offset:new double[][]{{0,0},{radius,0},{-radius,0},{0,radius},{0,-radius}})
                if(solid(level,room,x+offset[0],z+offset[1]))return true;
        }
        return false;
    }
    /** Optical rays and the game capsule test the same solid deck cells. */
    public static boolean blocksRay(LevelDefinition level,double x,double y,double z){
        for(var socket:level.sockets()){
            if(!socket.type().equals("deck_slab"))continue;
            Room room=level.room(socket.room());
            double depth=thickness(level,room);
            if(depth>0&&y>room.y()-depth+.005&&y<room.y()-.005
                &&solid(level,room,x,z))return true;
        }
        return false;
    }
    public static LevelDefinition physical(LevelDefinition l){
        var colliders=new ArrayList<>(l.colliders());
        for(var r:l.rooms())for(var c:cells(l,r))colliders.add(new Collider((c.x0()+c.x1())/2,c.bottom(),(c.z0()+c.z1())/2,(c.x1()-c.x0())/2,(c.z1()-c.z0())/2,c.top()-c.bottom(),0));
        return new LevelDefinition(l.id(),l.revision()+"-solid-decks",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),l.sockets(),colliders,l.dock(),l.ambience());
    }
    /** A swept capsule may overlap the final riser while its feet follow the
     * authored stair onto the deck. This grants no passage through other edges. */
    public static boolean landingContact(LevelDefinition l,Collider c,double x,double y,double z){
        double top=c.y()+c.height();
        if(c.yaw()!=0||top-y<0||top-y>.205)return false;
        for(var s:l.sockets())if(s.type().equals("deck_slab")
                &&Math.abs(l.room(s.room()).y()-top)<.0001
                &&Math.abs(Double.parseDouble(s.style())-c.height())<.0001
                &&stairLandingContact(l,l.room(s.room()),x,y,z,.22))return true;
        return false;
    }
}
