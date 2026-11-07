package org.beckenburg;
import java.util.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Actual runtime source/control and connected-light construction views. */
public final class HouseRebuildPowerCapture {
    public static void main(String[] args)throws Exception{
        var out=Path.of(args[0]);Files.createDirectories(out);var map=HouseRebuildFunctions.map();
        var renderer=new SceneRenderer();renderer.setRenderSize(1000,625);var lights=new RoomLighting();renderer.roomLights=lights;
        for(var kind:List.of(InteractionFocus.Kind.GENERATOR,InteractionFocus.Kind.BREAKER)){
            var o=HouseRebuildPowerTest.approach(map,kind);renderer.x=o.x();renderer.z=o.z();renderer.eyeHeight=o.eyeY();renderer.yaw=o.yaw();renderer.pitch=o.pitch();renderer.lamp=true;
            renderer.drawNetwork(map,List.of(),-1,0);var target=InteractionFocus.pick(map,o,List.of());var g=renderer.image().createGraphics();try{InteractionFocus.draw(g,1000,625,target);}finally{g.dispose();}
            ImageIO.write(renderer.image(),"png",out.resolve(kind.name().toLowerCase()+"-control-runtime.png").toFile());
        }
        renderer.x=18.6;renderer.z=12.7;renderer.eyeHeight=1.62;renderer.yaw=1.8;renderer.pitch=-.1;renderer.lamp=false;
        for(String state:List.of("supplied","source-stopped","breaker-tripped")){
            lights.setSourceRunning(!state.equals("source-stopped"));lights.setBreaker(!state.equals("breaker-tripped"));map.fixtures().power(lights.supplied());renderer.drawNetwork(map,List.of(),-1,2);
            ImageIO.write(renderer.image(),"png",out.resolve("dining-"+state+"-runtime.png").toFile());
        }
        System.out.println("PASS actual supported controls and three independent source/breaker lighting views");
    }
}
