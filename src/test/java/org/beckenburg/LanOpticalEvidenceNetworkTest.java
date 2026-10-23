package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** Real TCP: UV power/marks/empty battery and physical laser deployment, with ghost redaction. */
public final class LanOpticalEvidenceNetworkTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        try(var host=new LanHost("127.0.0.1",0,GhostIdentity.Type.SPIRIT);
            var a=new AuthoredLevelNetworkTest.Peer(host.port(),false,"UVTech");
            var b=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Observer");
            var c=new AuthoredLevelNetworkTest.Peer(host.port(),false,"Photographer");
            var ghost=new AuthoredLevelNetworkTest.Peer(host.port(),true,"Spirit")){
            a.client.ready();b.client.ready();c.client.ready();ghost.client.ready();a.await("LOBBY|3|HUMAN|4|WAITING");a.client.start();a.await("INFO|mission started");
            a.client.view3D();a.await("ACCEPT|VIEW3D");
            var map=(HouseMap)LooseClutterNetworkTest.field(host,"map");var world=(LanWorld)LooseClutterNetworkTest.field(host,"world");var mutex=LooseClutterNetworkTest.field(host,"mutex");
            var pose=((LanGhostSpatial)LooseClutterNetworkTest.field(host,"ghostPose")).pose();
            Vec view=null;
            for(double[] delta:new double[][]{{0,-1.4},{0,1.4},{-1.4,0},{1.4,0}}){
                double x=pose.x()+delta[0],z=pose.z()+delta[1];
                if(map.traversable(x,pose.y(),z)&&map.clearLineOfSight(x,pose.y()+1.62,z,pose.x(),pose.y()+.08,pose.z())){view=new Vec(x,pose.y(),z);break;}
            }
            check(view!=null,"actual ghost position has a clear inspection approach");
            synchronized(mutex){AuthoredTestSupport.place(map,world,0,view);}
            a.client.turn(Math.floorMod((int)Math.round(Math.toDegrees(Math.atan2(pose.x()-view.x(),pose.z()-view.z()))),360));
            a.client.selectGearSlot(3);a.await("GEARSTATE|");a.client.useHeldEquipment();a.await("GEARFX|0|UV_LIGHT|1");
            String trace;
            do{trace=a.await("UVTRACE|");}while(trace.equals("UVTRACE|RESET"));
            var mark=UvEvidenceMarks.Mark.decode(LanProtocol.parts(trace));check(mark.room()==pose.room(),"host sends a genuine local surface mark");
            synchronized(mutex){
                var kits=(FieldEquipment[])LooseClutterNetworkTest.field(host,"equipment");
                var battery=(Map<String,Integer>)LooseClutterNetworkTest.field(kits[0],"batteryTicks");battery.put("UV_LIGHT",1);
            }
            a.await("GEARFX|0|UV_LIGHT|0");check(ghost.inbox.stream().noneMatch(s->s.startsWith("UVTRACE|")||s.startsWith("LASER|")||s.startsWith("GEARFX|")),"ghost gets no investigator optics/trace packets");
            var rack=map.van().location(VanStaging.Station.EQUIPMENT);
            synchronized(mutex){AuthoredTestSupport.place(map,world,0,new Vec(rack.x(),VanStaging.FLOOR_Y,rack.z()));}
            a.client.equipAt(0,"LASER_GRID");a.await("ACCEPT|GEAREQUIP");
            synchronized(mutex){AuthoredTestSupport.place(map,world,0,map.spawn(2));}
            a.client.useHeldEquipment();check(a.await("SENSOR|PLACE|").contains("|LASER_GRID|2|"),"laser has an actual replicated installation");
            a.await("LASER|POWER|0|1");
            String reading=a.await("SENSOR|READ|");check(reading.contains("Laser beams aligned")||reading.contains("Grid displacement"),"laser station must not fall through to a thermometer reading");
            System.out.println("PASS TCP UV power, genuine positioned trace, exhaustion blackout, ghost privacy and physical powered laser installation");
        }
    }
}
