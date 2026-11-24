package org.beckenburg;

import org.beckenburg.LevelDefinition.Vec;
import org.beckenburg.NativeSchoolInteriorReview.Peer;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

/** Named native belongings rendered and handled through the normal game authority. */
final class MonasteryLooseReview {
    private static final String MUG="REFECTORY_COMMON_MUG";
    private static final Vec TABLE_SIDE=new Vec(54.88,0,34.91);

    private MonasteryLooseReview() {}

    static void review(HouseMap source,Path captures,List<String> report) throws Exception {
        var map=HouseMap.forNetworkRevision(source.site(),source.level().revision());
        var clutter=map.clutter();
        var original=clutter.states();
        require(original.size()==6,"Expected six independently packaged belongings");
        for(int i=0;i<30;i++) clutter.advance(List.of());
        require(clutter.states().equals(original),"An untouched native belonging left its measured bearing");
        confirmDatum(clutter,"BELL_RINGER_TEA_TIN",6);
        confirmDatum(clutter,"MEMORIAL_OIL_BOTTLE",-3);
        var mug=belonging(clutter,MUG);
        var observer=look(TABLE_SIDE,mug);
        var focus=focus(map,observer,mug);
        var display=new Vec(mug.x(),mug.y(),mug.z());
        MonasteryNamedReview.capture(map,TABLE_SIDE,display,captures.resolve("monastery-loose-refectory-table.png"));
        clutter.takeFocused(mug.id(),observer,1);
        require(clutter.held(0)==mug.id() && clutter.hint(observer).contains("ration sheet"),
            "Named mesh pickup or examination failed "+focus);
        var held=belonging(clutter,MUG);
        MonasteryNamedReview.capture(map,TABLE_SIDE,new Vec(held.x(),held.y(),held.z()),
            captures.resolve("monastery-loose-refectory-examined.png"));
        clutter.action("DROP",mug.id(),new LooseClutter.Observer(0,TABLE_SIDE.x(),0,TABLE_SIDE.z(),Math.PI/2,0,true),2);
        for(int i=0;i<45;i++) clutter.advance(List.of());
        var settled=belonging(clutter,MUG);
        require(settled.holder()==-1 && Math.abs(settled.y()-.917)<.014,"Mug fell through the actual refectory table "+settled);
        dropOnFloor(map,"BELL_RINGER_TEA_TIN",new Vec(58.78,6,23.18),6);
        dropOnFloor(map,"MEMORIAL_OIL_BOTTLE",new Vec(21.75,-3,10.72),-3);
        var fresh=HouseMap.forNetworkRevision(source.site(),source.level().revision());
        require(fresh.clutter().states().equals(original),"New mission did not restore the exact authored seeds");
        var disturbed=fresh.clutter();
        require(disturbed.sanityScatter(4,371)==2,"Expected the ghost to disturb both refectory belongings");
        var before=belonging(disturbed,MUG);
        for(int i=0;i<65;i++){
            disturbed.advance(List.of());
            require(belonging(disturbed,MUG).y()>=0,"Tumbling mug fell through the actual refectory floor");
        }
        var after=belonging(disturbed,MUG);
        require(Math.hypot(before.x()-after.x(),before.z()-after.z())>.25,"Disturbed mug did not move");
        report.add("native_loose_support_and_reset\tPASS six supported seeds, upper and lower floor drops, tabletop drop, tumbling contact and fresh mission reset");
    }

