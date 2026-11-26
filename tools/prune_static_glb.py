"""Remove detached static mesh bytes without re-exporting live GLB assets."""
import argparse
import json
from pathlib import Path
import struct

from append_static_glb import read


def pack(document, binary):
    encoded = json.dumps(document, separators=(",", ":"), ensure_ascii=False).encode()
    encoded += b" "*((-len(encoded))%4)
    binary += b"\0"*((-len(binary))%4)
    size = 28+len(encoded)+len(binary)
    return (struct.pack("<IIIII",0x46546c67,2,size,len(encoded),0x4e4f534a)+encoded
            +struct.pack("<II",len(binary),0x004e4942)+binary)


def subset(entries, used):
    original = sorted(used)
    return [entries[i] for i in original], {i:j for j,i in enumerate(original)}


def compact(source, target):
    document, original = read(source)
    assert not any(document.get(k) for k in ("animations","skins","extensions"))
    assert not any(node.get("extensions") for node in document["nodes"])
    roots = [i for scene in document["scenes"] for i in scene.get("nodes",[])]
    seen = set()
    while roots:
        index = roots.pop()
        if index not in seen:
            seen.add(index)
            roots.extend(document["nodes"][index].get("children",[]))
    meshes = {node["mesh"] for i,node in enumerate(document["nodes"]) if i in seen and "mesh" in node}
    document["meshes"], mesh_ids = subset(document["meshes"],meshes)
    for i,node in enumerate(document["nodes"]):
        if "mesh" in node:
            if i in seen:
                node["mesh"] = mesh_ids[node["mesh"]]
            else:
                del node["mesh"]

    meshes = [p for m in document["meshes"] for p in m["primitives"]]
    assert not any(p.get("targets") or p.get("extensions") for p in meshes)
    ids = {ref for p in meshes for ref in p["attributes"].values()}
    ids.update(p["indices"] for p in meshes if "indices" in p)
    document["accessors"],accessor_ids = subset(document["accessors"],ids)
    for p in meshes:
        p["attributes"] = {k:accessor_ids[v] for k,v in p["attributes"].items()}
        if "indices" in p:
            p["indices"] = accessor_ids[p["indices"]]

    bindings = [*document["images"],*document["accessors"]]
    for accessor in document["accessors"]:
        if "sparse" in accessor:
            bindings.extend(accessor["sparse"][k] for k in ("indices","values"))
    view_ids = {a["bufferView"] for a in bindings if "bufferView" in a}
    document["bufferViews"], view_ids = subset(document["bufferViews"],view_ids)
    for a in bindings:
        if "bufferView" in a:
            a["bufferView"] = view_ids[a["bufferView"]]

    binary = bytearray()
    original_views = []
    for view in document["bufferViews"]:
        assert view["buffer"]==0 and not view.get("extensions")
        offset = view.get("byteOffset",0)
        data = original[offset:offset+view["byteLength"]]
        assert len(data)==view["byteLength"]
        original_views.append(data)
        binary.extend(b"\0"*((-len(binary))%4))
        view["byteOffset"] = len(binary)
        binary.extend(data)
    document["buffers"] = [dict(byteLength=len(binary))]
    target = Path(target)
    target.write_bytes(pack(document,bytes(binary)))
    final, payload = read(target)
    for view,data in zip(final["bufferViews"],original_views):
        start = view["byteOffset"]
        assert payload[start:start+view["byteLength"]]==data
    assert len(final["bufferViews"])==len(original_views)
    return len(original)-len(payload)


if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source",type=Path)
    parser.add_argument("target",type=Path)
    args=parser.parse_args()
    assert args.source.resolve()!=args.target.resolve()
    print("PASS removed",compact(args.source,args.target),"inactive GLB buffer bytes")
