package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;
/** Required daily functions: actual source supports, door sweeps and complete circulation. */
public final class HouseRebuildFurnishingsTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static String corner(GlbProps.Vertex v){return Math.round(v.x()*1e5)+":"+Math.round(v.y()*1e5)+":"+Math.round(v.z()*1e5)+":"+Math.round(v.u()*1e5)+":"+Math.round(v.v()*1e5);}
    static String face(GlbProps.Triangle t){var keys=new ArrayList<>(List.of(corner(t.a()),corner(t.b()),corner(t.c())));Collections.sort(keys);return GlbProps.materialName(t.material())+keys;}
    static String point(GlbProps.Vertex v){return Math.round(v.x()*1e5)+":"+Math.round(v.y()*1e5)+":"+Math.round(v.z()*1e5);}
    static List<double[]> sourceBoxes(String model){
        var triangles=GlbProps.model(model).triangles();var ids=new HashMap<String,Integer>();var roots=new ArrayList<Integer>();
        for(var t:triangles){int first=-1;for(var v:List.of(t.a(),t.b(),t.c())){String key=point(v);Integer id=ids.get(key);if(id==null){id=roots.size();ids.put(key,id);roots.add(id);}if(first<0)first=id;else roots.set(HouseRebuildFunctions.Fit.root(roots,id),HouseRebuildFunctions.Fit.root(roots,first));}}
        var groups=new HashMap<Integer,List<GlbProps.Vertex>>();var counts=new HashMap<Integer,Integer>();
        for(var t:triangles){int root=HouseRebuildFunctions.Fit.root(roots,ids.get(point(t.a())));groups.computeIfAbsent(root,k->new ArrayList<>()).addAll(List.of(t.a(),t.b(),t.c()));counts.merge(root,1,Integer::sum);}
        var boxes=new ArrayList<double[]>();
        for(var entry:groups.entrySet())if(counts.get(entry.getKey())==12){var b=new double[]{9,9,9,-9,-9,-9};var unique=new HashSet<String>();
            for(var v:entry.getValue()){unique.add(point(v));double[] p={v.x(),v.y(),v.z()};for(int i=0;i<3;i++){b[i]=Math.min(b[i],p[i]);b[i+3]=Math.max(b[i+3],p[i]);}}
            boolean box=unique.size()==8;for(var v:entry.getValue()){double[] p={v.x(),v.y(),v.z()};for(int i=0;i<3;i++)box&=Math.abs(p[i]-b[i])<1e-5||Math.abs(p[i]-b[i+3])<1e-5;}if(box)boxes.add(b);
        }return boxes;
    }
    static boolean inside(Collider c,double x,double y,double z){double dx=x-c.x(),dz=z-c.z(),co=Math.cos(c.yaw()),si=Math.sin(c.yaw());return Math.abs(dx*co+dz*si)<c.halfX()-.003&&Math.abs(-dx*si+dz*co)<c.halfZ()-.003&&y>c.y()+.003&&y<c.y()+c.height()-.003;}
    public static void main(String[] args)throws Exception{
        var map=HouseRebuildFurnishings.map();var base=HouseRebuildServices.source();
        check(map.level().props().size()==45,"Missing daily-use/garden functions");check(map.level().stairs().equals(base.stairs())&&map.level().voids().equals(base.voids())&&map.level().dock().equals(base.dock())&&map.level().portals().equals(base.portals()),"Furnishing changed authored structure/arrival");
        check(GlbProps.model("level_house_fridge_worn").triangles().size()==180,"Fridge must keep inspected180triangle asset");
        var fridge=map.level().props().stream().filter(p->p.mesh().equals("level_house_fridge_worn")).findFirst().orElseThrow();
        var coldBody=LevelPlans.propColliders(map.level(),fridge).get(0);
        check(Math.abs(coldBody.x()-14.9)<1e-8&&Math.abs(coldBody.z()-14.1925)<1e-8&&Math.abs(fridge.yaw()-Math.PI)<1e-8,"Fridge must occupy dedicated south-wall storage, facing into kitchen");
        check(Math.abs(14.62-(coldBody.z()+coldBody.halfZ())-.06)<.001,"Fridge rear ventilation gap at actual finished south wall");
        for(var door:map.level().portals())if(door.door()&&(door.joins(2,12)||door.joins(2,3)))
            check(Math.hypot(coldBody.x()-door.x(),coldBody.z()-door.z())>2.0,"Fridge crowds garden or dining doorway");
        check(GlbProps.model("level_house_coat_hung").triangles().size()==200,"Coat must keep supported hanger and original pocket gusset");
        var retained=new HashSet<String>();for(var t:GlbProps.model("level_house_coat_hung").triangles())retained.add(face(t));
        for(var t:GlbProps.model("level_trace_coat").triangles())check(retained.contains(face(t)),"Coat source geometry/UV/material was replaced");
        double garment=HouseRebuildServices.face(new Prop("level_trace_coat",0,0,0,0,1,0,1,1,2,"source"),2,-.24,.83,false).z();
        var pocket=sourceBoxes("level_trace_coat").stream().filter(b->b[0]<-.24&&b[3]>-.24&&b[1]<.83&&b[4]>.83&&b[2]>garment+.002).findFirst().orElseThrow();
        check(sourceBoxes("level_house_coat_hung").stream().anyMatch(b->b[0]<-.24&&b[3]>-.24&&b[1]<.83&&b[4]>.83&&b[2]<garment&&b[5]>pocket[2]),"Original pocket still floats off garment without a real closed gusset");
        int mat=GlbProps.model("level_house_fridge_worn").triangles().get(0).material();var atlas=ImageIO.read(Path.of("development/levels/model-production/house-fridge/atlas-v1.png").toFile());
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){double u=(x+.5)/16,v=(y+.5)/16;int expected=atlas.getRGB((int)(u*atlas.getWidth()),(int)(v*atlas.getHeight()))&0xffffff;check(GlbProps.sample(mat,u,v)==expected,"Fridge source atlas origin/factor differs at "+x+","+y);}
        var terrain=WorldTerrainSurface.of(map.level());var bench=map.level().props().stream().filter(p->p.mesh().equals("level_camp_bench")).findFirst().orElseThrow();
        for(double dx:new double[]{-.685,-.595,.595,.685})for(double dz:new double[]{-.235,-.145,.145,.235})check(Math.abs(bench.y()-HouseRebuildLandscape.height(terrain,13.25+dx,8.5+dz))<.003,"Garden bench foot floats or sinks off actual terrain");
        for(var p:map.level().props())if(p.mesh().equals("planter_pot_mp_1_full"))for(int i=0;i<16;i++){double a=i*Math.PI/8;check(Math.abs(p.y()-HouseRebuildLandscape.height(terrain,p.x()+Math.sin(a)*.105,p.z()+Math.cos(a)*.105))<.003,"Unsupported garden pot footprint");}
        var coat=map.level().props().stream().filter(p->p.mesh().equals("level_house_coat_hung")).findFirst().orElseThrow();check(Math.abs(coat.x()+.025-21.52)<1e-6,"Coat peg back detached from actual east wall");
        check(!map.envelope().physics().clearSight(new Vec(21.51,1.65,19.9),new Vec(21.53,1.65,19.9)),"Coat support is not a physical finished wall");
        var added=map.level().colliders().stream().filter(c->!base.colliders().contains(c)).toList();
        for(var door:map.level().portals())if(door.door())for(int step=0;step<=20;step++){
            double angle=(door.alongX()?0:Math.PI/2)+step*Math.PI/40,c=Math.cos(angle),s=Math.sin(angle),hx=door.x()+(door.alongX()?door.width()/2:0),hz=door.z()+(door.alongX()?0:door.width()/2);
            for(var t:GlbProps.model("door_1_loose").triangles())for(var v:List.of(t.a(),t.b(),t.c())){double x=hx+v.x()*door.width()*c-v.z()*s,y=door.y()+v.y()*2.28/2.1,z=hz+v.x()*door.width()*s+v.z()*c;
                for(var collider:added)check(!inside(collider,x,y,z),"Door swing intersects new furnishing portal="+door.id()+" step="+step+" at="+collider.x()+","+collider.z());}
        }
        check(map.clutter().states().size()==8,"Functional fill changed authored loose objects");check(HidingBudget.candidates(map.level()).equals(HidingBudget.candidates(base)),"Decorative garden objects invented hiding pockets");
        var audit=LevelTraversalAudit.inspect(map);check(audit.passed(),"Furnishing blocks required routes "+audit.findings());HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.GENERATOR);HouseRebuildPowerTest.approach(map,InteractionFocus.Kind.BREAKER);
        HouseRebuildPowerTest.main(new String[]{"FURNISHINGS"});GhostElectricalTest.main(new String[]{"FURNISHINGS"});
        if(args.length>0){var out=Path.of(args[0]);Files.createDirectories(out);var scene=new SceneRenderer();scene.setRenderSize(1000,625);scene.lamp=true;scene.roomLights=new RoomLighting();
            double[][] views={{19.35,20.05,0,20.65,19.125,.55},{19.8,19.7,0,21.42875,19.9,1.2},{16,12.4,2,14.9,14.1925,.85},{16,12.4,2,17.5,10.5075,.85},{14.8,9.4,12,13.25,8.5,.45}};String[] names={"vestibule-boot-storage","supported-coat-hooks","kitchen-cold-storage","kitchen-doorways-clear","garden-sitting-and-pots"};
            for(int i=0;i<views.length;i++){var p=views[i];double floor=map.groundHeight(p[0],p[1],(int)p[2]);check(map.traversable(p[0],floor,p[1])&&map.roomAt(p[0],floor,p[1])==(int)p[2],"Furnishing camera blocked or on wrong floor "+names[i]);scene.x=p[0];scene.z=p[1];scene.eyeHeight=floor+1.62;scene.yaw=Math.atan2(p[3]-p[0],p[4]-p[1]);scene.pitch=Math.atan2(p[5]-1.62,Math.hypot(p[3]-p[0],p[4]-p[1]));scene.drawNetwork(map,List.of(),-1,.4);ImageIO.write(scene.image(),"png",out.resolve(names[i]+"-runtime.png").toFile());}
        }
        System.out.println("PASS actual180triangle fridge atlas/UVs,retained coat source surfaces,attached wall rail,bench/pot terrain contact,21door poses,unchanged cover and whole-House routes="+audit.positions());
    }
}
