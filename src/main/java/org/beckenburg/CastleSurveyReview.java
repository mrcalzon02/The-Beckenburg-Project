package org.beckenburg;

import java.util.List;

/** Repeatable, hidden-window GPU survey of the actual home-menu level. */
public final class CastleSurveyReview {
    private CastleSurveyReview(){}
    private static LevelOpeningFlyover.Pose look(double x,double y,double z,
            double tx,double ty,double tz){
        return new LevelOpeningFlyover.Pose(x,y,z,Math.atan2(tx-x,tz-z),
            Math.atan2(ty-y,Math.hypot(tx-x,tz-z)));
    }
    public static void execute()throws Exception{
        var map=new CastleHubMap();
        var renderer=new SceneRenderer();renderer.setRenderSize(800,500);
        renderer.roomLights=new RoomLighting();
        renderer.setLampHardware(EquipmentTiers.Tier.II,false);
        var shots=List.of(
            new MapCompatibilityReview.Shot("01-overflight-south",look(22,29,57,22,2,18),true,false),
            new MapCompatibilityReview.Shot("02-overflight-north",look(22,27,-18,22,2,18),true,false),
            new MapCompatibilityReview.Shot("03-overflight-east",look(62,25,20,22,2,18),true,false),
            new MapCompatibilityReview.Shot("04-overflight-west",look(-18,25,20,22,2,18),true,false),
            new MapCompatibilityReview.Shot("05-courtyard",look(13.5,1.62,20.5,15,2,8),true,false),
            new MapCompatibilityReview.Shot("06-stair-up",look(17.5,1.62,32.5,17.5,4.0,22),true,false),
            new MapCompatibilityReview.Shot("07-stair-down",look(17.5,5.02,21.4,17.5,1,32),true,false),
            new MapCompatibilityReview.Shot("08-gallery-under",look(14,1.62,24,18,3.3,17),true,false),
            new MapCompatibilityReview.Shot("09-upper-library",look(17.5,5.02,9.2,13.5,4.5,3),true,false),
            new MapCompatibilityReview.Shot("10-ramparts",look(20.2,5.02,22.5,12,4.1,32),true,false),
            new MapCompatibilityReview.Shot("11-armory",look(8,1.62,7,4,1,5),true,false),
            new MapCompatibilityReview.Shot("12-contract",look(17.5,1.62,6.5,13.5,.9,6.8),true,false),
            new MapCompatibilityReview.Shot("13-lounge",look(22.5,1.62,6.5,25,1,2),true,false),
            new MapCompatibilityReview.Shot("14-practice",look(8,1.62,22,5,1,16),true,false),
            new MapCompatibilityReview.Shot("15-operations",look(24,1.62,22.5,22.5,.8,16.6),true,false),
            new MapCompatibilityReview.Shot("16-archive",look(39,1.62,14.5,36,1,11.8),true,false),
            new MapCompatibilityReview.Shot("17-warehouse",look(35,1.62,27,40.7,1,25.9),true,false),
            new MapCompatibilityReview.Shot("18-arrival",look(15.5,1.62,33,13.3,1,27.7),true,false),
            new MapCompatibilityReview.Shot("19-courtyard-blackout",look(13.5,1.62,20.5,15,2,8),false,false),
            new MapCompatibilityReview.Shot("20-library-blackout",look(17.5,5.02,9.2,13.5,4.5,3),false,false),
            new MapCompatibilityReview.Shot("21-library-torch",look(17.5,5.02,9.2,13.5,4.5,3),false,true),
            new MapCompatibilityReview.Shot("22-store-west-tiers",look(32.3,1.55,27.5,29.65,1.35,27.5),true,false),
            new MapCompatibilityReview.Shot("23-store-east-tiers",look(39.2,1.55,23.7,41.95,1.35,23.7),true,false),
            new MapCompatibilityReview.Shot("24-store-north-tiers",look(32.2,1.55,21,32.2,1.35,18.1),true,false),
            new MapCompatibilityReview.Shot("25-store-south-tiers",look(37,1.55,30.1,37,1.35,33.1),true,false),
            new MapCompatibilityReview.Shot("26-lounge-clear",look(22,1.62,10,25,1.2,4),true,false));
        for(var shot:shots)MapCompatibilityReview.capture("castle",shot,renderer,null,map);
        System.out.println("PASS Castle survey: "+shots.size()+" completed OpenGL frames; visual/gameplay acceptance recorded separately");
    }
}
