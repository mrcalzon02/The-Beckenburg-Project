import java.nio.file.*;
import org.beckenburg.editor.*;
public class MaterialLossControl {
 public static void main(String[] args)throws Exception {
  var mesh=EditorMeshPipeline.read(Path.of(args[0]));
  try {mesh.getClass().getMethod("materials");System.out.println("Native material API present");}
  catch(NoSuchMethodException missing) {System.out.println("REPRODUCED: reviewed OBJ/MTL usemtl/map_Kd imports geometry but has no native material table; GLB uses generated checkerboard");}
  Files.write(Path.of(args[1]),EditorGlbWriter.encode(mesh));
 }
}
