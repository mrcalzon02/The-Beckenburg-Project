package org.beckenburg;
import java.util.*;

/** Shared fictional history. A record of survival and reclamation, never a live ghost/evidence oracle. */
public final class WorldLore {
    private WorldLore(){}
    public static final String EMERGENCE="Late 2022";
    public static final String KEEPER="Matthis Rook";
    public static final String PORTRAIT="level-textures/lorekeeper-matthis-rook.png";
    public static final String MASTER_ART_STATUS="Approved in-game source artwork: lorekeeper-matthis-rook.png is the master for the castle resident and archive portrait; separate directional turnarounds and animations remain unverified.";
    public record Faction(InvestigationSession.Role role,String name,String origin,String purpose){}
    public static final List<Faction> FACTIONS=List.of(
        new Faction(InvestigationSession.Role.CLERIC,"The Lantern Concord","Clergy, lay wardens and refuge keepers who protected evacuation shelters after Emergence Day.","Keep living people alive; hold thresholds, tend the harmed and lead cleansing rites."),
        new Faction(InvestigationSession.Role.OBSERVER,"The Meridian Survey","Surviving surveyors, engineers, emergency radio crews and civilian researchers who mapped routes through the lost districts.","Measure the danger, preserve reliable testimony and prove that a road or building can be reclaimed."),
        new Faction(InvestigationSession.Role.DEMONOLOGIST,"The Ashen Collegium","Occult scholars and survivors of failed bargains who pooled dangerous knowledge after the old institutions collapsed.","Find and sever supernatural claims on the living; controlled contact is a means to removal, never worship.")
    );
    public static Faction faction(InvestigationSession.Role role){return FACTIONS.stream().filter(f->f.role()==role).findFirst().orElseThrow();}
    public record SiteThread(String id,String title,String emergence,String survival,String trace){}
    public static final List<SiteThread> SITES=List.of(
        new SiteThread("house","The Briar House","A family shelter closed its doors during the first attacks.","Neighbours shared a cellar refuge before evacuation.","A late-2022 shelter card tucked into older family correspondence."),
        new SiteThread("farmhouse","Hollowmere Farm","Road refugees reached the farm as transport failed.","Growers and Lantern wardens kept a ration and wash rota.","A seed ledger, bed-allocation list and later safe-route correction."),
        new SiteThread("school","Ravenshade School","Classrooms became an emergency reception point.","Volunteer crews kept names and departure routes as halls were lost.","An evacuation register with a later Meridian verification slip."),
        new SiteThread("mansion","Blackthorn Manor","Private gates shut while the district sought shelter.","Survivors later reopened one service wing as a shelter.","Contradictory gate logs, communal cooking and repaired locks."),
        new SiteThread("prison","Beckenburg Penitentiary","Staff and prisoners faced the same supernatural assault.","An improvised evacuation crossed former security boundaries.","A joint headcount and keys tied to an evacuation route, without treating prisoners as monsters."),
        new SiteThread("asylum","St. Dymphna Hospital","Care and transport failed during Emergence Day.","Patients and staff built a protected ward before evacuating.","A continuity-of-care list, mobility aids and a later rescue annotation; illness is never the haunting's cause."),
        new SiteThread("academy","St. Alban Academy","The academy's old assumptions failed in the first attacks.","Researchers joined the Ashen Collegium to test and unbind dangerous claims.","Crossed-out theories, a failed seal and a later safe procedure."),
        new SiteThread("college","Beckenburg College","Student buildings became temporary aid stations.","Engineering and radio volunteers helped form Meridian field crews.","A hand-built route board and a repaired receiver beside older coursework."),
        new SiteThread("church","St. Brigid Parish","The church sheltered people whose beliefs did not protect them from the attacks.","Refuge keepers helped establish the Lantern Concord.","A shared shelter register and a repaired ward threshold; belief alone never guarantees safety."),
        new SiteThread("blackpine","Blackpine Logging Camp","The camp was already closed when the supernatural returned in late 2022.","Displaced survivors used one logging cabin in 2025; a caretaker later recorded their traces.","Pre-Emergence payroll discrepancy, later shelter journal and maintenance sheet are separate layers."),
        new SiteThread("wld-02","Camp Hollow Creek","Holiday shelters became a stranded convoy's refuge.","Survivors moved between shore and washhouse as routes failed.","Camp maps overwritten with refuge counts, repaired cooking gear and later route markers."),
        new SiteThread("wld-03","Red Mesa Ancestral Caves","Modern travellers were stranded by the wider catastrophe.","Rescue teams followed local guidance and documented safe passages.","Modern emergency ropes and dated survey tags, explicitly separate from ancestral markings or cultural blame."),
        new SiteThread("wld-04","Widow's Peak Fire Lookout","The lookout relayed reports as the forests and roads became unsafe.","A radio keeper maintained a signal for travelling survivors.","Weather records interrupted by an emergency frequency list and later Meridian bearings."),
        new SiteThread("wld-05","Dead Man's Claim","Dormant workings were unsafe again after Emergence Day.","A rescue party attempted a measured return, leaving checked retreat markers.","Old assay/payroll dates and new rescue tallies; links to Blackpine remain provisional."),
        new SiteThread("wld-06","Drowned Fisherman's Wharf","Boats carried refugees when land routes broke.","Crews rationed fuel and repeated rescue crossings.","Passenger lists, patched moorings and a later warning against an unsafe berth."),
        new SiteThread("wld-07","Saint Bartholomew's Forgotten Cemetery","The dead returned to a place already used for remembrance.","Survivors recorded missing people without assuming every missing person was dead.","A memorial ledger, temporary ward posts and named rescue searches."),
        new SiteThread("wld-08","Northwatch Research Station","The remote station lost contact during the first attacks.","A skeleton crew rationed heat and attempted to re-establish a safe link.","Fuel and radio logs distinguish equipment failure from reported supernatural events."),
        new SiteThread("wld-09","Blackwater Hydroelectric Dam","Power loss and supernatural danger disrupted evacuation.","Maintenance crews risked limited returns to restore civilian supply.","Isolation tags, rationed work lamps and a later restart checklist."),
        new SiteThread("wld-10","Monastery of Ashen Bells","A refuge bell sounded across the first lost settlements.","Shelter keepers disagreed over sealing the doors or admitting more people.","A disputed admissions ledger and a later Concord relief list; no automatic religious guilt."),
        new SiteThread("wld-11","Mourning Marsh","Flooded routes divided fleeing households.","Survivors maintained raised trails and passed refuge markers onward.","Boot repairs, water-level marks and a later rescue line above older fishing traces."),
        new SiteThread("wld-12","Gallows Pass Railway","Emergency trains carried people away from the first attacks.","Rail crews kept a vulnerable evacuation and supply corridor open.","A departure manifest, improvised signals and later track-clearance stamps."),
        new SiteThread("wld-13","Greywater Cannery","Workers opened stores to a hungry district during the collapse.","Survivors rationed food while attempting to restore the coastal supply chain.","Production labels beneath relief tallies and a later reclamation inspection."),
        new SiteThread("castle","Castle Beckenburg","The castle became a defensible refuge during the collapse.","Survivors of three major factions made it a shared reclamation headquarters.","The Common Archive keeps testimony, corrections, missing names and records of places made safe.")
    );
    public static SiteThread site(String id){return SITES.stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    public record Page(String id,String title,List<String> paragraphs){}
    public static final List<Page> PAGES=pages();
    private static List<Page> pages(){
        var p=new ArrayList<Page>();
        p.add(new Page("keeper","Matthis Rook / Keeper of the Common Archive",List.of(
            "I kept parish and council records before I kept the names of the missing. The castle took me in when there was nowhere left to carry the boxes.",
            "We do not clear a house to add a monster to a book. We clear it so a family can sleep there again. The Beckenburg Project exists to make ordinary life possible.",
            "Ask what a record can prove, who wrote it, and what they could not see. I am a witness and an archivist, not an oracle.")));
        p.add(new Page("emergence","Emergence Day / late 2022",List.of(
            "Supernatural forces that had lain dormant re-emerged in late 2022. Survivors call the catastrophe Emergence Day. The dead and other hostile forces took violent, bloody retribution upon the living.",
            "Homes, roads and places of refuge were lost. Conflicting local accounts record the collapse; the archive does not invent a single precise date, a proven cause or a shared motive for every entity.",
            "What is certain is the human cost. Missing lists, evacuation marks and interrupted work survive in the places we return to.")));
        p.add(new Page("reclamation","The long return / the Beckenburg Project",List.of(
            "Over many grueling years, humanity has fought to regain its place in a world occupied by the dead. Refuge networks became supply routes; supply routes became the first reclaimed districts.",
            "Castle Beckenburg holds a coalition whose purpose is to remove supernatural threats and make places safe for normal people. Naming a ghost is useful only when that knowledge helps us survive, sever its hold and finish the work.",
            "A return inspection matters as much as a victory report. The living must be able to use the place afterward.")));
        p.add(new Page("matthis","A life carried in record boxes",List.of(
            "Matthis Rook was already an elderly municipal archivist when Emergence Day came. He helped neighbours carry copied registers through a failed evacuation; the originals and several people were lost.",
            "His daughter Mara is listed as missing. He refuses to turn uncertainty into a death certificate. A hand tremor, repaired spectacles and a walking cane make patient work necessary.",
            "He maintains the Common Archive in the castle's east records wing. Factions supply testimony and shelter staff help him catalogue it. His counsel, records and portrait are available here; he is not a recruitable field ally.")));
        for(var f:FACTIONS)p.add(new Page("faction-"+f.role().name().toLowerCase(Locale.ROOT),f.name()+" / "+f.role().name(),List.of(f.origin(),f.purpose(),
            "This playable discipline represents a major faction inside the coalition. Faction disagreements do not prevent cooperation or lock ordinary survival tasks to one class.")));
        p.add(new Page("records","Reading a reclaimed place",List.of(
            "First distinguish what was here before late 2022, what happened during Emergence Day, and what survivors changed in the years afterward. A closed mill can contain new bedding; an old school can contain a later evacuation register.",
            "Look for heat, water, light, food, repairs and a way out. These tell us how people stayed alive. A document is testimony, not automatic proof of the current entity or a completed mission.",
            "The site ledger follows. Several locations remain proposals; their archive notes describe planned story threads rather than completed expeditions.")));
        for(int start=0;start<SITES.size();start+=4){
            var lines=new ArrayList<String>();for(var s:SITES.subList(start,Math.min(SITES.size(),start+4)))lines.add(s.title()+": "+s.emergence()+" "+s.survival());
            p.add(new Page("sites-"+(start/4+1),"Site threads / "+(start/4+1),lines));
        }
        return List.copyOf(p);
    }
    public static SiteStories.Document fieldRecord(String site){
        var s=site(site);return new SiteStories.Document("EMERGENCE",s.title()+" / survivor record",List.of(
            "Common Archive copy. Emergence Day: late 2022.",s.emergence()+" "+s.survival(),s.trace()+" This account does not identify the current haunting."));
    }
}
