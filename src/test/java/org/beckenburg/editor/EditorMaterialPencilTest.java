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
    public static void main(String[] args)throws Exception {
        historyBudget();
        Path dir=Files.createTempDirectory("pencil-native-");
        try {
            Path glb=Path.of("development/quality/evidence/g2-textured-roundtrip-2026-11-07/edited.glb");
            var project=EditorTopologyProject.fromGlb("box.glb",Files.readAllBytes(glb),dir.resolve("box.btop"));
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
