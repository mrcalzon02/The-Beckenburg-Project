"""Verify the physical House door-2 CPU archive and compute statistics directly from its CSVs."""
import argparse
import csv
import hashlib
import io
import json
import math
import re
from pathlib import Path
from zipfile import ZipFile

METRICS = ("end_to_end_ns", "render_thread_cpu_ns", "gl_worker_ns", "gl_issue_ns")
PARITY = ("argb_sha256", "depth_bits_sha256", "vertices_uploaded_bytes", "uploaded_batches")
ARMS = ("baseline", "candidate")
CAMERAS = ("standing", "step")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def timings(rows):
    result = {}
    for field in METRICS:
        values = sorted(int(row[field]) for row in rows)
        result[field] = {**{f"p{p}": values[math.ceil(len(values) * p / 100) - 1] / 1e6
                           for p in (50, 95, 99)}, "over100": sum(v > 100000000 for v in values),
                         "n": len(values)}
    return result


def groups(rows):
    toggles = [r for r in rows if r["phase"] == "toggle"]
    return {"all_toggles": toggles, "first_toggle": [toggles[0]], "later_toggles": toggles[1:],
            "toggle_11": [toggles[10]], "post_step_toggles": toggles[10:],
            **{f"swing_{sub}": [r for r in rows if r["subframe"] == str(sub)] for sub in range(1, 5)},
            "swing": [r for r in rows if r["phase"] == "swing"],
            "settled": [r for r in rows if r["phase"] == "settled"]}


def parity(a, b, lattice):
    result = {}
    for x, y in zip(a, b, strict=True):
        group = "first_toggle" if x["phase"] == "toggle" and x["toggle"] == "0" else x["phase"]
        out = result.setdefault(group, {"rows": 0, **{k: 0 for k in PARITY},
                                      "comparable_depth_samples": 0, "comparable_depth_differences": 0})
        out["rows"] += 1
        for k in PARITY:
            out[k] += x[k] != y[k]
        endpoint = float(x["door_open"] == "true")
        stable = (lattice or x["phase"] in ("warmup","settled")
                  or float(x["fraction"]) == float(y["fraction"]) == endpoint)
        out["comparable_depth_samples"] += stable
        out["comparable_depth_differences"] += stable and x["depth_bits_sha256"] != y["depth_bits_sha256"]
    require(all(r["comparable_depth_differences"] == 0 for r in result.values()), "Raw depth differs at comparable poses")
    if lattice:
        require(all(r[k] == 0 for r in result.values() for k in PARITY), "Lattice ARGB, depth or transfer mismatch")
    return result


def rows(archive, name, camera, arm, lattice):
    def read_csv(file):
        return list(csv.DictReader(io.StringIO(archive.read(f"{name}/{file}").decode("utf-8-sig"))))
    gl = archive.read(f"{name}/run.log").decode("utf-8-sig")
    require(any(line.startswith("OpenGL NVIDIA GeForce GTX 1660 SUPER") and
                line.endswith("; doors=2,3 window=19") for line in gl.splitlines()), f"GPU identity: {name}")
    data, scope = read_csv("door-frames.csv"), read_csv("scope.csv")
    step = camera == "step"
    width, length = (17, 144) if lattice else (6, 152)
    require(len(data) == len(scope) == length, f"Capture length: {name}")
    for i, (frame, ctx) in enumerate(zip(data, scope)):
        group, sub = divmod(i - 8, width) if i >= 8 else (-1, -1)
        phase = "warmup" if i < 8 else "toggle" if sub == 0 else "settled" if sub == width - 1 else "swing"
        opened = group >= 0 and group % 2 == 0
        stepped = step and group >= (4 if lattice else 10)
        require(frame["frame"] == ctx["frame"] == str(i) and frame["phase"] == ctx["phase"] == phase
                and ctx["toggle"] == str(group) and ctx["subframe"] == str(sub), f"Phase sequence: {name}/{i}")
        seconds = i * .1 if not lattice or i < 8 else .8 + .6 * group + .45 * sub / 16
        require(math.isclose(float(ctx["seconds"]), seconds, rel_tol=0, abs_tol=1e-12)
                and int(ctx["room_tick"]) == (math.floor(seconds * 10) if lattice else i)
                and int(ctx["factor_epoch"]) >= 1 and 0 <= float(ctx["fraction"]) <= 1,
                f"Lighting clock or motion: {name}/{i}")
        require(all(re.fullmatch(r"[a-f0-9]{64}", frame[k]) for k in PARITY[:2])
                and all(int(frame[k]) >= 0 for k in (*METRICS, *PARITY[2:])), f"Raw result: {name}/{i}")
        require(float(ctx["x"]) == 19.4 and float(ctx["z"]) == (22.31 if stepped else 22.0)
                and frame["door_b_open"] == frame["window_open"] == "false"
                and frame["door_open"] == str(opened).lower(), f"Scene state: {name}/{i}")
        require(not lattice or i < 8 or float(ctx["fraction"]) == (sub if opened else 16 - sub) / 16,
                f"Incomplete native pose lattice: {name}/{i}")
        require(phase != "settled" or float(ctx["fraction"]) == float(opened), f"Unsettled door: {name}/{i}")
        require(int(ctx["restored_vertices"]) >= 0 and (arm == "candidate" or int(ctx["restored_vertices"]) == 0)
                and (i == 0 or int(ctx["restored_vertices"]) >= int(scope[i-1]["restored_vertices"])), f"Vertex counter: {name}/{i}")
        frame.update(ctx)
    return data


