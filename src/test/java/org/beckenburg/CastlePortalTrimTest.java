package org.beckenburg;

import java.util.List;

/** Real authored Castle apertures, including upper-floor doors and the 4.6m gate. */
public final class CastlePortalTrimTest {
    private CastlePortalTrimTest(){}
    private static void check(boolean ok,String problem){
        if(!ok)throw new AssertionError(problem);
    }
    private static void near(double got,double expected,String issue){
        check(Math.abs(got-expected)<1e-6,issue+" got="+got+" expected="+expected);
    }
    public static void main(String[] ignored)throws java.io.IOException{
        var walls=CastleLevelEdits.current().walls();
        int checked=0,upper=0,gates=0;
        for(var marker:walls){
            if(!marker.hasDoor())continue;
            checked++;
            double head=CastleHubArchitecturalPlan.portalHead(marker,walls);
            var beams=CastlePortalTrim.beams(marker,walls);
            check(beams.size()==3,"Portal must have two side reveals and one header: "+marker);
            double low=marker.horizontal()?marker.x0():marker.z0();
            double high=marker.horizontal()?marker.x1():marker.z1();
            for(var beam:beams){
                check(beam.y()>=marker.bottom()-1e-8
                    &&beam.top()<=head+1e-8,
                    "Door trim pokes through its own lintel / upper floor: "+marker);
                double begin=marker.horizontal()?beam.x():beam.z();
                double finish=begin+(marker.horizontal()?beam.width():beam.depth());
                check(begin>=low-1e-8&&finish<=high+1e-8,
                    "Portal trim bleeds into neighbouring solid wall: "+marker);
                check(beam.height()>.01&&beam.width()>.01&&beam.depth()>.01,
                    "Portal trim contains invalid / degenerate 3D geometry");
            }
            if(marker.bottom()>3.0){
                upper++;
                near(head,marker.bottom()+CastleHubArchitecturalPlan.DOOR_HEIGHT,
                    "Upper-residence doorway must stop at its own floor-level lintel");
            }
            if(Math.abs(marker.portalWidth()-4.6)<.01){
                gates++;
                near(head,2.95,"Real gatehouse has a 2.95m opening, not the default 2.7m");
            }
        }
        check(checked>=10&&upper>=3&&gates==1,
            "Expected actual ground/upper Castle doors and courtyard gate were missing");
        // An editor-authoring change to a nonstandard header must alter the
        // reveal exactly, without emitting a post through a floor or roof.
        var portal=new CastleHubArchitecturalPlan.Wall(13,11,14.4,11,3.4,3.4,
            MaterialAtlas.STONE,13.7,1.4);
        var lintel=new CastleHubArchitecturalPlan.Wall(13,11,14.4,11,6.25,6.8,
            MaterialAtlas.STONE,Double.NaN,0);
        near(CastleHubArchitecturalPlan.portalHead(portal,List.of(portal,lintel)),
            6.25,"Nonstandard authored lintel must set trim height");
        for(var beam:CastlePortalTrim.beams(portal,List.of(portal,lintel)))
            check(beam.top()<=6.25+1e-8,
                "Editor-defined doorway extends into upper wall");
        if(ignored.length>0){
            // Genuine scene captures: stage the upper residence aperture and
            // courtyard gate through the EXACT playable renderer. These are
            // not image mockups or approximated portal sketches.
            var directory=java.nio.file.Path.of(ignored[0]);
            java.nio.file.Files.createDirectories(directory);
            var map=new CastleHubMap();
            var scene=new SceneRenderer();
            scene.setRenderSize(960,540);
            scene.setPsxPreset(PsxPipeline.Preset.OFF);
            scene.lamp=false;
            scene.x=9.4;scene.z=6.5;scene.eyeHeight=5.02;
            scene.yaw=Math.PI/2;scene.pitch=-.07;
            scene.drawCastle(map,0,true);
            check(javax.imageio.ImageIO.write(scene.image(),"png",
                directory.resolve("castle-upper-residence-doorway.png").toFile()),
                "Could not save actual Castle upper doorway");
            scene.x=15.5;scene.z=30.0;scene.eyeHeight=1.62;
            scene.yaw=0;scene.pitch=-.08;
            scene.drawCastle(map,0,true);
            check(javax.imageio.ImageIO.write(scene.image(),"png",
                directory.resolve("castle-courtyard-gate-trim.png").toFile()),
                "Could not save actual Castle gate scene");
        }
        System.out.println("PASS Castle authored portal geometry: "+checked+
            " openings ("+upper+" upper, "+gates+" gate), no floor or wall intrusion");
    }
}
