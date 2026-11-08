package org.beckenburg.editor;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Native authority and interchange checks against real files, without a game dependency. */
public final class EditorMeshMaterialTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private interface Action{void run()throws Exception;}
    private static void refuse(Action a)throws Exception{try{a.run();throw new AssertionError("Expected refusal");}catch(IOException|IllegalArgumentException expected){}}
    public static byte[] png()throws IOException{
        var im=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<4;x++)im.setRGB(x,y,(x<2?(y<2?0xffff0000:0xff0000ff):(y<2?0x8000ff00:0xffffff00)));var out=new ByteArrayOutputStream();ImageIO.write(im,"png",out);return out.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("native-material-");
        try{
            byte[] original=png(),mutable=original.clone();var material=EditorMeshMaterial.fromPng("Asymmetric \"test\"",mutable,33071,33648,EditorMeshMaterial.Alpha.MASK,EditorMeshMaterial.Origin.TOP_LEFT);mutable[40]^=1;material.png()[40]^=1;
            check(Arrays.equals(material.png(),original),"Captured original PNG must be immutable");
            String obj="o panel\nv 0 0 0\nv 1 0 0\nv 0 1 0\nv 1 1 0\nvt 0 0\nvt 1 0\nvt 0 1\nvt .25 .25\nvt .75 .25\nvt 1 1\nvn 0 0 2\nf 1/1/1 2/2/1 3/3/1\nf 3/4/1 2/5/1 4/6/1\n";
            var mesh=EditorMeshPipeline.read("source.obj",obj.getBytes(java.nio.charset.StandardCharsets.UTF_8));var p=EditorTopologyProject.fromMesh(mesh,"f".repeat(64),root.resolve("native.btop"));var history=new EditorHistory<>(p,20);var initial=p.snapshot();p.save();check(ByteBuffer.wrap(Files.readAllBytes(p.documentPath())).getInt(4)==2,"Unbound normal data retains v2");
            var candidate=p.previewBindMaterial(0,material);check(p.snapshot()==initial&&candidate.snapshot().vertices().equals(initial.vertices()),"Binding preview preserves authoritative state and all native identities");p.cancelPreview(candidate);check(p.snapshot()==initial&&!history.canUndo(),"Cancel changes neither native state nor history");
            var edit=p.previewBindMaterial(0,material);history.execute(()->p.applyPreview(edit));var bound=p.snapshot();check(bound.faces().get(0).material().equals(material.key())&&bound.faces().get(1).material().isEmpty(),"Only chosen native face changes binding");
            check(history.undo()&&p.snapshot().equals(initial)&&history.redo()&&p.snapshot().equals(bound),"One shared history restores PNG and assignments exactly");refuse(()->p.applyPreview(edit));
            p.save();byte[] encoded=Files.readAllBytes(p.documentPath());check(ByteBuffer.wrap(encoded).getInt(4)==3&&EditorTopologyProject.open(p.documentPath()).snapshot().equals(bound),"Actual v3 save/fresh open retains original PNG, UV, raw normals, sampler and bindings");p.save();check(Arrays.equals(encoded,Files.readAllBytes(p.documentPath())),"Material encoding deterministic");
            var future=encoded.clone();ByteBuffer.wrap(future).putInt(4,8);refuse(()->EditorTopologyProject.decode(future));refuse(()->EditorTopologyProject.decode(Arrays.copyOf(encoded,encoded.length-1)));
            refuse(()->p.previewBindMaterial(999,material));refuse(()->p.previewBindMaterial(0,material));
            var conflicting=new EditorMeshMaterial(material.key(),"other",original,material.color(),10497,10497,material.alpha(),.5,material.origin(),Map.of());refuse(()->p.previewBindMaterial(1,conflicting));
            var extrude=p.previewFaceEdit(0,EditorTopologyProject.FaceEdit.EXTRUDE,.2);check(extrude.snapshot().materials().equals(bound.materials())&&extrude.snapshot().faces().stream().filter(f->f.id()!=1).allMatch(f->f.material().equals(material.key())),"Caps and new walls retain selected source material");
            var transform=p.previewTransform(bound.vertices().stream().map(EditorTopologyProject.Vertex::id).toList(),EditorTopologyProject.Transform.ROTATE,1,90,true);check(transform.snapshot().materials().equals(bound.materials())&&transform.snapshot().faces().get(0).material().equals(material.key()),"Transforms retain immutable bindings");
            byte[] glb=EditorGlbWriter.encode(p.mesh());check(Arrays.equals(glb,EditorGlbWriter.encode(p.mesh())),"Material GLB deterministic");check(new String(glb,java.nio.charset.StandardCharsets.ISO_8859_1).contains("beckenburgUvOrigin"),"Actual GLB includes explicit origin metadata");
            var exportable=EditorMeshMaterial.fromPng("asymmetric.png",original,33071,33071,EditorMeshMaterial.Alpha.BLEND,EditorMeshMaterial.Origin.TOP_LEFT);var exportProject=EditorTopologyProject.fromMesh(mesh,"e".repeat(64),root.resolve("export.btop"));exportProject.applyPreview(exportProject.previewBindMaterial(0,exportable));
            Path bundle=root.resolve("materials.obj.zip");exportProject.exportObj(exportProject.observe(),bundle,null);byte[] bundleBytes=Files.readAllBytes(bundle);EditorObjBundle.validate(exportProject.snapshot(),bundleBytes);var roundtrip=EditorMeshPipeline.read(bundle);check(roundtrip.materials().equals(exportProject.snapshot().materials())&&roundtrip.cornerVertexIds().equals(mesh.cornerVertexIds()),"Actual OBJ/MTL/PNG bundle roundtrip retains original embedded data and connectivity");exportProject.exportObj(exportProject.observe(),bundle,EditorDocumentStore.fingerprint(bundleBytes));check(Arrays.equals(bundleBytes,Files.readAllBytes(bundle)),"OBJ bundle deterministic");
            var bad=new ByteArrayOutputStream();try(var zip=new java.util.zip.ZipOutputStream(bad)){zip.putNextEntry(new java.util.zip.ZipEntry("../escape"));zip.write(1);zip.closeEntry();}refuse(()->EditorObjBundle.read("bad.obj.zip",bad.toByteArray()));
            var altered=new ByteArrayOutputStream();try(var input=new java.util.zip.ZipInputStream(new ByteArrayInputStream(bundleBytes));var zip=new java.util.zip.ZipOutputStream(altered)){for(java.util.zip.ZipEntry e;(e=input.getNextEntry())!=null;){byte[] content=input.readAllBytes();if(e.getName().equals("materials.mtl"))content="# inconsistent".getBytes();zip.putNextEntry(new java.util.zip.ZipEntry(e.getName()));zip.write(content);zip.closeEntry();}}refuse(()->EditorObjBundle.read("altered.obj.zip",altered.toByteArray()));
            Path protectedFile=root.resolve("protected.obj");Files.writeString(protectedFile,"keep");refuse(()->p.exportObj(p.observe(),protectedFile,EditorDocumentStore.fingerprint(Files.readAllBytes(protectedFile))));check(Files.readString(protectedFile).equals("keep"),"Plain OBJ refuses material loss without touching destination");
            Files.write(root.resolve("texture.png"),original);Files.writeString(root.resolve("paint.mtl"),"newmtl Painted panel\nKd .5 1 .25\nd .75\nmap_Kd -clamp on texture.png\nNs 20\n");Path source=root.resolve("source.obj");String withMaterial="mtllib paint.mtl\nusemtl Painted panel\n"+obj;Files.writeString(source,withMaterial);
            var capture=EditorObjMaterials.capture(source);var imported=capture.mesh();var m=imported.materials().get(0);check(m.name().equals("Painted panel")&&m.color().equals(new EditorMeshMaterial.Color(.5,1,.25,.75))&&m.wrapS()==33071&&m.alpha()==EditorMeshMaterial.Alpha.BLEND&&Arrays.equals(m.png(),original),"Real OBJ/MTL captures authored diffuse, opacity, clamp and exact PNG");check(imported.cornerVertexIds().equals(mesh.cornerVertexIds())&&imported.triangles().stream().allMatch(t->t.material().equals(m.key())),"Material assignment does not split source geometry identities");capture.validateDependencies();Files.writeString(root.resolve("paint.mtl"),"changed");refuse(capture::validateDependencies);
            refuse(()->EditorMeshPipeline.read("missing-context.obj",withMaterial.getBytes(java.nio.charset.StandardCharsets.UTF_8)));Files.writeString(root.resolve("paint.mtl"),"newmtl Painted panel\nmap_Kd -s 2 2 2 texture.png\n");refuse(()->EditorObjMaterials.capture(source));Files.writeString(root.resolve("paint.mtl"),"newmtl Painted panel\nmap_Kd ../outside.png\n");refuse(()->EditorObjMaterials.capture(source));
            System.out.println("PASS native materials: exact PNG, face bindings/IDs/UV/normals, cancel/history, deterministic v3 fresh reopen, real OBJ/MTL dependencies/options guards, GLB and lossless-export refusal");
        }finally{try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
}
