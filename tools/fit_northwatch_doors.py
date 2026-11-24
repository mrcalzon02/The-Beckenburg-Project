"""Fit all Northwatch leaves to the retained wall reveals and landing geometry."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from build_college_electrical import point
from prepare_cemetery_doors import SOURCE_LEAF, write_tsv
from review_roster_macroterrain import sha
from verify_college_services import bvh
from verify_northwatch_boundary import OWNERS, PACKET as BOUNDARY

BASE = ROOT / "development/levels/rebuild"
OUT = BASE / "northwatch-door-pass/wld-08"
OUTSIDE = {
    "WLD-08-x-20-41":"court", "WLD-08-z-56-11":"station-exterior",
    "WLD-08-z-26-26":"court", "WLD-08-z-26-38":"court",
    "WLD-08-x-44-16":"array", "WLD-08-z-26-53":"array-yard",
    "WLD-08-x-44-41":"court", "WLD-08-z-56-53":"station-exterior",
    "WLD-08-x-62-14":"array",
}
SWINGS = {
    "WLD-08-z-26-7":(2,1,1), "WLD-08-x-20-19":(1,1,1), "WLD-08-z-44-11":(2,1,1),
    "WLD-08-x-20-41":(1,1,-1), "WLD-08-z-56-11":(2,1,-1), "WLD-08-z-26-26":(1,1,-1),
    "WLD-08-x-32-19":(1,1,1), "WLD-08-z-26-38":(1,-1,-1), "WLD-08-x-44-16":(2,1,-1),
    "WLD-08-z-26-53":(1,-1,1), "WLD-08-z-44-53":(1,1,1), "WLD-08-x-44-41":(1,-1,1),
    "WLD-08-z-56-53":(1,-1,-1), "WLD-08-x-62-14":(1,1,1),
    "WLD-08-cell-tunnel-N-22":(2,1,1), "WLD-08-cell-tunnel-n-E-23":(2,1,1),
}
STATIONS = {
    "stage":(35,0,74),
    "tunnel-n":(11,-2.5,23.4),
}
# Additional ownership volumes on the actual aprons and reserved flight bays.
SPACES = (
    ("array-yard",10,44,20,62,26,0,3,"true","Array service apron",53,0,24),
    ("lab-flight",0,11,34.15,22.05,35.85,-2.28,2.22,"false","Laboratory stair bay",11,0,35),
    ("dorm-flight",1,41.95,34.15,53,35.85,-2.28,2.22,"false","Dormitory stair bay",53,0,35),
    ("generator-flight",5,9.83,17,12.17,22.12,-2.28,2.22,"false","Generator stair bay",11,0,17),
)
OPEN = (
    (900,12,13,32,0,70,"z",3.5,"trail"),
    (901,11,12,32,0,56,"z",3.5,"trail"),
    (902,0,7,22.13,-2.5,35,"x",1.5,"stair"),
    (903,7,1,41.87,-2.5,35,"x",1.5,"stair"),
    (904,5,8,11,-2.5,22.21,"z",1.5,"stair"),
)


def measure(tree, frame):
    across = frame["axis"] == "z"
    tangent, normal, floor = (frame[key] for key in ("along","boundary","sill_y"))
    width, height = frame["clear_width"],frame["clear_height"]
    direction=point(0,0,1) if across else point(1,0,0)

    def at(u,h,v):
        return point(u,h,v) if across else point(v,h,u)

    def clear(u,h):
        return tree.ray_cast(at(tangent+u,floor+h,normal-.72),direction,1.44)[0] is None

    for h in (.16,1.15,min(1.83,height-.07)):
        for sign in (-1,1):
            assert clear(sign*(width/2-.025),h) and not clear(sign*(width/2+.025),h), (frame["id"],h,sign)
    assert clear(0,height-.025) and not clear(0,height+.025),frame["id"]
    supported=[]
    for lateral in (-.26,0,.26):
        for away in (-.68,0,.68):
            hit,vertical,_,_=tree.ray_cast(at(tangent+lateral,floor+.45,normal+away),Vector((0,0,-1)),1.2)
            assert hit is not None and abs(hit.z-floor)<.005 and vertical.z>.98,(frame["id"],lateral,away,hit)
            supported.append(round(hit.z,5))
    return supported


def build():
    parent=json.loads((BOUNDARY.parent/"checkpoint.json").read_bytes())
    for name,digest in (parent["exports"]|parent["evidence"]).items():
        assert sha(ROOT/name)==digest,name
    boundary=json.loads((BOUNDARY/"construction.json").read_bytes())
    layout=ROOT/boundary["layout"]
    assert sha(layout)==boundary["layout_sha256"]
    bpy.ops.wm.open_mainfile(filepath=str(BOUNDARY/"bounded-station.blend"))
    tree=bvh([obj for obj in bpy.context.scene.objects if obj.type=="MESH"])
    frames=json.loads(layout.read_bytes())["authored_doorways"]
    assert {frame["id"] for frame in frames}==SWINGS.keys()
    owners=OWNERS|{"station-exterior":12,"array-yard":10}
    schedule=[]
    details=[]
    leaf_hash=sha(SOURCE_LEAF)
    for i,frame in enumerate(frames,840):
        key=frame["id"]
        a,*adjacent=frame["rooms"]
        b=adjacent[0] if adjacent else OUTSIDE[key]
        leaves,hinge,swing=SWINGS[key]
        floors=measure(tree,frame)
        # Hang at the receiving-side reveal face, which clears the deep wall
        # after opening. The installed hinge axis is 4 mm beyond that face.
        mount=frame["frame_span"][int(swing>0)]+swing*.004
        axis=frame["axis"]
        x,z=(frame["along"],mount) if axis=="z" else (mount,frame["along"])
        schedule.append((i,key,a,b,owners[a],owners[b],x,frame["sill_y"],z,frame["clear_width"],axis,
                         mount-.026,mount+.026,frame["clear_height"],leaves,hinge,swing,leaf_hash))
        details.append(dict(frame=key,physical_reveal=frame["frame_span"],hinge_axis=mount,
                            opening_m=[frame["clear_width"],frame["clear_height"]],
                            swing_direction=swing,leaves=leaves,floor_samples=floors))
    runtime=[]
    for row in (BOUNDARY/"runtime-input.tsv").read_text(encoding="utf-8").splitlines():
        fields=row.split("\t")
        if fields[0]=="SPACE" and fields[1] in STATIONS:
            fields[11:]=STATIONS[fields[1]]
        runtime.append(fields)
    runtime.extend(("SPACE",*row) for row in SPACES)
    runtime.extend(("OPEN",*row) for row in OPEN)
    OUT.mkdir(parents=True,exist_ok=True)
    write_tsv(OUT/"runtime-input.tsv",runtime)
    write_tsv(OUT/"door-schedule.tsv",schedule)
    receipt=dict(glb_sha256=boundary["glb_sha256"],physics_sha256=boundary["physics_sha256"],
                 source=parent["exports"],prior_layout=boundary["layout"],prior_layout_sha256=sha(layout),
                 leaf_source=SOURCE_LEAF.relative_to(ROOT).as_posix(),leaf_hash=leaf_hash,
                 supported_frames=details)
    (OUT/"construction.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",len(schedule),"fitted frames,",sum(len(row["floor_samples"]) for row in details),"floor measurements")


if __name__=="__main__":
    build()
