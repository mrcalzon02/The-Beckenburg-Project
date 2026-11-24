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

    private static final Circuit STORE=new Circuit("store",6,new Vec(14,0,47));
    private static final Circuit DORM=new Circuit("dorm",1,new Vec(55,0,35));
    private static final Circuit MESS=new Circuit("mess",2,new Vec(55,0,46.5));
    private static final Circuit SHELTER=new Circuit("shelter",9,new Vec(64.4,0,13.4));
    private static final List<Circuit> SURFACE=List.of(STORE,DORM,MESS,SHELTER);
    private static final Circuit ARRAY=new Circuit("array",10,new Vec(47,0,12.6));
    private static final Circuit COURT=new Circuit("court",11,new Vec(24,0,29));
    private static final Circuit BRANCH=new Circuit("tunnel-n",8,new Vec(7,-2.5,24.7));
    private static final Circuit TUNNEL=new Circuit("tunnel",7,new Vec(27,-2.5,29));
    private static final List<Circuit> LOWER_AND_YARD=List.of(ARRAY,COURT,BRANCH,TUNNEL);
    private NorthwatchDistributionReview() {}

    static void audit(HouseMap map,Map<String,List<Vec>> routes,Path captures,List<String> report)throws Exception {
        var lighting=new RoomLighting();
        var fixtures=new FixtureLighting(map);
        var fitted=new java.util.ArrayList<>(CIRCUITS);
        boolean surface=routes.containsKey("Northwatch-switch-store");
        if(surface)fitted.addAll(SURFACE);
        boolean complete=routes.containsKey("Northwatch-switch-tunnel");
        if(complete)fitted.addAll(LOWER_AND_YARD);
        for(var circuit:fitted) {
            int owner=circuit.owner();
            Socket control=map.level().socket("switch",owner);
            Socket lamp=map.level().socket("light",owner);
            var route=routes.get("Northwatch-switch-"+circuit.room());
            Vec feet=route.get(route.size()-1);
            var space=map.nativeInvestigation().spaceAt(feet.x(),feet.y(),feet.z());
            require(control!=null&&lamp!=null&&space!=null&&space.id().equals(circuit.room())
                &&map.roomAt(feet.x(),feet.y(),feet.z())==owner,
                "Circuit must have a supported operating stance in "+circuit.room());
            require(map.traversable(circuit.viewpoint().x(),circuit.viewpoint().y(),circuit.viewpoint().z(),
                HunterPosture.STANDING_HEIGHT),"Capture eye is inside an obstruction in "+circuit.room());
            var target=WharfPowerChecks.focus(map,feet,control);
            require(target.kind()==InteractionFocus.Kind.SWITCH&&target.id().equals(Integer.toString(owner)),
                "Aim did not resolve to the "+circuit.room()+" switch");
            var lamps=map.level().sockets().stream().filter(s->s.type().equals("light")&&s.room()==owner).toList();
            double[] supplied=energies(fixtures,lighting,lamps);
            lighting.toggle(owner);
            double[] switched=energies(fixtures,lighting,lamps);
            for(int other=0;other<HouseMap.INDOOR_ROOMS;other++)
                require(lighting.on(other)==(other!=owner),"A local switch affected another circuit");
            lighting.toggle(owner);
            lighting.setSourceRunning(false);
            double[] stopped=energies(fixtures,lighting,lamps);
            lighting.setSourceRunning(true);
            boolean capture=!routes.containsKey("Northwatch-switch-shed-wash-basin")
                &&(complete?LOWER_AND_YARD.contains(circuit):!surface||SURFACE.contains(circuit));
            Path on=captures.resolve("northwatch-"+circuit.room()+"-supplied.png");
            Path off=captures.resolve("northwatch-"+circuit.room()+"-tripped.png");
            if(capture)render(map,lighting,circuit,lamp,on);
            lighting.setBreaker(false);
            double[] tripped=energies(fixtures,lighting,lamps);
            if(capture)render(map,lighting,circuit,lamp,off);
            lighting.setBreaker(true);
            for(int i=0;i<lamps.size();i++) {
                require(supplied[i]>1.5*switched[i] && Math.abs(stopped[i]-tripped[i])<.001 && supplied[i]>2*tripped[i],
                    "Wrong branch response in "+lamps.get(i).id()+": "+supplied[i]+" / "+switched[i]+" / "+stopped[i]+" / "+tripped[i]);
                report.add(lamps.get(i).id()+"_energy\t"+supplied[i]+" supplied, "+switched[i]+" switch off, "
                    +stopped[i]+" source stopped, "+tripped[i]+" breaker tripped");
            }
            if(capture)assertVisible(on,off,circuit,report);
            report.add(circuit.room()+"_circuit_energy\t"+lamps.size()+" fixtures independently switched and cut by source or breaker");
        }
    }

    static void live(HouseMap map,HouseMap replica,NativeInvestigationScene scene,LanHost host,Peer leader,
                     Peer witness,Vec at,Map<String,List<Vec>> routes,List<NorthwatchFurnishingReview.Record> stories,
                     List<String> report)throws Exception {
        var lighting=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(11.9,0,11.03),new Vec(11,0,13),
            new Vec(11,0,16),new Vec(7,0,16),new Vec(7,0,25.05));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,840);
        at=switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(0),report);
        if(routes.containsKey("Northwatch-switch-lab-wash-basin"))
            at=NorthwatchWaterReview.liveLab(map,leader,witness,at,routes,report);
        boolean surface=routes.containsKey("Northwatch-switch-store");
        if(surface) {
            var labFeet=at;
            at=NorthwatchDoorRuntime.walk(leader,at,new Vec(8.8,0,29.25),new Vec(7,0,29.25),
                new Vec(7,0,35),new Vec(10,0,35),new Vec(11,0,43.2));
            at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,842);
            at=switchRoom(map,leader,witness,lighting,at,routes,STORE,report);
            at=NorthwatchDoorRuntime.walk(leader,at,new Vec(13.05,0,47),new Vec(11,0,47),
                new Vec(11,0,45.32),new Vec(11,0,43.2),new Vec(10,0,35),new Vec(7,0,35),
                new Vec(7,0,29.25),new Vec(8.8,0,29.25),labFeet);
        }
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(8.8,0,29.25),new Vec(7,0,29.25),new Vec(7,0,16),new Vec(11,0,16),
            new Vec(13.35,0,16.05),new Vec(13.35,0,19));
        at=switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(1),report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(21.44,0,18.4),new Vec(23,0,19),new Vec(31.3,0,19));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,846);
        at=switchRoom(map,leader,witness,lighting,at,routes,CIRCUITS.get(2),report);
        if(!stories.isEmpty())at=NorthwatchFurnishingReview.readRecords(map,leader,at,routes,stories,report);
        if(surface)at=visitEast(map,replica,scene,leader,witness,lighting,at,routes,report);
        if(routes.containsKey("Northwatch-switch-tunnel"))
            visitLowerAndYard(map,replica,scene,leader,witness,lighting,at,routes,report);
    }

    private static Vec visitEast(HouseMap map,HouseMap replica,NativeInvestigationScene scene,Peer leader,
                                  Peer witness,RoomLighting lights,Vec at,Map<String,List<Vec>> routes,
                                  List<String> report)throws Exception {
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(33.44,0,18.4),new Vec(38,0,19),new Vec(38,0,24.7));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,847);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(38,0,41),new Vec(43.2,0,41));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,851);
        at=switchRoom(map,leader,witness,lights,at,routes,DORM,report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(46.7,0,38.9),new Vec(46.7,0,41),
            new Vec(53,0,41),new Vec(53,0,43.25));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,850);
        at=switchRoom(map,leader,witness,lights,at,routes,MESS,report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(54.85,0,47),new Vec(53,0,47),
            new Vec(53,0,45.31),new Vec(53,0,41),new Vec(55,0,41),
            new Vec(55,0,28),new Vec(53,0,28),new Vec(53,0,27.4));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,849);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(53,0,16),new Vec(53,0,13.5),
            new Vec(53.5,0,14),new Vec(61.1,0,14));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,853);
        return switchRoom(map,leader,witness,lights,at,routes,SHELTER,report);
    }

    private static void visitLowerAndYard(HouseMap map,HouseMap replica,NativeInvestigationScene scene,
               Peer leader,Peer witness,RoomLighting lights,Vec at,Map<String,List<Vec>> routes,List<String> report)throws Exception {
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(64.6,0,12.44),new Vec(64.6,0,14),new Vec(53.5,0,14),
            new Vec(53,0,16),new Vec(46.5,0,16));
        at=switchRoom(map,leader,witness,lights,at,routes,ARRAY,report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(46.2,0,12.76),new Vec(46.2,0,16),new Vec(44.9,0,16));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,848);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(38,0,16),new Vec(38,0,25.1),new Vec(38,0,40.8),
            new Vec(26,0,40.8),new Vec(26,0,27.2));
        at=switchRoom(map,leader,witness,lights,at,routes,COURT,report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(24.6,0,28.4),new Vec(26,0,28.4),new Vec(26,0,19),
            new Vec(13.35,0,19),new Vec(13.35,0,16.05),new Vec(11,0,16),new Vec(11,0,17),
            new Vec(11,-2.5,22.2),new Vec(11,-2.5,23.4));
        at=switchRoom(map,leader,witness,lights,at,routes,BRANCH,report);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(13.55,-2.5,23.4),new Vec(18.95,-2.5,23));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,855);
        at=NorthwatchDoorRuntime.walk(leader,at,new Vec(22,-2.5,23),new Vec(22,-2.5,25.12));
        at=NorthwatchDoorRuntime.open(map,replica,scene,leader,witness,at,854);
        switchRoom(map,leader,witness,lights,at,routes,TUNNEL,report);
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

    private static double[] energies(FixtureLighting fixtures,RoomLighting lighting,List<Socket> lamps) {
        fixtures.frame(lighting,0);
        return lamps.stream().mapToDouble(lamp->fixtures.sample(lamp.room(),lamp.x(),lamp.y()-.60,lamp.z()).energy()).toArray();
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
