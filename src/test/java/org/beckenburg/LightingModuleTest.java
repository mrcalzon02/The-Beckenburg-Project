package org.beckenburg;

import dev.mrcalzon.lighting.LightingModule;

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
            check(new RoomLighting().snapshot().length()==HouseMap.INDOOR_ROOMS,
                "Local graphics options must not change host lighting circuits");
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
