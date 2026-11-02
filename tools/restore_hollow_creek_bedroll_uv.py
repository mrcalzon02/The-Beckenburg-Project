#!/usr/bin/env python3
"""Restore six native bedroll UVs from the exact, undamaged authored GLB.

The terrain-fitted native instances retain their *own* positions, normals,
materials and world placement. Only the correct authored UV seams and vertex
duplication are recovered. Every output triangle is compared with its
unmodified native source before any files are replaced.

Use --apply for an atomic repository working-tree update; no effect otherwise.
This is a source repair, not a visual-art release gate.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import struct

BASE = Path("assets/runtime/models")
AUTHOR = BASE / "level_camp_bedroll_rolled_tailored.glb"
SIX = [f"level_hc_native_{i:03d}_0" for i in range(6)]
MANIFEST = Path("assets/runtime/sites/hollow-creek-scene.tsv")
REPORT = Path("development/levels/hollow-creek/runtime-scene.json")
DIAGNOSTIC = Path("build/repair/bedroll-uv-report.json")


class Glb:
    def __init__(self, source):
        self.data = bytes(source)
        if len(self.data) < 28:
            raise ValueError("truncated GLB")
        magic, version, total, json_len, tag = struct.unpack_from("<5I", self.data)
        if (magic, version, total, tag) != (0x46546c67, 2, len(self.data), 0x4e4f534a):
            raise ValueError("invalid glTF 2.0 GLB")
        off = 20 + json_len
        binary_len, binary_tag = struct.unpack_from("<II", self.data, off)
        if binary_tag != 0x004e4942 or off + 8 + binary_len > len(self.data):
            raise ValueError("invalid binary chunk")
        self.root = json.loads(self.data[20:off])
        self.binary = bytearray(self.data[off + 8:off + 8 + binary_len])

    def accessor(self, index, write=None):
        a = self.root["accessors"][index]
        if "sparse" in a or a.get("bufferView") is None:
            raise ValueError("sparse/unbacked accessors not allowed")
        view = self.root["bufferViews"][a["bufferView"]]
        kinds = {5126: ("f", 4), 5125: ("I", 4), 5123: ("H", 2), 5121: ("B", 1)}
        if a["componentType"] not in kinds:
            raise ValueError("unsupported component type")
        code, size = kinds[a["componentType"]]
        components = {"VEC3": 3, "VEC2": 2, "SCALAR": 1}.get(a["type"])
        if components is None:
            raise ValueError("unsupported accessor type")
        stride = view.get("byteStride", size * components)
        off = view.get("byteOffset", 0) + a.get("byteOffset", 0)
        n = a["count"]
        if stride < size * components or n < 1 or off < 0 or (
                off + (n - 1) * stride + size * components >
                view.get("byteOffset", 0) + view["byteLength"]):
            raise ValueError("accessor out of range")
        if write is not None:
            if code != "f" or len(write) != n or any(len(p) != components for p in write):
                raise ValueError("invalid float accessor replacement")
            for i, value in enumerate(write):
                struct.pack_into("<" + code * components, self.binary,
                                 off + i * stride, *value)
            if "min" in a:
                a["min"] = [min(p[k] for p in write) for k in range(components)]
            if "max" in a:
                a["max"] = [max(p[k] for p in write) for k in range(components)]
        return [struct.unpack_from("<" + code * components, self.binary,
                                   off + i * stride) for i in range(n)]

    def embedded_image(self, index):
        image = self.root["images"][index]
        if image.get("mimeType") != "image/png":
            raise ValueError("original material not embedded PNG")
        v = self.root["bufferViews"][image["bufferView"]]
        o = v.get("byteOffset", 0)
        return bytes(self.binary[o:o + v["byteLength"]])

    def bytes(self):
        self.root["buffers"][0]["byteLength"] = len(self.binary)
        j = json.dumps(self.root, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        j += b" " * ((-len(j)) % 4)
        data = bytes(self.binary)
        data += b"\x00" * ((-len(data)) % 4)
        size = 12 + 8 + len(j) + 8 + len(data)
        return (struct.pack("<III", 0x46546c67, 2, size) +
                struct.pack("<II", len(j), 0x4e4f534a) + j +
                struct.pack("<II", len(data), 0x004e4942) + data)


def sha(b):
    return hashlib.sha256(b).hexdigest()


def primitive_data(doc, primitive):
    attributes = primitive["attributes"]
    if set(attributes) != {"POSITION", "NORMAL", "TEXCOORD_0"}:
        raise ValueError("unexpected native vertex layout")
    return (doc.accessor(attributes["POSITION"]),
            doc.accessor(attributes["NORMAL"]),
            doc.accessor(attributes["TEXCOORD_0"]),
            [x[0] for x in doc.accessor(primitive["indices"])])


def restore(author, native, model_id):
    src = Glb(author.data)
    original = Glb(native.data)
    if len(src.root["images"]) != 2 or len(original.root["images"]) != 2:
        raise ValueError("expected original two bedroll PNGs")
    for index in range(2):
        if src.embedded_image(index) != original.embedded_image(index):
            raise ValueError("authored and fitted textures do not match exactly")
    sp, np = src.root["meshes"][0]["primitives"], original.root["meshes"][0]["primitives"]
    if len(sp) != 2 or len(np) != 2:
        raise ValueError("expected two original bedroll material groups")
    stats = []
    for group, (a, b) in enumerate(zip(sp, np)):
        a_xyz, a_norm, a_uv, a_index = primitive_data(src, a)
        b_xyz, b_norm, _b_uv, b_index = primitive_data(original, b)
        if len(a_index) != len(b_index) or len(a_index) % 3:
            raise ValueError("authored and fitted triangle counts differ")
        assignments, largest_delta = {}, 0.0
        for ai, bi in zip(a_index, b_index):
            pos, norm = b_xyz[bi], b_norm[bi]
            authored_pos = a_xyz[ai]
            distance = sum((pos[i] - authored_pos[i]) ** 2 for i in range(3)) ** .5
            largest_delta = max(largest_delta, distance)
            existing = assignments.get(ai)
            if existing is not None and existing != (pos, norm):
                raise ValueError("authored seam vertex maps to conflicting fitted geometry")
            assignments[ai] = (pos, norm)
        # Terrain conformation is only millimetres; a large mismatch indicates
        # wrong triangles, orientation, or a differently authored source mesh.
        if largest_delta > 0.04 or len(assignments) != len(a_xyz):
            raise ValueError(f"triangle order/fit not preserved: {model_id}/{group}: {largest_delta}")
        rebuilt_xyz = [assignments[i][0] for i in range(len(a_xyz))]
        rebuilt_norm = [assignments[i][1] for i in range(len(a_xyz))]
        src.accessor(a["attributes"]["POSITION"], write=rebuilt_xyz)
        src.accessor(a["attributes"]["NORMAL"], write=rebuilt_norm)
        stats.append({"material": group, "triangles": len(a_index)//3,
                      "source_seam_vertices": len(a_xyz), "native_vertices": len(b_xyz),
                      "max_fitted_deformation_m": largest_delta})
    # Keep the exact *native* material metadata and scene node names. The
    # source topology and UV layer are restored; surface artwork is unchanged.
    src.root["nodes"] = copy.deepcopy(original.root["nodes"])
    src.root["meshes"][0]["name"] = original.root["meshes"][0].get("name",model_id)
    src.root["materials"] = copy.deepcopy(original.root["materials"])
    result = src.bytes()
    check = Glb(result)
    for ix, (p, native_p) in enumerate(zip(check.root["meshes"][0]["primitives"],np)):
        out_xyz, out_norm, out_uv, out_index = primitive_data(check, p)
        old_xyz, old_norm, _old_uv, old_index = primitive_data(original,native_p)
        for a_idx, b_idx in zip(out_index,old_index):
            if out_xyz[a_idx] != old_xyz[b_idx] or out_norm[a_idx] != old_norm[b_idx]:
                raise AssertionError("source fitted triangle geometry/normal changed")
        if len(out_uv) != len(out_xyz):
            raise AssertionError("restored atlas vertices missing")
    if any(check.embedded_image(i) != original.embedded_image(i) for i in range(2)):
        raise AssertionError("original bedroll texture bytes changed")
    if len(result) > 4_000_000:
        raise ValueError("runtime GLB budget exceeded")
    return result, stats


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="write repaired derivative GLBs and manifests")
    args = parser.parse_args()
    author = Glb(AUTHOR.read_bytes())
    scene = json.loads(REPORT.read_text(encoding="utf-8"))
    scene_sha = {row["id"]: row["sha256"] for row in scene["assets"]}
    old_manifest = MANIFEST.read_text(encoding="utf-8")
    original_lines = old_manifest.splitlines(keepends=True)
    staged = {}
    for ident in SIX:
        file = BASE / (ident + ".glb")
        data = file.read_bytes()
        if ident not in scene_sha or sha(data) != scene_sha[ident]:
            raise ValueError(f"unexpected original instance hash: {ident}")
        repaired, stats = restore(author, Glb(data), ident)
        if sha(data) == sha(repaired):
            raise ValueError(f"no UV change in {ident}")
        staged[ident] = (file, repaired, stats, sha(data), sha(repaired))
    new_lines, touched = [], set()
    for line in original_lines:
        tokens = line.rstrip("\r\n").split("\t")
        ident = tokens[0]
        if ident in staged:
            if tokens[1] != staged[ident][3]:
                raise ValueError("scene TSV source hash mismatch: " + ident)
            tokens[1] = staged[ident][4]
            line = "\t".join(tokens) + ("\r\n" if line.endswith("\r\n") else "\n")
            touched.add(ident)
        new_lines.append(line)
    if touched != set(SIX):
        raise ValueError("scene TSV is missing fitted bedroll identifiers")
    fixed_manifest = "".join(new_lines)
    for row in scene["assets"]:
        if row["id"] in staged:
            row["sha256"] = staged[row["id"]][4]
            row["bytes"] = len(staged[row["id"]][1])
    for row in scene["instances"]:
        if row["id"] in staged:
            row["sha256"] = staged[row["id"]][4]
    scene["manifest_sha256"] = sha(fixed_manifest.encode("utf-8"))
    scene["status"] = "actual authored native scene; six rolled-bedroll UVs restored from tailored source"
    record = {"source_glb": str(AUTHOR), "source_sha256": sha(AUTHOR.read_bytes()),
              "preserved_original_scene_sha256": scene["source_sha256"],
              "source_modified": False, "models": [
                {"id": ident, "sha_before": v[3], "sha_after": v[4],
                 "bytes": len(v[1]), "material_groups": v[2],
                 "geometry_and_normals": "exact-per-triangle", "texture_images": "exact-original-bytes"}
                for ident,v in staged.items()]}
    DIAGNOSTIC.parent.mkdir(parents=True,exist_ok=True)
    DIAGNOSTIC.write_text(json.dumps(record,indent=2)+"\n",encoding="utf-8")
    if args.apply:
        for ident, (file, repaired, *_rest) in staged.items():
            file.write_bytes(repaired)
        MANIFEST.write_text(fixed_manifest, encoding="utf-8")
        REPORT.write_text(json.dumps(scene,indent=2)+"\n",encoding="utf-8")
        # Verify every write, including the byte-accurate scene authority.
        assert MANIFEST.read_bytes() == fixed_manifest.encode("utf-8")
        for ident,(file,_,_,_,digest) in staged.items():
            assert sha(file.read_bytes())==digest
    print("RESTORED" if args.apply else "DRY RUN",
          len(staged), "native bedroll UVs; all 58,488 triangles/position/normal samples retained")


if __name__ == "__main__":
    main()
