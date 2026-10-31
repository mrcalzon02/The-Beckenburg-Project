package org.beckenburg;

import org.beckenburg.LevelDefinition.*;
import org.beckenburg.editor.EditorProject;
import org.beckenburg.editor.InMemoryRuntimeScene;
import org.beckenburg.editor.RuntimeScene;
import org.beckenburg.editor.RuntimeSceneDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Mission-site authoring adapter. The editor and LevelPlans both consume the
 * same versioned document. A saved prop position is used by game rendering,
 * traversability, sight and interaction collision on the next level load.
 * Unlike a separate scene preview, this adapter mutates the LevelDefinition
 * that HouseMap owns.
 */
public final class LevelAuthoringSession implements EditorProject<List<Prop>> {
    private static final String COMPONENT = "beckenburg.level.prop";
    private final HouseMap.Site site;
    private final LevelDefinition source;
    private final ArrayList<Prop> props = new ArrayList<>();

    public LevelAuthoringSession(HouseMap.Site site) throws IOException {
        this.site=Objects.requireNonNull(site);
        this.source=LevelPlans.buildSource(site);
        props.addAll(loadProps(site,source));
    }
    public String id(){return sceneId(site);}
    private static String sceneId(HouseMap.Site site){
        return site==HouseMap.Site.USER
            ?"beckenburg/user/"+UserLevelLibrary.activeId()
            :"beckenburg/"+site.name().toLowerCase(Locale.ROOT);
    }
    public String title(){return source.title();}
    public Path documentPath(){return path(site);}
    public static Path path(HouseMap.Site site){
        if(site==HouseMap.Site.USER)return UserLevelLibrary.activeDocument();
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","levels",
            site.name().toLowerCase(Locale.ROOT)+".bscn").toAbsolutePath().normalize();
    }
    public synchronized List<Prop> props(){return List.copyOf(props);}
    public synchronized int size(){return props.size();}
    public synchronized Prop prop(int index){return props.get(index);}
    public synchronized void put(int index,Prop prop){
        checkProp(prop);props.set(index,prop);
    }
    public synchronized void add(Prop prop){
        checkProp(prop);
        if(props.size()>=4096)throw new IllegalStateException("Level object limit");
        props.add(prop);
    }
    public synchronized void remove(int index){props.remove(index);}
    private void checkProp(Prop p){
        Objects.requireNonNull(p);
        if(p.room()<0||p.room()>=source.rooms().size()
            ||p.mesh()==null||!GlbProps.availableModels().contains(p.mesh())
            ||!Double.isFinite(p.x()+p.y()+p.z()+p.scale()+p.yaw()
                +p.halfX()+p.halfZ()+p.height())
            ||p.scale()<=0||p.scale()>10||p.halfX()<0||p.halfZ()<0||p.height()<0)
            throw new IllegalArgumentException("Invalid mission prop");
    }
    public synchronized void resetToSource(){
        props.clear();props.addAll(source.props());
    }
    public LevelDefinition sourceLevel(){return source;}
    public synchronized List<Prop> snapshot(){return List.copyOf(props);}
    public synchronized void restore(List<Prop> snapshot){
        Objects.requireNonNull(snapshot);
        props.clear();props.addAll(snapshot);
    }
    public synchronized LevelDefinition level(){
        return compose(source,props);
    }
    public List<Node> hierarchy(){
        ArrayList<Node> result=new ArrayList<>();
        for(int i=0;i<props.size();i++){
            Prop p=props.get(i);
            result.add(new Node("prop-"+i,"PROP",p.mesh()+" / room "+p.room(),
                p.x(),p.y(),p.z()));
        }
        return List.copyOf(result);
    }
    public List<Asset> assets(){
        ArrayList<Asset> result=new ArrayList<>();
        for(String mesh:GlbProps.availableModels())
            result.add(new Asset("assets://models/"+mesh,"Models",mesh));
        return List.copyOf(result);
    }
    public synchronized void save() throws IOException {
        saveProps(documentPath(),id(),props);
    }
    static void saveProps(Path destination,String id,List<Prop> source) throws IOException {
        InMemoryRuntimeScene document=new InMemoryRuntimeScene(id);
        for(int i=0;i<source.size();i++){
            Prop p=source.get(i);
            document.replace(new RuntimeScene.Node("prop-"+i,p.mesh(),null,"world",
                new RuntimeScene.Transform(p.x(),p.y(),p.z(),0,
                    Math.toDegrees(p.yaw()),0,p.scale(),p.scale(),p.scale()),
                List.of(new RuntimeScene.Component(COMPONENT,Map.of(
                    "mesh",p.mesh(),"room",Integer.toString(p.room()),
                    "yawRad",Double.toString(p.yaw()),
                    "halfX",Double.toString(p.halfX()),
                    "halfZ",Double.toString(p.halfZ()),
                    "height",Double.toString(p.height()),
                    "support",p.support())))));
        }
        RuntimeSceneDocument.save(destination,document);
    }
    private static List<Prop> loadProps(HouseMap.Site site,LevelDefinition source) throws IOException {
        Path path=path(site);
        if(!Files.exists(path))return source.props();
        var scene=RuntimeSceneDocument.load(path);
        String id=sceneId(site);
        if(!scene.sceneId().equals(id))
            throw new IOException("Level document identity mismatch: "+path);
        ArrayList<Prop> result=new ArrayList<>();
        for(var node:scene.nodes()){
            if(node.components().size()!=1
                ||!node.components().get(0).type().equals(COMPONENT))
                throw new IOException("Unsupported level node "+node.id());
            try{
                var data=node.components().get(0).properties();
                var t=node.transform();
                Prop prop=new Prop(data.get("mesh"),Integer.parseInt(data.get("room")),
                    t.x(),t.y(),t.z(),t.scaleX(),
                    data.containsKey("yawRad")?Double.parseDouble(data.get("yawRad"))
                        :Math.toRadians(t.yaw()),
                    Double.parseDouble(data.get("halfX")),
                    Double.parseDouble(data.get("halfZ")),
                    Double.parseDouble(data.get("height")),data.get("support"));
                if(prop.room()<0||prop.room()>=source.rooms().size()
                    ||!GlbProps.availableModels().contains(prop.mesh()))
                    throw new IllegalArgumentException("Unknown room or model");
                result.add(prop);
            }catch(RuntimeException invalid){
                throw new IOException("Invalid level object "+node.id()+" in "+path,invalid);
            }
        }
        return List.copyOf(result);
    }
    static LevelDefinition apply(HouseMap.Site site,LevelDefinition source){
        if(!Files.exists(path(site)))return source;
        try{return compose(source,loadProps(site,source));}
        catch(IOException bad){
            throw new IllegalStateException("Cannot load authored "+site+" level",bad);
        }
    }
    /**
     * The authored props replace their native source entries, and their
     * matching physical collider bodies are rebuilt from the SAME placement
     * helper used by LevelPlans during initial map construction.
     */
    static LevelDefinition compose(LevelDefinition source,List<Prop> edited){
        ArrayList<Collider> collision=new ArrayList<>(source.colliders());
        for(Prop p:source.props())if(p.halfX()>0&&p.halfZ()>0)
            collision.remove(LevelPlans.propCollider(p));
        for(Prop p:edited)if(p.halfX()>0&&p.halfZ()>0)
            collision.add(LevelPlans.propCollider(p));
        return new LevelDefinition(source.id(),source.revision(),source.title(),
            source.rooms(),source.portals(),source.stairs(),source.voids(),
            List.copyOf(edited),source.sockets(),collision,source.dock(),
            source.ambience(),source.water(),source.sounds());
    }
}
