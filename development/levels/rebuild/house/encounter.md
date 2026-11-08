# House encounter: the wooden train

Core, actual solo and real TCP integration checks pass locally. Remote encounter acceptance and subjective audio review remain open.

The original small wooden locomotive beside unfinished schoolwork supplies the
household context. After an investigator spends time beside that authored desk,
the train may turn once while the room is empty and no living investigator has
line of sight. A short original wooden ratchet cue accompanies the change. No
ghost identity, evidence, reward, mission score or hiding socket is added.

The host owns eligibility, dwell, absence, timeout and object mutation. Existing
public CLUTTER snapshots replicate only the actual unheld object pose; trigger
state and owner IDs are not transmitted. Actual walls, decks and doors govern
visibility and normal acoustic projection. Handling/moving the toy, owner death
or disconnect, mission end and timeout abort the pending sequence. A fresh
mission owns a fresh controller. A reconnect receives current object state
without replaying the cue. The complete source object remains supported on its
authored desk; no detached pieces or new player collision are introduced.

Protocol checkpoint: LAN version3 reserves the new authored-location audio
vocabulary. JOIN/RESUME version2 clients must be refused before receiving it.
XYZ, item frames, stable site/room IDs and saved native documents keep their
existing shapes. This is an explicit compatibility boundary, not silent parsing
of an unknown sound by an older client.

Checkpoint: HouseEncounterTest checks source support, whole-object occlusion,
ownership, cancellation and reset; its before/after captures were inspected.
HouseEncounterSessionTest exercises actual solo ticks and confirms normal film
consumption without ghost photo/evidence credit. HouseEncounterNetworkTest uses
four actual TCP connections, rejects JOIN/RESUME2, verifies public pose and
projected sound, persists rotated reconnect capabilities before acknowledgment,
and verifies no replay after reconnect or owner disconnect. Independent power
and ghost possession/privacy negatives pass. These checks use the real tick
path; they do not measure round duration. Host close cancels the controller.

The original synthesized wooden ratchet WAV is preserved with the captures.
Subjective audio, exact-source remote full/native/GPU checks, fresh default
promotion and whole-location acceptance remain unfinished. Source construction,
original model UVs/materials and the corrected south-wall fridge are unchanged.

## Requested sync-only checkpoint — 7 November 2026

Owner fridge objection is already fixed in merged PR81/main1fe0e2490a93c5f148f9f16c686ff7794aa7617e: south-wall centre14.9/14.1925/yawPI,6cm rear gap, clear of both doorways. No additional placement or model change in this checkpoint.

Reconciled encounter parent142601aa includes corrected native recoveryc93d141d. Actual solo and four-TCP encounter integration tests pass at256MB, including normal film/no ghost credit, legacy protocol refusal, public pose, independent power, durable rotated reconnect/no replay and owner-disconnect abort. Host close cancels controller. Rejected test fixtures DEV346–349 and final logs retained. Core support/occlusion captures remain inspected; subjective audio and exact-source remote encounter/full/native/GPU acceptance remain pending.

Recovery native42426323930 at c93d141d is SUCCESS; raw complete native/CLI PASS retained. Corrected full365 run42426485514 remains in progress at checkpoint; no inferred acceptance or merge. Main1fe0e249 full42422292792 actual362/0/0/362, installer42422292783 and native42422292731 PASS. New encounter captures remain branch-published only; deployed gallery readback awaits accepted merge. Gallery generated and checked. Stop after sync/commit as requested; next work is parent365 raw result, PR82 closeout and encounter remote acceptance, then House default/final whole-location review and remaining maps.
