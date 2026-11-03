package org.beckenburg;

import java.util.concurrent.*;

/** Four real LAN clients prove spatial clue range and a recoverable, successful case. */
public final class LanSpatialEvidenceIntegrationTest {
    static void ok(boolean value,String name){if(!value)throw new AssertionError(name);}
    static final class Peer implements AutoCloseable {
        final BlockingQueue<String> queue=new LinkedBlockingQueue<>();
        final LanClient client;
        String lastBody;
        Peer(int port,boolean ghost,String name)throws Exception{
            client=new LanClient("127.0.0.1",port,ghost,name,queue::offer);
        }
        String await(String prefix)throws Exception{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<end){
                String s=queue.poll(80,TimeUnit.MILLISECONDS);if(s==null)continue;
                if(!"GHOST".equals(client.seat())&&s.startsWith("BODY|"+(Integer.parseInt(client.seat().substring(1))-1)+"|"))lastBody=s;
                if(s.startsWith("REJECT|")&&prefix.startsWith("ACCEPT|"))
                    throw new AssertionError(client.seat()+" rejected "+prefix+": "+s);
                if("GHOST".equals(client.seat())&&(s.startsWith("BODY|")
                    ||s.startsWith("PROGRESS|")||s.startsWith("TEAMMAP|")
                    ||s.startsWith("EVIDENCE|")
                    ||s.startsWith("CASEFILE|")))
                    throw new AssertionError("ghost received hunter-private state: "+s);
                if(s.startsWith(prefix))return s;
            }
            throw new AssertionError("missing "+prefix+" on "+client.seat());
        }
        void room(int r)throws Exception{LanTestRoomTravel.move(client,this::await,r);}
        void step(int dx,int dz)throws Exception{
            Thread.sleep(135);client.step(dx,dz);
            try{await("ACCEPT|STEP");}catch(AssertionError failure){throw new AssertionError("Motion "+dx+","+dz+" from "+lastBody+": "+failure.getMessage(),failure);}
            lastBody=await("BODY|"+(Integer.parseInt(client.seat().substring(1))-1)+"|");
        }
        LanWorld.Body body(HouseMap map){
            var fields=lastBody.split("\\|");double x=Integer.parseInt(fields[2])/1000.0,
                y=Integer.parseInt(fields[3])/1000.0,z=Integer.parseInt(fields[4])/1000.0;
            return new LanWorld.Body(x,y,z,Integer.parseInt(fields[5]),map.roomAt(x,y,z));
        }
        public void close(){client.close();}
    }
    public static void main(String[] args)throws Exception{
        try(LanHost host=new LanHost("127.0.0.1",0);
            Peer hunter=new Peer(host.port(),false,"Cleric");
            Peer other=new Peer(host.port(),false,"Observer");
            Peer third=new Peer(host.port(),false,"Demonologist");
            Peer ghost=new Peer(host.port(),true,"Spirit")){
            hunter.client.ready();other.client.ready();third.client.ready();ghost.client.ready();
            hunter.await("LOBBY|3|HUMAN|4|WAITING");
            hunter.client.start();hunter.await("INFO|mission started");
            for(int r:new int[]{11,8,5,4})hunter.room(r);
            hunter.client.view3D();hunter.await("ACCEPT|VIEW3D");
            HouseMap geometry=new HouseMap();geometry.configureHiding(CustomContract.standard(3).hidingSpots());
            ok(hunter.await("SITE|").equals("SITE|HOUSE|"+geometry.level().revision()),"Fixture must match actual host scene revision");
            hunter.lastBody=hunter.await("BODY|0|");
            // The human ghost must actually disturb the room to create >6 EMF.
            ghost.client.ghost(GhostDomain.Action.DISTURB,4);
            ghost.await("ACCEPT|GHOST|DISTURB|");
            hunter.client.scan(LanCase.Tool.EMF);
            ok(hunter.await("ACCEPT|SCAN|READING").contains("EMF"),"event-driven EMF >6");
            // A 3.1m blind +X line now strikes an authored wall. Choose a real,
            // collision-validated 2.17m corridor beyond the Spirit Box's 2.1m reach.
            hunter.lastBody=hunter.await("BODY|0|");
            var initial=hunter.body(geometry);
            ok(initial.room()==4,"Authoritative hunter body reached event room");
            int travelX=0,travelZ=0;
            for(int[] candidate:new int[][]{{-100,0},{100,0},{0,-100},{0,100}}){
                var at=new LevelDefinition.Vec(initial.x(),initial.y(),initial.z());boolean clear=true;
                for(int i=0;i<7;i++){
                    var next=geometry.resolveMove(at,at.x()+candidate[0]*.0031,
                        at.z()+candidate[1]*.0031);
                    if(next==null||geometry.roomAt(next.x(),next.y(),next.z())!=4
                        ||Math.abs(next.y()-initial.y())>.01){clear=false;break;}
                    at=next;
                }
                if(clear&&Math.hypot(at.x()-geometry.centerX(4),at.z()-geometry.centerZ(4))
                        >LanCase.Tool.SPIRIT_BOX.sensingRadius()){
                    travelX=candidate[0];travelZ=candidate[1];break;
                }
            }
            ok(travelX!=0||travelZ!=0,"room 4 has an accessible corridor beyond Spirit Box event range");
            for(int i=0;i<7;i++)hunter.step(travelX,travelZ);
            var far=hunter.body(geometry);
            ok(far.room()==4&&Math.abs(far.y()-initial.y())<.01
                &&Math.hypot(far.x()-geometry.centerX(4),far.z()-geometry.centerZ(4))>LanCase.Tool.SPIRIT_BOX.sensingRadius(),
                "Actual host body, on same room/floor, moved beyond Spirit Box event reach");
            hunter.client.scan(LanCase.Tool.SPIRIT_BOX);
            ok(hunter.await("ACCEPT|SCAN|READING").contains("Radio static; no intelligible voice"),
                "spirit box remains operational and reads silence away from the source");
            hunter.client.scan(LanCase.Tool.THERMOMETER);
            ok(hunter.await("ACCEPT|SCAN|READING").contains("THERMOMETER"),
                "thermometer reports within its practical range");
            for(int i=0;i<7;i++)hunter.step(-travelX,-travelZ);
            var returned=hunter.body(geometry);
            ok(returned.room()==4&&Math.abs(returned.y()-initial.y())<.01
                &&Math.hypot(returned.x()-initial.x(),returned.z()-initial.z())<.01,
                "Host movement returns to the actual original measurement point");
            hunter.client.scan(LanCase.Tool.SPIRIT_BOX);
            ok(hunter.await("ACCEPT|SCAN|READING").contains("SPIRIT_BOX"),
                "approaching the source permits another sample");
            hunter.client.ward();hunter.await("ACCEPT|WARD|ANCHOR WARD PREPARED");
            hunter.client.cleanse();hunter.await("ACCEPT|CLEANSE|RITUAL STARTED");
            hunter.await("RESULT|CLEANSED");
            ghost.await("RESULT|CLEANSED");
            ok(!host.running(),"spatial tool gating still permits completing the actual case");
            System.out.println("PASS: four-peer spatial tool range, in-range proof, ghost privacy and defended resolution");
        }
    }
}
