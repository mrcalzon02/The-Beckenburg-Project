"""Reopen the saved College campus, compare checked render and collision, and draw the residence plan."""
from collections import Counter
from html import escape
from pathlib import Path
import json
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import image_identity, read
from build_college_doors import aabb
from build_college_electrical import point
from build_college_residence import BASE, FLOOR, STEM
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry


def verify_geometry(parts, data, retained_names):
    scene = bpy.context.scene
    for part in parts:
        name = part[0]
        geometry = [face for face, _ in records(scene.objects[name])]
        assert match_geometry(triangles(part), geometry), (name, part[2])

    floors = {item["room"]: bvh([scene.objects["Floor " + item["room"]]]) for item in data["furniture"]}
    retained = []
    for obj in scene.objects:
        if obj.type != "MESH" or obj.name not in retained_names:
            continue
        box = aabb(obj)
        if box[3] > 42 and box[0] < 62 and box[4] > FLOOR + .004 and box[1] < 6 and box[5] > 30 and box[2] < 66:
            retained.append((obj, box))
    bearings, retained_contact_checks = 0, 0
    for item in data["furniture"]:
        objects = [scene.objects[name] for name in item["parts"]]
        low, high = item["size"]
        possible = [obj for obj, box in retained
                    if all(min(high[axis], box[axis + 3]) - max(low[axis], box[axis]) > .004 for axis in range(3))]
        if possible:
            model = bvh(objects)
            for obstacle in possible:
                assert not model.overlap(bvh([obstacle])), (item["key"], obstacle.name)
                retained_contact_checks += 1
        vertices = [obj.matrix_world @ v.co for obj in objects for v in obj.data.vertices]
        bottom = min(p.z for p in vertices)
        assert abs(bottom - FLOOR) < .0002, item["key"]
        contacts = {(round(p.x, 4), round(p.y, 4)) for p in vertices if p.z < bottom + .008}
        assert len(contacts) == item["foot_contact_count"], (item["key"], len(contacts))
        for x, y in contacts:
            hit, normal, _, _ = floors[item["room"]].ray_cast(
                Vector((x, y, FLOOR + .03)), Vector((0, 0, -1)), .09)
            assert hit is not None and abs(hit.z - FLOOR) < .001 and normal.z > .99, (item["key"], x, -y)
            bearings += 1
    return bearings, retained_contact_checks


def verify(stage):
    data = json.loads((stage / "residence.json").read_bytes())
    assert {ext: sha(stage / (STEM + "." + ext)) for ext in data["exports"]} == data["exports"]
    assert {ext: sha(BASE / "college" / ("college-services." + ext)) for ext in data["source"]} == data["source"]
    for item in data["assets"].values():
        assert sha(ROOT / item["path"]) == item["sha256"], item["path"]
    old, buffer = read(BASE / "college/college-services.glb")
    new, next_buffer = read(stage / (STEM + ".glb"))
    assert next_buffer.startswith(buffer)
    replaced = set(data["replaced_wall_meshes"])
    for before, after in zip(old["nodes"], new["nodes"]):
        expected = {k: v for k, v in before.items() if k != "mesh"} if before.get("name") in replaced else before
        assert after == expected, before.get("name")
    for field in ("bufferViews", "accessors", "meshes", "materials", "samplers"):
        assert new[field][:len(old[field])] == old[field], field
    texture_bindings = 0
    for before, after in zip(old["textures"], new["textures"]):
        assert {k: v for k, v in before.items() if k != "source"} == {k: v for k, v in after.items() if k != "source"}
        assert image_identity(old, buffer, before["source"]) == image_identity(new, next_buffer, after["source"])
        texture_bindings += 1
    assert len(new["images"]) <= 64
    assert len(new["images"]) == len({image_identity(new, next_buffer, i) for i in range(len(new["images"]))})
    previous_runtime = (BASE / "verification/runtime.tsv").read_bytes().splitlines()
    current_runtime = (stage / "runtime.tsv").read_bytes().splitlines()
    assert current_runtime[1:len(previous_runtime)] == previous_runtime[1:]
    assert len(current_runtime) - len(previous_runtime) == 2
    assert all(row.startswith(b"LOCAL_DOOR\tres-") for row in current_runtime[len(previous_runtime):])

    previous_physics = native_records(BASE / "college/college-services.hcs2")
    retained = [part for part in previous_physics if part[0].split(":", 1)[-1] not in replaced]
    all_parts = native_records(stage / (STEM + ".hcs2"))
    assert all_parts[:len(retained)] == retained
    new_parts = all_parts[len(retained):]
    assert len(new_parts) == data["added_native_parts"]
    assert sum(p[2] for p in new_parts) == data["added_native_triangles"]
    for door in data["doors"]:
        if door["accessible"]:
            assert "static_closure" not in door
        else:
            core = door["static_closure"][-1]
            assert any(name == core and closed for name, closed, *_ in new_parts), core

    retained_names = {node["name"] for node in old["nodes"] if "mesh" in node and node["name"] not in replaced}
    bpy.ops.wm.open_mainfile(filepath=str(stage / (STEM + ".blend")))
    editable_bearings, editable_contacts = verify_geometry(new_parts, data, retained_names)
    packed_images = sum(bool(image.packed_file) for image in bpy.data.images)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(stage / (STEM + ".glb")))
    exported_bearings, exported_contacts = verify_geometry(new_parts, data, retained_names)
    assert (editable_bearings, editable_contacts) == (exported_bearings, exported_contacts)
    count = Counter(row["asset"] for row in data["furniture"])
    assert set(count.values()) == {16} and sum(count.values()) == 64
    return data, dict(
        source_export=data["exports"], retained_collision_components=len(retained), checked_source_texture_bindings=texture_bindings,
        original_image_entries=len(old["images"]), fitted_image_entries=len(new["images"]), packed_source_images=packed_images,
        measured_floor_contacts_per_export=exported_bearings, retained_geometry_bvh_contact_checks=exported_contacts,
        changed_triangle_matches_per_export=data["added_native_triangles"],
        floor_m=FLOOR, furniture_count_by_asset=count, fitted_working_doors=2, captive_closed_doors=6)


