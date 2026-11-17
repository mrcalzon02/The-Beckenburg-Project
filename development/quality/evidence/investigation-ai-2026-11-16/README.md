# Campaign investigation AI and round timing

[Gameplay implementation on main](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/a794c144cb4974ca4be00e588e9f6e2e7c717a41) · [Instrument coverage](instrument-coverage.md) · [All 130 species and contract configurations](instrument-coverage.csv) · [Round milestones](round-results.csv) · [Event transcripts, host energy accounts, rack exchanges and test output](round-traces.zip)

## What happened in the house

Three autonomous campaign hunters completed seed 94 against a Spirit on the published gameplay revision. The team warded room 4 at tick 401, saw a contact-plate imprint at 950, heard a spirit-box reply at 2976, submitted Spirit at 5326, began Absolution at 5651 and completed it at 6712. The run took 11.18 game minutes. The host recorded 21 mid-case rack exchanges, 226 readings across ten instruments, 23 ghost returns to its home and 42 damage. It used the normal chapter, sockets, four-slot inventories and player actions. The profile had eleven specialist instruments purchased at Tier I before deployment. The owner seat and two hires made their own decisions.

On the same published controller and Spirit seed, a team with a fresh campaign inventory warded room 4 at tick 457. Its 887 readings left the species unresolved. All three hunters were incapacitated at 24.72 game minutes. There were zero rack changes. Both seed-94 trials used the same chapter and party size. Different wall intervals and live event schedules mean this is a gameplay comparison, rather than a deterministic paired experiment.

In the separate radio-order scenario, H2 took a UV reading in the Foyer, acknowledged the operator's request for the resonance tuner, returned to the equipment station, then measured Main hall with it. The casefile recorded ordinary spectral noise. The human's saved loadout was intact. The purpose of that scenario is the physical transfer and measurement. The species does not have to produce a positive result for a correctly executed test.

A final two-game-minute test with a van operator and two active hires also verified that H2 acknowledged the hold order, stayed still, then resumed search. The hires entered five indoor rooms, made 41 readings and sent 18 radio messages. The ghost returned home four times. That run used a 12 ms test clock.

The fresh campaign issues an EMF reader, thermometer, spirit box and still camera. Even with every compatible positive instrument reading, the four are conclusive for zero of 26 species on Easy and one of 26 on Medium. The full rack reaches 20 and 23 respectively. Some related species still require an observed distinguishing behavior or appearance. See the coverage report for tested hardware refresh times, Death Wish cases and an example where a clean swab leaves the candidates unchanged.

## Round timing

The design target is a 20–30-minute average. A game minute is 600 host ticks. Owners and hires use live TCP, so command ordering and spontaneous events can vary across attempts with the same seed. A case still marked ACTIVE when observation ends is censored. Its final duration is unknown.

The three consecutive House seeds 1001–1003 were selected before reviewing their species or home rooms. Holding the earlier hunter controller fixed, the initial Twins cases ended in losses at 9.18 and 10.53 minutes. The initial Hantu remained active at 40.00 minutes after becoming energy-starved under a team camping its room. With the revised physical strike/claw recovery and enough fallback energy to leave that condition, the same controller lost the Twins cases at 22.02 and 21.00 minutes and cleansed the Hantu at 21.69 minutes. Those three completed cases averaged 21.57 minutes. This comparison isolates the change in the ghost. The published hunter controller also chooses rack tests, protects teammates and evades perceived strikes.

With the final hunter controller and stocked rack, the same consecutive seeds completed as follows. Each used ten wall milliseconds per tick.

| Seed | Species | Final outcome | Game minutes | Mid-case rack exchanges | Ghost home returns | Damage |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 1001 | Twins | Crew lost | 18.45 | 29 | 40 | 300 |
| 1002 | Hantu | Cleansed | 24.30 | 39 | 61 | 177 |
| 1003 | Twins | Crew lost | 18.18 | 37 | 41 | 300 |

The final mean was **20.31 game minutes**. The Hantu used all eight available powers. Its opponents secured the right room at 3:56, identified it at 22:10 and began cleansing at 23:03. Two of the three seeds drew Twins, both with a R9 anchor. The sample measures these autonomous House cases and gives no estimate of the average for a human crew across all sites.

Other experiments are labeled separately in the CSV. The intermediate Mimic crew warded the correct room but still had no identification at its 35-minute cutoff with starter equipment. Seed 244 was selected for a short path between the home room and the rite and finished unusually quickly. Neither belongs in the three-seed mean.

