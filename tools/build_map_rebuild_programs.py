"""Build per-site function/route worksheets from authored requirements and pinned layouts.

Inherited dimensions and polyline routes remain explicitly unaccepted. This tool
does not turn a planning graph into collision geometry or promote rebuild gates.
"""
import argparse
import csv
import hashlib
import io
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'development/levels/rebuild'


def table(fields, rows):
    stream = io.StringIO(newline='')
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator='\n')
    writer.writeheader()
    writer.writerows(rows)
    return stream.getvalue()


def build():
    programs = json.loads((OUT / 'function-programs.json').read_text(encoding='utf-8'))
    catalog = json.loads((ROOT / 'development/levels/edge-sky-plans/catalog.json').read_text(encoding='utf-8'))
    expected = {site['id']: site['folder'] for site in catalog['maps']}
    actual = {site['id']: site['folder'] for site in programs['sites']}
    if actual != expected or len(actual) != len(programs['sites']):
        raise ValueError('Function programs do not match the unique full roster')
    outputs = {}
    spaces_count = routes_count = 0
    for site in programs['sites']:
        source = ROOT / site['source_layout']
        raw = source.read_bytes()
        if hashlib.sha256(raw).hexdigest() != site['source_layout_sha256']:
            raise ValueError('Layout changed; review the function mapping: ' + site['id'])
        layout = json.loads(raw)
        zones = {zone['id']: zone for zone in layout['zones']}
        functions = {space['id']: space for space in site['spaces']}
        if set(zones) != set(functions) or len(functions) != len(site['spaces']):
            raise ValueError('Incomplete/duplicate space functions: ' + site['id'])
        children = site['required_child_spaces_or_task_zones']
        if not site['normal_use_design_capacity'].strip() or not children:
            raise ValueError('Missing capacity/support program: ' + site['id'])
        if any(child['parent_space'] not in zones for child in children):
            raise ValueError('Child requirement belongs to an unknown space: ' + site['id'])
        brief = [f"# {layout['title']} — first-principles function brief", '',
                 '**S01 IN_PROGRESS.** These are authored design requirements, not completed construction.', '',
                 '## Place and normal use', '', layout['intent'], '',
                 site['normal_use_design_capacity'], '', site['capacity_note'], '',
                 '## Privacy, services and circulation', '', site['privacy_and_service_rule'], '',
                 'The adjacent `space-program.csv` assigns a specific everyday activity to every named space. '
                 '`routes.csv` preserves the existing measured route reservations for review; lengths and walking '
                 'estimates are planning quantities, not swept movement results.', '',
                 '## Required subordinate spaces and working zones', '',
                 'Inspect existing facilities first. Each row is a requirement to resolve in the rough plan, '
                 'not a declaration that the source currently lacks the facility.', '',
                 '| Parent reservation | Required child space / task zone | Decision |',
                 '| --- | --- | --- |']
        for child in children:
            brief.append(f"| {child['parent_space']} | {child['requirement']} | {child['status']} |")
        brief += ['', '## Dimension and construction decisions', '',
                  'Review inherited room size against actual task furniture, capacity, source-mesh bounds, '
                  'door swings, window privacy, stair headroom, service access and the ritual/hiding contracts. '
                  'No inherited rectangle is accepted merely because it already has a name. Real child rooms '
                  'need physical walls/openings and an explicit mapping into stable gameplay zones.', '',
                  '## Reference and acceptance status', '',
                  'Preserve the approved concept/lore and consult `ARCHITECTURAL_REFERENCES.md`; '
                  'per-location measured reference inspection and final capacity/dimension acceptance are still required. '
                  'Use the [site work package](../../FIRST_PRINCIPLES_SITE_PACKAGES.md) and the existing baseline record '
                  'to distinguish retained assets, changes and missing runtime integration. This brief grants no map unlock.', '',
                  f"Pinned existing layout: `{site['source_layout']}`; SHA-256 `{site['source_layout_sha256']}`."]
        outputs[OUT / site['folder'] / 'brief.md'] = '\n'.join(brief) + '\n'
        rows = []
        for key, zone in zones.items():
            function = functions[key]
            if function['source_name'] != zone['name'] or not function['intended_activity'].strip():
                raise ValueError('Wrong/empty function: ' + site['id'] + '/' + key)
            x0, z0, x1, z1 = zone['bounds']
            neighbors = sorted({link['b'] if link['a'] == key else link['a']
                                for link in layout['links'] if key in (link['a'], link['b'])})
            rows.append(dict(space_id=key, source_name=zone['name'],
                             intended_activity=function['intended_activity'],
                             inherited_width_m=x1-x0, inherited_depth_m=z1-z0,
                             inherited_standing_y_m=zone['y'], inherited_clear_height_m=zone['height'],
                             source_adjacencies=';'.join(neighbors),
                             dimension_decision='REVIEW_IN_S02',
                             physical_acceptance='NOT_ESTABLISHED'))
        space_fields = ['space_id', 'source_name', 'intended_activity', 'inherited_width_m',
                        'inherited_depth_m', 'inherited_standing_y_m', 'inherited_clear_height_m',
                        'source_adjacencies', 'dimension_decision', 'physical_acceptance']
        outputs[OUT / site['folder'] / 'space-program.csv'] = table(space_fields, rows)
        route_rows = []
        for link in layout['links']:
            points = link['points']
            length = sum(math.dist(a, b) for a, b in zip(points, points[1:]))
            route_rows.append(dict(route_id=link['id'], source_a=link['a'], source_b=link['b'],
                                   source_kind=link['kind'], reserved_clear_width_m=link['width'],
                                   source_polyline_length_m=round(length, 3),
                                   predicted_walk_seconds_at_1_4_mps=round(length/1.4, 2),
                                   source_note=link.get('note', ''),
                                   physical_acceptance='NOT_PHYSICALLY_VERIFIED'))
        outputs[OUT / site['folder'] / 'routes.csv'] = table(
            ['route_id', 'source_a', 'source_b', 'source_kind', 'reserved_clear_width_m',
             'source_polyline_length_m', 'predicted_walk_seconds_at_1_4_mps', 'source_note',
             'physical_acceptance'], route_rows)
        spaces_count += len(rows)
        routes_count += len(route_rows)
    return outputs, spaces_count, routes_count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    outputs, spaces, routes = build()
    for path, content in outputs.items():
        if args.check:
            if not path.is_file() or path.read_text(encoding='utf-8') != content:
                raise SystemExit('Stale rebuild program: ' + str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content.encode('utf-8'))
    print(f'PASS: 29 packets / {spaces} authored space functions / {routes} source route reservations; '
          'dimensions and physical access remain unaccepted.')


if __name__ == '__main__':
    main()
