package org.beckenburg;

/** Verify that original Blender geometry and source glTF UV orientation survive. */
public final class GgBotBlenderSourceUvTest {
    private static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] args){
        check(GlbProps.imageRow(.125,128,true)==16,
            "Blender glTF top-of-atlas must map to top pixels");
        check(GlbProps.imageRow(.875,128,true)==112,
            "Blender glTF bottom-of-atlas must map to bottom pixels");
        check(GlbProps.imageRow(.125,128,false)==112,
            "Unrelated imported materials retain their previous mapping");
        // CRT surfaces are inserted into the same material registry. They
        // must register a UV-origin flag or all subsequent material IDs drift.
        int crt=GlbProps.televisionMaterial(1.25,2.50,3.75);
        GlbProps.updateTelevision(crt,true,0);
        GlbProps.sample(crt,.3,.4);
        GlbProps.sampleAlpha(crt,.3,.4);
        for(String id:new String[]{"ggbot_car01","ggbot_car05","ggbot_car08_van_shell"}){
            var model=GlbProps.model(id);
            check(model.triangles().size()>100,id+" real source mesh");
            int painted=0;
            java.util.Set<Integer> colors=new java.util.HashSet<>();
            for(var triangle:model.triangles()){
                if(triangle.material()<GlbProps.TEXTURE_BASE)continue;
                painted++;
                double u=(triangle.a().u()+triangle.b().u()+triangle.c().u())/3;
                double v=(triangle.a().v()+triangle.b().v()+triangle.c().v())/3;
                colors.add(GlbProps.sample(triangle.material(),u,v));
            }
            check(painted>100,id+" painted triangle count");
            check(colors.size()>12,id+" original 128px textured color atlas");
        }
        GlbProps.sample(crt,.8,.2);
        System.out.println("PASS GGBot Blender-source meshes, glTF UV mapping and CRT material registry");
    }
}
