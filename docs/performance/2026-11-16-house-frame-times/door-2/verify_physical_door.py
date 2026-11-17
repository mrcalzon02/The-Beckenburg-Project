"""Check the physical four-pair House door 2 frame archive and summarize timing."""
import csv
import io
import hashlib
import json
import math
from pathlib import Path
import sys
from zipfile import ZipFile


def percentile(values, fraction):
    return sorted(values)[math.ceil(len(values) * fraction) - 1]


def verify(path):
    arms = ("baseline", "rgb")
    samples = {arm: [] for arm in arms}
    paired = {arm: [] for arm in arms}
    identity = ("frame", "phase", "door_open", "door_b_open", "window_open",
                "argb_sha256", "depth_bits_sha256")
    reference = None
    assert hashlib.sha256(path.read_bytes()).hexdigest() == "ef92f74a798eb6b043cb3bcd0742665179d74588c8f786a56ca7ea894e194283"
    manifest = path.with_name("physical-door-members.sha256").read_text().splitlines()
    with ZipFile(path) as archive:
        expected = dict(line.split("  ", 1)[::-1] for line in manifest)
        assert set(archive.namelist()) == set(expected)
        for member, digest in expected.items():
            assert hashlib.sha256(archive.read(member)).hexdigest() == digest, member
        provenance = json.loads(archive.read("provenance.json"))
        print(json.dumps(provenance, sort_keys=True))
        assert provenance["route"] == "door-2" and provenance["backend"] == "gpu"
        assert provenance["controller_sha"] == "8688d9e3659b190b9fb6b92d8a319e1099225cac"
        assert provenance["runner"].lower() == "old-core30-homebrew"
        for pair in range(1, 5):
            toggle_rows = {}
            for arm in arms:
                prefix = f"door-{pair}-{arm}/"
                assert b"1660 SUPER" in archive.read(prefix + "run.log")
                with archive.open(prefix + "door-frames.csv") as raw:
                    rows = list(csv.DictReader(io.TextIOWrapper(raw, encoding="utf-8-sig")))
                assert len(rows) == 58
                current = [tuple(row[field] for field in identity) for row in rows]
                if reference is None:
                    reference = current
                assert current == reference, (pair, arm, "different color, depth or scene")
                toggles = [row for row in rows if row["phase"] == "toggle"]
                assert len(toggles) == 24
                assert all(int(row["uploaded_batches"]) == expected for row, expected in
                           zip(toggles, [20] + [18] * 23)), (pair, arm, "batch count")
                toggle_rows[arm] = toggles
                samples[arm].extend(toggles)
                paired[arm].append(percentile([int(row["end_to_end_ns"]) for row in toggles], .95))
            for a, b in zip(toggle_rows["baseline"], toggle_rows["rgb"]):
                assert int(a["vertices_uploaded_bytes"]) - int(b["vertices_uploaded_bytes"]) == 251_160
        for arm in arms:
            receipt = archive.read(arm + "-full-round.txt").decode()
            assert "Beckenburg full multiplayer round: PASS" in receipt
            assert "both hunters received the winning settlement" in receipt
            print(arm, receipt.rstrip())
            print("Individual toggle end-to-end p95 in ms:", [round(v / 1e6, 2) for v in paired[arm]])
            for field in ("render_thread_cpu_ns", "gl_issue_ns", "gl_worker_ns", "end_to_end_ns"):
                values = [int(row[field]) for row in samples[arm]]
                print(arm, field, *(round(percentile(values, p) / 1e6, 2) for p in (.50, .95, .99)),
                      f">100 ms {sum(v > 100_000_000 for v in values)} / {len(values)}")


if __name__ == "__main__":
    verify(Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("physical-door.zip"))
