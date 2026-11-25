"""Rebuild complete College wall bays, then publish fitted frame and guide datums."""
from dataclasses import dataclass
from pathlib import Path
import argparse
import json
import sys

import bpy
import bmesh

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_architectural_doorways import MODEL, frame_template
from build_circulation_construction import native_write
from build_deliberate_entrances import solid
from review_roster_macroterrain import records, sha
from verify_college_electrical import native_records

BASE = ROOT / "development/levels/rebuild/college-electrical-pass/college"
MASTER = ROOT / "development/levels/rebuild/college-ground-pass/college"
LEAF = ROOT / "assets/runtime/models/door_1_loose.glb"
WALL_ROLES = {"rough shell wall", "rough opening header", "retained clipped old wall"}
HALF_DEPTH = .08
TRIM = .085

@dataclass(frozen=True)
class Bay:
    name: str
    x: float
    z0: float
    z1: float
    floor: float
    top: float
    frames: tuple

# Coordinates describe complete common wall runs at the architectural datums.
# In particular, the west teaching wall is one uninterrupted construction run.
BAYS = (
    Bay("West teaching partition", 20, 22, 54, 0, 3.6, (
        ("L07", "teaching-hall", "lab-entry", 23, 1.4, 2.22, 1, -1),
        ("L06", "teaching-hall", "seminar", 28, 1.4, 2.22, 1, -1),
        ("L05", "teaching-hall", "lecture", 44, 1.4, 2.22, 1, -1),
    )),
    Bay("Meeting room partition", 28, 54, 58, 0, 3.6, (
        ("L14", "teaching-lobby", "meeting", 56, 1.4, 2.22, 1, 1),
    )),
    Bay("Upper library partition", 34, 16, 20, 3.6, 7.2, (
        ("L52", "upper-library-landing", "bridge-lobby", 18, 1.4, 2.22, 1, 1),
    )),
    Bay("Upper teaching link partition", 24, 12, 16, 3.6, 7.2, (
        ("L86", "upper-teaching", "teaching-library-link", 14, 1.4, 2.22, 1, 1),
    )),
    Bay("Maintenance and tunnel partition", 20, 24, 34, -3, 0, (
        ("L67", "west-tunnel", "maintenance", 28, 1.4, 2.22, 1, -1),
    )),
)
# These seven frames were already constructed with the actual stair bays.
# Keep the walls, bearings, flights and landings. Receive leaves on that joinery.
STAIR_FRAMES = (
    # The low landing turns west onto the first flight. Put the hinged
    # pocket east of the aperture so the ordinary approach clears the leaf.
    ("library-stair", "issue", 1, -1),
    ("upper-library-stair", "upper-library-landing", 1, 1),
    ("teaching-library-link", "upper-library-stair", -1, -1),
    ("west-tunnel", "west-cellar-stair", 1, -1),
    ("west-cellar-stair", "cross-tunnel", -1, 1),
    ("teaching-hall", "teaching-stair", -1, 1),
    ("upper-teaching", "upper-teaching-stair", 1, -1),
)

def aabb(obj):
    vs = [obj.matrix_world @ v.co for v in obj.data.vertices]
    return tuple(round(f(p[i] for p in [(v.x, v.z, -v.y) for v in vs]), 5)
                 for f in (min, max) for i in range(3))

def validate_volume(obj):
    mesh = bmesh.new()
    mesh.from_mesh(obj.data)
    bmesh.ops.remove_doubles(mesh, verts=list(mesh.verts), dist=1e-6)
    bmesh.ops.recalc_face_normals(mesh, faces=list(mesh.faces))
    assert all(e.is_manifold for e in mesh.edges) and mesh.calc_volume(signed=True) > 1e-6, obj.name
    mesh.to_mesh(obj.data)
    mesh.free()

def overlaps(bounds, bay):
    x0,y0,z0,x1,y1,z1 = bounds
    return (
        abs((x0+x1)/2 - bay.x) < .05 and x1-x0 < .30
        and min(z1, bay.z1)-max(z0, bay.z0) > 1e-4
        and min(y1, bay.top)-max(y0, bay.floor) > 1e-4
    )

