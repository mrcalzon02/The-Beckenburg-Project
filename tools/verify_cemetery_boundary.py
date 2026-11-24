"""Replay the native perimeter and canonical arrival with both shared renderers."""
import argparse
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKET = ROOT / "development/levels/rebuild/cemetery-boundary-pass"
BUILD = ROOT / "build/cemetery-boundary-review"


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    args=parser.parse_args()
    BUILD.mkdir(parents=True,exist_ok=True)
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    gpu=ROOT/"gpu-client/target/BeckenburgGPU.jar"
    source=ROOT/"tools/java"
    helpers=("CemeteryBoundaryReview.java","NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java")
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(BUILD),
                    *(str(source/name) for name in helpers)],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(BUILD,jar,gpu) if args.gpu else (BUILD,jar)))
    for site in ("wld-07","wld-07-mortuary"):
        folder=PACKET/site
        report=json.loads((folder/"construction.json").read_bytes())
        table=folder/"boundary-stations.tsv"
        layout=json.loads((ROOT/report["layout"]).read_bytes())
        zones={zone["id"]:zone for zone in layout["zones"]}
        rituals=folder/"review-rituals.tsv"
        rows=[]
        for index,rite in enumerate(layout["rituals"]):
            zone=zones[rite["zone"]]
            rows.append((index,rite["rite"].lower(),zone["id"],*zone["bounds"],zone["y"],zone["height"],
                         str(zone["kind"]!="inside").lower(),*rite["point"]))
        rituals.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())
        subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,
                        "org.beckenburg.CemeteryBoundaryReview",str(folder),report["glb_sha256"],
                        report["physics_sha256"],str(ROOT/report["source_physics"]),
                        report["source_physics_sha256"],str(table),str(rituals),*(["--gpu"] if args.gpu else [])],cwd=ROOT,check=True)


if __name__=="__main__":
    main()
