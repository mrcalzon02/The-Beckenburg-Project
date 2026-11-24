"""Prepare the two Mine runoff catchments as saved editor terrain."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, sha, tsv

BASE=ROOT/"development/levels/rebuild/mine-yard-pass/worlds/wld-05/terrain-world"
FIELDS=(
    ("Roadhead perimeter infiltration apron","GRAVEL",1.1,"plane:2.374:-.012:-.03",
     ((42.0,65.85),(43.85,67.0),(45.55,70.42),(44.66,74.05),(42.1,74.22),(41.5,70.25))),
    ("Crew and stores filter bed","GRAVEL",1.15,"plane:1.214:0:-.026",
     ((4.0,63.98),(8.4,63.37),(11.7,64.65),(10.8,68.0),(6.33,68.72),(3.96,66.29))),
)


def prepare(target):
    world=target/"worlds/wld-05/terrain-world"
    program=target/"program/wld-05"
    assert not world.exists(),f"Use a fresh target for the saved drainage terrain: {world}"
    program.mkdir(parents=True,exist_ok=True)
    shutil.copytree(BASE,world,ignore=shutil.ignore_patterns("*.lock"))
    tsv(program/"features.tsv",([name,mat,feather,grade,*(v for p in polygon for v in p)]
                               for name,mat,feather,grade,polygon in FIELDS))
    tsv(program/"protected-rooms.tsv",[(2,46,20,62),(2,30,14,46),(40,46,54,62),(40,30,54,46),
                                      (26.5,49,31.25,54.25),(33.95,67.35,39.55,71.35)])
    tsv(program/"underlays.tsv",[])
    directory=ROOT/"build/mine-yard-java"
    directory.mkdir(parents=True,exist_ok=True)
    builder=ROOT/"tools/java/RebuildTieredEarthworks.java"
    jar=ROOT/"build/beckenburg-game.jar"
    jdk=Path("C:/Program Files/Java/jdk-17/bin")
    subprocess.run([str(jdk/"javac.exe"),"-cp",str(jar),"-d",str(directory),str(builder)],check=True)
    subprocess.run([str(jdk/"java.exe"),"-Xmx2g","-cp",f"{directory};{jar}","RebuildTieredEarthworks",str(target),
                    "WLD-05",str(BASE)],check=True)
    result=json.loads((world/"earthworks-verification.json").read_bytes())
    assert result["lowered_vertices"]>=10 and result["raised_vertices"]==0,result
    assert 0<result["max_change_m"]<.33,result
    for filename in ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv"):
        assert sha(world/filename)==sha(BASE/filename),filename
    result.update(source_world=BASE.relative_to(ROOT).as_posix(),builder_sha256=sha(builder),
                  design_source=Path(__file__).resolve().relative_to(ROOT).as_posix(),design_sha256=sha(Path(__file__)),
                  terrain_features=FIELDS)
    for name in ("verification.json","source-provenance.json"):
        (world/name).write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS saved runoff catchments,",result["lowered_vertices"],"lowered controls and",result["hole_controls"],"unchanged excavation controls")


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("target",type=Path)
    prepare(parser.parse_args().target.resolve())
