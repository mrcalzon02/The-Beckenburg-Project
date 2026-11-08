package org.beckenburg;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.beckenburg.editor.*;

/** Persisted architecture belongs to the native document and participates in undo. */
final class HouseSourceVersions {
    private HouseSourceVersions(){}
    static final String NODE="beckenburg-level-source",COMPONENT="beckenburg.level.source";
    static String generation(LevelDefinition source){
        return source.id().equals("briar-rebuild")?UserLevelLibrary.HOUSE_REBUILD:UserLevelLibrary.HOUSE_ORIGINAL;
    }
    static LevelDefinition source(String generation){
        return UserLevelLibrary.templateSource(HouseMap.Site.HOUSE,generation);
    }
    static RuntimeScene.Node node(String generation){
        validate(generation);
        return new RuntimeScene.Node(NODE,"House architectural source",null,"metadata",RuntimeScene.Transform.identity(),
            List.of(new RuntimeScene.Component(COMPONENT,Map.of("architecture",generation))));
    }
    static void validate(String generation){
        if(!UserLevelLibrary.HOUSE_ORIGINAL.equals(generation)&&!UserLevelLibrary.HOUSE_REBUILD.equals(generation))
            throw new IllegalArgumentException("Unsupported House architectural generation: "+generation);
    }
    static String generation(RuntimeScene scene)throws IOException{
        var node=scene.find(NODE);
        if(node==null)return UserLevelLibrary.HOUSE_ORIGINAL;
        try{
            if(node.components().size()!=1||!node.components().get(0).type().equals(COMPONENT)
                ||!node.components().get(0).properties().keySet().equals(Set.of("architecture")))
                throw new IllegalArgumentException("Invalid source component");
            String generation=node.components().get(0).properties().get("architecture");
            if(!node.equals(node(generation)))throw new IllegalArgumentException("Source metadata has unsupported fields");
            return generation;
        }catch(IllegalArgumentException invalid){throw new IOException("Invalid native House source metadata",invalid);}
    }
    static LevelDefinition selected(){
        Path path=LevelAuthoringSession.path(HouseMap.Site.HOUSE);
        try{
            RuntimeSceneDocument.recoverPending(path);
            // A fresh installation starts in the authored rebuild. Existing files
            // without metadata retain their historical building and coordinates.
            if(!Files.exists(path))return source(UserLevelLibrary.HOUSE_REBUILD);
            var document=RuntimeSceneDocument.load(path);
            if(!document.sceneId().equals("beckenburg/house"))throw new IOException("House scene identity mismatch");
            return source(generation(document));
        }catch(IOException failed){throw new IllegalStateException("Cannot select saved House architecture",failed);}
    }
}
