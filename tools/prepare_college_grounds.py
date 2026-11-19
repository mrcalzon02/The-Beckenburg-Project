"""Sculpt the College's forecourt, quadrangle and building terraces as areas."""

import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'development/levels/rebuild'
PASS = BASE / 'college-ground-pass'
PROGRAM = PASS / 'program/college'
VAN = ROOT / 'build/level-verification/scenes/hollow-creek-canonical-van'
OLD_WORLD = BASE / 'terrain-overall-pass/worlds/college/terrain-world'
WORLD = PASS / 'worlds/college/terrain-world'
BASELINE = BASE / 'dam-spillway-gallery-pass/roster.json'

# Coordinates use the three building groups and campus boundaries. The first
# field owns the level van manoeuvring area. A planted north verge, the green
# and the widened rear approach are separate geographic fields.
FIELDS = [
    ('arrival forecourt', 'GRAVEL', 5, 'plane:0:0:0',
     [(24,104),(20,91),(23,80),(31,74),(43,74),(55,76),(57,82),(52,90),(41,95),(42,103)]),
    ('teaching south garden', 'GRASS', 4, 'plane:0:0:0',
     [(15,57),(29,58),(38,59),(42,65),(41,77),(31,80),(20,77),(15,67)]),
    ('central quadrangle', 'GRASS', 3, 'plane:0:0:0',
     [(33,25),(40,23),(43,28),(43,69),(44,76),(39,81),(35,75),(35,56),(34,39)]),
    ('library front green', 'GRASS', 5, 'plane:0:0:0',
     [(23,20),(36,19),(43,20),(45,26),(40,35),(35,39),(29,34),(24,30)]),
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tsv(path, rows):
    path.write_bytes(''.join('\t'.join(map(str, row)) + '\n' for row in rows).encode('utf-8'))


def main():
    PROGRAM.mkdir(parents=True, exist_ok=True)
    # The surveyed source has no sculpt tile. Drop only our generated tile
    # before copying it again so a revised footprint cannot inherit old strokes.
    previous_tile = Path('terrain-layers/tile_0_0.bin')
    if not (OLD_WORLD/previous_tile).exists():
        (WORLD/previous_tile).unlink(missing_ok=True)
    shutil.copytree(OLD_WORLD, WORLD, dirs_exist_ok=True)
    source = next(r for r in json.loads(BASELINE.read_text(encoding='utf-8'))['active_sources'] if r['id'] == 'COLLEGE')
    layout = json.loads((ROOT / source['layout']).read_text(encoding='utf-8'))
    protected = [z['bounds'] for z in layout['zones']
                 if z['y'] <= 0 and z['kind'] not in ('staging','outside')]
    tsv(PROGRAM/'features.tsv', ([name, material, shoulder, plane, *(v for p in poly for v in p)]
                                 for name,material,shoulder,plane,poly in FIELDS))
    tsv(PROGRAM/'protected-rooms.tsv', protected)
    tsv(PROGRAM/'underlays.tsv', [])

    out = ROOT/'build/college-grounds-java'
    out.mkdir(parents=True, exist_ok=True)
    jdk = Path(r'C:/Program Files/Java/jdk-17/bin')
    jar = ROOT/'build/beckenburg-game.jar'
    checks = ROOT/'src/test/java/org/beckenburg'
    writer = ROOT/'tools/java/RebuildTieredEarthworks.java'
    subprocess.run([str(jdk/'javac.exe'), '-cp', str(jar), '-d', str(out), str(writer),
                    str(checks/'LevelSceneSource.java'), str(checks/'HollowCreekVanSource.java')], cwd=ROOT, check=True)
    cp = str(out)+';'+str(jar)
    subprocess.run([str(jdk/'java.exe'), '-Xmx2g', '-cp', cp, 'RebuildTieredEarthworks',
                    str(PASS), 'COLLEGE', str(OLD_WORLD)], cwd=ROOT, check=True)
    subprocess.run([str(jdk/'java.exe'), '-Djava.awt.headless=true', '-cp', cp,
                    'org.beckenburg.HollowCreekVanSource'], cwd=ROOT, check=True)
    assert VAN.joinpath('geometry.json').is_file()

    receipt = json.loads((WORLD/'earthworks-verification.json').read_text(encoding='utf-8'))
    for name in ('tile_0_0.png','adaptive-topology.bin','authored-cuts.tsv'):
        assert sha(WORLD/name) == sha(OLD_WORLD/name), name
    assert receipt['disk_reopen_verified'] and sha(WORLD/'terrain-physics.hcs2') == receipt['physics_sha256']
    provenance = dict(source_world=OLD_WORLD.relative_to(ROOT).as_posix(),
                      features=(PROGRAM/'features.tsv').relative_to(ROOT).as_posix(),
                      features_sha256=sha(PROGRAM/'features.tsv'), protected_room_bounds=protected,
                      writer=writer.relative_to(ROOT).as_posix(), writer_sha256=sha(writer),
                      before_files={p.relative_to(OLD_WORLD).as_posix():sha(p) for p in OLD_WORLD.rglob('*')
                                    if p.is_file() and not p.name.endswith('.lock')},
                      van_triangle_source_sha256=sha(VAN/'geometry.json'),
                      published_terrain=receipt)
    (WORLD/'source-provenance.json').write_bytes((json.dumps(provenance,indent=2)+'\n').encode())
    (WORLD/'verification.json').write_bytes((WORLD/'earthworks-verification.json').read_bytes())


if __name__ == '__main__':
    main()
