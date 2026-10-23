package org.beckenburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Verifies actual imported GLB triangles do not intersect opaque castle walls.
 * Collision circles or prop IDs alone cannot prove visible models are fitted.
 */
public final class CastleFurnitureWallAuditTest {
    private record Bounds(double x,double y,double z){}
    private static Bounds[] localBounds(String mesh){
        double x0=Double.POSITIVE_INFINITY,x1=-x0;
        double y0=Double.POSITIVE_INFINITY,z0=y0,z1=-y0;
        for(var t:GlbProps.model(mesh).triangles())
            for(var v:new GlbProps.Vertex[]{t.a(),t.b(),t.c()}){
                x0=Math.min(x0,v.x());x1=Math.max(x1,v.x());
                y0=Math.min(y0,v.y());z0=Math.min(z0,v.z());z1=Math.max(z1,v.z());
            }
        return new Bounds[]{new Bounds((x0+x1)*.5,y0,(z0+z1)*.5)};
    }
    private static Bounds position(CastleLevelEdits.Prop prop,Bounds origin,GlbProps.Vertex v){
        double dx=(v.x()-origin.x())*prop.scale();
        double dz=(v.z()-origin.z())*prop.scale();
        double c=Math.cos(prop.yaw()),s=Math.sin(prop.yaw());
        return new Bounds(prop.x()+dx*c-dz*s,
            prop.y()+(v.y()-origin.y())*prop.scale(),
            prop.z()+dx*s+dz*c);
    }
    private static boolean crossing(Bounds a,Bounds b,CastleHubArchitecturalPlan.Wall wall){
        double da=wall.horizontal()?a.z()-wall.z0():a.x()-wall.x0();
        double db=wall.horizontal()?b.z()-wall.z0():b.x()-wall.x0();
        // Ignore flush contact and numerical roundoff at a mounting surface.
        if(!(da<-.085&&db>.085||db<-.085&&da>.085))return false;
        double f=-da/(db-da),y=a.y()+f*(b.y()-a.y());
        if(y<=wall.bottom()+.08||y>=wall.top()-.08)return false;
        double tangent=wall.horizontal()?a.x()+f*(b.x()-a.x()):a.z()+f*(b.z()-a.z());
        double low=wall.horizontal()?Math.min(wall.x0(),wall.x1()):Math.min(wall.z0(),wall.z1());
        double high=wall.horizontal()?Math.max(wall.x0(),wall.x1()):Math.max(wall.z0(),wall.z1());
        return tangent>low+.06&&tangent<high-.06;
    }
    public static void main(String[] args){
        var geometry=CastleLevelEdits.current();
        Map<String,Bounds> cache=new HashMap<>();
        List<String> intersections=new ArrayList<>();
        for(var prop:geometry.props()){
            Bounds origin=cache.computeIfAbsent(prop.mesh(),m->localBounds(m)[0]);
            for(var wall:geometry.walls()){
                if(wall.hasDoor())continue;
                if(prop.upper() && wall.top()<=CastleHubArchitecturalPlan.GROUND_CEILING+.01)continue;
                if(!prop.upper()&&wall.bottom()>=CastleHubArchitecturalPlan.GROUND_CEILING-.01)continue;
                boolean penetrated=false;
                for(var tri:GlbProps.model(prop.mesh()).triangles()){
                    Bounds a=position(prop,origin,tri.a()),b=position(prop,origin,tri.b()),
                        c=position(prop,origin,tri.c());
                    if(crossing(a,b,wall)||crossing(b,c,wall)||crossing(c,a,wall)){
                        penetrated=true;break;
                    }
                }
                if(penetrated)intersections.add(prop.id()+" "+prop.mesh()+
                    " at "+prop.x()+","+prop.z()+
                    " through wall "+wall.x0()+","+wall.z0()+" -> "+wall.x1()+","+wall.z1());
            }
        }
        if(!intersections.isEmpty())
            throw new AssertionError("Imported castle props intersect real opaque wall meshes:\n"+
                String.join("\n",intersections));
        System.out.println("PASS all castle furnishing GLBs remain clear of solid wall volumes");
    }
}
