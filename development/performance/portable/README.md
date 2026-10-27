# Portable engineering performance playbook

This is a **copyable, version-controlled method** for performance and runtime modernization across game projects. It is **not** a claim that any other repository has been audited, modified or optimized. The initial authoritative source is The-Beckenburg-Project and its milestone issue #7.

Documents:
- [MILESTONES_A-G.md](MILESTONES_A-G.md): cross-engine engineering gates, measurements, correctness rules, dependencies and rollback.
- [ADOPTION_MATRIX.md](ADOPTION_MATRIX.md): target-specific *hypotheses* for selected game repositories; every entry is unverified until that target is inspected.
- [CHECKPOINT_TEMPLATE.md](CHECKPOINT_TEMPLATE.md): source-control and benchmark evidence template for copying into each target repository.
- [BECKENBURG_LEDGER.md](BECKENBURG_LEDGER.md): Beckenburg-only verified results, pending work and incident history. Do not copy statuses as if they applied to another game.
- [../RENDERING_MILESTONES_A-G.md](../RENDERING_MILESTONES_A-G.md): detailed Beckenburg Java/OpenGL engineering contract.

## Controlled adoption

1. Pin this repository and a **commit SHA** in the receiving project's issue; do not rely on an unpinned moving link when reproducing a decision.
2. Read the target engine and runtime, player hardware, asset pipeline, repository rules and CI before choosing milestones. Adapt the technique, **not** Beckenburg code.
3. Start with A and the verification template. Drop non-applicable gates instead of inventing a GPU renderer in a mod that cannot own one.
4. Implement one reversible, scoped change at a time. Keep the previous rendering/runtime path until behavioral tests demonstrate parity. Save benchmark CSV, diagnostics, screenshots where meaningful, and exact revision/run identifiers.
5. Keep each target project's own issue and acceptance ledger; the master playbook tracks techniques, not its completion status.
6. Propose documented cross-project learning here once there is a concrete source-specific example. Never copy assets, secrets, keys, binaries or licensed content from one repository merely to reuse the process.

The playbook itself never authorizes changes to another repository. Functional defects may block releases; unsolicited cosmetic screenshot comparisons may not. Never force push or claim completion after a failed build, CI job or remote update.

## Beckenburg goal-oriented Codex handoff

The actual Beckenburg finish plan is saved at [../CODEX_PERFORMANCE_FINISH_PLAN.md](../CODEX_PERFORMANCE_FINISH_PLAN.md) with a standalone [../CODEX_HANDOFF.md](../CODEX_HANDOFF.md) and [../CODEX_PROGRESS_LOG.md](../CODEX_PROGRESS_LOG.md). They are **Beckenburg-specific**; other projects can reuse their acceptance and evidence method, but must create their own goal and tests. No other project is authorized or changed by this reference.
