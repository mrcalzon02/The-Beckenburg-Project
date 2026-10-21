package org.beckenburg;

/** Real camera-only visibility uses device geometry, direction and manifestation state. */
public final class LanCameraSightTest {
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void main(String[] args){
        HouseMap map=new HouseMap();
        var origin=map.spawn(4);
        LanGhostSpatial.Pose ghost=null;
        for(double dx:new double[]{0,.35,-.35,.7,-.7})
            for(double dz:new double[]{.35,-.35,.7,-.7})
                if(ghost==null){
                    double tx=origin.x()+dx,tz=origin.z()+dz;
                    if(map.traversable(tx,origin.y(),tz)
                        &&map.clearLineOfSight(origin.x(),origin.y()+1.3,origin.z(),
                            tx,origin.y()+1.3,tz))
                        ghost=new LanGhostSpatial.Pose(map.roomAt(tx,origin.y(),tz),
                            tx,origin.y(),tz,0);
                }
        check(ghost!=null,"Authored room should have room-space LOS near spawn");
        int towards=Math.floorMod((int)Math.round(Math.toDegrees(
            Math.atan2(ghost.x()-origin.x(),ghost.z()-origin.z()))),360);
        var camera=new LanVideoCameras.Camera(0,4,origin.x(),origin.y(),origin.z(),
            towards,EquipmentTiers.Tier.II);
        check(LanCameraSight.sees(map,camera,ghost,true),
            "Manifested ghost directly facing a real active camera should be in view");
        check(!LanCameraSight.sees(map,camera,ghost,false),
            "Unmanifested ghost cannot appear in live camera feed");
        var away=new LanVideoCameras.Camera(0,4,origin.x(),origin.y(),origin.z(),
            Math.floorMod(towards+180,360),EquipmentTiers.Tier.II);
        check(!LanCameraSight.sees(map,away,ghost,true),
            "Camera must not see behind its physical lens");
        String visible=LanCameraSight.frame(camera,"a",ghost,true,
            GhostIdentity.Type.SHADE,21);
        String hidden=LanCameraSight.frame(camera,"a",ghost,false,
            GhostIdentity.Type.SHADE,22);
        check(visible.startsWith("CAMGHOST|0|a|VISIBLE|")
            &&visible.endsWith("|shade|21"),"Camera-only figure has own correct identity");
        check(hidden.equals("CAMGHOST|0|a|HIDDEN|22"),"Hidden camera feed must not reveal ghost position or identity");
        String pair=LanCameraSight.frame(camera,"b",ghost,true,
            GhostIdentity.Type.ANCIENT_TWINS,23);
        check(pair.endsWith("|ancient_twins_b|23")&&pair.contains("|GREEN|"),
            "Ancient Twin member B has private, correct distinct camera art");
        System.out.println("PASS: independent camera optics, facing, concealment and species-correct Twin projection");
    }
}
