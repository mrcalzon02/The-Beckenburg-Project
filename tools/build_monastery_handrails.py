"""Fit graspable handrails to the existing Monastery stairs and landing."""
import json
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_monastery import CRYPT_WELLS, LANDING, RISERS, STAIRS, rel
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-refuge-pass"
STAGE = BASE / "monastery-handrail-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-handrails"
STATUS = "MONASTERY_FOUR_STAIR_FLIGHTS_WITH_FOUNDED_HANDRAILS_STAGED"
GRIP = .96
OFFSET = .125
RADIUS = .027


def xyz(axis, run, height, side):
    return (run, height, side) if axis == "x" else (side, height, run)


def survey(assembly):
    rails = []
    fixings = []

    def run(label, axis, face, inward, start, end, low, high, supports, return_ends=()):
        """Record a rail and its anchors from one continuous receiving face."""
        side = face + inward * OFFSET
        begin = xyz(axis, start, low + GRIP, side)
        finish = xyz(axis, end, high + GRIP, side)
        rails.append((label, axis, inward, begin, finish))
        ray = point(*xyz(axis, 0, 0, -inward))
        for part in (.08, .34, .66, .92, *return_ends):
            t = start + part * (end - start)
            y = low + part * (high - low) + GRIP
            grip = xyz(axis, t, y, side)
            p, _, receiver = assembly.measured(point(*xyz(axis, t, y, side + inward * .085)),
                                                ray, .54, f"{label} at {grip}")
            assert any(receiver.name.startswith(support) for support in supports), (label, receiver.name)
            back = (p.x, p.z, -p.y)
            mount = xyz(axis, t, y, (back[2] if axis == "x" else back[0]) - inward * .009)
            fixings.append((label, receiver.name, grip, mount, part in return_ends))

    first, second = STAIRS
    for i, (x0, x1, start, end, y0, y1) in enumerate(STAIRS):
        left, right = (("west", "central") if i == 0 else ("central", "east"))
        for face, inward, wall in ((x0, 1, left), (x1, -1, right)):
            returns = (0,1) if wall == "central" else ((0,) if i == 0 else (1,))
            # Put the returned ends just inside the exposed noses of their
            # string walls. At the landing, the outer rails join without a break.
            a, b = (.015 if 0 in returns else 0), (.985 if 1 in returns else 1)
            run(f"bell flight {i+1} {wall}", "z", face, inward,
                start+a*(end-start), start+b*(end-start), y0+a*(y1-y0), y0+b*(y1-y0),
                (f"Monastery bell stair {wall} string wall",), returns)
    x0, z0, x1, z1 = LANDING
    run("bell landing west", "z", x0, 1, first[3], z1-OFFSET, 3, 3,
        ("Monastery bell intermediate landing west parapet",))
    run("bell landing south", "x", z1, -1, x0+OFFSET, x1-OFFSET, 3, 3,
        ("Monastery bell intermediate landing south parapet",))
    run("bell landing east", "z", x1, -1, z1-OFFSET, second[2], 3, 3,
        ("Monastery bell intermediate landing east parapet",))
    by_name = {rail[0]: rail for rail in rails}
    continuous = ("bell flight 1 west", "bell landing west", "bell landing south",
                  "bell landing east", "bell flight 2 east")
    for a, b in zip(continuous, continuous[1:]):
        assert (point(*by_name[a][-1])-point(*by_name[b][-2])).length < .0001, (a,b)

    for index, (a, z0, b, z1) in enumerate(CRYPT_WELLS):
        bay = ("west", "east")[index]
        start, end = (a,b) if index == 0 else (b,a)
        for face, inward, side in ((z0+.15, 1, "north"), (z1-.15, -1, "south")):
            support = (f"Monastery {bay} catacomb stair {side} lining",
                       f"Monastery {bay} catacomb well {side} coping")
            # Keep end returns on their respective walls, inside the two stair mouths.
            direction = 1 if end > start else -1
            span = abs(end - start)
            run(f"{bay} catacomb {side}", "x", face, inward, start+direction*.07, end-direction*.07,
                -3*.07/span, -3+3*.07/span, support, (0,1))
    return rails, fixings, continuous


