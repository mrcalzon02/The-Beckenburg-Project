"""Run the connected hospital case on the exact ordinary Asylum archive."""
from hashlib import sha256
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile

from verify_blackwater_named import CLASSES, GAME, ROOT
from verify_northwatch_named import jdk


def main():
    payload = ROOT / "development/levels/rebuild/asylum-flight-rails-pass"
    names = ("asylum-stair.glb", "asylum-stair.hcs2", "arrival.tsv", "door-survey.tsv", "mission.tsv", "services.tsv")
    with ZipFile(GAME) as archive:
        for name in names:
            resource = name if name.startswith("asylum-") else "asylum-" + name
            expected = sha256((payload / name).read_bytes()).digest()
            assert sha256(archive.read("org/beckenburg/sites/" + resource)).digest() == expected, name
        assert b"Main-Class: org.beckenburg.InvestigationGame" in archive.read("META-INF/MANIFEST.MF")
    print("PASS ordinary game archive contains all six exact St. Dymphna resources", flush=True)
    classes = ROOT / "build/asylum-continuous"
    classes.mkdir(parents=True, exist_ok=True)
    sources = [str(ROOT / "tools/java" / (name + ".java")) for name in (*CLASSES, "AsylumContinuousReview")]
    subprocess.run([jdk("javac"), "--release", "17", "-encoding", "UTF-8", "-cp", str(GAME),
                    "-d", str(classes), *sources], cwd=ROOT, check=True)
    cp = os.pathsep.join(map(str, (classes, GAME)))
    subprocess.run([jdk("java"), "-Djava.awt.headless=true", "-Xmx3g", "-cp", cp,
                    "org.beckenburg.AsylumContinuousReview", str(classes / "case.tsv")],
                   cwd=ROOT, check=True, timeout=540)


if __name__ == "__main__":
    main()
