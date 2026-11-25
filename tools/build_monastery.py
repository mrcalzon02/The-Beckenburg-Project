"""Build the Monastery's founded courts, doorway walls and orthogonal bell stair."""
import copy
import json
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_college_grounds import import_scene, place_review_van
from build_deliberate_entrances import write
from build_mine_yard import combined_plate, punched_plate
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from prepare_monastery import BASE, BASELINE, OLD_WORLD, PASS, PROGRAM, SITE, VAN, WORLD
from review_roster_macroterrain import records, sha

OUT = PASS / "wld-10"
STEM = "monastery-cloister"
CAPTURES = BASE / "captures/monastery-cloister-batch"
BELL_FLOOR = (50, 16, 60, 30)
GARDEN_BEDS = ((17.6, 26, 24.4, 33.4), (17.6, 37, 24.4, 46),
               (33.6, 26, 40.4, 33.4), (33.6, 37, 40.4, 46))
STAIRS = ((54.1, 55.9, 17.8, 23.07, 0, 3), (56.14, 57.94, 23.07, 17.8, 3, 6))
LANDING = (54.1, 23.07, 57.94, 25.02)
BELL_WELL = (56.0, 17.8, 58.08, 23.15)
CRYPT_WELLS = ((12.6, 18.10, 18.00, 19.90), (26.30, 18.10, 31.70, 19.90))
RISERS = 17


