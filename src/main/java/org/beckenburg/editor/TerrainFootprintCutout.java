package org.beckenburg.editor;

import java.util.Objects;

/** Transactional footprint authoring through the editor's real punched leaves.
 * Never lowers source heights, clips an unrelated render plane or adds a cap.
 * Inexact boundaries are rejected rather than silently cutting outside the site.
 */
public final class TerrainFootprintCutout {
    private TerrainFootprintCutout(){}
    private static final double EPS=1e-7;
    public record Result(int newSplits,int newCutouts){}

    public static Result punch(AdaptiveTerrainTopology topology,TerrainEntranceMesh.Bounds footprint){
        Objects.requireNonNull(topology);Objects.requireNonNull(footprint);
        var before=topology.snapshot();
        int oldSplits=topology.splitCount(),oldCuts=topology.cutoutCount();
        try{
            var s=topology.world().settings();
            double cell=s.metresPerCell();
            long x0=(long)Math.floor((footprint.x0()-s.originX())/cell+EPS);
            long x1=(long)Math.ceil((footprint.x1()-s.originX())/cell-EPS);
            long z0=(long)Math.floor((footprint.z0()-s.originZ())/cell+EPS);
            long z1=(long)Math.ceil((footprint.z1()-s.originZ())/cell-EPS);
            if(x1-x0>2048||z1-z0>2048||(x1-x0)*(z1-z0)>2048)
                throw new IllegalArgumentException("Footprint exceeds bounded source-cell budget");
            for(long z=z0;z<z1;z++)for(long x=x0;x<x1;x++)
                for(var leaf:topology.leaves(x,z))cut(topology,leaf,footprint);
            return new Result(topology.splitCount()-oldSplits,topology.cutoutCount()-oldCuts);
        }catch(RuntimeException failure){
            topology.restore(before);
            throw failure;
        }
    }

    private static void cut(AdaptiveTerrainTopology topo,AdaptiveTerrainTopology.Node leaf,
                            TerrainEntranceMesh.Bounds bounds){
        var s=topo.world().settings();
        double unit=s.metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
        double x0=s.originX()+leaf.x0()*unit,x1=s.originX()+leaf.x1()*unit;
        double z0=s.originZ()+leaf.z0()*unit,z1=s.originZ()+leaf.z1()*unit;
        if(x1<=bounds.x0()+EPS||x0>=bounds.x1()-EPS
            ||z1<=bounds.z0()+EPS||z0>=bounds.z1()-EPS)return;
        // Existing removed geometry is preserved, including any earlier holes.
        if(topo.isCutout(leaf))return;
        if(x0>=bounds.x0()-EPS&&x1<=bounds.x1()+EPS
            &&z0>=bounds.z0()-EPS&&z1<=bounds.z1()+EPS){
            topo.punch(leaf);return;
        }
        double next=s.metresPerCell()/(1<<(leaf.depth()+1));
        if(leaf.depth()>=AdaptiveTerrainTopology.MAX_DEPTH
            ||next<AdaptiveTerrainTopology.MIN_LEAF_METRES)
            throw new IllegalArgumentException("Footprint boundary does not conform to the editable terrain grid: "
                +bounds+" near "+x0+","+z0+"; align the foundation or author its rim explicitly");
        topo.split(leaf);
        for(int z=0;z<2;z++)for(int x=0;x<2;x++)
            cut(topo,new AdaptiveTerrainTopology.Node(leaf.cellX(),leaf.cellZ(),leaf.depth()+1,
                leaf.localX()*2+x,leaf.localZ()*2+z),bounds);
    }
}
