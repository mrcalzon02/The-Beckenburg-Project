package org.beckenburg.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Authoritative *query* geometry for an isolated, stitched and layered terrain.
 * Independent of render LOD or triangle count. Not connected to HouseMap yet.
 *
 * The source surface is absent wherever AdaptiveTerrainTopology has cut leaves.
 * Other floors exist at explicit stable layer IDs, even at identical X/Z.
 * Solids supply conservative axis-aligned obstructions and overhead clearance.
 */
public final class LayeredTerrainQueries {
    public static final String SURFACE = "surface";
    public record Floor(String layer, double x0, double z0, double x1, double z1,
                        double y0, double riseX, double riseZ) {
        public Floor {
            if (layer == null || layer.isBlank() || layer.equals(SURFACE)
                || !finite(x0,z0,x1,z1,y0,riseX,riseZ)
                || x1 <= x0 || z1 <= z0)
                throw new IllegalArgumentException("Invalid independent floor");
        }
        public boolean contains(double x,double z) {
            return x>=x0 && x<x1 && z>=z0 && z<z1;
        }
        public double height(double x,double z) {
            return y0+(x-x0)*riseX+(z-z0)*riseZ;
        }
    }
    /** Solid colliders, not decor: walls, ceilings, supporting stone, equipment. */
    public record Solid(String id,double x0,double y0,double z0,
                        double x1,double y1,double z1) {
        public Solid {
            if(id==null||id.isBlank()||!finite(x0,y0,z0,x1,y1,z1)
                || x1<=x0||y1<=y0||z1<=z0)
                throw new IllegalArgumentException("Invalid solid geometry");
        }
    }
    public record Agent(double radius,double height,double maxSlopeDegrees,
                        double maxStep) {
        public Agent {
            if(!finite(radius,height,maxSlopeDegrees,maxStep)||radius<=0
                    ||radius>3||height<=0||height>8||maxSlopeDegrees<=0
                    ||maxSlopeDegrees>=89||maxStep<0||maxStep>3)
                throw new IllegalArgumentException("Invalid navigation agent shape");
        }
        double slope() {return Math.tan(Math.toRadians(maxSlopeDegrees));}
    }
    public record Support(String layer,double x,double y,double z) {}
    public record Rect(double x0,double z0,double x1,double z1) {
        public Rect {
            if(!finite(x0,z0,x1,z1)||x1<=x0||z1<=z0)
                throw new IllegalArgumentException("Invalid world area");
        }
        public boolean crosses(Rect other) {
            return x0<other.x1 && x1>other.x0 && z0<other.z1 && z1>other.z0;
        }
        public boolean includes(Rect other) {
            return x0<=other.x0&&x1>=other.x1&&z0<=other.z0&&z1>=other.z1;
        }
    }

    private final StitchedHeightmapWorld world;
    private final AdaptiveTerrainTopology topology;
    private final List<Floor> floors;
    private final List<Solid> solids;
    private final Set<String> layers;

    public LayeredTerrainQueries(StitchedHeightmapWorld world,AdaptiveTerrainTopology topology,
                                 List<Floor> floors,List<Solid> solids) {
        this.world=Objects.requireNonNull(world);
        this.topology=Objects.requireNonNull(topology);
        this.floors=List.copyOf(floors);
        this.solids=List.copyOf(solids);
        var ids=new HashSet<String>();
        ids.add(SURFACE);
        for(Floor floor:this.floors)ids.add(floor.layer());
        // Each separately named layer has at most one floor at a given X/Z.
        for(int i=0;i<this.floors.size();i++)
            for(int j=i+1;j<this.floors.size();j++){
                Floor a=this.floors.get(i),b=this.floors.get(j);
                if(a.layer().equals(b.layer()) &&
                    new Rect(a.x0(),a.z0(),a.x1(),a.z1())
                        .crosses(new Rect(b.x0(),b.z0(),b.x1(),b.z1())))
                    throw new IllegalArgumentException("Overlapping floors on layer "+a.layer());
            }
        this.layers=Set.copyOf(ids);
    }
    private static boolean finite(double... values) {
        for(double v:values)if(!Double.isFinite(v))return false;
        return true;
    }
    public Set<String> layers(){return layers;}
    public List<Floor> floors(){return floors;}
    public List<Solid> solids(){return solids;}
    public long topologyRevision(){return topology.revision();}