def retained_piece(obj, bay, bounds, index):
    _, y0, z0, _, y1, z1 = bounds
    if bay.z0-.0001<=z0 and z1<=bay.z1+.0001 and bay.floor-.0001<=y0 and y1<=bay.top+.0001:
        return None
    # Clip only the previous solid, to retain the portion outside the surveyed
    # construction run. Opening geometry comes from the new wall schedule.
    copy = obj.copy()
    copy.data = obj.data.copy()
    bpy.context.scene.collection.objects.link(copy)
    copy.name = f"College bearing wall retained part {index:02d}"
    copy["role"] = "retained original partition beyond rebuilt bay"
    copy["retained_from"] = obj.name
    bpy.ops.mesh.primitive_cube_add(size=1, location=(
        bay.x,-(bay.z0+bay.z1)/2,(bay.floor+bay.top)/2))
    boundary = bpy.context.object
    boundary.scale = (.5,bay.z1-bay.z0,bay.top-bay.floor)
    bpy.context.view_layer.objects.active = copy
    m = copy.modifiers.new("Preserve outside of entire replaced wall run", "BOOLEAN")
    m.operation = "DIFFERENCE"
    m.solver = "EXACT"
    m.object = boundary
    bpy.ops.object.modifier_apply(modifier=m.name)
    bpy.data.objects.remove(boundary, do_unlink=True)
    assert copy.data.polygons, obj.name
    return copy

def fitted_frame(template, bay, key, center, width, height):
    object_ = template.copy()
    object_.data = template.data.copy()
    object_.name = f"College {key} fitted jambs and head"
    bpy.context.scene.collection.objects.link(object_)
    for vertex in object_.data.vertices:
        source = template.matrix_world @ vertex.co
        u = source.x * width if abs(source.x) <= .5 else (
            (.5 * width + (abs(source.x)-.5)*TRIM/.12) * (1 if source.x>0 else -1))
        depth = source.y*(2*HALF_DEPTH)/.18
        vertex.co = (bay.x+depth,-center-u,bay.floor+source.z*height/2.125)
    object_.matrix_world.identity()
    object_["role"] = "fitted doorway jambs and head"
    object_["source_asset"] = "level_blockout_doorway"
    object_["door_id"] = key
    return object_

def construct_bay(bay, template, mat):
    pieces, specs = [], []
    def part(a, b, y, label):
        if b-a>1e-4:
            obj = solid(f"College {bay.name} {label}", [bay.x-HALF_DEPTH,a,bay.x+HALF_DEPTH,b],
                        y,bay.top,mat,"complete authored wall partition",bay.name)
            pieces.append(obj)
    limit = bay.z0
    for key,a,b,center,width,clear,hinge,swing in bay.frames:
        left,right = center-width/2-TRIM,center+width/2+TRIM
        assert limit+.04<left and right<bay.z1-.04
        part(limit,left,bay.floor,f"pier before {key}")
        part(left,right,bay.floor+clear*2.275/2.125,f"full masonry head at {key}")
        pieces.append(fitted_frame(template,bay,key,center,width,clear))
        specs.append(dict(id=key, rooms=[a,b], axis="x", sill_y=bay.floor,
                          boundary=bay.x, along=center, frame_span=[bay.x-HALF_DEPTH,bay.x+HALF_DEPTH],
                          clear_width=width, clear_height=clear, leaves=1,
                          hinge_side=hinge, swing_direction=swing, rebuilt_wall=bay.name))
        limit=right
    part(limit,bay.z1,bay.floor,"end pier")
    return pieces,specs

