# Realtime movement / footstep acoustic work reduction

The authoritative host accepts physical hunter movement at 50 Hz, independent
of the 10 Hz game/evidence tick. Before this change, every accepted HINPUT also
called `recordSound(FOOTSTEP)`, including expensive acoustic reachability and
AI hearing/sensor evaluation. That could evaluate the same 100ms simulation
tick up to five times for one continuously walking hunter.

`FootstepStimulusCadence` admits at most one footstep stimulus per hunter per
authoritative tick. Host authority, real movement distances, collision,
animations, fixed gameplay/evidence clocks, source coordinates, volume,
hearing range and actual hunter/ghost audio continue unchanged. Distinct
hunters are independent; duplicate or old tick commands cannot mint sounds.

Existing legacy STEP was once per tick and keeps its existing maximum
stimulus rate. Environmental sounds, voice, chat, equipment, door and ritual
events do not use this gate and must remain individually observable.

Acceptance: Java 17 FootstepStimulusCadenceTest simulates five accepted
20ms movement updates during each of 100 host ticks and requires exactly
100 permitted stimuli, independent seats and no duplicate replay.
Full multiplayer acceptance is the integration gate. Frame-time reduction
and behavioral balance require separate physical hardware evidence; neither
is claimed by this deterministic test.
