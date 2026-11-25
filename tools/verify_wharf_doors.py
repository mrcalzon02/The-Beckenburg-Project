"""Verify the Wharf door layout using the current staged shared runtime."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "development/levels/rebuild"
SOURCE = BASE / "native-foyer-door-pass/wld-06"
LAND = BASE / "wharf-landward-pass"
STAGE = BASE / "wharf-door-pass"
BUILD = ROOT / "build/wharf-door-review"
OWNERS = ("cottage-a","cottage-b","boathouse","net","fish","undercroft","west-stair",
          "east-stair","pier-w","pier-e","crossdock","quay","entry","stage")
ANCHORS = ((12,0,48),(52,0,48),(12,0,19),(50,0,17),(32,0,17),(32,-2.5,30),
           (17,0,33),(47.3,0,33),(22,0,23),(42,0,23),(32,0,8),(32,0,45),
           (32,0,61),(32,0,69.5))
FIT = {
    "WLD-06-z-54-12": ("west-cottage-street", 12, -1),
    "WLD-06-x-20-48": ("quay", 11, -1),
    "WLD-06-z-54-52": ("east-cottage-street", 12, -1),
    "WLD-06-x-44-48": ("quay", 11, 1),
    "WLD-06-z-28-16": ("west-stair", 6, -1),
    "WLD-06-x-20-19": ("pier-w", 8, -1),
    "WLD-06-x-44-17": ("pier-e", 9, 1),
    "WLD-06-z-10-32": ("crossdock", 10, 1),
}
# Open transitions sit on the actual grade, deck landings or stair bay.
OPEN = {
    "L01": (32,0,68,"z"),
    "L04": (32,0,54,"z"),
    "L08": (22,0,10,"z"),
    "L09": (42,0,10,"z"),
    "L15": (20,0,39,"x"),
    "L16": (27,-2.5,33,"x"),
    "L17": (37,-2.5,33,"x"),
    "L18": (40.75,0,38,"z"),
}


def read(p):
    return json.loads(p.read_bytes())


def tsv(path, rows):
    path.write_bytes("".join("\t".join(map(str,r))+"\n" for r in rows).encode())


def build_inputs(layout):
    source = {z["id"]:z for z in layout["zones"]}
    rows = []
    for i, (identifier,pos) in enumerate(zip(OWNERS,ANCHORS,strict=True)):
        zone = source[identifier]
        rows.append(("SPACE",identifier,i,*zone["bounds"],zone["y"],zone["height"],
                     str(zone["kind"]!="inside").lower(),zone["name"]))
        rows.append(("ANCHOR",i,identifier,*pos))
    rows += [
        ("SPACE","west-cottage-street",12,6.7,54,20,62,0,3,"true","West cottage street frontage"),
        ("SPACE","east-cottage-street",12,44,54,58,62,0,3,"true","East cottage street frontage"),
        ("SPACE","west-boardwalk-return",6,13.1,38,20,39.9,0,3,"true","Guarded shore boardwalk return"),
        ("SPACE","west-lower-flight",6,17.7,31.7,27.0,34.3,-2.5,2.48,"false","West cast stair"),
        ("SPACE","east-lower-flight",7,37.0,31.7,46.3,34.3,-2.5,2.48,"false","East cast stair"),
    ]
    rows.extend(("TREAD",name) for name in read(SOURCE/"construction.json")["stair_components"])
    rows.extend(("RITUAL",r["rite"].lower(),OWNERS.index(r["zone"]),*r["point"]) for r in layout["rituals"])
    for i,link in enumerate(layout["links"],1):
        name=link["id"]
        points = [point.copy() for point in link["points"]]
        if name == "L16":
            points[1][0] = 26.35
        elif name == "L17":
            points[-2][0] = 37.65
        rows.extend(("POINT",name,*point) for point in points)
        if name in OPEN:
            x,y,z,axis=OPEN[name]
            rows.append(("TRANSITION",840+i,name,OWNERS.index(link["a"]),OWNERS.index(link["b"]),
                         x,y,z,axis,link["width"],"stair" if name in ("L16","L17") else "trail"))
    rows.extend((
        ("TRANSITION", 864, "west-pier-boardwalk", 6, 8, 20, 0, 29, "x", 1.1, "trail"),
        ("TRANSITION", 865, "east-pier-pierhead", 9, 7, 44, 0, 29, "x", 1.5, "trail"),
    ))
    asset = ROOT/"assets/runtime/models/door_1_loose.glb"
    checksum=hashlib.sha256(asset.read_bytes()).hexdigest()
    doors=[]
    for i,d in enumerate(layout["authored_doorways"],810):
        outside,owner,sign=FIT[d["id"]]
        inside,=d["rooms"]
        x,z=(d["along"],d["boundary"]) if d["axis"]=="z" else (d["boundary"],d["along"])
        doors.append((i,d["id"],inside,outside,OWNERS.index(inside),owner,x,d["sill_y"],z,
                      d["clear_width"],d["axis"],*d["frame_span"],d["clear_height"],
                      1 if d["clear_width"]<=1.10 else 2,1,sign,checksum))
    return rows, doors


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    stages=parser.add_mutually_exclusive_group()
    stages.add_argument("--cover",action="store_true")
    stages.add_argument("--power",action="store_true")
    stages.add_argument("--distribution",action="store_true")
    stages.add_argument("--case",action="store_true")
    args=parser.parse_args()
    stem="wharf-case" if args.case else "wharf-distribution" if args.distribution else "wharf-power" if args.power else "wharf-cover" if args.cover else None
    stage=BASE/(stem+"-pass") if stem else STAGE
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    gpu=ROOT/"gpu-client/target/BeckenburgGPU.jar"
    glb=LAND/"enclosed-wharf.glb"
    physics=LAND/"enclosed-wharf.hcs2"
    exports=read(LAND/"checkpoint.json")["exports"]
    hashes=[exports[p.relative_to(ROOT).as_posix()] for p in (glb,physics)]
    stage.mkdir(exist_ok=True)
    BUILD.mkdir(parents=True,exist_ok=True)
    if stem:
        glb=stage/(stem+".glb")
        physics=stage/(stem+".hcs2")
        exports=read(stage/"construction.json")["exports"]
        hashes=[exports[p.relative_to(ROOT).as_posix()] for p in (glb,physics)]
    else:
        rows, doors = build_inputs(read(SOURCE/"authored-layout.json"))
        tsv(stage/"room-layout.tsv",rows)
        tsv(stage/"door-schedule.tsv",doors)
    helpers=[ROOT/"tools/java"/f for f in ("WharfDoorReview.java","WharfCaseChecks.java","WharfPowerChecks.java","NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java")]
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(BUILD),*map(str,helpers)],
                   cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(BUILD,jar,gpu) if args.gpu else (BUILD,jar)))
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true",*(["-Dbeckenburg.review.wharfNative=true"] if args.case else []),"-Xmx3g","-cp",cp,
                    "org.beckenburg.WharfDoorReview",str(stage),str(glb),hashes[0],str(physics),hashes[1],
                    str(LAND/"wharf-tide.water"),*(["--gpu"] if args.gpu else [])],cwd=ROOT,check=True)


if __name__=="__main__":
    main()
