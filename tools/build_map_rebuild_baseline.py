"""Inventory the real rebuild inputs and capture coverage without granting acceptance.

Source hashes make missing/stale references visible. Planning dimensions, graph
routes and runtime camera availability are kept separate from physical acceptance.
Run after baseline capture checkpoints; --check verifies the committed inventory.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEVELS = ROOT / 'development/levels'
OUT = LEVELS / 'rebuild'


def relative(path):
    return path.relative_to(ROOT).as_posix()


def fingerprint(path):
    data = path.read_bytes()
    return {'path': relative(path), 'bytes': len(data),
            'sha256': hashlib.sha256(data).hexdigest()}


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def runtime_folder(folder):
    return {'wld-01': 'blackpine', 'wld-02': 'hollow_creek'}.get(folder, folder)


def build():
    catalog_path = LEVELS / 'edge-sky-plans/catalog.json'
    catalog = read(catalog_path)
    outputs = {}
    records = []
    for entry in catalog['maps']:
        folder = entry['folder']
        layout_path = LEVELS / ('layout-expansions' if entry['expansion'] else 'layouts') / folder / 'layout.json'
        layout = read(layout_path)
        if layout['id'] != entry['id']:
            raise ValueError('Wrong owning layout: ' + relative(layout_path))
        candidates = [layout_path,
            LEVELS / 'blockouts' / folder / 'implementation.json',
            LEVELS / 'blockouts' / folder / 'NEXT.md',
            LEVELS / 'blockouts' / folder / 'rough-shell.blend',
            LEVELS / 'blockouts' / folder / 'rough-shell.glb',
            LEVELS / 'blockouts' / folder / 'preview.png',
            LEVELS / 'model-requirements' / (folder + '.json'),
            LEVELS / 'edge-sky-plans' / folder / 'plan.json',
            LEVELS / 'power-plans' / (folder + '.json')]
        native_sources = {
            'house': ['blender/house-complete.blend'],
            'farmhouse': ['blender/farmhouse-complete.blend'],
            'school': ['blender/school-complete.blend'],
            'castle': ['blender/castle-rebuild-complete.blend', 'CASTLE_REBUILD_WORKFLOW.md'],
            'wld-01': ['blender/blackpine-worklights.blend', 'blender/blackpine-mill-expanded.blend'],
            'wld-02': ['hollow-creek/hollow-creek-route-grade.blend', 'hollow-creek/route-grade.json',
                       'hollow-creek/hollow-creek-green-grass.blend', 'hollow-creek/green-grass.json',
                       'DEVELOPMENT_HANDOFF_2026-10-25_WATER_RUNTIME.md']}
        candidates += [LEVELS / name for name in native_sources.get(folder, [])]
        sources = [fingerprint(p) for p in candidates if p.is_file()]
        runtime = OUT / 'captures/baseline' / runtime_folder(folder)
        matrix = runtime / ('castle-rooms.csv' if folder == 'castle' else 'room-matrix.csv')
        capture = {'kind': 'NO_RUNTIME_CAPTURE', 'matrix': None, 'rooms': 0,
                   'directional_views': 0, 'missing_views': 0, 'displaced_cameras': 0,
                   'manual_acceptance': 'NOT_ACCEPTED'}
        if matrix.is_file():
            rows = list(csv.DictReader(matrix.open(encoding='utf-8')))
            capture.update(kind='RUNTIME_RENDERER_DIAGNOSTIC', matrix=fingerprint(matrix), rooms=len(rows))
            for row in rows:
                for direction in ['north', 'east', 'south', 'west']:
                    image = runtime / row.get(direction, 'MISSING')
                    capture['directional_views' if image.is_file() and image.suffix == '.png' else 'missing_views'] += 1
                if row.get('status') == 'CENTER_BLOCKED_FALLBACK':
                    capture['displaced_cameras'] += 1
            essays = sorted(runtime.glob('*/room-essay.txt'))
            if essays:
                capture['authored_scene_identity'] = next(
                    (line for line in essays[0].read_text(encoding='utf-8').splitlines()
                     if line.startswith('Site: ')), 'UNRECORDED')
        missing_sources = [relative(p) for p in candidates if not p.is_file()]
        record = {'id': entry['id'], 'title': entry['title'], 'folder': folder,
                  'expansion': entry['expansion'], 'source_inventory': sources,
                  'absent_optional_or_unimplemented_inputs': missing_sources,
                  'planning_spaces': len(layout['zones']),
                  'planning_elevations_m': sorted({z['y'] for z in layout['zones']}),
                  'generic_purpose_reservations': [z['id'] for z in layout['zones']
                      if z.get('purpose') == 'Investigation and ordinary objects'],
                  'runtime_capture': capture,
                  'stage_decision': 'IN_PROGRESS',
                  'next_action': 'Inspect sources/captures and finish function program; no automatic architecture approval.'}
        records.append(record)
        lines = [f"# {entry['title']} — rebuild baseline", '',
                 f"Packet: `{entry['id']}`. **S00 IN_PROGRESS; no construction stage accepted.**", '',
                 '## Preserved source inventory', '', '| Source | Bytes | SHA-256 |', '| --- | ---: | --- |']
        for source in sources:
            lines.append(f"| `{source['path']}` | {source['bytes']} | `{source['sha256']}` |")
        lines += ['', '## Source planning reservations', '',
                  'These are existing layout reservations, not measurements of accepted runtime rooms.', '',
                  '| Space | Purpose in existing source | X/Z bounds (m) | Standing Y / clear height (m) |',
                  '| --- | --- | --- | --- |']
        for zone in layout['zones']:
            lines.append(f"| {zone['id']}: {zone['name']} | {zone.get('purpose', 'Unspecified')} | {zone['bounds']} | {zone['y']} / {zone['height']} |")
        lines += ['', '## Actual capture coverage', '',
                  f"Kind: **{capture['kind']}**. Rooms: {capture['rooms']}; directional views: {capture['directional_views']}; missing views: {capture['missing_views']}; displaced cameras: {capture['displaced_cameras']}.", '',
                  'Source-only packets need native source inspection and later actual runtime integration. '
                  'A complete camera matrix proves capture coverage, not a structurally coherent place, swept access, gameplay or hardware performance.', '',
                  '## Next acceptance work', '',
                  'Inspect every room/child space, resolve generic function descriptions, compare against the site work package, '
                  'record retain/refit/replace decisions and attach physical route/fixture evidence. '
                  'Consult the complete fitness checklist and preserve the canonical van contract.']
        if missing_sources:
            lines += ['', 'Absent candidate inputs (not all apply to every site):', ''] + [f'- `{p}`' for p in missing_sources]
        outputs[OUT / folder / 'baseline.md'] = '\n'.join(lines) + '\n'
    runtime_sources = ['LevelPlans.java', 'LevelDefinition.java', 'CastleHubMap.java',
                       'CastleRelations.java', 'DomesticFitout.java', 'LevelDressing.java',
                       'BlackpineLevel.java', 'HollowCreekLevel.java']
    document = {'schema_version': 1, 'roster_source': fingerprint(catalog_path),
                'baseline_main_sha': 'fba22b2a3eb56871eeda009f7cd0ab8c4ad4aa5c',
                'current_runtime_source_fingerprints': [fingerprint(ROOT / 'src/main/java/org/beckenburg' / name)
                                                       for name in runtime_sources],
                'capture_provenance_note': 'Built-in investigation geometry captured from the synchronized baseline; '
                    'diagnostic camera corrections do not alter geometry. Castle after-resident-fix captures are separate; '
                    'current source fingerprints include the resident-placement correction. Existing native shell previews '
                    'are historical source studies, especially Hollow Creek, and do not replace its newer native/runtime authority.',
                'scope': {'base_locations': catalog['base_maps'], 'annexes': catalog['additive_expansions']},
                'acceptance_policy': 'Inventory and capture coverage only; never auto-accept a map or stage.',
                'sites': records}
    outputs[OUT / 'source-baseline.json'] = json.dumps(document, ensure_ascii=False, indent=2) + '\n'
    return outputs, records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    outputs, records = build()
    for path, content in outputs.items():
        if args.check:
            if not path.is_file() or path.read_text(encoding='utf-8') != content:
                raise SystemExit('Stale rebuild baseline: ' + relative(path))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content.encode('utf-8'))
    print(f"PASS: {len(records)} location inventories; "
          f"{sum(s['runtime_capture']['directional_views'] for s in records)} actual directional views; "
          'no automatic acceptance. ' + ('Current.' if args.check else 'Written.'))


if __name__ == '__main__':
    main()
