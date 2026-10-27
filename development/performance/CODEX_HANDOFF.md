# Codex handoff: goal-oriented performance completion

Use this as the **opening instruction to a future Codex agent working with the actual Beckenburg repository**, preferably on the Windows machine with access to the intended GPU. The handoff is complete as a document; implementation is still outstanding.

## Mission prompt (copy into Codex)

Your goal is to FINISH the Beckenburg A–G performance and startup recovery program, not to produce more disconnected design proposals.

Repository: https://github.com/mrcalzon02/The-Beckenburg-Project
First read AGENTS.md, development/RECOVERY_PLAN_2026-10-25.md, development/performance/CODEX_PERFORMANCE_FINISH_PLAN.md, development/performance/CODEX_PROGRESS_LOG.md, development/performance/RENDERING_MILESTONES_A-G.md, development/performance/portable/MILESTONES_A-G.md and issues #7 and #20.

Fetch current main and all relevant PRs, compare status by exact SHA, and update the log before editing code. The pinned baseline in the finish plan is historical. Respect concurrently running workflows; do not overwrite others' changes or force-push.

Begin with work packages 0 and 1. Then implement a **real live-case, feature-gated, GPU-resident House static geometry path** with camera-invariant lighting and separately revisioned fixture/occluder samples. Reconcile the existing tested D0–D4 prototypes rather than recreating them or blindly merging stacks. Keep dynamic flashlight/UV, ghosts, equipment, live CRT, editor updates and network behavior on correct current paths. Preserve normals, UVs, wall occlusion, ghost reveal rules, depth and the software renderer. Camera-only turns must not rebuild/re-upload static buffers; changed doors/fixtures must invalidate exactly what they affect.

Use reproducible benchmarks, targeted functional tests, actual Windows GPU captures and exact-SHA CI at each step. Only after the real House investigation operates correctly should you advance depth-safe portal culling (C), GPU composition/readback (E), genuine asset/shader warmup and case-start recovery (F), and all-map installed/hardware acceptance (G). Stable 30 FPS under stated Windows hardware/preset conditions is an **acceptance gate**, not something inferred from Mesa CI.

Work one verifiable milestone at a time. Commit and push every verified recovery checkpoint (no force pushes), read back the remote SHA, update CODEX_PROGRESS_LOG.md and issue #7 with CI links and actual metrics. On failures preserve logs and fix root causes; do not silence tests or invent cosmetic release gates. If hardware cannot be run, explicitly mark hardware acceptance blocked; do not claim A–G complete. Do not modify other game repositories. Do not claim background work or publish a release whose full functional/installer gates have not passed.

Stop only when an actual installed game can complete the full investigation-to-Castle loop with accepted frame timing and all relevant gameplay/multiplayer/editor/release checks, OR when a concrete external blocker prevents further safe progress. In the latter case leave a pushed recoverable checkpoint with exact next action and evidence.

## First command/action checklist in that session

- Inspect git status and identify the actual authenticated repository/branch, original material and local Windows/JDK/GPU capability.
- Fetch main and PRs #12, #13, #14, #15, #17, #18, #19, #21, #22, #26, #27, #28, #30 (plus any newer ones); never assume they are still open. Historical v0.1.82 and D4 GPU passes are recorded, but actual installed/hardware validation is still required.
- Read CI: renderer acceptance, full multiplayer, focused Castle, installer, and exact release SHA. Confirm the first-case hang defect status.
- Capture a baseline installed House case on target hardware if available; else use repeatable CI and flag hardware proof missing.
- Select the smallest unresolved work-package task. Commit code plus a *functional* test, verify the exact remote SHA and CI, update the progress log and issue, then choose the next task.
- If you cannot physically operate the user's local Windows installation, explain that clearly and continue only where the remote/CI tools can genuinely verify changes.

## Handoff completion report format

For each iteration report: source branch and SHA; change and fallback; original measured issue; test names and exact runs; gameplay/renderer effect; actual before/after numeric evidence; user-machine hardware proof or PENDING; release link/checksum if actually published; remaining defect; next task. Never conflate "committed", "CI passed", "installed", "hardware accepted" and "released".

**This is a future handoff request, not a claim a Codex job is currently active.**
