#!/usr/bin/env python3
"""Non-destructive diagnostic of original GLB texture binding and UV data.

This does not grade visual quality and never blocks shipping. Run on individual
author/source GLBs before re-import, and compare the real runtime screenshots.
"""
import argparse
import json
import math
from pathlib import Path
import struct


def audit(path):
    result = {"path": str(path), "status": "inspected", "warnings": [], "materials": [],
              "primitives": [], "textures": [], "source_bytes": path.stat().st_size}
    warn = result["warnings"]
    data = path.read_bytes()
    if len(data) < 28 or data[:4] != b"glTF" or struct.unpack_from("<II", data, 4)[0] != 2:
        raise ValueError("not GLB version 2")
    declared = struct.unpack_from("<I", data, 8)[0]
    if declared != len(data):
        raise ValueError("GLB byte length mismatch")
    js_len, js_type = struct.unpack_from("<I4s", data, 12)
    if js_type != b"JSON" or js_len > len(data) - 28:
        raise ValueError("bad GLB JSON chunk")
    gltf = json.loads(data[20:20 + js_len])
    bin_header = 20 + js_len
    bin_len, bin_type = struct.unpack_from("<I4s", data, bin_header)
    if bin_type != b"BIN\x00" or bin_header + 8 + bin_len > len(data):
        raise ValueError("invalid GLB binary chunk")
    bin_start = bin_header + 8
    buffers = gltf.get("bufferViews", [])
    accessors = gltf.get("accessors", [])
    images = gltf.get("images", [])
    textures = gltf.get("textures", [])
    materials = gltf.get("materials", [])

    def read_view(index):
        view = buffers[index]
        start = bin_start + view.get("byteOffset", 0)
        end = start + view["byteLength"]
        if start < bin_start or end > bin_start + bin_len:
            raise ValueError("bufferView exceeds binary payload")
        return data[start:end]

    for index, img in enumerate(images):
        item = {"image": index, "mimeType": img.get("mimeType"),
                "uri": img.get("uri"), "embedded": "bufferView" in img}
        if "bufferView" in img:
            raw = read_view(img["bufferView"])
            if raw.startswith(b"\x89PNG\r\n\x1a\n") and len(raw) >= 24:
                item["width"], item["height"] = struct.unpack_from(">II", raw, 16)
            else:
                warn.append(f"image {index}: non-PNG embedded bitmap (review runtime decoder support)")
        else:
            warn.append(f"image {index}: external URI or image buffer absent")
        result["textures"].append(item)
    if not images:
        warn.append("no image data in GLB")

    def uv_statistics(index):
        accessor = accessors[index]
        if accessor.get("type") != "VEC2" or accessor.get("componentType") != 5126:
            return {"valid": False, "reason": "UV accessor is not float VEC2"}
        if "sparse" in accessor or "bufferView" not in accessor:
            return {"valid": False, "reason": "sparse or missing UV buffer"}
        view = buffers[accessor["bufferView"]]
        count = accessor["count"]
        stride = view.get("byteStride", 8)
        start = bin_start + view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
        end = bin_start + view.get("byteOffset", 0) + view["byteLength"]
        if stride < 8 or start < bin_start or start + (count - 1) * stride + 8 > end:
            return {"valid": False, "reason": "UV range outside bufferView"}
        umin = vmin = math.inf
        umax = vmax = -math.inf
        for i in range(count):
            u, v = struct.unpack_from("<ff", data, start + i * stride)
            if not math.isfinite(u) or not math.isfinite(v):
                return {"valid": False, "reason": "nonfinite UV"}
            umin, umax, vmin, vmax = min(umin, u), max(umax, u), min(vmin, v), max(vmax, v)
        return {"valid": True, "count": count, "u": [umin, umax], "v": [vmin, vmax],
                "tiled": min(umin, vmin) < 0 or max(umax, vmax) > 1}

    for i, material in enumerate(materials):
        pbr = material.get("pbrMetallicRoughness", {})
        base = pbr.get("baseColorTexture")
        info = {"index": i, "name": material.get("name", ""), "baseColorFactor": pbr.get("baseColorFactor"),
                "baseColorTexture": base, "doubleSided": material.get("doubleSided", False)}
        if base is None:
            warn.append(f"material {i}: no base color texture")
        else:
            ti = base.get("index", -1)
            if ti < 0 or ti >= len(textures):
                warn.append(f"material {i}: invalid base-color texture index")
            else:
                info["image"] = textures[ti].get("source")
            if base.get("texCoord", 0) != 0:
                warn.append(f"material {i}: nonzero texCoord set; verify runtime decoder")
        if "fallback" in info["name"].lower() or "checker" in info["name"].lower():
            warn.append(f"material {i}: explicitly named placeholder/checker atlas")
        result["materials"].append(info)

    for mi, mesh in enumerate(gltf.get("meshes", [])):
        for pi, primitive in enumerate(mesh.get("primitives", [])):
            attrs = primitive.get("attributes", {})
            item = {"mesh": mi, "primitive": pi, "mode": primitive.get("mode", 4),
                    "material": primitive.get("material"), "position": attrs.get("POSITION")}
            uv_id = attrs.get("TEXCOORD_0")
            if uv_id is None:
                warn.append(f"mesh {mi} primitive {pi}: no TEXCOORD_0")
            else:
                item["uv"] = uv_statistics(uv_id)
                if not item["uv"]["valid"]:
                    warn.append(f"mesh {mi} primitive {pi}: {item['uv']['reason']}")
            if item["material"] is None or not 0 <= item["material"] < len(materials):
                warn.append(f"mesh {mi} primitive {pi}: missing/invalid material assignment")
            if item["mode"] != 4:
                warn.append(f"mesh {mi} primitive {pi}: non-triangle primitive")
            result["primitives"].append(item)
    if not result["primitives"]:
        warn.append("no mesh primitives found")
    result["status"] = "needs-technical-review" if warn else "structure-parses"
    result["note"] = ("A clean structural scan does not establish proper UV island placement, "
                      "correct source art, or a correct in-game render. Inspect real source/runtime images.")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("models", nargs="*", type=Path, help="GLB file paths")
    parser.add_argument("--output", type=Path, help="JSON report path")
    args = parser.parse_args()
    models = args.models or sorted(Path("assets/runtime/models").glob("*.glb"))
    results = []
    for model in models:
        try:
            results.append(audit(model))
        except (OSError, ValueError, KeyError, IndexError, TypeError, struct.error) as exc:
            results.append({"path": str(model), "status": "cannot-parse", "warnings": [str(exc)]})
    output = {"schema_version": 1, "diagnostic_only": True, "count": len(results), "models": results}
    payload = json.dumps(output, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
    else:
        print(payload, end="")
    # Visual/art findings never block builds; inspect the report and source renders.


if __name__ == "__main__":
    main()
