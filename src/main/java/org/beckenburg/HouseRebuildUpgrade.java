package org.beckenburg;

import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Three-way native House upgrade; authored deltas survive or abort with their stable ID. */
final class HouseRebuildUpgrade {
    private HouseRebuildUpgrade(){}
    record Plan(LevelAuthoringSession.Snapshot state,Set<String> retained){Plan{retained=Set.copyOf(retained);}}
    private static List<String> keys(List<Prop> props){
        var counts=new HashMap<String,Integer>();var result=new ArrayList<String>();
        for(var p:props){String key=p.room()+"/"+p.mesh();result.add(key+"/"+counts.merge(key,1,Integer::sum));}
        return result;
    }
    private static double ratio(double edited,double old){return old==0?1:edited/old;}
    private static Prop delta(Prop original,Prop edited,Prop target){
        return new Prop(edited.mesh(),edited.room(),target.x()+edited.x()-original.x(),target.y()+edited.y()-original.y(),
            target.z()+edited.z()-original.z(),target.scale()*edited.scale()/original.scale(),target.yaw()+edited.yaw()-original.yaw(),
            target.halfX()*ratio(edited.halfX(),original.halfX()),target.halfZ()*ratio(edited.halfZ(),original.halfZ()),
            target.height()*ratio(edited.height(),original.height()),edited.support().equals(original.support())?target.support():edited.support());
    }
    static Plan plan(LevelAuthoringSession.Snapshot saved){
        if(!saved.sceneId().equals("beckenburg/house"))throw new IllegalArgumentException("House rebuild belongs to another scene");
        if(UserLevelLibrary.HOUSE_REBUILD.equals(saved.architecture()))return new Plan(saved,Set.of());
        var baseline=LevelPlans.legacyHouseSource();var target=HouseRebuildFurnishings.source();
        var oldKeys=keys(baseline.props());var newKeys=keys(target.props());var oldByKey=new HashMap<String,Integer>();
        for(int i=0;i<oldKeys.size();i++)oldByKey.put(oldKeys.get(i),i);
        var byId=new LinkedHashMap<String,Prop>();for(int i=0;i<saved.ids().size();i++)byId.put(saved.ids().get(i),saved.props().get(i));
        var ids=new ArrayList<String>();var props=new ArrayList<Prop>();var retained=new HashSet<String>();var consumed=new HashSet<String>();
        for(int i=0;i<target.props().size();i++){
            Prop desired=target.props().get(i);Integer oldIndex=oldByKey.get(newKeys.get(i));String id="house-rebuild-"+i;
            if(oldIndex!=null){
                id="prop-"+oldIndex;consumed.add(id);var edited=byId.get(id);
                if(edited==null)continue; // A user's source deletion is not resurrected.
                Prop original=baseline.props().get(oldIndex);
                if(!edited.equals(original)){desired=delta(original,edited,desired);retained.add(id);}
            }
            if(byId.containsKey(id)&&oldIndex==null)throw conflict(id,"reserved rebuild ID is already authored");
            ids.add(id);props.add(desired);
        }
        for(var entry:byId.entrySet()){
            String id=entry.getKey();if(consumed.contains(id))continue;var edited=entry.getValue();int old=-1;
            for(int i=0;i<baseline.props().size();i++)if(id.equals("prop-"+i)){old=i;break;}
            if(old>=0&&edited.equals(baseline.props().get(old)))continue;
            // Unmatched personal objects keep X/Z and their authored storey offset.
            double offset=target.floorY(edited.room())-baseline.floorY(edited.room());
            var moved=new Prop(edited.mesh(),edited.room(),edited.x(),edited.y()+offset,edited.z(),edited.scale(),edited.yaw(),edited.halfX(),edited.halfZ(),edited.height(),edited.support());
            ids.add(id);props.add(moved);retained.add(id);
        }
        return new Plan(new LevelAuthoringSession.Snapshot(saved.sceneId(),ids,props,UserLevelLibrary.HOUSE_REBUILD),retained);
    }
    private static IllegalArgumentException conflict(String id,String why){return new IllegalArgumentException("House rebuild conflict: "+id+": "+why);}
    private static Vec world(Prop p,GlbProps.Vertex v){double x=v.x()*p.scale(),z=v.z()*p.scale(),c=Math.cos(p.yaw()),s=Math.sin(p.yaw());return new Vec(p.x()+x*c-z*s,p.y()+v.y()*p.scale(),p.z()+x*s+z*c);}
    static List<Vec[]> triangles(Prop p){
        var result=new ArrayList<Vec[]>();for(var t:GlbProps.model(p.mesh()).triangles())result.add(new Vec[]{world(p,t.a()),world(p,t.b()),world(p,t.c())});return result;
    }
    private static boolean inside(Collider b,Vec v){
        double c=Math.cos(b.yaw()),s=Math.sin(b.yaw()),dx=v.x()-b.x(),dz=v.z()-b.z();
        return Math.abs(dx*c+dz*s)<b.halfX()-.003&&Math.abs(-dx*s+dz*c)<b.halfZ()-.003&&v.y()>b.y()+.003&&v.y()<b.y()+b.height()-.003;
    }
    static void validate(Plan plan){
        var state=plan.state();var target=HouseRebuildFurnishings.source();
        var level=LevelAuthoringSession.compose(target,state.props());var map=new HouseMap(HouseMap.Site.HOUSE,level);
        var geometry=new HashMap<Prop,List<Vec[]>>();
        for(String id:plan.retained()){
            var p=state.props().get(state.ids().indexOf(id));var room=level.room(p.room());
            var vertices=new ArrayList<Vec>();for(var t:GlbProps.model(p.mesh()).triangles())for(var v:List.of(t.a(),t.b(),t.c()))vertices.add(world(p,v));
            if(vertices.isEmpty())throw conflict(id,"model has no source surfaces");
            double bottom=vertices.stream().mapToDouble(Vec::y).min().orElseThrow();
            for(var v:vertices)if(v.x()<room.x0()+.077||v.x()>room.x1()-.077||v.z()<room.z0()+.077||v.z()>room.z1()-.077||v.y()>room.y()+room.height()+.003)
                throw conflict(id,"source model crosses the rebuilt room boundary");
            var others=new ArrayList<>(level.props());others.remove(p);
            for(var v:vertices)if(Math.abs(v.y()-bottom)<.002){
                Double floor=map.supportNear(v.x(),v.z(),v.y());boolean supported=floor!=null&&Math.abs(v.y()-floor)<.01;
                if(!supported)for(var parent:others)try{if(Math.abs(HouseRebuildStory.surface(parent,v.x(),v.z())-v.y())<.01){supported=true;break;}}catch(IllegalArgumentException noSurface){}
                if(!supported)throw conflict(id,"source foot/support no longer meets terrain, deck or a retained model");
            }
            var remaining=new ArrayList<>(level.colliders());for(var own:LevelPlans.propColliders(level,p)){
                for(int i=0;i<remaining.size();i++)if(LevelPlans.sameBody(remaining.get(i),own)){remaining.remove(i);break;}
            }
            for(var v:vertices)for(var body:remaining)if(inside(body,v))throw conflict(id,"retained source intersects another physical body");
            var mesh=geometry.computeIfAbsent(p,HouseRebuildUpgrade::triangles);
            for(var other:others)if(CastleRebuildValidation.modelConflict(mesh,geometry.computeIfAbsent(other,HouseRebuildUpgrade::triangles)))
                throw conflict(id,"source triangles cross, duplicate or contain "+other.mesh()+"/room"+other.room());
        }
        var routes=LevelTraversalAudit.inspect(map);
        if(!routes.passed())throw conflict("routes","retained edits obstruct the rebuilt route: "+routes.findings());
    }
}
