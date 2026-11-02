package org.beckenburg.editor;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import org.beckenburg.editor.EditorMeshPipeline.*;

public final class EditorCornerNormalsTest {
    public static final String SOURCE="o panel\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nvt .25 .25\nvt .75 .25\nvt 1 1\nvn 0 0 2\nvn 1 0 1\nf 1/1/1 2/2/1 3/3/1\nf 3/4/-1 2/5/-1 4/6/-1\n";
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void near(double a,double b){check(Math.abs(a-b)<1e-10,"Normal component "+a+" differs from "+b);}
    private static EditorTopologyProject project(String raw,Path path)throws Exception{var bytes=raw.getBytes(StandardCharsets.UTF_8);return EditorTopologyProject.fromMesh(EditorMeshPipeline.read("normals.obj",bytes),EditorMeshPipeline.sourceFingerprint(bytes),path);}
    private interface IO{void run()throws Exception;}
    private static void refused(IO run)throws Exception{try{run.run();throw new AssertionError("Invalid normal input accepted");}catch(IOException|IllegalArgumentException expected){}}
    private static EditorTopologyProject maximum(Path target){
        var triangles=new ArrayList<Triangle>();var identities=new ArrayList<Long>();var normal=new Normal(0,0,1);
        for(int y=0;y<300;y++)for(int x=0;x<200;x++){
            var a=new Vertex(x,y,0,x,y,normal);var b=new Vertex(x+1,y,0,x+1,y,normal);var c=new Vertex(x,y+1,0,x,y+1,normal);var d=new Vertex(x+1,y+1,0,x+1,y+1,normal);
            triangles.add(new Triangle("grid",a,b,c));triangles.add(new Triangle("grid",c,b,d));
        }
        for(int i=0;i<360000;i++)identities.add((long)i);
        return EditorTopologyProject.fromMesh(new Mesh("normal maximum",triangles,0,identities),"3".repeat(64),target);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-corner-normals-");
        try{
            var p=project(SOURCE,root.resolve("mesh.btop"));var original=p.snapshot();var history=new EditorHistory<>(p,8);var ids=original.vertices().stream().map(EditorTopologyProject.Vertex::id).toList();
            check(original.vertices().size()==4&&p.topology().uvSeams()==1,"Shared native IDs retain separate UV and normal corner attributes");
            check(original.faces().get(0).a().normal().equals(new Normal(0,0,2))&&original.faces().get(1).a().normal().equals(new Normal(1,0,1)),"Authored magnitudes, negative normal references and normal seam preserved");
            p.save();byte[] nativeBytes=Files.readAllBytes(p.documentPath());check(ByteBuffer.wrap(nativeBytes).getInt(4)==2&&EditorTopologyProject.open(p.documentPath()).snapshot().equals(original),"Normal-bearing v2 roundtrip exact");
            p.save();check(Arrays.equals(nativeBytes,Files.readAllBytes(p.documentPath())),"Normal-bearing serialization deterministic");
            long identityEpoch=p.revision();refused(()->p.previewTransform(ids,EditorTopologyProject.Transform.SCALE_AXIS,0,1,true));refused(()->p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,1,360,true));check(p.snapshot()==original&&p.revision()==identityEpoch&&!history.canUndo(),"Identity transforms cannot normalize raw values, drift coordinates or change native epoch/history");
            var preview=p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,1,90,true);check(p.snapshot()==original,"Preview does not edit native normal authority");history.execute(()->p.applyPreview(preview));var rotated=p.snapshot();var n=rotated.faces().get(0).a().normal();near(n.x(),2);near(n.y(),0);near(n.z(),0);
            var scaled=p.previewTransform(ids,EditorTopologyProject.Transform.SCALE_AXIS,0,2,true);history.execute(()->p.applyPreview(scaled));n=p.snapshot().faces().get(1).a().normal();near(n.x(),1/Math.sqrt(5));near(n.z(),-2/Math.sqrt(5));
            check(history.undo()&&p.snapshot().equals(rotated)&&history.undo()&&p.snapshot().equals(original)&&history.redo()&&p.snapshot().equals(rotated),"Undo/redo restore exact normal-bearing snapshots");
            var beforeTranslation=p.snapshot().faces();p.translateVertices(ids,.25,0,0);check(p.snapshot().faces().equals(beforeTranslation),"Whole-face translation preserves exact authored directions");
            Path copy=root.resolve("copy.obj");p.exportObj(p.observe(),copy,null);EditorObjWriter.validate(p.snapshot(),Files.readAllBytes(copy));var imported=EditorMeshPipeline.read(copy);check(imported.triangles().equals(p.mesh().triangles()),"OBJ native roundtrip preserves every explicit normal and corner geometry/UV");
            var partial=project("v 0 0 0\nv 1 0 0\nv 0 1 0\nvn 0 0 3\nf 1//1 2//1 3//1\n",root.resolve("partial.btop"));partial.moveVertex(2,0,1,1);for(var c:List.of(partial.snapshot().faces().get(0).a(),partial.snapshot().faces().get(0).b(),partial.snapshot().faces().get(0).c())){near(c.normal().y(),-1/Math.sqrt(2));near(c.normal().z(),1/Math.sqrt(2));}
            var extrude=project(SOURCE,root.resolve("extrude.btop"));var cap=extrude.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,.25).snapshot();check(cap.faces().get(0).a().normal().equals(original.faces().get(0).a().normal())&&cap.faces().subList(2,cap.faces().size()).stream().allMatch(f->f.a().normal()==null&&f.b().normal()==null&&f.c().normal()==null),"Extruded cap retains authored normals; new sides derive their own facing");
            var inset=extrude.previewFaceEdit(0,EditorTopologyProject.FaceEdit.INSET,.25);check(inset.snapshot().faces().get(0).a().normal()!=null,"Inset retains transported corner normals");
            var legacy=project("v 0 0 0\nv 1 0 0\nv 0 1 0\nf 1 2 3\n",root.resolve("legacy.btop"));legacy.save();byte[] v1=Files.readAllBytes(legacy.documentPath());check(ByteBuffer.wrap(v1).getInt(4)==1,"No-normal project retains exact v1 schema");check(EditorTopologyProject.open(legacy.documentPath()).snapshot().equals(legacy.snapshot()),"V1 migration reads normals as absent");
            var stream=new ByteArrayInputStream(nativeBytes);var input=new DataInputStream(stream);input.readInt();input.readInt();input.readUTF();input.readUTF();input.readUTF();input.readLong();int vertexCount=input.readInt();for(int i=0;i<vertexCount;i++){input.readLong();input.readUTF();input.readDouble();input.readDouble();input.readDouble();}input.readInt();input.readLong();input.readUTF();input.readLong();input.readDouble();input.readDouble();int flagOffset=nativeBytes.length-stream.available();byte[] badFlag=nativeBytes.clone();badFlag[flagOffset]=2;refused(()->EditorTopologyProject.decode(badFlag));byte[] badNormal=nativeBytes.clone();ByteBuffer.wrap(badNormal).putDouble(flagOffset+1,Double.NaN);refused(()->EditorTopologyProject.decode(badNormal));
            byte[] future=nativeBytes.clone();ByteBuffer.wrap(future).putInt(4,5);refused(()->EditorTopologyProject.decode(future));
            for(String bad:new String[]{SOURCE.replace("vn 0 0 2","vn 0 0 0"),SOURCE.replace("vn 0 0 2","vn NaN 0 1"),SOURCE.replace("f 1/1/1","f 1/1/99")})refused(()->EditorMeshPipeline.read("bad.obj",bad.getBytes(StandardCharsets.UTF_8)));
            var exactSource=p.mesh();check(EditorMeshPipeline.regress(exactSource,0).optimized()==exactSource,"Level0 retains exact normal-bearing source");
            check(EditorMeshPipeline.regress(p.mesh(),1).optimized().triangles().stream().allMatch(t->t.a().normal()!=null&&t.b().normal()!=null&&t.c().normal()!=null),"Explicit reduction transports retained normals instead of dropping them");
            for(boolean authored:new boolean[]{true,false}){
                String invalid="v 0 0 0\nv 1 0 0\nv 2 0 0\n"+(authored?"vn 0 0 1\nf 1//1 2//1 3//1\n":"f 1 2 3\n");var degenerate=project(invalid,root.resolve("degenerate-"+authored+".btop"));Path destination=root.resolve("bad-export-"+authored+".obj");
                try{degenerate.exportObj(degenerate.observe(),destination,null);throw new AssertionError("Degenerate face exported, authored="+authored);}catch(IOException expected){check(!Files.exists(destination),"Invalid normal export preserves missing destination and returns structured I/O refusal");}
            }
            var stl=EditorMeshPipeline.read("normal.stl","solid X\nfacet normal 0 0 2\nouter loop\nvertex 0 0 0\nvertex 1 0 0\nvertex 0 1 0\nendloop\nendfacet\nendsolid\n".getBytes(StandardCharsets.US_ASCII));check(stl.triangles().get(0).a().normal().equals(new Normal(0,0,2)),"ASCII STL facet direction retained");
            var binary=ByteBuffer.allocate(134).order(ByteOrder.LITTLE_ENDIAN);binary.putInt(80,1);binary.putFloat(92,3);binary.putFloat(108,1);binary.putFloat(124,1);check(EditorMeshPipeline.read("binary.stl",binary.array()).triangles().get(0).a().normal().equals(new Normal(0,0,3)),"Binary STL facet normal retained");
            if(args.length>0){var maximum=maximum(root.resolve("maximum.btop"));var candidate=maximum.previewTransform(maximum.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList(),EditorTopologyProject.Transform.ROTATE,1,90,true);check(candidate.snapshot().vertices().size()==360000&&candidate.snapshot().faces().size()==120000,"Maximum normal-bearing native transform counts");near(candidate.snapshot().faces().get(119999).c().normal().x(),1);System.out.println("PASS maximum normal-bearing transform:120000 faces/360000 vertices under256MiB");}
            System.out.println("PASS authored corner normals: native IDs/UV seams, v1/v2/future guards, exact history/save/OBJ, transform inverse-transpose, partial deformation, cap/sides and STL/reduction preservation");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
