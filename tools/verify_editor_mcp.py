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
            args = dict(revision=revision, sequence=1, commands=[{"operation": "increment", "arguments": {}}])
            edited = await call("edit", **args)
            assert await call("edit", **args) == edited
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
            assert stale.isError
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
                assert mismatch.isError and "different arguments" in mismatch.content[0].text
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
                await call("redo", revision=again["revision"])
        print(f"PASS MCP {scope} {state['sceneId']}: authenticated typed discovery, live readback, resource, diagnostic and PNG")


if __name__ == "__main__":
    asyncio.run(verify(sys.argv[1]))
