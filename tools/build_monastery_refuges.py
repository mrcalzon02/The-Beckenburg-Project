"""Fit three walk-in refuges to existing Monastery floors without changing the building."""
import json
import math
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly
from build_college_grounds import import_scene
from build_dam_spillway_gallery import rel
from build_deliberate_entrances import write
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-door-pass"
STAGE = BASE / "monastery-refuge-pass"
OUT = STAGE / "wld-10"
STATUS = "MONASTERY_THREE_FOUNDED_WALK_IN_REFUGES_STAGED"
# Place storage against solid wall bays, facing interior aisles clear of door swings.
POSITIONS = (("archive", 22, 3.35, 0),
             ("dorm", -6.65, 39, -math.pi / 2),
             ("chamber", 23, 3.35, 0))


def build():
    roster = json.loads((PRIOR / "roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"] == "WLD-10")
    source = ROOT / row["construction"]
    meta = json.loads(source.read_bytes())
    for key, digest in (("blend", "blend_sha256"), ("glb", "glb_sha256"),
                        ("native", "physics_sha256"), ("layout", "layout_sha256")):
        assert sha(ROOT / row[key]) == meta[digest], key
    authority = json.loads((PRIOR / "checkpoint.json").read_bytes())["project_bytes_sha256"]
    packet = PRIOR / "wld-10"
    runtime = packet / "runtime-input.tsv"
    doors = packet / "door-schedule.tsv"
    for path in (runtime, doors, source):
        assert sha(path) == authority[rel(path)], path

    rows = [line.split("\t") for line in runtime.read_text(encoding="utf8").splitlines()]
    zones = {p[1]: dict(owner=int(p[2]), y=float(p[7]), height=float(p[8]))
             for p in rows if p[0] == "ROOM"}
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / row["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    fits = cupboards(assembly, zones, POSITIONS, site="monastery")
    for obj in assembly.built:
        obj.name = "Monastery " + obj.name.removeprefix("College ")
    assert len(assembly.built) == len(POSITIONS) and all(p["base_contacts"] == 12 for p in fits)

    OUT.mkdir(parents=True, exist_ok=True)
    blend, model, collision = (OUT / ("monastery-with-refuges" + suffix)
                               for suffix in (".blend", ".glb", ".hcs2"))
    fragment = ROOT / "build/monastery-refuges-addition.glb"
    export(fragment, assembly.built)
    append(ROOT / row["glb"], fragment, model, reuse_embedded_images=True)
    added = native_write(ROOT / row["native"], collision, set(), assembly.built,
                         open_components={obj.name for obj in assembly.built})
    retained = split_native(ROOT / row["native"])
    assert split_native(collision)[:len(retained)] == retained
    assert len(split_native(collision)) == len(retained) + len(POSITIONS)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend), compress=True)
    models = import_scene(model)
    for name, triangles in added:
        assert match_geometry([t for t, _ in triangles],
                              [t for t, _ in records(models[name])]), name

    rows.extend(("COVER", p["id"], p["owner"], *p["position"], p["yaw"]) for p in fits)
    write_tsv(OUT / "runtime-input.tsv", rows)
    (OUT / "door-schedule.tsv").write_bytes(doors.read_bytes())
    meta.update(status=STATUS, before_construction=rel(source), before_construction_sha256=sha(source),
                source_glb=row["glb"], source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"], source_physics_sha256=row["native_sha256"],
                before_layout=row["layout"], before_layout_sha256=sha(ROOT / row["layout"]),
                before_segment_map=meta["source_segment_map"], before_stair_components=meta["stair_components"],
                retained_native_records=len(retained), removed_source_meshes=[],
                new_interface_parts=[name for name, _ in added], refuges=fits,
                door_schedule_sha256=sha(doors),
                builder=rel(Path(__file__)), builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(model), physics_sha256=sha(collision), blend_sha256=sha(blend))
    construction = OUT / "construction.json"
    write(construction, meta)
    row.update(construction=rel(construction), glb=rel(model), native=rel(collision), blend=rel(blend),
               glb_sha256=sha(model), native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"] == "WLD-10")["decision"] = STATUS
    roster["baseline"] = rel(PRIOR / "roster.json")
    write(STAGE / "roster.json", roster)
    print("PASS", len(fits), "Monastery refuges on", sum(fit["base_contacts"] for fit in fits), "slab contacts")


if __name__ == "__main__":
    build()
