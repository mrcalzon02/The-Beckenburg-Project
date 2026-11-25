"""Fit and bind the surveyed Gallows Pass Railway to the native investigation."""
from pathlib import Path
import json
import math
import sys

import bpy
from mathutils import Matrix

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import bounds, point, world
from build_lookout_native_arrival import table
from build_mine_case import Case, DOWN
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import sha
from verify_college_electrical import native_records
from verify_college_services import bvh

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "workyard-station-pass/wld-12"
OUT = BASE / "gallows-pass"
OWNERS = {
    "tunnel": (0, 34, 0, 7), "coach-a": (1, 20, 0, 54), "coach-b": (2, 48, 0, 54),
    "store": (3, 58, 0, 23), "signal": (4, 20, 0, 69), "refuge": (5, 46, 0, 6),
    "lower": (6, 31, -3, 32), "west-return": (7, 11, 0, 42), "siding": (8, 57, 0, 53),
    "cutting": (9, 34, 0, 59), "trestle": (10, 34, 0, 32), "crossing": (11, 51, 0, 17),
    "entry": (12, 36, 0, 82), "stage": (13, 36, 0, 90.2),
}
# Yard and flight volumes assign gameplay ownership. Support remains in the model.
RECEIVING = (
    ("signal-apron", "entry", (18.6, 75.96, 31, 82), 0, 3),
    ("siding-approach", "entry", (42, 75.5, 59, 83), 0, 3),
    ("west-signal-approach", "west-return", (12, 60, 15.5, 62.0), 0, 3),
    ("west-track-approach", "cutting", (24, 52, 30, 56), 0, 3),
    ("east-track-approach", "cutting", (38, 52, 44, 56), 0, 3),
    ("trestle-return", "crossing", (39.5, 16, 50, 34.9), 0, 3),
    ("refuge-apron", "crossing", (44.4, 12, 50, 17), 0, 3),
    ("ballast-siding-return", "siding", (47.5, 32.6, 57, 35.4), 0, 3),
    ("walkway-north-flight", "lower", (26.8, 26.5, 29.2, 33.4), -3, 3),
    ("west-flight", "lower", (12.3, 36.8, 32.25, 39.2), -3, 3),
)
OPENINGS = (
    ("stage", "entry", 36, 0, 88, 2, "z", "trail"),
    ("entry", "siding", 56, 0, 76, 2, "z", "trail"),
    ("cutting", "trestle", 34, 0, 42, 2, "z", "trail"),
    ("trestle", "lower", 28, -1.5, 30.15, 1.5, "z", "stair"),
    ("lower", "west-return", 22, -1.5, 38, 1.5, "x", "stair"),
    ("crossing", "siding", 48, 0, 33, 2, "z", "trail"),
    ("crossing", "trestle", 40, 0, 32, 2, "x", "trail"),
)
EXTERIOR = {0: "west-signal-approach", 1: "signal-apron", 2: "cutting", 3: "west-return",
            4: "west-track-approach", 5: "east-track-approach", 6: "siding", 7: "siding",
            8: "trestle", 10: "refuge-apron"}
RECEIVING_OWNERS = {name: key for name, key, *_ in RECEIVING}
STAIR_KNOTS = {
    (28, -3, 28): (28, -3, 27.87),
    (31, -3, 38): (31.36, -3, 38),
}
SWINGS = (1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1)
REFUGES = (("signal", 15, 72.5, -math.pi / 2), ("store", 61.5, 24, math.pi / 2),
           ("refuge", 49.5, 3.0, math.pi / 2))


