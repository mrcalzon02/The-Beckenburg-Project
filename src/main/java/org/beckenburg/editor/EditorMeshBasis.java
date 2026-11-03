package org.beckenburg.editor;

import java.util.*;
import org.beckenburg.editor.EditorMeshPipeline.Normal;

/** Immutable surface basis derived from actual native indexed geometry, never editable scene authority. */
public record EditorMeshBasis(long faceId,Normal tangent,Normal bitangent,Normal normal,double x,double y,double z) {
    public EditorMeshBasis {
        Objects.requireNonNull(tangent);Objects.requireNonNull(bitangent);Objects.requireNonNull(normal);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(dot(tangent,tangent)-1)>1e-9||Math.abs(dot(bitangent,bitangent)-1)>1e-9||Math.abs(dot(normal,normal)-1)>1e-9||Math.abs(dot(tangent,bitangent))>1e-9||Math.abs(dot(tangent,normal))>1e-9||Math.abs(dot(bitangent,normal))>1e-9||dot(cross(tangent,bitangent),normal)<1-1e-9)throw new IllegalArgumentException("Invalid surface transform basis");
    }
    public static EditorMeshBasis face(EditorTopologyProject.State state,long id){
        var face=state.faces().stream().filter(f->f.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Pick an active native face for surface-normal axes"));
        var vertices=new HashMap<Long,EditorTopologyProject.Vertex>();for(var v:state.vertices())vertices.put(v.id(),v);var a=vertices.get(face.a().vertex());var b=vertices.get(face.b().vertex());var c=vertices.get(face.c().vertex());
        var tangent=EditorMeshNormals.unit(new Normal(b.x()-a.x(),b.y()-a.y(),b.z()-a.z()));var other=new Normal(c.x()-a.x(),c.y()-a.y(),c.z()-a.z());var normal=EditorMeshNormals.unit(cross(tangent,other));var bitangent=EditorMeshNormals.unit(cross(normal,tangent));
        return new EditorMeshBasis(id,tangent,bitangent,normal,a.x()/3+b.x()/3+c.x()/3,a.y()/3+b.y()/3+c.y()/3,a.z()/3+b.z()/3+c.z()/3);
    }
    private static double dot(Normal a,Normal b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    private static Normal cross(Normal a,Normal b){return new Normal(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
    public Normal axis(int index){return switch(index){case 0->tangent;case 1->bitangent;case 2->normal;default->throw new IllegalArgumentException("Surface axis must be0..2");};}
    public double component(double vx,double vy,double vz,int index){var n=axis(index);return vx*n.x()+vy*n.y()+vz*n.z();}
    public double world(double a,double b,double c,int index){return switch(index){case 0->a*tangent.x()+b*bitangent.x()+c*normal.x();case 1->a*tangent.y()+b*bitangent.y()+c*normal.y();case 2->a*tangent.z()+b*bitangent.z()+c*normal.z();default->throw new IllegalArgumentException("World component must be0..2");};}
    public Normal transformNormal(Normal value,EditorTopologyProject.Transform operation,int axis,double amount){
        if(value==null||operation==EditorTopologyProject.Transform.SCALE_UNIFORM)return value;
        var local=new Normal(dot(value,tangent),dot(value,bitangent),dot(value,normal));local=operation==EditorTopologyProject.Transform.ROTATE?EditorMeshNormals.rotate(local,axis,StrictMath.toRadians(amount)):EditorMeshNormals.scale(local,axis,amount);
        return new Normal(world(local.x(),local.y(),local.z(),0),world(local.x(),local.y(),local.z(),1),world(local.x(),local.y(),local.z(),2));
    }
}
