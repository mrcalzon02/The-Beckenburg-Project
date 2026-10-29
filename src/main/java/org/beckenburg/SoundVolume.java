package org.beckenburg;

/** Visible authoring volume for ordinary ambience, never a ghost event source. */
public record SoundVolume(String id,Kind kind,Shape shape,Source source,
        double x,double y,double z,double width,double height,double depth,
        double falloff,double gain,double pitch,double fade,double duration,
        boolean enabled,boolean loop,boolean requiresPower) {
    public enum Kind { AMBIENT, ENVIRONMENTAL }
    public enum Shape { BOX, SPHERE }
    public enum Source { WIND, RUNNING_WATER, INSECTS, FIRE, ELECTRIC_HUM, DRIPS, TV_STATIC, WASHER, WOOD_CREAK, STONE_SETTLE }
    public SoundVolume {
        if(id==null||!id.matches("[A-Za-z0-9_-]{1,80}")||kind==null||shape==null||source==null)
            throw new IllegalArgumentException("Sound volume identity");
        for(double n:new double[]{x,y,z,width,height,depth,falloff,gain,pitch,fade,duration})
            if(!Double.isFinite(n))throw new IllegalArgumentException("Non-finite sound volume");
        if(Math.abs(x)>10000||Math.abs(y)>10000||Math.abs(z)>10000||width<.1||width>2000||height<.1||height>2000||depth<.1||depth>2000||falloff<.01||falloff>1000||gain<0||gain>1||pitch<.25||pitch>4||fade<.02||fade>10||duration<.1||duration>60)
            throw new IllegalArgumentException("Sound volume limits");
        if(shape==Shape.SPHERE){height=width;depth=width;}
    }
    public static SoundVolume create(String id,double x,double y,double z){return new SoundVolume(id,Kind.AMBIENT,Shape.BOX,Source.WIND,x,y,z,8,4,8,4,.18,1,.5,8,true,true,false);}
    public SoundVolume move(double x,double y,double z){return new SoundVolume(id,kind,shape,source,x,y,z,width,height,depth,falloff,gain,pitch,fade,duration,enabled,loop,requiresPower);}
    /** Full gain inside the volume; smooth fade to zero beyond its outer boundary. */
    public double attenuation(double px,double py,double pz,boolean power){
        if(!enabled||requiresPower&&!power)return 0;
        double dx=Math.abs(px-x),dy=Math.abs(py-y),dz=Math.abs(pz-z),distance;
        if(shape==Shape.SPHERE)distance=Math.max(0,Math.sqrt(dx*dx+dy*dy+dz*dz)-width/2);
        else{dx=Math.max(0,dx-width/2);dy=Math.max(0,dy-height/2);dz=Math.max(0,dz-depth/2);distance=Math.sqrt(dx*dx+dy*dy+dz*dz);}
        double t=Math.max(0,Math.min(1,distance/falloff));return gain*(1-t*t*(3-2*t));
    }
    public double pan(double px,double pz,double yaw){if(kind==Kind.AMBIENT)return 0;double dx=x-px,dz=z-pz,d=Math.hypot(dx,dz);return d<.01?0:Math.max(-1,Math.min(1,(-dx*Math.cos(yaw)+dz*Math.sin(yaw))/d));}
    private static double noise(long frame){long h=frame*0x9e3779b97f4a7c15L;h^=h>>>29;h*=0xbf58476d1ce4e5b9L;return ((h>>>40)&0xffffff)/8388607.5-1;}
    /** Original procedural sources and existing original fixture Foley; no new licensed media. */
    public short sample(long frame){double t=frame/16000d,v;switch(source){
        case RUNNING_WATER:return FixtureSoundscape.sample(FixtureSoundscape.Sound.RUNNING_WATER,frame);
        case TV_STATIC:return FixtureSoundscape.sample(FixtureSoundscape.Sound.TV_STATIC,frame);
        case WASHER:return FixtureSoundscape.sample(FixtureSoundscape.Sound.WASHER,frame);
        case ELECTRIC_HUM:v=(Math.sin(t*Math.PI*120)+Math.sin(t*Math.PI*240)*.2)*.12;break;
        case WIND:v=(noise(frame/17)*.13+noise(frame/67)*.10)*(0.65+.35*Math.sin(t*.7));break;
        case INSECTS:v=Math.sin(t*Math.PI*6200)*Math.pow(Math.max(0,Math.sin(t*17)),10)*.12;break;
        case DRIPS:double p=t%1.31;v=Math.sin(p*Math.PI*(1300-400*p))*Math.exp(-p*38)*.22;break;
        case WOOD_CREAK:
            double phase=(t+Math.floorMod(id.hashCode(),17))%19.0;
            double swell=phase<2.0?Math.sin(Math.PI*phase/2.0):0;
            v=swell*(Math.sin(2*Math.PI*(82+phase*17)*t)*.15+noise(frame/31)*.04);
            break;
        case STONE_SETTLE:
            double beat=(t+Math.floorMod(id.hashCode(),23))%31.0;
            double transient=beat<.9?Math.exp(-beat*7):0;
            v=transient*(Math.sin(2*Math.PI*(56-beat*12)*t)*.21+noise(frame/17)*.11);
            break;
        default:v=noise(frame/3)*.08+noise(frame/41)*.12*Math.pow(Math.max(0,Math.sin(t*21)),4);
    }return(short)Math.round(v*32767);}
}