def power(assembly, signal):
    for asset, kind, x, z, yaw, control_y in (
        ("level_trace_generator_refined", "generator", 24, 74.4, 0, .56),
        ("level_electrical_bank_worn", "breaker", 26.1, 75.45, math.pi, .89),
    ):
        parts = assembly.place(asset, Matrix.Translation(point(x, .06, z)) @ Matrix.Rotation(yaw, 4, "Z"),
                               "Gallows " + kind, "floor-mounted railway equipment", "signal")
        low, high = bounds(parts)
        for px in (low[0], high[0]):
            for pz in (low[2], high[2]):
                hit, _, _ = assembly.measured(point(px, .18, pz), point(0, -1, 0), .30, kind + " slab")
                assert abs(hit.z) < .006
        assembly.box("Gallows " + kind + " plinth", (low[0]-.04, low[2]-.04, high[0]+.04, high[2]+.04),
                     0, .06, assembly.concrete, "equipment foundation", "signal")
        hit, _, _, _ = bvh(parts).ray_cast(point(x, control_y, low[2]-1), point(0, 0, 1), 2)
        assert hit is not None, kind
        px, py, pz = world(hit)
        assembly.effect("gallows-" + kind, kind, signal["owner"], (px, py, pz-.016), math.pi, "GP-MAIN")
    assembly.switch(signal, 23, 74.7, 0, 1)
    assembly.sockets[-1]["id"] = "gallows-signal-switch"
    assembly.sockets[-1]["style"] = "GP-SIGNAL"
    assembly.mounted_lamp(signal, "gallows-signal-light", point(24.4, 2.42, 74.7),
                          point(0, 0, 1), 1.35, point(1, 0, 0))
    assembly.raceway("Gallows signal circuit", ((24, .65, 74.55), (24, .65, 75.72),
        (24, 2.68, 75.72), (26.1, 2.68, 75.72), (26.1, 1.85, 75.72)), .014)
    assembly.raceway("Gallows signal lamp branch", ((26.1, 2.68, 75.72), (23, 2.68, 75.72),
        (23, 2.60, 75.72), (24.4, 2.60, 75.72), (24.4, 2.49, 75.72)), .014)


def room_lighting(assembly, zones):
    branches = (
        ("tunnel",39.1,18.3,13.4,1),
        ("coach-a",16.85,51.6,49.7,-1),
        ("coach-b",51.1,56.5,58.2,1),
        ("store",52.9,26.7,24.75,-1),
        ("refuge",40.95,9.7,8.2,-1),
    )
    for room,x,switch_z,lamp_z,side in branches:
        zone=zones[room]
        circuit=f"GP-{room.upper()}"
        switch=f"gallows-{room}-switch"
        assembly.switch(zone,x,switch_z,side,0)
        assembly.sockets[-1].update(id=switch,style=circuit)
        assembly.controllers[-1].update(id=switch,circuit=circuit)
        drop=assembly.routes[-1]["points"][-1]
        assembly.mounted_lamp(zone,f"gallows-{room}-light",point(x,zone["y"]+2.36,lamp_z),
                              point(side,0,0),1.6,point(0,0,1))
        lamp=assembly.loads[-1]
        lamp["circuit"]=circuit
        wall_x,wall_y,mount_z=lamp["mount"]
        face_x=wall_x-.028*side
        assert abs(face_x-drop[0])<.012, room
        assembly.raceway(f"Gallows {room} light branch",
                         (drop,(face_x,drop[1],mount_z),(face_x,wall_y+.12,mount_z)),.012)


def signal_radio(assembly, desk):
    x,z=17.2,64.2
    hit,normal,_,_ = bvh(desk).ray_cast(point(x,1.45,z),point(0,-1,0),.75)
    assert hit is not None and normal.z>.98, "Signal radio needs the operator desk"
    radio=assembly.place("level_event_radio_worn",Matrix.Translation(point(x,hit.z+.002,z)),
                         "operator radio","working desk radio","signal")
    assert len(radio)==1
    radio[0].name="Gallows operator radio"
    low, high=bounds(radio)
    assert abs(low[1]-hit.z)<.012, (hit,low)
    ray_y=low[1]+.075
    face,_,_,_=bvh(radio).ray_cast(point((low[0]+high[0])/2,ray_y,high[2]+.8),point(0,0,-1),1.6)
    assert face is not None
    px,py,pz=world(face)
    assembly.effect(radio[0].name,"native-device",OWNERS["signal"][0],(px,py,pz+.035),0,"RADIO")


