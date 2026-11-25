"""Bind operating leaves to existing Monastery frames and keep its cloister arcade open."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from build_college_electrical import bounds, point
from build_deliberate_entrances import write
from prepare_cemetery_doors import SOURCE_LEAF, write_tsv
from review_roster_macroterrain import sha
from verify_college_services import bvh

BASE=ROOT/"development/levels/rebuild"
PARENT=BASE/"dam-named-pass"
ORIGINAL=BASE/"monastery-cloister-pass"
STAGE=BASE/"monastery-door-pass"
PACKET=STAGE/"wld-10"
STATUS="MONASTERY_FITTED_DOORS_AND_OPEN_CLOISTER_STAGED"
OWNERS={"gatehouse":0,"west":1,"south":1,"east":2,"north":2,"archive":3,"refectory":4,
        "dorm":5,"tower-base":6,"bell":7,"catacombs":8,"chamber":9,"vestry":10,
        "rear":11,"garden":12,"stage":13,"entry":13}
ANCHORS={"gatehouse":(32,0,59),"west":(11,0,36),"east":(47,0,36),"archive":(18,0,9),
         "refectory":(59,0,40),"dorm":(0,0,36),"tower-base":(52.97,0,24.5),"bell":(58.86,6,17),
         "catacombs":(23,-3,17.4),"chamber":(18,-3,9),"vestry":(36,0,9),"rear":(64,0,23),
         "garden":(29,0,36),"stage":(35,0,79)}
SWINGS={
    "public gate":(2,1,-1), "inner gatehouse door":(2,1,1),
    "south garden door":(2,1,1), "north garden door":(2,1,1),
    "archive cloister door":(2,1,-1), "vestry cloister door":(2,1,-1),
    "archive vestry door":(2,1,1), "dormitory cloister door":(2,1,-1),
    "tower cloister door":(2,1,-1), "tower service door":(2,1,1),
    "dining hall cloister door":(2,1,1), "refectory receiving door":(2,1,1),
    "memorial chamber door":(2,1,-1),
}


def schedule(model, layout):
    tree=bvh([o for o in bpy.context.scene.objects if o.type=="MESH"])
    doors=[]
    open_arcs=[]
    measured=[]
    digest=sha(SOURCE_LEAF)
    for ident,frame in enumerate(layout["authored_doorways"],930):
        label=frame["id"]
        opening=frame["axis"]
        center,y,width=(frame[k] for k in ("along","sill_y","clear_width"))
        a,b=frame["rooms"]
        casing=model["Monastery "+label+" fitted frame"]
        low,high=bounds([casing])
        fixed=frame["boundary"]
        index=2 if opening=="z" else 0
        plane=(low[index],high[index])
        assert max(fixed-plane[0],plane[1]-fixed)<.27 and plane[0]<fixed<plane[1],(label,plane)
        # The exact original opening still spans the usable section of the
        # fitted frame. Survey both receiving landings at natural standing points.
        pos=lambda u,v,h: point(u,h,v) if opening=="z" else point(v,h,u)
        direction=point(0,0,1) if opening=="z" else point(1,0,0)
        for f in (-.34,0,.34):
            for h in (.32,1.22,frame["clear_height"]-.10):
                assert tree.ray_cast(pos(center+f*width,fixed-.42,y+h),direction,.84)[0] is None,label
        floor=[]
        for u in (-.26,0,.26):
            for v in (-.68,0,.68):
                hit,n,_,_=tree.ray_cast(pos(center+u,fixed+v,y+.35),Vector((0,0,-1)),1)
                assert hit is not None and abs(hit.z-y)<.035 and n.z>.95,(label,u,v,hit)
                floor.append(round(hit.z,5))
        measured.append(dict(id=label,plane=plane,receiving_floor_samples=floor,
                             width=width,height=frame["clear_height"]))
        if label not in SWINGS:
            assert "cloister opening" in label,label
            if OWNERS[a]!=OWNERS[b]:
                x,z=(center,fixed) if opening=="z" else (fixed,center)
                open_arcs.append(("OPEN",ident,OWNERS[a],OWNERS[b],x,y,z,width,opening,"trail"))
            continue
        leaves,hinge,swing=SWINGS[label]
        mount=plane[int(swing>0)]+swing*.004
        x,z=(center,mount) if opening=="z" else (mount,center)
        doors.append((ident,label,a,b,OWNERS[a],OWNERS[b],x,y,z,width,opening,mount-.026,mount+.026,
                      frame["clear_height"],leaves,hinge,swing,digest))
    assert len(doors)==13 and len(measured)==17
    return doors,open_arcs,measured


def build():
    roster=json.loads((PARENT/"roster.json").read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-10")
    source=ROOT/row["construction"]
    layout_path=ROOT/row["layout"]
    meta=json.loads(source.read_bytes())
    layout=json.loads(layout_path.read_bytes())
    for key,digest in (("glb","glb_sha256"),("native","physics_sha256"),
                       ("blend","blend_sha256"),("layout","layout_sha256")):
        assert sha(ROOT/row[key])==meta[digest],key
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/row["blend"]))
    doors,open_arcs,measured=schedule(bpy.data.objects,layout)
    runtime=[]
    for zone in layout["zones"]:
        key=zone["id"]
        x0,z0,x1,z1=zone["bounds"]
        datum=zone["y"]
        p=ANCHORS.get(key,((x0+x1)/2,datum,(z0+z1)/2))
        outside=zone["kind"] in ("outside","staging")
        runtime.append(("ROOM" if key in ANCHORS else "SPACE",key,OWNERS[key],x0,z0,x1,z1,
                        datum,zone["height"],str(outside).lower(),zone["name"],*p))
    for name in meta["stair_components"]:
        runtime.append(("STEP",name))
    for ritual in layout["rituals"]:
        runtime.append(("RITUAL",ritual["rite"].lower(),OWNERS[ritual["zone"]],*ritual["point"]))
    runtime.extend(open_arcs)
    runtime.extend((
        ("OPEN",950,13,14,32,0,76.372,2.15,"z","trail"),
        ("OPEN",951,6,7,57.04,6,17.62,1.8,"z","stair"),
        ("OPEN",952,2,8,29,-1.6,19,1.8,"x","stair"),
        ("OPEN",953,2,8,15,-1.3,19,1.8,"x","stair"),
    ))
    # The authored points at the exact first and last tread edges can select
    # either neighboring support. Place the guide knots inside their landings.
    landing_guide={(31.7,0,19):(31.91,0,19),(26.3,-3,19):(26.08,-3,19),
                (18,-3,19):(18.21,-3,19),(12.6,0,19):(12.39,0,19),
                (55,0,17.8):(55,0,17.59),(55,3,23.07):(55,3,23.28),
                (57.04,3,23.07):(57.04,3,23.28),(57.04,6,17.8):(57.04,6,17.59)}
    for link in layout["links"]:
        points=[landing_guide.get(tuple(p),p) for p in link["points"]] if link["id"] in ("L16","L19","L21") else link["points"]
        runtime.extend(("POINT",link["id"],*p) for p in points)
    PACKET.mkdir(parents=True,exist_ok=True)
    write_tsv(PACKET/"door-schedule.tsv",doors)
    write_tsv(PACKET/"runtime-input.tsv",runtime)
    next(r for r in roster["sites"] if r["id"]=="WLD-10")["decision"]=STATUS
    roster["baseline"]=(PARENT/"roster.json").relative_to(ROOT).as_posix()
    write(STAGE/"roster.json",roster)
    write(PACKET/"door-installation.json",dict(id="WLD-10",status=STATUS,
        model=row["glb"],native=row["native"],layout=row["layout"],source=row["construction"],
        glb_sha256=row["glb_sha256"],native_sha256=row["native_sha256"],layout_sha256=meta["layout_sha256"],
        leaf=SOURCE_LEAF.relative_to(ROOT).as_posix(),leaf_sha256=sha(SOURCE_LEAF),
        architecture_retained=True,open_arcades=4,doors=len(doors),frames=measured))
    print("PASS",len(doors),"fitted doors on",sum(len(m["receiving_floor_samples"]) for m in measured),
          "surveyed floor positions; four cloister arcades kept open")


if __name__=="__main__":
    build()
