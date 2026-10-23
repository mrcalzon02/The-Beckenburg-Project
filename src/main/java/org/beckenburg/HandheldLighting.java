package org.beckenburg;
/** Shared physical optics; battery runtime remains host-owned FieldEquipment state. */
public final class HandheldLighting {
    private HandheldLighting(){}
    public record Beam(double reach,double intensity,double width){}
    public static final Beam DEFAULT=new Beam(7.5,.72,.44);
    private static final Beam[] FLASH=java.util.Arrays.stream(EquipmentTiers.Tier.values()).map(HandheldLighting::buildFlash).toArray(Beam[]::new);
    private static final Beam[] UV=java.util.Arrays.stream(EquipmentTiers.Tier.values()).map(HandheldLighting::buildUv).toArray(Beam[]::new);
    public static Beam flashlight(EquipmentTiers.Tier tier){return FLASH[java.util.Objects.requireNonNull(tier).ordinal()];}
    public static Beam uv(EquipmentTiers.Tier tier){return UV[java.util.Objects.requireNonNull(tier).ordinal()];}
    private static Beam buildFlash(EquipmentTiers.Tier tier){
        return new Beam(EquipmentTiers.stage("FLASHLIGHT",tier).reachCm()/100.0,
            new double[]{.98,1.18,1.40}[tier.ordinal()],new double[]{.46,.53,.62}[tier.ordinal()]);
    }
    private static Beam buildUv(EquipmentTiers.Tier tier){return new Beam(EquipmentTiers.stage("UV_LIGHT",tier).reachCm()/100.0,
        new double[]{1.5,1.8,2.1}[tier.ordinal()],new double[]{.35,.43,.52}[tier.ordinal()]);}
    public static double sample(Beam b,double depth,double aim){
        if(!Double.isFinite(depth+aim)||depth<=0||aim<0)return 0;
        return b.intensity()*Math.max(0,1-depth/b.reach())*Math.max(0,1-aim/b.width());
    }
}
