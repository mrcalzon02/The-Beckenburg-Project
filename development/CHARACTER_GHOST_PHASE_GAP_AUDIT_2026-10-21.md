# Character and Ghost Development — Pre-animation Phase Gap Audit

Audit date: 2026-10-21. Repository: `mrcalzon02/The-Beckenburg-Project`, reviewed at `1a3edf3f2e684f3ae97a482c12639ade40abbe66`. No gameplay source changed by this audit.

## Verification baseline

- Full Windows multiplayer acceptance **passed** at this commit: https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/39546299740 .
- The roster defines three Hunter appearance identities and 26 Ghost identities: 13 original, ten Ancient derivatives, three Elders.
- Six Hunter/Elder art profiles have transparent static four-view exports, with tests of alpha, visibility, image uniqueness, turning, movement and depth occlusion.
- The 23 original/Ancient Ghost art identities have directional atlas entries; the renderer currently deliberately uses static `idle` views rather than claiming genuine gait/attack animation.
- Unfinished gender and alternative-model appearances intentionally bind to **their own identity's base art**; these are deferred and are **not** a reason to block this stage.
- Green Ancient ghosts retain distinct existing colored art. Their origin species behaviors and doubled ritual endurance are implemented.
- The Windows CI green check does **not** constitute a visual-art acceptance sign-off for individual silhouettes or ghost-specific mechanics.
- Unrelated Pages deployments have failed; don't conflate this with the successful playable Windows acceptance.

## P0 — before calling entity visuals complete

### CV-01: Twins must be two actual bodies, one case identity

Canonical accepted Twins master: https://drive.google.com/file/d/1qGnNPvZ-7ZKoNIdyymoJg9bHYbo4iEBh/view . Its four main turnaround columns each show **two distinct figures** linked by a spectral tether. Its master design explicitly supports tandem and split-room manifestations, complementary movements, shared anchor and paired response.

Confirmed source discrepancy:
- `GhostIdentity.Type.TWINS` and `ANCIENT_TWINS` are each a single ghost *type*. Keep that, including one evidence signature, one investigation identity and one linked energy/ritual outcome.
- `GhostSpriteAtlas` indexes one compact bitmap per type/direction; this cannot independently preserve the two characters when the whole pair is cropped/fit as one 64×112 actor.
- `LanHost.ghostPose` is one pose. `LanHost.broadcastApparitions()` emits one body.
- `LanWorldPanel.apparition` and `SceneRenderer.Apparition` are singular. The figure rasterizer renders at most one apparition; Twins are additionally classified `GhostTraits.Sight.ORBS`, bypassing their actual body imagery.
- No dedicated pair-body, pair-split, bounded tether or independent world-space twin test currently proves two simultaneous complete figures. Reported half-cropped second twin has not yet been pixel-verified in the packaged ZIP.

**Required implementation**:
1. Extract `twins/a` and `twins/b` from each original front/side/back/three-quarter *pair* view independently, preserving their distinct faces, colors, feet/ground pivots and transparent silhouettes. Do not scale/crop the two-person group into one actor or fabricate missing poses. Derive `ancient_twins/a` and `ancient_twins/b` with the approved green treatment.
2. Keep one `TWINS` gameplay identity, evidence profile, shared energy, linked anchor, rite and payout; add a host-owned list of two individually positioned visible bodies with stable per-member IDs and headings. One primary controller, two presentation/behavior actors. No second player-controlled Ghost slot required.
3. Model bounded pair separation, shared tether/effect, synchronized and split-room manifestation, paired but fair sound/sensor footprints and complementary movement. Whether both bodies can independently harm must be specified and bounded: never unintentionally double victim damage or ghost action energy.
4. Project visibility and line of sight for **each** body and for each receiving Hunter or actual camera separately. A client must not learn the location of an unseen counterpart.
5. Regress four directions × two bodies, each intact and alpha-matted, tandem/split positions, tether endpoints, depth/occlusion, lobby and Ancient variants, camera views, desync recovery, and one shared investigation/ritual result.

**Acceptance:** both named twins simultaneously render with uncropped individual bodies; permitted split-room projection works without omniscient updates; both resolve to one Ghost type. Pair appearance does not automatically confirm a hard clue or mark the journal.

### CV-02: Orb-form classification currently replaces valid character sprites

`GhostTraits.of(TWINS|HANTU|YOKAI).sight()` is `ORBS`; `LanHost.broadcastApparitions()` emits `FORM|ORBS` and gates by orb evidence permission; `SceneRenderer.renderCharacterSprites()` chooses a single `renderSpiritOrb` instead of these figures. This does not fulfill the approved in-world character-art promise.

**Gate:** distinguish visible entity form during a legitimate manifestation from **ghost orb evidence** sampled by a camera. Define when each species may show its authored body versus an orb, without inventing evidence, mandatory clues or always-visible identity; Twins must support paired bodies and pair-specific environmental effects. Test easy/medium/Death Wish, camera and direct sight separately.

### CV-03: Camera-only species require independent real camera visibility projection

