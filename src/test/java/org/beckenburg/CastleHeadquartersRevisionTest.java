package org.beckenburg;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
/** Real source models, walkable warehouse and persisted Castle-to-mission preparation. */
public final class CastleHeadquartersRevisionTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  var map=new CastleHubMap();
  check(map.valid(),"original Castle Beckenburg stations remain connected");
  check(CastleWarehouse.racks().size()==15,"fifteen open retail fixtures with clear browsing aisles");
  long portable=EquipmentRegistry.all().stream()
      .filter(tool->tool.origin()!=EquipmentRegistry.Origin.TRUCK).count();
  check(CastleWarehouse.exhibits().size()==(portable+1)*3,
   "portable ghost-hunting tools and health supplies only; van fixtures are excluded");
  var ids=new HashSet<String>();
  for(var exhibit:CastleWarehouse.exhibits()){
   check(ids.add(exhibit.id()+"/"+exhibit.tier()),"exhibit families are not silently duplicated");
   check(GlbProps.availableModels().contains(exhibit.mesh()),"missing source-backed mesh "+exhibit.mesh());
   double scale=exhibit.scale();
   check(scale>0&&scale<=exhibit.scale(),"model scale is bounded by physical rack bay");
   var bounds=GlbProps.model(exhibit.mesh()).triangles();
   double x0=Double.POSITIVE_INFINITY,x1=-x0,y0=x0,y1=-x0,z0=x0,z1=-x0;
   for(var triangle:bounds)for(var vertex:new GlbProps.Vertex[]{
       triangle.a(),triangle.b(),triangle.c()}){
    x0=Math.min(x0,vertex.x());x1=Math.max(x1,vertex.x());
    y0=Math.min(y0,vertex.y());y1=Math.max(y1,vertex.y());
    z0=Math.min(z0,vertex.z());z1=Math.max(z1,vertex.z());
   }
   check((x1-x0)*scale<=.551&&(y1-y0)*scale<=.621&&(z1-z0)*scale<=.501,
       "source GLB mesh must fit a real display bay");
   check(exhibit.x()>29&&exhibit.x()<42&&exhibit.z()>18&&exhibit.z()<33.5,
     "models remain in enclosed east warehouse");
  }
  check(!map.traversable(CastleWarehouse.racks().get(0).x(),CastleWarehouse.racks().get(0).z()),
    "shelf is a physical navigation blocker");
  check(CastleWarehouse.aimed(map,33.9,24.5,-Math.PI/2),
    "shop can be activated from a real adjacent aisle");
  var van=CastleSurroundings.parkedVehicles().get(0);
  check(van.mesh().equals("ggbot_car08_van_shell")&&van.z()<33&&van.x()>10&&van.x()<20,
    "the actual source-backed expedition van is displayed in the open courtyard");
  check(!map.traversable(van.x(),van.z()),"courtyard van uses physical collision");
  check(map.traversable(19.9,21.0,3.4),
    "original upper gallery joins the castle eastern battlements");
  check(map.traversable(20.2,28.0,3.4),
    "eastern parapet has a solid upper walking deck");
  check(map.traversable(13.0,32.0,3.4),
    "southern wall-walk is connected to the upper battlements");
  check(!map.traversable(13.0,30.0,3.4),
    "open courtyard beside ramparts has no imaginary walkable floor");
  check(CastleHubArchitecturalPlan.trims().stream().filter(
    x->x.mesh().equals("kenney_medieval_tower_top")).count()>=8,
    "castle has exterior corner towers and gatehouse towers");
  var home=Files.createTempDirectory("castle-loadout-");
  try{
   var path=home.resolve("profile.properties");
   var profile=new ProfileStore(path);
   check(profile.addNetworkPayout(UUID.randomUUID(),300,0),"test account funds");
   check(profile.purchaseEquipment(EquipmentLoadout.Item.UV_LAMP),"castle equipment purchase persists");
   var selected=new EquipmentLoadout.Item[]{EquipmentLoadout.Item.UV_LAMP,
    EquipmentLoadout.Item.EMF,EquipmentLoadout.Item.THERMOMETER,
    EquipmentLoadout.Item.SPIRIT_BOX};
   profile.prepareLoadout(selected);
   var restored=new ProfileStore(path);restored.load();
   check(Arrays.equals(selected,restored.preparedLoadout()),"loadout saved across process start");
   var mission=new EquipmentLoadout();
   mission.replaceAtRack(restored.preparedLoadout());
   for(int i=0;i<EquipmentLoadout.SLOTS;i++)
    check(mission.at(i)==selected[i],"dispatch loads actual castle-selected equipment");
   boolean denied=false;
   try{restored.prepareLoadout(new EquipmentLoadout.Item[]{selected[0],selected[0],selected[2],selected[3]});}
   catch(IllegalArgumentException expected){denied=true;}
   check(denied,"cannot equip duplicate copies into four slots");
   var counter=new CampaignStorePanel(restored,()->{});
   check(counter.getName().equals("campaign.store"),"warehouse opens real persisted quartermaster");
  }finally{
   try(var walk=Files.walk(home)){for(var f:walk.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(f);}
  }
  System.out.println("PASS complete castle showroom, courtyard van, towers, shop interaction and pre-mission saved equipment");
 }
}
