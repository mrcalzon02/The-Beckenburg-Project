"""Re-run six stale map case receipts on a newly packaged game, without rewriting history.

This is a scoped TCP/physics development diagnostic, not visual or installer acceptance.
Run after build.cmd package. Output contains fresh case reports, logs and source provenance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[3]
CASES = (
    ("WLD-03", "RedMesaInvestigationReview", "ABSOLVED"),
    ("WLD-08", "NorthwatchInvestigationReview", "BANISHED"),
    ("WLD-09", "BlackwaterInvestigationReview", "CLEANSED"),
    ("WLD-11", "MourningMarshInvestigationReview", "ABSOLVED"),
    ("WLD-12", "GallowsInvestigationReview", "ABSOLVED"),
    ("WLD-13", "GreywaterInvestigationReview", "ABSOLVED"),
)

def digest(data):
    return hashlib.sha256(data).hexdigest()

def sha(path):
    return digest(path.read_bytes())

def run(command, log, timeout):
    with log.open("w", encoding="utf-8") as out:
        try:
            return subprocess.run(command, cwd=ROOT, stdout=out, stderr=subprocess.STDOUT,
                                  timeout=timeout, check=False).returncode
        except subprocess.TimeoutExpired:
            out.write("\nTIMEOUT after %d seconds\n" % timeout)
            return 124

def read_receipt(path):
    lines = path.read_text(encoding="utf-8-sig").splitlines()
    if not lines or lines[0] != "check\tresult":
        raise ValueError("invalid case receipt header")
    entries = {}
    for line in lines[1:]:
        key, value = line.split("\t", 1)
        if key in entries:
            raise ValueError("duplicate case receipt key: " + key)
        entries[key] = value
    return entries

def write_summary(path, summary):
    path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--jar", type=Path, default=ROOT / "build/beckenburg-game.jar")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        raise SystemExit("Use a fresh output directory; never overwrite a prior receipt.")
    output.mkdir(parents=True)
    jar = args.jar.resolve()
    diagnostic = json.loads((ROOT / "development/levels/MAP_DEVELOPMENT_DIAGNOSTIC_2026-11-26.json").read_text(encoding="utf-8"))
    prior = {row["id"]: row for row in diagnostic["case_source_checks"]}
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    summary = {"source_commit": revision, "jar_sha256": sha(jar), "scope": "packaged TCP case replay only",
               "cases": []}
    summary_path = output / "summary.json"
    write_summary(summary_path, summary)

    # Stage the existing review helpers under their real Java package. javac
    # compiles only the requested entry points and their referenced helpers,
    # rather than requiring every historical one-off review to compile.
    work = output / "compile"
    package = work / "source/org/beckenburg"
    package.mkdir(parents=True)
    classes = work / "classes"
    classes.mkdir()
    for src in (ROOT / "tools/java").glob("*.java"):
        shutil.copyfile(src, package / src.name)
    shutil.copyfile(Path(__file__).with_name("GallowsInvestigationReview.java"),
                    package / "GallowsInvestigationReview.java")
    compile_cmd = ["javac", "--release", "17", "-encoding", "UTF-8",
                   "-cp", str(jar), "-sourcepath",
                   os.pathsep.join((str(work / "source"), str(ROOT / "src/test/java"))),
                   "-d", str(classes)]
    # Each case is compiled independently. One obsolete harness must not
    # suppress the remaining cases or hide its own failure.
    with zipfile.ZipFile(jar) as archive:
        for site, entrypoint, outcome in CASES:
            folder = output / site
            folder.mkdir()
            row = {"site": site, "entrypoint": entrypoint, "expected_outcome": outcome,
                   "status": "NOT_RUN"}
            summary["cases"].append(row)
            try:
                historical = prior[site]
                source = ROOT / historical["source_file"]
                packaged = "org/beckenburg/sites/" + site.lower() + "/" + source.name
                row.update(source_file=historical["source_file"], source_sha256=sha(source),
                           historical_receipt_sha256=historical["receipt_collision_sha256"],
                           packaged_entry=packaged, packaged_sha256=digest(archive.read(packaged)))
                if row["source_sha256"] != row["packaged_sha256"]:
                    raise ValueError("packaged collision differs from committed source")
                # A new independent compile directory prevents another case's
                # compiled helpers from concealing a missing dependency.
                case_classes = folder / "classes"
                case_classes.mkdir()
                cmd = compile_cmd.copy()
                cmd[cmd.index("-d")+1] = str(case_classes)
                rc = run(cmd + [str(package / (entrypoint + ".java"))], folder / "compile.log", 180)
                row["compile_exit"] = rc
                if rc:
                    raise ValueError("review compilation failed; inspect compile.log")
                receipt = folder / "case.tsv"
                home = folder / "home"
                home.mkdir()
                rc = run(["java", "-ea", "-Djava.awt.headless=true", "-Duser.home=" + str(home),
                          "-cp", os.pathsep.join((str(jar), str(case_classes))),
                          "org.beckenburg." + entrypoint, str(receipt)],
                         folder / "case.log", 480)
                row["replay_exit"] = rc
                if rc:
                    raise ValueError("case replay failed; inspect case.log")
                checks = read_receipt(receipt)
                row["runtime_collision_sha256"] = checks.get("collision_sha256")
                row["named_revision"] = checks.get("named_revision")
                if row["runtime_collision_sha256"] != row["source_sha256"]:
                    raise ValueError("case did not exercise the packaged committed collision")
                if not row["named_revision"]:
                    raise ValueError("case did not identify its runtime revision")
                # Require real settlement evidence, not simply exit zero.
                if not any(value.startswith("PASS " + outcome + " reported to all three peers;")
                           for value in checks.values()):
                    raise ValueError("case receipt lacks the expected replicated settlement")
                row["status"] = "PASS"
            except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
                row.update(status="FAIL", reason=str(error))
            finally:
                write_summary(summary_path, summary)
                print(site, row["status"], row.get("reason", ""), flush=True)
    return 0 if all(row["status"] == "PASS" for row in summary["cases"]) else 1

if __name__ == "__main__":
    sys.exit(main())
