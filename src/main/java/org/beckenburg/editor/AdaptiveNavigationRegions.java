package org.beckenburg.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * First standalone hierarchical navigation REGION layer for authored terrain.
 * Regions are based on exact physical supports, openings, slopes and clearance,
 * not on camera distance or renderer triangle count. No game AI is routed here
 * until the runtime collision and movement integration is accepted.
 */
public final class AdaptiveNavigationRegions {
    private static final double EPS=1e-7;
    private static final int MAX_CELLS=8192;
    private static final int MAX_REGIONS=2048;

    public record Area(String layer,double x0,double z0,double x1,double z1) {
        public Area {
            if(layer==null||layer.isBlank()||!Double.isFinite(x0)||
                    !Double.isFinite(z0)||!Double.isFinite(x1)||!Double.isFinite(z1)||
                    x1<=x0||z1<=z0)
                throw new IllegalArgumentException("Invalid authored navigation area");
        }
    }
    public enum Connection { STAIRS, LADDER, DROP }
    /** Authored traversal; an opening by itself is NEVER an automatic connection. */
    public record Transition(String from,String to,double fromX,double fromZ,
                             double toX,double toZ,Connection connection) {
        public Transition {
            if(from==null||to==null||from.equals(to)||connection==null
                    ||!Double.isFinite(fromX)||!Double.isFinite(fromZ)
                    ||!Double.isFinite(toX)||!Double.isFinite(toZ))
                throw new IllegalArgumentException("Invalid vertical transition");
        }
    }
    public record Profile(LayeredTerrainQueries.Agent footprint,boolean ladders,
                          boolean controlledDrops) {
        public Profile { Objects.requireNonNull(footprint); }
    }
    public record Region(int id,String layer,double x0,double z0,double x1,double z1,
                         double centreY) {
        boolean contains(double x,double z) {
            return x>=x0-EPS&&x<x1-EPS&&z>=z0-EPS&&z<z1-EPS;
        }
        double x(){return (x0+x1)*.5;}
        double z(){return (z0+z1)*.5;}
    }
    /** Exact verified traversal anchors, even when adjacent region centroids are offset. */
    public record Edge(int to,double cost,Connection connection,
                       double entryX,double entryZ,double exitX,double exitZ) {}
    public record PathStep(int region,String layer,double x,double y,double z,
                           Connection arriveVia){}
    public record Metrics(int evaluatedCells,int acceptedRegions,int edges,
                          int rejectedCells,long topologyRevision){}
    public static final class Graph {
        private final LayeredTerrainQueries query;
        private final Profile profile;
        private final AdaptiveNavMeshEdits edits;
        private final long editsRevision;
        private final Graph candidates;
        private final List<Region> regions;
        private final List<List<Edge>> adjacent;
        private final Metrics metrics;
        Graph(LayeredTerrainQueries query,Profile profile,List<Region> regions,
              List<List<Edge>> adjacent,int evaluated,int rejected,
              AdaptiveNavMeshEdits edits) {
            this(query,profile,regions,adjacent,evaluated,rejected,edits,null);
        }
        Graph(LayeredTerrainQueries query,Profile profile,List<Region> regions,
              List<List<Edge>> adjacent,int evaluated,int rejected,
              AdaptiveNavMeshEdits edits,Graph candidates) {
            this.query=query;this.profile=profile;this.edits=edits;
            this.candidates=candidates;
            this.editsRevision=edits==null?0:edits.revision();
            this.regions=List.copyOf(regions);
            this.adjacent=adjacent.stream().map(List::copyOf).toList();
            int links=this.adjacent.stream().mapToInt(List::size).sum();
            this.metrics=new Metrics(evaluated,regions.size(),links,rejected,query.topologyRevision());
        }
        public List<Region> regions(){return regions;}
        /** Inspector-only pre-flood graph, with authored gates/trims already applied.
         * Unreachable polygons are visible for manual regression but never routed. */
        public Graph candidates(){return candidates==null?this:candidates;}
        public Metrics metrics(){return metrics;}
        public List<Edge> connections(int id){return adjacent.get(id);}
        public boolean stale(){return metrics.topologyRevision()!=query.topologyRevision()
            ||edits!=null&&editsRevision!=edits.revision();}
        public long authoredRevision(){return editsRevision;}

