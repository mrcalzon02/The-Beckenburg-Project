"""Fit Cemetery records and essential mortuary furnishings to the retained rooms."""
import json
import math
import sys
from pathlib import Path

import bpy
from mathutils import Matrix, Vector

sys.path.insert(0, str(Path(__file__).resolve().parent))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import bounds, point, world
from build_mine_case import Case, DOWN
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import BASE, ROOT, load
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

SOURCE = BASE / "cemetery-distribution-pass"
OUTPUT = BASE / "cemetery-furnishings-pass"

MEMORIAL = ("MEMORIAL_REGISTER", "chamber", 6, "level_blockout_headmaster_desk",
            (18.72, -3, 6.6), (20.2, -3, 6.6), "level_trace_journal")
VISITORS = ("VISITOR_REGISTER", "mort-reception", 8, "level_blockout_mortuary_visitors_registry_desk",
            (69.6, 0, 56.5), (68.13, 0, 56.5), "level_story_letters")
MORTUARY_FURNISHINGS = (
    ("viewing seat", "mort-viewing", "level_blockout_mortuary_viewing_seat", (70.0, 0, 43.4), math.pi / 2),
    ("viewing bier", "mort-viewing", "level_blockout_mortuary_casket_bier", (62.9, 0, 43.4), math.pi / 2),
    ("preparation table", "mort-prep", "level_blockout_mortuary_prep_table", (80.8, 0, 45.2), math.pi / 2),
    ("cold drawer bank", "mort-cold", "level_blockout_mortuary_cold_drawer_wall", (83.04, 0, 54.6), math.pi / 2),
    ("receiving trolley", "mort-bay", "level_blockout_mortuary_trolley", (69.7, 0, 71.8), math.pi / 2),
)


def seat_memorial_detail(objects):
    # The inherited rough places the small plaque above its own pedestal.
    # Seat only that piece on the measured top of this placed instance.
    obj, = objects
    vertices = [v for v in obj.data.vertices
                if 70.44 < (obj.matrix_world @ v.co).x < 70.62
                and (obj.matrix_world @ v.co).z > 1.31]
    low = min((obj.matrix_world @ v.co).z for v in vertices)
    assert len(vertices) == 36 and abs(low - 1.33) < .001, obj.name
    solver = bvh(objects)
    bearings = []
    for x in (70.48, 70.57):
        for z in (43.28, 43.52):
            hit, normal, _, _ = solver.ray_cast(point(x, low - .004, z), DOWN, .09)
            assert hit is not None and normal.z > .98, (x, z, hit)
            bearings.append(hit.z)
    top = max(bearings)
    assert max(bearings) - min(bearings) < .001 and abs(top - 1.285) < .001
    offset = obj.matrix_world.inverted().to_3x3() @ Vector((0, 0, top - low - .001))
    for vertex in vertices:
        vertex.co += offset
    obj.data.update()
    return dict(old_bottom_m=round(low, 6), new_bottom_m=round(top - .001, 6),
                support_m=round(top, 6), bearing_samples=len(bearings))


def fit_record(case, spec):
    document, zone, owner, asset, center, feet, record = spec
    tag = document.lower().replace("_", " ")
    bench, furniture = case.fit(asset, tag + " desk", zone, *center, yaw=math.pi / 2)
    x, y, z = center
    top, normal, _, _ = bvh(bench).ray_cast(point(x, y + 1.7, z), DOWN, 1.65)
    assert top is not None and normal.z > .98, asset
    papers, placement = case.fit(record, tag, zone, x, top.z + .0005, z, scale=.88, support=bench)
    contact, _, _, _ = bvh(papers).ray_cast(point(x + .025, top.z + .1, z + .017), DOWN, .12)
    assert contact is not None, document
    view = point(feet[0], feet[1] + 1.63, feet[2])
    ray = contact - view
    room_parts = [obj for obj in case.scene.objects if obj.type == "MESH" and obj not in papers]
    assert bvh(room_parts).ray_cast(view, ray.normalized(), ray.length)[0] is None, document
    for obj in papers:
        obj["role"] = "desktop-supported readable record"
    row = ("STORY", "cemetery-story-" + document.lower(), document, owner, record,
           *placement["pivot"], placement["yaw"], placement["scale"], *feet, *world(contact))
    return row, dict(document=document, owner=owner, space=zone, desktop=round(top.z, 6),
                     reading_stance=feet, aim=world(contact), furniture=furniture, record=placement)


