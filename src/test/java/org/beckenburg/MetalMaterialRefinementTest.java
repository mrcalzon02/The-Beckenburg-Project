package org.beckenburg;
/** The painted-metal tile must not stamp periodic black grooves onto every panel. */
public final class MetalMaterialRefinementTest {
    public static void main(String[] args)throws Exception{
        var unchanged=java.security.MessageDigest.getInstance("SHA-256");
        double band=0,rest=0;int bands=0,others=0,min=255,max=0;
        for(int k=0;k<MaterialAtlas.COUNT;k++)for(int y=0;y<32;y++)for(int x=0;x<32;x++){
            double u=(x+.5)/32,v=(y+.5)/32;int p=MaterialAtlas.sample(k,u,v);
            if(p!=MaterialAtlas.sample(k,u-2,v+3))throw new AssertionError("Signed UV repeat changed");
            if(k!=MaterialAtlas.IRON){unchanged.update((byte)(p>>16));unchanged.update((byte)(p>>8));unchanged.update((byte)p);}
            else{int r=(p>>16)&255;min=Math.min(min,r);max=Math.max(max,r);if(x%8==0){band+=r;bands++;}else{rest+=r;others++;}}
        }
        String digest=java.util.HexFormat.of().formatHex(unchanged.digest());
        if(!digest.equals("baa65d08a699588d61ff1d923c84a4400ebffdde37f4d78488c96d19bf094441"))throw new AssertionError("An unrelated atlas material changed");
        double contrast=Math.abs(rest/others-band/bands);
        if(contrast>1.5)throw new AssertionError("Repeated eight-pixel dark stripe remains: "+contrast);
        if(max-min<6||max-min>24)throw new AssertionError("Metal wear must retain bounded visible variation");
        System.out.println("PASS periodic metal groove contrast16.652 -> "+contrast+"; original15other tiles exact, signed UV repeat and bounded wear preserved");
    }
}
