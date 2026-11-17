package org.beckenburg;

import java.util.Objects;

/**
 * Actual host-authorized additional paranormal phenomena. A reading requires an
 * existing, recent ghost action in the sensor's physical room and the matching
 * species + contract producer. Polling, weather or a client cannot create proof.
 * Baselines remain measurable on every difficulty.
 */
public final class AdditionalEvidence {
    private AdditionalEvidence(){}
    public record Sample(String reading,boolean detected,EquipmentRegistry.Evidence evidence){
        public Sample {
            Objects.requireNonNull(reading);
            if(reading.isBlank()||reading.length()>160||reading.indexOf('|')>=0)
                throw new IllegalArgumentException("invalid field sample");
        }
    }
    public static EquipmentRegistry.Evidence phenomenon(String id){
        return switch(Objects.requireNonNull(id)){
            case "ECTOPLASM_SAMPLER" ->EquipmentRegistry.Evidence.ECTOPLASM;
            case "RESONANCE_TUNER","SPECTRAL_FREQUENCY_ANALYZER" ->EquipmentRegistry.Evidence.RESONANCE;
            case "BAROMETRIC_PROBE" ->EquipmentRegistry.Evidence.ATMOSPHERIC_REVERSAL;
            case "SEISMIC_RECORDER" ->EquipmentRegistry.Evidence.SEISMIC_IMPACT;
            case "SPECTRAL_PRISM" ->EquipmentRegistry.Evidence.SPECTRAL_SHADOW;
            case "LASER_GRID" ->EquipmentRegistry.Evidence.STRUCTURED_LIGHT;
            case "OBJECT_IMPRINT_SCANNER" ->EquipmentRegistry.Evidence.OBJECT_IMPRINT;
            default ->null;
        };
    }
    public static HardEvidenceQuota.Family family(EquipmentRegistry.Evidence kind){
        return switch(Objects.requireNonNull(kind)){
            case ECTOPLASM ->HardEvidenceQuota.Family.ECTOPLASM;
            case RESONANCE ->HardEvidenceQuota.Family.RESONANCE;
            case SEISMIC_IMPACT ->HardEvidenceQuota.Family.SEISMIC;
            case ATMOSPHERIC_REVERSAL ->HardEvidenceQuota.Family.ATMOSPHERIC;
            case SPECTRAL_SHADOW ->HardEvidenceQuota.Family.SHADOW;
            case OBJECT_IMPRINT ->HardEvidenceQuota.Family.IMPRINT;
            case STRUCTURED_LIGHT ->HardEvidenceQuota.Family.LASER;
            default ->throw new IllegalArgumentException("not new hard evidence");
        };
    }
    private static boolean event(EquipmentRegistry.Evidence kind,GhostDomain.Action a){
        return switch(kind){
            case ECTOPLASM ->a==GhostDomain.Action.MANIFEST||a==GhostDomain.Action.DISTURB
                ||a==GhostDomain.Action.SIGNATURE;
            case RESONANCE ->a==GhostDomain.Action.WHISPER||a==GhostDomain.Action.MANIFEST
                ||a==GhostDomain.Action.SIGNATURE;
            case SEISMIC_IMPACT,OBJECT_IMPRINT ->a==GhostDomain.Action.DISTURB
                ||a==GhostDomain.Action.STRIKE||a==GhostDomain.Action.SIGNATURE;
            case ATMOSPHERIC_REVERSAL ->a==GhostDomain.Action.MANIFEST
                ||a==GhostDomain.Action.STRIKE||a==GhostDomain.Action.SIGNATURE;
            case SPECTRAL_SHADOW ->a==GhostDomain.Action.MANIFEST
                ||a==GhostDomain.Action.SIGNATURE;
            case STRUCTURED_LIGHT ->a==GhostDomain.Action.MANIFEST
                ||a==GhostDomain.Action.SIGNATURE;
            default ->false;
        };
    }
    static int lifetime(EquipmentRegistry.Evidence kind){
        return switch(kind){
            case ECTOPLASM->1800;
            case OBJECT_IMPRINT->2400;
            default->65; // Live acoustic, optical and other sensor events, in 100 ms ticks.
        };
    }

    /** Collectable residue belongs to the physical source of an accepted event, including the other Twin. */
    static final class Deposits {
        private record Deposit(int tick,LanGhostSpatial.Pose origin,GhostDomain.Action action,int power){}
        private final java.util.ArrayDeque<Deposit> active=new java.util.ArrayDeque<>();

