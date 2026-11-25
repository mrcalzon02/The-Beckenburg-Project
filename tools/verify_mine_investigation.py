"""Check and replay a continuous investigation in the ordinary packaged Mine."""
from hashlib import sha256
import json
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile

from verify_northwatch_named import jdk

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT / "development/levels/rebuild/mine-case-pass"
GAME = ROOT / "build/beckenburg-game.jar"
BUILD = ROOT / "build/mine-investigation"
CLASSES = (
    "MineInvestigationReview", "NativeSchoolInteriorReview", "NativeSchoolDoorReview",
    "CemeteryRefugeReview", "WharfPowerChecks", "NorthwatchDoorRuntime",
    "NorthwatchPowerReview", "NorthwatchDistributionReview", "NorthwatchWaterReview",
    "NorthwatchFurnishingReview",
)


def main():
    resources = json.loads((STAGE / "checkpoint.json").read_bytes())["packaged_resources"]
    with ZipFile(GAME) as archive:
        for name, digest in resources.items():
            assert sha256(archive.read(name)).hexdigest() == digest, name
        assert b"Main-Class: org.beckenburg.InvestigationGame" in archive.read("META-INF/MANIFEST.MF")
    print(f"PASS {len(resources)} exact Mine resources in the ordinary game archive", flush=True)
    classes = BUILD / "classes"
    classes.mkdir(parents=True, exist_ok=True)
    report = BUILD / "case.tsv"
    report.unlink(missing_ok=True)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in CLASSES]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp",
                    str(GAME), "-d", str(classes), *sources], cwd=ROOT, check=True)
    cp = os.pathsep.join(map(str, (classes, GAME)))
    subprocess.run([jdk("java"), "-Djava.awt.headless=true", "-Xmx3g", "-cp", cp,
                    "org.beckenburg.MineInvestigationReview", str(report)], cwd=ROOT, check=True, timeout=660)


if __name__ == "__main__":
    main()