def caretaker_phone(case):
    x,z=34.55,40.50
    table,furniture=case.fit("level_event_side_table_worn","caretaker telephone table","cottage",x,0,z)
    hit,n,_,_=bvh(table).ray_cast(point(x,1.6,z),DOWN,1.5)
    assert hit is not None and n.z>.98,"Caretaker telephone table has no top"
    parts,placement=case.fit("level_event_phone_worn","caretaker telephone","cottage",
                             x,hit.z+.001,z,support=table)
    assert len(parts)==1,"Expected one loaded telephone casing"
    phone=parts[0]
    low,high=bounds(parts)
    face,_,_,_=bvh(parts).ray_cast(point(x,low[1]+.065,high[2]+.8),point(0,0,-1),1.6)
    assert face is not None,"Telephone casing is not targetable"
    px,py,pz=world(face)
    phone.name="Cemetery caretaker telephone"
    socket=("SERVICE",phone.name,"native-device",1,px,py,pz+.038,0,"PHONE")
    return socket,dict(table=furniture,telephone=placement,face=[px,py,pz])


def preparation_basin(case):
    zone="mort-prep"
    x,z=76.65,40.68
    basin=case.place("level_bath_basin",Matrix.Translation(point(x,0,z)),
                     "Cemetery preparation basin","deck-supported preparation basin",zone)
    for u in (x-.08,x+.08):
        for v in (z-.08,z+.08):
            hit,normal,_,_=case.support_surfaces.ray_cast(point(u,.16,v),DOWN,.24)
            assert hit is not None and abs(hit.z)<.004 and normal.z>.98,(u,v)
    wall,_,_=case.measured(point(x,.6,40.45),point(0,0,-1),.52,"Mortuary north plumbing wall")
    _,_,back=world(wall)
    copper=material("Vale Mortuary wet-service copper",(.32,.19,.105))
    drain=material("Vale Mortuary sealed drain iron",(.15,.17,.17))
    case.tube("Mortuary basin wall riser",(x,.13,back+.04),(x,.97,back+.04),.017,copper)
    case.tube("Mortuary basin union",(x,.97,back+.04),(x,.97,z-.14),.017,copper)
    case.tube("Mortuary basin low waste union",(x,.25,z-.09),(x,.25,back+.028),.027,drain)
    socket=("SERVICE","vale-prep-basin","water-basin",11,x,.86,z,0,"preparation-wash")
    return socket,dict(bounds=bounds(basin),floor=0,wall=back,room=zone)