def signal_register(case,desk):
    x,z=17.56,64.16
    top,normal,_,_=bvh(desk).ray_cast(point(x,1.45,z),DOWN,.75)
    assert top is not None and normal.z>.98,"The register needs a real desktop"
    book,fit=case.fit("level_trace_journal","signal register","signal",x,top.z+.001,z,scale=.56,support=desk)
    assert len(book)==1, "Expected a single readable journal"
    book[0].name="Gallows signal register"
    page,_,_,_=bvh(book).ray_cast(point(x+.028,top.z+.12,z+.022),DOWN,.135)
    assert page is not None,"The signal register needs an exposed page surface"
    aim=world(page)
    case.effect(book[0].name,"native_story",4,aim,0,"GALLOWS_SIGNAL_REGISTER")
    return dict(label="signal register",room="signal",aim=aim,**fit)


def refuge_fittings(case):
    fittings=[]
    for asset,label,room,x,z in (
        ("level_work_repair_bench","Tunnel maintenance bench","refuge",42.15,9.85),
        ("shelf_mp_6","Track spares shelf","store",61.5,18.35),
    ):
        _,fit=case.fit(asset,label,room,x,0,z)
        fittings.append(dict(label=label,room=room,**fit))
    x,z=49.4,8.85
    stand,base=case.fit("level_event_side_table_worn","Tunnel telephone stand","refuge",x,0,z)
    top,normal,_,_=bvh(stand).ray_cast(point(x,1.65,z),DOWN,1.6)
    assert top is not None and normal.z>.98,"The tunnel phone needs a real stand"
    phone,placement=case.fit("level_event_phone_worn","Tunnel telephone","refuge",x,top.z+.001,z,support=stand)
    assert len(phone)==1
    phone[0].name="Gallows tunnel telephone"
    low,high=bounds(phone)
    hit,_,_,_=bvh(phone).ray_cast(point(x,low[1]+.065,high[2]+.8),point(0,0,-1),1.6)
    assert hit is not None,"The tunnel phone needs an exposed casing"
    px,py,pz=world(hit)
    case.effect(phone[0].name,"native-device",5,(px,py,pz+.035),0,"PHONE")
    fittings.append(dict(label="tunnel telephone",room="refuge",stand=base,phone=placement))
    return fittings


def furnishings(assembly):
    installed = []
    for asset, zone, label, x, z, yaw in (
        ("level_workbench_worn", "signal", "Signal operator desk", 17.2, 64.2, 0),
        ("level_camp_bench", "coach-a", "West carriage seats", 18.1, 48.1, 0),
        ("level_camp_bench", "coach-a", "West carriage rear seats", 21.9, 60.1, math.pi),
        ("level_camp_bench", "coach-b", "East carriage seats", 46.1, 48.1, 0),
        ("level_camp_bench", "coach-b", "East carriage rear seats", 49.9, 60.1, math.pi),
        ("level_work_repair_bench", "store", "Track repair bench", 58, 17.3, 0),
        ("level_work_crew_footlocker", "refuge", "Maintenance crew chest", 43.6, 2.15, 0),
    ):
        frame = Matrix.Translation(point(x, 0, z)) @ Matrix.Rotation(yaw, 4, "Z")
        parts = assembly.place(asset, frame, label, "floor-bearing railway furnishings", zone)
        low, high = bounds(parts)
        assert abs(low[1]) < .006, (label, low)
        for px in (low[0] + .025, high[0] - .025):
            for pz in (low[2] + .025, high[2] - .025):
                hit, normal, support = assembly.measured(point(px, .18, pz), point(0, -1, 0), .28, label)
                assert abs(hit.z) < .006 and normal.z > .98, (label, px, pz, support.name)
        if label=="Signal operator desk":
            signal_radio(assembly,parts)
            installed.append(signal_register(assembly,parts))
        installed.append(dict(label=label, room=zone, asset=asset, origin=[x, 0, z], yaw=yaw,
                              bounds=[low, high]))
    return installed


