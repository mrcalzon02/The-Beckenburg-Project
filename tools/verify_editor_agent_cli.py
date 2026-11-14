"""Exercise the real headless Java host over its public stdin/stdout protocol."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--java', default='java')
parser.add_argument('--classpath', default='build/classes;build/test')
parser.add_argument('--output', default='build/editor-agent-evidence')
parser.add_argument('--measure-tokens', action='store_true', help='Also count response text with tiktoken o200k_base')
args = parser.parse_args()
tokenizer = None
if args.measure_tokens:
    import tiktoken
    tokenizer = tiktoken.get_encoding('o200k_base')
output = Path(args.output).resolve()
output.mkdir(parents=True, exist_ok=True)

with tempfile.TemporaryDirectory(prefix='editor-agent-cli-') as home:
    command = [args.java, '-Dfile.encoding=windows-1252', '-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory',
               '-cp', args.classpath, 'org.beckenburg.EditorAgentCli', 'HOUSE', home]

    def run(lines, expected=0, recover=False, sizes=None, token_counts=None):
        result = subprocess.run(command + (['--recover-previous'] if recover else []), input='\n'.join(lines) + '\n', text=True,
                                encoding='utf-8', capture_output=True, timeout=120)
        assert result.returncode == expected, (result.returncode, result.stderr[-4000:], result.stdout[-1000:])
        if sizes is not None:
            sizes.extend(len(line.encode('utf-8')) for line in result.stdout.splitlines())
        if token_counts is not None and tokenizer is not None:
            token_counts.extend(len(tokenizer.encode_ordinary(line)) for line in result.stdout.splitlines())
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        assert len(rows) == len(lines), (len(rows), len(lines))
        return rows

    unrelated_castle = Path(home) / '.beckenburg' / 'editor' / 'castle.properties'
    unrelated_castle.parent.mkdir(parents=True, exist_ok=True)
    unrelated_castle.write_text('format=invalid', encoding='utf-8')
    first = run(['inspect'])[0]['result']
    assert 'move-prop' in first['operations'] and 'CAPTURE' in first['capabilities']
    # Fresh House now includes assemblies with attached services/story. Move
    # independent refrigeration rather than trying to detach the first counter.
    index = next(i for i, prop in enumerate(first['state']['props']) if prop['mesh'] == 'level_house_fridge_worn')
    prop = first['state']['props'][index]
    prop_id = first['state']['ids'][index]
    moved_x = prop['x'] + .125
    revision = first['revision']
    def verify_local_reads(full):
        rev = full['revision']
        node = full['hierarchy'][0]
        sizes = []
        observed, summary, page, one = [row['result'] for row in run([
            'inspect', 'summary', f'hierarchy\t{rev}\t0\t1', f'node\t{rev}\t{node["id"]}'
        ], sizes=sizes)]
        assert observed['hierarchy'] == full['hierarchy']
        assert summary['revision'] == rev and summary['sceneId'] == full['sceneId']
        assert 'state' not in summary and 'hierarchy' not in summary
        assert page['result'] == {'total': len(full['hierarchy']), 'offset': 0, 'nodes': [node]}
        assert one['result'] == node and one['revision'] == rev
        total = len(full['hierarchy'])
        catalog = run([f'assets\t{rev}\t0\t1'])[0]['result']['result']
        assert catalog['total'] > 0 and len(catalog['assets']) == 1
        asset_offsets = range(0, catalog['total'], 64)
        asset_pages = run([f'assets\t{rev}\t{offset}\t64' for offset in asset_offsets])
        entries = []
        for offset, reply in zip(asset_offsets, asset_pages):
            page = reply['result']['result']
            assert page['catalogFingerprint'] == catalog['catalogFingerprint'] and page['offset'] == offset
            entries.extend(page['assets'])
        assert len(entries) == catalog['total'] and entries[0] == catalog['assets'][0]
        if full['sceneId'] == 'beckenburg/house':
            assert all(asset['category'] == 'Models' for asset in entries)
        else:
            assert any(asset['uri'] == 'water://surface' for asset in entries)
        offsets = [*range(0, total, 17), total]
        pages = run([f'hierarchy\t{rev}\t{offset}\t17' for offset in offsets])
        for offset, reply in zip(offsets, pages):
            data = reply['result']['result']
            assert data == {'total': total, 'offset': offset, 'nodes': full['hierarchy'][offset:offset + 17]}
        first_by_id = {}
        for row in full['hierarchy']:
            first_by_id.setdefault(row['id'], row)
        direct = run([f'node\t{rev}\t{node_id}' for node_id in first_by_id])
        assert [reply['result']['result'] for reply in direct] == list(first_by_id.values())
        selection = list(reversed(first_by_id))[:64]
        request = f'nodes\t{rev}\t' + '\t'.join(selection)
        lengths = []
        combined = run([request], sizes=lengths)[0]['result']
        assert combined['revision'] == rev and combined['result'] == [first_by_id[key] for key in selection]
        individual = []
        run([f'node\t{rev}\t{key}' for key in selection], sizes=individual)
        assert lengths[0] < sum(individual)
        print(f'{full["sceneId"]}: {len(selection)} localized reads: {sum(individual)} bytes individually, {lengths[0]} bytes batched')
        invalid_selections = run([f'nodes\t{rev}', f'nodes\t{rev}\t{selection[0]}\tmissing-node', f'nodes\t{rev - 1}\t{selection[0]}',
                                  f'nodes\t{rev}\t' + '\t'.join([selection[0]] * 65)], expected=1)
        assert all(not row['ok'] for row in invalid_selections)
        invalid = run([f'node\t{rev-1}\t{node["id"]}', f'node\t{rev}\tmissing-node',
                       f'hierarchy\t{rev}\t0\t65', f'hierarchy\t{rev}\t{total + 1}\t1',
                       f'hierarchy\t{rev - 1}\t0\t1'], expected=1)
        assert all(not row['ok'] for row in invalid)
        return sizes[0], sizes[-1]

    def move(index, delta):
        item = first['state']['props'][index]
        return f'move-prop\tid=prop-{index}\tx={item["x"] + delta}\ty={item["y"]}\tz={item["z"]}'

    edits = f'batch\t{revision}\t{move(0, .125)}\t|\t{move(1, .25)}'
    def verify_prop(rev, x):
        return f'verify-position\t{rev}\tprop-0\t{x}\t{prop["y"]}\t{prop["z"]}'
    missing = run([verify_prop(revision, prop['x'])])[0]['result']['result']
    assert missing['verified'] and not missing['committed'] and missing['diskState'] == 'MISSING'
    accepted, inspected = run([edits, 'inspect'])
    after_revision = accepted['result']['revision']
    # Compare the bytes an agent receives for the same edit and resulting native state.
    sizes = []
    counts = []
    edit_reply, full_reply, proof_reply, current = run([edits, 'inspect', verify_prop(after_revision, moved_x), 'revision'],
                                                      sizes=sizes, token_counts=counts)
    assert current['result'] == {'sceneId': first['sceneId'], 'revision': after_revision}
    proof = proof_reply['result']
    assert edit_reply == accepted and full_reply['result']['state'] == inspected['result']['state']
    assert proof['revision'] == after_revision and proof['result']['verified'] and proof['result']['committed']
    assert proof['result']['actual'] == next(row for row in full_reply['result']['hierarchy'] if row['id'] == 'prop-0')
    assert sizes[2] < sizes[1]
    response_bytes = {'scene': first['sceneId'], 'revision': after_revision, 'unit': 'UTF-8 response bytes',
                      'editReceipt': sizes[0], 'fullInspection': sizes[1], 'positionProof': sizes[2], 'revisionRead': sizes[3]}
    if counts:
        response_bytes['o200k_base'] = dict(zip(('editReceipt', 'fullInspection', 'positionProof', 'revisionRead'), counts))
        print(f'House response text (o200k_base): localized edit + proof {counts[0]+counts[2]}, edit + inspection {counts[0]+counts[1]}')
    (output / 'localized-edit-response.json').write_text(json.dumps(response_bytes, indent=2) + '\n', encoding='utf-8')
    print(f'House edit plus position proof: {sizes[0]+sizes[2]} bytes; same edit plus full inspection: {sizes[0]+sizes[1]} bytes')
    pages = run([edits, f'changes\t{after_revision}\t0\t1', f'changes\t{after_revision}\t1\t1',
                 f'changes\t{after_revision}\t2\t1'])[1:]
    details = [row['result']['result'] for row in pages]
    assert all(p['total'] == 2 and p['beforeRevision'] == revision for p in details)
    nodes = details[0]['nodes'] + details[1]['nodes']
    assert {n['id'] for n in nodes} == {'prop-0', 'prop-1'} and not details[2]['nodes']
    proof = run([edits, verify_prop(after_revision, moved_x), verify_prop(after_revision, prop['x'])])
    matched, mismatch = [r['result']['result'] for r in proof[1:]]
    assert matched['verified'] and matched['committed'] and matched['actual']['x'] == moved_x
    assert not mismatch['verified'] and mismatch['actual'] == matched['actual']
    assert all(n['kind'] == 'UPDATED' and n['fingerprint'] != n['beforeFingerprint'] for n in nodes)
    undo = run([edits, f'undo\t{after_revision}', f'changes\t{after_revision + 1}\t0\t2'])[-1]
    reversed_nodes = {n['id']: n for n in undo['result']['result']['nodes']}
    for node in nodes:
        inverse = reversed_nodes[node['id']]
        assert inverse['fingerprint'] == node['beforeFingerprint']
        assert inverse['beforeFingerprint'] == node['fingerprint']
    invalid_pages = run([f'changes\t{revision}\t0\t1', edits, f'changes\t{revision}\t0\t1',
                         f'changes\t{after_revision}\t0\t65'], expected=1)
    assert [row['ok'] for row in invalid_pages] == [False, True, False, False]
    after = inspected['result']['state']
    for index, delta in enumerate((.125, .25)):
        assert after['props'][index]['x'] == first['state']['props'][index]['x'] + delta
    restored = run([edits, f'undo\t{after_revision}', 'inspect'])[-1]['result']
    assert restored['state'] == first['state'], 'One native undo reverses both edits'
    bad = run([f'batch\t{revision}\t{move(0, .125)}\t|\tmove-prop\tid=missing-id', 'inspect'], expected=1)
    assert not bad[0]['ok'] and bad[1]['result']['state'] == first['state'], 'Second operation failure rolls back the first'
    malformed = run([edits + '\t|', f'batch\t{revision}\t|\t{move(0, .125)}',
                     f'batch\t{revision}\t' + '\t|\t'.join([move(0, .125)] * 65), 'inspect'], expected=1)
    assert all(not row['ok'] for row in malformed[:-1]) and malformed[-1]['result']['revision'] == revision

    house_sizes = verify_local_reads(first)
    rows = run([
        'inspect',
        f'save\t{revision}',
        f'edit\t{revision}\tmove-prop\tid={prop_id}\tx={moved_x}\ty={prop["y"]}\tz={prop["z"]}',
        f'save\t{revision + 1}',
        f'capture\t{revision + 1}\t{output / "headless-house.png"}',
        f'test\t{revision + 1}\tmission-runtime-parity',
        'inspect',
    ])
    assert all(row['ok'] for row in rows)
    assert rows[-1]['result']['state']['props'][index]['x'] == moved_x
    assert all(check['passed'] for check in rows[-2]['result']['result'])
    assert Path(rows[4]['result']['result']['file']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    live_proof = run([verify_prop(run(['summary'])[0]['result']['revision'], moved_x)])[0]['result']['result']
    assert live_proof['verified'] and not live_proof['committed'] and live_proof['diskState'] == 'MATCHES'
    fresh_revision = run(['summary'])[0]['result']['revision']
    local = run([f'node\t{fresh_revision}\tprop-0'])[0]['result']
    assert local['result']['x'] == moved_x, 'Fresh host localized read observes the saved move'
    reopened = run(['inspect'])[0]['result']
    assert reopened['state'] == rows[-1]['result']['state'], 'Separate real host loads saved authoring state'
    recovered = run([f'recover\t{reopened["revision"]}', 'inspect'])[-1]['result']
    assert recovered['state'] == first['state'], 'Explicit headless recovery restores exact previous native document'
    stale = run([f'edit\t0\tmove-prop\tid={prop_id}\tx=0\ty=0\tz=0', 'inspect'], expected=1)
    assert not stale[0]['ok'] and stale[1]['result']['state'] == recovered['state']
    corrupt_file = Path(recovered['document'])
    corrupt_file.write_text('corrupt scene before host initialization', encoding='utf-8')
    opening = run(['inspect'], expected=1)[0]
    assert not opening['ok'] and opening['phase'] == 'open'
    assert corrupt_file.read_text(encoding='utf-8') == 'corrupt scene before host initialization'
    bootstrapped = run(['inspect'], recover=True)[0]['result']
    assert bootstrapped['state'] == reopened['state'], 'Explicit pre-open recovery restores actual prior mission bytes before creating runtime'
    (output / 'headless-results.json').write_text(json.dumps(rows, indent=2), encoding='utf-8')
    assert unrelated_castle.read_text(encoding='utf-8') == 'format=invalid', 'Mission host never loads or migrates an unrelated Castle scene'
    command[-2:] = ['CASTLE', str(Path(home) / 'castle-project-caf\u00e9')]
    first_castle = run(['inspect'])[0]['result']
    castle_sizes = verify_local_reads(first_castle)
    assert 'RECOVER_PREVIOUS' in first_castle['capabilities']
    assert '\u2022' in json.dumps(first_castle, ensure_ascii=False), 'Native Unicode labels survive UTF-8 transport'
    revision = first_castle['revision']
    bed = next(node for node in first_castle['hierarchy'] if node['id'] == 'upper-bed-0')
    x, y, z = bed['x'] + 1.5, bed['y'], bed['z']
    chair = f'edit\t{revision}\tplace-prop\tid=cli-upper-chair\tmesh=armchair_1\tx={x}\ty={y}\tz={z}'
    placed_revision = run([chair])[0]['result']['revision']
    walk = f'walkability\t{{}}\t{x}\t{y}\t{z}'
    queries = run([walk.format(revision), chair, f'focus\t{placed_revision}\tcli-upper-chair',
                   walk.format(placed_revision), f'walkability\t{placed_revision}\t{x}\t{y}\t{z + .85}'])
    assert [queries[i]['result']['result']['walkable'] for i in (0, 3, 4)] == [True, False, True]
    assert queries[2]['result']['result']['label'].endswith('armchair_1')
    rows_castle = run([f'save\t{revision}',
                       f'edit\t{revision}\tset-spawn\tx=13.5\tz=19\tyaw=1.125',
                       f'save\t{revision + 1}', 'inspect'])
    assert all(row['ok'] for row in rows_castle)
    saved_revision = run(['summary'])[0]['result']['revision']
    saved_spawn = run([f'verify-position\t{saved_revision}\tstart\t13.5\t0\t19'])[0]['result']['result']
    assert saved_spawn['verified'] and not saved_spawn['committed'] and saved_spawn['diskState'] == 'MATCHES'
    castle_change = run([f'edit\t{saved_revision}\tset-spawn\tx=12.5\tz=19\tyaw=1.125',
                         f'changes\t{saved_revision + 1}\t0\t1'])[-1]
    spawn_change = castle_change['result']['result']
    assert not spawn_change['fullScene'] and spawn_change['total'] == 1
    assert spawn_change['nodes'][0]['id'] == 'start' and spawn_change['nodes'][0]['kind'] == 'UPDATED'
    reopened_castle = run(['inspect'])[0]['result']
    assert reopened_castle['state'] == rows_castle[-1]['result']['state']
    recovered_castle = run([f'recover\t{reopened_castle["revision"]}', 'inspect'])[-1]['result']
    assert recovered_castle['state'] == first_castle['state'], 'Castle CLI recovery restores exact native scene'
    corrupt_castle = Path(recovered_castle['document'])
    corrupt_castle.write_text('format=invalid', encoding='utf-8')
    opening = run(['inspect'], expected=1)[0]
    assert not opening['ok'] and opening['phase'] == 'open'
    assert corrupt_castle.read_text(encoding='utf-8') == 'format=invalid'
    bootstrapped = run(['inspect'], recover=True)[0]['result']
    assert bootstrapped['state'] == reopened_castle['state'], 'Explicit pre-open recovery restores actual Castle source before singleton initialization'
    print(f'Full inspection / one node UTF-8 response bytes: House {house_sizes}, Castle {castle_sizes}')
    (output / 'headless-castle-results.json').write_text(json.dumps(rows_castle, indent=2), encoding='utf-8')
print('PASS headless public protocol: mission/Castle JSON edit/save/recovery, renderer capture, native parity, process reopen, corrupt-start bootstrap recovery and stale rejection')
