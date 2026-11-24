"""Fit the common Cemetery and Mortuary service yard to both saved grades."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_college_containment import terrain_reader
from build_prison_sally_gate import export
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import BASE, load
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

REFUGES = BASE/"cemetery-refuge-pass"
PLANT = BASE/"mine-service-yard-pass"
OUTPUT = BASE/"cemetery-power-pass"
OFFSET = (12.65,.182,-14.15)
FLOOR = .20
OFFICE = (46.60,53.20,52.20,56.80)
RAMP = (48.60,50.20,58.35)


def relative(path):
    return path.relative_to(ROOT).as_posix()


def shift(point):
    return tuple(round(value+delta,6) for value,delta in zip(point,OFFSET,strict=True))


def foundation(grade):
    left,back,right,front=OFFICE
    west,east,toe=RAMP
    bearing=[]
    for xi in range(29):
        for zi in range(21):
            x,z=left+(right-left)*xi/28,back+(front-back)*zi/20
            y=grade(x,z)
            assert y is not None and -.30<y<FLOOR-.03,(x,z,y)
            bearing.append(y)
    concrete=material("Caretaker yard weathered concrete",(.37,.364,.325))
    # Closed slab with a continuous sloped entry from its formed edge to
    # the measured soil on each side of the yard's single service approach.
    points=[(left,back),(right,back),(right,front),(east,front),
            (east,toe),(west,toe),(west,front),(left,front)]
    toe_heights=[grade(x,toe)-.020 for x in (east,west)]
    heights=[FLOOR,FLOOR,FLOOR,FLOOR,*toe_heights,FLOOR,FLOOR]
    vertices=[(x,-z,-.37) for x,z in points]+[(x,-z,y) for (x,z),y in zip(points,heights,strict=True)]
    caps=[(8,9,10,11,14,15),(11,12,13),(11,13,14)]
    faces=[tuple(reversed(range(8))),*caps]
    faces.extend((i,(i+1)%8,(i+1)%8+8,i+8) for i in range(8))
    # The source outline in X/-Z is clockwise. Reverse the entire closed
    # shell before creating the ramp's authoritative walking triangles.
    faces=[tuple(reversed(f)) for f in faces]
    mesh=bpy.data.meshes.new("Caretaker founded slab and service ramp")
    mesh.from_pydata(vertices,[],faces)
    mesh.materials.append(concrete)
    mesh.update()
    assert all(mesh.polygons[i].normal.z>.96 for i in (1,2,3))
    uv=mesh.uv_layers.new(name="Poured concrete metre UV")
    for polygon in mesh.polygons:
        planar=abs(polygon.normal.z)>.65
        for slot in polygon.loop_indices:
            p=mesh.vertices[mesh.loops[slot].vertex_index].co
            uv.data[slot].uv=(p.x/2,(-p.y if planar else p.z)/2)
    slab=bpy.data.objects.new(mesh.name,mesh)
    bpy.context.scene.collection.objects.link(slab)
    slab["role"]="Continuously founded slab with measured walk-in ramp"
    for xi in range(9):
        x=west+(east-west)*xi/8
        at=grade(x,toe)
        target=toe_heights[1]+(toe_heights[0]-toe_heights[1])*xi/8
        assert abs(target-at)<.045,(x,toe,at,target)
    return slab,dict(soil_min=min(bearing),soil_max=max(bearing),soil_samples=len(bearing),
                     slab_floor=FLOOR,footing_bottom=-.37,threshold_x=[west,east],
                     ramp_ends_z=[front,toe],toe_height=toe_heights)


def fit_plant(meta):
    model=PLANT/"mine-service-yard.blend"
    native=PLANT/"mine-service-yard.hcs2"
    assert sha(model)==meta["exports"]["blend"] and sha(native)==meta["exports"]["hcs2"]
    names=[name for name,_,_ in split_native(native)[meta["retained_components"]:]
           if "formed slab" not in name]
    assert len(names)==meta["added_components"]-1
    with bpy.data.libraries.load(str(model),link=False) as (available,result):
        assert set(names)<=set(available.objects)
        result.objects=list(names)
    for obj in result.objects:
        assert obj.type=="MESH" and obj.parent is None
        bpy.context.scene.collection.objects.link(obj)
    bpy.context.view_layer.update()
    x,y,z=OFFSET
    pose=Matrix.Translation(Vector((x,-z,y)))
    for obj in result.objects:
        obj.name="Caretaker "+obj.name.removeprefix("Mine ")
        obj.matrix_world=pose@obj.matrix_world
        obj["zone"]="caretaker-plant"
    bpy.context.view_layer.update()
    return result.objects


def build(site,source,plant):
    parent=REFUGES/site/"grounds-with-refuges"
    assert all(sha(ROOT/name)==value for name,value in source["exports"].items())
    assert all(sha(ROOT/name)==value for name,value in source["evidence"].items())
    bpy.ops.wm.open_mainfile(filepath=str(parent.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    scene=bpy.context.scene
    soil=next(obj for obj in scene.objects if obj.get("role","").startswith("saved editor terrain"))
    grade=terrain_reader(soil)
    existing=bvh([obj for obj in scene.objects if obj.type=="MESH" and obj!=soil
                  and "landform" not in obj.name.lower()])
    fittings=fit_plant(plant)
    slab,survey=foundation(grade)
    objects=[slab,*fittings]
    assert not existing.overlap(bvh(objects)),site
    out=OUTPUT/site
    out.mkdir(parents=True,exist_ok=True)
    dest=out/"caretaker-power"
    fragment=ROOT/"build/cemetery-power-fragment.glb"
    export(fragment,objects)
    append(parent.with_suffix(".glb"),fragment,dest.with_suffix(".glb"),reuse_embedded_images=True)
    fragment.unlink()
    changed=native_write(parent.with_suffix(".hcs2"),dest.with_suffix(".hcs2"),set(),objects,
                         open_components={obj.name for obj in fittings if obj.get("source_asset")})
    before=split_native(parent.with_suffix(".hcs2"))
    after=split_native(dest.with_suffix(".hcs2"))
    assert after[:len(before)]==before and len(after)-len(before)==len(changed)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(dest.with_suffix(".blend")),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(dest.with_suffix(".glb")))
    for name,expected in changed:
        reopened=records(bpy.context.scene.objects[name])
        assert match_geometry([face for face,_ in expected],[face for face,_ in reopened]),name

    services=[dict(s,id=s["id"].replace("mine-","cemetery-"),room=12,
                   position=shift(s["position"]),style=s["style"].replace("MC-","CT-")) for s in plant["sockets"]]
    approaches=[(28,0,66),(39,0,62),(46,round(grade(46,62),6),62),(49.4,round(grade(49.4,60),6),60),
                (49.4,round(grade(49.4,58.4),6),58.4),(49.4,FLOOR,56.8),(49.4,FLOOR,55.85)]
    rows=[("SPACE","caretaker-work-yard",12,45.5,56.75,52.3,63.2,0,2.9,"true","Caretaker work yard"),
          ("SPACE","caretaker-plant",12,*OFFICE,FLOOR,2.7,"false","Screened caretaker power shelter")]
    rows.extend(("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in services)
    rows.extend(("POINT","caretaker-service",*point) for point in approaches)
    prior=REFUGES/site
    (out/"room-layout.tsv").write_bytes((prior/"room-layout.tsv").read_bytes()
        +"".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    (out/"door-schedule.tsv").write_bytes((prior/"door-schedule.tsv").read_bytes())
    paths=[dest.with_suffix(ext) for ext in (".blend",".glb",".hcs2")]
    info=dict(footprint=OFFICE,soil_and_ramp=survey,placement_offset=OFFSET,source_plant=plant["exports"],
              services=services,plant_routes=[dict(r,points=[shift(p) for p in r["points"]]) for r in plant["routes"]],
              approach=approaches,retained_collision_parts=len(before),new_collision_parts=len(changed),
              exports={relative(p):sha(p) for p in paths})
    (out/"construction.json").write_text(json.dumps(info,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",site,survey["soil_samples"],"soil samples and a fitted screened plant",len(changed),"added parts")


def capture(site):
    packet=OUTPUT/site
    geometry=load(packet/"construction.json")["exports"]
    source=packet/"caretaker-power.glb"
    assert sha(source)==geometry[relative(source)]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.cavity_type="BOTH"
    scene.render.resolution_x=1440
    scene.render.resolution_y=900
    scene.render.resolution_percentage=100
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.location=(43.6,-64.6,6.9)
    camera.rotation_euler=(Vector((49.3,-55.5,1.36))-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.type="ORTHO"
    camera.data.ortho_scale=13.0
    folder=packet/"captures"/"structure"
    folder.mkdir(parents=True,exist_ok=True)
    scene.render.filepath=str(folder/"caretaker-service-yard.png")
    bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    sites=("wld-07","wld-07-mortuary")
    if "--captures-only" not in sys.argv:
        meta=load(REFUGES/"checkpoint.json")
        plant=load(PLANT/"electrical.json")
        for site in sites:
            build(site,meta["sites"][site.upper()])
    for site in sites:
        capture(site)
