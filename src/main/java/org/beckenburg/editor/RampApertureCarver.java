package org.beckenburg.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Opt-in cutout authoring for a smooth cave entrance. Never edits PNG heights;
 * only marks adaptive surface leaves absent where the roof intersects ramp
 * headroom, preserving the original open mouth and deep overhead cave roof.
 */
public final class RampApertureCarver {
    private static final int MAX_BASE_CELLS=500;
    private static final double MIN_CUT_GAP=.025;
    public record CarveResult(int newCutouts,int newSplits,boolean changed) {}
    private RampApertureCarver(){}

    public static CarveResult carve(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology topology,LayeredTerrainQueries.Ramp ramp,
            LayeredTerrainQueries.Agent actor)throws IOException {
        Objects.requireNonNull(world);Objects.requireNonNull(topology);
        Objects.requireNonNull(ramp);Objects.requireNonNull(actor);
        var settings=world.settings();
        double unit=settings.metresPerCell();
        long lx=(long)Math.floor((ramp.x0()-settings.originX())/unit);
        long hx=(long)Math.ceil((ramp.x1()-settings.originX())/unit)-1;
        long lz=(long)Math.floor((ramp.z0()-settings.originZ())/unit);
        long hz=(long)Math.ceil((ramp.z1()-settings.originZ())/unit)-1;
        if(hx<lx||hz<lz||hx-lx+1>MAX_BASE_CELLS
            ||hz-lz+1>MAX_BASE_CELLS
            ||(hx-lx+1)*(hz-lz+1)>MAX_BASE_CELLS)
            throw new IllegalArgumentException("Carve a smaller ramp area at a time");
        var before=topology.snapshot();
        int oldCut=topology.cutoutCount(),oldSplit=topology.splitCount();
        try{
            for(long gz=lz;gz<=hz;gz++)for(long gx=lx;gx<=hx;gx++){
                long tx=Math.floorDiv(gx,settings.tileCells());
                long tz=Math.floorDiv(gz,settings.tileCells());
                if(tx < -1_000_000||tx>1_000_000||tz< -1_000_000||tz>1_000_000
                    ||!world.hasTile((int)tx,(int)tz))
                    throw new IOException("Cannot carve ramp over missing source tile");
                refine(world,topology,ramp,actor,gx,gz,unit);
            }
            return new CarveResult(topology.cutoutCount()-oldCut,
                topology.splitCount()-oldSplit,
                topology.cutoutCount()!=oldCut||topology.splitCount()!=oldSplit);
        }catch(IOException|RuntimeException failure){
            topology.restore(before);
            throw failure;
        }
    }
    private static void refine(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology topo,LayeredTerrainQueries.Ramp ramp,
            LayeredTerrainQueries.Agent actor,long gx,long gz,double cell)
            throws IOException {
        boolean changed;
        int iterations=0;
        do{
            changed=false;
            for(var leaf:topo.leaves(gx,gz)){
                if(topo.isCutout(leaf))continue;
                double scale=cell/AdaptiveTerrainTopology.UNITS_PER_CELL;
                double x0=world.settings().originX()+leaf.x0()*scale;
                double z0=world.settings().originZ()+leaf.z0()*scale;
                double x1=world.settings().originX()+leaf.x1()*scale;
                double z1=world.settings().originZ()+leaf.z1()*scale;
                if(x0>=ramp.x1()||x1<=ramp.x0()
                    ||z0>=ramp.z1()||z1<=ramp.z0())continue;
                double x=(x0+x1)*.5,z=(z0+z1)*.5;
                double progress=ramp.axis(x,z);
                // Keep at least 0.65 m of the original mouth accessible.
                // This gives the physical RAMP portal an intact source lip.
                if(progress<Math.max(.65,actor.radius()*3))continue;
                if(!ramp.contains(x,z))continue;
                double ground=world.height(x,z),floor=ramp.height(x,z);
                double gap=ground-floor;
                boolean ceiling=gap>MIN_CUT_GAP
                    &&gap<actor.height()+actor.radius()*.5;
                if(!ceiling)continue;
                boolean inside=x0>=ramp.x0()&&x1<=ramp.x1()
                    &&z0>=ramp.z0()&&z1<=ramp.z1();
                if((!inside||(x1-x0)>.25+1e-7)&&leaf.depth()<AdaptiveTerrainTopology.MAX_DEPTH
                    &&(x1-x0)/2>=AdaptiveTerrainTopology.MIN_LEAF_METRES-1e-7){
                    topo.split(leaf);changed=true;break;
                }
                // Never eliminate terrain outside the ramp footprint.
                if(!inside)continue;
                topo.punch(leaf);
                changed=true;break;
            }
            if(++iterations>5000)
                throw new IllegalStateException("Ramp cutout refinement budget exceeded");
        }while(changed);
    }
}
