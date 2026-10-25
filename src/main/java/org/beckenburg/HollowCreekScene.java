package org.beckenburg;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.io.*;
import org.beckenburg.LevelDefinition.Vec;
/** Bounded original local meshes and exact source world matrices. */
public final class HollowCreekScene {
 public record Instance(String id,String sha,double[] matrix,double[] bounds,boolean billboard,String object){
  public Instance{matrix=matrix.clone();bounds=bounds.clone();}
  @Override public double[] matrix(){return matrix.clone();}
  @Override public double[] bounds(){return bounds.clone();}
  public double x(){return (bounds[0]+bounds[3])/2;}public double z(){return (bounds[2]+bounds[5])/2;}
  public double radius(){return Math.hypot(bounds[3]-bounds[0],bounds[5]-bounds[2])/2+.5;}
  public Vec transform(GlbProps.Vertex v,double eyeX,double eyeZ){
   double x=matrix[0]*v.x()+matrix[1]*v.y()+matrix[2]*v.z()+matrix[3],y=matrix[4]*v.x()+matrix[5]*v.y()+matrix[6]*v.z()+matrix[7],z=matrix[8]*v.x()+matrix[9]*v.y()+matrix[10]*v.z()+matrix[11];
   if(billboard){double length=Math.hypot(matrix[0],matrix[8]),view=Math.hypot(eyeX-matrix[3],eyeZ-matrix[11]);if(length>1e-8&&view>1e-8){double u=((x-matrix[3])*matrix[0]+(z-matrix[11])*matrix[8])/length;x=matrix[3]+(eyeZ-matrix[11])/view*u;z=matrix[11]-(eyeX-matrix[3])/view*u;}}
   return new Vec(x,y,z);
  }
 }
 private record Data(String hash,String source,List<Instance> instances,Set<String> ids){}
 private static final class Holder{static final Data DATA=load();}
 private static Data load(){
  try(var in=HollowCreekScene.class.getResourceAsStream("/org/beckenburg/sites/hollow-creek-scene.tsv")){
   if(in==null)throw new IOException("Missing authored Hollow Creek scene");byte[] bytes=in.readNBytes(2000001);if(bytes.length>2000000)throw new IOException("Native scene manifest budget");String text=new String(bytes,StandardCharsets.UTF_8).replace("\r\n","\n");String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));String[] rows=text.split("\\R");
   if(rows.length<2||rows.length>2049||!rows[0].matches("HOLLOW_CREEK_SCENE_1\\t[0-9a-f]{64}"))throw new IOException("Invalid native manifest version");
   var all=new ArrayList<Instance>();var ids=new HashSet<String>();
   for(int line=1;line<rows.length;line++){
    String[] p=rows[line].split("\\t",-1);if(p.length!=26||!p[0].matches("level_hc_native_[0-9]{3}_[0-9]{1,2}")||!p[1].matches("[0-9a-f]{64}")||!p[24].matches("[01]"))throw new IOException("Invalid native instance "+line);
    double[] matrix=new double[16],bounds=new double[6];for(int i=0;i<22;i++){double n=Double.parseDouble(p[i+2]);if(!Double.isFinite(n)||Math.abs(n)>1000)throw new IOException("Bad native transform/bounds");if(i<16)matrix[i]=n;else bounds[i-16]=n;}
    if(matrix[12]!=0||matrix[13]!=0||matrix[14]!=0||matrix[15]!=1||bounds[0]>bounds[3]||bounds[1]>bounds[4]||bounds[2]>bounds[5])throw new IOException("Malformed native matrix/bounds");
    all.add(new Instance(p[0],p[1],matrix,bounds,p[24].equals("1"),p[25]));ids.add(p[0]);
   }return new Data(hash,rows[0].substring(rows[0].indexOf('\t')+1),List.copyOf(all),Set.copyOf(ids));
  }catch(IOException|NoSuchAlgorithmException|NumberFormatException invalid){throw new IllegalStateException("Cannot load current native site",invalid);}
 }
 public static List<Instance> instances(){return Holder.DATA.instances;}
 public static boolean approved(String id){return id.startsWith("level_hc_native_")&&Holder.DATA.ids.contains(id);}
 public static String revision(){return Holder.DATA.hash;}
 public static String sourceSha(){return Holder.DATA.source;}
 private static final Set<String> VERIFIED=new HashSet<>();
 public static synchronized GlbProps.Model model(Instance instance){
  if(VERIFIED.add(instance.id))try(var in=HollowCreekScene.class.getResourceAsStream("/org/beckenburg/models/"+instance.id+".glb")){
   if(in==null)throw new IOException("Missing native model "+instance.id);byte[] bytes=in.readNBytes(4000001);String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));if(bytes.length>4000000||!sha.equals(instance.sha))throw new IOException("Native model hash mismatch "+instance.id);
  }catch(IOException|NoSuchAlgorithmException e){VERIFIED.remove(instance.id);throw new IllegalStateException(e);}
  return GlbProps.model(instance.id);
 }
 private HollowCreekScene(){}
}
