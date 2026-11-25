package org.beckenburg;

/** Packaged scene of Gallows Pass Railway. */
final class GallowsNativeLevel {
    private GallowsNativeLevel() {}

    private static final class Packaged {
        private static final SurveyedNativeLevel.Source SOURCE = SurveyedNativeLevel.load("wld-12", "railway", "gallows", "industrial");
    }

    static LevelDefinition definition() {
        return Packaged.SOURCE.level();
    }

    static NativeInvestigationScene sceneFor(LevelDefinition level) {
        return Packaged.SOURCE.sceneFor(level);
    }
}
