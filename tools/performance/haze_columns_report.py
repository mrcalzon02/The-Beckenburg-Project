#!/usr/bin/env python3
"""Audit the private Core30 same-package haze-columns experiment; timings are diagnostic.

Reads original worker ZIPs without extracting them. Outputs per-trial and pooled
distinct-consumed-frame timings, callback attribution, CPU and stable-pose checks.
"""
import argparse
import csv
import hashlib
import io
import json
import math
from collections import defaultdict
from pathlib import Path
from zipfile import ZipFile

from frame_budget_report import summarize

PHASES = ("idle", "moving", "stopping")


def intervals(rows):
    """Keep repeated-frame callback work attached to the next distinct GL frame."""
    previous = None
    pending = []
    last_stamp = -1
    for row in rows:
        stamp, frame = int(row["time_ns"]), int(row["composited_gl_frame_id"])
        if stamp <= last_stamp:
            raise ValueError("Non-increasing completion timestamps")
        last_stamp = stamp
        if frame < 0:
            continue
        if previous is None:
            previous = row
            continue
        old_id = int(previous["composited_gl_frame_id"])
        if frame < old_id:
            raise ValueError("Regressed consumed GL frame ID")
        pending.append(row)
        if frame == old_id:
            continue
        yield {"from_id": old_id, "to_id": frame,
               "from_ns": int(previous["time_ns"]), "to_ns": stamp,
               "frame_ms": (stamp - int(previous["time_ns"])) / 1e6,
               "callbacks": len(pending),
               "post_geometry_ms": sum(int(r["effects_ns"]) for r in pending) / 1e6,
               "render_ms": sum(int(r["render_ns"]) for r in pending) / 1e6}
        previous, pending = row, []


