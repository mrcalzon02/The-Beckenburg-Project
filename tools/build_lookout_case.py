"""Fit the Lookout records and furnish the maintenance and supply rooms."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, bounds, point, world
from build_lookout_native_arrival import table
from build_mine_case import Case, DOWN
from build_prison_sally_gate import export
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT/"development/levels/rebuild/lookout-power-pass"


def fit_workrooms(assembly):
    desk, mounting = assembly.fit("level_workbench_worn", "Hut communications bench",
                                  "hut", 9.3, 6, 15.7)
    top, normal, _, _ = bvh(desk).ray_cast(point(9.3, 7.55, 15.7), DOWN, 1.55)
    assert top is not None and normal.z > .98, "Communications bench needs a radio surface"
    radio, placement = assembly.fit("level_event_radio_worn", "Hut backup radio", "hut",
                                     9.3, top.z + .001, 15.7, support=desk)
    assert len(radio) == 1
    radio[0].name = "Lookout hut backup radio"
    low, high = bounds(radio)
    at, _, _, _ = bvh(radio).ray_cast(point(9.3, low[1] + .065, high[2] + .8),
                                      point(0, 0, -1), 1.6)
    assert at is not None, "Backup radio needs an exposed face"
    x, y, z = world(at)
    socket = ("SERVICE", radio[0].name, "native-device", 3, x, y, z + .035, 0, "RADIO")
    details = [dict(zone="hut", label="communications desk", **mounting),
               dict(zone="hut", label="backup radio", **placement)]
    for asset, label, room, x, z in (
        ("level_camp_bench", "Hut rest bench", "hut", 8.3, 23.3),
        ("level_work_repair_bench", "Supply repair bench", "store", 42.25, 36.35),
        ("level_work_crew_footlocker", "Supply equipment locker", "store", 42.15, 32.55),
    ):
        _, entry = assembly.fit(asset, label, room, x, 6, z)
        details.append(dict(zone=room, label=label, **entry))
    for part in assembly.built:
        part.name = part.name.replace("Mine case ", "Lookout ", 1)
    return socket, details


def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Case()
    old=bpy.data.objects["level_blockout_workbench.001"]
    bench=old.copy()
    assembly.scene.collection.objects.link(bench)
    bench.name="Ranger station radio workbench"
    bench.matrix_world=Matrix.Translation(point(35,0,1.10)) @ old.matrix_world
    bench["role"]="floor-bearing station radio workbench"
    bench["zone"]="ruin"
    assembly.built.append(bench)
    bpy.context.view_layer.update()

    foundation=bvh([o for o in assembly.scene.objects if o.type=="MESH"
                    and o.name.startswith("Lookout terrace ruin complete ground floor")])
    envelope=bounds([bench])
    lo,hi=envelope
    assert abs(lo[1]-6)<.003 and 40.25<lo[0]<hi[0]<53.75 and 14.25<lo[2]<hi[2]<29.75,envelope
    for x in (lo[0]+.15,hi[0]-.15):
        for z in (lo[2]+.12,hi[2]-.12):
            at,normal,_,_=foundation.ray_cast(point(x,6.12,z),Vector((0,0,-1)),.20)
            assert at is not None and abs(at.z-6)<.003 and normal.z>.98,(x,z,at)
    original=bvh([o for o in assembly.scene.objects if o.type=="MESH" and o.name in assembly.originals
                  and "ground floor" not in o.get("role","")])
    assert not original.overlap(bvh([bench])),"Ranger bench intersects retained work"

    placings=(
        ("TRANSMISSION","level_story_letters",4,43.05,15.73,1,bench),
        (None,"level_event_radio_worn",4,42.41,15.58,1,bench),
        ("SIGHTING_LOG","level_trace_journal",10,25.28,2.42,.48,bpy.data.objects["level_blockout_lookout_spotter.001"]),
    )
    details=[]
    sockets=[]
    for doc,asset,owner,x,z,scale,support in placings:
        surface=bvh([support])
        at,normal,_,_=surface.ray_cast(point(x,18,z),Vector((0,0,-1)),14)
        assert at is not None and normal.z>.995,(doc,at)
        name=doc or "disconnected station radio"
        parts=assembly.place(asset,Matrix.Translation(point(x,at.z+.0004,z)) @ Matrix.Scale(scale,4),
                             "Lookout "+name,"supported ranger record" if doc else "historical radio fixture",
                             "ruin" if owner==4 else "cab")
        lo,hi=bounds(parts)
        assert abs(lo[1]-at.z)<.002
        for a in (lo[0]+.008,hi[0]-.008):
            for b in (lo[2]+.008,hi[2]-.008):
                hit,n,_,_=surface.ray_cast(point(a,at.z+.045,b),Vector((0,0,-1)),.09)
                assert hit is not None and abs(hit.z-at.z)<.003 and n.z>.98,(name,a,b,hit)
        assert not original.overlap(bvh(parts)),name
        for obj in parts:
            obj.name=obj.name.removeprefix("College ")
        details.append(dict(id=name,asset=asset,asset_sha256=sha(ASSETS/(asset+".glb")),owner=owner,
                            position=[x,round(at.z+.0004,6),z],scale=scale,support=support.name,bounds=[lo,hi]))
        if doc:
            sockets.append(("STORY","lookout-story-"+doc.lower(),doc,owner,asset,x,round(at.z+.0004,6),z,0,scale,support.name))

    backup_radio, workrooms = fit_workrooms(assembly)
    sockets.append(backup_radio)

    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"lookout-case.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_additions.glb"
    export(addition,assembly.built)
    append(ROOT/source["glb"],addition,targets["glb"],reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],set(),assembly.built,
                 open_components={o.name for o in assembly.built})
    retained=native_records(ROOT/source["native"])
    current=native_records(targets["hcs2"])
    assert current[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in current[len(retained):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]

    hashes={ext:sha(f) for ext,f in targets.items()}
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    for line in (BASE/"arrival.tsv").read_text().splitlines()[1:]:
        row = line.split("\t")
        if row == ["POINT", "L14B", "27", "15", "5"]:
            # Follow the clear aisle beyond the inner swing before turning to the deck.
            rows.append(("POINT", "L14B", 25.5, 15, 5))
        rows.append(row)
    rows.extend(sockets)
    table(out/"arrival.tsv",rows)
    for filename in ("door-survey.tsv","mission.tsv"):
        (out/filename).write_bytes((BASE/filename).read_bytes())
    receipt=dict(exports=hashes,retained_components=len(retained),added_components=len(current)-len(retained),
                 preserved_bench_source="level_blockout_workbench.001",records=details,workrooms=workrooms)
    (out/"fitted-records.json").write_text(json.dumps(receipt,indent=2)+"\n",newline="\n")
    print("PASS grounded bench and",len(details),"mounted ranger objects;",len(retained),"retained components")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
