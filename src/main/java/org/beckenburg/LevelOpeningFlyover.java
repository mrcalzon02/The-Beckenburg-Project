package org.beckenburg;

import java.util.Objects;

/**
 * Scripted, visual-only opening shot. Never modifies the hunter's position,
 * game tick, collision world, host packets, ghost seed or physical van.
 * Cameras are in world metres; the path adapts to any authored level plan.
 */
public final class LevelOpeningFlyover {
    public static final double DURATION_SECONDS=7.5;
    public record Pose(double x,double y,double z,double yaw,double pitch){
        public Pose{
            if(!Double.isFinite(x+y+z+yaw+pitch))
                throw new IllegalArgumentException("Non-finite opening camera");
        }
        public void apply(SceneRenderer scene){
            scene.x=x;scene.eyeHeight=y;scene.z=z;
            scene.yaw=yaw;scene.pitch=pitch;
        }
    }
    private final Pose aerial,orbit,approach,van,insideVan,firstPerson;
    record Framing(double cx,double cz,double span,double top){}
    /** Outdoor ownership volumes are valid survey anchors, never building walls.
     * Exclude arrival/van zones so their long approach cannot dominate the shot. */
    static Framing framing(HouseMap map){
        double x0=Double.POSITIVE_INFINITY,z0=x0,x1=-x0,z1=-x0,top=-x0;
        for(var room:map.level().rooms()){
            if(map.nativeSite()?room.id()>=HouseMap.INDOOR_ROOMS:room.outside())continue;
            x0=Math.min(x0,room.x0());x1=Math.max(x1,room.x1());
            z0=Math.min(z0,room.z0());z1=Math.max(z1,room.z1());
            top=Math.max(top,room.y()+(map.nativeSite()?0:room.height()));
        }
        if(!Double.isFinite(x0+z0+x1+z1+top))throw new IllegalStateException("No authored survey anchors");
        return new Framing((x0+x1)/2,(z0+z1)/2,Math.max(15,Math.max(x1-x0,z1-z0)),top);
    }
    private static double ease(double t){
        t=Math.max(0,Math.min(1,t));
        return t*t*(3-2*t);
    }
    private static Pose lookAt(double x,double y,double z,double tx,double ty,double tz){
        double distance=Math.hypot(tx-x,tz-z);
        return new Pose(x,y,z,Math.atan2(tx-x,tz-z),
            Math.atan2(ty-y,Math.max(.01,distance)));
    }
    private static Pose interpolate(Pose a,Pose b,double t){
        double q=ease(t);
        double da=Math.atan2(Math.sin(b.yaw-a.yaw),Math.cos(b.yaw-a.yaw));
        return new Pose(a.x+(b.x-a.x)*q,a.y+(b.y-a.y)*q,
            a.z+(b.z-a.z)*q,a.yaw+da*q,
            a.pitch+(b.pitch-a.pitch)*q);
    }
    public LevelOpeningFlyover(HouseMap map,Pose firstPerson){
        Objects.requireNonNull(map);
        this.firstPerson=Objects.requireNonNull(firstPerson);
        var framing=framing(map);
        double cx=framing.cx(),cz=framing.cz(),span=framing.span(),top=framing.top();
        var dock=map.van().placement();
        double elevation=top+Math.max(15,span*.55);
        aerial=lookAt(cx-span*.58,elevation,cz-span*.79,cx,top*.35,cz);
        orbit=lookAt(cx+span*.48,top+Math.max(11,span*.30),cz-span*.38,
            cx,top*.35,cz);
        // An authored cargo entrance, NOT a guessed line towards the van's
        // centre: world() rotates this flight corridor with the real truck.
        // The default loading cargo door is open, with an existing walkable
        // ramp from FRONT-RAMP_LENGTH to FRONT and a clear central aisle.
        var truck=map.van();
        var rearApproach=truck.world(0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-2.0);
        var doorway=truck.world(0,VanStaging.FRONT-.22);
        var inside=truck.world(0,VanStaging.FRONT+.52);
        var target=truck.world(0,VanStaging.FRONT+1.40);
        double floor=truck.floorY();
        approach=lookAt(rearApproach.x(),floor+4.5,rearApproach.z(),
            doorway.x(),floor+1.55,doorway.z());
        van=lookAt(doorway.x(),floor+1.62,doorway.z(),
            target.x(),floor+1.62,target.z());
        insideVan=lookAt(inside.x(),floor+1.62,inside.z(),
            target.x(),floor+1.62,target.z());
    }
    /** 0..1 cinematic progress, independent of frame rate or simulation tick. */
    public Pose pose(double progress){
        double t=Math.max(0,Math.min(1,progress));
        if(t<=0)return aerial;
        if(t>=1)return firstPerson; // bit-exact player camera handoff
        if(t<.39)return interpolate(aerial,orbit,t/.39);
        if(t<.78)return interpolate(orbit,approach,(t-.39)/.39);
        if(t<.93)return interpolate(approach,van,(t-.78)/.15);
        if(t<.975)return interpolate(van,insideVan,(t-.93)/.045);
        return interpolate(insideVan,firstPerson,(t-.975)/.025);
    }
    public Pose firstPerson(){return firstPerson;}
}
