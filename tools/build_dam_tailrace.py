"""Fit the isolated downstream stilling channel to the surveyed Blackwater bank."""
import argparse
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_circulation_construction import native_write
from build_college_grounds import import_scene, terrain_tree
from build_dam_east_hall import rel, render, sha
from build_dam_spillway_gallery import validate_solid
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from review_roster_macroterrain import records

BASE=ROOT/"development/levels/rebuild"
PRIOR=BASE/"dam-catwalk-ground-pass/roster.json"
STAGE=BASE/"dam-tailrace-pass"
OUT=STAGE/"wld-09"
GLB, NATIVE, BLEND=(OUT/("tailrace"+ext) for ext in (".glb",".hcs2",".blend"))
STATUS="DAM_FOUNDED_TAILRACE_AND_CONTAINED_WATER_STAGED"
X0,X1=72.72,75.06
WEST,EAST=73.00,74.78
NORTH,SOUTH=15.42,43.64
BED,WATER,WALL_TOP=-7.18,-6.82,-6.17
FOUNDATION=-8.24
VIEWS=(
    ("east-bank-stilling-channel",(102,-62,14),(71,-32,-3.0),53),
    ("stilling-channel-cast-invert",(77.2,-28,-5.38),(73.86,-22,-7.05),0),
)


def load_source():
    roster=json.loads(PRIOR.read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-09")
    assert sha(ROOT/row["glb"])==row["glb_sha256"]
    assert sha(ROOT/row["native"])==row["native_sha256"]
    return roster,row


def build_channel(terrain):
    concrete=material("Dam tailrace weathered channel concrete",(.39,.41,.38))
    lining=material("Dam tailrace submerged invert liner",(.25,.28,.24))
    steel=material("Dam tailrace closed sluice steel",(.14,.16,.15))
    built=[]

    def part(label,bounds,lo,hi,mat=concrete):
        obj=solid("Dam tailrace "+label,bounds,lo,hi,mat,"founded hydraulic structure","tailrace")
        built.append(obj)

    # The closed base spans the entire structure, including both walls and the
    # terminal gate chambers. It bears below the unmodified trough grade.
    part("continuous reinforced base",(X0,14.97,X1,44.56),FOUNDATION,BED,lining)
    part("west continuous retaining wall",(X0,14.97,WEST,44.56),BED,WALL_TOP)
    part("east continuous retaining wall",(EAST,14.97,X1,44.56),BED,WALL_TOP)
    # Cast terminal piers leave an actual center bay. Closed steel sluices
    # seat between their guides, retaining water at both ends of this reach.
    for label,z0,z1,gate_z in (("downstream",14.97,NORTH,15.23),("upstream",SOUTH,44.56,44.13)):
        a,b=73.27,74.51
        part(label+" west jamb",(WEST,z0,a,z1),BED,WALL_TOP)
        part(label+" east jamb",(b,z0,EAST,z1),BED,WALL_TOP)
        part(label+" lintel",(a,z0,b,z1),-6.29,WALL_TOP)
        part(label+" closed sluice leaf",(a,gate_z-.058,b,gate_z+.058),BED,-6.27,steel)
        for x in (a+.035,b-.035):
            part(label+f" sluice guide X{x:.3f}",(x-.024,gate_z-.12,x+.024,gate_z+.12),
                 BED,-6.27,steel)

    surface=terrain_tree(terrain)
    grade=[]
    for i in range(118):
        z=15.05+i*.25
        for x in (X0+.02,WEST+.12,73.89,EAST-.12,X1-.02):
            hit=surface.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),22)[0]
            assert hit is not None and FOUNDATION+.14<hit.z<BED-.035,(x,z,hit)
            grade.append(hit.z)
    return built,dict(samples=len(grade),lowest_soil_y=round(min(grade),4),
                      highest_soil_y=round(max(grade),4),basis="surveyed saved bank")


def water_document():
    # The surface meets the actual retaining faces and the two closed leaves.
    n,s=15.288,44.072
    document={"version":1,"level":"wld-09","count":1,
        "water.0.id":"dam-isolated-tailrace","water.0.shape":"SQUARE",
        "water.0.x":(WEST+EAST)/2,"water.0.y":WATER,"water.0.z":(n+s)/2,
        "water.0.width":round(EAST-WEST-.008,3),"water.0.depth":round(s-n,3),"water.0.yaw":0}
    for key,value in {"bed.depth":round(WATER-BED,3),"wave.height":.018,"wave.grid":.45,
                      "wave.speed":.38,"shore.height":.005,"foam.opacity":.09,
                      "contact.enabled":"false","spray.enabled":"false"}.items():
        document["water.0.setting."+key]=value
    (OUT/"dam-tailrace.water").write_bytes("".join(f"{k}={v}\n" for k,v in document.items()).encode())


