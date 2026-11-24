package org.beckenburg;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.Socket;
import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;

/** The same supported caretaker plant is tested in both exact staged sites. */
final class CemeteryPowerReview {
    private static final double FLOOR=.20;
    private static final int OWNER=12;
    private static final Vec AISLE=new Vec(49.4,FLOOR,55.85);

    private CemeteryPowerReview() {}

    static void audit(HouseMap map,LevelDefinition level,NativeInvestigationScene scene,List<Vec> route,
                      Path captures,List<String> report,boolean gpu)throws Exception {
        var generator=level.socket("generator",OWNER);
        var breaker=RoomLighting.breaker(map);
        var lamp=level.socket("light",OWNER);
        require(generator!=null&&breaker!=null&&breaker.room()==OWNER&&lamp!=null,"Missing caretaker plant fixtures");
        require(route.get(route.size()-1).equals(AISLE),"Service approach missed the operator aisle");
        for(var control:List.of(generator,breaker)){
            var feet=operator(control);
            require("caretaker-plant".equals(map.architecturalRoomAt(feet.x(),feet.y(),feet.z())),
                "Control stance is outside the actual shed");
            for(double height:new double[]{HunterPosture.STANDING_HEIGHT,HunterPosture.CROUCH_HEIGHT})
                NativeSchoolInteriorReview.walk(map,List.of(AISLE,feet),height,control.id()+" operating stance");
            require(WharfPowerChecks.focus(map,feet,control).id().equals(control.id()),
                "Aimed control is obstructed "+control.id());
        }

        var lights=new RoomLighting();
        var fixtures=new FixtureLighting(map);
        double[] values=new double[3];
        for(int state=0;state<3;state++){
            lights.setSourceRunning(state!=1);
            lights.setBreaker(state!=2);
            fixtures.frame(lights,0);
            values[state]=fixtures.sample(lamp.room(),lamp.x(),lamp.y()-.62,lamp.z()).energy();
        }
        require(values[0]>2*values[1]&&Math.abs(values[1]-values[2])<.001,
            "Caretaker lamp failed to return to the same ambient floor after both cutoffs");
        report.add("caretaker_lamp_energy\t"+values[0]+" supplied, "+values[1]+" source stopped, "+values[2]+" breaker tripped");
        var on=captures.resolve("caretaker-power-supplied.png");
        var off=captures.resolve("caretaker-power-tripped.png");
        lights.setBreaker(true);
        render(map,lights,on);
        lights.setBreaker(false);
        render(map,lights,off);
        contrast(on,off,report);
        if(!gpu)live(level,scene,route,generator,breaker,report);
    }

    private static void render(HouseMap map,RoomLighting lights,Path image)throws Exception{
        var renderer=new SceneRenderer();
        renderer.setRenderSize(880,580);
        renderer.setPsxPreset(PsxPipeline.Preset.OFF);
        renderer.roomLights=lights;
        renderer.x=49.4;
        renderer.z=58.66;
        renderer.eyeHeight=map.groundHeight(renderer.x,renderer.z,OWNER)+HunterPosture.STANDING_EYE;
        renderer.yaw=Math.PI;
        renderer.pitch=-.11;
        NativeSchoolInteriorReview.drawFrame(renderer,map,image);
    }

    private static void contrast(Path lit,Path dark,List<String> report)throws Exception{
        var on=ImageIO.read(lit.toFile());
        var off=ImageIO.read(dark.toFile());
        require(on.getWidth()==off.getWidth()&&on.getHeight()==off.getHeight(),"Unequal service captures");
        long gained=0;
        int count=0;
        for(int y=0;y<on.getHeight();y++)for(int x=0;x<on.getWidth();x++){
            int a=on.getRGB(x,y),b=off.getRGB(x,y);
            if(a!=b)count++;
            for(int channel=0;channel<24;channel+=8)gained+=((a>>channel)&255)-((b>>channel)&255);
        }
        double mean=gained/(3.0*on.getWidth()*on.getHeight());
        require(count>25_000&&mean>.25,"Caretaker power had no visible operating contrast "+count+" / "+mean);
        report.add("caretaker_lamp_visible_response\t"+count+" changed pixels; "+mean+" mean RGB increase");
    }

