# What the instruments can distinguish

The coverage test takes the positive instrument readings that one species can actually produce on a contract and compares them with every public species profile. It exhausts all obtainable positive families and compatible writing styles. These are upper bounds for instrument-only identification. A silent spirit box, ordinary air, a blank book, an unused film frame and repeated EMF readings of 2–7 eliminate no candidates. A photographed figure counts only when a person actually sees it in the photo. The test treats handed-in photo receipts as neutral.

The fresh campaign uses its saved four slots (EMF reader, thermometer, spirit box, photo camera). The standard LAN belt replaces the photo camera with a UV lamp. The complete rack also permits a writing book, placed D.O.T.S. projector, deployed video camera, ectoplasm sampler, resonance tuner, seismic recorder, barometric probe, spectral prism, imprint scanner and laser grid. The algorithm ranks only gear the calling investigator can obtain. A tuner and a spectral frequency analyzer test the same resonance family.

| Contract | Fresh campaign, unique / total | Standard LAN, unique / total | Complete rack, unique / total | Mean candidates left, campaign / LAN / rack |
| --- | ---: | ---: | ---: | ---: |
| Easy | 0 / 26 | 0 / 26 | 20 / 26 | 12.00 / 9.38 / 1.23 |
| Medium | 1 / 26 | 1 / 26 | 23 / 26 | 10.00 / 10.00 / 1.19 |
| Death Wish | 0 / 78 | 0 / 78 | 2 / 78 | 19.33 / 19.33 / 6.72 |

Each Death Wish ghost has three possible single-family contracts. The audit counts each once. The game keeps its selected family private. Hantu on Medium is the lone starter-only identification here. The two Death Wish configurations with a decisive rack result are Devil with ritual writing and Twins with child drawings. Either requires actually seeing that style. A different style or a withheld family can leave the case ambiguous.

If a hunter also obtains a direct silhouette photo wherever the species can manifest to a handheld camera, the corresponding fresh-campaign means are 10.46, 8.96 and 16.94. The number of unique instrument-and-photo identifications stays the same. Physically observed wall travel, locomotion, salt reactions and the tint of an actual revealed figure can narrow other cases independently. The coverage table excludes these observations.

## Choosing the next test

A recommendation scores each available instrument by the candidates its possible positive results can eliminate. For example, after strong localized EMF and extreme heat on Medium, the Demon still shares those readings with other species. A resonance measurement distinguishes it. On Easy, ordinary and Ancient Demon both permit atmospheric reversal. Between those two, a positive ectoplasm swab supports the Ancient Demon. A clean swab leaves both candidates in play.

The CSV gives the observed families, candidate sets, number remaining and first recommended rack instrument for each species and difficulty configuration. It lists both starter inventories separately. An empty recommendation means that the considered gear offers no further discrimination under those observations and restrictions. [Per-case results](instrument-coverage.csv)

## What upgrading changes

| Instrument | Tier I reach / refresh | Tier III reach / refresh |
| --- | --- | --- |
| EMF reader | 2.60 m / 1.2 s | 4.55 m / 0.6 s |
| Thermometer | 3.40 m / 1.3 s | 5.95 m / 0.7 s |
| Spirit box | 2.10 m / 1.2 s | 3.68 m / 0.6 s |
| Ectoplasm sampler | 1.00 m / 1.2 s | 1.75 m / 0.6 s |
| Object imprint scanner | 1.60 m / 1.2 s | 2.80 m / 0.6 s |

The tier test exercises the host-side ready and sampled calls at the refresh boundary. Extra reach and shorter refresh intervals give investigators more opportunities during a real event. Higher tiers of a single device retain the same possible species readings. The UV lamp separately extends the lifetime of a detectable footprint from 3 to 5 minutes.

To reproduce the CSV after compiling the tests, run the command from the repository root. Windows uses a semicolon as the classpath separator.

```sh
java -cp build/classes:build/test org.beckenburg.InvestigationDiagnosticsTest instrument-coverage.csv
```