        private int regionAt(String layer,double x,double z){
            for(Region r:regions)if(r.layer().equals(layer)&&r.contains(x,z))
                return r.id();
            return -1;
        }
        /** Dijkstra through validated region portals, no render-based shortcuts. */
        public List<PathStep> route(String from,double sx,double sz,
                                    String to,double tx,double tz)throws IOException {
            if(stale())throw new IllegalStateException("Edited topology requires navigation rebuild");
            if(!query.canStand(from,sx,sz,profile.footprint())
                ||!query.canStand(to,tx,tz,profile.footprint()))return List.of();
            int start=regionAt(from,sx,sz),finish=regionAt(to,tx,tz);
            if(start<0||finish<0)return List.of();
            if(start==finish){
                if(!query.canTraverse(from,sx,sz,tx,tz,profile.footprint()))return List.of();
                var a=query.support(from,sx,sz);var b=query.support(to,tx,tz);
                return List.of(new PathStep(start,from,sx,a.y(),sz,null),
                    new PathStep(finish,to,tx,b.y(),tz,null));
            }
            Region initial=regions.get(start),finalRegion=regions.get(finish);
            if(!query.canTraverse(from,sx,sz,initial.x(),initial.z(),profile.footprint())
                ||!query.canTraverse(to,finalRegion.x(),finalRegion.z(),tx,tz,
                    profile.footprint()))return List.of();
            double[] costs=new double[regions.size()];
            Arrays.fill(costs,Double.POSITIVE_INFINITY);
            int[] previous=new int[regions.size()];
            Edge[] via=new Edge[regions.size()];
            Arrays.fill(previous,-1);
            record Candidate(int id,double score){}
            PriorityQueue<Candidate> pending=new PriorityQueue<>(
                Comparator.comparingDouble(Candidate::score));
            costs[start]=0;
            pending.add(new Candidate(start,0));
            while(!pending.isEmpty()){
                Candidate next=pending.poll();
                if(next.score()>costs[next.id()])continue;
                if(next.id()==finish)break;
                for(Edge edge:adjacent.get(next.id())){
                    double alt=costs[next.id()]+edge.cost();
                    if(alt<costs[edge.to()]){
                        costs[edge.to()]=alt;
                        previous[edge.to()]=next.id();
                        via[edge.to()]=edge;
                        pending.add(new Candidate(edge.to(),alt));
                    }
                }
            }
            if(!Double.isFinite(costs[finish]))return List.of();
            List<Integer> ids=new ArrayList<>();
            for(int i=finish;i!=-1;i=previous[i])ids.add(i);
            java.util.Collections.reverse(ids);
            List<PathStep> steps=new ArrayList<>();
            steps.add(new PathStep(start,from,sx,query.support(from,sx,sz).y(),sz,null));
            for(int i=0;i<ids.size()-1;i++){
                Region current=regions.get(ids.get(i));
                Region next=regions.get(ids.get(i+1));
                Edge edge=via[next.id()];
                if(edge==null)throw new IllegalStateException("Route lost its connector");
                // Preserve the centre->portal->centre corridor that was physically
                // tested during graph construction. Direct centroid shortcuts could
                // otherwise cross a punched hole or a corner of an obstructing wall.
                steps.add(new PathStep(current.id(),current.layer(),current.x(),
                    current.centreY(),current.z(),null));
                var entry=query.support(current.layer(),edge.entryX(),edge.entryZ());
                var exit=query.support(next.layer(),edge.exitX(),edge.exitZ());
                if(entry==null||exit==null)return List.of();
                steps.add(new PathStep(current.id(),current.layer(),entry.x(),entry.y(),
                    entry.z(),null));
                steps.add(new PathStep(next.id(),next.layer(),exit.x(),exit.y(),
                    exit.z(),edge.connection()));
            }
            Region last=regions.get(finish);
            steps.add(new PathStep(last.id(),last.layer(),last.x(),last.centreY(),last.z(),null));
            steps.add(new PathStep(finish,to,tx,query.support(to,tx,tz).y(),tz,null));
            return List.copyOf(steps);
        }
    }

