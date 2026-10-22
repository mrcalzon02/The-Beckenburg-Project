package org.beckenburg;

import java.util.Objects;

/**
 * Host-only camera optical projection. A camera's actual pose, power and
 * facing decide its sight; a Hunter's personal APPARITION state is irrelevant.
 */
public final class LanCameraSight {
    private LanCameraSight(){}
    // These values describe the lens actually used by the live van renderer.
    // An apparition is not camera-visible unless a part of its real silhouette
    // would be within that rendered lens and a physical ray reaches the body.
    private static final double LENS_HEIGHT=1.62;
    private static final double LENS_PITCH=-.035;
    private static final double NEAR=.065;

    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested) {
        return seesBody(map,camera,apparition,manifested,1.95);
    }

    /** Host uses the actual species/body dimensions, not a guessed humanoid. */
    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested,
                               GhostIdentity.Type species,String member) {
        Objects.requireNonNull(species);
        if(!"a".equals(member)&&!"b".equals(member))
            throw new IllegalArgumentException("invalid camera ghost member");
        String identity=species.baseType()==GhostIdentity.Type.TWINS
            ?"twins_"+member:CharacterVisuals.ghost(species);
        return seesBody(map,camera,apparition,manifested,
            CharacterVisuals.shape(identity).height());
    }

    private static boolean seesBody(HouseMap map,LanVideoCameras.Camera camera,
                                   LanGhostSpatial.Pose apparition,boolean manifested,
                                   double bodyHeight) {
        Objects.requireNonNull(map);Objects.requireNonNull(camera);Objects.requireNonNull(apparition);
        if(!manifested)return false;
        double dx=apparition.x()-camera.x(),dz=apparition.z()-camera.z();
        double distance=Math.hypot(dx,dz);
        if(distance>14||distance<.1)return false;
        double yaw=Math.toRadians(camera.heading());
        double angle=Math.atan2(dx,dz);
        double difference=Math.atan2(Math.sin(angle-yaw),Math.cos(angle-yaw));
        double fov=Math.toRadians(EquipmentOptics.forTier(camera.tier()).videoFieldOfView());
        if(Math.abs(difference)>fov*.5)return false;
        double forward=dx*Math.sin(yaw)+dz*Math.cos(yaw);
        if(forward<=NEAR)return false;
        // Same projection as SceneRenderer: 320x200, actual camera lens at Y+1.62,
        // tilted down 0.035 radians. The WHOLE body can be off screen even when
        // its floor position lies within the horizontal camera cone.
        double eye=camera.y()+LENS_HEIGHT;
        double bottom=apparition.y()-eye,top=bottom+bodyHeight;
        double cp=Math.cos(LENS_PITCH),sp=Math.sin(LENS_PITCH);
        double nearBottom=bottom*sp+forward*cp;
        double nearTop=top*sp+forward*cp;
        if(nearBottom<=NEAR||nearTop<=NEAR)return false;
        double projectedBottom=(bottom*cp-forward*sp)/nearBottom;
        double projectedTop=(top*cp-forward*sp)/nearTop;
        double halfVertical=(SceneRenderer.H*.5)/(SceneRenderer.W*.5)
            *Math.tan(fov*.5);
        if(projectedBottom>halfVertical||projectedTop< -halfVertical)return false;
        // A lens ray to a real visible segment of the figure is necessary.
        // Furnishings, closed doors and walls can hide the rest of the body.
        for(double fraction:new double[]{.40,.75})
            if(map.clearLineOfSight(camera.x(),eye,camera.z(),
                    apparition.x(),apparition.y()+bodyHeight*fraction,apparition.z()))
                return true;
        return false;
    }
    public static String frame(LanVideoCameras.Camera camera,String member,
                               LanGhostSpatial.Pose pose,boolean sees,
                               GhostIdentity.Type species,int tick) {
        return frame(camera,member,pose,sees,species,tick,false);
    }
    /** A camera may see an apparition without unmasking its true species. */
    public static String frame(LanVideoCameras.Camera camera,String member,
                               LanGhostSpatial.Pose pose,boolean sees,
                               GhostIdentity.Type species,int tick,boolean signatureReveal) {
        if(tick<0||!member.equals("a")&&!member.equals("b"))throw new IllegalArgumentException("camera frame");
        if(!sees)return "CAMGHOST|"+camera.owner()+"|"+member+"|HIDDEN|"+tick;
        String identity=signatureReveal
            ?(species.baseType()==GhostIdentity.Type.TWINS
                ?(species.ancient()?"ancient_":"")+"twins_"+member
                :species.name().toLowerCase(java.util.Locale.ROOT))
            :GhostManifestationVisual.UNREVEALED;
        return "CAMGHOST|"+camera.owner()+"|"+member+"|VISIBLE|"
            +Math.round(pose.x()*1000)+"|"+Math.round(pose.y()*1000)+"|"
            +Math.round(pose.z()*1000)+"|"+pose.heading()+"|"
            +(signatureReveal&&species.ancient()?"GREEN":"BASE")+"|"+identity+"|"+tick;
    }
}
