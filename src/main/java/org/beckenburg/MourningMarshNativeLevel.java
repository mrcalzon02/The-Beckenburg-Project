package org.beckenburg;

/** Packaged native review of Mourning Marsh. */
final class MourningMarshNativeLevel {
    static final String REVIEW_PROPERTY = "beckenburg.review.mourningMarshNative";

    private MourningMarshNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-11", "marsh-review", "marsh", "wetland");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
