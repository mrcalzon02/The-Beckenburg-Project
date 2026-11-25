package org.beckenburg;

import org.beckenburg.editor.DefaultPlayerMotion;

public final class PlayerJumpTest {
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static DefaultPlayerMotion.Collision space(double ceiling){return new DefaultPlayerMotion.Collision(){
        public boolean clear(double x,double y,double z,double h){return x<2&&y>=0&&y+h<=ceiling;}
        public double landing(double x,double z,double a,double b){return a>=0&&b<=0?0:Double.NaN;}
    };}
    public static void main(String[] args){
        var jump=new DefaultPlayerMotion();var open=space(10);
        jump.jump(0,0,0,4.65,0,1.05,open);
        check(jump.y()==DefaultPlayerMotion.TUCK,"tuck raises feet instead of lowering head");
        try{jump.jump(0,0,0,0,0,1.05,open);throw new AssertionError("double jump");}catch(IllegalArgumentException expected){}
        double peak=jump.y();boolean fell=false;
        for(int i=0;i<20;i++){double old=jump.y();jump.advance(.1,1.05,open);peak=Math.max(peak,jump.y());fell|=jump.y()<old;}
        check(peak>1.2&&fell&&!jump.active()&&jump.y()==0,"rise, fall and finish landing");
        check(jump.x()>1.8&&jump.x()<2,"running momentum and swept wall collision");
        jump.jump(0,0,0,0,0,1.05,space(1.9));
        for(int i=0;i<20;i++){jump.advance(.1,1.05,space(1.9));check(jump.y()+1.05<=1.9,"ceiling collision");}
        check(jump.y()==0&&!jump.active(),"head impact returns to floor");
        HouseMap map=new HouseMap();LanWorld world=new LanWorld(map);world.setRoom(0,4);
        double floor=world.body(0).y();world.jump(0,1);
        check(world.crouched(0)&&world.body(0).y()>floor+.7,"host tucked pose");
        for(int i=2;i<22;i++){world.advanceMotion(i);world.advanceStamina(i);}
        check(!world.airborne(0)&&!world.crouched(0)&&Math.abs(world.body(0).y()-floor)<.001,"host landing and automatic stand");
        world.posture(0,true);world.jump(0,23);
        for(int i=24;i<44;i++)world.advanceMotion(i);
        check(world.crouched(0),"intentional crouch survives landing");
        world.jump(0,44);world.setRoom(0,14);world.advanceMotion(45);
        check(!world.airborne(0)&&!world.crouched(0),"scene/spawn reset clears jump");
        world.setRoom(0,4);world.jump(0,50);world.advanceMotion(81);
        check(!world.airborne(0)&&world.body(0).room()==4,"void/contact timeout recovers to safe room");
        var base=map.level();var solids=new java.util.ArrayList<>(base.colliders());
        solids.add(new LevelDefinition.Collider(8.8,0,6.8,.7,.7,.5,0));
        var level=new LevelDefinition(base.id(),"jump-platform",base.title(),base.rooms(),base.portals(),base.stairs(),base.voids(),base.props(),base.sockets(),solids,base.dock(),base.ambience());
        var platform=new HouseMap(HouseMap.Site.HOUSE,level);
        check(platform.jumpLanding(8.8,6.8,.52,.48)==.5,"raised solid is landing support");
        check(platform.traversable(8.8,.5,6.8),"can stand on landed solid");
        check(platform.resolveMove(new LevelDefinition.Vec(8.8,.5,6.8),8.9,6.8)!=null,"can move after raised landing");
        System.out.println("PASS player jump tuck, gravity, momentum, wall/ceiling collision, landing, posture and reset");
    }
}
