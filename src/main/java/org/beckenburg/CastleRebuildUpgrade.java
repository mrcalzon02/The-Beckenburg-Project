package org.beckenburg;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

/** Three-way native rebuild: old built-in -> saved edits -> current built-in. */
public final class CastleRebuildUpgrade {
    public static final int VERSION=1;
    private CastleRebuildUpgrade(){}
    public enum Baseline {
        ORIGINAL("Original Castle before rebuilt wings","castle-baseline-original.properties","22ab766b1102ceaa93d9085b3e50044f66551c9627ebfbc061a4332b88607016"),
        BEFORE_FINISH("Rebuilt rooms before final furnishings","castle-baseline-before-finish.properties","6951abb11004f2c94cd0b5e2cc3947cf2e5550a5f27734d45ed94611ddeb1d30"),
        CURRENT("Rebuilt Castle with final furnishings","castle-baseline-rebuilt-v1.properties","7b6b68f9267841513a49e039c23a0302b6658722d7ed0e52c85614ebfc732d66");
        private final String title,file,sha;
        Baseline(String title,String file,String sha){this.title=title;this.file=file;this.sha=sha;}
        @Override public String toString(){return title;}
    }
    public record Plan(CastleLevelEdits.Snapshot before,CastleLevelEdits.Snapshot after,List<String> preserved){
        public Plan{preserved=List.copyOf(preserved);}
    }
    private static final Map<Baseline,CastleLevelEdits.Snapshot> CACHE=new EnumMap<>(Baseline.class);
    public static synchronized CastleLevelEdits.Snapshot baseline(Baseline which)throws IOException{
        Objects.requireNonNull(which);
        var existing=CACHE.get(which);if(existing!=null)return existing;
        try(var in=CastleRebuildUpgrade.class.getResourceAsStream("/org/beckenburg/sites/"+which.file)){
            if(in==null)throw new IOException("Missing Castle rebuild baseline "+which);
            byte[] bytes=in.readNBytes(1024*1024+1);
            if(bytes.length>1024*1024||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(which.sha))throw new IOException("Invalid Castle rebuild baseline "+which);
            var parsed=CastleLevelEdits.readBuiltinBaseline(bytes);CACHE.put(which,parsed);return parsed;
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static boolean sameContents(CastleLevelEdits.Snapshot a,CastleLevelEdits.Snapshot b){return a.withRebuildLayout(VERSION).equals(b.withRebuildLayout(VERSION));}
    /** Only exact known built-ins migrate automatically. Custom scenes remain untouched. */
    static CastleLevelEdits.Snapshot uneditedReplacement(CastleLevelEdits.Snapshot saved)throws IOException{
        var target=CastleLevelEdits.rebuiltDefaults();
        if(sameContents(saved,target))return target;
        for(var which:Baseline.values())if(sameContents(saved,baseline(which)))return target;
        return null;
    }
    private static <T>Map<String,T> index(List<T> list,Function<T,String> id){
        var result=new LinkedHashMap<String,T>();for(T value:list)result.put(id.apply(value),value);return result;
    }
    private static <K,V>Map<K,V> merge(String kind,Map<K,V> base,Map<K,V> saved,Map<K,V> target,List<String> kept){
        var result=new LinkedHashMap<>(target);var keys=new LinkedHashSet<>(base.keySet());keys.addAll(saved.keySet());
        for(K key:keys)if(!Objects.deepEquals(base.get(key),saved.get(key))){
            if(saved.containsKey(key))result.put(key,saved.get(key));else result.remove(key);
            kept.add(kind+":"+key+(saved.containsKey(key)?"":" (deleted)"));
        }
        return result;
    }
    private static Map<Integer,Boolean> nav(Set<Integer> cells){var map=new LinkedHashMap<Integer,Boolean>();for(int cell:cells)map.put(cell,true);return map;}
    public static Plan plan(CastleLevelEdits.Snapshot saved,Baseline which)throws IOException{
        Objects.requireNonNull(saved);Objects.requireNonNull(which);
        // Reapplying a historical baseline must never resurrect current-version deletions.
        if(saved.rebuildLayout()>=VERSION)return new Plan(saved,saved,List.of());
        var base=baseline(which);var target=CastleLevelEdits.rebuiltDefaults();var kept=new ArrayList<String>();
        var props=merge("prop",index(base.props(),CastleLevelEdits.Prop::id),index(saved.props(),CastleLevelEdits.Prop::id),index(target.props(),CastleLevelEdits.Prop::id),kept);
        var lamps=merge("lamp",index(base.lamps(),CastleLevelEdits.Lamp::id),index(saved.lamps(),CastleLevelEdits.Lamp::id),index(target.lamps(),CastleLevelEdits.Lamp::id),kept);
        var surfaces=merge("surface",index(base.surfaces(),CastleLevelEdits.Surface::id),index(saved.surfaces(),CastleLevelEdits.Surface::id),index(target.surfaces(),CastleLevelEdits.Surface::id),kept);
        var stations=merge("station",base.stations(),saved.stations(),target.stations(),kept);
        var floors=merge("floor-paint",base.floorPaint(),saved.floorPaint(),target.floorPaint(),kept);
        var walls=merge("wall-paint",base.wallPaint(),saved.wallPaint(),target.wallPaint(),kept);
        var blocked=merge("navigation",nav(base.navBlocked()),nav(saved.navBlocked()),nav(target.navBlocked()),kept);
        var doors=merge("door",base.doors(),saved.doors(),target.doors(),kept);
        boolean moved=saved.spawnX()!=base.spawnX()||saved.spawnZ()!=base.spawnZ()||saved.spawnYaw()!=base.spawnYaw();if(moved)kept.add("spawn");
        var result=new CastleLevelEdits.Snapshot(List.copyOf(props.values()),List.copyOf(lamps.values()),List.copyOf(surfaces.values()),stations,floors,walls,blocked.keySet(),doors,
            moved?saved.spawnX():target.spawnX(),moved?saved.spawnZ():target.spawnZ(),moved?saved.spawnYaw():target.spawnYaw(),VERSION);
        return new Plan(saved,result,kept);
    }
    /** Same authoritative singleton/history as the human editor; never saves implicitly. */
    public static Plan apply(CastleLevelEdits scene,Baseline which){
        if(scene!=CastleLevelEdits.current())throw new IllegalArgumentException("Rebuild requires the active native Castle");
        var before=scene.snapshot();
        try{
            Plan plan=plan(before,which);if(before.equals(plan.after()))return plan;
            var priorWalls=scene.walls();scene.restore(plan.after());
            CastleRebuildValidation.validate(scene,plan,priorWalls);
            return plan;
        }catch(IOException failure){scene.restore(before);throw new IllegalStateException("Cannot read original Castle baseline",failure);}
        catch(RuntimeException failure){scene.restore(before);throw failure;}
    }
}
