# Playable hunter animation

Run `play-hunters.cmd` from this checkout. It launches the standalone Windows app image under `build/playable-hunters/Beckenburg-Hunters/`, with its own bundled Java runtime and GPU game JAR. It starts the normal Castle and existing solo/LAN menus. Add `--software` to use the software renderer. This local build does not replace the older installed application or its shortcut.

The Cleric, Observer and Demonologist have two walk states and an authored crouch pose in all four existing viewing directions. Walking comes from host-accepted teammate displacement; stopping selects original idle; Ctrl uses the existing host-authoritative crouch toggle. Scene changes and departed seats discard prior pose state. The shared world renderer also covers solo allies, practice hunters, photographs and live cameras; no camera or client gains new privileged world information.

Gameplay acceptance runs through actual TCP clients, the normal LanWorldPanel packet handler and its production renderWorld method. Initial actor/camera locations are authored test fixtures; walking uses normal STEP commands and crouching uses actual Ctrl key handling. All three roles pass walk A/B, stop, crouch/stand, scene reset and departing-seat checks. Software and RTX 5070 OpenGL 3.3 runs are separate. The full packaged TCP investigation also passes host/join, evidence, identification, ritual, result and payout. Native executable smoke passes real House/Castle GPU rendering. These are local same-machine results, not a multi-PC network certification or a release installer publication.

To reproduce from a clean checkout (Java 17, Maven and cached/downloadable Maven dependencies required):

1. Run `build.cmd package`.
2. Run `mvn -B -ntp -f gpu-client/pom.xml -DskipTests package`.
3. Copy only `gpu-client/target/BeckenburgGPU.jar` to `build/hunter-playable-input/`.
4. Run `jpackage --type app-image --name Beckenburg-Hunters --app-version 0.1.0 --input build/hunter-playable-input --main-jar BeckenburgGPU.jar --main-class org.beckenburg.gpu.GpuDesktop --dest build/playable-hunters --icon build/Beckenburg.ico --java-options "-Dfile.encoding=UTF-8"`. Use a new empty destination for an existing app image.

For this delivery, the build ran in `build/hunter-gameplay-source/`, using committed sources plus only the scoped hunter integration. Concurrent uncommitted van/editor work was excluded. The local app image's JAR is byte-identical to that tested GPU package. Evidence and actual scene captures are under `development/quality/evidence/hunter-gameplay-2026-11-02/`.

The new gameplay regression is in `regression-tests.txt`. Run it with the packaged game JAR plus compiled tests on the classpath. For GPU acceptance use BeckenburgGPU.jar plus compiled tests and append `--gpu`; that mode starts/stops the real OpenGL worker and asserts GPU scene rendering.
