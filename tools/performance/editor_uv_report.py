#!/usr/bin/env python3
"""Recompute UV canvas repaint/invalidation measurements and require whole-image parity.

Each input directory is one fresh JVM run of EditorUvPaintProfile. This is a
headless Java2D diagnostic, not a Windows display or whole-editor FPS benchmark.
"""
import argparse
import csv
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path


def read(path):
    with Path(path).open(newline="", encoding="utf-8-sig") as stream:
        return list(csv.DictReader(stream))


def summarize(values):
    ordered = sorted(values)
    return {"samples": len(values), **{f"p{p}_ms": round(ordered[math.ceil(len(ordered) * p / 100) - 1] / 1e6, 6)
            for p in (50, 95, 99)}}


def analyze(baselines, candidates):
    if len(baselines) != len(candidates) or not baselines:
        raise ValueError("Expected matched fresh-JVM trial counts")
    groups = defaultdict(list)
    parity = 0
    trials = []
    for trial, (baseline, candidate) in enumerate(zip(baselines, candidates), 1):
        for filename, metric in (("paint.csv", "unchanged"), ("invalidated.csv", "selection-invalidated")):
            arms = {"baseline": read(Path(baseline) / filename), "candidate": read(Path(candidate) / filename)}
            key = "sample" if metric == "unchanged" else "operation"
            def fingerprints(rows): return [(r["case"], r[key], r["argb_sha256"]) for r in rows]
            if not arms["baseline"] or fingerprints(arms["baseline"]) != fingerprints(arms["candidate"]):
                raise ValueError(f"Missing or mismatched {metric} full-image parity in trial {trial}")
            parity += len(arms["baseline"])
            for arm, rows in arms.items():
                local = defaultdict(list)
                for row in rows:
                    if int(row["faces"]) != 120000 or any(int(row[k]) < 0 for k in ("wall_ns", "cpu_ns", "allocated_bytes")):
                        raise ValueError("Invalid source size/timing/allocation counter")
                    groups[arm, row["case"], metric].append(row)
                    local[row["case"]].append(row)
                for case, samples in local.items():
                    trials.append({"trial": trial, "arm": arm, "case": case, "metric": metric,
                                   **summarize([int(r["wall_ns"]) for r in samples])})
    summaries = []
    for (arm, case, metric), rows in sorted(groups.items()):
        summaries.append({"arm": arm, "case": case, "metric": metric,
                          **summarize([int(r["wall_ns"]) for r in rows]),
                          "median_cpu_ms": round(statistics.median(int(r["cpu_ns"]) for r in rows) / 1e6, 6),
                          "median_allocated_bytes": int(statistics.median(int(r["allocated_bytes"]) for r in rows))})
    return {"release_gate": False, "paired_full_image_comparisons": parity, "groups": summaries, "trials": trials,
            "limits": "520x500 headless ARGB Java2D canvas. 24 warm samples per case across two JVMs; p99 is the maximum, not a long-run guarantee. Invalidations reported separately."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", nargs="+", required=True)
    parser.add_argument("--candidate", nargs="+", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    report = analyze(args.baseline, args.candidate)
    Path(args.out).write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    for row in report["groups"]:
        print(row)
    print("PASS", report["paired_full_image_comparisons"], "paired full-image hashes")


if __name__ == "__main__":
    main()
