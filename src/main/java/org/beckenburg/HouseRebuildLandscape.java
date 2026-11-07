package org.beckenburg;

import java.util.ArrayList;
import org.beckenburg.LevelDefinition.*;

/** House parcel: editor terrain owns exterior support; measured source fixtures remain intact. */
public final class HouseRebuildLandscape {
    private HouseRebuildLandscape(){}
    static final String TERRAIN_SHA="30e12a7d833441b738fbe239e1da58218b1a6fddbafdd9ee21e722ec58974d58";
    // Parcel returns follow the garden beds and narrow side passage, then the real street apron.
    static final double[][] EDGE={{0,26},{6,26},{6,20.5},{6,11},{8,8},{8,4.2},{12,2.7},
        {18.6,2.7},{24,5},{26.6,10},{25,16},{27,20.5},{27,39.4},{0,39.4}};
    static final double[][] PATH={{19.8,20.5,19.8,24},{19.8,24,17,28},{17,28,17,32},
        {16.5,10,16.5,7},{16.5,7,11,6},{11,6,8.5,12},{8.5,12,8.5,18},
        {16.5,7,23.5,7},{23.5,7,24,20.5},{24,20.5,19.8,24}};
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var base=HouseRebuildFittings.source();var terrain=load();
        var sockets=new ArrayList<>(base.sockets());sockets.removeIf(s->s.type().equals("world-terrain"));
        sockets.add(new Socket("landscape-world","world-terrain",12,0,0,0,0,"briar-landscape-world.hcs2#"+TERRAIN_SHA));
        sockets.add(new Socket("landscape-walking","world-terrain-walk",12,0,0,0,0,"source-triangles-v1"));
        var solids=new ArrayList<>(base.colliders());
        // These exact residences already render in the canonical arrival module.
        // Consume its real private-parcel fences and building solids as well.
        var arrival=new LevelPlans.B(base.id(),base.title(),base.ambience());arrival.rooms.addAll(base.rooms());
        LevelEnclosure.populate(arrival);
        for(var s:arrival.sockets)if(s.type().equals("parcel_fence"))sockets.add(s);
        for(var c:arrival.colliders)if(arrival.sockets.stream().noneMatch(s->s.type().equals("enclosure")&&Math.hypot(s.x()-c.x(),s.z()-c.z())<.001))solids.add(c);
        for(int i=0;i<EDGE.length;i++){
            var a=EDGE[i];var b=EDGE[(i+1)%EDGE.length];double dx=b[0]-a[0],dz=b[1]-a[1],length=Math.hypot(dx,dz),yaw=Math.atan2(dz,dx);
            int count=(int)Math.ceil(length/2);double width=length/count;
            for(int j=0;j<count;j++){
                double t=(j+.5)/count,x=a[0]+dx*t,z=a[1]+dz*t;
                double y=Math.min(height(terrain,x-dx/count/2,z-dz/count/2),height(terrain,x+dx/count/2,z+dz/count/2))-.025;
                sockets.add(new Socket("parcel-"+i+"-"+j,"enclosure",13,x,y,z,-yaw,"garden:"+width));
                solids.add(new Collider(x,y,z,width/2+.012,.15,1.96,yaw));
            }
        }
        var rooms=new ArrayList<>(base.rooms());
        for(int i=12;i<14;i++){
            var r=rooms.get(i);double x=i==12?9.4:19.8,z=i==12?5.8:24;
            rooms.set(i,new Room(r.id(),r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.y(),r.height(),r.finish(),r.acoustic(),new Vec(x,height(terrain,x,z),z)));
        }
        var portals=new ArrayList<>(base.portals());
        for(int i=0;i<portals.size();i++){var p=portals.get(i);if(p.kind().equals("open")&&p.joins(12,13))
            portals.set(i,new Portal(p.id(),p.a(),p.b(),24,p.y(),p.z(),2.5,p.alongX(),p.kind(),p.loss()));}
        return new LevelDefinition(base.id(),"first-principles-landscape-v1",base.title().replace("fixture fit","landscape construction"),
            rooms,portals,base.stairs(),base.voids(),base.props(),sockets,solids,base.dock(),base.ambience(),base.water(),base.sounds());
    }
    private static AuthoredSitePhysics load(){
        try(var in=HouseRebuildLandscape.class.getResourceAsStream("/org/beckenburg/sites/briar-landscape-world.hcs2")){
            if(in==null)throw new IllegalStateException("Missing authored landscape");
            return AuthoredSitePhysics.read(in,TERRAIN_SHA,java.util.List.of());
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    static double height(AuthoredSitePhysics terrain,double x,double z){
        var support=terrain.support(x,z,0,1);if(support==null)throw new IllegalArgumentException("Missing parcel support "+x+","+z);return support.y();
    }
    static double pathDistance(double x,double z){
        double best=Double.POSITIVE_INFINITY;
        for(var p:PATH){double dx=p[2]-p[0],dz=p[3]-p[1],t=Math.max(0,Math.min(1,((x-p[0])*dx+(z-p[1])*dz)/(dx*dx+dz*dz)));
            best=Math.min(best,Math.hypot(x-p[0]-t*dx,z-p[1]-t*dz));}
        return best;
    }
    static boolean inside(double x,double z){
        boolean result=false;
        for(int i=0,j=EDGE.length-1;i<EDGE.length;j=i++){
            var a=EDGE[i];var b=EDGE[j];if((a[1]>z)!=(b[1]>z)&&x<(b[0]-a[0])*(z-a[1])/(b[1]-a[1])+a[0])result=!result;
        }return result;
    }
    private static int groundMaterial=-1;
    /** Blend existing library surfaces in world coordinates; paths never inherit terrain cell shapes. */
    static synchronized int material(){
        if(groundMaterial>=0)return groundMaterial;
        int grass=GlbProps.model("level_woodland_floor").triangles().get(0).material();
        int soil=GlbProps.model("level_woodland_track").triangles().get(0).material();
        var image=new java.awt.image.BufferedImage(2048,2048,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int pz=0;pz<2048;pz++)for(int px=0;px<2048;px++){
            double x=-39+(px+.5)/2048*125,z=-48+(2047-pz+.5)/2048*137;
            double edge=.065*Math.sin(x*5.7+z*3.1)+.035*Math.sin(x*2.3-z*7.2);
            double wear=Math.max(0,Math.min(1,(.79+edge-pathDistance(x,z))/.24));
            if(z>29)wear=0;
            int a=GlbProps.sample(grass,x*.35,z*.35),b=GlbProps.sample(soil,x*.7,z*.7),rgb=0;
            for(int shift:new int[]{16,8,0})rgb|=(int)(((a>>shift)&255)*(1-wear)+((b>>shift)&255)*wear)<<shift;
            image.setRGB(px,pz,rgb);
        }
        groundMaterial=GlbProps.surfaceMaterial("briar-landscape-paths-v1-"+TERRAIN_SHA,image);return groundMaterial;
    }
    static double u(double x){return (x+39)/125;}
    static double v(double z){return (z+48)/137;}
}
