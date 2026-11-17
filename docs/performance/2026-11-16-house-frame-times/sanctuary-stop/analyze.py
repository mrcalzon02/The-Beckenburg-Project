"""Read the Core30 worker ZIP. Each trial/phase starts a fresh interval series."""
import argparse
import csv
import hashlib
import io
import itertools
import json
import math
from collections import defaultdict
from pathlib import Path
from zipfile import ZipFile


def rank(values, fraction):
    return sorted(values)[math.ceil(len(values) * fraction) - 1]


def timings(gaps):
    return [len(gaps), *(round(rank(gaps, q), 3) for q in (.5, .95, .99)),
            sum(value > 50 for value in gaps), sum(value > 100 for value in gaps),
            round(max(gaps), 3)]


def write_csv(path, columns, rows):
    with path.open("w", newline="", encoding="utf-8") as output:
        writer = csv.writer(output)
        writer.writerow(columns)
        writer.writerows(rows)


def analyze(raw, output):
    output.mkdir(parents=True, exist_ok=True)
    pooled = defaultdict(list)
    trials, stops, slow = [], [], []
    metrics = ["intervals", "p50_ms", "p95_ms", "p99_ms", "over_50_ms", "over_100_ms", "max_ms"]
    with ZipFile(raw) as archive:
        provenance = json.loads(archive.read("provenance.json"))
        assert provenance["runner"] == "OLD-CORE30-homebrew" and provenance["backend"] == "gpu"
        csvs = sorted(name for name in archive.namelist() if name.startswith("pair-") and name.endswith("/frames.csv"))
        for name in csvs:
            trial = name.split("/")[0]
            _, pair, mode, arm = trial.split("-")
            with archive.open(name) as f:
                rows = list(csv.DictReader(io.TextIOWrapper(f, encoding="utf-8-sig")))
            summary = dict(line.split("=", 1) for line in archive.read(trial + "/summary.txt").decode().splitlines() if "=" in line)
            assert "1660 SUPER" in summary["renderer"] and float(summary["max_travel_metres"]) > .1
            if mode == "tcp":
                assert int(summary["visible_peer_hunters"]) >= 1 and int(summary["accepted_steps"]) > 0
            for phase in ("idle", "moving", "stopping"):
                frames = [r for r in rows if r["phase"] == phase]
                advancing = []
                for i, row in enumerate(frames):
                    frame = int(row["composited_gl_frame_id"])
                    if frame < 0 or (advancing and frame == advancing[-1][1]):
                        continue
                    advancing.append((i, frame, int(row["time_ns"])))
                gaps = [(b[2] - a[2]) / 1e6 for a, b in zip(advancing, advancing[1:])]
                assert gaps and min(gaps) > 0, (name, phase)
                trials.append([pair, mode, arm, phase, *timings(gaps)])
                pooled[mode, arm, phase].extend(gaps)
                for (i, frame, _), gap in zip(advancing[1:], gaps):
                    if gap > 100:
                        slow.append([pair, mode, arm, phase, i + 1, frame, round(gap, 3),
                                     round(int(frames[i]["effects_ns"]) / 1e6, 3), frames[i]["shadow_rays"]])
                if phase != "stopping":
                    continue
                assert all(r["inside_sanctuary"] == "true" for r in frames)
                def pose(row):
                    return tuple(float(row[k]) for k in ("x", "eye", "z", "yaw", "pitch"))
                holds = [list(g) for _, g in itertools.groupby(frames, key=pose)]
                final = holds[-1]
                before = [r for r in rows if r["phase"] == "settling"][-1]
                step = math.hypot(float(final[0]["x"]) - float(before["x"]), float(final[0]["z"]) - float(before["z"]))
                held = (int(final[-1]["time_ns"]) - int(final[0]["time_ns"])) / 1e9
                assert .1 < step < .5 and held >= 3 and len(final) >= 12, (name, step, held)
                sixth = len(frames) - len(final) + 5
                bounding = next(((b[2] - a[2]) / 1e6 for a, b in zip(advancing, advancing[1:]) if a[0] < sixth <= b[0]), None)
                stops.append([pair, mode, arm, round(step, 4), round(held, 3), len(final),
                              round(int(final[5]["effects_ns"]) / 1e6, 3),
                              round(bounding, 3) if bounding is not None else "",
                              round(max(int(r["effects_ns"]) for r in final) / 1e6, 3)])
        members = [f"{hashlib.sha256(archive.read(n)).hexdigest()}  {n}\n" for n in sorted(archive.namelist()) if not n.endswith("/")]
    write_csv(output / "pooled.csv", ["mode", "arm", "phase", *metrics],
              ([*key, *timings(gaps)] for key, gaps in sorted(pooled.items())))
    write_csv(output / "trials.csv", ["pair", "mode", "arm", "phase", *metrics], trials)
    write_csv(output / "stops.csv", ["pair", "mode", "arm", "step_m", "held_s", "held_frames",
              "sixth_effects_ms", "sixth_distinct_frame_gap_ms", "max_stable_effects_ms"], stops)
    write_csv(output / "over-100.csv", ["pair", "mode", "arm", "phase", "phase_frame_ordinal",
              "composited_gl_frame_id", "gap_ms", "ending_frame_effects_ms", "shadow_rays"], slow)
    (output / "raw-members.sha256").write_text("".join(members), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("raw_zip", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    analyze(args.raw_zip, args.output)
