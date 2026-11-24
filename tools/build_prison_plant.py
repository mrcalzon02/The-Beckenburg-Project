"""Build the Prison plant breaker on its measured wall and slab."""
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Matrix, Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, bounds, point, world
from build_roster_foundations import material
from build_prison_sally_gate import export
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE=ROOT/"development/levels/rebuild/prison-sally-gate-pass"
BANK="level_electrical_bank_worn"


# Each position belongs to the actual room and floor datum, with the
# switch aimed toward a surveyed wall face and the loads toward the ceiling.
CIRCUITS = (
    ("a-lower-gallery",(15,38.8,-1,0),((15,28),(15,38),(15,49))),
    ("reception",(16.4,66.8,0,1),((14,65),)),
    ("sally",(27.15,65.1,-1,0),((34,62),(34,65))),
    ("dining",(16,5.2,0,-1),((16,8),(16,13))),
    ("b-lower-gallery",(53,38.8,1,0),((53,28),(53,38),(53,49))),
    ("medical-link",(29.1,11.7,-1,0),((30,8),(30,13))),
    ("chapel",(59.2,42.5,-1,0),((64,40),(64,46))),
    ("a-upper-gallery",(15,38.8,-1,0),((15,31),(15,41),(15,53))),
    ("isolation-link",(59.25,26.5,-1,0),((59.5,23),(59.5,30))),
    ("records",(20,66.75,0,1),((20,65),)),
    ("b-upper-gallery",(53,38.8,1,0),((53,31),(53,41),(53,53))),
    ("plant",(35.6,23.15,0,-1),((38,24.15),(41,29))),
)

class PrisonElectrical(Assembly):
    def effect(self,key,kind,owner,xyz,yaw,style):
        super().effect(key.replace("college-","prison-"),kind,owner,xyz,yaw,style.replace("EM-","PR-"))


def install(assembly,zone):
    main_x=38.0
    toward=point(0,0,-1)
    low=Vector((0,0,-1))
    wall,_,receiver=assembly.measured(point(main_x,-1.45,23.3),toward,1.8,"Prison plant distribution wall")
    wall_z=-wall.y
    floor,_,bearing=assembly.measured(point(main_x,-2.62,wall_z+.35),low,.6,"Prison plant distribution slab")
    assert abs(floor.z+3)<.002
    for x in (main_x-1.05,main_x+1.05):
        back,_,_=assembly.measured(point(x,-1.45,23.3),toward,1.8,"Prison board back rail")
        base,_,_=assembly.measured(point(x,-2.62,wall_z+.35),low,.6,"Prison board end bearing")
        assert abs(back.y-wall.y)<.004 and abs(base.z-floor.z)<.004
    origin=(main_x,-2.94,wall_z+.18)
    assembly.box("Prison distribution slab plinth",
                 [main_x-1.22,wall_z,main_x+1.22,wall_z+.68],-3,-2.94,
                 assembly.concrete,"Founded distribution plinth",zone["id"])
    for y in (-2.53,-1.51):
        assembly.box("Prison distribution backing rail",
                     [main_x-1.16,wall_z,main_x+1.16,wall_z+.17],y,y+.08,
                     assembly.mount,"Wall anchored distribution rail",zone["id"])
    pieces=assembly.place(BANK,Matrix.Translation(point(*origin)),"Prison utility distribution",
                         "Mounted utility breaker bank",zone["id"])
    minimum,maximum=bounds(pieces)
    assembly.effect("prison-main-breaker","breaker",11,(main_x,-2.1,maximum[2]+.02),0,"PR-MAIN")
    terminals=[main_x+t for t in (-.61,.15,.91)]
    duct_z=origin[2]-.04
    drops=[]
    cabinet=bvh(pieces)
    for i,x in enumerate(terminals,1):
        assembly.raceway(f"Prison distribution terminal {i}",
                         [(x,-.856,duct_z),(x,-.48,duct_z)],.017)
        bpy.context.view_layer.update()
        drop=bvh(assembly.built[-1:])
        assert cabinet.overlap(drop),f"Prison distribution terminal {i}"
        drops.append(drop)
    assembly.raceway("Prison distribution manifold",
                     [(terminals[0]-.03,-.48,duct_z),(terminals[-1]+.03,-.48,duct_z)],.025)
    bpy.context.view_layer.update()
    manifold=bvh(assembly.built[-1:])
    assert all(manifold.overlap(drop) for drop in drops)
    supply_x=terminals[1]
    feed,_,feed_receiver=assembly.measured(point(supply_x,-.23,duct_z),toward,.45,"Prison utility feed box")
    assert abs(feed.y-wall.y)<.004
    assembly.box("Prison utility feed box",[supply_x-.065,wall_z,supply_x+.065,wall_z+.075],-.29,-.17,
                 assembly.mount,"Wall backed utility feed box",zone["id"])
    start=len(assembly.built)
    assembly.raceway("Prison incoming distribution riser",
                     [(supply_x,-.492,duct_z),(supply_x,-.23,duct_z),(supply_x,-.23,wall_z+.03)],.019)
    bpy.context.view_layer.update()
    assert manifold.overlap(bvh(assembly.built[start:start+1]))
    assert bvh(assembly.built[start:start+1]).overlap(bvh(assembly.built[start+1:]))
    assert bvh(assembly.built[start-1:start]).overlap(bvh(assembly.built[start+1:]))
    return dict(wall=receiver.name,floor=bearing.name,source=BANK,source_sha256=sha(ASSETS/(BANK+".glb")),
                bounds=[minimum,maximum],connected_terminals=[(x,-.856,duct_z) for x in terminals],
                incoming_receiver=dict(component=feed_receiver.name,at=world(feed)))


