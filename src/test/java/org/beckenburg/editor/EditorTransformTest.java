package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class EditorTransformTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static boolean near(double a,double b){return Math.abs(a-b)<1e-10;}
    private static void refused(Runnable action){try{action.run();throw new AssertionError("Invalid transform accepted");}catch(IllegalArgumentException expected){}}
    private static EditorTopologyProject maximum(Path target){
        var triangles=new ArrayList<EditorMeshPipeline.Triangle>();var identities=new ArrayList<Long>();
        for(int y=0;y<300;y++)for(int x=0;x<200;x++){
            var a=new EditorMeshPipeline.Vertex(x,y,0,x,y);var b=new EditorMeshPipeline.Vertex(x+1,y,0,x+1,y);var c=new EditorMeshPipeline.Vertex(x,y+1,0,x,y+1);var d=new EditorMeshPipeline.Vertex(x+1,y+1,0,x+1,y+1);
            triangles.add(new EditorMeshPipeline.Triangle("grid",a,b,c));triangles.add(new EditorMeshPipeline.Triangle("grid",c,b,d));
        }
        for(int i=0;i<360000;i++)identities.add((long)i);
        return EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("maximum",triangles,0,identities),"3".repeat(64),target);
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-transform-");
        try{
            byte[] raw="v 0 0 0\nv 1 0 0\nv 0 1 0\nvt .2 .3\nf 1/1 2/1 3/1\n".getBytes(StandardCharsets.UTF_8);
            var p=EditorTopologyProject.fromMesh(EditorMeshPipeline.read("triangle.obj",raw),EditorMeshPipeline.sourceFingerprint(raw),root.resolve("mesh.btop"));
            var before=p.snapshot();var ids=before.vertices().stream().map(EditorTopologyProject.Vertex::id).toList();var history=new EditorHistory<>(p,8);
            var aroundX=p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,0,90,true).snapshot();
            var aroundZ=p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,2,90,true).snapshot();
            check(near(aroundX.vertices().get(2).z(),1)&&near(aroundX.vertices().get(1).x(),1)&&near(aroundZ.vertices().get(1).y(),1)&&near(aroundZ.vertices().get(2).x(),-1),"All three global axes use right-handed rotation");
            var subset=p.previewTransform(List.of(ids.get(1)),EditorTopologyProject.Transform.ROTATE,1,90,true).snapshot();check(subset.vertices().get(0).equals(before.vertices().get(0))&&subset.vertices().get(2).equals(before.vertices().get(2)),"Unselected native vertices remain exact");
            var rotation=p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,1,90,true);
            check(p.snapshot()==before&&!history.canUndo(),"Transform preview never mutates native authority/history");
            var rotated=rotation.snapshot();check(rotated.faces().equals(before.faces())&&near(rotated.vertices().get(1).x(),0)&&near(rotated.vertices().get(1).z(),-1),"Right-handed global Y rotation retains stable identities, corner UVs and winding");
            var t=rotation.mesh().triangles().get(0);double nx=(t.b().y()-t.a().y())*(t.c().z()-t.a().z())-(t.b().z()-t.a().z())*(t.c().y()-t.a().y());check(near(nx,1),"Derived hard-edge normal follows rotated geometry");
            history.execute(()->p.applyPreview(rotation));check(history.undo()&&p.snapshot().equals(before)&&history.redo()&&p.snapshot().equals(rotated),"One undo/redo installs exact reviewed geometry");
            var scale=p.previewTransform(ids,EditorTopologyProject.Transform.SCALE_UNIFORM,0,2,false);var scaled=scale.snapshot();
            for(int axis=0;axis<3;axis++){double a=0,b=0;for(int i=0;i<3;i++){var v=rotated.vertices().get(i);var w=scaled.vertices().get(i);a+=axis==0?v.x():axis==1?v.y():v.z();b+=axis==0?w.x():axis==1?w.y():w.z();}check(near(a,b),"Selection centroid stays fixed");}
            p.cancelPreview(scale);check(p.snapshot()==rotated,"Cancel exact state");refused(()->p.applyPreview(scale));
            var axis=p.previewTransform(ids,EditorTopologyProject.Transform.SCALE_AXIS,1,3,true);check(near(axis.snapshot().vertices().get(2).y(),3)&&axis.snapshot().faces().equals(before.faces()),"Axis scale preserves UVs and topology");
            var old=p.snapshot();p.moveVertex(ids.get(0),.1,0,0);refused(()->p.applyPreview(axis));check(p.snapshot()!=old,"Stale transform cannot overwrite an intervening native edit");
            refused(()->p.previewTransform(List.of(9999L),EditorTopologyProject.Transform.ROTATE,0,90,true));
            for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1000001})refused(()->p.previewTransform(ids,EditorTopologyProject.Transform.SCALE_AXIS,0,bad,true));
            refused(()->p.previewTransform(ids,EditorTopologyProject.Transform.ROTATE,3,90,true));
            p.restore(before);refused(()->p.previewTransform(List.of(ids.get(1)),EditorTopologyProject.Transform.ROTATE,2,0,true));
            p.restore(rotated);p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(rotated),"Native transformed save/reopen retains exact data");
            if(args.length>0){
                var maximum=maximum(root.resolve("maximum.btop"));var candidate=maximum.previewTransform(maximum.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList(),EditorTopologyProject.Transform.ROTATE,1,90,true);
                check(candidate.snapshot().vertices().size()==360000&&candidate.snapshot().faces().size()==120000&&maximum.snapshot()!=candidate.snapshot(),"Maximum native transform is bounded and preview-only");
            }
            System.out.println("PASS native rotation/axis/uniform scale: global axes/pivots, transformed facing, exact preview/history, UV/ID preservation, invalid/stale controls and native persistence");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
