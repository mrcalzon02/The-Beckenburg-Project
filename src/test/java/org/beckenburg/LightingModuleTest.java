package org.beckenburg;

import dev.mrcalzon.lighting.LightingModule;
import dev.mrcalzon.lighting.LightModel;

/** Optional diagnostic: local graphics settings must not mutate game authority. */
public final class LightingModuleTest {
    private static void check(boolean ok,String message){
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var before=LightingModule.current();
        try {
            for(var preset:new LightingModule.Preset[]{
                    LightingModule.Preset.PERFORMANCE,
                    LightingModule.Preset.BALANCED,
                    LightingModule.Preset.QUALITY}){
                var settings=LightingModule.preset(preset);
                check(settings.preset()==preset,"Preset identity");
                check(settings.cacheLimit(1000)>0,"Positive bounded cache");
            }
            var standard=LightingModule.preset(LightingModule.Preset.BALANCED);
            // A lower-memory profile must not secretly darken or shorten
            // investigators' light sources. Those are explicit Custom controls.
            for(var preset:new LightingModule.Preset[]{
                    LightingModule.Preset.PERFORMANCE,
                    LightingModule.Preset.QUALITY}){
                var choice=LightingModule.preset(preset);
                check(choice.sourcePercent()==standard.sourcePercent()
                    &&choice.reachPercent()==standard.reachPercent()
                    &&choice.ambientPercent()==standard.ambientPercent()
                    &&choice.flickerPercent()==standard.flickerPercent(),
                    "A performance/quality preset silently changed scene illumination");
            }
            check(LightModel.angular(LightModel.Beam.OMNIDIRECTIONAL,-.8)==1,
                "Omnidirectional beams cannot be gated by source direction");
            check(LightModel.attenuation(2,1,.5,4,1)>0,
                "A visible light contribution must remain positive");
            check(LightModel.portalInfluences(0,1,0,4,2,0,0,1,true),
                "Nearby light cannot ignore an intersecting doorway");
            check(!LightModel.portalInfluences(90,1,90,4,2,0,0,1,true),
                "A distant source must not trigger a door's full lighting reset");
            check(LightModel.flicker(1.7,0)==1,
                "Zero cosmetic flicker should never modulate light intensity");
            check(LightingModule.preset(LightingModule.Preset.PERFORMANCE)
                .cacheLimit(1000)<standard.cacheLimit(1000),
                "Performance preset must actually reduce cache allocation");
            var brighter=new LightingModule.Settings(LightingModule.Preset.CUSTOM,
                120,110,115,0,80,2,false,true,true);
            LightingModule.apply(standard);
            var map=new HouseMap(HouseMap.Site.HOUSE);
            var regular=new FixtureLighting(map);
            var standardSource=regular.sources().get(0);
            LightingModule.apply(brighter);
            var custom=new FixtureLighting(map);
            var updatedSource=custom.sources().get(0);
            check(LightingModule.current().equals(brighter),"Saved settings not active");
            check(updatedSource.radius()>standardSource.radius(),
                "Fixture source reach did not change");
            check(updatedSource.power()>standardSource.power(),
                "Fixture brightness did not change");
            check(custom.sources().size()==regular.sources().size(),
                "Lighting settings must not add or delete game fixtures");
            var authoritativeCircuits=new RoomLighting();
            authoritativeCircuits.toggle(0);
            String switchState=authoritativeCircuits.snapshot();
            double roomState=authoritativeCircuits.brightness(0,30);
            LightingModule.apply(standard);
            LightingModule.apply(brighter);
            check(authoritativeCircuits.snapshot().equals(switchState)
                &&authoritativeCircuits.brightness(0,30)==roomState,
                "Local graphics settings must not alter authoritative room power state");
            boolean rejected=false;
            try {new LightingModule.Settings(LightingModule.Preset.CUSTOM,
                500,100,100,100,100,4,true,true,true);}
            catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"Unsafe lighting settings must be rejected");
            System.out.println("PASS lighting presets, persisted settings, visual source tuning, gameplay separation");
        }finally{
            LightingModule.apply(before);
        }
    }
}
