"""Independently read back College wall replacements and the actual frame datums."""
from collections import Counter
from pathlib import Path
import json
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import read as read_glb
from verify_college_electrical import native_records, sha

BASE = ROOT/"development/levels/rebuild/college-electrical-pass/college"

def packet(stage):
    data = json.loads((stage/"doors.json").read_bytes())
    glb, hcs, blend = (stage/("college-doors"+ext) for ext in (".glb",".hcs2",".blend"))
    oldglb, oldhcs = (BASE/("college-electrical"+ext) for ext in (".glb",".hcs2"))
    assert sha(oldglb)==data["previous_glb_sha256"] and sha(oldhcs)==data["previous_physics_sha256"]
    assert sha(glb)==data["glb_sha256"] and sha(hcs)==data["physics_sha256"] and sha(blend)==data["blend_sha256"]
    assert sha(stage/"authored-layout.json")==data["layout_sha256"]
    assert sha(ROOT/"assets/runtime/models/door_1_loose.glb")==data["source_leaf_sha256"]
    old, old_bytes = read_glb(oldglb)
    new, new_bytes = read_glb(glb)
    assert new_bytes.startswith(old_bytes)
    remove = set(data["replaced_wall_names"])
    for index,node in enumerate(old["nodes"]):
        edited=new["nodes"][index]
        expected={key:value for key,value in node.items() if key!="mesh"} if node.get("name") in remove else node
        assert edited==expected, (index,node.get("name"))
    for field in ("accessors","meshes","materials","bufferViews","images","textures"):
        values=old.get(field,[])
        assert new.get(field,[])[:len(values)]==values,field
    original_records = native_records(oldhcs)
    kept=[part for part in original_records if part[0].split(":",1)[-1] not in remove]
    final_records = native_records(hcs)
    assert final_records[:len(kept)]==kept
    parts=final_records[len(kept):]
    assert len(parts)==data["new_static_parts"] and sum(p[2] for p in parts)==data["new_static_triangles"]
    assert all(p[1]==1 for p in parts)
    return data,parts,dict(previous_native_parts=len(original_records),retained_native_parts=len(kept),
                           discarded_old_wall_records=len(original_records)-len(kept),new_native_parts=len(parts),
                           new_native_triangles=sum(p[2] for p in parts),prior_glb_binary_bytes=len(old_bytes))

def reopen(stage, data, parts):
    import bpy
    from mathutils import Vector
    sys.path.insert(0,str(ROOT/"development/levels/blockouts"))
    from review_roster_macroterrain import records

    def key(triangle):
        return tuple(sorted(tuple(round(triangle[j+i],4) for i in range(3)) for j in (0,3,6)))

    def geometry():
        for name, _, count, raw in parts:
            offset=7+len(name.encode())
            collision=[struct.unpack_from(">9f",raw,offset+i*37) for i in range(count)]
            visible=[face for face,_ in records(bpy.context.scene.objects[name])]
            assert Counter(map(key,collision))==Counter(map(key,visible)),name
        return dict(triangles=sum(i[2] for i in parts),comparison_in_metres=.0001)

    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-doors.blend").resolve()))
    blend = geometry()
    blend["packed_images"] = sum(bool(img.packed_file) for img in bpy.data.images)
    assert blend["packed_images"]>10
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str((stage/"college-doors.glb").resolve()))
    glb = geometry()
    scene=bpy.context.scene
    deps=bpy.context.evaluated_depsgraph_get()
    dims={}
    for frame in data["static_door_schedule"]:
        along_x=frame["axis"]=="z"
        boundary=sum(frame["frame_span"])/2
        x,z=(frame["along"],boundary) if along_x else (boundary,frame["along"])
        y=frame["sill_y"]
        def probe(offset, direction, distance):
            anchor=Vector((x,-z,y))+Vector(offset)
            hit,p,_,_,obj,_=scene.ray_cast(deps,anchor,Vector(direction),distance=distance)
            assert hit and obj.type=="MESH", (frame["id"],offset,direction)
            return p
        inward=(1,0,0) if along_x else (0,-1,0)
        outward=tuple(-v for v in inward)
        widths=[]
        for h in (.31, 1.12, 1.65):
            negative=probe((0,0,h),outward,2)
            positive=probe((0,0,h),inward,2)
            width=(positive-negative).length
            assert abs(width-frame["clear_width"])<.003, (frame["id"],"width",width)
            widths.append(round(width,5))
        head=probe((0,0,1.56),(0,0,1),2).z-y
        sill=probe((0,0,.25),(0,0,-1),.50).z
        assert abs(head-frame["clear_height"])<.003 and abs(sill-y)<.006,(frame["id"],head,sill)
        dims[frame["id"]]=dict(sill=round(sill,5),clear_height=round(head,5),clear_width_samples=widths)
    return dict(editable=blend,reimported_glb=glb,frame_datums=dims)

def main():
    args=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else sys.argv[1:]
    stage=Path(args[0])
    data,parts,result=packet(stage)
    if "--reopen" in args:
        result.update(reopen(stage,data,parts))
    rendered=json.dumps(result,indent=2)+"\n"
    print(rendered)
    if len(args)>1 and args[1]!="--reopen":
        Path(args[1]).write_text(rendered,encoding="utf-8",newline="\n")

if __name__=="__main__":
    main()
