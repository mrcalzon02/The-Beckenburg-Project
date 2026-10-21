package org.beckenburg;
import java.awt.*;
import java.util.List;
import org.beckenburg.LevelDefinition.*;
/** Fictional public history, supported belongings and their readable text; never paranormal evidence. */
public final class SiteStories {
 private SiteStories(){}
 public record Identity(String title,String address,String history){}
 public record Document(String id,String title,List<String> lines){}
 public static Identity identity(String site){return switch(site){
  case "house"->new Identity("The Briar House","17 Briar Lane, Beckenburg","Built in 1886. Three generations of the Vale family kept the house; its plumbing was renewed in 1983, while the old cellar became the laundry.");
  case "farmhouse"->new Identity("Hollowmere Farm","4 Mill Road, Beckenburg","Farmhouse dated 1798. Repaired after the 1947 winter; the landing washroom and shed laundry were later additions. The orchard and drying yard remain in use.");
  case "blackpine"->new Identity("Blackpine Logging Camp","North Forest logging road","A shuttered working camp. Payroll records and a shipping manifest disagree about the final crews. This fictional history does not identify the current haunting.");
  case "school"->new Identity("Ravenshade School","8 School Road, Beckenburg","Founded in 1894. The infant department opened in 1911 and a secondary extension followed in 1956. Successive coal, oil and modern boilers survive below the school.");
  default->new Identity("Beckenburg site","Beckenburg", "Building history not yet authored.");};}
 public static Document document(String site,String id){var place=identity(site);return switch(id){
  case "HISTORY"->new Document(id,place.title(),List.of(place.address(),place.history(),"Local history and household records. These details do not identify the haunting."));
  case "LETTERS"->new Document(id,site.equals("school")?"Caretaker's service papers":"Tied correspondence",site.equals("house")?List.of("17 Briar Lane. Mrs E. Vale, 1983.","Please keep Mother's mending tin. The little blue lid still fits, and her needles are inside.","The plumbing men return on Thursday. Leave the cellar laundry clear; the old photographs can stay on the sideboard."):
   site.equals("farmhouse")?List.of("Hollowmere Farm, Mill Road. Spring work list.","Put the clean linen on the yard line. Boots stay by the shed, not beside the upstairs washbasin.","The 1947 repair receipts are tied beneath these letters. Keep them with the farm papers."):
   List.of("Ravenshade School. Heating service ledger.","1894 coal installation retained out of service. Oil plant added in 1956. The modern unit now carries the daily load.","Keep the old furnace doors shut. Check the expansion vessel and leave the electrical aisle clear. Infant and senior washroom fittings are both in service."));
  case "MENDING"->new Document(id,"Inherited mending tin",List.of(place.address(),"A faded tin, wooden thread bobbins, crossed scissors and carefully saved needles. The lid has been kept with it.","Useful things outlived the people who first bought them; the household kept repairing instead of replacing."));
  case "BOOTS"->new Document(id,"Work boots by the laundry",List.of(place.address(),"Mud-darkened leather, worn soles and re-laced eyelets. Left beside the washing area to dry before the next day's work."));
  default->throw new IllegalArgumentException("Unknown readable object");};}
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
  if(!b.id.equals("school"))b.props.removeIf(p->p.room()==6&&p.mesh().equals("notebook_1"));
  if(!b.id.equals("school"))reading(b,"MENDING","level_story_sewing_tin",6,"bedside_table_1",.644,-.21,-.10);
  if(b.id.equals("farmhouse")){
   var washer=b.props.stream().filter(p->p.mesh().equals("level_laundry_washer")).findFirst().orElseThrow();
   b.props.add(new Prop("level_story_work_boots",11,washer.x()+.65,washer.y(),washer.z(),1,0,.21,.20,.33,"floor"));b.socket("story",11,washer.x()+.65,washer.y()+.18,washer.z(),0,"BOOTS");
  }
  var entry=b.rooms.get(0);double x=entry.x0()+1.2,z=entry.z1()+.10;
  b.props.add(new Prop("level_sign_"+b.id,0,x,1.75,z,1,0,0,0,.36,"exterior-wall"));b.socket("story",0,x,1.93,z,0,"HISTORY");
 }
 public static void draw(Graphics2D source,int width,int height,HouseMap map,String id){
  Document d=document(map.level().id(),id);Graphics2D g=(Graphics2D)source.create();try{
   int w=Math.min(670,width-36),x=(width-w)/2,y=Math.max(22,(height-340)/2),line=23;
   g.setColor(new Color(20,23,23,248));g.fillRoundRect(x,y,w,Math.min(340,height-44),10,10);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));g.setColor(new Color(226,209,166));g.drawString(d.title(),x+20,y+29);
   g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.setColor(new Color(216,214,198));int yy=y+60;
   for(String paragraph:d.lines()){
    String row="";for(String word:paragraph.split(" ")){String next=row.isEmpty()?word:row+" "+word;if(g.getFontMetrics().stringWidth(next)>w-40&&!row.isEmpty()){g.drawString(row,x+20,yy);yy+=line;row=word;}else row=next;}g.drawString(row,x+20,yy);yy+=line+10;
   }
   g.setColor(new Color(164,175,169));g.drawString("E / ESC CLOSE",x+20,y+Math.min(340,height-44)-18);
  }finally{g.dispose();}
 }
}
