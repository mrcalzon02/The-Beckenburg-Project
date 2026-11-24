package org.beckenburg;

import org.beckenburg.LevelDefinition.Dock;

/** Ordinary archive authority for the Monastery review site. */
final class MonasteryNativeLevel {
    static final String TITLE = "Monastery of Ashen Bells";
    static final String REVIEW_PROPERTY = "beckenburg.review.monasteryNative";

    private MonasteryNativeLevel() {}

    private static final class Loaded {
        private static final PackagedInvestigationLevel.Source SOURCE = load();
    }

    static PackagedInvestigationLevel.Source packaged() {
        return Loaded.SOURCE;
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        var source = packaged();
        if (!source.level().equals(level)) {
            throw new IllegalArgumentException("Monastery requires the exact authored revision");
        }
        return source.scene();
    }

    private static PackagedInvestigationLevel.Source load() {
        var source = PackagedInvestigationLevel.load(new PackagedInvestigationLevel.Profile(
            "wld-10", TITLE, "monastery-with-vertical-distribution", new Dock(32, 0, 80, 0), "monastery-",
            "stone", "stone", null, null));
        var level = source.level();
        if (source.scene().doors().size() != 13 || HidingBudget.candidates(level).size() != 3
            || level.socket("generator", 11) == null || level.socket("breaker", 11) == null
            || level.sockets().stream().filter(socket -> socket.type().equals("switch")).count() != 12
            || level.sockets().stream().filter(socket -> socket.type().equals("light")).count() != 36) {
            throw new IllegalStateException("Incomplete Monastery review source");
        }
        return source;
    }
}
