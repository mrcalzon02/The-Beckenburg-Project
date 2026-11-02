#!/usr/bin/env python3
"""Advisory source-atlas inspection for exact authored GLB meshes.

Never mutates model files. Extracts original embedded PNGs, lays UV edges over
each original image as an SVG, and emits objective per-material diagnostics.
This is not a texture-quality build gate or a substitute for in-engine captures.
"""
import argparse
import base64
import hashlib
import html
import json
import pathlib
import struct

DEFAULT_IDS = [
    *(f"level_hc_native_{i:03d}_0" for i in range(6)),
    "level_fireplace", "tv_mp_1", "open_book_mp_1",
    "book_mp_2", "book_mp_3", "book_mp_4", "book_mp_5",
    "metal_barrel_mp_1", "canned_food_mp_1", "canned_food_mp_2"
]

def load_glb(path):
    data = path.read_bytes()
    if len(data) < 28:
        raise ValueError("truncated GLB")
    magic, version, size, json_size, json_tag = struct.unpack_from("<5I", data)
    if (magic, version, size, json_tag) != (0x46546c67, 2, len(data), 0x4e4f534a):
        raise ValueError("invalid glTF 2 GLB header")
    offset = 20 + json_size
    if offset + 8 > len(data):
        raise ValueError("truncated binary header")
    binary_size, binary_tag = struct.unpack_from("<II", data, offset)
    if binary_tag != 0x004e4942 or offset + 8 + binary_size > len(data):
        raise ValueError("bad GLB buffer")
    return json.loads(data[20:offset]), memoryview(data)[offset + 8:offset + 8 + binary_size]

def view_bytes(root, binary, idx):
    v = root["bufferViews"][idx]
    off = v.get("byteOffset", 0)
    end = off + v["byteLength"]
    if off < 0 or end > len(binary):
        raise ValueError("bad bufferView bounds")
    return binary[off:end].tobytes()

def accessor(root, binary, idx):
    a = root["accessors"][idx]
    if a.get("componentType") != 5126 or a.get("type") != "VEC2" or "sparse" in a:
        raise ValueError(f"UV accessor {idx} unsupported")
    view = root["bufferViews"][a["bufferView"]]
    n = a["count"]
    stride = view.get("byteStride", 8)
    start = view.get("byteOffset", 0) + a.get("byteOffset", 0)
    end = view.get("byteOffset", 0) + view["byteLength"]
    if stride < 8 or start < 0 or start + max(0, n - 1) * stride + 8 > end:
        raise ValueError(f"UV accessor {idx} out of bounds")
    return [struct.unpack_from("<ff", binary, start + i * stride) for i in range(n)]

def indices(root, binary, idx):
    a = root["accessors"][idx]
    kind = {5121: ("B", 1), 5123: ("H", 2), 5125: ("I", 4)}.get(a["componentType"])
    if kind is None or a.get("type") != "SCALAR" or "sparse" in a:
        raise ValueError("unsupported indices")
    view = root["bufferViews"][a["bufferView"]]
    start = view.get("byteOffset", 0) + a.get("byteOffset", 0)
    stride = view.get("byteStride", kind[1])
    end = view.get("byteOffset", 0) + view["byteLength"]
    if start < 0 or stride < kind[1] or start + max(0, a["count"] - 1) * stride + kind[1] > end:
        raise ValueError("index buffer out of bounds")
    return [struct.unpack_from("<" + kind[0], binary, start + i * stride)[0] for i in range(a["count"])]

