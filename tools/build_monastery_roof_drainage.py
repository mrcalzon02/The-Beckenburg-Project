"""Flash the four opposing-eave valleys and carry runoff to founded outfalls."""
import json
import math
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
from build_prison_sally_gate import export
from build_roster_enclosed_roofs import mesh
from build_roster_foundations import material
from compose_room_terrain import split_native
from prepare_cemetery_doors import write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild"
PRIOR = BASE / "monastery-handrail-pass"
STAGE = BASE / "monastery-valley-pass"
OUT = STAGE / "wld-10"
STEM = "monastery-with-roof-valleys"
STATUS = "MONASTERY_FOUR_OPPOSING_EAVE_VALLEYS_DRAINED_STAGED"
HALF_SPAN = .62
THICKNESS = .028
EAVE = 3.24
INVERT = 3.282
FALL = .0046
PIPE = .105
# First roof is on the negative coordinate side of the seam. The outlets'
# exterior fastening points were chosen on solid facade faces below the ends.
VALLEYS = (
    ("gatehouse", "x", 56, 24, 40, "south-cloister roof south", "gatehouse roof north",
     (("west", 23.52,56.6,0,1), ("east",40.48,56.6,0,1))),
    ("dormitory", "z", 8, 22, 50, "dormitory roof east", "west-cloister roof west",
     (("north",7.48,21.57,-1,0), ("south",7.48,50.43,-1,0))),
    ("refectory", "z", 50, 30, 50, "east-cloister roof east", "refectory roof west",
     (None, ("south",50.52,50.43,1,0))),
    ("north range", "x", 16, 8, 44, ("archive roof south", "vestry roof south"), "north-cloister roof north",
     (("west",7.48,16.57,-1,0), ("east",44.48,15.48,0,-1))),
)


def coord(axis, t, y, side):
    return (t,y,side) if axis == "x" else (side,y,t)


def channel(name, sections, mat):
    """A thin closed profile with an open top and a tapered cross section."""
    vertices = []
    for section in sections:
        vertices.extend(section)
        vertices.extend((x,y-THICKNESS,z) for x,y,z in section)
    width = len(sections[0])
    edges = list(range(width)) + list(range(width*2-1,width-1,-1))
    faces = []
    # End caps are split into quads to avoid a concave cap fan.
    for end in (0, (len(sections)-1)*width*2):
        faces.extend((end+i,end+i+1,end+width+i+1,end+width+i) for i in range(width-1))
    for j in range(len(sections)-1):
        off = j*width*2
        next_off = off+width*2
        faces.extend((off+a,off+b,next_off+b,next_off+a)
                     for a,b in zip(edges,edges[1:]+edges[:1]))
    return mesh(name,vertices,faces,mat)


def pipe(name, x, z, low, high, mat):
    count = 14
    angles = [math.tau*i/count for i in range(count)]
    vertices = [(x+r*math.cos(a),y,z+r*math.sin(a)) for r in (PIPE,PIPE-.014) for y in (low,high) for a in angles]
    faces = []
    for a in range(count):
        b = (a+1)%count
        faces.extend(((a,b,count+b,count+a), (2*count+a,2*count+b,3*count+b,3*count+a),
                      (a,b,2*count+b,2*count+a), (count+a,count+b,3*count+b,3*count+a)))
    return mesh(name,vertices,faces,mat)


