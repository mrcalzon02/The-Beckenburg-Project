package org.beckenburg;

import org.beckenburg.LevelDefinition.*;

/** First-principles House construction pilot. Uses the real runtime authority;
 * no alternate renderer/collision engine and no change to dispatched House maps.
 * Empty graybox only: terrain, fittings, furniture and mission acceptance follow.
 */
public final class HouseRebuild {
    private HouseRebuild(){}
    static final String TERRAIN_SHA="d8089757803fdc2fae41f9fc606a1e99ea75f4da482c01e54cf136bc6da3f499";

    public static HouseMap graybox(){return new HouseMap(HouseMap.Site.HOUSE,source());}

    public static LevelDefinition source(){
        var b=new LevelPlans.B("briar-rebuild","The Briar House — rebuild graybox","residential");
        // One compact ground-floor loop, with a real service route to the garden.
        b.r("Entrance and coat vestibule",18,18.8,21.6,20.5,0,2.8,"plaster",19,19.4);
        b.r("Family sitting room",12,14.7,18,20.5,0,2.8,"parquet",15,17.6);
        b.r("Kitchen and cellar access",12,10,18,14.7,0,2.8,"tile",16,12.4);
        b.r("Family dining room",18,10,21.6,13.6,0,2.8,"parquet",19.8,11.8);
        b.r("Stair and service passage",18,13.6,21.6,18.8,0,2.8,"plaster",18.8,16.5);
        // Each private room has its own landing entrance; no bedroom shortcut.
        b.r("Upper landing and linen storage",18,13.2,21.6,20.5,2.8,2.8,"parquet",18.8,19.2);
        b.r("Parents bedroom and dressing",12,14.7,18,20.5,2.8,2.8,"wallpaper",15,17.6);
        b.r("Child bedroom and study",12,10,18,14.7,2.8,2.8,"wallpaper",15,12.4);
        b.r("Family bathroom",18,10,21.6,13.2,2.8,2.8,"tile",19.8,11.6);
        // Lower service access occupies a separate west-side stair, avoiding
        // superposed up/down flight queries in the main stair passage.
        b.r("Cellar work passage",12,10,15,20.5,-2.4,2.4,"brick",13.5,18.5);
        b.r("Heating hot-water and laundry",15,10,21.6,14.7,-2.4,2.4,"brick",18.3,12.4);
        b.r("Household and seasonal store",15,14.7,21.6,20.5,-2.4,2.4,"concrete",18.3,17.6);
        b.outdoors(6,2,27,20.5,26,39,"Rear garden and patio reservation","Street and entrance approach");

        b.door(0,1,18,0,19.6,1.05,false);
        b.door(1,2,16.4,0,14.7,1.1,true);
        b.door(2,3,18,0,11.8,1.05,false);
        b.door(3,4,18.8,0,13.6,1.05,true);
        b.door(4,0,18.8,0,18.8,1.1,true);
        b.door(1,4,18,0,16.5,1.05,false);
        b.door(5,6,18,2.8,19,1.05,false);
        b.door(5,7,18,2.8,13.95,1.05,false);
        b.door(5,8,19,2.8,13.2,1.05,true);
        b.door(9,10,15,-2.4,12.4,1.05,false);
        b.door(9,11,15,-2.4,18.5,1.05,false);
        b.door(10,11,18.3,-2.4,14.7,1.05,true);
        b.door(0,13,19.8,0,20.5,1.1,true);
        b.door(2,12,16.5,0,10,1.05,true);
        b.flight(4,5,20.5,14,20.5,18.2,0,2.8);
        b.flight(9,2,13.1,10.5,13.1,14.1,-2.4,0);

        b.window(1,12,12,0,17.6,false);
        b.window(1,13,15,0,20.5,true);
        b.window(2,12,15.7,0,10,true);
        b.window(3,12,21.6,0,11.8,false);
        b.window(6,12,12,2.8,17.6,false);
        b.window(6,13,15,2.8,20.5,true);
        b.window(7,12,15,2.8,10,true);
        b.window(8,12,19.8,2.8,10,true);
        b.window(5,13,19.8,2.8,20.5,true);

        for(var room:b.rooms){
            var p=room.spawn();
            b.socket("spawn",room.id(),p.x(),p.y(),p.z(),0,"graybox-standing");
            if(!room.outside()){
                b.socket("light",room.id(),p.x(),room.y()+room.height()-.025,p.z(),0,"utility");
                b.socket("evidence",room.id(),p.x(),room.y()+.03,p.z(),0,"floor-reservation");
                b.installSwitch(room.id());
            }
        }
        // Keep the panel on solid wall north of the service-room doorway.
        b.socket("breaker",10,15.035,-1.05,10.9,Math.PI/2,"electrical-panel");
        b.socket("ritual",1,15,0,17.6,0,"banishment");
        b.socket("ritual",11,18.3,-2.4,17.6,0,"cleansing");
        b.socket("ritual",12,9,0,6,0,"absolution");
        var van=b.rooms.get(14);
        b.socket("world-terrain",12,0,-.65,0,0,"briar-rebuild-world.hcs2#"+TERRAIN_SHA);
        return new LevelDefinition(b.id,"first-principles-graybox-v1",b.title,
            b.rooms,b.portals,b.stairs,b.voids,b.props,b.sockets,b.colliders,
            new Dock(van.cx(),0,van.cz(),-Math.PI/2),b.ambience);
    }
}
