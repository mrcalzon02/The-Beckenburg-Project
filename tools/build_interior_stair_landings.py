"""Rebuild the Manor service frames and the School ground-floor stair landing."""
import copy
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from append_static_glb import append, read
from build_coast_entrances import Structure, rectangle
from build_college_grounds import import_scene
from build_dam_spillway_gallery import native_write, rel, validate_solid
from build_deliberate_entrances import write
from build_mine_yard import punched_plate
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from prepare_cemetery_grounds import BASE
from review_roster_macroterrain import records, sha

BASELINE = BASE / "lookout-landing-pass/roster.json"
PASS = BASE / "interior-stair-landing-pass"
STEM = "interior-stair-landings"
CAPTURES = BASE / "captures/interior-stair-landing-batch"
STATUS = "INTERIOR_LANDINGS_AND_DOOR_APPROACHES_NATIVE_RUNTIME_OPEN"
SERVICE_DOORS = {
    2: (-3, 0, "cellar-stair", "cellar-pass", "L54"),
    4: (0, 3.6, "service-stair", "service", "L23"),
    5: (3.6, 7.2, "upper-service-stair", "upper-service", "L50"),
    6: (7.2, 10.2, "attic-stair", "attic-passage", "L51"),
}
SCHOOL_FLOOR = (27.2, 12, 30, 20)
SCHOOL_WELL = (27.4, 13.3, 28.65, 18.3)
VIEWS = {
    "MANSION": (
        ("ground-service-door", (41.5, -16.8, 1.55), (41.5, -11, 1.15), 0),
        ("first-floor-transfer", (42.87, -12.8, 5.2), (42.87, -5.8, 4.5), 0),
        ("attic-service-door", (41.5, -16.8, 8.75), (41.5, -11, 8.3), 0),
        ("great-stair-west-aisle", (21.95, -30.5, 1.5), (22.3, -24.75, .95), 0),
    ),
    "SCHOOL": (
        ("east-stair-aisle", (29.25, -19.1, 1.55), (29.25, -12.15, .95), 0),
        ("north-service-entry", (29.1, -10, 1.55), (29.1, -17, .8), 0),
        ("lower-flight", (28.04, -12.85, -1.45), (28.04, -18.2, .45), 0),
    ),
}


def build_manor(design, layout, meta):
    doors = {p["id"]: p for p in layout["authored_doorways"]}
    for number, (sill, next_floor, bay, passage, link) in SERVICE_DOORS.items():
        door_id = f"MANSION bay indexed door {number}"
        wall = f"MANSION bay wall {('z', 14, 40, 46, sill, next_floor)}"
        parts = [f"{wall} {part} region0" for part in ("pier0", "lintel0", "end")]
        assert set(parts + [door_id]) <= design.old.keys()
        masonry = design.old[parts[0]].data.materials[0]
        frame = design.old[door_id].data.materials[0]
        door = doors[door_id]
        assert (door["axis"], door["boundary"], door["along"], door["sill_y"]) == ("z", 14, 41.5, sill)
        design.face(
            f"{bay} south structural wall", "z", 14, (40, 46), sill, next_floor-sill-.12,
            masonry, [(door_id, 41.5, 1.35, door["clear_height"], [bay, passage])], bay,
        )
        fitted = next(obj for obj in design.built if obj.name.endswith(door_id + " fitted frame"))
        fitted.data.materials[0] = frame
        fitted["portal_id"] = door_id
        design.retired.update(parts + [door_id])
        door.update(clear_width=1.35, rooms=[bay, passage], link=link)
    meta["doorways"] = [doors.get(p["id"], p) for p in meta["doorways"]]
    for footprint, _, _, faces in meta["architectural_plan"]["rooms"]:
        if footprint == [40, 2, 46, 14]:
            faces["S"] = [[41.5, 1.35]]
    layout["interior_stair_landings"] = {
        "ground_service_transfer": [43.13, 0, 8],
        "first_floor_service_transfer": [42.87, 3.6, 8],
        "attic_service_transfer": [43.20, 7.2, 8],
        "service_aisle_clear_width_m": 1.16,
        "service_door_clear_width_m": 1.35,
        "service_door_south_wall_z": 14,
        "service_door_ids": [p["id"] for p in design.portals],
    }


