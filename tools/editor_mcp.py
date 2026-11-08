"""MCP stdio adapter for the authenticated session opened in the human editor."""
import http.client
import json
import os
import re
from typing import Annotated, Literal, Union
from urllib.parse import urlencode

from mcp.server.fastmcp import FastMCP, Image
from pydantic import BaseModel, ConfigDict, Field, FiniteFloat, create_model


class LiveEditor:
    def __init__(self, address: str, token: str):
        match = re.fullmatch(r"http://127\.0\.0\.1:([1-9][0-9]{0,4})/?", address)
        if not match or not 1 <= int(match[1]) <= 65535:
            raise ValueError("Expected the editor's numeric loopback address")
        if not re.fullmatch(r"[A-Za-z0-9_-]{43}", token):
            raise ValueError("Expected a session token from the editor")
        self.port = int(match[1])
        self.token = token

    def call(self, route, fields=None, body=None):
        path = route + ("?" + urlencode(fields, doseq=True) if fields else "")
        conn = http.client.HTTPConnection("127.0.0.1", self.port, timeout=35)
        try:
            conn.request("POST" if body is not None else "GET", path, body=body,
                         headers={"Authorization": "Bearer " + self.token})
            reply = conn.getresponse()
            data = reply.read(4 * 1024 * 1024 + 1)
            if len(data) > 4 * 1024 * 1024:
                raise ValueError("Editor response exceeds limit")
            if reply.status != 200:
                raise ValueError(f"Editor HTTP {reply.status}: {json.loads(data)['error']}")
            if route == "/capture":
                if reply.headers.get_content_type() != "image/png" or not data.startswith(b"\x89PNG\r\n\x1a\n"):
                    raise ValueError("Invalid editor capture")
                return Image(data=data, format="png")
            return data.decode("utf-8")
        finally:
            conn.close()


def edit_type(operations):
    variants = []
    types = {"number": FiniteFloat, "integer": int, "string": str}
    for name, fields in sorted(operations.items()):
        parameters = {}
        for key, kind in fields.items():
            value_type = Literal[tuple(kind.split("|"))] if "|" in kind else types[kind]
            parameters[key] = (value_type, ...)
        args = create_model(name + "-arguments", __config__=ConfigDict(extra="forbid"), **parameters)
        variants.append(create_model(name, __config__=ConfigDict(extra="forbid"),
                                     operation=(Literal[name], ...), arguments=(args, ...)))
    return Union[tuple(variants)]


def encode_batch(commands: list[BaseModel]) -> bytes:
    fields = []
    for command in commands:
        if fields:
            fields.append("|")
        fields.append(command.operation)
        for key, raw in command.arguments.model_dump().items():
            value = str(raw)
            if not key or "=" in key or any(ch in key + value for ch in "\t\r\n"):
                raise ValueError("Invalid native argument")
            fields.append(key + "=" + value)
    data = "\t".join(fields).encode("utf-8")
    if len(data) > 16384:
        raise ValueError("Batch exceeds editor request limit")
    return data


def server(editor: LiveEditor):
    summary = json.loads(editor.call("/summary"))
    mcp = FastMCP("Editor Agent Service")

    native_tool = mcp.tool(structured_output=False)

    @mcp.resource("editor://active/summary", mime_type="application/json")
    def active_scene() -> str:
        """Current scene, revision, tests, edit operations and authorized routes."""
        return editor.call("/summary")

    @native_tool
    def inspect_scene(revision_only: bool = False) -> str:
        """Inspect the active scene. Use revision_only for its current ID and revision."""
        if not revision_only:
            return active_scene()
        if "/revision" in summary["routes"]:
            return editor.call("/revision")
        current = json.loads(active_scene())
        return json.dumps({key: current[key] for key in ("sceneId", "revision")}, separators=(",", ":"))

    @native_tool
    def inspect_nodes(revision: int, ids: Annotated[list[str], Field(min_length=1, max_length=64)]) -> str:
        """Read selected native hierarchy nodes in the supplied order."""
        return editor.call("/nodes", {"revision": revision, "id": ids})

    def page(route, revision, offset, limit):
        return editor.call(route, {"revision": revision, "offset": offset, "limit": limit})

    @native_tool
    def hierarchy(revision: int, offset: int = 0, limit: int = 32) -> str:
        """Read up to 64 hierarchy rows in editor order."""
        return page("/hierarchy", revision, offset, limit)

    @native_tool
    def changes(revision: int, offset: int = 0, limit: int = 32) -> str:
        """Read object changes recorded for the latest committed revision."""
        return page("/changes", revision, offset, limit)

    @native_tool
    def assets(revision: int, offset: int = 0, limit: int = 32) -> str:
        """Read the active editor palette with a catalog fingerprint."""
        return page("/assets", revision, offset, limit)

    @native_tool
    def verify_position(revision: int, id: str, x: float, y: float, z: float) -> str:
        """Compare expected coordinates with the native node and report disk agreement."""
        return editor.call("/verify-position", dict(revision=revision, id=id, x=x, y=y, z=z))

    if "/test" in summary["routes"]:
        @native_tool
        def run_test(revision: int, id: str) -> str:
            """Run one of the active editor's named diagnostics."""
            return editor.call("/test", {"revision": revision, "id": id})

    if "/capture" in summary["routes"]:
        @native_tool
        def capture_viewport(revision: int) -> Image:
            """Capture the active scene using the game renderer."""
            return editor.call("/capture", {"revision": revision})

    if "/edit" in summary["writeRoutes"] and summary["operations"]:
        command_type = edit_type(summary["operations"])
        @native_tool
        def edit(revision: int, commands: Annotated[list[command_type], Field(min_length=1, max_length=64)]) -> str:
            """Apply supported native commands in a single validated, undoable transaction."""
            return editor.call("/edit", {"revision": revision}, encode_batch(commands))

    if "/undo" in summary["writeRoutes"]:
        @native_tool
        def undo(revision: int) -> str:
            """Undo one shared editor history entry."""
            return editor.call("/undo", {"revision": revision}, b"")

    if "/redo" in summary["writeRoutes"]:
        @native_tool
        def redo(revision: int) -> str:
            """Redo one shared editor history entry."""
            return editor.call("/redo", {"revision": revision}, b"")

    if "/save" in summary["writeRoutes"]:
        @native_tool
        def save(revision: int) -> str:
            """Save the active document to its existing native path."""
            return editor.call("/save", {"revision": revision}, b"")
    if "/reopen" in summary["writeRoutes"]:
        @native_tool
        def reopen(revision: int) -> str:
            """Reopen this scene's saved document through shared undo history."""
            return editor.call("/reopen", {"revision": revision}, b"")
    return mcp


if __name__ == "__main__":
    server(LiveEditor(os.environ["EDITOR_AGENT_URL"], os.environ["EDITOR_AGENT_TOKEN"])).run(transport="stdio")
