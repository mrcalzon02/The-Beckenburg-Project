package org.beckenburg;

/** Mip policy must not alter evidence, UI, live displays or imported alpha atlases. */
public final class RuntimeTextureMipPolicyTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        for(int id=LevelMaterials.BASE;id<=LevelMaterials.ROOF;id++)
            check(RuntimeTextureMipPolicy.eligible(id,128,128,false,false),"environment material "+id);
        check(!RuntimeTextureMipPolicy.eligible(MaterialAtlas.FOLIAGE,128,128,false,false),"procedural atlas excluded");
        check(!RuntimeTextureMipPolicy.eligible(GlbProps.TEXTURE_BASE,1024,1024,false,false),"imported GLB excluded");
        check(!RuntimeTextureMipPolicy.eligible(GlbProps.VAN_MAP_MATERIAL,768,512,true,true),"van CRT excluded");
        check(!RuntimeTextureMipPolicy.eligible(LevelMaterials.PLASTER,128,128,true,false),"live texture excluded");
        check(!RuntimeTextureMipPolicy.eligible(LevelMaterials.PLASTER,128,128,false,true),"readable label excluded");
        check(!RuntimeTextureMipPolicy.eligible(LevelMaterials.PLASTER,16,16,false,false),"tiny texture excluded");
        check(RuntimeTextureMipPolicy.levels(128,128)==8,"full 128 mip chain");
        check(RuntimeTextureMipPolicy.levels(256,128)==9,"nonsquare mip chain");
        check(RuntimeTextureMipPolicy.levels(1,1)==1,"one pixel base only");
        check(RuntimeTextureMipPolicy.extraRgbaBytes(128,128)==21844,"128x128 rgba mip extra bytes");
        check(RuntimeTextureMipPolicy.extraRgbaBytes(1,1)==0,"1x1 mip has no extras");
        for(int[] bad:new int[][]{{0,64},{16,0},{-1,128}}){
            try{RuntimeTextureMipPolicy.levels(bad[0],bad[1]);throw new AssertionError("accepted invalid dimensions");}
            catch(IllegalArgumentException good){}
        }
        System.out.println("PASS: conservative static-world mip policy, texture exceptions, chain sizes");
    }
}
