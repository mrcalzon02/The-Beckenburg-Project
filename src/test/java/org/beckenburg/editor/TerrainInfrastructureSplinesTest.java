package org.beckenburg.editor;

import java.nio.file.*;
import java.util.List;

/** Independent rail/road/river spline authoring and seam-safe terrain integration. */
public final class TerrainInfrastructureSplinesTest {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("portable-rail-terrain");
        try{
            var world=StitchedHeightmapWorld.create(root.resolve("map"),
                new StitchedHeightmapWorld.Settings(32,2,0,0,-200,1000,100L,4));
            world.generateRegion(0,0,2,1);
            var stack=new TerrainLayerStack(world,0,0);
            var input=List.of(new TerrainLayerStack.Point(3,12,100),
                new TerrainLayerStack.Point(27,12,130));
            var controls=TerrainInfrastructureSplines.limitGrade(input,2,3);
            check(Math.abs(controls.get(1).elevation()-101.44)<.0001,
                "Rail max incline limits terrain grade independently of existing topography");
            var a=new TerrainInfrastructureSplines.Alignment("rail-main",
                TerrainInfrastructureSplines.Kind.RAIL,controls,2,4,4);
            var sampled=TerrainInfrastructureSplines.evaluate(a,2);
            check(sampled.size()>4,"Spline centerline is tessellated");
            check(sampled.get(0).x()==3&&sampled.get(sampled.size()-1).x()==27,
                "Spline endpoints preserved");
            var routes=TerrainInfrastructureSplines.open(world);
            routes.put(a);routes.save();
            var reopened=TerrainInfrastructureSplines.open(world);
            check(reopened.list().size()==1,"Route document persisted");
            reopened.apply(stack,reopened.list().get(0));
            check(stack.material(15,12)==TerrainLayerStack.Material.GRAVEL.ordinal(),
                "Railway corridor auto-materialized as gravel");
            check(Math.abs(stack.height(15,12)-100.72)<.01,"Railway corridor graded");
            check(world.verifySeams().isEmpty(),"Spline authoring does not touch source seams");
            var road=new TerrainInfrastructureSplines.Alignment("road-1",
                TerrainInfrastructureSplines.Kind.ROAD,
                List.of(new TerrainLayerStack.Point(5,5,90),
                    new TerrainLayerStack.Point(15,8,91),
                    new TerrainLayerStack.Point(25,15,92)),3,4,12);
            var curved=TerrainInfrastructureSplines.evaluate(road,2);
            check(curved.size()>10&&curved.get(4).z()!=5,
                "Curved infrastructure path has interpolated control geometry");
            reopened.put(road);reopened.save();
            check(TerrainInfrastructureSplines.open(world).list().size()==2,
                "Multiple infrastructure kinds can coexist");
            stack.save();var again=new TerrainLayerStack(world,0,0);
            check(again.material(15,12)==TerrainLayerStack.Material.GRAVEL.ordinal(),
                "Corridor material survives editor reopening");
            var longRoute=new TerrainInfrastructureSplines.Alignment("rail-long",
                TerrainInfrastructureSplines.Kind.RAIL,
                List.of(new TerrainLayerStack.Point(-250,20,25),
                    new TerrainLayerStack.Point(250,20,25)),2,2,4);
            check(TerrainInfrastructureSplines.evaluate(longRoute,2).size()>256,
                "Long smooth rail route exceeds former 256-sample limit");
            reopened.apply(again,longRoute);
            check(Math.abs(again.height(16,20)-25)<.01,
                "Long rail profile grades tile without artificial segment cap");
            check(!reopened.remove("missing"),"Removing unknown route is harmless");
            check(reopened.remove("road-1"),"Route deletion works");
            System.out.println("PASS terrain spline persistence, multi-point interpolation, rail grade constraints, gravel corridor and source protection");
        }finally{
            try(var paths=Files.walk(root)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(p);}
        }
    }
}
