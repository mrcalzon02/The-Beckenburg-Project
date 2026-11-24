package org.beckenburg;

import org.beckenburg.LevelDefinition.Dock;

/** Ordinary archive authority for the Blackwater review site. */
final class BlackwaterNativeLevel {
    static final String TITLE = "Blackwater Hydroelectric Dam";
    static final String REVIEW_PROPERTY = "beckenburg.review.blackwaterNative";

    private BlackwaterNativeLevel() {}

    private static final class Loaded {
        private static final PackagedInvestigationLevel.Source SOURCE = load();
    }

    static PackagedInvestigationLevel.Source packaged() {
        return Loaded.SOURCE;
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        var source = packaged();
        if (!source.level().equals(level)) {
            throw new IllegalArgumentException("Blackwater requires the exact authored revision");
        }
        return source.scene();
    }

    private static PackagedInvestigationLevel.Source load() {
        var source = PackagedInvestigationLevel.load(new PackagedInvestigationLevel.Profile(
            "wld-09", TITLE, "dam-with-ventilation", new Dock(36, 0, 82, 0), "dam-",
            "metal", "concrete", "dam-closed-channel", "blackwater.water"));
        var level = source.level();
        if (source.scene().doors().size() != 14 || HidingBudget.candidates(level).size() != 3
            || level.socket("generator", 10) == null || level.socket("breaker", 10) == null
            || level.water().placements().size() != 2) {
            throw new IllegalStateException("Incomplete Blackwater review source");
        }
        return source;
    }
}
