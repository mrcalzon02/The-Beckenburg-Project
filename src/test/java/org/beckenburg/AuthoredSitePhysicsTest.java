package org.beckenburg;
import java.io.*;
import java.security.*;
import java.util.*;
import org.beckenburg.LevelDefinition.*;

/** Analytic geometry and hostile-file cases independent of the native authoring script. */
public final class AuthoredSitePhysicsTest {
 private record Face(Vec a,Vec b,Vec c,int flags){}
 private record Part(String name,boolean closed,List<Face> faces){}
 private static int checks;
 private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 private static byte[] encode(List<Part> parts)throws Exception{
  var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(0x48435332);out.writeInt(parts.size());out.writeInt(parts.stream().mapToInt(p->p.faces().size()).sum());
  for(var p:parts){byte[] name=p.name().getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeShort(name.length);out.write(name);out.writeByte(p.closed()?1:0);out.writeInt(p.faces().size());
   for(var f:p.faces()){for(var v:List.of(f.a(),f.b(),f.c())){out.writeFloat((float)v.x());out.writeFloat((float)v.y());out.writeFloat((float)v.z());}out.writeByte(f.flags());}
  }return bytes.toByteArray();
 }
 private static AuthoredSitePhysics read(byte[] bytes,List<Collider> blockers)throws Exception{return AuthoredSitePhysics.read(new ByteArrayInputStream(bytes),sha(bytes),blockers);}
 private static Part floor(double slope){return new Part("analytic floor",false,List.of(new Face(new Vec(-5,-5*slope,-5),new Vec(-5,-5*slope,5),new Vec(5,5*slope,5),7),new Face(new Vec(-5,-5*slope,-5),new Vec(5,5*slope,5),new Vec(5,5*slope,-5),7)));}
 private static Part box(){
  Vec[] v={new Vec(0,0,0),new Vec(2,0,0),new Vec(2,2,0),new Vec(0,2,0),new Vec(0,0,2),new Vec(2,0,2),new Vec(2,2,2),new Vec(0,2,2)};
  int[][] f={{0,2,1},{0,3,2},{4,5,6},{4,6,7},{0,1,5},{0,5,4},{3,7,6},{3,6,2},{0,4,7},{0,7,3},{1,2,6},{1,6,5}};
  var faces=new ArrayList<Face>();for(var t:f)faces.add(new Face(v[t[0]],v[t[1]],v[t[2]],6));return new Part("closed opaque box",true,faces);
 }
 private static void bad(byte[] bytes,String why)throws Exception{
  try{read(bytes,List.of());throw new AssertionError("Accepted "+why);}catch(IOException expected){checks++;}
 }
 public static void main(String[] args)throws Exception{
  byte[] bytes=encode(List.of(floor(0),box()));var map=read(bytes,List.of(new Collider(-3,0,-1,.04,.7,3,0)));
  check(map.triangleCount()==14&&map.componentCount()==2,"wire counts");
  check(map.support(-2,0,0,.01).y()==0,"actual floor");
  check(map.support(20,0,0,.25)==null,"no fabricated support outside mesh");
  check(map.support(-2,0,8,.25)==null,"no wrong-storey snap");
  check(map.traversable(new Vec(-2,0,0),1.78),"unobstructed supported capsule");
  check(map.bodyBlocked(new Vec(1,0,1),1.78,.22),"reject spawn entirely inside closed solid");
  check(!map.traversable(new Vec(1,0,1),1.78),"solid volume is not walkable ground");
  check(map.resolveMove(new Vec(-.5,0,1),.5,1,1.78)==null,"sweep does not tunnel through a wall");
  check(map.resolveMove(new Vec(-2,0,0),-1.5,.5,1.78)!=null,"valid swept input follows source support");
  check(map.resolveMove(new Vec(-2,0,0),4,0,1.78)==null,"reject oversized movement");
  check(!map.traversable(new Vec(4.9,0,0),1.78),"full capsule footprint cannot hang off a floor");
  check(map.bodyBlocked(new Vec(-3,0,-1),1.78,.22),"movement-only barrier contains capsule");
  check(map.clearSight(new Vec(-3.5,1.2,-1),new Vec(-2.5,1.2,-1)),"invisible barrier never occludes light");
  check(!map.clearSight(new Vec(-1,1,1),new Vec(3,1,1)),"opaque visible solid occludes light");
  check(map.clearSight(new Vec(-1,3,1),new Vec(3,3,1)),"open ray above solid");
  check(map.clearSight(new Vec(3,3,3),new Vec(0,3,0)),"negative-going grid-corner endpoint terminates");
  check(map.clearSight(new Vec(3,3,-3),new Vec(0,3,0)),"mixed-direction grid-corner endpoint does not overrun");
  check(map.clearSight(new Vec(-3,3,3),new Vec(0,3,0)),"reverse mixed-direction grid-corner endpoint does not overrun");
  check(map.clearSight(new Vec(0,3,1),new Vec(-0.0,3,-1)),"negative-zero ray axis terminates");
  var steep=read(encode(List.of(floor(.3))),List.of());check(steep.support(0,0,0,.2)==null,"reject unwalkable grade");
  var grade=read(encode(List.of(floor(.2))),List.of());check(grade.support(1,0,.2,.01)!=null,"accept intended maximum grade");
  check(grade.resolveMove(new Vec(-2,-.4,0),-1.5,0,1.78)!=null,"supported ascent matches actual triangles");
  check(map.bodyBlocked(new Vec(Double.NaN,0,0),1.78,.22),"nonfinite actor");
  check(!map.clearSight(new Vec(0,0,0),new Vec(Double.POSITIVE_INFINITY,1,0)),"nonfinite optical ray");
  try{AuthoredSitePhysics.read(new ByteArrayInputStream(bytes),"00",List.of());throw new AssertionError("accepted wrong hash");}catch(IOException expected){checks++;}
  bad(Arrays.copyOf(bytes,bytes.length-1),"truncation");bad(Arrays.copyOf(bytes,bytes.length+1),"trailing data");
  byte[] legacy=bytes.clone();legacy[3]=0x31;bad(legacy,"old stale HCS1 payload");
  bad(encode(List.of(new Part("flags",false,List.of(new Face(new Vec(0,0,0),new Vec(0,0,1),new Vec(1,0,0),8))))),"unknown surface flags");
  bad(encode(List.of(new Part("unphysical floor",false,List.of(new Face(new Vec(0,0,0),new Vec(0,0,1),new Vec(1,0,0),1))))),"support without body surface");
  bad(encode(List.of(new Part("bad vertex",false,List.of(new Face(new Vec(Double.NaN,0,0),new Vec(0,0,1),new Vec(1,0,0),6))))),"nonfinite vertex");
  bad(encode(List.of(new Part("empty triangle",false,List.of(new Face(new Vec(0,0,0),new Vec(0,0,0),new Vec(1,0,0),6))))),"degenerate triangle");
  var nativeMap=AuthoredSitePhysics.hollowCreek();check(nativeMap.sha256().equals(HollowCreekContract.SHA256),"actual packaged native source hash");
  check(nativeMap.triangleCount()>100000&&nativeMap.movementBlockerCount()==250,"actual current native payload");
  System.out.println("PASS "+checks+" analytic/parser/source-identity checks; actual native "+nativeMap.triangleCount()+" triangles and "+nativeMap.componentCount()+" components");
 }
}