def geometry(old):
    ground = next(obj for obj in old.values() if obj.get("role", "").startswith("saved editor terrain"))
    studies = {obj.name for obj in old.values() if obj.get("role", "").startswith("rough route support")}
    assert len(studies) == 15
    retired = {ground.name, *studies, "L21 landing1", "L21 landing2"}
    retired.update(obj.name for obj in old.values() if obj.get("role") == "structural-arrival-end-bearing"
                   or (obj.get("role") == "floor" and obj.get("zone") in {"north", "bell"})
                   or obj.name.startswith("L16 flight0 step"))
    ashlar = material("Monastery warm grey ashlar", (.51, .48, .42))
    paving = material("Monastery worn limestone flags", (.48, .45, .39))
    vault = material("Monastery dressed crypt stone", (.41, .40, .37))
    soil = material("Monastery dark herb loam", (.26, .23, .13))
    frame_source = frame_template()
    parts, portal_records, stair_parts = [], [], []

    def solid(name, cells, bottom, top, mat, role, zone):
        obj = combined_plate("Monastery " + name, cells, top, top - bottom, mat)
        obj["role"], obj["zone"] = role, zone
        parts.append(obj)
        return obj

    def face(name, axis, fixed, start, end, y, height, openings, rooms, mat=ashlar):
        depth = .12

        def masonry(label, low, high, base, top):
            box = ((low, fixed-depth, high, fixed+depth) if axis == "z"
                   else (fixed-depth, low, fixed+depth, high))
            solid(name + " " + label, [box], base, top, mat, "bearing wall or door lintel", rooms[0])

        cursor = start
        for label, center, width, clear, adjacent in openings:
            lo, hi = center - .62 * width, center + .62 * width
            assert cursor < lo < hi < end
            masonry(label + " preceding pier", cursor, lo, y, y + height)
            masonry(label + " bearing lintel", lo, hi, y + clear * 2.275 / 2.125, y + height)
            frame = frame_source.copy()
            frame.data = frame_source.data.copy()
            bpy.context.collection.objects.link(frame)
            frame.name = "Monastery " + label + " fitted frame"
            frame.data.materials.clear()
            frame.data.materials.append(mat)
            for vertex in frame.data.vertices:
                x, z, h = vertex.co
                u, n, v = x * width, z * .24 / .18, y + h * clear / 2.125
                vertex.co = (center-u, -fixed-n, v) if axis == "z" else (fixed+n, -center-u, v)
            frame["role"], frame["source_asset"] = "indexed architectural doorway", "level_blockout_doorway"
            parts.append(frame)
            portal_records.append(dict(id=label, axis=axis, boundary=fixed, along=center, sill_y=y,
                                       clear_width=width, clear_height=clear, rooms=adjacent))
            cursor = hi
        masonry("end pier", cursor, end, y, y + height)

    def retire_faces(*sides):
        for room, side in sides:
            label = f"{room} {side}"
            names = [name for name in old if any(name == prefix + label or name.startswith(prefix + label + ".")
                     for prefix in ("Wall ", "Header ", "Flat route shell Wall "))]
            retired.update(names)

    # Circulation within the ground plan uses openings in continuous wall lines.
    retire_faces(("gatehouse", "S"), ("gatehouse", "N"), ("south", "S"),
                 ("south", "N"), ("north", "S"), ("west", "N"), ("west", "S"),
                 ("east", "N"), ("east", "S"), ("north", "N"), ("archive", "S"), ("vestry", "S"),
                 ("archive", "E"), ("vestry", "W"), ("west", "W"), ("dorm", "E"),
                 ("east", "E"), ("north", "E"), ("tower-base", "W"), ("refectory", "W"),
                 ("tower-base", "E"), ("refectory", "N"), ("catacombs", "N"), ("chamber", "S"))
    face("gatehouse public elevation", "z", 62, 24, 40, 0, 3,
         [("public gate", 32, 2.16, 2.36, ["entry", "gatehouse"])], ["gatehouse"])
    face("shared gatehouse and south cloister wall", "z", 56, 8, 50, 0, 3,
         [("inner gatehouse door", 32, 1.85, 2.26, ["gatehouse", "south"])], ["south"])
    for label, datum in (("south", 50), ("north", 22)):
        face(label + " courtyard elevation", "z", datum, 8, 50, 0, 3,
             [(label + " west cloister opening", 11, 1.92, 2.32, ["west", label]),
              (label + " garden door", 29, 2.05, 2.32, ["garden", label]),
              (label + " east cloister opening", 47, 1.92, 2.32, ["east", label])], [label])
    face("north range doors", "z", 16, 8, 50, 0, 3,
         [("archive cloister door", 18, 1.83, 2.24, ["north", "archive"]),
          ("vestry cloister door", 36, 1.83, 2.24, ["north", "vestry"])], ["north"])
    face("archive and vestry shared wall", "x", 28, 2, 16, 0, 3,
         [("archive vestry door", 9, 1.82, 2.24, ["archive", "vestry"])], ["archive"])
    face("western dormitory partition", "x", 8, 22, 50, 0, 3,
         [("dormitory cloister door", 36, 1.85, 2.26, ["west", "dorm"])], ["dorm"])
    face("western tower elevation", "x", 50, 16, 30, 0, 6,
         [("tower cloister door", 26, 1.85, 2.26, ["east", "tower-base"])], ["tower-base"])
    face("tower service elevation", "x", 60, 16, 30, 0, 6,
         [("tower service door", 23, 2, 2.35, ["tower-base", "rear"])], ["tower-base"])
    face("refectory cloister wall", "x", 50, 30, 50, 0, 3,
         [("dining hall cloister door", 40, 1.86, 2.28, ["east", "refectory"])], ["refectory"])
    face("refectory receiving elevation", "z", 30, 50, 68, 0, 3,
         [("refectory receiving door", 64, 2, 2.35, ["rear", "refectory"])], ["refectory"])
    face("memorial chamber partition", "z", 16, 8, 50, -3, 3,
         [("memorial chamber door", 18, 1.82, 2.24, ["catacombs", "chamber"])], ["catacombs"], vault)
    bpy.data.objects.remove(frame_source, do_unlink=True)

    # The vault roof is also the north cloister floor. Its only wells are the
    # two measured catacomb flights, with guards at both sides and low ends.
    a, b = CRYPT_WELLS
    cover = [(8, 16, 50, a[1]), (8, a[3], 50, 22), (8, a[1], a[0], a[3]),
             (a[2], a[1], b[0], a[3]), (b[2], a[1], 50, a[3])]
    solid("north cloister continuous vaulted deck", cover, -.23, 0, vault, "cover around two rectangular wells", "north")
    for label, (x0, z0, x1, z1), low_end in (("west", a, a[2]), ("east", b, b[0])):
        for side, lo, hi in (("north", z0-.16, z0), ("south", z1, z1+.16)):
            solid(f"{label} catacomb well {side} coping", [(x0, lo, x1, hi)], 0, 1.06, vault,
                  "founded stone stair guard", "north")
        for side, lo, hi in (("north", z0-.16, z0+.15), ("south", z1-.15, z1+.16)):
            solid(f"{label} catacomb stair {side} lining", [(x0, lo, x1, hi)], -3.16, 0, vault,
                  "founded stair wall under the vaulted deck", "catacombs")
        end = (low_end, low_end+.16) if label == "west" else (low_end-.16, low_end)
        solid(f"{label} catacomb low-end guard", [(end[0], z0-.16, end[1], z1+.16)], 0, 1.06, vault,
              "founded stone stair guard", "north")

    # Raise the working herb beds above the broad enclosed garden grade.
    for number, (x0, z0, x1, z1) in enumerate(GARDEN_BEDS, 1):
        inner = (x0+.27, z0+.27, x1-.27, z1-.27)
        solid(f"garden bed {number} retaining surround", punched_plate((x0, z0, x1, z1), inner),
              -.13, .48, ashlar, "founded raised garden bed", "garden")
        solid(f"garden bed {number} planted soil", [inner], -.07, .37, soil, "cultivated soil", "garden")

    # Both 3 m rises stand on the base slab. The lower run fits below the bell
    # deck. Only the upper run passes through its rectangular stair well.
    solid("bell floor around upper stair well", punched_plate(BELL_FLOOR, BELL_WELL), 5.76, 6,
          paving, "continuous upper bell floor with explicit stair well", "bell")
    for number, (x0, x1, start, end, low, high) in enumerate(STAIRS, 1):
        for step in range(RISERS):
            z0 = start + (end - start) * step / RISERS
            z1 = start + (end - start) * (step + 1) / RISERS
            y = low + (high - low) * (step + 1) / RISERS
            tread = solid(f"bell flight {number} riser {step+1:02}", [(x0, min(z0,z1), x1, max(z0,z1))],
                          0, y, paving, "founded limestone stair tread", "tower-base")
            stair_parts.append(tread.name)
    mid = solid("bell stair common intermediate landing", [LANDING], 0, 3, paving,
                "full supported stair landing", "tower-base")
    stair_parts.append(mid.name)
    # Stair-side construction rises continuously along the steps. The 0.24 m
    # spine separates flights, and the upper lip continues onto the bell deck.
    slope_wall(parts, "bell stair west string wall", 53.94, 54.1, 17.8, 23.07, 1.10, 4.1, ashlar)
    slope_wall(parts, "bell stair central string wall", 55.9, 56.14, 17.8, 23.07, 7.08, 4.12, ashlar)
    slope_wall(parts, "bell stair east string wall", 57.94, 58.10, 17.8, 23.07, 7.08, 4.1, ashlar)
    for label, box in (("west", (53.94, 23.07, 54.1, 25.02)),
                       ("south", (53.94, 25.02, 58.10, 25.18)),
                       ("east", (57.94, 23.07, 58.10, 25.02))):
        solid("bell intermediate landing " + label + " parapet", [box], 0, 4.1, ashlar,
              "continuous landing guard and bearing", "tower-base")
    solid("bell upper well south guard", [(55.9, 23.07, 58.10, 23.23)], 5.76, 7.08, ashlar,
          "bell gallery opening guard", "bell")
    # The flight-side walls rise with the flights below the deck. The upper
    # level needs continuous guards across the remaining open sides as well.
    for label, x0, x1 in (("west", 55.86, 56.00), ("east", 58.08, 58.22)):
        solid("bell upper well " + label + " guard", [(x0, 17.8, x1, 23.15)], 5.76, 7.08, ashlar,
              "bell gallery opening guard", "bell")
    assert retired <= old.keys()
    return ground, retired, parts, portal_records, stair_parts