def build(out):
    original=json.loads((BASE/"construction.json").read_bytes())
    source=BASE/"prison-sally-port.blend"
    assert sha(source)==original["exports"]["blend"]
    roster=json.loads((BASE/"roster.json").read_bytes())
    row=next(s for s in roster["active_sources"] if s["id"]=="PRISON")
    layout=json.loads((ROOT/row["layout"]).read_bytes())
    zones={s["id"]:s for s in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source))
    bpy.context.preferences.filepaths.save_version=0
    assembly=PrisonElectrical()
    board=install(assembly,zones["plant"])
    for owner,(room,control,stations) in enumerate(CIRCUITS):
        zone=zones[room]
        assert zone["owner"]==owner
        assembly.switch(zone,*control)
        assembly.controllers[-1]["id"]=f"prison-switch-{owner:02d}"
        assembly.controllers[-1]["circuit"]=f"PR-{owner:02d}"
        for ordinal,(x,z) in enumerate(stations,1):
            tangent=Vector((0,1,0)) if "gallery" in room or "link" in room else Vector((1,0,0))
            assembly.mounted_lamp(zone,f"prison-{room}-load-{ordinal}",point(x,zone["y"]+.95,z),
                                 Vector((0,0,1)),6,tangent)
    for lamp in assembly.loads:
        lamp["circuit"]=lamp["circuit"].replace("EM-","PR-")
    out.mkdir(parents=True,exist_ok=True)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ").replace("EM-","PR-")
    model=out/"prison-plant.glb"
    collision=out/"prison-plant.hcs2"
    static_source=BASE/"prison-sally-port.glb"
    physics=BASE/"prison-sally-port.hcs2"
    assert sha(static_source)==original["exports"]["glb"] and sha(physics)==original["exports"]["hcs2"]
    additions=out/"_plant-services.glb"
    export(additions,assembly.built)
    append(static_source,additions,model,reuse_embedded_images=True)
    additions.unlink()
    native_write(physics,collision,set(),assembly.built,
                 open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    before,after=native_records(physics),native_records(collision)
    assert after[:len(before)]==before
    for prop in assembly.built:
        for collection in list(prop.users_collection):
            assert not collection.name.startswith("Sally gate ")
    blend=out/"prison-plant.blend"
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(model))
    for part in after[len(before):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    rows=(BASE/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",model.relative_to(ROOT).as_posix(),sha(model),
                      collision.relative_to(ROOT).as_posix(),sha(collision)))
    (out/"arrival.tsv").write_bytes(("\n".join(rows)+"\n").encode())
    for name in ("gate.tsv","mission.tsv","door-survey.tsv"):
        copyfile(BASE/name,out/name)
    sockets=[(r["id"],r["type"],r["room"],*r["position"],r["yaw"],r["style"]) for r in assembly.sockets]
    (out/"services.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in sockets).encode())
    data=dict(board=board,sockets=assembly.sockets,loads=assembly.loads,controllers=assembly.controllers,
              conduit=assembly.routes,preserved_components=len(before),new_components=len(after)-len(before),
              exports={name:sha(path) for name,path in (("blend",blend),("glb",model),("hcs2",collision))})
    (out/"survey.json").write_bytes((json.dumps(data,indent=2)+"\n").encode())
    print("PASS",len(assembly.controllers),"mounted switches,",len(assembly.loads),"mounted loads and",len(board["connected_terminals"]),"connected main terminals")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
