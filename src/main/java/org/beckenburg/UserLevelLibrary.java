package org.beckenburg;

import org.beckenburg.LevelDefinition.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Named editable mission maps. A user level selects a shipped architectural
 * source as its initial scaffold; edits are stored in the same .bscn format
 * and fed into HouseMap by LevelPlans, not a separate preview-only scene.
 */
public final class UserLevelLibrary {
    private UserLevelLibrary(){}
    static final String HOUSE_ORIGINAL="house-original-v1";
    static final String HOUSE_REBUILD="house-rebuild-v1";
    private static final String SHIPPED="shipped-v1";

    /** Explicit architectural generation, independent of the current default dispatch. */
    static LevelDefinition templateSource(HouseMap.Site template,String generation){
        if(template==HouseMap.Site.USER||template==HouseMap.Site.HOLLOW_CREEK)
            throw new IllegalArgumentException("Choose an existing standard map template");
        if(template==HouseMap.Site.HOUSE){
            if(HOUSE_ORIGINAL.equals(generation))return LevelPlans.legacyHouseSource();
            if(HOUSE_REBUILD.equals(generation))return HouseRebuildFurnishings.source();
        }else if(SHIPPED.equals(generation))return LevelPlans.buildSource(template);
        throw new IllegalArgumentException("Unsupported architectural generation "+generation+" for "+template);
    }
    public static Path root(){
        return Path.of(System.getProperty("user.home"),".beckenburg","editor","user-levels")
            .toAbsolutePath().normalize();
    }
    public static String validateId(String id){
        if(id==null||!id.matches("[a-zA-Z][a-zA-Z0-9_-]{0,39}"))
            throw new IllegalArgumentException("Level name: 1–40 ASCII letters, digits, _ or -, starting with a letter");
        return id;
    }
    public static Path folder(String id){return root().resolve(validateId(id));}
    /** Author-owned custom terrain worlds never share a directory with built-in maps. */
    public static Path terrainWorldsFolder(String id){
        return folder(id).resolve("terrain-worlds");
    }
    public static Path document(String id){return folder(id).resolve("scene.bscn");}
    public static List<String> names()throws IOException {
        if(!Files.isDirectory(root()))return List.of();
        var ids=new ArrayList<String>();
        try(var entries=Files.list(root())){
            entries.filter(Files::isDirectory).forEach(p->{
                String id=p.getFileName().toString();
                try{validateId(id);
                    if(Files.isRegularFile(p.resolve("template.properties"))
                        &&Files.isRegularFile(p.resolve("scene.bscn")))ids.add(id);
                }catch(IllegalArgumentException invalid){/* not an authored scene */}
            });
        }
        ids.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(ids);
    }
    public static String activeId() {
        try{
            Path selected=root().resolve("active-level.txt");
            if(!Files.isRegularFile(selected))return null;
            String id=validateId(Files.readString(selected,StandardCharsets.UTF_8).strip());
            return Files.isRegularFile(document(id))&&Files.isRegularFile(folder(id).resolve("template.properties"))
                ?id:null;
        }catch(IOException|IllegalArgumentException invalid){return null;}
    }
    public static Path activeDocument(){
        String id=activeId();
        if(id==null)throw new IllegalStateException("No selected authored user level");
        return document(id);
    }
    public static void activate(String id)throws IOException {
        id=validateId(id);
        if(!Files.isRegularFile(document(id))
            ||!Files.isRegularFile(folder(id).resolve("template.properties")))
            throw new IOException("Unknown user level: "+id);
        Files.createDirectories(root());
        Path pending=Files.createTempFile(root(),"level-",".pending");
        try{
            Files.writeString(pending,id+"\n",StandardCharsets.UTF_8);
            try{Files.move(pending,root().resolve("active-level.txt"),
                StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(java.nio.file.AtomicMoveNotSupportedException unsupported){
                Files.move(pending,root().resolve("active-level.txt"),StandardCopyOption.REPLACE_EXISTING);
            }
        }finally{Files.deleteIfExists(pending);}
    }
    /** Never overwrite a previous authored level, including an incomplete one. */
    public static void create(String id,HouseMap.Site template)throws IOException {
        create(id,template,template==HouseMap.Site.HOUSE?HOUSE_ORIGINAL:SHIPPED);
    }
    static void create(String id,HouseMap.Site template,String generation)throws IOException {
        id=validateId(id);
        // Validate before making a folder: unsupported source versions must leave no partial project.
        var source=templateSource(template,generation);
        Path folder=folder(id);
        if(Files.exists(folder))throw new IOException("Level already exists: "+id);
        Files.createDirectories(folder);
        Properties properties=new Properties();
        properties.setProperty("format","1");
        properties.setProperty("template",template.name());
        properties.setProperty("architecture",generation);
        try(OutputStream out=Files.newOutputStream(folder.resolve("template.properties"))){
            properties.store(out,"Beckenburg authored level source");
        }
        // Persist the exact native source objects by reusing the standard
        // adapter serializer. Do not copy an unrelated Castle renderer.
        LevelAuthoringSession.saveProps(document(id),"beckenburg/user/"+id,source.props());
        activate(id);
    }
    public static LevelDefinition source() {
        String id=activeId();
        if(id==null)throw new IllegalStateException("No authored user level selected");
        Properties meta=new Properties();
        try(InputStream in=Files.newInputStream(folder(id).resolve("template.properties"))){
            meta.load(in);
        }catch(IOException failed){throw new IllegalStateException("Cannot open user level metadata",failed);}
        if(!"1".equals(meta.getProperty("format")))
            throw new IllegalStateException("Unsupported user level template version");
        HouseMap.Site template;
        try{template=HouseMap.Site.valueOf(meta.getProperty("template"));}
        catch(RuntimeException invalid){throw new IllegalStateException("Bad user level source",invalid);}
        if(template==HouseMap.Site.USER||template==HouseMap.Site.HOLLOW_CREEK)
            throw new IllegalStateException("User level template cannot refer to itself/native-only prototype");
        // Versionless House projects predate the rebuild. Never reinterpret their
        // authored coordinates against a later default building, or rewrite on open.
        String generation=meta.getProperty("architecture",
            template==HouseMap.Site.HOUSE?HOUSE_ORIGINAL:SHIPPED);
        LevelDefinition original;
        try{original=templateSource(template,generation);}
        catch(IllegalArgumentException invalid){throw new IllegalStateException("Unsupported user level architecture",invalid);}
        // Preserve source asset, water and sound IDs until the reusable module
        // contract supports custom IDs for all three systems.
        return new LevelDefinition(original.id(),original.revision()+"-custom-"+id,
            "Custom: "+id,original.rooms(),original.portals(),original.stairs(),
            original.voids(),original.props(),original.sockets(),original.colliders(),
            original.dock(),original.ambience(),original.water(),original.sounds());
    }
}
