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
