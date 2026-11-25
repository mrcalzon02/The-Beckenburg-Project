package org.beckenburg;

/** Packaged scene of MV Saint Elara. */
final class ElaraNativeLevel {
    private ElaraNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-15", "vessel", "elara", "coast");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