def analyze(path, manifest):
    hashes = dict(line.split("  ", 1)[::-1] for line in manifest.read_text().splitlines())
    require(hashlib.sha256(path.read_bytes()).hexdigest() == hashes.pop(path.name), "Original archive SHA-256 mismatch")
    require(hashlib.sha256(path.read_bytes()).hexdigest() == "dfd62935083b1a0f1c31c7ffd34d9c27e4663bf7d8672a0c35b3d93dac6cef0b",
            "Unexpected original archive for Core30 run 44003061335")
    result = {"pairs": {}, "pool": {}, "controls": {}, "parity": {}, "lattice": {}, "transfers": {},
              "restored": {}, "first": {}, "receipts": {}, "over100_toggle_ordinals": {}}
    with ZipFile(path) as archive:
        names = archive.namelist()
        require(len(names) == len(hashes) and set(names) == set(hashes), "Archive member list")
        for name, digest in hashes.items():
            require(hashlib.sha256(archive.read(name)).hexdigest() == digest, "Member SHA-256: " + name)
        provenance = json.loads(archive.read("provenance.json"))
        result["provenance"] = provenance
        require(provenance["route"] == "door-2-cpu" and provenance["backend"] == "gpu"
                and provenance["runner"].lower() == "old-core30-homebrew", "Core30 route/backend")
        require(provenance["controller_sha"] == "83a03869c94f04ff7c0717085a6729922ad81371"
                and provenance["revisions"]["candidate"] == "5d7c933b7d49e98f9dd9484d1049b98781fba699",
                "Immutable controller or game revision")
        compiled = json.loads(archive.read("door-cpu-result.json"))
        require(compiled["archive_sha256"] == "38a6fbc17f88b4dc601430a2e1f610b576befbc837e823a5c0edca0a420bd81c"
                and not compiled["failures"] and len(compiled["overlay_classes_sha256"]) >= 2, "Reviewed overlay / worker parity")
        result["package"] = archive.read("candidate-jar.sha256").decode().strip()
        result["environment"] = archive.read("environment.log").decode("utf-8-sig",errors="replace")
        order = [f"door-cpu-lattice-{camera}-{arm}" for camera in CAMERAS
                 for arm in (ARMS if camera == "standing" else tuple(reversed(ARMS)))]
        for pair in range(4):
            for camera in (CAMERAS if pair % 2 == 0 else tuple(reversed(CAMERAS))):
                arms = ARMS if (pair + int(camera == "step")) % 2 == 0 else tuple(reversed(ARMS))
                order.extend(f"door-cpu-pair-{pair+1}-{camera}-{arm}" for arm in arms)
        require(compiled["order"] == order and set(order) == set(compiled["trials"]), "Fresh process/arm order mismatch")
        for arm in ARMS:
            receipt = archive.read(arm + "-full-round.txt").decode()
            for criterion in ("Beckenburg full multiplayer round: PASS", "hunter owner, remote hunter and human ghost joined",
                              "All evidence tools measured and shared", "All three seats received ABSOLVED",
                              "both hunters received the winning settlement", "Post-round commands rejected"):
                require(criterion in receipt, arm + " settlement: " + criterion)
            result["receipts"][arm] = receipt
        for camera in CAMERAS:
            lattice = [rows(archive, f"door-cpu-lattice-{camera}-{arm}", camera, arm, True) for arm in ARMS]
            result["lattice"][camera] = parity(*lattice, True)
            first = {}
            pool = {arm: {} for arm in ARMS}
            for pair in range(1,5):
                case = f"door-cpu-pair-{pair}-{camera}"
                captures = {arm: rows(archive, f"{case}-{arm}", camera, arm, False) for arm in ARMS}
                result["parity"][case] = parity(*captures.values(), False)
                for arm, capture in captures.items():
                    name = case + "-" + arm
                    result["first"][name] = {r: int(capture[8][r]) / 1e6 for r in METRICS}
                    result["restored"][name] = int(capture[-1]["restored_vertices"])
                    result["over100_toggle_ordinals"][name] = [int(r["toggle"]) + 1 for r in capture
                                                              if r["phase"] == "toggle" and int(r["end_to_end_ns"]) > 100000000]
                    buckets = groups(capture)
                    result["pairs"][name] = {phase: timings(samples) for phase,samples in buckets.items()}
                    for phase,samples in buckets.items():
                        pool[arm].setdefault(phase, []).extend(samples)
                    if pair == 1:
                        first[arm] = capture
                    else:
                        result["controls"][name] = parity(first[arm], capture, False)
            for arm in ARMS:
                name = f"{camera}-{arm}"
                result["pool"][name] = {phase: timings(samples) for phase,samples in pool[arm].items()}
                result["transfers"][name] = {}
                for phase, samples in pool[arm].items():
                    by_field = {}
                    for k in PARITY[2:]:
                        values = sorted(int(r[k]) for r in samples)
                        by_field[k] = {"total": sum(values), "min": values[0], "p50": values[math.ceil(len(values)*.5)-1],
                                       "max": values[-1]}
                    result["transfers"][name][phase] = by_field
    return result


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("archive", nargs="?", type=Path, default=Path(__file__).with_name("physical-door-cpu.zip"))
    cli.add_argument("--manifest", type=Path)
    cli.add_argument("--json", type=Path)
    args = cli.parse_args()
    data = analyze(args.archive, args.manifest or args.archive.with_name("physical-door-cpu.sha256"))
    payload = json.dumps(data, indent=2)
    if args.json:
        args.json.write_text(payload + "\n", encoding="utf-8")
    else:
        print(payload)
