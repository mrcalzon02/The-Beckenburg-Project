"""Fit the College twin rooms to their existing slabs and residence corridor."""
from dataclasses import dataclass
from math import pi
from pathlib import Path
import json
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_architectural_doorways import frame_template
from build_circulation_construction import native_write
from build_college_doors import (Bay, WALL_ROLES, aabb, construct_bay, overlaps, retained_piece,
                                 validate_volume)
from build_college_electrical import point
from build_college_services import Fixture, Installation
from build_deliberate_entrances import solid
from build_roster_foundations import material
from review_roster_macroterrain import sha
from verify_college_electrical import native_records

BASE = ROOT / "development/levels/rebuild/college-services-pass"
STEM = "college-residence"
FLOOR = 3.6
CEILING = 7.2
WIDTH = 1.4
CLEAR = 2.22
BED = "bed_5"
DESK = "level_student_desk"
CHAIR = "chair_mp_5"
WARDROBE = "wardrobe_mp_1"
LEAF = "door_1_loose"


@dataclass(frozen=True)
class Residence:
    room: str
    hall: str
    threshold: float
    center: float
    accessible: bool
    inward: int

    @property
    def key(self):
        return self.room + "-door"

    def frame(self):
        return (self.key, self.hall, self.room, self.center, WIDTH, CLEAR, -1, self.inward)


# Existing supported thresholds surveyed in the source. Maintain the east cross
# passage between z38 and z42, the lift return, and the original electrical wall face.
WEST = (
    Residence("res-west-1", "upper-res-hall", 50, 37, True, -1),
    Residence("res-west-2", "upper-res-hall", 50, 45, False, -1),
    Residence("res-west-3", "upper-res-hall", 50, 53, False, -1),
    Residence("res-west-4", "upper-res-hall", 50, 61, False, -1),
)
EAST = (
    Residence("res-east-1", "res-bridge-approach", 54, 33, True, 1),
    Residence("res-east-2", "upper-res-hall", 54, 45, False, 1),
    Residence("res-east-3", "upper-res-hall", 54, 53, False, 1),
    Residence("res-east-4", "upper-res-hall", 54, 61, False, 1),
)
ROOMS = WEST + EAST

BAYS = (
    Bay("west residence partition", 50, 34, 66, FLOOR, CEILING, tuple(r.frame() for r in WEST)),
    Bay("east residence bridge partition", 54, 30, 38, FLOOR, CEILING, (EAST[0].frame(),)),
    Bay("east residence south partition", 54, 42, 66, FLOOR, CEILING, tuple(r.frame() for r in EAST[1:])),
)


def rebuild_partitions(scene, original):
    meshes = [obj for obj in scene.objects if obj.type == "MESH" and obj.get("role") in WALL_ROLES]
    collision_names = {r[0].split(":", 1)[-1] for r in original}
    retained, obsolete = [], set()
    for obj in meshes:
        bounds = aabb(obj)
        matches = [bay for bay in BAYS if overlaps(bounds, bay)]
        if not matches:
            continue
        assert len(matches) == 1 and obj.name in collision_names, obj.name
        fragment = retained_piece(obj, matches[0], bounds, len(retained) + 1)
        if fragment is not None:
            fragment.name = f"College residence original return {len(retained) + 1:02d}"
            retained.append(fragment)
        obsolete.add(obj.name)
    assert len(obsolete) == 17, sorted(obsolete)
    template = frame_template()
    finish = scene.objects["Wall upper-res-hall W"].data.materials[0]
    pieces, frames = [], []
    for bay in BAYS:
        parts, specs = construct_bay(bay, template, finish)
        for part in parts:
            validate_volume(part)
        pieces.extend(parts)
        frames.extend(specs)
    bpy.data.objects.remove(template, do_unlink=True)
    for name in sorted(obsolete):
        bpy.data.objects.remove(scene.objects[name], do_unlink=True)
    return [*retained, *pieces], frames, obsolete


