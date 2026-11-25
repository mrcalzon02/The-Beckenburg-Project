#!/usr/bin/env python3
"""One bounded Ollama proposal exercised against the actual Beckenburg editor CLI."""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import queue
import shutil
import subprocess
import tempfile
import threading
import urllib.request

SITE = "SCHOOL"
INCLUDE = ("chair", "stool", "desk", "table", "hamper", "basket", "bench", "crate", "plant")
EXCLUDE = ("wall", "floor", "roof", "door", "window", "ceiling", "lamp", "light", "pipe", "rail", "toilet", "sink", "fixture")
OFFSETS = ((0.125, 0), (0, 0.125), (-0.125, 0), (0, -0.125))

def checked(response):
    if not response.get("ok"):
        raise RuntimeError("Native editor refusal: " + str(response.get("code")) + " " + str(response.get("error", ""))[:250])
    return response["result"]

class Editor:
    def __init__(self, java, classpath, home, error_log):
        self.process = subprocess.Popen([java, "-Djava.awt.headless=true", "-cp", classpath,
            "org.beckenburg.EditorAgentCli", SITE, str(home)],
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=error_log,
            text=True, encoding="utf-8", bufsize=1)
        self.inbound = queue.Queue(maxsize=2)
        def read():
            try:
                for line in self.process.stdout:
                    self.inbound.put(line)
            finally:
                self.inbound.put(None)
        threading.Thread(target=read, daemon=True).start()
    def ask(self, command):
        if "\r" in command or "\n" in command or len(command) > 15000:
            raise ValueError("Unbounded or multiline native request")
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        try:
            line = self.inbound.get(timeout=120)
        except queue.Empty as exc:
            raise TimeoutError("Native editor timed out") from exc
        if line is None:
            raise RuntimeError("Native editor exited without replying")
        return checked(json.loads(line))
    def close(self):
        if self.process.poll() is None:
            self.process.stdin.close()
            try:
                self.process.wait(timeout=12)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=8)

def make_choices(snapshot):
    props = snapshot["state"]["props"]
    ids = snapshot["state"]["ids"]
    nodes = snapshot["hierarchy"]
    if len(props) != len(ids) or len(ids) != len(nodes):
        raise ValueError("Native hierarchy/prop count mismatch")
    choices = []
    for ident, prop, node in zip(ids, props, nodes):
        if ident != node["id"]:
            raise ValueError("Native identity mismatch")
        mesh = prop["mesh"].lower()
        if (not any(s in mesh for s in INCLUDE) or any(s in mesh for s in EXCLUDE)
                or prop.get("support") not in ("floor", "surface", "ground")):
            continue
        if not all(math.isfinite(float(prop[k])) for k in ("x", "y", "z")):
            continue
        for dx, dz in OFFSETS:
            choices.append({"choice_id": "C%02d" % (len(choices) + 1),
                "id": ident, "mesh": prop["mesh"], "room": prop["room"],
                "before": [prop["x"], prop["y"], prop["z"]],
                "after": [round(prop["x"] + dx, 6), prop["y"], round(prop["z"] + dz, 6)]})
        if len(choices) >= 20:
            break
    return choices

def validate_plan(plan, choices):
    if not isinstance(plan, dict) or set(plan) != {"choice_id", "reason"}:
        raise ValueError("Model response must have only choice_id and reason")
    if not isinstance(plan["reason"], str) or not 5 <= len(plan["reason"]) <= 240:
        raise ValueError("Invalid design rationale")
    selected = next((choice for choice in choices if choice["choice_id"] == plan["choice_id"]), None)
    if selected is None or math.dist(selected["before"], selected["after"]) > 0.2:
        raise ValueError("Model tried to select an unapproved edit")
    return selected

