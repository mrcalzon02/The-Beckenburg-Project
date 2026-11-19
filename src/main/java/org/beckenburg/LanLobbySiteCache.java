package org.beckenburg;

/** The lobby only needs room names and site revision, not a new level every tick. */
final class LanLobbySiteCache {
    private HouseMap.Site site;
    private HouseMap map;
    private int builds;
    HouseMap get(HouseMap.Site wanted){
        if(wanted==null)throw new IllegalArgumentException("Missing lobby site");
        if(map==null||site!=wanted){
            map=new HouseMap(wanted);
            site=wanted;
            builds++;
        }
        return map;
    }
    int builds(){return builds;}
}
