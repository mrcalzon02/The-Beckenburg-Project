package org.beckenburg;

import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;

/** Closed source geometry, source openings, actual physical mass and complete circulation. */
public final class HouseRebuildEnvelopeTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static String point(Vec v){return Math.round(v.x()*1e6)+","+Math.round(v.y()*1e6)+","+Math.round(v.z()*1e6);}
    static boolean close(Vec a,Vec b){return Math.abs(a.x()-b.x())<2e-6&&Math.abs(a.y()-b.y())<2e-6&&Math.abs(a.z()-b.z())<2e-6;}
    static double determinant(Vec a,Vec b,Vec c){return a.x()*(b.y()*c.z()-b.z()*c.y())-a.y()*(b.x()*c.z()-b.z()*c.x())+a.z()*(b.x()*c.y()-b.y()*c.x());}
    static LevelDefinition with(LevelDefinition base,List<Room> rooms,List<Portal> portals){return new LevelDefinition(base.id(),"envelope-edit",base.title(),rooms,portals,base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),base.dock(),base.ambience(),base.water(),base.sounds());}
    static boolean contains(HouseRebuildEnvelope.Part part,Vec p)throws Exception{
        var builder=new HouseRebuildEnvelope.Builder();builder.parts.add(part);var physical=builder.finish(0,0,1,1,1,2).physics();
        var method=AuthoredSitePhysics.class.getDeclaredMethod("insideSolid",Vec.class);method.setAccessible(true);return (Boolean)method.invoke(physical,p);
    }
    static void contact(HouseRebuildEnvelope.Geometry g,String a,String b,Vec point)throws Exception{
        boolean first=false,second=false;
        for(var part:g.parts()){
            if(part.name().startsWith(a)&&contains(part,point))first=true;
            if(part.name().startsWith(b)&&contains(part,point))second=true;
        }
        check(first&&second,"Unsupported assembly junction "+a+" / "+b+" at "+point);
    }
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildEnvelope.map();var level=map.level();var base=HouseRebuildLandscape.source();var g=map.envelope();
        check(base.revision().equals("first-principles-landscape-v1-09a90b8dc9035bf9"),"Frozen landscape network revision changed");
        check(g.digest().equals(HouseRebuildEnvelope.fingerprint(level.rooms(),level.portals(),level.sockets())),"Network geometry fingerprint differs from actual physics");
        double porchFloor=map.groundHeight(19.8,21.7,13),soil=map.worldSurface().support(19.8,21.7,0,1).y();
        check(porchFloor>soil+.01,"Porch slab does not clear rendered terrain: deck="+porchFloor+" soil="+soil);
        check(level.rooms().equals(base.rooms())&&level.stairs().equals(base.stairs())&&level.voids().equals(base.voids()),"Architecture coordinates changed");
        int adjusted=0;
        for(int i=0;i<base.portals().size();i++){var p=base.portals().get(i);var q=level.portals().get(i);
            if(!p.equals(q)){check(p.kind().equals("window")&&p.a()==2&&p.x()==15.7&&q.x()==14.9&&p.id()==q.id()&&p.y()==q.y()&&p.z()==q.z()&&p.width()==q.width(),"Unrelated portal changed");adjusted++;}}
        check(adjusted==1,"Missing kitchen opening repair");
        for(var p:level.portals())for(var q:level.portals())if(p.id()<q.id()&&(p.door()||p.kind().equals("window"))&&(q.door()||q.kind().equals("window"))&&p.alongX()==q.alongX()&&Math.abs((p.alongX()?p.z():p.x())-(q.alongX()?q.z():q.x()))<.01){
            double lo=Math.max(p.y()+(p.kind().equals("window")?1:0),q.y()+(q.kind().equals("window")?1:0)),hi=Math.min(p.y()+(p.kind().equals("window")?2.35:2.32),q.y()+(q.kind().equals("window")?2.35:2.32));
            if(hi>lo)check(Math.abs((p.alongX()?p.x():p.z())-(q.alongX()?q.x():q.z()))>=(p.width()+q.width())/2+.12,"Overlapping opening/frame reservations "+p.id()+"/"+q.id());
        }
        check(level.props().equals(base.props())&&level.dock().equals(base.dock()),"Furniture/van moved");
        check(level.colliders().size()==base.colliders().size()&&level.colliders().containsAll(base.colliders().subList(0,5)),"Fixture solids changed");
        check(base.sockets().stream().filter(s->!s.type().equals("parcel_fence")).allMatch(level.sockets()::contains),"Source service/terrain/garden sockets changed");
        var residences=SceneRenderer.class.getDeclaredMethod("streetResidence",List.class,VanStaging.class,VanStreetHouses.House.class);residences.setAccessible(true);
        double nearest=Double.POSITIVE_INFINITY;int shifted=0;
        for(int i=0;i<VanStreetHouses.HOMES.size();i++){
            var before=VanStreetHouses.HOMES.get(i);var after=VanStreetHouses.homes(level).get(i);
            if(before.side()<0){check(before.equals(after),"Far street context changed");continue;}
            check(after.frontageZ()==before.frontageZ()-12&&after.depth()==before.depth()&&after.frontage()==before.frontage()&&after.wallHeight()==before.wallHeight()&&after.masonry()==before.masonry(),"Background model changed instead of moving");
            var rendered=new ArrayList<>();residences.invoke(null,rendered,map.van(),after);double maxX=-100;
            for(var f:rendered)for(String corner:List.of("a","b","c"))maxX=Math.max(maxX,((Number)LevelSceneSource.read(LevelSceneSource.read(f,corner),"x")).doubleValue());
            nearest=Math.min(nearest,g.west()-.4-maxX);shifted++;
            var center=map.van().world((after.buildingStartX()+after.buildingEndX())/2,after.frontageZ());
            check(level.colliders().stream().anyMatch(c->Math.hypot(c.x()-center.x(),c.z()-center.z())<1e-8&&Math.abs(c.halfX()-after.depth()/2)<1e-8),"Moved context has stale collision");
        }
        check(shifted==3&&nearest>=6,"Background houses still crowd dwelling: "+nearest);
        System.out.println("PASS3 background residences/fences/solids moved12m down street; actual rendered clearance="+nearest+"m; models, road and van retained");
        var main=g.parts().stream().filter(p->p.name().equals("main-roof")).findFirst().orElseThrow();
        check(main.triangles().size()==20,"Main roof should be one20triangle closed folded sheet");
        int count=0,visible=0;
        for(var part:g.parts()){
            var edges=new HashMap<String,Integer>();var winding=new HashMap<String,Integer>();double volume=0;
            for(var f:part.triangles()){
                Vec[] p={f.a().p(),f.b().p(),f.c().p()};volume+=determinant(p[0],p[1],p[2])/6;
                for(int i=0;i<3;i++){String a=point(p[i]),b=point(p[(i+1)%3]),key=a.compareTo(b)<0?a+"|"+b:b+"|"+a;
                    edges.merge(key,1,Integer::sum);winding.merge(key,a.compareTo(b)<0?1:-1,Integer::sum);}
                var physical=g.physics().triangle(count++);
                check(close(physical.a(),p[0])&&close(physical.b(),p[1])&&close(physical.c(),p[2]),"Rendered/physical source triangle differs");
                for(var v:List.of(f.a(),f.b(),f.c()))check(Double.isFinite(v.u()+v.v()),"Invalid model UV");
                if(f.visible())visible++;
            }
            check(edges.values().stream().allMatch(n->n==2)&&winding.values().stream().allMatch(n->n==0),"Open/nonmanifold or inconsistent component "+part.name());
            check(volume>1e-7,"Inward or zero-volume component "+part.name()+" "+volume);
        }
        check(count==g.physics().triangleCount(),"Missing physical source triangles");
        contact(g,"main-roof","facade-west",new Vec(11.98,5.59,15));
        contact(g,"main-roof","south-gable",new Vec(16.8,7.33,20.5));
        contact(g,"porch-roof","porch-front-beam",new Vec(19.8,2.495,22.28));
        contact(g,"porch-front-beam","porch-post-18",new Vec(18.43,2.42,22.22));
        contact(g,"porch-post-18","porch-post-foot-18",new Vec(18.43,.12,22.22));
        contact(g,"corner-board-","facade-north",new Vec(11.97,1.5,10.06));
        contact(g,"front-storey-belt","facade-south",new Vec(16.8,2.8,20.55));
        check(!g.physics().clearSight(new Vec(14,8,15),new Vec(14,5,15)),"Roof has no optical mass");
        check(g.physics().clearSight(new Vec(13,8,15),new Vec(20,8,15)),"Clear sky above roof blocked");
        check(!g.physics().clearSight(new Vec(16.8,6.5,22),new Vec(16.8,6.5,19)),"Gable is open");
        check(!g.physics().clearSight(new Vec(19.8,3.6,21.6),new Vec(19.8,2,21.6)),"Porch roof is open");
        check(map.traversable(19.8,.04,21.7),"Porch arrival blocked");
        check(!map.traversable(18.43,.04,22.22),"Porch post has no body contact");
        check(Math.abs(map.groundHeight(19.8,21.7,13)-.04)<1e-8,"Porch floor and feet differ");
        int openings=0;
        for(var p:level.portals())if(p.door()||p.kind().equals("window")){
            boolean front=p.alongX()&&(Math.abs(p.z()-g.north())<.01||Math.abs(p.z()-g.south())<.01),side=!p.alongX()&&(Math.abs(p.x()-g.west())<.01||Math.abs(p.x()-g.east())<.01);
            if(!front&&!side)continue;
            double y=p.y()+(p.kind().equals("window")?1.6:1.3);
            Vec a=new Vec(p.x()-(side?.5:0),y,p.z()-(front?.5:0)),b=new Vec(p.x()+(side?.5:0),y,p.z()+(front?.5:0));
            check(g.physics().clearSight(a,b),"Cladding filled source opening "+p.id());openings++;
        }
        var window=level.portals().stream().filter(p->p.kind().equals("window")&&p.alongX()&&p.z()==10&&p.y()==0).findFirst().orElseThrow();
        var ports=new ArrayList<>(level.portals());ports.remove(window);var edited=HouseRebuildEnvelope.geometry(with(level,level.rooms(),ports));
        check(!edited.physics().clearSight(new Vec(window.x(),1.6,9.5),new Vec(window.x(),1.6,10.5)),"Deleted source opening left a skin hole");
        var rooms=new ArrayList<>(level.rooms());var r=rooms.get(8);rooms.set(8,new Room(r.id(),r.name(),r.x0(),r.z0(),r.x1(),r.z1(),r.y(),r.height(),"parquet",r.acoustic(),r.spawn()));
        check(g.parts().equals(HouseRebuildEnvelope.geometry(with(level,rooms,level.portals())).parts()),"Bathroom finish changes exterior cladding");
        var shell=SceneRenderer.class.getDeclaredMethod("buildLevelShell",HouseMap.class);shell.setAccessible(true);
        List<?> faces=(List<?>)shell.invoke(null,map);int roofFaces=0;
        for(Object f:faces)if(((Number)LevelSceneSource.read(f,"material")).intValue()==LevelMaterials.ROOF)roofFaces++;
        check(roofFaces==6,"Legacy room roofs retained or authored roof omitted: "+roofFaces);
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Envelope blocks real route "+audit.findings());
        if(args.length>0){
            var out=Path.of(args[0]);Files.createDirectories(out);var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);renderer.lamp=true;
            renderer.weatherModule().setTimeOfDay(.42);renderer.weatherModule().pauseClock(true);
            double[][] views={{20,24,1.62,Math.PI,.08},{16.5,6.7,1.62,0,.05},{25.1,18.8,1.62,-1.95,.18},{18.2,28.5,1.62,.827,-.05},{24,25.5,1.62,-2.53,.23}};
            String[] names={"covered-entrance","rear-service","east-envelope","canonical-van","front-architecture"};
            for(int i=0;i<views.length;i++){var v=views[i];double ground=map.groundHeight(v[0],v[1],13);check(map.traversable(v[0],ground,v[1]),"Camera inside solid "+names[i]);renderer.x=v[0];renderer.z=v[1];renderer.eyeHeight=ground+v[2];renderer.yaw=v[3];renderer.pitch=v[4];renderer.drawNetwork(map,List.of(),-1,0);ImageIO.write(renderer.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS dwelling envelope: "+g.parts().size()+" closed outward parts, "+count+" physical/"+visible+" visible triangles; one20triangle main roof; "+openings+" real openings, source edit/material independence, porch contact and all15zone/socket traversal at "+audit.positions()+" positions; digest="+g.digest());
    }
}
