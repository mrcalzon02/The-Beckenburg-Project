"""Review the exact staged cemetery doors with the shared game renderers and LAN host."""
import argparse
import os
from pathlib import Path
import subprocess

from prepare_cemetery_doors import BOUNDARY, OUTPUT, ROOT, load, prepare


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    args=parser.parse_args()
    prepare()
    build=ROOT/"build/cemetery-door-review"
    build.mkdir(parents=True,exist_ok=True)
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    gpu=ROOT/"gpu-client/target/BeckenburgGPU.jar"
    source=ROOT/"tools/java"
    helpers=("CemeteryDoorReview.java","NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java")
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                    *(str(source/name) for name in helpers)],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(build,jar,gpu) if args.gpu else (build,jar)))
    for site in ("wld-07","wld-07-mortuary"):
        authority=BOUNDARY/site
        data=load(authority/"construction.json")
        subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,
                        "org.beckenburg.CemeteryDoorReview",str(OUTPUT/site),str(authority/"bounded-grounds.glb"),
                        data["glb_sha256"],str(authority/"bounded-grounds.hcs2"),data["physics_sha256"],
                        *(["--gpu"] if args.gpu else [])],cwd=ROOT,check=True)


if __name__=="__main__":
    main()
