"""Save the proven Lookout roadhead and lower court on their native terrain world."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, sha, tsv

SITE = "WLD-04"
PASS = BASE/"lookout-landing-pass"
BASELINE = BASE/"dam-east-hall-pass/roster.json"
SOURCE_WORLD = BASE/"service-ground-bay-pass/worlds/wld-04/terrain-world"
WORLD = PASS/"worlds/wld-04/terrain-world"
INPUTS = PASS/"program/wld-04"
PREVIEW_PHYSICS = "c3a3ed40a82b9813c0dc1997d0db04cf286ddb90525edcb9bdbb95e6dfebe047"
FIELDS = (
    ("Van roadhead and turning ground", "GRAVEL", 4.1,
     [(16.8,69.4),(20,66.5),(31.6,64.8),(36.4,65.9),(39.2,71.7),
      (38.3,80),(34.4,84.6),(20.2,84.8),(17.5,81.1),(15.7,74.7)]),
    # The north limit leaves the existing structural stair toe and its slope out of the grade.
    ("Full lower switchback court", "GRAVEL", 1.35,
     [(11.9,60.4),(17.1,60.2),(22.8,60.1),(29,60.25),(35.7,60.6),
      (35.1,65.8),(29.4,67.8),(20.2,67.4),(14.4,64.9),(10.9,62.8)]),
)


def main():
    roster = json.loads(BASELINE.read_bytes())
    source = next(row for row in roster["active_sources"] if row["id"]==SITE)
    layout = json.loads((ROOT/source["layout"]).read_bytes())
    meta = json.loads((ROOT/source["construction"]).read_bytes())
    assert (ROOT/meta["saved_world"]).resolve()==SOURCE_WORLD.resolve()
    INPUTS.mkdir(parents=True,exist_ok=True)
    shutil.copytree(SOURCE_WORLD,WORLD,dirs_exist_ok=True)
    tsv(INPUTS/"protected-rooms.tsv",(room["bounds"] for room in layout["zones"]
                                     if room["kind"] in ("inside","circulation","sealed")))
    tsv(INPUTS/"underlays.tsv",[])
    tsv(INPUTS/"features.tsv",([label,material,feather,"plane:0:0:0",*(v for point in shape for v in point)]
                               for label,material,feather,shape in FIELDS))
    classes = ROOT/"build/lookout-landing-java"
    classes.mkdir(parents=True,exist_ok=True)
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    editor = ROOT/"tools/java/RebuildTieredEarthworks.java"
    subprocess.run([str(java/"javac.exe"),"-cp",str(jar),"-d",str(classes),str(editor)],check=True)
    subprocess.run([str(java/"java.exe"),"-Xmx2g","-cp",f"{classes};{jar}","RebuildTieredEarthworks",
                    str(PASS),SITE,str(SOURCE_WORLD)],check=True)
    for name in ("tile_0_0.png","adaptive-topology.bin","authored-cuts.tsv"):
        assert sha(WORLD/name)==sha(SOURCE_WORLD/name),name
    actual = sha(WORLD/"terrain-physics.hcs2")
    assert actual==PREVIEW_PHYSICS,(actual,PREVIEW_PHYSICS)
    grade = json.loads((WORLD/"earthworks-verification.json").read_bytes())
    proof = dict(source_world=SOURCE_WORLD.relative_to(ROOT).as_posix(),
                 writer=editor.relative_to(ROOT).as_posix(),writer_sha256=sha(editor),grade=grade,
                 preview_terrain_sha256=PREVIEW_PHYSICS,native_terrain_sha256=actual)
    for name in ("verification.json","source-provenance.json"):
        (WORLD/name).write_bytes((json.dumps(proof,indent=2)+"\n").encode())


if __name__=="__main__":
    main()