def build_school(design, layout, meta):
    floor = "SCHOOL cell continuous floor service-stair region0"
    guard = "SCHOOL cell guard service bypass rim "
    return_guard = "SCHOOL cell guard service north shaft return 0"
    old_guards = {name for name in design.old if name.startswith(guard)}
    assert old_guards == {guard+str(i) for i in range(3)}
    floor_material = design.old[floor].data.materials[0]
    design.part(
        "service-bay full ground diaphragm",
        [rectangle(box) for box in punched_plate(SCHOOL_FLOOR, SCHOOL_WELL)],
        -.30, 0, floor_material, "floor", "service-stair",
    )
    design.retired.update({floor, return_guard} | old_guards)
    # Refit the indexed guard to the slab rim beside the 1.20 m gross flight.
    # The entire wall-supported plate has an actual rectangular stair opening.
    for name in sorted(old_guards | {return_guard}):
        old = design.old[name]
        obj = old.copy()
        obj.data = old.data.copy()
        bpy.context.collection.objects.link(obj)
        obj.name = name.replace("SCHOOL cell guard", "School grounded guard")
        is_return = name == return_guard
        for vertex in obj.data.vertices:
            pos = old.matrix_world @ vertex.co
            if is_return:
                pos.x = 27.45 + (pos.x-27.45)*(28.70-27.45)/(28.76-27.45)
            else:
                pos.x -= .06
                if name == guard + "0":
                    pos.y += .08 * (14.95 + pos.y) / (14.95 - 13.3)
            # Embed each post in the full slab while keeping the original top rail level.
            pos.z = -.025 + pos.z*(1.075/1.05)
            vertex.co = pos
        obj.matrix_world.identity()
        design.built.append(obj)
    cells = layout["structural_stair_cells"]
    opening = next(row for row in cells["floor_openings"] if row["zone"] == "service-stair")
    opening["rectangular_holes"] = [rectangle(SCHOOL_WELL)]
    meta["structural_stair_cells"] = cells
    layout["interior_stair_landings"] = {
        "service_ground_floor": SCHOOL_FLOOR,
        "service_ground_well": SCHOOL_WELL,
        "service_aisle": {"guard_axis_x": 28.70, "east_wall_inner_x": 29.76,
                         "clear_width_m": 1.02, "north_door": "SCHOOL-cell-service-stair-N-29.1",
                         "north_turn": [29.10, 0, 12.83]},
    }