def survey(assembly, scene):
    roofs = {}
    for _,_,_,_,_,negative,positive,_ in VALLEYS:
        for name in (*(negative if isinstance(negative,tuple) else (negative,)),positive):
            roofs[name] = bvh([scene[name]])

    def pitch(name, xz):
        x,_,z = xz
        hit, _, _, _ = roofs[name].ray_cast(point(x,12,z),Vector((0,0,-1)),11)
        assert hit is not None, (name,x,z)
        return hit.z

    details = []
    for label,axis,seam,start,end,negative,positive,outlets in VALLEYS:
        stations = sorted({start,end,(start+end)/2,*([27.998,28.002] if label=="north range" else []),
                           *(start+(end-start)*i/16 for i in range(1,16))})
        profiles = []
        for t in stations:
            at = min(end-.002,max(start+.002,t))
            negative_name = negative[int(t>28)] if isinstance(negative,tuple) else negative
            invert = INVERT + (min(t-start,end-t) if all(outlets) else end-t)*FALL
            left = pitch(negative_name,coord(axis,at,0,seam-HALF_SPAN))
            right = pitch(positive,coord(axis,at,0,seam+HALF_SPAN))
            # Laps follow the actual slate. The channel is lower than both lips
            # and its outer edges lap into the original pitched slate.
            assert min(left,right)-invert > .055 and invert > EAVE
            profiles.append([coord(axis,t,left+.003,seam-HALF_SPAN),
                             coord(axis,t,invert,seam),
                             coord(axis,t,right+.003,seam+HALF_SPAN)])
        outfalls = []
        for index,outlet in enumerate(outlets):
            if outlet is None:
                continue
            side,x,z,nx,nz = outlet
            outward = point(nx,0,nz)
            receivers = []
            for y in (.65,1.55,2.60):
                p,_,receiver = assembly.measured(point(x,y,z),-outward,1.24,label+" "+side+" bracket")
                face = (p.x,p.z,-p.y)
                assert (x-face[0])*nx+(z-face[2])*nz > PIPE+.04, (label,side,face)
                receivers.append(dict(object=receiver.name, face=face, shoulder=(x,y,z)))
            # Fit the sump from the lowest of nine measured terrain and foundation contacts.
            # The stone apron is foundation-depth; its gravel pocket is open.
            footprint = [(x+nx*.29+dx,z+nz*.29+dz) for dx in (-.27,0,.27) for dz in (-.27,0,.27)]
            ground = []
            for ax,az in footprint:
                at,_,support = assembly.measured(point(ax,1.20,az),Vector((0,0,-1)),3,
                                                  label+" "+side+" sump bearing")
                assert support.name == "WLD-10 saved ground" or support.name.startswith("Bearing skirt "), (label,side,support.name)
                ground.append((ax,at.z,az,support.name))
            outfalls.append(dict(side=side,position=(x,z),normal=(nx,nz),mounts=receivers,
                                 bearing_samples=ground,entry_section=profiles[0 if index==0 else -1]))
        details.append(dict(id=label,axis=axis,seam=seam,span=(start,end),roof_lips=(negative,positive),
                            profiles=profiles,outfalls=outfalls))
    return details


def install(assembly, drains):
    zinc = material("Monastery aged folded lead flashing",(.31,.35,.36))
    iron = material("Monastery dark cast rainwater pipe",(.23,.22,.18))
    stone = material("Monastery dressed stone soakaway",(.34,.32,.27))
    gravel = material("Monastery coarse gravel soakaway bed",(.20,.22,.19))

    def built(obj, role):
        obj["role"] = role
        assembly.built.append(obj)

    for run in drains:
        label = "Monastery "+run["id"]
        built(channel(label+" lapped valley liner",run["profiles"],zinc),"supported lapped roof valley")
        for drain in run["outfalls"]:
            name = label+" "+drain["side"]
            x,z = drain["position"]
            nx,nz = drain["normal"]
            roof_edge = drain["entry_section"]
            center = roof_edge[1]
            # Align the mouth with the actual liner and taper it into the pipe.
            top = 3.165
            axis = run["axis"]
            first = [tuple(p) for p in roof_edge]
            small = [coord(axis,x if axis=="x" else z,top+h,(z if axis=="x" else x)+d)
                     for h,d in ((.075,-.075),(0,0),(.075,.075))]
            # An end departing backwards uses the opposite section winding,
            # while the profile itself keeps the ordering of the roof's laps.
            built(channel(name+" tapered outlet shoe",[first,small],zinc),"roof outlet shoe feeding open pipe")
            gy = min(p[1] for p in drain["bearing_samples"])
            rim = max(p[1] for p in drain["bearing_samples"])+.075
            lip = rim+.085
            built(pipe(name+" hollow downpipe",x,z,lip,top-.012,iron),"wall-mounted open rainwater bore")
            for index,m in enumerate(drain["mounts"],1):
                fx,fy,fz = m["face"]
                a = (fx-nx*.01,fy,fz-nz*.01)
                b = (x-nx*(PIPE-.014),fy,z-nz*(PIPE-.014))
                obj = assembly.tube(name+f" pipe wall strap {index}",a,b,.028,iron)
                obj["role"] = "masonry-mounted rainwater strap"

            cx,cz = x+nx*.29,z+nz*.29
            outer = (cx-.27,cz-.27,cx+.27,cz+.27)
            inner = (cx-.195,cz-.195,cx+.195,cz+.195)
            ax,az,bx,bz = outer
            ix,iz,jx,jz = inner
            def sill(suffix,bounds,start,end,mat):
                assembly.box(name+suffix,bounds,start,end,mat,"founded roof runoff soakaway","exterior")
            sill(" stone sump foot",outer,gy-.13,rim-.05,stone)
            for i,bounds in enumerate(((ax,az,ix,bz),(jx,az,bx,bz),(ix,az,jx,iz),(ix,jz,jx,bz)),1):
                sill(f" sump surround {i}",bounds,rim-.05,rim,stone)
            sill(" gravel sump bottom",inner,rim-.05,rim-.027,gravel)
            # The downpipe offset leaves its outlet just behind the soakaway.
            # A cast shoe carries the discharge outward inside the gravel bed.
            lower_sections = [[(x-nz*.08,lip+.055,z+nx*.08),(x,lip-.015,z),(x+nz*.08,lip+.055,z-nx*.08)],
                [(cx-nz*.073,rim+.044,cz+nx*.073),(cx,rim-.007,cz),(cx+nz*.073,rim+.044,cz-nx*.073)]]
            built(channel(name+" sump discharge shoe",lower_sections,iron),"open shoe emptying into founded soakaway")
            drain.update(rim_y=rim,liner_low_point=center,pipe_top=top-.012,pipe_bottom=lip,soakaway=(cx,rim,cz))

    for obj in assembly.built:
        obj.name = obj.name.removeprefix("College ")


