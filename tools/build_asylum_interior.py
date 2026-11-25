"""Fit Asylum leaves to the existing walls, headers and floor datums."""
import csv
import hashlib
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Vector


ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild/asylum-power-pass"
# Named wall pieces provide the jamb faces. The supported room volumes on
# either side give each opening its actual architectural owners.
APERTURES = (
    (476, "L09", "ground-ward-link", "spine", "x", "ground-ward-link E",
     "Flat route shell Wall ground-ward-link E", "Flat route shell Wall ground-ward-link E.001", 2, -1),
    (477, "L25", "spine", "day", "x", "spine W.006",
     "Flat route shell Wall spine W.003", "Flat route shell Wall spine W.004", 2, -1),
    (478, "L33", "spine", "clinical-link", "x", "spine E.004",
     "Flat route shell Wall spine E.002", "Flat route shell Wall spine E.003", 2, 1),
    (479, "L40", "treatment", "hydro-change", "x", "treatment E",
     "Wall treatment E", "Flat route shell Wall treatment E.001", 1, 1),
    (480, "L44", "spine", "dining", "x", "spine E.006",
     "Flat route shell Wall spine E.003", "Flat route shell Wall spine E.004", 2, 1),
    (481, "L55", "service-link", "quiet-lobby", "z", "service-link N",
     "Flat route shell Wall service-link N", "Flat route shell Wall service-link N.001", 1, -1),
    (482, "L71", "grounds", "chapel", "z", "chapel S",
     "Flat route shell Wall chapel S", "Flat route shell Wall chapel S.001", 2, -1),
)


def read(path):
    with path.open(encoding="utf-8", newline="") as source:
        return list(csv.reader(source, delimiter="\t"))


def write(path, rows):
    with path.open("w", encoding="utf-8", newline="") as target:
        csv.writer(target, delimiter="\t", lineterminator="\n").writerows(rows)


def bounds(name):
    obj = bpy.context.scene.objects[name]
    points = [obj.matrix_world @ Vector(v) for v in obj.bound_box]
    axes = [[p.x for p in points], [p.z for p in points], [-p.y for p in points]]
    return tuple((round(min(values), 5), round(max(values), 5)) for values in axes)


def build(out):
    source = json.loads((BASE / "checkpoint.json").read_bytes())["source_authority"]
    for kind in ("blend", "glb", "native"):
        assert hashlib.sha256((ROOT / source[kind]).read_bytes()).hexdigest() == source[kind + "_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / source["blend"]))
    spaces = {row[1]: row for row in read(BASE / "arrival.tsv") if row[0] == "SPACE"}
    leaf_hash = hashlib.sha256((ROOT / "assets/runtime/models/door_1_loose.glb").read_bytes()).hexdigest()
    doors = read(BASE / "door-survey.tsv")
    installed = {str(aperture[0]) for aperture in APERTURES}
    mission = read(BASE / "mission.tsv")
    by_id = {row[1]: row for row in mission if row[0] == "TRANSITION"}
    report = [("portal", "route", "low_jamb", "high_jamb", "header", "near_space", "far_space",
               "sill_m", "clear_width_m", "clear_height_m", "wall_thickness_m")]
    for portal, guide, a, b, axis, header, low, high, count, swing in APERTURES:
        head = f"Header {header}"
        normal, tangent = (0, 2) if axis == "x" else (2, 0)
        ha, ja, jb = map(bounds, (head, low, high))
        near, far = ha[normal]
        left, right = ja[tangent][1], jb[tangent][0]
        assert ja[normal] == jb[normal] == ha[normal] and ha[tangent] == (left, right), head
        sill = ja[1][0]
        height, width = round(ha[1][0] - sill, 5), round(right - left, 5)
        assert sill == jb[1][0] and ha[1][1] <= min(ja[1][1], jb[1][1]) and height >= 2 and width >= 1.2, head
        boundary, center = round((near + far) / 2, 5), round((left + right) / 2, 5)
        x, z = (boundary, center) if axis == "x" else (center, boundary)
        owners = spaces[a][2], spaces[b][2]
        old = by_id[str(portal)]
        assert old[2] == guide and set(owners) == set(old[3:5]) and (x, sill, z) == tuple(map(float, old[5:8])), old
        doors.append((portal, "ASYLUM-" + guide, a, b, *owners, x, sill, z, width, axis, near, far,
                      height, count, 1, swing, leaf_hash))
        report.append((portal, guide, low, high, head, a, b, sill, width, height, round(far - near, 5)))
    out.mkdir(exist_ok=True, parents=True)
    write(out / "door-survey.tsv", doors)
    write(out / "mission.tsv", [row for row in mission if row[0] != "TRANSITION" or row[1] not in installed])
    write(out / "architectural-apertures.tsv", report)
    for name in ("arrival.tsv", "services.tsv"):
        copyfile(BASE / name, out / name)


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]))
