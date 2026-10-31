package org.beckenburg;

import java.util.Arrays;

/**
 * One PSX-resolution post-process for already-authorized local haunting events.
 * Replaces camera-space rectangles with smoothly evolving, mottled spectral
 * fields. No screen sprites, ghost identities, evidence or game logic originate
 * here. Single reusable RGB scratch buffer; no image decoding or per-frame GC.
 */
public final class HauntingScreenFx {
    private HauntingScreenFx() {}
    private static final ThreadLocal<Scratch> SCRATCH=ThreadLocal.withInitial(Scratch::new);
    private static final class Scratch {
        int[] source=new int[0], cloud=new int[0];
        void ensure(int pixels,int cells){
            if(source.length<pixels)source=new int[pixels];
            if(cloud.length<cells)cloud=new int[cells];
        }
    }
    private static int hash(int x,int y,int seed,int time){
        int h=seed^x*0x27d4eb2d^y*0x165667b1^time*0x9e3779b9;
        h^=h>>>16;h*=0x7feb352d;h^=h>>>15;h*=0x846ca68b;h^=h>>>16;
        return h;
    }
    private static float limit(float n){return n<0?0:n>1?1:n;}
    private static int clip(int n){return n<0?0:n>255?255:n;}
    private static int pixelIndex(int x,int max){return x<0?0:x>=max?max-1:x;}
    private static int tier(GhostHauntingFolio.Tier tier){
        return switch(tier){
            case AMBIENT->42;
            case MINOR->63;
            case MODERATE->89;
            case MAJOR->111;
            case DANGEROUS->129;
            case SIGNATURE->145;
        };
    }
    private static int[] palette(GhostHauntingFolio.Visual visual,
            GhostHauntingChoreography.Motif motif) {
        if(motif==GhostHauntingChoreography.Motif.PLAGUE_MIASMA)
            return new int[]{31,49,30};
        if(motif==GhostHauntingChoreography.Motif.BLADE_SWEEP)
            return new int[]{52,23,26};
        if(motif==GhostHauntingChoreography.Motif.ICE_BLOOM)
            return new int[]{69,112,145};
        if(motif==GhostHauntingChoreography.Motif.NOTHIC_IRIS)
            return new int[]{54,32,76};
        return switch(visual){
            case VEIL->new int[]{54,72,88};
            case SHADOW->new int[]{9,13,19};
            case STATIC->new int[]{30,71,77};
            case CHILL->new int[]{62,111,142};
            case PULSE->new int[]{70,42,65};
            case DISTORT->new int[]{57,46,71};
        };
    }

