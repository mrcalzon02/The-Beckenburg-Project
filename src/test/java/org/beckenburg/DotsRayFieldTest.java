package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.Vec;
public final class DotsRayFieldTest {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static DotsRayField.Mesh wall(double z){return new DotsRayField.Mesh(List.of(
        new DotsRayField.Triangle(new Vec(-5,-2,z),new Vec(5,-2,z),new Vec(5,5,z),-1,0,0,0,0,0,0),
        new DotsRayField.Triangle(new Vec(-5,-2,z),new Vec(5,5,z),new Vec(-5,5,z),-1,0,0,0,0,0,0)));}
    public static void main(String[] args){
        var ray=new DotsRayField.Ray(new Vec(0,1,0),new Vec(0,0,1),5);
        var body=new LanGhostSpatial.Pose(0,0,0,2,0);var back=wall(4);
        var empty=DotsRayField.project(ray,back,null);check(!empty.onGhost()&&empty.hit().distance()==4,"unintercepted beam hits actual wall");
        var intercepted=DotsRayField.project(ray,back,body);
        check(intercepted.onGhost()&&Math.abs(intercepted.hit().distance()-1.78)<1e-8,"same ray transfers to nearest front capsule surface");
        check(intercepted.hit().position().z()<2&&intercepted.hit().normal().z()<0,"no far-side or whole-volume dot");
        check(!DotsRayField.project(ray,wall(1),body).onGhost(),"furniture/wall before ghost wins");
        check(!DotsRayField.project(ray,back,new LanGhostSpatial.Pose(0,1,0,2,0)).onGhost(),"nearby ghost outside beam does nothing");
        check(DotsRayField.project(new DotsRayField.Ray(ray.origin(),ray.direction(),1),back,body)==null,"range limits both room and ghost");
        var basic=new LanDotsProjectors.Placement(0,0,0,0,0,EquipmentTiers.Tier.I,0);
        check(DotsRayField.rays(basic,0).equals(DotsRayField.rays(basic,51)),"basic pattern is stationary");
        for(var tier:List.of(EquipmentTiers.Tier.II,EquipmentTiers.Tier.III)){
            var moving=new LanDotsProjectors.Placement(0,0,0,0,0,tier,0);
            check(!DotsRayField.rays(moving,0).equals(DotsRayField.rays(moving,17)),"higher tier physically pans rays");
        }
        int hit=0,miss=0;for(var r:DotsRayField.rays(basic,0)){
            var projected=DotsRayField.project(r,wall(1.9),new LanGhostSpatial.Pose(0,0,0,1.2,0));
            if(projected!=null&&projected.onGhost()){hit++;check(projected.hit().position().z()<1.2,"only projector-facing surface marked");}else miss++;
        }
        check(hit>2&&miss>hit,"only a subset of the real pattern intersects the ghost");
        var rotated=new LanDotsProjectors.Placement(0,0,0,0,0,EquipmentTiers.Tier.I,180);
        check(DotsRayField.ghostHits(null,rotated,body,0).isEmpty(),"orientation prevents behind-projector evidence");
        var cap=new DotsRayField.Ray(new Vec(0,3,2),new Vec(0,-1,0),4);
        check(Math.abs(DotsRayField.capsule(cap,body).position().y()-StructuredLightVolume.HEIGHT)<1e-8,"rounded capsule top hit");
        System.out.println("PASS DOTS nearest-hit projection, transferred wall dot, occlusion, near misses, one-sided hits, static/panning patterns and heading");
    }
}
