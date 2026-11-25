package org.beckenburg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import org.beckenburg.editor.EditorGlbWriter;
import org.beckenburg.editor.EditorMeshMaterial;
import org.beckenburg.editor.EditorTopologyProject;

/** Trusted stdio session on the BTOP file itself. View settings are local to this process. */
final class HeadlessMaterialEditor extends MaterialEditorAgent {
    private final Thread owner=Thread.currentThread();
    private final Long faceOverride;
    private MaterialReviewFrame rendered;
    private long renderedRevision=-1;
    HeadlessMaterialEditor(Path file,Long face)throws IOException{
        super(EditorTopologyProject.open(file));
        if(face!=null&&document.snapshot().faces().stream().noneMatch(f->f.id()==face))
            throw new IllegalArgumentException("Unknown native face "+face);
        faceOverride=face;
    }
    public void checkAccess(){
        if(Thread.currentThread()!=owner)throw new IllegalStateException("Wrong mesh owner thread");
    }
    Long selectedFaceId(){
        if(faceOverride!=null)return faceOverride;
        var workspace=document.savedWorkspace();
        return workspace==null||workspace.hit()==null?null:workspace.hit().faceId();
    }
    EditorMeshMaterial.LayerInfo selectedLayer(EditorMeshMaterial material){
        var layers=material.layers();return layers.isEmpty()?null:layers.get(layers.size()-1);
    }
    MaterialReviewFrame frame()throws IOException{
        if(rendered==null||renderedRevision!=document.revision()){
            var mesh=document.mesh();
            var renderer=new SceneRenderer();renderer.setRenderSize(280,190);
            rendered=GlbProps.materialReviewFrame(EditorGlbWriter.encode(mesh),renderer,0,mesh,null,0);
            renderedRevision=document.revision();
        }
        return rendered;
    }
    @Override OutputStream openCapture(Path path)throws IOException{
        if(path.getFileName()==null||!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
            throw new IOException("Choose a new PNG file for mesh capture");
        return Files.newOutputStream(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
    }
}