    private final LayeredTerrainQueries query;
    private final Profile profile;
    private final AdaptiveNavMeshEdits edits;
    private final double minCell;
    private final ArrayList<Region> regions=new ArrayList<>();
    private final List<List<Edge>> adjacent=new ArrayList<>();
    private int evaluated,rejected;
    private AdaptiveNavigationRegions(LayeredTerrainQueries query,Profile profile,
                                      AdaptiveNavMeshEdits edits,double minCell) {
        this.query=query;this.profile=profile;this.edits=edits;this.minCell=minCell;
    }
    /**
     * Build a sparse graph for EXPLICIT areas/layers. Authored polygons/ramp
     * surfaces can later replace this conservative rect-based bootstrap.
     */
    public static Graph build(LayeredTerrainQueries query,Profile profile,
                              List<Area> areas,List<Transition> transitions,
                              double minCell)throws IOException {
        return build(query,profile,areas,transitions,null,minCell);
    }
    /**
     * Authored flood fill: keep only the regions reachable from editor seeds
     * across physically valid, non-excluded/non-barred connections. Manual trims
     * and blocked transition doors are applied BEFORE flooding, so they cannot
     * silently regrow on a later pulse or continuous rebuild.
     */
    public static Graph build(LayeredTerrainQueries query,Profile profile,
                              List<Area> areas,List<Transition> transitions,
                              AdaptiveNavMeshEdits edits,double minCell)throws IOException {
        Objects.requireNonNull(query);Objects.requireNonNull(profile);
        Objects.requireNonNull(areas);Objects.requireNonNull(transitions);
        if(!Double.isFinite(minCell)||minCell<.125||minCell>20)
            throw new IllegalArgumentException("Invalid navigation sampling resolution");
        AdaptiveNavigationRegions builder=new AdaptiveNavigationRegions(query,profile,edits,minCell);
        for(Area area:areas){
            if(!query.layers().contains(area.layer()))
                throw new IllegalArgumentException("Unknown layer "+area.layer());
            builder.partition(area.layer(),area.x0(),area.z0(),area.x1(),area.z1());
        }
        for(int i=0;i<builder.regions.size();i++)builder.adjacent.add(new ArrayList<>());
        builder.joinPeers();
        builder.joinTransitions(transitions);
        return builder.floodFromSeeds();
    }
    private Graph floodFromSeeds() {
        Graph full=new Graph(query,profile,regions,adjacent,evaluated,rejected,edits);
        if(edits==null||edits.seeds().isEmpty())
            return full;
        boolean[] reached=new boolean[regions.size()];
        java.util.ArrayDeque<Integer> pending=new java.util.ArrayDeque<>();
        for(var seed:edits.seeds()){
            int id=locate(seed.layer(),seed.x(),seed.z());
            if(id>=0&&!reached[id]){reached[id]=true;pending.add(id);}
        }
        while(!pending.isEmpty()){
            int id=pending.removeFirst();
            for(Edge e:adjacent.get(id))
                if(!reached[e.to()]){reached[e.to()]=true;pending.addLast(e.to());}
        }
        int[] remap=new int[regions.size()];
        java.util.Arrays.fill(remap,-1);
        List<Region> kept=new ArrayList<>();
        for(int i=0;i<regions.size();i++)if(reached[i]){
            Region r=regions.get(i);
            remap[i]=kept.size();
            kept.add(new Region(kept.size(),r.layer(),r.x0(),r.z0(),
                r.x1(),r.z1(),r.centreY()));
        }
        List<List<Edge>> linked=new ArrayList<>();
        for(int i=0;i<regions.size();i++)if(reached[i]){
            List<Edge> edges=new ArrayList<>();
            for(Edge e:adjacent.get(i))if(reached[e.to()])
                edges.add(new Edge(remap[e.to()],e.cost(),e.connection(),
                    e.entryX(),e.entryZ(),e.exitX(),e.exitZ()));
            linked.add(edges);
        }
        return new Graph(query,profile,kept,linked,evaluated,
            rejected+regions.size()-kept.size(),edits,full);
    }
    private boolean safe(String layer,double x,double z)throws IOException {
        return query.canStand(layer,x,z,profile.footprint());
    }
    private boolean allSamplesSafe(String layer,double x0,double z0,double x1,double z1)
            throws IOException {
        double w=x1-x0,d=z1-z0;
        for(double v:new double[]{.15,.5,.85})for(double u:new double[]{.15,.5,.85}){
            if(!safe(layer,x0+w*u,z0+d*v))return false;
        }
        double cx=(x0+x1)*.5,cz=(z0+z1)*.5;
        // Validate the whole local corridor, not just the nine vertices.
        for(double[] p:new double[][]{{x0+w*.15,z0+d*.15},{x0+w*.85,z0+d*.15},
                {x0+w*.15,z0+d*.85},{x0+w*.85,z0+d*.85}})
            if(!query.canTraverse(layer,cx,cz,p[0],p[1],profile.footprint()))
                return false;
        return true;
    }
    private void partition(String layer,double x0,double z0,double x1,double z1)
            throws IOException {
        if(++evaluated>MAX_CELLS)
            throw new IllegalStateException("Navigation refinement budget exceeded");
        var extent=new LayeredTerrainQueries.Rect(x0,z0,x1,z1);
        boolean editedRegion=edits!=null&&edits.excluded(layer,x0,z0,x1,z1);
        boolean editBoundary=edits!=null&&edits.needsSplit(layer,x0,z0,x1,z1);
        if(editedRegion&&!editBoundary){rejected++;return;}
        boolean details=query.crossesFeature(layer,extent)||editBoundary
            ||(layer.equals(LayeredTerrainQueries.SURFACE)
                &&query.steepSourceWithin(extent,profile.footprint()));
        double w=x1-x0,d=z1-z0;
        boolean safe=allSamplesSafe(layer,x0,z0,x1,z1);
        // A doubtful region is subdivided; at the minimum size it is blocked,
        // NEVER blindly considered traversable because a corner looks fine.
        if(details||!safe||editedRegion){
            if(w>minCell+EPS||d>minCell+EPS){
                double mx=(x0+x1)*.5,mz=(z0+z1)*.5;
                if(w>minCell+EPS&&d>minCell+EPS){
                    partition(layer,x0,z0,mx,mz);
                    partition(layer,mx,z0,x1,mz);
                    partition(layer,x0,mz,mx,z1);
                    partition(layer,mx,mz,x1,z1);
                }else if(w>minCell+EPS){
                    partition(layer,x0,z0,mx,z1);
                    partition(layer,mx,z0,x1,z1);
                }else{
                    partition(layer,x0,z0,x1,mz);
                    partition(layer,x0,mz,x1,z1);
                }
                return;
            }
            if(!safe||details||editedRegion){rejected++;return;}
        }
        if(regions.size()>=MAX_REGIONS)
            throw new IllegalStateException("Navigation region budget exceeded");
        var support=query.support(layer,(x0+x1)*.5,(z0+z1)*.5);
        if(support!=null)
            regions.add(new Region(regions.size(),layer,x0,z0,x1,z1,support.y()));
        else rejected++;
    }
    private static double overlap(double a0,double a1,double b0,double b1){
        return Math.min(a1,b1)-Math.max(a0,b0);
    }
    private static boolean sharesBoundary(Region a,Region b){
        return (Math.abs(a.x1()-b.x0())<EPS||Math.abs(b.x1()-a.x0())<EPS)
                &&overlap(a.z0(),a.z1(),b.z0(),b.z1())>EPS
            || (Math.abs(a.z1()-b.z0())<EPS||Math.abs(b.z1()-a.z0())<EPS)
                &&overlap(a.x0(),a.x1(),b.x0(),b.x1())>EPS;
    }
    private void joinPeers()throws IOException {
        for(int i=0;i<regions.size();i++)for(int j=i+1;j<regions.size();j++){
            Region a=regions.get(i),b=regions.get(j);
            if(!a.layer().equals(b.layer())||!sharesBoundary(a,b))continue;
            boolean vertical=Math.abs(a.x1()-b.x0())<EPS
                ||Math.abs(b.x1()-a.x0())<EPS;
            double px=vertical?(a.x1()+b.x0())*.5:(a.x()+b.x())*.5;
            double pz=vertical?(a.z()+b.z())*.5:(a.z1()+b.z0())*.5;
            // Portal centre is taken from actual overlapping edge, not tile centre.
            if(vertical) {
                px=Math.abs(a.x1()-b.x0())<EPS?a.x1():b.x1();
                pz=(Math.max(a.z0(),b.z0())+Math.min(a.z1(),b.z1()))*.5;
            } else {
                pz=Math.abs(a.z1()-b.z0())<EPS?a.z1():b.z1();
                px=(Math.max(a.x0(),b.x0())+Math.min(a.x1(),b.x1()))*.5;
            }
            String layer=a.layer();
            if(edits!=null&&edits.blocks(layer,a.x(),a.z(),b.x(),b.z()))
                continue;
            if(!query.canTraverse(layer,a.x(),a.z(),px,pz,profile.footprint())
                ||!query.canTraverse(layer,px,pz,b.x(),b.z(),profile.footprint()))
                continue;
            double cost=Math.hypot(a.x()-px,a.z()-pz)
                +Math.hypot(b.x()-px,b.z()-pz);
            adjacent.get(i).add(new Edge(j,cost,null,px,pz,px,pz));
            adjacent.get(j).add(new Edge(i,cost,null,px,pz,px,pz));
        }
    }
    private int locate(String layer,double x,double z){
        for(Region r:regions)if(r.layer().equals(layer)&&r.contains(x,z))return r.id();
        return -1;
    }
    private void joinTransitions(List<Transition> transitions)throws IOException {
        for(Transition t:transitions){
            if(!query.layers().contains(t.from())||!query.layers().contains(t.to()))
                throw new IllegalArgumentException("Transition references unknown layer");
            if(t.connection()==Connection.LADDER&&!profile.ladders())continue;
            if(t.connection()==Connection.DROP&&!profile.controlledDrops())continue;
            int from=locate(t.from(),t.fromX(),t.fromZ());
            int to=locate(t.to(),t.toX(),t.toZ());
            if(from<0||to<0||!safe(t.from(),t.fromX(),t.fromZ())
                ||!safe(t.to(),t.toX(),t.toZ()))continue;
            Region a=regions.get(from),b=regions.get(to);
            if(edits!=null&&(
                    edits.blocks(a.layer(),a.x(),a.z(),t.fromX(),t.fromZ())
                    ||edits.blocks(b.layer(),t.toX(),t.toZ(),b.x(),b.z())))
                continue;
            if(!query.canTraverse(a.layer(),a.x(),a.z(),t.fromX(),t.fromZ(),
                    profile.footprint()) ||
                !query.canTraverse(b.layer(),t.toX(),t.toZ(),b.x(),b.z(),
                    profile.footprint()))continue;
            double rise=Math.abs(query.support(t.from(),t.fromX(),t.fromZ()).y()
                -query.support(t.to(),t.toX(),t.toZ()).y());
            double cost=Math.hypot(a.x()-t.fromX(),a.z()-t.fromZ())
                +Math.hypot(b.x()-t.toX(),b.z()-t.toZ())+rise+1;
            adjacent.get(from).add(new Edge(to,cost,t.connection(),
                t.fromX(),t.fromZ(),t.toX(),t.toZ()));
            if(t.connection()!=Connection.DROP)
                adjacent.get(to).add(new Edge(from,cost,t.connection(),
                    t.toX(),t.toZ(),t.fromX(),t.fromZ()));
        }
    }
}
