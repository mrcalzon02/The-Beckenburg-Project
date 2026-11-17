#!/usr/bin/env python3
"""Diagnostic (never release-gating) frame-time summaries for distinct-frame intervals.

Input CSV: frame_ms by default (or --column); optional mode, phase, trial.
Use one row per interval between DISTINCT consumed GL frame IDs. Repeated
readbacks and phase-transition intervals must be excluded by the collector.
Pooling is weighted by intervals, so also inspect --per-trial results.
"""
import argparse
import csv
import json
import math
from collections import defaultdict
from pathlib import Path


def percentile(values, pct):
    """Nearest-rank percentile used by the House physical benchmarks."""
    if not values:
        raise ValueError("cannot calculate percentile of an empty sample")
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * pct / 100) - 1)]


def summarize(values):
    return {
        "intervals": len(values),
        "p50_ms": round(percentile(values, 50), 3),
        "p95_ms": round(percentile(values, 95), 3),
        "p99_ms": round(percentile(values, 99), 3),
        "max_ms": round(max(values), 3),
        "over_33_3_ms": sum(v > 33.3 for v in values),
        "over_50_ms": sum(v > 50 for v in values),
        "over_100_ms": sum(v > 100 for v in values),
    }


def analyze(paths, column="frame_ms", per_trial=False):
    groups = defaultdict(list)
    for path in paths:
        with Path(path).open(newline="", encoding="utf-8-sig") as stream:
            reader = csv.DictReader(stream)
            if not reader.fieldnames or column not in reader.fieldnames:
                raise ValueError(f"{path}: missing required column {column!r}")
            if per_trial and "trial" not in reader.fieldnames:
                raise ValueError(f"{path}: --per-trial requires a trial column")
            for line, row in enumerate(reader, start=2):
                raw = row.get(column, "")
                try:
                    value = float(raw)
                except (TypeError, ValueError):
                    raise ValueError(f"{path}:{line}: invalid interval {raw!r}") from None
                if not math.isfinite(value) or value <= 0:
                    raise ValueError(f"{path}:{line}: interval must be finite and positive")
                labels = [(row.get(name) or "unspecified").strip() or "unspecified"
                          for name in ("mode", "phase")]
                if per_trial:
                    trial = (row.get("trial") or "").strip()
                    if not trial:
                        raise ValueError(f"{path}:{line}: blank trial ID")
                    # Do not merge repeated trial IDs from independent files.
                    labels += [str(Path(path)), trial]
                groups[tuple(labels)].append(value)
    if not groups:
        raise ValueError("no frame intervals found")
    keys = ("mode", "phase", "source", "trial") if per_trial else ("mode", "phase")
    return [{**dict(zip(keys, labels)), **summarize(values)}
            for labels, values in sorted(groups.items())]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", nargs="+", help="CSV files with distinct-frame intervals")
    parser.add_argument("--column", default="frame_ms", help="interval column")
    parser.add_argument("--per-trial", action="store_true",
                        help="report each trial separately to expose pooled-percentile masking")
    parser.add_argument("--json", dest="json_path", help="optional JSON report path")
    args = parser.parse_args()
    try:
        rows = analyze(args.csv, args.column, args.per_trial)
    except (OSError, ValueError, csv.Error) as exc:
        parser.error(str(exc))
    print("MODE | PHASE | TRIAL | N | p50 ms | p95 ms | p99 ms | MAX ms | >33.3 | >50 | >100")
    for r in rows:
        print(f"{r['mode']} | {r['phase']} | {r.get('trial', 'pooled')} | {r['intervals']} | "
              f"{r['p50_ms']:.3f} | {r['p95_ms']:.3f} | {r['p99_ms']:.3f} | "
              f"{r['max_ms']:.3f} | {r['over_33_3_ms']} | {r['over_50_ms']} | {r['over_100_ms']}")
    if args.json_path:
        Path(args.json_path).write_text(
            json.dumps({"schema": 2, "unit": "ms", "percentile": "nearest-rank",
                        "release_gate": False, "per_trial": args.per_trial, "groups": rows},
                       indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
