package org.beckenburg;

import org.beckenburg.editor.EditorProject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Only the adapter knows what a castle is. Editor toolkit stays reusable. */
public final class CastleEditorProject implements EditorProject<CastleLevelEdits.Snapshot> {
    private final CastleLevelEdits castle;
    public CastleEditorProject(CastleLevelEdits castle){this.castle=java.util.Objects.requireNonNull(castle);}
    public String id(){return "beckenburg/castle";}
    public String title(){return "Castle Beckenburg";}
    public Path documentPath(){return castle.path();}
    public CastleLevelEdits.Snapshot snapshot(){return castle.snapshot();}
    public void restore(CastleLevelEdits.Snapshot snapshot){castle.restore(snapshot);}
    public void save()throws IOException{castle.save();}
    public List<Asset> assets(){
        ArrayList<Asset> list=new ArrayList<>();
        list.add(new Asset("water://surface","Modules","Water surface module"));
        for(String mesh:GlbProps.availableModels())
            list.add(new Asset("assets://models/"+mesh,modelCategory(mesh),mesh));
        for(int material=0;material<MaterialAtlas.COUNT;material++)
            list.add(new Asset("assets://materials/"+material,"Materials","Material "+material));
        list.sort(Comparator.comparing(Asset::category).thenComparing(Asset::label));
        return List.copyOf(list);
    }
    /** Models are still one selectable GLB each: category names organize the
     * real source meshes, never group them into decorative fake prefabs. */
    private static String modelCategory(String mesh){
        if(mesh.endsWith("_fixed")||mesh.equals("monitoring_screen_mp_1")||
                mesh.equals("blender_site_map_fixed"))return "Models / Van fixtures";
        if(mesh.equals("painting_1")||mesh.equals("carpet_mp_1")||
                mesh.startsWith("photo_frame_")||mesh.startsWith("level_loose_")||
                mesh.startsWith("level_story_"))return "Models / Decoration";
        if(mesh.contains("wardrobe")||mesh.contains("cabinet")||
                mesh.contains("shelf")||mesh.contains("table")||
                mesh.contains("chair")||mesh.contains("sofa")||
                mesh.startsWith("bed_")||mesh.startsWith("bedside_"))return "Models / Furniture";
        if(EquipmentModelCatalog.approved(mesh)||mesh.startsWith("exploration_"))
            return "Models / Investigation equipment";
        if(mesh.startsWith("kenney_")||mesh.startsWith("level_castle_"))
            return "Models / Architecture";
        if(mesh.startsWith("level_"))return "Models / Environment";
        return "Models / Other";
    }
    public List<Node> hierarchy(){
        ArrayList<Node> nodes=new ArrayList<>();
        for(var surface:castle.surfaces())nodes.add(new Node(surface.id(),
            surface.kind().name(),surface.kind()+" • "+surface.id(),
            surface.cx(),surface.base(),surface.cz()));
        for(var prop:castle.props())nodes.add(new Node(prop.id(),"PROP",
            prop.id()+" • "+prop.mesh(),prop.x(),prop.y(),prop.z()));
        for(var lamp:castle.lamps())nodes.add(new Node(lamp.id(),"LAMP",
            lamp.id()+" • "+lamp.on(),lamp.x(),lamp.y(),lamp.z()));
        for(var station:CastleHubMap.Station.values()){
            double[] point=castle.station(station);
            nodes.add(new Node(station.name(),"STATION","Station • "+station.name(),point[0],0,point[1]));
        }
        double[] spawn=castle.spawn();
        nodes.add(new Node("start","SPAWN","Player spawn",spawn[0],0,spawn[1]));
        return List.copyOf(nodes);
    }
}
