package org.beckenburg;

import java.util.Objects;

/** Contract weather, chosen and evolved only by the host's authoritative 100ms tick. */
public final class MissionWeather {
    public enum Mode { FIXED, DYNAMIC, RANDOM }
    public record State(EvidenceSignals.Season season,EvidenceSignals.Weather weather,
            Mode mode,int intensity,int revision) {
        public State {
            Objects.requireNonNull(season);Objects.requireNonNull(weather);Objects.requireNonNull(mode);
            if(intensity<1||intensity>3||revision<0)throw new IllegalArgumentException("weather state");
        }
        public int noiseMask(){
            int base=switch(weather){
                case CLEAR->0;
                case OVERCAST,DRIZZLE,HEATWAVE->1;
                case FOG,SNOW->2;
                case RAIN,SLEET,DENSE_FOG,WIND->3;
                case HEAVY_RAIN,HAIL,DUST_STORM,ASHFALL,MIASMA->4;
                case THUNDERSTORM,BLIZZARD->5;
            };
            return base*intensity;
        }
        /** Extra mission challenge rating, separate from the mandatory party-size modifier. */
        public int challenge(){return Math.min(3,(noiseMask()+3)/6);}
        public String packet(){return "WEATHER|"+season+"|"+weather+"|"+mode+"|"+intensity+"|"+revision;}
        public String caption(){return displayName(weather)+" / INTENSITY "+intensity+"/3";}
        public dev.mrcalzon.weather.WeatherType moduleType(){return weather.moduleType();}
        public static State parse(String packet){
            String[] p=packet.split("\\|",-1);
            if(p.length!=6||!p[0].equals("WEATHER"))
                throw new IllegalArgumentException("weather frame");
            try{
                return new State(EvidenceSignals.Season.valueOf(p[1]),EvidenceSignals.Weather.valueOf(p[2]),
                    Mode.valueOf(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]));
            }catch(NumberFormatException ex){throw new IllegalArgumentException("weather numbers",ex);}
        }
    }
    private final long seed;
    private final EvidenceSignals.Season season;
    private final Mode mode;
    private final int intensity;
    private EvidenceSignals.Weather weather;
    private int revision;
    private int nextChange;

    public MissionWeather(EvidenceSignals.Season season,Mode mode,
            EvidenceSignals.Weather selected,int intensity,long seed){
        this.season=Objects.requireNonNull(season);
        this.mode=Objects.requireNonNull(mode);
        this.intensity=intensity;
        if(intensity<1||intensity>3)throw new IllegalArgumentException("weather intensity 1..3");
        this.seed=seed;
        EvidenceSignals.Weather requested=Objects.requireNonNull(selected);
        this.weather=mode==Mode.RANDOM?pick(seed,season):normalizeLegacy(requested);
        this.nextChange=750+Math.floorMod(mix(seed),451); // 75-120 s between fronts
    }
    public State state(){return new State(season,weather,mode,intensity,revision);}
    public int nextChangeTick(){return nextChange;}
    public static EvidenceSignals.Weather[] selectableWeather(){
        return EvidenceSignals.Weather.selectableValues();
    }
    public static String displayName(EvidenceSignals.Weather weather){
        String raw=Objects.requireNonNull(weather).name().toLowerCase(java.util.Locale.ROOT).replace('_',' ');
        StringBuilder out=new StringBuilder(raw.length());
        boolean cap=true;
        for(char ch:raw.toCharArray()){
            out.append(cap?Character.toUpperCase(ch):ch);
            cap=ch==' ';
        }
        return out.toString();
    }
    public boolean advance(int tick){
        if(tick<0)throw new IllegalArgumentException("negative weather tick");
        if(mode!=Mode.DYNAMIC||tick<nextChange)return false;
        EvidenceSignals.Weather prior=weather;
        weather=transition(weather,season,seed+revision*947L);
        revision++;
        nextChange=tick+750+Math.floorMod(mix(seed+revision*1019L),451);
        return weather!=prior;
    }
    private static EvidenceSignals.Weather normalizeLegacy(EvidenceSignals.Weather weather){
        return switch(weather){
            case WIND->EvidenceSignals.Weather.OVERCAST;
            case HEATWAVE->EvidenceSignals.Weather.CLEAR;
            default->weather;
        };
    }
    private static int mix(long seed){
        long x=seed^0x9e3779b97f4a7c15L;
        x^=x>>>30;x*=0xbf58476d1ce4e5b9L;
        x^=x>>>27;x*=0x94d049bb133111ebL;
        return (int)(x^(x>>>31));
    }
    private static EvidenceSignals.Weather pick(long seed,EvidenceSignals.Season season){
        EvidenceSignals.Weather[] available=switch(season){
            case WINTER->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.FOG,EvidenceSignals.Weather.DENSE_FOG,
                EvidenceSignals.Weather.SNOW,EvidenceSignals.Weather.SNOW,
                EvidenceSignals.Weather.SLEET,EvidenceSignals.Weather.HAIL,
                EvidenceSignals.Weather.BLIZZARD};
            case SPRING->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.DRIZZLE,EvidenceSignals.Weather.RAIN,
                EvidenceSignals.Weather.HEAVY_RAIN,EvidenceSignals.Weather.THUNDERSTORM,
                EvidenceSignals.Weather.FOG,EvidenceSignals.Weather.DENSE_FOG,
                EvidenceSignals.Weather.SLEET,EvidenceSignals.Weather.HAIL};
            case SUMMER->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.CLEAR,
                EvidenceSignals.Weather.OVERCAST,EvidenceSignals.Weather.DRIZZLE,
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.HEAVY_RAIN,
                EvidenceSignals.Weather.THUNDERSTORM,EvidenceSignals.Weather.FOG,
                EvidenceSignals.Weather.DENSE_FOG};
            case AUTUMN->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.DRIZZLE,EvidenceSignals.Weather.RAIN,
                EvidenceSignals.Weather.HEAVY_RAIN,EvidenceSignals.Weather.THUNDERSTORM,
                EvidenceSignals.Weather.FOG,EvidenceSignals.Weather.DENSE_FOG,
                EvidenceSignals.Weather.SLEET,EvidenceSignals.Weather.HAIL,
                EvidenceSignals.Weather.SNOW};
        };
        return available[Math.floorMod(mix(seed),available.length)];
    }
    private static EvidenceSignals.Weather transition(EvidenceSignals.Weather prior,
            EvidenceSignals.Season season,long seed){
        prior=normalizeLegacy(prior);
        EvidenceSignals.Weather[] possibilities=switch(prior){
            case CLEAR->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.FOG,pick(seed,season)};
            case OVERCAST->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.DRIZZLE,EvidenceSignals.Weather.FOG,pick(seed,season)};
            case DRIZZLE->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.OVERCAST,EvidenceSignals.Weather.DRIZZLE,
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.FOG};
            case RAIN->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.DRIZZLE,EvidenceSignals.Weather.RAIN,
                EvidenceSignals.Weather.HEAVY_RAIN,EvidenceSignals.Weather.THUNDERSTORM,
                EvidenceSignals.Weather.OVERCAST};
            case HEAVY_RAIN->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.HEAVY_RAIN,
                EvidenceSignals.Weather.THUNDERSTORM,EvidenceSignals.Weather.OVERCAST};
            case THUNDERSTORM->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.THUNDERSTORM,EvidenceSignals.Weather.HEAVY_RAIN,
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.OVERCAST};
            case FOG->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.FOG,EvidenceSignals.Weather.DENSE_FOG,
                EvidenceSignals.Weather.OVERCAST,EvidenceSignals.Weather.CLEAR};
            case DENSE_FOG->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.DENSE_FOG,EvidenceSignals.Weather.FOG,
                EvidenceSignals.Weather.OVERCAST};
            case SNOW->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.SNOW,EvidenceSignals.Weather.BLIZZARD,
                EvidenceSignals.Weather.SLEET,EvidenceSignals.Weather.OVERCAST,
                EvidenceSignals.Weather.CLEAR};
            case BLIZZARD->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.BLIZZARD,EvidenceSignals.Weather.SNOW,
                EvidenceSignals.Weather.OVERCAST};
            case SLEET->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.SLEET,EvidenceSignals.Weather.SNOW,
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.OVERCAST};
            case HAIL->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.HAIL,EvidenceSignals.Weather.HEAVY_RAIN,
                EvidenceSignals.Weather.RAIN,EvidenceSignals.Weather.OVERCAST};
            // Special atmospheres are valid fixed/custom choices but do not
            // randomly invade ordinary seasonal fronts. Dynamic contracts that
            // explicitly begin in one decay back toward normal weather.
            case DUST_STORM,ASHFALL,MIASMA->new EvidenceSignals.Weather[]{
                prior,EvidenceSignals.Weather.OVERCAST,EvidenceSignals.Weather.CLEAR};
            case HEATWAVE,WIND->new EvidenceSignals.Weather[]{
                EvidenceSignals.Weather.CLEAR,EvidenceSignals.Weather.OVERCAST};
        };
        EvidenceSignals.Weather next=possibilities[Math.floorMod(mix(seed^0xB3CC3BL),possibilities.length)];
        if((season==EvidenceSignals.Season.WINTER)&&
                (next==EvidenceSignals.Weather.RAIN||next==EvidenceSignals.Weather.HEAVY_RAIN))
            return EvidenceSignals.Weather.SLEET;
        if(season==EvidenceSignals.Season.SUMMER&&
                (next==EvidenceSignals.Weather.SNOW||next==EvidenceSignals.Weather.BLIZZARD
                 ||next==EvidenceSignals.Weather.SLEET))
            return EvidenceSignals.Weather.RAIN;
        return next;
    }
}
