"""Verify native game leaf triangles match the reopened editor poses."""
import argparse
from collections import defaultdict
from itertools import permutations, product
from math import floor
from pathlib import Path

TOLERANCE = .00005
CELL = .0001


def load(path):
    groups = defaultdict(list)
    for line in path.read_text(encoding="utf-8").splitlines():
        portal, state, *coords = line.split("\t")
        xyz = tuple(map(float, coords))
        groups[(portal, state)].append(tuple(xyz[i:i+3] for i in (0,3,6)))
    return groups


def key(t):
    return tuple(floor(sum(v[i] for v in t) / (3 * CELL)) for i in range(3))


def matches(a, b):
    return any(all(abs(x-y) < TOLERANCE for p,q in zip(a,candidate) for x,y in zip(p,q))
               for candidate in permutations(b))


def compare(runtime, authored, report):
    expected, actual = load(runtime), load(authored)
    assert expected.keys() == actual.keys()
    lines = ["portal\tpose\tmatching_triangles\ttolerance_m"]
    neighbors = tuple(product((-1,0,1), repeat=3))
    for pair, triangles in expected.items():
        bins = defaultdict(list)
        for triangle in actual[pair]:
            bins[key(triangle)].append(triangle)
        for triangle in triangles:
            x,y,z = key(triangle)
            for dx,dy,dz in neighbors:
                bucket = bins.get((x+dx,y+dy,z+dz), [])
                match = next((i for i,candidate in enumerate(bucket) if matches(triangle,candidate)), None)
                if match is not None:
                    bucket.pop(match)
                    break
            else:
                raise AssertionError((pair, triangle))
        assert all(not bucket for bucket in bins.values()), pair
        lines.append(f"{pair[0]}\t{pair[1]}\t{len(triangles)}\t{TOLERANCE}")
    report.write_text("\n".join(lines)+"\n", encoding="utf-8")
    print(f"PASS {len(expected)} fitted poses, {sum(map(len,expected.values()))} triangles")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    for name in ("runtime", "authored", "report"):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    compare(args.runtime,args.authored,args.report)
