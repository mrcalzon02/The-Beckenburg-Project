package org.beckenburg;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Canonical visual IDs and physical billboard extents.
 *
 * A render appearance is distinct from a ghost's hidden gameplay identity. The
 * host broadcasts a ghost ID only on an already authorized visible apparition.
 * Hunter selections are cosmetic until their respective gameplay classes exist.
 */
public final class CharacterVisuals {
    private CharacterVisuals() { }

    public enum Hunter { CLERIC, OBSERVER, DEMONOLOGIST }

    public record Shape(double height,double width) {
        public Shape {
            if(height<=0||width<=0||!Double.isFinite(height)||!Double.isFinite(width))
                throw new IllegalArgumentException("invalid visual dimensions");
        }
    }

    public static final Set<String> HUNTERS=Set.of("cleric","observer","demonologist");
    public static final Set<String> ELDERS=Set.of("elder_blade_demon","elder_plague_demon","elder_nothic");
    private static final Map<String,Shape> SHAPES=Map.of(
        "cleric",new Shape(1.82,1.06),
        "observer",new Shape(1.75,1.04),
        "demonologist",new Shape(1.80,1.08),
        "elder_blade_demon",new Shape(2.72,2.05),
        "elder_plague_demon",new Shape(2.86,2.17),
        "elder_nothic",new Shape(3.15,2.34)
    );

    public static String hunter(String raw) {
        if(raw==null)throw new IllegalArgumentException("missing hunter art identity");
        String id=raw.toLowerCase(Locale.ROOT);
        if(!HUNTERS.contains(id))throw new IllegalArgumentException("invalid hunter art identity: "+raw);
        return id;
    }
    public static String ghost(GhostIdentity.Type type) {
        if(type==null)throw new IllegalArgumentException("missing ghost type");
        return type.name().toLowerCase(Locale.ROOT);
    }
    public static boolean isHunter(String id) {return HUNTERS.contains(id);}
    public static boolean isElder(String id) {return ELDERS.contains(id);}
    public static Shape shape(String id) {return SHAPES.getOrDefault(id,new Shape(1.95,1.16));}
}
