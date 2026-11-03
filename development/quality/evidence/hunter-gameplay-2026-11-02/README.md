# Real gameplay hunter animation acceptance

Runtime source: `c876e163`. The isolated build excludes concurrent uncommitted van/editor changes. The gameplay regression receives real TCP host packets in the actual LanWorldPanel and invokes the same renderWorld method as its live frame loop. Initial authored actor/camera positions are test fixtures; movement uses normal client STEP commands and crouch uses the real Ctrl key handler. Captures are actual complete House scene frames with geometry, lighting, furniture and depth occlusion, not sprite-sheet mockups.

All three classes pass both walk states, stop-to-idle, Ctrl crouch/stand, scene reset and departing-seat cleanup on software and physical NVIDIA RTX 5070 OpenGL 3.3. The strengthened scene-reset assertion uses a continuous clock; omitting only the production scene-reset line produces the expected failure in scene-reset-negative.log. GPU positive log records all three passes and clean worker completion (process exit 0).

The packaged full multiplayer round passes actual host/join, evidence, player hypothesis, ritual, shared result and payout. Unit pose/occlusion, crouch/cover physics, original sprite world motion and floor-anchor checks pass. Native Windows executable smoke passes actual House/Castle rendering, opening flights, flashlight and VBO reuse. acceptance.json verifies that the app image contains the byte-identical tested GPU JAR.

`captures/runtime/software/` and `captures/runtime/gpu/` each contain 12 frames: three classes x walk A, walk B, idle and crouch. Furniture may occlude legs or lower a crouched character out of view, as it should under the real depth buffer. The four-view source sprites retain their original alpha/pivots; no geometry, collision or movement authority was changed.

Run the local native build using `play-hunters.cmd` at the repository root. The older installed copy/shortcut is separate. See `docs/development/PLAYABLE_HUNTER_ANIMATION.md` for reproduction. This is local same-machine gameplay acceptance, not a multi-PC certification or a published installer. Exact-source remote CI and branch gallery deployment remain separate.
