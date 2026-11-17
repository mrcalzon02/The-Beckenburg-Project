#!/usr/bin/env python3
"""Audit renderer callbacks against distinct consumed OpenGL-frame intervals.

Input CSV columns: trial, phase, completion_ns, composited_gl_frame_id,
post_geometry_ns, and optional mode. Rows are ordered within a trial.
Output represents offscreen frame consumption, not monitor scanout latency.
"""
import argparse
import csv
import json
from collections import defaultdict
from pathlib import Path

REQUIRED = {"trial", "phase", "completion_ns", "composited_gl_frame_id", "post_geometry_ns"}


def analyze(paths):
    groups = defaultdict(list)
    for path in paths:
        with Path(path).open(newline="", encoding="utf-8-sig") as stream:
            reader = csv.DictReader(stream)
            missing = REQUIRED - set(reader.fieldnames or [])
            if missing:
                raise ValueError(f"{path}: missing columns: {', '.join(sorted(missing))}")
            for lineno, row in enumerate(reader, 2):
                try:
                    t = int(row["completion_ns"])
                    fid = int(row["composited_gl_frame_id"])
                    post = int(row["post_geometry_ns"])
                except (TypeError, ValueError):
                    raise ValueError(f"{path}:{lineno}: invalid integer field") from None
                if t < 0 or post < 0:
                    raise ValueError(f"{path}:{lineno}: negative timestamp or stage duration")
                trial = row["trial"].strip()
                phase = row["phase"].strip()
                if not trial or not phase:
                    raise ValueError(f"{path}:{lineno}: blank trial or phase")
                key = (str(path), (row.get("mode") or "unspecified").strip(), trial, phase)
                groups[key].append((t, fid, post, lineno))
    output = []
    for (path, mode, trial, phase), rows in sorted(groups.items()):
        prev_t = None
        prev_fid = None
        last_t = -1
        pending_ns = pending_callbacks = 0
        for t, fid, post, line in rows:
            if t <= last_t:
                raise ValueError(f"{path}:{line}: timestamps must strictly increase per trial/phase")
            last_t = t
            if fid < 0:
                continue
            if prev_fid is None:
                prev_fid, prev_t = fid, t
                continue
            if fid < prev_fid:
                raise ValueError(f"{path}:{line}: GL frame ID regressed within trial/phase")
            pending_ns += post
            pending_callbacks += 1
            if fid == prev_fid:
                continue
            gap = t - prev_t
            output.append({
                "source": path, "mode": mode, "trial": trial, "phase": phase,
                "from_id": prev_fid, "to_id": fid,
                "gap_ms": round(gap / 1e6, 6),
                "callbacks": pending_callbacks,
                "post_geometry_ms": round(pending_ns / 1e6, 6),
                "over_50_ms": gap > 50_000_000,
                "over_100_ms": gap > 100_000_000,
            })
            prev_fid, prev_t = fid, t
            pending_ns = pending_callbacks = 0
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", nargs="+", help="timestamped per-callback CSV files")
    parser.add_argument("--json", help="write report to file instead of stdout")
    args = parser.parse_args()
    try:
        data = analyze(args.csv)
    except (OSError, ValueError, csv.Error) as error:
        parser.error(str(error))
    payload = json.dumps(data, indent=2) + "\n"
    if args.json:
        Path(args.json).write_text(payload, encoding="utf-8")
    else:
        print(payload, end="")


if __name__ == "__main__":
    main()