def railway_kit(assembly):
    library = ROOT / "development/levels/primitive-props-v1"
    formation = bvh([obj for obj in assembly.scene.objects if obj.type == "MESH"
                     and obj.name in assembly.originals])
    installed = []
    for folder, key, name, placements in (
        ("models", "level_blockout_rail_straight_track_bay", "Track bay",
         [(37.4, z, -.01) for z in (61.5, 65.17, 68.84, 72.51)]),
        ("models", "level_blockout_signal_hut_lever_bank", "Mechanical lever frame", [(24.7, 64.3, -.035)]),
        ("refinement", "level_blockout_refined_rail_mechanical_semaphore", "Approach semaphore", [(39.1, 68.8, 0)]),
    ):
        path = library / folder / (key + ".glb")
        before = set(assembly.scene.objects)
        bpy.ops.import_scene.gltf(filepath=str(path))
        imported = set(assembly.scene.objects) - before
        templates = [(o.data, o.matrix_world.copy()) for o in sorted(imported, key=lambda o: o.name)
                     if o.type == "MESH"]
        if name == "Track bay":
            # Sleepers and ballast keep their original module pitch. Resize
            # only the steel to leave a 5 mm joint instead of an open gap.
            for mesh, transform in templates:
                inverse = transform.inverted()
                steel = {vertex for face in mesh.polygons
                         if mesh.materials[face.material_index].name.startswith("blockout_metal")
                         for vertex in face.vertices}
                for vertex in steel:
                    position = transform @ mesh.vertices[vertex].co
                    position.y *= 3.665 / 3.48
                    mesh.vertices[vertex].co = inverse @ position
        for obj in imported:
            bpy.data.objects.remove(obj, do_unlink=True)
        for i, (x, z, offset) in enumerate(placements, 1):
            parts = []
            for j, (mesh, transform) in enumerate(templates, 1):
                obj = bpy.data.objects.new(f"Gallows {name} {i:02d}-{j:02d}", mesh)
                assembly.scene.collection.objects.link(obj)
                obj.matrix_world = Matrix.Translation(point(x, offset, z)) @ transform
                obj["source_asset"] = key
                obj["role"] = "railway kit on measured formation"
                parts.append(obj)
            bpy.context.view_layer.update()
            low, high = bounds(parts)
            # Compare the track sleepers at their actual outline. For the
            # semaphore the post foot is smaller than its signal arm.
            pad = .16 if name == "Approach semaphore" else None
            corners_x = (x-pad, x+pad) if pad else (low[0]+.04, high[0]-.04)
            corners_z = (z-pad, z+pad) if pad else (low[2]+.04, high[2]-.04)
            for px in corners_x:
                for pz in corners_z:
                    hit, normal, _, _ = formation.ray_cast(point(px, .28, pz), point(0, -1, 0), .40)
                    assert hit is not None and abs(hit.z) < .018 and normal.z > .98, (name, px, pz, hit)
            assembly.built.extend(parts)
            installed.append(dict(asset=path.relative_to(ROOT).as_posix(), sha256=sha(path),
                                  name=name, origin=[x, offset, z], bounds=[low, high]))
    return installed


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    source = {ext: SOURCE / f"workyard-station.{ext}" for ext in ("blend", "glb", "hcs2")}
    contract = json.loads((SOURCE / "construction.json").read_bytes())
    layout_path = SOURCE / "authored-layout.json"
    for ext, digest in (("blend", "blend_sha256"), ("glb", "glb_sha256"), ("hcs2", "physics_sha256")):
        assert sha(source[ext]) == contract[digest]
    assert sha(layout_path) == contract["layout_sha256"]
    layout = json.loads(layout_path.read_bytes())
    zones = {z["id"]: {**z, "owner": OWNERS.get(z["id"], (7,))[0]} for z in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Case()
    refuges = cupboards(assembly, zones, REFUGES, site="gallows", floor_roles=("floor",))
    power(assembly, zones["signal"])
    room_lighting(assembly,zones)
    installed = furnishings(assembly) + refuge_fittings(assembly) + railway_kit(assembly)
    for piece in assembly.built:
        piece.name = piece.name.replace("College ", "Gallows ", 1).replace("Mine case ","Gallows ",1)
    target = {ext: OUT / f"railway.{ext}" for ext in source}
    addition = ROOT / "build/gallows-native-installation.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], reuse_embedded_images=True)
    native_write(source["hcs2"], target["hcs2"], set(), assembly.built,
                 open_components={p.name for p in assembly.built if p.get("source_asset")})
    prior = native_records(source["hcs2"])
    assert native_records(target["hcs2"])[:len(prior)] == prior
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    dock = layout["staging"]
    rows = [("SOURCE", sha(target["glb"]), sha(target["hcs2"])), ("DOCK", *dock["point"], dock["yaw"])]
    for key, (owner, *spawn) in OWNERS.items():
        z = zones[key]
        rows.append(("ROOM", key, owner, *z["bounds"], z["y"], z["height"],
                     str(z["kind"] in ("outside", "staging")).lower(), z["name"], *spawn))
    for name, key, area, datum, headroom in RECEIVING:
        rows.append(("SPACE", name, OWNERS[key][0], *area, datum, headroom, "true"))
    rows.extend(("TREAD", name) for name in contract["stair_components"])
    for portal, (a, b, x, y, z, width, axis, kind) in enumerate(OPENINGS, 940):
        rows.append(("OPEN", portal, OWNERS[a][0], OWNERS[b][0], x, y, z, width, axis, kind))
    for rite in layout["rituals"]:
        rows.append(("RITUAL", rite["rite"].lower(), OWNERS[rite["zone"]][0], *rite["point"]))
    for socket in assembly.sockets:
        if socket["type"] != "hide":
            rows.append(("SERVICE", socket["id"], socket["type"], socket["room"],
                         *socket["position"], socket["yaw"], socket["style"]))
    for pocket in refuges:
        rows.append(("COVER", pocket["id"], pocket["owner"], *pocket["position"], pocket["yaw"]))
    for path in layout["links"]:
        rows.extend(("POINT", path["id"], *STAIR_KNOTS.get(tuple(point), point)) for point in path["points"])
    table(OUT / "runtime-input.tsv", rows)
    leaf = sha(ROOT / "assets/runtime/models/door_1_loose.glb")
    doors = []
    for index, frame in enumerate(layout["authored_doorways"]):
        rooms = frame["rooms"]
        a = rooms[0]
        b = EXTERIOR[index] if index in EXTERIOR else rooms[1]
        owner_b = OWNERS[RECEIVING_OWNERS.get(b, b)][0]
        x, y, z = frame["frame_center"]
        width = frame["clear_width"]
        doors.append((1100 + index, frame["id"], a, b, OWNERS[a][0], owner_b, x, y, z,
                      width, frame["axis"], *frame["frame_span"], frame["clear_height"],
                      2 if width >= 1.7 else 1, 1, SWINGS[index], leaf))
    table(OUT / "door-schedule.tsv", doors)
    (OUT / "construction.json").write_text(json.dumps(dict(
        source=layout_path.relative_to(ROOT).as_posix(),
        exports={ext: sha(path) for ext, path in target.items()}, unchanged_components=len(prior),
        cover=refuges, furnishings=installed, service=[s for s in assembly.sockets if s["type"] != "hide"]), indent=2)+"\n",
        encoding="utf-8", newline="\n")


if __name__ == "__main__":
    build()
