# Shared controls and solo runtime audit — 2026-11-06

Baseline: a59665bff3e11bd0348500384182a2a6c43b619f. Concurrent van departure commit ccb20204c3cb9265667ed95114ef4d5ed9761e9b retained. Unrelated pending safe-area, map and renderer changes were excluded from the staged-source compilation and acceptance.

Removed the duplicate 1,100+ line solo GamePanel. Both free play and campaign solo now use a private loopback LanHost/LanClient, FieldEquipment and LanWorldPanel, matching co-op interaction, placement, gear, physical checks and authority. Saved Castle inventory feeds local free play; campaign settlements and hired hunters retain their existing shared-host code. Local launch runs off the EDT and can be cancelled; returning home closes the private host.

## Controls

WASD move; Space run; Shift crouch (Ctrl alias); E interact/pick up; Q cycle gear; 1–4 select; F use/raise camera; LMB operate/shutter; RMB power; T flashlight; G placement preview; R drop; LMB throws held clutter; wheel rotates held clutter or placement; Tab journal; J casefile; M van map; C radio and V local push-to-talk; Enter text then Tab channel; K named field actions; H help; Escape pause. Existing 5/6 class skills, L hiding and U UV shortcuts remain. Ghost fixtures use E and exits use Q/Z. Standard play requires no F keys; F11 is an optional fullscreen alias, with Alt+Enter also available. Developer/editor/debug bindings are outside normal play.

K exposes the equipment rack, supplies, writing-book operations, wards, rituals, countermeasures, medicine, identification, recorder playback and voice settings. Access, range and ownership stay host-validated. Drop supports carried catalog devices, stable owner/device identities, original tiers and resources, host floor/LOS/reach checks, empty belt slots and physical pickup. Dropped powered devices switch off; deployed equipment must be retrieved first. Existing indoor-support and owner-only pickup limits remain; this does not implement item sharing between hunters.

## Verification

Java 17: every staged main source and test source compiled successfully in an isolated source snapshot. All 16 focused tests passed; full outputs are in focused-results.json. tested-source-sha256.json fingerprints each changed Java source directly from the tested Git index.

LocalInvestigationTest proves actual one-player host startup, the same multiplayer viewport, F temperature measurements, Q slot selection, independent T flashlight, Shift authoritative posture, Tab journal, real ward consumption, camera drop and TCP pickup, packet/belt drop roundtrips for every functional catalog device, physical van departure, successful identification/payout, durable duplicate-proof solo payout after reload, and campaign startup with a reserved hired hunter on the same server.

Existing checks cover multiplayer actions, incapacitation, crouch/low ceilings, DOTS placement acknowledgment/rotation/retrieval, field-equipment privacy, manual journals, campaign/co-op launch and career/progression, physical EMF drop ownership/reach/rollback and catalog descriptions/selection.

Not claimed: installed-copy replacement, installer publication, exact-source remote CI, headset hardware, multi-PC play or physical GPU/desktop acceptance. Updated desktop E2E readiness now inspects the shared viewport/cinematic, but that graphical test has not been run for this source. Existing remote green runs (full multiplayer 42345812287, installer 42345812284 at fb980c64; renderer 42344509831 at fcc394f2) precede this change and do not certify it.
