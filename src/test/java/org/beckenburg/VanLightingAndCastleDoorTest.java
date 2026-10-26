package org.beckenburg;
/** Physical sources and hinges, not screen-space brightness shortcuts. */
public final class VanLightingAndCastleDoorTest {
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    public static void main(String[] args){
        for(var site:HouseMap.Site.values()){
            var map=new HouseMap(site);var lighting=new FixtureLighting(map);var circuits=new RoomLighting();circuits.setBreaker(false);lighting.frame(circuits,0);
            check(lighting.sources().stream().filter(s->s.kind()==FixtureLighting.Kind.VAN_DIFFUSER).count()==6,"Six real diffuser samples on three fittings");
            var van=map.van();for(double z:new double[]{-1.5,0,1.4})for(double x:new double[]{-.7,0,.7}){var p=van.world(x,z);double e=lighting.sample(14,p.x(),van.floorY()+1.65,p.z()).energy();check(e>1.05,"Van upper rack/work area must be illuminated on vehicle supply: "+site+" "+e);}
        }
        for(double bottom:new double[]{0,3.4})for(boolean horizontal:new boolean[]{true,false}){
            var slot=new CastleDoorSlots.Slot("test",2,4,horizontal?4.5:2,horizontal?4:6.5,bottom,2.7,false);
            var closed=CastleTimberDoors.leaves(slot,false);var open=CastleTimberDoors.leaves(slot,true);
            for(int i=0;i<2;i++){check(closed.get(i).y()==bottom+.04,"Door fitted to actual storey");check(closed.get(i).x()==open.get(i).x()&&closed.get(i).z()==open.get(i).z(),"Opening retains actual hinge position");}
            double x=slot.centerX(),z=slot.centerZ();check(CastleTimberDoors.blocks(slot,false,x,bottom,z,.18),"Closed leaves block centre");check(!CastleTimberDoors.blocks(slot,true,x,bottom,z,.18),"Open paired leaves clear centre");
            check(CastleTimberDoors.crosses(slot,false,x-(horizontal?0:1),bottom+1,z-(horizontal?1:0),x+(horizontal?0:1),bottom+1,z+(horizontal?1:0)),"Closed fitted leaf casts shadow");
            check(!CastleTimberDoors.crosses(slot,true,x-(horizontal?0:1),bottom+1,z-(horizontal?1:0),x+(horizontal?0:1),bottom+1,z+(horizontal?1:0)),"Open leaves clear central light path");
        }
        System.out.println("PASS all10van layouts: upper racks and work area on own supply; Castle door hinges, storey mounting, closed/open collision and shadows");
    }
}
