package org.beckenburg;

/** Packaged native review of Red Mesa. */
final class RedMesaNativeLevel {
    static final String REVIEW_PROPERTY = "beckenburg.review.redMesaNative";

    private RedMesaNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-03", "red-mesa-review", "red-mesa", "canyon");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
