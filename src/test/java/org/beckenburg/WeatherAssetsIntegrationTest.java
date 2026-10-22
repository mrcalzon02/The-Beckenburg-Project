package org.beckenburg;

import java.util.ArrayList;
import java.util.List;

/** Verify actual release media, not only weather enum labels. */
public final class WeatherAssetsIntegrationTest {
    private static void ok(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args){
        WeatherAssetLibrary.verifyPackaged();
        for(String id:new String[]{"weather_cloud_bank","weather_dense_fog_bank",
            "weather_ground_fog_bank","weather_ground_splash_frame_00",
            "weather_ground_splash_frame_01","weather_ground_splash_frame_02",
            "weather_heavy_rain_cell","weather_rain_cell","weather_storm_cloud_bank"}){
            var triangles=WeatherAssetLibrary.mesh(id).triangles();
            ok(!triangles.isEmpty(),"Original weather mesh missing "+id);
            GlbProps.sample(triangles.get(0).material(),triangles.get(0).a().u(),triangles.get(0).a().v());
            GlbProps.sampleAlpha(triangles.get(0).material(),triangles.get(0).a().u(),triangles.get(0).a().v());
        }
        for(String family:new String[]{"cloud_to_ground","fork","sheet","horizontal_arc"})
            for(int i=0;i<4;i++)ok(!WeatherAssetLibrary.mesh(
                dev.mrcalzon.weather.WeatherMeshes.lightningFrame(family,i))
                .triangles().isEmpty(),"Missing animated lightning frame "+family+i);
        for(String wav:new String[]{"ambient_fog","ambient_rain","ambient_snow",
            "ambient_storm","ambient_wind","music_day","music_night","music_fog",
            "music_snow","music_storm","thunder_near","thunder_distant"})
            ok(WeatherSoundscape.originalSamples(wav)>1000,"Missing PCM audio "+wav);
        var weather=new dev.mrcalzon.weather.WeatherModule(89031);
        var view=dev.mrcalzon.weather.WeatherView.outdoors(0,1.5,0,0);
        var ground=dev.mrcalzon.weather.WeatherSurfaceGrid.Material.STONE;
        weather.surfaces().register(1,ground,1);
        weather.surfaces().register(2,ground,0);
        weather.setWeather(dev.mrcalzon.weather.WeatherType.RAIN,0);
        weather.setTemperature(5);
        for(int i=0;i<110;i++)weather.advance(1,view);
        ok(weather.surfaces().surface(1).wet()>0,"Rain does not wet exposed ground");
        ok(weather.surfaces().surface(2).wet()==0,"Rain passes through roof");
        ok(weather.surfaces().shade(1,0x808080).rgb()!=0x808080,"Wet texture unmodified");
        weather.setWeather(dev.mrcalzon.weather.WeatherType.SNOW,0);
        weather.setTemperature(-7);
        for(int i=0;i<110;i++)weather.advance(1,view);
        ok(weather.surfaces().surface(1).snowMetres()>0,"Snow not accumulating");
        ok(weather.surfaces().surface(2).snowMetres()==0,"Snow enters roofed room");
        weather.weather().setAutoLightning(false);
        weather.setWeather(dev.mrcalzon.weather.WeatherType.THUNDERSTORM,0);
        weather.weather().strikeRelative(25,0.3,20,"fork",
            dev.mrcalzon.weather.WeatherSystem.StrikeMode.FULL);
        var mesh=new ArrayList<dev.mrcalzon.weather.WeatherMeshes.Instance>();
        weather.emitMeshes(mesh::add);
        ok(mesh.stream().anyMatch(m->m.meshId().startsWith("lightning_fork_")),
            "Lightning mesh never submitted for rendering");
        weather.advance(.2,view);
        ok(weather.audio().drainCues().stream().anyMatch(c->c.id().startsWith("thunder_")),
            "Delayed thunder event never reaches sound mixer");
        HouseMap map=new HouseMap();
        var viewer=map.spawn(13);
        SceneRenderer render=new SceneRenderer();
        render.roomLights=new RoomLighting();
        render.x=viewer.x();render.z=viewer.z();render.eyeHeight=viewer.y()+1.62;
        render.setWeather(new MissionWeather.State(EvidenceSignals.Season.WINTER,
            EvidenceSignals.Weather.SNOW,MissionWeather.Mode.FIXED,2,0));
        render.drawNetwork(map,List.of(),-1,2.5);
        ok(render.weatherModule().weather().state().snow()>0,
            "Snow does not reach live world renderer");
        System.out.println("PASS all original media, weather geometry, snow, roof coverage and thunder");
    }
}
