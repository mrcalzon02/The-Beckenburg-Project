package org.beckenburg;

import dev.mrcalzon.water.*;
import org.beckenburg.editor.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Three detailed material/construction exhibits IN the existing DEV asset
 * showroom's free side aisle. No second renderer or duplicate development map.
 *
 * Rail parts come from the real portable rail mesh generator; road cross section
 * and pole cables are genuine authored triangles. Trees/rocks are sourced from
 * real existing GLBs by the showroom's normal model importer. The water bodies
 * are true portable WaterPlacement instances joined to river sockets through
 * the installed water-module WaterSnap math. The DEV studio shows their
 * footprint geometry, NOT a falsely labelled gameplay water shader.
 */
public final class DevInfrastructureShowcase {
    public record Prop(String modelId,double x,double y,double z,
                       double height,double yaw) {}
    private final List<DevTerrainTopologyLab.Triangle> geometry;
    private final List<Prop> props;
    private final WaterDocument water;
    private static final int LIMIT=6500;
    private final ArrayList<DevTerrainTopologyLab.Triangle> faces=new ArrayList<>();
    private int railCount,roadCount,poleCount,riverCount,waterCount;
    private static final int GRAVEL=0x827e70,IRON=0xbbb9b2,WOOD=0x755236,
        ASPHALT=0x42454a,SHOULDER=0x8c7658,GRASS=0x5b7850,
        BANK=0x927d60,SEDIMENT=0x655d4a,WATER=0x345d77;
    private static DevTerrainTopologyLab.Vertex v(double x,double y,double z){
        return new DevTerrainTopologyLab.Vertex(x,y,z);
    }
    private void tri(int color,DevTerrainTopologyLab.Vertex a,
                     DevTerrainTopologyLab.Vertex b,DevTerrainTopologyLab.Vertex c){
        if(faces.size()>=LIMIT)throw new IllegalArgumentException(
            "Infrastructure DEV exhibit exceeds triangle budget");
        faces.add(new DevTerrainTopologyLab.Triangle(a,b,c,color));
    }
    private void quad(int color,DevTerrainTopologyLab.Vertex a,
                      DevTerrainTopologyLab.Vertex b,
                      DevTerrainTopologyLab.Vertex c,
                      DevTerrainTopologyLab.Vertex d){
        tri(color,a,b,c);tri(color,a,c,d);
    }
    private void slab(int color,double x0,double y,double z0,double x1,
                      double z1){
        quad(color,v(x0,y,z0),v(x1,y,z0),v(x1,y,z1),v(x0,y,z1));
    }
    private void ramp(int color,double x0,double y0,double x1,double y1,
                      double z0,double z1){
        quad(color,v(x0,y0,z0),v(x1,y1,z0),
            v(x1,y1,z1),v(x0,y0,z1));
    }
    private void tube(int color,double ax,double ay,double az,double bx,double by,
                      double bz,double radius,int sides){
        double dx=bx-ax,dy=by-ay,dz=bz-az,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(len<1e-7)throw new IllegalArgumentException("Zero length DEV pole/wire");
        dx/=len;dy/=len;dz/=len;
        double ux=dz,uy=0,uz=-dx,un=Math.hypot(ux,uz);
        if(un<1e-6){ux=1;uz=0;un=1;}
        ux/=un;uz/=un;
        double vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;
        for(int i=0;i<sides;i++){
            double a=i*Math.PI*2/sides,b=(i+1)*Math.PI*2/sides;
            double qx=Math.cos(a)*ux+Math.sin(a)*vx,
                qy=Math.cos(a)*uy+Math.sin(a)*vy,
                qz=Math.cos(a)*uz+Math.sin(a)*vz;
            double rx=Math.cos(b)*ux+Math.sin(b)*vx,
                ry=Math.cos(b)*uy+Math.sin(b)*vy,
                rz=Math.cos(b)*uz+Math.sin(b)*vz;
            quad(color,v(ax+radius*qx,ay+radius*qy,az+radius*qz),
                v(ax+radius*rx,ay+radius*ry,az+radius*rz),
                v(bx+radius*rx,by+radius*ry,bz+radius*rz),
                v(bx+radius*qx,by+radius*qy,bz+radius*qz));
        }
    }
    private void rail() {
        var settings=new StitchedHeightmapWorld.Settings(32,.5,0,0,-50,50,7121L,4);
        var alignment=new TerrainInfrastructureSplines.Alignment("dev-track-sample",
            TerrainInfrastructureSplines.Kind.RAIL,List.of(
                new TerrainLayerStack.Point(16.5,7.2,.28),
                new TerrainLayerStack.Point(16.5,14.6,.28)),
            1.4,1,5,true);
        var track=TerrainRailMesh.build(settings,alignment,TerrainRailMesh.Profile.standard());
        for(var face:track.mesh().triangles()){
            int color=switch(face.part()){
                case "rail_ballast" -> GRAVEL;
                case "rail_left","rail_right" -> IRON;
                case "rail_sleepers" -> WOOD;
                default -> throw new IllegalStateException("Unrecognized railway part");
            };
            tri(color,v(face.a().x(),face.a().y(),face.a().z()),
                v(face.b().x(),face.b().y(),face.b().z()),
                v(face.c().x(),face.c().y(),face.c().z()));
        }
        railCount=track.mesh().triangles().size();
    }
    private void road(){
        int before=faces.size();
        // Genuine highway cut/fill section: compacted subgrade and feathered
        // lateral slope, crowned asphalt, narrow shoulder + road markings.
        for(int s=0;s<2;s++){
            double z0=8.3+s*1.85,z1=z0+1.85;
            ramp(GRASS,6.25,.025,6.9,.24,z0,z1);
            ramp(SHOULDER,6.9,.24,7.3,.305,z0,z1);
            ramp(ASPHALT,7.3,.305,8.25,.34,z0,z1);
            ramp(ASPHALT,8.25,.34,9.2,.305,z0,z1);
            ramp(SHOULDER,9.2,.305,9.6,.24,z0,z1);
            ramp(GRASS,9.6,.24,10.28,.025,z0,z1);
            // broken road centerline: separately colored physical 3D faces
            slab(0xd3c6a1,8.22,.343,z0+.22,8.28,z0+.95);
        }
        roadCount=faces.size()-before;
        int p0=faces.size();
        double[] locations={8.45,10.25,12.05};
        for(double z:locations){
            double x=9.85;
            tube(0x644b38,x,.23,z,x,2.05,z,.055,8);
            tube(WOOD,x-.30,1.90,z,x+.30,1.90,z,.033,6);
            for(double side:new double[]{-.20,.20})
                tube(0x45423e,x+side,1.9,z,x+side,1.97,z,.023,6);
        }
        for(int k=1;k<locations.length;k++){
            double z0=locations[k-1],z1=locations[k];
            for(double side:new double[]{-.20,.20})
                for(int n=1;n<=6;n++){
                    double t0=(n-1)/6.,t1=n/6.;
                    double h0=1.95-.09*4*t0*(1-t0),
                           h1=1.95-.09*4*t1*(1-t1);
                    tube(0x373739,9.85+side,h0,z0+(z1-z0)*t0,
                        9.85+side,h1,z0+(z1-z0)*t1,.008,5);
                }
        }
        poleCount=faces.size()-p0;
    }
    private static StitchedHeightmapWorld.Settings demoWorld(){
        return new StitchedHeightmapWorld.Settings(32,.5,0,0,-50,50,7121L,4);
    }
    public static TerrainInfrastructureSplines.Alignment riverRoute(){
        return new TerrainInfrastructureSplines.Alignment("dev-river",
            TerrainInfrastructureSplines.Kind.RIVER,
            List.of(new TerrainLayerStack.Point(16.2,26.2,.075),
                new TerrainLayerStack.Point(16.2,30.1,.075),
                new TerrainLayerStack.Point(16.2,34.6,.075)),
            1.16,1,100,true);
    }
    public static List<WaterTerrainSnap.Socket> riverSockets(){
        return TerrainRiverWaterSockets.fromRoute(riverRoute(),demoWorld());
    }
    public static WaterTerrainSnap.Socket riverSocket(){
        return riverSockets().get(1);
    }
    private WaterDocument river(WaterDocument sourceDocument){
        int from=faces.size();
        double z0=13.1,z1=17.3;
        // Contiguous riverbed with real sloped banks and exposed sediment.
        ramp(GRASS,6.25,.05,6.72,.19,z0,z1);
        ramp(BANK,6.72,.19,7.52,-.20,z0,z1);
        ramp(SEDIMENT,7.52,-.20,8.68,-.20,z0,z1);
        ramp(BANK,8.68,-.20,9.5,.19,z0,z1);
        ramp(GRASS,9.5,.19,10.25,.05,z0,z1);
        riverCount=faces.size()-from;
        WaterSettings setting=new WaterSettings()
            .with("bed.depth",.45)
            .with("wave.height",.04)
            .with("foam.depth",.16)
            .with("foam.opacity",.55);
        var a=new WaterPlacement("dev-river-upstream",8.1,.075,14.05,
            1.16,2,0,setting,WaterPlacement.Shape.SQUARE);
        var initial=new WaterPlacement("dev-river-downstream",8.1,.075,16.18,
            1.16,2,0,setting,WaterPlacement.Shape.SQUARE);
        var socket=riverSocket();
        var connected=WaterTerrainSnap.connect(initial,socket,.4,false);
        if(connected==null)throw new IllegalStateException(
            "Water module failed to snap downstream river to terrain end socket");
        var b=connected.water();
        if(Math.abs(b.z()-16.05)>.001)
            throw new IllegalStateException("Water river snap was not flush");
        var document=sourceDocument==null?
            new WaterDocument("dev-infrastructure-demo",List.of(a,b)):sourceDocument;
        // The water module owns true footprints, authoring and simulation data.
        // The showroom depicts those exact footprints as flat diagnostic surfaces;
        // this is not the game's animated/reflective water shader.
        int waterBefore=faces.size();
        for(var placement:document.placements()){
            var shape=placement.polygon().points();
            for(int i=1;i+1<shape.size();i++)
                tri(WATER,v(shape.get(0).x(),placement.y(),shape.get(0).z()),
                    v(shape.get(i).x(),placement.y(),shape.get(i).z()),
                    v(shape.get(i+1).x(),placement.y(),shape.get(i+1).z()));
        }
        waterCount=faces.size()-waterBefore;
        return document;
    }
    private DevInfrastructureShowcase(WaterDocument chosenWater){
        if(chosenWater!=null&&!"dev-infrastructure-demo".equals(chosenWater.level()))
            throw new IllegalArgumentException("Wrong DEV water document");
        rail();road();
        water=river(chosenWater);
        geometry=List.copyOf(faces);
        props=List.of(
            new Prop("level_tree_conifer",6.5,0.14,12.25,1.85,0),
            new Prop("level_tree_broadleaf",10.18,.10,13.3,1.7,.5),
            new Prop("level_camp_pine_refined_a",6.38,.10,17.7,1.7,1.2),
            new Prop("level_camp_shrub_decorative_a_refined",9.95,.11,17.8,.53,.2),
            new Prop("level_woodland_boulder",7.0,-.06,13.85,.38,.5),
            new Prop("level_camp_siteart_rocks",9.32,-.08,15.7,.38,.3),
            new Prop("level_woodland_boulder",7.12,-.05,16.7,.32,1.6)
        );
    }
    public static DevInfrastructureShowcase create(){
        return new DevInfrastructureShowcase(null);
    }
    public static DevInfrastructureShowcase create(WaterDocument water){
        return new DevInfrastructureShowcase(Objects.requireNonNull(water));
    }
    public List<DevTerrainTopologyLab.Triangle> triangles(){return geometry;}
    public List<Prop> props(){return props;}
    public WaterDocument water(){return water;}
    public int railFaces(){return railCount;}
    public int roadFaces(){return roadCount;}
    public int poleFaces(){return poleCount;}
    public int riverFaces(){return riverCount;}
    public int waterFaces(){return waterCount;}
    /** Explicit publish only: normal showroom access never rewrites game maps. */
    public Path exportWater(Path destination)throws IOException{
        Objects.requireNonNull(destination);
        water.save(destination);
        try(var input=Files.newInputStream(destination)){
            if(!water.equals(WaterDocument.read(input)))
                throw new IOException("Demo water module readback mismatch");
        }
        return destination;
    }
}
