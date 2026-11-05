#!/usr/bin/env python3
"""Independent standard-library audit for the entire Beckenburg graybox library.

Checks actual embedded PNG bytes/CRC/IDAT decompression, glTF 2.0 chunk structure,
reproducible SHA-256 records, explicit source-only status and roster assignments.
This deliberately does NOT call the generator's white_png() or its GLB serializer.
"""
from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path
import re
import struct
import zlib

ROOT = Path(__file__).resolve().parent
LIB = ROOT / "models"
SIGNATURE = bytes((137, 80, 78, 71, 13, 10, 26, 10))


def verify_png(data: bytes) -> None:
    if not data.startswith(SIGNATURE):
        raise ValueError("GLB image lacks actual PNG signature")
    chunks = []
    offset = len(SIGNATURE)
    while offset + 12 <= len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        name = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise ValueError("Truncated PNG chunk")
        payload = data[offset + 8:offset + 8 + length]
        crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        if zlib.crc32(name + payload) & 0xffffffff != crc:
            raise ValueError("Corrupt PNG chunk CRC " + name.decode("ascii", "replace"))
        chunks.append((name, payload))
        offset = end
        if name == b"IEND":
            break
    if [name for name, _ in chunks] != [b"IHDR", b"IDAT", b"IEND"] or offset != len(data):
        raise ValueError("Unexpected PNG structure / trailing bytes")
    width, height, depth, colors, comp, filt, interlace = struct.unpack(
        ">IIBBBBB", chunks[0][1])
    if (width, height, depth, colors, comp, filt, interlace) != (2, 2, 8, 2, 0, 0, 0):
        raise ValueError("Unexpected 2x2 RGB study swatch")
    raw = zlib.decompress(chunks[1][1])
    if raw != (bytes((0,)) + bytes((255,)) * 6) * 2:
        raise ValueError("PNG RGB payload was not losslessly decoded to white 2x2")


def read_glb(path: Path) -> tuple[dict, bytes]:
    raw = path.read_bytes()
    if len(raw) < 36:
        raise ValueError("Truncated GLB " + str(path))
    magic, version, length = struct.unpack_from("<III", raw, 0)
    if (magic, version, length) != (0x46546c67, 2, len(raw)):
        raise ValueError("Incorrect glTF header " + str(path))
    jsize, jtype = struct.unpack_from("<II", raw, 12)
    if jtype != 0x4e4f534a or 20 + jsize + 8 > len(raw) or jsize % 4:
        raise ValueError("Missing aligned JSON chunk")
    doc = json.loads(raw[20:20 + jsize])
    pos = 20 + jsize
    bsize, btype = struct.unpack_from("<II", raw, pos)
    if btype != 0x004e4942 or bsize % 4 or pos + 8 + bsize != len(raw):
        raise ValueError("Missing aligned binary chunk")
    binary = raw[pos + 8:]
    if doc["asset"]["version"] != "2.0" or len(doc["meshes"]) != 1:
        raise ValueError("Unexpected GLB document type")
    if doc["buffers"][0]["byteLength"] != len(binary):
        raise ValueError("BIN chunk length mismatch")
    if doc["extras"]["runtimeApproved"] is not False:
        raise ValueError("A primitive was silently runtime-approved")
    images = doc["images"]
    if len(images) != 1 or images[0]["mimeType"] != "image/png":
        raise ValueError("Missing embedded PNG")
    view = doc["bufferViews"][images[0]["bufferView"]]
    off, size = view["byteOffset"], view["byteLength"]
    if off < 0 or size <= 0 or off + size > len(binary):
        raise ValueError("PNG image out of bounds")
    verify_png(binary[off:off + size])
    for mesh in doc["meshes"]:
        for part in mesh["primitives"]:
            attrs = part["attributes"]
            if part["mode"] != 4 or not all(x in attrs for x in
                                              ("POSITION", "NORMAL", "TEXCOORD_0")):
                raise ValueError("Missing indexed triangles/UV/normals")
            if part["material"] < 0 or part["material"] >= len(doc["materials"]):
                raise ValueError("Invalid material index")
            positions = doc["accessors"][attrs["POSITION"]]
            uv = doc["accessors"][attrs["TEXCOORD_0"]]
            if (positions["count"] != uv["count"]
                or positions["count"] <= 0 or uv["type"] != "VEC2"):
                raise ValueError("Invalid UV accessor")
            if not all(math.isfinite(v) for vector in (positions["min"],
                                                       positions["max"]) for v in vector):
                raise ValueError("Nonfinite mesh bounds")
            if any(a > b for a, b in zip(positions["min"], positions["max"])):
                raise ValueError("Inverted mesh bounds")
    return doc, raw


