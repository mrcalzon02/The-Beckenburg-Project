# Defect ID reconciliation — 26 November 2026

This record resolves numeric identity collisions during the authorized roster integration. It does not close gameplay defects, certify the combined source or change map-stage acceptance.

## Pinned inputs

| Input | Revision | Original records | Distinct ID/title pairs |
| --- | --- | ---: | ---: |
| main | `9f1cf5e2b5535b2ded46eba07d55c78f61211775` | 393 | 391 |
| map/roster-structure-batch | `d9f706f44984aaf05cadaf0911eb058880056bb2` | 751 | 749 |

The common base is `7c4fe3be13e9f330aeaf554002ac0df97dc1ea71`. The 351 shared ID/title pairs have identical issue fields in both inputs. Their recorded statuses and evidence therefore need no conflicting-field resolution. The union contains **789 distinct records**: 351 shared, 40 main-only and 398 candidate-only. Main DEV-746 and DEV-747 remain unchanged.

## Identity rule and preservation

Match a historical issue by its original ID **and exact title**, never by ID alone. Keep main records first. Main already used DEV-263–270 for two different meanings each; the first occurrence in pinned main keeps its original ID and the second meaning receives DEV-748–755. This is a documented disambiguation of an existing collision, not a claim that either issue was invalid. Candidate-only meanings that collide with main receive DEV-756–796. All eight inherited secondary meanings also exist on the candidate and share the same new canonical ID.

All fields other than the mapped ID are preserved exactly. The two repeated DEV-254 and DEV-255 entries were exact duplicates within each input; each now has one canonical record. No distinct symptom, cause, action, evidence, status or older-map obligation was discarded. The structured `id_reconciliation` metadata in [defects.json](defects.json) records every affected branch, source revision and original array index.

Historical recovery checkpoints, case receipts and the pinned map diagnostic keep their original references. Consult the table below with the source title and revision; an old bare numeric ID can be ambiguous. The earlier [1 November reconciliation](DEFECT_ID_RECONCILIATION_2026-11-01.md) remains valid for its original IDs.

**Allocation:** 49 remapped meanings occupy DEV-748 through DEV-796. DEV-797 is the next available ID at this checkpoint; check the current registry before allocating further IDs.

## Complete alias mapping

