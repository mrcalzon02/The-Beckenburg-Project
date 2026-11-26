"""Fit the Blackwater Dam auxiliary supply and its first two real circuits."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_dam_refuges import (
    ROOT, BASE, append, native_write, import_scene, rel, write,
    export, split_native, write_tsv, records, sha, match_geometry,
)
from build_college_electrical import ASSETS, Assembly, bounds, point, world
from verify_college_services import bvh

PARENT = BASE / "dam-refuge-pass"
STAGE = BASE / "dam-power-pass"
OUT = STAGE / "wld-09"
STATUS = "DAM_FOUNDED_AUXILIARY_POWER_AND_TWO_CIRCUITS_STAGED"
PLANT = dict(id="plant", owner=10, y=-6, height=3)
TURBINE = dict(id="turbine", owner=3, y=-6, height=8)
STEM = "dam-with-power"
FLOOR = -6
NORTH_WALL = 8.08
CEILING = -3.16
PLANT_FACE = 21.92
TURBINE_FACE = 22.08


def install(assembly):
    supports = []
    equipment = {}
    original_objects = [obj for obj in assembly.scene.objects if obj.type == "MESH"]
    bearings = {}

    def measure(at, direction, expected, receiver):
        start = [x - d * .12 for x, d in zip(at, direction)]
        if receiver not in bearings:
            bearings[receiver] = bvh([obj for obj in original_objects if receiver in obj.name])
        hit, normal, _, _ = bearings[receiver].ray_cast(point(*start), point(*direction), .20)
        assert hit is not None and normal.dot(point(*direction)) < -.98, (at, receiver, expected)

        assert math.dist(world(hit), at) < .005, (at, world(hit), receiver)
        supports.append(dict(at=world(hit), bearing=receiver))
        return hit

    def run(name, path, direction, receiver, offset=.043, radius=.016):
        assembly.raceway(name, path, radius)
        for first, last in zip(path, path[1:]):
            count = math.ceil(math.dist(first, last) / 1.1)
            assert count > 0
            for i in range(count):
                center = tuple(x + (y-x)*(i+.5)/count for x,y in zip(first,last))
                contact = tuple(x + d*offset for x,d in zip(center,direction))
                measure(contact, direction, "surface", receiver)
                a,b,c = center
                x,y,z = contact
                assembly.box(name+" saddle", (min(a,x)-.025, min(c,z)-.025, max(a,x)+.025, max(c,z)+.025),
                             min(b,y)-.024, max(b,y)+.003, assembly.mount, "anchored conduit saddle", "power")

    def north(name, path, radius=.016):
        run(name, [(x,y,NORTH_WALL+.043) for x,y in path], (0,0,-1), "Wall plant N", radius=radius)

    ceiling_y = CEILING-.043
    branch_y = -3.49
    for key, name, origin, yaw, size in (
        ("level_trace_generator_refined", "auxiliary maintenance generator", (41,-5.92,9.08),
         math.pi, (.775,.44,.44)),
        ("level_electrical_bank_worn", "main distribution board", (49,-5.94,8.25), 0, (1.11,.14,.23)),
    ):
        x,y,z = origin
        half, front, rear = size
        for xx in (x-half-.08,x,x+half+.08):
            for zz in (max(NORTH_WALL+.012,z-front-.04),z,z+rear+.04):
                measure((xx,FLOOR,zz), (0,-1,0), "slab", "Floor plant")
        assembly.box(name+" plinth", (x-half-.08,max(NORTH_WALL+.007,z-front-.04),x+half+.08,z+rear+.04),
                     FLOOR-.005,y,assembly.concrete,"slab-bearing bolted equipment plinth","plant")
        frame = Matrix.Translation(point(*origin)) @ Matrix.Rotation(yaw,4,'Z')
        objects = assembly.place(key,frame,name,"fixed auxiliary electrical equipment","plant")
        equipment[key] = dict(origin=origin,yaw=yaw,sha256=sha(ASSETS/(key+".glb")),bounds=bounds(objects))
        if key=="level_trace_generator_refined":
            generator=bvh(objects)
        else:
            cabinet=bvh(objects)

    for height in (-5.44,-4.45):
        for x in (48.05,49,49.95):
            measure((x,height,NORTH_WALL),(0,0,-1),"cabinet rail","Wall plant N")
        assembly.box("cabinet rear fixing rail",(47.90,8.076,50.10,8.127),height-.037,height+.037,
                     assembly.mount,"anchored distribution cabinet bracket","plant")

    # The starter face bears on the original control-side casing. Its operating
    # socket and the breaker are approached from the open switchgear aisle.
    assembly.box("generator starter backing plate",(40.94,9.378,41.13,9.442),-5.478,-5.188,
                 assembly.housing,"mounted generator starter backing plate","plant")
    assembly.effect("dam-aux-generator","generator",10,[41.035,-5.445,9.443],0,"BW-MAIN")
    assembly.effect("dam-main-breaker","breaker",10,[49,-5.10,8.485],0,"BW-MAIN")

    feed_x, feed_y = 41.25, -5.54
    feed, normal, _, _ = generator.ray_cast(point(feed_x,feed_y,8.30),point(0,0,1),.68)
    assert feed is not None and normal.dot(point(0,0,1)) < -.97
    feed_z = -feed.y
    assembly.box("sealed generator cable gland",(feed_x-.038,feed_z-.033,feed_x+.038,feed_z+.006),
                 feed_y-.036,feed_y+.036,assembly.mount,"generator casing gland","plant")
    assembly.raceway("BW-MAIN short generator output",
                     [(feed_x,feed_y,feed_z),(feed_x,feed_y,NORTH_WALL+.043)],.019)
    north("BW-MAIN plant wall feeder",[(feed_x,feed_y),(feed_x,branch_y),(49.8978,branch_y)],.024)
    for i, x in enumerate((48.3778,49.1378,49.8978),1):
        first=len(assembly.built)
        assembly.raceway(f"BW-MAIN board terminal {i}",[(x,-3.832,8.21),(x,branch_y,8.21),
                                                      (x,branch_y,NORTH_WALL+.043)],.016)
        bpy.context.view_layer.update()
        assert cabinet.overlap(bvh(assembly.built[first:first+1])), x

    # Each switched return hugs the actual ceiling or structural wall.
    for zone,sx,sz in ((PLANT,46.9,9.35),(TURBINE,52,23.35)):
        assembly.switch(zone,sx,sz,0,-1)
        assembly.controllers[-1]["id"] = f"dam-{zone['id']}-switch"
        assembly.controllers[-1]["circuit"] = f"BW-{zone['owner']:02d}"
        assembly.sockets[-1]["id"] = assembly.controllers[-1]["id"]
        assembly.sockets[-1]["style"] = assembly.controllers[-1]["circuit"]
    for x in (41.75,49.2):
        assembly.mounted_lamp(PLANT,f"dam-switchgear-light-{x}",point(x,-4.1,13.35),
                              point(0,1,0),1.3,point(1,0,0))
        assert assembly.loads[-1]["support"]=="Ceiling cap plant"
    for x in (53.2,56.4):
        assembly.mounted_lamp(TURBINE,f"dam-turbine-north-light-{x}",point(x,-3.65,23.05),
                              point(0,0,-1),1.3,point(1,0,0))
        assert "Wall turbine N" in assembly.loads[-1]["support"]
    for load in assembly.loads:
        load["circuit"] = f"BW-{load['room']:02d}"

    north("BW-10 switch feed",[(46.9,branch_y),(46.9,-4.58)],.014)
    north("BW-10 switched rise",[(46.96,-4.72),(46.96,ceiling_y)],.014)
    for direction in (-1,1):
        x = 41.75 if direction < 0 else 49.2
        run("BW-10 switchgear ceiling branch",
            [(46.96,ceiling_y,8.123),(46.96,ceiling_y,8.55),(x,ceiling_y,8.55),(x,ceiling_y,13.35)],
            (0,1,0),"Ceiling cap plant")
    # Paired glands meet opposite faces of the same intact partition.
    # The small inter-wall sleeve is recorded as an enclosed service route.
    x = 52
    run("BW-03 switchgear overhead outgoing feeder",
        [(49.8978,ceiling_y,8.123),(x,ceiling_y,8.123),(x,ceiling_y,PLANT_FACE-.043)],
        (0,1,0),"Ceiling cap plant",radius=.021)
    assembly.raceway("BW-03 bank riser",[(49.8978,branch_y,8.123),(49.8978,ceiling_y,8.123)],.018)
    assembly.raceway("BW-03 south wall elbow",[(x,ceiling_y,PLANT_FACE-.043),(x,branch_y,PLANT_FACE-.043)],.018)
    for z, dz in ((PLANT_FACE,1),(TURBINE_FACE,-1)):
        measure((x,branch_y,z),(0,0,dz),"partition gland","Wall turbine N")
        assembly.box("sealed partition face gland",(x-.06,z-.051,x+.06,z+.051),
                     branch_y-.052,branch_y+.052,assembly.housing,"anchored sealed conduit gland","power")
    assembly.raceway("BW-03 sealed electrical sleeve",[(x,branch_y,PLANT_FACE),(x,branch_y,TURBINE_FACE)],.018,False)
    run("BW-03 turbine switch feed and return",[(52,branch_y,22.123),(52,-4.66,22.123)],
        (0,0,-1),"Wall turbine N")
    run("BW-03 turbine header",[(52,branch_y,22.123),(56.4,branch_y,22.123)],
        (0,0,-1),"Wall turbine N")
    for x in (53.2,56.4):
        assembly.raceway("BW-03 wall light takeoff",[(x,branch_y,22.123),(x,-3.65,22.123)],.013)
    return equipment, supports


def build():
    roster=json.loads((PARENT/"roster.json").read_bytes())
    row=next(site for site in roster["active_sources"] if site["id"]=="WLD-09")
    prior=ROOT/row["construction"]
    meta=json.loads(prior.read_bytes())
    authority=json.loads((PARENT/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    runtime=prior.with_name("runtime-input.tsv")
    doors=prior.with_name("door-schedule.tsv")
    for file in (prior,runtime,doors,*(ROOT/row[k] for k in ("glb","native","blend","layout"))):
        expected=authority.get(rel(file),meta.get("layout_sha256") if file==ROOT/row["layout"] else None)
        assert expected==sha(file),file
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    equipment,supports=install(assembly)
    for obj in assembly.built:
        obj.name="Blackwater "+obj.name.removeprefix("College ")
    OUT.mkdir(parents=True,exist_ok=True)
    files={kind: OUT/(STEM+"."+kind) for kind in ("blend","glb","hcs2")}
    fragment=ROOT/"build/dam-power-addition.glb"
    export(fragment,assembly.built)
    append(ROOT/row["glb"],fragment,files["glb"],reuse_embedded_images=True)
    added=native_write(ROOT/row["native"],files["hcs2"],set(),assembly.built,
                       open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    previous=split_native(ROOT/row["native"])
    current=split_native(files["hcs2"])
    assert current[:len(previous)]==previous and len(current)-len(previous)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(files["blend"]),compress=True)
    models=import_scene(files["glb"])
    for name,faces in added:
        assert match_geometry([face for face,_ in faces],[face for face,_ in records(models[name])]),name

    (OUT/"runtime-input.tsv").write_bytes(runtime.read_bytes())
    rows=[(s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets]
    write_tsv(OUT/"electrical-schedule.tsv",rows)
    (OUT/"door-schedule.tsv").write_bytes(doors.read_bytes())
    meta.update(status=STATUS,source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                previous_construction=rel(prior),builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                removed_source_meshes=[],new_interface_parts=[name for name,_ in added],
                retained_native_records=len(previous),equipment=equipment,power_bearings=supports,
                services=assembly.sockets,loads=assembly.loads,controllers=assembly.controllers,raceways=assembly.routes,
                glb_sha256=sha(files["glb"]),physics_sha256=sha(files["hcs2"]),blend_sha256=sha(files["blend"]))
    write(OUT/"construction.json",meta)
    row.update(construction=rel(OUT/"construction.json"),glb=rel(files["glb"]),native=rel(files["hcs2"]),
               blend=rel(files["blend"]),glb_sha256=meta["glb_sha256"],native_sha256=meta["physics_sha256"])
    next(site for site in roster["sites"] if site["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PARENT/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(supports),"surveyed electrical bearings;",len(added),"new native components")


if __name__=="__main__":
    build()