def build():
    roster = json.loads((PRIOR / "roster.json").read_bytes())
    row = next(item for item in roster["active_sources"] if item["id"] == "WLD-10")
    prior = PRIOR / "wld-10"
    source = ROOT / row["construction"]
    authority = json.loads((PRIOR / "checkpoint.json").read_bytes())["project_bytes_sha256"]
    runtime = ("runtime-input.tsv","door-schedule.tsv","handrail-schedule.tsv")
    for path in (source,*(ROOT/row[key] for key in ("blend","glb","native","layout"))):
        # The construction receipt is listed in the new-stage record.
        assert sha(path) == authority[rel(path)], path
    for name in runtime:
        assert sha(prior/name) == authority[rel(prior/name)], name
    meta = json.loads(source.read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT / row["blend"]))
    bpy.context.preferences.filepaths.save_version = 0
    assembly = Assembly()
    drains = survey(assembly,bpy.data.objects)
    install(assembly,drains)

    OUT.mkdir(parents=True,exist_ok=True)
    blend,model,collision = (OUT/(STEM+ext) for ext in (".blend",".glb",".hcs2"))
    fragment = ROOT/"build/monastery-valley-addition.glb"
    export(fragment,assembly.built)
    append(ROOT/row["glb"],fragment,model,reuse_embedded_images=True)
    added = native_write(ROOT/row["native"],collision,set(),assembly.built)
    before,after = split_native(ROOT/row["native"]),split_native(collision)
    assert after[:len(before)] == before and len(after)-len(before) == len(added)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    models = import_scene(model)
    for name,pts in added:
        assert match_geometry([t for t,_ in pts],[t for t,_ in records(models[name])]),name

    for name in runtime:
        (OUT/name).write_bytes((prior/name).read_bytes())
    rows = []
    for run in drains:
        rows.extend(("PROFILE",run["id"],*left,*middle,*right) for left,middle,right in run["profiles"])
        for d in run["outfalls"]:
            rows.append(("OUTFALL",run["id"],d["side"],*d["position"],d["pipe_bottom"],d["pipe_top"],*d["soakaway"]))
    write_tsv(OUT/"valley-schedule.tsv",rows)
    meta.update(status=STATUS, before_construction=rel(source), before_construction_sha256=sha(source),
                before_layout=row["layout"], before_layout_sha256=sha(ROOT/row["layout"]),
                source_glb=row["glb"], source_glb_sha256=row["glb_sha256"],
                source_physics=row["native"], source_physics_sha256=row["native_sha256"],
                retained_native_records=len(before), removed_source_meshes=[],
                new_interface_parts=[name for name,_ in added], valleys=drains,
                glb_sha256=sha(model), physics_sha256=sha(collision), blend_sha256=sha(blend),
                builder=rel(Path(__file__)), builder_sha256=sha(Path(__file__)))
    construction = OUT/"construction.json"
    write(construction,meta)
    row.update(construction=rel(construction),glb=rel(model),native=rel(collision),blend=rel(blend),
               glb_sha256=sha(model),native_sha256=sha(collision))
    next(item for item in roster["sites"] if item["id"]=="WLD-10")["decision"] = STATUS
    roster["baseline"] = rel(PRIOR/"roster.json")
    write(STAGE/"roster.json",roster)
    print("PASS",len(drains),"lapped roof valleys,",sum(len(d["outfalls"]) for d in drains),
          "grounded outfalls,",len(added),"matched assembly components")


if __name__ == "__main__":
    build()