        private void expire(int tick){
            while(!active.isEmpty()&&tick-active.getFirst().tick()>lifetime(EquipmentRegistry.Evidence.OBJECT_IMPRINT))
                active.removeFirst();
        }
        void record(int tick,GhostDomain.Resolution result,LanGhostSpatial.Pose origin,int power){
            expire(tick);
            if(!result.applied()||result.room()!=origin.room()||power<=0)return;
            if(event(EquipmentRegistry.Evidence.ECTOPLASM,result.action())
                    ||event(EquipmentRegistry.Evidence.OBJECT_IMPRINT,result.action()))
                active.addLast(new Deposit(tick,origin,result.action(),power));
        }
        Sample collect(String id,GhostIdentity.Type species,HardEvidenceQuota quota,int tick,
                LanWorld.Body collector,EquipmentTiers.Stage stage){
            var kind=phenomenon(id);
            if(kind!=EquipmentRegistry.Evidence.ECTOPLASM&&kind!=EquipmentRegistry.Evidence.OBJECT_IMPRINT)
                throw new IllegalArgumentException("surface sampler required");
            expire(tick);
            for(var it=active.descendingIterator();it.hasNext();){
                var deposit=it.next();
                if(tick-deposit.tick()>lifetime(kind))break;
                var origin=deposit.origin();
                if(origin.room()!=collector.room()||!event(kind,deposit.action()))continue;
                double distance=Math.hypot(Math.hypot(collector.x()-origin.x(),collector.z()-origin.z()),collector.y()-origin.y());
                if(distance>stage.reachCm()/100.0)continue;
                return measure(id,species,quota,tick,deposit.tick(),deposit.power(),deposit.action(),true,distance,stage);
            }
            return measure(id,species,quota,tick,tick,0,GhostDomain.Action.DRIFT,false,0,stage);
        }
    }

    /**
     * The host checks source range and the instrument's sampling conditions.
     * For the prism this requires a visible figure in the lens, which can look through a doorway.
     */
    public static Sample measure(String id,GhostIdentity.Type species,
            HardEvidenceQuota quota, int tick,int lastEventTick,int eventStrength,
            GhostDomain.Action lastAction,boolean sourceReachable,double sourceDistance,
            EquipmentTiers.Stage stage){
        EquipmentRegistry.Evidence kind=phenomenon(id);
        if(kind==null)throw new IllegalArgumentException("not a paranormal detector: "+id);
        Objects.requireNonNull(species);Objects.requireNonNull(quota);
        Objects.requireNonNull(lastAction);Objects.requireNonNull(stage);
        if(tick<0||sourceDistance<0||!Double.isFinite(sourceDistance)
                ||eventStrength<0||eventStrength>10)
            throw new IllegalArgumentException("invalid source measurement");
        int eventAge=tick-lastEventTick;
        // A sensor can have battery, range and time to respond without the
        // ghost ever authoring a positive signature.
        boolean physical=sourceReachable&&eventAge>=0&&eventAge<=lifetime(kind)
            &&sourceDistance<=stage.reachCm()/100.0&&eventStrength>0
            &&event(kind,lastAction);
        boolean speciesSupports=GhostEvidenceMatrix.forGhost(species).future().contains(kind);
        boolean allowed=quota.permits(family(kind));
        boolean detected=physical&&speciesSupports&&allowed;
        if(!detected)return new Sample(switch(kind){
            case ECTOPLASM->"Swab clean / no visible residue";
            case RESONANCE->"Spectrum noise only / no stable harmonic";
            case SEISMIC_IMPACT->"No distinct floor vibration";
            case ATMOSPHERIC_REVERSAL->"Pressure stable / ambient fluctuation";
            case SPECTRAL_SHADOW->"Prism clear / no silhouette";
            case OBJECT_IMPRINT->"Contact plate clear / no imprint";
            case STRUCTURED_LIGHT->"Laser beams aligned / no displacement";
            default->"No source signal";
        },false,kind);
        int n=1+Math.floorMod(tick*17+species.ordinal()*11+eventStrength*7,9);
        String report=switch(kind){
            case ECTOPLASM->"Swab residue "+(2+n*3)+" mg / glassy film";
            case RESONANCE->"Peak harmonic "+(71+n*19)+" Hz / "+
                (23+n*3)+" dB";
            case SEISMIC_IMPACT->"Floor impulse "+(3+n*2)+" mm/s / transient vibration";
            case ATMOSPHERIC_REVERSAL->"Differential pressure -"+(3+n)+" hPa / localized pulse";
            case SPECTRAL_SHADOW->"Silhouette area "+(9+n*4)+
                " percent frame";
            case OBJECT_IMPRINT->"Surface contact pattern "+n+"/10 / imprint visible";
            case STRUCTURED_LIGHT->"Grid occlusion "+(2+n)+" beams / offset observed";
            default->throw new AssertionError(kind);
        };
        return new Sample(report,true,kind);
    }
}