def inspect(path, target):
    root, binary = load_glb(path)
    images = root.get("images", [])
    mat = root.get("materials", [])
    textures = root.get("textures", [])
    report = {"source": str(path), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
              "materials": [], "warnings": []}
    atlas_edges = {i: [] for i in range(len(images))}
    for mesh_idx, mesh in enumerate(root.get("meshes", [])):
        for prim_idx, p in enumerate(mesh.get("primitives", [])):
            m_idx = p.get("material")
            if m_idx is None or m_idx >= len(mat):
                report["warnings"].append(f"{mesh_idx}:{prim_idx} no valid material")
                continue
            pbr = mat[m_idx].get("pbrMetallicRoughness", {})
            tex = pbr.get("baseColorTexture", {})
            ti = tex.get("index")
            if ti is None or ti >= len(textures) or "source" not in textures[ti]:
                report["warnings"].append(f"{mesh_idx}:{prim_idx} no base texture image")
                continue
            image_idx = textures[ti]["source"]
            uvset = tex.get("texCoord", 0)
            ai = p["attributes"].get(f"TEXCOORD_{uvset}")
            if ai is None:
                report["warnings"].append(f"{mesh_idx}:{prim_idx} missing UV{uvset}")
                continue
            uv, ii = accessor(root, binary, ai), indices(root, binary, p["indices"])
            if len(ii) % 3:
                raise ValueError("not triangles")
            for k in range(0, len(ii), 3):
                ids = ii[k:k+3]
                if max(ids) >= len(uv):
                    raise ValueError("UV index outside accessor")
                atlas_edges[image_idx].append([uv[ids[0]], uv[ids[1]], uv[ids[2]]])
            values = [uv[i] for i in set(ii)]
            report["materials"].append({
                "mesh": mesh_idx, "primitive": prim_idx,
                "material": mat[m_idx].get("name", str(m_idx)),
                "image": image_idx, "triangles": len(ii)//3,
                "uv_u": [min(x for x,y in values), max(x for x,y in values)],
                "uv_v": [min(y for x,y in values), max(y for x,y in values)],
                "source_sampler": "glTF normalized UV0, SVG V-origin top-left"
            })
    target.mkdir(parents=True, exist_ok=True)
    for image_idx, image in enumerate(images):
        if image.get("mimeType") != "image/png" or "bufferView" not in image:
            report["warnings"].append(f"image {image_idx} not embedded PNG")
            continue
        png = view_bytes(root, binary, image["bufferView"])
        if png[:8] != bytes.fromhex("89504e470d0a1a0a"):
            report["warnings"].append(f"image {image_idx} invalid PNG signature")
            continue
        w, h = struct.unpack_from(">II", png, 16)
        if not (1 <= w <= 4096 and 1 <= h <= 4096):
            raise ValueError("source texture dimension over budget")
        (target / f"image-{image_idx}.png").write_bytes(png)
        # SVG wireframe is a diagnostic overlay over the exact original PNG.
        paths = []
        for triangle in atlas_edges[image_idx]:
            coords = [(u*w, v*h) for u,v in triangle]
            paths.append(f'<path d="M {coords[0][0]:.3f},{coords[0][1]:.3f} '
                         f'L {coords[1][0]:.3f},{coords[1][1]:.3f} '
                         f'L {coords[2][0]:.3f},{coords[2][1]:.3f} Z"/>')
        pic = base64.b64encode(png).decode("ascii")
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
               f'<image width="{w}" height="{h}" href="data:image/png;base64,{pic}"/>'
               '<g fill="none" stroke="#ff00ff" stroke-width="0.9" stroke-opacity=".65">'
               + "".join(paths) + '</g></svg>')
        (target / f"uv-image-{image_idx}.svg").write_text(svg, encoding="utf-8")
        report.setdefault("images", []).append({"id": image_idx, "name": image.get("name"),
            "width": w, "height": h, "png_sha256": hashlib.sha256(png).hexdigest(),
            "overlay_triangles": len(paths)})
    (target / "audit.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    return report

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--models", nargs="*", default=DEFAULT_IDS)
    ap.add_argument("--output", default="build/dev-room/source-uv")
    options = ap.parse_args()
    dest = pathlib.Path(options.output)
    for name in options.models:
        try:
            result = inspect(pathlib.Path("assets/runtime/models")/(name+".glb"), dest/name)
            print(name, "source images", len(result.get("images", [])), "warnings", result["warnings"])
        except (OSError, ValueError, IndexError, KeyError, struct.error) as exc:
            target = dest/name
            target.mkdir(parents=True, exist_ok=True)
            (target/"error.txt").write_text(str(exc),encoding="utf-8")
            print("ADVISORY GLB INSPECTION FAILED", name, exc)
    # Source-art diagnostics are never build or release gates.

if __name__ == "__main__":
    main()
