"""Review the exact staged cemetery doors with the shared game renderers and LAN host."""
import argparse
import os
from pathlib import Path
import subprocess

from prepare_cemetery_doors import BASE, BOUNDARY, OUTPUT, ROOT, load, prepare


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    stages=parser.add_mutually_exclusive_group()
    stages.add_argument("--refuges",action="store_true")
    stages.add_argument("--power",action="store_true")
    stages.add_argument("--distribution",action="store_true")
    args=parser.parse_args()
    if args.distribution:
        review,stem=BASE/"cemetery-distribution-pass","distributed-grounds"
    elif args.power:
        review,stem=BASE/"cemetery-power-pass","caretaker-power"
    elif args.refuges:
        review,stem=BASE/"cemetery-refuge-pass","grounds-with-refuges"
    else:
        prepare()
        review,stem=OUTPUT,None
    build=ROOT/"build/cemetery-door-review"
    build.mkdir(parents=True,exist_ok=True)
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    gpu=ROOT/"gpu-client/target/BeckenburgGPU.jar"
    source=ROOT/"tools/java"
    helpers=("CemeteryDoorReview.java","CemeteryRefugeReview.java","CemeteryPowerReview.java","WharfPowerChecks.java",
             "NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java","CemeteryDistributionReview.java")
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                    *(str(source/name) for name in helpers)],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(build,jar,gpu) if args.gpu else (build,jar)))
    for site in ("wld-07","wld-07-mortuary"):
        if stem is not None:
            authority=review/site
            data=load(authority/"construction.json")["exports"]
            glb,physics=(authority/f"{stem}.{ext}" for ext in ("glb","hcs2"))
            hashes=[data[path.relative_to(ROOT).as_posix()] for path in (glb,physics)]
        else:
            authority=BOUNDARY/site
            data=load(authority/"construction.json")
            glb,physics=(authority/f"bounded-grounds.{ext}" for ext in ("glb","hcs2"))
            hashes=[data["glb_sha256"],data["physics_sha256"]]
        subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,
                        "org.beckenburg.CemeteryDoorReview",str(review/site),str(glb),hashes[0],str(physics),hashes[1],
                        *(["--gpu"] if args.gpu else [])],cwd=ROOT,check=True)


if __name__=="__main__":
    main()
