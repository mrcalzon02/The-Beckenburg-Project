package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Physical traces of work, closure, weather and recent shelter; never ghost-type evidence. */
public final class BlackpineStoryDressing {
    private BlackpineStoryDressing(){}
    public static final Set<String> ASSETS=Set.of("level_trace_records","level_trace_journal","level_trace_calendar",
        "level_trace_coat","level_trace_photograph","level_trace_clothes","level_trace_bedroll","level_trace_food_trash",
        "level_trace_floor_wear","level_trace_wall_damage","level_trace_damp_wall","level_trace_leak_bucket",
        "level_trace_window_boards","level_trace_woodstove","level_trace_electric_branch","level_trace_generator",
        "level_trace_oil_waterheater","level_trace_wash_trough","level_trace_camp_bed");
    public record RoomStory(int room,String formerUse,String heat,String condition,String recentTrace){}
    public static final List<RoomStory> ROOMS=List.of(
        new RoomStory(0,"Sawmill production","Unheated industrial volume","Dragged timber scuffs; damp on the low wall","No fresh domestic rubbish"),
        new RoomStory(1,"Dispatch office","Unheated after closure","Manifest and stopped calendar survive above the damp","No bed or food tins"),
        new RoomStory(2,"Crew meals","Wood fireplace with masonry chimney","Worn perimeter seating; cold hearth","No new sleeping area"),
        new RoomStory(3,"Food preparation","Solid fuel range; no underfloor heat","Dry tins and utensils remain; water isolated","Supplies taken toward the cabin"),
        new RoomStory(4,"Crew sleeping quarters","Flued woodstove","Three surviving beds, coat and clothes; boarded east window","Old belongings, no recent food tins"),
        new RoomStory(5,"Crew washing","Oil water heater heats water only; room otherwise cold","Trough, dry supply and low damp; no radiant floor","Leak bucket was moved under a drip"),
        new RoomStory(6,"Foreman's lodging","Wood fireplace with masonry chimney","Bed, family photograph, work journal and old boots","Sealed off from the recent camp"),
        new RoomStory(7,"Logging crew cabin","Flued woodstove","Boarded west window and old single bed","One clean blue bedroll, opened tins and recent journal"),
        new RoomStory(8,"Equipment repair and power service","Unheated","Oil-dark floor and generator with maintenance inlet","Recent caretaker electrical inspection"),
        new RoomStory(9,"Payroll and stores records","Unheated records office","Final payroll beside stopped calendar","Records untouched by the recent occupants"),
        new RoomStory(10,"Tool issue and sharpening","Unheated","Tool ledger, coat and boot scuffs","No signs of domestic occupation"),
        new RoomStory(11,"Mill inspection and tallying","Unheated open inspection loft","Tally book and light surface splits; deck remains intact","No bedroll or fresh rubbish")
    );
    public static LevelDefinition apply(LevelDefinition base){
        var d=new Dressing(base);d.build();
        return new LevelDefinition(base.id(),"blackpine-story-v1",base.title(),base.rooms(),base.portals(),base.stairs(),base.voids(),d.props,d.sockets,d.colliders,base.dock(),base.ambience());
    }
    private static final class Dressing {
        final LevelDefinition base;final List<Prop> props;final List<Socket> sockets;final List<Collider> colliders;
        Dressing(LevelDefinition l){base=l;props=new ArrayList<>(l.props());sockets=new ArrayList<>(l.sockets());colliders=new ArrayList<>(l.colliders());}
        void item(String name,int room,double x,double y,double z,double yaw,String support){
            props.add(new Prop(name.startsWith("level_")?name:"level_trace_"+name,room,x,y,z,1,yaw,0,0,0,support));
        }
        void solid(String name,int room,double x,double z,double yaw,double hx,double hz,double h){
            item(name,room,x,base.floorY(room),z,yaw,"floor");
            colliders.add(new Collider(x,base.floorY(room),z,hx,hz,h,yaw));
            if(name.equals("woodstove"))colliders.add(new Collider(x,base.floorY(room)+.84,z-.13,.08,.08,2.88,0));
            if(name.equals("oil_waterheater"))colliders.add(new Collider(x,base.floorY(room)+1.36,z,.08,.08,2.20,0));
        }
        void read(String id,String mesh,int room){
            var table=props.stream().filter(p->p.room()==room&&p.mesh().equals("dinner_table_1")).findFirst().orElseThrow();
            props.removeIf(p->p.room()==room&&p.mesh().equals("book_mp_2")&&p.support().equals("dinner_table_1"));
            item(mesh,room,table.x(),table.y()+.867,table.z(),table.yaw(),"dinner_table_1");
            sockets.add(new Socket("blackpine-story-"+id,"story",room,table.x(),table.y()+.867,table.z(),table.yaw(),id));
        }
        void wall(String mesh,int room,double x,double z,double yaw){item(mesh,room,x,base.floorY(room)+.008,z,yaw,"wall");}
        void boards(int room,double x,double z,double yaw){
            item("window_boards",room,x,base.floorY(room)+1.02,z,yaw,"window-frame");
            colliders.add(new Collider(x,base.floorY(room)+1.02,z,.66,.045,1.31,yaw));
        }
        void moveForemanDesk(){
            // The fireplace occupies the old generic desk position. Move its complete support group.
            for(int i=0;i<props.size();i++){
                var p=props.get(i);if(p.room()!=6||!Set.of("dinner_table_1","chair_mp_5","book_mp_2").contains(p.mesh()))continue;
                if(p.halfX()>0){colliders.removeIf(c->Math.hypot(c.x()-p.x(),c.z()-p.z())<.001&&Math.abs(c.y()-p.y())<.001);
                    colliders.add(new Collider(p.x()+.4,p.y(),p.z()+3.8,p.halfX(),p.halfZ(),p.height(),p.yaw()));}
                props.set(i,new Prop(p.mesh(),p.room(),p.x()+.4,p.y(),p.z()+3.8,p.scale(),p.yaw(),p.halfX(),p.halfZ(),p.height(),p.support()));
            }
        }
        void build(){
            moveForemanDesk();
            read("MANIFEST","records",1);read("PAYROLL","records",9);read("FOREMAN","journal",6);
            read("SHELTER","journal",7);read("CREW","journal",4);read("KITCHEN","journal",3);
            props.removeIf(p->p.room()==3&&p.mesh().equals("plate_mp_1")&&p.support().equals("dinner_table_1"));
            props.add(new Prop("plate_mp_1",3,20.0,.925,30.5,1,0,0,0,.03,"level_kitchen_counter"));
            read("TOOLS","records",10);read("TALLY","journal",11);
            for(var story:ROOMS){
                var r=base.room(story.room());double fy=r.y();
                // Utility runs are surface mounted on the real north wall, away from door openings.
                wall("electric_branch",r.id(),r.x1()-1.3,r.z0()+.085,0);
                wall("wall_damage",r.id(),r.x0()+.095,r.z1()-1.35,-Math.PI/2);
                wall("damp_wall",r.id(),r.x1()-1.45,r.z1()-.095,Math.PI);
                item("floor_wear",r.id(),r.x0()+1.4,fy+.004,r.z1()-1.4,.23,"floor-surface");
                // A readable room ledger also participates in the map data hash, without a new UI.
                sockets.add(new Socket("blackpine-room-history-"+r.id(),"site_history",r.id(),r.x0()+.10,fy,r.z0()+.1,0,
                    story.formerUse()+" | "+story.heat()+" | "+story.condition()+" | "+story.recentTrace()));
            }
            wall("calendar",1,45.9,42.7,Math.PI/2);wall("calendar",9,23.9,6.5,Math.PI/2);
            solid("camp_bed",4,14.9,22.4,0,.465,1.025,.82);
            solid("camp_bed",4,9.1,23.8,0,.465,1.025,.82);
            solid("camp_bed",4,14.9,15.5,0,.465,1.025,.82);
            solid("woodstove",4,9.05,18.0,0,.48,.515,.84);
            solid("camp_bed",6,28.95,9.6,0,.465,1.025,.82);
            solid("camp_bed",7,38.9,5.8,0,.465,1.025,.82);
            solid("woodstove",7,34.9,10.75,0,.48,.515,.84);
            solid("oil_waterheater",5,6.8,37.0,0,.60,.49,1.36);
            solid("wash_trough",5,9,36.5,0,.86,.30,1.14);
            solid("generator",8,45.1,47.8,0,.79,.45,1.16);
            // A cold solid-fuel range supplies the kitchen; the same reusable stove has a real roof flue.
            solid("woodstove",3,21.05,34.8,0,.48,.515,.84);
            props.add(new Prop("dinner_table_1",2,19.7,0,37.7,1,0,.85,.55,.86,"floor"));
            colliders.add(new Collider(19.7,0,37.7,.85,.55,.86,0));
            solid("level_camp_bench",2,19.7,38.65,0,.875,.31,.91);
            item("food_trash",3,20.65,.925,30.5,0,"level_kitchen_counter");
            item("photograph",6,26.45,.867,13.52,0,"dinner_table_1");
            item("clothes",4,14.8,0,24.3,.2,"floor");item("clothes",6,29,0,11.2,.8,"floor");
            solid("bedroll",7,38.65,10.2,.07,.37,.95,.22);
            item("clothes",7,39.05,0,8.9,.4,"floor");item("food_trash",7,38.0,0,10.8,.5,"floor");
            for(int room:new int[]{4,6,10}){
                var r=base.room(room);item("level_story_work_boots_worn",room,r.x0()+.5,0,r.z1()-2.3,0,"floor");
                wall("coat",room,r.x0()+.10,r.z1()-3.2,-Math.PI/2);
            }
            for(double[] at:new double[][]{{5,10.9,40.8},{0,51.2,21.3},{1,45.2,43.2}})
                solid("leak_bucket",(int)at[0],at[1],at[2],0,.22,.22,.52);
            boards(4,15.88,18,Math.PI/2);boards(7,34.12,8,-Math.PI/2);
            wall("damp_wall",4,15.87,18.15,Math.PI/2);wall("damp_wall",7,34.13,8.1,-Math.PI/2);
            // Readable service sheet uses its own support, so it never floats on a wall or in a route.
            item("records",8,49,1.006,47,0,"wooden_crate_1");
            sockets.add(new Socket("blackpine-story-SERVICE","story",8,49,1.006,47,0,"SERVICE"));
        }
    }
    public static SiteStories.Document document(String id){
        var lines=switch(id){
            case "HISTORY"->List.of("Blackpine Logging Camp. Work suspended in November 2009.","The final payroll and dispatch manifest disagree about the last crew. Weather damaged the empty camp; a few people used the logging cabin for shelter much later.","These records describe the site, not the identity of its haunting.");
            case "PAYROLL"->List.of("Payroll office. Final week, November 2009.","Eleven crew wages signed out. Two envelopes remain uncollected. The dispatch sheet lists a different final headcount.","Do not amend either list until the transport book is found.");
            case "MANIFEST"->List.of("Mill office. Last outward load, November 2009.","Timber bundles tallied beside a note: nine crew places on the return truck. Payroll records eleven signatures.","The figures were never reconciled. A shipping discrepancy is not proof of what happened to anyone.");
            case "FOREMAN"->List.of("Foreman's work journal, November 2009.","Bank the mess fire after supper. Cabins and bunkhouse use their own wood heat; there is no heating beneath these floors.","Leave the family photograph here. We were told the closure would last only a fortnight.");
            case "CREW"->List.of("Bunkhouse notebook, November 2009.","Dry boots beside the stove, never on it. Leave the centre aisle clear. The east window keeps admitting rain around the frame.","Beds and winter coats were left for the next shift. The next page is blank.");
            case "SHELTER"->List.of("Logging cabin journal, February 2025. After Emergence Day.","We lost our home when the dead returned in late 2022. Two of us stayed here three nights, sharing one dry bedroll. We boarded the west window and used the stove.","Water is shut off. Take the bottles next trip. Leave the old office papers alone.");
            case "KITCHEN"->List.of("Kitchen closing list, November 2009.","Drain the cold supply. Empty perishables. The range burns wood; the washhouse oil unit heats wash water only.","Dry tins can stay. Nothing on this list authorizes leaving the fires lit.");
            case "TOOLS"->List.of("Tool issue ledger, November 2009.","Two saws returned for sharpening; handles and gloves retained in the store. Record every borrowed tool before the final inventory.","Dust and old boot scuffs cover the floor. There are no fresh meals or bedding here.");
            case "TALLY"->List.of("Inspection loft tally, November 2009.","Finish the last bundle count before the truck leaves. The loft receives no heat of its own.","Surface checks in the boards were marked for repair. The walking deck and stair opening remain intact.");
            case "SERVICE","LETTERS"->List.of("Caretaker service note, March 2025. Return-route inspection.","Fuel isolated; maintenance inlet supplies lighting. Water and oil heater remain shut off. Recent tins and bedding are in the logging cabin; someone moved the washhouse leak bucket.","A Meridian crew checked the road. Safe passage is not a cleared camp: the Beckenburg Project must remove the threat before families return.");
            case "BOOTS"->List.of("Old crew work boots.","Re-laced leather and ingrained sawdust, left to dry beside the stove when work stopped.");
            default->throw new IllegalArgumentException("Unknown Blackpine record: "+id);
        };
        String title=switch(id){case "PAYROLL"->"Uncollected payroll";case "MANIFEST"->"Final dispatch manifest";case "SHELTER"->"Recent shelter journal";case "SERVICE","LETTERS"->"Caretaker service note";default->id.charAt(0)+id.substring(1).toLowerCase(Locale.ROOT)+" record";};
        return new SiteStories.Document(id,title,lines);
    }
}
