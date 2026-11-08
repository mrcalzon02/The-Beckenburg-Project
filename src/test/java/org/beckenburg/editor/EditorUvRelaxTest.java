package org.beckenburg.editor;

import java.nio.file.*;
import java.util.*;

public final class EditorUvRelaxTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static Path source(Path dir)throws Exception{
        Files.createDirectories(dir);
        Files.copy(Path.of("development/quality/evidence/material-pencil-2026-11-07/checker.png"),
            dir.resolve("checker.png"),StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(dir.resolve("fan.mtl"),"newmtl checker\nKd 1 1 1\nmap_Kd checker.png\n");
        return Files.writeString(dir.resolve("fan.obj"),"""
            mtllib fan.mtl
            o fan
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            v .5 .5 0
            v .5 2 0
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            vt .8 .25
            vt 2 1
            vt 3 1
            vt 2.5 2
            usemtl checker
            f 1/1 2/2 5/5
            f 2/2 3/3 5/5
            f 3/3 4/4 5/5
            f 4/4 1/1 5/5
            f 4/6 3/7 6/8
            """);
    }
    public static void verify(EditorTopologyProject.State before,EditorTopologyProject.State after){
        check(before.vertices()==after.vertices() && before.faces().get(4)==after.faces().get(4),"Changed geometry or crossed a UV seam");
        for(int i=0;i<4;i++){
            var old=before.faces().get(i);var f=after.faces().get(i);
            check(old.a()==f.a() && old.b()==f.b() && old.c().vertex()==f.c().vertex(),"Changed a pinned boundary corner or vertex identity");
            check(Math.abs(f.c().u()-.5)<1e-8 && Math.abs(f.c().v()-.5)<1e-8,"Interior UV did not reach the harmonic center");
            double area=(f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u());
            check(area>0,"Relaxation flipped a triangle");
        }
    }
    private static void maximum(Path dir)throws Exception{
        var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("grid",List.of(),0),"0".repeat(64),dir.resolve("grid.btop"));
        var base=doc.snapshot();
        var material=EditorMeshMaterial.fromPng("grid.png",Files.readAllBytes(dir.resolve("checker.png")),10497,10497,
            EditorMeshMaterial.Alpha.OPAQUE,EditorMeshMaterial.Origin.TOP_LEFT);
        var vertices=new ArrayList<EditorTopologyProject.Vertex>();
        var corners=new ArrayList<EditorTopologyProject.Corner>();
        var faces=new ArrayList<EditorTopologyProject.Face>();
        int center=100*301+150;
        for(int y=0;y<=200;y++)for(int x=0;x<=300;x++){
            int id=vertices.size();vertices.add(new EditorTopologyProject.Vertex(id,"grid",x,y,0));
            corners.add(new EditorTopologyProject.Corner(id,(x+(id==center?.3:0))/300,y/200d));
        }
        for(int y=0;y<200;y++)for(int x=0;x<300;x++){
            int a=y*301+x;
            faces.add(new EditorTopologyProject.Face(faces.size(),"grid",corners.get(a),corners.get(a+1),corners.get(a+302),material.key()));
            faces.add(new EditorTopologyProject.Face(faces.size(),"grid",corners.get(a),corners.get(a+302),corners.get(a+301),material.key()));
        }
        doc.restore(new EditorTopologyProject.State(base.sceneId(),base.sourceFingerprint(),"grid",0,vertices,faces,List.of(material)));
        var next=doc.previewRelaxInterior(0).snapshot();
        var corrected=next.faces().get((100*300+150)*2).a();
        check(Math.abs(corrected.u()-.5)<Math.abs(corners.get(center).u()-.5) && next.faces().get(0).a()==corners.get(0),
            "Maximum chart must reduce interior displacement and retain pinned boundary");
        System.out.println("PASS 120000-face fixed-boundary UV relaxation");
    }
    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("native-uv-relax-");
        try{
            var captured=EditorObjMaterials.capture(source(dir));
            var project=EditorTopologyProject.fromMesh(captured.mesh(),captured.sourceFingerprint(),dir.resolve("fan.btop"));
            var before=project.snapshot();
            var edit=project.previewRelaxInterior(before.faces().get(0).id());
            verify(before,edit.snapshot());
            project.applyPreview(edit);project.save();
            check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(project.snapshot()),"Native reopen lost relaxed corners");
            for(int index:List.of(0,4)){
                try{project.previewRelaxInterior(before.faces().get(index).id());throw new AssertionError("No-op or boundary-only edit accepted");}
                catch(IllegalArgumentException expected){}
            }
            project.restore(before);
            project.applyPreview(project.previewUvTransform(before.faces().get(0).id(),true,EditorTopologyProject.UvTransform.SCALE,-1,1,true));
            var mirror=project.previewRelaxInterior(before.faces().get(0).id()).snapshot();
            check(Math.abs(mirror.faces().get(0).c().u()+.5)<1e-8,"Mirrored chart lost orientation");
            if(args.length==1 && args[0].equals("--stress"))maximum(dir);
            System.out.println("PASS native UV interior relaxation: fixed boundary, seam exclusion, analytic center, mirrored winding and native reopen");
        }finally{
            try(var paths=Files.walk(dir)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
        }
    }
}
