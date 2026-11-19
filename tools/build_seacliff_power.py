"""Install the Seacliff service-stack circuits on the retained native building."""
from math import pi
from pathlib import Path
import json
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_doors import aabb
from build_college_electrical import Assembly, point
from build_college_services import Installation
from review_roster_macroterrain import sha
from verify_college_electrical import native_records
from verify_college_services import bvh

BASE = ROOT / "development/levels/rebuild/seacliff-receiving-pass"
STEM = "powered-service-stack"
WALL = 59.92


def write(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def build(out):
    out.mkdir(parents=True, exist_ok=True)
    folder = BASE / "wld-17"
    source = {ext: folder / ("founded-receiving-stairs." + ext) for ext in ("blend", "glb", "hcs2")}
    old = json.loads((folder / "construction.json").read_bytes())
    assert sha(source["glb"]) == old["glb_sha256"] and sha(source["hcs2"]) == old["physics_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    fit, raceway = Installation(), Assembly()
    props, mounts, runs, sockets = [], [], [], []

    def surface(x, y, z, dx, dy, dz, expected, tag):
        direction = point(dx, dy, dz)
        hit, normal, obj = raceway.measured(point(x, y, z), direction, 2, tag)
        axis = next(i for i, value in enumerate(direction) if value)
        assert abs(hit[axis] - expected) < .003, (tag, tuple(hit), obj.name)
        mounts.append(dict(id=tag, support=obj.name, position=[hit.x, hit.z, -hit.y], normal=list(normal)))

    def put(key, origin, angle, label, zone, scale=1):
        frame = Matrix.Translation(point(*origin)) @ Matrix.Rotation(angle, 4, "Z")
        frame = frame @ Matrix.Scale(scale, 4)
        objects = fit.place(key, frame, label, zone, "Fixed mounted equipment")
        for obj in objects:
            obj.name = obj.name.replace("College ", "Seacliff ", 1)
        a = [aabb(obj) for obj in objects]
        props.append(dict(id=label, asset=key, parts=[obj.name for obj in objects], bounds=[list(b) for b in a],
                          source=fit.assets[key].path.relative_to(ROOT).as_posix(), sha256=sha(fit.assets[key].path)))
        return objects

    def solid(label, rect, low, high, material, zone):
        obj = fit.box(label, rect, low, high, material, zone, "Installed electrical support")
        obj.name = obj.name.replace("College ", "Seacliff ", 1)

    def socket(id, kind, owner, xyz, yaw, style):
        sockets.append(("SOCKET", id, kind, owner, *xyz, yaw, style))

    def run(name, points, radius=.023):
        runs.append(dict(id=name, points=points, radius=radius))
        for i, (a,b) in enumerate(zip(points, points[1:]), 1):
            obj = raceway.tube(f"{name} {i}", a, b, radius)
            obj.name = obj.name.replace("College ", "Seacliff ", 1)

    generator = fit.scene.objects["level_blockout_diesel_generator_skid.001"]
    assert generator.get("zone") == "plant"
    original_parts = native_records(source["hcs2"])
    assert any(row[0].split(":",1)[-1] == generator.name for row in original_parts)
    floor = bvh([o for o in fit.scene.objects if o.get("zone") == "plant" and o.get("role") == "floor"])
    feet = [generator.matrix_world @ v.co for v in generator.data.vertices]
    for p in {tuple(v) for v in feet if abs(v.z) < .001}:
        hit, normal, _, _ = floor.ray_cast(Vector(p) + Vector((0,0,.035)), Vector((0,0,-1)), .07)
        assert hit is not None and abs(hit.z) < .001 and normal.z > .98, "existing diesel skid foot"
    for z in (17.24, 17.50):
        for y in (.72, 1.03):
            surface(58, y, z, 1, 0, 0, 58.78, "diesel starter backplate")
    solid("diesel starter seating plate", [58.716,17.22,58.786,17.54], .69, 1.06, "painted", "plant")
    starter = (58.714,.75,17.38)
    put("button_12", starter, -pi/2, "diesel starter", "plant", .30)
    socket("battery-generator", "generator", 9, starter, -pi/2, "BATTERY-MAIN")

    for z in (19.32,20.3,21.28):
        fit.require_floor(59.62, 0, z, "main board footing")
        for y in (.47,1.64):
            surface(58.5,y,z,1,0,0,WALL,"main board fixing rail")
    solid("main board plinth",[59.48,19.16,59.92,21.44],0,.055,"concrete","plant")
    for lo in (.46,1.63):
        solid("main board fixing rail",[59.887,19.20,59.924,21.40],lo,lo+.08,"galvanised","plant")
    bank = put("level_electrical_bank_worn",(59.778,.055,20.30),-pi/2,"main distribution bank","plant")
    terminals = bvh(bank)
    for index, z in enumerate((19.6778,20.4378,21.1978),1):
        end, _, _, distance = terminals.find_nearest(point(59.818,2.163,z),.025)
        assert end is not None and distance < .025, ("board terminal", index, z)
        x,y,z = end.x,end.z,-end.y
        run(f"main distribution takeoff {index}",[(x,y,z),(x,2.75,z),(59.88,2.75,z)],.017)
        mounts.append(dict(id=f"main distribution takeoff {index}",support=bank[0].name,
                           position=[x,y,z]))
    socket("battery-breaker","breaker",9,(59.54,.895,20.30),-pi/2,"BATTERY-MAIN")

    run("diesel isolated feeder",[(59.82,.34,17.42),(59.88,.34,17.42),(59.88,.34,20.3),(59.88,2.75,20.3)],.027)
    for y in (.42,1.44,2.63):
        solid("main feeder wall saddle",[59.85,20.266,59.924,20.334],y,y+.045,"galvanised","plant")
    run("east plant wall trunk",[(59.88,2.75,19.65),(59.88,2.75,23.55)],.027)
    solid("sealed interstorey feed sleeve",[59.80,23.47,59.919,23.63],2.81,4.04,"galvanised","service")
    run("protected service riser",[(59.88,2.75,23.55),(59.88,6.75,23.55)],.023)

    for zone, owner, y, loads in (
        ("plant",9,0,((51,19.6),(56.4,19.6),(51,23.2),(56.4,23.2))),
        ("service",6,4,((52,23.2),(55.1,23.2),(58.2,23.2))),
    ):
        circuit = f"BATTERY-{'PLANT' if owner==9 else 'SERVICE'}"
        for z in (23.15,23.25):
            surface(58.7,y+1.25,z,1,0,0,WALL,circuit+" switch fixing")
        face = (59.919,y+1.25,23.2)
        put("light_switch_1",face,-pi/2,circuit+" switch",zone)
        socket(circuit+"-switch","switch",owner,face,-pi/2,circuit)
        run(circuit+" overhead raceway",[(59.88,y+2.75,23.55),(59.88,y+2.75,23.2),
                                          (min(x for x,_ in loads),y+2.75,23.2)])
        run(circuit+" switch drop",[(59.902,y+1.30,23.2),(59.902,y+2.75,23.2)],.016)
        for h in (1.49,2.09,2.66):
            solid(circuit+" switch saddle",[59.875,23.17,59.925,23.23],y+h,y+h+.04,"galvanised",zone)
        for h in (2.47,2.77):
            solid(circuit+" riser clip",[59.85,23.50,59.925,23.60],y+h,y+h+.04,"galvanised",zone)
        if owner==9:
            run(circuit+" forward branch",[(56.4,y+2.75,23.2),(56.4,y+2.75,19.6),(51,y+2.75,19.6)])
        for i,(x,z) in enumerate(loads,1):
            for dx in (-.56,.56):
                for dz in (-.11,.11):
                    surface(x+dx,y+2.45,z+dz,0,1,0,y+2.84,circuit+" load seat")
            label=f"{circuit} lamp {i}"
            put("level_utility_light",(x,y+2.839,z),0,label,zone)
            socket(f"{circuit}-light-{i}","light",owner,(x,y+2.655,z),0,"native-string")
            solid(label+" terminal saddle",[x-.036,z-.04,x+.036,z+.04],
                  y+2.725,y+2.842,"galvanised",zone)
        for x in (52,54,56,58,59.5):
            surface(x,y+2.52,23.38,0,1,0,y+2.84,circuit+" conduit support")
            solid(circuit+" overhead saddle",[x-.044,23.157,x+.044,23.243],
                  y+2.723,y+2.842,"galvanised",zone)

    additions = out / "_power-additions.glb"
    pieces = fit.pieces + raceway.built
    outputs = {ext: out / (STEM + "." + ext) for ext in source}
    bpy.ops.object.select_all(action="DESELECT")
    for o in pieces:
        o.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(additions), export_format="GLB", use_selection=True, export_extras=True)
    append(source["glb"], additions, outputs["glb"], reuse_embedded_images=True)
    parts = native_write(source["hcs2"], outputs["hcs2"], set(), pieces,
                         open_components=[o.name for o in pieces if o.get("source_asset")])
    assert native_records(outputs["hcs2"])[:len(original_parts)] == original_parts
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(outputs["blend"]),compress=True)

    hashes = {ext: sha(path) for ext,path in outputs.items()}
    source_line = ("\t".join(("SOURCE",outputs["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
                              outputs["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])))
    rows = (BASE / "verification/battery-runtime-input.tsv").read_text(encoding="utf-8").splitlines()[1:]
    rows.extend("\t".join(map(str,s)) for s in sockets)
    (out/"runtime.tsv").write_text("\n".join((source_line,*rows))+"\n",encoding="utf-8",newline="\n")
    write(out/"power.json",dict(scope="Plant and upstairs service stack", generator=generator.name,
         source={ext: sha(path) for ext,path in source.items()}, export=hashes, mounts=mounts, installed=props,
         circuits={6:[r[1] for r in sockets if r[3]==6],9:[r[1] for r in sockets if r[3]==9]},
         raceways=runs, native_triangle_additions=sum(len(t) for _,t in parts),
         retained_physical_parts=len(original_parts)))
    print("PASS situated service stack",len(mounts),"bearing checks",len(sockets),"physical interactions and emitters")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
