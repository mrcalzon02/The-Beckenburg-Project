# All-map edge blocking and sky inclusion plans

23 October 2026. [Visual atlas](REVIEW.html) · [Catalog](catalog.json).

28 existing/new base packets and the separate cemetery/mortuary annex now have site-specific edge recipes, proposed irregular containment envelopes, exact protected source reservations, 8 azimuth sectors and actual-coordinate viewpoint reservations. No map geometry, runtime registry, engine or original layout is changed.

Sky inclusion means the sky/horizon revealed between real terrain, vegetation, buildings and openings. It is a view-composition contract, not a texture pasted over the map. Near physical containment, middle world-space scenery and far sky/weather have distinct jobs. Frame diagrams are deliberately schematic; they are not fabricated game screenshots.

Existing renderer source: SceneRenderer.paintSky paints weather and a shared hill waveform behind depth-tested world geometry. WeatherSky supplies procedural day/night/cloud weather pixels. This plan retains that system as the integration baseline; it does not implement new per-site runtime horizons. The shared generic hill silhouette needs a coordinated, separately tested per-map treatment for open coasts and other distinct settings.

Do not use low decorative logs, water alpha, fog, a sky picture or invisible walls to claim containment. All proposed barriers need grounded support, closed returns and human/ghost/crouch checks; upper windows/towers require broader scenery coverage. Preserve the van and its cargo approach.

- [St. Alban Academy](academy/README.md)
- [St. Dymphna Hospital](asylum/README.md)
- [Castle Beckenburg](castle/README.md)
- [St. Brigid Parish](church/README.md)
- [Beckenburg College](college/README.md)
- [Hollowmere Farm](farmhouse/README.md)
- [The Briar House](house/README.md)
- [Blackthorn Manor](mansion/README.md)
- [Beckenburg Penitentiary](prison/README.md)
- [Ravenshade School](school/README.md)
- [Blackpine Logging Camp](wld-01/README.md)
- [Camp Hollow Creek](wld-02/README.md)
- [Red Mesa Ancestral Caves](wld-03/README.md)
- [Widow's Peak Fire Lookout](wld-04/README.md)
- [Dead Man's Claim](wld-05/README.md)
- [Drowned Fisherman's Wharf](wld-06/README.md)
- [Saint Bartholomew's Forgotten Cemetery](wld-07/README.md)
- [Northwatch Research Station](wld-08/README.md)
- [Blackwater Hydroelectric Dam](wld-09/README.md)
- [Monastery of Ashen Bells](wld-10/README.md)
- [Mourning Marsh](wld-11/README.md)
- [Gallows Pass Railway](wld-12/README.md)
- [Greywater Cannery](wld-13/README.md)
- [Morrow Point Lightstation](wld-14/README.md)
- [MV Saint Elara](wld-15/README.md)
- [Eight Pines Motor Lodge](wld-16/README.md)
- [Battery Brackenhead](wld-17/README.md)
- [Site Cinder-6](wld-18/README.md)
- [Saint Bartholomew's Cemetery & Vale Mortuary](wld-07-mortuary/README.md)
