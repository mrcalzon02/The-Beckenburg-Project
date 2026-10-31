package org.beckenburg;
import java.util.*;

/** Host-owned physical EMF device. Stable identity is owner seat+1 across redrops.
 * Cross-owner transfers and save migration are not implemented in this first slice.
 */
final class LanDroppedGear {
    record Drop(long id,int owner,int sourceSlot,String tool,EquipmentTiers.Tier tier,
            int room,double x,double y,double z) {
        Drop {
            if(owner<0||owner>=6||id!=owner+1L||sourceSlot<0||sourceSlot>=FieldEquipment.SLOTS
                    ||!"EMF_READER".equals(tool)||tier==null||room<0||room>=HouseMap.INDOOR_ROOMS
                    ||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))
                throw new IllegalArgumentException("invalid dropped gear identity or pose");
        }
        String frame(){
            return "GEARDROP|ITEM|"+id+"|"+owner+"|"+sourceSlot+"|"+tool+"|"+tier+"|"+room
                +"|"+Math.round(x*1000)+"|"+Math.round(y*1000)+"|"+Math.round(z*1000);
        }
        static Drop decode(List<String> p){
            if(p.size()!=11||!p.get(0).equals("GEARDROP")||!p.get(1).equals("ITEM"))
                throw new IllegalArgumentException("invalid dropped gear frame");
            return new Drop(LanProtocol.integer(p.get(2),1,6),
                LanProtocol.integer(p.get(3),0,5),LanProtocol.integer(p.get(4),0,3),
                p.get(5),EquipmentTiers.Tier.valueOf(p.get(6)),
                LanProtocol.integer(p.get(7),0,HouseMap.INDOOR_ROOMS-1),
                LanProtocol.integer(p.get(8),0,99999999)/1000.0,
                LanProtocol.integer(p.get(9),-50000,50000)/1000.0,
                LanProtocol.integer(p.get(10),0,99999999)/1000.0);
        }
    }
    private final Map<Long,Drop> inWorld=new LinkedHashMap<>();
    List<Drop> snapshot(){return List.copyOf(inWorld.values());}
    boolean hasOwner(int owner){return inWorld.containsKey(owner+1L);}
    boolean heldInWorld(int owner,String tool){
        Drop d=inWorld.get(owner+1L);return d!=null&&d.tool().equals(tool);
    }
    Set<String> worldKeys(){
        Set<String> keys=new HashSet<>();
        for(Drop d:inWorld.values())keys.add(d.owner()+":"+d.tool());
        return Set.copyOf(keys);
    }
    Drop drop(FieldEquipment kit,int owner,LanWorld.Body body,HouseMap map){
        if(hasOwner(owner))throw new IllegalArgumentException("instrument already dropped");
        String tool=kit.active();
        if(!"EMF_READER".equals(tool))
            throw new IllegalArgumentException("only EMF reader supports world drop in this slice");
        if(body.room()<0||body.room()>=HouseMap.INDOOR_ROOMS)
            throw new IllegalArgumentException("drop requires a physical indoor floor");
        double angle=Math.toRadians(body.heading());
        double xx=body.x()+Math.cos(angle)*.38,zz=body.z()+Math.sin(angle)*.38;
        if(!map.traversable(xx,body.y(),zz)||map.roomAt(xx,body.y(),zz)!=body.room())
            throw new IllegalArgumentException("no clear floor immediately ahead");
        double floor=map.groundHeight(xx,zz,body.room());
        if(!Double.isFinite(floor)||Math.abs(floor-body.y())>1.0)
            throw new IllegalArgumentException("invalid supporting floor");
        int slot=kit.activeSlot();
        EquipmentTiers.Tier tier=kit.owned(tool);
        if(tier==null)throw new IllegalArgumentException("equipment not owned");
        Drop item=new Drop(owner+1L,owner,slot,tool,tier,body.room(),xx,floor,zz);
        kit.dropActive(tool); // No fallible validation after removing it from the belt.
        inWorld.put(item.id(),item);
        return item;
    }
    Drop pickup(FieldEquipment kit,int owner,long id,LanWorld.Body body,HouseMap map){
        Drop item=inWorld.get(id);
        if(item==null)throw new IllegalArgumentException("dropped equipment not found");
        if(item.owner()!=owner)throw new IllegalArgumentException("another hunter owns this instrument");
        if(item.room()!=body.room()||Math.hypot(item.x()-body.x(),item.z()-body.z())>1.65
                ||Math.abs(item.y()-body.y())>1.0
                ||!map.clearLineOfSight(body.x(),body.y()+1.35,body.z(),
                    item.x(),item.y()+.2,item.z()))
            throw new IllegalArgumentException("dropped equipment not within physical reach");
        kit.restoreDropped(item.tool(),item.sourceSlot());
        inWorld.remove(id);
        return item;
    }
    /** Local aim is advisory; the host repeats ownership, reach and LOS validation. */
    static Drop aimed(Collection<Drop> drops,int owner,HouseMap map,
            double x,double eyeY,double z,double yaw){
        Drop nearest=null;double best=1.65;
        for(Drop item:drops){
            if(item.owner()!=owner)continue;
            double dx=item.x()-x,dz=item.z()-z,d=Math.hypot(dx,dz);
            if(d>=best||d<.001||Math.abs(item.y()-(eyeY-1.62))>1.2)continue;
            if((dx*Math.cos(yaw)+dz*Math.sin(yaw))/d<.48)continue;
            if(!map.clearLineOfSight(x,eyeY,z,item.x(),item.y()+.2,item.z()))continue;
            nearest=item;best=d;
        }
        return nearest;
    }
}
