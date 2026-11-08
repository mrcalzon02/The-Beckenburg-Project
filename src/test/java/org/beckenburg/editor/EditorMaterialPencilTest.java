package org.beckenburg.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public final class EditorMaterialPencilTest {
    private static EditorTopologyProject.Face tiny(EditorMeshMaterial material,EditorUvTile tile,long id,int x,int y){
        var c=new EditorTopologyProject.Corner[3];
        for(int i=0;i<3;i++)c[i]=new EditorTopologyProject.Corner(id*3+i,
            tile.nativeU((x+(i==1?.13:.02))/material.width(),material),
            tile.nativeV((y+(i==2?.13:.02))/material.height(),material));
        return new EditorTopologyProject.Face(id,"gutter",c[0],c[1],c[2],material.key());
    }
    private static void projectionPadding()throws Exception{
        var png=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(12,6,java.awt.image.BufferedImage.TYPE_INT_ARGB),"png",png);
        for(var origin:EditorMeshMaterial.Origin.values())for(int wrap:List.of(10497,33648,33071)){
            var m=EditorMeshMaterial.fromPng("gutter.png",png.toByteArray(),wrap,wrap,EditorMeshMaterial.Alpha.OPAQUE,origin);
            var tile=wrap==33071?EditorUvTile.ORIGIN:new EditorUvTile(-1,3);
            var faces=List.of(tiny(m,tile,1,2,2),tiny(m,tile,2,6,2));int row=24;
            var padded=new EditorPaintPadding(m).expand(faces,Set.of(row+2),3);
            check(padded.contains(row+3) && !padded.contains(row+4) && !padded.contains(row+5) && !padded.contains(row+6),
                "Protect ties and the competing island");
            check(padded.contains(row+11)==(wrap==10497),"Padding must honor the source image sampler at its borders");
            var sliver=tiny(m,tile,3,3,3);
            var protectedArea=new EditorPaintPadding(m).expand(List.of(faces.get(0),sliver),Set.of(row+2),2);
            check(!protectedArea.contains(row+12+3),"Protect triangles too small to cover a texel center");
            if(wrap==33071){
                var outside=tiny(m,tile,4,-3,2);
                check(!new EditorPaintPadding(m).expand(List.of(faces.get(0),outside),Set.of(row+2),3).contains(row),
                    "Off-image clamped geometry owns its border texel");
            }
        }
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void historyBudget() throws IOException {
        var dots=new ArrayList<Integer>();
        for(int tile=0;tile<EditorPaintLayer.MAX_TILES;tile++)
            dots.add((tile/128)*16*2048+(tile%128)*16);
        var first=EditorPaintLayer.EMPTY.stroke(2048,2048,dots,0xff00ff00);
        var history=new ArrayList<EditorPaintLayer>();
        history.add(first);
        for(int i=1;i<=60;i++)history.add(history.get(i-1).stroke(2048,2048,List.of(i),0xff000000+i));
        check(history.size()==61 && history.get(60).size()==2048,"Retain bounded sparse history");
        var bytes=new ByteArrayOutputStream();
        first.write(new DataOutputStream(bytes));
        var read=EditorPaintLayer.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),2048,2048,2048);
        check(read.equals(first) && !read.equals(history.get(60)),"Restore original tile payload");
        try {
            EditorPaintLayer.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),2048,2048,2047);
            throw new AssertionError("Paint reader accepted excess tiles");
        } catch(IOException expected) {}
    }
    private static void blendModes() throws IOException {
        var base=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        base.setRGB(0,0,0xff4080c0);
        var bytes=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(base,"png",bytes);
        var source=EditorMeshMaterial.fromPng("blend.png",bytes.toByteArray(),10497,10497,
            EditorMeshMaterial.Alpha.BLEND,EditorMeshMaterial.Origin.TOP_LEFT);
        var painted=source.pencil(source.key(),List.of(0,1),0xff804020);
        var expected=Map.of(EditorMeshMaterial.Blend.NORMAL,0xff804020,EditorMeshMaterial.Blend.MULTIPLY,0xff202018,
            EditorMeshMaterial.Blend.ADD,0xffc0c0e0,EditorMeshMaterial.Blend.SCREEN,0xffa0a0c8);
        for(var mode:EditorMeshMaterial.Blend.values()){
            var full=painted.withStyle(new EditorMeshMaterial.PaintStyle(mode,100));
            var image=javax.imageio.ImageIO.read(new ByteArrayInputStream(full.png()));
            check(image.getRGB(0,0)==expected.get(mode),"Unexpected "+mode+" RGB");
            var half=painted.withStyle(new EditorMeshMaterial.PaintStyle(mode,50));
            image=javax.imageio.ImageIO.read(new ByteArrayInputStream(half.png()));
            check(image.getRGB(1,0)==0x80804020,"Transparent backdrop must preserve ink color and effective alpha");
            var zero=painted.withStyle(new EditorMeshMaterial.PaintStyle(mode,0));
            check(Arrays.equals(zero.png(),source.png()) && zero.paintedTileCount()==1,"Zero opacity retains paint");
            check(EditorMeshMaterial.paintHistoryBytes(List.of(full),List.of(half))==0,"Style-only edit copied tiles");
        }
    }
    private static void uvDensity(){
        var a=new EditorMeshPipeline.Vertex(0,0,0,0,0);
        var b=new EditorMeshPipeline.Vertex(0,2,0,1,0);
        var c=new EditorMeshPipeline.Vertex(0,0,1,0,1);
        var triangle=new EditorMeshPipeline.Triangle("metric",a,b,c);
        var uniform=EditorUvDensity.measure(triangle,16,8).orElseThrow();
        check(uniform.texelsPerUnit()==8 && uniform.stretch()==1,"Uniform texel density on a rotated face");
        var stretched=EditorUvDensity.measure(triangle,32,8).orElseThrow();
        check(Math.abs(stretched.texelsPerUnit()-Math.sqrt(128))<1e-10 && Math.abs(stretched.stretch()-2)<1e-10,
            "Non-square texture measures principal stretch");
        var shear=new EditorMeshPipeline.Vertex(0,0,1,.5,1);
        var sheared=EditorUvDensity.measure(new EditorMeshPipeline.Triangle("metric",a,b,shear),16,8).orElseThrow();
        check(sheared.texelsPerUnit()==8 && Math.abs(sheared.stretch()-(3+Math.sqrt(5))/2)<1e-10,"Shear distortion");
        var mirror=new EditorMeshPipeline.Vertex(0,2,0,-1,0);
        check(EditorUvDensity.measure(new EditorMeshPipeline.Triangle("metric",a,mirror,c),16,8).orElseThrow().equals(uniform),
            "Mirrored UVs preserve density");
        var collapsed=new EditorMeshPipeline.Vertex(0,0,1,0,0);
        check(EditorUvDensity.measure(new EditorMeshPipeline.Triangle("metric",a,b,collapsed),16,8).orElseThrow().texelsPerUnit()==0,
            "Collapsed UVs have zero texel area");
        check(EditorUvDensity.measure(new EditorMeshPipeline.Triangle("metric",a,b,b),16,8).isEmpty(),
            "Degenerate world triangle has no density");
        var face=new EditorTopologyProject.Face(9,"metric",new EditorTopologyProject.Corner(3,0,0),
            new EditorTopologyProject.Corner(4,-1,0),new EditorTopologyProject.Corner(5,.5,1));
        var fixed=EditorUvDensity.correct(face,new EditorMeshPipeline.Triangle("metric",a,mirror,shear),16,8);
        double signed=(fixed.b().u()-fixed.a().u())*(fixed.c().v()-fixed.a().v())-
            (fixed.b().v()-fixed.a().v())*(fixed.c().u()-fixed.a().u());
        check(Math.abs(signed+1)<1e-10 && Math.abs(fixed.a().u()+fixed.b().u()+fixed.c().u()+.5)<1e-10 &&
            Math.abs(fixed.a().v()+fixed.b().v()+fixed.c().v()-1)<1e-10,"Relax must preserve signed area and centroid");
    }
    private static void historyEviction(EditorTopologyProject p){
        var clean=p.snapshot();
        long face=clean.faces().get(10).id();
        var history=new EditorHistory<>(p,60,3072,(before,after)->
            EditorMeshMaterial.paintHistoryBytes(before.materials(),after.materials()));
        history.execute(()->p.applyPreview(p.previewPencil(face,List.of(1),0xffff00ff)));
        check(history.retainedBytes()==1024,"First stroke retains one new tile");
        history.execute(()->p.applyPreview(p.previewPencil(face,List.of(1),0xff0000ff)));
        var blue=p.snapshot();
        check(history.retainedBytes()==3072,"Replacement charges old and new tile payloads");
        history.execute(()->p.applyPreview(p.previewPencil(face,List.of(2),0xff00ff00)));
        check(history.retainedBytes()==2048 && history.undo() && p.snapshot().equals(blue) && !history.canUndo(),
            "Evict oldest complete transactions at byte budget");
        check(history.canRedo() && history.retainedBytes()==2048,"Undo and redo share one byte budget");
        history.execute(()->p.applyPreview(p.previewPencil(face,List.of(3),0xffffffff)));
        check(history.retainedBytes()==2048 && !history.canRedo(),"A new branch releases redo tile cost");
        var before=p.snapshot();
        var small=new EditorHistory<>(p,60,512,(a,b)->EditorMeshMaterial.paintHistoryBytes(a.materials(),b.materials()));
        try {
            small.execute(()->p.applyPreview(p.previewPencil(face,List.of(3),0xff00ffff)));
            throw new AssertionError("Oversized undo accepted");
        }catch(IllegalArgumentException expected){
            check(before.equals(p.snapshot()) && !small.canUndo() && small.retainedBytes()==0,"Oversized edit must roll back");
        }
        p.restore(clean);
    }
    private static void stackContracts(EditorTopologyProject project)throws Exception{
        var original=project.snapshot();
        var face=original.faces().get(10);
        project.applyPreview(project.previewPaintLayer(face.id(),"paint",EditorTopologyProject.LayerEdit.ADD,
            "Detail",EditorMeshMaterial.PaintStyle.DEFAULT));
        var empty=project.snapshot();
        var derived=empty.materials().get(empty.materials().size()-1);
        String detail=derived.layers().get(1).id();
        check(empty.materials().containsAll(original.materials()) && derived.paintedTileCount()==0 && derived.hasLayerEdits(),
            "Empty layer survives as native authoring without dropping original GLB materials");
        check(ByteBuffer.wrap(project.snapshotBytes()).getInt(4)==10 &&
            EditorTopologyProject.decode(project.snapshotBytes()).equals(empty),"Empty native stack roundtrip");
        project.applyPreview(project.previewPencil(face.id(),detail,List.of(2),0xff00ffff));
        var painted=project.snapshot();
        project.applyPreview(project.previewPaintLayer(face.id(),detail,EditorTopologyProject.LayerEdit.DOWN,"",
            EditorMeshMaterial.PaintStyle.DEFAULT));
        check(EditorMeshMaterial.paintHistoryBytes(painted.materials(),project.snapshot().materials())==0,
            "Layer movement must retain tile payloads");
        EditorGlbWriter.validate(project.mesh(),EditorGlbWriter.encode(project.mesh()));
        for(int i=2;i<EditorPaintStack.LIMIT;i++)project.applyPreview(project.previewPaintLayer(face.id(),detail,
            EditorTopologyProject.LayerEdit.ADD,"Layer "+i,EditorMeshMaterial.PaintStyle.DEFAULT));
        var maximum=project.snapshot();
        try{
            project.previewPaintLayer(face.id(),detail,EditorTopologyProject.LayerEdit.ADD,"Excess",EditorMeshMaterial.PaintStyle.DEFAULT);
            throw new AssertionError("Layer count limit ignored");
        }catch(IllegalArgumentException expected){check(project.snapshot()==maximum,"Excess layer changed native state");}
        try{
            project.previewPencil(face.id(),"missing",List.of(2),0xff000000);
            throw new AssertionError("Unknown layer accepted");
        }catch(IllegalArgumentException expected){check(project.snapshot()==maximum,"Invalid layer changed native state");}
        project.restore(original);
    }
    private static void legacyPaintFiles()throws IOException{
        var dir=Path.of("development/quality/evidence/material-pencil-2026-11-07");
        for(var name:List.of("painted","styled")){
            byte[] document=Files.readAllBytes(dir.resolve(name+".btop"));
            var state=EditorTopologyProject.decode(document);
            var m=state.materials().get(0);
            check(m.layers().size()==1 && m.layers().get(0).id().equals("paint"),"Legacy overlay must remain editable");
            var image=javax.imageio.ImageIO.read(new ByteArrayInputStream(m.png()));
            boolean styled=name.equals("styled");
            check(ByteBuffer.wrap(document).getInt(4)==(styled?9:8) &&
                image.getRGB(2,0)==(styled?0xff008000:0xffff00ff),"Legacy blend and paint texel");
            EditorObjBundle.validate(state,Files.readAllBytes(dir.resolve(name+".obj.zip")));
        }
    }
    private static void islandBalance(){
        var a=new EditorTopologyProject.Corner(0,0,0);var b=new EditorTopologyProject.Corner(1,-1,0);
        var c=new EditorTopologyProject.Corner(2,-1,1);var d=new EditorTopologyProject.Corner(3,0,1);
        var faces=List.of(new EditorTopologyProject.Face(4,"island",a,b,c),new EditorTopologyProject.Face(7,"island",a,c,d));
        double[][] points={{0,0},{2,0},{2,1},{0,2}};
        java.util.function.Function<EditorTopologyProject.Corner,EditorMeshPipeline.Vertex> vertex=corner->{
            var p=points[(int)corner.vertex()];return new EditorMeshPipeline.Vertex(p[0],p[1],0,corner.u(),corner.v());
        };
        java.util.function.Function<EditorTopologyProject.Face,EditorMeshPipeline.Triangle> geometry=
            f->new EditorMeshPipeline.Triangle(f.part(),vertex.apply(f.a()),vertex.apply(f.b()),vertex.apply(f.c()));
        var fixed=EditorUvDensity.balance(faces,new int[]{0,1},geometry,8,16);
        double oldScore=0,newScore=0;
        for(int i=0;i<2;i++){
            var old=EditorUvDensity.measure(geometry.apply(faces.get(i)),8,16).orElseThrow();
            var next=EditorUvDensity.measure(geometry.apply(fixed.get(i)),8,16).orElseThrow();
            double area=i==0?1:2;
            oldScore+=area*(old.stretch()+1/old.stretch());newScore+=area*(next.stretch()+1/next.stretch());
            check(Math.abs(next.texelsPerUnit()-old.texelsPerUnit())<1e-9,"Per-face texel area changed");
            var f=fixed.get(i);
            double signed=(f.b().u()-f.a().u())*(f.c().v()-f.a().v())-(f.b().v()-f.a().v())*(f.c().u()-f.a().u());
            check(Math.abs(signed+1)<1e-9,"Mirrored island changed orientation");
        }
        check(newScore<oldScore && fixed.get(0).a().equals(fixed.get(1).a()) && fixed.get(0).c().equals(fixed.get(1).b()),
            "Island balance must reduce aggregate distortion without splitting its diagonal");
        try{
            EditorUvDensity.balance(fixed,new int[]{0,1},geometry,8,16);
            throw new AssertionError("Accepted an island with no further common stretch correction");
        }catch(IllegalArgumentException expected){}
    }
    private static void tiledFace(EditorTopologyProject p)throws Exception{
        var original=p.snapshot();
        long face=original.faces().get(10).id();
        byte[] png=Files.readAllBytes(Path.of("development/quality/evidence/material-pencil-2026-11-07/checker.png"));
        for(int wrap:List.of(10497,33648))for(var origin:EditorMeshMaterial.Origin.values()){
            var binding=EditorMeshMaterial.fromPng("tile.png",png,wrap,wrap,EditorMeshMaterial.Alpha.OPAQUE,origin);
            p.applyPreview(p.previewBindMaterial(face,binding));
            p.applyPreview(p.previewUvTransform(face,false,EditorTopologyProject.UvTransform.MOVE,.75,-.25,false));
            var current=p.snapshot();
            int row=wrap==10497?3:0;
            boolean bottom=origin==EditorMeshMaterial.Origin.BOTTOM_LEFT;
            int left=3+4*(bottom?3-row:row);
            int right=(wrap==10497?1:2)+(bottom?12:0);
            try{
                p.previewPencil(face,"paint",List.of(left),0xff00ffff,new EditorUvTile(-5,1));
                throw new AssertionError("Painted an unrelated UV tile");
            }catch(IllegalArgumentException expected){check(p.snapshot()==current,"Rejected tile changed geometry");}
            p.applyPreview(p.previewPencil(face,"paint",List.of(left),0xff00ffff,new EditorUvTile(0,-1)));
            p.applyPreview(p.previewPencil(face,"paint",List.of(right),0xffff00ff,new EditorUvTile(1,0)));
            var material=p.snapshot().materials().stream().filter(m->m.key().equals(binding.key())).findFirst().orElseThrow();
            var im=javax.imageio.ImageIO.read(new ByteArrayInputStream(material.png()));
            check(im.getRGB(left%4,left/4)==0xff00ffff && im.getRGB(right%4,right/4)==0xffff00ff,
                "Both sides of a tile boundary must address the expected original-image texels");
            EditorGlbWriter.validate(p.mesh(),EditorGlbWriter.encode(p.mesh()));
            p.restore(original);
        }
    }
    public static void main(String[] args)throws Exception {
        projectionPadding();
        historyBudget();
        blendModes();
        uvDensity();
        islandBalance();
        legacyPaintFiles();
        Path dir=Files.createTempDirectory("pencil-native-");
        try {
            Path glb=Path.of("development/quality/evidence/g2-textured-roundtrip-2026-11-07/edited.glb");
            var project=EditorTopologyProject.fromGlb("box.glb",Files.readAllBytes(glb),dir.resolve("box.btop"));
            historyEviction(project);
            stackContracts(project);
            tiledFace(project);
            var original=project.snapshot();
            var face=original.faces().get(10);
            var stale=project.observe();project.restore(original);
            var binding=original.materials().stream().filter(m->m.key().equals(face.material())).findFirst().orElseThrow();
            String active=binding.layers().get(0).id();
            try{project.previewProjectedPencil(stale,face.id(),active,Map.of(face.id(),List.of(0)),0xffff00ff);throw new AssertionError("Accepted stale projection");}
            catch(IllegalArgumentException expected){check(project.snapshot()==original,"Stale projected input changed the scene");}
            for(var invalid:List.of(Map.of(face.id(),List.of(-1)),Map.of(Long.MAX_VALUE,List.of(0)))){
                try{project.previewProjectedPencil(project.observe(),face.id(),active,invalid,0xffff00ff);throw new AssertionError("Accepted invalid projected sample");}
                catch(IllegalArgumentException expected){check(project.snapshot()==original,"Invalid projected input changed native state");}
            }
            var preview=project.previewPencil(face.id(),List.of(1,2,3),0xffff00ff);
            project.applyPreview(preview);
            var painted=project.snapshot();
            check(painted.materials().size()==original.materials().size()+1 &&
                painted.materials().containsAll(original.materials()),"Retain every source GLB material");
            var layer=painted.materials().get(painted.materials().size()-1);
            check(layer.paintedTileCount()==1 && painted.faces().stream().allMatch(f->f.material().equals(layer.key())),
                "Shared texture binds the derived paint material");
            byte[] nativeBytes=project.snapshotBytes();
            check(ByteBuffer.wrap(nativeBytes).getInt(4)==8 && EditorTopologyProject.decode(nativeBytes).equals(painted),
                "BTOP8 retains sparse tiles even without a saved workspace");
            project.save();
            check(EditorTopologyProject.open(project.documentPath()).snapshot().equals(painted),"Fresh native open");
            EditorGlbWriter.validate(project.mesh(),EditorGlbWriter.encode(project.mesh()));
            project.applyPreview(project.previewPencil(face.id(),List.of(1,2,3),0));
            var erased=project.snapshot().materials().get(painted.materials().size()-1);
            check(erased.paintedTileCount()==0 && Arrays.equals(erased.png(),layer.originalPng()),"Erase reveals source and drops empty tile");
            var originalBinding=original.materials().stream().filter(m->m.key().equals(face.material())).findFirst().orElseThrow();
            project.applyPreview(project.previewBindMaterial(face.id(),originalBinding));
            project.applyPreview(project.previewPencil(face.id(),List.of(2),0xff0000ff));
            var second=project.snapshot();
            check(second.materials().contains(erased) && second.materials().size()==painted.materials().size()+1,
                "Painting a rebound original preserves its previous derivative");
            EditorGlbWriter.validate(project.mesh(),EditorGlbWriter.encode(project.mesh()));
            System.out.println("PASS base-color pencil: 61 sparse versions at 2048 tiles, bounded decode, GLB source retained, shared texture derivative, native roundtrip and erase");
        }finally{
            try(var paths=Files.walk(dir)){for(var file:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}
        }
    }
}
