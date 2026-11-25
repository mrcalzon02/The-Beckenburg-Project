"""Compose the saved Dam terrain repair and founded east hall wall base."""
import argparse
import json
from pathlib import Path
import struct
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append, read
from build_circulation_construction import native_write
from build_college_containment import surveyed_terrain_loops
from build_college_grounds import import_scene, terrain_tree
from build_dam_east_hall import rel, render, sha
from build_dam_spillway_gallery import bounds, validate_solid
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from review_roster_macroterrain import records

BASE = ROOT / "development/levels/rebuild"
ROSTER = BASE / "northwatch-named-pass/roster.json"
STAGE = BASE / "dam-catwalk-ground-pass"
OUT = STAGE / "wld-09"
WORLD = STAGE / "worlds/wld-09/civil-terrain"
TERRAIN_PHYSICS = WORLD / "terrain-physics.hcs2"
GLB, COLLISION, BLEND = (OUT / ("east-bank" + ext) for ext in (".glb", ".hcs2", ".blend"))
STATUS = "DAM_EAST_BANK_EXCAVATION_RESTORED_AND_WALL_FOUNDED"
VIEWS = (
    ("east-bank-overview", (100, -62, 14), (69, -39, -4), 38),
    ("east-catwalk-standing", (72.05, -46.3, 1.62), (77, -37.5, -5), 0),
    ("east-bank-exterior", (80, -46, -4.77), (70.2, -39, -5.95), 0),
)


def assemble_terrain(original):
    vertices, faces = [], []
    for packet in sorted((WORLD / "scene-package/meshes").glob("site*.obj")):
        offset = len(vertices)
        for line in packet.read_text(encoding="utf8").splitlines():
            values = line.split()
            if values and values[0] == "v":
                x, y, z = map(float, values[1:])
                vertices.append((x, -z, y))
            elif values and values[0] == "f":
                faces.append(tuple(offset + int(i.split("/")[0]) - 1 for i in values[1:]))
    mesh = bpy.data.meshes.new("Dam saved east-bank ground")
    mesh.from_pydata(vertices, [], faces)
    mesh.update()
    for mat in original.data.materials:
        mesh.materials.append(mat)
    terrain = bpy.data.objects.new(mesh.name, mesh)
    bpy.context.collection.objects.link(terrain)
    terrain["role"] = "saved editor terrain / restored Dam east bank"
    terrain["saved_world"] = rel(WORLD)
    old_faces = terrain_tree(original)
    uv = mesh.uv_layers.new(name="World metre terrain UV")
    for face in mesh.polygons:
        previous = old_faces.find_nearest(face.center)[2]
        face.material_index = original.data.polygons[previous].material_index
        for index in face.loop_indices:
            v = vertices[mesh.loops[index].vertex_index]
            uv.data[index].uv = (v[0]/8, v[1]/8)
    assert face_keys(records(terrain)) == face_keys(native_faces(TERRAIN_PHYSICS))
    return terrain


def wall_footing(objects, terrain):
    wall = objects["WLD-09 hall E pier0 region0"]
    assert all(abs(a-b)<.002 for a,b in zip(bounds(wall), (70.26,22,70.5,46.35,-6,3), strict=True)), bounds(wall)
    mat = wall.data.materials[0]
    # The stem meets the lower floor and the wall base. The footing toes
    # share the exposed bank's bearing level below the measured soil.
    stem = solid("Dam east hall foundation stem", (70.18,22,70.50,48), -8.10,-6,mat,
                 "continuous concrete foundation stem", "east-lower-stair")
    foot = solid("Dam east hall buried strip footing", (69.95,22,70.90,48), -8.45,-8.10,mat,
                 "buried concrete strip footing", "east-lower-stair")
    soil = terrain_tree(terrain)
    controls = []
    for j in range(52):
        z = 22.25 + j*.5
        at = Vector((70.75,-z,10))
        hit = soil.ray_cast(at,Vector((0,0,-1)),22)[0]
        assert hit is not None and -7.86<hit.z<-6.04, (z,hit)
        controls.append([70.75,z,round(hit.z,5)])
    return [stem,foot], controls


def write_collision(previous, bearing_parts):
    native_write(previous,COLLISION,[],bearing_parts)
    dry = [p for p in split_native(COLLISION) if not p[0].startswith("terrain-")]
    payload = TERRAIN_PHYSICS.read_bytes()
    magic, chunks, triangles = struct.unpack_from(">III",payload)
    COLLISION.write_bytes(struct.pack(">III", magic,len(dry)+chunks,sum(p[1] for p in dry)+triangles)
                          + b"".join(p[2] for p in dry) + payload[12:])


