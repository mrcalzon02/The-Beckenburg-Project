"""Fast schema regression for the actual MCP command manifest, no live editor needed."""
import json

from pydantic import TypeAdapter, ValidationError

from editor_mcp import edit_type


def main():
    operations = {
        "rebuild-house": {"baseline": "ORIGINAL"},
        "set-door": {"slot": "string", "leaf": "EMPTY|WOOD|PORTCULLIS"},
        "move-prop": {"id": "string", "x": "number", "y": "number", "z": "number"},
    }
    typed = TypeAdapter(edit_type(operations))
    samples = [
        {"operation": "rebuild-house", "arguments": {"baseline": "ORIGINAL"}},
        {"operation": "set-door", "arguments": {"slot": "test", "leaf": "WOOD"}},
        {"operation": "move-prop", "arguments": {"id": "chair-1", "x": 1.25, "y": 0, "z": 4.5}},
    ]
    for sample in samples:
        parsed = typed.validate_python(sample)
        assert parsed.operation == sample["operation"]
        assert parsed.arguments.model_dump() == sample["arguments"]
    for invalid in (
        {"operation": "rebuild-house", "arguments": {"baseline": "OTHER"}},
        {"operation": "rebuild-house", "arguments": {"baseline": "ORIGINAL", "script": "execute"}},
        {"operation": "set-door", "arguments": {"slot": "test", "leaf": "NOT_A_LEAF"}},
        {"operation": "move-prop", "arguments": {"id": "chair-1", "x": float("nan"), "y": 0, "z": 4.5}},
    ):
        try:
            typed.validate_python(invalid)
        except ValidationError:
            pass
        else:
            raise AssertionError("Invalid native operation accepted: " + repr(invalid))
    try:
        edit_type({"bad": {"value": "numbre"}})
    except ValueError:
        pass
    else:
        raise AssertionError("Typo in primitive type silently accepted")
    schema = json.dumps(typed.json_schema())
    assert "ORIGINAL" in schema and "PORTCULLIS" in schema
    print("PASS native MCP schema: singleton ORIGINAL, multi-value enum, finite types, exact fields and invalid-command rejection")


if __name__ == "__main__":
    main()
