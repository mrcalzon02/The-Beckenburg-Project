package org.beckenburg.editor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persisted ramp entrances are separate from pre-existing BLAY v1 documents.
 * A ramp is a physical C1-continuous height profile, not a teleport link.
 * Mouth and exit are anchored to authored layers with same-X/Z smooth links.
 */
public final class RampEntranceDocument {
    private static final int MAGIC=0x4252414d; // BRAM
    public static final int FORMAT=1,MAX_RAMPS=512;
    private final Path file;
    private final ArrayList<LayeredTerrainQueries.Ramp> ramps=new ArrayList<>();
    private long revision;
    private RampEntranceDocument(Path file){this.file=file;}
    public Path path(){return file;}
    public long revision(){return revision;}
    public List<LayeredTerrainQueries.Ramp> ramps(){return List.copyOf(ramps);}
    public record Snapshot(List<LayeredTerrainQueries.Ramp> ramps){
        public Snapshot {ramps=List.copyOf(ramps);}
    }
    public Snapshot snapshot(){return new Snapshot(ramps());}
    public static RampEntranceDocument open(StitchedHeightmapWorld source)throws IOException{
        Objects.requireNonNull(source);
        var doc=new RampEntranceDocument(source.directory().resolve("ramp-entrances.bin"));
        if(!Files.exists(doc.file))return doc;
        if(Files.size(doc.file)>250_000)throw new IOException("Ramp file too large");
        try(var in=new DataInputStream(new BufferedInputStream(
                Files.newInputStream(doc.file)))){
            if(in.readInt()!=MAGIC||in.readInt()!=FORMAT)
                throw new IOException("Unsupported ramp authoring format");
            int count=in.readInt();
            if(count<0||count>MAX_RAMPS)throw new IOException("Ramp authoring budget exceeded");
            for(int i=0;i<count;i++){
                String id=in.readUTF(),layer=in.readUTF(),target=in.readUTF();
                int dir=in.readUnsignedByte();
                if(dir>=LayeredTerrainQueries.Ramp.Direction.values().length)
                    throw new IOException("Unknown ramp direction");
                doc.ramps.add(new LayeredTerrainQueries.Ramp(id,layer,target,
                    LayeredTerrainQueries.Ramp.Direction.values()[dir],
                    in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),
                    in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()));
            }
            if(in.read()!=-1)throw new IOException("Trailing ramp authoring data");
            validate(doc.ramps);
        }catch(IllegalArgumentException error){
            throw new IOException("Invalid ramp geometry",error);
        }
        return doc;
    }
    private static void validate(List<LayeredTerrainQueries.Ramp> ramps){
        if(ramps.size()>MAX_RAMPS)throw new IllegalArgumentException("Too many ramps");
        var ids=new java.util.HashSet<String>();
        var layers=new java.util.HashSet<String>();
        for(var r:ramps){
            if(!ids.add(r.id())||!layers.add(r.layer()))
                throw new IllegalArgumentException("Duplicate ramp ID or layer");
        }
    }
    public void restore(Snapshot state){
        Objects.requireNonNull(state);
        validate(state.ramps());
        ramps.clear();ramps.addAll(state.ramps());
        revision++;
    }
    public void add(LayeredTerrainQueries.Ramp ramp){
        Objects.requireNonNull(ramp);
        var next=new ArrayList<>(ramps);
        if(next.contains(ramp))return;
        next.add(ramp);restore(new Snapshot(next));
    }
    public void remove(LayeredTerrainQueries.Ramp ramp){
        var next=new ArrayList<>(ramps);
        if(next.remove(ramp))restore(new Snapshot(next));
    }
    /**
     * Reject phantom mouths, impassably steep curves and disjoint exit floors.
     * This does NOT pretend that the terrain has already been excavated.
     */
    public static List<String> audit(StitchedHeightmapWorld world,
            AdaptiveTerrainTopology topology,LayeredTerrainDocument geometry,
            LayeredTerrainQueries.Ramp ramp,
            LayeredTerrainQueries.Agent agent)throws IOException {
        var issues=new ArrayList<String>();
        if(!ramp.targetLayer().equals(LayeredTerrainQueries.SURFACE)
            &&geometry.floors().stream().noneMatch(f->f.layer().equals(ramp.targetLayer())))
            issues.add("Destination floor layer does not exist");
        var physical=new LayeredTerrainQueries(world,topology,
            geometry.floors(),geometry.solids(),List.of(ramp));
        double delta=.3;
        double x=ramp.entranceX(delta),z=ramp.entranceZ(delta);
        var surface=physical.support("surface",x,z);
        var rampMouth=physical.support(ramp.layer(),x,z);
        if(surface==null)issues.add("Surface at ramp mouth is missing or cut out");
        else if(Math.abs(surface.y()-rampMouth.y())>agent.maxStep())
            issues.add("Ramp lip does not meet source heightmap within agent step clearance");
        for(double offset:new double[]{-.25,0,.25}){
            double sx=x,sz=z;
            if(ramp.direction()==LayeredTerrainQueries.Ramp.Direction.EAST
                ||ramp.direction()==LayeredTerrainQueries.Ramp.Direction.WEST)
                sz=(ramp.z0()+ramp.z1())*.5+offset*ramp.width();
            else sx=(ramp.x0()+ramp.x1())*.5+offset*ramp.width();
            var lip=physical.support("surface",sx,sz);
            if(lip==null||Math.abs(lip.y()-ramp.height(sx,sz))>agent.maxStep())
                issues.add("Mouth width does not follow existing surface topology");
        }
        double bx=ramp.exitX(delta),bz=ramp.exitZ(delta);
        var inside=physical.support(ramp.targetLayer(),bx,bz);
        var rampExit=physical.support(ramp.layer(),bx,bz);
        if(inside==null)issues.add("Ramp exit is not over the destination floor");
        else if(Math.abs(inside.y()-rampExit.y())>agent.maxStep())
            issues.add("Ramp exit elevation does not match destination floor");
        int samples=Math.max(40,(int)Math.ceil(ramp.length()/.125));
        double previous=ramp.height(ramp.entranceX(0),ramp.entranceZ(0));
        for(int i=1;i<=samples;i++){
            double at=ramp.length()*i/samples;
            double cx=ramp.entranceX(at),cz=ramp.entranceZ(at);
            double y=ramp.height(cx,cz);
            double grade=Math.abs(y-previous)/(ramp.length()/samples);
            if(grade>agent.slope()+1e-5){
                issues.add("Ramp grade exceeds agent maximum slope");
                break;
            }
            previous=y;
        }
        // Source geometry must be cut where it would be an overhead ceiling;
        // report the offending interval instead of silently deleting terrain.
        for(int i=1;i<samples;i++){
            double at=ramp.length()*i/samples;
            double cx=ramp.entranceX(at),cz=ramp.entranceZ(at);
            if(!physical.canStand(ramp.layer(),cx,cz,agent)){
                issues.add("Ramp lacks footprint/headroom: carve surface opening or clear solids");
                break;
            }
        }
        return List.copyOf(issues);
    }
    /** Same-X/Z contacts; all graph connections are checked for near-zero
     * vertical gap and horizontal displacement before they are accepted. */
    public List<AdaptiveNavigationRegions.Transition> connections(){
        var result=new ArrayList<AdaptiveNavigationRegions.Transition>();
        for(var r:ramps){
            result.add(new AdaptiveNavigationRegions.Transition(
                LayeredTerrainQueries.SURFACE,r.layer(),
                r.entranceX(.3),r.entranceZ(.3),
                r.entranceX(.3),r.entranceZ(.3),
                AdaptiveNavigationRegions.Connection.RAMP));
            result.add(new AdaptiveNavigationRegions.Transition(
                r.layer(),r.targetLayer(),
                r.exitX(.3),r.exitZ(.3),
                r.exitX(.3),r.exitZ(.3),
                AdaptiveNavigationRegions.Connection.RAMP));
        }
        return List.copyOf(result);
    }
    public void save()throws IOException{
        validate(ramps);
        Path temp=Files.createTempFile(file.getParent(),".ramp-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(FORMAT);out.writeInt(ramps.size());
                for(var r:ramps){
                    out.writeUTF(r.id());out.writeUTF(r.layer());out.writeUTF(r.targetLayer());
                    out.writeByte(r.direction().ordinal());
                    out.writeDouble(r.x0());out.writeDouble(r.z0());
                    out.writeDouble(r.x1());out.writeDouble(r.z1());
                    out.writeDouble(r.mouthY());out.writeDouble(r.interiorY());
                    out.writeDouble(r.mouthGrade());out.writeDouble(r.interiorGrade());
                }
            }
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
}