def source_row():
    roster = json.loads(ROSTER.read_bytes())
    source = next(row for row in roster["active_sources"] if row["id"]=="WLD-09")
    return roster,source


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    roster,source = source_row()
    assert sha(ROOT/source["glb"])==source["glb_sha256"]
    assert sha(ROOT/source["native"])==source["native_sha256"]
    meta = json.loads((ROOT/source["construction"]).read_bytes())
    objects = import_scene(ROOT/source["glb"])
    old, = [o for o in objects.values() if o.get("role","").startswith("saved editor terrain")]
    assert face_keys(records(old)) == face_keys(native_faces(ROOT/meta["saved_world"]/"terrain-physics.hcs2"))
    saved = assemble_terrain(old)
    footing, controls = wall_footing(objects,saved)
    retired = old.name
    bpy.data.objects.remove(old,do_unlink=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND),compress=True)
    addition = ROOT/"build/dam-east-bank-additions.glb"
    export(addition,[saved,*footing])
    append(ROOT/source["glb"],addition,GLB,remove_mesh_names=[retired])
    write_collision(ROOT/source["native"],footing)
    world_assets = sorted(p for p in WORLD.rglob("*") if p.is_file() and not p.name.startswith("."))
    meta.update(status=STATUS,saved_world=rel(WORLD),previous_saved_world=meta["saved_world"],
        source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
        source_physics=source["native"],source_physics_sha256=source["native_sha256"],
        before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
        before_stair_components=meta["stair_components"],
        before_segment_map=meta["source_segment_map"],
        removed_source_meshes=[retired],new_interface_parts=[p.name for p in footing],
        restored_grade_footing_controls=controls,
        saved_world_files_sha256={rel(p):sha(p) for p in world_assets},
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(GLB),physics_sha256=sha(COLLISION),blend_sha256=sha(BLEND))
    write(OUT/"construction.json",meta)
    source.update(construction=rel(OUT/"construction.json"),glb=rel(GLB),native=rel(COLLISION),
                  blend=rel(BLEND),glb_sha256=sha(GLB),native_sha256=sha(COLLISION))
    next(row for row in roster["sites"] if row["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(ROSTER)
    write(STAGE/"roster.json",roster)


def review():
    import build_dam_east_hall
    build_dam_east_hall.CAPTURES = OUT/"captures"
    _,source = source_row()
    layout = json.loads((ROOT/source["layout"]).read_bytes())
    meta = json.loads((OUT/"construction.json").read_bytes())
    old,old_buffer = read(ROOT/source["glb"])
    current,new_buffer = read(GLB)
    assert new_buffer.startswith(old_buffer)
    for field in ("meshes","materials","accessors","bufferViews","textures","images"):
        assert current.get(field,[])[:len(old.get(field,[]))]==old.get(field,[]),field
    for i,node in enumerate(old["nodes"]):
        expected = dict(node)
        if node.get("name") in meta["removed_source_meshes"]:
            expected.pop("mesh",None)
        assert current["nodes"][i]==expected,node.get("name")
    actual = split_native(COLLISION)
    retained = [row for row in split_native(ROOT/source["native"]) if not row[0].startswith("terrain-")]
    assert actual[:len(retained)]==retained
    assert [row for row in actual if row[0].startswith("terrain-")]==split_native(TERRAIN_PHYSICS)

    objects = import_scene(GLB)
    terrain = objects["Dam saved east-bank ground"]
    old_terrain_loops = [1159,210,136]
    loops = surveyed_terrain_loops(terrain)
    assert len(loops)==2 and len(loops[0])==old_terrain_loops[0], [len(loop) for loop in loops]
    assert face_keys(records(terrain))==face_keys(native_faces(TERRAIN_PHYSICS))
    physical = read_parts(COLLISION)
    for name in meta["new_interface_parts"]:
        obj = objects[name]
        validate_solid(obj)
        assert face_keys(records(obj))==face_keys(physical[name][1]),name
    points,faces = [],[]
    for obj in objects.values():
        start = len(points)
        points.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in p.vertices) for p in obj.data.polygons)
    tree = BVHTree.FromPolygons(points,faces)
    for z in (23,28,34,38,42,47):
        for y in (-6.55,-7.05):
            hit = tree.ray_cast(Vector((71.10,-z,y)),Vector((-1,0,0)),1)[0]
            assert hit is not None and abs(hit.x-70.50)<.002, (z,y,hit)

    captures,van=render(layout,VIEWS,sha(GLB))
    import_scene(ROOT/source["glb"])
    earlier,prior_van=render(layout,VIEWS,sha(ROOT/source["glb"]),"-before")
    assert van==prior_van
    write(OUT/"export-verification.json",dict(
        glb_sha256=sha(GLB),native_sha256=sha(COLLISION),retained_native_records=len(retained),
        retired_terrain_rim_counts=old_terrain_loops,restored_terrain_rim_counts=list(map(len,loops)),
        founded_lower_wall_face_rays=12,buried_footing_grade_controls=len(meta["restored_grade_footing_controls"]),
        canonical_van_triangles=van,captures=captures+earlier))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--review-only",action="store_true")
    args = parser.parse_args(sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else [])
    (review if args.review_only else build)()
