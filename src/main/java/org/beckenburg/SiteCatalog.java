package org.beckenburg;

import java.util.Objects;

/** A location is selectable only when its authored production geometry can be built. */
public final class SiteCatalog {
    private SiteCatalog(){}
    public enum Location {
        HOUSE(MissionSites.title("HOUSE"),true), MANSION(MissionSites.title("MANSION"),false), PRISON(MissionSites.title("PRISON"),false),
        ASYLUM(MissionSites.title("ASYLUM"),false), SCHOOL(MissionSites.title("SCHOOL"),true), FARMHOUSE(MissionSites.title("FARMHOUSE"),true),
        ACADEMY(MissionSites.title("ACADEMY"),false), COLLEGE(MissionSites.title("COLLEGE"),false), CHURCH(MissionSites.title("CHURCH"),false);
        public final String label;
        private final boolean playable;
        private volatile Boolean geometryValidated;
        Location(String label,boolean playable){this.label=label;this.playable=playable;}
        /** Authoring availability is static per build. Do not rebuild a complete
         * 15-room level every time Swing paints a combo-box label. */
        public synchronized boolean playable(){
            if(!playable)return false;
            if(geometryValidated!=null)return geometryValidated;
            try{geometryValidated=LevelPlans.build(HouseMap.Site.valueOf(name())).rooms().size()==15;}
            catch(IllegalArgumentException unavailable){geometryValidated=false;}
            return geometryValidated;
        }
        @Override public String toString(){return label+(playable()?" / PLAYABLE":" / IN PRODUCTION");}
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
