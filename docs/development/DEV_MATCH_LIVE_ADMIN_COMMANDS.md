# Live human developer admin — seven AI clients / eight roles

**Developer-only entrypoint.** Run from the project checkout on Windows after running `build.cmd e2e` once:

```cmd
dev-match.cmd
```

Equivalent explicit JVM command, useful in PowerShell or a command prompt:

```cmd
java -Djava.awt.headless=false -ea -cp "build\beckenburg-game.jar" org.beckenburg.DevMatchSession --dev-ai-match --admin
```

The `--dev-ai-match` opt-in is mandatory, and `--admin` opens the live desktop viewer while keeping the seven ordinary TCP client peers in their normal seats. There are **H1–H6, GHOST, and ADMIN (YOU)** (eight roles; seven network players and one read-only local observer). The admin is not advertised in the public game menu and does not occupy H7, a ghost seat, inventory, a collision body, team evidence, or match result. The admin window is local to the host process, not a remote login.

## Human admin controls

- **1–6**: select/follow H1–H6; **7**: select/follow ghost; **0**: free camera
- **WASD**, **Space/Ctrl**, **Shift**: move freely, vertically and fast, noclip; mouse drag: look; mouse wheel: dolly
- **F**: toggle follow; **M**: overhead cutaway; **X**: through-wall actor debug bounds
- Admin window command field: `help`, `follow H3`, `follow GHOST`, `free`, `overview`, `xray`, `status`, `quit`
- Closing the admin window cleanly ends the dev match; it does **not** close a normal public player lobby.

The renderer is the actual `SceneRenderer`, using a separate authored `HouseMap` for the developer camera and live host-reported door/window state. Actor overlay coordinates come from one immutable host tick snapshot and are deliberately drawn over wall geometry. This first overlay draws the physical hunter capsule **envelope** from `HunterPosture.STANDING_HEIGHT` and the world movement radius, not every collision triangle or strike volume. Host-world collision is unchanged. Camera movement is **admin-only**, not an input to `LanClient` bots.

The evidence panel shows the true host ghost species and actual entries from the authoritative observed casefile. The separation from hunter observations is intentional: none of these administrator truths are supplied to the ordinary hunter/ghost AI drivers.

## Current AI scope and validation

`--admin` starts persistent, independent, minimal investigation-driver loops on six real hunter clients and a spectral AI driver in the real ghost client. They navigate authored thresholds, try door actions, sample real instruments, make ghost spatial movement and legal/host-rejected major ghost action requests. **These are early smoke AI behaviors, not yet full evidence reasoning, equipment planning, hunt survival or ritual completion.** Command rejection is not papered over; the host remains authoritative.

For bounded, headless socket tests, run:

```cmd
java -Djava.awt.headless=true -ea -cp "build\beckenburg-game.jar;build\test" org.beckenburg.DevMatchSession --dev-ai-match --bots --seconds=8
```

Artifacts from each bounded or human-closed run are placed in `build/dev-match/`: `match-manifest.json`, `admin-snapshots.jsonl`, `timeline.jsonl`, `host-profiling.csv`, `result.txt`. For long manual sessions, snapshot logs rotate their oldest entries to avoid unbounded memory.

**Important:** GitHub Actions uses a Windows runner for bounded, headless testing and artifacts, **not** an interactive desktop you can connect to. Run `dev-match.cmd` locally to watch the AI match live. This is an independent tool rather than a public menu option.
