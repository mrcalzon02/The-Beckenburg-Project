"""Rebuild the Prison vehicle facade and fit a manually released sliding gate."""
import json
from pathlib import Path
from shutil import copyfile
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append, read as read_glb
from build_circulation_construction import native_write
from build_college_electrical import Assembly, bounds, point, world
from build_deliberate_entrances import solid
from build_prison_sally_gate import DEPTH as YARD_DEPTH, GATE, export, moving_panel
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild/prison-hiding-pass"
REMOVED = {"Header sally S", "Flat route shell Wall sally S", "Flat route shell Wall sally S.001",
           "sally-port eave closure south"}
WALL = [26, 67.76, 42, 68]
OPENING = [32.5, 35.5, 3.04]
CENTRE = 34
DEPTH = 67.60
TRAVEL = 3.13
CONTROL_X = 32.10
HEIGHT_SCALE = OPENING[2] / 2.6
WIDTH_SCALE = 2.996 / 2.396


def facade(masonry,eave):
    west,z0,east,z1 = WALL
    left,right,head = OPENING
    return [
        solid("Prison south vehicle facade west pier", [west,z0,left,z1], 0,head,masonry,
              "Full-depth south vehicle masonry pier","sally"),
        solid("Prison south vehicle facade east pier", [right,z0,east,z1], 0,head,masonry,
              "Full-depth south vehicle masonry pier","sally"),
        solid("Prison south vehicle facade continuous lintel and head", WALL,head,3.84,masonry,
              "Continuous head on both founded piers","sally"),
        solid("Prison south vehicle raised eave closure",[west,67.8,east,z1],4.0,4.24,eave,
              "Raised roof enclosure bearing on the retained ceiling cap","sally"),
    ]


def mounts(assembly, steel, enamel):
    bearings = []
    for x in (32.08,33.18,34.28,35.38,36.48,37.58,38.58):
        hit,_,obj = assembly.measured(point(x,3.185,67.35),point(0,0,1),.64,"vehicle gate rail bearing")
        face = -hit.y
        assert abs(face-WALL[1]) < .006
        bearings.append(dict(component=obj.name,contact=world(hit)))
        assembly.box(f"Vehicle gate bracket {x:g}", [x-.07,DEPTH-.075,x+.07,face+.015],
                     3.17,3.25,steel,"Masonry-anchored vehicle gate track bracket","sally")
    assembly.box("Vehicle gate overhead track",[32.03,DEPTH-.060,38.73,DEPTH+.060],
                 2.678*HEIGHT_SCALE,3.194,steel,"Wall mounted vehicle track","sally")
    for x in (32.07,38.70):
        assembly.box(f"Vehicle gate travel stop {x:g}",[x-.05,DEPTH-.080,x+.05,DEPTH+.080],
                     3.082,3.222,steel,"Vehicle track travel stop","sally")
    releases=[]
    for side,owner,zone in ((-1,2,"sally"),(1,13,"forecourt")):
        hit,_,obj = assembly.measured(point(CONTROL_X,1.30,67.88+side*.69),point(0,0,-side),.75,
                                       "two-sided vehicle release")
        face=-hit.y
        low,high=sorted((face-side*.012,face+side*.038))
        assembly.box(f"Vehicle gate release backing {zone}",[CONTROL_X-.12,low,CONTROL_X+.12,high],
                     1.155,1.445,steel,"Wall backed manual release",zone)
        low,high=sorted((face+side*.034,face+side*.104))
        assembly.box(f"Vehicle gate lever {zone}",[CONTROL_X-.085,low,CONTROL_X+.085,high],
                     1.21,1.39,enamel,"Manual vehicle latch release handle",zone)
        releases.append(dict(id=f"vehicle-release-{zone}",owner=owner,at=[CONTROL_X,1.30,(low+high)/2],
                             receiver=world(hit),component=obj.name))
    return bearings,releases


def leaf(steel):
    objects=moving_panel(steel)
    transform=(Matrix.Translation(point(CENTRE,0,DEPTH)) @ Matrix.Diagonal((WIDTH_SCALE,1,HEIGHT_SCALE,1))
               @ Matrix.Translation(-point(CENTRE,0,YARD_DEPTH)))
    for obj in objects:
        obj.name=obj.name.replace("Sally gate","Vehicle gate").removesuffix(".001")
        obj.matrix_world=transform @ obj.matrix_world
    bpy.context.view_layer.update()
    lo,hi=bounds(objects)
    assert abs(lo[0]-32.502)<.001 and abs(hi[0]-35.498)<.001
    return objects


def save_poses(objects):
    closed=bpy.data.collections.new("Vehicle gate closed pose")
    opened=bpy.data.collections.new("Vehicle gate open pose")
    for collection in (closed,opened):
        bpy.context.scene.collection.children.link(collection)
    for obj in objects:
        closed.objects.link(obj)
        for collection in list(obj.users_collection):
            if collection != closed:
                collection.objects.unlink(obj)
        parked=obj.copy()
        parked.name=obj.name+" parked east"
        parked.location.x+=TRAVEL
        opened.objects.link(parked)
    opened.hide_viewport=True
    opened.hide_render=True


