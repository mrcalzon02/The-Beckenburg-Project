package org.beckenburg;

/** Packaged native review of Greywater Cannery. */
final class GreywaterNativeLevel {
    private GreywaterNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-13", "greywater-review", "greywater", "industrial");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
