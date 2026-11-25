"""Fit the occupied Seacliff rooms to the installed battery plant."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Matrix

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, bounds, point
from build_prison_sally_gate import export
from review_roster_macroterrain import sha
from verify_college_electrical import native_records
from verify_college_services import bvh

BASE = ROOT / "development/levels/rebuild/seacliff-dispatch-pass"
OUT = ROOT / "development/levels/rebuild/seacliff-room-services"

# Each switch and lamp share a solid section of the same wall. Keep the
# illuminated path and wiring out of the treads, door swings and cupboards.
BRANCHES = (
    ("gallery", 0, 4, (38, 27.2), (35.6, 27.2), (0, -1)),
    ("director-stair", 1, 4, (29.95, 24.5), (29.95, 22.45), (-1, 0)),
    ("director", 2, 7, (38.6, 25), (37, 25), (0, 1)),
    ("west", 3, 4, (15, 38.8), (18, 38.8), (0, 1)),
    ("east", 4, 4, (52, 38.8), (55, 38.8), (0, 1)),
    ("magazine", 5, 4, (10, 15.2), (12.2, 15.2), (0, -1)),
    ("barracks", 7, 0, (13, 24.8), (17, 24.8), (0, 1)),
    ("lowgallery", 8, 0, (33, 27.2), (35.6, 27.2), (0, -1)),
    ("records", 10, 0, (31, 15.3), (29, 15.3), (0, -1)),
    ("pump", 11, 0, (41, 15.2), (39, 15.2), (0, -1)),
)


def fitout_rooms(assembly):
    places=(
        ("director","plotting table","table_large_2",35.25,7,14.5,1.0),
        ("barracks","west bunk 1","bed_1",10.4,0,17.6,.9),
        ("barracks","west bunk 2","bed_1",10.4,0,22.2,.9),
        ("barracks","bedding chest","level_work_crew_footlocker",13.2,0,16.5,.9),
    )
    for zone,name,asset,x,y,z,scale in places:
        parts=assembly.place(asset,Matrix.Scale(scale,4),name,"floor-bearing room furniture",zone)
        low,high=bounds(parts)
        dx,dz=x-(low[0]+high[0])/2,z-(low[2]+high[2])/2
        for px in (low[0]+dx,high[0]+dx):
            for pz in (low[2]+dz,high[2]+dz):
                hit,_,_=assembly.measured(point(px,y+.18,pz),point(0,-1,0),.28,name)
                assert abs(hit.z-y)<.005, (name,hit,y)
        move=Matrix.Translation(point(dx,y-low[1],dz))
        for part in parts:
            part.matrix_world=move @ part.matrix_world
        bpy.context.view_layer.update()
        if zone=="director":
            director_log(assembly,parts,x,z)


def director_log(assembly,table,x,z):
    _,top=bounds(table)
    hit,normal,_,_=bvh(table).ray_cast(point(x,top[1]+.1,z),point(0,-1,0),.3)
    assert hit is not None and normal.z>.98
    parts=assembly.place("notebook_1",Matrix.Identity(4),"director plotting log","readable battery log","director")
    assert len(parts)==1
    book=parts[0]
    book.name="Seacliff director plotting log"
    low,high=bounds(parts)
    scale=min(.35/(high[0]-low[0]),.28/(high[2]-low[2]),1)
    book.matrix_world=Matrix.Translation(point(x-scale*(low[0]+high[0])/2,hit.z+.002-scale*low[1],
                                              z-scale*(low[2]+high[2])/2)) @ Matrix.Scale(scale,4) @ book.matrix_world
    bpy.context.view_layer.update()
    _,high=bounds(parts)
    assembly.effect(book.name,"native_story",2,(x,high[1],z),0,"SEACLIFF_DIRECTOR_LOG")


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    source = {ext: BASE / "wld-17" / f"battery-cupboards.{ext}" for ext in ("blend", "glb", "hcs2")}
    rows = (BASE / "verification/runtime.tsv").read_text(encoding="utf-8").splitlines()
    source_row = rows[0].split("\t")
    assert sha(source["glb"]) == source_row[2] and sha(source["hcs2"]) == source_row[4]
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    landing = bpy.data.objects["Battery intermediate cast concrete switchback court region0"]
    vertices = [landing.matrix_world @ v.co for v in landing.data.vertices]
    foot = max(v.z for v in vertices)
    edge = (min(v.x for v in vertices), min(-v.y for v in vertices),
            max(v.x for v in vertices), max(-v.y for v in vertices))
    rows.append("	".join(map(str, ("SPACE", "arrival-switchback-court", 13,
                                    *edge, foot, 2.3, "True"))))
    assembly = Assembly()
    for space, owner, floor, switch, lamp, (dx, dz) in BRANCHES:
        print("Mounting Seacliff", space, flush=True)
        zone = dict(id=space, owner=owner, y=floor, height=3)
        assembly.switch(zone, *switch, dx, dz)
        drop = assembly.routes[-1]["points"][-1]
        direction = point(dx, 0, dz)
        key = f"battery-{space}-light"
        assembly.mounted_lamp(zone, key, point(lamp[0], floor + 2.36, lamp[1]),
                              direction, 1.8, point(-dz, 0, dx))
        x, y, z = assembly.loads[-1]["mount"]
        lamp_junction = [x - dx*.028, drop[1], z - dz*.028]
        assert abs((lamp_junction[0]-drop[0])*dx + (lamp_junction[2]-drop[2])*dz) < .01, space
        assembly.raceway(f"Battery {space} lamp branch", (drop, lamp_junction,
                         (lamp_junction[0], y+.08, lamp_junction[2])), .012)
        assembly.sockets[-2]["id"] = f"battery-{space}-switch"
        assembly.sockets[-2]["style"] = f"BATTERY-{space.upper()}"
    fitout_rooms(assembly)
    for piece in assembly.built:
        piece.name = piece.name.replace("College ", "Seacliff ", 1)
    target = {ext: OUT / f"battery.{ext}" for ext in source}
    addition = ROOT / "build/seacliff-room-branches.glb"
    export(addition, assembly.built)
    append(source["glb"], addition, target["glb"], reuse_embedded_images=True)
    native_write(source["hcs2"], target["hcs2"], set(), assembly.built,
                 open_components={p.name for p in assembly.built if p.get("source_asset")})
    prior = native_records(source["hcs2"])
    assert native_records(target["hcs2"])[:len(prior)] == prior
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    source_row = ("SOURCE", target["glb"].relative_to(ROOT).as_posix(), sha(target["glb"]),
                  target["hcs2"].relative_to(ROOT).as_posix(), sha(target["hcs2"]))
    rows[0] = "	".join(source_row)
    for row in ("POINT	west wing", "LINK	route-04"):
        threshold = row + "	22.5	4	37.889"
        rows.insert(rows.index(threshold) + 1, row + "	20.5	4	37.889")
    for socket in assembly.sockets:
        rows.append("\t".join(map(str, ("SOCKET", socket["id"], socket["type"], socket["room"],
            *socket["position"], socket["yaw"], socket["style"]))))
    (OUT / "runtime.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    (OUT / "circuits.json").write_text(json.dumps(dict(
        source=source["blend"].relative_to(ROOT).as_posix(),
        installed=assembly.sockets, lights=assembly.loads, conduit=assembly.routes,
        exports={ext: sha(path) for ext,path in target.items()}), indent=2)+"\n",
        encoding="utf-8", newline="\n")


if __name__ == "__main__":
    build()
