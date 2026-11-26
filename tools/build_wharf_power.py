"""Fit the verified above-grade plant assembly to the Wharf forecourt."""
import json
from pathlib import Path
import sys
import struct

import bpy
from mathutils import Matrix, Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild"
SOURCE=BASE/"wharf-cover-pass"
PLANT=BASE/"mine-service-yard-pass"
OUT=BASE/"wharf-power-pass"
OFFSET=(.75,0,-12.7)
FOOTPRINT=(34.7,54.65,40.3,58.25)


def read(p):
    return json.loads(p.read_bytes())


def rel(p):
    return p.relative_to(ROOT).as_posix()


def shift(xyz):
    return [round(a+b,6) for a,b in zip(xyz,OFFSET,strict=True)]


def bearings():
    scene=bpy.context.scene
    deps=bpy.context.evaluated_depsgraph_get()
    x0,z0,x1,z1=FOOTPRINT
    hits=[]
    for i in range(31):
        x=x0-.18+(x1-x0+.36)*i/30
        for j in range(23):
            z=z0-.18+(z1-z0+.64)*j/22
            hit,at,normal,_,obj,_=scene.ray_cast(deps,Vector((x,-z,.30)),Vector((0,0,-1)),distance=.40)
            assert hit and abs(at.z)<.002 and normal.z>.98 and obj.name=="Saved editor geographic earthworks WLD-06",(x,z,at,obj)
            hits.append((round(x,5),round(z,5),at.z))
    return len(hits)


def fit_plant(assembly):
    model=PLANT/"mine-service-yard.blend"
    native=PLANT/"mine-service-yard.hcs2"
    assert sha(model)==assembly["exports"]["blend"] and sha(native)==assembly["exports"]["hcs2"]
    names=[n for n,_,_ in split_native(native)[assembly["retained_components"]:]]
    assert len(names)==assembly["added_components"]
    with bpy.data.libraries.load(str(model),link=False) as (available,result):
        assert set(names)<=set(available.objects)
        result.objects=names
    transform=Matrix.Translation(Vector((OFFSET[0],-OFFSET[2],OFFSET[1])))
    for obj in result.objects:
        assert obj.type=="MESH" and obj.parent is None
        bpy.context.scene.collection.objects.link(obj)
    bpy.context.view_layer.update()
    for obj in result.objects:
        obj.name="Wharf "+obj.name.removeprefix("Mine ")
        obj.matrix_world=transform @ obj.matrix_world
        obj["zone"]="wharf-power-shed"
    bpy.context.view_layer.update()
    return result.objects


def main():
    source=SOURCE/"wharf-cover"
    evidence=read(SOURCE/"checkpoint.json")
    assert all(sha(ROOT/p)==h for p,h in evidence["exact_fit"]["exports"].items())
    placed=read(PLANT/"electrical.json")
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    ground_checks=bearings()
    objects=fit_plant(placed)
    OUT.mkdir(exist_ok=True)
    scene=OUT/"wharf-power"
    fragment=ROOT/"build/wharf-power-fragment.glb"
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(scene.with_suffix(".blend")),compress=True)
    export(fragment,objects)
    append(source.with_suffix(".glb"),fragment,scene.with_suffix(".glb"),reuse_embedded_images=True)
    parts=native_write(source.with_suffix(".hcs2"),scene.with_suffix(".hcs2"),set(),objects,
                     open_components={o.name for o in objects if o.get("source_asset")})
    before=split_native(source.with_suffix(".hcs2"))
    after=split_native(scene.with_suffix(".hcs2"))
    assert after[:len(before)]==before and len(after)-len(before)==len(objects)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(scene.with_suffix(".glb")))
    plant_parts=split_native(PLANT/"mine-service-yard.hcs2")[placed["retained_components"]:]
    for (old_name, count, raw), (new_name, tris) in zip(plant_parts,parts,strict=True):
        assert new_name=="Wharf "+old_name.removeprefix("Mine "),new_name
        start=2+len(old_name.encode())+5
        shifted=[]
        for i in range(count):
            xyz=struct.unpack_from(">9fB",raw,start+i*37)[:9]
            shifted.append([v+OFFSET[k%3] for k,v in enumerate(xyz)])
        assert match_geometry(shifted,[xyz for xyz,_ in tris]),new_name
    for name,tri in parts:
        assert match_geometry([xyz for xyz,_ in tri],[xyz for xyz,_ in records(bpy.context.scene.objects[name])]),name

    sockets=[dict(s,id=s["id"].replace("mine-","wharf-"),room=12,
                  position=shift(s["position"]),style=s["style"].replace("MC-","WH-"))
             for s in placed["sockets"]]
    rows=[("SPACE","wharf-power-shed",12,*FOOTPRINT,.018,2.7,"false","Above-tide power shed")]
    rows.extend(("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in sockets)
    points=((32,0,61),(37.5,0,61),(37.5,0,58.75),(37.5,.018,57.65))
    rows.extend(("POINT","service-from-street",*at) for at in points)
    (OUT/"room-layout.tsv").write_bytes((SOURCE/"room-layout.tsv").read_bytes()
        +"".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    (OUT/"door-schedule.tsv").write_bytes((SOURCE/"door-schedule.tsv").read_bytes())
    paths=[scene.with_suffix(e) for e in (".blend",".glb",".hcs2")]
    packet=dict(footprint=FOOTPRINT,placement_offset=OFFSET,ground_checks=ground_checks,
                covered_service_source=placed["exports"],retained_collision_parts=len(before),
                fitted_parts=len(objects),sockets=sockets,approach=points,
                exports={rel(path):sha(path) for path in paths})
    (OUT/"construction.json").write_bytes((json.dumps(packet,indent=2)+"\n").encode())
    print("PASS Wharf forecourt plant",ground_checks,"ground checks,",len(objects),"matched parts")


if __name__=="__main__":
    main()