def slope_wall(parts, label, x0, x1, z0, z1, y0, y1, mat):
    points = [(x0,-z0,0), (x1,-z0,0), (x1,-z1,0), (x0,-z1,0),
              (x0,-z0,y0), (x1,-z0,y0), (x1,-z1,y1), (x0,-z1,y1)]
    faces = [(0,1,2,3), (7,6,5,4), (0,4,5,1), (1,5,6,2), (2,6,7,3), (3,7,4,0)]
    mesh = bpy.data.meshes.new(label)
    mesh.from_pydata(points, [], faces)
    mesh.update()
    mesh.materials.append(mat)
    obj = bpy.data.objects.new("Monastery " + label, mesh)
    bpy.context.collection.objects.link(obj)
    obj["role"], obj["zone"] = "closed founded stair string wall", "tower-base"
    parts.append(obj)


def guidance(layout, portals):
    links = {link["id"]: link for link in layout["links"]}
    prior = {}

    def guide(id, points, width=None):
        prior[id] = copy.deepcopy(links[id])
        links[id]["points"] = [list(point) for point in points]
        if width is not None:
            links[id]["width"] = width
        links[id]["note"] = "Nonphysical circulation guide through built floors, doorway sills and landings."

    def level(id, points, y=0, width=None):
        guide(id, [(x,y,z) for x,z in points], width)

    public = (32, 68)
    north, south, west, east = (29, 17), (29, 53), (11, 36), (47, 36)
    tower, service, dining, crypt = (52.97, 24.5), (64, 23), (59, 40), (23, 17.40)
    level("L01", [(32,76.372), (32,74), public])
    level("L03", [(32,59), (32,56), (32,54.9), south])
    level("L04", [south, (11,53), (11,50), west])
    level("L05", [west, (11,22), (11,20.98), (23.6,20.98), (23.6,17), north])
    level("L06", [north, (47,17), (47,22), east])
    level("L09", [(29,36), (29,22), (29,20.98), (23.6,20.98), (23.6,17), north], width=1.65)
    level("L14", [east, (47,40), (50,40), dining])
    level("L15", [east, (47,26), (50,26), (51.4,26), tower])
    guide("L16", [(tower[0],0,tower[1]), (52.97,0,17.0), (55,0,17.0), (55,0,17.8),
                  (55,3,23.07), (55,3,24.10), (57.04,3,24.10), (57.04,3,23.07),
                  (57.04,6,17.8), (57.04,6,17.0), (58.86,6,17.0)])
    level("L17", [tower, (52.97,27), (59.03,27), (59.03,23), (60,23), service], width=1.40)
    level("L18", [service, (64,30), (64,40), dining])
    guide("L19", [(29,0,17), (32.5,0,17), (32.5,0,19), (31.7,0,19),
                  (26.3,-3,19), (23,-3,19), (23,-3,crypt[1])])
    level("L20", [crypt, (21.25,17.20), (18,17.20), (18,16), (18,9)], -3)
    guide("L21", [(23,-3,crypt[1]), (23,-3,19), (18,-3,19), (12.6,0,19),
                  (11,0,19), (11,0,22), (11,0,36)])
    layout["authored_doorways"] = portals
    layout["monastery_architecture"] = dict(
        garden_beds=GARDEN_BEDS, bell_floor=BELL_FLOOR, bell_stair_well=BELL_WELL,
        stair_runs=STAIRS, stair_risers_per_flight=RISERS, bell_intermediate_landing=LANDING,
        catacomb_stair_wells=CRYPT_WELLS, circulation_guides_are_nonphysical=True)
    maps = {"L16": {3:0, 7:34}, "L19": {3:0}, "L21": {2:0}}
    added = [dict(link="L16", segment=34, layout_segment=7, points=links["L16"]["points"][7:9])]
    return prior, maps, added