def install_captive_leaf(fit, room, core_material):
    """Seat the canonical closed leaf on a solid core with continuous frame stops."""
    normal, center = room.threshold, room.center
    # Match the native closed pose. Local -X runs from the hinge toward the latch.
    frame = Matrix.Translation(point(normal, FLOOR, center - WIDTH / 2))
    frame = frame @ Matrix.Rotation(pi / 2, 4, "Z")
    frame = frame @ Matrix.Translation(Vector((0, .03, 0)))
    frame = frame @ Matrix.Diagonal((WIDTH, 1, CLEAR / 2.1, 1))
    leaves = fit.place(LEAF, frame, room.key + " fixed leaf", room.room, "Captive context leaf")
    core = solid("College " + room.key + " continuous inner seal",
                 [normal - .009, center - WIDTH / 2 - .012, normal + .009, center + WIDTH / 2 + .012],
                 FLOOR, FLOOR + CLEAR + .012, core_material, "Opaque inner panel and jamb stop", room.room)
    fit.add(core, "Captive opaque boundary beneath the native leaf", room.room)
    return [part.name for part in leaves] + [core.name]


def furniture(room, zones):
    x0, z0, x1, _ = zones[room.room]["bounds"]
    west = room.inward < 0
    bed_x = 45.75 if west else 58.25
    study_x = 48.10 if west else 55.90
    storage_x = x0 + .48 if west else x1 - .48
    # Keep the retained west radiator and east kitchen riser serviceable.
    north_wardrobe = {"res-west-1": 4.14, "res-east-3": 2.16}.get(room.room, 1.04)
    return (
        ("bed-a", BED, bed_x, z0 + 1.52, pi if west else 0),
        ("bed-b", BED, bed_x, z0 + 6.45, pi if west else 0),
        ("desk-a", DESK, study_x, z0 + .51, 0),
        ("desk-b", DESK, study_x, z0 + 7.49, pi),
        ("chair-a", CHAIR, study_x, z0 + 1.54, pi),
        ("chair-b", CHAIR, study_x, z0 + 6.46, 0),
        ("wardrobe-a", WARDROBE, storage_x, z0 + north_wardrobe, -pi / 2 if west else pi / 2),
        ("wardrobe-b", WARDROBE, storage_x, z0 + 6.68, -pi / 2 if west else pi / 2),
    )


def place_furniture(fit, zones):
    for room in ROOMS:
        for label, asset, x, z, yaw in furniture(room, zones):
            key = f"{room.room} {label}"
            source = fit.load(asset)
            rotation = Matrix.Rotation(-yaw, 4, "Z")
            bottom = min(p.z for p in source.corners)
            # Verify the actual load-bearing parts before placement. The bounding
            # rectangle alone cannot show a foot suspended above the local floor.
            contacts = sorted({(round(p.x, 4), round(p.y, 4)) for p in source.corners if p.z < bottom + .008})
            assert len(contacts) >= 3, key
            for ox, oy in contacts:
                offset = rotation @ Vector((ox, oy, 0))
                fit.require_floor(x + offset.x, FLOOR, z - offset.y, key)
            fit.floor_item(Fixture(key, room.room, asset, x, z, yaw), zones)
            placed = fit.fixtures[-1]
            placed["foot_contact_count"] = len(contacts)
            placed["rotation_radians"] = yaw
            placed["pivot"] = [x, FLOOR - bottom, z]
            lower, upper = placed["size"]
            for earlier in fit.fixtures[:-1]:
                if earlier["room"] != placed["room"]:
                    continue
                a, b = earlier["size"]
                separation = max(lower[0] - b[0], a[0] - upper[0], lower[2] - b[2], a[2] - upper[2])
                assert separation >= .16, (earlier["key"], placed["key"], separation)
    assert len(fit.fixtures) == 64