def build(site, authority):
    assert all(sha(ROOT / name) == digest for name, digest in authority["exports"].items())
    assert all(sha(ROOT / name) == digest for name, digest in authority["evidence"].items())
    source = SOURCE / site
    parent = source / "distributed-grounds"
    bpy.ops.wm.open_mainfile(filepath=str(parent.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version = 0
    case = Case()
    annex = site.endswith("mortuary")
    furnishing_receipts = []
    if annex:
        for name, zone, asset, center, yaw in MORTUARY_FURNISHINGS:
            pieces, receipt = case.fit(asset, name, zone, *center, yaw=yaw)
            if name == "viewing seat":
                receipt["pedestal_detail"] = seat_memorial_detail(pieces)
                receipt["bounds"] = bounds(pieces)
            furnishing_receipts.append(dict(space=zone, function=name, **receipt))
    fitted = [fit_record(case, item) for item in ((MEMORIAL, VISITORS) if annex else (MEMORIAL,))]
    services=[caretaker_phone(case)]
    if annex:services.append(preparation_basin(case))
    for obj in case.built:
        obj.name = obj.name.replace("Mine case ", "Cemetery ", 1)
        if obj["role"] != "desktop-supported readable record":
            obj["role"] = "floor-supported case furnishing"

    folder = OUTPUT / site
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / "furnished-grounds"
    fragment = ROOT / "build/cemetery-furnishings-fragment.glb"
    export(fragment, case.built)
    append(parent.with_suffix(".glb"), fragment, target.with_suffix(".glb"), reuse_embedded_images=True)
    fragment.unlink()
    added = native_write(parent.with_suffix(".hcs2"), target.with_suffix(".hcs2"), set(), case.built,
                         open_components={obj.name for obj in case.built})
    before = split_native(parent.with_suffix(".hcs2"))
    after = split_native(target.with_suffix(".hcs2"))
    assert after[:len(before)] == before and len(after) - len(before) == len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target.with_suffix(".blend")), compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target.with_suffix(".glb")))
    for name, geometry in added:
        actual = records(bpy.context.scene.objects[name])
        assert match_geometry([face for face, _ in geometry], [face for face, _ in actual]), name

    header = ("SOURCE", sha(target.with_suffix(".glb")), sha(target.with_suffix(".hcs2")))
    (folder / "room-layout.tsv").write_bytes(
        ("\t".join(header) + "\n").encode()
        + (source / "room-layout.tsv").read_bytes()
        + "".join("\t".join(map(str, row)) + "\n" for row, _ in (*fitted,*services)).encode())
    (folder / "door-schedule.tsv").write_bytes((source / "door-schedule.tsv").read_bytes())
    paths = [target.with_suffix(ext) for ext in (".blend", ".glb", ".hcs2")]
    receipt = dict(exports={path.relative_to(ROOT).as_posix(): sha(path) for path in paths},
                   retained_native_parts=len(before), added_native_parts=len(added),
                   supported_records=[record for _, record in fitted], furnishings=furnishing_receipts,
                   services=[dict(socket=row,details=detail) for row,detail in services])
    (folder / "construction.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("PASS", site, len(before), "retained parts", len(added), "new parts", len(fitted), "record surfaces")


def capture_mortuary():
    packet = OUTPUT / "wld-07-mortuary"
    source = packet / "furnished-grounds.glb"
    assert sha(source) == load(packet / "construction.json")["exports"][source.relative_to(ROOT).as_posix()]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source))
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.display.shading.cavity_type = "BOTH"
    scene.render.resolution_x = 1120
    scene.render.resolution_y = 700
    scene.render.resolution_percentage = 100
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.lens = 31
    scene.camera = camera
    folder = packet / "captures" / "structure"
    folder.mkdir(parents=True, exist_ok=True)
    for name, eye, look in (
        ("viewing-and-bier", (66.3, 1.72, 51), (66.5, .75, 43.4)),
        ("preparation-table", (76.7, 1.58, 46.8), (80.8, .8, 45.2)),
        ("cold-drawers", (78.7, 1.58, 55.8), (83.04, .92, 54.6)),
        ("receiving-trolley", (66.03, 1.58, 69.6), (69.7, .8, 71.8)),
    ):
        camera.location = point(*eye)
        camera.rotation_euler = (point(*look) - camera.location).to_track_quat("-Z", "Y").to_euler()
        scene.render.filepath = str(folder / (name + ".png"))
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    if "--captures-only" not in sys.argv:
        checkpoint = load(SOURCE / "checkpoint.json")
        for site in ("wld-07", "wld-07-mortuary"):
            build(site, checkpoint["sites"][site.upper()])
    capture_mortuary()
