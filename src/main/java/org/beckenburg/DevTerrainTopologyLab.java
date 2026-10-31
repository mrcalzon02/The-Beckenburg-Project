package org.beckenburg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.beckenburg.editor.AdaptiveTerrainTopology;
import org.beckenburg.editor.StitchedHeightmapWorld;

/**
 * Opt-in, isolated topology test patch for the existing DEV asset showroom.
 * Uses real source PNGs and authoring splits; the game physics adapter is NOT
 * connected and the sample terrain is never registered as a gameplay map.
 */
public final class DevTerrainTopologyLab {
    public static final double PATCH_X=6.15, PATCH_Z=4.0;
    private static final int START_X=12, START_Z=5, CELLS_X=8, CELLS_Z=6;
    private static final int MAX_TRIANGLES=4000;
    public record Vertex(double x,double y,double z){}
    public record Triangle(Vertex a,Vertex b,Vertex c,int color){}
    private record Sample(double x,double z){}
    private final StitchedHeightmapWorld source;
    private AdaptiveTerrainTopology topology;
    private List<Triangle> surface=List.of();

    private DevTerrainTopologyLab(StitchedHeightmapWorld source)throws IOException {
        this.source=source;
        refresh();
    }
    public static DevTerrainTopologyLab open()throws IOException {
        Path folder=Path.of(System.getProperty("user.home"),".beckenburg",
                            "dev-room","topology-qa");
        boolean fresh=!Files.exists(folder.resolve("world.properties"));
        StitchedHeightmapWorld source=fresh
            ? StitchedHeightmapWorld.create(folder,
                new StitchedHeightmapWorld.Settings(16,.5,0,0,-1,1,11741L,4))
            : StitchedHeightmapWorld.open(folder);
        if(fresh)source.generateRegion(0,0,2,1);
        if(!source.hasTile(0,0)||!source.hasTile(1,0))
            throw new IOException("Topology QA requires source tiles 0,0 and 1,0");
        if(!source.verifySeams().isEmpty())
            throw new IOException("QA source PNG seams do not match");
        if(fresh){
            AdaptiveTerrainTopology topology=AdaptiveTerrainTopology.open(source);
            topology.split(new AdaptiveTerrainTopology.Node(15,7,0,0,0));
            topology.split(new AdaptiveTerrainTopology.Node(15,7,1,1,0));
            // Real missing surface patches in the two-tile source fixture.
            topology.punch(new AdaptiveTerrainTopology.Node(15,7,1,0,1));
            topology.punch(new AdaptiveTerrainTopology.Node(14,7,0,0,0));
            topology.save();
        }
        return new DevTerrainTopologyLab(source);
    }
    public StitchedHeightmapWorld source(){return source;}
    public int splitCount(){return topology.splitCount();}
    public int cutoutCount(){return topology.cutoutCount();}
    public List<Triangle> triangles(){return surface;}
    public void refresh()throws IOException {
        AdaptiveTerrainTopology next=AdaptiveTerrainTopology.open(source);
        List<Triangle> rebuilt=build(next);
        // Publish together only after successful read/triangulation.
        topology=next;
        surface=List.copyOf(rebuilt);
    }
    private List<Triangle> build(AdaptiveTerrainTopology state)throws IOException {
        List<Triangle> out=new ArrayList<>();
        for(int gz=START_Z;gz<START_Z+CELLS_Z;gz++)
            for(int gx=START_X;gx<START_X+CELLS_X;gx++)
                for(var leaf:state.leaves(gx,gz)){
                    // Omit actual surface triangles. No invisible texture or zero-height plane.
                    if(state.isCutout(leaf))continue;
                    List<Sample> perimeter=new ArrayList<>();
                    for(long x:state.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.NORTH))
                        append(perimeter,new Sample(x,leaf.z0()));
                    for(long z:state.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.EAST))
                        append(perimeter,new Sample(leaf.x1(),z));
                    var south=state.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.SOUTH);
                    for(int j=south.size()-1;j>=0;j--)
                        append(perimeter,new Sample(south.get(j),leaf.z1()));
                    var west=state.edgeBreaks(leaf,AdaptiveTerrainTopology.Edge.WEST);
                    for(int j=west.size()-1;j>=0;j--)
                        append(perimeter,new Sample(leaf.x0(),west.get(j)));
                    if(perimeter.size()>1&&perimeter.get(0).equals(perimeter.get(perimeter.size()-1)))
                        perimeter.remove(perimeter.size()-1);
                    if(perimeter.size()<3)throw new IOException("Degenerate topology leaf "+leaf);
                    Sample center=new Sample((leaf.x0()+leaf.x1())*.5,(leaf.z0()+leaf.z1())*.5);
                    Vertex c=point(center);
                    for(int i=0;i<perimeter.size();i++){
                        if(out.size()>=MAX_TRIANGLES)
                            throw new IOException("DEV topology patch exceeds "+MAX_TRIANGLES+" triangles");
                        Vertex a=point(perimeter.get(i));
                        Vertex b=point(perimeter.get((i+1)%perimeter.size()));
                        int depth=leaf.depth();
                        int color=switch(Math.min(depth,5)){
                            case 0->0x66848b;case 1->0x83a4a0;case 2->0xa5b998;
                            case 3->0xc0ac81;case 4->0xc7977e;default->0xb889a9;
                        };
                        out.add(new Triangle(c,a,b,color));
                    }
                }
        return out;
    }
    private static void append(List<Sample> out,Sample item){
        if(out.isEmpty()||!out.get(out.size()-1).equals(item))out.add(item);
    }
    private Vertex point(Sample sample)throws IOException {
        double scale=source.settings().metresPerCell()/
            AdaptiveTerrainTopology.UNITS_PER_CELL;
        double wx=sample.x()*scale,wz=sample.z()*scale;
        // Exact canonical triangle sampler; no bilinear smoothing.
        double h=source.height(wx,wz);
        return new Vertex(PATCH_X+(wx-START_X*source.settings().metresPerCell()),
            .09+h,PATCH_Z+(wz-START_Z*source.settings().metresPerCell()));
    }
}