def verify_bearings(assembly):
    supported=[]
    for x in (26.35,28,32.35,35.65,40,41.65):
        for z in (67.79,67.95):
            foot,_,floor=assembly.measured(point(x,.32,z),Vector((0,0,-1)),.7,"south facade pier footing")
            assert abs(foot.z)<.004
            supported.append(dict(component=floor.name,contact=world(foot)))
    return supported


def build(out):
    source=json.loads((BASE/"hiding.json").read_bytes())
    old={ext:BASE/f"prison-hiding.{ext}" for ext in ("blend","glb","hcs2")}
    assert all(sha(path)==source["exports"][ext] for ext,path in old.items())
    model,_=read_glb(old["glb"])
    removed_nodes={node["name"] for node in model["nodes"]
                   if node.get("name","").split(":",1)[-1] in REMOVED and "mesh" in node}
    assert len(removed_nodes)==4,removed_nodes

    bpy.ops.wm.open_mainfile(filepath=str(old["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    masonry=bpy.context.scene.objects["Flat route shell Wall sally S"].data.materials[0]
    eave=bpy.context.scene.objects["sally-port eave closure south"].data.materials[0]
    for name in sorted(REMOVED):
        bpy.data.objects.remove(bpy.context.scene.objects[name],do_unlink=True)
    bpy.context.view_layer.update()
    assembly=Assembly()
    supports=verify_bearings(assembly)
    wall=facade(masonry,eave)
    bpy.context.view_layer.update()
    assembly.originals.update(obj.name for obj in wall)
    steel=bpy.data.materials["Penitentiary worn galvanized gate steel"]
    enamel=bpy.data.materials["Penitentiary manual release ochre"]
    brackets,controls=mounts(assembly,steel,enamel)
    for obj in assembly.built:
        obj.name=obj.name.removeprefix("College ")
    parts=wall+assembly.built
    moved=leaf(steel)
    out.mkdir(parents=True,exist_ok=True)
    target={ext:out/f"prison-vehicle-sally.{ext}" for ext in old}
    additions=out/"_facade.glb"
    export(additions,parts)
    append(old["glb"],additions,target["glb"],remove_mesh_names=removed_nodes,reuse_embedded_images=True)
    additions.unlink()
    native_write(old["hcs2"],target["hcs2"],REMOVED,parts)
    before=native_records(old["hcs2"])
    expected=[row for row in before if row[0].split(":",1)[-1] not in REMOVED]
    new=native_records(target["hcs2"])
    assert len(before)-len(expected)==4 and new[:len(expected)]==expected
    leaf_file=out/"vehicle-moving-panel.glb"
    export(leaf_file,moved)
    save_poses(moved)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(target["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(target["glb"]))
    for part in new[len(expected):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]

    rows=(BASE/"arrival.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",target["glb"].relative_to(ROOT).as_posix(),sha(target["glb"]),
                              target["hcs2"].relative_to(ROOT).as_posix(),sha(target["hcs2"])))
    (out/"arrival.tsv").write_bytes(("\n".join(rows)+"\n").encode())
    for name in ("door-survey.tsv","gate.tsv","services.tsv"):
        copyfile(BASE/name,out/name)
    mission=(BASE/"mission.tsv").read_text(encoding="utf-8").splitlines()
    drop=[r for r in mission if r.startswith("TRANSITION\t457\tL065\t")]
    assert len(drop)==1
    (out/"mission.tsv").write_bytes(("\n".join(r for r in mission if r not in drop)+"\n").encode())
    metadata=(457,"PRISON-sally-vehicle-south","sally","entry",2,13,CENTRE,0,68,3.0,"z",67.76,68,
              OPENING[2],leaf_file.relative_to(ROOT).as_posix(),sha(leaf_file),TRAVEL)
    release_rows=[(c["id"],c["owner"],*c["at"],0,457) for c in controls]
    (out/"vehicle-gate.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in [metadata,*release_rows]).encode())
    proof=dict(replaced=sorted(removed_nodes),retained_components=len(expected),new_static_components=len(parts),
        facade_layers=["founded west pier","founded east pier","continuous lintel and head",
                       "eave closure founded on the retained ceiling cap"],
        clear_opening_m=[3.0,OPENING[2]],travel_m=TRAVEL,slab_bearings=supports,rail_bearings=brackets,
        releases=controls,asset=GATE.relative_to(ROOT).as_posix(),asset_sha256=sha(GATE),leaf_sha256=sha(leaf_file),
        exports={ext:sha(path) for ext,path in target.items()})
    (out/"construction.json").write_bytes((json.dumps(proof,indent=2)+"\n").encode())
    print("PASS",len(expected),"retained components, four replaced facade and eave components, twelve slab bearings and seven rail bearings")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
