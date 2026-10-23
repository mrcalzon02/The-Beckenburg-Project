package org.beckenburg;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.beckenburg.LevelDefinition.*;

/** Real graded support, worn-route clearance, upright rooted planting and authoritative recovery. */
public final class TerrainAndRecoveryTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void lower(Object object,String name,int index,double value)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);if(index>=0)((double[])f.get(object))[index]=value;else f.setDouble(object,value);}
    public static void main(String[] args)throws Exception{
        var map=BlackpineLevel.map();map.configureHiding(100);var terrain=map.terrain();
        for(var p:map.level().portals())if(p.door())map.toggleDoor(p.id());
        check(Math.abs(terrain.height(4,1)-terrain.height(35.5,72)-HunterPosture.STANDING_HEIGHT)<1e-9,"far edge is exactly one standing person above the road");
        check(terrain.height(35.5,72)==0,"canonical road and van retain zero ground");
        double relief=0,maxSlope=0;
        for(double x=4;x<67;x+=1)for(double z=2;z<60;z+=1){
            var normal=terrain.normal(x,z);maxSlope=Math.max(maxSlope,Math.hypot(normal.x(),normal.z())/normal.y());
            relief=Math.max(relief,terrain.height(x,z)-SiteTerrain.grade(z));
        }
        check(relief>.12&&maxSlope<.75,"actual shoulder relief and walkable terrain gradients");
        check(map.level().sockets().stream().noneMatch(s->s.type().equals("landscape_detail")&&(s.style().startsWith("level_woodland_track|")||s.style().startsWith("level_woodland_litter|"))),"wear is baked into the terrain rather than overlapping path stamps");
        var scenery=new SceneryInstances(map.level(),100);double minClearance=Double.MAX_VALUE;int roots=0;
        for(var instance:scenery.instances()){
            boolean obstruct=instance.mesh.equals("level_camp_logpile")||instance.mesh.equals("level_shrub_refuge")||instance.mesh.equals("level_woodland_scrub");
            for(int i=0;i<instance.template.vertexCount();i++){
                var p=instance.point(i);
                if(obstruct&&instance.room==12)minClearance=Math.min(minClearance,BlackpineDressing.distanceToPath(p.x(),p.z()));
                if(instance.room==12&&(instance.mesh.equals("level_woodland_grass")||instance.mesh.equals("level_woodland_flowers"))&&Math.abs(instance.template.vertex(i).y())<1e-5){
                    check(Math.abs(p.y()-terrain.height(p.x(),p.z()))<1e-5,"upright soft planting roots touch the actual triangle surface");roots++;
                }
            }
        }
        check(minClearance>.99,"logs and shrubs must be wholly outside worn paths: "+minClearance);check(roots>100,"real grass/flower roots checked");
        // Walk up and down an unobstructed exposed strip, with exact triangle support at every step.
        Vec at=new Vec(65,terrain.height(65,3),3);double start=at.y();
        for(int i=1;i<=450;i++){double z=3+i*.1;var next=map.resolveMove(at,65,z);check(next!=null,"continuous graded walk "+z);check(Math.abs(next.y()-terrain.height(65,z))<1e-8,"feet match render triangles");at=next;}
        check(start-at.y()>.8,"walking downhill produces significant actual elevation change");
        for(int i=449;i>=0;i--){var next=map.resolveMove(at,65,3+i*.1);check(next!=null,"continuous uphill return");at=next;}
        var world=new LanWorld(map);world.setRoom(0,6);var original=world.body(0);lower(world,"ys",0,original.y()-4);world.advanceStamina(2);
        check(Math.abs(world.body(0).y()-original.y())<.01,"stationary host hunter recovers on authoritative tick");
        lower(world,"ys",0,original.y()-.06);world.advanceStamina(4);check(Math.abs(world.body(0).y()-original.y())<.01,"six-centimetre ground penetration is recovered too");
        var ghost=new LanGhostSpatial(map,6);lower(ghost,"y",-1,original.y()-4);ghost.advanceStamina(2);check(ghost.pose().y()>=map.floorY(6)-.01,"host ghost recovers on tick");
        var outsideGhost=new LanGhostSpatial.Pose(12,31,terrain.height(31,40),40,0);
        check(LanGhostHearing.project(map,outsideGhost,12,32,40,LanGhostPursuit.Sound.EQUIPMENT,20)!=null,"outdoor ghost hearing uses the graded source and listener elevation");
        var pursuit=new LanGhostPursuit(map,42);pursuit.heard(12,12,32,40,LanGhostPursuit.Sound.EQUIPMENT,20);check(pursuit.memory(20)!=null,"graded outdoor sound enters host AI acoustic memory");
        var blocked=GroundRecovery.recover(map,new Vec(26,-5,36),12,HunterPosture.STANDING_HEIGHT);check(map.traversable(blocked.x(),blocked.y(),blocked.z()),"recovery under the solid well selects a clear supported spot");
        for(var site:AuthoredTestSupport.SITES){var reference=new HouseMap(site);for(var room:reference.level().rooms()){
            var p=room.spawn();check(GroundRecovery.recover(reference,p,room.id(),HunterPosture.STANDING_HEIGHT).equals(p),"valid upstairs/basement/van pose is never snapped to another floor");
            var fixed=GroundRecovery.recover(reference,new Vec(p.x(),p.y()-10,p.z()),room.id(),HunterPosture.STANDING_HEIGHT);check(reference.traversable(fixed.x(),fixed.y(),fixed.z()),"safe recovery on reference map");
        }
            Vec ramp=reference.spawn(14);
            for(int i=0;i<125;i++){
                var local=reference.van().local(ramp.x(),ramp.z());var point=reference.van().world(0,local.z()-.05);
                var moved=reference.resolveMove(ramp,point.x(),point.z());if(moved==null)break;
                check(GroundRecovery.recover(reference,moved,14,HunterPosture.STANDING_HEIGHT).equals(moved),"valid van ramp/road support cannot be treated as falling below the van");ramp=moved;
            }
        }
        var clutter=map.clutter();Field all=LooseClutter.class.getDeclaredField("items");all.setAccessible(true);Object item=((List<?>)all.get(clutter)).get(0);lower(item,"y",-1,-20);clutter.advance(List.of());var recovered=clutter.states().get(0);
        check(recovered.y()>-1&&recovered.holder()!=-2,"lost loose entity returns above support instead of being deleted");
        ImageIO.write(terrain.diffuse(),"PNG",Path.of("assets/runtime/level-textures/blackpine-terrain-diffuse.png").toFile());
        var report=new LinkedHashMap<String,Object>();report.put("status","pass");report.put("terrain_version",SiteTerrain.VERSION);report.put("road_z_m",SiteTerrain.ROAD_Z);report.put("back_z_m",SiteTerrain.BACK_Z);report.put("rise_m",SiteTerrain.RISE);report.put("cell_m",SiteTerrain.STEP);report.put("vertices",terrain.nx*terrain.nz);report.put("maximum_measured_slope",maxSlope);report.put("shoulder_relief_m",relief);report.put("minimum_shrub_log_route_clearance_m",minClearance);report.put("conforming_roots_checked",roots);report.put("baked_texture","assets/runtime/level-textures/blackpine-terrain-diffuse.png");report.put("player_ghost_loose_entity_recovery",true);
        Files.writeString(Path.of("development/levels/verification/blackpine-terrain.json"),DressingReport.json(report)+"\n");
        System.out.println("PASS graded terrain, baked erosion paths, wholly clear shrubs/logs, rooted upright plants, 900 physical slope steps and player/ghost/loose-entity recovery");
    }
}
