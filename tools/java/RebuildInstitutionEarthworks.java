import java.nio.file.*;
import java.util.*;
import java.security.*;
import org.beckenburg.editor.*;
import org.beckenburg.TerrainSceneNativePhysics;
/** Saved sculpt layers for geographic site clearings. No route meshes or centerline input. */
public final class RebuildInstitutionEarthworks {
 record P(double x,double z){}
 record Field(String name,TerrainLayerStack.Material material,double feather,String profile,List<P> polygon){}
 static String hash(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
 static boolean inside(double x,double z,List<P> p){boolean in=false;for(int i=0,j=p.size()-1;i<p.size();j=i++){P a=p.get(i),b=p.get(j);if((a.z>z)!=(b.z>z)&&x<(b.x-a.x)*(z-a.z)/(b.z-a.z)+a.x)in=!in;}return in;}
 static double edgeDistance(double x,double z,List<P> polygon){double result=Double.POSITIVE_INFINITY;for(int i=0;i<polygon.size();i++){P a=polygon.get(i),b=polygon.get((i+1)%polygon.size());double dx=b.x-a.x,dz=b.z-a.z,t=Math.max(0,Math.min(1,((x-a.x)*dx+(z-a.z)*dz)/(dx*dx+dz*dz)));result=Math.min(result,Math.hypot(x-a.x-t*dx,z-a.z-t*dz));}return result;}
 static boolean covered(double x,double z,List<List<P>> polygons){for(var p:polygons)if(inside(x,z,p)||edgeDistance(x,z,p)<.00001)return true;return false;}
 static List<P> polygon(String[] v,int start){List<P> p=new ArrayList<>();for(int i=start;i<v.length;i+=2)p.add(new P(Double.parseDouble(v[i]),Double.parseDouble(v[i+1])));return p;}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);String site=args[1];Path worldPath=root.resolve("worlds").resolve(site.toLowerCase()).resolve("terrain-world"),program=root.resolve("program").resolve(site.toLowerCase());
  String heightBefore=hash(worldPath.resolve("tile_0_0.png")),topologyBefore=hash(worldPath.resolve("adaptive-topology.bin")),cutsBefore=hash(worldPath.resolve("authored-cuts.tsv"));
  var world=StitchedHeightmapWorld.open(worldPath);var settings=world.settings();var layers=new TerrainLayerStack(world,0,0);String name="Geographic site earthworks "+site;
  boolean authored=layers.layers().stream().anyMatch(l->l.name().equals(name));
  System.out.println("EARTHWORK_SOURCE "+site+" layers="+layers.layerCount());if(!authored)layers.selectLayer(layers.addLayer(name));else for(int i=0;i<layers.layerCount();i++)if(layers.layers().get(i).name().equals(name))layers.selectLayer(i);
  List<Field> fields=new ArrayList<>();for(String row:Files.readAllLines(program.resolve("features.tsv"))){if(row.isBlank())continue;String[] v=row.split("\t");fields.add(new Field(v[0],TerrainLayerStack.Material.valueOf(v[1]),Double.parseDouble(v[2]),v[3],polygon(v,4)));}
  List<double[]> protectedRooms=new ArrayList<>();for(String row:Files.readAllLines(program.resolve("protected-rooms.tsv"))){if(!row.isBlank())protectedRooms.add(Arrays.stream(row.split("\t")).mapToDouble(Double::parseDouble).toArray());}
  List<List<P>> underlays=new ArrayList<>();for(String row:Files.readAllLines(program.resolve("underlays.tsv")))if(!row.isBlank())underlays.add(polygon(row.split("\t"),0));
  int changed=0,raised=0,lowered=0;double maxChange=0;int n=layers.cells();var baseline=new TerrainLayerStack(StitchedHeightmapWorld.open(Path.of(args[2])),0,0);double[][] before=new double[n+1][n+1];for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)before[z][x]=baseline.height(x,z);
  for(int z=1;z<n;z++)for(int x=1;x<n;x++){
   double wx=settings.originX()+x*settings.metresPerCell(),wz=settings.originZ()+z*settings.metresPerCell();
   boolean protect=false;for(double[] box:protectedRooms)if(wx>box[0]+.00001&&wx<box[2]-.00001&&wz>box[1]+.00001&&wz<box[3]-.00001){protect=true;break;}if(protect)continue;
   double strongest=-1;
   for(Field f:fields){boolean core=inside(wx,wz,f.polygon)||edgeDistance(wx,wz,f.polygon)<.00001;double distance=core?0:edgeDistance(wx,wz,f.polygon);if(distance>=f.feather)continue;
    double blend=core?1:1-distance/f.feather;blend=blend*blend*(3-2*blend);if(blend<=strongest)continue;strongest=blend;double target=0;
    if(f.profile.equals("woodland")){double away=Math.min(1,Math.max(0,(-8-wx)/3));target=Math.min(0,away*(-.06+.02*Math.sin(wz/7)+.012*Math.cos(wx/4)));}
    boolean under=covered(wx,wz,underlays)&&covered(wx+.05,wz,underlays)&&covered(wx-.05,wz,underlays)&&covered(wx,wz+.05,underlays)&&covered(wx,wz-.05,underlays);
    if(under)target=Math.min(target,-.04);
    double h=baseline.height(x,z),desired=h+(target-h)*blend;
    layers.brush(TerrainLayerStack.Brush.FLATTEN,x,z,.49,1,desired,0,1,TerrainLayerStack.FlattenLimit.BOTH);
    if(core&&!under&&(!f.profile.equals("woodland")||wx>-14))layers.paintMaterial(x,z,.49,f.material,90);
   }
   double delta=layers.height(x,z)-before[z][x];if(Math.abs(delta)>1e-7){changed++;if(delta>0)raised++;else lowered++;maxChange=Math.max(maxChange,Math.abs(delta));}
  }
  layers.finishStroke();layers.save();changed=0;raised=0;lowered=0;maxChange=0;for(int z=0;z<=n;z++)for(int x=0;x<=n;x++){double delta=layers.height(x,z)-before[z][x];if(Math.abs(delta)>1e-7){changed++;if(delta>0)raised++;else lowered++;maxChange=Math.max(maxChange,Math.abs(delta));}}
  if(!heightBefore.equals(hash(worldPath.resolve("tile_0_0.png")))||!topologyBefore.equals(hash(worldPath.resolve("adaptive-topology.bin")))||!cutsBefore.equals(hash(worldPath.resolve("authored-cuts.tsv"))))throw new IllegalStateException("Changed protected source image or punches");
  Path marker=worldPath.resolve("authored-path-grading.sha256");if(Files.exists(marker)){var old=Files.readAllLines(marker);Files.writeString(marker,old.get(0)+"\n"+hash(layers.path())+"\n");}
  var scene=new TerrainScenePackage(world).openScene();record Chunk(String name,TerrainEntranceMesh.Bounds bounds){}List<Chunk> chunks=new ArrayList<>();
  for(var node:scene.nodes())for(var comp:node.components())if(comp.type().equals(TerrainScenePackage.COMPONENT)){var p=comp.properties();chunks.add(new Chunk(p.get("chunk"),new TerrainEntranceMesh.Bounds(Double.parseDouble(p.get("boundsX0")),Double.parseDouble(p.get("boundsZ0")),Double.parseDouble(p.get("boundsX1")),Double.parseDouble(p.get("boundsZ1")))));}
  if(chunks.isEmpty())throw new IllegalStateException("No authoritative published bounds");
  for(var chunk:chunks)TerrainScenePackage.publish(world,scene,chunk.name,chunk.bounds);
  var disk=StitchedHeightmapWorld.open(worldPath);var diskLayers=new TerrainLayerStack(disk,0,0);for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)if(Math.abs(layers.height(x,z)-diskLayers.height(x,z))>1e-7)throw new IllegalStateException("Sculpt did not survive disk reopen");
  var diskScene=new TerrainScenePackage(disk).openScene();if(!TerrainScenePackage.inspect(disk,diskScene).isEmpty())throw new IllegalStateException("Stale scene after sculpt");
  Files.write(worldPath.resolve("terrain-physics.hcs2"),TerrainSceneNativePhysics.encode(disk,diskScene));var physics=TerrainSceneNativePhysics.load(disk,diskScene).physics();int holes=0;
  for(String row:Files.readAllLines(worldPath.resolve("authored-cuts.tsv"))){if(row.isBlank())continue;double[] q=Arrays.stream(row.split("\t")).limit(4).mapToDouble(Double::parseDouble).toArray();double x=(q[0]+q[2])/2,z=(q[1]+q[3])/2;if(!physics.clearSight(new org.beckenburg.LevelDefinition.Vec(x,24,z),new org.beckenburg.LevelDefinition.Vec(x,-24,z)))throw new IllegalStateException("Saved hole filled by sculpt");holes++;}
  StringBuilder grid=new StringBuilder("x\tz\tbefore_y\tafter_y\tmaterial\n");for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)grid.append(settings.originX()+x*settings.metresPerCell()).append('\t').append(settings.originZ()+z*settings.metresPerCell()).append('\t').append(before[z][x]).append('\t').append(diskLayers.height(x,z)).append('\t').append(diskLayers.material(x,z)).append('\n');Files.writeString(program.resolve("saved-grid.tsv"),grid);
  Files.writeString(worldPath.resolve("earthworks-verification.json"),String.format(Locale.ROOT,"{\"site\":\"%s\",\"changed_vertices\":%d,\"raised_vertices\":%d,\"lowered_vertices\":%d,\"max_change_m\":%.9f,\"source_height_sha256\":\"%s\",\"saved_topology_sha256\":\"%s\",\"saved_cuts_sha256\":\"%s\",\"hole_controls\":%d,\"disk_reopen_verified\":true,\"physics_sha256\":\"%s\",\"status\":\"GEOGRAPHIC_SAVED_EARTHWORKS_RUNTIME_OPEN\"}\n",site,changed,raised,lowered,maxChange,heightBefore,topologyBefore,cutsBefore,holes,hash(worldPath.resolve("terrain-physics.hcs2"))));
  System.out.println("SAVED_SITE_EARTHWORKS "+site+" raised="+raised+" lowered="+lowered+" holes preserved="+holes);
 }
}
