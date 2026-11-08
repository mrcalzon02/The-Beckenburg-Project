"""Compare standalone native mesh sessions with the actual Material Editor fixture."""
import hashlib
import json
import subprocess
import sys
import tempfile
from contextlib import contextmanager
from pathlib import Path

java, classpath, filename, expected_png, rear_face = sys.argv[1:]
document = Path(filename)
command = [java, "-cp", classpath, "org.beckenburg.EditorAgentCli", "MESH", str(document)]


@contextmanager
def session(*options):
    with subprocess.Popen(command + list(options), stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                          text=True, encoding="utf-8") as child:
        failed = False

        def request(*fields, ok=True):
            nonlocal failed
            child.stdin.write("\t".join(map(str, fields)) + "\n")
            child.stdin.flush()
            reply = json.loads(child.stdout.readline())
            assert reply["ok"] == ok, reply
            result = reply.get("result")
            if not ok or fields[0] == "test" and any(not check["passed"] for check in result["result"]):
                failed = True
            return result

        try:
            yield request
            child.stdin.close()
            assert child.wait(timeout=15) == int(failed)
        finally:
            if child.poll() is None:
                child.kill()


original = document.read_bytes()
with tempfile.TemporaryDirectory() as directory, session() as first:
    rev = first("revision")["revision"]
    selected = first("selection", rev)["result"]
    face, layer = selected["id"], selected["properties"]["layer"]
    assert selected["properties"]["opacity"] == "40"
    baseline = Path(directory) / "desktop-parity.png"
    assert first("capture", rev, baseline)["result"]["width"] == 280
    assert hashlib.sha256(baseline.read_bytes()).hexdigest() == expected_png
    first("capture", rev, baseline, ok=False)
    first("capture", rev, document, ok=False)
    assert document.read_bytes() == original
    checks = first("test", rev, "visible-material")["result"]
    assert {c["code"] for c in checks} == {"BOUND_MATERIAL", "VISIBLE_FACE"} and all(c["passed"] for c in checks)
    with session("--face", face) as second:
        other = second("revision")["revision"]
        args = ["set-layer-opacity", "face=" + face, "layer=" + layer]
        initial = rev
        edit = first("once-v1", 1, "edit", initial, *args, "opacity=75")
        assert first("once-v1", 1, "edit", initial, *args, "opacity=75") == edit
        first("once-v1", 1, "edit", initial, *args, "opacity=80", ok=False)
        rev = edit["revision"]
        assert first("revision")["revision"] == rev
        changed = Path(directory) / "edited.png"
        first("capture", rev, changed)
        assert changed.read_bytes() != baseline.read_bytes()
        first("save", rev)
        assert document.read_bytes() != original
        other = second("edit", other, *args, "opacity=50")["revision"]
        second("save", other, ok=False)
        other = second("reopen", other)["revision"]
        assert second("selection", other)["result"]["properties"]["opacity"] == "75"
        refused = ("once-v1", 2, "batch", rev, *args, "opacity=60", "|", *args, "opacity=101")
        first(*refused, ok=False)
        fresh = first("revision")["revision"]
        first(*refused, ok=False)
        assert first("revision")["revision"] == fresh
        first("selection", rev, ok=False)
        assert first("selection", fresh)["result"]["properties"]["opacity"] == "75"
        rev = first("undo", fresh)["revision"]
        assert first("selection", rev)["result"] == selected
        assert first("once-v1", 1, "edit", initial, *args, "opacity=75") == edit
        assert first("selection", rev)["result"] == selected
        rev = first("redo", rev)["revision"]
        assert first("selection", rev)["result"]["properties"]["opacity"] == "75"
        rev = first("undo", rev)["revision"]
        first("save", rev)
        other = second("reopen", other)["revision"]
        assert second("selection", other)["result"] == selected
    assert document.read_bytes() == original
    with session() as reopened:
        fresh = reopened("revision")["revision"]
        assert reopened("selection", fresh)["result"] == selected
        restored = Path(directory) / "reopened.png"
        reopened("capture", fresh, restored)
        assert restored.read_bytes() == baseline.read_bytes()
    with session("--face", rear_face) as covered:
        fresh = covered("revision")["revision"]
        assert covered("selection", fresh)["result"]["id"] == rear_face
        checks = {row["code"]: row["passed"] for row in covered("test", fresh, "visible-material")["result"]}
        assert checks == {"BOUND_MATERIAL": True, "VISIBLE_FACE": False}
    missing = Path(directory) / "absent.btop"
    failure = subprocess.run(command[:4] + ["MESH", str(missing)], input="", capture_output=True,
                             text=True, encoding="utf-8", timeout=15)
    assert failure.returncode == 1 and json.loads(failure.stdout)["phase"] == "open" and not missing.exists()
print("PASS native MESH stdio: exact desktop PNG, validated edit/undo/redo, concurrent save refusal, workspace and fresh reopen")
