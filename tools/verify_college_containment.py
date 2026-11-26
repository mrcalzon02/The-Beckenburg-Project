"""Read back the College containment mesh, shared collision and old assets."""
from collections import Counter
from pathlib import Path
import json
import struct
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import read as read_glb
from review_roster_macroterrain import records,sha
from verify_college_electrical import native_records

BASE=ROOT/"development/levels/rebuild/college-doors-pass/college"


def xyz_key(triangle):
    return tuple(sorted(tuple(round(triangle[j+i],4) for i in range(3)) for j in (0,3,6)))


def triangles(raw_record):
    name,_,count,raw=raw_record
    start=7+len(name.encode())
    return [struct.unpack_from(">9f",raw,start+37*i) for i in range(count)]


def mesh_edges(tris):
    return Counter(tuple(sorted((a,b))) for t in tris
                   for a,b in zip(t,t[1:]+t[:1]))


def geometry(parts):
    for part in parts:
        name,_,count,_=part
        visible=[face for face,_ in records(bpy.context.scene.objects[name])]
        assert Counter(map(xyz_key,triangles(part)))==Counter(map(xyz_key,visible)),name
    return sum(part[2] for part in parts)


def readback(stage):
    meta=json.loads((stage/"containment.json").read_bytes())
    for file,value in meta["output_sha256"].items():
        assert sha(stage/file)==value,file
    old,old_data=read_glb(BASE/"college-doors.glb")
    new,new_data=read_glb(stage/"college-containment.glb")
    assert new_data.startswith(old_data)
    replaced=set(meta["removed_component_names"])
    render_nodes=replaced|{meta["forecourt_terrain_render_node"]}
    for old_node,node in zip(old["nodes"],new["nodes"]):
        required={k:v for k,v in old_node.items() if k!="mesh"} if old_node.get("name") in render_nodes else old_node
        assert required==node,old_node.get("name")
    for field in ("meshes","materials","images","textures","bufferViews","accessors"):
        assert new[field][:len(old[field])]==old[field],field
    baseline=native_records(BASE/"college-doors.hcs2")
    kept=[p for p in baseline if p[0].split(":",1)[-1] not in replaced]
    native=native_records(stage/"college-containment.hcs2")
    assert len(baseline)-len(kept)==len(replaced) and native[:len(kept)]==kept
    built=native[len(kept):]
    assert len(built)==meta["new_parts"]
    apron,=[p for p in built if p[0]=="College district apron"]
    terrain,=[p for p in kept if p[0]=="terrain-site:surface"]
    assert apron[1]==0 and all(p[1]==1 for p in built if p!=apron)
    old_boundary={e for e,n in mesh_edges(list(map(xyz_key,triangles(terrain)))).items() if n==1}
    new_boundary={e for e,n in mesh_edges(list(map(xyz_key,triangles(apron)))).items() if n==1}
    aligned=old_boundary & new_boundary
    assert len(aligned)==1083 and len(old_boundary-aligned)==220 and len(new_boundary-aligned)==len(meta["district_apron"]["outer_limits"]), (len(old_boundary),len(new_boundary),len(aligned),list(new_boundary-aligned)[:5])
    assert len(triangles(apron))==meta["district_apron"]["soil_triangles"]
    def check_terrain():
        object_=bpy.context.scene.objects[meta["current_terrain_render_node"]]
        faces=[face for face,_ in records(object_)]
        assert Counter(map(xyz_key,faces))==Counter(map(xyz_key,triangles(terrain))), "Saved road ground changed position"
        return len(faces)

    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-containment.blend").resolve()))
    terrain_faces=check_terrain()
    editable=geometry(built)
    packed=sum(bool(img.packed_file) for img in bpy.data.images)
    assert packed>=30
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str((stage/"college-containment.glb").resolve()))
    assert check_terrain()==terrain_faces
    glb_triangles=geometry(built)
    assert editable==glb_triangles==meta["new_triangles"]
    scene=bpy.context.scene
    # A vertical section separates the new basement wall top from the
    # retained walking plane and confirms both heads touch the soffit.
    tests={}
    for label,x,z in (("plant",12,18),("maintenance",12,29)):
        floor=scene.objects["College slab bearing Wall "+label+" W"]
        cap=max((floor.matrix_world@v.co).z for v in floor.data.vertices)
        assert abs(cap+.16)<.0001
        for xdelta in (-.14,0,.14):
            hit,where,_,_,obj,_=scene.ray_cast(bpy.context.evaluated_depsgraph_get(),
                Vector((x+xdelta,-z,.25)),Vector((0,0,-1)),distance=.38)
            assert hit and abs(where.z)<.001 and obj.name.startswith("Floor "), (x,z,obj)
        tests[label]=dict(slab_top=0,wall_head=round(cap,4))
    return dict(retained_components=len(kept),removed_components=len(replaced),
                old_glb_binary_bytes_retained=len(old_data),editable_matched_triangles=editable,
                reimported_glb_matched_triangles=glb_triangles,packed_images=packed,
                exterior_stitched_edges=len(aligned),original_interior_cut_edges=len(old_boundary-aligned),
                outside_outline_edges=len(new_boundary-aligned),slab_bearings=tests,
                terrain_triangles_same_position=terrain_faces,
                terrain_faces_refinished_as_service_gravel=meta["forecourt_refinished_triangles"])


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    data=readback(Path(args[0]))
    Path(args[1]).write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(json.dumps(data,indent=2))