def draw_plan(stage, data, target):
    layout = json.loads((stage / "authored-layout.json").read_bytes())
    rooms = {door["rooms"][1]: door for door in data["doors"]}
    zone_ids = set(rooms) | {"upper-res-hall", "res-bridge-approach", "upper-north-cross"}
    scale = 16

    def position(x, z):
        return 70 + scale*(x-42), 126 + scale*(z-26)

    out = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 780 906" role="img" aria-labelledby="title desc">',
           '<title id="title">Beckenburg College upper residence fit-out</title>',
           '<desc id="desc">Measured room, furniture and doorway plan at elevation 3.60 metres. Two rooms can be entered from the residential corridor and six have fixed leaves.</desc>',
           '<rect width="780" height="906" fill="#f7f4ea"/>',
           '<g font-family="Segoe UI,Arial,sans-serif" fill="#243535" stroke-linejoin="round">']

    def label(x, y, value, size=14, weight=500):
        out.append(f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}">{escape(value)}</text>')

    def rectangle(x, z, u, v, color, line, width=1):
        a, b = position(x, z)
        out.append(f'<rect x="{a:.2f}" y="{b:.2f}" width="{(u-x)*scale:.2f}" height="{(v-z)*scale:.2f}" fill="{color}" stroke="{line}" stroke-width="{width}"/>')

    label(56, 58, "Beckenburg College", 34, 650)
    label(57, 89, "Upper residence   /   finished floor +3.60 m", 18)
    for zone in layout["zones"]:
        key = zone["id"]
        if key not in zone_ids:
            continue
        color = "#e2e9de" if key not in rooms else ("#e1ece4" if rooms[key]["accessible"] else "#eae1d5")
        rectangle(*zone["bounds"], color, "#465858", 1.8)

    palette = {"bed_5": "#6c8d83", "level_student_desk": "#a77c50", "chair_mp_5": "#c49d59", "wardrobe_mp_1": "#776655"}
    for item in data["furniture"]:
        a, b = item["size"]
        rectangle(a[0], a[2], b[0], b[2], palette[item["asset"]], "#384643", .85)

    for route in ("L72", "L76"):
        link = next(row for row in layout["links"] if row["id"] == route)
        points = [position(x,z) for x,_,z in link["points"]]
        coords = " ".join(f"{x:.2f},{y:.2f}" for x, y in points)
        out.append(f'<polyline points="{coords}" fill="none" stroke="#04766a" stroke-width="2.4" stroke-dasharray="5 4"/>')

    for door in data["doors"]:
        x, y = position(door["boundary"], door["along"])
        half = door["clear_width"] * scale / 2
        swing = door["swing_direction"]
        color = "#04766a" if door["accessible"] else "#71483f"
        out.append(f'<path d="M{x} {y-half}v{half*2}" stroke="{color}" stroke-width="4.4"/>')
        if door["accessible"]:
            tip = x+swing*half*2
            sweep = 0 if swing > 0 else 1
            out.append(f'<path d="M{x} {y-half}H{tip} M{x} {y+half}A{half*2} {half*2} 0 0 {sweep} {tip} {y-half}" fill="none" stroke="{color}" stroke-width="1.5"/>')
        else:
            out.append(f'<path d="M{x-4} {y-4}l8 8m-8 0 8-8" stroke="#f7f4ea" stroke-width="1.2"/>')
    cx, cy=position(54,40)
    out.append(f'<path d="M{cx} {cy-11}v22" stroke="#e2e9de" stroke-width="3"/>')
    label(61, 844, "West", 13)
    label(351, 844, "East", 13)
    out.append('<path d="M392 143v-18m-5 6 5-6 5 6" fill="none" stroke="#243535" stroke-width="1.6"/>')
    label(386, 115, "N", 12)

    label(467, 170, "Eight twin rooms", 22, 650)
    label(467, 204, "16 bed places on the existing slab.")
    label(467, 230, "Each room has two desks and wardrobes.")
    labels = [
        ("#04766a", "Working privacy door"),
        ("#71483f", "Fixed closed leaf"),
        (palette["bed_5"], "Bed"),
        (palette["level_student_desk"], "Desk and chair"),
        (palette["wardrobe_mp_1"], "Wardrobe"),
    ]
    for i, (color, title) in enumerate(labels):
        offset = 292 + i*38
        out.append(f'<rect x="468" y="{offset-9}" width="19" height="13" fill="{color}"/>')
        label(501, offset+2, title)
    out.append('<path d="M469 493h24" stroke="#04766a" stroke-width="2.4" stroke-dasharray="5 4"/>')
    label(501, 498, "Walking guide to a working door")
    label(467, 550, "The east cross passage stays open.")
    label(467, 577, "Six furnished context rooms stay sealed.")
    out.append('<path d="M468 630h80m-80-5v10m80-10v10" stroke="#243535" fill="none" stroke-width="1.5"/>')
    label(469, 655, "5 m")
    out.append("</g></svg>")
    target.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")


