#!/usr/bin/env python3
"""Attribute long distinct GL frame intervals to instrumented House render stages."""
import argparse
import csv
import io
import json
from collections import defaultdict
from pathlib import Path
from zipfile import ZipFile

from haze_columns_report import analyze

STAGES = ("sprites", "distance", "weather", "sanctuary", "hiding", "psx", "graphics", "folio")


def stage_rows(archive, trial, frames):
    data = archive.read(trial + "/stages.csv").decode("utf-8-sig")
    rows = list(csv.DictReader(io.StringIO(data)))
    if len(rows) != len(frames):
        raise ValueError("Missing stage callbacks for " + trial)
    by_phase = defaultdict(list)
    for row, frame in zip(rows, frames):
        if (row["time_ns"], row["phase"]) != (frame["time_ns"], frame["phase"]):
            raise ValueError("Mismatched callback stamp or phase for " + trial)
        if row["phase"] not in ("idle", "moving", "stopping"):
            continue
        costs = {s: (int(row[s + "_wall_ns"]), int(row[s + "_edt_cpu_ns"])) for s in STAGES}
        if any(min(pair) < 0 for pair in costs.values()) or any(int(row[key]) < 0 for key in ("gc_count_delta", "compiler_ms_delta")):
            raise ValueError("Invalid stage clock")
        wall = sum(pair[0] for pair in costs.values())
        if not 0 < wall <= int(frame["effects_ns"]):
            raise ValueError("Stage times do not fit the production effects timer")
        by_phase[row["phase"]].append((int(row["time_ns"]), row, costs))
    if not any(sum(cpu for _, cpu in costs.values()) > 0 for phase in by_phase.values() for _, _, costs in phase):
        raise ValueError("No advancing render-thread CPU clock in " + trial)
    return by_phase


def report(path):
    study = analyze(path, "effect-stages")
    intervals = study.pop("intervals")
    study["distinct_intervals"] = len(intervals)
    hitches = defaultdict(list)
    for interval in intervals:
        if interval["frame_ms"] > 50:
            hitches[interval["trial"]].append(interval)
    detail = []
    with ZipFile(path) as archive:
        for stop in study["stops"]:
            trial = stop["trial"]
            has_clocks = trial + "/stages.csv" in archive.namelist()
            if has_clocks != (stop["arm"] == "candidate"):
                raise ValueError("Clock/control arm mismatch")
            frames = list(csv.DictReader(io.StringIO(archive.read(trial + "/frames.csv").decode("utf-8-sig"))))
            by_phase = stage_rows(archive, trial, frames) if has_clocks else {}
            for gap in hitches[trial]:
                phase = gap["phase"]
                start, end = gap["from_ns"], gap["to_ns"]
                phase_rows = [row for row in frames if row["phase"] == phase]
                gap_rows = [row for row in phase_rows if start < int(row["time_ns"]) <= end]
                if len(gap_rows) != gap["callbacks"]:
                    raise ValueError("Wrong number of callbacks in gap")
                first = int(phase_rows[0]["time_ns"])
                result = {**gap, "seconds_from_first_phase_callback": (end-first)/1e9,
                          "inside_at_end": gap_rows[-1]["inside_sanctuary"] == "true",
                          "shadow_rays": sum(int(row["shadow_rays"]) for row in gap_rows)}
                if has_clocks:
                    samples = [(row, costs) for stamp, row, costs in by_phase[phase] if start < stamp <= end]
                    result["wall_ms"] = {s: sum(costs[s][0] for _, costs in samples)/1e6 for s in STAGES}
                    result["edt_cpu_ms"] = {s: sum(costs[s][1] for _, costs in samples)/1e6 for s in STAGES}
                    for field in ("gc_count_delta", "compiler_ms_delta"):
                        result[field] = sum(int(row[field]) for row, _ in samples)
                detail.append(result)
    study["hitches_over_50ms"] = detail
    return study


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.write_text(json.dumps(report(args.archive), indent=2) + "\n")
