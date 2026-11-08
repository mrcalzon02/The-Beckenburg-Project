package org.beckenburg.editor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public final class EditorMaterialPencilTest {
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
    public static void main(String[] args)throws Exception {
        historyBudget();
        blendModes();
        legacyPaintFiles();
        Path dir=Files.createTempDirectory("pencil-native-");
        try {
            Path glb=Path.of("development/quality/evidence/g2-textured-roundtrip-2026-11-07/edited.glb");
            var project=EditorTopologyProject.fromGlb("box.glb",Files.readAllBytes(glb),dir.resolve("box.btop"));
            historyEviction(project);
            stackContracts(project);
            var original=project.snapshot();
            var face=original.faces().get(10);
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
