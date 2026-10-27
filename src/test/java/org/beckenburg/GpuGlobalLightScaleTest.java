package org.beckenburg;

import java.util.List;
import dev.mrcalzon.weather.WeatherType;

/** Real scene assembly carries global lighting without altering retained vertices. */
public final class GpuGlobalLightScaleTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        var backend=new LiveHouseResidencyTest.Backend();GpuWorldRenderer.install(backend);
        try{
            var map=new HouseMap();var renderer=new SceneRenderer();renderer.setResidentHouse(true);
            renderer.setRenderSize(160,120);renderer.roomLights=new RoomLighting();
            LiveHouseResidencyTest.pose(renderer,map,0,4);
            for(int i=0;i<4;i++){renderer.drawNetwork(map,List.of(),-1,0);backend.complete();}
            var stable=renderer.liveHouseStats();
            for(double scale:new double[]{.25,0,1.4}){
                renderer.environmentLight=scale;
                renderer.weatherModule().setWeather(WeatherType.DENSE_FOG,0);
                renderer.drawNetwork(map,List.of(),-1,0);backend.complete();
                var weather=renderer.weatherModule().weather().state();
                double expected=scale*(Math.max(.50,1-(1-weather.visibility())*.24)+weather.flash()*2.2);
                check(Math.abs(backend.submitted.lightScale()-expected)<1e-6,"environment and weather scale survive GPU submission");
                var now=renderer.liveHouseStats();
                check(now.builds()==stable.builds()&&now.packedChunks()==stable.packedChunks()
                    &&now.recomputedProbes()==stable.recomputedProbes()&&now.descriptorBytes()==0,
                    "global brightness changes only a uniform, never static lighting or geometry");
            }
            renderer.close();
            System.out.println("PASS normal House global/weather light scale, darkness, brightening and zero static rebuild/repack");
        }finally{GpuWorldRenderer.INSTALLED.set(null);}
    }
}