def apply_guidance(site, layout, meta):
    links = {l["id"]: l for l in layout["links"]}
    original = copy.deepcopy(links)
    if site == "MANSION":
        routes = {
            "L05": [[22,0,47],[22,0,44],[22,0,42.9],[24,0,40]],
            "L11": [[24,0,40],[26.6,0,43],[28,0,43],[29.4,0,43],[34,0,46]],
            "L23": [[41.5,0,22],[41.5,0,14],[41.5,0,12.25],[43.13,0,12.25],[43.13,0,8]],
            "L28": [[43.13,0,8],[43.13,0,4.3],[43,0,3],[43,0,2]] + links["L28"]["points"][2:],
            "L36": [[24,3.6,37],[24,3.6,33.1],[21.5,3.6,33.1],[21.5,3.6,28.5],[20,3.6,28.5],[18,3.6,28.5]],
            "L44": [[37,3.6,26],[37,3.6,24],[37,3.6,22.65],[34,3.6,22]],
            "L46": [[34,3.6,22],[31,3.6,22],[31,3.6,20],[31,3.6,17]],
            "L47": [[34,3.6,22],[37,3.6,22],[37,3.6,20],[37,3.6,17]],
            "L50": [[41.5,3.6,22],[41.5,3.6,14],[41.5,3.6,12.3],[42.87,3.6,12.3],[42.87,3.6,8]],
            "L51": [[43.2,7.2,8],[43.2,7.2,12.55],[41.5,7.2,12.55],[41.5,7.2,14],[41.5,7.2,21]],
            "L58": [[22.5,0,30],[21.95,0,30],[21.95,0,25.05],[24,0,25.05],[24,0,26],[24,3.6,31.76],[24,3.6,37]],
            "L59": [[43,-3,8],[43,-3,11.8],[41.5,-3,11.8],[41.5,-3,10.4],[41.5,0,5.75],[41.5,0,4.2],[43.13,0,4.2],[43.13,0,8]],
            "L60": [[43.13,0,8],[43.13,0,4.2],[44.5,0,4.2],[44.5,0,5],[44.5,3.6,10.25],[44.5,3.6,11.5],[42.87,3.6,11.5],[42.87,3.6,8]],
            "L61": [[42.87,3.6,8],[42.87,3.6,11.9],[41.5,3.6,11.9],[41.5,3.6,11],[41.5,7.2,5],[41.5,7.2,3.8],[43.2,7.2,3.8],[43.2,7.2,8]],
        }
        widths = dict.fromkeys(("L23", "L50", "L59", "L60", "L61"), 1.10)
        widths["L08"] = 1.05
        bindings = {p[4]: f"MANSION bay indexed door {n}" for n, p in SERVICE_DOORS.items()}
        bindings["L08"] = "MANSION bay indexed door 1"
    else:
        routes = {
            "L14": [[26.5,0,25],[28.9,0,26],[30,0,26],[30.62,0,26],
                    [30.62,0,26.58],[31.8,0,26.58],[34,0,30]],
            "L36": [[28.6,0,19.2],[29.25,0,19.2],[29.25,0,13.15],[29.1,0,12.83],
                    [29.1,0,12],[29.1,0,10],[29.1,0,6],[52,0,6],[52,0,26]],
        }
        bindings = {
            "L01": "SCHOOL-x-25-31.5", "L02": "SCHOOL-x-25-34.5", "L03": "SCHOOL-z-33-28.75",
            "L08": "SCHOOL-cell-main-stair-N-28.6", "L09": "SCHOOL-cell-main-stair-W-21",
            "L14": "SCHOOL-cell-main-stair-E-26", "L19": "SCHOOL-cell-main-stair-E-21",
            "L27": "SCHOOL-cell-upper-stair-W-29", "L33": "SCHOOL-cell-lower-stair-W-13",
            "L36": "SCHOOL-cell-service-stair-N-29.1",
            "L37": "SCHOOL-court-science-W", "L38": "SCHOOL-court-class-a-W",
        }
        widths = dict.fromkeys(bindings, 1.05)
        widths.update(L36=1.0, L44=.98)
    for link_id, points in routes.items():
        links[link_id]["points"] = points
    for link_id, width in widths.items():
        links[link_id]["width"] = width
    doors = {p["id"]: p for p in layout["authored_doorways"]}
    for link_id, door_id in bindings.items():
        door = doors[door_id]
        x, z = (door["along"], door["boundary"]) if door["axis"] == "z" else (door["boundary"], door["along"])
        links[link_id].update(portal=door_id, opening="doorway", threshold=[x, door["sill_y"], z])
    for link_id in routes.keys() | widths.keys() | bindings.keys():
        links[link_id]["note"] = "Nonphysical guidance measured from retained or rebuilt floors, guards and door apertures."
    previous_map = copy.deepcopy(meta["source_segment_map"])
    meta["before_segment_map"] = previous_map
    for link_id, old_segments in previous_map.items():
        old_points, points = original[link_id]["points"], links[link_id]["points"]
        current = {}
        for index, component in old_segments.items():
            i = int(index)
            matches = [j for j in range(len(points) - 1) if points[j:j+2] == old_points[i:i+2]]
            assert len(matches) == 1, (link_id, old_points[i:i+2])
            current[str(matches[0])] = component
        meta["source_segment_map"][link_id] = current
    return [original[lid] for lid in original if original[lid] != links[lid]]


