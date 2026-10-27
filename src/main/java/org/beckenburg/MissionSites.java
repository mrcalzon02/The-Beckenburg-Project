package org.beckenburg;
import java.util.*;
/** Lore names and stable image slots; shorthand codes stay internal protocol IDs. */
public final class MissionSites {
    private MissionSites(){}
    public record Entry(String id,String title,String region,String scale,String description,String imageKey,String imageCaption){
        public SiteCatalog.Location location(){try{return SiteCatalog.Location.valueOf(id);}catch(IllegalArgumentException planned){return null;}}
        public boolean headquarters(){return id.equals("CASTLE");}
        public boolean playable(){var site=location();return site!=null&&site.playable();}
        public String status(){return headquarters()?"HEADQUARTERS / NO INVESTIGATION":playable()?"AVAILABLE":"IN DEVELOPMENT / NOT DISPATCHABLE";}
        @Override public String toString(){return title;}
    }
    private static final List<Entry> ALL=List.of(
        new Entry("ACADEMY","St. Alban Academy","Beckenburg district","Medium-large / cloister","Four unequal ranges frame a real open quadrangle. Public teaching stays on the south/east, library on the west, upper dormitories on the north, and sealed archive below.","academy","STATIC BLOCKOUT VIEW"),
        new Entry("ASYLUM","St. Dymphna Hospital","Beckenburg district","Large / stepped wards","A stepped pavilion plan: administration faces a daylight court, with a quiet ward west and wet treatment/service spaces east. This replaces the generic cross-wing layout.","asylum","STATIC BLOCKOUT VIEW"),
        new Entry("CASTLE","Castle Beckenburg","Headquarters","Hub / campaign facility expansion","A complete proposed headquarters plan around a courtyard: briefing and dispatch at the south gate, archive and lodging west, armory/workshop/infirmary east, sanctuary and research north, protected training annex beyond the archive.","castle","STATIC SITE VIEW"),
        new Entry("CHURCH","St. Brigid Parish","Beckenburg district","Established reference","Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.","church","STATIC LAYOUT PLACEHOLDER"),
        new Entry("COLLEGE","Beckenburg College","Beckenburg district","Large / bounded campus cluster","Three distinct campus buildings share a compact green: teaching block west, library north, residence/union east. A service tunnel and upper library bridge create alternate routes.","college","STATIC BLOCKOUT VIEW"),
        new Entry("FARMHOUSE","Hollowmere Farm","Beckenburg district","Established reference","Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.","farmhouse","STATIC SITE VIEW"),
        new Entry("HOUSE","The Briar House","Beckenburg district","Established reference","Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.","house","STATIC SITE VIEW"),
        new Entry("MANSION","Blackthorn Manor","Beckenburg district","Large / estate","An asymmetric service court beside a formal reception wing; grand stair and attic are offset from the kitchen servants route.","mansion","STATIC BLOCKOUT VIEW"),
        new Entry("PRISON","Beckenburg Penitentiary","Beckenburg district","Large / secured compound","Two parallel cell blocks face a bounded exercise yard. Administration forms a front bar and mess/medical spaces close the rear, giving a legible perimeter circuit.","prison","STATIC BLOCKOUT VIEW"),
        new Entry("SCHOOL","Ravenshade School","Beckenburg district","Established reference","Trace of current authored architecture for review. Preserve its measured footprint and floor elevations while planning the full roster.","school","STATIC SITE VIEW"),
        new Entry("WLD-01","Blackpine Logging Camp","Northern Woodlands","Medium-large / dispersed buildings","A horseshoe of working buildings around a log yard, with a separate cabin path and original foundations beyond the sawmill. Distinct roofs and yard landmarks bound the search.","wld-01","STATIC SITE VIEW"),
        new Entry("WLD-02","Camp Hollow Creek","Northern Woodlands","Medium / lakeside campground","Two short woodland trail loops link readable tent clusters, ranger services and lake frontage. The lake is a hard boundary, with no required swimming or boat traversal.","wld-02","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-03","Red Mesa Ancestral Caves","Western Badlands","Medium-large / canyon and caves","A bounded canyon floor rises through a switchback terrace into fictional cliff dwellings and branching caves. Modern expedition traces provide the investigation installations.","wld-03","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-04","Widow's Peak Fire Lookout","Northern Woodlands","Small / vertical outpost","A small hillside station clusters around a steep but bounded path and a tall lookout. Investigations span hut, ruined station and clearing; the tower is an optional risky observation branch.","wld-04","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-05","Dead Man's Claim","Highland Frontier","Medium-large / mine levels","Small surface shacks lead into a two-level mine with a main haul loop and ventilation return. Collapsed lift remains visible scenery, never a required traversal mechanic.","wld-05","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-06","Drowned Fisherman's Wharf","Coastal Territories","Medium / shore and piers","A shore street feeds two parallel working piers joined by a cross-dock. Cottages sit on land, work sheds on piles, and a dry tidal undercroft is reached by two guarded descents.","wld-06","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-07","Saint Bartholomew's Forgotten Cemetery","Northern Woodlands","Small-medium / enclosed grounds","A bounded burial ground is organized by a main avenue and memorial ring. Ruined chapel and caretaker cottage face the approach; two ossuary entrances join underground.","wld-07","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-08","Northwatch Research Station","Northern Expanse","Medium / polar compound","A compact ring of insulated prefabs surrounds a flagged service court. A heated tunnel links lab, dormitory and generator while the instrument array remains an exposed optional branch.","wld-08","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-09","Blackwater Hydroelectric Dam","Highland Frontier","Large / industrial vertical","The roadhead feeds a dam crest and an offset powerhouse. A guarded external stair and internal service stair connect crest, turbine floor and lower galleries without entering the spillway.","wld-09","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-10","Monastery of Ashen Bells","Highland Frontier","Medium-large / remote cloister","A compact enclosed monastery wraps a high garden, with a detached bell stair on the east and catacombs below the north range. Its service path and terraces distinguish it from the urban academy.","wld-10","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-11","Mourning Marsh","Coastal Territories","Medium / stilt settlement","A small settlement straddles a main raised boardwalk and a parallel service boardwalk. Homes, chapel and warehouse are reachable on dry supported decks; flooded terrain is a visible boundary.","wld-11","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-12","Gallows Pass Railway","Highland Frontier","Medium-large / linear railway with return","A bounded railway cutting runs past derailed coaches to a tunnel. A maintenance siding path parallels the track, while a guarded trestle has a service walkway beneath its deck.","wld-12","STATIC BLOCKOUT VIEW"),
        new Entry("WLD-13","Greywater Cannery","Coastal Territories","Medium-large / industrial wilderness","A shore-side processing hall connects cold stores, boiler plant and shipping dock. Workers housing sits along the forest edge, linked by a short service-yard loop.","wld-13","STATIC BLOCKOUT VIEW"));
    public static List<Entry> all(){return ALL;}
    /** Campaign progression is saved by ordinal; browsing order must never reorder it. */
    public static List<Entry> campaign(){
        return List.of("HOUSE","SCHOOL","FARMHOUSE","MANSION","PRISON","ASYLUM","ACADEMY","COLLEGE","CHURCH",
            "WLD-01","WLD-02","WLD-03","WLD-04","WLD-05","WLD-06","WLD-07","WLD-08","WLD-09","WLD-10","WLD-11","WLD-12","WLD-13")
            .stream().map(MissionSites::byId).toList();
    }
    public static Entry byId(String id){return ALL.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown site "+id));}
    public static String title(String id){return byId(id.equals("HOLLOW_CREEK")?"WLD-02":id).title();}
}
