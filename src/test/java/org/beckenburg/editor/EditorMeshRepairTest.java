package org.beckenburg.editor;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class EditorMeshRepairTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void refused(Runnable action){try{action.run();throw new AssertionError("Invalid repair accepted");}catch(IllegalArgumentException expected){}}
    private static EditorTopologyProject project(String text,Path path)throws Exception{byte[] raw=text.getBytes(StandardCharsets.UTF_8);return EditorTopologyProject.fromMesh(EditorMeshPipeline.read("repair.obj",raw),EditorMeshPipeline.sourceFingerprint(raw),path);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-mesh-repair-");
        try{
            String seam="""
                o surface
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 0 1 0
                v 1 0 0
                v 1 1 0
                vt 0 0
                vt 1 0
                vt 0 1
                vt .25 .25
                vt .75 .25
                vt 1 1
                f 1/1 2/2 3/3
                f 4/4 5/5 6/6
                """;
            var p=project(seam,root.resolve("seam.btop"));var before=p.snapshot();var original=p.mesh();var history=new EditorHistory<>(p,8);
            var ids=before.vertices().stream().map(EditorTopologyProject.Vertex::id).toList();var preview=p.previewWeld(ids);
            check(p.snapshot()==before&&!history.canUndo(),"Weld preview does not alter native authority");
            check(preview.snapshot().vertices().size()==4&&preview.snapshot().faces().size()==2&&preview.topology().manifold()&&preview.topology().boundaryEdges()==4&&preview.topology().uvSeams()==1,"Weld joins source seam while retaining per-corner UV seam");
            check(preview.mesh().equals(original),"Explicit identity weld cannot change rendered positions, winding or UV");
            history.execute(()->p.applyPreview(preview));var welded=p.snapshot();
            check(history.undo()&&p.snapshot().equals(before)&&history.redo()&&p.snapshot().equals(welded),"One history step restores exact pre/post weld identities");
            p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(welded),"Native persistence retains welded IDs and corner UVs");
            refused(()->p.previewWeld(p.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList()));
            var touch=project("o surface\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 0\nv -1 0 0\nv 0 -1 0\nf 1 2 3\nf 4 5 6\n",root.resolve("touch.btop"));
            var untouched=touch.snapshot();refused(()->touch.previewWeld(untouched.vertices().stream().map(EditorTopologyProject.Vertex::id).toList()));check(touch.snapshot()==untouched,"Point-only contact cannot become a nonmanifold shared vertex");
            var parts=project(seam.replace("f 4/4 5/5 6/6","o other\nf 4/4 5/5 6/6"),root.resolve("parts.btop"));refused(()->parts.previewWeld(parts.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList()));
            String degenerate="o surface\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 0\nf 1 2 3\nf 1 4 2\n";
            var bad=project(degenerate,root.resolve("bad.btop"));var invalid=bad.snapshot();check(!bad.topology().manifold(),"Actual source contains a degenerate native triangle");
            var repair=bad.previewRemoveDegenerates();check(bad.snapshot()==invalid&&repair.snapshot().vertices().equals(invalid.vertices())&&repair.snapshot().faces().equals(List.of(invalid.faces().get(0))),"Repair explicitly removes only invalid face and preserves all vertex/surviving face data");
            check(repair.topology().manifold(),"Degenerate repair yields valid native topology");bad.applyPreview(repair);bad.save();check(EditorTopologyProject.open(bad.documentPath()).snapshot().equals(bad.snapshot()),"Repaired native document roundtrip");
            var collapsed=project(degenerate,root.resolve("collapsed.btop"));var drop=collapsed.previewWeld(collapsed.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList());check(drop.snapshot().vertices().size()==3&&drop.snapshot().faces().size()==1&&drop.topology().manifold(),"Weld preview explicitly removes collapsed zero-area face");
            if(args.length>0){
                var triangles=new ArrayList<EditorMeshPipeline.Triangle>();var origins=new ArrayList<Long>();
                for(int y=0;y<300;y++)for(int x=0;x<200;x++){
                    var a=new EditorMeshPipeline.Vertex(x,y,0,x,y);var b=new EditorMeshPipeline.Vertex(x+1,y,0,x+1,y);var c=new EditorMeshPipeline.Vertex(x,y+1,0,x,y+1);var d=new EditorMeshPipeline.Vertex(x+1,y+1,0,x+1,y+1);
                    triangles.add(new EditorMeshPipeline.Triangle("grid",a,b,c));triangles.add(new EditorMeshPipeline.Triangle("grid",c,b,d));
                }
                for(int i=0;i<360000;i++)origins.add((long)i);
                var maximum=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("maximum",triangles,0,origins),"3".repeat(64),root.resolve("maximum.btop"));triangles=null;origins=null;
                var weldedMax=maximum.previewWeld(maximum.snapshot().vertices().stream().map(EditorTopologyProject.Vertex::id).toList());
                check(weldedMax.snapshot().vertices().size()==60501&&weldedMax.snapshot().faces().size()==120000,"Maximum exact native weld counts under256MiB");
            }
            System.out.println("PASS native explicit weld/degenerate repair: unchanged geometry/UVs, shared seam, manifold controls, stable surviving IDs, exact preview/history and native save/reopen");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
