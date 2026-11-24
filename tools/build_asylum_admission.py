"""Bind Asylum admission and patient-court frames to exact retained assets."""
from pathlib import Path
import hashlib
import json
import sys

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"development/levels/rebuild"
OUT=BASE/"asylum-admission-pass"
DOORS=(
    (470,"ASYLUM-z-58-30","admission","entry",2,1,1),
    (471,"ASYLUM-x-35-52","admission","interview",1,1,1),
    (472,"ASYLUM-z-54-39.5","interview","records",1,1,1),
    (473,"ASYLUM-z-50-27.5","admission","spine",2,1,-1),
)

COURT_DOORS=(
    (474,"ASYLUM-court-day-N","day","court",2,1,-1),
    (475,"ASYLUM-court-ground-south-a-S","ground-south-a","court",1,1,1),
)
ANCHORS=(
    ("spine",27.5,0,31), ("admission",30.5,0,54), ("day",18,0,40.85),
    ("dining",35,0,45), ("ground-ward-link",18,0,20.5), ("treatment",40,0,35),
    ("hydro-change",47,0,32.5), ("upper-spine",27.5,3.4,31), ("records",41,0,56),
    ("quiet-lobby",49.5,0,14), ("chapel",16,0,6), ("plant",40,-3,30),
    ("court",18,0,34.5), ("entry",34,0,64),
)
# These retained openings and landings follow the same physical links used by the mover.
TRANSITIONS=(
    ("L09",476,"x",None), ("L25",477,"x",None), ("L33",478,"x",None),
    ("L40",479,"x",None), ("L44",480,"x",None), ("L55",481,"z",None),
    ("L71",482,"z",None), ("L73",483,"z",(18,0,58)),
    ("L77",484,"z",(32,3.4,27.44)), ("L78",485,"z",(57,0,18.8)),
)
LOWER_LANDINGS={"L77":(32,0,21.1),"L78":(57,-3,13.1)}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def table(path,rows):
    path.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())


def build(out=OUT,include_court=False):
    roster=json.loads((BASE/"prison-vehicle-sally-pass/roster.json").read_bytes())
    source=next(row for row in roster["active_sources"] if row["id"]=="ASYLUM")
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    construction=json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["layout"])==construction["layout_sha256"]
    zones={row["id"]:row for row in layout["zones"]}
    staging=layout["staging"]
    rows=[("SOURCE",source["glb"],source["glb_sha256"],source["native"],source["native_sha256"]),
          ("DOCK",*staging["point"],staging["yaw"])]
    for zone in layout["zones"]:
        stage=zone["id"]==staging["zone"]
        rows.append(("SPACE",zone["id"],13 if stage else zone["owner"],*zone["bounds"],zone["y"],zone["height"],
                     str(stage or zone["kind"]=="outside").lower(),str(zone["gameplay_eligible"]).lower()))
    rows.extend(("RITUAL",rite["rite"].lower(),zones[rite["zone"]]["owner"],*rite["point"]) for rite in layout["rituals"])
    rows.extend(("TREAD",name) for name in construction.get("stair_components",[]))
    links={link["id"]:link for link in layout["links"]}
    for key,link in links.items():
        if include_court:
            if link["player_access"]!="OPEN_ROUTE" or not all(zones[link[end]]["gameplay_eligible"] for end in ("a","b")):
                continue
        elif key not in ("L19","L20","L22","L23","L24"):
            continue
        points=link["points"]
        if include_court and key in LOWER_LANDINGS:
            points=[LOWER_LANDINGS[key],*points[1:]]
        rows.extend(("POINT",key,*point) for point in points)
    out.mkdir(exist_ok=True)
    table(out/"arrival.tsv",rows)
    frames={row["id"]:row for row in layout["authored_doorways"]}
    asset=sha(ROOT/"assets/runtime/models/door_1_loose.glb")
    doors=[]
    for id,frame,a,b,count,hinge,swing in (*DOORS,*COURT_DOORS) if include_court else DOORS:
        d=frames[frame]
        x,z=(d["along"],d["boundary"]) if d["axis"]=="z" else (d["boundary"],d["along"])
        doors.append((id,frame,a,b,zones[a]["owner"],zones[b]["owner"],x,d["sill_y"],z,d["clear_width"],
                      d["axis"],*d["frame_span"],d["clear_height"],count,hinge,swing,asset))
    table(out/"door-survey.tsv",doors)
    if include_court:
        mission=[("ANCHOR",owner,*room) for owner,room in enumerate(ANCHORS)]
        for key,portal,axis,corrected in TRANSITIONS:
            link=links[key]
            owners=[zones[link[end]]["owner"] for end in ("a","b")]
            at=corrected if corrected is not None else link["threshold"]
            mission.append(("TRANSITION",portal,key,*owners,*at,link["width"],axis,
                            "stair" if link["kind"]=="stairs" else "trail"))
        table(out/"mission.tsv",mission)


if __name__=="__main__":
    court="--court" in sys.argv[1:]
    build(BASE/"asylum-patient-court-pass" if court else OUT,court)
