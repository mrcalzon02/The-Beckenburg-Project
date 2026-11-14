package org.beckenburg;

import java.util.Arrays;

/** Real cargo button position, collision, reopen and visual shutter in rotated vans. */
public final class VanCargoDoorTest {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static LooseClutter.Observer fromDesk(HouseMap map){
        var van=map.van();var desk=van.world(.15,1.10);
        return aimButton(map,desk.x(),van.floorY(),desk.z());
    }
    static LooseClutter.Observer aimButton(HouseMap map,double px,double py,double pz){
        var van=map.van();var button=van.doorButton();
        double angle=van.placement().yaw()+Math.PI/2,c=Math.cos(angle),sn=Math.sin(angle);
        // Aim at actual triangle centres on the rendered button, from the desk aisle.
        for(var triangle:GlbProps.model("button_12").triangles()){
            var a=triangle.a();var b=triangle.b();var v=triangle.c();
            double x=(a.x()+b.x()+v.x())/3,y=(a.y()+b.y()+v.y())/3,z=(a.z()+b.z()+v.z())/3;
            double tx=button.x()+.30*(x*c-z*sn),tz=button.z()+.30*(x*sn+z*c),ty=van.floorY()+1.2+.30*y;
            var observer=InteractionFocusTest.aim(px,py,pz,tx,ty,tz);
            var target=InteractionFocus.pick(map,observer,java.util.List.of());
            if(target!=null&&target.kind()==InteractionFocus.Kind.VAN_DOOR)return observer;
        }
        throw new AssertionError("rear door button cannot be targeted from front desk: "+map.site());
    }
    public static void main(String[] args){
        for(double yaw:new double[]{0,Math.PI/2,-Math.PI/2,Math.PI}){
            VanStaging van=new VanStaging(new VanStaging.Placement(50.0,50.0,yaw));
            VanStaging.Position button=van.doorButton();
            ok(van.nearDoorButton(button.x(),button.z()),"button reachable for all rotations");
            ok(!van.nearDoorButton(button.x()+20,button.z()),
                "remote interaction cannot toggle door");
            VanStaging.Position entrance=van.world(0,VanStaging.FRONT+.04);
            ok(!van.collides(entrance.x(),entrance.z(),.15),"van cargo entrance initially open");
            ok(van.toggleCargoDoor()&&van.cargoDoorClosed(),"button closes door");
            ok(van.collides(entrance.x(),entrance.z(),.15),
                "closed cargo door genuinely blocks physical movement");
            ok(!van.toggleCargoDoor()&&!van.cargoDoorClosed(),"button reopens door");
            ok(!van.collides(entrance.x(),entrance.z(),.15),"open cargo area traversable");
        }
        for(var site:new HouseMap.Site[]{HouseMap.Site.HOUSE,HouseMap.Site.FARMHOUSE,HouseMap.Site.SCHOOL,HouseMap.Site.HOLLOW_CREEK}){
            HouseMap map=new HouseMap(site);var observer=fromDesk(map);
            var target=InteractionFocus.pick(map,observer,java.util.List.of());
            ok(target.name().equals("Close doors and end mission"),"departure prompt explains action");
            var away=new LooseClutter.Observer(0,observer.x(),observer.y(),observer.z(),observer.yaw()+Math.PI,observer.pitch(),true);
            var roof=new LooseClutter.Observer(0,observer.x(),observer.y()+3,observer.z(),observer.yaw(),observer.pitch(),true);
            var roofTarget=InteractionFocus.pick(map,roof,java.util.List.of());
            ok(roofTarget==null||roofTarget.kind()!=InteractionFocus.Kind.VAN_DOOR,"roof cannot use departure control");
            var missed=InteractionFocus.pick(map,away,java.util.List.of());
            ok(missed==null||missed.kind()!=InteractionFocus.Kind.VAN_DOOR,"must look at rear button");
        }
        HouseMap house=new HouseMap();
        SceneRenderer r=new SceneRenderer();
        r.x=house.van().spawn().x();r.z=house.van().spawn().z();
        r.yaw=-Math.PI/2;
        r.drawNetwork(house,java.util.List.of(),-1,1);
        int[] open=r.pixels().clone();
        house.van().toggleCargoDoor();
        r.drawNetwork(house,java.util.List.of(),-1,2);
        ok(!Arrays.equals(open,r.pixels()),"door is rendered in its actual closed position");
        System.out.println("PASS: switchable server-driven van door changes collision and rendered scene");
    }
}
