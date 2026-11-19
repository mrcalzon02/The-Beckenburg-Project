"""Read back the College utility models and measure their installed connections."""
from collections import Counter, defaultdict
from itertools import permutations, product
from math import floor
from pathlib import Path
import json
import sys

import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import read as read_glb
from build_college_electrical import point
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles,xyz_key
from verify_college_electrical import native_records

BASE=ROOT/"development/levels/rebuild/college-containment-pass/college"


def bvh(items):
    vertices,faces=[],[]
    for obj in items:
        assert obj.type=="MESH",obj.name
        start=len(vertices)
        vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        faces.extend(tuple(start+j for j in p.vertices) for p in obj.data.polygons)
    return BVHTree.FromPolygons(vertices,faces)


def match_geometry(left,right):
    if Counter(map(xyz_key,left))==Counter(map(xyz_key,right)):
        return True
    if len(left)!=len(right):
        return False

    def key(t):
        return tuple(floor((t[k]+t[k+3]+t[k+6])/(3*.0001)) for k in range(3))

    def agrees(a,b):
        aa=[a[i:i+3] for i in (0,3,6)]
        return any(all(abs(x-y)<.00004 for p,q in zip(aa,vertices) for x,y in zip(p,q))
                   for vertices in permutations([b[i:i+3] for i in (0,3,6)]))

    bins=defaultdict(list)
    for tri in right:
        bins[key(tri)].append(tri)
    offsets=list(product((-1,0,1),repeat=3))
    for tri in left:
        x,y,z=key(tri)
        for dx,dy,dz in offsets:
            options=bins[(x+dx,y+dy,z+dz)]
            index=next((i for i,other in enumerate(options) if agrees(tri,other)),None)
            if index is not None:
                options.pop(index)
                break
        else:
            return False
    return True


def audit_scene(parts,data):
    figure=bpy.context.scene
    for part in parts:
        name,closed,count,_=part
        model=[face for face,_ in records(figure.objects[name])]
        assert match_geometry(triangles(part),model),(name,count,closed)
    fixtures={row["key"]:row for row in data["fixtures"]}
    by_fixture={key:bvh([figure.objects[name] for name in item["parts"]]) for key,item in fixtures.items()}
    max_error=0
    for terminal in data["terminals"]:
        tree=by_fixture[terminal["fixture"]]
        nearest,_,_,error=tree.find_nearest(point(*terminal["position"]),.002)
        assert nearest is not None and error<.001,(terminal,error)
        max_error=max(max_error,error)
    parts={p[0] for p in parts}
    max_cap_overhang=0
    for run in data["service_runs"]:
        for index,(u,v) in enumerate(zip(run["nodes"],run["nodes"][1:])):
            name=run["parts"][index]
            assert name in parts,name
            o=figure.objects[name]
            start,stop=point(*u),point(*v)
            axis=(stop-start).normalized()
            length=(stop-start).length
            coords=[(o.matrix_world@vertex.co)-start for vertex in o.data.vertices]
            along=[delta.dot(axis) for delta in coords]
            radial=[(delta-axis*t).length for delta,t in zip(coords,along)]
            assert -.0091<min(along)<-.0069 and .0069<max(along)-length<.0091,name
            assert all(abs(d-run["radius"])<.0008 for d in radial),name
            max_cap_overhang=max(max_cap_overhang,max(along)-length)
    return dict(measured_terminals=len(data["terminals"]),max_terminal_error_m=round(max_error,8),
                max_pipe_joint_overlap_m=round(max_cap_overhang,7))


def audit_network(data):
    by_id={run["id"]:run for run in data["service_runs"]}
    for circuit in ("flow","return","cold","hot"):
        carrier=by_id[circuit+" campus carrier"]
        start,end=carrier["nodes"]
        assert start[:2]==end[:2] and start[2]<end[2]
        assert len(by_id["plant isolation "+circuit]["nodes"])>=5
        branches=[run for run in data["service_runs"] if run["circuit"]==circuit
                    and run["id"] not in (carrier["id"],"plant isolation "+circuit)]
        for b in branches:
            x,y,z=b["nodes"][-1]
            assert [x,y]==start[:2] and start[2]<=z<=end[2],b["id"]
    main=by_id["south gravity collector"]
    a,b=main["nodes"]
    slope=(a[1]-b[1])/(b[2]-a[2])
    assert abs(slope-.018)<1e-8
    for run in data["service_runs"]:
        if run["circuit"]!="waste":continue
        nodes=run["nodes"]
        assert all(u[1]>=v[1] for u,v in zip(nodes,nodes[1:])),run["id"]
        if run!=main:
            mouth=nodes[-1]
            assert abs(mouth[0]-a[0])<.0001 and abs(a[1]-(mouth[2]-a[2])*slope-mouth[1])<.0001
            assert mouth[1]<-3.2
    for vent in data["ventilation"]:
        assert len(vent["mount_heights"])==4 and vent["room_intake_y"]<=vent["pickup"][1]-.16
        assert len(vent["roof"])>=1


def readback(stage):
    data=json.loads((stage/"services.json").read_bytes())
    for name,digest in data["exports"].items():
        assert sha(stage/name)==digest,name
    for ext,v in data["source"].items():
        assert sha(BASE/("college-containment."+ext))==v
    for item in data["assets"].values():
        assert sha(ROOT/item["path"])==item["sha256"]
    old,raw=read_glb(BASE/"college-containment.glb")
    new,new_raw=read_glb(stage/"college-services.glb")
    replaced=set(data["removed"])
    assert replaced=={"Wall plant S"}
    assert new_raw.startswith(raw)
    for prior,current in zip(old["nodes"],new["nodes"]):
        assert current==({k:v for k,v in prior.items() if k!="mesh"} if prior.get("name") in replaced else prior)
    for field in ("bufferViews","meshes","accessors","images","materials","textures"):
        assert old[field]==new[field][:len(old[field])],field
    kept=[p for p in native_records(BASE/"college-containment.hcs2") if p[0].split(":",1)[-1] not in replaced]
    rendered=native_records(stage/"college-services.hcs2")
    assert rendered[:len(kept)]==kept
    parts=rendered[len(kept):]
    assert len(parts)==data["native_parts"] and sum(p[2] for p in parts)==data["native_triangles"]
    audit_network(data)
    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-services.blend").resolve()))
    edited=audit_scene(parts,data)
    packed=sum(bool(image.packed_file) for image in bpy.data.images)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str((stage/"college-services.glb").resolve()))
    exported=audit_scene(parts,data)
    return dict(retained_native_parts=len(kept),matched_addition_triangles=sum(p[2] for p in parts),
                retained_glb_binary_bytes=len(raw),packed_source_images=packed,editable=edited,exported=exported)


if __name__=="__main__":
    stage,target=(Path(p) for p in sys.argv[sys.argv.index("--")+1:])
    report=readback(stage)
    result=json.dumps(report,indent=2)+"\n"
    target.write_text(result,encoding="utf-8",newline="\n")
    print(result)
