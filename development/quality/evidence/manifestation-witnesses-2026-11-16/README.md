# Watching a manifestation in the House

[Gameplay revision a85a47e](https://github.com/mrcalzon02/The-Beckenburg-Project/commit/a85a47eee203b7fb4d29abcf2a19568262fa5eb6) · [Actual video frames](witness-camera-frames.png) · [Live TCP optics check](optics.txt) · [Campaign radio and physical formation check](crew.txt)

The comparison uses a deployed video camera looking at the same point. Rows are species and columns are living hunters in the subject room. Each photograph in the comparison is a diagnostic capture of the live renderer. The game does not record or retrieve unattended event clips.

## What the checks exercise

- `LanWitnessOccupancyIntegrationTest` starts a host, a ghost controller, a van observer and two field clients. It stages the test positions and then uses ordinary movement commands to cross the physical doorway during the same manifestation. Each species is tested with zero, one, two, one and zero occupants, plus a quiet interval. It compares direct sight, a raised doorway camera, the shutter and a powered remote feed. An empty Shade room also tests the spectral prism across an open door.
- A second powered camera supplies a real wrong-channel control. An unattended receiver, a lowered lens, and an operator viewing the other camera create no figure note, alert or photo bonus. On the right feed the figure becomes visible. A human's explicit note is what reaches the team journal.
- A shutter produces a print with the same acknowledgment for a blank frame and an exposed figure. Only a matching report by its photographer can contribute an optional photo objective at settlement. The test covers a report by the other player, a mistaken headcount and withdrawing a correct report. The journal accepts player observations without grading them live.
- `LanProjectedLightWatchTest` installs both a D.O.T.S. projector and a laser grid during an actual Wraith encounter. Turning away in the field and deselecting the van feed stop each optical contact. A camera by itself generates neither a projected pattern nor a sensor reading. The standalone instruments retain their own functions. [Network output](projected-light.txt) · [Related regressions](optical-regressions.txt)
- `CampaignWitnessWatchScenario` gives three campaign hires an owned Tier I video camera. It issues four ordinary team-radio commands, independently checking the powered camera, living field bodies, operator at the relay and heading toward the screen. Navigation, equipment and assigned seating use the normal campaign clients.
- `FieldJournalTest` checks all 26 identities, all three observation media and counts from zero to six wherever physical observation is possible. `HunterWitnessExperimentTest` checks explicit attention, channel choice, sighting decisions and the effect of losing a view.

The network scenario tests observation timing in one controlled House position. It provides no estimate of average human round length or of sightlines on another site.