def propose(manifest, snapshot, checks, choices, model):
    prompt = ("You are an assistant selecting one reversible edit in a REAL headless mission editor. "
        "This is Ravenshade School, NOT the unfinished WLD-14 Lighthouse. "
        "Select exactly one supplied choice_id. The controller will run the command and all tests; "
        "you cannot choose arbitrary IDs, coordinates, scripts, or tools. "
        "Reply ONLY as JSON with the two keys choice_id and reason (5-240 characters). "
        "No claims of map completion or fully fixed circulation.\n"
        "EDITOR MANIFEST:\n" + manifest[:8500] + "\nNATIVE SCENE STATE:\n" +
        json.dumps({"sceneId": snapshot["sceneId"], "revision": snapshot["revision"],
                    "operations": snapshot["operations"], "tests": snapshot["tests"],
                    "baseline": checks, "choices": choices}, separators=(",", ":")))
    payload = json.dumps({"model": model, "prompt": prompt, "stream": False, "format": "json",
                          "options": {"temperature": 0, "num_predict": 180, "num_ctx": 4096}}).encode("utf-8")
    req = urllib.request.Request("http://127.0.0.1:11434/api/generate", data=payload,
                                 headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=180) as resp:
        reply = json.load(resp)
    parsed = json.loads(reply["response"])
    return validate_plan(parsed, choices), parsed

def native_checks(editor, revision, test_id):
    return editor.ask("test\t%s\t%s" % (revision, test_id))["result"]

def new_failures(before, after):
    def counts(rows):
        result = {}
        for row in rows:
            if not row["passed"]:
                result[row["code"]] = result.get(row["code"], 0) + 1
        return result
    old, new = counts(before), counts(after)
    return {key: count for key, count in new.items() if count > old.get(key, 0)}

