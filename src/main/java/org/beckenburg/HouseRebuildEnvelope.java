package org.beckenburg;

import java.util.*;
import java.io.*;
import java.security.*;
import org.beckenburg.LevelDefinition.*;

/** One authored dwelling envelope; rooms, openings and structural skin share coordinates. */
public final class HouseRebuildEnvelope {
    private HouseRebuildEnvelope(){}
    static final String STYLE="gable-v1|1.88|0.4|0.16|3.0|1.9|3.0|2.55|0.04";
    public record Vertex(Vec p,double u,double v){}
    public record Triangle(Vertex a,Vertex b,Vertex c,int material,boolean visible){}
    public record Part(String name,List<Triangle> triangles,boolean support){
        public Part{triangles=List.copyOf(triangles);}
    }
    public record Geometry(List<Part> parts,AuthoredSitePhysics physics,String digest,
            double west,double north,double east,double south,double plate,double ridge){
        public Geometry{parts=List.copyOf(parts);}
    }
    private record Key(List<Room> rooms,List<Portal> portals,String style){Key{rooms=List.copyOf(rooms);portals=List.copyOf(portals);}}
    private static final Map<Key,Geometry> CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var base=HouseRebuildLandscape.source();var sockets=new ArrayList<>(base.sockets());
        var colliders=new ArrayList<>(base.colliders());
        var oldArrival=arrival(base,List.of());colliders.removeAll(arrivalSolids(oldArrival));sockets.removeIf(s->s.type().equals("parcel_fence"));
        var context=new Socket("house-approach-reservation","arrival-context",13,0,0,0,0,"house-side-setback-v1:12.0");sockets.add(context);
        var newArrival=arrival(base,List.of(context));colliders.addAll(arrivalSolids(newArrival));
        for(var socket:newArrival.sockets)if(socket.type().equals("parcel_fence"))sockets.add(socket);
        var portals=new ArrayList<>(base.portals());
        for(int i=0;i<portals.size();i++){var p=portals.get(i);if(p.kind().equals("window")&&p.a()==2&&p.alongX()&&p.z()==10)
            portals.set(i,new Portal(p.id(),p.a(),p.b(),14.9,p.y(),p.z(),p.width(),p.alongX(),p.kind(),p.loss()));}
        sockets.add(new Socket("dwelling-envelope","structure-envelope",13,16.8,5.6,15.25,0,STYLE));
        return new LevelDefinition(base.id(),"first-principles-envelope-v1",base.title().replace("landscape construction","dwelling construction"),
            base.rooms(),portals,base.stairs(),base.voids(),base.props(),sockets,colliders,base.dock(),base.ambience(),base.water(),base.sounds());
    }
    private static LevelPlans.B arrival(LevelDefinition base,List<Socket> context){
        var b=new LevelPlans.B(base.id(),base.title(),base.ambience());b.rooms.addAll(base.rooms());b.sockets.addAll(context);LevelEnclosure.populate(b);return b;
    }
    private static List<Collider> arrivalSolids(LevelPlans.B b){return b.colliders.stream().filter(c->b.sockets.stream().noneMatch(s->s.type().equals("enclosure")&&Math.hypot(s.x()-c.x(),s.z()-c.z())<.001)).toList();}
    static Geometry geometry(LevelDefinition level){
        return geometry(level.rooms(),level.portals(),level.sockets());
    }
    static String fingerprint(List<Room> rooms,List<Portal> portals,List<Socket> sockets){var g=geometry(rooms,portals,sockets);return g==null?"":g.digest();}
    private static Geometry geometry(List<Room> rooms,List<Portal> portals,List<Socket> sockets){
        var bindings=sockets.stream().filter(s->s.type().equals("structure-envelope")).toList();
        if(bindings.isEmpty())return null;
        if(bindings.size()!=1)throw new IllegalArgumentException("One structure envelope required");
        var key=new Key(rooms,portals,bindings.get(0).style());return CACHE.computeIfAbsent(key,k->build(k.rooms(),k.portals(),k.style()));
    }
    static Geometry build(List<Room> sourceRooms,List<Portal> portals,String style){
        var values=style.split("\\|");if(values.length!=9||!values[0].equals("gable-v1"))throw new IllegalArgumentException("Unknown dwelling envelope");
        double[] p=new double[8];for(int i=0;i<8;i++){p[i]=Double.parseDouble(values[i+1]);if(!Double.isFinite(p[i])||p[i]<=0)throw new IllegalArgumentException("Invalid envelope dimensions");}
        var rooms=sourceRooms.stream().filter(r->!r.outside()&&r.y()>=0).toList();
        double x0=rooms.stream().mapToDouble(Room::x0).min().orElseThrow(),x1=rooms.stream().mapToDouble(Room::x1).max().orElseThrow();
        double z0=rooms.stream().mapToDouble(Room::z0).min().orElseThrow(),z1=rooms.stream().mapToDouble(Room::z1).max().orElseThrow();
        double plate=rooms.stream().mapToDouble(r->r.y()+r.height()).max().orElseThrow(),ridge=plate+p[0],mid=(x0+x1)/2;
        var b=new Builder();
        wall(b,portals,true,z0,x0-.08,x1+.08,-1,plate,"north");
        wall(b,portals,true,z1,x0-.08,x1+.08,1,plate,"south");
        wall(b,portals,false,x0,z0+.08,z1-.08,-1,plate,"west");
        wall(b,portals,false,x1,z0+.08,z1-.08,1,plate,"east");
        double left=x0-p[1],right=x1+p[1],near=z0-p[1],far=z1+p[1],thick=p[2];
        b.roof("main-roof",left,mid,right,near,far,plate,ridge,thick);
        double edge=plate+(x0-.08-left)/(mid-left)*(ridge-plate)-thick+.02;
        double[][] gable={{x0-.08,plate-.1},{x1+.08,plate-.1},{x1+.08,edge},{mid,ridge-thick+.02},{x0-.08,edge}};
        b.prism("north-gable",gable,z0-.08,z0+.08,LevelMaterials.CLADDING);
        b.prism("south-gable",gable,z1-.08,z1+.08,LevelMaterials.CLADDING);
        // Corner boards and storey belt have real overlap with the structural wall.
        for(double x:new double[]{x0,x1})for(double z:new double[]{z0,z1})b.box("corner-board-"+x+"-"+z,x-.115,.30,z-.115,x+.115,plate,z+.115,LevelMaterials.WOOD,-1,false);
        b.box("front-storey-belt",x0-.11,2.73,z1+.04,x1+.11,2.87,z1+.12,LevelMaterials.WOOD,-1,false);
        b.box("rear-storey-belt",x0-.11,2.73,z0-.12,x1+.11,2.87,z0-.04,LevelMaterials.WOOD,-1,false);
        b.box("west-storey-belt",x0-.12,2.73,z0-.04,x0-.04,2.87,z1+.04,LevelMaterials.WOOD,-1,false);
        b.box("east-storey-belt",x1+.04,2.73,z0-.04,x1+.12,2.87,z1+.04,LevelMaterials.WOOD,-1,false);
        var entry=portals.stream().filter(q->q.door()&&q.alongX()&&Math.abs(q.z()-z1)<.01&&q.y()==0&&(q.a()==13||q.b()==13)).findFirst().orElseThrow();
        double px0=entry.x()-p[3]/2,px1=entry.x()+p[3]/2,pz=z1+p[4];
        b.box("porch-deck",px0,-.12,z1,px1,p[7],pz,LevelMaterials.STONE,-1,true);
        for(double x:new double[]{px0+.13,px1-.13}){
            b.box("porch-post-"+x,x-.075,p[7],pz-.25,x+.075,p[6]-.06,pz-.10,LevelMaterials.WOOD,-1,false);
            b.box("porch-post-foot-"+x,x-.12,p[7],pz-.295,x+.12,p[7]+.16,pz-.055,LevelMaterials.STONE,-1,false);
        }
        b.box("porch-front-beam",px0,p[6]-.20,pz-.26,px1,p[6]-.04,pz-.09,LevelMaterials.WOOD,-1,false);
        b.box("porch-wall-ledger",px0,p[5]-.20,z1-.04,px1,p[5]-.07,z1+.12,LevelMaterials.WOOD,-1,false);
        b.slope("porch-roof",px0-.12,px1+.12,z1-.06,pz+.12,p[5],p[6],.12);
        return b.finish(x0,z0,x1,z1,plate,ridge);
    }
    private static void wall(Builder b,List<Portal> portals,boolean ax,double plane,double from,double to,int outward,double plate,String name){
        var openings=portals.stream().filter(p->(p.door()||p.kind().equals("window"))&&p.alongX()==ax&&Math.abs((ax?p.z():p.x())-plane)<.01).toList();
        var xs=new TreeSet<Double>();var ys=new TreeSet<Double>();xs.add(from);xs.add(to);ys.add(-.20);ys.add(0.0);ys.add(.45);ys.add(plate);
        for(var p:openings){double c=ax?p.x():p.z();xs.add(Math.max(from,Math.min(to,c-p.width()/2)));xs.add(Math.max(from,Math.min(to,c+p.width()/2)));
            for(double y:new double[]{p.y()+(p.kind().equals("window")?1:0),p.y()+(p.kind().equals("window")?2.35:2.32)})if(y>-.2&&y<plate)ys.add(y);}
        var xx=new ArrayList<>(xs);var yy=new ArrayList<>(ys);
        for(int i=0;i<xx.size()-1;i++)for(int j=0;j<yy.size()-1;j++){
            double a=xx.get(i),c=xx.get(i+1),lo=yy.get(j),hi=yy.get(j+1),mx=(a+c)/2,my=(lo+hi)/2;
            if(c-a<.001||hi-lo<.001)continue;
            if(openings.stream().anyMatch(p->Math.abs(mx-(ax?p.x():p.z()))<p.width()/2&&my>p.y()+(p.kind().equals("window")?1:0)&&my<p.y()+(p.kind().equals("window")?2.35:2.32)))continue;
            int material=my<.45?LevelMaterials.STONE:LevelMaterials.CLADDING;
            b.box("facade-"+name+"-"+i+"-"+j,ax?a:plane-.08,lo,ax?plane-.08:a,ax?c:plane+.08,hi,ax?plane+.08:c,material,ax?(outward<0?1:0):(outward<0?3:2),false);
        }
    }
    static final class Builder {
        final ArrayList<Part> parts=new ArrayList<>();ArrayList<Triangle> triangles;
        static Vec v(double x,double y,double z){return new Vec(x,y,z);}
        static Vec sub(Vec a,Vec b){return v(a.x()-b.x(),a.y()-b.y(),a.z()-b.z());}
        static double dot(Vec a,Vec b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
        static Vec cross(Vec a,Vec b){return v(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
        static Vec unit(Vec a){double n=Math.sqrt(dot(a,a));return v(a.x()/n,a.y()/n,a.z()/n);}
        void tri(Vec a,Vec b,Vec c,int material,boolean visible){
            Vec u=unit(sub(b,a)),normal=unit(cross(sub(b,a),sub(c,a))),w=cross(normal,u);
            triangles.add(new Triangle(new Vertex(a,dot(a,u)*.5,dot(a,w)*.5),new Vertex(b,dot(b,u)*.5,dot(b,w)*.5),new Vertex(c,dot(c,u)*.5,dot(c,w)*.5),material,visible));
        }
        void quad(Vec a,Vec b,Vec c,Vec d,int mat,boolean visible){
            Vec u=unit(sub(b,a)),normal=unit(cross(sub(b,a),sub(c,a))),w=cross(normal,u);
            Vertex aa=new Vertex(a,dot(a,u)*.5,dot(a,w)*.5),bb=new Vertex(b,dot(b,u)*.5,dot(b,w)*.5),cc=new Vertex(c,dot(c,u)*.5,dot(c,w)*.5),dd=new Vertex(d,dot(d,u)*.5,dot(d,w)*.5);
            triangles.add(new Triangle(aa,bb,cc,mat,visible));triangles.add(new Triangle(aa,cc,dd,mat,visible));
        }
        void box(String name,double x0,double y0,double z0,double x1,double y1,double z1,int mat,int hidden,boolean support){
            triangles=new ArrayList<>();
            quad(v(x1,y0,z0),v(x0,y0,z0),v(x0,y1,z0),v(x1,y1,z0),mat,hidden!=0);
            quad(v(x0,y0,z1),v(x1,y0,z1),v(x1,y1,z1),v(x0,y1,z1),mat,hidden!=1);
            quad(v(x0,y0,z0),v(x0,y0,z1),v(x0,y1,z1),v(x0,y1,z0),mat,hidden!=2);
            quad(v(x1,y0,z1),v(x1,y0,z0),v(x1,y1,z0),v(x1,y1,z1),mat,hidden!=3);
            quad(v(x0,y1,z1),v(x1,y1,z1),v(x1,y1,z0),v(x0,y1,z0),mat,hidden!=4);
            quad(v(x0,y0,z0),v(x1,y0,z0),v(x1,y0,z1),v(x0,y0,z1),mat,hidden!=5);
            parts.add(new Part(name,triangles,support));
        }
        void prism(String name,double[][] ring,double z0,double z1,int mat){
            triangles=new ArrayList<>();
            for(int i=1;i<ring.length-1;i++){
                tri(v(ring[0][0],ring[0][1],z0),v(ring[i+1][0],ring[i+1][1],z0),v(ring[i][0],ring[i][1],z0),mat,true);
                tri(v(ring[0][0],ring[0][1],z1),v(ring[i][0],ring[i][1],z1),v(ring[i+1][0],ring[i+1][1],z1),mat,true);
            }
            for(int i=0;i<ring.length;i++){var a=ring[i];var c=ring[(i+1)%ring.length];quad(v(a[0],a[1],z0),v(c[0],c[1],z0),v(c[0],c[1],z1),v(a[0],a[1],z1),mat,true);}
            parts.add(new Part(name,triangles,false));
        }
        void roof(String name,double left,double mid,double right,double z0,double z1,double eave,double ridge,double t){
            // The concave cross-section is two joined strips; their internal ridge face is omitted.
            triangles=new ArrayList<>();
            roofStrip(left,mid,z0,z1,eave,ridge,t,true);roofStrip(mid,right,z0,z1,ridge,eave,t,false);
            parts.add(new Part(name,triangles,false));
        }
        void roofStrip(double x0,double x1,double z0,double z1,double y0,double y1,double t,boolean left){
            quad(v(x0,y0,z1),v(x1,y1,z1),v(x1,y1,z0),v(x0,y0,z0),LevelMaterials.ROOF,true);
            quad(v(x0,y0-t,z0),v(x1,y1-t,z0),v(x1,y1-t,z1),v(x0,y0-t,z1),LevelMaterials.WOOD,true);
            quad(v(x1,y1-t,z0),v(x0,y0-t,z0),v(x0,y0,z0),v(x1,y1,z0),LevelMaterials.WOOD,true);
            quad(v(x0,y0-t,z1),v(x1,y1-t,z1),v(x1,y1,z1),v(x0,y0,z1),LevelMaterials.WOOD,true);
            if(left)quad(v(x0,y0-t,z0),v(x0,y0-t,z1),v(x0,y0,z1),v(x0,y0,z0),LevelMaterials.WOOD,true);
            else quad(v(x1,y1-t,z1),v(x1,y1-t,z0),v(x1,y1,z0),v(x1,y1,z1),LevelMaterials.WOOD,true);
        }
        void slope(String name,double x0,double x1,double z0,double z1,double y0,double y1,double t){
            triangles=new ArrayList<>();
            quad(v(x0,y1,z1),v(x1,y1,z1),v(x1,y0,z0),v(x0,y0,z0),LevelMaterials.ROOF,true);
            quad(v(x0,y0-t,z0),v(x1,y0-t,z0),v(x1,y1-t,z1),v(x0,y1-t,z1),LevelMaterials.WOOD,true);
            quad(v(x1,y0-t,z0),v(x0,y0-t,z0),v(x0,y0,z0),v(x1,y0,z0),LevelMaterials.WOOD,true);
            quad(v(x0,y1-t,z1),v(x1,y1-t,z1),v(x1,y1,z1),v(x0,y1,z1),LevelMaterials.WOOD,true);
            quad(v(x0,y0-t,z0),v(x0,y1-t,z1),v(x0,y1,z1),v(x0,y0,z0),LevelMaterials.WOOD,true);
            quad(v(x1,y1-t,z1),v(x1,y0-t,z0),v(x1,y0,z0),v(x1,y1,z1),LevelMaterials.WOOD,true);
            parts.add(new Part(name,triangles,false));
        }
        Geometry finish(double x0,double z0,double x1,double z1,double plate,double ridge){
            try{
                var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
                out.writeInt(0x48435332);out.writeInt(parts.size());out.writeInt(parts.stream().mapToInt(p->p.triangles().size()).sum());
                for(var part:parts){byte[] name=part.name().getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeShort(name.length);out.write(name);out.writeByte(1);out.writeInt(part.triangles().size());
                    for(var f:part.triangles()){
                        for(var q:List.of(f.a(),f.b(),f.c())){out.writeFloat((float)q.p().x());out.writeFloat((float)q.p().y());out.writeFloat((float)q.p().z());}
                        double ny=cross(sub(f.b().p(),f.a().p()),sub(f.c().p(),f.a().p())).y();out.writeByte(6+(part.support()&&ny>0?1:0));
                    }
                }
                out.flush();byte[] data=bytes.toByteArray();String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
                return new Geometry(parts,AuthoredSitePhysics.read(new ByteArrayInputStream(data),sha,List.of()),sha,x0,z0,x1,z1,plate,ridge);
            }catch(IOException|NoSuchAlgorithmException e){throw new IllegalStateException("Invalid authored dwelling",e);}
        }
    }
}
