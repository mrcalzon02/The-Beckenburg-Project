package org.beckenburg;

/** A single physical doorway controls reciprocal optical and acoustic transfer.
 * Optical output is diagnostic until screenshot and GPU p95/p99 parity approval. */
public final class DoorwayTransmissionTest {
    private static void ok(boolean value,String message){
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args){
        var map=new HouseMap(HouseMap.Site.HOUSE);
        var portal=AuthoredTestSupport.door(map,3,4);
        var sourceSide=AuthoredTestSupport.side(map,portal,3,.78);
        var receiverSide=AuthoredTestSupport.side(map,portal,4,.78);
        var a=new FixtureLighting.Source(0,FixtureLighting.Kind.BARE_BULB,3,0,
                sourceSide.x(),portal.y()+1.35,sourceSide.z(),
                0,-1,0,6,4,1,.75,.55);
        var b=new FixtureLighting.Source(1,FixtureLighting.Kind.BARE_BULB,4,0,
                receiverSide.x(),portal.y()+1.35,receiverSide.z(),
                0,-1,0,6,4,.6,.85,1);
        ok(DoorwayTransmission.opticalTransmission(map,portal,3,4)==0,
                "Opaque closed door must not leak optical energy");
        ok(DoorwayTransmission.projectedLight(map,portal,3,4,a,
                receiverSide.x(),portal.y()+1.35,receiverSide.z(),1).energy()==0,
                "No virtual light through closed door");
        int closedLoss=DoorwayTransmission.soundLoss(map,portal,3,4);
        ok(closedLoss==DoorwayTransmission.soundLoss(map,portal,4,3),
                "Closed door sound loss must be reciprocal");
        map.toggleDoor(portal.id());
        ok(DoorwayTransmission.opticalTransmission(map,portal,3,4)==1
                &&DoorwayTransmission.opticalTransmission(map,portal,4,3)==1,
                "One open doorway transmits both directions");
        var forward=DoorwayTransmission.projectedLight(map,portal,3,4,a,
                receiverSide.x(),portal.y()+1.35,receiverSide.z(),1);
        var reverse=DoorwayTransmission.projectedLight(map,portal,4,3,b,
                sourceSide.x(),portal.y()+1.35,sourceSide.z(),1);
        ok(forward.energy()>0&&reverse.energy()>0,
                "Physical doorway must transfer real incident light in both directions");
        ok(forward.red()>forward.blue()&&reverse.blue()>reverse.red(),
                "Doorway must preserve its source spectrum, not generate neutral light");
        ok(DoorwayTransmission.projectedLight(map,portal,3,4,a,
                receiverSide.x(),portal.y()+1.35,receiverSide.z(),0).energy()==0,
                "Disabled light cannot generate portal flux");
        int openLoss=DoorwayTransmission.soundLoss(map,portal,3,4);
        ok(closedLoss-openLoss==2&&openLoss==LanAcoustics.physicalEdgeCost(map,3,4),
                "Sound attenuation must obey the same authored open threshold");
        map.toggleDoor(portal.id());
        ok(DoorwayTransmission.projectedLight(map,portal,4,3,b,
                sourceSide.x(),portal.y()+1.35,sourceSide.z(),1).energy()==0,
                "Shutting the shared doorway stops reverse optical transmission");
        ok(DoorwayTransmission.soundLoss(map,portal,3,4)==closedLoss,
                "Sound attenuation must restore on closing");
        System.out.println("PASS shared bidirectional doorway: optical transfer, colour, closure, source-off and acoustic loss");
    }
}
