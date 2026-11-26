"""Fit runoff collectors above Monastery door approaches and lap the north range seam."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_monastery import rel
from build_monastery_roof_drainage import PIPE, channel, coord, pipe
from build_monastery_roof_flashings import height, scene_roof
from build_prison_sally_gate import export
from build_roster_enclosed_roofs import mesh
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-flashing-pass"
STAGE = BASE / "monastery-eave-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-eave-gutters"
STATUS = "MONASTERY_GATE_AND_HERB_COURT_EAVES_DRAINED_STAGED"
FALL = .0045
INVERT = 3.095
OUTER = .32
# Geometry follows the five actual eaves. Outlets are on surveyed solid piers
# and terminate beside the path users take through each individual doorway.
RUNS = (
    ("public gate","x",62,1,24,40,(24.55,39.45),"gatehouse roof south"),
    ("north herb court","x",22,1,14,44,(15.70,42.25),"north-cloister roof south"),
    ("south herb court","x",50,-1,14,44,(15.70,42.25),"south-cloister roof north"),
    ("west herb court","z",14,1,22,50,(24.55,47.45),"west-cloister roof east"),
    ("east herb court","z",44,-1,22,50,(24.55,47.45),"east-cloister roof west"),
)


def survey(assembly):
    runs = []
    for label,axis,eave,side,lo,hi,outlet_stations,roof in RUNS:
        slate = scene_roof([roof])
        normal = (0,side) if axis=="x" else (side,0)
        nx,nz = normal
        direction = point(-nx,0,-nz)
        left,right = outlet_stations
        crown = (left+right)/2
        stations = sorted({lo+.035,hi-.035,crown,
                           *(lo+(hi-lo)*i/20 for i in range(1,20)),
                           *(t+d for t in outlet_stations for d in (-.085,0,.085))})

        def profile(t):
            distance = min(abs(t-left),abs(t-right))
            invert = INVERT+distance*FALL
            back = eave-side*.12
            roof_y = height(slate,axis,t,back)
            assert roof_y > invert+.075, (label,t,roof_y,invert)
            outer_y = min(3.237,invert+distance*.84) if distance<.17 else 3.237
            return [coord(axis,t,roof_y+.007,back),
                    coord(axis,t,3.202,eave+side*.022),
                    coord(axis,t,invert,eave+side*.185),
                    coord(axis,t,outer_y,eave+side*OUTER)]

        profiles = [profile(t) for t in stations]
        supports = []
        for i in range(13):
            t = lo+.28+(hi-lo-.56)*i/12
            origin = coord(axis,t,2.984,eave+side*.39)
            p,_,face = assembly.measured(point(*origin),direction,.69,label+" fascia")
            assert face.name.startswith(("Monastery gatehouse public elevation", "Monastery north courtyard elevation", "Monastery south courtyard elevation", "Wall west E", "Wall east W")) or "eave closure" in face.name, (label,t,face.name)
            foot = (p.x-nx*.008,p.z,-p.y-nz*.008)
            gutter = coord(axis,t,min(v[1] for v in profile(t))-.015,eave+side*.182)
            supports.append(dict(receiver=face.name,foot=foot,seat=gutter))
        outlets = []
        for station in outlet_stations:
            x,_,z = coord(axis,station,0,eave+side*.52)
            mounts = []
            for y in (.72,1.58,2.61):
                p,_,receiver = assembly.measured(point(x,y,z),direction,1.15,label+" downpipe strap")
                mounts.append(dict(receiver=receiver.name,face=(p.x,p.z,-p.y)))
            cx,cz = x+nx*.30,z+nz*.30
            bearings = []
            for dx in (-.24,0,.24):
                for dz in (-.24,0,.24):
                    p,_,receiver = assembly.measured(point(cx+dx,1.1,cz+dz),Vector((0,0,-1)),3,label+" outfall")
                    assert receiver.name=="WLD-10 saved ground",(label,station,receiver.name)
                    bearings.append((p.x,p.z,-p.y))
            outlets.append(dict(station=station,position=(x,z),center=(cx,cz),normal=normal,
                                mounts=mounts,bearings=bearings))
        runs.append(dict(id=label,axis=axis,edge=eave,side=side,profiles=profiles,supports=supports,
                         outlets=outlets,roof=roof))
    return runs


def install(assembly,runs):
    lead = material("Monastery blue-grey rolled lead gutters",(.28,.32,.34))
    iron = material("Monastery cast iron eave brackets and pipes",(.24,.22,.18))
    stone = material("Monastery eave drain stone rims",(.37,.35,.30))
    gravel = material("Monastery eave drain gravel",(.22,.23,.20))

    def add(obj,role):
        obj["role"]=role
        assembly.built.append(obj)

    def tube(name,a,b,r,role):
        assembly.tube(name,a,b,r,iron)["role"]=role

    for run in runs:
        label = "Monastery "+run["id"]
        axis,eave,side = (run[key] for key in ("axis","edge","side"))
        add(channel(label+" crown-fed eave gutter",run["profiles"],lead),"lapped graded rainwater gutter")
        for i,mount in enumerate(run["supports"],1):
            tube(label+f" fascia saddle {i:02}",mount["foot"],mount["seat"],.021,"founded fascia gutter bracket")
        for i,end in enumerate((run["profiles"][0],run["profiles"][-1]),1):
            inner,base,rim = end[1:]
            t = base[0] if axis=="x" else base[2]
            delta = -.025 if i==1 else .025
            # Close the exposed end without raising a lip at either outlet.
            cap = [coord(axis,t,h,v) for h,v in
                   ((3.233,eave+side*.013),(3.234,eave+side*.177),(3.235,eave+side*.32))]
            shift = [(x+(delta if axis=="x" else 0),y,z+(delta if axis=="z" else 0)) for x,y,z in cap]
            add(channel(label+f" gutter terminal lip {i}",[cap,shift],lead),"eave gutter end cover")
            # Seat the end plate between the graded floor and its small cover.
            wall = [(x,y-.013,z) for x,y,z in (inner,base,rim)]+cap
            thickness = [(x+(delta if axis=="x" else 0),y,z+(delta if axis=="z" else 0)) for x,y,z in wall]
            faces=[(0,1,4,3),(1,2,5,4),(6,7,10,9),(7,8,11,10)]
            ring=(0,1,2,5,4,3)
            faces.extend((a,b,b+6,a+6) for a,b in zip(ring,ring[1:]+ring[:1]))
            add(mesh(label+f" gutter terminal plate {i}",wall+thickness,faces,lead),"closed physical gutter end")
        for i,outlet in enumerate(run["outlets"],1):
            name = label+f" outlet {i}"
            t = outlet["station"]
            x,z = outlet["position"]
            cx,cz = outlet["center"]
            nx,nz = outlet["normal"]
            mouth_side = eave+side*.52
            mouth = 3.019
            throat = [coord(axis,t+d,INVERT+h,eave+side*OUTER) for d,h in ((-.075,.069),(0,-.003),(.075,.069))]
            lower = [coord(axis,t+d,mouth+h,mouth_side) for d,h in ((-.072,.062),(0,0),(.072,.062))]
            add(channel(name+" folded inlet shoe",[throat,lower],lead),"open tapered gutter outlet")
            base = min(p[1] for p in outlet["bearings"])
            rim = max(p[1] for p in outlet["bearings"])+.073
            lip = rim+.095
            add(pipe(name+" hollow pipe",x,z,lip,mouth-.039,iron),"open pipe on masonry straps")
            for j,mount in enumerate(outlet["mounts"],1):
                mx,y,mz = mount["face"]
                tube(name+f" pipe strap {j}",(mx-.01*nx,y,mz-.01*nz),
                     (x-(PIPE-.014)*nx,y,z-(PIPE-.014)*nz),.024,"stone-mounted pipe strap")
            box = (cx-.24,cz-.24,cx+.24,cz+.24)
            a,b,c,d = box
            left,right,north,south = cx-.174,cx+.174,cz-.174,cz+.174
            def build_box(part,bounds,low,high,mat):
                assembly.box(name+part,bounds,low,high,mat,"founded open eave soakaway","exterior")
            build_box(" footing",box,base-.13,rim-.05,stone)
            for j,bounds in enumerate(((a,b,left,d),(right,b,c,d),(left,b,right,north),(left,south,right,d)),1):
                build_box(f" rim {j}",bounds,rim-.05,rim,stone)
            build_box(" gravel bed",(left,north,right,south),rim-.05,rim-.026,gravel)
            shoe = [[(u-dz*nz,y,v+dz*nx) for dz,y in ((-.08,level+.055),(0,level),(.08,level+.055))]
                    for u,level,v in ((x,lip-.014,z),(cx,rim-.007,cz))]
            add(channel(name+" low drainage shoe",shoe,iron),"descending drain shoe into open gravel bed")
            outlet.update(pipe_top=mouth-.039,pipe_bottom=lip,bed=(cx,rim,cz),
                          inlet=(throat[1],lower[1]))
    for obj in assembly.built:
        obj.name = obj.name.removeprefix("College ")


def range_lap(assembly):
    higher = scene_roof(["archive roof north","archive roof south"])
    lower = scene_roof(["vestry roof north","vestry roof south"])
    profiles = []
    for i in range(29):
        z = 2.015+i*(16-2-.03)/28
        if i==14:
            z=9
        a,b = height(higher,"z",z,27.8),height(lower,"z",z,28.22)
        profiles.append([(27.74,a+.005,z),(27.985,a+.006,z),
                         (28.026,b+.011,z),(28.27,b+.004,z)])
    gray = material("Monastery archive–vestry ridge step lead",(.29,.32,.34))
    obj = channel("Monastery archive vestry shared gable step lap",profiles,gray)
    obj["role"]="lapped roof step on both slate leaves"
    assembly.built.append(obj)
    return profiles


def build():
    roster = json.loads((PRIOR/"roster.json").read_bytes())
    row = next(site for site in roster["active_sources"] if site["id"]=="WLD-10")
    source = ROOT/row["construction"]
    prior = PRIOR/"wld-10"
    runtime = ("runtime-input.tsv","door-schedule.tsv","handrail-schedule.tsv","valley-schedule.tsv","flashing-schedule.tsv")
    authority = json.loads((PRIOR/"checkpoint.json").read_bytes())["project_bytes_sha256"]
    for p in (source,*(ROOT/row[k] for k in ("glb","native","blend","layout")),*(prior/n for n in runtime)):
        assert sha(p)==authority[rel(p)],p
    meta = json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    runs = survey(assembly)
    install(assembly,runs)
    lap = range_lap(assembly)

    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision = (OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment = ROOT/"build/monastery-eave-addition.glb"
    export(fragment,assembly.built)
    append(ROOT/row["glb"],fragment,model,reuse_embedded_images=True)
    added = native_write(ROOT/row["native"],collision,set(),assembly.built)
    before,after = split_native(ROOT/row["native"]),split_native(collision)
    assert after[:len(before)]==before and len(after)-len(before)==len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    objects = import_scene(model)
    for name,triangles in added:
        assert match_geometry([t for t,_ in triangles],[t for t,_ in records(objects[name])]),name

    for name in runtime:
        (OUT/name).write_bytes((prior/name).read_bytes())
    schedule = []
    for run in runs:
        schedule.extend(("GUTTER",run["id"],run["axis"],run["side"],*a,*b,*c,*d) for a,b,c,d in run["profiles"])
        schedule.extend(("OUTLET",run["id"],i,*o["position"],o["pipe_bottom"],o["pipe_top"],*o["bed"],
                         *o["inlet"][0],*o["inlet"][1]) for i,o in enumerate(run["outlets"],1))
    schedule.extend(("LAP",*a,*b,*c,*d) for a,b,c,d in lap)
    write_tsv(OUT/"eave-schedule.tsv",schedule)
    meta.update(status=STATUS,before_construction=rel(source),before_construction_sha256=sha(source),
                before_layout=row["layout"],before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"],source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"],source_physics_sha256=row["native_sha256"],
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(model),physics_sha256=sha(collision),blend_sha256=sha(blend),
                retained_native_records=len(before),removed_source_meshes=[],eave_gutters=runs,north_range_lap=lap,
                new_interface_parts=[name for name,_ in added])
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),glb=rel(model),native=rel(collision),blend=rel(blend),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(site for site in roster["sites"] if site["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(runs),"founded eave runs,",sum(len(r["outlets"]) for r in runs),"outlets, and one lapped north-range seam")


if __name__=="__main__":
    build()
