package org.beckenburg;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
/** Authoring export of the actual canonical van, not a replacement or envelope. */
public final class HollowCreekVanSource {
 public static void main(String[] args)throws Exception{
  var renderer=new SceneRenderer();var van=new VanStaging(new VanStaging.Placement(0,0,0));
  Method method=SceneRenderer.class.getDeclaredMethod("vanModel",java.util.List.class,VanStaging.class,Set.class);method.setAccessible(true);
  var faces=new ArrayList<Object>();method.invoke(renderer,faces,van,Set.of());
  LevelSceneSource.writeFaces(Path.of("build/level-verification/scenes/hollow-creek-canonical-van"),faces);
 }
}
