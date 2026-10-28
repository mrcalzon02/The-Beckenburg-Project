# Multiplayer test receipts — 2026-10-28

Source base: `74a2691ef16b1b0e04d998c058a1dcd5b723891e`. Platform: Windows, Oracle Java 17.0.12 targeting `--release 17`. Local implementation SHA and remote read-back are recorded below after publication. These are source-development results, not production release acceptance.

Final test invocations: **265 passed, 2 failed**. Compilation/package command invocations: 2 passed, 0 failed. Repeated manifest entries count as separate invocations. Exact argv, duration, exit code and local log hash are in [delivery.json](receipts/delivery.json); [output](receipts/delivery.log) retains failures. Full-suite failures are not waived. The `local` hbnet invocation deliberately does not run socket acceptance; its 39 local cases are independent results.

The failed tests are:
- `hbnet.HbnetTest`: exit 1, 2.828 s. The strict pin is rejected before application credentials; this environment presents a changed certificate. Actual encrypted gameplay remains unverified.
- `org.beckenburg.SecureLanIntegrationTest`: exit 1, 1.25 s. The strict pin is rejected before application credentials; this environment presents a changed certificate. Actual encrypted gameplay remains unverified.

Total measured command duration: 406.172 s. This sums child-command elapsed times; concurrent work and setup are not included.

Reproduce from the repository root with JAVA_HOME set to JDK 17:

```text
python tools/verify_hbnet.py
python tools/build_server.py
python tools/network_checks.py beckenburg --label acceptance --full
```

## Build/start commands

[Dedicated server instructions](DEDICATED_SERVER.md) give exact Windows/Linux build/start/config commands. No secrets are present in those commands or receipts. `server.cmd` and `server.sh` are committed; standalone JARs were built locally without a GPU/model exporter.

## Not verified / remaining gates

Physical Windows↔Windows/Linux Ethernet/Wi-Fi; NAT-forwarded separate-network WAN; Android↔desktop and API-26/29 TLS/IME behavior; long soak and OS packet impairment; full concurrent adversarial authentication, ACL/invite-revocation and reconnect-theft/race matrix; all Stream Safe loading/diagnostic/crash/overlay/screenshot transitions; clean-machine installation and service-manager lifecycle. The certificate-interception observation is an environment diagnosis, not proof that no further TLS/game bugs exist. Run the unchanged tests on a network presenting the configured certificate and repair any failures. Detailed per-feature gaps are in [LOCAL_FIRST_MULTIPLAYER.md](LOCAL_FIRST_MULTIPLAYER.md).

## Artifact hashes

- `delivery.json` SHA-256 `79dcbd6c935f365ab9e4c503fb33f160c1259c064d381ef7e0e308bbfbc481bc`
- `delivery.log` SHA-256 `653a1e20c42ba72e522add66851e5147e301dd2b7127da37d5823adbab513104`

The newly registered `StreamSafePresentationTest` was recompiled and passed after strengthening existing-widget OFF→ON and list-model inspection. `CommonNetworkTest` was recompiled and reproduced the same pin rejection. Both wrappers are now in regression-tests.txt together with DedicatedLifecycleTest and SecureLanIntegrationTest; the original 262 manifest invocations and the separately listed new gates produced the counts above.


## Exact-byte evidence check

- `delivery.json` SHA-256 `79dcbd6c935f365ab9e4c503fb33f160c1259c064d381ef7e0e308bbfbc481bc`
- `delivery.log` SHA-256 `653a1e20c42ba72e522add66851e5147e301dd2b7127da37d5823adbab513104`

Source parity uses UTF-8/LF Git-blob canonicalization so Windows checkout line endings cannot create false source drift; all three copies were compared and the checksummed verifier passed.

Standalone Beckenburg dedicated JAR process also passed: no phantom seats, local status/stop, process exit 0 and released port. This had no connected game clients and is not TLS gameplay acceptance. [Receipt](receipts/dedicated-process.json), SHA-256 `8b8e6d920b6ec6033e43c055dbd57898d6e18931f6301d6e072cdafe65ec3136`.