def build():
    roster = json.loads(BASELINE.read_bytes())
    for row in roster["active_sources"]:
        site = row["id"]
        if site not in VIEWS:
            continue
        source = dict(row)
        layout = json.loads((ROOT / row["layout"]).read_bytes())
        meta = json.loads((ROOT / row["construction"]).read_bytes())
        assert sha(ROOT / row["glb"]) == row["glb_sha256"]
        assert sha(ROOT / row["native"]) == row["native_sha256"]
        objects = import_scene(ROOT / row["glb"])
        design = Structure("Manor wing" if site == "MANSION" else "School service stair", objects)
        assert not design.strips
        (build_manor if site == "MANSION" else build_school)(design, layout, meta)
        old_guides = apply_guidance(site, layout, meta)
        bpy.data.objects.remove(design.template, do_unlink=True)
        for name in design.retired:
            bpy.data.objects.remove(objects[name], do_unlink=True)
        for obj in design.built:
            validate_solid(obj)
        out = PASS / site.lower()
        laid = out / "authored-layout.json"
        write(laid, layout)
        glb, native, blend = (out / (STEM+ext) for ext in (".glb", ".hcs2", ".blend"))
        bpy.ops.wm.save_as_mainfile(filepath=str(blend), compress=True)
        bpy.ops.object.select_all(action="DESELECT")
        for obj in design.built:
            obj.select_set(True)
        addition = ROOT / "build" / f"interior-stair-additions-{site.lower()}.glb"
        bpy.ops.export_scene.gltf(filepath=str(addition), export_format="GLB", export_extras=True, use_selection=True)
        append(ROOT / row["glb"], addition, glb, remove_mesh_names=sorted(design.retired))
        native_write(ROOT / row["native"], native, design.retired, design.built)
        meta.update(
            status=STATUS, source_glb=row["glb"], source_glb_sha256=row["glb_sha256"],
            source_physics=row["native"], source_physics_sha256=row["native_sha256"],
            before_layout=row["layout"], before_layout_sha256=sha(ROOT / row["layout"]),
            before_stair_components=list(meta["stair_components"]), layout=rel(laid), layout_sha256=sha(laid),
            removed_source_meshes=sorted(design.retired), new_interface_parts=[obj.name for obj in design.built],
            interior_stair_landings=layout["interior_stair_landings"], builder=rel(Path(__file__)),
            builder_sha256=sha(Path(__file__)), glb_sha256=sha(glb), physics_sha256=sha(native), blend_sha256=sha(blend),
        )
        write(out / "construction.json", meta)
        write(out / "quarantine.json", dict(source=source, retired_structural_parts=sorted(design.retired),
                                             superseded_guides=old_guides))
        row.update(construction=rel(out / "construction.json"), layout=rel(laid), glb=rel(glb), native=rel(native),
                   blend=rel(blend), glb_sha256=sha(glb), native_sha256=sha(native))
        next(entry for entry in roster["sites"] if entry["id"] == site)["decision"] = STATUS
    roster["baseline"] = rel(BASELINE)
    write(PASS / "roster.json", roster)


def segment_ray(tree, start, end):
    a, b = Vector(start), Vector(end)
    delta = b-a
    return tree.ray_cast(a, delta.normalized(), delta.length)[0]


def review_doors(tree, doors):
    aperture_count = sill_count = closed_jambs = 0
    for door in doors:
        fixed, center, y, w, h = (door[field] for field in
            ("boundary", "along", "sill_y", "clear_width", "clear_height"))
        near, far = door["frame_span"][0]-.14, door["frame_span"][1]+.14

        def point(across, depth, height):
            return (across, -depth, height) if door["axis"] == "z" else (depth, -across, height)

        for across in (center-.35*w, center, center+.35*w):
            for height in (y+.32, y+1.15, y+h-.14):
                assert segment_ray(tree, point(across, near, height), point(across, far, height)) is None, door
                aperture_count += 1
            for depth in (near, far):
                hit = segment_ray(tree, point(across, depth, y+.07), point(across, depth, y-.12))
                assert hit is not None and abs(hit.z-y) < .05, (door, across, depth, hit)
                sill_count += 1
        for side in (-1, 1):
            across = center + side*.54*w
            hit = segment_ray(tree, point(across, near, y+1.10), point(across, far, y+1.10))
            assert hit is not None, (door, fixed, across, "missing jamb")
            closed_jambs += 1
    return dict(aperture_rays=aperture_count, standing_sills=sill_count, solid_jamb_rays=closed_jambs)


def inspect_school_floor(objects, new_parts):
    floor = next(objects[name] for name in new_parts if "ground diaphragm" in name)
    deck = BVHTree.FromPolygons(
        [floor.matrix_world @ v.co for v in floor.data.vertices],
        [tuple(face.vertices) for face in floor.data.polygons],
    )
    distributions = dict(floor_top_and_soffit=0, open_well=0, guard_post_seats=0)
    for i in range(11):
        for j in range(32):
            x, z = SCHOOL_FLOOR[0]+.125+i*.25, SCHOOL_FLOOR[1]+.125+j*.25
            hit = segment_ray(deck, (x,-z,.07), (x,-z,-.17))
            in_well = SCHOOL_WELL[0] < x < SCHOOL_WELL[2] and SCHOOL_WELL[1] < z < SCHOOL_WELL[3]
            if in_well:
                assert hit is None, (x,z,hit)
                distributions["open_well"] += 1
            else:
                assert hit is not None and abs(hit.z) < 1e-4, (x,z,hit)
                soffit = segment_ray(deck, (x,-z,-.4), (x,-z,-.22))
                assert soffit is not None and abs(soffit.z+.3) < 1e-4, (x,z,soffit)
                distributions["floor_top_and_soffit"] += 1
    for name in new_parts:
        if "grounded guard" not in name:
            continue
        obj = objects[name]
        for vector in (obj.matrix_world @ v.co for v in obj.data.vertices):
            if vector.z >= -.02:
                continue
            assert segment_ray(deck, (vector.x,vector.y,.06), (vector.x,vector.y,-.14)) is not None, (name,vector)
            distributions["guard_post_seats"] += 1
    return distributions


