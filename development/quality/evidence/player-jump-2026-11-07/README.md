# Player jump checkpoint — 2026-11-07

Source base: `516bc2d0310c03909192b787a46b795fa1ac64b5`, existing `map/first-principles-rebuild-plan` checkout. Concurrent safe-area work is preserved and excluded from this checkpoint.

Implemented: Space jump, Shift run, Ctrl crouch in the shared solo/co-op investigation viewport. Host-authoritative tuck raises feet .73 m, then 4.8 m/s takeoff under 18 m/s² gravity; carried running velocity, 10 ms collision sweeps, ceiling impacts, floor/deck landing, brief landing crouch and headroom-aware stand. Existing deliberate crouch persists. Repeated airborne jumps are rejected. Host ticks advance motion without client packets. A three-second void/contact timeout recovers to the last safe room. The existing crouch atlas is rendered at accepted airborne feet Y.

The engine-independent `editor.DefaultPlayerMotion` ships automatically in LevelEditorKit. `EditorProject.createPlayerMotion()` provides it by default. Other runtime adapters must supply collision and descending support queries and wire their own controls; Castle jumping is not connected by this change.

Validation was repeated on an isolated snapshot of the staged source, excluding concurrent safe-area changes (427 main and 542 test Java sources). Java 17 full main/test source compilation; standalone editor-kit compilation; PlayerJumpTest, PlayerJumpNetworkTest, LanWorldTest, CrouchAndOutdoorHidingTest, CrouchCameraTest, CrouchNetworkTest, LanWorldControlsTest, LanWorldIntegrationTest, HunterSpriteAnimationTest. TCP tests use actual localhost clients. `jump-poses.png` is a visually reviewed raster fixture through the actual hunter sprite drawing function, not an installed gameplay capture.

Recovery inspection: remote main observed at ea6dbee005bf5b5f97d7241251c8daca375eccf5; newer full-round run 42358684986 and renderer 42358685109 at 2bbc771f26fa22605a6e666947f0f812f5777173 were running. Latest inspected installer run 42356499806 at c675ed0f5afebb78da814478db892f83c8efa054 passed. None validates this feature source.

Remaining acceptance: exact-source CI, installed GPU/first-person gameplay review, older-map jump and stair/ledge/prop audits, and downstream runtime adoption. No installed or release acceptance is claimed.

Implementation commit: `234664343edd7d9088853ed23dbb51e48f4e0662`. Standalone LevelEditorKit JAR built locally and verified to contain DefaultPlayerMotion, its collision adapter and EditorProject.

Publication: automatic approval review rejected the push to `mrcalzon02/The-Beckenburg-Project`, branch `map/first-principles-rebuild-plan`, because explicit user authorization for exporting this source payload was missing and the repository was not verified as trusted. No workaround attempted. Next: obtain explicit approval, push the scoped checkpoint and verify the remote SHA.
