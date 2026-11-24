"""Fit the retained Prison sally-port opening with a wall-mounted manual slide gate."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point, world
from build_deliberate_entrances import solid
from build_roster_foundations import material
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild/prison-public-entry-pass"
GATE = ROOT / "development/levels/primitive-props-v1/models/level_blockout_prison_sally_gate.glb"
TRAVEL = 2.53
DEPTH = 58.382
CONTROL_X = 32.44
CONTROL_Y = 1.30


def export(path, objects):
    bpy.ops.object.select_all(action="DESELECT")
    for obj in objects:
        obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(path), export_format="GLB", use_selection=True, export_extras=True)


def moving_panel(steel):
    previous = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(GATE))
    imported = set(bpy.context.scene.objects) - previous
    source, = [obj for obj in imported if obj.type == "MESH"]
    # The rough is a single mesh. Preserve its moving bars and cross rail.
    # Its stock posts, overhead member and raised stone base belong to the
    # freestanding rough and would duplicate the surveyed jambs and pavement.
    import bmesh
    bm = bmesh.new()
    bm.from_mesh(source.data)
    coordinates = {vert: world(source.matrix_world @ vert.co) for vert in bm.verts}
    fixed = [face for face in bm.faces
             if all(abs(coordinates[v][2]) >= .15 for v in face.verts)
             or any(abs(coordinates[v][0]) > 1.181 or coordinates[v][1] > 2.70
                    for v in face.verts)]
    bmesh.ops.delete(bm, geom=fixed, context="FACES")
    # All surviving triangles must be part of the original nine bars and cross rail.
    assert len(bm.faces) == 120, len(bm.faces)
    for vert in bm.verts:
        x, y, z = coordinates[vert]
        assert abs(x) <= 1.181 and .144 <= y <= 2.696 and abs(z) <= .066
        vert.co = point(34 + x * (1.198 / 1.18), .058 + (y - .145) * .975, DEPTH + z)
    obj = bpy.data.objects.new("Sally gate retained bars and cross rail", bpy.data.meshes.new("Sally gate moving steel"))
    bpy.context.scene.collection.objects.link(obj)
    for mat in source.data.materials:
        obj.data.materials.append(mat)
    bm.to_mesh(obj.data)
    bm.free()
    for old in imported:
        bpy.data.objects.remove(old, do_unlink=True)
    obj["source_asset"] = GATE.stem
    obj["role"] = "Sliding gate panel"
    parts = [obj]
    for name, low, high in (("toe rail", .05, .12), ("head rail", 2.51, 2.584)):
        parts.append(solid(f"Sally gate moving {name}", [32.802,DEPTH-.05,35.198,DEPTH+.05],
                           low,high,steel,"Sliding leaf","sally"))
    for x in (33.02, 34.98):
        parts.append(solid(f"Sally gate travelling hanger {x:g}", [x-.063,DEPTH-.034,x+.063,DEPTH+.034],
                           2.565,2.692,steel,"Sliding leaf hanger","sally"))
    return parts


def fixed_mounts(assembly, steel, enamel):
    receivers = []
    for x in (32.55, 33.7, 34.65, 35.65, 36.65, 37.73):
        hit, _, obj = assembly.measured(point(x,2.705,58.68),point(0,0,-1),.7,"gate track bracket")
        face = -hit.y
        receivers.append(dict(component=obj.name, contact=world(hit)))
        assembly.box(f"Sally gate bracket {x:g}", [x-.07,face-.015,x+.07,DEPTH+.066],
                     2.70,2.76,steel,"Anchored sally gate track bracket","sally")
    assembly.box("Sally gate overhead guide", [32.48,DEPTH-.056,37.78,DEPTH+.056],
                 2.678,2.725,steel,"Wall mounted sliding track","sally")
    for x in (32.52,37.70):
        assembly.box(f"Sally gate track stop {x:g}", [x-.045,DEPTH-.075,x+.045,DEPTH+.075],
                     2.635,2.741,steel,"Mechanical travel stop","sally")
    controls = []
    for side, owner, zone in ((1,2,"sally"),(-1,12,"yard")):
        hit, _, obj = assembly.measured(point(CONTROL_X,CONTROL_Y,58.12+side*.65),
                                        point(0,0,-side),.65,"manual gate release")
        face=-hit.y
        back, front = sorted((face-side*.012, face+side*.038))
        assembly.box(f"Sally gate release backing {zone}", [CONTROL_X-.12,back,CONTROL_X+.12,front],
                     1.155,1.445,steel,"Wall backed manual release",zone)
        back, front = sorted((face+side*.034,face+side*.104))
        assembly.box(f"Sally gate lever {zone}", [CONTROL_X-.085,back,CONTROL_X+.085,front],
                     1.21,1.39,enamel,"Manual latch release handle",zone)
        controls.append(dict(id=f"sally-release-{zone}",owner=owner,at=[CONTROL_X,CONTROL_Y,(back+front)/2],
                             depth=(front-back)/2,wall=obj.name,receiver=world(hit)))
    return receivers, controls


# Measured supported destinations in the original room volumes.
ANCHORS = (
    ("a-lower-gallery",15,0,38), ("reception",14,0,65), ("sally",32.44,0,59.109),
    ("dining",16,0,10), ("b-lower-gallery",53,0,38), ("medical-link",30,0,10),
    ("chapel",64,0,43), ("a-upper-gallery",15,3.4,54.8),
    ("isolation-link",59.5,0,25), ("records",20,0,65), ("b-upper-gallery",53,3.4,54.8),
    ("plant",36,-3,30), ("yard",32.44,0,57.131), ("entry",34,0,73),
)

# The open entries here identify existing surveyed apertures and real stair
# landings. The guide stations below only drive movement verification.
TRANSITIONS = (
    ("L042",453,"z"), ("L047",454,"z"), ("L058",455,"z"), ("L063",456,"z"),
    ("L065",457,"z"), ("L073",458,"z"), ("L085",459,"z"), ("L088",460,"z"),
    ("L089",461,"x"), ("L095",462,"z"), ("L100",463,"z"),
)
WALKS = ("L066","L068","L080","L112")


def mission_table(out, layout, construction):
    rooms={r["id"]:r for r in layout["zones"]}
    links={r["id"]:r for r in layout["links"]}
    rows=[("ANCHOR",owner,*anchor) for owner,anchor in enumerate(ANCHORS)]
    for key, id, axis in TRANSITIONS:
        link=links[key]
        a,b=(rooms[link[k]] for k in ("a","b"))
        assert link["player_access"]=="OPEN_ROUTE" and a["gameplay_eligible"] and b["gameplay_eligible"]
        at=link.get("threshold",link["points"][-1])
        rows.append(("TRANSITION",id,key,a["owner"],b["owner"],*at,link["width"],axis,
                     "stair" if link["kind"]=="stairs" else "trail"))
    for key in [row[0] for row in TRANSITIONS]+list(WALKS):
        points=links[key]["points"]
        if key in ("L047","L063"):
            # Start on the lower landing. The old station at Z48 lies on the first tread.
            points=[[points[0][0],0,47.1],*points[1:]]
        if key=="L112":
            # The lower flight point at Z24 is already one riser up.
            points=[points[0],*points[2:]]
        rows.extend(("WALK",key,*point) for point in points)
    rows.extend(("TREAD",name) for name in construction["stair_components"])
    (out/"mission.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())


def build(out, editor_probe=None):
    packet=json.loads((BASE/"checkpoint.json").read_bytes())["source_authority"]
    src={key:ROOT/packet[key] for key in ("blend","glb","native")}
    assert sha(src["glb"])==packet["glb_sha256"] and sha(src["native"])==packet["native_sha256"]
    assert GATE.is_file() and sha(GATE) in (ROOT/"development/assets/asset-library-index.json").read_text(encoding="utf-8")
    layout=json.loads((ROOT/packet["layout"]).read_bytes())
    frame=next(row for row in layout["authored_doorways"] if row["id"]=="PRISON-court-sally-N")
    assert (frame["along"],frame["boundary"],frame["sill_y"],frame["clear_width"],frame["clear_height"])==(34,58,0,2.4,2.6)

    bpy.ops.wm.open_mainfile(filepath=str(src["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=Assembly()
    steel=material("Penitentiary worn galvanized gate steel",(.25,.29,.30))
    enamel=material("Penitentiary manual release ochre",(.71,.37,.10))
    brackets, controls=fixed_mounts(assembly,steel,enamel)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    moved=moving_panel(steel)
    out.mkdir(parents=True,exist_ok=True)
    additions=out/"_mounts.glb"
    targets={ext:out/f"prison-sally-port.{ext}" for ext in ("blend","glb","hcs2")}
    export(additions,assembly.built)
    append(src["glb"],additions,targets["glb"],reuse_embedded_images=True)
    additions.unlink()
    native_write(src["native"],targets["hcs2"],set(),assembly.built)
    retained=native_records(src["native"])
    result=native_records(targets["hcs2"])
    assert result[:len(retained)]==retained
    leaf=out/"sally-moving-panel.glb"
    export(leaf,moved)
    closed=bpy.data.collections.new("Sally gate closed pose")
    opened=bpy.data.collections.new("Sally gate open pose")
    for collection in (closed,opened):
        assembly.scene.collection.children.link(collection)
    for obj in moved:
        closed.objects.link(obj)
        for collection in list(obj.users_collection):
            if collection != closed:
                collection.objects.unlink(obj)
        peer=obj.copy()
        peer.name=obj.name+" parked east"
        peer.location.x+=TRAVEL
        opened.objects.link(peer)
    opened.hide_render=True
    opened.hide_viewport=True
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in result[len(retained):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    rows=(BASE/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),sha(targets["glb"]),
                       targets["hcs2"].relative_to(ROOT).as_posix(),sha(targets["hcs2"])))
    (out/"arrival.tsv").write_bytes(("\n".join(rows)+"\n").encode())
    (out/"door-survey.tsv").write_bytes((BASE/"door-survey.tsv").read_bytes())
    gate=(452,frame["id"],"sally","yard",2,12,34,0,58,2.4,"z",*frame["frame_span"],2.6,
          leaf.relative_to(ROOT).as_posix(),sha(leaf),TRAVEL)
    release=[(c["id"],c["owner"],*c["at"],0,452) for c in controls]
    (out/"gate.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in [gate,*release]).encode())
    proof=dict(asset=GATE.relative_to(ROOT).as_posix(),asset_sha256=sha(GATE),mount_brackets=brackets,
               release_controls=controls,travel_m=TRAVEL,retained_components=len(retained),
               added_components=len(result)-len(retained),exports={ext:sha(path) for ext,path in targets.items()})
    (out/"construction.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())
    if editor_probe is not None:
        export_editor_poses(targets["blend"],editor_probe)
    mission_table(out,layout,json.loads((ROOT/packet["construction"]).read_bytes()))
    print("PASS six wall bearings, two manual release controls and",len(retained),"unchanged components")


def export_editor_poses(source,target):
    bpy.ops.wm.open_mainfile(filepath=str(source))
    with target.open("w",encoding="utf-8",newline="\n") as stream:
        for state in ("closed","open"):
            group=bpy.data.collections[f"Sally gate {state} pose"]
            for obj in group.objects:
                obj.data.calc_loop_triangles()
                for triangle in obj.data.loop_triangles:
                    vertices=[obj.matrix_basis @ obj.data.vertices[i].co for i in triangle.vertices]
                    coords=[value for p in vertices for value in (p.x,p.z,-p.y)]
                    stream.write(f"452\t{state}\t"+"\t".join(f"{v:.8f}" for v in coords)+"\n")


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    build(Path(args[0]).resolve(),Path(args[1]).resolve() if len(args)>1 else None)
