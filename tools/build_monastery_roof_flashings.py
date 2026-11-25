"""Fit the Monastery's tower and cloister gable junction flashings."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import point
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_monastery import rel
from build_monastery_roof_drainage import channel, coord
from build_prison_sally_gate import export
from build_roster_enclosed_roofs import mesh
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-valley-pass"
STAGE = BASE / "monastery-flashing-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-roof-flashings"
STATUS = "MONASTERY_SEVEN_FITTED_ROOF_JUNCTIONS_STAGED"
# side is the direction from the receiving wall towards the lower roof.
TOWER = (
    ("tower west eave","z",50,22,30,-1,("east-cloister roof east",),
     ("Monastery western tower elevation",),(23,28)),
    ("tower west rake","z",50,16,22,-1,("north-cloister roof north","north-cloister roof south"),
     ("Monastery western tower elevation",),(19,)),
    ("tower south rake","x",30,50,60,1,("refectory roof west","refectory roof east"),
     ("Wall bell S","Wall tower-base S"),(56.85,59)),
)


def scene_roof(names):
    return bvh([bpy.data.objects[name] for name in names])


def height(tree, axis, t, side):
    origin = point(*coord(axis,t,9,side))
    at, _, _, _ = tree.ray_cast(origin, Vector((0,0,-1)),8)
    assert at is not None, tuple(origin)
    return at.z


def samples(lo,hi,knots):
    return sorted({lo,hi,*knots,*(lo+(hi-lo)*i/16 for i in range(1,16))})


def folded(name, sections, push, mat):
    """Solid apron and steep upstand with continuous sealed seams."""
    vertices = []
    for section in sections:
        vertices.extend(section)
        vertices.extend((x+push[0],y-.015,z+push[1]) for x,y,z in section)
    faces = []
    for cap in (0,(len(sections)-1)*6):
        faces.extend((cap+i,cap+i+1,cap+i+4,cap+i+3) for i in range(2))
    ring = (0,1,2,5,4,3)
    for i in range(len(sections)-1):
        base = i*6
        faces.extend((base+a,base+b,base+b+6,base+a+6)
                     for a,b in zip(ring,ring[1:]+ring[:1]))
    return mesh(name,vertices,faces,mat)


def tower_profiles():
    joints = []
    for name,axis,seam,lo,hi,side,slates,walls,knots in TOWER:
        lower = scene_roof(slates)
        receiving = [obj for obj in bpy.context.scene.objects if obj.type=="MESH" and
                     any(obj.name.startswith(wall) for wall in walls)]
        tree = bvh(receiving)
        profiles, contacts = [], []
        for t in samples(lo,hi,knots):
            at = min(hi-.009,max(lo+.009,t))
            wall_y = height(lower,axis,at,seam+side*.22)
            wall,normal,index,_ = tree.ray_cast(point(*coord(axis,at,wall_y+.19,seam+side*.45)),
                                                point(*coord(axis,0,0,-side)),.7)
            assert wall is not None and normal.dot(point(*coord(axis,0,0,side))) > .99,(name,t,wall)
            plumb = wall.x if axis=="z" else -wall.y
            rim = plumb+side*(.54 if name=="tower west eave" else .42)
            knee = plumb+side*.047
            top = plumb-side*.018
            foot_y = height(lower,axis,at,knee)
            rim_y = height(lower,axis,at,rim)
            if name=="tower west eave":
                # The lower roof pitches into this wall. The apron forms the
                # gutter floor, falling into the fitted refectory valley at Z 30.
                foot_y = 3.391+(30-t)*.005
                assert rim_y > foot_y+.02
            profiles.append([coord(axis,t,rim_y+.005,rim),coord(axis,t,foot_y+.014,knee),
                             coord(axis,t,foot_y+.20,top)])
            contacts.append((t,plumb,rim_y))
        joints.append(dict(id=name,kind="tower",axis=axis,seam=seam,side=side,
                           profiles=profiles,contacts=contacts,wall_names=[o.name for o in receiving]))
    return joints


def cloister_profiles():
    joints = []
    for cardinal,z,side in (("north",22,-1),("south",50,1)):
        opposite = "south" if cardinal=="north" else "north"
        lower = scene_roof([f"{cardinal}-cloister roof {opposite}"])
        for wing,lo in (("west",8),("east",44)):
            hi=lo+6
            upper = scene_roof([f"{wing}-cloister roof east",f"{wing}-cloister roof west"])
            gable = scene_roof([f"{wing}-cloister gable {cardinal}"])
            profiles, contacts = [], []
            for t in samples(lo+.035,hi-.035,(lo+3,lo+.6,hi-.6)):
                rim_y = height(lower,"x",t,z+side*.34)
                foot_y = height(lower,"x",t,z+side*.035)
                cap_y = height(upper,"x",t,z-side*.082)
                reveal = cap_y-foot_y
                if reveal > .29:
                    receiver,normal,_,_ = gable.ray_cast(point(t,foot_y+.063,z+side*.23),point(0,0,-side),.44)
                    assert receiver is not None and normal.dot(point(0,0,side))>.98,(cardinal,wing,t)
                    back,top = z-side*.015,foot_y+.125
                else:
                    # With no deep gable shoulder at the eave, bridge over the
                    # edge of the higher slate rather than leaving a floating fold.
                    back,top = z-side*.082,cap_y+.007
                profiles.append([(t,rim_y+.005,z+side*.34),(t,foot_y+.012,z+side*.035),(t,top,back)])
                contacts.append((t,z,rim_y,cap_y))
            joints.append(dict(id=f"{cardinal} {wing} cloister gable",kind="gable",axis="x",seam=z,side=side,
                               profiles=profiles,contacts=contacts,wall_names=[f"{wing}-cloister gable {cardinal}"]))
    return joints


def build():
    roster = json.loads((PRIOR/"roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"]=="WLD-10")
    source = ROOT/row["construction"]
    prior = PRIOR/"wld-10"
    runtime = ("runtime-input.tsv","door-schedule.tsv","handrail-schedule.tsv","valley-schedule.tsv")
    authority = json.loads((PRIOR/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    for p in (source,*(ROOT/row[k] for k in ("glb","native","blend","layout")),*(prior/n for n in runtime)):
        assert sha(p)==authority[rel(p)],p
    meta = json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    joints = tower_profiles()+cloister_profiles()
    flashing = material("Monastery weathered folded lead roof flashing",(.29,.315,.32))
    built = []
    for joint in joints:
        side = joint["side"]
        push = (-side*.006,0) if joint["axis"]=="z" else (0,-side*.006)
        part = folded("Monastery "+joint["id"]+" flashing",joint["profiles"],push,flashing)
        part["role"]="fitted roof abutment flashing"
        built.append(part)
    # The end of the tower west apron empties directly down into the installed
    # refectory collector. The bridge keeps the east-cloister flow over the lip.
    terminal = joints[0]["profiles"][-1]
    left,middle,_ = terminal
    downstream = [[(left[0]+.05,left[1]-.016,30.22),(49.995,3.377,30.22),(50.12,3.394,30.22)]]
    assert middle[1]>downstream[0][1][1]>3.374
    transition = channel("Monastery tower gutter to refectory valley shoe",
                         [[left,middle,(50.12,3.428,30)],*downstream],flashing)
    transition["role"]="roof gutter connection to surveyed valley"
    built.append(transition)

    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision = (OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment = ROOT/"build/monastery-flashing-addition.glb"
    export(fragment,built)
    append(ROOT/row["glb"],fragment,model,reuse_embedded_images=True)
    additions = native_write(ROOT/row["native"],collision,set(),built)
    before,after = split_native(ROOT/row["native"]),split_native(collision)
    assert after[:len(before)]==before and len(after)-len(before)==len(additions)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    objects = import_scene(model)
    for name,triangles in additions:
        assert match_geometry([t for t,_ in triangles],[t for t,_ in records(objects[name])]),name

    for name in runtime:
        (OUT/name).write_bytes((prior/name).read_bytes())
    write_tsv(OUT/"flashing-schedule.tsv",[
        (joint["id"],joint["kind"],joint["axis"],joint["side"],*a,*b,*c)
        for joint in joints for a,b,c in joint["profiles"]])
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                retained_native_records=len(before),removed_source_meshes=[],roof_flashings=joints,
                new_interface_parts=[name for name,_ in additions],
                tower_to_refectory_connection_end=downstream[0][1])
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),glb=rel(model),native=rel(collision),blend=rel(blend),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(joints),"fitted junctions,",sum(len(x["profiles"]) for x in joints),"measured profiles")


if __name__=="__main__":
    build()
