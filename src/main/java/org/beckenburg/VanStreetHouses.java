package org.beckenburg;

import java.util.List;

/**
 * Authored roadside residential parcels for the reusable VAN approach.
 * These are background buildings with real façades and roofs, not a wall
 * enclosing the play field or randomly scattered cube props.
 * Local X is across the road, local Z is down the street.
 */
public final class VanStreetHouses {
    private VanStreetHouses(){}
    public record House(double outerX,double frontageZ,double depth,
                        double frontage,double wallHeight,double roofRise,
                        int masonry,int roof,String character) {
        public House {
            if(!Double.isFinite(outerX+frontageZ+depth+frontage+wallHeight+roofRise)
                ||depth<3||frontage<3||wallHeight<2.5||roofRise<.3)
                throw new IllegalArgumentException("Invalid street residence");
        }
        public int side(){return outerX<0?-1:1;}
        public double streetFace(){return side()*(VanArrivalStreet.ROAD_HALF_WIDTH+3.25);}
        public double buildingStartX(){return side()>0?streetFace():streetFace()-depth;}
        public double buildingEndX(){return side()>0?streetFace()+depth:streetFace();}
        public double frontageStartZ(){return frontageZ-frontage*.5;}
        public double frontageEndZ(){return frontageZ+frontage*.5;}
        public boolean intrudesOnStreet(){
            return buildingStartX()<VanArrivalStreet.ROAD_HALF_WIDTH+.08&&
                buildingEndX()>-VanArrivalStreet.ROAD_HALF_WIDTH-.08;
        }
    }
    public static final List<House> HOMES=List.of(
        // Alternating detached brick/wood façades and a stone townhouse at the
        // far end prevent the approach from looking like an infinite grey wall.
        new House(-1,-7.20,3.65,4.8,3.95,1.04,
            MaterialAtlas.STONE,MaterialAtlas.AGED_WOOD,"west residence"),
        new House(1,-7.30,3.80,4.85,4.24,1.21,
            MaterialAtlas.AGED_WOOD,MaterialAtlas.STONE,"east residence"),
        new House(-1,-13.00,4.0,4.75,4.48,.92,
            MaterialAtlas.AGED_WOOD,MaterialAtlas.STONE,"west workshop"),
        new House(1,-13.28,3.75,4.85,3.77,1.23,
            MaterialAtlas.STONE,MaterialAtlas.AGED_WOOD,"east terrace"),
        new House(-1,-18.62,4.2,4.6,4.20,1.15,
            MaterialAtlas.STONE,MaterialAtlas.STONE,"west corner home"),
        new House(1,-18.69,3.8,4.6,4.42,.92,
            MaterialAtlas.PARQUET,MaterialAtlas.AGED_WOOD,"east corner home")
    );
    /** A site's authored frontage reservation moves context without moving the vehicle or road. */
    public static List<House> homes(LevelDefinition level){return homes(level.sockets());}
    static List<House> homes(List<LevelDefinition.Socket> sockets){
        var reservation=sockets.stream().filter(s->s.type().equals("arrival-context")).findFirst().orElse(null);
        if(reservation==null)return HOMES;
        var parts=reservation.style().split(":");
        if(parts.length!=2||!parts[0].equals("house-side-setback-v1"))throw new IllegalArgumentException("Unknown arrival context reservation");
        double setback=Double.parseDouble(parts[1]);
        if(!Double.isFinite(setback)||setback<0||setback>30)throw new IllegalArgumentException("Invalid arrival context setback");
        return HOMES.stream().map(h->h.side()>0?new House(h.outerX(),h.frontageZ()-setback,h.depth(),h.frontage(),h.wallHeight(),h.roofRise(),h.masonry(),h.roof(),h.character()):h).toList();
    }
    /** A real terminus façade, behind the 16-metre playable roadway.
     * It forms a street closure, but doesn't masquerade as a navigation wall.
     */
    public record EndBuilding(double centreZ,double width,double depth,double height){}
    public static final EndBuilding TERMINUS=new EndBuilding(
        VanArrivalStreet.ROAD_END-4.6,11.2,3.9,5.4);
    static{
        for(House house:HOMES)
            if(house.intrudesOnStreet())
                throw new ExceptionInInitializerError("House is inside road: "+house.character());
    }
}
