"""Exercise the public MCP adapter from a separate official SDK client against the open GUI fixture."""
import asyncio
import base64
import hashlib
import json
import os
from pathlib import Path
import sys

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


async def verify(scope):
    adapter = os.environ.get("EDITOR_MCP_SCRIPT", str(Path(__file__).with_name("editor_mcp.py")))
    settings = StdioServerParameters(command=sys.executable, args=[adapter],
                                     env={key: os.environ[key] for key in ("EDITOR_AGENT_URL", "EDITOR_AGENT_TOKEN")})
    async with stdio_client(settings) as (reader, writer), ClientSession(reader, writer) as client:
        await client.initialize()
        tools = {tool.name: tool for tool in (await client.list_tools()).tools}
        assert ("edit" in tools) == (scope not in ("observe", "material"))
        assert ("save" in tools) == (scope in ("save", "material-save"))
        assert ("reopen" in tools) == (scope in ("save", "material-save"))
        assert ("playtest" in tools) == (scope in ("edit", "save"))
        assert ("check_walkability" in tools) == (scope in ("observe", "edit", "save"))
        assert ("inspect_properties" in tools) == (scope in ("observe", "edit", "save"))
        assert ("recent_actions" in tools) == (scope not in ("observe", "material"))
        assert set(tools["verify_position"].inputSchema["required"]) == {"revision", "id", "x", "y", "z"}

        async def call(name, **args):
            result = await client.call_tool(name, args)
            assert not result.isError, (name, result.content)
            assert len(result.content) == 1 and result.structuredContent is None
            return json.loads(result.content[0].text)

        state = await call("inspect_scene")
        revision = state["revision"]
        assert await call("inspect_scene", revision_only=True) == {"sceneId": state["sceneId"], "revision": revision}
        resource = await client.read_resource("editor://active/summary")
        assert json.loads(resource.contents[0].text) == state
        if scope in ("material", "material-edit", "material-save"):
            assert state["sceneId"] == os.environ["EDITOR_EXPECT_SCENE"]
            routes = set() if scope == "material" else {"/edit", "/undo", "/redo"}
            if scope == "material-save":
                routes.update(("/save", "/reopen"))
            assert set(state["writeRoutes"]) == routes
            selection = (await call("inspect_selection", revision=revision))["result"]
            assert selection["kind"] == "FACE" and selection["properties"]["opacity"] == "100"
            key = os.environ["EDITOR_EXPECT_VERTEX"]
            node = (await call("inspect_nodes", revision=revision, ids=[key]))["result"][0]
            assert node["id"] == key and node["kind"] == "VERTEX"
            checks = (await call("run_test", revision=revision, id="visible-material"))["result"]
            assert {row["code"] for row in checks} == {"BOUND_MATERIAL", "VISIBLE_FACE"} and all(row["passed"] for row in checks)
            captured = await client.call_tool("capture_viewport", {"revision": revision})
            assert not captured.isError and captured.content[0].mimeType == "image/png"
            assert hashlib.sha256(base64.b64decode(captured.content[0].data)).hexdigest() == os.environ["EDITOR_EXPECT_PNG"]
            if scope != "material":
                arguments = dict(face=int(selection["id"]), layer=selection["properties"]["layer"], opacity=40)
                change = dict(operation="set-layer-opacity", arguments=arguments)
                invalid = dict(operation="set-layer-opacity", arguments=dict(arguments, opacity=101))
                rejected = await client.call_tool("edit", {"revision": revision, "commands": [change, invalid]})
                assert rejected.isError
                revision = (await call("inspect_scene", revision_only=True))["revision"]
                assert (await call("inspect_selection", revision=revision))["result"] == selection
                edited = (await call("edit", revision=revision, commands=[change]))["revision"]
                assert (await call("inspect_selection", revision=edited))["result"]["properties"]["opacity"] == "40"
                undone = (await call("undo", revision=edited))["revision"]
                assert (await call("inspect_selection", revision=undone))["result"] == selection
                redone = (await call("redo", revision=undone))["revision"]
                assert (await call("inspect_selection", revision=redone))["result"]["properties"]["opacity"] == "40"
                if scope == "material-save":
                    await call("save", revision=redone)
                    xyz = {axis: node[axis] for axis in "xyz"}
                    assert (await call("verify_position", revision=redone, id=key, **xyz))["result"]["diskState"] == "MATCHES"
                    later = dict(operation="set-layer-opacity", arguments=dict(arguments, opacity=70))
                    later_rev = (await call("edit", revision=redone, commands=[later]))["revision"]
                    assert (await call("verify_position", revision=later_rev, id=key, **xyz))["result"]["diskState"] == "DIFFERS"
                    reopened = (await call("reopen", revision=later_rev))["revision"]
                    readback = (await call("inspect_selection", revision=reopened))["result"]
                    assert readback["id"] == selection["id"] and readback["properties"]["opacity"] == "40"
                    assert (await call("verify_position", revision=reopened, id=key, **xyz))["result"]["diskState"] == "MATCHES"
                print("PASS MCP material opacity: rejected batch restored the layer; edit, undo and redo read back")
            print("PASS Material Editor MCP: native vertex, visible face binding and exact displayed runtime PNG")
            return
        if scope == "portable":
            assert state["sceneId"] == "another-game" and "capture_viewport" not in tools
            assert state["operations"] == {"increment": {}}
            first = (await call("inspect_nodes", revision=revision, ids=["room"]))["result"][0]
            cursor = (await call("recent_actions", revision=revision))["result"]["lastEvent"]
            args = dict(revision=revision, sequence=1, commands=[{"operation": "increment", "arguments": {}}])
            edited = await call("edit", **args)
            assert await call("edit", **args) == edited
            audit = (await call("recent_actions", revision=edited["revision"], after=cursor))["result"]["events"]
            assert [e["outcome"] for e in audit] == ["applied", "replayed"]
            assert all(e["operations"] == ["increment"] and e["sequence"] == 1 for e in audit)
            proof = (await call("verify_position", revision=edited["revision"], id="room",
                                x=first["x"] + 1, y=first["y"], z=first["z"]))["result"]
            assert proof["verified"] and proof["diskState"] == "UNSUPPORTED"
            undone = await call("undo", revision=edited["revision"])
            assert (await call("inspect_nodes", revision=undone["revision"], ids=["room"]))["result"] == [first]
            print("PASS standalone JAR: MCP dispatches another project's typed edit, native readback and undo")
            return
        castle = state["sceneId"] == "beckenburg/castle"
        key = "start" if castle else "prop-0"
        node = (await call("inspect_nodes", revision=revision, ids=[key]))["result"][0]
        xyz = {axis: node[axis] for axis in "xyz"}
        props = (await call("inspect_properties", revision=revision, id=key))["result"]
        assert props["node"] == node and "yawDegrees" in props["fields"]
        proof = await call("verify_position", revision=revision, id=key, **xyz)
        assert proof["result"]["verified"]
        if scope == "observe":
            assert proof["result"]["committed"] and proof["result"]["diskState"] == "DIFFERS"
        footprint = (await call("check_walkability", revision=revision, **xyz))["result"]
        assert {axis: footprint[axis] for axis in "xyz"} == xyz and isinstance(footprint["walkable"], bool)
        captured = await client.call_tool("capture_viewport", {"revision": revision})
        assert not captured.isError and captured.content[0].type == "image" and captured.content[0].mimeType == "image/png"
        diagnostics = await call("run_test", revision=revision, id=state["tests"][0])
        assert diagnostics["revision"] == revision and diagnostics["result"]
        async def playtest(revision):
            before = await call("inspect_nodes", revision=revision, ids=[key])
            disk = await call("verify_position", revision=revision, id=key, **xyz)
            stale = await client.call_tool("playtest", {"revision": revision - 1})
            assert stale.isError and "STALE_REVISION" in stale.content[0].text
            played = await client.call_tool("playtest", {"revision": revision, "sequence": 30})
            assert not played.isError and len(played.content) == 1 and played.content[0].mimeType == "image/png", played.content[0].type
            png = base64.b64decode(played.content[0].data, validate=True)
            replay = await client.call_tool("playtest", {"revision": revision, "sequence": 30})
            assert not replay.isError and replay.content[0].data == played.content[0].data
            assert png[:8] == b"\x89PNG\r\n\x1a\n" and png != base64.b64decode(captured.content[0].data)
            assert tuple(int.from_bytes(png[i:i+4], "big") for i in (16, 20)) == (880, 550)
            assert before == await call("inspect_nodes", revision=revision, ids=[key])
            assert disk == await call("verify_position", revision=revision, id=key, **xyz)
            name = "castle" if castle else "house"
            (Path("build/editor-agent-evidence") / f"mcp-{name}-playtest.png").write_bytes(png)
            print(f"PASS {name} MCP playtest: live gameplay PNG, revision, position and disk state retained")
        if scope != "observe":
            schema = json.dumps(tools["edit"].inputSchema)
            assert all(name in schema for name in state["operations"])
            if castle:
                assert any(check["code"] == "SPAWN_WALL_INTERSECTION" for check in diagnostics["result"])
            xyz["x"] += .125
            operation = "set-spawn" if castle else "move-prop"
            arguments = {"x": xyz["x"], "z": xyz["z"]}
            arguments.update({"yaw": 0.125} if castle else {"id": key, "y": xyz["y"]})
            commands = [{"operation": operation, "arguments": arguments}]
            invalid = ({"operation": "remove-surface", "arguments": {"id": "absent-mcp-fixture"}} if castle else
                       {"operation": "move-prop", "arguments": dict(arguments, id="absent-mcp-fixture")})
            rejected = await client.call_tool("edit", {"revision": revision, "commands": [*commands, invalid]})
            assert rejected.isError
            revision = (await call("inspect_scene", revision_only=True))["revision"]
            assert (await call("inspect_nodes", revision=revision, ids=[key]))["result"] == [node]
            edit = await call("edit", revision=revision, commands=commands)
            stale = await client.call_tool("edit", {"revision": revision, "commands": commands})
            assert stale.isError
            revision = edit["revision"]
            assert await call("inspect_scene", revision_only=True) == {"sceneId": state["sceneId"], "revision": revision}
            matched = (await call("verify_position", revision=revision, id=key, **xyz))["result"]
            assert matched["verified"] and matched["committed"]
            if not castle:
                await playtest(revision)
            undone = await call("undo", revision=revision)
            assert (await call("inspect_nodes", revision=undone["revision"], ids=[key]))["result"] == [node]
            revision = (await call("redo", revision=undone["revision"]))["revision"]
            if scope == "save":
                bed = (await call("inspect_nodes", revision=revision, ids=["upper-bed-0"]))["result"][0]
                assert bed["kind"] == "PROP" and bed["y"] > 2
                assert (await call("focus_node", revision=revision, id=bed["id"]))["result"] == bed
                async def walk(rev, position):
                    return (await call("check_walkability", revision=rev, **position))["result"]["walkable"]
                spot = None
                for dx, dz in ((1.5, 0), (1.5, 1), (2, 0), (-1.5, 0)):
                    candidate = dict(x=bed["x"] + dx, y=bed["y"], z=bed["z"] + dz)
                    aisle = dict(candidate, z=candidate["z"] + .85)
                    if await walk(revision, candidate) and await walk(revision, aisle):
                        spot = candidate
                        break
                assert spot is not None, "No walkable upper lodging position and adjacent aisle"
                chair = "armchair_1"
                offset = 0
                while True:
                    page = (await call("assets", revision=revision, offset=offset, limit=64))["result"]
                    if any(a["uri"] == "assets://models/" + chair for a in page["assets"]):
                        break
                    offset += len(page["assets"])
                    assert offset < page["total"], "Required furnishing absent from native palette"
                prop_id = "mcp-upper-lodging-chair"
                place = {"operation": "place-prop", "arguments": dict(id=prop_id, mesh=chair, **spot)}
                duplicate = dict(operation="place-prop", arguments=dict(place["arguments"], id=bed["id"]))
                wrong = {"revision": revision, "commands": [place, duplicate], "sequence": 10}
                refused = await client.call_tool("edit", wrong)
                assert refused.isError
                revision = (await call("inspect_scene", revision_only=True))["revision"]
                repeated = await client.call_tool("edit", wrong)
                assert repeated.isError and repeated.content[0].text == refused.content[0].text
                assert (await call("inspect_scene", revision_only=True))["revision"] == revision
                assert await walk(revision, spot)
                transaction = dict(revision=revision, sequence=11, commands=[
                    {"operation": "remove-surface", "arguments": {"id": "mcp-test-divider"}}, place])
                result = await call("edit", **transaction)
                assert result == await call("edit", **transaction)
                mismatch = await client.call_tool("edit", dict(transaction, commands=[place]))
                assert mismatch.isError and "SEQUENCE_CONFLICT" in mismatch.content[0].text
                revision = result["revision"]
                audit = (await call("run_test", revision=revision, id="castle-scene-audit"))["result"]
                assert not any(check["code"] == "SPAWN_WALL_INTERSECTION" for check in audit)
                delta = (await call("changes", revision=revision))["result"]
                assert any(row["id"] == "mcp-test-divider" and row["kind"] == "REMOVED" for row in delta["nodes"])
                assert any(row["id"] == prop_id and row["kind"] == "ADDED" for row in delta["nodes"])
                placed = (await call("verify_position", revision=revision, id=prop_id, **spot))["result"]
                assert placed["verified"] and placed["committed"]
                assert not await walk(revision, spot) and await walk(revision, aisle)
                assert (await call("focus_node", revision=revision, id=prop_id))["result"] == placed["actual"]
                await playtest(revision)
                capture = await client.call_tool("capture_viewport", {"revision": revision})
                assert not capture.isError
                output = Path("build/editor-agent-evidence")
                (output / "mcp-castle-repair.png").write_bytes(base64.b64decode(capture.content[0].data, validate=True))
                (output / "mcp-castle-repair.json").write_text(json.dumps(
                    {"sceneId": state["sceneId"], "revision": revision, "sourceCommit": os.environ.get("GITHUB_SHA"),
                     "beforeAudit": diagnostics["result"], "change": delta, "audit": audit,
                     "furnished": placed, "clearAisle": aisle}, indent=2), encoding="utf-8")
                await call("save", revision=revision)
                assert (await call("verify_position", revision=revision, id=key, **xyz))["result"]["diskState"] == "MATCHES"
                # Undo the removal, reopen its saved repair, then undo that reopen.
                reverted = await call("undo", revision=revision)
                assert result == await call("edit", **transaction)
                assert await walk(reverted["revision"], spot)
                page = (await call("recent_actions", revision=reverted["revision"], limit=64))["result"]
                sequence = [e for e in page["events"] if e["sequence"] == 11]
                assert [e["outcome"] for e in sequence] == ["applied", "replayed", "sequence-conflict", "replayed"]
                assert sequence[0]["operations"] == ["remove-surface", "place-prop"]
                assert sequence[0]["digest"] == sequence[3]["digest"] != sequence[2]["digest"]
                assert sequence[3]["observedRevision"] == reverted["revision"]
                assert os.environ["EDITOR_AGENT_TOKEN"] not in json.dumps(page) and prop_id not in json.dumps(page)
                assert not (await call("recent_actions", revision=reverted["revision"], after=page["lastEvent"]))["result"]["events"]
                (output / "mcp-castle-actions.json").write_text(json.dumps(
                    {"sceneId": state["sceneId"], "revision": reverted["revision"], "sourceCommit": os.environ.get("GITHUB_SHA"),
                     "actions": page}, indent=2), encoding="utf-8")
                empty = await client.call_tool("capture_viewport", {"revision": reverted["revision"]})
                assert not empty.isError and empty.content[0].data != capture.content[0].data
                (output / "mcp-upper-room-before.png").write_bytes(base64.b64decode(empty.content[0].data, validate=True))
                dirty = await call("verify_position", revision=reverted["revision"], id=key, **xyz)
                assert dirty["result"]["diskState"] == "DIFFERS"
                reopened = await call("reopen", revision=reverted["revision"])
                saved = await call("verify_position", revision=reopened["revision"], id=key, **xyz)
                assert saved["result"]["diskState"] == "MATCHES"
                reopened_prop = (await call("verify_position", revision=reopened["revision"], id=prop_id, **spot))["result"]
                assert reopened_prop["verified"] and reopened_prop["diskState"] == "MATCHES"
                assert not await walk(reopened["revision"], spot) and await walk(reopened["revision"], aisle)
                again = await call("undo", revision=reopened["revision"])
                again_audit = (await call("run_test", revision=again["revision"], id="castle-scene-audit"))["result"]
                assert any(check["code"] == "SPAWN_WALL_INTERSECTION" for check in again_audit)
                # Restore the repaired state so the next actual human Undo restores the wall.
                restored = (await call("redo", revision=again["revision"]))["revision"]
                async def cellar(rev):
                    return (await call("run_test", revision=rev, id="castle-cellar-passage"))["result"]
                baseline = await cellar(restored)
                assert {c["code"] for c in baseline} == {"CELLAR_DESCENT", "CELLAR_ASCENT"} and all(c["passed"] for c in baseline)
                sockets = (await call("run_test", revision=restored, id="castle-door-sockets"))["result"]
                slot = "portal-v-3700-2200"
                assert any(c["passed"] and c["detail"].startswith(slot+" ") and c["detail"].endswith("mounted EMPTY") for c in sockets)
                portal = dict(x=37, y=-3, z=22)
                landing = dict(x=40, y=0, z=24.8)
                assert await walk(restored, portal) and await walk(restored, landing)
                nav = dict(operation="set-nav-cell", arguments=dict(x=40, z=24, state="BLOCKED"))
                door = dict(operation="set-door", arguments=dict(slot=slot, leaf="WOOD"))
                bad = dict(operation="set-door", arguments=dict(slot=slot, leaf="PORTCULLIS"))
                invalid = await client.call_tool("edit", dict(revision=restored, commands=[nav, bad]))
                assert invalid.isError
                rev = (await call("inspect_scene", revision_only=True))["revision"]
                assert await walk(rev, landing)
                trial = (await call("edit", revision=rev, commands=[door, nav], sequence=40))["revision"]
                blocked = await cellar(trial)
                assert not any(c["passed"] for c in blocked)
                assert not await walk(trial, portal) and not await walk(trial, landing)
                await call("save", revision=trial)
                undo = (await call("undo", revision=trial))["revision"]
                assert await walk(undo, portal) and await walk(undo, landing)
                reopening = (await call("reopen", revision=undo))["revision"]
                assert not await walk(reopening, portal) and not await walk(reopening, landing)
                final = (await call("undo", revision=reopening))["revision"]
                assert await walk(final, portal) and all(c["passed"] for c in await cellar(final))
                await call("save", revision=final)
                # Leave the furnishing transaction on top of undo history after the two reversible checks.
                hole = dict(operation="remove-surface", arguments=dict(id="cellar-stair-opening"))
                missing = (await call("edit", revision=final, commands=[hole]))["revision"]
                capped = await cellar(missing)
                assert not any(c["passed"] for c in capped)
                final = (await call("undo", revision=missing))["revision"]
                assert all(c["passed"] for c in await cellar(final))
                (output / "mcp-cellar-passage.json").write_text(json.dumps(
                    {"source": os.environ.get("GITHUB_SHA"), "baseline": baseline, "blockedLanding": blocked,
                     "missingOpening": capped, "restoredRevision": final}, indent=2), encoding="utf-8")
                async def properties(rev, native_id):
                    return (await call("inspect_properties", revision=rev, id=native_id))["result"]
                async def capture_at(rev, name):
                    captured = await client.call_tool("capture_viewport", {"revision": rev})
                    assert not captured.isError
                    pixels = base64.b64decode(captured.content[0].data, validate=True)
                    (output / name).write_bytes(pixels)
                    return pixels
                await call("focus_node", revision=final, id=prop_id)
                original_prop = await properties(final, prop_id)
                lamp_id = "mcp-upper-lodging-light"
                lamp = dict(operation="place-ceiling-lamp", arguments=dict(id=lamp_id,
                            x=spot["x"]+.65, floorY=spot["y"], z=spot["z"]+.4, radius=8, power=0))
                def field(kind, native_id, name, value):
                    return dict(operation=f"set-{kind}-field", arguments=dict(id=native_id, field=name, value=value))
                edits = [lamp, field("prop", prop_id, "SCALE", .92), field("prop", prop_id, "YAW_DEGREES", 30),
                         field("prop", prop_id, "COLLISION_Z", 1.1)]
                trial = (await call("edit", revision=final, commands=edits, sequence=50))["revision"]
                assert not await walk(trial, aisle)
                transformed = await properties(trial, prop_id)
                assert transformed["fields"]["scale"] == .92 and abs(transformed["fields"]["yawDegrees"]-30) < 1e-8
                assert transformed["fields"]["collisionZ"] == 1.1
                unlit = await capture_at(trial, "mcp-lamp-zero.png")
                bright = (await call("edit", revision=trial, commands=[field("lamp", lamp_id, "POWER", 8),
                              field("lamp", lamp_id, "RADIUS", 9)], sequence=51))["revision"]
                fixture = await properties(bright, lamp_id)
                assert fixture["fields"]["power"] == 8 and fixture["fields"]["radius"] == 9 and fixture["fields"]["mount"] == "CEILING"
                # The native model's top is fixed to the underside of the upper 6.8 m ceiling
                # (22 cm structural slab), not embedded 20 cm into the roof.
                assert abs(fixture["node"]["y"] - (spot["y"] + 3.4 - .22)) < 1e-8
                assert await capture_at(bright, "mcp-lamp-powered.png") != unlit
                removed = (await call("edit", revision=bright, commands=[
                    dict(operation="remove-lamp", arguments=dict(id=lamp_id)), dict(operation="remove-prop", arguments=dict(id=prop_id))]))["revision"]
                assert await walk(removed, spot)
                lit = (await call("undo", revision=removed))["revision"]
                assert await properties(lit, lamp_id) == fixture
                await call("save", revision=lit)
                reverted = (await call("undo", revision=lit))["revision"]
                assert (await properties(reverted, lamp_id))["fields"]["power"] == 0
                opened = (await call("reopen", revision=reverted))["revision"]
                assert await properties(opened, lamp_id) == fixture and await properties(opened, prop_id) == transformed
                reverted = (await call("undo", revision=opened))["revision"]
                final = (await call("undo", revision=reverted))["revision"]
                assert await properties(final, prop_id) == original_prop and await walk(final, aisle)
                await call("save", revision=final)
                (output / "mcp-lamp-properties.json").write_text(json.dumps(
                    {"source": os.environ.get("GITHUB_SHA"), "fixture": fixture, "transformedProp": transformed,
                     "restoredProp": original_prop, "restoredRevision": final}, indent=2), encoding="utf-8")
        print(f"PASS MCP {scope} {state['sceneId']}: authenticated typed discovery, live readback, resource, diagnostic and PNG")


if __name__ == "__main__":
    asyncio.run(verify(sys.argv[1]))
