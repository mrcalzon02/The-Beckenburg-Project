"""Sweep the staged Battery from its real van to the upper director deck."""
from pathlib import Path
from math import hypot
from subprocess import run
import sys

from build_measured_approaches import ROOT, read, sha, relative, write
from prepare_seacliff_battery import PASS, SITE
from prepare_cemetery_grounds import tsv

STEM = "founded-receiving-stairs"
OWNERS = {
    "gallery":0,"director-stair":1,"director":2,"west":3,"east":4,"magazine":5,
    "service":6,"barracks":7,"lowgallery":8,"plant":9,"records":10,"pump":11,
    "yard":12,"entry":13,"stage":13,"approach":13,
}


def verify(target):
    folder=target/SITE.lower()
    layout,meta=(read(folder/file) for file in ("authored-layout.json","construction.json"))
    glb,hcs=(folder/(STEM+suffix) for suffix in (".glb",".hcs2"))
    assert sha(glb)==meta["glb_sha256"] and sha(hcs)==meta["physics_sha256"]
    (door,)=[d for d in layout["authored_doorways"] if d["id"]=="WLD-17-gallery-north-stair"]
    ax,ay,az=door["frame_center"]
    directory=target/"verification"
    directory.mkdir(parents=True,exist_ok=True)
    leaf=ROOT/"assets/runtime/models/door_1_loose.glb"
    if not leaf.exists():
        leaf=next(p for p in ROOT.glob("assets/runtime/**/door_1_loose.glb") if p.is_file())
    rows=[
        ("SOURCE",relative(glb),sha(glb),relative(hcs),sha(hcs)),
        ("DOCK",*layout["staging"]["point"],layout["staging"]["yaw"]),
        ("FRAME",420,door["id"],ax,ay,az,door["clear_width"],*door["frame_span"],door["clear_height"],sha(leaf)),
    ]
    links={l["id"]:l["points"] for l in layout["links"]}
    placements={
        "gallery":[34,4,30],"director":[39,7,23.2],"west":[16,4,33],"east":[52,4,33],
        "magazine":[16,4,20],"service":[52,4,20],"barracks":[16,0,20],"lowgallery":[34,0,30],
        "plant":[52,0,20],"records":[29,0,20],"pump":[39,0,20],"yard":[36.5,4,42.5],"entry":[32,0,75],
    }
    for zone in layout["zones"]:
        key=zone["id"]
        rows.append(("SPACE",key,OWNERS[key],*zone["bounds"],zone["y"],zone["height"],
                     zone["kind"] in ("staging","outside"),*placements.get(key,())))
    rows.extend((("SPACE","director-stair",1,28.84,20.9,31.1,26,4,3,False,30,4,23),
                 ("SPACE","director-flight",1,31.1,20.9,38,23.1,4,3,False)))
    rows.extend(("TREAD",name) for name in meta["stair_components"])
    rows.extend(("RITUAL",site["rite"].lower(),OWNERS[site["zone"]],*site["point"])
                for site in layout["rituals"])
    def join(*parts):
        points=[]
        for p in (p for segment in parts for p in segment):
            if not points or p!=points[-1]:
                points.append(p)
        return points
    # A riser plane belongs to the adjoining flat and tread. Set the
    # verification poses on those two standing surfaces on either side.
    for key,(toe,head,risers) in meta["physical_stair_cells"].items():
        points=links[key]
        i=points.index(toe)
        dx,dz=head[0]-toe[0],head[2]-toe[2]
        assert hypot(dx,dz)>0
        from_x,from_z=toe[0]-points[i-1][0],toe[2]-points[i-1][2]
        before=hypot(from_x,from_z)
        receiving=[toe[0]-.35*from_x/before,toe[1],toe[2]-.35*from_z/before]
        first=[toe[0]+dx/(2*risers),toe[1]+(head[1]-toe[1])/risers,toe[2]+dz/(2*risers)]
        points[i:i+1]=[receiving,first]
    lower=join(links["route-01"],links["route-02"],links["route-03"],[[40,4,46],[36.5,4,42.5]])
    drives={
        "west wing":join(lower,links["route-04"],links["route-06"],links["route-10"]),
        "east wing":join(lower,links["route-05"],list(reversed(links["route-07"])),links["route-10"]),
    }
    rows.extend(("POINT",name,*p) for name,l in drives.items() for p in l)
    source,result=directory/"battery-runtime-input.tsv",directory/"battery-runtime-result.tsv"
    tsv(source,rows)
    classes=ROOT/"build/seacliff-receiving/runtime"
    classes.mkdir(parents=True,exist_ok=True)
    jdk=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    code=ROOT/"tools/java/NativeSeacliffReceivingProbe.java"
    run([str(jdk/"javac.exe"),"-encoding","UTF-8","-Xprefer:source","-sourcepath","src/main/java;src/test/java",
         "-cp",str(jar),"-d",str(classes),str(code)],cwd=ROOT,check=True)
    images=ROOT/"build/seacliff-receiving/runtime-captures"
    run([str(jdk/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",f"{classes};{jar}",
         "org.beckenburg.NativeSeacliffReceivingProbe",str(source),str(images),str(result)],cwd=ROOT,check=True)
    image_names=("battery-gallery-closed-runtime.png","battery-gallery-open-runtime.png","battery-director-stair-runtime.png")
    write(directory/"battery-runtime.json",dict(scope="Staged production scene loader, game mover, fitted frame, software renderer and state snapshots.",
          tests=dict(line.split("\t",1) for line in result.read_text(encoding="utf-8").splitlines()[1:]),
          glb_sha256=sha(glb),physics_sha256=sha(hcs),door_leaf_sha256=sha(leaf),
          evidence={relative(p):sha(p) for p in (source,result,Path(__file__),code)},
          captures={name:sha(images/name) for name in image_names}))


if __name__=="__main__":
    verify((ROOT/sys.argv[1]).resolve() if len(sys.argv)>1 else PASS)
