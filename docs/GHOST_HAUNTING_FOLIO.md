# Beckenburg: Haunting Folio

The ghost simulation, identity, evidence matrix, and actual world interactions remain authoritative. This folio controls how **accepted** events are presented. An environmental disturbance is not automatic proof of any ghost. The player's eyes, ears, cameras, and instruments determine what they observe.

## Reveal and escalation policy

| Tier | Host action | Appearance | World/audio |
| --- | --- | --- | --- |
| Ambient | DRIFT | Only atmospheric effects | Subtle audio and peripheral pressure |
| Minor | WHISPER | Only atmospheric effects | Whispers, soft lighting interference |
| Moderate | DISTURB | No identifying sprite | Existing real fixtures and clutter, light surges, audio |
| Major | MANIFEST | Shared `unrevealed_ghost` appearance | Generic silhouettes with spectral overlays, light surges |
| Dangerous | STRIKE | Shared generic apparition during ordinary attacks | Host-authoritative attacks and defensive counterplay |
| Signature | SIGNATURE | Actual species directional art **for a limited time** | Stronger room lighting and the ghost's signature behavior |

The `SIGNATURE` animation window lasts 10 simulation ticks (one second at the current 10 Hz simulation), independent of the surrounding visual effects. The event overlay may linger longer. Character appearance is never a permanent unlock after one signature, and neither a signature nor a sighting marks a journal guess as confirmed.

Ancient ghosts use base-archetype signature event behavior with their own existing approved tinted appearance. Elders have individual signature entries. The Twins remain two separately positioned and animated actors; each is a neutral silhouette before the signature, then uses its own authored directional frame. Camera-only ghosts can be fully revealed only on a valid camera feed.

The folio supports six reusable PSX-effect families: **veil, shadow, static, chill, pulse, distortion**. The full authored list below is executable through `GhostHauntingFolio` rather than decorative documentation. Each row uses a real sound cue and plays alongside the existing physical fixture, clutter and lighting systems. More bespoke audio and visuals can be added without changing the selection, timing or reveal contracts.

## Indexed 50-event catalogue

| ID | Haunting | Tier | Effect | Audio | Restricted signature |
| --- | --- | --- | --- | --- | --- |
| `corridor_exhale` | Corridor Exhale | ambient | veil | ghost_drift | No |
| `passing_shadow` | Passing Shadow | ambient | shadow | ghost_drift | No |
| `threshold_tremor` | Threshold Tremor | ambient | pulse | ghost_drift | No |
| `airless_hall` | Airless Hall | ambient | chill | whisper | No |
| `distant_presence` | Distant Presence | ambient | static | whisper | No |
| `wall_murmur` | Wall Murmur | minor | veil | whisper | No |
| `shoulder_breath` | Shoulder Breath | minor | shadow | whisper | No |
| `broken_sentence` | Broken Sentence | minor | static | whisper | No |
| `winter_breath` | Winter Breath | minor | chill | whisper | No |
| `hushed_warning` | Hushed Warning | minor | pulse | whisper | No |
| `hollow_voice` | Hollow Voice | minor | distort | whisper | No |
| `doorway_sigh` | Doorway Sigh | minor | veil | horror_sting_a | No |
| `lightning_static` | Lightning Static | moderate | static | disturb | No |
| `lamp_convulsion` | Lamp Convulsion | moderate | pulse | disturb | No |
| `bent_perspective` | Bent Perspective | moderate | distort | disturb | No |
| `window_shadow` | Window Shadow | moderate | shadow | disturb | No |
| `floor_chill` | Floor Chill | moderate | chill | disturb | No |
| `stairwell_mist` | Stairwell Mist | moderate | veil | disturb | No |
| `radio_crawl` | Radio Crawl | moderate | static | horror_sting_b | No |
| `corner_darkening` | Corner Darkening | moderate | shadow | horror_sting_c | No |
| `doorframe_figure` | Doorframe Figure | major | shadow | manifest | No |
| `shrouded_witness` | Shrouded Witness | major | veil | manifest | No |
| `bent_silhouette` | Bent Silhouette | major | distort | manifest | No |
| `frozen_silhouette` | Frozen Silhouette | major | chill | manifest | No |
| `flicker_walker` | Flicker Walker | major | static | manifest | No |
| `silent_vigil` | Silent Vigil | major | pulse | manifest | No |
| `mist_crossing` | Mist Crossing | major | veil | horror_sting_a | No |
| `blindspot_watcher` | Blind-Spot Watcher | major | shadow | horror_sting_c | No |
| `pressure_wave` | Pressure Wave | dangerous | pulse | strike | No |
| `vision_collapse` | Vision Collapse | dangerous | distort | strike | No |
| `grasping_darkness` | Grasping Darkness | dangerous | shadow | strike | No |
| `rending_static` | Rending Static | dangerous | static | horror_sting_b | No |
| `death_chill` | Death Chill | dangerous | chill | strike | No |
| `suffocating_veil` | Suffocating Veil | dangerous | veil | tension_build | No |
| `bond_shattering` | Bond Shattering | signature | pulse | signature | POSSESSIVE |
| `violent_cascade` | Violent Cascade | signature | distort | signature | POLTERGEIST |
| `infernal_apparition` | Infernal Apparition | signature | shadow | signature | DEMON |
| `voice_in_flesh` | Voice in the Flesh | signature | static | signature | YOKAI |
| `widows_lament` | Widow's Lament | signature | chill | signature | BANSHEE |
| `through_wall` | Through the Wall | signature | veil | signature | WRAITH |
| `camera_witness` | Camera Witness | signature | shadow | signature | SHADE |
| `lens_borne_figure` | Lens-Borne Figure | signature | static | signature | SPECTER |
| `quietus_revelation` | Quietus Revelation | signature | pulse | signature | SPIRIT |
| `glacial_revelation` | Glacial Revelation | signature | chill | signature | HANTU |
| `pact_bearer` | Pact Bearer | signature | distort | signature | DEVIL |
| `borrowed_visage` | Borrowed Visage | signature | shadow | signature | MIMIC |
| `paired_revelation` | Paired Revelation | signature | veil | signature | TWINS |
| `executioners_coming` | Executioner's Coming | signature | pulse | signature | ELDER_BLADE_DEMON |
| `plague_procession` | Plague Procession | signature | veil | signature | ELDER_PLAGUE_DEMON |
| `unblinking_judgment` | Unblinking Judgment | signature | distort | signature | ELDER_NOTHIC |

## Integration and QA requirements

An accepted `GhostDomain.Resolution` chooses an event deterministically from a seeded anti-repeat folio. In LAN the host sends `HAUNT|id|room|tick` to investigators physically in the affected room or monitoring a powered camera there. The client applies visual filters in the appropriate room; audiovisual events cannot mutate gameplay evidence, ghost damage, inventory, or rituals. Solo uses the identical registry and effect implementation.

The player-facing apparition, live camera, and two independent Twins bodies must never select character-specific texture files for ordinary manifestations. The only authorized reveal comes from a real host SIGNATURE action in its 10-tick window. The catalog is validated by `GhostHauntingFolioTest` and camera appearance by `LanCameraSightTest` in the Java 17 regression suite.

**Limits:** Environmental names are authored scenario identifiers, not promises of unique prop animations. Six shared renderer filters, existing spatial audio and existing physical interactions are the current shipped primitives; dedicated bespoke 3D events, audio recordings, per-object authored choreography and animation loops are future asset passes.
