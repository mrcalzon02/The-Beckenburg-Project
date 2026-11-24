"""Fit canonical walk-in cover to the Lookout's surveyed interior slabs."""
import json
import math
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly
from build_lookout_native_arrival import OUT as ARRIVAL, OWNERS, SOURCE, table
from build_prison_sally_gate import export
from build_school_electrical import cupboards
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

POSITIONS = (
    ("hut", 5.5, 19, -math.pi / 2),
    ("store", 48.5, 35, math.pi / 2),
    ("cab", 28.05, 3.25, 0),
)


def build(out):
    receipt = json.loads((ARRIVAL / "checkpoint.json").read_bytes())
    authority = receipt["source_authority"]
    source_model, source_physics = ROOT / authority["glb"], ROOT / authority["native"]
    source = (ARRIVAL / "arrival.tsv").read_text().splitlines()
    assert [sha(source_model), sha(source_physics)] == source[0].split("\t")[2::2]
    layout = json.loads((SOURCE / "authored-layout.json").read_bytes())
    zones = {zone["id"]: dict(zone, owner=OWNERS[zone["id"]][0])
             for zone in layout["zones"] if zone["id"] in {"hut", "store", "cab"}}

    bpy.ops.wm.open_mainfile(filepath=str(ARRIVAL / "lookout-fitted.blend"))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    fitted = cupboards(assembly, zones, POSITIONS, site="lookout",
                       floor_roles=("floor", "complete building ground floor",
                                    "complete supported top floor"))
    for obj in assembly.built:
        obj.name = obj.name.removeprefix("College ")

    out.mkdir(parents=True, exist_ok=True)
    target = {ext: out / f"lookout-cover.{ext}" for ext in ("blend", "glb", "hcs2")}
    additions = out / "_additions.glb"
    export(additions, assembly.built)
    append(source_model, additions, target["glb"], reuse_embedded_images=True)
    additions.unlink()
    native_write(source_physics, target["hcs2"], set(), assembly.built,
                 open_components={obj.name for obj in assembly.built})
    previous = native_records(source_physics)
    result = native_records(target["hcs2"])
    assert result[:len(previous)] == previous

    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]), compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for part in result[len(previous):]:
        visible = [tri for tri, _ in records(bpy.context.scene.objects[part[0]])]
        assert match_geometry(triangles(part), visible), part[0]

    hashes = {ext: sha(path) for ext, path in target.items()}
    rows = [("SOURCE", target["glb"].relative_to(ROOT).as_posix(), hashes["glb"],
             target["hcs2"].relative_to(ROOT).as_posix(), hashes["hcs2"])]
    rows.extend(line.split("\t") for line in source[1:])
    rows.extend(("COVER", s["id"], s["room"], *s["position"], s["yaw"]) for s in assembly.sockets)
    table(out / "arrival.tsv", rows)
    for filename in ("mission.tsv", "door-survey.tsv"):
        (out / filename).write_bytes((ARRIVAL / filename).read_bytes())
    result = dict(exports=hashes, retained_components=len(previous),
                  cupboard_components=len(result)-len(previous), cupboards=fitted)
    (out / "hiding.json").write_text(json.dumps(result, indent=2)+"\n", newline="\n")
    print("PASS three grounded walk-in cupboards,", len(previous), "retained components")


if __name__ == "__main__":
    build(Path(sys.argv[sys.argv.index("--") + 1]).resolve())