def render_cutaway(path):
    scene = bpy.context.scene
    for obj in scene.objects:
        if obj.type != "MESH":
            continue
        x0, y0, z0, x1, y1, z1 = aabb(obj)
        role = obj.get("role", "")
        obj.hide_render = (
            x1 < 41.96 or x0 > 62.06 or z1 <= 29.8 or z0 >= 66.06
            or y1 < FLOOR-.035 or y0 >= 6.7 or "roof" in role or "ceiling" in role
            or "external water" in role
        )
    scene.render.engine = "BLENDER_WORKBENCH"
    shading = scene.display.shading
    shading.light = "STUDIO"
    shading.color_type = "MATERIAL"
    shading.show_cavity = True
    shading.cavity_type = "BOTH"
    shading.background_type = "WORLD"
    scene.world = bpy.data.worlds.new("College residence export review")
    scene.world.color = (.12, .145, .15)
    scene.view_settings.view_transform = "Standard"
    scene.render.resolution_x = 1050
    scene.render.resolution_y = 1360
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 43
    camera.location = point(52, 45, 51)
    camera.rotation_euler = (point(52, 3.6, 47) - camera.location).to_track_quat("-Z", "Y").to_euler()
    scene.camera = camera
    scene.render.filepath = str(path.resolve())
    bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    stage, output = (Path(p).resolve() for p in sys.argv[sys.argv.index("--") + 1:][:2])
    output.mkdir(parents=True, exist_ok=True)
    data, report = verify(stage)
    (output / "structural-readback.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    draw_plan(stage, data, output / "college-residence-plan.svg")
    render_cutaway(output / "college-native-residence-cutaway.png")
    print(json.dumps(report, indent=2))
