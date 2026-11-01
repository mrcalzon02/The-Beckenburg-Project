package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * One shared surface-height calculation for authored mesh geometry and
 * navigation/collision queries. Samples the exact same diagonal triangles as
 * StitchedHeightmapWorld, with the non-destructive layers composed beforehand.
 * The authoritative 16-bit source pixels remain unchanged.
 */
public final class TerrainComposedHeightSampler {
    private final StitchedHeightmapWorld world;
    private final int n;
    private final Map<StitchedHeightmapWorld.Tile,Optional<TerrainLayerStack>> cached=
        new LinkedHashMap<>(5,.75f,true){
            @Override protected boolean removeEldestEntry(
                    Map.Entry<StitchedHeightmapWorld.Tile,Optional<TerrainLayerStack>> e){
                return size()>4;
            }
        };
    public TerrainComposedHeightSampler(StitchedHeightmapWorld world){
        this.world=Objects.requireNonNull(world);n=world.settings().tileCells();
    }
    private Optional<TerrainLayerStack> layers(int x,int z)throws IOException{
        var key=new StitchedHeightmapWorld.Tile(x,z);
        if(cached.containsKey(key))return cached.get(key);
        Path file=world.directory().resolve("terrain-layers")
            .resolve("tile_"+x+"_"+z+".bin");
        Optional<TerrainLayerStack> result=Files.isRegularFile(file)?
            Optional.of(new TerrainLayerStack(world,x,z)):Optional.empty();
        cached.put(key,result);
        return result;
    }
    private static boolean exactBorder(double f){
        return Math.abs(f-Math.rint(f))<1e-9;
    }
    /**
     * Sparse tile far borders use the existing tile, never an invented neighbor.
     * A real adjacent tile is preferred if it owns the queried interval.
     */
    public double height(double x,double z)throws IOException{
        if(!Double.isFinite(x)||!Double.isFinite(z))
            throw new IllegalArgumentException("Non-finite composed terrain point");
        var settings=world.settings();
        double span=settings.tileWidthMetres();
        double gridX=(x-settings.originX())/span;
        double gridZ=(z-settings.originZ())/span;
        if(gridX< -1_000_000||gridX>=1_000_001||
            gridZ< -1_000_000||gridZ>=1_000_001)
            throw new IllegalArgumentException("World point outside supported tile space");
        int tx=(int)Math.floor(gridX),tz=(int)Math.floor(gridZ);
        int[] ownersX=exactBorder(gridX)?new int[]{tx,tx-1}:new int[]{tx};
        int[] ownersZ=exactBorder(gridZ)?new int[]{tz,tz-1}:new int[]{tz};
        for(int ownerZ:ownersZ)for(int ownerX:ownersX){
            if(Math.abs((long)ownerX)>1_000_000||
                Math.abs((long)ownerZ)>1_000_000||
                !world.hasTile(ownerX,ownerZ))continue;
            double localX=(gridX-ownerX)*n,localZ=(gridZ-ownerZ)*n;
            if(localX< -1e-7||localZ< -1e-7||
                localX>n+1e-7||localZ>n+1e-7)continue;
            localX=Math.max(0,Math.min(n,localX));
            localZ=Math.max(0,Math.min(n,localZ));
            int ix=Math.min(n-1,(int)Math.floor(localX));
            int iz=Math.min(n-1,(int)Math.floor(localZ));
            double fx=localX-ix,fz=localZ-iz;
            var layer=layers(ownerX,ownerZ);
            double a,b,c,d;
            if(layer.isPresent()){
                var t=layer.get();
                a=t.height(ix,iz);b=t.height(ix+1,iz);
                c=t.height(ix+1,iz+1);d=t.height(ix,iz+1);
            }else {
                a=world.metres(world.sample(ownerX,ownerZ,ix,iz));
                b=world.metres(world.sample(ownerX,ownerZ,ix+1,iz));
                c=world.metres(world.sample(ownerX,ownerZ,ix+1,iz+1));
                d=world.metres(world.sample(ownerX,ownerZ,ix,iz+1));
            }
            return fz<=fx?a+(b-a)*fx+(c-b)*fz:
                a+(c-d)*fx+(d-a)*fz;
        }
        throw new IOException("No stitched terrain tile owns world position "+x+","+z);
    }
}
