package org.beckenburg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Actual six fitted van screens, independent GPU texture revisions and live data. */
public final class VanLiveScreensTest {
    private static void check(boolean yes,String why){
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args){
        EnumSet<VanLiveScreens.Panel> fitted=EnumSet.noneOf(VanLiveScreens.Panel.class);
        int count=0;
        for(var fixture:VanInteriorLayout.FIXTURES){
            if(!fixture.mesh().startsWith("beckenburg_"))continue;
            count++;
            check(VanInteriorLayout.insideShell(fixture),
                "flat monitor must fit roof and side wall: "+fixture.purpose());
            var bounds=VanInteriorLayout.bounds(fixture.mesh());
            check(bounds.x1()>bounds.x0()&&bounds.y1()>bounds.y0(),
                "screen model has physical face: "+fixture.purpose());
            fitted.add(VanLiveScreens.forPurpose(fixture.purpose()));
        }
        check(count==6&&fitted.size()==6,"six independent live stations expected");
        int contract=VanLiveScreens.material(VanLiveScreens.Panel.CONTRACT);
        int team=VanLiveScreens.material(VanLiveScreens.Panel.TEAM);
        int camera=VanLiveScreens.material(VanLiveScreens.Panel.CAMERA);
        check(contract!=team&&contract!=camera&&team!=camera,
            "monitor faces cannot share a single image material");
        long teamRevision=GlbProps.dynamicTextureRevision(team);
        long contractRevision=GlbProps.dynamicTextureRevision(contract);
        VanLiveScreens.contract("House / First Investigation","MEDIUM",
            List.of("[OPEN] EMF sample","[DONE] Temperature","[OPEN] Survive"));
        check(GlbProps.dynamicTextureRevision(contract)==contractRevision+1,
            "contract change must upload its own face only");
        check(GlbProps.dynamicTextureRevision(team)==teamRevision,
            "contract must not dirty team monitor");
        VanLiveScreens.team(new VanTelemetry.Display(40,3,new int[]{73,42,-1,-1,-1,-1}));
        check(GlbProps.dynamicTextureRevision(team)==teamRevision+1,
            "sanity must update its own display material");
        VanLiveScreens.activity(new VanTelemetry.Display(44,8,new int[]{65,-1,-1,-1,-1,-1}),
            List.of(0,2,5,8,3,9,8));
        VanLiveScreens.sound(Map.of(),List.of(),"QUIET");
        long before=GlbProps.dynamicTextureRevision(camera);
        BufferedImage realFrame=new BufferedImage(160,90,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<90;y++)for(int x=0;x<160;x++)
            realFrame.setRGB(x,y,new Color(40,115,169).getRGB());
        VanLiveScreens.camera(realFrame,"CAM H1 / ROOM 3");
        check(GlbProps.dynamicTextureRevision(camera)==before+1,
            "actual camera frame must upload only camera texture");
        VanLiveScreens.suspendCamera();
        check(VanLiveScreens.snapshot(VanLiveScreens.Panel.CAMERA)==null,
            "An empty van must not retain an accessible camera image");
        check(GlbProps.dynamicTextureRevision(camera)==before+1,
            "Camera suspension must not upload a new frame or dirty GPU textures");
        int pixel=GlbProps.sample(camera,.5,.5);
        check(pixel!=0,"live camera face must sample actual non-black pixels");
        VanLiveScreens.camera(null,"NO POWERED CAMERA");
        check(GlbProps.dynamicTextureRevision(camera)==before+2,
            "camera disconnect must clear the last displayed feed");
        check(VanLiveScreens.material(VanLiveScreens.Panel.MAP)==GlbProps.VAN_MAP_MATERIAL,
            "map must reuse the existing host-authoritative tactical texture");
        check(!VanDisplayCadence.due(false,300,-100000,
                VanDisplayCadence.CAMERA_TICKS),
            "Camera sampling without a VAN occupant is forbidden");
        check(VanDisplayCadence.due(true,10,-100000,
                VanDisplayCadence.CAMERA_TICKS),
            "An entering viewer should receive the first image without waiting");
        check(!VanDisplayCadence.due(true,29,10,
                VanDisplayCadence.CAMERA_TICKS)
            &&VanDisplayCadence.due(true,30,10,VanDisplayCadence.CAMERA_TICKS),
            "Camera cadence must cap remote captures at one per two seconds");
        check(!VanDisplayCadence.due(true,29,0,
                VanDisplayCadence.TELEMETRY_TICKS)
            &&VanDisplayCadence.due(true,30,0,
                VanDisplayCadence.TELEMETRY_TICKS),
            "Van telemetry must refresh only every three seconds");
        check(!VanInteriorLayout.collides(0,0,.18),
            "the flat screens must not block the van's central operating aisle");
        System.out.println("PASS six real van LCD faces, finite physical bounds, "+
            "independent live revisions and camera offline fallback");
    }
}
