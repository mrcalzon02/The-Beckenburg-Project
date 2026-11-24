package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

final class NorthwatchDistributionReview {
    private record Circuit(String room, int owner, Vec viewpoint) {}
    private static final List<Circuit> CIRCUITS=List.of(
        new Circuit("lab",0,new Vec(8,0,31)),
        new Circuit("radio",3,new Vec(22,0,18.3)),
        new Circuit("records",4,new Vec(34.2,0,18.5)));

    private NorthwatchDistributionReview() {}

    static void audit(HouseMap map,Map<String,List<Vec>> routes,Path captures,List<String> report)throws Exception {
        var lighting=new RoomLighting();
        var fixtures=new FixtureLighting(map);
        for(var circuit:CIRCUITS) {
            int owner=circuit.owner();
            Socket control=map.level().socket("switch",owner);
            Socket lamp=map.level().socket("light",owner);
            var route=routes.get("Northwatch-switch-"+circuit.room());
            Vec feet=route.get(route.size()-1);
            require(control!=null&&lamp!=null&&circuit.room().equals(map.architecturalRoomAt(feet.x(),feet.y(),feet.z())),
                "Circuit must have a supported operating stance in "+circuit.room());
            require(map.traversable(circuit.viewpoint().x(),circuit.viewpoint().y(),circuit.viewpoint().z(),
                HunterPosture.STANDING_HEIGHT),"Capture eye is inside an obstruction in "+circuit.room());
            var target=WharfPowerChecks.focus(map,feet,control);
            require(target.kind()==InteractionFocus.Kind.SWITCH&&target.id().equals(Integer.toString(owner)),
                "Aim did not resolve to the "+circuit.room()+" switch");
            double supplied=energy(fixtures,lighting,lamp);
            lighting.toggle(owner);
            double switched=energy(fixtures,lighting,lamp);
            for(int other=0;other<HouseMap.INDOOR_ROOMS;other++)
                require(lighting.on(other)==(other!=owner),"A local switch affected another circuit");
            lighting.toggle(owner);
            lighting.setSourceRunning(false);
            double stopped=energy(fixtures,lighting,lamp);
            lighting.setSourceRunning(true);
            Path on=captures.resolve("northwatch-"+circuit.room()+"-supplied.png");
            Path off=captures.resolve("northwatch-"+circuit.room()+"-tripped.png");
            render(map,lighting,circuit,lamp,on);
            lighting.setBreaker(false);
            double tripped=energy(fixtures,lighting,lamp);
            render(map,lighting,circuit,lamp,off);
            lighting.setBreaker(true);
            require(supplied>1.5*switched && Math.abs(stopped-tripped)<.001 && supplied>2*tripped,
                "Wrong branch response in "+circuit.room()+": "+supplied+" / "+switched+" / "+stopped+" / "+tripped);
            assertVisible(on,off,circuit,report);
            report.add(circuit.room()+"_circuit_energy\t"+supplied+" supplied, "+switched+" switch off, "
                +stopped+" source stopped, "+tripped+" breaker tripped");
        }
    }

    static void live(HouseMap map,HouseMap replica,NativeInvestigationScene scene,LanHost host,Peer leader,
                     Peer witness,Vec at,Map<String,List<Vec>> routes,List<String> report)throws Exception {
        var lighting=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(11.9,0,11.03),new Vec(11,0,13),
            new Vec(11,0,16),new Vec(7,0,16),new Vec(7,0,25.05));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,840);
        at=switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(0),report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(8.8,0,29.25),new Vec(7,0,29.25),new Vec(7,0,16),new Vec(11,0,16),
            new Vec(13.35,0,16.05),new Vec(13.35,0,19));
        at=switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(1),report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(21.44,0,18.4),new Vec(23,0,19),new Vec(31.3,0,19));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,846);
        switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(2),report);
    }

    private static Vec switchRoom(HouseMap map,Peer leader,Peer witness,RoomLighting lights,Vec at,
                                  Map<String,List<Vec>> routes,Circuit circuit,List<String> report)throws Exception {
        for(Vec point:routes.get("Northwatch-switch-"+circuit.room()))
            at=CemeteryRefugeReview.walk(leader,0,at,point);
        var contact=map.level().socket("switch",circuit.owner());
        var target=WharfPowerChecks.focus(map,at,contact);
        require(target.kind()==InteractionFocus.Kind.SWITCH&&target.id().equals(Integer.toString(circuit.owner())),
            "Live aim missed "+circuit.room());
        double dx=contact.x()-at.x(),dz=contact.z()-at.z();
        double yaw=Math.atan2(dx,dz),pitch=Math.atan2(contact.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz));
        var after=new StringBuilder(lights.snapshot());
        require(after.charAt(circuit.owner())=='1',"Room circuit was already off before live interaction");
        after.setCharAt(circuit.owner(),'0');
        leader.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360));
        leader.client.interact(target,yaw,pitch);
        leader.awaitExact("ACCEPT|INTERACT|"+target.name());
        witness.awaitExact("LIGHTS|"+after+"|1");
        require(lights.supplied()&&lights.snapshot().contentEquals(after),"Host changed another branch during live switching");
        report.add(circuit.room()+"_live_aimed_switch\tPASS through physical door; witness received only the selected branch change");
        return at;
    }

    private static double energy(FixtureLighting fixtures,RoomLighting lighting,Socket lamp) {
        fixtures.frame(lighting,0);
        return fixtures.sample(lamp.room(),lamp.x(),lamp.y()-.60,lamp.z()).energy();
    }

    private static void render(HouseMap map,RoomLighting lighting,Circuit circuit,Socket lamp,Path destination)throws Exception {
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=lighting;
        renderer.x=circuit.viewpoint().x();
        renderer.z=circuit.viewpoint().z();
        renderer.eyeHeight=circuit.viewpoint().y()+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.atan2(lamp.x()-renderer.x,lamp.z()-renderer.z);
        renderer.pitch=.09;
        NativeSchoolInteriorReview.drawFrame(renderer,map,destination);
    }

    private static void assertVisible(Path onFile,Path offFile,Circuit circuit,List<String> report)throws Exception {
        var on=ImageIO.read(onFile.toFile());
        var off=ImageIO.read(offFile.toFile());
        require(on.getWidth()==off.getWidth()&&on.getHeight()==off.getHeight(),"Mismatched capture dimensions");
        int count=0;
        long sum=0;
        for(int y=0;y<on.getHeight();y++)for(int x=0;x<on.getWidth();x++) {
            int a=on.getRGB(x,y),b=off.getRGB(x,y);
            if(a!=b)count++;
            for(int c=0;c<24;c+=8)sum+=((a>>c)&255)-((b>>c)&255);
        }
        double mean=sum/(3.0*on.getWidth()*on.getHeight());
        require(count>15_000&&mean>.15,"Blackout has no visible contrast in "+circuit.room()+" "+count+" / "+mean);
        report.add(circuit.room()+"_visible_blackout\t"+count+" changed pixels; "+mean+" mean RGB increase");
    }

    private static void require(boolean pass,String message) {
        if(!pass)throw new AssertionError(message);
    }
}
