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

    def run(lines, expected=0, recover=False):
        result = subprocess.run(command + (['--recover-previous'] if recover else []), input='\n'.join(lines) + '\n', text=True,
                                encoding='utf-8', capture_output=True, timeout=120)
        assert result.returncode == expected, (result.returncode, result.stderr[-4000:], result.stdout[-1000:])
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        assert len(rows) == len(lines), (len(rows), len(lines))
        return rows

    first = run(['inspect'])[0]['result']
    assert 'move-prop' in first['operations'] and 'CAPTURE' in first['capabilities']
    prop = first['state']['props'][0]
    moved_x = prop['x'] + .125
    revision = first['revision']
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
    command[-2:] = ['CASTLE', str(Path(home) / 'castle-project-caf\u00e9')]
    first_castle = run(['inspect'])[0]['result']
    assert 'RECOVER_PREVIOUS' in first_castle['capabilities']
    assert '\u2022' in json.dumps(first_castle, ensure_ascii=False), 'Native Unicode labels survive UTF-8 transport'
    revision = first_castle['revision']
    rows_castle = run([f'save\t{revision}',
                       f'edit\t{revision}\tset-spawn\tx=13.5\tz=19\tyaw=1.125',
                       f'save\t{revision + 1}', 'inspect'])
    assert all(row['ok'] for row in rows_castle)
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
    (output / 'headless-castle-results.json').write_text(json.dumps(rows_castle, indent=2), encoding='utf-8')
print('PASS headless public protocol: mission/Castle JSON edit/save/recovery, renderer capture, native parity, process reopen, corrupt-start bootstrap recovery and stale rejection')
