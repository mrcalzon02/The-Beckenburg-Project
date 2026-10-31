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
    /** Smooth C1 ramp whose height is actual query geometry, not a nav-only jump.
     * The mouth face is at the minimum edge for EAST/SOUTH and the maximum
     * edge for WEST/NORTH. A Hermite curve matches authored mouth/interior
     * elevations and grades, including zero-grade flat bunker transitions. */
    public record Ramp(String id,String layer,String targetLayer,Direction direction,
                       double x0,double z0,double x1,double z1,
                       double mouthY,double interiorY,
                       double mouthGrade,double interiorGrade) {
        public enum Direction { EAST,WEST,NORTH,SOUTH }
        public Ramp {
            if(id==null||id.isBlank()||layer==null||layer.isBlank()
                ||targetLayer==null||targetLayer.isBlank()
                ||layer.equals(SURFACE)||layer.equals(targetLayer)||direction==null
                ||!finite(x0,z0,x1,z1,mouthY,interiorY,mouthGrade,interiorGrade)
                ||x1-x0<.6||z1-z0<.6)
                throw new IllegalArgumentException("Invalid ramp dimensions or layers");
            if(Math.abs(mouthGrade)>2||Math.abs(interiorGrade)>2)
                throw new IllegalArgumentException("Ramp grades exceed supported range");
            if(length(direction,x0,z0,x1,z1)<1.0)
                throw new IllegalArgumentException("Ramp must be at least one meter long");
        }
        private static double length(Direction d,double x0,double z0,double x1,double z1){
            return d==Direction.EAST||d==Direction.WEST?x1-x0:z1-z0;
        }
        public double length(){return length(direction,x0,z0,x1,z1);}
        public double width(){return direction==Direction.EAST||direction==Direction.WEST?
            z1-z0:x1-x0;}
        public double axis(double x,double z){
            return switch(direction){
                case EAST->x-x0;case WEST->x1-x;
                case SOUTH->z-z0;case NORTH->z1-z;
            };
        }
        public boolean contains(double x,double z){
            return x>=x0&&x<x1&&z>=z0&&z<z1;
        }
        public double height(double x,double z){
            double t=Math.max(0,Math.min(1,axis(x,z)/length()));
            // Cubic Hermite with derivatives measured in metres rise per
            // horizontal metre. Derivative at t=0 = mouthGrade, t=1 = interiorGrade.
            double t2=t*t,t3=t2*t,L=length();
            return (2*t3-3*t2+1)*mouthY
                +(t3-2*t2+t)*L*mouthGrade
                +(-2*t3+3*t2)*interiorY
                +(t3-t2)*L*interiorGrade;
        }
        public double mouthX(){return direction==Direction.EAST?x0:
            direction==Direction.WEST?x1:(x0+x1)*.5;}
        public double mouthZ(){return direction==Direction.SOUTH?z0:
            direction==Direction.NORTH?z1:(z0+z1)*.5;}
        public double insideX(){return direction==Direction.EAST?x1:
            direction==Direction.WEST?x0:(x0+x1)*.5;}
        public double insideZ(){return direction==Direction.SOUTH?z1:
            direction==Direction.NORTH?z0:(z0+z1)*.5;}
        public double entranceX(double inset){return direction==Direction.EAST?x0+inset:
            direction==Direction.WEST?x1-inset:(x0+x1)*.5;}
        public double entranceZ(double inset){return direction==Direction.SOUTH?z0+inset:
            direction==Direction.NORTH?z1-inset:(z0+z1)*.5;}
        public double exitX(double inset){return direction==Direction.EAST?x1-inset:
            direction==Direction.WEST?x0+inset:(x0+x1)*.5;}
        public double exitZ(double inset){return direction==Direction.SOUTH?z1-inset:
            direction==Direction.NORTH?z0+inset:(z0+z1)*.5;}
        /** Geometry triangles are sampled from the same height function as collision. */
        public List<double[]> triangles(int longitudinalSamples) {
            if(longitudinalSamples<1||longitudinalSamples>256)
                throw new IllegalArgumentException("Ramp tessellation samples must be 1..256");
            List<double[]> out=new ArrayList<>();
            for(int i=0;i<longitudinalSamples;i++){
                double a=i*length()/longitudinalSamples;
                double b=(i+1)*length()/longitudinalSamples;
                double[] v0=corner(a,0),v1=corner(a,1);
                double[] v2=corner(b,0),v3=corner(b,1);
                out.add(new double[]{v0[0],v0[1],v0[2],v2[0],v2[1],v2[2],
                    v1[0],v1[1],v1[2]});
                out.add(new double[]{v2[0],v2[1],v2[2],v3[0],v3[1],v3[2],
                    v1[0],v1[1],v1[2]});
            }
            return List.copyOf(out);
        }
        private double[] corner(double fromMouth,double lateral){
            double x=entranceX(fromMouth),z=entranceZ(fromMouth);
            if(direction==Direction.EAST||direction==Direction.WEST)
                z=z0+(z1-z0)*lateral;
            else x=x0+(x1-x0)*lateral;
            return new double[]{x,height(x,z),z};
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
    private final List<Ramp> ramps;
    private final List<Solid> solids;
    private final EntranceSeamDocument seams;
    private final Set<String> layers;

    public LayeredTerrainQueries(StitchedHeightmapWorld world,AdaptiveTerrainTopology topology,
                                 List<Floor> floors,List<Solid> solids) {
        this(world,topology,floors,solids,List.of(),null);
    }
    public LayeredTerrainQueries(StitchedHeightmapWorld world,AdaptiveTerrainTopology topology,
                                 List<Floor> floors,List<Solid> solids,List<Ramp> ramps) {
        this(world,topology,floors,solids,ramps,null);
    }
    public LayeredTerrainQueries(StitchedHeightmapWorld world,AdaptiveTerrainTopology topology,
                                 List<Floor> floors,List<Solid> solids,List<Ramp> ramps,
                                 EntranceSeamDocument seams) {
        this.world=Objects.requireNonNull(world);
        this.topology=Objects.requireNonNull(topology);
        this.floors=List.copyOf(floors);
        this.ramps=List.copyOf(ramps);
        this.solids=List.copyOf(solids);
        this.seams=seams;
        var ids=new HashSet<String>();
        ids.add(SURFACE);
        for(Floor floor:this.floors)ids.add(floor.layer());
        if(seams!=null)for(var tab:seams.tabs())
            if(!ids.add(tab.layer()))
                throw new IllegalArgumentException("Detached tab layer ID must be unique: "+tab.layer());
        for(Ramp ramp:this.ramps){
            if(!ids.add(ramp.layer()))
                throw new IllegalArgumentException("Ramp layer ID must be unique: "+ramp.layer());
        }
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
    private boolean isDetachedTab(String layer,double x,double z){
        if(seams==null)return false;
        for(var tab:seams.tabs())if(tab.layer().equals(layer)&&tab.contains(x,z))
            return true;
        return false;
    }
    public List<Ramp> ramps(){return ramps;}
    public EntranceSeamDocument seams(){return seams;}
    public boolean blocksAuthoredSeam(String layer,double ax,double az,double bx,double bz){
        return seams!=null&&layer.equals(SURFACE)
            &&seams.blocksSurface(ax,az,bx,bz);
    }
    public List<Solid> solids(){return solids;}
    public long topologyRevision(){return topology.revision();}

    /** Never returns a source support for a punched-out leaf or missing tile. */
    public Support support(String layer,double x,double z)throws IOException {
        if(!finite(x,z)||!layers.contains(layer))
            throw new IllegalArgumentException("Unknown layer or non-finite position");
        if(seams!=null){
            for(var tab:seams.tabs())if(tab.contains(x,z)){
                if(layer.equals(SURFACE))return null;
                if(layer.equals(tab.layer())){
                    var w=world.settings();
                    long cx=(long)Math.floor((x-w.originX())/w.metresPerCell());
                    long cz=(long)Math.floor((z-w.originZ())/w.metresPerCell());
                    long tx=Math.floorDiv(cx,w.tileCells());
                    long tz=Math.floorDiv(cz,w.tileCells());
                    if(tx < -1000000||tx>1000000||tz < -1000000||tz>1000000
                        ||!world.hasTile((int)tx,(int)tz))return null;
                    return new Support(layer,x,world.height(x,z),z);
                }
            }
        }
        if(!layer.equals(SURFACE)){
            for(Ramp ramp:ramps)
                if(ramp.layer().equals(layer)&&ramp.contains(x,z))
                    return new Support(layer,x,ramp.height(x,z),z);
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
            if(higher.y()>p.y()+1e-7 && higher.y()<p.y()+agent.height()+1e-7){
                // Co-located doorway lips are separate named walkable layers
                // but should not act as 5 cm high ceilings. This exemption
                // applies ONLY at an explicitly authored ramp overlap.
                boolean seam=false;
                for(Ramp ramp:ramps){
                    if(!ramp.contains(p.x(),p.z()))continue;
                    boolean upperApproach=higher.layer().equals(SURFACE)
                        ||higher.layer().equals(ramp.targetLayer())
                        ||isDetachedTab(higher.layer(),p.x(),p.z());
                    boolean lowerApproach=p.layer().equals(SURFACE)
                        ||p.layer().equals(ramp.targetLayer())
                        ||isDetachedTab(p.layer(),p.x(),p.z());
                    if(!(ramp.layer().equals(p.layer())&&upperApproach
                         ||ramp.layer().equals(higher.layer())&&lowerApproach))
                        continue;
                    double axis=ramp.axis(p.x(),p.z());
                    boolean fromExterior=p.layer().equals(SURFACE)||higher.layer().equals(SURFACE);
                    if(seams!=null)for(var tab:seams.tabs())if(tab.contains(p.x(),p.z())
                        &&(p.layer().equals(tab.layer())||higher.layer().equals(tab.layer())))
                        fromExterior=true;
                    boolean atMouth=axis<=Math.min(1.5,ramp.length()*.25)
                        &&fromExterior;
                    boolean atExit=ramp.length()-axis<=.8
                        &&(p.layer().equals(ramp.targetLayer())
                           ||higher.layer().equals(ramp.targetLayer()));
                    seam=(atMouth||atExit)&&higher.y()-p.y()<=agent.maxStep()+1e-7;
                    if(seam)break;
                }
                if(!seam)return false;
            }
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
        if(blocksAuthoredSeam(layer,ax,az,bx,bz))return false;
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
    /**
     * Conservative source-triangle slope audit for coarse regions. Nine navigation
     * probes alone can miss a narrow ridge or ravine between their sample rays.
     * Scan the actual stitched PNG grid (two source triangles per base cell)
     * independent of how coarsely the renderer has tessellated that area.
     * Large/unknown areas demand subdivision instead of speculative clearance.
     */
    public boolean steepSourceWithin(Rect area,Agent agent)throws IOException {
        Objects.requireNonNull(agent);
        var settings=world.settings();
        double size=settings.metresPerCell();
        double xmin=(area.x0()-settings.originX())/size;
        double xmax=(area.x1()-settings.originX())/size;
        double zmin=(area.z0()-settings.originZ())/size;
        double zmax=(area.z1()-settings.originZ())/size;
        if(!finite(xmin,xmax,zmin,zmax))return true;
        long loX=(long)Math.floor(xmin),hiX=(long)Math.ceil(xmax)-1;
        long loZ=(long)Math.floor(zmin),hiZ=(long)Math.ceil(zmax)-1;
        long width=hiX-loX+1,depth=hiZ-loZ+1;
        if(width<=0||depth<=0)return true;
        if(width>32||depth>32||width*depth>512)return true;
        int n=settings.tileCells();
        for(long z=loZ;z<=hiZ;z++)for(long x=loX;x<=hiX;x++){
            long tileX=Math.floorDiv(x,n),tileZ=Math.floorDiv(z,n);
            if(tileX < -1_000_000||tileX>1_000_000
                ||tileZ < -1_000_000||tileZ>1_000_000
                ||!world.hasTile((int)tileX,(int)tileZ))return true;
            int tx=(int)tileX,tz=(int)tileZ;
            int ix=(int)Math.floorMod(x,n),iz=(int)Math.floorMod(z,n);
            double a=world.metres(world.sample(tx,tz,ix,iz));
            double b=world.metres(world.sample(tx,tz,ix+1,iz));
            double c=world.metres(world.sample(tx,tz,ix+1,iz+1));
            double d=world.metres(world.sample(tx,tz,ix,iz+1));
            // This is EXACTLY the diagonal split used by StitchedHeightmapWorld.height.
            double slope1=Math.hypot((b-a)/size,(c-b)/size);
            double slope2=Math.hypot((c-d)/size,(d-a)/size);
            if(Math.max(slope1,slope2)>agent.slope()+1e-7)
                return true;
        }
        return false;
    }

    /** Does a region contain a physical feature requiring local nav refinement? */
    public boolean crossesFeature(String layer,Rect area) {
        if(seams!=null&&seams.crossesFeature(layer,area))return true;
        if(layer.equals(SURFACE)){
            for(var cut:topology.cutoutNodes()){
                double factor=world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
                double ox=world.settings().originX(),oz=world.settings().originZ();
                Rect cutRect=new Rect(ox+cut.x0()*factor,oz+cut.z0()*factor,
                    ox+cut.x1()*factor,oz+cut.z1()*factor);
                if(area.crosses(cutRect)&&!cutRect.includes(area))return true;
            }
        }else {
            for(Ramp ramp:ramps)if(ramp.layer().equals(layer)){
                Rect bound=new Rect(ramp.x0(),ramp.z0(),ramp.x1(),ramp.z1());
                if(area.crosses(bound)&&!bound.includes(area))return true;
            }
            for(Floor floor:floors)if(floor.layer().equals(layer)){
            Rect bound=new Rect(floor.x0(),floor.z0(),floor.x1(),floor.z1());
            if(area.crosses(bound)&&!bound.includes(area))return true;
            }
        }
        for(Solid solid:solids){
            Rect bound=new Rect(solid.x0(),solid.z0(),solid.x1(),solid.z1());
            if(area.crosses(bound)&&!bound.includes(area))return true;
        }
        return false;
    }
}
