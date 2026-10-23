package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
/** Native mesh bounds, six real capsule pads and swept deck/ramp/station routes. */
public final class VanHumanScaleTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static Vec walk(HouseMap map,Vec from,double localX,double localZ){
        var target=map.van().world(localX,localZ);int steps=(int)Math.ceil(Math.hypot(target.x()-from.x(),target.z()-from.z())/.04);
        Vec origin=from,at=from;
        for(int n=1;n<=steps;n++){
            var next=map.resolveMove(at,origin.x()+(target.x()-origin.x())*n/steps,origin.z()+(target.z()-origin.z())*n/steps);
            check(next!=null,"blocked human route at "+at+" toward local "+localX+","+localZ);at=next;
        }return at;
    }
    public static void main(String[]args){
        check(VanStaging.HALF_WIDTH*2<2.1,"van cannot be truck-width");
        check(Math.abs((VanStaging.REAR+VanStaging.CAB_LENGTH)-(VanStaging.FRONT-.18)-6.704)<.001,"overall road-vehicle length");
        for(var fixture:VanInteriorLayout.FIXTURES)check(VanInteriorLayout.insideShell(fixture),"actual transformed mesh exceeds cabin: "+fixture.purpose());
        // Mounted truck interfaces are already supplied by van fixture
        // geometry. They remain usable, but cannot be inventoried as giant
        // duplicate portable COMPUTER / wall-monitor boxes on the shelf.
        for(var monitor:EquipmentRegistry.byOrigin(EquipmentRegistry.Origin.TRUCK)){
            check(FieldEquipment.functional(monitor.id()),
                "mounted monitoring console lost its live host handler: "+monitor.id());
            check(VanInteriorLayout.toolSockets().stream()
                .noneMatch(socket->socket.equipment().equals(monitor.id())),
                "fixed monitor incorrectly consumes a portable rack bay: "+monitor.id());
        }
        check(VanInteriorLayout.FIXTURES.stream().anyMatch(
                fixture->fixture.purpose().equals("command workstation")),
            "camera console requires its native mounted workstation");
        // Rack storage now uses measured native meshes with rigid 90-degree
        // orientation (VanRackStock.place), not the obsolete 42 fixed tiny
        // preview slots. Validate each real stocked tier fits at full scale.
        // In particular, tripods must be packed folded, not scaled-down open.
        for(var socket:VanInteriorLayout.toolSockets())for(var tier:EquipmentTiers.Tier.values()){
            var entry=new VanRackStock.Entry(socket.equipment(),tier,1);
            var placements=VanRackStock.place(java.util.List.of(entry));
            check(placements.size()==1,"real shelf failed to fit "+socket.equipment()+" "+tier);
            var place=placements.get(0);
            var b=place.bounds();
            double[] nativeEdges={b.x1()-b.x0(),b.y1()-b.y0(),b.z1()-b.z0()};
            double[] placedMin={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
            double[] placedMax={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            for(double xx:new double[]{b.x0(),b.x1()})
                for(double yy:new double[]{b.y0(),b.y1()})
                    for(double zz:new double[]{b.z0(),b.z1()}){
                        var v=place.vertex(new GlbProps.Vertex(xx,yy,zz,0,0));
                        double[] p={v.x(),v.y(),v.z()};
                        for(int axis=0;axis<3;axis++){
                            placedMin[axis]=Math.min(placedMin[axis],p[axis]);
                            placedMax[axis]=Math.max(placedMax[axis],p[axis]);
                        }
                    }
            double[] placedEdges={
                placedMax[0]-placedMin[0],placedMax[1]-placedMin[1],placedMax[2]-placedMin[2]};
            java.util.Arrays.sort(nativeEdges);java.util.Arrays.sort(placedEdges);
            for(int axis=0;axis<3;axis++)
                check(Math.abs(nativeEdges[axis]-placedEdges[axis])<1e-7,
                    "rack may not miniaturize "+socket.equipment()+" "+tier);
            check(placedMin[0]>-VanStaging.INTERIOR_HALF_WIDTH
                    &&placedMax[0]<-.51+.001
                    &&placedMin[1]>VanStaging.FLOOR_Y
                    &&placedMax[1]<VanInteriorLayout.ROOF_Y
                    &&placedMin[2]>=-1.941&&placedMax[2]<1.541,
                "native rack mesh exceeds physical cabin: "+socket.equipment()+" "+tier);
        }
        for(var site:AuthoredTestSupport.SITES){
            var map=new HouseMap(site);var van=map.van();var world=new LanWorld(map);
            for(int seat=0;seat<6;seat++){
                var body=world.body(seat);check(body.room()==14&&Math.abs(body.y()-VanStaging.FLOOR_Y)<.001,"pad must be on actual deck");
                for(int other=0;other<seat;other++){var b=world.body(other);check(Math.hypot(b.x()-body.x(),b.z()-body.z())>.44,"six spawn capsules overlap");}
                Vec at=walk(map,new Vec(body.x(),body.y(),body.z()),0,van.local(body.x(),body.z()).z());
                at=walk(map,at,0,VanStaging.FRONT+.10);
                at=walk(map,at,0,VanStaging.FRONT-VanStaging.RAMP_LENGTH-.30);
                check(Math.abs(at.y())<.01&&map.roomAt(at.x(),at.y(),at.z())==13,"ramp must descend into outdoor approach");
                at=walk(map,at,0,0);check(Math.abs(at.y()-VanStaging.FLOOR_Y)<.01,"return must ascend actual deck");
                for(var station:VanStaging.Station.values()){
                    var point=van.location(station);var local=van.local(point.x(),point.z());
                    Vec operating=walk(map,walk(map,at,0,local.z()),local.x(),local.z());
                    check(van.stationAt(operating.x(),operating.z())==station,"station not usable from clear aisle");
                }
            }
            var button=van.doorOperatingPoint();check(map.traversable(button.x(),VanStaging.FLOOR_Y,button.z())&&van.nearDoorButton(button.x(),button.z()),"mounted door control lacks clear operating point");
            van.setCargoDoorClosed(true);var p=van.world(0,VanStaging.FRONT+.12);
            check(!map.traversable(p.x(),VanStaging.FLOOR_Y,p.z()),"closed rear leaves must seal actual threshold");
            System.out.println("PASS "+site+": six distinct deck pads, all station routes, outdoor ramp descent/return and cargo seal");
        }
        System.out.println("PASS human-scale body, actual furniture bounds and native-sized full-tier tool bays");
    }
}
