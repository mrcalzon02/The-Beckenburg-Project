package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Proves two independently complete, canonical Twins sprite actors and packets. */
public final class TwinBodyVisualTest {
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static long fingerprint(BufferedImage image){
        long v=1469598103934665603L;
        for(int y=0;y<image.getHeight();y+=3)
            for(int x=0;x<image.getWidth();x+=3)
                v=(v^image.getRGB(x,y))*1099511628211L;
        return v;
    }
    public static void main(String[] args){
        Set<Long> facesA=new HashSet<>(),facesB=new HashSet<>();
        for(int facing:new int[]{0,2,4,5}){
            var a=DirectionalSprites.frame("twins_a",facing);
            var b=DirectionalSprites.frame("twins_b",facing);
            check(a.getWidth()==128&&a.getHeight()==224
                  &&b.getWidth()==128&&b.getHeight()==224,"Both twins must be independent native frames");
            check(a.getColorModel().hasAlpha()&&b.getColorModel().hasAlpha(),
                "Independent transparent body contours");
            check(fingerprint(a)!=fingerprint(b),"Twins were incorrectly flattened into the same sprite");
            check(DirectionalSprites.frame("ancient_twins_a",facing)==a,
                "Ancient member A must reuse its proper parent, not a third person");
            check(DirectionalSprites.frame("ancient_twins_b",facing)==b,
                "Ancient member B must reuse its proper parent, not a third person");
            facesA.add(fingerprint(a));facesB.add(fingerprint(b));
        }
        check(facesA.size()==4&&facesB.size()==4,"Four distinct canonical direction crops for both members");
        HouseMap map=new HouseMap();
        var spawn=map.spawn(4);
        var primary=new LanGhostSpatial.Pose(4,spawn.x(),spawn.y(),spawn.z(),180);
        List<TwinGhostPair.Member> actors=TwinGhostPair.position(map,primary,60);
        check(actors.size()==2&&actors.get(0).id().equals("a")
            &&actors.get(1).id().equals("b"),"One identity must have TWO bodies");
        check(actors.get(0).pose().equals(primary),"Primary body location must remain host-controlled");
        var echo=actors.get(1).pose();
        check(Math.hypot(primary.x()-echo.x(),primary.z()-echo.z())<=3.0,
            "The sibling must remain bounded to the same spectral pair");
        check(map.traversable(echo.x(),echo.y(),echo.z()),"Companion must not teleport into geometry");
        var bodyB=actors.get(1);
        String visible=TwinGhostPair.wire(bodyB,60,true,false);
        String hidden=TwinGhostPair.wire(bodyB,61,false,false);
        check(visible.startsWith("TWIN|b|VISIBLE|")&&visible.split("\\|").length==9,
            "Each Twin body has its own packet with coordinates only when visible");
        check(hidden.equals("TWIN|b|HIDDEN|61"),"Hidden sibling must not leak world position");
        check(TwinGhostPair.wire(bodyB,62,true,true).endsWith("|GREEN"),
            "Ancient sibling must retain its inherited approved green variant");
        check(GhostIdentity.Type.TWINS.baseType()==GhostIdentity.Type.TWINS
            &&GhostIdentity.Type.ANCIENT_TWINS.baseType()==GhostIdentity.Type.TWINS,
            "Twins must remain ONE linked supernatural identity");
        System.out.println("PASS: two individual, distinct four-view Twins; bounded physical sibling and private per-body disclosure");
    }
}
