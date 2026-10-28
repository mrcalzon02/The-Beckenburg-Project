package org.beckenburg;
import java.util.*;
import javax.imageio.ImageIO;
/** Every tier entry resolves its own full-size singular Blender render. */
public final class EquipmentTierIconTest {
    public static void main(String[] args)throws Exception{
        int count=0;Map<String,Integer> modelPixels=new HashMap<>();
        for(var offer:EquipmentCatalogPanel.catalog()){
            String path="/org/beckenburg/ui/item-icons/"+offer.icon()+"_t"+(offer.tier().ordinal()+1)+".png";
            try(var stream=EquipmentTierIconTest.class.getResourceAsStream(path)){
                if(stream==null)throw new AssertionError("Missing individual icon: "+path);
                var image=ImageIO.read(stream);
                if(image.getWidth()!=512||image.getHeight()!=512)throw new AssertionError("Icon must preserve 512px render: "+path);
                int minX=512,minY=512,maxX=-1,maxY=-1;
                for(int y=0;y<512;y++)for(int x=0;x<512;x++)if((image.getRGB(x,y)>>>24)>3){
                    minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
                double fill=Math.max(maxX-minX+1,maxY-minY+1)/512.;
                if(fill<.78||fill>.96||minX<4||minY<4||maxX>507||maxY>507)
                    throw new AssertionError("Poor framing / clipping: "+path+" fill="+fill);
                String model=EquipmentModelCatalog.model(offer.id(),offer.tier());
                int hash=Arrays.hashCode(image.getRGB(0,0,512,512,null,0,512));
                for(var existing:modelPixels.entrySet())if(existing.getValue()==hash&&!existing.getKey().equals(model))
                    throw new AssertionError("Different models reused one image: "+existing.getKey()+" / "+model);
                modelPixels.put(model,hash);count++;
            }
        }
        if(count!=129)throw new AssertionError("Expected 129 item-tier images");
        System.out.println("PASS 129 distinct tier resources, actual 512px Blender images, close framing and no clipping");
    }
}