def plan_guides(layout):
    # Square approaches to two existing fitted thresholds. The points are
    # travel guidance over retained floors and have no mesh representation.
    guidance = {
        "L06": [[22,0,32],[22,0,28],[20,0,28],[18.85,0,28],[13,0,29]],
        "L67": [[22,-3,32],[22,-3,28],[20,-3,28],[18.85,-3,28],[16,-3,29]],
    }
    links = {link["id"]:link for link in layout["links"]}
    for key,points in guidance.items():
        links[key]["points"] = points
        links[key]["note"] = "Square approach to the physically fitted jambs followed by a turn on the real room floor."
    # Use the south court directly at the existing junction, without making
    # the van-to-lobby journey double back from the north green anchor.
    site_walk = [*links["L01"]["points"],[38,0,78],[38,0,65]]
    court = links["L03"]["points"]
    pivot = court.index([38,0,65])
    layout["staging"]["cargo_route"] = [*site_walk,*court[pivot+1:]]
    layout["staging"]["note"] = "The cargo approach follows the saved forecourt, the south court and the fitted lobby door."
    return [*sorted(guidance),"van-to-lobby"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output",type=Path)
    out = parser.parse_args(sys.argv[sys.argv.index("--")+1:]).output.resolve()
    out.mkdir(parents=True,exist_ok=True)
    source,phys = (BASE / ("college-electrical"+suffix) for suffix in (".glb",".hcs2"))
    meta = json.loads((BASE/"construction.json").read_bytes())
    original_layout = MASTER/"authored-layout.json"
    layout = json.loads(original_layout.read_bytes())
    assert sha(source)==meta["glb_sha256"] and sha(phys)==meta["physics_sha256"]
    assert sha(original_layout)==meta["layout_sha256"]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.import_scene.gltf(filepath=str(source))
    bpy.context.view_layer.update()
    originals = [obj for obj in bpy.context.scene.objects if obj.type=="MESH"]
    retained, built, removed, specs = [], [], [], []
    source_records = native_records(phys)
    collision_names = {name.split(":",1)[-1] for name,*_ in source_records}
    for obj in originals:
        if obj.get("role") not in WALL_ROLES:
            continue
        bounds = aabb(obj)
        bays = [bay for bay in BAYS if overlaps(bounds,bay)]
        assert len(bays)<=1,(obj.name,bays)
        if not bays:
            continue
        assert obj.name in collision_names,obj.name
        retained_part = retained_piece(obj,bays[0],bounds,len(retained)+1)
        if retained_part:
            retained.append(retained_part)
        removed.append(obj.name)
    material = next(obj.data.materials[0] for obj in originals
                    if obj.get("role")=="rough shell wall" and obj.data.materials)
    template = frame_template()
    for bay in BAYS:
        parts,frame_rows=construct_bay(bay,template,material)
        built.extend(parts)
        specs.extend(frame_rows)
    bpy.data.objects.remove(template,do_unlink=True)
    excluded=set(removed)
    for obj in originals:
        if obj.name in excluded:
            bpy.data.objects.remove(obj,do_unlink=True)
    for obj in built:
        validate_volume(obj)
    pieces = [*retained,*built]
    for door in meta["new_portals"]:
        specs.append({**door,"leaves":2,"hinge_side":1,
                      "swing_direction":1 if door["axis"]=="x" else -1,"reuse_static_frame":True})
    for i,(a,b,hinge,swing) in enumerate(STAIR_FRAMES):
        frame = next(row for row in layout["authored_doorways"] if row["id"]==f"COLLEGE bay indexed door {i}")
        specs.append({**frame,"rooms":[a,b],"leaves":1,"hinge_side":hinge,
                      "swing_direction":swing,"reuse_static_frame":True})
    assert len({spec["id"] for spec in specs})==len(specs)==18
    adjusted = plan_guides(layout)
    layout["fitted_door_schedule"] = specs
    new_layout = out/"authored-layout.json"
    new_layout.write_text(json.dumps(layout,indent=2)+"\n",encoding="utf-8",newline="\n")

    bpy.ops.object.select_all(action="DESELECT")
    for obj in pieces:
        obj.select_set(True)
    bpy.context.view_layer.update()
    addition = out/"doorway-additions.glb"
    glb,hcs,blend = (out/("college-doors"+suffix) for suffix in (".glb",".hcs2",".blend"))
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",use_selection=True,export_extras=True)
    append(source,addition,glb,remove_mesh_names=removed)
    added = native_write(phys,hcs,excluded,pieces)
    remaining = [r for r in source_records if r[0].split(":",1)[-1] not in excluded]
    assert native_records(hcs)[:len(remaining)]==remaining
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    data = dict(status="COLLEGE_FITTED_DOOR_CONSTRUCTION",
                previous_glb_sha256=sha(source),previous_physics_sha256=sha(phys),
                previous_layout_sha256=sha(original_layout),layout_sha256=sha(new_layout),
                glb_sha256=sha(glb),physics_sha256=sha(hcs),blend_sha256=sha(blend),
                source_frame_sha256=sha(MODEL),source_leaf_sha256=sha(LEAF),
                static_door_schedule=specs,room_bays=[bay.__dict__ for bay in BAYS],
                replaced_wall_names=sorted(removed),
                retained_native_parts=len(remaining),retained_wall_remnants=len(retained),
                new_static_parts=len(added),new_static_triangles=sum(len(ts) for _,ts in added),
                guide_changes=adjusted)
    (out/"doors.json").write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("DOORS",len(specs),"fitted positions,",len(removed),"old wall components replaced,",len(added),"new static pieces")

if __name__=="__main__":
    main()
