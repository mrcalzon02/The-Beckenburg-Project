package org.beckenburg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.beckenburg.editor.EditorAgentService;
import org.beckenburg.editor.EditorDocumentStore;
import org.beckenburg.editor.EditorMeshMaterial;
import org.beckenburg.editor.EditorTopologyProject;

/** Shared host operations on the actual mesh document and its production renderer frame. */
abstract class MaterialEditorAgent implements EditorAgentService.Host<EditorTopologyProject.State>,EditorAgentService.SelectionSource {
    final EditorTopologyProject document;
    MaterialEditorAgent(EditorTopologyProject document){this.document=document;}
    abstract Long selectedFaceId();
    abstract EditorMeshMaterial.LayerInfo selectedLayer(EditorMeshMaterial material);
    abstract MaterialReviewFrame frame() throws IOException;
    public EditorTopologyProject project(){return document;}
    public long revision(){return document.revision();}
    public EditorAgentService.DiskState compareSaved(EditorTopologyProject.State live)throws IOException{
        var saved=EditorTopologyProject.decode(EditorDocumentStore.readDocument(document.documentPath()));
        return live.equals(saved)?EditorAgentService.DiskState.MATCHES:EditorAgentService.DiskState.DIFFERS;
    }
    public Set<EditorAgentService.Capability> capabilities(){
        return Set.of(EditorAgentService.Capability.INSPECT,EditorAgentService.Capability.CAPTURE,EditorAgentService.Capability.TEST,
            EditorAgentService.Capability.TRANSACT,EditorAgentService.Capability.UNDO,EditorAgentService.Capability.REDO,
            EditorAgentService.Capability.SAVE,EditorAgentService.Capability.REOPEN);
    }
    public List<String> operations(){return List.of("set-layer-opacity");}
    public Map<String,Map<String,String>> operationParameters(){
        return Map.of("set-layer-opacity",Map.of("face","integer","layer","string","opacity","integer"));
    }
    private EditorTopologyProject.Face selectedFace(){
        var id=selectedFaceId();
        return id==null?null:document.snapshot().faces().stream().filter(f->f.id()==id).findFirst().orElse(null);
    }
    private EditorMeshMaterial binding(EditorTopologyProject.Face face){
        return document.snapshot().materials().stream().filter(m->m.key().equals(face.material())).findFirst().orElse(null);
    }
    public EditorAgentService.Selection selection(){
        var face=selectedFace();
        if(face==null)return null;
        var details=new LinkedHashMap<String,String>();details.put("material",face.material());
        var material=binding(face);
        var layer=material==null?null:selectedLayer(material);
        if(layer!=null){
            details.put("layer",layer.id());details.put("opacity",Integer.toString(layer.style().opacity()));
            details.put("blend",layer.style().blend().name());
        }
        return new EditorAgentService.Selection("FACE",Long.toString(face.id()),details);
    }
    public void apply(String operation,Map<String,String> arguments){
        if(!operation.equals("set-layer-opacity")||!arguments.keySet().equals(Set.of("face","layer","opacity")))
            throw new IllegalArgumentException("Expected face, layer and opacity");
        var preview=document.previewLayerOpacity(Long.parseLong(arguments.get("face")),arguments.get("layer"),Integer.parseInt(arguments.get("opacity")));
        document.applyPreview(preview);
    }
    public void validate(){checkAccess();}
    public void reopen()throws IOException{document.reopen();}
    OutputStream openCapture(Path path)throws IOException{return Files.newOutputStream(path);}
    public EditorAgentService.Capture capture(Path path)throws IOException{
        var image=frame().image;
        try(var output=openCapture(path)){
            if(!ImageIO.write(image,"png",output))throw new IOException("PNG writer unavailable");
        }
        return new EditorAgentService.Capture(path,image.getWidth(),image.getHeight(),"SceneRenderer material review");
    }
    public List<String> tests(){return List.of("visible-material");}
    public List<EditorAgentService.Check> test(String id){
        var face=selectedFace();
        if(face==null)return List.of(new EditorAgentService.Check("FACE_SELECTED",false,"Select a native face"));
        var material=binding(face);
        try{
            return List.of(new EditorAgentService.Check("BOUND_MATERIAL",material!=null,material==null?
                "Face "+face.id()+" has no bound material":"Face "+face.id()+" uses "+material.key()+" ("+material.width()+" × "+material.height()+")"),
                new EditorAgentService.Check("VISIBLE_FACE",frame().boundTo(face.id(),face.material()),"Native rendered coverage for face "+face.id()));
        }catch(IOException failure){
            return List.of(new EditorAgentService.Check("RENDER_AVAILABLE",false,failure.toString()));
        }
    }
}