    /**
     * RGB pixels use the already composed, depth-correct world image. For
     * distortion, source sampling reads the same unmodified pre-effect frame;
     * effects cannot recursively smear previously distorted scan rows.
     */
    public static void paint(int[] pixels,int width,int height,
            GhostHauntingFolio.Occurrence occurrence,int tick,
            GhostHauntingChoreography.Motif motif){
        if(occurrence==null||!occurrence.active(tick,occurrence.room()))return;
        if(pixels==null||width<=0||height<=0||(long)width*height!=pixels.length)
            throw new IllegalArgumentException("invalid haunting framebuffer");
        int age=tick-occurrence.startTick();
        int duration=occurrence.entry().duration();
        float envelope=(float)Math.sin(Math.PI*age/duration);
        if(envelope<=.001f)return;
        // One peak cannot obscure the player's central aiming/evidence area.
        float intensity=envelope*tier(occurrence.entry().tier())/145f;
        GhostHauntingFolio.Visual visual=occurrence.entry().visual();
        int[] tint=palette(visual,motif);
        int tile=Math.max(9,height/20);
        int cellsX=(width+tile-1)/tile+3;
        int cellsY=(height+tile-1)/tile+3;
        Scratch scratch=SCRATCH.get();
        scratch.ensure(pixels.length,cellsX*cellsY);
        System.arraycopy(pixels,0,scratch.source,0,pixels.length);

        // Time-interpolated low-frequency spectral "texture": smooth clouds,
        // not screen bars, checkerboard blocks or periodic scan-line stripes.
        int phase=tick/13;
        float blend=(tick%13)/13f;
        int seed=occurrence.entry().id().hashCode();
        for(int gy=0;gy<cellsY;gy++)for(int gx=0;gx<cellsX;gx++){
            int first=hash(gx,gy,seed,phase)&255;
            int next=hash(gx,gy,seed,phase+1)&255;
            scratch.cloud[gy*cellsX+gx]=(int)(first+(next-first)*blend);
        }

        boolean bend=visual==GhostHauntingFolio.Visual.DISTORT
            ||visual==GhostHauntingFolio.Visual.STATIC
            ||motif==GhostHauntingChoreography.Motif.PERSPECTIVE_BEND
            ||motif==GhostHauntingChoreography.Motif.RADIO_GLITCH
            ||motif==GhostHauntingChoreography.Motif.BLADE_SWEEP;
        float progress=age/(float)duration;
        // Process at PSX half-resolution. Output remains full-resolution RGB;
        // each 2x2 block shares the organic mask but reads its own scene pixel.
        for(int y=0;y<height;y+=2){
            float ny=(2f*y+1-height)/height;
            float fy=(y%tile)/(float)tile;
            int row0=(y/tile)*cellsX,row1=row0+cellsX;
            int wave=(int)(2.8*Math.sin(y*.035+tick*.105+seed*.0001));
            for(int x=0;x<width;x+=2){
                float nx=(2f*x+1-width)/width;
                float dist2=nx*nx*.90f+ny*ny;
                float edge=limit((dist2-.15f)/.92f);
                int ux=pixelIndex(x+wave,width);
                int col=ux/tile;
                float fx=(ux%tile)/(float)tile;
                float low0=scratch.cloud[row0+col]*(1-fx)+scratch.cloud[row0+col+1]*fx;
                float low1=scratch.cloud[row1+col]*(1-fx)+scratch.cloud[row1+col+1]*fx;
                float cloud=(low0*(1-fy)+low1*fy)/255f;
                float drift=(float)Math.sin(x*.024+y*.041+tick*.12+cloud*5.2);
                float wisps=limit(.44f+drift*.28f+(cloud-.5f)*.65f);
                float form=edge*(.46f+.54f*wisps);
                int grain=hash(x>>1,y>>1,seed,tick);
                float accent=0f;
                if(motif!=null){
                    switch(motif){
                        case LAMP_STROBE -> accent=(age%14<3?.22f:0f)*edge;
                        case RADIO_GLITCH -> accent=((grain&63)<7?.58f:0f)*(edge*.65f+.15f);
                        case PERSPECTIVE_BEND -> accent=wisps*.25f*edge;
                        case DOORFRAME_WIPE -> accent=limit(1f-Math.abs(nx+.79f+drift*.045f)*4f)
                            *(.35f+.65f*cloud);
                        case SHROUD_SWIRL -> accent=wisps*.48f*edge;
                        case FLICKER_STEP -> {
                            float px=nx-.76f+(float)Math.sin(age*.09)*.07f;
                            accent=(age%17<8?1f:.40f)*limit(1f-(px*px*10+ny*ny*2.9f))
                                *(.4f+.6f*cloud);
                        }
                        case ICE_BLOOM -> {
                            float vein=Math.abs(nx*10.9f+ny*6.3f+cloud*2.1f);
                            accent=(vein-(int)vein<.09f?.62f:.10f)*edge;
                        }
                        case EDGE_WATCHER -> {
                            float px=Math.abs(nx)-.84f;
                            float py=ny+.05f+drift*.035f;
                            accent=limit(1f-px*px*24f-py*py*3.2f)
                                *(.18f+.56f*cloud)*edge;
                        }
                        case IMPACT_RING -> {
                            float radial=(float)Math.sqrt(dist2);
                            accent=limit(1f-Math.abs(radial-(progress*1.6f))*8f)*.75f;
                        }
                        case GRASPING_EDGES -> accent=limit(edge*(cloud*.75f
                            +.3f*Math.abs(drift)))*.64f;
                        case TWIN_ECHO -> {
                            float px=Math.abs(nx)-.72f;
                            accent=limit(1f-px*px*18f-(ny+.06f)*(ny+.06f)*3.5f)
                                *(.18f+.5f*wisps);
                        }
                        case BLADE_SWEEP -> {
                            float cut=Math.abs(nx+ny*.63f-1.32f+progress*2.20f+cloud*.14f);
                            accent=limit(1f-cut*11f)*(.22f+.5f*edge);
                        }
                        case PLAGUE_MIASMA -> accent=wisps*.62f*edge;
                        case NOTHIC_IRIS -> {
                            float radial=(float)Math.sqrt(dist2);
                            float rim=.80f-.54f*progress+(cloud-.5f)*.08f;
                            accent=limit(1f-Math.abs(radial-rim)*7.6f)*.70f;
                        }
                    }
                }else{
                    if(visual==GhostHauntingFolio.Visual.PULSE){
                        float radial=(float)Math.sqrt(dist2);
                        accent=limit(1f-Math.abs(radial-(progress*.94f+.18f))*6f)*.36f;
                    }else if(visual==GhostHauntingFolio.Visual.CHILL)
                        accent=limit(cloud-.58f)*edge*.55f;
                    else if(visual==GhostHauntingFolio.Visual.SHADOW)
                        accent=limit(1f-Math.abs(nx)*.2f-Math.abs(ny)*.85f)*edge*.15f;
                }
                float opacity=limit(form*.88f+accent*.43f)*intensity;
                int alpha=Math.min(155,(int)(opacity*175));
                int chroma=(bend && edge>.36f)?(int)(2*edge*intensity):0;
                int dx=bend?(int)(drift*(2f+cloud*4f)*edge*intensity):0;
                // A few individual dust/electrical motes; never a full-width bar.
                boolean spark=(visual==GhostHauntingFolio.Visual.STATIC
                        ||motif==GhostHauntingChoreography.Motif.RADIO_GLITCH)
                    &&(grain&255)<5&&edge>.17f;
                int mistLift=(visual==GhostHauntingFolio.Visual.VEIL
                        ||visual==GhostHauntingFolio.Visual.CHILL
                        ||motif==GhostHauntingChoreography.Motif.ICE_BLOOM)
                    ?(int)(18*intensity*wisps*edge):0;
                for(int py=y;py<y+2&&py<height;py++){
                    int offset=py*width;
                    for(int px=x;px<x+2&&px<width;px++){
                        int sample=pixelIndex(px+dx,width);
                        int colRgb=scratch.source[offset+sample];
                        int red=chroma==0?(colRgb>>>16)&255:
                            (scratch.source[offset+pixelIndex(sample+chroma,width)]>>>16)&255;
                        int green=(colRgb>>>8)&255;
                        int blue=chroma==0?colRgb&255:
                            scratch.source[offset+pixelIndex(sample-chroma,width)]&255;
                        red=(red*(255-alpha)+tint[0]*alpha+127)/255;
                        green=(green*(255-alpha)+tint[1]*alpha+127)/255;
                        blue=(blue*(255-alpha)+tint[2]*alpha+127)/255;
                        int lift=mistLift+(spark?36:0);
                        pixels[offset+px]=(clip(red+lift)<<16)
                            |(clip(green+lift)<<8)|clip(blue+lift);
                    }
                }
            }
        }
    }
}