    /** Never returns a source support for a punched-out leaf or missing tile. */
    public Support support(String layer,double x,double z)throws IOException {
        if(!finite(x,z)||!layers.contains(layer))
            throw new IllegalArgumentException("Unknown layer or non-finite position");
        if(!layer.equals(SURFACE)){
            for(Floor floor:floors)
                if(floor.layer().equals(layer)&&floor.contains(x,z))
                    return new Support(layer,x,floor.height(x,z),z);
            return null;
        }
        var s=world.settings();
        double gx=(x-s.originX())/s.metresPerCell();
        double gz=(z-s.originZ())/s.metresPerCell();
        if(!finite(gx,gz)||gx < -s.tileCells()*1_000_000.0
            ||gz < -s.tileCells()*1_000_000.0
            ||gx >= s.tileCells()*1_000_001.0
            ||gz >= s.tileCells()*1_000_001.0)
            return null;
        long cellX=(long)Math.floor(gx),cellZ=(long)Math.floor(gz);
        long tx=Math.floorDiv(cellX,s.tileCells()),tz=Math.floorDiv(cellZ,s.tileCells());
        if(tx < -1_000_000||tz < -1_000_000||tx>1_000_000||tz>1_000_000
            ||!world.hasTile((int)tx,(int)tz))return null;
        double localX=(gx-cellX)*AdaptiveTerrainTopology.UNITS_PER_CELL;
        double localZ=(gz-cellZ)*AdaptiveTerrainTopology.UNITS_PER_CELL;
        for(var leaf:topology.leaves(cellX,cellZ)){
            double ax=leaf.localX()*leaf.units(),az=leaf.localZ()*leaf.units();
            if(localX>=ax&&localX<ax+leaf.units()
                &&localZ>=az&&localZ<az+leaf.units())
                return topology.isCutout(leaf)?null:
                    new Support(layer,x,world.height(x,z),z);
        }
        throw new IllegalStateException("Valid source cell has no adaptive leaf");
    }
    /** Returns stacked supports without ever guessing which vertical floor an actor occupies. */
    public List<Support> stack(double x,double z)throws IOException {
        List<Support> out=new ArrayList<>();
        for(String layer:layers){
            Support sample=support(layer,x,z);
            if(sample!=null)out.add(sample);
        }
        out.sort((a,b)->Double.compare(b.y(),a.y()));
        return List.copyOf(out);
    }
    private boolean clearance(Support p,Agent agent) throws IOException {
        // A different overhead walkable floor is also a solid slab/ceiling.
        for(Support higher:stack(p.x(),p.z()))
            if(higher.y()>p.y()+1e-7 && higher.y()<p.y()+agent.height()+1e-7)
                return false;
        for(Solid solid:solids){
            double cx=Math.max(solid.x0(),Math.min(p.x(),solid.x1()));
            double cz=Math.max(solid.z0(),Math.min(p.z(),solid.z1()));
            if(Math.hypot(cx-p.x(),cz-p.z())<agent.radius()
                && solid.y1()>p.y()+1e-6
                && solid.y0()<p.y()+agent.height()-1e-6)
                return false;
        }
        return true;
    }
    public boolean canStand(String layer,double x,double z,Agent agent)throws IOException {
        Objects.requireNonNull(agent);
        Support base=support(layer,x,z);
        if(base==null||!clearance(base,agent))return false;
        // Capsule footprint probes validate real support and real headroom,
        // independent of how many triangles the 3D renderer emitted.
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4,dx=Math.cos(a)*agent.radius()*.98,
                dz=Math.sin(a)*agent.radius()*.98;
            Support side=support(layer,x+dx,z+dz);
            if(side==null||!clearance(side,agent))return false;
            if(Math.abs(side.y()-base.y()) >
                    agent.radius()*.98*agent.slope()+1e-7)
                return false;
        }
        return true;
    }
    /**
     * Bounded local swept check; not a global pathfinder. Sampling <= 0.0625 m
     * handles the minimum allowed cutout width with footprint probes.
     * Edits must invalidate routes built from earlier topologyRevision().
     */
    public boolean canTraverse(String layer,double ax,double az,double bx,double bz,
                               Agent agent)throws IOException {
        if(!finite(ax,az,bx,bz))throw new IllegalArgumentException("Non-finite segment");
        double distance=Math.hypot(bx-ax,bz-az);
        int steps=Math.max(1,(int)Math.ceil(distance/.0625));
        if(steps>8192)return false; // Long routes belong to the region graph.
        Support prior=null;
        for(int i=0;i<=steps;i++){
            double f=i/(double)steps,x=ax+(bx-ax)*f,z=az+(bz-az)*f;
            if(!canStand(layer,x,z,agent))return false;
            Support current=support(layer,x,z);
            if(prior!=null && Math.abs(current.y()-prior.y()) >
                    agent.slope()*(distance/steps)+1e-6)
                return false;
            prior=current;
        }
        return true;
    }
    /** Does a region contain a physical feature requiring local nav refinement? */
    public boolean crossesFeature(String layer,Rect area) {
        if(layer.equals(SURFACE)){
            for(var cut:topology.cutoutNodes()){
                double factor=world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
                double ox=world.settings().originX(),oz=world.settings().originZ();
                Rect cutRect=new Rect(ox+cut.x0()*factor,oz+cut.z0()*factor,
                    ox+cut.x1()*factor,oz+cut.z1()*factor);
                if(area.crosses(cutRect)&&!cutRect.includes(area))return true;
            }
        }else for(Floor floor:floors)if(floor.layer().equals(layer)){
            Rect bound=new Rect(floor.x0(),floor.z0(),floor.x1(),floor.z1());
            if(area.crosses(bound)&&!bound.includes(area))return true;
        }
        for(Solid solid:solids){
            Rect bound=new Rect(solid.x0(),solid.z0(),solid.x1(),solid.z1());
            if(area.crosses(bound)&&!bound.includes(area))return true;
        }
        return false;
    }
}
