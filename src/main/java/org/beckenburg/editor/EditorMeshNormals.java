package org.beckenburg.editor;

import org.beckenburg.editor.EditorMeshPipeline.*;

/** Native corner-normal math shared by human edits, staged interchange and actual render output. */
public final class EditorMeshNormals {
    private EditorMeshNormals(){}
    public static Normal unit(Normal n){double length=Math.hypot(Math.hypot(n.x(),n.y()),n.z());return new Normal(n.x()/length,n.y()/length,n.z()/length);}
    public static Normal face(Triangle t){return unit(cross(edge(t.a(),t.b()),edge(t.a(),t.c())));}
    public static Normal effective(Triangle t,Vertex corner){return corner.normal()==null?face(t):corner.normal();}
    public static Normal rotate(Normal n,int axis,double radians){
        if(n==null)return null;double x=n.x(),y=n.y(),z=n.z(),c=StrictMath.cos(radians),s=StrictMath.sin(radians);
        return axis==0?new Normal(x,c*y-s*z,s*y+c*z):axis==1?new Normal(c*x+s*z,y,-s*x+c*z):new Normal(c*x-s*y,s*x+c*y,z);
    }
    public static Normal scale(Normal n,int axis,double factor){if(n==null)return null;var u=unit(n);return unit(new Normal(axis==0?u.x()/factor:u.x(),axis==1?u.y()/factor:u.y(),axis==2?u.z()/factor:u.z()));}
    private static Normal edge(Vertex a,Vertex b){return new Normal(b.x()-a.x(),b.y()-a.y(),b.z()-a.z());}
    private static Normal cross(Normal a,Normal b){return new Normal(a.y()*b.z()-a.z()*b.y(),a.z()*b.x()-a.x()*b.z(),a.x()*b.y()-a.y()*b.x());}
    private static double dot(Normal a,Normal b){return a.x()*b.x()+a.y()*b.y()+a.z()*b.z();}
    /** Per-triangle affine differential for partial vertex edits: maps both edges and unit face depth. */
    public static Triangle transport(Triangle before,Triangle after){
        if(before.a().normal()==null&&before.b().normal()==null&&before.c().normal()==null)return after;
        if(before.a().x()==after.a().x()&&before.a().y()==after.a().y()&&before.a().z()==after.a().z()&&before.b().x()==after.b().x()&&before.b().y()==after.b().y()&&before.b().z()==after.b().z()&&before.c().x()==after.c().x()&&before.c().y()==after.c().y()&&before.c().z()==after.c().z())return after;
        var oldA=edge(before.a(),before.b());var oldB=edge(before.a(),before.c());var oldN=face(before);
        var a=edge(after.a(),after.b());var b=edge(after.a(),after.c());var n=face(after);double determinant=dot(a,cross(b,n));
        if(!Double.isFinite(determinant)||determinant<=0)throw new IllegalArgumentException("Invalid normal differential");
        var dualA=cross(b,n);var dualB=cross(n,a);
        return new Triangle(after.part(),transport(before.a(),after.a(),oldA,oldB,oldN,dualA,dualB,n,determinant),transport(before.b(),after.b(),oldA,oldB,oldN,dualA,dualB,n,determinant),transport(before.c(),after.c(),oldA,oldB,oldN,dualA,dualB,n,determinant),after.material());
    }
    private static Vertex transport(Vertex before,Vertex after,Normal oldA,Normal oldB,Normal oldN,Normal dualA,Normal dualB,Normal n,double determinant){
        if(before.normal()==null)return after;var normal=unit(before.normal());double a=dot(oldA,normal)/determinant,b=dot(oldB,normal)/determinant,c=dot(oldN,normal);
        var updated=unit(new Normal(a*dualA.x()+b*dualB.x()+c*n.x(),a*dualA.y()+b*dualB.y()+c*n.y(),a*dualA.z()+b*dualB.z()+c*n.z()));
        return new Vertex(after.x(),after.y(),after.z(),after.u(),after.v(),updated);
    }
}
