package org.beckenburg;
import dev.mrcalzon.water.*;
import dev.mrcalzon.water.WaterModule.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Game adapter for the independent module. Each constructed map owns an
 * immutable document; edits take effect in preview immediately and in a new
 * playable map after reload, with the document included in its revision. */
public final class WorldWater {
 private static final Map<String,Texture> textures=new HashMap<>();
 private static final Map<String,WaterModule> modules=new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<String,WaterModule> e){return size()>64;}};
 public static Path documents(){String root=System.getProperty("beckenburg.water.documents","");return root.isBlank()?Path.of(System.getProperty("user.home"),".beckenburg","editor","water"):Path.of(root);}
 public static Path assets(){String root=System.getProperty("beckenburg.water.assets","");if(!root.isBlank())return Path.of(root);Path project=Path.of("assets/runtime");return Files.isDirectory(project)?project:Path.of(System.getProperty("user.home"),".beckenburg","editor","water-assets");}
 public static Path path(String level){if(!level.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Water level identifier");return documents().resolve(level+".water");}
 public static WaterDocument defaults(String level){
  if(level.equals("hollow-creek"))return new WaterDocument(level,List.of(new WaterPlacement("hollow-creek-main",20,HollowCreekWater.LEVEL,-45,250,150,0,new WaterSettings().with("bed.profile","hollow-creek"))));
  return new WaterDocument(level,List.of());
 }
 public static WaterDocument load(String level){try{
  Path file=path(level);WaterDocument doc;if(Files.exists(file))doc=WaterDocument.read(Files.newInputStream(file));else try(var in=WorldWater.class.getResourceAsStream("/org/beckenburg/water/"+level+".water")){doc=in==null?defaults(level):WaterDocument.read(in);}
  if(!doc.level().equals(level))throw new IOException("Water module belongs to another map");return doc;
 }catch(IOException e){throw new IllegalStateException("Cannot load water for "+level+": "+e.getMessage(),e);}}
 public static synchronized Texture texture(String uri){
  if(uri.startsWith("water://"))return WaterTextures.builtin(uri);
  org.beckenburg.editor.EditorAssets.validateUri(uri);if(!uri.startsWith("assets://"))throw new IllegalArgumentException("Water texture provider must be water:// or assets://");
  String relative=uri.substring("assets://".length());Path root=assets().toAbsolutePath().normalize(),file=root.resolve(relative).normalize();if(!file.startsWith(root))throw new IllegalArgumentException("Texture escapes asset root");
  try{String key=uri+":"+(Files.isRegularFile(file)?Files.getLastModifiedTime(file).toMillis()+":"+Files.size(file):"packaged");var found=textures.get(key);if(found!=null)return found;Texture value=Files.isRegularFile(file)?WaterTextures.png(Files.newInputStream(file)):WaterTextures.png(WorldWater.class.getResourceAsStream("/org/beckenburg/"+relative));if(textures.size()>64)textures.clear();textures.put(key,value);return value;}catch(IOException e){throw new IllegalArgumentException("Cannot load water texture "+uri+": "+e.getMessage(),e);}
 }
 public static WaterModule build(WaterDocument doc){var bodies=new ArrayList<Body>();for(var p:doc.placements()){
  boolean nativeBed=p.settings().text("bed.profile").equals("hollow-creek");if(nativeBed&&!doc.level().equals("hollow-creek"))throw new IllegalArgumentException("Hollow Creek basin is available only on its authored map; use flat for other maps");
  if(!nativeBed&&!p.settings().text("bed.profile").equals("flat"))throw new IllegalArgumentException("Unknown water bed profile");
  List<? extends Exclusion> dry=doc.level().equals("hollow-creek")?HollowCreekWater.module().bodies().get(0).dry():List.of();
  bodies.add(p.body(WorldWater::texture,placement->(x,z)->{double y=nativeBed?HollowCreekWater.bed().bottomY(x,z):Double.NaN;return Double.isFinite(y)?y:placement.y()-placement.settings().number("bed.depth");},dry));
 }return new WaterModule(bodies);}
 public static synchronized WaterModule module(WaterDocument doc){return modules.computeIfAbsent(doc.revision(),key->build(doc));}
 public static void save(WaterDocument doc)throws IOException{doc.save(path(doc.level()));}
 public static String importTexture(Path source)throws IOException{
  byte[] bytes;try(var in=Files.newInputStream(source)){bytes=in.readNBytes(16*1024*1024+1);}if(bytes.length>16*1024*1024)throw new IOException("Water texture exceeds16MB");WaterTextures.png(new ByteArrayInputStream(bytes));String hash;try{hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
  Path folder=assets().resolve("water-imports");Files.createDirectories(folder);Path target=folder.resolve(hash+".png");if(!Files.exists(target))Files.write(target,bytes,StandardOpenOption.CREATE_NEW);return "assets://water-imports/"+hash+".png";
 }
 private WorldWater(){}
}
