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
        assert set(tools["verify_position"].inputSchema["required"]) == {"revision", "id", "x", "y", "z"}

        async def call(name, **args):
            result = await client.call_tool(name, args)
            assert not result.isError, (name, result.content)
            assert len(result.content) == 1 and result.structuredContent is None
            return json.loads(result.content[0].text)

        state = await call("inspect_scene")
        revision = state["revision"]
        resource = await client.read_resource("editor://active/summary")
        assert json.loads(resource.contents[0].text) == state
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
            edit = await call("edit", revision=revision, commands=commands)
            stale = await client.call_tool("edit", {"revision": revision, "commands": commands})
            assert stale.isError
            revision = edit["revision"]
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
        print(f"PASS MCP {scope} {state['sceneId']}: authenticated typed discovery, live readback, resource, diagnostic and PNG")


if __name__ == "__main__":
    asyncio.run(verify(sys.argv[1]))
