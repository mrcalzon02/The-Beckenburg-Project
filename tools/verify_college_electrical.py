"""Read back College electrical geometry independently of its builder."""
from pathlib import Path
import hashlib
import json
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import read as read_glb

BASE = ROOT / "development/levels/rebuild/college-ground-pass/college"

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def native_records(path):
    data = path.read_bytes()
    magic, parts, total = struct.unpack_from(">III", data)
    assert magic == 0x48435332
    cursor = 12
    records = []
    for _ in range(parts):
        start = cursor
        length, = struct.unpack_from(">H", data, cursor)
        cursor += 2
        name = data[cursor:cursor+length].decode()
        cursor += length
        closed, count = struct.unpack_from(">BI", data, cursor)
        cursor += 5 + count*37
        records.append((name, closed, count, data[start:cursor]))
    assert cursor == len(data) and sum(row[2] for row in records) == total
    return records

def verify(stage):
    stage = stage.resolve()
    meta = json.loads((stage/"electrical.json").read_text(encoding="utf-8"))
    original_glb = BASE/"campus-grounds.glb"
    original_native = BASE/"campus-grounds.hcs2"
    glb = stage/"college-electrical.glb"
    native = stage/"college-electrical.hcs2"
    blend = stage/"college-electrical.blend"
    for path, key in ((original_glb,"baseline_glb_sha256"), (original_native,"baseline_physics_sha256"),
                      (glb,"glb_sha256"), (native,"physics_sha256"), (blend,"blend_sha256"),
                      (BASE/"authored-layout.json","layout_sha256")):
        assert sha(path)==meta[key], path.name
    source, original_bytes = read_glb(original_glb)
    fitted, fitted_bytes = read_glb(glb)
    assert fitted_bytes.startswith(original_bytes)
    for field in ("bufferViews", "accessors", "samplers", "images", "textures", "materials", "meshes", "nodes"):
        initial=source.get(field, [])
        assert fitted.get(field, [])[:len(initial)] == initial, field
    before = native_records(original_native)
    after = native_records(native)
    assert after[:len(before)] == before
    added = after[len(before):]
    assert len(added) == meta["added_parts"] and sum(row[2] for row in added) == meta["added_triangles"]
    assert all(known[1]==1 for known in added)
    added_names={part[0] for part in added}
    new_scene_nodes={node.get("name") for node in fitted["nodes"][len(source["nodes"]):]}
    assert added_names == new_scene_nodes, sorted(added_names-new_scene_nodes)
    for device in meta["sources"]:
        assert sha(ROOT/"assets/runtime/models"/(device+".glb")) == meta["sources"][device]
    for s in meta["switches"]:
        assert s["id"]+" 01" in {name.removeprefix("College ") for name in new_scene_nodes}
        assert abs(s["at"][1] - s["feet"][1] - 1.25) < .002
    for load in meta["loads"]:
        assert load["id"]+" 01" in {name.removeprefix("College ") for name in new_scene_nodes}
    structural = {
        "existing_glb_binary_bytes_retained":len(original_bytes),
        "existing_glb_nodes_retained":len(source["nodes"]),
        "existing_hcs2_records_retained":len(before),
        "existing_hcs2_triangles_retained":sum(row[2] for row in before),
        "shared_addition_records":len(added),
        "shared_addition_triangles":sum(row[2] for row in added),
        "library_model_hashes_verified":len(meta["sources"]),
        "electric_loads":len(meta["loads"]),
        "switch_mounts":len(meta["switches"]),
        "conduit_runs":len(meta["cable_runs"]),
    }
    print(json.dumps(structural,indent=2))
    return structural

def reopen(stage):
    from collections import Counter
    import bpy

    sys.path[:0] = [str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
    from review_roster_macroterrain import records

    before = native_records(BASE/"campus-grounds.hcs2")
    parts = native_records(stage/"college-electrical.hcs2")[len(before):]

    def face_key(vertices):
        return tuple(sorted(tuple(round(vertices[j+i],4) for i in range(3)) for j in (0,3,6)))

    def verify_foreground():
        scene = bpy.context.scene
        for name,_,count,raw in parts:
            offset = 7+len(name.encode())
            preserved = [struct.unpack_from(">9f",raw,offset+i*37) for i in range(count)]
            exported = [vertices for vertices,_ in records(scene.objects[name])]
            assert Counter(map(face_key,preserved))==Counter(map(face_key,exported)), name
        return dict(triangles=sum(part[2] for part in parts),parts=len(parts),
                    coordinate_comparison_metres=.0001)

    bpy.ops.wm.open_mainfile(filepath=str((stage/"college-electrical.blend").resolve()))
    editable = verify_foreground()
    editable["packed_sources"] = sum(bool(image.packed_file) for image in bpy.data.images)
    assert editable["packed_sources"] > 0
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str((stage/"college-electrical.glb").resolve()))
    return dict(reopened_editable_source=editable,reimported_runtime_mesh=verify_foreground())

if __name__=="__main__":
    args = sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else sys.argv[1:]
    stage = Path(args[0])
    result=verify(stage)
    if "--reopen" in args:
        result.update(reopen(stage))
    if len(args)>1 and args[1]!="--reopen":
        Path(args[1]).write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")
