package org.beckenburg;

/**
 * Conservative, opt-in GPU mipmapping for immutable architectural diffuse surfaces.
 * Imported GLB atlases (especially transparent foliage, labels, cameras and HUDs)
 * are intentionally excluded until their sampling parity has hardware proof.
 * This class is pure policy: it never changes world geometry or gameplay clocks.
 */
public final class RuntimeTextureMipPolicy {
    public static final String OPT_IN_PROPERTY="beckenburg.experimental.staticWorldMipmaps";
    private RuntimeTextureMipPolicy(){}

    public static boolean eligible(int material,int width,int height,
                                   boolean dynamic,boolean readableLabel){
        return material>=LevelMaterials.BASE&&material<=LevelMaterials.ROOF
            &&width>=32&&height>=32&&!dynamic&&!readableLabel;
    }

    /** Count the complete 2D mip chain, including the full-resolution base level. */
    public static int levels(int width,int height){
        if(width<1||height<1)throw new IllegalArgumentException("invalid texture dimensions");
        int count=1;
        while(width>1||height>1){
            width=Math.max(1,width/2);height=Math.max(1,height/2);count++;
        }
        return count;
    }

    /** Estimated extra uncompressed RGBA8888 texels; not measured GPU residency. */
    public static long extraRgbaBytes(int width,int height){
        if(width<1||height<1)throw new IllegalArgumentException("invalid texture dimensions");
        long bytes=0;
        while(width>1||height>1){
            width=Math.max(1,width/2);height=Math.max(1,height/2);
            bytes+=4L*width*height;
        }
        return bytes;
    }
}