| Original scope | Original ID | Canonical ID | Historical title |
| --- | --- | --- | --- |
| main + roster candidate | DEV-270 | **DEV-748** | Screenshot pushes leave development gallery index stale |
| main + roster candidate | DEV-269 | **DEV-749** | Shallow Windows installer tag push exceeds GitHub 2 GiB Git-pack limit |
| main + roster candidate | DEV-268 | **DEV-750** | Castle NPC screenshot audit overwrites failed angles and falsely accepts one passing view |
| main + roster candidate | DEV-267 | **DEV-751** | Camera-selected directional art still moves stationary NPC world-space geometry |
| main + roster candidate | DEV-264 | **DEV-752** | Castle light-switch source GLB gains an enormous stretched sconce backplate |
| main + roster candidate | DEV-265 | **DEV-753** | Door state refresh discards unrelated stationary shadow visibility and causes frame stalls |
| main + roster candidate | DEV-266 | **DEV-754** | Castle tabletop decorative models ignore edited supporting furniture rotation |
| main + roster candidate | DEV-263 | **DEV-755** | Character boots visibly float above the Castle floor despite earlier alpha-pad correction |
| map/roster-structure-batch | DEV-356 | **DEV-756** | Fresh House promotion exposes editor fixtures that mutate the first attached assembly |
| map/roster-structure-batch | DEV-357 | **DEV-757** | Remaining native rough shells omit enclosed ceilings and reserve only top-end stair holes |
| map/roster-structure-batch | DEV-358 | **DEV-758** | Planning text and diagram stand-ins enter structural GLB exports |
| map/roster-structure-batch | DEV-356 | **DEV-759** | Search-envelope annotations leave context doors and exterior service mouths open |
| map/roster-structure-batch | DEV-357 | **DEV-760** | Reusing an intermediate circular deck core can leave a top cap open |
| map/roster-structure-batch | DEV-358 | **DEV-761** | Native batch review assumes Pillow is installed in the active Python |
| map/roster-structure-batch | DEV-359 | **DEV-762** | Eight remote elevation links have zero or insufficient physical stair run |
| map/roster-structure-batch | DEV-360 | **DEV-763** | Level route reservation strips refill stair openings and obstruct heads |
| map/roster-structure-batch | DEV-361 | **DEV-764** | Native stepped candidates fail current body and traversability queries |
| map/roster-structure-batch | DEV-362 | **DEV-765** | Expanded stair apertures create collapsed ceiling sliver cells |
| map/roster-structure-batch | DEV-363 | **DEV-766** | Roster collision probe exposes crossing stairs and terrain covering authored flights |
| map/roster-structure-batch | DEV-364 | **DEV-767** | Hydroelectric return stair still needs authored shaft enclosure and foundations |
| map/roster-structure-batch | DEV-365 | **DEV-768** | Offline terrain sampler resolves seed inputs using newly created working paths and can swallow Blender errors |
| map/roster-structure-batch | DEV-366 | **DEV-769** | Saved terrain seeds retain rough proposal perimeters and unfinished aperture rims |
| map/roster-structure-batch | DEV-367 | **DEV-770** | Asset discovery overlooks staged site kits and treats a small runtime sample as the working library |
| map/roster-structure-batch | DEV-368 | **DEV-771** | Furnishing export guard reads deleted Blender objects after independent reset |
| map/roster-structure-batch | DEV-369 | **DEV-772** | Furnishing fit double-counts route buffers and overextends endpoint clearance |
| map/roster-structure-batch | DEV-370 | **DEV-773** | Props can obstruct physical routes that cross rooms without naming those rooms |
| map/roster-structure-batch | DEV-371 | **DEV-774** | Many authored flat route samples fail baseline native support or body checks |
| map/roster-structure-batch | DEV-372 | **DEV-775** | Asset index regeneration encounters Windows invalid-argument error on direct overwrite |
| map/roster-structure-batch | DEV-373 | **DEV-776** | Rectangular stair treads overlap inside curved lighthouse flights and leave outside gaps |
| map/roster-structure-batch | DEV-374 | **DEV-777** | Float32 shared stair joins can report unsupported micrometre seams |
| map/roster-structure-batch | DEV-375 | **DEV-778** | Turn landings overhang the incoming stair flight and block standing movement |
| map/roster-structure-batch | DEV-376 | **DEV-779** | Cyclic stair end boundaries and missing final landing interrupt lighthouse ascent |
| map/roster-structure-batch | DEV-377 | **DEV-780** | Flight-only chain audit omits flat landing segments and resets at link boundaries |
| map/roster-structure-batch | DEV-378 | **DEV-781** | Regenerated native scaffold next-step notes retain superseded sloped-mass instructions |
| map/roster-structure-batch | DEV-379 | **DEV-782** | Legacy advisory stair run/riser fields lag the actual authored coordinate revision |
| map/roster-structure-batch | DEV-380 | **DEV-783** | Terrain sampling uses immutable seed-layout cuts instead of current authored basement entries |
| map/roster-structure-batch | DEV-381 | **DEV-784** | Base cemetery lacks a terrain source and profile for saved-world integration |
| map/roster-structure-batch | DEV-382 | **DEV-785** | Cemetery ossuary stairs cross vestibule walls before reaching the basement floor |
| map/roster-structure-batch | DEV-383 | **DEV-786** | Native collision exporter overwrites its previous payload before terrain validation rejects a candidate |
| map/roster-structure-batch | DEV-384 | **DEV-787** | Moving cemetery stair openings invalidates old approach endpoints and room-centre anchors |
| map/roster-structure-batch | DEV-385 | **DEV-788** | Uncut route slabs expose inward top faces and mark undersides as walking supports |
| map/roster-structure-batch | DEV-386 | **DEV-789** | Local path grading can disturb adjoining route shoulders when overall terrain is unsettled |
| map/roster-structure-batch | DEV-387 | **DEV-790** | Before/after route comparator reuses old feet Y after valid terrain support changes |
| map/roster-structure-batch | DEV-388 | **DEV-791** | Prison blocking overview shows isolated unsupported boundary panels |
| map/roster-structure-batch | DEV-389 | **DEV-792** | Route reservation surfaces overlap real floor planes in rough native assemblies |
| map/roster-structure-batch | DEV-390 | **DEV-793** | Thin outside-cut strips exhaust editor topology budget without adding footprint fidelity |
| map/roster-structure-batch | DEV-391 | **DEV-794** | Prison construction footprint does not align with editable terrain leaves |
| map/roster-structure-batch | DEV-392 | **DEV-795** | New site terrain exposes old flat-route and blockout elevation conflicts |
| map/roster-structure-batch | DEV-393 | **DEV-796** | Larger macroterrain sites exceed the single editor mesh export area |

