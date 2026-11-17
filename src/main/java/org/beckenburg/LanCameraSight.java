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

    private static final VanInteriorLayout.Fixture SCREEN=VanInteriorLayout.FIXTURES.stream()
        .filter(f->f.purpose().equals("camera feed")).findFirst().orElseThrow();
    /** Is the physical van LCD in the central part of this first-person view? */
    static boolean screenInView(HouseMap map,double x,double eye,double z,double yaw,double pitch){
        var van=map.van();
        var screen=van.world(SCREEN.localX(),SCREEN.localZ());
        double dx=screen.x()-x,dz=screen.z()-z;
        double dy=van.placement().y()+SCREEN.y()+.17-eye;
        double forward=dx*Math.sin(yaw)+dz*Math.cos(yaw);
        double side=dx*Math.cos(yaw)-dz*Math.sin(yaw);
        double depth=forward*Math.cos(pitch)+dy*Math.sin(pitch);
        double up=dy*Math.cos(pitch)-forward*Math.sin(pitch);
        return depth>.10&&depth<5&&Math.abs(side)<=depth*.6&&Math.abs(up)<=depth*.38;
    }

    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested) {
        return seesBody(map,camera,apparition,manifested,1.95);
    }

    /** Host uses the actual species/body dimensions, not a guessed humanoid. */
    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested,
                               GhostIdentity.Type species,String member) {
        return sees(map,camera,apparition,manifested,species,member,false);
    }
    /** Camera frustum must test the silhouette that will actually render. */
    public static boolean sees(HouseMap map,LanVideoCameras.Camera camera,
                               LanGhostSpatial.Pose apparition,boolean manifested,
                               GhostIdentity.Type species,String member,boolean signatureReveal) {
        return seesBody(map,camera,apparition,manifested,bodyHeight(species,member,signatureReveal));
    }
    private static double bodyHeight(GhostIdentity.Type species,String member,boolean signatureReveal){
        Objects.requireNonNull(species);
        if(!"a".equals(member)&&!"b".equals(member))
            throw new IllegalArgumentException("invalid camera ghost member");
        String identity=signatureReveal
            ?(species.baseType()==GhostIdentity.Type.TWINS
                ?"twins_"+member:CharacterVisuals.ghost(species))
            :GhostManifestationVisual.UNREVEALED;
        return CharacterVisuals.shape(identity).height();
    }

    static boolean seesPattern(HouseMap map,LanVideoCameras.Camera camera,LanGhostSpatial.Pose pose){
        return seesBody(map,camera,pose,true,1.65);
    }
    static boolean seesPatternInField(HouseMap map,int seat,LanWorld.Body body,boolean crouched,double pitch,
            LanGhostSpatial.Pose pose){
        var view=new LanVideoCameras.Camera(seat,body.room(),body.x(),body.y(),body.z(),body.heading(),EquipmentTiers.Tier.II);
        return seesBody(map,view,pose,true,1.65,HunterPosture.eye(crouched),pitch,1);
    }

    /** A short-lived optical event stays where the haunting occurred, even if the ghost walks away. */
    record Orb(LanGhostSpatial.Pose source,int born){
        String sample(HouseMap map,LanVideoCameras.Camera camera,boolean powered,int tick){
            int age=tick-born;
            if(!powered||age<0||age>=40)return null;
            var center=new LanGhostSpatial.Pose(source.room(),source.x(),
                source.y()+1.15+.12*Math.sin(age*.19),source.z(),0);
            if(!seesBody(map,camera,center,true,0))return null;
            return "CAMORB|"+camera.owner()+"|"+Math.round(center.x()*1000)+"|"+Math.round(center.y()*1000)+
                "|"+Math.round(center.z()*1000)+"|"+tick;
        }
    }

    private static boolean seesBody(HouseMap map,LanVideoCameras.Camera camera,
                                   LanGhostSpatial.Pose apparition,boolean manifested,
                                   double bodyHeight) {
        return seesBody(map,camera,apparition,manifested,bodyHeight,LENS_HEIGHT,LENS_PITCH,1);
    }
    /** Held lens matches the raised viewfinder position, crouch, pitch and Tier-I print crop. */
    static boolean seesHeld(HouseMap map,LanVideoCameras.Camera camera,
            LanGhostSpatial.Pose pose,boolean manifested,GhostIdentity.Type species,String member,
            boolean signature,boolean crouched,double pitch,boolean print){
        double yaw=Math.toRadians(camera.heading());
        var lens=new LanVideoCameras.Camera(camera.owner(),camera.room(),camera.x()+Math.sin(yaw)*.12,
            camera.y(),camera.z()+Math.cos(yaw)*.12,camera.heading(),camera.tier());
        double crop=print&&camera.tier()==EquipmentTiers.Tier.I?(double)SceneRenderer.H/SceneRenderer.W:1;
        return seesBody(map,lens,pose,manifested,bodyHeight(species,member,signature),
            HunterPosture.eye(crouched)-.04,pitch,crop);
    }
    private static boolean seesBody(HouseMap map,LanVideoCameras.Camera camera,
            LanGhostSpatial.Pose apparition,boolean manifested,double bodyHeight,
            double eyeHeight,double pitch,double crop){
        Objects.requireNonNull(map);Objects.requireNonNull(camera);Objects.requireNonNull(apparition);
        if(!manifested)return false;
        double dx=apparition.x()-camera.x(),dz=apparition.z()-camera.z();
        double distance=Math.hypot(dx,dz);
        if(distance>14||distance<.1)return false;
        double yaw=Math.toRadians(camera.heading());
        double forward=dx*Math.sin(yaw)+dz*Math.cos(yaw);
        double side=dx*Math.cos(yaw)-dz*Math.sin(yaw);
        double horizontal=Math.tan(Math.toRadians(EquipmentOptics.forTier(camera.tier()).videoFieldOfView())*.5);
        double vertical=horizontal*SceneRenderer.H/SceneRenderer.W;
        horizontal*=crop;
        double eye=camera.y()+eyeHeight,relativeY=apparition.y()-eye;
        double cp=Math.cos(pitch),sp=Math.sin(pitch);
        double depth=forward*cp+relativeY*sp,up=relativeY*cp-forward*sp;
        // Clip the real vertical segment to the five planes of the raised or mounted lens.
        double[] span={0,bodyHeight};
        if(!clip(span,depth-NEAR,sp)
                ||!clip(span,depth*horizontal-Math.abs(side),sp*horizontal)
                ||!clip(span,depth*vertical-up,sp*vertical-cp)
                ||!clip(span,depth*vertical+up,sp*vertical+cp))return false;
        for(double part:new double[]{.4,.75,.15}){
            double height=span[0]+part*(span[1]-span[0]);
            if(map.clearLineOfSight(camera.x(),eye,camera.z(),apparition.x(),
                    apparition.y()+height,apparition.z()))return true;
        }
        return false;
    }

    /** Intersect an interval of body heights with base+slope*height >= 0. */
    private static boolean clip(double[] span,double base,double slope){
        if(Math.abs(slope)<1e-9)return base>=0;
        if(slope>0)span[0]=Math.max(span[0],-base/slope);
        else span[1]=Math.min(span[1],-base/slope);
        return span[0]<=span[1];
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
        return frame("CAMGHOST",camera,member,pose,sees,species,tick,signatureReveal);
    }
    static String frame(String channel,LanVideoCameras.Camera camera,String member,
            LanGhostSpatial.Pose pose,boolean sees,GhostIdentity.Type species,int tick,boolean signatureReveal){
        if(tick<0||!member.equals("a")&&!member.equals("b"))throw new IllegalArgumentException("camera frame");
        if(!sees)return channel+"|"+camera.owner()+"|"+member+"|HIDDEN|"+tick;
        String identity=signatureReveal
            ?(species.baseType()==GhostIdentity.Type.TWINS
                ?(species.ancient()?"ancient_":"")+"twins_"+member
                :species.name().toLowerCase(java.util.Locale.ROOT))
            :GhostManifestationVisual.UNREVEALED;
        return channel+"|"+camera.owner()+"|"+member+"|VISIBLE|"
            +Math.round(pose.x()*1000)+"|"+Math.round(pose.y()*1000)+"|"
            +Math.round(pose.z()*1000)+"|"+pose.heading()+"|"
            +(signatureReveal&&species.ancient()?"GREEN":"BASE")+"|"+identity+"|"+tick;
    }
}
