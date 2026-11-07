# House encounter: the wooden train

Implementation is in progress; a design or protocol checkpoint alone is not runtime acceptance.

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
of an unknown sound by an older client. Runtime/negative/old-version/real TCP,
photo/evidence, support, occlusion, reset and visual/audio checks remain pending.
