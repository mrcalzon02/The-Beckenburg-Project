"""Fit the Northwatch generator shed's source, cabinet and service lighting."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, bounds, point, world
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
PARENT = BASE / "northwatch-refuge-pass"
OUT = BASE / "northwatch-power-pass/wld-08"
SHED = dict(id="generator", owner=5, y=0, height=3)
GENERATOR = "level_trace_generator_refined"
BANK = "level_electrical_bank_worn"
WALL = 8.24
GENERATOR_POSITION = (9.05, 0, 9.12)
BANK_POSITION = (5.55, .042, 8.54)
AISLE = [(11, 0, 16), (11, 0, 13), (9.085, 0, 11.03), (5.55, 0, 11.03)]


def install(assembly):
    def box(name, rect, low, high, material, role):
        return assembly.box(name, rect, low, high, material, role, SHED["id"])

    bearings = []
    for label, xs, zs in (("cabinet plinth", (4.35, 5.55, 6.75), (8.31, 8.52, 8.91)),
                          ("diesel base", (8.62, 9.05, 9.48), (8.86, 9.12, 9.42))):
        for x in xs:
            for z in zs:
                p, _, obj = assembly.measured(point(x, .5, z), Vector((0, 0, -1)), .6, label)
                assert abs(p.z) < .002 and "generator floor" in obj.name, (label, x, z, obj.name)
                bearings.append(dict(part=label, point=world(p), structure=obj.name))
    for x in (4.40, 5.55, 6.70, 8.25, 9.05):
        for y in (.66, 1.59, 2.44):
            p, _, obj = assembly.measured(point(x, y, 8.70), point(0, 0, -1), .55, "shed north wall")
            assert abs(-p.y - WALL) < .003 and obj.name == "WLD-08 clean generator N end"
            bearings.append(dict(part="wall fixings", point=world(p), structure=obj.name))

    for z in (8.75, 9.65, 10.5):
        at, _, obj = assembly.measured(point(7.65,2.65,z), Vector((0,0,1)), .3, "shed ceiling clip")
        assert obj.name == "Ceiling cap generator" and abs(at.z-2.84) < .003
        bearings.append(dict(part="ceiling clip", point=world(at), structure=obj.name))

    box("distribution cabinet plinth", (4.35, 8.27, 6.75, 8.94), 0, .042, assembly.concrete,
        "slab-bearing cabinet plinth")
    rails = []
    for y in (.66, 1.59):
        rails.append(box("distribution cabinet fixing rail", (4.40, 8.237, 6.70, 8.46), y, y+.09,
                         assembly.mount, "masonry-anchored equipment rail"))
    equipment = {}
    objects = {}
    for asset, at, yaw, label in ((GENERATOR, GENERATOR_POSITION, math.pi, "shed diesel generator"),
                                  (BANK, BANK_POSITION, 0, "shed distribution cabinet")):
        items = assembly.place(asset, Matrix.Translation(point(*at)) @ Matrix.Rotation(yaw, 4, "Z"),
                               label, "fitted source or distribution equipment", SHED["id"])
        envelope = bounds(items)
        assert abs(envelope[0][1] - at[1]) < .003 and envelope[0][2] > WALL and envelope[1][1] < 2.4
        objects[asset] = items
        equipment[asset] = dict(position=at, yaw=yaw, bounds=envelope, sha256=sha(ASSETS / (asset + ".glb")))

    cabinet = bvh(objects[BANK])
    assert all(cabinet.overlap(bvh([rail])) for rail in rails), "Cabinet misses the north-wall fixing rails"
    panel = bvh(objects[GENERATOR])
    gx, _, gz = GENERATOR_POSITION
    for x in (gx-.065, gx+.135):
        for y in (.426, .692):
            hit, normal, _, _ = panel.ray_cast(point(x,y,gz+.5), point(0,0,-1), .3)
            assert hit is not None and abs(-hit.y-gz-.305) < .004 and normal.y < -.98, (x,y,hit)
    box("generator starter backing plate", (gx-.065,gz+.302,gx+.135,gz+.330), .424, .694,
        assembly.mount, "casing-backed generator starter")
    starter = (gx+.035, .457, gz+.331)
    assembly.place("button_12", Matrix.Translation(point(*starter)) @ Matrix.Scale(.30, 4),
                   "generator starter", "mounted stop-start switch", SHED["id"])
    assembly.effect("northwatch-generator", "generator", 5, starter, 0, "NW-MAIN")
    bx, by, bz = BANK_POSITION
    assembly.effect("northwatch-breaker", "breaker", 5, (bx, by+.84, bz+.235), 0, "NW-MAIN")

    # Genset output and cabinet share a clipped surface service run.
    feed_x, feed_y = gx+.25, .38
    terminal, normal, _, _ = panel.ray_cast(point(feed_x,feed_y,8.30), point(0,0,1), 1.25)
    assert terminal is not None and normal.y > .98, (feed_x,feed_y,terminal,normal)
    terminal_z = -terminal.y
    box("generator cable terminal gland",
        (feed_x-.055,terminal_z-.045,feed_x+.055,terminal_z+.007),feed_y-.048,feed_y+.048,
        assembly.mount,"generator casing cable gland")
    first_feed = len(assembly.built)
    assembly.raceway("shed generator output and wall riser",
                     [(feed_x,feed_y,terminal_z+.008),(feed_x,feed_y,8.29),
                      (feed_x,2.44,8.29),(4.89,2.44,8.29)],.026)
    bpy.context.view_layer.update()
    assert panel.overlap(bvh(assembly.built[first_feed:first_feed+1])), "Feeder misses the generator casing"
    for y in (.66, 1.59, 2.44):
        box("shed main riser saddle", (gx+.20,8.236,gx+.30,8.32), y-.032,y+.032, assembly.mount,
            "measured north wall conduit saddle")
    for x in (5.18, 6.6, 8.25):
        box("shed main overhead saddle", (x-.033,8.236,x+.033,8.32),2.41,2.47,assembly.mount,
            "measured north wall conduit saddle")
    for terminal, x in enumerate((bx-.6222, bx+.1378, bx+.8978), 1):
        at = len(assembly.built)
        assembly.raceway(f"shed cabinet lead {terminal}",
            [(x,by+2.108,bz-.04),(x,2.44,bz-.04),(x,2.44,8.29)],.018)
        bpy.context.view_layer.update()
        assert cabinet.overlap(bvh(assembly.built[at:at+1])), x

    assembly.switch(SHED, 11.9, 9.45, 0, -1)
    assembly.mounted_lamp(SHED, "northwatch-shed-task-light", point(7.65, 1.8, 10.6),
                          Vector((0,0,1)), 1.3, point(1,0,0))
    lx, ly, lz = assembly.loads[0]["mount"]
    drop = next(route["points"][-1] for route in assembly.routes if route["id"] == "EM-05 switch drop")
    assembly.raceway("NW-05 shed switch branch",
                     [(8.25,2.44,8.29),(11.9,2.44,8.29),(drop[0],2.44,drop[2]),drop],.014)
    assembly.raceway("NW-05 shed lighting branch",
                     [drop,(lx,drop[1],drop[2]),(lx,ly-.030,drop[2]),(lx,ly-.030,lz)],.016)
    for x in (8.05, 9.70, 11.25):
        box("shed switched-leg saddle", (x-.033,8.236,x+.033,8.302),drop[1]-.030,drop[1]+.030,
            assembly.mount,"north wall conduit saddle")
    for z in (8.75, 9.65, 10.5):
        box("shed ceiling conduit clip", (lx-.033,z-.032,lx+.033,z+.032),ly-.052,ly+.003,assembly.mount,
            "measured ceiling conduit clip")
    for collection in (assembly.sockets, assembly.controllers, assembly.loads, assembly.routes):
        for row in collection:
            row["id"] = row["id"].replace("college-", "northwatch-").replace("EM-", "NW-")
            for field in ("circuit", "style"):
                if field in row:
                    row[field] = row[field].replace("EM-", "NW-")
    return equipment, bearings


def build():
    checkpoint = json.loads((PARENT / "checkpoint.json").read_bytes())
    for name, digest in (checkpoint["exports"] | checkpoint["evidence"]).items():
        assert sha(ROOT / name) == digest, name
    old = {Path(name).suffix[1:]: ROOT / name for name in checkpoint["exports"]}
    bpy.ops.wm.open_mainfile(filepath=str(old["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    equipment, bearings = install(assembly)
    for obj in assembly.built:
        obj.name = "Northwatch " + obj.name.removeprefix("College ")
    OUT.mkdir(parents=True, exist_ok=True)
    paths = {kind: OUT / f"station-with-power.{kind}" for kind in ("blend", "glb", "hcs2")}
    fragment = ROOT / "build/northwatch-power-fragment.glb"
    export(fragment, assembly.built)
    append(old["glb"], fragment, paths["glb"], reuse_embedded_images=True)
    fragment.unlink()
    added = native_write(old["hcs2"], paths["hcs2"], set(), assembly.built,
                         open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    before, after = (split_native(path) for path in (old["hcs2"], paths["hcs2"]))
    assert after[:len(before)] == before and len(after) - len(before) == len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(paths["blend"]), compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(paths["glb"]))
    for name, expected in added:
        actual = records(bpy.context.scene.objects[name])
        assert match_geometry([face for face, _ in expected], [face for face, _ in actual]), name

    packet = PARENT / "wld-08"
    rows = [("SERVICE", s["id"], s["type"], s["room"], *s["position"], s["yaw"], s["style"])
            for s in assembly.sockets]
    rows.extend(("POINT", "Northwatch-shed-controls", *p) for p in AISLE)
    (OUT / "runtime-input.tsv").write_bytes((packet / "runtime-input.tsv").read_bytes()
        + "".join("\t".join(map(str, row))+"\n" for row in rows).encode())
    (OUT / "door-schedule.tsv").write_bytes((packet / "door-schedule.tsv").read_bytes())
    receipt = dict(exports={p.relative_to(ROOT).as_posix(): sha(p) for p in paths.values()},
                   previous_scene=checkpoint["native_scene_revision"], equipment=equipment, bearings=bearings,
                   services=assembly.sockets, loads=assembly.loads, controllers=assembly.controllers,
                   raceways=assembly.routes, operator_aisle=AISLE,
                   retained_collision_parts=len(before), added_collision_parts=len(added))
    (OUT / "construction.json").write_text(json.dumps(receipt,indent=2)+"\n", encoding="utf-8",newline="\n")
    print("PASS Northwatch fitted shed source, cabinet, circuit and service light", len(bearings), len(added))


if __name__ == "__main__":
    build()