## Timing and energy settings

Standard cases start the ghost at 100 energy. Over the first 20 minutes, qualifying feeding grows from 48% to 116% of the species rate. A signature's opening surcharge falls from 30 energy to zero. An ordinary species with a 9-second base physical strike cooldown recovers from an opening strike in 42.3 seconds and from a late strike in 16.2 seconds. Claw cooldown, the rest after a frenzy and the natural hunt sanity threshold follow the same case clock. Only eligible signatures receive a cast opportunity. When an isolation feeder is trapped with empty energy, it can slowly recover the fare for one legal drift.

After 18 seconds away, a ghost starts taking connected exits toward its home room, paying the normal costs. On arrival it stays home for 8 seconds. The route, wards, interruptions and available energy determine actual return time. An investigator can revisit that room with new instruments. A powered UV lamp detects a footprint for 3, 4 or 5 minutes at tiers I, II or III. Ectoplasm lasts 3 minutes and contact imprints 4. Recorded casefile observations survive expiry.

On ordinary species, the defended portion of Cleansing is 75 seconds, Banishment 90 and Absolution 105. Ancient and Elder subjects multiply those durations by two and four. Leaving the circle unguarded pauses progress. Eight seconds without a guard breaks a typed rite. A defended ritual on the wrong species disproves the submitted hypothesis.

## Reproduce

With current game and test classes plus the packaged resources on the classpath, invoke these classes from the game repository.

| Class and arguments | Output |
| --- | --- |
| `org.beckenburg.CampaignAiInvestigationScenario 21 120` | Two hires, a van operator, actual hold/resume check, two minutes of game time |
| `org.beckenburg.CampaignHunterRackScenario` | Human radio order, physical rack visit and subsequent field test |
| `org.beckenburg.CampaignAiInvestigationScenario --pace 1002 2400 10` | Three autonomous seats, stocked rack, up to 40 game minutes |
| `org.beckenburg.CampaignAiInvestigationScenario --starter 94 1800 10` | Same dispatch and movement rules, fresh campaign inventory, 30-minute cutoff |
| `org.beckenburg.InvestigationDiagnosticsTest instrument-coverage.csv` | Per-species guide compatibility, recommended tests, and verified tier refresh periods |

The last pacing argument selects milliseconds of wall time per game tick. The shipped clock runs at 100 ms. The accelerated harness uses the same host clock units for feeding, cooldowns, field deposits and rites. The energy CSV reconciles each balance with initial energy plus measured gains minus ordinary, frenzy and repulsion costs. The intervention logs also retain a seven-socket navigation control in which zero of six hunters reached the interior in 90 seconds, and an intermediate 100-second rerun in which six entered and the ghost dealt 20 HP.

## Player views

The [AI inspector](ai-ghost-inspection.png) follows the authoritative host-controlled Demon. The [spectral player viewport](ghost-ability-bar.png) shows Disturb cooling down after a successful activation. The [van monitor capture](van-camera-orb.png) shows an actual short-lived camera orb. The inspector's species and player locations are privileged development information. Hunters make their decisions from their normal seat observations.

## Verification scope

The local runs used Java 17-compatible game code and baked assets. The physical paths and autonomous duration samples here cover House. Tests also cover all three rite families, incorrect hypotheses, floor-aware movement, field residue expiry, acoustic targeting, legal ghost powers, room returns, camera privacy, rack service, hold orders and protective responses. A human-played, multisite timing distribution remains outstanding. Some high-speed concurrent test processes were interrupted by local resource contention. Their partial clocks are excluded from every duration mean. The final cohort was retried with smaller processes.


## Windows packaging

The gameplay revision passed the packaged multiplayer round and renderer checks. Its installer check then caught a genuine occupied-shutter response while a hired hunter was departing from the van. The host now checks the shutter volume at the occupant's posture, and the desktop acceptance test waits for an unobstructed press. The physical collision/operating point passed on House, Farmhouse and School. The network extraction test passed for solo, departed and outside hunters with one settlement per participant. On the corrected revision, Windows passed both the packaged multiplayer investigation and the previously failing Castle, solo and multiplayer desktop acceptance stage. Local virtual-display reruns had insufficient memory to complete that stage. [Extraction change](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/82b234b4f4b6300367b8de37373dc3dbae428674) · [Initial installer result](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44008762227) · [Replacement installer check](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44010028477) · [Seven-client scenarios](https://github.com/mrcalzon02/The-Beckenburg-Project/actions/runs/44010028459)
