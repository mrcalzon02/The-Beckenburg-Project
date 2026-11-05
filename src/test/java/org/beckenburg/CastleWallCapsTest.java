package org.beckenburg;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** DEV-271: real wall mesh top must never z-fight an upper-floor slab. */
public final class CastleWallCapsTest {
    private static void check(boolean pass,String reason){if(!pass)throw new AssertionError(reason);}
    private static double area(List<CastleWallCaps.Patch> p){return p.stream().mapToDouble(CastleWallCaps.Patch::area).sum();}
    private static void near(double actual,double expected,String reason){
        check(Math.abs(actual-expected)<1e-7,reason+": "+actual+" expected "+expected);
    }
    private static CastleHubArchitecturalPlan.Deck floor(double x0,double z0,double x1,double z1,double y){
        return new CastleHubArchitecturalPlan.Deck(x0,z0,x1,z1,y,MaterialAtlas.PARQUET);
    }
    private static int renderedTopTriangles(CastleHubArchitecturalPlan.Wall wall)throws Exception{
        Method mesh=SceneRenderer.class.getDeclaredMethod("architecturalWall",List.class,CastleHubArchitecturalPlan.Wall.class);
        mesh.setAccessible(true);
        var faces=new ArrayList<Object>();
        mesh.invoke(null,faces,wall);
        check(faces.size()>=8,"Masonry side geometry was removed instead of hidden caps");
        int caps=0;
        for(var face:faces){
            Class<?> type=face.getClass();
            Method a=type.getDeclaredMethod("a"),b=type.getDeclaredMethod("b"),c=type.getDeclaredMethod("c");
            a.setAccessible(true);b.setAccessible(true);c.setAccessible(true);
            double[] ys=new double[3];int n=0;
            for(Object point:List.of(a.invoke(face),b.invoke(face),c.invoke(face))){
                Method y=point.getClass().getDeclaredMethod("y");y.setAccessible(true);
                ys[n++]=((Number)y.invoke(point)).doubleValue();
            }
            if(Math.abs(ys[0]-wall.top())<1e-8&&Math.abs(ys[1]-wall.top())<1e-8
                    &&Math.abs(ys[2]-wall.top())<1e-8)caps++;
        }
        return caps;
    }
    public static void main(String[] args)throws Exception{
        double y=CastleHubArchitecturalPlan.GROUND_CEILING;
        var corridor=floor(7.5,1,10.5,34,y);
        near(area(CastleWallCaps.exposed(7.5,21.79,10.5,22.21,y,List.of(corridor))),0,
            "Stone ground-wall cap draws through upper west hall parquet");
        var westWalk=floor(10.5,12,12.5,33,y);
        var partial=CastleWallCaps.exposed(9,17.79,13,18.21,y,List.of(corridor,westWalk));
        near(area(partial),.5*.42,"Partially covered upper/courtyard wall cap");
        for(var patch:partial)check(patch.x0()>=12.5-1e-8,"Covered strip leaked a stone cap");
        near(area(CastleWallCaps.exposed(13,17.79,14,18.21,y,List.of(corridor,westWalk))),.42,
            "Uncovered courtyard wall loses exposed masonry top");
        near(area(CastleWallCaps.exposed(9,17.79,13,18.21,y+.2,List.of(corridor,westWalk))),4*.42,
            "Slab at a different height erases the cap");
        var wall=new CastleHubArchitecturalPlan.Wall(7.5,22,10.5,22,0,y,
            MaterialAtlas.STONE,Double.NaN,0);
        check(renderedTopTriangles(wall)==0,
            "Renderer still emits two coplanar stone triangles over the upper corridor");
        // Preserve the same runtime collision authority and original room elevation.
        var map=new CastleHubMap();
        check(CastleLevelEdits.current().wallBlocks(8.5,1.5,22,.10),
            "Ground-floor partition collision was weakened");
        check(map.traversable(9,20,3.4),"Upper corridor floor no longer walkable");
        System.out.println("PASS DEV-271: wall cap union, partially exposed edges, real triangles and unchanged castle collision");
    }
}
