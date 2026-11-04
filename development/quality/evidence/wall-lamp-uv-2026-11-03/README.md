# Wall-light texture repair

The original embedded glTF PNG is top-left image space. The legacy sampler inverted V for lamp_3_on and lamp_3_off, placing gray casing on the tube face and luminous strips on housing edges. Exactly these two models now use the original source orientation at runtime. No GLB/image/UV/geometry/pivot bytes changed.

The source regression compares the actual runtime triangle-centroid samples against original embedded PNG pixels for both power states. All64 source samples distinguish the inverted mapping. The original pre-fix JAR fails; the corrected sampler passes. Source hashes are retained separately.

WallLampReview.java records three camera angles in the real SceneRenderer.drawCastle with the same scene/cameras. Off views switch the actual contracts-room circuit while adjacent illumination remains on. The before/after comparison visibly restores the tube strips to the recessed face and gray metal to the casing. These are runtime captures, not edited images or studio mockups. Mounting shape and lighting strength were not changed.

Castle expansion is held at the owner's request while this shared model correction is checkpointed. The unfinished upper-floor work remains local and is not claimed as delivered by this fix.
