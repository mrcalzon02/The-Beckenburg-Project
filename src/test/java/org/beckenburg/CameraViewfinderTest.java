package org.beckenburg;
import java.awt.image.BufferedImage;
import java.util.List;

/** Real LCD geometry and depth-tested placement across all six camera tiers. */
public final class CameraViewfinderTest {
    private static void require(boolean good,String why){
        if(!good)throw new AssertionError(why);
    }
    private static boolean lensPixel(int rgb){
        return (rgb&0xffffff)==0xff00b3||(rgb&0xffffff)==0x00ffcc;
    }
    private static void checkScreen(String kind,EquipmentTiers.Tier tier,BufferedImage scene){
        String name=HeldInstrumentMesh.source(kind,tier);
        List<GlbProps.Triangle> mesh=GlbProps.model(name).triangles();
        double[] low={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] high={-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
        double rear=Double.POSITIVE_INFINITY;
        for(var tri:mesh)for(var source:List.of(tri.a(),tri.b(),tri.c())){
            rear=Math.min(rear,source.z());
            var p=HeldInstrumentMesh.pose(name,source);
            double[] xyz={p.x(),p.y(),p.z()};
            for(int i=0;i<3;i++){
                low[i]=Math.min(low[i],xyz[i]);high[i]=Math.max(high[i],xyz[i]);
            }
        }
        double cx=(low[0]+high[0])/2,cy=(low[1]+high[1])/2,cz=(low[2]+high[2])/2;
        double scale=192/Math.max(.04,Math.max(high[0]-low[0],
            Math.max(high[1]-low[1],high[2]-low[2])));
        double left=Double.POSITIVE_INFINITY,top=Double.POSITIVE_INFINITY;
        double right=Double.NEGATIVE_INFINITY,bottom=Double.NEGATIVE_INFINITY;
        int faces=0;
        for(var tri:mesh)if(HeldInstrumentMesh.cameraScreenSurface(name,tri,rear)!=0){
            faces++;
            if(kind.equals("PHOTO_CAMERA"))
                require("Photo camera rear LCD".equals(GlbProps.materialName(tri.material())),
                    name+": authored LCD incorrectly uses the front-camera atlas");
            for(var source:List.of(tri.a(),tri.b(),tri.c())){
                var p=HeldInstrumentMesh.pose(name,source);
                double x=p.x()-cx,y=p.y()-cy,z=p.z()-cz;
                double sx=120+(x*.94-z*.30)*scale;
                double sy=250*.51-(y*.98-z*.18+x*.04)*scale;
                left=Math.min(left,sx);right=Math.max(right,sx);
                top=Math.min(top,sy);bottom=Math.max(bottom,sy);
            }
        }
        require(faces==2,name+": expected exactly two LCD faces, found "+faces);
        BufferedImage frame=HeldInstrumentMesh.image(kind,tier,null,scene);
        int pixels=0;
        for(int y=0;y<frame.getHeight();y++)for(int x=0;x<frame.getWidth();x++){
            if(!lensPixel(frame.getRGB(x,y)))continue;
            pixels++;
            require(x>=left-2&&x<=right+2&&y>=top-2&&y<=bottom+2,
                name+": camera pixels spill onto rear controls at "+x+","+y);
        }
        require(pixels>10,name+": actual viewfinder has no visible real lens pixels");
        BufferedImage off=HeldInstrumentMesh.image(kind,tier,null,null);
        for(int y=0;y<off.getHeight();y++)for(int x=0;x<off.getWidth();x++)
            require(!lensPixel(off.getRGB(x,y)),name+": stale lens pixels after power-off");
    }
    public static void main(String[] args){
        require(ToolBeltHud.stillCamera("PHOTO_CAMERA"),"still camera shutter");
        require(ToolBeltHud.cameraView("VIDEO_CAMERA"),"live video lens");
        require(!ToolBeltHud.shutterAllowed("PHOTO_CAMERA",false),"shutter requires raised camera");
        var grip=new ToolBeltHud.Grip();grip.hold();
        require(ToolBeltHud.shutterAllowed("PHOTO_CAMERA",grip.held()),"raised shutter works");
        grip.release();require(!ToolBeltHud.shutterAllowed("PHOTO_CAMERA",grip.held()),"release disarms");
        require(!ToolBeltHud.shutterAllowed("VIDEO_CAMERA",true),"video cannot expend photo film");
        var scene=new BufferedImage(320,200,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<200;y++)for(int x=0;x<320;x++)
            scene.setRGB(x,y,x<160?0xff00b3:0x00ffcc);
        for(var tier:EquipmentTiers.Tier.values()){
            checkScreen("PHOTO_CAMERA",tier,scene);
            checkScreen("VIDEO_CAMERA",tier,scene);
        }
        System.out.println("PASS six real camera LCDs: localized live pixels, matte off state, no control spill");
    }
}
