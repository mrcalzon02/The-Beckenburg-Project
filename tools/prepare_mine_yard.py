"""Save Mine arrival and building courts with the surveyed headhouse foundation cut."""
import json
from pathlib import Path
import shutil
import subprocess

from prepare_cemetery_grounds import ROOT, BASE, VAN, sha, tsv

PASS = BASE/"mine-yard-pass"
PROGRAM = PASS/"program/wld-05"
OLD_WORLD = BASE/"flat-terrain-pass/worlds/wld-05/terrain-world"
WORLD = PASS/"worlds/wld-05/terrain-world"
BASELINE = BASE/"cemetery-grounds-pass/roster.json"
HEADHOUSE = (26.5,49,31.25,54.25)
FIELDS = [
    ("Roadhead turn and van apron","GRAVEL",6,[(20,87),(18,79),(20,71),(24,65),(36,65),(42,70),(39,78),(37,86)]),
    ("Common sorting and workshop yard","GRAVEL",6,[(17,66),(17,56),(20,52),(23,50),(28,49),(35,50),(41,52),(46,59),(44,67),(36,72),(26,72)]),
    ("Stores lower bank and forecourt","DIRT",6,[(13,37),(17,38),(22,40),(25,43),(25,48),(24,54),(20,56),(16,52),(13,45)]),
    ("Assay and wash-house bench","DIRT",6,[(40,40),(41,45),(41,53),(41,59),(38,62),(35,58),(33,52),(33,46),(35,42)]),
]


def main():
    PROGRAM.mkdir(parents=True,exist_ok=True)
    shutil.copytree(OLD_WORLD,WORLD,dirs_exist_ok=True)
    roster=json.loads(BASELINE.read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-05")
    layout=json.loads((ROOT/row["layout"]).read_bytes())
    tsv(PROGRAM/"protected-rooms.tsv",(z["bounds"] for z in layout["zones"] if z["y"]==0 and z["kind"]=="inside"))
    tsv(PROGRAM/"underlays.tsv",[])
    tsv(PROGRAM/"features.tsv",([name,material,feather,"plane:0:0:0",*(v for p in boundary for v in p)]
                                for name,material,feather,boundary in FIELDS))
    tsv(PROGRAM/"headhouse-foundation.tsv",[HEADHOUSE])
    jdk=Path("C:/Program Files/Java/jdk-17/bin")
    classes=ROOT/"build/mine-yard-java"
    classes.mkdir(parents=True,exist_ok=True)
    jar=ROOT/"build/beckenburg-game.jar"
    builders=[ROOT/"tools/java"/t for t in ("RebuildTieredEarthworks.java","RebuildStairCellPunches.java")]
    subprocess.run([str(jdk/"javac.exe"),"-cp",str(jar),"-d",str(classes),*map(str,builders)],check=True)
    cp=f"{classes};{jar}"
    subprocess.run([str(jdk/"java.exe"),"-Xmx2g","-cp",cp,"RebuildTieredEarthworks",str(PASS),"WLD-05",str(OLD_WORLD)],check=True)
    subprocess.run([str(jdk/"java.exe"),"-Xmx2g","-cp",cp,"RebuildStairCellPunches",str(WORLD),
                    str(PROGRAM/"headhouse-foundation.tsv")],check=True)
    receipt=dict(source_world=OLD_WORLD.relative_to(ROOT).as_posix(),
                 builders={p.relative_to(ROOT).as_posix():sha(p) for p in builders},
                 grade=json.loads((WORLD/"earthworks-verification.json").read_bytes()),
                 foundation=json.loads((WORLD/"stair-cell-punch-verification.json").read_bytes()),
                 terrain_sha256=sha(WORLD/"terrain-physics.hcs2"))
    assert sha(WORLD/"tile_0_0.png")==sha(OLD_WORLD/"tile_0_0.png")
    assert receipt["foundation"]["physics_sha256"]==receipt["terrain_sha256"]
    (WORLD/"verification.json").write_bytes((json.dumps(receipt,indent=2)+"\n").encode())
    (WORLD/"source-provenance.json").write_bytes((json.dumps(receipt,indent=2)+"\n").encode())


if __name__=="__main__":
    main()
