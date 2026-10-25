package org.beckenburg;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Headless material and near-plane renderer regressions; no GUI needed. */
public final class MaterialAtlasTest {
    private static void check(boolean value,String name) { if(!value)throw new AssertionError(name); }
    public static void main(String[] args) {
        BufferedImage atlas=MaterialAtlas.image();
        check(atlas.getWidth()==128 && atlas.getHeight()==128,"atlas dimensions");
        Set<Integer> tiles=new HashSet<>();
        for(int i=0;i<MaterialAtlas.COUNT;i++){
            int first=MaterialAtlas.sample(i,.25,.25);
            check(first==MaterialAtlas.sample(i,1.25,-.75),"repeat in positive and negative coordinates "+i);
            check(first==(atlas.getRGB((i%4)*32+8,(i/4)*32+8) & 0xffffff),"samples match source pixels "+i);
            tiles.add(first);
        }
        check(tiles.size()>=12,"palette has distinguishable materials");
        check(MaterialAtlas.floor(3)!=MaterialAtlas.floor(1),"kitchen and library floor use different materials");
        check(MaterialAtlas.wall(6)!=MaterialAtlas.wall(2),"cellar and nursery walls differ");
        HouseMap map=new HouseMap();
        InvestigationSession session=new InvestigationSession(map,InvestigationSession.Role.OBSERVER,765L);
        SceneRenderer renderer=new SceneRenderer();
        int[][] frames=new int[4][];
        for(int i=0;i<4;i++){
            // The house now occupies real metre-based rooms at x=12..24,
            // z=10..24. Old 4.5/3.5 coordinates tested only empty grounds.
            var spawn=map.spawn(i);
            check(map.traversable(spawn.x(),spawn.y(),spawn.z()),
                "texture inspection camera stands in authored room "+i);
            renderer.x=spawn.x();renderer.z=spawn.z();
            renderer.eyeHeight=spawn.y()+1.62;
            renderer.yaw=i*Math.PI/2;renderer.pitch=-.12;
            renderer.draw(map,session,0.5);
            check(Arrays.stream(renderer.depths()).anyMatch(Double::isFinite),"depth in view "+i);
            check(Arrays.stream(renderer.pixels()).distinct().count()>65,"real texture detail in view "+i);
            frames[i]=renderer.pixels().clone();
        }
        check(!Arrays.equals(frames[0],frames[1]),"different rooms create different rendered imagery");
        var interior=map.spawn(1);
        renderer.x=interior.x();renderer.z=interior.z();
        renderer.eyeHeight=interior.y()+1.62;renderer.yaw=0;renderer.pitch=0;
        renderer.draw(map,session,0.5);
        int[] lit=renderer.pixels().clone();
        renderer.lamp=false;renderer.draw(map,session,0.5);
        check(!Arrays.equals(lit,renderer.pixels()),"flashlight affects textured world");
        // Deliberately move close to a wall: triangulated faces cross NEAR and must
        // clip position AND UV coordinates without generating invalid colors/depth.
        // Approach the living-room's authored east wall (x=18), not
        // the old grid's x=8 wall, and verify actual physical support.
        renderer.x=17.91;renderer.z=19.0;renderer.yaw=Math.PI/2;
        renderer.eyeHeight=map.floorY(1)+1.62;
        renderer.lamp=true;renderer.draw(map,session,0.5);
        check(Arrays.stream(renderer.depths()).anyMatch(Double::isFinite),"near-plane textured scene visible");
        check(Arrays.stream(renderer.pixels()).allMatch(c->c>=0 && c<=0xffffff),"all near-plane pixels finite and bounded");
        System.out.println("PASS: 16 reproducible tiles, 128px atlas, world UV wrapping, room materials, view variants, lamp and near-plane clipping");
    }
}
