package org.beckenburg;

import java.util.*;

public final class CastleShelfLabelsTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        var items=CastleWarehouse.exhibits();var materials=new HashSet<Integer>();var tiles=new HashSet<String>();
        var map=new CastleHubMap();
        for(var item:items){
            var tile=CastleShelfLabels.tile(item);materials.add(tile.material());
            check(tile.u0()>=0&&tile.v0()>=0&&tile.u1()<=1&&tile.v1()<=1,"Atlas bounds");
            check(tile.u1()>tile.u0()&&tile.v1()>tile.v0(),"Nonempty readable label");
            check(tiles.add(tile.u0()+":"+tile.v0()),"Each true family and tier has a distinct atlas tile");
        }
        long portable=EquipmentRegistry.all().stream()
            .filter(tool->tool.origin()!=EquipmentRegistry.Origin.TRUCK).count();
        check(items.size()==(portable+1)*3&&materials.size()==1,
            "All portable display labels must batch into one shared material");
        check(CastleShelfLabels.HEIGHT>.105*1.7,"Physical plaque is substantially taller");
        var item=items.stream().filter(e->e.rack()==2&&e.column()==1&&e.tier()==EquipmentTiers.Tier.II).findFirst().orElseThrow();
        var rack=CastleWarehouse.racks().get(item.rack());var center=rack.world(0,.326);var eye=rack.world(0,2.3);
        double y=item.y()+.2,eyeY=1.62,heading=Math.atan2(center[0]-eye[0],center[1]-eye[1]);
        double pitch=Math.atan2(y-eyeY,Math.hypot(center[0]-eye[0],center[1]-eye[1]));
        check(CastleWarehouse.aimedExhibit(map,eye[0],eyeY,eye[1],heading,pitch)==item,"Looking at an actual display reveals its full-size family/tier label");
        check(CastleWarehouse.aimedExhibit(map,eye[0],eyeY,eye[1],heading+Math.PI,pitch)==null,"Looking away cannot reveal labels");
        var rear=rack.world(0,-2.3);
        check(CastleWarehouse.aimedExhibit(map,rear[0],eyeY,rear[1],Math.atan2(center[0]-rear[0],center[1]-rear[1]),pitch)==null,"Labels cannot be read through the back of a rack");
        System.out.println("PASS portable equipment atlas labels, one material, larger plaques and targeted readable exhibit titles");
    }
}
