"""Fit the Wharf records, cottage furnishings and working devices into the native scene."""
import json
from pathlib import Path
import sys

import bpy

sys.path.insert(0, str(Path(__file__).resolve().parent))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import bounds, point, world
from build_lookout_native_arrival import table
from build_mine_case import Case, DOWN, ROOT
from build_prison_sally_gate import export
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "wharf-distribution-pass"
OUTPUT = BASE / "wharf-case-pass"



def cottage_furnishings(case):
    services, receipt = [], []
    for zone, owner, label, kind, x, z in (
        ("cottage-a", 0, "Fisherman telephone", "PHONE", 9.4, 44.2),
        ("cottage-b", 1, "Marine radio", "RADIO", 54.6, 44.2),
    ):
        supports, table_receipt = case.fit("level_event_side_table_worn", label + " table", zone, x, 0, z)
        contact, normal, _, _ = bvh(supports).ray_cast(point(x, 1.65, z), DOWN, 1.6)
        assert contact is not None and normal.z > .98, label
        parts, device_receipt = case.fit(f"level_event_{kind.lower()}_worn", label, zone, x,
                                         contact.z + .001, z, support=supports)
        assert len(parts) == 1, label
        parts[0].name = "Wharf " + label
        low, high = bounds(parts)
        face, _, _, _ = bvh(parts).ray_cast(point(x, low[1] + .065, high[2] + .8),
                                            point(0, 0, -1), 1.6)
        assert face is not None, label
        px, py, pz = world(face)
        services.append(("SERVICE", parts[0].name, "native-device", owner, px, py, pz + .035, 0, kind))
        receipt.append(dict(zone=zone, table=table_receipt, device=device_receipt))
    for asset, zone, label, x, z in (
        ("level_trace_camp_bed", "cottage-a", "West cottage bunk", 8.5, 49.2),
        ("level_trace_camp_bed", "cottage-b", "East cottage bunk", 55.5, 49.2),
        ("level_camp_bench", "cottage-a", "West cottage seat", 16.4, 44.2),
        ("level_camp_bench", "cottage-b", "East cottage seat", 47.6, 44.2),
    ):
        _, placement = case.fit(asset, label, zone, x, 0, z)
        receipt.append(dict(zone=zone, label=label, **placement))
    for part in case.built:
        part.name = part.name.replace("Mine case ", "Wharf ", 1)
    return services, receipt


def build():
    authority = json.loads((SOURCE / "construction.json").read_bytes())
    assert all(sha(ROOT / name) == checksum for name, checksum in authority["exports"].items())
    parent = SOURCE / "wharf-distribution"
    bpy.ops.wm.open_mainfile(filepath=str(parent.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version = 0
    case = Case()
    receipt = []
    story = []
    for doc, zone, owner, bench, mesh, center, approach in (
        ("DEPARTURE_TALLY", "boathouse", 2, "level_blockout_workbench.001",
         "level_trace_journal", (7.8, 10.56), (7.825, 0, 11.9)),
        ("CROSSING_SHEET", "fish", 4, "level_blockout_fish_sorting_bench.001",
         "level_story_letters", (39.29, 13.56), (37.94, 0, 13.577)),
    ):
        x, z = center
        support = bpy.context.scene.objects[bench]
        surface, normal, _, _ = bvh([support]).ray_cast(point(x, 1.45, z), DOWN, 1.4)
        assert surface is not None and normal.z > .98, bench
        props, placement = case.fit(mesh, doc.lower(), zone, x, surface.z + .0005, z,
                                    scale=.42, support=[support])
        for prop in props:
            prop.name = "Wharf " + prop.name.removeprefix("Mine ")
            prop["role"] = "workbench-supported readable record"
        contact, _, _, _ = bvh(props).ray_cast(point(x + .025, surface.z + .1, z + .017), DOWN, .12)
        assert contact is not None, doc
        aim = world(contact)
        story.append(("STORY", "wharf-story-" + doc.lower(), doc, owner, mesh,
                      *placement["pivot"], placement["yaw"], placement["scale"], *approach, *aim))
        receipt.append(dict(document=doc, owner=owner, zone=zone, top_m=surface.z,
                            approach=approach, aim=aim, **placement))

    services, cottage_receipt = cottage_furnishings(case)

    OUTPUT.mkdir(exist_ok=True)
    targets = {ext: OUTPUT / f"wharf-case.{ext}" for ext in ("blend", "glb", "hcs2")}
    fragment = ROOT / "build/wharf-case-fragment.glb"
    export(fragment, case.built)
    append(parent.with_suffix(".glb"), fragment, targets["glb"], reuse_embedded_images=True)
    fragment.unlink()
    native_write(parent.with_suffix(".hcs2"), targets["hcs2"], set(), case.built,
                 open_components={obj.name for obj in case.built})
    prior, current = (native_records(path) for path in (parent.with_suffix(".hcs2"), targets["hcs2"]))
    assert current[:len(prior)] == prior and len(current) - len(prior) == len(case.built)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]), compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in current[len(prior):]:
        rendered = records(bpy.context.scene.objects[part[0]])
        assert match_geometry(triangles(part), [face for face, _ in rendered]), part[0]

    table(OUTPUT / "room-layout.tsv",
          [("SOURCE", sha(targets["glb"]), sha(targets["hcs2"])), *(line.split("\t") for line in (SOURCE / "room-layout.tsv").read_text().splitlines()), *story, *services])
    (OUTPUT / "door-schedule.tsv").write_bytes((SOURCE / "door-schedule.tsv").read_bytes())
    result = dict(exports={path.relative_to(ROOT).as_posix(): sha(path) for path in targets.values()},
                  prior_exports=authority["exports"], retained_parts=len(prior),
                  added_parts=len(current) - len(prior), supported_records=receipt,
                  cottage_furnishings=cottage_receipt)
    (OUTPUT / "construction.json").write_text(json.dumps(result, indent=2) + "\n",
                                               encoding="utf-8", newline="\n")
    print(f"PASS Wharf records, cottage furnishing and mounted devices; {len(prior)} exact retained collision parts")


if __name__ == "__main__":
    build()