`GhostTraits` marks Shade/Specter `CAMERA_ONLY`. `LanHost.broadcastApparitions()` excludes this form from the Hunter apparition feed, and `LanWorldPanel` passes that same apparition to `videoRenderer.drawNetwork()`. No camera-specific host-authorized apparition channel was found in these inspected paths.

**Gate:** a camera on-site can receive its own line-of-sight-, obstruction-, direction-, range- and active-manifestation-gated projection even when its holder cannot directly see the ghost. No camera should inherit the nearby Hunter's privileged projection; no person should see a camera-only figure directly. Cover remote monitors and recording conditions with socket-level tests.

## P1 — before committing real animation loops

### CV-04: Directional silhouette and matte acceptance remains visual, not only structural

The 24 Hunter/Elder PNGs and 23 Ghost atlas appearances pass pixel/alpha tests. But `development/CHARACTER_VISUAL_COVERAGE.md` still identifies weapon and costume edge cropping, dark matte halos, differing proportions and gameplay-scale legibility as unreviewed. Use real camera orbit at near, mid and far ranges, all four source views, environmental lighting, floor and stairs; annotate and correct each frame's source rectangle/matte individually.

### CV-05: Hunter class roles are separated from multiplayer cosmetics

`CharacterVisuals.Hunter` is a selectable three-way cosmetic in `LanLobbyWindow`; `LanHost` stores `hunterVisual` and propagates `VISUAL`, but no corresponding three-class gameplay role assignment was found in the reviewed LAN host. In contrast, solo `InvestigationSession.Role` grants different starting wards/damage/stress effects. 

**Gate:** decide whether the lobby selection is just a costume choice or the authoritative Cleric/Observer/Demonologist class choice. If the latter, bind host-authoritative role rules, class selection/reconnect snapshots, allowed actions/upgrades and HUD in both solo and LAN, independently of gender/appearance.

### CV-06: Elder boss gameplay differs numerically but signature mechanics are incomplete

All three Elders have unique `GhostTraits`, `GhostBehavior`, `GhostFrenzy` profiles and separate visuals. Current primary Ghost actions are generic `DRIFT/WHISPER/DISTURB/MANIFEST/STRIKE/SIGNATURE`; the inspected LAN host does not implement special Blade Demon charge/rush, Plague Demon persistent miasma, or Nothic ground-slam command paths. Descriptive combat text is not implementation evidence.

**Gate:** map accepted boss signature actions to real host-authoritative events, area/LOS/terrain constraints, distinct physical/audio tells, damage caps, AI-human parity and counters **before** commissioning corresponding sprite animation loops.

### CV-07: Phase packets already exist, but static image changes cannot yet telegraph attacks

`LanGhostAnimation` projects MATERIALIZE/LURK/WINDUP/RECOVER/WALK/SPRINT/SIGNATURE according to authoritative movement/events. `DirectionalSprites.frame(...phase...)` deliberately resolves to `idle` art. This is the agreed interim behavior, **not a broken animation system**. Nevertheless attacks and evasive reactions must remain readable via current sound, spatial windup overlays and timing. Verify visibility/occlusion and audiovisual readability with no animation frames, and preserve phase identifiers for later one-appearance-at-a-time animation production.

### CV-08: Practice / solo presentation is not full LAN player fidelity

`SceneRenderer.renderCharacterSprites` still draws a fixed Observer dummy at a hardcoded room offset for its `hunterRoom` practice path; it is not a host-owned, class-specific moving Hunter. LAN remote Hunters do use their class-selected, networked physical body poses. Treat practice dummy as a training stand-in and avoid counting it as a real class+multiplayer acceptance scenario. Solo currently chooses the real Ghost type at room-centered positioning; visually assess actual height/facing and non-human types.

## P2 — safe to defer without pretending finished

- Missing alternate-gender and alternate-model directional sheets: approved same-identity base fallbacks remain correct; don't synthesize alternate sex/body art.
- True authored idle/walk/sprint/attack/ritual/recovery loops, one character/variant at a time, following the canonical master. Do **not** repurpose static 8-state atlas slots as accepted animation.
- Deep first-person held-body/costume articulation and class-specific hand poses: respect validated equipment usage and wait for correctly sourced art.
- Repair outdated art documentation pointing to the frozen homebrew repository, claiming Ghost masters/Elders unavailable or mixing 3D rig requirements with the expressly selected current 2D billboard implementation. Canonical present-day pipeline is the accepted four-view static solution plus deferred animations.

## Recommended next execution order

1. CV-01 Twins dual extraction and dual-body source/pose protocol.
2. CV-02 manifestation versus orb-evidence visual routing.
3. CV-03 true camera-only species feed and independent camera sight.
4. CV-04 visually approved real in-world character capture and crop cleanup.
5. CV-05 Hunter class/appearance contract; CV-06 Elder actions before corresponding action animations.
6. CV-07 readable phase telegraphs, CV-08 practice-mode distinction.
7. Only after these gates, resume one-appearance-at-a-time animation authoring.

**Hard constraints:** preserve all current game content, original image bytes, single main branch, Ghost hidden identity/privacy, lack of automatic evidence confirmation, approved gender baseline fallback, remote GitHub-hosted tests (no local runtime test claims), and existing Windows release verification. Do not confuse the healthy Windows test state with the unrelated Pages deployment failure.
