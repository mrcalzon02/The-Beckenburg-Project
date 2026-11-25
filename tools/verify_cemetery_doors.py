"""Review the exact staged cemetery doors with the shared game renderers and LAN host."""
import argparse
import hashlib
import json
import os
import shutil
from pathlib import Path
import subprocess
import zipfile

from prepare_cemetery_doors import BASE, BOUNDARY, OUTPUT, ROOT, load, prepare


def verify_named_resources(jar, authority):
    packets=("wld-07","wld-07-mortuary")
    names=("furnished-grounds.glb","furnished-grounds.hcs2","room-layout.tsv","door-schedule.tsv")
    digests={}
    with zipfile.ZipFile(jar) as package:
        for site in packets:
            for name in names:
                resource=f"org/beckenburg/sites/{site}/{name}"
                shipped=package.read(resource)
                source=(authority/site/name).read_bytes()
                if shipped!=source:
                    raise AssertionError(f"Package resource differs from reviewed file: {resource}")
                digests[resource]=hashlib.sha256(shipped).hexdigest()
    return digests


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    stages=parser.add_mutually_exclusive_group()
    stages.add_argument("--refuges",action="store_true")
    stages.add_argument("--power",action="store_true")
    stages.add_argument("--distribution",action="store_true")
    stages.add_argument("--furnishings",action="store_true")
    stages.add_argument("--named",action="store_true")
    args=parser.parse_args()
    if args.named:
        review,stem=BASE/"cemetery-named-pass","furnished-grounds"
    elif args.furnishings:
        review,stem=BASE/"cemetery-furnishings-pass","furnished-grounds"
    elif args.distribution:
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
             "NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java","CemeteryDistributionReview.java",
             "CemeteryFurnishingsReview.java","CemeteryNamedCaseReview.java")
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                    *(str(source/name) for name in helpers)],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(build,jar,gpu) if args.gpu else (build,jar)))
    furnished=BASE/"cemetery-furnishings-pass"
    if args.named:
        review.mkdir(parents=True,exist_ok=True)
        manifest=verify_named_resources(jar,furnished)
        (review/"package-resources.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8",newline="\n")
    for site in ("wld-07","wld-07-mortuary"):
        if stem is not None:
            authority=furnished/site if args.named else review/site
            if args.named:
                stage=review/site
                stage.mkdir(parents=True,exist_ok=True)
                for filename in ("room-layout.tsv","door-schedule.tsv"):
                    shutil.copyfile(authority/filename,stage/filename)
            data=load(authority/"construction.json")["exports"]
            glb,physics=(authority/f"{stem}.{ext}" for ext in ("glb","hcs2"))
            hashes=[data[path.relative_to(ROOT).as_posix()] for path in (glb,physics)]
        else:
            authority=BOUNDARY/site
            data=load(authority/"construction.json")
            glb,physics=(authority/f"bounded-grounds.{ext}" for ext in ("glb","hcs2"))
            hashes=[data["glb_sha256"],data["physics_sha256"]]
        subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx3g",
                         *(["-Dbeckenburg.review.cemeteryNative=true"] if args.named else []),"-cp",cp,
                        "org.beckenburg.CemeteryDoorReview",str(review/site),str(glb),hashes[0],str(physics),hashes[1],
                        *(["--gpu"] if args.gpu else []),*(["--named"] if args.named else [])],cwd=ROOT,check=True)


if __name__=="__main__":
    main()