## Manual ledger notes survive regeneration

The candidate ledger contained **102 manually appended headings** after its generated continuity paragraph, from DEV-638 through DEV-745 with additional combined/follow-up headings. Their exact UTF-8 text is retained in `ledger_notes` within [defects.json](defects.json) and emitted under “Historical manual roster notes” in [DEFECT_LEDGER.md](DEFECT_LEDGER.md). The note text is unchanged, including its source links, historical wording and original DEV references.

Preserved note-text SHA-256: `b00f7649f8885f61e94e7f76a8bbb9c3d6eae0a43a5308305f2bc29aa2d3b99b`. This digest covers the retained `text` string, including its original leading/trailing newlines. It is a preservation check, not gameplay verification.

`python tools/build_defect_ledger.py` regenerates the ledger from structured records and retained notes. `python tools/build_defect_ledger.py --check` verifies that the checked-in ledger matches without writing it. Put future retained notes in `ledger_notes`; direct additions to the generated Markdown are not the authoritative source.

### Inline historical sections

A complete section-by-section comparison also preserves **34 inline ledger sections** whose Markdown differs from the JSON-derived view: four from main and thirty from the candidate. Together with the 102 appended headings, **136 historical note headings** now survive regeneration. The complete section text is retained verbatim, including older status wording, inline links, numerical measurements and follow-up paragraphs; it does not replace or upgrade the canonical structured status.

- Main: DEV-362, DEV-364, DEV-385 and DEV-386 at `9f1cf5e2b5535b2ded46eba07d55c78f61211775`. All four exact sections are also present at pre-diagnostic main `771b55ae652c12a43aeacfbe65d55c3ffc1bc57c`.
- Candidate: DEV-537, DEV-613–637 and DEV-734–737 at `d9f706f44984aaf05cadaf0911eb058880056bb2`.

The main DEV-385 Markdown uses a different historical title for its Shade manifestation check; the original heading and complete verification paragraph are preserved alongside the canonical structured entry. The new note metadata includes each section’s original line and digest, so a wording or evidence discrepancy can be traced to the actual source ledger.

| Retained inline group | Headings | UTF-8 note-text SHA-256 |
| --- | ---: | --- |
| main | 4 | `bf4ea61062985729ef4bac1d7acd9b96fe8c0789e3197557401b84fab5f23d63` |
| map/roster-structure-batch | 30 | `d3340d777bb55fee8adb1e643de284e84f198ceca0d7a0ddfe3a4e255972e7c9` |

Each digest covers the stored group’s exact `text` string. Source section digests are also recorded individually. These are preservation checks, not new runtime results.

## Remaining acceptance work

No new tests were executed by this identity reconciliation. The combined source still needs its scoped Castle/House, packaged solo/TCP, installed launch and relevant physical/visual review evidence. DEV-746 remains open for broader stage-tracker reconciliation; DEV-747 remains open for current-source replacement of the six historical case receipts. Existing defect status text retains the scope of its original checkpoint, including source-only fixes and historical verification.
