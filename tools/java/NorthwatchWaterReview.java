package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** Reviews the modeled basins through the production fixture and water effects. */
final class NorthwatchWaterReview {
    private static final String ROUTE="Northwatch-switch-";
    private NorthwatchWaterReview() {}

    static void audit(HouseMap map,Map<String,List<Vec>> routes,Path captures,List<String> report)throws Exception {
        var fixtures=map.fixtures();
        var sinks=fixtures.states().stream().filter(f->f.kind()==HauntedFixtures.Kind.SINK_BATH).toList();
        require(sinks.size()==2,"Both native wash basins must be bound to the shared fixture state");
        fixtures.power(false);
        for(var f:sinks) {
            String key=f.room()==5?"shed-wash-basin":"lab-wash-basin";
            var way=routes.get(ROUTE+key);
            Vec feet=way.get(way.size()-1);
            require(map.roomAt(feet.x(),feet.y(),feet.z())==f.room(),"Wrong operating room");
            var look=aim(f,feet);
            var target=InteractionFocus.pick(map,look,List.of());
            require(target!=null&&target.kind()==InteractionFocus.Kind.FIXTURE&&target.id().equals(""+f.id()),
                "Actual basin could not be aimed at from "+feet+" / "+target);
            Path dry=captures.resolve("northwatch-"+key+"-dry.png");
            Path wet=captures.resolve("northwatch-"+key+"-running.png");
            render(map,look,dry);
            fixtures.toggleFocused(f.id(),look,1);
            boolean sound=false;
            for(int tick=1;tick<=135;tick++)
                sound|=fixtures.advance(tick).stream().anyMatch(cue->cue.source().id()==f.id()
                    &&cue.cue()==LanSound.Cue.RUNNING_WATER);
            require(sound&&fixtures.state(f.id()).water()==810,"Water did not fill or emit its ordinary sound with power off");
            render(map,look,wet);
            var dryImage=ImageIO.read(dry.toFile());
            var wetImage=ImageIO.read(wet.toFile());
            int changed=0;
            for(int y=0;y<dryImage.getHeight();y++)for(int x=0;x<dryImage.getWidth();x++)
                if(dryImage.getRGB(x,y)!=wetImage.getRGB(x,y))changed++;
            require(changed>=60,"Running water was not visible in "+key+": "+changed);
            fixtures.toggleFocused(f.id(),look,136);
            for(int tick=136;tick<=220;tick++)fixtures.advance(tick);
            require(!fixtures.state(f.id()).active()&&fixtures.state(f.id()).water()==0,"Basin did not drain");
            report.add(key+"_water\tPASS fill, shutoff, drain, sound and "+changed+" changed render pixels with mains off");
        }
    }

    static Vec liveShed(HouseMap map,Peer leader,Peer witness,Vec at,Map<String,List<Vec>> routes,
                         List<String> report)throws Exception {
        witness.client.view3D();
        witness.awaitExact("ACCEPT|VIEW3D");
        Vec returnTo=at;
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(11.9,0,11.03),new Vec(11,0,13),new Vec(11,0,16));
        at=use(map,leader,witness,at,routes.get(ROUTE+"shed-wash-basin"),5);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(5.6,0,10.1),new Vec(5.6,0,15),new Vec(7,0,16),
            new Vec(11,0,16),new Vec(11,0,13),new Vec(11.9,0,11.03),returnTo);
        report.add("shed-wash-basin_live\tPASS aimed fill, shutoff and drained witness state from the actual shed aisle");
        return at;
    }

    static Vec liveLab(HouseMap map,Peer leader,Peer witness,Vec at,Map<String,List<Vec>> routes,
                        List<String> report)throws Exception {
        Vec returnTo=at;
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(8.8,0,29.25));
        at=use(map,leader,witness,at,routes.get(ROUTE+"lab-wash-basin"),0);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(7,0,31.65),new Vec(7,0,29.25),new Vec(8.8,0,29.25),returnTo);
        report.add("lab-wash-basin_live\tPASS aimed fill, shutoff and drained witness state from the fitted lab");
        return at;
    }

    private static Vec use(HouseMap map,Peer leader,Peer witness,Vec at,List<Vec> path,int owner)throws Exception {
        for(var point:path)at=CemeteryRefugeReview.walk(leader,0,at,point);
        var f=map.fixtures().states().stream().filter(item->item.room()==owner).findFirst().orElseThrow();
        var look=aim(f,at);
        if(f.active()){
            click(map,leader,look,f.id());
            Thread.sleep(400);
        }
        click(map,leader,look,f.id());
        long until=System.nanoTime()+5_000_000_000L;
        String frame;
        do {
            frame=witness.await("FIXTURE|"+f.id()+"|1|");
            require(System.nanoTime()<until,"Active basin never filled on the witness");
        } while(Integer.parseInt(frame.split("\\|")[3])<12);
        Thread.sleep(400);
        click(map,leader,look,f.id());
        until=System.nanoTime()+5_000_000_000L;
        do {
            frame=witness.await("FIXTURE|"+f.id()+"|0|");
            require(System.nanoTime()<until,"Basin never drained on the witness");
        } while(Integer.parseInt(frame.split("\\|")[3])!=0);
        require(!map.fixtures().state(f.id()).active(),"Host retained running water after shutoff");
        return at;
    }

    private static void click(HouseMap map,Peer leader,LooseClutter.Observer look,int id)throws Exception {
        var target=InteractionFocus.pick(map,look,List.of());
        require(target!=null&&target.kind()==InteractionFocus.Kind.FIXTURE&&target.id().equals(""+id),
            "Live aim missed the physical basin: "+target);
        leader.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(look.yaw())),360));
        leader.client.interact(target,look.yaw(),look.pitch());
        leader.awaitExact("ACCEPT|INTERACT|"+target.name());
    }

    private static LooseClutter.Observer aim(HauntedFixtures.State f,Vec feet) {
        double dx=f.x()-feet.x(),dz=f.z()-feet.z();
        double yaw=Math.atan2(dx,dz);
        double pitch=Math.atan2(f.y()-.018-feet.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        return new LooseClutter.Observer(0,feet.x(),feet.y(),feet.z(),yaw,pitch,true);
    }

    private static void render(HouseMap map,LooseClutter.Observer look,Path file)throws Exception {
        var view=new SceneRenderer();
        view.setRenderSize(880,580);
        view.setPsxPreset(PsxPipeline.Preset.OFF);
        view.roomLights=new RoomLighting();
        view.roomLights.setBreaker(false);
        view.x=look.x();
        view.z=look.z();
        view.eyeHeight=look.eyeY();
        view.yaw=look.yaw();
        view.pitch=look.pitch();
        view.lamp=true;
        view.setLampHardware(EquipmentTiers.Tier.III,false);
        NativeSchoolInteriorReview.drawFrame(view,map,file);
    }

    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
