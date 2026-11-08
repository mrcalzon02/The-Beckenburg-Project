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
args = parser.parse_args()
output = Path(args.output).resolve()
output.mkdir(parents=True, exist_ok=True)

with tempfile.TemporaryDirectory(prefix='editor-agent-cli-') as home:
    command = [args.java, '-Dfile.encoding=windows-1252', '-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory',
               '-cp', args.classpath, 'org.beckenburg.EditorAgentCli', 'HOUSE', home]

    def run(lines, expected=0, recover=False, sizes=None):
        result = subprocess.run(command + (['--recover-previous'] if recover else []), input='\n'.join(lines) + '\n', text=True,
                                encoding='utf-8', capture_output=True, timeout=120)
        assert result.returncode == expected, (result.returncode, result.stderr[-4000:], result.stdout[-1000:])
        if sizes is not None:
            sizes.extend(len(line.encode('utf-8')) for line in result.stdout.splitlines())
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        assert len(rows) == len(lines), (len(rows), len(lines))
        return rows

    unrelated_castle = Path(home) / '.beckenburg' / 'editor' / 'castle.properties'
    unrelated_castle.parent.mkdir(parents=True, exist_ok=True)
    unrelated_castle.write_text('format=invalid', encoding='utf-8')
    first = run(['inspect'])[0]['result']
    assert 'move-prop' in first['operations'] and 'CAPTURE' in first['capabilities']
    prop = first['state']['props'][0]
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
        invalid = run([f'node\t{rev-1}\t{node["id"]}', f'node\t{rev}\tmissing-node',
                       f'hierarchy\t{rev}\t0\t65'], expected=1)
        assert all(not row['ok'] for row in invalid)
        return sizes[0], sizes[-1]

    def move(index, delta):
        item = first['state']['props'][index]
        return f'move-prop\tid=prop-{index}\tx={item["x"] + delta}\ty={item["y"]}\tz={item["z"]}'

    edits = f'batch\t{revision}\t{move(0, .125)}\t|\t{move(1, .25)}'
    accepted, inspected = run([edits, 'inspect'])
    after_revision = accepted['result']['revision']
    pages = run([edits, f'changes\t{after_revision}\t0\t1', f'changes\t{after_revision}\t1\t1',
                 f'changes\t{after_revision}\t2\t1'])[1:]
    details = [row['result']['result'] for row in pages]
    assert all(p['total'] == 2 and p['beforeRevision'] == revision for p in details)
    nodes = details[0]['nodes'] + details[1]['nodes']
    assert {n['id'] for n in nodes} == {'prop-0', 'prop-1'} and not details[2]['nodes']
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
        f'edit\t{revision}\tmove-prop\tid=prop-0\tx={moved_x}\ty={prop["y"]}\tz={prop["z"]}',
        f'save\t{revision + 1}',
        f'capture\t{revision + 1}\t{output / "headless-house.png"}',
        f'test\t{revision + 1}\tmission-runtime-parity',
        'inspect',
    ])
    assert all(row['ok'] for row in rows)
    assert rows[-1]['result']['state']['props'][0]['x'] == moved_x
    assert all(check['passed'] for check in rows[-2]['result']['result'])
    assert Path(rows[4]['result']['result']['file']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    fresh_revision = run(['summary'])[0]['result']['revision']
    local = run([f'node\t{fresh_revision}\tprop-0'])[0]['result']
    assert local['result']['x'] == moved_x, 'Fresh host localized read observes the saved move'
    reopened = run(['inspect'])[0]['result']
    assert reopened['state'] == rows[-1]['result']['state'], 'Separate real host loads saved authoring state'
    recovered = run([f'recover\t{reopened["revision"]}', 'inspect'])[-1]['result']
    assert recovered['state'] == first['state'], 'Explicit headless recovery restores exact previous native document'
    stale = run([f'edit\t0\tmove-prop\tid=prop-0\tx=0\ty=0\tz=0', 'inspect'], expected=1)
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
    rows_castle = run([f'save\t{revision}',
                       f'edit\t{revision}\tset-spawn\tx=13.5\tz=19\tyaw=1.125',
                       f'save\t{revision + 1}', 'inspect'])
    assert all(row['ok'] for row in rows_castle)
    saved_revision = run(['summary'])[0]['result']['revision']
    castle_change = run([f'edit\t{saved_revision}\tset-spawn\tx=12.5\tz=19\tyaw=1.125',
                         f'changes\t{saved_revision + 1}\t0\t1'])[-1]
    assert castle_change['result']['result']['fullScene'], 'Castle reports conservative invalidation'
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