def file_hash(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def guard_self_test():
    fixture = {"state": {"ids": ["prop-0", "prop-1"], "props": [
        {"mesh": "school_chair", "room": 7, "x": 4., "y": 3., "z": 5., "support": "floor"},
        {"mesh": "school_wall", "room": 7, "x": 8., "y": 3., "z": 8., "support": "wall"}]},
        "hierarchy": [{"id": "prop-0"}, {"id": "prop-1"}]}
    picks = make_choices(fixture)
    assert len(picks) == 4
    assert validate_plan({"choice_id": "C01", "reason": "Nudge a movable floor chair."}, picks) == picks[0]
    for response in ({"choice_id": "C98", "reason": "Nonexistent item"},
                     {"choice_id": "C01", "reason": "Bad", "script": "rm -rf ."},
                     {"choice_id": "C01", "reason": "x"}):
        try:
            validate_plan(response, picks)
        except ValueError:
            pass
        else:
            raise AssertionError("Unsafe model response accepted")
    assert new_failures([{"code": "EXISTING", "passed": False}],
                        [{"code": "EXISTING", "passed": False}]) == {}
    assert new_failures([], [{"code": "NEW", "passed": False}]) == {"NEW": 1}
    print("PASS model plan validation and regression safeguards")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--java", default="java")
    parser.add_argument("--classpath", default=os.pathsep.join(["build/classes", "build/test"]))
    parser.add_argument("--model", default="qwen2.5:0.5b")
    parser.add_argument("--site", choices=[SITE], default=SITE)
    parser.add_argument("--manifest", default="docs/development/OLLAMA_EDITOR_AGENT_MANIFEST.md")
    parser.add_argument("--output", default="build/editor-agent-evidence/ollama-pilot")
    args = parser.parse_args()
    if args.self_test:
        guard_self_test()
        return
    output = Path(args.output).resolve()
    output.mkdir(parents=True, exist_ok=True)
    report = {"status": "STARTED", "site": SITE, "model": args.model,
              "source_commit": os.environ.get("GITHUB_SHA", "local"), "runtime_promoted": False}
    try:
        manifest = Path(args.manifest).read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory(prefix="beckenburg-editor-agent-") as dirname:
            home = Path(dirname)
            with (output / "editor-stderr.log").open("w", encoding="utf-8") as stderr:
                editor = Editor(args.java, args.classpath, home, stderr)
                try:
                    before = editor.ask("inspect")
                    revision = before["revision"]
                    if before["sceneId"] != "beckenburg/school" or "move-prop" not in before["operations"]:
                        raise RuntimeError("Unexpected editor scene or missing native edit capability")
                    parity = native_checks(editor, revision, "mission-runtime-parity")
                    if not all(item["passed"] for item in parity):
                        raise RuntimeError("Pre-existing native prop/collider parity failure")
                    baseline = native_checks(editor, revision, "mission-open-routes")
                    choices = make_choices(before)
                    if not choices:
                        raise RuntimeError("No native floor props eligible for the bounded pilot")
                    report.update({"scene_id": before["sceneId"], "initial_revision": revision,
                                   "baseline_routes": baseline, "candidate_count": len(choices)})
                    choice, plan = propose(manifest, before, baseline, choices, args.model)
                    report.update({"selected": choice, "ollama_proposal": plan})
                    identifier = choice["id"]
                    editor.ask(f"focus\t{revision}\t{identifier}")
                    first_png = output / "before.png"
                    editor.ask(f"capture\t{revision}\t{first_png}")
                    if not first_png.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                        raise RuntimeError("Before-capture not an actual PNG")
                    x, y, z = choice["after"]
                    result = editor.ask(f"once-v1\t1\tedit\t{revision}\tmove-prop\tid={identifier}\tx={x}\ty={y}\tz={z}")
                    changed_revision = result["revision"]
                    if changed_revision <= revision:
                        raise RuntimeError("Native transaction did not advance revision")
                    proof = editor.ask(f"verify-position\t{changed_revision}\t{identifier}\t{x}\t{y}\t{z}")["result"]
                    if not proof["verified"] or not proof["committed"]:
                        raise RuntimeError("Changed native position not independently verified")
                    altered = editor.ask("inspect")
                    index = before["state"]["ids"].index(identifier)
                    if [altered["state"]["props"][index][k] for k in ("x", "y", "z")] != choice["after"]:
                        raise RuntimeError("Changed native position mismatch")
                    if ([p for j, p in enumerate(altered["state"]["props"]) if j != index] !=
                            [p for j, p in enumerate(before["state"]["props"]) if j != index]):
                        raise RuntimeError("Unrelated native props changed")
                    after_parity = native_checks(editor, changed_revision, "mission-runtime-parity")
                    after_routes = native_checks(editor, changed_revision, "mission-open-routes")
                    failures = new_failures(baseline, after_routes)
                    report.update({"after_parity": after_parity, "after_routes": after_routes,
                                   "new_route_failures": failures})
                    if not all(item["passed"] for item in after_parity) or failures:
                        raise RuntimeError("Native parity or new circulation regression")
                    editor.ask(f"save\t{changed_revision}")
                    editor.ask(f"reopen\t{changed_revision}")
                    loaded = editor.ask("inspect")
                    reloaded_rev = loaded["revision"]
                    if loaded["state"] != altered["state"]:
                        raise RuntimeError("Native saved document changed across reopen")
                    persisted = editor.ask(f"verify-position\t{reloaded_rev}\t{identifier}\t{x}\t{y}\t{z}")["result"]
                    if not persisted["verified"] or persisted["diskState"] != "MATCHES":
                        raise RuntimeError("Native disk parity did not pass")
                    # Keep the exact same native renderer camera pose for before/after proof.
                    # Refocusing here follows the moved prop and contaminates the visual diff.
                    after_png = output / "after.png"
                    editor.ask(f"capture\t{reloaded_rev}\t{after_png}")
                    if not after_png.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"):
                        raise RuntimeError("After-capture not an actual PNG")
                    if file_hash(first_png) == file_hash(after_png):
                        raise RuntimeError("Native edit not visible in identical-camera renderer captures")
                    saved = home / ".beckenburg" / "editor" / "levels" / "school.bscn"
                    if not saved.is_file():
                        raise RuntimeError("Expected native BSCN document missing")
                    shutil.copy2(saved, output / "school-reviewed.bscn")
                    report.update({"status": "SAVED_REOPENED_ISOLATED", "final_revision": reloaded_rev,
                                   "camera_fixed_between_captures": True,
                                   "before_png_sha256": file_hash(first_png),
                                   "after_png_sha256": file_hash(after_png),
                                   "native_document_sha256": file_hash(output / "school-reviewed.bscn"),
                                   "disk_state": "MATCHES"})
                finally:
                    editor.close()
    except Exception as exc:
        report.update({"status": "BLOCKED_UNVERIFIED", "error": str(exc)[:700]})
        raise
    finally:
        (output / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(json.dumps({"status": report["status"], "selected": report.get("selected"), "error": report.get("error")}))

if __name__ == "__main__":
    main()
