package org.beckenburg;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Native exports must use the exact runtime sampler, including glTF atlas origin and diffuse factor. */
public final class LevelSceneTextureParityTest {
    public static void main(String[] args)throws Exception{
        boolean fitout=args.length>0&&args[0].equals("HOUSE_REBUILD_FITOUT");
        boolean functions=args.length>0&&args[0].equals("HOUSE_REBUILD_FUNCTIONS");
        String output=fitout?"build/native-texture-parity-fitout":functions?"build/native-texture-parity-functions":"build/native-texture-parity";
        LevelSceneSource.main(new String[]{fitout?"HOUSE_REBUILD_FITOUT":functions?"HOUSE_REBUILD_FUNCTIONS":"HOUSE_REBUILD_ENVELOPE","--output",output});
        var source=Path.of(output,"textures");
        int van=GlbProps.model("ggbot_car08_van_shell").triangles().get(0).material(),images=0;long texels=0;
        var matcher=java.util.regex.Pattern.compile("\"id\":(-?[0-9]+)").matcher(Files.readString(source.getParent().resolve("material-metadata.json")));
        while(matcher.find()){
        int mat=Integer.parseInt(matcher.group(1));if(mat<0)continue;
        var image=ImageIO.read(source.resolve(mat+".png").toFile());int errors=0,large=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            double u=(x+.5)/image.getWidth(),v=1-(y+.5)/image.getHeight();
            int expected=mat>=256?GlbProps.sample(mat,u,v):mat>=32?LevelMaterials.sample(mat,u,v):MaterialAtlas.sample(mat,u,v);
            int actual=image.getRGB(x,y)&0xffffff;if(actual!=expected)errors++;
            int delta=0;for(int shift:new int[]{0,8,16})delta+=Math.abs(((actual>>shift)&255)-((expected>>shift)&255));
            if(delta>12)large++;
        }
        if(errors!=0)throw new AssertionError("Native export material "+mat+" (van="+(mat==van)+") differs from actual runtime: "+errors+"/"+(image.getWidth()*image.getHeight())+" texels; "+large+" exceed12combined channel levels");
        images++;texels+=(long)image.getWidth()*image.getHeight();
        }
        if(images<10)throw new AssertionError("Incomplete texture review");
        System.out.println("PASS "+images+" native materials / "+texels+" texels exactly match actual runtime sampling, including original van atlas; UVs/mesh untouched");
    }
}
