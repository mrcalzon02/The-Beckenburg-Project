package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.*;

/** Closed native bevel geometry, preserved attributes, transactions and actual interchange. */
public final class EditorMeshBevelTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-8,a+" != "+b);}
    private interface Action{void run()throws Exception;}
    private static void refuse(Action a)throws Exception{try{a.run();throw new AssertionError("Expected bevel refusal");}catch(IllegalArgumentException|IOException expected){}}
    public static EditorMeshPipeline.Mesh box(){return EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.BOX,2,2,2,12,1));}
    private static EditorMeshPipeline.Mesh attributedBox()throws IOException{
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,0xffff0000);image.setRGB(1,0,0xff00ff00);image.setRGB(0,1,0xff0000ff);image.setRGB(1,1,0xffffffff);var png=new ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",png);var material=EditorMeshMaterial.fromPng("bevel paint",png.toByteArray(),10497,10497,EditorMeshMaterial.Alpha.OPAQUE,EditorMeshMaterial.Origin.TOP_LEFT);var second=EditorMeshMaterial.fromPng("second paint",png.toByteArray(),10497,10497,EditorMeshMaterial.Alpha.OPAQUE,EditorMeshMaterial.Origin.TOP_LEFT);var triangles=new ArrayList<EditorMeshPipeline.Triangle>();
        for(var t:box().triangles()){var n=EditorMeshNormals.face(t);var raw=new EditorMeshPipeline.Normal(n.x()*2,n.y()*2,n.z()*2);var corners=new ArrayList<EditorMeshPipeline.Vertex>();for(var v:List.of(t.a(),t.b(),t.c()))corners.add(new EditorMeshPipeline.Vertex(v.x(),v.y(),v.z(),v.u(),v.v(),raw));triangles.add(new EditorMeshPipeline.Triangle(t.part(),corners.get(0),corners.get(1),corners.get(2),triangles.size()%2==0?material.key():second.key()));}return new EditorMeshPipeline.Mesh("painted box",triangles,0,List.of(),List.of(material,second));
    }
    public static long vertex(State s,double x,double y,double z){return s.vertices().stream().filter(v->v.x()==x&&v.y()==y&&v.z()==z).findFirst().orElseThrow().id();}
    public static Face face(State s,long a,long b){return s.faces().stream().filter(f->{var ids=Set.of(f.a().vertex(),f.b().vertex(),f.c().vertex());return ids.contains(a)&&ids.contains(b);}).findFirst().orElseThrow();}
    private static void geometry(EditPreview edit,State old,Set<Long> removed,double sum,double divisor,String capMaterial){
        var s=edit.snapshot();check(edit.topology().manifold()&&edit.topology().boundaryEdges()==0,"Bevel remains closed oriented manifold");
        check(s.vertices().size()-edit.topology().edges().size()/2+s.faces().size()==2,"Closed bevel retains sphere Euler characteristic");
        for(var v:old.vertices())if(!removed.contains(v.id()))check(s.vertices().contains(v),"Every retained original native vertex stays exact");
        var ids=new HashSet<Long>();for(var v:old.vertices())ids.add(v.id());for(var v:s.vertices())if(!ids.contains(v.id()))near((v.x()+v.y()+(removed.size()==1?v.z():0))/divisor,sum/divisor);
        for(var f:old.faces())if(List.of(f.a(),f.b(),f.c()).stream().noneMatch(c->removed.contains(c.vertex())))check(s.faces().contains(f),"Uncut native faces, IDs, materials, raw normals and UVs stay exact");
        for(var f:s.faces())for(var c:List.of(f.a(),f.b(),f.c()))if(ids.contains(c.vertex())){boolean retained=old.faces().stream().flatMap(o->List.of(o.a(),o.b(),o.c()).stream()).anyMatch(c::equals);check(retained,"Original retained corner attributes stay exact");}
        var oldFaceIds=new HashSet<Long>();for(var f:old.faces())oldFaceIds.add(f.id());var byId=new HashMap<Long,Vertex>();for(var v:s.vertices())byId.put(v.id(),v);int caps=0;
        for(var f:s.faces())if(!oldFaceIds.contains(f.id())&&List.of(f.a(),f.b(),f.c()).stream().noneMatch(c->ids.contains(c.vertex()))){caps++;var a=byId.get(f.a().vertex());var b=byId.get(f.b().vertex());var c=byId.get(f.c().vertex());double nx=(b.y()-a.y())*(c.z()-a.z())-(b.z()-a.z())*(c.y()-a.y()),ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z()),nz=(b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x());check(nx+ny+(removed.size()==1?nz:0)>0,"Every cap triangle faces the removed convex support");check(f.material().equals(capMaterial),"Cap inherits actual selected face material");}check(caps>0,"Bevel produces native cap triangles");
        var oldPoints=new HashMap<Long,Vertex>();for(var v:old.vertices())oldPoints.put(v.id(),v);
        for(var original:old.faces()){
            var clipped=s.faces().stream().filter(f->f.id()==original.id()).findFirst().orElseThrow();var originalCorners=List.of(original.a(),original.b(),original.c());
            for(var c:List.of(clipped.a(),clipped.b(),clipped.c()))if(!ids.contains(c.vertex())){var point=byId.get(c.vertex());boolean matched=false;
                for(int i=0;i<3;i++){var ca=originalCorners.get(i);var cb=originalCorners.get((i+1)%3);var a=oldPoints.get(ca.vertex());var b=oldPoints.get(cb.vertex());double dx=b.x()-a.x(),dy=b.y()-a.y(),dz=b.z()-a.z(),t=((point.x()-a.x())*dx+(point.y()-a.y())*dy+(point.z()-a.z())*dz)/(dx*dx+dy*dy+dz*dz);if(t<=0||t>=1||Math.abs(point.x()-a.x()-t*dx)+Math.abs(point.y()-a.y()-t*dy)+Math.abs(point.z()-a.z()-t*dz)>1e-8)continue;near(c.u(),ca.u()+(cb.u()-ca.u())*t);near(c.v(),ca.v()+(cb.v()-ca.v())*t);if(ca.normal()!=null&&cb.normal()!=null){var n=EditorMeshNormals.unit(ca.normal());near(c.normal().x(),n.x());near(c.normal().y(),n.y());near(c.normal().z(),n.z());}matched=true;break;}check(matched,"Every cut corner comes from its actual original edge with interpolated seam UV/normal");
            }
        }
        check(s.materials().equals(old.materials())&&s.frames().equals(old.frames())&&s.glbSource()==old.glbSource(),"Native material/source/frame ownership retained");
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-bevel-");try{
            var p=EditorTopologyProject.fromMesh(attributedBox(),"0".repeat(64),root.resolve("box.btop"));var plain=p.snapshot();p.restore(new State(plain.sceneId(),plain.sourceFingerprint(),plain.name(),plain.sourceBytes(),plain.vertices(),plain.faces(),plain.materials(),null,Map.of(plain.vertices().get(0).part(),EditorMeshFrame.IDENTITY)));var initial=p.snapshot();long a=vertex(initial,1,1,-1),b=vertex(initial,1,1,1);var active=face(initial,a,b);var edit=p.previewBevel(a,b,active.id(),.2);
            check(p.snapshot()==initial,"Bevel preview does not mutate native state");geometry(edit,initial,Set.of(a,b),2-.2*Math.sqrt(2),Math.sqrt(2),active.material());
            var history=new EditorHistory<>(p,20);history.execute(()->p.applyPreview(edit));var beveled=p.snapshot();check(history.undo()&&p.snapshot().equals(initial)&&history.redo()&&p.snapshot().equals(beveled),"Bevel shares exact one-step native history");
            p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(beveled),"Native fresh reopen retains actual bevel");var obj=new ByteArrayOutputStream();EditorObjBundle.write(beveled,obj);EditorObjBundle.validate(beveled,obj.toByteArray());byte[] glb=EditorGlbWriter.encode(p.mesh());EditorGlbWriter.validate(p.mesh(),glb);
            p.restore(initial);var corner=p.previewBevel(b,b,face(initial,b,b).id(),.2);geometry(corner,initial,Set.of(b),3-.2*Math.sqrt(3),Math.sqrt(3),face(initial,b,b).material());p.cancelPreview(corner);refuse(()->p.applyPreview(corner));var stale=p.previewBevel(a,b,active.id(),.2);p.restore(initial);refuse(()->p.applyPreview(stale));var other=EditorTopologyProject.fromMesh(box(),"1".repeat(64),root.resolve("other.btop"));refuse(()->other.applyPreview(p.previewBevel(a,b,active.id(),.2)));
            long epoch=p.revision();for(double depth:new double[]{0,-.1,Double.NaN,Double.POSITIVE_INFINITY,3})refuse(()->p.previewBevel(a,b,active.id(),depth));refuse(()->p.previewBevel(999,b,active.id(),.2));refuse(()->p.previewBevel(a,b,999,.2));
            var first=initial.faces().get(0);boolean diagonalRefused=false;var corners=List.of(first.a(),first.b(),first.c());for(int i=0;i<3;i++){long x=corners.get(i).vertex(),y=corners.get((i+1)%3).vertex();try{p.previewBevel(x,y,first.id(),.2);}catch(IllegalArgumentException bad){if(bad.getMessage().contains("Coplanar"))diagonalRefused=true;}}check(diagonalRefused,"Coplanar triangulation diagonal explicitly refused");check(p.snapshot()==initial&&p.revision()==epoch,"Refused previews leave native epoch unchanged");
            var plane=EditorTopologyProject.fromMesh(EditorMeshPrimitives.preview(new EditorMeshPrimitives.Parameters(EditorMeshPrimitives.Kind.PLANE,2,2,2,12,1)),"2".repeat(64),root.resolve("plane.btop"));refuse(()->plane.previewBevel(0,1,0,.2));
            var source=EditorTopologyProject.fromGlb("original.glb",EditorGlbWriter.encode(new EditorMeshPipeline.Mesh(p.mesh().name(),p.mesh().triangles(),0,p.mesh().cornerVertexIds(),p.mesh().materials(),null,p.mesh().faceIds(),Map.of(initial.vertices().get(0).part(),EditorMeshFrame.IDENTITY))),root.resolve("source.btop"));var sourceBefore=source.snapshot();long sa=vertex(sourceBefore,1,1,-1),sb=vertex(sourceBefore,1,1,1);source.applyPreview(source.previewBevel(sa,sb,face(sourceBefore,sa,sb).id(),.2));byte[] sourceGlb=EditorGlbWriter.encode(source.mesh());EditorGlbWriter.validate(source.mesh(),sourceGlb);check(source.snapshot().glbSource()==sourceBefore.glbSource(),"Source-backed bevel retains original GLB document");
            if(args.length>0&&!args[0].equals("--stress")){Path out=Path.of(args[0]);Files.createDirectories(out);p.restore(beveled);Files.write(out.resolve("edge.btop"),p.snapshotBytes());Files.write(out.resolve("edge.glb"),glb);Files.write(out.resolve("edge.obj.zip"),obj.toByteArray());p.restore(corner.snapshot());Files.write(out.resolve("corner.btop"),p.snapshotBytes());Files.write(out.resolve("source-edge.glb"),sourceGlb);}
            if(args.length>0&&args[0].equals("--stress")){
                var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();for(int i=0;i<9999;i++){long offset=i*8L;String part="cube-"+i;for(var v:initial.vertices())vertices.add(new Vertex(offset+v.id(),part,v.x()+i*4,v.y(),v.z()));for(var f:initial.faces())faces.add(new Face(i*12L+f.id(),part,new Corner(offset+f.a().vertex(),f.a().u(),f.a().v()),new Corner(offset+f.b().vertex(),f.b().u(),f.b().v()),new Corner(offset+f.c().vertex(),f.c().u(),f.c().v())));}
                p.restore(new State(initial.sceneId(),initial.sourceFingerprint(),"maximum",0,vertices,faces));var maximum=p.previewBevel(a,b,active.id(),.2);check(maximum.snapshot().faces().size()<=120000&&maximum.snapshot().faces().size()>119988&&maximum.topology().boundaryEdges()==0,"Near-maximum closed native bevel stays bounded under256MiB");
            }
            System.out.println("PASS native edge/corner bevel: closed actual cut/cap, stable original IDs/attributes, atomic history, refusal guards, native/OBJ/GLB and original source-backed runtime geometry");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