def manifest(path, hashes, frames):
    lines = (BASE / "verification/runtime.tsv").read_bytes().splitlines()
    lines[0] = f"SOURCE\t{hashes['glb']}\t{hashes['hcs2']}".encode("utf-8")
    for spec, room in zip(frames, ROOMS, strict=True):
        assert spec["id"] == room.key
        if not room.accessible:
            continue
        row = ("LOCAL_DOOR", room.key, room.hall, room.room, room.threshold, FLOOR, room.center,
               WIDTH, "x", "door", *spec["frame_span"], CLEAR, 1, -1, room.inward,
               sha(ROOT / "assets/runtime/models/door_1_loose.glb"))
        lines.append("\t".join(map(str, row)).encode("utf-8"))
    path.write_bytes(b"\n".join(lines) + b"\n")


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def build(out):
    out.mkdir(parents=True, exist_ok=True)
    source = {ext: BASE / "college" / ("college-services." + ext) for ext in ("blend", "glb", "hcs2")}
    prior = json.loads((BASE / "college/construction.json").read_bytes())
    expected = {"blend": "blend_sha256", "glb": "glb_sha256", "hcs2": "physics_sha256"}
    assert all(sha(path) == prior[expected[ext]] for ext, path in source.items()), "Unverified source"
    layout = json.loads((BASE / "college/authored-layout.json").read_bytes())
    zones = {row["id"]: row for row in layout["zones"]}
    assert all(zones[room.room]["y"] == FLOOR for room in ROOMS)
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    records = native_records(source["hcs2"])
    fit = Installation()
    walls, frames, obsolete = rebuild_partitions(fit.scene, records)
    core_material = material("College solid stained leaf lining", (.19, .13, .08))
    for frame, room in zip(frames, ROOMS, strict=True):
        frame["accessible"] = room.accessible
        for distance in (-.8, 0, .8):
            fit.require_floor(room.threshold + distance, FLOOR, room.center, room.key)
        if not room.accessible:
            frame["static_closure"] = install_captive_leaf(fit, room, core_material)
    place_furniture(fit, zones)

    pieces = [*walls, *fit.pieces]
    bpy.ops.object.select_all(action="DESELECT")
    for part in pieces:
        part.select_set(True)
    additions = out / "_additions.glb"
    targets = {ext: out / (STEM + "." + ext) for ext in source}
    bpy.ops.export_scene.gltf(filepath=str(additions), export_format="GLB", use_selection=True, export_extras=True)
    append(source["glb"], additions, targets["glb"], remove_mesh_names=obsolete, reuse_embedded_images=True)
    # The pierced source assets are surface obstacles. The continuous captive
    # door cores and all architectural pieces remain closed objects.
    surfaces = [part.name for part in fit.pieces if part.get("source_asset")]
    added = native_write(source["hcs2"], targets["hcs2"], obsolete, pieces, open_components=surfaces)
    retained = [r for r in records if r[0].split(":", 1)[-1] not in obsolete]
    assert native_records(targets["hcs2"])[:len(retained)] == retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]), compress=True)
    hashes = {ext: sha(path) for ext, path in targets.items()}
    assert targets["glb"].stat().st_size < 16_000_000, "The ordinary native GLB reader must load the exact export"
    manifest(out / "runtime.tsv", hashes, frames)

    # Use the inherited supported rectangular approaches at the same actual jambs.
    # Furniture placement was governed by room bearings and normal daily activities.
    layout["residence_fitout"] = dict(floor=FLOOR, rooms=list(r.room for r in ROOMS), bed_places=16,
                                       doors=frames, furnishing_schedule="residence.json")
    layout["fitted_door_schedule"] += [frame for frame in frames if frame["accessible"]]
    write_json(out / "authored-layout.json", layout)
    packet = dict(source={ext: sha(path) for ext, path in source.items()}, outline="authored-layout.json",
                  assets={key: dict(path=item.path.relative_to(ROOT).as_posix(), sha256=sha(item.path))
                          for key, item in sorted(fit.assets.items())},
                  replaced_wall_meshes=sorted(obsolete), retained_native_records=len(retained),
                  added_native_parts=len(added), added_native_triangles=sum(len(rows) for _, rows in added),
                  doors=frames, furniture=fit.fixtures, exports=hashes)
    write_json(out / "residence.json", packet)
    construction = dict(prior, scope="College upper residence fitted twin rooms and corridor partitions",
                        previous_construction=str((BASE / "college/construction.json").relative_to(ROOT).as_posix()),
                        glb_sha256=hashes["glb"], physics_sha256=hashes["hcs2"], blend_sha256=hashes["blend"],
                        layout_sha256=sha(out / "authored-layout.json"), residence_fitout="residence.json",
                        residence_fitout_sha256=sha(out / "residence.json"), residence_builder_sha256=sha(Path(__file__)))
    write_json(out / "construction.json", construction)
    print("COLLEGE RESIDENCE", len(fit.fixtures), "floor-seated pieces,", len(frames), "complete jamb and leaf sets")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
