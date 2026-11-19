"""Independently reopen the Battery installation and match its changed mesh triangles."""
from pathlib import Path
import json
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import image_identity, read
from build_seacliff_power import BASE, STEM
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry


def verify(source):
    meta = json.loads((source / "power.json").read_bytes())
    for ext, value in meta["export"].items():
        assert sha(source / (STEM + "." + ext)) == value
    original = BASE / "wld-17" / "founded-receiving-stairs"
    old, old_bytes = read(original.with_suffix(".glb"))
    current, current_bytes = read(source / (STEM + ".glb"))
    assert current_bytes.startswith(old_bytes)
    for key in ("nodes", "meshes", "bufferViews", "accessors", "materials"):
        assert current[key][:len(old[key])] == old[key], key
    for former, later in zip(old["textures"], current["textures"], strict=False):
        assert {k:v for k,v in former.items() if k!="source"} == {k:v for k,v in later.items() if k!="source"}
        assert image_identity(old,old_bytes,former["source"]) == image_identity(current,current_bytes,later["source"])
    before = native_records(original.with_suffix(".hcs2"))
    parts = native_records(source / (STEM + ".hcs2"))
    assert len(before) == meta["retained_physical_parts"] and parts[:len(before)] == before
    added = parts[len(before):]
    count = sum(part[2] for part in added)
    assert count == meta["native_triangle_additions"]

    def compare():
        for part in added:
            obj = bpy.context.scene.objects[part[0]]
            assert match_geometry(triangles(part),[t for t,_ in records(obj)]), part[0]

    bpy.ops.wm.open_mainfile(filepath=str(source / (STEM + ".blend")))
    compare()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source / (STEM + ".glb")))
    compare()
    return dict(retained_components=len(before), matching_new_components=len(added),
                matching_triangles_per_export=count, exact_original_texture_bindings=len(old["textures"]),
                hashes=meta["export"])


if __name__ == "__main__":
    source,target = (Path(p).resolve() for p in sys.argv[sys.argv.index("--")+1:])
    report = json.dumps(verify(source), indent=2)+"\n"
    target.write_text(report,encoding="utf-8",newline="\n")
    print(report)
