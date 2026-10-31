package org.beckenburg.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Explicit editor command: conform split leaves to authored seam lines and
 * detach tabs by punching the original outer-surface leaves. The original
 * stitched 16-bit terrain PNG is never edited or resampled.
 *
 * Important: a destitched edge remains represented by EntranceSeamDocument,
 * not by deleting vertices; the eventual mesh emitter must honor that rule.
 */
public final class EntranceSeamTopology {
    public record Result(int newSplits,int newCutouts){}
    private EntranceSeamTopology(){}
    private static final double EPS=1e-7;
    private static double x0(StitchedHeightmapWorld world,AdaptiveTerrainTopology.Node leaf){
        return world.settings().originX()
            +leaf.x0()*world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private static double z0(StitchedHeightmapWorld world,AdaptiveTerrainTopology.Node leaf){
        return world.settings().originZ()
            +leaf.z0()*world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private static double x1(StitchedHeightmapWorld world,AdaptiveTerrainTopology.Node leaf){
        return world.settings().originX()
            +leaf.x1()*world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private static double z1(StitchedHeightmapWorld world,AdaptiveTerrainTopology.Node leaf){
        return world.settings().originZ()
            +leaf.z1()*world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
    }
    private static boolean overlap(double a,double b,double c,double d){
        return Math.min(b,d)-Math.max(a,c)>EPS;
    }
    private static boolean splitNeeded(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology.Node leaf,EntranceSeamDocument.EdgeCut edge){
        double ax=x0(world,leaf),az=z0(world,leaf);
        double bx=x1(world,leaf),bz=z1(world,leaf);
        if(edge.vertical()){
            double x=edge.x0(),lo=Math.min(edge.z0(),edge.z1()),
                hi=Math.max(edge.z0(),edge.z1());
            if(!overlap(az,bz,lo,hi))return false;
            // Refine when an interior cut intersects a leaf, or when the end
            // of the authored seam needs a matching adaptive edge breakpoint.
            boolean onEdge=x>ax-EPS&&x<bx+EPS;
            boolean interior=x>ax+EPS&&x<bx-EPS;
            return interior||onEdge&&((lo>az+EPS&&lo<bz-EPS)
                ||(hi>az+EPS&&hi<bz-EPS));
        }
        double z=edge.z0(),lo=Math.min(edge.x0(),edge.x1()),
            hi=Math.max(edge.x0(),edge.x1());
        if(!overlap(ax,bx,lo,hi))return false;
        boolean onEdge=z>az-EPS&&z<bz+EPS;
        boolean interior=z>az+EPS&&z<bz-EPS;
        return interior||onEdge&&((lo>ax+EPS&&lo<bx-EPS)
            ||(hi>ax+EPS&&hi<bx-EPS));
    }
    private static boolean intersectsTab(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology.Node leaf,EntranceSeamDocument.DetachedTab tab){
        return overlap(x0(world,leaf),x1(world,leaf),tab.x0(),tab.x1())
            &&overlap(z0(world,leaf),z1(world,leaf),tab.z0(),tab.z1());
    }
    private static boolean contained(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology.Node leaf,EntranceSeamDocument.DetachedTab tab){
        return x0(world,leaf)>=tab.x0()-EPS&&x1(world,leaf)<=tab.x1()+EPS
            &&z0(world,leaf)>=tab.z0()-EPS&&z1(world,leaf)<=tab.z1()+EPS;
    }
    private static List<long[]> sourceCells(StitchedHeightmapWorld world,
            double x0,double z0,double x1,double z1){
        var s=world.settings();
        long xa=(long)Math.floor((x0-s.originX())/s.metresPerCell());
        long xb=(long)Math.floor((x1-s.originX())/s.metresPerCell());
        long za=(long)Math.floor((z0-s.originZ())/s.metresPerCell());
        long zb=(long)Math.floor((z1-s.originZ())/s.metresPerCell());
        if(xb-xa>100||zb-za>100)
            throw new IllegalArgumentException("Entrance operation too large; apply in sections");
        List<long[]> found=new ArrayList<>();
        for(long z=za-1;z<=zb+1;z++)for(long x=xa-1;x<=xb+1;x++){
            long tx=Math.floorDiv(x,s.tileCells()),tz=Math.floorDiv(z,s.tileCells());
            if(tx>=-1_000_000&&tx<=1_000_000&&tz>=-1_000_000&&tz<=1_000_000
                &&world.hasTile((int)tx,(int)tz))found.add(new long[]{x,z});
        }
        return found;
    }
    public static Result apply(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology topology,EntranceSeamDocument document)
            throws IOException {
        var before=topology.snapshot();
        int priorSplits=topology.splitCount(),priorHoles=topology.cutoutCount();
        try{
            for(var edge:document.edges())if(edge.method()==EntranceSeamDocument.Method.EXTRA_SEAM){
                double xa=Math.min(edge.x0(),edge.x1()),xb=Math.max(edge.x0(),edge.x1());
                double za=Math.min(edge.z0(),edge.z1()),zb=Math.max(edge.z0(),edge.z1());
                for(var cell:sourceCells(world,xa,za,xb,zb)){
                    for(int pass=0;pass<=AdaptiveTerrainTopology.MAX_DEPTH;pass++){
                        boolean changed=false;
                        for(var leaf:topology.leaves(cell[0],cell[1])){
                            if(!splitNeeded(world,leaf,edge))continue;
                            topology.split(leaf);changed=true;
                        }
                        if(!changed)break;
                    }
                }
            }
            for(var tab:document.tabs()){
                for(var cell:sourceCells(world,tab.x0(),tab.z0(),tab.x1(),tab.z1())){
                    for(int pass=0;pass<=AdaptiveTerrainTopology.MAX_DEPTH;pass++){
                        boolean changed=false;
                        for(var leaf:topology.leaves(cell[0],cell[1])){
                            if(!intersectsTab(world,leaf,tab)||contained(world,leaf,tab))
                                continue;
                            topology.split(leaf);changed=true;
                        }
                        if(!changed)break;
                    }
                    for(var leaf:topology.leaves(cell[0],cell[1])){
                        if(!intersectsTab(world,leaf,tab))continue;
                        if(!contained(world,leaf,tab))
                            throw new IllegalArgumentException(
                                "Tab boundaries finer than adaptive terrain can represent");
                        if(!topology.isCutout(leaf))topology.punch(leaf);
                    }
                }
            }
            topology.save();
            if(!AdaptiveTerrainTopology.open(world).snapshot().equals(topology.snapshot()))
                throw new IOException("Conformed adaptive topology failed disk readback");
            return new Result(topology.splitCount()-priorSplits,
                topology.cutoutCount()-priorHoles);
        }catch(IOException|RuntimeException failed){
            topology.restore(before);
            throw failed;
        }
    }
}
