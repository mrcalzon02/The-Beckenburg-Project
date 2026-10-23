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
    public static LevelDefinition physical(LevelDefinition l){
        var colliders=new ArrayList<>(l.colliders());
        for(var r:l.rooms())for(var c:cells(l,r))colliders.add(new Collider((c.x0()+c.x1())/2,c.bottom(),(c.z0()+c.z1())/2,(c.x1()-c.x0())/2,(c.z1()-c.z0())/2,c.top()-c.bottom(),0));
        return new LevelDefinition(l.id(),l.revision()+"-solid-decks",l.title(),l.rooms(),l.portals(),l.stairs(),l.voids(),l.props(),l.sockets(),colliders,l.dock(),l.ambience());
    }
    /** A swept capsule may overlap the final riser while its feet follow the
     * authored stair onto the deck. This grants no passage through other edges. */
    public static boolean landingContact(LevelDefinition l,Collider c,double x,double y,double z){
        double top=c.y()+c.height();
        if(c.yaw()!=0||top-y<0||top-y>.20)return false;
        for(var s:l.sockets())if(s.type().equals("deck_slab")&&Math.abs(l.room(s.room()).y()-top)<.0001
                &&Math.abs(Double.parseDouble(s.style())-c.height())<.0001)
            for(var stair:l.stairs())if(stair.b()==s.room()&&stair.contains(x,z,0)
                    &&Math.abs(stair.yAt(x,z)-y)<.025&&Math.abs(stair.y1()-top)<.0001)return true;
        return false;
    }
}
