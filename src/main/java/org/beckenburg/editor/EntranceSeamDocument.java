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

/**
 * Three explicitly different terrain entrance operations:
 * EXTRA_SEAM adds a new interior source-tile boundary where an aperture is authored;
 * TILE_DESTITCH separates an existing source tile edge for underground attachment;
 * DETACHED_TAB removes a source patch from the outer surface navigation layer and
 * reassigns its identical source height to an independent walkable tab layer.
 *
 * Render mesh emission remains a separate downstream consumer. This document
 * supplies stable world-space ownership and seam constraints to the query/nav layer.
 */
public final class EntranceSeamDocument {
    private static final int MAGIC=0x4245534d; // BESM
    public static final int VERSION=1,MAX_ITEMS=1024;
    public enum Method { EXTRA_SEAM,TILE_DESTITCH }
    public record EdgeCut(String id,Method method,String destination,
                          double x0,double z0,double x1,double z1) {
        public EdgeCut {
            if(id==null||id.isBlank()||destination==null||destination.isBlank()
                ||method==null||!finite(x0,z0,x1,z1)
                ||Math.hypot(x1-x0,z1-z0)<.125
                ||Math.abs(x1-x0)>1e-7&&Math.abs(z1-z0)>1e-7)
                throw new IllegalArgumentException("Entrance seam must be an axis-aligned segment");
        }
        public boolean vertical(){return Math.abs(x0-x1)<1e-7;}
        public boolean crosses(double ax,double az,double bx,double bz){
            if(vertical()){
                double mid=x0;
                if((ax-mid)*(bx-mid)>=-1e-12)return false;
                double z=az+(bz-az)*(mid-ax)/(bx-ax);
                return z>=Math.min(z0,z1)-1e-7&&z<=Math.max(z0,z1)+1e-7;
            }
            double mid=z0;
            if((az-mid)*(bz-mid)>=-1e-12)return false;
            double x=ax+(bx-ax)*(mid-az)/(bz-az);
            return x>=Math.min(x0,x1)-1e-7&&x<=Math.max(x0,x1)+1e-7;
        }
        public boolean intersectsInterior(double xa,double za,double xb,double zb){
            if(vertical())return x0>xa+1e-8&&x0<xb-1e-8
                &&Math.max(z0,z1)>za+1e-8&&Math.min(z0,z1)<zb-1e-8;
            return z0>za+1e-8&&z0<zb-1e-8
                &&Math.max(x0,x1)>xa+1e-8&&Math.min(x0,x1)<xb-1e-8;
        }
    }
    public record DetachedTab(String id,String layer,String destination,
                              double x0,double z0,double x1,double z1) {
        public DetachedTab {
            if(id==null||id.isBlank()||layer==null||layer.isBlank()
                ||layer.equals(LayeredTerrainQueries.SURFACE)
                ||destination==null||destination.isBlank()
                ||!finite(x0,z0,x1,z1)||x1-x0<.125||z1-z0<.125)
                throw new IllegalArgumentException("Invalid detached nav tab");
        }
        public boolean contains(double x,double z){
            return x>=x0&&x<x1&&z>=z0&&z<z1;
        }
    }
    public record Snapshot(List<EdgeCut> edges,List<DetachedTab> tabs){
        public Snapshot { edges=List.copyOf(edges);tabs=List.copyOf(tabs); }
    }
    private final Path path;
    private final StitchedHeightmapWorld world;
    private final ArrayList<EdgeCut> edges=new ArrayList<>();
    private final ArrayList<DetachedTab> tabs=new ArrayList<>();
    private long revision;
    private EntranceSeamDocument(StitchedHeightmapWorld world){
        this.world=world;
        path=world.directory().resolve("entrance-seams.bin");
    }
    private static boolean finite(double... values){
        for(double v:values)if(!Double.isFinite(v))return false;
        return true;
    }
    public Path path(){return path;}
    public long revision(){return revision;}
    public List<EdgeCut> edges(){return List.copyOf(edges);}
    public List<DetachedTab> tabs(){return List.copyOf(tabs);}
    public Snapshot snapshot(){return new Snapshot(edges(),tabs());}
    public static EntranceSeamDocument open(StitchedHeightmapWorld world)throws IOException{
        var doc=new EntranceSeamDocument(Objects.requireNonNull(world));
        if(!Files.exists(doc.path))return doc;
        if(Files.size(doc.path)>400_000)throw new IOException("Oversized entrance seams");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(doc.path)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION)
                throw new IOException("Unsupported entrance seam document");
            int count=in.readInt(),tabs=in.readInt();
            if(count<0||count>MAX_ITEMS||tabs<0||tabs>MAX_ITEMS)
                throw new IOException("Invalid entrance seam counts");
            var cuts=new ArrayList<EdgeCut>();
            var patches=new ArrayList<DetachedTab>();
            for(int i=0;i<count;i++){
                String id=in.readUTF(),dest=in.readUTF();
                int method=in.readUnsignedByte();
                if(method>=Method.values().length)throw new IOException("Unknown entrance method");
                cuts.add(new EdgeCut(id,Method.values()[method],dest,
                    in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()));
            }
            for(int i=0;i<tabs;i++)patches.add(new DetachedTab(in.readUTF(),in.readUTF(),
                in.readUTF(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()));
            if(in.read()!=-1)throw new IOException("Trailing entrance authoring data");
            doc.restore(new Snapshot(cuts,patches));
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid entrance seams",invalid);}
        return doc;
    }
    private boolean gridLine(double coordinate,double origin){
        double base=world.settings().metresPerCell();
        double q=(coordinate-origin)/base;
        return Math.abs(q-Math.rint(q))<1e-7;
    }
    private boolean lattice(double coordinate,double origin){
        double step=world.settings().metresPerCell()/AdaptiveTerrainTopology.UNITS_PER_CELL;
        double q=(coordinate-origin)/step;
        return Math.abs(q-Math.rint(q))<1e-7;
    }
    private void validate(Snapshot data){
        if(data.edges().size()>MAX_ITEMS||data.tabs().size()>MAX_ITEMS)
            throw new IllegalArgumentException("Entrance topology budget exceeded");
        var ids=new HashSet<String>();
        var layers=new HashSet<String>();
        var settings=world.settings();
        double tile=settings.tileCells()*settings.metresPerCell();
        for(var e:data.edges()){
            if(!ids.add(e.id()))throw new IllegalArgumentException("Duplicate seam/tab ID");
            if(!lattice(e.x0(),settings.originX())
                ||!lattice(e.x1(),settings.originX())
                ||!lattice(e.z0(),settings.originZ())
                ||!lattice(e.z1(),settings.originZ()))
                throw new IllegalArgumentException("Seam endpoints must align with adaptive terrain lattice");
            double pos=e.vertical()?e.x0():e.z0();
            double origin=e.vertical()?settings.originX():settings.originZ();
            double normalized=(pos-origin)/tile;
            boolean atTileEdge=Math.abs(normalized-Math.rint(normalized))<1e-7;
            if(e.method()==Method.TILE_DESTITCH&&!atTileEdge)
                throw new IllegalArgumentException("Method 2 must select an existing tile boundary");
            if(e.method()==Method.EXTRA_SEAM&&atTileEdge)
                throw new IllegalArgumentException("Method 1 must create a new interior tile seam");
        }
        for(var tab:data.tabs()){
            if(!ids.add(tab.id())||!layers.add(tab.layer()))
                throw new IllegalArgumentException("Duplicate tab ID or layer");
            if(!lattice(tab.x0(),settings.originX())||!lattice(tab.x1(),settings.originX())
                ||!lattice(tab.z0(),settings.originZ())||!lattice(tab.z1(),settings.originZ()))
                throw new IllegalArgumentException("Tab bounds must align with terrain lattice");
            for(var other:data.tabs())if(other!=tab&&
                tab.x0()<other.x1()&&tab.x1()>other.x0()
                &&tab.z0()<other.z1()&&tab.z1()>other.z0())
                throw new IllegalArgumentException("Detached tabs must not overlap");
            boolean seeded=false;
            for(var e:data.edges())if(e.method()==Method.EXTRA_SEAM
                &&e.destination().equals(tab.destination())
                &&(e.vertical()&&(Math.abs(e.x0()-tab.x0())<1e-7
                    ||Math.abs(e.x0()-tab.x1())<1e-7)
                    &&Math.min(e.z0(),e.z1())<=tab.z0()
                    &&Math.max(e.z0(),e.z1())>=tab.z1()
                  ||!e.vertical()&&(Math.abs(e.z0()-tab.z0())<1e-7
                    ||Math.abs(e.z0()-tab.z1())<1e-7)
                    &&Math.min(e.x0(),e.x1())<=tab.x0()
                    &&Math.max(e.x0(),e.x1())>=tab.x1()))seeded=true;
            if(!seeded)
                throw new IllegalArgumentException("Method 3 needs a matching method-1 seam along the tab edge");
        }
    }
    public void restore(Snapshot state){
        Objects.requireNonNull(state);validate(state);
        edges.clear();edges.addAll(state.edges());
        tabs.clear();tabs.addAll(state.tabs());revision++;
    }
    public void addEdge(EdgeCut e){
        var next=new ArrayList<>(edges);if(next.contains(e))return;next.add(e);
        restore(new Snapshot(next,tabs));
    }
    public void addTab(DetachedTab t){
        var next=new ArrayList<>(tabs);if(next.contains(t))return;next.add(t);
        restore(new Snapshot(edges,next));
    }
    public void removeEdge(EdgeCut e){
        var next=new ArrayList<>(edges);if(next.remove(e))restore(new Snapshot(next,tabs));
    }
    public void removeTab(DetachedTab t){
        var next=new ArrayList<>(tabs);if(next.remove(t))restore(new Snapshot(edges,next));
    }
    public boolean blocksSurface(double ax,double az,double bx,double bz){
        for(var e:edges)if(e.crosses(ax,az,bx,bz))return true;
        return false;
    }
    public boolean crossesFeature(String layer,LayeredTerrainQueries.Rect area){
        if(layer.equals(LayeredTerrainQueries.SURFACE)){
            for(var e:edges)if(e.intersectsInterior(
                area.x0(),area.z0(),area.x1(),area.z1()))return true;
            for(var t:tabs)if(area.crosses(new LayeredTerrainQueries.Rect(
                t.x0(),t.z0(),t.x1(),t.z1())))return true;
        }else for(var t:tabs)if(t.layer().equals(layer)&&
                area.crosses(new LayeredTerrainQueries.Rect(t.x0(),t.z0(),t.x1(),t.z1())))
                return true;
        return false;
    }
    /** Return only physically co-located walk transitions, never imagined
     * links from an underground destination to a high-altitude surface. */
    public List<AdaptiveNavigationRegions.Transition> rampConnections(RampEntranceDocument ramps){
        var links=new ArrayList<AdaptiveNavigationRegions.Transition>();
        for(var ramp:ramps.ramps()){
            double x=ramp.entranceX(.3),z=ramp.entranceZ(.3);
            String start=LayeredTerrainQueries.SURFACE;
            for(var tab:tabs)if(tab.contains(x,z)){start=tab.layer();break;}
            links.add(new AdaptiveNavigationRegions.Transition(start,ramp.layer(),
                x,z,x,z,AdaptiveNavigationRegions.Connection.RAMP));
            links.add(new AdaptiveNavigationRegions.Transition(ramp.layer(),ramp.targetLayer(),
                ramp.exitX(.3),ramp.exitZ(.3),ramp.exitX(.3),ramp.exitZ(.3),
                AdaptiveNavigationRegions.Connection.RAMP));
        }
        return List.copyOf(links);
    }
    public void save()throws IOException{
        validate(snapshot());
        Path tmp=Files.createTempFile(path.getParent(),".entrance-seam-",".tmp");
        try{
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);
                out.writeInt(edges.size());out.writeInt(tabs.size());
                for(var e:edges){
                    out.writeUTF(e.id());out.writeUTF(e.destination());
                    out.writeByte(e.method().ordinal());
                    out.writeDouble(e.x0());out.writeDouble(e.z0());
                    out.writeDouble(e.x1());out.writeDouble(e.z1());
                }
                for(var t:tabs){
                    out.writeUTF(t.id());out.writeUTF(t.layer());out.writeUTF(t.destination());
                    out.writeDouble(t.x0());out.writeDouble(t.z0());
                    out.writeDouble(t.x1());out.writeDouble(t.z1());
                }
            }
            Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(tmp);}
    }
}
