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
    CastleLevelEdits scene(){return castle;}
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
        if(ImportedAssetLibrary.approved(mesh))return "Models / Imported";
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
        for(var surface:castle.surfaces()) nodes.add(row(surface));
        for(var prop:castle.props()) nodes.add(row(prop));
        for(var lamp:castle.lamps()) nodes.add(row(lamp));
        for(var station:CastleHubMap.Station.values()) nodes.add(row(station));
        nodes.add(spawnRow());
        return List.copyOf(nodes);
    }
    public Node node(String id) {
        var surface=castle.surfaceById(id);
        if(surface!=null) return row(surface);
        var prop=castle.propById(id);
        if(prop!=null) return row(prop);
        var lamp=castle.lampById(id);
        if(lamp!=null) return row(lamp);
        for(var station:CastleHubMap.Station.values()) if(station.name().equals(id)) return row(station);
        if("start".equals(id)) return spawnRow();
        throw new IllegalArgumentException("Unknown native hierarchy node");
    }
    private static Node row(CastleLevelEdits.Surface s) {
        return new Node(s.id(),s.kind().name(),s.kind()+" • "+s.id(),s.cx(),s.base(),s.cz());
    }
    private static Node row(CastleLevelEdits.Prop p) {
        return new Node(p.id(),"PROP",p.id()+" • "+p.mesh(),p.x(),p.y(),p.z());
    }
    private static Node row(CastleLevelEdits.Lamp l) {
        return new Node(l.id(),"LAMP",l.id()+" • "+l.on(),l.x(),l.y(),l.z());
    }
    private Node row(CastleHubMap.Station station) {
        double[] point=castle.station(station);
        return new Node(station.name(),"STATION","Station • "+station.name(),point[0],0,point[1]);
    }
    private Node spawnRow() {
        double[] spawn=castle.spawn();
        return new Node("start","SPAWN","Player spawn",spawn[0],0,spawn[1]);
    }
}
