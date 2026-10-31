package org.beckenburg;

import org.beckenburg.LevelDefinition.Prop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Objective gate: editor data must round-trip into live investigation physics. */
public final class LevelAuthoringSessionTest {
    private static void check(boolean ok,String why){
        if(!ok)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        String home=System.getProperty("user.home");
        Path temporary=Files.createTempDirectory("beckenburg-level-authoring-");
        System.setProperty("user.home",temporary.toString());
        try{
            var site=HouseMap.Site.HOUSE;
            var nativeSource=LevelPlans.buildSource(site);
            var session=new LevelAuthoringSession(site);
            check(session.props().equals(nativeSource.props()),"Opening an untouched level is lossless");
            int index=-1;
            for(int i=0;i<session.size();i++){
                var p=session.prop(i);
                if(p.halfX()>0&&p.halfZ()>0){index=i;break;}
            }
            check(index>=0,"House has a colliding real object to edit");
            Prop before=session.prop(index);
            var moved=new Prop(before.mesh(),before.room(),before.x()+.125,
                before.y(),before.z()+.125,before.scale(),before.yaw(),
                before.halfX(),before.halfZ(),before.height(),before.support());
            session.put(index,moved);
            var staged=session.level();
            check(staged.props().get(index).equals(moved),"Editor preview uses changed source object");
            check(staged.colliders().contains(LevelPlans.propCollider(moved)),
                "Edited prop gets physical collision in preview");
            session.save();
            check(Files.isRegularFile(session.documentPath()),"Scene document was not persisted");
            var reopened=new LevelAuthoringSession(site);
            check(reopened.prop(index).equals(moved),"Open reuses the exact serialized position");
            var gameLevel=LevelPlans.build(site);
            check(gameLevel.props().get(index).equals(moved),"Game loads editor-modified prop");
            check(gameLevel.colliders().contains(LevelPlans.propCollider(moved)),
                "Game uses editor-modified collider");
            check(new HouseMap(site).level().props().get(index).equals(moved),
                "Playable HouseMap uses the saved edited level, not a disconnected preview");
            check(LevelPlans.build(HouseMap.Site.SCHOOL).props().equals(
                LevelPlans.buildSource(HouseMap.Site.SCHOOL).props()),
                "Editing House does not mutate School");
            reopened.resetToSource();
            reopened.save();
            check(LevelPlans.build(site).props().equals(nativeSource.props()),
                "Reset returns to the source level without destroying other maps");
            System.out.println("PASS authored GLB and physics roundtrip, reset and cross-map isolation");
        }finally{
            System.setProperty("user.home",home);
            try(var files=Files.walk(temporary)){
                files.sorted(Comparator.reverseOrder()).forEach(p->{
                    try{Files.deleteIfExists(p);}catch(Exception ignored){}
                });
            }
        }
    }
}
