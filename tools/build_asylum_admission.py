"""Bind the four measured Asylum admission frames to exact retained assets."""
from pathlib import Path
import hashlib
import json

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"development/levels/rebuild"
OUT=BASE/"asylum-admission-pass"
DOORS=(
    (470,"ASYLUM-z-58-30","admission","entry",2,1,1),
    (471,"ASYLUM-x-35-52","admission","interview",1,1,1),
    (472,"ASYLUM-z-54-39.5","interview","records",1,1,1),
    (473,"ASYLUM-z-50-27.5","admission","spine",2,1,-1),
)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def table(path,rows):
    path.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())


def build():
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
    for link in layout["links"]:
        if link["id"] in ("L19","L20","L22","L23","L24"):
            rows.extend(("POINT",link["id"],*point) for point in link["points"])
    OUT.mkdir(exist_ok=True)
    table(OUT/"arrival.tsv",rows)
    frames={row["id"]:row for row in layout["authored_doorways"]}
    asset=sha(ROOT/"assets/runtime/models/door_1_loose.glb")
    doors=[]
    for id,frame,a,b,count,hinge,swing in DOORS:
        d=frames[frame]
        x,z=(d["along"],d["boundary"]) if d["axis"]=="z" else (d["boundary"],d["along"])
        doors.append((id,frame,a,b,zones[a]["owner"],zones[b]["owner"],x,d["sill_y"],z,d["clear_width"],
                      d["axis"],*d["frame_span"],d["clear_height"],count,hinge,swing,asset))
    table(OUT/"door-survey.tsv",doors)


if __name__=="__main__":
    build()
