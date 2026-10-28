# House continuation: actual RTX diagnostic, not installed acceptance

Branch performance/live-house-residency; source2e85049f54a4a0a8900169c551909f68eee55a1d. Clean Java17 package SHA25671b581b19e3f43d0db186641b2a3b330e6dc11d3ec2b2f2d2374ef52c9f9770b. RTX5070/616.64,712x200,FOV93,Classic PS1,12 van warmup frames then120 yard/entrance/upstairs camera samples. No competing local builds/tests. Camera waypoints exercise normal drawNetwork, not physical traversal or a completed investigation. GL issue/readback counters are wall times, not GPU timestamp queries.

All ten final runs use the SAME NEW JAR with residentHouse=true. Fixed runs hold lighting tick zero. Events use scripted30Hz roomLightTick=i/3: door15 at20,circuit0 at60,door5 at100. Explicit flag values, start times and source/JAR hashes are in per-run metadata and exact-run-script.ps1. Raw frames.csv/process.log/summary.txt and unmodified PNGs are retained.

| Configuration | Frame p50, runs1/2 ms | Frame p95, runs1/2 ms | Frame p99, runs1/2 ms |
| --- | --- | --- | --- |
| Fixed, old worker | 19.872 / 19.888 | 35.968 / 35.799 | 184.800 / 203.187 |
| Fixed, wake worker | 15.808 / 15.534 | 31.732 / 32.329 | 201.890 / 203.555 |
| Events, old worker/cache off | 19.792 / 19.894 | 53.625 / 55.087 | 642.074 / 626.030 |
| Events, wake worker/cache off | 15.815 / 24.903 | 50.291 / 53.222 | 656.342 / 615.647 |
| Events, wake worker/cache on | 15.729 / 26.257 | 50.709 / 59.065 | 233.612 / 239.767 |

The wake worker improves fixed p95 by about4ms; first-view/p99 work remains. Retained visibility reduces door15 scalar rays67432->23617 and door5 rays47385->30600. Initial scalar rays remain44434 in EVERY final configuration. With the wake worker, door15 frames change671.295/765.579->215.208/201.274ms; door5 frames388.831/398.675->233.612/239.767ms. Event-route static transfers match7083648bytes. All eight saved cache-off/on RGB pairs have ZERO changed pixels. Event p95 is mixed and is NOT claimed improved.

Remaining failure: the first exposed House frame still takes about616-656ms, including641-650ms with retained visibility; cold init/prewarm about5.3s. Zero fixed-camera static uploads and32044800bytes resident vertex capacity do not establish process-memory/GC or full-case acceptance. A route p95 below33.33ms does not prove stable30FPS. Native investigation-to-Castle, optical-tool performance, multi-camera churn, context recreation, all-map/editor/multiplayer installed checks remain pending.

Both final wake-worker repeats also PASS actual GL two-way House depth, inactive/active viewport lifetime, fixture/precomputed-emission/flashlight scaling, and evicted draw-only handle restoration, with exit0. The synthetic emission test covers supplied attributes; existing physical-lighting/House tests complement source preparation.

Default-off flags: beckenburg.gpu.residentHouse, beckenburg.gpu.lowLatency, beckenburg.fixture.retainVisibility. Disabling the latter two restores original worker/cache behavior. Optical/software functional fallbacks remain. No merge/release/installed acceptance claimed.

Historical-baseline retains four earlier runs from frozen JAR40dbbbd9 (saved head75868f79,runtimefe217a09). Historical events use elapsed-wall-clock ticks, so are not same-state A/B against the new scripted route. Exploratory-idle1000-not-final-source is an earlier uncommitted experiment, excluded from final statistics. Sandbox-preferences-failure-not-renderer-failure preserves registry/diagnostic access denial; successful final runs used normal Windows preferences access, with NO desktop input.

GPU run40562195922 checks out exact source2e85049f and passes. Full run40562204969 passes262 invocations/0failed/0skipped, packaged House dispatch/3 advancing frames,multiplayer UI and native app-image menu. Its actual checkout is PR merge2f44c8e35b7d29be66e6cec22b9b55bcca5709dc. GitHub commit metadata proves tree dd3d2a33dbdc6f4984d024a19fd26c0ad99f1b2a equals source2e85049f exactly. These are DISTINCT commits with identical trees. Raw metadata and selected exact log lines are in ci/2026-10-27/continuation-2e85049f. Menu launch and diagnostics are not installed full-loop proof.
