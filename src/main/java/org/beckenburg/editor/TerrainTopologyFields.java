package org.beckenburg.editor;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Height-derived topology fields for reusable terrain authoring. These are
 * deterministic estimates of slope, curvature, D8 drainage and moisture.
 *
 * Flow speed is a gravity-based proxy, NOT hydraulic simulation particle
 * velocity or measured weather wetness. A renderer/physics adapter can consume
 * these fields explicitly. Their source fingerprint forbids stale readback.
 */
public final class TerrainTopologyFields {
    private static final int MAGIC=0x544F5046, VERSION=1;
    private static final double GRAVITY=9.81;
    private final int cells,side,pixels;
    private final byte[] fingerprint;
    private final float[] slope,curvature,flowX,flowZ,accumulation,moisture;
    private TerrainTopologyFields(int cells,byte[] fingerprint,
            float[] slope,float[] curvature,float[] flowX,float[] flowZ,
            float[] accumulation,float[] moisture){
        this.cells=cells;side=cells+1;pixels=side*side;
        this.fingerprint=fingerprint.clone();
        this.slope=slope;this.curvature=curvature;this.flowX=flowX;
        this.flowZ=flowZ;this.accumulation=accumulation;this.moisture=moisture;
    }
    private int index(int x,int z){
        if(x<0||x>cells||z<0||z>cells)
            throw new IllegalArgumentException("Terrain vertex outside tile");
        return z*side+x;
    }
    public int cells(){return cells;}
    public double slopeDegrees(int x,int z){return slope[index(x,z)];}
    public double curvature(int x,int z){return curvature[index(x,z)];}
    public double flowX(int x,int z){return flowX[index(x,z)];}
    public double flowZ(int x,int z){return flowZ[index(x,z)];}
    public double accumulation(int x,int z){return accumulation[index(x,z)];}
    public double moisture(int x,int z){return moisture[index(x,z)];}
    public String fingerprint(){return HexFormat.of().formatHex(fingerprint);}
    public static Path path(TerrainLayerStack stack){
        Objects.requireNonNull(stack);
        return stack.worldForWorkbench().directory().resolve("terrain-topology")
            .resolve("tile_"+stack.tileX()+"_"+stack.tileZ()+".bin");
    }
    private static byte[] fingerprint(TerrainLayerStack stack){
        try{
            var hash=MessageDigest.getInstance("SHA-256");
            int n=stack.cells();
            for(int z=0;z<=n;z++)for(int x=0;x<=n;x++){
                long bits=Double.doubleToLongBits(stack.height(x,z));
                for(int shift=56;shift>=0;shift-=8)
                    hash.update((byte)(bits>>>shift));
            }
            return hash.digest();
        }catch(NoSuchAlgorithmException unavailable){
            throw new IllegalStateException("SHA-256 unavailable",unavailable);
        }
    }
    private static float[] readArray(DataInputStream in,int count)throws IOException {
        float[] values=new float[count];
        for(int i=0;i<count;i++){
            float value=in.readFloat();
            if(!Float.isFinite(value)||Math.abs(value)>1e9f)
                throw new IOException("Invalid topology sample");
            values[i]=value;
        }
        return values;
    }
    private static void writeArray(DataOutputStream out,float[] values)throws IOException {
        for(float value:values)out.writeFloat(value);
    }
    public static TerrainTopologyFields calculate(TerrainLayerStack stack){
        Objects.requireNonNull(stack);
        int n=stack.cells(),side=n+1,count=side*side;
        double sampleSpacing=stack.metresPerCell();
        if(sampleSpacing<=0)throw new IllegalArgumentException("Invalid sample spacing");
        double[] heights=new double[count];
        for(int z=0;z<=n;z++)for(int x=0;x<=n;x++)
            heights[z*side+x]=stack.height(x,z);
        float[] slopes=new float[count],curves=new float[count],
            flowX=new float[count],flowZ=new float[count],
            acc=new float[count],moisture=new float[count];
        int[] destination=new int[count],inDegree=new int[count];
        Arrays.fill(destination,-1);
        Arrays.fill(acc,1f);
        final int[] offsetX={-1,0,1,-1,1,-1,0,1};
        final int[] offsetZ={-1,-1,-1,0,0,1,1,1};
        for(int z=0;z<=n;z++)for(int x=0;x<=n;x++){
            int i=z*side+x;
            int west=Math.max(0,x-1),east=Math.min(n,x+1);
            int north=Math.max(0,z-1),south=Math.min(n,z+1);
            double center=heights[i];
            double dhdx=(heights[z*side+east]-heights[z*side+west])/
                ((east-west)*sampleSpacing);
            double dhdz=(heights[south*side+x]-heights[north*side+x])/
                ((south-north)*sampleSpacing);
            slopes[i]=(float)Math.toDegrees(Math.atan(Math.hypot(dhdx,dhdz)));
            curves[i]=(float)((heights[z*side+west]+heights[z*side+east]+
                heights[north*side+x]+heights[south*side+x]-4*center)/
                (sampleSpacing*sampleSpacing));
            double best=0;int receiver=-1;int chosenX=0,chosenZ=0;
            for(int k=0;k<8;k++){
                int xx=x+offsetX[k],zz=z+offsetZ[k];
                if(xx<0||xx>n||zz<0||zz>n)continue;
                int neighbor=zz*side+xx;
                double drop=center-heights[neighbor];
                double horizontal=sampleSpacing*
                    ((offsetX[k]!=0&&offsetZ[k]!=0)?Math.sqrt(2):1);
                double gradient=drop/horizontal;
                if(gradient>best){
                    best=gradient;receiver=neighbor;
                    chosenX=offsetX[k];chosenZ=offsetZ[k];
                }
            }
            if(receiver>=0){
                destination[i]=receiver;inDegree[receiver]++;
                double speed=Math.sqrt(2*GRAVITY*best*sampleSpacing);
                double length=Math.hypot(chosenX,chosenZ);
                flowX[i]=(float)(chosenX/length*speed);
                flowZ[i]=(float)(chosenZ/length*speed);
            }
        }
        // Strictly descending height guarantees an acyclic D8 graph.
        int[] queue=new int[count];int head=0,tail=0;
        for(int i=0;i<count;i++)if(inDegree[i]==0)queue[tail++]=i;
        while(head<tail){
            int i=queue[head++];
            int next=destination[i];
            if(next>=0){
                acc[next]+=acc[i];
                if(--inDegree[next]==0)queue[tail++]=next;
            }
        }
        if(tail!=count)throw new IllegalStateException("Drainage cycle in strict downhill topology");
        double normalization=Math.log1p(count);
        for(int i=0;i<count;i++)
            moisture[i]=(float)Math.min(1,Math.log1p(acc[i])/normalization);
        return new TerrainTopologyFields(n,fingerprint(stack),slopes,curves,
            flowX,flowZ,acc,moisture);
    }
    /** Write a derived cache only when its input terrain still matches. */
    public void save(TerrainLayerStack stack)throws IOException {
        if(cells!=stack.cells()||!Arrays.equals(fingerprint,fingerprint(stack)))
            throw new IOException("Terrain was edited after topology calculation; rebuild");
        Path target=path(stack);
        Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),".topology-",".tmp");
        try {
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))){
                out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(cells);
                out.writeInt(fingerprint.length);out.write(fingerprint);
                writeArray(out,slope);writeArray(out,curvature);
                writeArray(out,flowX);writeArray(out,flowZ);
                writeArray(out,accumulation);writeArray(out,moisture);
            }
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        }finally{Files.deleteIfExists(temp);}
    }
    public static TerrainTopologyFields open(TerrainLayerStack stack)throws IOException{
        Path source=path(stack);
        if(!Files.isRegularFile(source))throw new IOException("No derived terrain topology cache");
        int n=stack.cells(),count=(n+1)*(n+1);
        if(Files.size(source)>count*6L*4+4096)
            throw new IOException("Oversized derived topology data");
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(source)))){
            if(in.readInt()!=MAGIC||in.readInt()!=VERSION||in.readInt()!=n)
                throw new IOException("Unsupported topology field format or resolution");
            int size=in.readInt();if(size!=32)throw new IOException("Invalid fingerprint size");
            byte[] hash=new byte[size];in.readFully(hash);
            if(!Arrays.equals(hash,fingerprint(stack)))
                throw new IOException("Stale topology: terrain heights changed, rebuild required");
            var result=new TerrainTopologyFields(n,hash,
                readArray(in,count),readArray(in,count),readArray(in,count),
                readArray(in,count),readArray(in,count),readArray(in,count));
            if(in.read()!=-1)throw new IOException("Unexpected trailing topology data");
            return result;
        }
    }
}
