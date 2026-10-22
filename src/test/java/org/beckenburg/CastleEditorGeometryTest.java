package org.beckenburg;

import java.nio.file.Files;
import java.util.Properties;

/** Real geometry palette and persistence / runtime collision test. */
public final class CastleEditorGeometryTest {
    private static void check(boolean ok,String detail){if(!ok)throw new AssertionError(detail);}
    public static void main(String[] args)throws Exception{
        var folder=Files.createTempDirectory("beckenburg-architecture-");
        try{
            var path=folder.resolve("castle.properties");
            var edits=new CastleLevelEdits(path);
            check(edits.surfaces().stream().anyMatch(s->s.kind()==CastleLevelEdits.Kind.WALL),
                "Castle walls must be in editable palette");
            check(edits.surfaces().stream().anyMatch(s->s.kind()==CastleLevelEdits.Kind.FLOOR),
                "Upper floors must be editable");
            check(edits.surfaces().stream().anyMatch(s->s.kind()==CastleLevelEdits.Kind.ROOF),
                "Castle roofs must be editable");
            edits.put(new CastleLevelEdits.Surface("newWall",CastleLevelEdits.Kind.WALL,
                24,25,26,25,0,3.4,MaterialAtlas.BRICK));
            edits.put(new CastleLevelEdits.Surface("newDeck",CastleLevelEdits.Kind.FLOOR,
                23,24,27,27,3.4,.16,MaterialAtlas.PARQUET));
            edits.put(new CastleLevelEdits.Surface("newRoof",CastleLevelEdits.Kind.ROOF,
                12,24,16,27,3.6,1.7,MaterialAtlas.STONE));
            check(edits.wallBlocks(25,0,25,.21),"Placed wall blocks passage");
            check(edits.floorSupports(25,25,3.4),"Placed floor supports upper navigation");
            check(edits.roofs().stream().anyMatch(r->r.west()==12&&r.rise()==1.7),
                "Placed roof has authored dimensions");
            var before=edits.snapshot();
            edits.removeSurface("newWall");
            check(!edits.wallBlocks(25,0,25,.21),"Removing wall removes collider");
            edits.restore(before);
            check(edits.wallBlocks(25,0,25,.21),"Undo restores collider");
            edits.save();
            var restored=new CastleLevelEdits(path);
            check(restored.surfaces().stream().filter(s->s.id().startsWith("new")).count()==3,
                "All architecture types survive save/load");
            check(restored.walls().stream().anyMatch(w->w.material()==MaterialAtlas.BRICK
                &&w.horizontal()&&w.z0()==25),"Painted wall material persists");
            check(restored.roofs().stream().anyMatch(r->r.west()==12&&r.rise()==1.7),
                "Roof pitch persists");
            var props=new Properties();
            try(var in=Files.newInputStream(path)){props.load(in);}
            for(String key:java.util.List.copyOf(props.stringPropertyNames()))
                if(key.startsWith("surfaces."))props.remove(key);
            try(var out=Files.newOutputStream(path)){props.store(out,"old saved editor version");}
            var legacy=new CastleLevelEdits(path);
            check(legacy.surfaces().size()>20,"Existing scene preserved across schema upgrade");
            var live=CastleLevelEdits.current();
            var old=live.snapshot();
            try{
                var map=new CastleHubMap();
                live.put(new CastleLevelEdits.Surface("temporaryWall",CastleLevelEdits.Kind.WALL,
                    24,25,26,25,0,3.4,MaterialAtlas.BRICK));
                check(!map.traversable(25,25),"Live wall edit updates character collision");
                live.removeSurface("temporaryWall");
                live.put(new CastleLevelEdits.Surface("temporaryFloor",CastleLevelEdits.Kind.FLOOR,
                    23,24,27,27,3.4,.16,MaterialAtlas.STONE));
                check(CastleHubArchitecturalPlan.upperWalkable(25,25),
                    "Live floor edit changes upper walkability");
                live.put(new CastleLevelEdits.Surface("temporaryRoof",CastleLevelEdits.Kind.ROOF,
                    12,24,16,27,3.6,1.7,MaterialAtlas.STONE));
                check(!FixtureLighting.castleRayClear(map,13,2,25,13,6,25),
                    "Live roof geometry blocks light through roof");
            }finally{live.restore(old);}
            var project=new CastleEditorProject(restored);
            check(project.assets().stream().anyMatch(x->x.category().equals("Models")),
                "Adapter offers licensed game models");
            check(project.assets().stream().anyMatch(x->x.category().equals("Materials")),
                "Adapter offers game textures");
            check(project.hierarchy().stream().anyMatch(x->x.kind().equals("ROOF")),
                "Adapter exposes roof nodes");
            System.out.println("PASS wall/floor/roof editing, paint, undo, migration, game collision and shadows");
        }finally{
            try(var walk=Files.walk(folder)){walk.sorted(java.util.Comparator.reverseOrder())
                .forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
    }
}
