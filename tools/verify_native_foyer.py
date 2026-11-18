"""Bind and review the School's actual entrance frame using the current native runtime."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT/"development/levels/rebuild"
FROM = BASE/"measured-arrival-and-approach-pass"
OUT = BASE/"native-foyer-door-pass"
CAPTURES = BASE/"captures/native-foyer-door-batch"
ASSET = ROOT/"assets/runtime/models/door_1_loose.glb"


def read(path):
    return json.loads(path.read_bytes())


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path):
    return path.relative_to(ROOT).as_posix()


def write(path, data):
    path.write_bytes((json.dumps(data, indent=2) + "\n").encode())


def main():
    row = next(r for r in read(FROM/"roster.json")["active_sources"] if r["id"]=="SCHOOL")
    school = OUT/"school"
    plan = read(school/"door-schedule.json")
    layout = read(ROOT/row["layout"])
    door = next(d for d in layout["authored_doorways"] if d["id"]==plan["frame_id"])
    rooms = {r["id"]:r for r in layout["zones"]}
    room_a, room_b = (rooms[plan[key]] for key in ("room_a","room_b"))
    assert room_a["id"] in door["rooms"] and room_b["kind"]=="outside"
    a,b = door["frame_span"]
    x,z = (door["along"],door["boundary"]) if door["axis"]=="z" else (door["boundary"],door["along"])
    assert sha(ROOT/row["glb"])==row["glb_sha256"] and sha(ROOT/row["native"])==row["native_sha256"]
    assert sha(ROOT/row["layout"])==read(ROOT/row["construction"])["layout_sha256"]
    verification = OUT/"verification"
    verification.mkdir(parents=True,exist_ok=True)
    gate = verification/"frame-input.tsv"
    rows = [(plan["portal_id"],door["id"],room_a["id"],room_b["id"],room_a["owner"],room_b["owner"],
             x,door["sill_y"],z,door["clear_width"],door["axis"],a,b,door["clear_height"],
             plan["leaves"],plan["hinge_side"],plan["swing_direction"],plan["leaf_source_sha256"])]
    rows.extend(("RITUAL",ritual["rite"].lower(),rooms[ritual["zone"]]["owner"],*ritual["point"])
                for ritual in layout["rituals"])
    gate.write_bytes("".join("\t".join(map(str,row))+"\n" for row in rows).encode())
    room_plan = FROM/"verification/ramp-runtime-input.tsv"
    assert room_plan.read_text().splitlines()[0].split("\t")[1:] == [
        row["glb"],row["glb_sha256"],row["native"],row["native_sha256"]]
    assert sha(ASSET)==plan["leaf_source_sha256"]
    classes = ROOT/"build/native-foyer-door"
    classes.mkdir(parents=True,exist_ok=True)
    java = Path("C:/Program Files/Java/jdk-17/bin")
    jar = ROOT/"build/beckenburg-game.jar"
    review = ROOT/"tools/java/NativeSchoolDoorReview.java"
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-Xprefer:source",
                    "-sourcepath",str(ROOT/"src/main/java"),"-cp",str(jar),
                    "-d",str(classes),str(review)],cwd=ROOT,check=True)
    cp = os.pathsep.join(map(str,(classes,ROOT/"src/main/resources",jar)))
    result = verification/"foyer-runtime-proof.tsv"
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx2g","-cp",cp,
                    "org.beckenburg.NativeSchoolDoorReview",str(gate),str(room_plan),str(result),str(CAPTURES)],
                   cwd=ROOT,check=True)
    engine = ROOT/"src/main/java/org/beckenburg"
    sources = [Path(__file__),review,ASSET,*[engine/n for n in ("AuthoredSitePhysics.java",
        "NativeInvestigationDoor.java","NativeInvestigationScene.java","HouseMap.java","SceneRenderer.java",
        "LanAcoustics.java","GlbProps.java")]]
    results = dict(line.split("\t",1) for line in result.read_text().splitlines()[1:])
    report = dict(status="VERIFIED_STAGED_FOYER_DOOR",source_packet=row,actual_frame=door,leaf_schedule=plan,
                  source_hashes={relative(p):sha(p) for p in sources},runtime_dependency_jar_sha256=sha(jar),
                  proof={relative(p):sha(p) for p in [result,gate,room_plan,*sorted(CAPTURES.glob("school-foyer-*.png"))]},
                  controls=results,
                  scope="Exact source School entrance on two staged HouseMap instances with host/guest snapshot round trips and CPU renderer captures. Network transport, other building leaves, services and full runtime promotion remain open.")
    write(verification/"school-foyer-runtime.json",report)


if __name__ == "__main__":
    main()
