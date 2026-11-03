package org.beckenburg;
import org.beckenburg.editor.*;
import java.nio.charset.StandardCharsets;
public final class NormalLossControl {
 public static void main(String[] args)throws Exception{
  byte[] bytes="v 0 0 0\nv 1 0 0\nv 0 1 0\nvn 0 0 2\nf 1//1 2//1 3//1\n".getBytes(StandardCharsets.UTF_8);
  var actual=GlbProps.decode(EditorGlbWriter.encode(EditorMeshPipeline.read("authored.obj",bytes))).triangles().get(0).a();
  if(Math.abs(actual.nx())>1e-8||Math.abs(actual.ny())>1e-8||Math.abs(actual.nz()-1)>1e-8)throw new AssertionError("Authored source normal lost before actual game decoder: "+actual.nx()+","+actual.ny()+","+actual.nz());
  System.out.println("PASS exact source-authored corner direction reaches actual game decoder");
 }
}
