package org.beckenburg;

/** The editor's drawn influence envelope must match the real wall/portal rays. */
public final class LightVolumePreviewTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    private static CastleLevelEdits.Lamp lamp(double x,double y,double z,
                                              double radius){
        var approved=CastleHubDecor.LIGHTS.get(0);
        return new CastleLevelEdits.Lamp("preview_test",approved.on(),approved.off(),
            x,y,z,1,0,CastleHubDecor.Mount.WALL,radius,2.5);
    }
    public static void main(String[] args){
        var map=new CastleHubMap();
        var behindWall=LightVolumePreview.trace(map,lamp(9.8,1.5,3,3.2));
        check(behindWall.rays().size()==48,"Light influence must cover 3 spherical angle slices");
        var forward=behindWall.rays().get(16);
        check(forward.blocked(),"The solid Castle west wall must clip an editor light cone");
        check(Math.hypot(forward.x()-9.8,forward.z()-3)<3.2,
            "Closed wall must limit the displayed reach, not show through masonry");

        var openArch=LightVolumePreview.trace(map,lamp(9.8,1.5,6.5,1.6));
        var through=openArch.rays().get(16);
        check(!through.blocked(),"A designed open arch must permit the editor light ray");
        check(Math.abs(through.x()-11.4)<.001,
            "Open arch must preserve the full authored 1.6m radius");
        check(Math.abs(openArch.radius()-1.6)<1e-9,
            "Influence gizmo must use the actual inspector light radius");

        for(var ray:behindWall.rays())
            check(Double.isFinite(ray.x()+ray.y()+ray.z()),
                "Editor light volume must not project invalid endpoints");
        System.out.println("PASS 48-ray editable light volume: solid-wall clipping, arch transmission, authored radius");
    }
}