def captures(site, digest, suffix=""):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x, scene.render.resolution_y = 1050, 740
    scene.render.resolution_percentage = 100
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    scene.camera = camera
    camera.data.clip_start = .018
    CAPTURES.mkdir(parents=True, exist_ok=True)
    results = []
    for label, position, target, scale in VIEWS[site]:
        camera.data.type = "ORTHO" if scale else "PERSP"
        camera.data.ortho_scale = scale or 1
        camera.data.lens = 21
        camera.location = position
        camera.rotation_euler = (Vector(target)-camera.location).to_track_quat("-Z", "Y").to_euler()
        image = CAPTURES / f"{site.lower()}-{label}{suffix}.png"
        scene.render.filepath = str(image)
        bpy.ops.render.render(write_still=True)
        results.append(dict(path=rel(image), sha256=sha(image), source_glb_sha256=digest))
    return results


def review():
    roster = json.loads((PASS / "roster.json").read_bytes())
    control_ids = {
        "MANSION": {f"MANSION bay indexed door {n}" for n in SERVICE_DOORS},
        "SCHOOL": {"SCHOOL-cell-service-stair-N-29.1", "SCHOOL-cell-main-stair-N-28.6",
                   "SCHOOL-cell-lower-stair-W-13", "SCHOOL-cell-main-stair-E-26"},
    }
    for row in roster["active_sources"]:
        site = row["id"]
        if site not in VIEWS:
            continue
        layout = json.loads((ROOT / row["layout"]).read_bytes())
        meta = json.loads((ROOT / row["construction"]).read_bytes())
        glb, native = ROOT / row["glb"], ROOT / row["native"]
        source, source_native = ROOT / meta["source_glb"], ROOT / meta["source_physics"]
        old, old_buffer = read(source)
        current, buffer = read(glb)
        assert buffer[:len(old_buffer)] == old_buffer
        for field in ("meshes", "materials", "accessors", "bufferViews", "textures", "images"):
            assert current.get(field, [])[:len(old.get(field, []))] == old.get(field, []), field
        retired = set(meta["removed_source_meshes"])
        retired_nodes = set()
        for i, node in enumerate(old["nodes"]):
            expected = dict(node)
            if node.get("name") in retired:
                expected.pop("mesh")
                retired_nodes.add(node["name"])
            assert current["nodes"][i] == expected, node.get("name")
        assert retired_nodes == retired
        baseline_records = split_native(source_native)
        retained = [row for row in baseline_records if row[0].split(":",1)[-1] not in retired]
        actual = split_native(native)
        assert len(baseline_records)-len(retained) == len(retired)
        assert actual[:len(retained)] == retained
        terrain = [part for part in actual if part[0].startswith("terrain-")]
        assert terrain and terrain == [part for part in baseline_records if part[0].startswith("terrain-")]

        objects = import_scene(glb)
        physical = read_parts(native)
        new_parts = meta["new_interface_parts"]
        for name in new_parts:
            validate_solid(objects[name])
            closed, native_faces = physical[name]
            assert closed == 1 and face_keys(records(objects[name])) == face_keys(native_faces), name
        vertices, faces = [], []
        for obj in objects.values():
            start = len(vertices)
            vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
            faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
        geometry = BVHTree.FromPolygons(vertices, faces)
        doors = [p for p in layout["authored_doorways"] if p["id"] in control_ids[site]]
        assert len(doors) == len(control_ids[site])
        inspection = review_doors(geometry, doors)
        if site == "SCHOOL":
            inspection.update(inspect_school_floor(objects, new_parts))
        after = captures(site, sha(glb))
        import_scene(source)
        before = captures(site, sha(source), "-before")
        write(PASS / site.lower() / "export-verification.json", dict(
            bindings={rel(p):sha(p) for p in (glb, native, source, source_native)},
            retained_native_records=len(retained), exact_terrain_records=len(terrain),
            closed_additions=len(new_parts), controls=inspection,
            captures=after+before,
        ))
        print("EXPORTED STRUCTURAL REVIEW", site, inspection, flush=True)


if __name__ == "__main__":
    review() if "--review-only" in sys.argv else build()
