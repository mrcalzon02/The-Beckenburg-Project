package org.beckenburg;

/** Packaged scene of Eight Pines Motor Lodge. */
final class PinesNativeLevel {
    private PinesNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-16", "motor-lodge", "pines", "domestic");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
