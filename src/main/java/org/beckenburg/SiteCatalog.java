package org.beckenburg;

import java.util.Objects;

/** A location is selectable only when its authored production geometry can be built. */
public final class SiteCatalog {
    private SiteCatalog(){}
    public enum Location {
        HOUSE(MissionSites.title("HOUSE")), MANSION(MissionSites.title("MANSION")), PRISON(MissionSites.title("PRISON")),
        ASYLUM(MissionSites.title("ASYLUM")), SCHOOL(MissionSites.title("SCHOOL")), FARMHOUSE(MissionSites.title("FARMHOUSE")),
        ACADEMY(MissionSites.title("ACADEMY")), COLLEGE(MissionSites.title("COLLEGE")),
        CHURCH(MissionSites.title("CHURCH")), USER("User authored level"),
        HOLLOW_CREEK(MissionSites.title("HOLLOW_CREEK"),"beckenburg.review.hollowCreek"),
        SEACLIFF(MissionSites.title("SEACLIFF"),SeacliffNativeLevel.REVIEW_PROPERTY),
        BLACKPINE(MissionSites.title("BLACKPINE"),BlackpineLevel.REVIEW_PROPERTY),
        LOOKOUT(MissionSites.title("LOOKOUT"),LookoutNativeLevel.REVIEW_PROPERTY),
        MINE(MissionSites.title("MINE"),MineNativeLevel.REVIEW_PROPERTY),
        WHARF(WharfNativeLevel.TITLE,WharfNativeLevel.REVIEW_PROPERTY),
        CEMETERY(CemeteryNativeLevel.TITLE,CemeteryNativeLevel.REVIEW_PROPERTY),
        MORTUARY(CemeteryNativeLevel.ANNEX_TITLE,CemeteryNativeLevel.REVIEW_PROPERTY);
        public final String label;
        private final String reviewProperty;
        private volatile Boolean geometryValidated;
        Location(String label){this(label,null);}
        Location(String label,String reviewProperty){this.label=label;this.reviewProperty=reviewProperty;}
        /** Authoring availability is static per build. Do not rebuild a complete
         * 15-room level every time Swing paints a combo-box label. */
        public synchronized boolean playable(){
            if(this==USER)return UserLevelLibrary.activeId()!=null;
            if(reviewProperty!=null&&!Boolean.getBoolean(reviewProperty))return false;
            if(geometryValidated!=null)return geometryValidated;
            try{geometryValidated=LevelPlans.build(HouseMap.Site.valueOf(name())).rooms().size()==15;}
            catch(IllegalArgumentException unavailable){geometryValidated=false;}
            return geometryValidated;
        }
        @Override public String toString(){
            return (this==USER&&UserLevelLibrary.activeId()!=null
                ?label+" ("+UserLevelLibrary.activeId()+")":label)
                +(playable()?" / PLAYABLE":" / IN PRODUCTION");
        }
    }
    public enum Conditions {
        STANDARD("Normal weather",1.0,1.0,true),
        BLACKOUT("Power outage",1.0,1.15,false),
        FOGBOUND("Heavy ground fog",.88,2.0,true);
        public final String label;
        private final double light,fog;
        private final boolean powered;
        Conditions(String label,double light,double fog,boolean powered){
            this.label=label;this.light=light;this.fog=fog;this.powered=powered;
        }
        public double lightMultiplier(){return light;}
        public double fogMultiplier(){return fog;}
        public boolean initiallyPowered(){return powered;}
        @Override public String toString(){return label;}
    }
    static Location[] soloLocations(){
        return java.util.Arrays.stream(Location.values())
            .filter(location->location.reviewProperty==null||location.playable())
            .toArray(Location[]::new);
    }
    public static void requirePlayable(Location chosen){
        if(!Objects.requireNonNull(chosen).playable())
            throw new IllegalArgumentException(chosen.label+" has no shipped level geometry");
    }
    /**
     * Multiplayer lists the entire HouseMap.Site authoring roster, which can
     * contain native prototypes before they receive a public contract entry.
     * Never throw during lobby construction or silently advertise an
     * unfinished prototype (for example HOLLOW_CREEK) as an active mission.
     */
    public static boolean playable(HouseMap.Site site){
        Objects.requireNonNull(site,"site");
        try{return Location.valueOf(site.name()).playable();}
        catch(IllegalArgumentException notYetCatalogued){return false;}
    }
    public static HouseMap.Site mapFor(Location chosen){
        requirePlayable(chosen);
        return HouseMap.Site.valueOf(chosen.name());
    }
}