def main() -> None:
    catalog = json.loads((ROOT / "catalog.json").read_text(encoding="utf-8"))
    manifest = json.loads((ROOT / "verification.json").read_text(encoding="utf-8"))
    assert manifest["runtime_verified"] is False
    models = catalog["models"]
    id_to_model = {m["id"]: m for m in models}
    if len(id_to_model) != len(models):
        raise ValueError("Duplicate model IDs")
    if set(x["id"] for x in manifest["models"]) != set(id_to_model):
        raise ValueError("Manifest models out of sync")
    all_files = set(p.name for p in LIB.glob("*.glb"))
    if all_files != {name + ".glb" for name in id_to_model}:
        raise ValueError("Missing or extra GLBs in model directory")
    sites = {s["id"]: s for s in catalog["sites"]}
    if len(sites) != 29:
        raise ValueError("Expected exactly 28 maps and one mortuary expansion")
    for site in sites.values():
        prototypes = site["prototypes"]
        if len(prototypes) != len(set(prototypes)):
            raise ValueError("Duplicate site assignment " + site["id"])
        if any(p not in id_to_model for p in prototypes):
            raise ValueError("Unknown prototype in site " + site["id"])
    tris = 0
    for record in manifest["models"]:
        name = record["id"]
        model = id_to_model[name]
        if (not re.fullmatch(r"level_blockout_[a-z0-9_]+", name)
            or model["runtime_approved"] is not False
            or not model["parts"]):
            raise ValueError("Invalid source-only model definition " + name)
        for part in model["parts"]:
            if part["type"] not in ("box", "cylinder", "tri_prism"):
                raise ValueError("Unexpected primitive construction")
            nums = part["p"] + (part["size"] if "size" in part
                                 else [part["radius"], part["height"]])
            if not all(isinstance(n, (int, float)) and math.isfinite(n) for n in nums):
                raise ValueError("Nonfinite primitive geometry")
            dims = part.get("size", [part.get("radius"), part.get("height")])
            if any(n <= 0 for n in dims):
                raise ValueError("Non-positive dimension")
        actual = [sid for sid, site in sites.items() if name in site["prototypes"]]
        expected = set(sites) if "*" in model["maps"] else set(model["maps"])
        if set(actual) != expected:
            raise ValueError("Map assignment mismatch for " + name)
        doc, binary = read_glb(LIB / (name + ".glb"))
        if doc["nodes"][0]["name"] != name:
            raise ValueError("Unstable GLB identity")
        if hashlib.sha256(binary).hexdigest() != record["sha256"]:
            raise ValueError("GLB SHA256 mismatch")
        if len(binary) != record["bytes"]:
            raise ValueError("GLB manifest byte count mismatch")
        indices = sum(doc["accessors"][p["indices"]]["count"]
                      for p in doc["meshes"][0]["primitives"])
        if indices % 3 or indices // 3 != record["triangles"] or indices // 3 > 3500:
            raise ValueError("Invalid mesh triangle count")
        tris += indices // 3
    for site in sites.values():
        if len(site["prototypes"]) < 6:
            raise ValueError("Incomplete site palette " + site["id"])
    kitfile = ROOT / "site-kits" / "index.json"
    if not kitfile.is_file():
        raise ValueError("Missing source-only 3D map-kit index")
    kits = json.loads(kitfile.read_text(encoding="utf-8"))
    if kits["planning_packets"] != 29 or kits["model_library_count"] != len(models):
        raise ValueError("Stale map-kit catalog")
    if kits["runtime_approved"] is not False:
        raise ValueError("Map kit scenes accidentally marked for gameplay")
    rendered = set()
    stand_count = 0
    for kit in kits["sites"]:
        sid = kit["site"]
        if sid not in sites:
            raise ValueError("Unknown model kit site " + sid)
        if kit["shared_shell_models"] != sorted(catalog["shared_reuse"]):
            raise ValueError("Unstable shared kit designations")
        expected = [name for name in sites[sid]["prototypes"]
                    if name not in catalog["shared_reuse"]]
        if kit["specific_models"] != expected:
            raise ValueError("Kit missing or reordering model sample " + sid)
        stands = []
        for page in kit["pages"]:
            filename = page["file"]
            if filename in rendered or not re.fullmatch(
                    r"level_blockout_kit_[a-z0-9_]+_p[0-9]{2}\\.glb", filename):
                raise ValueError("Duplicate or invalid kit page name")
            rendered.add(filename)
            doc, data = read_glb(ROOT / "site-kits" / filename)
            if (doc["nodes"][0]["name"] != page["name"]
                or hashlib.sha256(data).hexdigest() != page["sha256"]
                or len(data) != page["bytes"]):
                raise ValueError("Corrupt or mismatched source-only map kit " + filename)
            triangles = sum(doc["accessors"][p["indices"]]["count"] // 3
                            for p in doc["meshes"][0]["primitives"])
            if triangles != page["triangles"] or triangles > 3500:
                raise ValueError("Exceeded kit-page triangle budget")
            stands.extend(sample["id"] for sample in page["display_slots"])
        if stands != expected:
            raise ValueError("Site contact sheets omit a prototype " + sid)
        stand_count += len(stands)
    actual_kits = {p.name for p in (ROOT / "site-kits").glob("*.glb")}
    if actual_kits != rendered or kits["total_pages"] != len(rendered):
        raise ValueError("Untracked or missing site kit GLBs")
    if kits["total_stands"] != stand_count or len(kits["sites"]) != 29:
        raise ValueError("Map-kit contact sheet count mismatch")
    print("PASS 3D kit gallery:", len(rendered), "pages,", stand_count,
          "true-scale source-only model stands")
    print("PASS graybox library:", len(models), "separate GLBs,",
          len(sites), "planning packets,", tris, "triangles;",
          "all embedded PNGs decode with valid signatures, CRCs and IDAT")


if __name__ == "__main__":
    main()
