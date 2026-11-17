#!/usr/bin/env python3
"""Match House first-scene GL upload receipts to the first consumed walking frame."""
import argparse
import csv
import io
import re
import sys
from pathlib import Path
from zipfile import ZipFile


def receipts(path):
    with ZipFile(path) as archive:
        trials = sorted(name.removesuffix("/run.log") for name in archive.namelist()
                        if name.endswith("/run.log"))
        if not trials:
            raise ValueError(f"No trial logs in {path}")
        for trial in trials:
            with archive.open(f"{trial}/frames.csv") as source:
                frames = csv.DictReader(io.TextIOWrapper(source, encoding="utf-8-sig"))
                first_moving = next(int(row["composited_gl_frame_id"]) for row in frames
                                    if row["phase"] == "moving" and int(row["composited_gl_frame_id"]) >= 0)
            lines = archive.read(f"{trial}/run.log").decode("utf-8-sig").splitlines()
            first_scenes = [line for line in lines if "GPU SCENE FIRST FRAME " in line]
            if len(first_scenes) != 1:
                raise ValueError(f"{path}/{trial} has {len(first_scenes)} first-scene receipts")
            values = dict(re.findall(r"(\w+)=(\S+)", first_scenes[0]))
            if values["measurement"] != "gl_worker_not_gpu_timestamp":
                raise ValueError(f"Unknown clock in {path}/{trial}")
            yield {"archive": path.name, "trial": trial, "first_scene_id": int(values["frame"]),
                   "first_moving_id": first_moving, "textures": int(values["textures_uploaded"]),
                   "texture_bytes": int(values["texture_uploaded_bytes"]),
                   "vertex_buffer_bytes": int(values["vbo_uploaded_bytes"]),
                   "gl_worker_ms": float(values["gl_worker_ms"]),
                   "readback_ms": float(values["readback_ms"])}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archives", type=Path, nargs="+")
    rows = [r for archive in parser.parse_args().archives for r in receipts(archive)]
    writer = csv.DictWriter(sys.stdout, fieldnames=rows[0].keys(), lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
