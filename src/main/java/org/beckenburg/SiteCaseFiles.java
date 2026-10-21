package org.beckenburg;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Public assignment dossier. It contains no current entity identity or hidden mission state. */
public final class SiteCaseFiles {
    private SiteCaseFiles(){}
    public record File(String reference,String name,String address,List<String> history,List<String> conditions){}
    public static File forSite(String id){
        var identity=SiteStories.identity(id);
        return switch(id){
            case "house" -> new File("BB / RES-017",identity.title(),identity.address(),List.of(
                "1886: Built as a family home. The Vale family kept photographs, correspondence and repaired household goods across three generations.",
                "1983: Plumbing renewed. The cellar laundry and boiler served a house heated by water radiators, without underfloor heating.",
                "Late 2022 / Emergence Day: Neighbours joined the Vales in a cellar refuge. Their shelter card records a shared headcount and a departure, rather than a list of confirmed deaths.",
                "After evacuation: A Lantern Concord relief crew collected bedding. A later return volunteer left repair notes; those visits did not certify the whole house safe."),List.of(
                "OBJECTIVE: Remove the supernatural threat and prepare the house for a civilian return inspection. Establish findings in the field.",
                "ACCESS: Front approach, two domestic floors and cellar, linked by stairs. Preserve retreat through the hall and foyer.",
                "SERVICES: Existing electricity, boiler and radiator pipework. A working switch is not proof of safe wiring, hot water or a lit boiler.",
                "TRACES: Mending tin, household letters, interrupted chores, refuge bedding and dated return notes. Ordinary wear and old names do not identify the current entity."));
            case "farmhouse" -> new File("BB / AGR-004",identity.title(),identity.address(),List.of(
                "1798: Farmhouse established beside the orchard and farm lane. Repair receipts describe the hard winter of 1947.",
                "Before late 2022: The working farm added a landing washroom and a shed laundry. Tools, seed stores and family rooms shared the same yard.",
                "Emergence Day: Refugees reached Hollowmere as transport failed. Growers pooled food and recorded sleeping places rather than treating the farm as an empty ruin.",
                "The following years: Lantern wardens kept ration and wash rotas. A Meridian Survey correction withdrew an earlier lane clearance; the farm remains a reclamation assignment."),List.of(
                "OBJECTIVE: Clear the occupied buildings so food production and a dependable rural refuge can return.",
                "ACCESS: House, root cellar, barn, hayloft and tool shed. Keep gates, stairs and routes between buildings clear.",
                "SERVICES: Radiators in the house, electrical service and a separate laundry. Barn and loft have no domestic heating; there is no underfloor heat.",
                "TRACES: Saved seed, repaired boots, laundry, allocation lists and later shelter supplies. Recent tins date an occupation, not an entity."));
            case "school" -> new File("BB / CIV-008",identity.title(),identity.address(),List.of(
                "1894: Ravenshade opened as a neighbourhood school. The infant department followed in 1911 and a secondary extension in 1956.",
                "Before Emergence Day: Lessons, school meals, sport and caretaker repairs continued around successive heating installations.",
                "Late 2022: Classrooms became an emergency reception point. Staff and volunteers recorded arrivals, reunions and departures as parts of the building were lost.",
                "The following years: Relief workers used selected rooms. Meridian volunteers checked copied routes; blank names in the evacuation register remain unresolved, not presumed dead."),List.of(
                "OBJECTIVE: Remove the threat and recover a building large enough for public services, shelter and future teaching.",
                "ACCESS: Ground classrooms, cafeteria, nurse room and gym; upper library and assembly rooms; basement plant. Corridors are the main withdrawal routes.",
                "SERVICES: Old coal and oil plant retained out of service beside a modern boiler. Radiators heat occupied rooms; no underfloor heat. Toilets and washbasins show later repairs.",
                "TRACES: Coursework beneath relief registers, meal records, medical supplies and caretaker isolation notes. Children's belongings are human history, never automatic evidence of a child ghost."));
            default -> {
                var story=WorldLore.SITES.stream().filter(s->s.id().equals(id)).findFirst().orElse(null);
                yield new File("BB / "+id.toUpperCase(java.util.Locale.ROOT),story==null?identity.title():story.title(),identity.address(),
                    story==null?List.of(identity.history()):List.of(story.emergence(),story.survival(),story.trace()),
                    List.of("OBJECTIVE: Remove the supernatural threat and make the place safe for ordinary people.","Survey access, services and retreat routes on arrival. Historical testimony is not proof of the current entity."));
            }
        };
    }
    public static List<String> page(HouseMap map,int page,List<String> status){
        var f=forSite(map.level().id());
        return switch(Math.floorMod(page,3)){
            case 0 -> List.of(f.reference(),f.name(),f.address(),"ASSIGNMENT / RECLAMATION",f.conditions().get(0),"HISTORY: "+f.history().get(0),"Turn to the history and field-condition sheets. Observe, record, remove the threat, then return for inspection.");
            case 1 -> f.history();
            default -> {var lines=new ArrayList<>(f.conditions());lines.addAll(status);yield lines;}
        };
    }
    /** Texture is printed on the existing paper triangles, inside their original UV boundary. */
    public static BufferedImage paper(String id){
        var f=forSite(id);var image=new BufferedImage(512,768,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        try{
            g.setColor(new Color(220,211,184));g.fillRect(0,0,512,768);
            g.setColor(new Color(207,198,172));for(int y=0;y<768;y+=3)g.drawLine(0,y,511,y);
            g.setColor(new Color(44,53,50));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,21));g.drawString("BECKENBURG PROJECT",30,55);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,16));g.drawString(f.reference(),30,83);
            g.drawLine(30,100,482,100);g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,28));int y=143;
            for(String line:wrap(g,f.name(),450)){g.drawString(line,30,y);y+=33;}
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,18));
            for(String paragraph:List.of(f.address(),"FIELD ASSIGNMENT",f.conditions().get(0),f.history().get(0),"E: OPEN COMPLETE CASE FILE")){
                y+=18;for(String line:wrap(g,paragraph,450)){if(y>714)break;g.drawString(line,30,y);y+=25;}
            }
        }finally{g.dispose();}return image;
    }
    public static int material(HouseMap map){return GlbProps.surfaceMaterial("Site case sheet v1 / "+map.level().id(),paper(map.level().id()));}
    static List<String> wrap(Graphics2D g,String paragraph,int width){
        var result=new ArrayList<String>();String row="";
        for(String word:paragraph.split(" ")){String next=row.isEmpty()?word:row+" "+word;if(!row.isEmpty()&&g.getFontMetrics().stringWidth(next)>width){result.add(row);row=word;}else row=next;}
        if(!row.isEmpty())result.add(row);return result;
    }
    public static final class Reader {
        int page,scroll;
        public void reset(){page=scroll=0;}
        public void key(int key){
            if(key==KeyEvent.VK_RIGHT||key==KeyEvent.VK_PAGE_DOWN){page=(page+1)%3;scroll=0;}
            if(key==KeyEvent.VK_LEFT||key==KeyEvent.VK_PAGE_UP){page=Math.floorMod(page-1,3);scroll=0;}
            if(key==KeyEvent.VK_UP)scroll=Math.max(0,scroll-1);
            if(key==KeyEvent.VK_DOWN)scroll++;
        }
        public void draw(Graphics2D g,int w,int h,HouseMap map,List<String> status){
            sheet(g,w,h,forSite(map.level().id()).name()+" / "+List.of("Assignment","History","Field conditions").get(page),page(map,page,status),this);
        }
    }
    static void sheet(Graphics2D source,int width,int height,String title,List<String> paragraphs,Reader reader){
        var g=(Graphics2D)source.create();try{
            int w=Math.min(720,width-28),h=Math.min(510,height-32),x=(width-w)/2,y=(height-h)/2;
            g.setColor(new Color(20,25,26,249));g.fillRoundRect(x,y,w,h,10,10);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));g.setColor(new Color(230,212,171));
            int yy=y+26;for(String line:wrap(g,title,w-36)){g.drawString(line,x+18,yy);yy+=21;}
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));var lines=new ArrayList<String>();
            for(String p:paragraphs){lines.addAll(wrap(g,p,w-40));lines.add("");}
            int rows=Math.max(1,(y+h-53-yy)/21);reader.scroll=Math.min(reader.scroll,Math.max(0,lines.size()-rows));
            g.setColor(new Color(216,218,205));for(int i=0;i<rows&&i+reader.scroll<lines.size();i++)g.drawString(lines.get(i+reader.scroll),x+18,yy+11+i*21);
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.setColor(new Color(166,186,177));
            g.drawString("LEFT / RIGHT: PAGES   UP / DOWN: SCROLL   E / ESC: CLOSE",x+18,y+h-18);
        }finally{g.dispose();}
    }
}
