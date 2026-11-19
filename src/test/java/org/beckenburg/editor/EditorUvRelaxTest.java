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
        var packed=doc.previewPackMaterial(0,1,false).snapshot();
        check(packed.faces().size()==120000 && Math.abs(packed.faces().get(0).a().u()-.25)<1e-9,
            "Maximum material pack must place the island at its texel border");
        var flat=doc.previewUnwrap(0).snapshot();
        var f=flat.faces().get(0);
        double ux=f.b().u()-f.a().u(),uy=f.b().v()-f.a().v(),vx=f.c().u()-f.b().u(),vy=f.c().v()-f.b().v();
        check(Math.abs(ux*vx+uy*vy)<1e-12 && Math.abs(Math.hypot(ux,uy)-Math.hypot(vx,vy))<1e-10,"Maximum flat chart lost angles or edge scale");
        check(Math.abs(flat.faces().get((100*300+150)*2).a().u()-.5)<1e-9 && flat.faces().size()==120000,"Maximum chart interior did not unfold");
        System.out.println("PASS 120000-face UV relaxation, packing and LSCM unwrap");
    }
    public static void verifyPack(EditorTopologyProject.State before,EditorTopologyProject.State after){
        double[][] bounds={{4,4,0,0},{4,4,0,0}};
        double scale=0;
        for(int i=0;i<5;i++){
            var a=before.faces().get(i);var b=after.faces().get(i);
            double[] box=bounds[i<4?0:1];
            for(var c:List.of(b.a(),b.b(),b.c())){
                box[0]=Math.min(box[0],c.u()*4);box[1]=Math.min(box[1],c.v()*4);
                box[2]=Math.max(box[2],c.u()*4);box[3]=Math.max(box[3],c.v()*4);
            }
            if(i==0)scale=(b.b().u()-b.a().u())/(a.b().u()-a.a().u());
            var old=List.of(a.a(),a.b(),a.c());var next=List.of(b.a(),b.b(),b.c());
            for(int k=1;k<3;k++){
                check(scale>0 && Math.abs((next.get(k).u()-next.get(0).u())-scale*(old.get(k).u()-old.get(0).u()))<1e-10 &&
                    Math.abs((next.get(k).v()-next.get(0).v())-scale*(old.get(k).v()-old.get(0).v()))<1e-10,
                    "Packing must retain orientation and use one uniform scale");
            }
        }
        for(var b:bounds)check(b[0]>=1-1e-10&&b[1]>=1-1e-10&&b[2]<=3+1e-10&&b[3]<=3+1e-10,"Atlas border");
        var a=bounds[0];var b=bounds[1];
        check(Math.max(Math.max(b[0]-a[2],a[0]-b[2]),Math.max(b[1]-a[3],a[1]-b[3]))>=1-1e-10,"One texel gutter between islands");
        check(before.vertices()==after.vertices() && before.materials()==after.materials(),"Packing modified source geometry or texture");
    }
    private static void packing(EditorTopologyProject p)throws Exception{
        var before=p.snapshot();
        long id=before.faces().get(0).id();
        try{p.previewPackMaterial(id,1,true);throw new AssertionError("Packed oversized islands at original density");}
        catch(IllegalArgumentException expected){check(p.snapshot()==before,"Rejected packing mutated the document");}
        var preview=p.previewPackMaterial(id,1,false);verifyPack(before,preview.snapshot());
        check(p.previewPackMaterial(before.faces().get(4).id(),1,false).snapshot().equals(preview.snapshot()),"Packing depends on selected seed");
        p.applyPreview(preview);
        p.applyPreview(p.previewUvPins(id,2,true));
        var pinned=p.snapshot();
        for(int i=0;i<4;i++)check(pinned.faces().get(i).c().pinned(),"UV fan pin missed a shared corner");
        check(pinned.faces().get(4)==preview.snapshot().faces().get(4),"Pin crossed a seam");
        try{p.previewUvTransform(id,true,EditorTopologyProject.UvTransform.MOVE,1,0,true);throw new AssertionError("Moved a pinned UV");}
        catch(IllegalArgumentException expected){check(p.snapshot()==pinned,"Rejected move changed document");}
        p.applyPreview(p.previewUvPins(id,-1,true));
        var frozen=p.snapshot();p.save();
        check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(frozen),"Native reopen lost pins");
        p.applyPreview(p.previewUvTransform(before.faces().get(4).id(),true,EditorTopologyProject.UvTransform.MOVE,2,0,true));
        var repacked=p.previewPackMaterial(id,1,false).snapshot();
        for(int i=0;i<4;i++)check(repacked.faces().get(i)==frozen.faces().get(i),"Packing moved a frozen island");
        check(repacked.faces().get(4).a().u()<1,"Packing did not recover free island");
        p.restore(before);
        for(int index:List.of(0,4))p.applyPreview(p.previewUvTransform(before.faces().get(index).id(),true,
            EditorTopologyProject.UvTransform.SCALE,.125,.125,true));
        var small=p.snapshot();var kept=p.previewPackMaterial(id,1,true).snapshot();
        for(int i=0;i<5;i++){
            var a=small.faces().get(i);var b=kept.faces().get(i);
            check(Math.abs((a.b().u()-a.a().u())-(b.b().u()-b.a().u()))<1e-10 &&
                Math.abs((a.c().v()-a.a().v())-(b.c().v()-b.a().v()))<1e-10,"Keep-density pack changed UV scale");
        }
        p.restore(before);
    }
    private static void seams(EditorTopologyProject p)throws Exception{
        var before=p.snapshot();long id=before.faces().get(0).id();
        try{p.previewSeam(id,0,true);throw new AssertionError("Marked a boundary without a twin");}
        catch(IllegalArgumentException expected){check(p.snapshot()==before,"Boundary refusal mutated native state");}
        p.applyPreview(p.previewSeam(id,1,true));
        try{p.previewRelaxInterior(id);throw new AssertionError("Relaxation moved the marked edge through an alternate fan path");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("interior vertices"),"Unexpected relaxation refusal");}
        p.applyPreview(p.previewSeam(id,2,true));
        var marked=p.snapshot();
        check(marked.vertices()==before.vertices() && marked.materials()==before.materials(),"Marking a seam changed geometry or material");
        for(int i=0;i<before.faces().size();i++){
            var a=before.faces().get(i);var b=marked.faces().get(i);
            check(a.a()==b.a() && a.b()==b.b() && a.c()==b.c(),"Marking a seam changed native UVs");
        }
        check(marked.faces().get(0).seamMask()==6 && marked.faces().get(1).marked(2) && marked.faces().get(3).marked(1),"Both sides of the cut must be marked");
        var fan=p.previewUvPins(id,2,true).snapshot();
        var pack=EditorUvPack.pack(marked,p.topology(),marked.materials().get(0),1,false);
        check(pack.islands()==3 && pack.faces().get(0).seamMask()==6,"Packing must separate marked UV islands and retain the cuts");
        check(fan.faces().get(0).c().pinned() && !fan.faces().get(1).c().pinned(),"UV pin crossed a marked seam");
        p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(marked),"Native reopen lost explicit seams");
        var move=p.previewUvTransform(id,true,EditorTopologyProject.UvTransform.MOVE,.25,0,true);
        p.applyPreview(move);var moved=p.snapshot();
        for(int i=1;i<5;i++)check(moved.faces().get(i)==marked.faces().get(i),"Island move crossed an authored seam");
        check(moved.faces().get(0).marked(1),"UV movement erased seam intent");
        for(int edge:new int[]{1,2})p.applyPreview(p.previewSeam(id,edge,false));
        var clear=p.snapshot();
        check(clear.faces().get(0).seamMask()==0 && clear.faces().get(0).a()==moved.faces().get(0).a(),"Clearing a mark moved a UV");
        var repeat=p.previewUvTransform(id,true,EditorTopologyProject.UvTransform.MOVE,.25,0,true).snapshot();
        check(repeat.faces().get(1)==clear.faces().get(1),"Clearing an explicit mark joined discontinuous UVs");
        p.restore(before);
    }
    private static EditorTopologyProject imported(Path dir,String name,String body)throws Exception{
        var file=dir.resolve(name+".obj");Files.writeString(file,"mtllib fan.mtl\no "+name+"\nusemtl checker\n"+body);
        var source=EditorObjMaterials.capture(file);
        return EditorTopologyProject.fromMesh(source.mesh(),source.sourceFingerprint(),dir.resolve(name+".btop"));
    }
    private static void unwrap(Path dir)throws Exception{
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8,4,java.awt.image.BufferedImage.TYPE_INT_ARGB),
            "PNG",dir.resolve("checker.png").toFile());
        String folded="""
            v 0 0 0
            v 1 0 0
            v 0 1 0
            v 1 0 1
            vt 0 0
            vt 1 0
            vt .1 .4
            vt .8 -.3
            f 1/1 2/2 3/3
            f 2/2 1/1 4/4
            """;
        var p=imported(dir,"fold",folded);
        long id=p.snapshot().faces().get(0).id();
        p.applyPreview(p.previewUvPins(id,0,true));p.applyPreview(p.previewUvPins(id,1,true));
        var pinned=p.snapshot();var candidate=p.previewUnwrap(id);
        check(p.snapshot()==pinned,"LSCM preview changed live document");
        var first=candidate.snapshot().faces().get(0);var second=candidate.snapshot().faces().get(1);
        check(first.a()==pinned.faces().get(0).a() && first.b()==pinned.faces().get(0).b(),"LSCM moved pinned corners");
        check(Math.abs(first.c().u())<1e-9 && Math.abs(first.c().v()-2)<1e-9 &&
            Math.abs(second.c().u()-1)<1e-9 && Math.abs(second.c().v()+2)<1e-9,"Folded triangles must unfold isometrically in texture pixels");
        check(candidate.description().endsWith("→ 0.00°"),"Analytic folded fixture should have zero measured angular error: "+candidate.description());
        p.applyPreview(candidate);p.save();check(EditorTopologyProject.open(p.documentPath()).snapshot().equals(p.snapshot()),"Unwrapped native document lost UVs");
        var fitted=p.snapshot();
        try{p.previewUnwrap(id);throw new AssertionError("Repeated LSCM created a numerical-only edit");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("already") && p.snapshot()==fitted,"Unexpected repeat refusal");}
        p.restore(pinned);
        p.applyPreview(p.previewUvPins(id,0,false));p.applyPreview(p.previewUvPins(id,1,false));
        p.applyPreview(p.previewUvTransform(id,true,EditorTopologyProject.UvTransform.SCALE,-1,1,true));
        p.applyPreview(p.previewUvPins(id,0,true));p.applyPreview(p.previewUvPins(id,1,true));
        var reflected=p.previewUnwrap(id).snapshot();
        check(Math.abs(reflected.faces().get(1).c().u()+1)<1e-9 && Math.abs(reflected.faces().get(0).c().v()-2)<1e-9,
            "Mirrored chart lost its authored handedness");
        var crossed=imported(dir,"pinned-fold",folded.replace("vt .8 -.3","vt .8 .3"));
        long crossedId=crossed.snapshot().faces().get(0).id();
        crossed.applyPreview(crossed.previewUvPins(crossedId,-1,true));var inverted=crossed.snapshot();
        try{crossed.previewUnwrap(crossedId);throw new AssertionError("Folded fixed layout accepted");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("folds") && crossed.snapshot()==inverted,"Pinned fold refusal");}
        var adjacent=imported(dir,"neighbor","""
            v 0 0 0
            v 1 0 0
            v 0 1 0
            v 1 1 0
            vt 0 0
            vt 1 0
            vt .25 .5
            vt 0 2
            vt 1 2
            f 1/1 2/2 3/3
            f 3/4 2/2 4/5
            """);
        long firstId=adjacent.snapshot().faces().get(0).id();
        adjacent.applyPreview(adjacent.previewUvPins(firstId,0,true));adjacent.applyPreview(adjacent.previewUvPins(firstId,1,true));
        var split=adjacent.snapshot();
        try{adjacent.previewUnwrap(firstId);throw new AssertionError("Unwrap silently joined a UV discontinuity");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("retain the cut") && adjacent.snapshot()==split,"Seam preservation refusal");}
        adjacent.applyPreview(adjacent.previewSeam(firstId,1,true));
        var neighbor=adjacent.snapshot().faces().get(1);
        check(adjacent.previewUnwrap(firstId).snapshot().faces().get(1)==neighbor,"Explicit seam allows coincident UVs without joining the neighbor");
        var cone=imported(dir,"cone","""
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            v .5 .5 .75
            vt 0 0
            f 1/1 2/1 5/1
            f 2/1 3/1 5/1
            f 3/1 4/1 5/1
            f 4/1 1/1 5/1
            """);
        var coneSource=cone.snapshot();long base=coneSource.faces().get(0).id();
        var curved=cone.previewUnwrap(base);
        check(curved.description().contains("undefined → ") && !curved.description().endsWith("→ 0.00°"),"Curved chart must report its residual angular distortion");
        check(curved.snapshot().equals(cone.previewUnwrap(coneSource.faces().get(2).id()).snapshot()),"Unwrap changed when the seed face changed");
        cone.applyPreview(cone.previewUvPins(base,0,true));cone.applyPreview(cone.previewUvPins(base,1,true));var conflict=cone.snapshot();
        try{cone.previewUnwrap(base);throw new AssertionError("Coincident pin pair accepted");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("distinct") && cone.snapshot()==conflict,"Conflicting pins mutated native state");}
        var closed=imported(dir,"tetra","""
            v 0 0 0
            v 1 0 0
            v .5 .8660254 0
            v .5 .2886751 .8164966
            vt 0 0
            f 1/1 3/1 2/1
            f 1/1 2/1 4/1
            f 2/1 3/1 4/1
            f 3/1 1/1 4/1
            """);
        var before=closed.snapshot();long seed=before.faces().get(0).id();
        try{closed.previewUnwrap(seed);throw new AssertionError("Uncut closed chart accepted");}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains("disk") && closed.snapshot()==before,"Closed-chart refusal");}
        closed.applyPreview(closed.previewSeam(seed,0,true));closed.applyPreview(closed.previewSeam(seed,2,true));
        closed.applyPreview(closed.previewSeam(before.faces().get(1).id(),2,true));
        var disk=closed.previewUnwrap(seed).snapshot();
        for(var f:disk.faces())check((f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u())>0,
            "LSCM must open a collapsed tetrahedron along its authored cuts");
    }
    public static void main(String[] args)throws Exception{
        var dir=Files.createTempDirectory("native-uv-relax-");
        try{
            var captured=EditorObjMaterials.capture(source(dir));
            var project=EditorTopologyProject.fromMesh(captured.mesh(),captured.sourceFingerprint(),dir.resolve("fan.btop"));
            var before=project.snapshot();
            seams(project);packing(project);
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
            unwrap(dir);
            System.out.println("PASS native UV relaxation and conformal unfold: pinned fold in rectangular texels, mirrored chart, cut tetrahedron and native reopen");
        }finally{
            try(var paths=Files.walk(dir)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
        }
    }
}
