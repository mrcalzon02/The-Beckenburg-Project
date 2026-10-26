package org.beckenburg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Scene-owned, renderer-thread-only static fixture probe cache. Does not
 * incorporate handheld UV, ghosts, flashlight or camera-distance LOD.
 */
public final class PreparedFixtureProbeCache {
    public record Probe(int room,double x,double y,double z,double nx,double ny,double nz){
        public Probe{
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||
                !Double.isFinite(nx)||!Double.isFinite(ny)||!Double.isFinite(nz))
                throw new IllegalArgumentException("Finite world-space probe required");
        }
        public boolean normal(){return nx!=0||ny!=0||nz!=0;}
    }
    public record Refresh(int recomputed,int retained,List<Integer> indices,
                          List<FixtureLighting.RGB> colours){
        public Refresh{indices=List.copyOf(indices);colours=List.copyOf(colours);}
    }
    private final FixtureLighting lighting;
    private final List<Probe> probes;
    private final long[] versions;
    private final FixtureLighting.RGB[] values;
    public PreparedFixtureProbeCache(FixtureLighting owner,List<Probe> probes){
        lighting=Objects.requireNonNull(owner,"scene fixture lighting");
        this.probes=List.copyOf(Objects.requireNonNull(probes,"authored probes"));
        versions=new long[this.probes.size()];
        values=new FixtureLighting.RGB[this.probes.size()];
    }
    /** Only changed influence cells or occluder revisions generate rays.
     * Never expose the mutable backing cache to the GPU or callers. */
    public Refresh refresh(){
        var changed=new ArrayList<Integer>();
        for(int i=0;i<probes.size();i++){
            Probe p=probes.get(i);
            long revision=lighting.regionRevision(p.x(),p.y(),p.z());
            if(values[i]!=null&&versions[i]==revision)continue;
            values[i]=p.normal()
                ?lighting.sampleNormal(p.room(),p.x(),p.y(),p.z(),p.nx(),p.ny(),p.nz())
                :lighting.sample(p.room(),p.x(),p.y(),p.z());
            versions[i]=revision;
            changed.add(i);
        }
        return new Refresh(changed.size(),probes.size()-changed.size(),
            changed,Arrays.asList(values.clone()));
    }
    public int size(){return probes.size();}
}
