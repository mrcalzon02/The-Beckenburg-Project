package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Daily household functions identified by actual complete-room review; source-preserving fit. */
public final class HouseRebuildFurnishings {
    private HouseRebuildFurnishings(){}
    public static HouseMap map(){return new HouseMap(HouseMap.Site.HOUSE,source());}
    public static LevelDefinition source(){
        var fit=new HouseRebuildFunctions.Fit(HouseRebuildServices.source());
        fit.floor("vestibule-boot-storage","level_work_boot_rack",0,20.65,19.125,0);
        // Original peg rail back touches the east wall; additive hanger closes the source gap.
        fit.place("vestibule-coat-hooks","level_house_coat_hung",0,21.42875,.715,19.9,Math.PI/2,"east-finished-wall");
        fit.floor("kitchen-cold-storage","level_house_fridge_worn",2,17.5,10.5075,0);
        var terrain=WorldTerrainSurface.of(fit.base);double benchY=Double.NEGATIVE_INFINITY;
        for(double dx:new double[]{-.685,-.595,.595,.685})for(double dz:new double[]{-.235,-.145,.145,.235})benchY=Math.max(benchY,HouseRebuildLandscape.height(terrain,13.25+dx,8.5+dz));
        fit.place("garden-sitting-bench","level_camp_bench",12,13.25,benchY,8.5,0,"actual-editor-terrain");
        fit.floor("garden-pot-west","planter_pot_mp_1_full",12,12.05,8.5,0);
        fit.floor("garden-pot-east","planter_pot_mp_1_full",12,14.45,8.5,0);
        var base=fit.finish().level();
        return new LevelDefinition(base.id(),"first-principles-functional-furnishings-v1",base.title().replace("heating construction","household furnishing construction"),base.rooms(),base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),base.colliders(),base.dock(),base.ambience(),base.water(),base.sounds());
    }
}
