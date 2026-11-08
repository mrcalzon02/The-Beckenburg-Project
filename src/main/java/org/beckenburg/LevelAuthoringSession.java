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
public final class LevelAuthoringSession implements EditorProject<LevelAuthoringSession.Snapshot> {
    private static final String COMPONENT = "beckenburg.level.prop";
    private final String documentId;
    private final Path document;
    private final LevelDefinition source;
    private final ArrayList<Prop> props = new ArrayList<>();
    private final ArrayList<String> propIds = new ArrayList<>();
    private long revision=1;
    private String diskFingerprint;
    /** Identity is authoring metadata paired with the native props, not another scene. */
    public record Snapshot(String sceneId,List<String> ids,List<Prop> props){
        public Snapshot{
            Objects.requireNonNull(sceneId);ids=List.copyOf(ids);props=List.copyOf(props);
            if(sceneId.isBlank()||ids.size()!=props.size()||ids.size()>4096
                ||new java.util.HashSet<>(ids).size()!=ids.size()
                ||ids.stream().anyMatch(id->id.isBlank()||id.length()>128))
                throw new IllegalArgumentException("Invalid mission identity snapshot");
        }
    }

    public LevelAuthoringSession(HouseMap.Site site) throws IOException {
        Objects.requireNonNull(site);
        this.documentId=sceneId(site);this.document=path(site);
        this.source=LevelPlans.buildSource(site);
        var loaded=loadRevision(document,documentId,source);restore(loaded.state());diskFingerprint=loaded.fingerprint();
    }
    public String id(){return documentId;}
    private static String sceneId(HouseMap.Site site){
        return site==HouseMap.Site.USER
            ?"beckenburg/user/"+UserLevelLibrary.activeId()
            :"beckenburg/"+site.name().toLowerCase(Locale.ROOT);
    }
    public String title(){return source.title();}
    public Path documentPath(){return document;}
    public static Path path(HouseMap.Site site){
        if(site==HouseMap.Site.USER)return UserLevelLibrary.activeDocument();
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","levels",
            site.name().toLowerCase(Locale.ROOT)+".bscn").toAbsolutePath().normalize();
    }
    public synchronized List<Prop> props(){return List.copyOf(props);}
    public synchronized int size(){return props.size();}
    public synchronized Prop prop(int index){return props.get(index);}
    public synchronized String nodeId(int index){return propIds.get(index);}
    public synchronized int indexOf(String id){
        int index=propIds.indexOf(Objects.requireNonNull(id));
        if(index<0)throw new IllegalArgumentException("Unknown prop ID: "+id);
        return index;
    }
    public synchronized void put(int index,Prop prop){
        checkProp(prop);props.set(index,prop);revision++;
    }
    public synchronized void add(Prop prop){
        checkProp(prop);
        if(props.size()>=4096)throw new IllegalStateException("Level object limit");
        props.add(prop);propIds.add("prop-"+java.util.UUID.randomUUID());revision++;
    }
    public synchronized void remove(int index){props.remove(index);propIds.remove(index);revision++;}
    public synchronized long revision(){return revision;}
    public synchronized void validate(){snapshot();for(Prop prop:props)checkProp(prop);}
    public synchronized void reopen()throws IOException{
        var loaded=loadRevision(document,documentId,source);restore(loaded.state());diskFingerprint=loaded.fingerprint();
    }
    public static void recoverSavedDocument(HouseMap.Site site)throws IOException{
        recoverPreviousDocument(path(site),sceneId(site),LevelPlans.buildSource(site));
    }
    private static void recoverPreviousDocument(Path document,String documentId,LevelDefinition source)throws IOException{
        RuntimeSceneDocument.restorePrevious(document,path->{
            var candidate=loadState(path,documentId,source);
            for(var prop:candidate.props())checkProp(prop,source);
        });
    }
    public synchronized void recoverPrevious()throws IOException{recoverPreviousDocument(document,documentId,source);reopen();}
    private void checkProp(Prop p){
        checkProp(p,source);
    }
    private static void checkProp(Prop p,LevelDefinition source){
        Objects.requireNonNull(p);
        if(p.room()<0||p.room()>=source.rooms().size()
            ||p.mesh()==null||!GlbProps.availableModels().contains(p.mesh())
            ||!Double.isFinite(p.x()+p.y()+p.z()+p.scale()+p.yaw()
                +p.halfX()+p.halfZ()+p.height())
            ||p.scale()<=0||p.scale()>10||p.halfX()<0||p.halfZ()<0||p.height()<0)
            throw new IllegalArgumentException("Invalid mission prop");
    }
    public synchronized void resetToSource(){
        restore(sourceState(documentId,source.props()));
    }
    public LevelDefinition sourceLevel(){return source;}
    public synchronized Snapshot snapshot(){return new Snapshot(documentId,propIds,props);}
    public synchronized void restore(Snapshot snapshot){
        Objects.requireNonNull(snapshot);
        if(!documentId.equals(snapshot.sceneId()))throw new IllegalArgumentException("Snapshot belongs to another mission");
        for(Prop prop:snapshot.props())checkProp(prop);
        props.clear();props.addAll(snapshot.props());propIds.clear();propIds.addAll(snapshot.ids());revision++;
    }
    public synchronized LevelDefinition level(){
        return compose(source,props);
    }
    public synchronized List<Node> hierarchy(){
        ArrayList<Node> result=new ArrayList<>();
        for(int i=0;i<props.size();i++) result.add(propNode(i));
        return List.copyOf(result);
    }
    public synchronized Node node(String id) { return propNode(indexOf(id)); }
    public synchronized HierarchyPage hierarchyPage(int offset,int limit) {
        int end=HierarchyPage.end(props.size(),offset,limit);
        var rows=new ArrayList<Node>(end-offset);
        for (int i=offset;i<end;i++) rows.add(propNode(i));
        return new HierarchyPage(props.size(),offset,rows);
    }
    private Node propNode(int index) {
        var prop = props.get(index);
        return new Node(propIds.get(index),"PROP",prop.mesh()+" / room "+prop.room(),prop.x(),prop.y(),prop.z());
    }
    public List<Asset> assets(){
        ArrayList<Asset> result=new ArrayList<>();
        for(String mesh:GlbProps.availableModels())
            result.add(new Asset("assets://models/"+mesh,"Models",mesh));
        return List.copyOf(result);
    }
    public synchronized void save() throws IOException {
        diskFingerprint=RuntimeSceneDocument.saveIfUnchanged(documentPath(),sceneDocument(snapshot()),diskFingerprint);
    }
    static void saveProps(Path destination,String id,List<Prop> source) throws IOException {
        saveState(destination,sourceState(id,source));
    }
    private static Snapshot sourceState(String id,List<Prop> props){
        var ids=new ArrayList<String>();for(int i=0;i<props.size();i++)ids.add("prop-"+i);
        return new Snapshot(id,ids,props);
    }
    private static void saveState(Path destination,Snapshot state)throws IOException{RuntimeSceneDocument.save(destination,sceneDocument(state));}
    private static InMemoryRuntimeScene sceneDocument(Snapshot state){
        InMemoryRuntimeScene document=new InMemoryRuntimeScene(state.sceneId());
        for(int i=0;i<state.props().size();i++){
            Prop p=state.props().get(i);
            document.replace(new RuntimeScene.Node(state.ids().get(i),p.mesh(),null,"world",
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
        return document;
    }
    private record Loaded(Snapshot state,String fingerprint){}
    private static Snapshot loadState(Path path,String id,LevelDefinition source)throws IOException{return loadRevision(path,id,source).state();}
    private static Loaded loadRevision(Path path,String id,LevelDefinition source) throws IOException {
        RuntimeSceneDocument.recoverPending(path);
        if(!Files.exists(path))return new Loaded(sourceState(id,source.props()),null);
        return readStored(path,id,source);
    }
    synchronized boolean matchesSaved(Snapshot live)throws IOException{
        return live.equals(readStored(document,documentId,source).state());
    }
    private static Loaded readStored(Path path,String id,LevelDefinition source)throws IOException{
        var loaded=RuntimeSceneDocument.readStoredRevision(path);var scene=loaded.scene();
        if(!scene.sceneId().equals(id))
            throw new IOException("Level document identity mismatch: "+path);
        ArrayList<Prop> result=new ArrayList<>();
        ArrayList<String> ids=new ArrayList<>();
        for(var node:scene.nodes()){
            if(node.components().size()!=1
                ||!node.components().get(0).type().equals(COMPONENT))
                throw new IOException("Unsupported level node "+node.id());
            try{
                var data=node.components().get(0).properties();
                var required=java.util.Set.of("mesh","room","halfX","halfZ","height","support");
                var allowed=java.util.Set.of("mesh","room","halfX","halfZ","height","support","yawRad");
                if(!data.keySet().containsAll(required)||!allowed.containsAll(data.keySet()))
                    throw new IllegalArgumentException("Unsupported native prop properties");
                if(!node.name().equals(data.get("mesh"))||!node.layer().equals("world"))
                    throw new IllegalArgumentException("Native mission cannot preserve custom node labels/layers");
                var t=node.transform();
                if(data.containsKey("yawRad")){
                    double degrees=Math.toDegrees(Double.parseDouble(data.get("yawRad")));
                    if(!Double.isFinite(degrees)||Math.abs(degrees-t.yaw())>Math.max(1e-10,Math.ulp(t.yaw())*4))
                        throw new IllegalArgumentException("Contradictory native and portable yaw values");
                }
                if(node.parentId()!=null||t.pitch()!=0||t.roll()!=0
                    ||t.scaleX()!=t.scaleY()||t.scaleX()!=t.scaleZ())
                    throw new IllegalArgumentException("Mission runtime supports root props with yaw and uniform scale only");
                Prop prop=new Prop(data.get("mesh"),Integer.parseInt(data.get("room")),
                    t.x(),t.y(),t.z(),t.scaleX(),
                    data.containsKey("yawRad")?Double.parseDouble(data.get("yawRad"))
                        :Math.toRadians(t.yaw()),
                    Double.parseDouble(data.get("halfX")),
                    Double.parseDouble(data.get("halfZ")),
                    Double.parseDouble(data.get("height")),data.get("support"));
                checkProp(prop,source);
                result.add(prop);ids.add(node.id());
            }catch(RuntimeException invalid){
                throw new IOException("Invalid level object "+node.id()+" in "+path,invalid);
            }
        }
        return new Loaded(new Snapshot(id,ids,result),loaded.fingerprint());
    }
    static LevelDefinition apply(HouseMap.Site site,LevelDefinition source){
        try{
            RuntimeSceneDocument.recoverPending(path(site));
            if(!Files.exists(path(site)))return source;
            return compose(source,loadState(path(site),sceneId(site),source).props());
        }
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