    private static void live(LevelDefinition level,NativeInvestigationScene scene,List<Vec> route,Socket generator,
                             Socket breaker,List<String> report)throws Exception{
        var map=new HouseMap(HouseMap.Site.USER,level,null,scene);
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT,CustomContract.standard(3),map);
            var operator=new Peer(host,false,"Caretaker electrician");
            var witness=new Peer(host,false,"Caretaker witness");
            var reconnecting=new Peer(host,false,"Caretaker crew");
            var ghost=new Peer(host,true,"Cemetery ghost")){
            for(var peer:List.of(operator,witness,reconnecting,ghost)){
                peer.awaitExact("SITE|USER|"+level.revision());
                peer.client.ready();
            }
            operator.awaitExact("LOBBY|3|HUMAN|4|WAITING");
            operator.client.start();
            operator.await("INFO|mission started");
            operator.client.view3D();
            operator.awaitExact("ACCEPT|VIEW3D");
            var feet=NativeSchoolInteriorReview.body(operator.await("BODY|0|"));
            require(map.roomAt(feet.x(),feet.y(),feet.z())==14,"Caretaker live approach did not begin in the van");
            ghost.client.viewGhost3D();
            ghost.awaitExact("ACCEPT|GVIEW3D");
            ghost.await("GPOSE|");
            NativeSchoolInteriorReview.ghostTrip(host,witness,ghost);
            for(int i=0;i<15;i++)feet=NativeSchoolInteriorReview.step(operator,0,0,-100);
            for(var point:route)feet=CemeteryRefugeReview.walk(operator,0,feet,point);
            feet=CemeteryRefugeReview.walk(operator,0,feet,operator(generator));

            var lights=(RoomLighting)NativeSchoolInteriorReview.field(host,"lights");
            WharfPowerChecks.use(map,operator,feet,generator);
            witness.awaitExact("SOURCE|0");
            require(!lights.supplied()&&!lights.breaker()&&!lights.sourceRunning(),"Generator stop lost trip state");
            Thread.sleep(400);
            WharfPowerChecks.use(map,operator,feet,generator);
            witness.awaitExact("SOURCE|1");
            require(!lights.supplied()&&!lights.breaker()&&lights.sourceRunning(),"Generator restart reset the breaker");

            var ticket=reconnecting.client.resumeTicket();
            require(ticket!=null,"Missing caretaker reconnect ticket");
            ((java.net.Socket)NativeSchoolInteriorReview.field(reconnecting.client,"socket")).close();
            witness.await("PEER|LOST|2");
            var frames=new LinkedBlockingQueue<String>();
            try(var resumed=new LanClient("127.0.0.1",host.port(),ticket,frames::offer)){
                WharfPowerChecks.await(frames,"INFO|mission resumed");
                resumed.view3D();
                WharfPowerChecks.await(frames,"ACCEPT|VIEW3D");
                WharfPowerChecks.await(frames,"SITE|USER|"+level.revision());
                WharfPowerChecks.await(frames,"LIGHTS|111111111111|0");
                WharfPowerChecks.await(frames,"SOURCE|1");
            }
            feet=CemeteryRefugeReview.walk(operator,0,feet,AISLE);
            feet=CemeteryRefugeReview.walk(operator,0,feet,operator(breaker));
            WharfPowerChecks.use(map,operator,feet,breaker);
            witness.awaitExact("LIGHTS|111111111111|1");
            require(lights.supplied(),"Aimed reset did not restore caretaker supply");
        }
        report.add("four_peer_caretaker_supply\tPASS ghost trip, actual van-to-plant walk, aimed generator stop/restart, tripped reconnect and aimed breaker reset");
    }

    private static Vec operator(Socket control){
        return new Vec(control.x(),FLOOR,control.z()+1.18);
    }

    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
}
