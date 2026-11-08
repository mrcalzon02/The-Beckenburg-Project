"""Exercise the public MCP adapter from a separate official SDK client against the open GUI fixture."""
import asyncio
import base64
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
        assert ("edit" in tools) == (scope != "observe")
        assert ("save" in tools) == (scope == "save")
        assert ("reopen" in tools) == (scope == "save")
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
        if scope == "portable":
            assert state["sceneId"] == "another-game" and "capture_viewport" not in tools
            assert state["operations"] == {"increment": {}}
            first = (await call("inspect_nodes", revision=revision, ids=["room"]))["result"][0]
            edited = await call("edit", revision=revision, commands=[{"operation": "increment", "arguments": {}}])
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
        captured = await client.call_tool("capture_viewport", {"revision": revision})
        assert not captured.isError and captured.content[0].type == "image" and captured.content[0].mimeType == "image/png"
        diagnostics = await call("run_test", revision=revision, id=state["tests"][0])
        assert diagnostics["revision"] == revision and diagnostics["result"]
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
            undone = await call("undo", revision=revision)
            assert (await call("inspect_nodes", revision=undone["revision"], ids=[key]))["result"] == [node]
            revision = (await call("redo", revision=undone["revision"]))["revision"]
            if scope == "save":
                result = await call("edit", revision=revision, commands=[
                    {"operation": "remove-surface", "arguments": {"id": "mcp-test-divider"}}])
                revision = result["revision"]
                audit = (await call("run_test", revision=revision, id="castle-scene-audit"))["result"]
                assert not any(check["code"] == "SPAWN_WALL_INTERSECTION" for check in audit)
                delta = (await call("changes", revision=revision))["result"]
                assert any(row["id"] == "mcp-test-divider" and row["kind"] == "REMOVED" for row in delta["nodes"])
                capture = await client.call_tool("capture_viewport", {"revision": revision})
                assert not capture.isError
                output = Path("build/editor-agent-evidence")
                (output / "mcp-castle-repair.png").write_bytes(base64.b64decode(capture.content[0].data, validate=True))
                (output / "mcp-castle-repair.json").write_text(json.dumps(
                    {"sceneId": state["sceneId"], "revision": revision, "sourceCommit": os.environ.get("GITHUB_SHA"),
                     "beforeAudit": diagnostics["result"], "removed": delta, "audit": audit}, indent=2), encoding="utf-8")
                await call("save", revision=revision)
                assert (await call("verify_position", revision=revision, id=key, **xyz))["result"]["diskState"] == "MATCHES"
                # Undo the removal, reopen its saved repair, then undo that reopen.
                reverted = await call("undo", revision=revision)
                dirty = await call("verify_position", revision=reverted["revision"], id=key, **xyz)
                assert dirty["result"]["diskState"] == "DIFFERS"
                reopened = await call("reopen", revision=reverted["revision"])
                saved = await call("verify_position", revision=reopened["revision"], id=key, **xyz)
                assert saved["result"]["diskState"] == "MATCHES"
                again = await call("undo", revision=reopened["revision"])
                again_audit = (await call("run_test", revision=again["revision"], id="castle-scene-audit"))["result"]
                assert any(check["code"] == "SPAWN_WALL_INTERSECTION" for check in again_audit)
                # Restore the repaired state so the next actual human Undo restores the wall.
                await call("redo", revision=again["revision"])
        print(f"PASS MCP {scope} {state['sceneId']}: authenticated typed discovery, live readback, resource, diagnostic and PNG")


if __name__ == "__main__":
    asyncio.run(verify(sys.argv[1]))
