package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.HashSet;

/** Required runtime art, not empty castle dialogue targets or decorative placeholders. */
public final class CastleSupportArtTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        String[] ids={"elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash"};
        var used=new HashSet<String>();
        for(String id:ids){
            check(DirectionalSprites.isCastleSupport(id),"Missing approved Castle resident identity: "+id);
            for(int facing=0;facing<8;facing++){
                BufferedImage img=DirectionalSprites.frame(id,facing);
                check(img.getWidth()==128&&img.getHeight()==224&&img.getColorModel().hasAlpha(),
                    "Missing packaged source cutout for "+id+" facing "+facing);
                int opaque=0;
                for(int y=0;y<img.getHeight();y+=4)
                    for(int x=0;x<img.getWidth();x+=4)
                        if((img.getRGB(x,y)>>>24)>96)opaque++;
                check(opaque>80,"Empty resident sprite: "+id+" facing "+facing);
            }
            used.add(id);
        }
        check(used.size()==4,"All four support characters must have unique loaded artwork");
        check(CastleRelations.Resident.values().length==5,
            "Matthis Rook and four distinct support character interactions");
        var stations=CastleHubMap.Station.values();
        for(var resident:CastleRelations.Resident.values()){
            check(new CastleHubMap().traversable(resident.x,resident.z),
                "Resident sprite must stand on accessible Castle floor: "+resident.name);
            for(var station:stations)check(Math.hypot(station.x-resident.x,
                station.z-resident.z)>.45,
                "Resident must not occupy the workstation itself: "+resident.name);
        }
        check(DirectionalSprites.frame("lore_master",0)!=null,
            "The canonical Matthis Rook sprite remains loaded");
        System.out.println("PASS Castle support character sprites: 16 cutouts, valid placements, five residents");
    }
}
