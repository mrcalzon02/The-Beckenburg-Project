# Playable hunter animation evidence

Runtime source: `1e7f3dd670d2c34545040eba085e22ac2d73ffa2`. Asset checkpoint: `3108b484`.

The preview is a generated-art contact sheet, not a gameplay capture. It shows all 36 exported PNGs at standing/crouching display proportions. Source sheets, animated preview and generation prompt set: `art/hunters/animation-v1/`.

Actual local verification:

- Full main-source Java 17 compilation: PASS (includes water and lighting modules).
- HunterSpriteAnimationTest: PASS hunter pose atlas, received movement, stop, crouch, teleport, clock reset and depth occlusion.
- CharacterDirectionalVisualTest: PASS six separately authored four-view masters; 26 static ghost identities; four camera facings and alpha masks.
- CharacterSpriteWorldMotionTest: PASS six real camera-depth sprites, four headings, motion, alpha and wall occlusion.
- GroundedSpriteFeetTest: PASS human sprite world foot anchor, camera movement and original alpha bounds.
- CharacterBillboardAnchorTest: PASS castle billboards, alpha pivot and stable view transitions.
- HunterSpriteAnimationTest repeated from `build/hunter-animation-check.jar`: PASS. This diagnostic JAR includes actual new ZIP resources and compiled production classes; it is not an installer or shipped release.
- Rebuilding the artwork leaves the committed source/export PNGs, ZIP and manifest unchanged.
- Gallery generation and `--check`: PASS, 2241 original PNGs with valid dimensions/hashes.

Use the direct `C:/Program Files/Java/jdk-17/bin/` tools on this machine; the default Oracle shim did not return. Quote PowerShell's `-Djava.awt.headless=true` as a single argument. JAR creation used a workspace-local `-J-Djava.io.tmpdir=.../build/hunter-tmp` because moving the Java tool's sandbox-temp output into the workspace was denied. The workspace-temp retry and packaged check passed.

Pillow was installed only under ignored `build/hunter-python`; export used that directory via PYTHONPATH. Original concept profiles and standing sprite archive are unchanged. Unrelated pending AssetImportDialog and AssetImportPublicationRevisionGuiTest edits are excluded from these commits.

Installed Windows multiplayer gait review and exact-source CI remain pending. Alternate gender/model concept boards are not runtime appearances and were not exported. Main's pre-existing failing installer/full-round/renderer runs are recorded independently in the recovery checkpoint.
