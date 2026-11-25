import java.io.IOException;
import java.nio.file.Path;
import org.beckenburg.editor.EditorTopologyProject;
class OldWorkspaceReader {
 public static void main(String[] args)throws Exception {
  try{EditorTopologyProject.open(Path.of(args[0]));throw new AssertionError("Old reader accepted version7");}
  catch(IOException expected){if(!"Unsupported native topology version".equals(expected.getMessage()))throw expected;System.out.println("PASS previous native6 reader explicitly refuses workspace-bearing native7 before loss");}
 }
}
