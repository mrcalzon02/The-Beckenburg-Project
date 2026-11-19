package org.beckenburg;

/** Lobby STATE and TEAMMAP must reuse cached site metadata, not rebuild each tick. */
public final class LanLobbySiteCacheTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var cache=new LanLobbySiteCache();
        var first=cache.get(HouseMap.Site.HOUSE);
        check(cache.get(HouseMap.Site.HOUSE)==first,"unchanged site should reuse map");
        check(first.site()==HouseMap.Site.HOUSE&&cache.builds()==1,"one house build");
        var second=cache.get(HouseMap.Site.FARMHOUSE);
        check(second!=first&&second.site()==HouseMap.Site.FARMHOUSE,
            "a genuine site switch must invalidate cached metadata");
        check(cache.get(HouseMap.Site.FARMHOUSE)==second&&cache.builds()==2,
            "repeated team packets must not build new maps");
        try{cache.get(null);throw new AssertionError("accepted null site");}
        catch(IllegalArgumentException expected){}
        System.out.println("PASS: read-only lobby site cache, site transition and metadata reuse");
    }
}
