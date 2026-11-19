# Profile host mutex contention without changing gameplay

The 50 Hz hunter/ghost packet handlers and the required 10 Hz authoritative
simulation tick share the same `LanHost.mutex`. Heavy tick processing may
temporarily delay physical input packets even when the network and GPU are fast.

Run the host with the **local-only, opt-in** JVM property:

```text
-Dbeckenburg.host.lockProfile=true
```

The host records elapsed wall-clock time spent **waiting for** the mutex and
**holding** it, separately for realtime movement packets, simulation ticks
and other network commands. A bounded 256-sample window reports p50/p95/p99
and >20 ms input waits / >50 ms tick holds. The aggregate log prints every
100 host ticks (~10 seconds) as `HOST_LOCK`.

This diagnostic never changes lock order, input movement frequency,
10 Hz gameplay/evidence ticks, ghost behavior, collision, wire packets, or
authentication. All counters are in-process and do not include hunter or
spectral positions, identities, private evidence or chat text.

The default is **OFF** to avoid per-packet timestamp costs in a normal release.
Use `LanHostLockTelemetryTest` to verify samples, percentiles and opt-in
behavior; collect physical Core30 traces only after the existing GPU A/B
benchmark has completed. Never interpret the CPU timing as GPU execution.