def build():
    roster = json.loads((PRIOR / "roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"] == "WLD-10")
    source = ROOT / row["construction"]
    authority = json.loads((PRIOR / "checkpoint.json").read_bytes())["project_bytes_sha256"]
    prior = PRIOR / "wld-10"
    sources = [source, *(ROOT / row[k] for k in ("blend", "native", "glb", "layout")),
               prior / "runtime-input.tsv", prior / "door-schedule.tsv"]
    for path in sources:
        assert sha(path) == authority[rel(path)], path
    meta = json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / row["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    rails, fixings, continuous = survey(assembly)
    iron = material("Monastery dark patinated iron stair hardware", (.20,.16,.12))
    for label, _, _, a, b in rails:
        assembly.tube(label+" grip", a, b, RADIUS, iron)
    for index, (label, _, a, b, returned) in enumerate(fixings, 1):
        assembly.tube(label+f" {'closed return' if returned else 'bearing bracket'} {index:02d}",
                      a, b, RADIUS if returned else .034, iron)
    for obj in assembly.built:
        obj.name = "Monastery " + obj.name.removeprefix("College ")
        obj["role"] = "supported iron stair handrail hardware"

    OUT.mkdir(parents=True, exist_ok=True)
    blend, model, collision = (OUT / (STEM + suffix) for suffix in (".blend", ".glb", ".hcs2"))
    fragment = ROOT / "build/monastery-handrails-addition.glb"
    export(fragment, assembly.built)
    append(ROOT / row["glb"], fragment, model, reuse_embedded_images=True)
    built = native_write(ROOT / row["native"], collision, set(), assembly.built)
    retained = split_native(ROOT / row["native"])
    after = split_native(collision)
    assert after[:len(retained)] == retained and len(after) == len(retained) + len(assembly.built)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend), compress=True)
    models = import_scene(model)
    for name, triangles in built:
        assert match_geometry([t for t, _ in triangles], [t for t, _ in records(models[name])]), name

    for name in ("runtime-input.tsv","door-schedule.tsv"):
        (OUT / name).write_bytes((prior / name).read_bytes())
    flights = [("FLIGHT", f"bell-{index}", "z", (x0+x1)/2, start, end, low, high, RISERS)
               for index, (x0, x1, start, end, low, high) in enumerate(STAIRS, 1)]
    for index, (a, z0, b, z1) in enumerate(CRYPT_WELLS):
        start, end = (a,b) if index == 0 else (b,a)
        flights.append(("FLIGHT", ("west-crypt","east-crypt")[index], "x",
                        (z0+z1)/2, start, end, 0, -3, RISERS))
    write_tsv(OUT / "handrail-schedule.tsv", [
        *(("RAIL", label, axis, toward, *a, *b) for label, axis, toward, a, b in rails), *flights])
    meta.update(status=STATUS, before_construction=rel(source), before_construction_sha256=sha(source),
                source_glb=row["glb"], source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"], source_physics_sha256=row["native_sha256"],
                before_layout=row["layout"], before_layout_sha256=sha(ROOT / row["layout"]),
                retained_native_records=len(retained), removed_source_meshes=[],
                new_interface_parts=[name for name, _ in built], continuous_bell_grip=continuous,
                grip_radius_m=RADIUS, grip_height_above_pitch_m=GRIP,
                furnished_fixed_mounts=[dict(rail=label, receiver=wall, attachment=b, closed_return=returned)
                                        for label, wall, _, b, returned in fixings],
                builder=rel(Path(__file__)), builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(model), physics_sha256=sha(collision), blend_sha256=sha(blend))
    construction = OUT / "construction.json"
    write(construction, meta)
    row.update(construction=rel(construction), glb=rel(model), native=rel(collision), blend=rel(blend),
               glb_sha256=sha(model), native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"] == "WLD-10")["decision"] = STATUS
    roster["baseline"] = rel(PRIOR / "roster.json")
    write(STAGE / "roster.json", roster)
    print("PASS", len(rails), "grip runs,", len(fixings), "measured masonry mounts,", len(retained),
          "retained native records")


if __name__ == "__main__":
    build()
