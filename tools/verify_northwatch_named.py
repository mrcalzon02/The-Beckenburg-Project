"""Assemble an isolated Northwatch game archive and replay named solo/LAN actions."""
import argparse
from hashlib import sha256
import json
import os
from pathlib import Path
import shutil
import subprocess
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/"development/levels/rebuild"
STAGE=BASE/"northwatch-named-pass"
FURNISHED=BASE/"northwatch-furnishing-pass"
PACKET=FURNISHED/"wld-08"
BUILD=ROOT/"build/northwatch-named-review"
BASE_JAR=ROOT/"build/beckenburg-game.jar"
REVIEW_JAR=BUILD/"NorthwatchReview.jar"
GPU=ROOT/"gpu-client/target/BeckenburgGPU.jar"
RESOURCE=Path("org/beckenburg/sites/wld-08")
RUNTIME_PAYLOADS=("station-with-furnishings.glb","station-with-furnishings.hcs2","runtime-input.tsv","door-schedule.tsv")
REVIEW_CLASSES=(
    "NorthwatchNamedReview","NorthwatchInvestigationReview","NorthwatchDoorRuntime","NorthwatchDistributionReview",
    "NorthwatchPowerReview","NorthwatchWaterReview","NorthwatchFurnishingReview",
    "NativeSchoolInteriorReview","NativeSchoolDoorReview","CemeteryRefugeReview","WharfPowerChecks",
)


def jdk(name):
    suffix=".exe" if os.name=="nt" else ""
    bundled=Path("C:/Program Files/Java/jdk-17/bin")/(name+suffix)
    if bundled.is_file():
        return str(bundled)
    if found:=shutil.which(name):
        return found
    raise FileNotFoundError(f"Java 17 JDK tool not found: {name}")


def run(args):
    subprocess.run(args,cwd=ROOT,check=True)


def package():
    checkpoint=json.loads((FURNISHED/"checkpoint.json").read_bytes())
    for name,expected in (checkpoint["exports"]|checkpoint["evidence"]).items():
        assert sha256((ROOT/name).read_bytes()).hexdigest()==expected,name
    compiled=BUILD/"production"
    assets=BUILD/"resources"/RESOURCE
    compiled.mkdir(parents=True,exist_ok=True)
    assets.mkdir(parents=True,exist_ok=True)
    roots=("src/main/java","water-module/src/main/java","lighting-module/src/main/java")
    paths=sorted(path.relative_to(ROOT).as_posix() for source in roots for path in (ROOT/source).rglob("*.java"))
    arguments=BUILD/"main-sources.txt"
    arguments.write_text("".join(f'"{path}"\n' for path in paths),encoding="utf8",newline="\n")
    run([jdk("javac"),"--release","17","-encoding","UTF-8","-d",str(compiled),"@"+str(arguments)])
    for name in RUNTIME_PAYLOADS:
        shutil.copyfile(PACKET/name,assets/name)
    shutil.copyfile(STAGE/"source-hashes.tsv",assets/"source-hashes.tsv")
    partial=BUILD/"NorthwatchReview-building.jar"
    shutil.copyfile(BASE_JAR,partial)
    run([jdk("jar"),"--update","--file",str(partial),"-C",str(compiled),".",
         "-C",str(BUILD/"resources"),"."])
    with ZipFile(partial) as archive:
        for name in (*RUNTIME_PAYLOADS,"source-hashes.tsv"):
            assert archive.read((RESOURCE/name).as_posix())==(assets/name).read_bytes(),name
        assert "Main-Class: org.beckenburg.InvestigationGame" in archive.read("META-INF/MANIFEST.MF").decode()
    partial.replace(REVIEW_JAR)
    print("PASS all production Java sources and five exact archived Northwatch resources",flush=True)


def check_ordinary_archive():
    resources=json.loads((STAGE/"checkpoint.json").read_bytes())["packaged_resources"]
    with ZipFile(BASE_JAR) as archive:
        for name,metadata in resources.items():
            assert sha256(archive.read(name)).hexdigest()==metadata["sha256"],name
        assert b"Main-Class: org.beckenburg.InvestigationGame" in archive.read("META-INF/MANIFEST.MF")
    print("PASS five exact Northwatch resources in the ordinary game archive",flush=True)


def main():
    parser=argparse.ArgumentParser()
    mode=parser.add_mutually_exclusive_group()
    mode.add_argument("--gpu",action="store_true")
    mode.add_argument("--investigation",action="store_true")
    parser.add_argument("--reuse-package",action="store_true")
    parser.add_argument("--package-only",action="store_true")
    args=parser.parse_args()
    if args.investigation:
        if args.reuse_package or args.package_only:
            parser.error("--investigation uses the existing ordinary game archive")
        check_ordinary_archive()
    elif not args.reuse_package:
        package()
    if args.package_only:
        return
    archive=BASE_JAR if args.investigation else REVIEW_JAR
    verifier=BUILD/("investigation" if args.investigation else "opengl" if args.gpu else "software")
    verifier.mkdir(parents=True,exist_ok=True)
    sources=[str(ROOT/"tools/java"/(name+".java")) for name in REVIEW_CLASSES]
    run([jdk("javac"),"--release","17","-encoding","UTF-8","-cp",str(archive),"-d",str(verifier),*sources])
    cp=os.pathsep.join(map(str,(verifier,archive,GPU) if args.gpu else (verifier,archive)))
    if args.investigation:
        report=BUILD/"investigation.tsv"
        report.unlink(missing_ok=True)
        subprocess.run([jdk("java"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,
                        "org.beckenburg.NorthwatchInvestigationReview",str(report)],cwd=ROOT,check=True,timeout=660)
        return
    run([jdk("java"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,"org.beckenburg.NorthwatchNamedReview",str(STAGE),
         *(["--gpu"] if args.gpu else [])])


if __name__=="__main__":
    main()
