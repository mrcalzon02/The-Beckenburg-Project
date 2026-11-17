"""Recover the Core30 worker archive from a GitHub Actions job log."""
import argparse
import base64
import hashlib
import io
from pathlib import Path
import re
import zipfile


def recover(log_path):
    header = re.compile(r"\bBECK_ARCHIVE (\d+) ([0-9a-f]{64})\s*$")
    chunk = re.compile(r"\bBECK_DATA (\d+) ([a-zA-Z0-9+/=]+)\s*$")
    manifests = set()
    parts = {}
    for line in log_path.read_text(encoding="utf-8-sig").splitlines():
        if match := header.search(line):
            manifests.add((int(match[1]), match[2]))
        elif match := chunk.search(line):
            offset = int(match[1])
            data = base64.b64decode(match[2], validate=True)
            if parts.setdefault(offset, data) != data:
                raise ValueError(f"Conflicting archive data at offset {offset}")
    if len(manifests) != 1:
        raise ValueError("Expected one distinct archive in the completed job log")
    expected_size, expected_hash = manifests.pop()
    result = bytearray()
    for offset, data in sorted(parts.items()):
        if offset != len(result):
            raise ValueError(f"Missing archive data at offset {len(result)}")
        result.extend(data)
    if len(result) != expected_size or hashlib.sha256(result).hexdigest() != expected_hash:
        raise ValueError("Archive size or SHA-256 mismatch")
    with zipfile.ZipFile(io.BytesIO(result)) as archive:
        if archive.testzip() is not None:
            raise ValueError("ZIP checksum mismatch")
    return result, expected_hash


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    payload, digest = recover(args.log)
    args.output.write_bytes(payload)
    print(f"{digest}  {args.output}")
