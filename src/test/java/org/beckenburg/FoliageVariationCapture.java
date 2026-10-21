package org.beckenburg;
import java.util.*;
import java.nio.file.*;

/** Export eight equal-scale placements of the single conifer source through the real variation code. */
public final class FoliageVariationCapture {
    public static void main(String[] args)throws Exception{
        var method=SceneRenderer.class.getDeclaredMethod("imported",List.class,String.class,double.class,double.class,double.class,double.class,double.class);method.setAccessible(true);
        List<Object> faces=new ArrayList<>();for(int i=0;i<8;i++)method.invoke(null,faces,"level_tree_conifer_worn",i*4.7,0.,0.,1.,0.);
        var room=new LevelDefinition.Room(13,"One source, eight stable variations",-4,-4,39,4,0,14,"earth","exterior",new LevelDefinition.Vec(0,0,0));
        var definition=new LevelSceneSource.Module("foliage-variation","stable-foliage-v1","One conifer source, eight instances",List.of(room),List.of(),List.of(),List.of(),new LevelDefinition.Dock(0,0,0,0));
        Path out=Path.of("build/level-verification/scenes/foliage-variation");Files.createDirectories(out);Files.writeString(out.resolve("definition.json"),LevelSceneSource.json(definition));
        LevelSceneSource.writeFaces(out,faces);
    }
}
