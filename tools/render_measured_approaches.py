"""Reopen and photograph the exact three physical approach scenes."""
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/"tools"))

from build_college_grounds import import_scene, place_review_van
from build_interior_stair_landings import review_doors, segment_ray
from build_measured_approaches import BASE, OUT, read, relative, sha, write
from check_roster_foundations import tree
from prepare_cemetery_grounds import VAN

CAPTURES = BASE/"captures/measured-arrival-and-approach-batch"
DOORS = {
    "FARMHOUSE": {"FARMHOUSE bay indexed door "+n for n in ("1","2","3","6")},
    "WLD-08": {"WLD-08-x-20-19","WLD-08-z-44-53","WLD-08-x-62-14"},
    "SCHOOL": {"SCHOOL-z-36-22.5"},
}
VIEWS = {
    "FARMHOUSE": [
        ("farmhouse-ground-stair-transfer",(23.13,-20.2,1.55),(23.13,-13.1,1.1),0),
        ("farmhouse-barn-loft-threshold",(37.2,-17.5,1.57),(41.1,-17.35,1.15),0),
    ],
    "WLD-08": [
        ("northwatch-radio-generator-threshold",(24,-19,1.57),(15,-19,1.25),0),
        ("northwatch-generator-well-turn",(13.35,-19.45,1.55),(11.35,-16.15,.95),0),
        ("northwatch-canonical-roadhead",(42,-84,15),(32,-60,0),34),
    ],
    "SCHOOL": [
        ("school-van-and-forecourt",(41,-64,21),(25,-42,.35),37),
        ("school-cargo-ramp-receiver",(20.8,-55,2.1),(26.2,-51,1.18),0),
        ("school-ramp-to-the-foyer",(23.74,-50.98,1.58),(22.5,-35.6,1.22),0),
        ("school-foyer-threshold",(22.5,-38.65,1.57),(22.5,-33.6,1.35),0),
    ],
}


def review(row):
    site = row["id"]
    meta = read(ROOT/row["construction"])
    layout = read(ROOT/row["layout"])
    assert sha(ROOT/row["glb"]) == sha(ROOT/meta["source_glb"]) == row["glb_sha256"]
    assert sha(ROOT/row["native"]) == sha(ROOT/meta["source_physics"]) == row["native_sha256"]
    objects = import_scene(ROOT/row["glb"])
    mesh, owners = tree(objects.values())
    apertures = [d for d in layout["authored_doorways"] if d["id"] in DOORS[site]]
    assert len(apertures) == len(DOORS[site])
    counts = review_doors(mesh,apertures)
    support = []
    if site == "SCHOOL":
        targets = [
            (24.372,51,"Saved editor geographic earthworks SCHOOL"),
            (23.75,51,"Saved editor geographic earthworks SCHOOL"),
            (22.5,48,"Saved editor geographic earthworks SCHOOL"),
            (22.5,44,"Saved editor geographic earthworks SCHOOL"),
            (22.5,42.06,"Saved editor geographic earthworks SCHOOL"),
            (22.5,41.4,"SCHOOL south school forecourt pavement region0"),
            (22.5,39.35,"SCHOOL south school forecourt pavement region0"),
            (22.5,38.45,"SCHOOL architectural entrance landing and soil-bearing plinth"),
        ]
        for x,z,owner in targets:
            hit,_,face,_ = mesh.ray_cast(Vector((x,-z,.08)),Vector((0,0,-1)),.18)
            assert hit is not None and abs(hit.z) < .04 and owners[face] == owner, (x,z,hit,owners[face] if hit is not None else None)
            assert segment_ray(mesh,(x,-z,.09),(x,-z,1.94)) is None,(x,z,"obstructed standing volume")
            support.append(dict(point=[x,round(hit.z,5),z],physical_receiver=owner))
    van_triangles = place_review_van(layout["staging"])
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.render.resolution_x,scene.render.resolution_y = 1190,810
    scene.render.resolution_percentage = 100
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.data.lens = 22
    camera.data.clip_start = .018
    scene.camera = camera
    CAPTURES.mkdir(parents=True, exist_ok=True)
    files = []
    for name,location,target,scale in VIEWS[site]:
        camera.data.type = "ORTHO" if scale else "PERSP"
        camera.data.ortho_scale = scale or 1
        camera.location = location
        camera.rotation_euler = (Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        capture = CAPTURES/(name+".png")
        scene.render.filepath = str(capture)
        bpy.ops.render.render(write_still=True)
        files.append(dict(path=relative(capture),sha256=sha(capture),camera=list(location),target=list(target)))
    write(OUT/site.lower()/"export-review.json", dict(
        source_glb_sha256=row["glb_sha256"],source_native_sha256=row["native_sha256"],
        canonical_van_triangles=van_triangles,canonical_van_geometry_sha256=sha(VAN/"geometry.json"),
        door_controls=counts,school_walkable_receivers=support,captures=files,
    ))
    print("PHYSICAL APPROACH VISUAL",site,counts,"arrival receivers",len(support),flush=True)


if __name__ == "__main__":
    for row in read(OUT/"roster.json")["active_sources"]:
        if row["id"] in VIEWS:
            review(row)
