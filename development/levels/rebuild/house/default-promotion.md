# Fresh House promotion candidate

On this branch, missing built-in House documents select the furnished rebuild.
Newly created House templates explicitly pin `house-rebuild-v1`. Existing native
House files without source metadata still select `house-original-v1`; existing
versionless custom template metadata also retains its original fallback. Opening
gameplay/editor does not rewrite either form. Explicit recorded architecture
continues to govern saved rebuilt documents.

Actual `HouseDefaultPromotionTest` passes locally at 256MB: fresh gameplay/editor
agree on all rooms/props/colliders and the generator; opening writes no native
file; save/reopen preserves identities and network revision; actual four-TCP
source/breaker controls pass; manufactured historical versionless bytes reopen
unchanged; new custom House templates use the rebuild without forcing its named
location encounter. The historical compatibility fixture now requests its old
generation explicitly rather than assuming a fresh new project is historical.

This is a candidate checkpoint, not accepted default promotion. The broader
legacy/new-default suite and whole-location/client/GPU gates remain unfinished.
The raw parent full run42426485514 at c93d141d completed with364passed/1failed:
LanHunterAnimationGameplayTest observed IDLE/WALK_A but missed WALK_B. Its green
workflow wrapper is rejected as full acceptance. Prioritize that recovery before
merging this candidate or PR82. Do not restart already live PR84 acceptance runs.

The isolated gait correction21d20b29 is now reconciled into this candidate,
preserving all capture/defect/checkpoint histories. The actual prior failure is
not waived; its new full365 run42429527482 remains live.

Seven focused candidate checks now pass locally: fresh default promotion,
historical/rebuilt custom-template compatibility, saved native rebuild upgrade,
recovery (including corrupt-primary legacy/rebuilt bootstrap), stable identity,
source collision round-trip and Editor Agent Services transactions. The latter
four fixtures now choose independent furniture or explicitly manufacture the
historical document they claim to test. Required kitchen/service/story assemblies
remain protected; native colliders use the actual generation's pivot contract.
All existing assertions remain. Rejected fixture results are retained as DEV353.

The actual Windows GUI also passes at256MB: fresh human House opens45props with
rebuilt metadata, writes nothing on opening, captures and saves, and gameplay
loads that save. The same window then opens an explicitly historical document and
completes its recovery/rebuild/undo/redo/save/reopen workflow. Background failures
remain fatal. The separate real CLI process now chooses the inspected fridge's
stable ID rather than detaching the first kitchen counter; edit/save/parity,
reopen/stale and corrupt-start recovery pass. Both fresh captures were inspected.

All369regression entrypoints validate, with no suite execution implied. Candidate
full/native/GPU remote acceptance, deployed gallery readback, remaining matched
compass/join and whole-location/client/hardware gates are unfinished.
