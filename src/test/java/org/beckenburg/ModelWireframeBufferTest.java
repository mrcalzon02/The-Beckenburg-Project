package org.beckenburg;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import org.beckenburg.editor.*;
import org.beckenburg.editor.EditorMeshPipeline.Triangle;

/** Compare the cached viewport drawing to independently projected native triangles. */
public final class ModelWireframeBufferTest {
    private record View(int offset,int scale) implements ModelWireframeBuffer.View {
        public int x(double x){return offset+(int)Math.round(x*scale);}
        public int y(double y){return offset+(int)Math.round(y*scale);}
    }
    private static final class Count implements EditorMeshPicking.Projector {
        final EditorMeshPicking.Projector projector;
        int calls;
        Count(EditorMeshPicking.Projector projector){this.projector=projector;}
        public EditorMeshPicking.Point project(double x,double y,double z){calls++;return projector.project(x,y,z);}
    }
    private static void frame(ModelWireframeBuffer buffer,List<Triangle> triangles,Count camera,View view,int expected) {
        var cached=new BufferedImage(160,160,BufferedImage.TYPE_INT_ARGB);
        var reference=new BufferedImage(160,160,BufferedImage.TYPE_INT_ARGB);
        var cg=cached.createGraphics();var rg=reference.createGraphics();
        try {
            var color=new Color(113,168,188,120);cg.setColor(color);rg.setColor(color);camera.calls=0;
            buffer.draw(cg,triangles,camera,view);
            if(camera.calls!=expected)throw new AssertionError("Expected "+expected+" projections, observed "+camera.calls);
            int stride=Math.max(1,triangles.size()/12000);
            for(int i=0;i<triangles.size();i+=stride){
                var t=triangles.get(i);int[] xs=new int[3],ys=new int[3];int corner=0;
                for(var v:List.of(t.a(),t.b(),t.c())){var point=camera.projector.project(v.x(),v.y(),v.z());xs[corner]=view.x(point.x());ys[corner++]=view.y(point.y());}
                rg.drawPolygon(xs,ys,3);
            }
            if(!Arrays.equals(cached.getRGB(0,0,160,160,null,0,160),reference.getRGB(0,0,160,160,null,0,160)))throw new AssertionError("Cached pixels differ from direct native projection");
        }finally{cg.dispose();rg.dispose();}
    }
    public static void main(String[] args){
        var mesh=EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,2,2,2,12,2));
        var project=EditorTopologyProject.fromMesh(mesh,"0".repeat(64),Path.of("wireframe.btop"));
        var nativeTriangles=project.mesh().triangles();var buffer=new ModelWireframeBuffer();var camera=new Count(EditorMeshPicking.Projection.TOP);
        var view=new View(72,46);frame(buffer,nativeTriangles,camera,view,24);frame(buffer,nativeTriangles,camera,view,0);
        var center=project.snapshot().vertices().stream().filter(v->v.x()==0&&v.z()==0).findFirst().orElseThrow();
        int incident=(int)project.mesh().cornerVertexIds().stream().filter(id->id==center.id()).count();
        project.moveVertex(center.id(),.25,center.y(),center.z());
        var changed=project.mesh().triangles();frame(buffer,changed,camera,view,incident);
        var resized=new View(80,50);frame(buffer,changed,camera,resized,0);
        project.applyPreview(project.previewUvTransform(project.snapshot().faces().get(0).id(),false,EditorTopologyProject.UvTransform.MOVE,.1,.1,false));
        var textured=project.mesh().triangles();frame(buffer,textured,camera,resized,0);
        var rotated=new Count(EditorMeshPicking.Projection.ISOMETRIC);frame(buffer,textured,rotated,resized,24);
        var perspective=new Count(EditorMeshPicking.cameraForTriangles(textured,35,15));
        frame(buffer,textured,perspective,resized,24);
        var reordered=new ArrayList<>(textured);Collections.reverse(reordered);
        int dirty=0;
        for(int i=0;i<textured.size();i++){
            var a=textured.get(i);var b=reordered.get(i);var left=List.of(a.a(),a.b(),a.c());var right=List.of(b.a(),b.b(),b.c());
            for(int j=0;j<3;j++){var v=left.get(j);var w=right.get(j);if(v.x()!=w.x()||v.y()!=w.y()||v.z()!=w.z())dirty++;}
        }
        frame(buffer,List.copyOf(reordered),perspective,resized,dirty);
        buffer.clear();frame(buffer,textured,rotated,resized,24);
        var maximum=new ArrayList<>(Collections.nCopies(EditorMeshPipeline.MAX_TRIANGLES,textured.get(0)));
        frame(buffer,maximum,rotated,resized,36000);
        var revised=new ArrayList<>(maximum);var t=maximum.get(10);var a=t.a();
        revised.set(10,new Triangle(t.part(),new EditorMeshPipeline.Vertex(a.x()+.25,a.y(),a.z(),a.u(),a.v()),t.b(),t.c()));
        frame(buffer,List.copyOf(revised),rotated,resized,1);
        System.out.println("PASS native wireframe pixels and projection counts: hover 0, local center "+incident+"/24, resized/UV 0, new camera 24; maximum sampled edit 1/36000");
    }
}
