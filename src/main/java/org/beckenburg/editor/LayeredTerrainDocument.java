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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Human-authored underground collision-query surfaces, solid obstacles, and
 * explicit cross-layer navigation links. Separate from the 16-bit heightmap,
 * cutout topology and generated navigation graph.
 *
 * These are real authoring/query shapes, NOT rendered cave meshes or runtime
 * physical movement. Game collision integration is a later controlled step.
 */
public final class LayeredTerrainDocument {
    public static final int FORMAT_VERSION=1;
    private static final int MAGIC=0x424C4159; // BLAY
    private static final int MAX_ITEMS=1024, MAX_BYTES=2_000_000;
    private final Path path;
    private final ArrayList<LayeredTerrainQueries.Floor> floors=new ArrayList<>();
    private final ArrayList<LayeredTerrainQueries.Solid> solids=new ArrayList<>();
    private final ArrayList<AdaptiveNavigationRegions.Transition> links=new ArrayList<>();
    private long revision;

    public record Snapshot(List<LayeredTerrainQueries.Floor> floors,
                           List<LayeredTerrainQueries.Solid> solids,
                           List<AdaptiveNavigationRegions.Transition> links) {
        public Snapshot {
            floors=List.copyOf(floors);
            solids=List.copyOf(solids);
            links=List.copyOf(links);
        }
    }
    private LayeredTerrainDocument(Path path){this.path=path;}
    public Path path(){return path;}
    public long revision(){return revision;}
    public List<LayeredTerrainQueries.Floor> floors(){return List.copyOf(floors);}
    public List<LayeredTerrainQueries.Solid> solids(){return List.copyOf(solids);}
    public List<AdaptiveNavigationRegions.Transition> links(){return List.copyOf(links);}
    public Snapshot snapshot(){return new Snapshot(floors(),solids(),links());}
    public static LayeredTerrainDocument open(StitchedHeightmapWorld world)throws IOException {
        Objects.requireNonNull(world);
        var file=world.directory().resolve("layered-geometry.bin");
        var result=new LayeredTerrainDocument(file);
        if(!Files.exists(file))return result;
        if(Files.size(file)>MAX_BYTES)throw new IOException("Oversized layered authoring file");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))){
            if(in.readInt()!=MAGIC||in.readInt()!=FORMAT_VERSION)
                throw new IOException("Invalid layered geometry format");
            int f=count(in),s=count(in),l=count(in);
            var floors=new ArrayList<LayeredTerrainQueries.Floor>();
            var solids=new ArrayList<LayeredTerrainQueries.Solid>();
            var links=new ArrayList<AdaptiveNavigationRegions.Transition>();
            for(int i=0;i<f;i++)floors.add(new LayeredTerrainQueries.Floor(
                in.readUTF(),in.readDouble(),in.readDouble(),in.readDouble(),
                in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()));
            for(int i=0;i<s;i++)solids.add(new LayeredTerrainQueries.Solid(
                in.readUTF(),in.readDouble(),in.readDouble(),in.readDouble(),
                in.readDouble(),in.readDouble(),in.readDouble()));
            for(int i=0;i<l;i++){
                String from=in.readUTF(),to=in.readUTF();
                double fx=in.readDouble(),fz=in.readDouble(),tx=in.readDouble(),tz=in.readDouble();
                int kind=in.readUnsignedByte();
                if(kind>=AdaptiveNavigationRegions.Connection.values().length)
                    throw new IOException("Invalid authored traversal type");
                links.add(new AdaptiveNavigationRegions.Transition(from,to,fx,fz,tx,tz,
                    AdaptiveNavigationRegions.Connection.values()[kind]));
            }
            if(in.read()!=-1)throw new IOException("Trailing layered authoring data");
            result.restore(new Snapshot(floors,solids,links));
        }catch(IllegalArgumentException problem){
            throw new IOException("Invalid layered geometry authoring data",problem);
        }
        return result;
    }
    private static int count(DataInputStream in)throws IOException {
        int n=in.readInt();
        if(n<0||n>MAX_ITEMS)throw new IOException("Unsafe layered geometry count");
        return n;
    }
    private static void validate(Snapshot snapshot){
        if(snapshot.floors().size()>MAX_ITEMS||snapshot.solids().size()>MAX_ITEMS
            ||snapshot.links().size()>MAX_ITEMS)
            throw new IllegalArgumentException("Layered geometry budget exceeded");
        Set<String> ids=new HashSet<>();
        ids.add(LayeredTerrainQueries.SURFACE);
        for(var floor:snapshot.floors())ids.add(floor.layer());
        for(int i=0;i<snapshot.floors().size();i++){
            var a=snapshot.floors().get(i);
            for(int j=i+1;j<snapshot.floors().size();j++){
                var b=snapshot.floors().get(j);
                if(a.layer().equals(b.layer())
                    &&a.x0()<b.x1()&&a.x1()>b.x0()
                    &&a.z0()<b.z1()&&a.z1()>b.z0())
                    throw new IllegalArgumentException("Overlapping walkable floors on "+a.layer());
            }
        }
        Set<String> solidIds=new HashSet<>();
        for(var solid:snapshot.solids())
            if(!solidIds.add(solid.id()))
                throw new IllegalArgumentException("Duplicate solid ID "+solid.id());
        for(var link:snapshot.links()){
            if(!ids.contains(link.from())||!ids.contains(link.to()))
                throw new IllegalArgumentException("Transition references missing floor layer");
            if(!supports(snapshot.floors(),link.from(),link.fromX(),link.fromZ())
                ||!supports(snapshot.floors(),link.to(),link.toX(),link.toZ()))
                throw new IllegalArgumentException("Transition endpoint lies outside authored floor");
        }
    }
    private static boolean supports(List<LayeredTerrainQueries.Floor> floors,
                                    String layer,double x,double z){
        if(layer.equals(LayeredTerrainQueries.SURFACE))return true;
        for(var floor:floors)
            if(floor.layer().equals(layer)&&floor.contains(x,z))return true;
        return false;
    }
    public void restore(Snapshot snapshot){
        Objects.requireNonNull(snapshot);
        validate(snapshot);
        floors.clear();floors.addAll(snapshot.floors());
        solids.clear();solids.addAll(snapshot.solids());
        links.clear();links.addAll(snapshot.links());
        revision++;
    }
    public void addFloor(LayeredTerrainQueries.Floor floor){
        Objects.requireNonNull(floor);
        if(floors.contains(floor))return;
        var list=new ArrayList<>(floors);list.add(floor);
        restore(new Snapshot(list,solids,links));
    }
    public void removeFloor(LayeredTerrainQueries.Floor floor){
        var list=new ArrayList<>(floors);
        if(list.remove(floor))restore(new Snapshot(list,solids,links));
    }
    public void addSolid(LayeredTerrainQueries.Solid solid){
        Objects.requireNonNull(solid);
        if(solids.contains(solid))return;
        var list=new ArrayList<>(solids);list.add(solid);
        restore(new Snapshot(floors,list,links));
    }
    public void removeSolid(LayeredTerrainQueries.Solid solid){
        var list=new ArrayList<>(solids);
        if(list.remove(solid))restore(new Snapshot(floors,list,links));
    }
    public void addLink(AdaptiveNavigationRegions.Transition link){
        Objects.requireNonNull(link);
        if(links.contains(link))return;
        var list=new ArrayList<>(links);list.add(link);
        restore(new Snapshot(floors,solids,list));
    }
    public void removeLink(AdaptiveNavigationRegions.Transition link){
        var list=new ArrayList<>(links);
        if(list.remove(link))restore(new Snapshot(floors,solids,list));
    }
    public void save()throws IOException {
        validate(snapshot());
        Files.createDirectories(path.getParent());
        Path temp=Files.createTempFile(path.getParent(),".layered-geometry-",".tmp");
        try{
            try(var out=new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(FORMAT_VERSION);
                out.writeInt(floors.size());out.writeInt(solids.size());out.writeInt(links.size());
                for(var f:floors){
                    out.writeUTF(f.layer());out.writeDouble(f.x0());out.writeDouble(f.z0());
                    out.writeDouble(f.x1());out.writeDouble(f.z1());out.writeDouble(f.y0());
                    out.writeDouble(f.riseX());out.writeDouble(f.riseZ());
                }
                for(var s:solids){
                    out.writeUTF(s.id());out.writeDouble(s.x0());out.writeDouble(s.y0());
                    out.writeDouble(s.z0());out.writeDouble(s.x1());out.writeDouble(s.y1());
                    out.writeDouble(s.z1());
                }
                for(var l:links){
                    out.writeUTF(l.from());out.writeUTF(l.to());
                    out.writeDouble(l.fromX());out.writeDouble(l.fromZ());
                    out.writeDouble(l.toX());out.writeDouble(l.toZ());
                    out.writeByte(l.connection().ordinal());
                }
            }
            Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
}
