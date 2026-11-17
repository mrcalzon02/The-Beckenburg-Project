#!/usr/bin/env python3
"""Non-gating frame-time report for already de-duplicated frame interval CSVs.

Input must contain frame_ms (or --column NAME); optional mode, phase, trial.
Rows must represent intervals between distinct displayed frame IDs, not render
callbacks or repeated readbacks. This script never declares a release failure.

Example:
  python3 tools/performance/frame_budget_report.py frames.csv
  python3 tools/performance/frame_budget_report.py frames.csv --column gap_ms --json report.json
"""
import argparse
import csv
import json
import math
from collections import defaultdict
from pathlib import Path


def percentile(values, pct):
    """Nearest-rank percentile, matching the physical House reports."""
    return sorted(values)[max(0, math.ceil(len(values) * pct / 100) - 1)]


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


def analyze(paths, column):
    groups = defaultdict(list)
    for path in paths:
        with Path(path).open(newline="", encoding="utf-8-sig") as stream:
            reader = csv.DictReader(stream)
            if not reader.fieldnames or column not in reader.fieldnames:
                raise ValueError(f"{path}: missing required column {column!r}")
            for line, row in enumerate(reader, start=2):
                raw = row.get(column, "")
                try:
                    value = float(raw)
                except (TypeError, ValueError):
                    raise ValueError(f"{path}:{line}: invalid interval {raw!r}") from None
                if not math.isfinite(value) or value <= 0:
                    raise ValueError(f"{path}:{line}: interval must be finite and positive")
                labels = tuple((row.get(name) or "unspecified").strip() for name in ("mode", "phase"))
                groups[labels].append(value)
    if not groups:
        raise ValueError("no frame intervals found")
    return [
        {"mode": mode, "phase": phase, **summarize(values)}
        for (mode, phase), values in sorted(groups.items())
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", nargs="+", help="CSV files of distinct-frame intervals")
    parser.add_argument("--column", default="frame_ms", help="interval column (default: frame_ms)")
    parser.add_argument("--json", dest="json_path", help="optional JSON report output path")
    args = parser.parse_args()
    try:
        rows = analyze(args.csv, args.column)
    except (OSError, ValueError, csv.Error) as exc:
        parser.error(str(exc))
    print("MODE | PHASE | N | p50 ms | p95 ms | p99 ms | MAX ms | >33.3 | >50 | >100")
    for r in rows:
        print(f"{r['mode']} | {r['phase']} | {r['intervals']} | "
              f"{r['p50_ms']:.3f} | {r['p95_ms']:.3f} | {r['p99_ms']:.3f} | "
              f"{r['max_ms']:.3f} | {r['over_33_3_ms']} | {r['over_50_ms']} | {r['over_100_ms']}")
    if args.json_path:
        Path(args.json_path).write_text(
            json.dumps({"schema": 1, "unit": "ms", "percentile": "nearest-rank",
                        "release_gate": False, "groups": rows}, indent=2) + "\n",
            encoding="utf-8")


if __name__ == "__main__":
    main()
