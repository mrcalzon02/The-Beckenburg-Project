"""Summarize completed-frame CSVs without averaging trial percentiles.

Usage: python tools/summarize_world_performance.py EVIDENCE_DIRECTORY
The directory contains old/, before/ and after/ trial trees, each with frames.csv.
Only raw completed-frame samples contribute; no timings are inferred from FPS.
"""
import csv
import json
import math
import statistics
import sys
from collections import defaultdict
from pathlib import Path


def percentile(values, fraction):
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * fraction) - 1)]


def main(root):
    summaries = []
    for variant in ("old", "before", "after"):
        groups = defaultdict(list)
        trials = defaultdict(lambda: defaultdict(list))
        for path in sorted((root / variant).rglob("frames.csv")):
            with path.open(encoding="utf-8-sig", newline="") as source:
                for row in csv.DictReader(source):
                    key = row["site"], row["view"]
                    groups[key].append(row)
                    trials[key][str(path)].append(float(row["total_ms"]))
        for (site, view), rows in groups.items():
            values = lambda name: [float(row[name]) for row in rows]
            medians = [statistics.median(v) for v in trials[site, view].values()]
            summaries.append({
                "variant": variant, "site": site, "view": view,
                "trials": len(medians), "frames": len(rows),
                "total_p50_ms": percentile(values("total_ms"), .50),
                "total_p95_ms": percentile(values("total_ms"), .95),
                "total_p99_ms": percentile(values("total_ms"), .99),
                "cpu_p50_ms": percentile(values("cpu_ms"), .50),
                "cpu_p95_ms": percentile(values("cpu_ms"), .95),
                "trial_median_min_ms": min(medians),
                "trial_median_max_ms": max(medians),
                "allocated_p50_mib": percentile(values("allocated_bytes"), .50) / 1048576,
                "uploaded_p50_bytes": percentile(values("uploaded_bytes"), .50),
                "uploaded_p95_bytes": percentile(values("uploaded_bytes"), .95),
                "shadow_rays_p95": percentile(values("shadow_rays"), .95),
                "gc_total_ms": sum(values("gc_ms")),
            })
            for column in ("color_sync_ms", "depth_sync_ms", "cpu_conversion_ms"):
                observed = values(column) if all(column in row for row in rows) else []
                summaries[-1][column.replace("_ms", "_p50_ms")] = (
                    percentile(observed, .50) if observed and min(observed) > 0 else None)
    if not summaries:
        raise SystemExit("No completed-frame CSV samples found")
    with (root / "summary.csv").open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, summaries[0].keys())
        writer.writeheader()
        writer.writerows(summaries)
    (root / "summary.json").write_text(json.dumps(summaries, indent=2) + "\n", encoding="utf-8")
    index = {(s["variant"], s["site"], s["view"]): s for s in summaries}
    lines = ["| Site / view | Before p50 / p95 / p99 ms | Fixed p50 / p95 / p99 ms | Median reduction |",
             "|---|---:|---:|---:|"]
    for result in summaries:
        if result["variant"] != "after":
            continue
        before = index.get(("before", result["site"], result["view"]))
        if before is None:
            continue
        timing = lambda row: " / ".join(f"{row['total_p'+p+'_ms']:.2f}" for p in ("50", "95", "99"))
        reduction = 100 * (1 - result["total_p50_ms"] / before["total_p50_ms"])
        lines.append(f"| {result['site']} / {result['view']} | {timing(before)} | {timing(result)} | {reduction:.1f}% |")
    (root / "comparison.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    phases = ["| Site / view | Before color / depth / CPU conversion ms | After color / depth / CPU conversion ms |",
              "|---|---:|---:|"]
    columns = ("color_sync_p50_ms", "depth_sync_p50_ms", "cpu_conversion_p50_ms")
    for result in summaries:
        before = index.get(("before", result["site"], result["view"]))
        if result["variant"] != "after" or before is None:
            continue
        if any(row[column] is None for row in (before, result) for column in columns):
            continue
        timing = lambda row: " / ".join(f"{row[column]:.3f}" for column in columns)
        phases.append(f"| {result['site']} / {result['view']} | {timing(before)} | {timing(result)} |")
    if len(phases) > 2:
        (root / "readback-phases.md").write_text("\n".join(phases) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main(Path(sys.argv[1]))
