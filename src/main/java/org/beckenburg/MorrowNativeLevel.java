package org.beckenburg;

/** Packaged scene of Morrow Point Lightstation. */
final class MorrowNativeLevel {
    private MorrowNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-14", "lightstation", "morrow", "coast");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