def runtime_schedule(layout,meta):
    # The probe uses the surveyed rooms. The review owners are local to
    # this harness until the full mission and door schedules are installed.
    owners="control office west-stair turbine workshop east-stair gallery valve return spillway plant crest entry stage".split()
    alias={"west-lower-stair":"west-stair","east-lower-stair":"east-stair","east-service-catwalk":"crest"}
    rows=[]
    for zone in layout["zones"]:
        ident=zone["id"]
        owner=owners.index(alias.get(ident,ident))
        pos=zone.get("label_point")
        x0,z0,x1,z1=zone["bounds"]
        if pos is None:
            pos=((x0+x1)/2,zone["y"],(z0+z1)/2)
        outside=zone["kind"] in ("outside","staging")
        rows.append(("ROOM" if ident in owners else "SPACE",ident,owner,x0,z0,x1,z1,zone["y"],zone["height"],
                     str(outside).lower(),zone["name"],*pos))
    rows.append(("SPACE","dam-closed-channel",11,X0,14.97,X1,44.56,BED,2,
                 "true","Contained water",73.89,BED,31))
    rows.extend(("RITUAL",r["rite"].lower(),owners.index(r["zone"]),*r["point"]) for r in layout["rituals"])
    rows.extend(("STEP",tread) for tread in meta["stair_components"])
    (OUT/"runtime-zones.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode("utf8"))


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    roster,source=load_source()
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    objects=import_scene(ROOT/source["glb"])
    existing=objects["Dam saved east-bank ground"]
    built,grades=build_channel(existing)
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND),compress=True)
    addition=ROOT/"build/dam-tailrace-addition.glb"
    export(addition,built)
    append(ROOT/source["glb"],addition,GLB)
    native_write(ROOT/source["native"],NATIVE,[],built)
    water_document()
    runtime_schedule(layout,meta)
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
        source_physics=source["native"],source_physics_sha256=source["native_sha256"],
        before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
        before_segment_map=meta["source_segment_map"],before_stair_components=meta["stair_components"],
        removed_source_meshes=[],new_interface_parts=[p.name for p in built],
        tailrace=dict(invert_y=BED,water_y=WATER,freeboard=WALL_TOP-WATER,
                      outer_bounds=[X0,14.97,X1,44.56],soil=grades,
                      document=rel(OUT/"dam-tailrace.water"),water_sha256=sha(OUT/"dam-tailrace.water"),
                      operating_state="isolated by two closed sluices"),
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(GLB),physics_sha256=sha(NATIVE),blend_sha256=sha(BLEND))
    write(OUT/"construction.json",meta)
    source.update(construction=rel(OUT/"construction.json"),glb=rel(GLB),native=rel(NATIVE),
                  blend=rel(BLEND),glb_sha256=sha(GLB),native_sha256=sha(NATIVE))
    next(row for row in roster["sites"] if row["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR)
    write(STAGE/"roster.json",roster)


def review():
    import build_dam_east_hall
    build_dam_east_hall.CAPTURES=OUT/"captures/structure"
    _,source=load_source()
    meta=json.loads((OUT/"construction.json").read_bytes())
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    old,old_buffer=read(ROOT/source["glb"])
    current,new_buffer=read(GLB)
    assert new_buffer.startswith(old_buffer)
    for field in ("nodes","meshes","materials","accessors","bufferViews","textures","images"):
        assert current.get(field,[])[:len(old.get(field,[]))]==old.get(field,[]),field
    retained=split_native(ROOT/source["native"])
    assert split_native(NATIVE)[:len(retained)]==retained
    objects=import_scene(GLB)
    native=read_parts(NATIVE)
    for name in meta["new_interface_parts"]:
        obj=objects[name]
        validate_solid(obj)
        assert face_keys(records(obj))==face_keys(native[name][1]),name
    captures,van=render(layout,VIEWS,sha(GLB))
    write(OUT/"export-verification.json",dict(glb_sha256=sha(GLB),native_sha256=sha(NATIVE),
        retained_native_records=len(retained),new_closed_parts=len(meta["new_interface_parts"]),
        canonical_van_triangles=van,captures=captures))


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--review-only",action="store_true")
    args=parser.parse_args(sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else [])
    (review if args.review_only else build)()