def build():
    roster = json.loads(BASELINE.read_bytes())
    source = next(row for row in roster["active_sources"] if row["id"] == SITE)
    layout = json.loads((ROOT / source["layout"]).read_bytes())
    meta = json.loads((ROOT / source["construction"]).read_bytes())
    assert sha(ROOT / source["glb"]) == source["glb_sha256"]
    assert sha(ROOT / source["native"]) == source["native_sha256"]
    prior_terrain = [part for part in split_native(ROOT / source["native"]) if part[0].startswith("terrain-")]
    assert prior_terrain == split_native(OLD_WORLD / "terrain-physics.hcs2")
    OUT.mkdir(parents=True, exist_ok=True)
    objects = import_scene(ROOT / source["glb"])
    source_ground, retired, built, portals, stairs = geometry(objects)
    prior_guides, mapping, added = guidance(layout, portals)
    ground = terrain_surface(SITE, source_ground, WORLD, PROGRAM)
    ground["role"] = "saved editor terrain / monastery geographic courts"
    grit = ground.data.materials[-1]
    grit.name = "Monastery compacted courtyard aggregate"
    grit.diffuse_color = (.44, .41, .35, 1)
    for name in retired:
        bpy.data.objects.remove(objects[name], do_unlink=True)
    files = {rel(p): sha(p) for p in WORLD.rglob("*") if p.is_file() and not p.name.endswith(".lock")}
    layout["authoritative_saved_terrain"] = dict(world=rel(WORLD), files_sha256=files)
    layout_path = OUT / "authored-layout.json"
    write(layout_path, layout)
    glb, native, blend = (OUT / (STEM + suffix) for suffix in (".glb", ".hcs2", ".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend), compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground, *built]:
        obj.select_set(True)
    additions = ROOT / "build/monastery-cloister-additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(additions), export_format="GLB", export_extras=True, use_selection=True)
    append(ROOT / source["glb"], additions, glb, remove_mesh_names=sorted(retired))
    replace_terrain_on_export(ROOT / source["native"], native, retired, built, WORLD, False)

    previous = copy.deepcopy(meta["stair_components"])
    preserved = [name for name in previous if name.split(":",1)[-1] not in retired]
    assert len(preserved) == 34
    old_map = copy.deepcopy(meta.get("source_segment_map", {}))
    meta.update(status="MONASTERY_GEOGRAPHIC_COURTS_AND_FOUNDED_BELL_STAIR_RUNTIME_OPEN",
                source_glb=source["glb"], source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"], source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"], before_layout_sha256=sha(ROOT / source["layout"]),
                layout=rel(layout_path), layout_sha256=sha(layout_path), before_stair_components=previous,
                stair_components=preserved+stairs, before_segment_map=old_map, source_segment_map={**old_map, **mapping},
                interface_added_flights=added, saved_world=rel(WORLD), saved_world_files_sha256=files,
                terrain_mesh=ground.name, retired_study_strips=sorted(name for name in retired if " support " in name),
                removed_source_meshes=sorted(retired), new_portals=portals,
                new_interface_parts=[obj.name for obj in built], builder=rel(Path(__file__)),
                builder_sha256=sha(Path(__file__)), glb_sha256=sha(glb), physics_sha256=sha(native),
                blend_sha256=sha(blend))
    write(OUT / "construction.json", meta)
    write(OUT / "quarantine.json", dict(source=source, retired=sorted(retired), superseded_guides=prior_guides))
    row = dict(source, construction=rel(OUT / "construction.json"), layout=rel(layout_path), glb=rel(glb),
               native=rel(native), blend=rel(blend), glb_sha256=sha(glb), native_sha256=sha(native))
    roster["active_sources"] = [row if old["id"] == SITE else old for old in roster["active_sources"]]
    roster["baseline"] = rel(BASELINE)
    next(row for row in roster["sites"] if row["id"] == SITE)["decision"] = meta["status"]
    write(PASS / "roster.json", roster)



def review():
    layout = json.loads((OUT / "authored-layout.json").read_bytes())
    meta = json.loads((OUT / "construction.json").read_bytes())
    glb, native = (OUT / (STEM + suffix) for suffix in (".glb", ".hcs2"))
    original, binary = read(ROOT / meta["source_glb"])
    revised, revised_binary = read(glb)
    assert revised_binary[:len(binary)] == binary
    for field in ("meshes", "materials", "accessors", "bufferViews", "textures", "images"):
        previous = original.get(field, [])
        assert revised.get(field, [])[:len(previous)] == previous, field
    removed = set(meta["removed_source_meshes"])
    for index, node in enumerate(original["nodes"]):
        expected = dict(node)
        if node.get("name") in removed:
            expected.pop("mesh", None)
        assert revised["nodes"][index] == expected, node.get("name")
    retained = [part for part in split_native(ROOT / meta["source_physics"])
                if not part[0].startswith("terrain-") and part[0].split(":",1)[-1] not in removed]
    assert split_native(native)[:len(retained)] == retained
    objects = import_scene(glb)
    ground = objects[meta["terrain_mesh"]]
    assert face_keys(records(ground)) == face_keys(native_faces(WORLD / "terrain-physics.hcs2"))
    assert not any(obj.get("role","").startswith("rough route support") for obj in objects.values())
    collision = read_parts(native)
    for name in meta["new_interface_parts"]:
        obj = objects[name]
        assert face_keys(records(obj)) == face_keys(collision[name][1]), name
        mesh = bmesh.new()
        mesh.from_mesh(obj.data)
        bmesh.ops.remove_doubles(mesh, verts=list(mesh.verts), dist=.00001)
        assert all(edge.is_manifold for edge in mesh.edges) and mesh.calc_volume(signed=True) > 0, name
        mesh.free()
    vertices, faces = [], []
    for obj in objects.values():
        start = len(vertices)
        vertices.extend(obj.matrix_world @ vertex.co for vertex in obj.data.vertices)
        faces.extend(tuple(start + index for index in face.vertices) for face in obj.data.polygons)
    tree = BVHTree.FromPolygons(vertices, faces)

    def trace(a, b):
        a, b = Vector(a), Vector(b)
        return tree.ray_cast(a, (b-a).normalized(), (b-a).length)[0]

    def standing(x, y, z, tolerance):
        hit = trace((x,-z,y+.055),(x,-z,y-.14))
        assert hit is not None and abs(hit.z-y) <= tolerance, (x,y,z,hit)
        assert trace((x,-z,y+.071),(x,-z,y+1.95)) is None, (x,y,z,"headroom")

    aperture, sills = 0, 0
    for door in meta["new_portals"]:
        base, fixed, center = door["sill_y"], door["boundary"], door["along"]
        for percentage in (-.34,0,.34):
            offset = center + door["clear_width"] * percentage
            for height in (.32,1.22,door["clear_height"]-.1):
                y = base + height
                a, b = (((offset,-fixed-.42,y),(offset,-fixed+.42,y)) if door["axis"] == "z"
                        else ((fixed-.42,-offset,y),(fixed+.42,-offset,y)))
                assert trace(a,b) is None, (door["id"],a,b)
                aperture += 1
            for delta in (-.20,-.06,.06,.20):
                x,z = (offset,fixed+delta) if door["axis"] == "z" else (fixed+delta,offset)
                standing(x,base,z,.046)
                sills += 1
    tread = 0
    for x0,x1,start,end,low,high in STAIRS:
        for i in range(RISERS):
            z = start + (end-start)*(i+.5)/RISERS
            y = low + (high-low)*(i+1)/RISERS
            standing((x0+x1)/2,y,z,.0001)
            tread += 1
    controls = [(x,0,z) for x in (31.2,32,32.8) for z in (76.2,76.372,78,80,82)]
    controls += [(x,0,z) for x in (27.5,29,30.5) for z in (24,30,42,48)]
    controls += [(x,0,z) for x in (61,64,67) for z in (21,23,28)]
    controls += [(55,3,24.1),(57.04,3,24.1),(57.04,6,16.9),(52.97,0,24.5)]
    for x,y,z in controls:
        standing(x,y,z,.046)

    van_triangles = place_review_van(layout["staging"])
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x, scene.render.resolution_y = 1280, 920
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    scene.camera = cam
    CAPTURES.mkdir(parents=True, exist_ok=True)
    views = [
        ("overview", (48,-110,104), (31,-42,0), 119),
        ("roadhead-and-gate", (26,-74,1.62), (32,-63,1.45), 0),
        ("herb-court", (30.4,-46,1.62), (24,-24,1.4), 0),
        ("tower-receiving-court", (66.5,-25,1.61), (60,-23,1.6), 0),
        ("bell-lower-flight", (55,-17.03,1.62), (55,-23.5,3.18), 0),
        ("bell-upper-flight", (57.04,-24.65,4.62), (57.04,-17.4,6.43), 0),
        ("catacomb-approach", (23,-18.8,-1.38), (11.8,-19,0), 0),
        ("tower-section", (75,-12,34), (55,-22,3), 18),
    ]
    captures = []
    for label, pos, target, scale in views:
        hidden = []
        if label == "tower-section":
            hidden = [name for name in objects if name == meta["terrain_mesh"] or name.startswith(
                ("Monastery tower service elevation", "Monastery tower service door",
                 "Wall bell N", "Wall bell E", "Wall tower-base N", "Ceiling cap bell", "Monastery bell floor around",
                 "Monastery bell stair east", "Monastery bell upper well east", "bell-tower"))]
        for name in hidden:
            objects[name].hide_render = True
        cam.data.type = "ORTHO" if scale else "PERSP"
        cam.data.ortho_scale = scale or 1
        cam.data.lens = 23
        cam.location = pos
        cam.rotation_euler = (Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        png = CAPTURES / (label+".png")
        scene.render.filepath = str(png)
        bpy.ops.render.render(write_still=True)
        for name in hidden:
            objects[name].hide_render = False
        captures.append(dict(path=rel(png),sha256=sha(png),source_glb_sha256=sha(glb),hidden=hidden))
    write(OUT / "export-verification.json",
          dict(glb_sha256=sha(glb), native_sha256=sha(native), retained_native_records=len(retained),
               added_closed_parts=len(meta["new_interface_parts"]), terrain_matches_editor=True,
               doorway_aperture_controls=aperture, door_sill_standing_controls=sills,
               new_stair_tread_and_headroom_controls=tread, non_corridor_standing_controls=len(controls),
               van_triangle_count=van_triangles, van_source_sha256=sha(VAN / "geometry.json"), captures=captures))
    print("MONASTERY EXPORT REVIEW", aperture, sills, tread, len(controls), flush=True)


if __name__ == "__main__":
    if "--review-only" not in sys.argv:
        build()
    if "--build-only" not in sys.argv:
        review()
