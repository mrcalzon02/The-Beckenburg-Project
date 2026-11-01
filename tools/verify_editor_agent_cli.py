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
    command = [args.java, '-Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory',
               '-cp', args.classpath, 'org.beckenburg.EditorAgentCli', 'HOUSE', home]

    def run(lines, expected=0):
        result = subprocess.run(command, input='\n'.join(lines) + '\n', text=True,
                                encoding='utf-8', capture_output=True, timeout=120)
        assert result.returncode == expected, (result.returncode, result.stderr[-4000:], result.stdout[-1000:])
        rows = [json.loads(line) for line in result.stdout.splitlines()]
        assert len(rows) == len(lines), (len(rows), len(lines))
        return rows

    first = run(['inspect'])[0]['result']
    assert 'move-prop' in first['operations'] and 'CAPTURE' in first['capabilities']
    prop = first['state'][0]
    moved_x = prop['x'] + .125
    revision = first['revision']
    rows = run([
        'inspect',
        f'edit\t{revision}\tmove-prop\tid=prop-0\tx={moved_x}\ty={prop["y"]}\tz={prop["z"]}',
        f'save\t{revision + 1}',
        f'capture\t{revision + 1}\t{output / "headless-house.png"}',
        f'test\t{revision + 1}\tmission-runtime-parity',
        'inspect',
    ])
    assert all(row['ok'] for row in rows)
    assert rows[-1]['result']['state'][0]['x'] == moved_x
    assert all(check['passed'] for check in rows[-2]['result']['result'])
    assert Path(rows[3]['result']['result']['file']).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    reopened = run(['inspect'])[0]['result']
    assert reopened['state'] == rows[-1]['result']['state'], 'Separate real host loads saved authoring state'
    stale = run([f'edit\t0\tmove-prop\tid=prop-0\tx=0\ty=0\tz=0', 'inspect'], expected=1)
    assert not stale[0]['ok'] and stale[1]['result']['state'] == reopened['state']
    (output / 'headless-results.json').write_text(json.dumps(rows, indent=2), encoding='utf-8')
print('PASS headless public protocol: JSON, real edit/save, renderer capture, native parity, process reopen and stale rejection')
