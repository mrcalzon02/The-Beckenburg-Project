package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Room-by-room material history for the first three reclamation sites. */
public final class LegacySiteStories {
    private LegacySiteStories(){}
    public record RoomStory(String use,String heat,String trace,String account){}
    private static RoomStory r(String use,String heat,String trace,String account){return new RoomStory(use,heat,trace,account);}
    public static List<RoomStory> rooms(String site){return switch(site){
        case "house" -> List.of(
            r("Vale family arrivals","Water radiator","Coat rail, rubbed floor and family papers","A departure list says six neighbours joined the household in late 2022. It records who left together, not who died."),
            r("Family sitting room","Water radiator","Photographs, old correspondence and interrupted mending","E. Vale's older letters lie beside the newer shelter card. The family home became a shared refuge on Emergence Day."),
            r("Daily cooking","Radiator; cooking appliance is not room heating","Used utensils, pantry stores and a ration notebook","The last kitchen rota split the remaining tins between the cellar group and neighbours waiting for transport."),
            r("Family meals","Water radiator","Writing materials beside inherited tableware","A later Concord collection list asks that the Vales' photographs remain in the house when spare linen is taken to a refuge."),
            r("Ground-floor circulation","Water radiator","Repeated foot scuffs toward the cellar stair","The hall was the route between the front door and the shelter. Keep that route open during reclamation."),
            r("Upper landing","Water radiator","Linen left ready for carrying downstairs","A bag-packing note separates medicine, dry socks and documents from possessions the evacuees could not carry."),
            r("Main bedroom","Water radiator","Repaired clothing and inherited mending tin","The tin predates Emergence Day. A newer note asks a return crew to save the family letters, without promising that their owners can return yet."),
            r("Child's bedroom","Water radiator","School things, an unfinished notebook and folded clothing","The notebook stops at an ordinary homework exercise. The evacuation card gives no reliable later fate for its owner."),
            r("Family bathroom","Water radiator; stored domestic hot water","Older fittings, toiletries and later plumbing repairs","The 1983 invoice lists renewed supply and waste pipes. Nothing in the room indicates radiant floor heating."),
            r("Cellar stair and laundry approach","Incidental warmth from plant only","Wear at the stair foot and refuge instructions","Neighbours agreed to leave the stair clear even when bedding filled the storage room. The note points upstairs for evacuation."),
            r("Boiler service","Boiler supplies radiators; no habitable heated floor","Maintenance papers and low damp","The later return sheet records an electrical inspection request. It does not certify a hot boiler, safe fuel supply or restored water."),
            r("Storage and temporary refuge","Unheated cellar storage","Emergency bedroll, used ration tins and damp lower wall","A dated shelter inventory records borrowed bedding. The Lantern Concord collected usable spares after evacuation, leaving one damaged roll and a copied headcount."));
        case "farmhouse" -> List.of(
            r("Farm arrivals and boot removal","House radiator","Peg rail, muddy scuffs and work clothes","Road refugees were asked to leave wet outer layers here before entering the shared sleeping rooms."),
            r("Family parlour","House radiator","Old farm correspondence beside relief records","The family took in stranded travellers during Emergence Day. A later Concord rota distinguishes resident growers from temporary guests."),
            r("Farm kitchen and ration point","House radiator; cooking heat incidental","Used cooking equipment, seed and ration ledger","Seed for planting was listed separately from food to be eaten. The growers hoped a safe farm would feed more than the current shelter."),
            r("Household and relief meals","House radiator","Meal allocation notes and repaired tableware","A ration note gives portions by headcount, with extra for those working the water and wash rota. It makes no claim that the surrounding lane was safe."),
            r("House circulation","House radiator","Floor wear between yard and stair","A later Meridian correction cancels an earlier lane clearance. Survey marks outside the farm are dated observations, not permanent protection."),
            r("Landing washroom and linen","House radiator; domestic hot water","Clean linen, wash supplies and a carrying list","The landing washroom was a later improvement. Refuge volunteers carried laundry out to the shed and kept washing separate from seed storage."),
            r("Parents' bedroom","House radiator","Inherited sewing supplies and folded garments","The family surrendered spare bedding but kept the old mending tin. Repairs allowed boots and clothes to survive several more winters."),
            r("Child's bedroom and allocated sleeping space","House radiator","School notebook beneath a later bed list","A relief list assigns places by household. The original pupil's work and later guests belong to different periods of the room's history."),
            r("Hayloft storage","Unheated agricultural volume","Dry goods, worn boards and an inventory","The loft was not a heated dormitory. The surviving allocation list directs overnight guests into the house rather than promising shelter among the hay."),
            r("Root cellar and reserve food","Unheated below-ground storage","Ration remnants, damp and stock-taking papers","A storekeeper crossed out spoiled food and protected seed stock. The corrections show rationing over time, not a single untouched meal."),
            r("Barn work and covered loading","Unheated agricultural volume","Repair records, boot scuffs and saved materials","The barn provided covered work while refugees occupied the house. Exit and loft approaches were kept clear for moving supplies."),
            r("Tool repair and shed laundry","Unheated; washer heats its own cycle","Boots, wash clothes, tool notes and recent wear","A later wash rota records shared water and an isolated electrical fault. The shed was useful without being a warm sleeping room."));
        case "school" -> List.of(
            r("Front office, then reception point","Water radiator","School service papers beneath an evacuation register","During Emergence Day volunteers wrote arrivals, reunions and departure parties here. Missing entries remain unresolved names."),
            r("Main circulation and queue","Radiators from school plant","Wear between reception and classrooms","A copied route card asks volunteers to keep the corridor open for stretchers and families moving together."),
            r("Classroom A, then family waiting room","Water radiator","Lessons beneath emergency allocation notes","The desk work predates late 2022. Later lists group evacuees by family rather than identifying the current haunting."),
            r("Science teaching room","Water radiator","Coursework, pens and a later isolation checklist","Volunteers shut teaching stores and isolated supplies before using nearby rooms for relief. The checklist is practical work, not an occult diagram."),
            r("School meals, then relief kitchen","Water radiator; kitchen heat incidental","Meal records, used utensils and ration remnants","Meal counts rose when transport stopped. A later relief note says to boil only water from checked supplies."),
            r("Nurse office and aid point","Water radiator","Care notebook and repairable personal belongings","Names in a treatment register are not death certificates. A copied departure list asks the next refuge to continue care."),
            r("Sport hall and temporary assembly space","No added portable heating; high hall loses heat quickly","Bag allocation note, wall wear and emergency bedding","A volunteer report describes a short stay while departure groups formed. It does not present the gym as a permanently safe ward."),
            r("Upper circulation and washrooms","Water radiators","Locker wear and a later route correction","Meridian volunteers checked a copied route after the first evacuation. Their correction withdrew an unchecked section rather than hiding uncertainty."),
            r("School library and record sorting","Water radiator","Saved books, pencils and a copied names ledger","Survivors sorted copied registers among the teaching books. The Common Archive wants corrections preserved alongside the first reports."),
            r("Classroom B and volunteer workroom","Water radiator","Pupils' work beneath volunteer lists","A rota separates pre-Emergence lessons from later relief work. Reused paper and saved pencils show a shortage of ordinary supplies."),
            r("Assembly room and departure briefing","Existing building heating only","Headcount instructions and floor wear","The final instruction was to count living people at both ends of a journey. No blank in the register is permission to presume a death."),
            r("Heating and electrical service","Modern boiler feeds radiators; old coal and oil plant isolated","Service notes, low damp and maintenance wear","Coal plant from 1894 and later oil equipment remain out of service. The modern system carries the load when supplied. Isolation notes reserve clear working aisles; there is no underfloor heating."));
        default -> List.of();
    };}
    public static SiteStories.Document document(String site,int room){
        var r=rooms(site).get(room);return new SiteStories.Document("ROOM_"+room,SiteStories.identity(site).title()+" / "+r.use(),
            List.of("Common Archive / building and survivor record",r.account(),"HEAT AND SERVICES: "+r.heat()+".","PHYSICAL TRACES: "+r.trace()+".","Historical testimony requires field verification. This record does not identify the current entity."));
    }
    static void populate(LevelPlans.B b){
        var stories=rooms(b.id);if(stories.isEmpty())return;
        for(int i=0;i<stories.size();i++){
            var r=b.rooms.get(i);var story=stories.get(i);
            b.socket("site_history",i,r.x0()+.1,r.y(),r.z0()+.1,0,story.use()+" | "+story.heat()+" | "+story.trace()+" | "+story.account());
            wall(b,"floor_wear",i,false); // shallow surface wear carries no navigation obstruction
            if(i==11||b.id.equals("farmhouse")&&i==9||b.id.equals("house")&&i==10)wall(b,"damp_wall",i,true);
            else if(i%3==0)wall(b,"wall_damage",i,true);
            if(i==0||i==6&&!b.id.equals("school"))wall(b,"coat",i,true);
            if(i==2||i==4&&b.id.equals("school"))wall(b,"calendar",i,true);
            record(b,i);
        }
        // Recent occupation is concentrated where the room account actually places it.
        int shelter=b.id.equals("house")?11:b.id.equals("school")?6:-1;
        if(shelter>=0)floor(b,"bedroll",shelter,.34,.94,.20);
        floor(b,"food_trash",b.id.equals("house")?11:b.id.equals("farmhouse")?9:4,.30,.30,.22);
        if(!b.id.equals("school"))floor(b,"clothes",7,.34,.30,.22);
    }
    private static void record(LevelPlans.B b,int room){
        var support=b.props.stream().filter(p->p.room()==room&&Set.of("level_lived_sideboard","dinner_table_1","bedside_table_1").contains(p.mesh())
            && !(p.mesh().equals("dinner_table_1")&&b.props.stream().anyMatch(q->q.room()==room&&q.mesh().equals("level_lived_kitchenware")&&Math.hypot(q.x()-p.x(),q.z()-p.z())<.5))
            && b.sockets.stream().noneMatch(s->s.type().equals("story")&&s.room()==room&&Math.hypot(s.x()-p.x(),s.z()-p.z())<1)).findFirst().orElse(null);
        if(support==null)return;
        double top=support.mesh().equals("level_lived_sideboard")?.85:support.mesh().equals("dinner_table_1")?.864:.644;
        // Replace the old generic notebook/desk-clutter group on this surface with an authored ledger and its pen/pencil.
        b.props.removeIf(p->p.room()==room&&p.support().equals(support.mesh())&&Set.of("level_lived_desk_clutter","notebook_1","book_mp_3").contains(p.mesh())&&Math.hypot(p.x()-support.x(),p.z()-support.z())<.8);
        double depth=support.mesh().equals("level_lived_sideboard")?.15:0,scale=support.mesh().equals("dinner_table_1")?.85:.65;
        double side=support.mesh().equals("bedside_table_1")?-.20:0;
        double x=support.x()+Math.cos(support.yaw())*side-Math.sin(support.yaw())*depth,z=support.z()+Math.sin(support.yaw())*side+Math.cos(support.yaw())*depth;
        String mesh=support.mesh().equals("bedside_table_1")?"level_trace_journal":"level_trace_records";
        b.props.add(new Prop(mesh,room,x,support.y()+top,z,scale,support.yaw(),0,0,.05,support.mesh()));
        b.socket("story",room,x,support.y()+top,z,support.yaw(),"ROOM_"+room);
    }
    private static void floor(LevelPlans.B b,String mesh,int room,double hx,double hz,double height){
        var r=b.rooms.get(room);
        for(double z=r.z0()+hz+.35;z<r.z1()-hz-.35;z+=.4)for(double x=r.x0()+hx+.35;x<r.x1()-hx-.35;x+=.4){
            var p=new Prop("level_trace_"+mesh,room,x,r.y()+.003,z,1,0,hx,hz,height,"floor");
            final double xx=x,zz=z;
            boolean hiding=b.sockets.stream().anyMatch(s->s.type().equals("hide")&&s.room()==room&&Math.hypot(s.x()-xx,s.z()-zz)<1.9);
            if(!hiding&&b.circulation(p,x,z)&&!b.overlap(p,b.props)){b.props.add(p);return;}
        }
        throw new IllegalStateException("No clear story position for "+b.id+" "+mesh);
    }
    private static void wall(LevelPlans.B b,String mesh,int room,boolean vertical){
        var r=b.rooms.get(room);
        for(int wall=0;wall<4;wall++)for(double t=.9;t<(wall%2==0?r.x1()-r.x0():r.z1()-r.z0())-.9;t+=.65){
            double inset=vertical?.035:.65;
            double x=wall==0||wall==2?r.x0()+t:wall==1?r.x1()-inset:r.x0()+inset;
            double z=wall==1||wall==3?r.z0()+t:wall==0?r.z0()+inset:r.z1()-inset;
            double yaw=new double[]{0,Math.PI/2,Math.PI,-Math.PI/2}[wall];
            if(b.portals.stream().anyMatch(p->(p.a()==room||p.b()==room)&&Math.abs(p.y()-r.y())<.1&&Math.hypot(p.x()-x,p.z()-z)<p.width()/2+.9))continue;
            if(b.sockets.stream().anyMatch(s->s.room()==room&&Set.of("hide","switch","device","story").contains(s.type())&&Math.hypot(s.x()-x,s.z()-z)<1.05))continue;
            // Avoid covering furniture, pipes, existing boards or other new wall details.
            if(b.props.stream().anyMatch(p->p.room()==room&&Math.hypot(p.x()-x,p.z()-z)<.85))continue;
            b.props.add(new Prop("level_trace_"+mesh,room,x,r.y()+.012,z,vertical?.7:.6,yaw,0,0,0,vertical?"wall":"floor-surface"));return;
        }
    }
}
