package org.beckenburg;

import java.util.Objects;

/**
 * Host-only camera optical projection. A camera's actual pose, power and
 * facing decide its sight; a Hunter's personal APPARITION state is irrelevant.
 */
public final class LanCameraSight {
    private LanCameraSight(){}
    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested) {
        Objects.requireNonNull(map);Objects.requireNonNull(camera);Objects.requireNonNull(apparition);
        if(!manifested)return false;
        double dx=apparition.x()-camera.x(),dz=apparition.z()-camera.z();
        double distance=Math.hypot(dx,dz);
        if(distance>14||distance<.1||Math.abs(apparition.y()-camera.y())>3.0)return false;
        double radians=Math.toRadians(camera.heading());
        double angle=Math.atan2(dx,dz);
        double difference=Math.atan2(Math.sin(angle-radians),Math.cos(angle-radians));
        double fov=Math.toRadians(EquipmentOptics.forTier(camera.tier()).videoFieldOfView());
        if(Math.abs(difference)>fov*.5)return false;
        return map.clearLineOfSight(camera.x(),camera.y()+1.3,camera.z(),
            apparition.x(),apparition.y()+1.3,apparition.z());
    }
    public static String frame(LanVideoCameras.Camera camera,String member,
                               LanGhostSpatial.Pose pose,boolean sees,
                               GhostIdentity.Type species,int tick) {
        if(tick<0||!member.equals("a")&&!member.equals("b"))throw new IllegalArgumentException("camera frame");
        if(!sees)return "CAMGHOST|"+camera.owner()+"|"+member+"|HIDDEN|"+tick;
        String identity=species.baseType()==GhostIdentity.Type.TWINS
            ?(species.ancient()?"ancient_":"")+"twins_"+member
            :species.name().toLowerCase(java.util.Locale.ROOT);
        return "CAMGHOST|"+camera.owner()+"|"+member+"|VISIBLE|"
            +Math.round(pose.x()*1000)+"|"+Math.round(pose.y()*1000)+"|"
            +Math.round(pose.z()*1000)+"|"+pose.heading()+"|"
            +(species.ancient()?"GREEN":"BASE")+"|"+identity+"|"+tick;
    }
}