def analyze(path, experiment="haze-columns"):
    def require(ok, message):
        if not ok:
            raise ValueError(message)
    pooled = defaultdict(list)
    trial_summaries, all_intervals, stops, cpus = [], [], [], []
    startup = []
    with ZipFile(path) as archive:
        def read(name):
            return archive.read(name).decode("utf-8-sig")
        def rows(name):
            return list(csv.DictReader(io.StringIO(read(name))))
        provenance = json.loads(read("provenance.json"))
        manifest = json.loads(read("compiled-overlays.json"))
        require(provenance["route"] == manifest["experiment"] == experiment, "Wrong route")
        require(provenance["backend"] == "gpu" and provenance["runner"] == "OLD-CORE30-homebrew", "Wrong hardware controller")
        require(provenance["revisions"]["candidate"] == manifest["candidate_sha"], "Candidate SHA mismatch")
        if experiment in ("haze-columns", "haze-warmup"):
            require(manifest["runtime"] == "same unmodified candidate GPU JAR for both arms", "Not a same-package comparison")
        elif experiment == "guarded-cubic":
            require(manifest["runtime"] == "one candidate GPU JAR with a shader overlay in candidate only"
                    and manifest["shader_overlay_lf_sha256"] == "b2cca0dba7c71efb88b84868ea61dfcb6cdd0e608eb327300476581d98b2fde8",
                    "Unexpected shader overlay")
        elif experiment == "effect-stages":
            require(manifest["runtime"] == "one candidate GPU JAR, test-only overlay in probe arm"
                    and manifest["instrumented_lf_sha256"] == {
                        "SceneRenderer.java": "4ae516db70fbe49dfbf8c37b1c7b9151f1b07d40237b7efe4953760236b81a3f",
                        "LanViewportPerformanceReview.java": "3a022e06d6c4a236ac87fe6c4ae443e2630abbb78e2e12382d0c7694baf7322e"},
                    "Unexpected stage diagnostic overlay")
        else:
            raise ValueError("Unsupported experiment")
        require(manifest["test_driver_lf_sha256"] == "264e0bfae9cb77664a57751d88e01300e216cf77c3da93d00445f9c3b224041e", "Wrong route driver")
        if experiment == "haze-warmup":
            require(manifest["priming_driver_lf_sha256"] == "7cd59187c0fb1c71230fe1a896e709b3fda8759e1ad03aac4a50e5317a17137a"
                    and manifest["draws"] == 12, "Unexpected warmup protocol")
        jar_sha = read("candidate-jar.sha256").strip()
        require(len(jar_sha) == 64 and all(c in "0123456789abcdef" for c in jar_sha), "Invalid package digest")
        expected = {f"pair-{pair}-{mode}-{arm}" for pair in range(1, 5)
                    for mode in ("solo", "tcp") for arm in ("baseline", "candidate")}
        actual = {name.split("/")[0] for name in archive.namelist() if name.startswith("pair-")}
        require(expected == actual, "Incomplete four-pair trial matrix")
        for arm in ("baseline", "candidate"):
            receipt = read(arm + "-full-round.txt")
            require(all(s in receipt for s in ("full multiplayer round: PASS", "both hunters received the winning settlement",
                                              "Post-round commands rejected")), "Incomplete TCP settlement")
            log = read(arm + "-e2e.log")
            require("exact horizontal projection samples" in log and "against unculled analytic dome" in log,
                    "Missing functional optical parity controls")
        for trial in sorted(expected):
            _, pair, mode, arm = trial.split("-")
            labels = {"trial": trial, "pair": int(pair), "mode": mode, "arm": arm}
            details = dict(line.split("=", 1) for line in read(trial + "/summary.txt").splitlines())
            require("NVIDIA" in details["renderer"] and "1660 SUPER" in details["renderer"], "Not the physical GTX")
            require(details["rendered_hunters"] == "1" and float(details["max_travel_metres"]) > .1, "No rendered movement")
            prop = "-Dbeckenburg.experimental.hazeColumns=" + str(experiment == "haze-columns" and arm == "candidate").lower()
            require(prop in read(trial + "/run.log"), "Wrong feature flag")
            if experiment == "haze-warmup":
                primed = arm == "candidate"
                require("-Dbeckenburg.profile.primeHaze=" + str(primed).lower() in read(trial + "/run.log"),
                        "Warmup flag mismatch")
                samples = rows(trial + "/priming.csv")
                require(len(samples) == 1, "Missing startup boundary")
                row = samples[0]
                draws, changed = int(row["draws"]), int(row["changed_pixels"])
                prime_ns, first_ns = int(row["prime_ns"]), int(row["main_to_first_view_callback_ns"])
                require(row["enabled"] == str(primed).lower() and draws == (12 if primed else 0)
                        and (changed > 0 if primed else changed == 0) and 0 <= prime_ns < first_ns,
                        "Invalid priming receipt")
                startup.append({**labels, "draws": draws, "changed_pixels": changed,
                                "prime_ms": prime_ns / 1e6, "main_to_first_view_callback_ms": first_ns / 1e6})
            frames = rows(trial + "/frames.csv")
            require(any(r["phase"] == "moving" and r["inside_sanctuary"] == "false" for r in frames), "Never left sanctuary")
            if mode == "tcp":
                require(details["other_tcp_peers"] == "2" and int(details["visible_peer_hunters"]) >= 1
                        and int(details["accepted_steps"]) > 0, "Missing real TCP peers/steps")
                require(bool(rows(trial + "/state.csv")), "Missing TCP arrival evidence")
            for phase in PHASES:
                selected = [r for r in frames if r["phase"] == phase]
                require(all((r["width"], r["height"]) == ("960", "540") for r in selected), "Changed viewport")
                values = list(intervals(selected))
                require(bool(values), "No advancing distinct frames in " + trial + "/" + phase)
                for value in values:
                    all_intervals.append({**labels, "phase": phase, **value})
                times = [r["frame_ms"] for r in values]
                trial_summaries.append({**labels, "phase": phase, **summarize(times)})
                pooled[mode, arm, phase].extend(times)
            held = [r for r in frames if r["phase"] == "stopping"]
            require(all(r["inside_sanctuary"] == "true" for r in held), "Stop outside sanctuary")
            pose_keys = ("x", "eye", "z", "yaw", "pitch")
            def pose(row): return tuple(row[k] for k in pose_keys)
            first = len(held) - 1
            while first > 0 and pose(held[first - 1]) == pose(held[-1]):
                first -= 1
            stable = held[first:]
            require(len(stable) >= 12 and int(stable[-1]["time_ns"]) - int(stable[0]["time_ns"]) >= 3_000_000_000,
                    "No three-second identical-pose hold")
            before = [r for r in frames if r["phase"] == "settling"][-1]
            travel = math.hypot(float(stable[-1]["x"]) - float(before["x"]), float(stable[-1]["z"]) - float(before["z"]))
            require(.1 < travel < .5, "Not one small step before stopping")
            stops.append({**labels, "step_metres": travel, "identical_callbacks": len(stable),
                          "sixth_render_ms": int(stable[5]["render_ns"]) / 1e6,
                          "sixth_post_geometry_ms": int(stable[5]["effects_ns"]) / 1e6})
            cpu = rows(trial + "/phase-cpu.csv")
            require({r["phase"] for r in cpu} == set(PHASES), "Missing phase CPU boundaries")
            for row in cpu:
                wall = int(row["end_wall_ns"]) - int(row["start_wall_ns"])
                used = int(row["end_process_cpu_ns"]) - int(row["start_process_cpu_ns"])
                require(wall > 0 and used >= 0 and used == int(row["process_cpu_ns"]), "Invalid process CPU boundary")
                cpus.append({**labels, "phase": row["phase"], "wall_ns": wall, "process_cpu_ns": used,
                             "core_equivalents": used / wall})
    if startup and len({s["changed_pixels"] for s in startup if s["arm"] == "candidate"}) != 1:
        raise ValueError("Warmup workload changed between trials")
    return {**({"startup": startup} if startup else {}), "release_gate": False, "archive_sha256": hashlib.sha256(Path(path).read_bytes()).hexdigest(),
            "provenance": provenance, "manifest": manifest, "candidate_jar_sha256": jar_sha,
            "pooled": [{"mode": mode, "arm": arm, "phase": phase, **summarize(values)}
                       for (mode, arm, phase), values in sorted(pooled.items())],
            "trials": trial_summaries, "intervals": all_intervals, "stops": stops, "cpu": cpus,
            "limits": "One offscreen consumed viewport and two loopback peers; not monitor presentation or cross-PC latency."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive")
    parser.add_argument("output")
    parser.add_argument("--summary-only", action="store_true", help="keep summaries and >50ms hitches; original CSVs remain in the input archive")
    parser.add_argument("--experiment", choices=("haze-columns", "haze-warmup", "guarded-cubic"), default="haze-columns")
    args = parser.parse_args()
    report = analyze(args.archive, args.experiment)
    if args.summary_only:
        values = report.pop("intervals")
        report["distinct_frame_intervals"] = len(values)
        report["hitches_over_50_ms"] = [row for row in values if row["frame_ms"] > 50]
    Path(args.output).write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    for row in report["pooled"]:
        print(row)


if __name__ == "__main__":
    main()
