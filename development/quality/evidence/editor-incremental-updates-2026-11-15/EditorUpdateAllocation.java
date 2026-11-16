package org.beckenburg.editor;
import java.nio.file.Path;
import java.lang.management.ManagementFactory;
import java.util.*;
import org.beckenburg.editor.EditorTopologyProject.*;
public class EditorUpdateAllocation {
 public static void main(String[] args) {
  var doc=EditorTopologyProject.fromMesh(new EditorMeshPipeline.Mesh("profile",List.of(),0),"0".repeat(64),Path.of("profile.btop"));
  var seed=doc.snapshot();var vertices=new ArrayList<Vertex>();var faces=new ArrayList<Face>();
  for(int i=0;i<120000;i++){long id=i*3L;vertices.add(new Vertex(id,"p",i*2,0,0));vertices.add(new Vertex(id+1,"p",i*2+1,0,0));vertices.add(new Vertex(id+2,"p",i*2,1,0));faces.add(new Face(i,"p",new Corner(id,0,0),new Corner(id+1,1,0),new Corner(id+2,0,1)));}
  var initial=new State(seed.sceneId(),seed.sourceFingerprint(),"profile",0,vertices,faces);doc.restore(initial);
  var old=doc.observe();old.topology();var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().getId();
  long bytes=0;int count=12;
  for(int i=0;i<count+4;i++) {var prior=doc.snapshot();var next=new ArrayList<>(prior.faces());var f=next.get(119999);next.set(119999,f.withCorners(new Corner(f.a().vertex(),i+.25,0),f.b(),f.c()));doc.restore(new State(seed.sceneId(),seed.sourceFingerprint(),"profile",0,prior.vertices(),next));long start=bean.getThreadAllocatedBytes(thread);var updated=doc.observe(old);updated.topology();long allocated=bean.getThreadAllocatedBytes(thread)-start;if(i>=4)bytes+=allocated;old=updated;}
  System.out.println("120000-face UV update: mean thread allocation="+bytes/count+" bytes; "+count+" measured iterations after 4 warmups");
 }
}
