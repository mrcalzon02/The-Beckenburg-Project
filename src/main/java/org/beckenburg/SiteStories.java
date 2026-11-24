package org.beckenburg;
import java.awt.*;
import java.util.List;
import org.beckenburg.LevelDefinition.*;
/** Fictional public history, supported belongings and their readable text; never paranormal evidence. */
public final class SiteStories {
 private SiteStories(){}
 public record Identity(String title,String address,String history){}
 public record Document(String id,String title,List<String> lines){}
 public static String canonical(String site){return site.equals("briar-rebuild")?"house":site;}
 public static Identity identity(String site){site=canonical(site);return switch(site){
  case "house"->new Identity("The Briar House","17 Briar Lane, Beckenburg","Built in 1886. Three generations of the Vale family kept the house; its plumbing was renewed in 1983, while the old cellar became the laundry.");
  case "farmhouse"->new Identity("Hollowmere Farm","4 Mill Road, Beckenburg","Farmhouse dated 1798. Repaired after the 1947 winter; the landing washroom and shed laundry were later additions. The orchard and drying yard remain in use.");
  case "blackpine"->new Identity("Blackpine Logging Camp","North Forest logging road","A shuttered working camp. Payroll records and a shipping manifest disagree about the final crews. This fictional history does not identify the current haunting.");
  case "school"->new Identity("Ravenshade School","8 School Road, Beckenburg","Founded in 1894. The infant department opened in 1911 and a secondary extension followed in 1956. Successive coal, oil and modern boilers survive below the school.");
  case "wld-04"->new Identity("Widow's Peak Fire Lookout","North Forest service road, Widow's Peak",
   "The station logged weather, wind and smoke bearings. It became an emergency relay in late 2022. A radio keeper later maintained the road signal for travellers.");
  case "wld-05"->new Identity("Dead Man's Claim","Highland Frontier mine service road",
   "The assay office recorded grades and wages for the haul crews. The lower workings closed before Emergence Day. A rescue party returned with a tally and marked its withdrawal through the ventilation route.");
  case "wld-06"->new Identity(WharfNativeLevel.TITLE,"Shore street, Coastal Territories",
   "Fishing households and the two working piers shared a repair berth and fish landing. When the roads broke in late 2022, the crews ferried refugees and rationed their remaining fuel.");
  case "wld-07"->new Identity("Saint Bartholomew's Forgotten Cemetery","Cemetery lane, Northern Woodlands",
   "The parish kept separate registers for numbered plots and memorials. The caretaker maintained the grounds and both stairs to the ossuary. A rescue party used the old register to account for missing people in late 2022.");
  case "wld-07-mortuary"->new Identity("Saint Bartholomew's Cemetery and Vale Mortuary","Cemetery lane, Northern Woodlands",
   "Vale Mortuary later opened beside the burial grounds, with a public entrance and a separate vehicle receiving bay. The visitor registry and the parish register record different kinds of arrivals.");
  default->new Identity("Beckenburg site","Beckenburg", "Building history not yet authored.");};}
 public static Document document(String site,String id){site=canonical(site);if(id.startsWith("ROOM_"))return LegacySiteStories.document(site,Integer.parseInt(id.substring(5)));if(id.equals("EMERGENCE"))return WorldLore.fieldRecord(site);if(site.equals("blackpine"))return BlackpineStoryDressing.document(id);var place=identity(site);return switch(id){
  case "TRANSMISSION"->lookoutDocument(site,id,"Ranger relay transcript",List.of(
   "Final entry on the regular ranger frequency, late 2022.",
   "Ground crews reported the forest road interrupted beyond the lower gate. The radio keeper relayed travellers' reports from the ranger room and directed groups to the roadhead.",
   "A later hand copied the channel numbers and marked which replies were heard. The missing replies remain unconfirmed."));
  case "SIGHTING_LOG"->lookoutDocument(site,id,"Cab sighting log",List.of(
   "Fire watch bearings and wind log, Widow's Peak observation deck.",
   "Ordinary smoke bearings stop halfway down the page. The following lines list an emergency frequency and times of calls from the road.",
   "A Meridian team added a return bearing to the lower gate. Its note recommends making contact from the ranger room when the tower stairs are unsafe."));
  case "ASSAY_LEDGER"->mineDocument(site,id,"Assay and wages ledger",List.of(
   "Dead Man's Claim assay office. Final entries before suspension.",
   "The west ore samples are entered against the haul crew's wages. The foreman has struck out the lift allocation and moved the stores tally to the surface.",
   "A later pencil note asks the crew to leave the sampling bench clear for the return party."));
  case "RESCUE_TALLY"->mineDocument(site,id,"Return party tally",List.of(
   "Crew house dispatch bench. Late 2022.",
   "The return party checked the haul stairs and marked the ventilation way back to the headhouse. Two names are ticked on return. The last line is blank.",
   "The writer asks the next party to carry a lamp and leave someone at the roadhead. The lift remains out of service."));
  case "DEPARTURE_TALLY"->wharfDocument(site,id,"Boathouse departure tally",List.of(
   "Boathouse repair bench. First week after the shore road closed.",
   "The coxswain lists the people aboard each crossing by household. The transferred fuel cans are struck off as they go. Two berths were left empty for the return crew.",
   "A later hand rings the last landing time. Leave the western mooring line out for boats returning in the dark."));
  case "CROSSING_SHEET"->wharfDocument(site,id,"Catch and crossing sheet",List.of(
   "Fish house sorting bench. The catch weights stop halfway down the sheet.",
   "The remaining lines list fuel issued against crossings and people waiting on the dock. Use the cross-dock to reach the other pier. The loose outer berth is closed until it can be inspected.",
   "A Meridian surveyor later copied this sheet and left the original at the bench."));
  case "MEMORIAL_REGISTER"->cemeteryDocument(site,id,false,"Memorial register",List.of(
   "Parish copy. Plot numbers run east from the avenue. Replacement stones keep the original number.",
   "The last two caretaker signatures overlap. One hand has crossed out the succession date and left room for a parish countersignature.",
   "Late 2022. The rescue party copied a second list of missing people. Keep it separate from the memorial entries until someone brings word."));
  case "VISITOR_REGISTER"->cemeteryDocument(site,id,true,"Vale visitor register",List.of(
   "Visitors sign in at reception and wait for the attendant before entering the viewing room.",
   "The lined entries are names of visitors. Transfers are recorded on a separate sheet kept at the receiving bay.",
   "Late 2022. The attendant has circled three addresses for the next search party and written a time to meet at the cemetery gate."));
  case "HISTORY"->new Document(id,place.title(),List.of(place.address(),place.history(),"Local history and household records. These details do not identify the haunting."));
  case "LETTERS"->new Document(id,site.equals("school")?"Caretaker's service papers":"Tied correspondence",site.equals("house")?List.of("17 Briar Lane. Mrs E. Vale, 1983.","Please keep Mother's mending tin. The little blue lid still fits, and her needles are inside.","The plumbing men return on Thursday. Leave the cellar laundry clear; the old photographs can stay on the sideboard."):
   site.equals("farmhouse")?List.of("Hollowmere Farm, Mill Road. Spring work list.","Put the clean linen on the yard line. Boots stay by the shed, not beside the upstairs washbasin.","The 1947 repair receipts are tied beneath these letters. Keep them with the farm papers."):
   List.of("Ravenshade School. Heating service ledger.","1894 coal installation retained out of service. Oil plant added in 1956. The modern unit now carries the daily load.","Keep the old furnace doors shut. Check the expansion vessel and leave the electrical aisle clear. Infant and senior washroom fittings are both in service."));
  case "MENDING"->new Document(id,"Inherited mending tin",List.of(place.address(),"A faded tin, wooden thread bobbins, crossed scissors and carefully saved needles. The lid has been kept with it.","Useful things outlived the people who first bought them; the household kept repairing instead of replacing."));
  case "BOOTS"->new Document(id,"Work boots by the laundry",List.of(place.address(),"Mud-darkened leather, worn soles and re-laced eyelets. Left beside the washing area to dry before the next day's work."));
  default->throw new IllegalArgumentException("Unknown readable object");};}
 private static Document cemeteryDocument(String site,String id,boolean annexOnly,String title,List<String> lines){
  if(!site.equals("wld-07-mortuary")&&(annexOnly||!site.equals("wld-07")))
   throw new IllegalArgumentException("Unknown readable object");
  return new Document(id,title,lines);
 }
 private static Document wharfDocument(String site,String id,String title,List<String> lines){
  if(!site.equals("wld-06"))throw new IllegalArgumentException("Unknown readable object");
  return new Document(id,title,lines);
 }
 private static Document mineDocument(String site,String id,String title,List<String> lines){
  if(!site.equals("wld-05"))throw new IllegalArgumentException("Unknown readable object");
  return new Document(id,title,lines);
 }
 private static Document lookoutDocument(String site,String id,String title,List<String> lines){
  if(!site.equals("wld-04"))throw new IllegalArgumentException("Unknown readable object");
  return new Document(id,title,lines);
 }
 private static void reading(LevelPlans.B b,String id,String mesh,int room,String parent,double top,double side,double depth){
  var support=b.props.stream().filter(p->p.room()==room&&p.mesh().equals(parent)).findFirst().orElse(null);if(support==null)throw new IllegalStateException("Missing narrative support "+b.id+" "+parent+" room "+room);
  double x=support.x()+side*Math.cos(support.yaw())-depth*Math.sin(support.yaw()),z=support.z()+side*Math.sin(support.yaw())+depth*Math.cos(support.yaw());
  b.props.add(new Prop(mesh,room,x,support.y()+top,z,1,support.yaw(),0,0,.18,parent));b.socket("story",room,x,support.y()+top,z,0,id);
 }
 static void populate(LevelPlans.B b){
  if(!List.of("house","farmhouse","school").contains(b.id))return;
  int room=b.id.equals("school")?0:1;
  // The centre-front of each sideboard is clear of its built-in books, photographs and cup.
  reading(b,"LETTERS","level_story_letters",room,"level_lived_sideboard",.850,0,.17);
  reading(b,"EMERGENCE","level_trace_journal",room,"level_lived_sideboard",.850,-.44,.17);
  if(!b.id.equals("school"))b.props.removeIf(p->p.room()==6&&p.mesh().equals("notebook_1"));
  if(!b.id.equals("school"))reading(b,"MENDING","level_story_sewing_tin",6,"bedside_table_1",.644,-.21,-.10);
  if(b.id.equals("farmhouse")){
   var washer=b.props.stream().filter(p->p.mesh().equals("level_laundry_washer")).findFirst().orElseThrow();
   b.props.add(new Prop("level_story_work_boots",11,washer.x()+.65,washer.y(),washer.z(),1,0,.21,.20,.33,"floor"));b.socket("story",11,washer.x()+.65,washer.y()+.18,washer.z(),0,"BOOTS");
  }
  var entry=b.rooms.get(0);double x=entry.x0()+1.2,z=entry.z1()+.10;
  b.props.add(new Prop("level_sign_"+b.id,0,x,1.75,z,1,0,0,0,.36,"exterior-wall"));b.socket("story",0,x,1.93,z,0,"HISTORY");
 }
 public static void draw(Graphics2D g,int width,int height,HouseMap map,String id){draw(g,width,height,map,id,new SiteCaseFiles.Reader());}
 public static void draw(Graphics2D g,int width,int height,HouseMap map,String id,SiteCaseFiles.Reader reader){
  var d=document(map.level().id(),id);SiteCaseFiles.sheet(g,width,height,d.title(),d.lines(),reader);
 }
}
