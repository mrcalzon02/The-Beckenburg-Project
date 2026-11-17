"""Verify checksummed door traces and summarize each arm. Run from any directory."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import csv
import io
import math

root = Path(__file__).resolve().parent
trace = ZipFile(root / "door-frames.zip")
manifest = trace.read("SHA256SUMS").decode().splitlines()
for entry in manifest:
    digest, name = entry.split("  ", 1)
    assert hashlib.sha256(trace.read(name)).hexdigest() == digest, name

def rows(name):
    return list(csv.DictReader(io.StringIO(trace.read(name).decode())))

def quantile(values, p):
    return sorted(values)[math.ceil(len(values)*p)-1]

base = rows("baseline-1/frames.csv")
assert len(base) == 58
for arm in ("baseline", "receiver", "rgb", "both"):
    toggles = []
    for i in (1, 2):
        frames = rows(f"{arm}-{i}/frames.csv")
        assert len(frames) == len(base)
        for a, b in zip(frames, base):
            for field in ("frame","phase","door_open","door_b_open","window_open","argb_sha256","depth_bits_sha256"):
                assert a[field] == b[field], (arm,i,a["frame"],field)
            if a["phase"] == "toggle":
                savings = int(b["vertices_uploaded_bytes"]) - int(a["vertices_uploaded_bytes"])
                assert savings == (251160 if arm in ("rgb","both") else 0)
                toggles.append(a)
    print(arm, "toggles", len(toggles))
    for field in ("render_thread_cpu_ns", "gl_issue_ns", "gl_worker_ns", "end_to_end_ns"):
        ms = [int(row[field])/1e6 for row in toggles]
        print(f"  {field}: p50/p95/p99 ms", *(round(quantile(ms,p),2) for p in (.50,.95,.99)),
              ">100 ms", sum(v > 100 for v in ms))
corrected = rows("exact-keys/frames.csv")
assert len(corrected) == len(base)
print("Exact-key alternative, differential frames out of 58")
for field in ("argb_sha256","depth_bits_sha256"):
    print(field, sum(a[field] != b[field] for a,b in zip(corrected,base)))
for arm in ("base","candidate","exact"):
    samples = list(csv.reader(io.StringIO(trace.read(f"witness/receivers-{arm}.csv").decode())))
    print("Fresh-receiver oracle matches",arm,sum(r[3]=="true" for r in samples),"/",len(samples))
software = rows("software/frames.csv")
checkpoint = root.parent / "benchmark-data.zip"
if checkpoint.exists():
    with ZipFile(checkpoint) as historical:
        for name in ("exact-software-baseline.csv","exact-software-candidate.csv"):
            reference=list(csv.DictReader(io.StringIO(historical.read("software/final/"+name).decode())))
            assert len(reference)==len(software)==72
            assert all(a["rgb_depth_sha256"]==b["rgb_depth_sha256"] for a,b in zip(reference,software))
            print("Software color + raw depth",name,"72/72")


pixels = ZipFile(root / "door-pixels.zip")
delta = list(csv.DictReader(io.StringIO(pixels.read("pixel-differences.csv").decode())))
assert len(delta)==len(base) and len(base)==len(corrected)
assert (root / "pixel-differences.csv").read_bytes()==pixels.read("pixel-differences.csv")
total_pixels = 0
changed_pixels = 0
weighted_delta = 0
largest = 0
for stats,b,x in zip(delta,base,corrected):
    left=pixels.read(stats["baseline_argb_sha256"]+".argb")
    right=pixels.read(stats["exact_argb_sha256"]+".argb")
    assert hashlib.sha256(left).hexdigest()==b["argb_sha256"]
    assert hashlib.sha256(right).hexdigest()==x["argb_sha256"]
    assert len(left)==len(right)==320*200*4
    assert left[0::4]==right[0::4] and stats["changed_alpha"]=="0"
    assert stats["depth_bits_sha256"]==b["depth_bits_sha256"]==x["depth_bits_sha256"]
    count=0
    distance=0
    maximum=0
    for v in range(0,len(left),4):
        if left[v:v+4] == right[v:v+4]:
            continue
        count+=1
        for c in range(v+1,v+4):
            error=abs(left[c]-right[c])
            distance+=error
            maximum=max(maximum,error)
    assert (count,distance,maximum)==tuple(int(stats[k]) for k in
        ("changed_pixels","sum_abs_rgb_channel_delta","max_abs_rgb_channel_delta"))
    total_pixels+=len(left)//4
    changed_pixels+=count
    weighted_delta+=distance
    largest=max(largest,maximum)
print("Corrected-key ARGB comparison",changed_pixels,"/",total_pixels,"changed;",
      "max channel",largest,"mean absolute channel all/changed",
      round(weighted_delta/(total_pixels*3),4),round(weighted_delta/(changed_pixels*3),4))
