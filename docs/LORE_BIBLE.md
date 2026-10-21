# The Beckenburg Project / shared world lore

## Foundational canon

**Emergence Day occurred in late 2022.** Supernatural forces that had lain dormant in the world re-emerged and took violent, bloody retribution upon the living. The catastrophe destroyed ordinary safety and displaced communities. Over many long, grueling years, humanity has fought the dead and clawed back its place in the world.

**The Beckenburg Project exists to remove supernatural threats and make places safe for normal people.** Castle Beckenburg is the coalition's refuge and headquarters. Reclamation is the purpose of an expedition. Identification, evidence and research are practical means of finding an entity's rules, surviving it and severing its hold; naming a ghost is not the world's ultimate goal.

These points come directly from the user's world direction. Do not silently move Emergence Day, make it an ancient event, or reduce the project to a paranormal collecting club. The exact calendar day, cause, global casualty count and campaign present year are deliberately unspecified. The story takes place after years of struggle; it is not tied to the real computer's calendar. An entity's claimed motive is not automatically established history.

## Historical layers and the map contract

Every site receives three distinct layers: **life before late 2022**, **the catastrophe and local response**, and **survival / attempted return in the years afterward**. A site may already have been empty before Emergence Day. Blackpine closed in 2009; its old payroll discrepancy is separate from the 2025 displaced-survivor journal. The catastrophe does not erase older family, institutional or cultural histories.

Ordinary traces carry this story: evacuation marks, lost names, shelter registers, ration changes, dry water systems, isolated fuel, damaged windows, shared bedding, repaired boots, makeshift heating, radio bearings and later inspection corrections. A board over a window should have a plausible purpose. A fresh camp belongs to particular survivors, not every empty room. Historic occupation does not prescribe the current ghost, evidence combination or mission outcome.

The [23-site thread register](lore/archive-content.json) is exported from `WorldLore.java`, the same source read in the castle archive. Each atlas packet has a `LORE.md` reserving local Emergence, survival and environmental traces. Proposed maps remain proposals; these notes are not completed expedition records. The existing house, farm and school now have supported readable survivor notes, and Blackpine's shelter/service records connect to the same chronology. Full earlier-map redressing still waits for each location's authored story pass.

## Major factions and playable disciplines

| Class | Major faction | Origin and contribution |
|---|---|---|
| Cleric | **The Lantern Concord** | Clergy, lay wardens and refuge keepers who defended evacuation shelters. Protection, care, defensible thresholds and cleansing serve the living, including people outside their beliefs. |
| Observer | **The Meridian Survey** | Surveyors, engineers, radio volunteers and civilian researchers who mapped safe routes. Reliable measurements, corrected testimony and return inspections make reclamation accountable. |
| Demonologist | **The Ashen Collegium** | Occult scholars and survivors of failed bargains who pooled dangerous knowledge. Controlled contact and pact-breaking help sever hostile claims; the faction does not worship the attackers. |

These authored faction names develop the user's three-faction direction and may be refined. Their philosophies can conflict without preventing a working coalition. All retain the existing shared survival baseline; faction membership adds no new hard class locks, rewards or powers in this change. Other civilian and relief organizations can exist. The Common Archive is a coalition service, not a fourth playable class.

## Matthis Rook / Keeper of the Common Archive

Already an elderly municipal archivist at Emergence Day, Matthis helped carry copied registers through a failed evacuation. Original records and several people were lost. His daughter **Mara Rook remains missing**; he refuses to convert uncertain testimony into a death certificate. He brings a hand tremor, repaired round spectacles, an ash cane, worn charcoal wool, a moss waistcoat and patient, exacting habits to the castle's east records wing.

Matthis keeps names, testimonies, contradictions, routes and corrections. He asks who wrote a statement and what that person could actually see. He is compassionate toward survivors and impatient with triumphant reports that leave a place unsafe. His recurring principle: *we clear a house so a family can sleep there again*. His memory and interpretations are fallible; he cannot tell the player the current ghost's secret identity.

His support is a reading desk in the existing east records wing, existing shelves and seating, a prototype physical resident, and **fourteen readable archive pages** covering his history, Emergence Day, reclamation, the factions and all proposed sites. Approach the desk and press E; use left/right to turn, up/down or wheel to scroll, E/Escape to close. Movement and pointer capture pause while reading. This is not a voiced dialogue tree, quest-giving system, recruitable ally, combat AI or finished NPC animation.

## Art status / explicit outstanding master

**Matthis Rook does not yet have approved master art.** The user approved the generated concept portrait for in-game use. That approval covers the portrait in the archive reader; it does **not** imply approval of a complete character master, turnaround, production model or animation set.

[In-game portrait](../assets/runtime/level-textures/lorekeeper-matthis-rook.png) · [Prompt and provenance](art/MATTHIS_ROOK_ART.json) · [Original proxy model/source](../development/levels/lorekeeper-proxy.json). The 1,239-triangle original proxy is deliberately temporary. Future art work needs an identity-consistent full character sheet, front/side/back turnaround, clothing/face/hand details, cane and ledger prop treatment, production UV/texture plan, seated/standing/reading and dialogue poses, movement/animation decisions and explicit master approval. Preserve the accepted portrait when creating those assets.

## Runtime boundaries

Lore, maps, classes and archive pages share one authored source. Public history must not expose hidden host ghost state or automatically check evidence. The existing identification and ritual mechanics remain in place. A new campaign narrative or site text is not proof that persistent reclamation, NPC recruitment or all planned maps have been implemented. Current checks and rendered reader examples are in [world-lore verification](../development/levels/verification/world-lore.json) and the [Blackpine review](../development/levels/BLACKPINE_REVIEW.html).