    static void live(HouseMap authority,Peer engineer,Peer witness,Peer ghost,Vec at,List<String> report) throws Exception {
        var objects=authority.clutter();
        var original=objects.authoredItem(MUG);
        if(original==null)return;
        // The previous interaction read the ration sheet. The host gates this command for three ticks.
        Thread.sleep(350);
        var observer=look(at,original);
        var focus=focus(authority,observer,original);
        engineer.client.turn(heading(observer.yaw()));
        engineer.client.interact(focus,observer.yaw(),observer.pitch());
        engineer.awaitExact("ACCEPT|INTERACT|"+focus.name());
        require(objects.held(0)==original.id(),"The host did not grant physical mug ownership");
        ghost.awaitMatching(frame->frame.startsWith("CLUTTER|") &&
            java.util.Arrays.stream(frame.substring(8).split(";")).anyMatch(row->
                row.startsWith(original.id()+",MUG,0,0,0,") && row.endsWith(",-2")),
            "redacted held mug",4000);

        witness.client.view3D();
        witness.awaitExact("ACCEPT|VIEW3D");
        witness.client.clutter("DROP",original.id(),0);
        require(witness.await("REJECT|").contains("not in your hand"),"Bystander acquired authority over the mug");
        var turned=belonging(objects,MUG);
        Thread.sleep(125);
        engineer.client.clutter("ROTATE",original.id(),0);
        engineer.awaitExact("ACCEPT|OBJECT");
        require(belonging(objects,MUG).yaw()>turned.yaw()+.30,"Host did not rotate the held object");

        engineer.client.turn(90);
        Thread.sleep(125);
        engineer.client.clutter("DROP",original.id(),0);
        engineer.awaitExact("ACCEPT|OBJECT");
        var landed=await(objects,state->state.holder()==-1 && Math.abs(state.y()-.917)<.014,"tabletop drop");
        var aim=look(at,landed);
        var pickup=focus(authority,aim,landed);
        engineer.client.turn(heading(aim.yaw()));
        engineer.client.interact(pickup,aim.yaw(),aim.pitch());
        engineer.awaitExact("ACCEPT|INTERACT|"+pickup.name());

        engineer.client.turn(90);
        Thread.sleep(125);
        engineer.client.clutter("THROW",original.id(),0);
        engineer.awaitExact("ACCEPT|OBJECT");
        var floor=await(objects,state->state.x()>original.x()+1.2 && state.y()<.19 && state.holder()==-1,"fall to the real dining floor");
        require(authority.roomAt(floor.x(),0,floor.z())==4,"Thrown mug escaped the fitted refectory");
        report.add("native_loose_live_owner_throw_and_privacy\tPASS actual interaction, ghost redaction, rejected remote drop, rotation, tabletop and floor contacts");
    }

    private static void dropOnFloor(HouseMap map,String id,Vec at,double floor) {
        require(map.traversable(at.x(),at.y(),at.z()),"Missing standing support for "+id+" at "+at);
        var objects=map.clutter();
        var item=belonging(objects,id);
        var view=look(at,item);
        focus(map,view,item);
        int tick=id.startsWith("BELL_")?3:5;
        objects.takeFocused(item.id(),view,tick);
        objects.action("DROP",item.id(),view,tick+1);
        for(int i=0;i<40;i++)objects.advance(List.of());
        var released=belonging(objects,id);
        require(released.holder()==-1 && Math.abs(released.y()-released.kind().half-floor-.001)<.014,
            "Failed fall onto the correct physical floor "+id+" "+released);
    }

    private static void confirmDatum(LooseClutter clutter,String id,double datum) {
        var state=belonging(clutter,id);
        require(Math.abs(state.y()-state.kind().half-datum-.001)<.004,"Wrong authored floor for "+id+" "+state);
    }

    private static LooseClutter.State await(LooseClutter objects,Predicate<LooseClutter.State> condition,String label)
            throws InterruptedException {
        long end=System.nanoTime()+5_000_000_000L;
        LooseClutter.State state;
        do {
            state=belonging(objects,MUG);
            if(condition.test(state))return state;
            Thread.sleep(35);
        } while(System.nanoTime()<end);
        throw new AssertionError("Missing "+label+" "+state);
    }

    private static LooseClutter.State belonging(LooseClutter objects,String key) {
        var result=objects.authoredItem(key);
        require(result!=null,"Missing packaged belonging "+key);
        return result;
    }

    private static LooseClutter.Observer look(Vec at,LooseClutter.State state) {
        double dx=state.x()-at.x(),dz=state.z()-at.z();
        return new LooseClutter.Observer(0,at.x(),at.y(),at.z(),Math.atan2(dx,dz),
            Math.atan2(state.y()-at.y()-HunterPosture.STANDING_EYE,Math.hypot(dx,dz)),true);
    }

    private static InteractionFocus.Target focus(HouseMap map,LooseClutter.Observer view,LooseClutter.State state) {
        var target=InteractionFocus.pick(map,view,List.of());
        require(target!=null && target.kind()==InteractionFocus.Kind.CLUTTER && target.id().equals(""+state.id()),
            "Aim missed native mug at "+view+" "+state+" "+target);
        return target;
    }

    private static int heading(double yaw) {
        return Math.floorMod((int)Math.round(Math.toDegrees(yaw)),360);
    }

    private static void require(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
