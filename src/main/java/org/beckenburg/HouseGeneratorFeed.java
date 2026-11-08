package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Visible generator service along the site edge, sleeved below the pedestrian crossing. */
final class HouseGeneratorFeed {
    private HouseGeneratorFeed(){}
    static LevelDefinition source(){
        var base=HouseRebuildFitout.source();var nodes=new ArrayList<>(base.sockets());nodes.removeIf(s->s.id().startsWith("BHR-SOURCE-"));
        var terrain=WorldTerrainSurface.of(base);var old=base.sockets().stream().filter(s->s.id().equals("BHR-SOURCE-0")).findFirst().orElseThrow();
        double x=old.x(),z=old.z()+.04,ground=height(terrain,x,z);
        HouseRebuildFitout.run(nodes,"BHR-OUTLET",13,"black","source-terminal",new Vec(old.x(),old.y(),old.z()),new Vec(x,old.y(),z),new Vec(x,ground+.012,z));
        ground(nodes,terrain,"BHR-FEED-EDGE",x,z,x,20.8);
        ground(nodes,terrain,"BHR-FEED-APPROACH",x,20.8,24.9,20.8);
        // The arrival/garden path remains free of a cable-sized trip/barrier.
        HouseRebuildFitout.run(nodes,"BHR-CROSSING-SLEEVE",13,"black","buried-path-sleeve",new Vec(24.9,height(terrain,24.9,20.8)+.012,20.8),new Vec(24.9,-.35,20.8),new Vec(22.6,-.35,20.8),new Vec(22.6,height(terrain,22.6,20.8)+.012,20.8));
        ground(nodes,terrain,"BHR-FEED-HOUSE",22.6,20.8,21.692,20.8);
        ground(nodes,terrain,"BHR-FEED-WALL",21.692,20.8,21.692,10.9);
        HouseRebuildFitout.run(nodes,"BHR-INLET",12,"black","wall-entry",new Vec(21.692,height(terrain,21.692,10.9)+.012,10.9),new Vec(21.692,.45,10.9),new Vec(21.508,.45,10.9),new Vec(21.508,-.14,10.9),new Vec(21.508,-.14,10.092),new Vec(15.092,-.14,10.092),new Vec(15.092,-.14,10.76),new Vec(15.092,-.66,10.76));
        return new LevelDefinition(base.id(),"first-principles-visible-feed-v1",base.title(),base.rooms(),base.portals(),base.stairs(),base.voids(),base.props(),nodes,base.colliders(),base.dock(),base.ambience(),base.water(),base.sounds());
    }
    private static double height(AuthoredSitePhysics t,double x,double z){return HouseRebuildLandscape.height(t,x,z);}
    private static void ground(List<Socket> nodes,AuthoredSitePhysics t,String id,double ax,double az,double bx,double bz){
        var points=new ArrayList<Vec>();points.add(new Vec(ax,height(t,ax,az)+.012,az));sample(points,t,ax,az,bx,bz,0);
        HouseRebuildFitout.run(nodes,id,13,"black","ground-cable",points.toArray(Vec[]::new));
    }
    private static void sample(List<Vec> out,AuthoredSitePhysics t,double ax,double az,double bx,double bz,int depth){
        double mx=(ax+bx)/2,mz=(az+bz)/2,ya=height(t,ax,az),yb=height(t,bx,bz),ym=height(t,mx,mz);
        if(depth<12&&(Math.hypot(bx-ax,bz-az)>1||Math.abs(ym-(ya+yb)/2)>.001)){
            sample(out,t,ax,az,mx,mz,depth+1);sample(out,t,mx,mz,bx,bz,depth+1);
        }else out.add(new Vec(bx,yb+.012,bz));
    }
}
